"""Generating the required data for training/inference of the embedding model"""
import itertools
import utils
import numpy as np
import os
from nltk.tokenize import word_tokenize
from citations_extractor import CitationExtractor
import random

stopwords = ['figure', 'fig', 'figure.', 'fig.', '...', ',', '.', '<', '>', '=', ':', ";", '\'', '\"', '!', '@',
             '#', '$', '%', '^', '&', '*', '(', ')', '[', ']', '`', '}', '{', '\``', '\'s', '``', '|']


def text_to_tokens(figure_text):
    """Convert a text representing a figure into tokens.

    Args:
      figure_text: (string) input directory.

    Returns:
      (list). The tokens.
    """
    text = utils.pre_process_nltk(figure_text, additional_stopwords=stopwords)
    words = word_tokenize(text)
    words_filtered = []
    for i, word in enumerate(words):
        word = word.replace('\'', '').replace('.', '').replace('-','').replace('=','').replace('\'','').replace('|','')
        word = word.replace('/','')
        if word not in stopwords and len(word) > 2 and not utils.is_with_number(word) and len(word) < 20:
            words_filtered += [word]
    words = words_filtered
    return words


def extract_all_figures(all_figures_dir, count_threshold=5):
    """Generate textual representations to all figures in the collection.

    Args:
      all_figures_dir: (string) the folder with the textual fields for the figures (the files used to build an index).
      count_threshold: (int) number of words to consider a figure.

    Returns:
      (list, list, list). figure tokens, figure identifiers, figure_image_files.
    """

    text_data, figure_identifiers, image_file_names = [], [], []
    for i, article_file_name in enumerate(os.listdir(all_figures_dir)):
        figure_flag = False
        caption, mention, figure_num, image_file_name = '', '', '', ''

        for line in utils.read_lines_from_file(all_figures_dir + '/' + article_file_name):
            if '<figure>' in line:
                if figure_flag:
                    tokens = text_to_tokens(caption + ' ' + mention)
                    if len(tokens) > count_threshold:
                        text_data.append(tokens)
                        figure_identifiers.append(article_file_name.split('.')[0] + '_' + figure_num)
                        image_file_names.append(image_file_name)
                figure_flag = False
                caption, mention, figure_num, image_file_name = '', '', '', ''

                if '<figure>' in line:
                    figure_num = line.rstrip('\n').rstrip('</figure>').lstrip('<figure>')
                    if len(figure_num) == 2:
                        figure_num = figure_num.replace('.', '')
                    figure_flag = True

            if '<caption>' in line and figure_flag:
                caption = line.rstrip('\n').rstrip('</caption>').lstrip('<caption>')
            if '<lines3>' in line and figure_flag:
                mention = line.rstrip('\n').rstrip('</lines3>').lstrip('<lines3>')
            if '<file>' in line and figure_flag:
                image_file_name = line.rstrip('\n').rstrip('</file>').lstrip('<file>')

        if figure_flag:
            tokens = text_to_tokens(caption + ' ' + mention)
            if len(tokens) > count_threshold:
                text_data.append(tokens)
                figure_identifiers.append(article_file_name.split('.')[0] + '_' + figure_num)
                image_file_names.append(image_file_name)

    return text_data, figure_identifiers, image_file_names


def generate_feature_vectors(all_figures_dir, output_data_folder):
    """Generating feature vectors for figures from text data to be used in a neural model.

    Args:
      all_figures_dir: (string) the folder with the textual fields for the figures (the files used to build an index).
      output_data_folder: (string) a folder to write the different output files.

    Returns:
      (list). Writing the outputs to the output folder, and returning the list of figure identifiers.
    """
    if not os.path.exists(output_data_folder):
        os.mkdir(output_data_folder)

    text_data, figure_identifiers, image_file_names = extract_all_figures(all_figures_dir)

    with open(output_data_folder + '/raw_data.txt', 'w+') as raw_data_file:
        for words in text_data:
            words_encoded = [w for w in words]
            raw_data_file.write(' '.join(words_encoded) + '\n')

    utils.write_lines_to_file(output_data_folder + '/image_files.txt', image_file_names)
    all_tokens = itertools.chain.from_iterable(text_data)
    word_to_id = {token: idx for idx, token in enumerate(set(all_tokens))}
    all_tokens = itertools.chain.from_iterable(text_data)
    id_to_word = [token for idx, token in enumerate(set(all_tokens))]
    id_to_word = np.asarray(id_to_word)

    x_token_ids = [[word_to_id[token] for token in x] for x in text_data]
    count = np.zeros(id_to_word.shape)
    for x in x_token_ids:
        for token in x:
            count[token] += 1
    indices = np.argsort(-count)
    id_to_word = id_to_word[indices]
    word_to_id = {token: idx for idx, token in enumerate(id_to_word)}
    x_token_ids = [[word_to_id.get(token, -1) + 1 for token in x] for x in text_data]

    np.save(output_data_folder + '/words_map.npy', np.asarray(id_to_word))

    with open(output_data_folder + '/vectors.txt', 'w+') as f:
        for tokens in x_token_ids:
            for token in tokens:
                f.write(str(token) + ' ')
            f.write("\n")

    with open(output_data_folder + '/identifiers.txt', 'w+') as identifier_file:
        for identifier in figure_identifiers:
            identifier_file.write(identifier + '\n')

    return figure_identifiers


def generate_figure_pairs(figure_identifiers, grobid_files_dir, output_directory):
    """Generate figure pairs for Siamese network training.

    Args:
      figure_identifiers: (list) the full data set of figures (identifiers).
      grobid_files_dir: (string) a folder with the Grobid output (the raw output, no pre-processing).
      output_directory: (string) directory for the output.

    Returns:
      None. Writing the outputs to the output file.
    """

    # Get pairs of figures that are in the same paper.
    article_figures_dict = {}
    for figure_id in figure_identifiers:
        article_id = figure_id.split('_')[0]
        article_figures_dict[article_id] = article_figures_dict.get(article_id, []) + [figure_id]

    same_article_pairs = []
    for article_id in article_figures_dict:
        for i in range(len(article_figures_dict[article_id])):
            for j in range(i+1, len(article_figures_dict[article_id])):
                first_figure = figure_identifiers.index(article_figures_dict[article_id][i])
                second_figure = figure_identifiers.index(article_figures_dict[article_id][j])
                same_article_pairs.append((first_figure, second_figure))

    # Get pairs of figures that are in citing papers.
    ce = CitationExtractor(grobid_files_dir)
    citations = ce.get_all_citations()
    citing_article_pairs = []
    for article_id in citations:
        for other_article in citations[article_id]:
            figure_citations = utils.get_all_figure_pairs(article_id, other_article, figure_identifiers)
            for pair in figure_citations:
                citing_article_pairs.append((pair[0], pair[1]))

    # Sampling negative random pairs to balance the data set.
    negative_pairs = []
    for _ in range(len(same_article_pairs)+len(citing_article_pairs)):
        first_figure = random.choice(range(len(figure_identifiers)))
        second_figure = random.choice(range(len(figure_identifiers)))
        negative_pairs.append((first_figure, second_figure))

    # The output format is: first_figure_id, second_figure_id, relevance_label, relevance_score
    with open(output_directory, 'w+') as output_file:
        for pair in citing_article_pairs:
            output_file.write(str(pair[0]) + ',' + str(pair[1]) + ',1,0.6\n')
        for pair in same_article_pairs:
            output_file.write(str(pair[0]) + ',' + str(pair[1]) + ',1,1\n')
        for pair in negative_pairs:
            output_file.write(str(pair[0]) + ',' + str(pair[1]) + ',0,0\n')


def main():
    all_figures_dir = '../small_dataset/acl_figures'
    grobid_files_dir = '../small_dataset/grobid_files'

    output_data_folder = 'example_embedding_dataset'
    figure_identifiers = generate_feature_vectors(all_figures_dir, output_data_folder)
    generate_figure_pairs(figure_identifiers, grobid_files_dir, output_data_folder + '/pairs.txt')

if __name__ == '__main__':
    main()