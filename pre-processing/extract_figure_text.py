"""Building textual representation of figures from articles"""
import os
import json
from nltk.tokenize import sent_tokenize
import utils

figure_formats = ['fig', 'figure', 'fig.', 'figure.']


def get_figure_ids_list(grobid_article_dir):
    """Get the list of all figure identifiers in a single article.

    Args:
      grobid_article_dir: (string) the directory the processed grobid file of the article.
    Returns:
      (list). The list of figure identifiers (numbers stored as strings).
    """
    figure_list = []
    doc_txt = ''

    lines = utils.read_lines_from_file(grobid_article_dir)
    for line in lines:
        line = line.rstrip('\n')
        doc_txt += line + ' '
    doc_txt = doc_txt.rstrip(' ')
    words = doc_txt.split(' ')

    for word_id, word in enumerate(words):
        word = utils.process_line(word)
        if len(word) == 0:
            continue

        word = word[0]
        if word.split('.')[0].lower() in figure_formats:
            if len(word.split('.')) > 1:
                word_length = len(word.split('.'))
                figure_num = utils.extract_number('.'.join(word.split('.')[1:word_length]))
            elif len(words) > word_id + 1:
                figure_num = utils.extract_number(words[word_id + 1])
            else:
                continue

            if figure_num not in figure_list and figure_num != '':
                figure_list += [figure_num]
    return figure_list


def get_captions(grobid_article_dir):
    """Get the captions of all figures from the Grobid processed files.

    Args:
      grobid_article_dir: (string) the directory the processed grobid file of the article.
    Returns:
      (dictionary). Mapping from figure identifiers to their caption.
    """
    figure_caption_dict = {}
    article_lines = utils.read_lines_from_file(grobid_article_dir)
    fig_captions_flag = False

    for line_id, line in enumerate(article_lines):
        if '<figcaptions>' in line:
            fig_captions_flag = True
            continue

        if '<title>' in line:
            break

        if not fig_captions_flag:
            continue

        words = utils.process_line(line)
        for i, word in enumerate(words):
            figure_flag = False
            for fig_format in figure_formats:
                if fig_format in word.lower():
                    figure_flag = True
            if not figure_flag:
                continue

            figure_num = utils.extract_number(words[i])

            try:
                int(figure_num)
            except:
                if len(words) > i + 1:
                    figure_num = utils.extract_number(words[i + 1])
                else:
                    continue

            if figure_num not in figure_caption_dict.keys():
                figure_caption_dict[figure_num] = []
            if line_id not in figure_caption_dict[figure_num]:
                figure_caption_dict[figure_num] += [line_id]

    return figure_caption_dict


def get_figures_positions(grobid_article_dir):
    """Get the exact positions of figure mentions in the text.

    Args:
      grobid_article_dir: (string) the directory the processed grobid file of the article.
    Returns:
      (dictionary of lists). Mapping from figure identifiers to the list of indexes in the text where the mentions are.
    """
    figures_dict = {}

    doc_txt = ''
    with open(grobid_article_dir, 'r') as input_file:
        for line in input_file:
            line = line.rstrip('\n')
            doc_txt += line + ' '
        doc_txt = doc_txt.rstrip(' ')
    words = doc_txt.split(' ')

    for word_id, word in enumerate(words):

        if '<figcaptions>' in word:
            break

        word = utils.process_line(word)
        if len(word) == 0:
            continue

        word = word[0]

        if word.split('.')[0].lower() in figure_formats:
            word = word.replace('III', '3').replace('II', '2').replace('I', '1')
            if len(word.split('.')) > 1:
                word_length = len(word.split('.'))
                figure_num = utils.extract_number('.'.join(word.split('.')[1:word_length]))
            else:
                figure_num = utils.extract_number(words[word_id+1].replace('III','3').replace('II','2').replace('I','1'))

            figures_dict[figure_num] = figures_dict.get(figure_num, []) + [word_id]
    return figures_dict


def get_figure_ids_line_pos(grobid_article_dir):
    """Get the line numbers of lines in the text that have the figure mentions.

    Args:
      grobid_article_dir: (string) the directory the processed grobid file of the article.
    Returns:
      (dictionary of lists). Mapping from figure identifiers to the list of indexes in the text where the mentions are.
    """
    figures_dict = {}
    doc_txt = ''
    with open(grobid_article_dir, 'r') as input_file:
        for line in input_file:
            line = line.rstrip('\n')
            doc_txt += line + ' '
        doc_txt = doc_txt.rstrip(' ')

    doc_txt = utils.process_text(doc_txt)
    sentences = sent_tokenize(doc_txt)

    for sentence_id, sentence in enumerate(sentences):
        if '<figcaptions>' in sentence:
            break

        words = sentence.split()
        for word_id, word in enumerate(words):
            if word.split('.')[0].lower().lstrip('(').rstrip(')') in figure_formats:
                word = word.replace('III', '3').replace('II', '2').replace('I', '1')
                if len(word.split('.')) > 1:
                    word_length = len(word.split('.'))
                    fig_num = utils.extract_number('.'.join(word.split('.')[1:word_length]))
                else:
                    fig_num = utils.extract_number(words[word_id+1].replace('III','3').replace('II','2').replace('I','1'))

                figures_dict[fig_num] = figures_dict.get(fig_num, []) + [sentence_id]
    return figures_dict


def parse_json(pdffigures_article_dir):
    """Extract the figure information from the output of the pdffigures toolkit.

    Args:
      pdffigures_article_dir: (string) the directory the pdffigures file of the article.
    Returns:
      (dictionary of tuples). Mapping from figure identifiers its caption and the image file directory.
    """
    figures_dict = {}

    json_file = open(pdffigures_article_dir)
    parsed = json.load(json_file)
    for element in parsed:
        number = utils.roman_converter(element['name'])
        caption = element['caption']

        num_args = len(element['renderURL'].split('/'))
        image_file_dir = element['renderURL'].split('/')[num_args-1]
        figure_type = element['figType'].lower()

        if figure_type == 'figure':
            figures_dict[number] = (caption, image_file_dir)

    json_file.close()
    return figures_dict


def get_article_field(grobid_article_dir, field_name):
    """Get a specific article field of the figure's article.

    Args:
      grobid_article_dir: (string) the directory the processed Grobid file of the article.
      field_name: (string) the field name such as 'introduction'
    Returns:
      (string). The content of the textual field.
    """
    output = ''
    predicate = False
    lines = utils.read_lines_from_file(grobid_article_dir)
    for line in lines:
        if predicate:
            output = line.rstrip('\n')
            predicate = False
        if '<' + field_name + '>' in line:
            predicate = True
    return output


def merge_texts(text_indexes):
    """Merge overlapping textual representations of a figure.

    Args:
      text_indexes: (list of lists of int) the textual representation represented by indexes.
    Returns:
      (list). The merged representation.
    """
    merged_summaries = []
    change_flag = False

    while len(text_indexes) > 0:
        current_summary = set(text_indexes[0])
        indexes_to_remove = []
        for i in range(1,len(text_indexes)):
            other_summary = set(text_indexes[i])
            if len(current_summary.intersection(other_summary)) > 0:
                current_summary = current_summary.union(other_summary)
                indexes_to_remove += [i]
                change_flag = True

        text_indexes[0] = list(current_summary)
        text_indexes = utils.remove_list_element(text_indexes, indexes_to_remove)

        if not change_flag:
            merged_summaries += [sorted(list(current_summary))]
            text_indexes = utils.remove_list_element(text_indexes, [0])

        change_flag = False

    return merged_summaries


def get_figure_mentions_by_words(grobid_article_dir, figure_num, window_size, captions_dict, mentions_dict):
    """Get the text of figure mentions in the article using word windows.

    Args:
      grobid_article_dir: (string) the directory the processed Grobid file of the article.
      figure_num: (string) the figure number.
      window_size: (int) the number of words that should surround a figure mention.
      captions_dict: (dictionary) the captions of the figures.
      mentions_dict: (dictionary) the locations in the texts where the mentions are.

    Returns:
      (list). The merged representation.
    """
    mentions_text = '\"...'
    caption_text = ''

    article_lines = utils.read_lines_from_file(grobid_article_dir)
    doc_txt = ''

    for line in article_lines:
        line = line.rstrip('\n')
        doc_txt += line + ' '
    doc_txt = doc_txt.rstrip(' ')
    words = doc_txt.split(' ')

    if str(figure_num) in captions_dict.keys():
        for caption_id in captions_dict[str(figure_num)]:
            if len(article_lines[caption_id]) < len(caption_text) or len(caption_text) == 0:
                caption_text = article_lines[caption_id] + ' '
        caption_text = caption_text.rstrip(' ')
        caption_text = utils.process_caption(caption_text, figure_num)

    if str(figure_num) in mentions_dict.keys():
        all_summary_ids = []
        for word_id in mentions_dict[str(figure_num)]:
            summary_ids = []
            for i in range(window_size, 0, -1):
                summary_ids += [word_id - i]

            summary_ids += [word_id]
            for i in range(1, window_size+1):
                summary_ids += [word_id + i]

            all_summary_ids += [summary_ids]

        all_summary_ids = merge_texts(all_summary_ids)

        for summary_ids in all_summary_ids:
            for i in summary_ids:
                if i < 0 or i >= len(words):
                    continue
                mentions_text += ' ' + words[i]
            mentions_text += ' ... '
    mentions_text = mentions_text.rstrip(' ') + '\"'

    return caption_text, mentions_text


def get_figure_mentions_by_lines(grobid_article_dir, figure_num, length, mentions_dict):
    """Get the text of figure mentions in the article using sentence windows.

    Args:
      grobid_article_dir: (string) the directory the processed Grobid file of the article.
      figure_num: (string) the figure number.
      length: (int) the number of lines that should surround a figure mention.
      mentions_dict: (dictionary) the locations of sentences in the texts where the mentions are.

    Returns:
      (list). The merged representation.
    """
    mentions_text = ''
    doc_txt = ''
    with open(grobid_article_dir, 'r') as input_file:
        for line in input_file:
            line = line.rstrip('\n')
            doc_txt += line + ' '
        doc_txt = doc_txt.rstrip(' ')

    doc_txt = utils.process_text(doc_txt)
    sentences = sent_tokenize(doc_txt)

    if str(figure_num) in mentions_dict.keys():
        all_summary_ids = []
        for sentence_id in mentions_dict[str(figure_num)]:

            summary_ids = []
            for i in range(length, 0, -1):
                summary_ids += [sentence_id - i]
            summary_ids += [sentence_id]
            for i in range(1, length+1):
                summary_ids += [sentence_id + i]
            all_summary_ids += [summary_ids]
        all_summary_ids = merge_texts(all_summary_ids)

        for summary_ids in all_summary_ids:
            for i in summary_ids:
                if i < 0 or i >= len(sentences):
                    continue
                mentions_text += ' ' + sentences[i]
            mentions_text += ' ... '
    mentions_text = mentions_text.rstrip(' ...') + '\"'
    return str(mentions_text)


def extract_all_article_figures_text(article_name, pdffigures_dir, grobid_dir, output_dir):
    """Generate a textual representation of all figures in the article to be indexed by Lucene.

    Args:
      article_name: (string) the name of the article (usually the pdf file name).
      pdffigures_dir: (string) the folder with the outputs of the pdffigures tool.
      grobid_dir: (string) the folder of the Grobid output after processing.
      output_dir: (dictionary) folder to output the text.

    Returns:
      None. Outputs to a file.
    """

    grobid_article_dir = grobid_dir + '/' + article_name + '.tei.xml'
    figures_list = get_figure_ids_list(grobid_article_dir)
    figures_caption_dict = get_captions(grobid_article_dir)
    figures_position_dict = get_figures_positions(grobid_article_dir)
    figures_line_position_dict = get_figure_ids_line_pos(grobid_article_dir)

    json_figure_dict = {}
    json_file_dir = pdffigures_dir + '/' + article_name.split('.')[0] + '.json'
    if os.path.exists(json_file_dir):
        json_figure_dict = parse_json(json_file_dir)

    abstract_field = get_article_field(grobid_article_dir, 'abstract')
    intro_field = get_article_field(grobid_article_dir, 'introduction')
    title_field = get_article_field(grobid_article_dir, 'title')

    output_file = open(output_dir + '/' + article_name + '.txt', 'w+')
    for figure_num in figures_list:
        if len(figure_num) == 0:
            continue

        output_file.write('<figure>' + str(figure_num) + '</figure>\n')
        caption_text, mentions_text_50 = get_figure_mentions_by_words(grobid_article_dir, figure_num, 50, figures_caption_dict, figures_position_dict)
        _, mentions_text_20 = get_figure_mentions_by_words(grobid_article_dir, figure_num, 20, figures_caption_dict, figures_position_dict)
        _, mentions_text_10 = get_figure_mentions_by_words(grobid_article_dir, figure_num, 10, figures_caption_dict, figures_position_dict)
        lines_3 = get_figure_mentions_by_lines(grobid_article_dir, figure_num, 1, figures_line_position_dict)
        lines_5 = get_figure_mentions_by_lines(grobid_article_dir, figure_num, 2, figures_line_position_dict)

        if str(figure_num) in json_figure_dict.keys():
            caption_text = json_figure_dict[str(figure_num)][0]
        else:
            caption_text = 'Figure ' + figure_num + '. ' + caption_text

        output_file.write('<caption>' + caption_text + '</caption>\n')
        output_file.write('<mention10>' + mentions_text_10 + '</mention10>\n')
        output_file.write('<mention20>' + mentions_text_20 + '</mention20>\n')
        output_file.write('<mention50>' + mentions_text_50 + '</mention50>\n')
        output_file.write('<lines3>' + lines_3 + '</lines3>\n')
        output_file.write('<lines5>' + lines_5 + '</lines5>\n')
        output_file.write('<snippet3>' + lines_3.split(' ... ')[0] + '</snippet3>\n')
        output_file.write('<snippet5>' + lines_5.split(' ... ')[0] + '</snippet5>\n')
        output_file.write('<abstract>' + abstract_field + '</abstract>\n')
        output_file.write('<title>' + title_field + '</title>\n')
        output_file.write('<introduction>' + intro_field + '</introduction>\n')

        if str(figure_num) in json_figure_dict:
            output_file.write('<file>' + json_figure_dict[str(figure_num)][1] + '</file>\n')

    for fig_num in json_figure_dict.keys():
        if fig_num not in figures_list:
            try:
                output_file.write('<figure>' + fig_num.encode('utf-8') + '</figure>\n')
                output_file.write('<caption>' + json_figure_dict[fig_num][0].encode('utf-8') + '</caption>\n')
                output_file.write('<file>' + json_figure_dict[str(fig_num)][1].encode('utf-8') + '</file>\n')
            except:
                print("failed figure")
                continue

    output_file.close()