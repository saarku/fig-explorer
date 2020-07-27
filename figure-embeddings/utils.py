"""Different utilities for the embedding module"""
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
from nltk import stem
import numpy as np


def write_lines_to_file(output_dir, lines):
    """Write lines into an output file.

    Args:
      output_dir: (string) the directory for the output.
      lines: (list) the output lines.
    Returns:
      (list). The list of figure identifiers (numbers stored as strings).
    """
    output = ''
    with open(output_dir, 'w') as output_file:
        for line in lines:
            output += str(line) + '\n'
        output = output.rstrip('\n')
        output_file.write(output)


def read_lines_from_file(input_dir):
    """Read text file and put its lines in a list.

    Args:
      input_dir: (string) input directory.
    Returns:
      (list). The file lines.
    """
    lines = []
    with open(input_dir, 'r') as input_file:
        for line in input_file:
            lines += [line.rstrip('\n')]
    return lines


def is_with_number(string_val):
    """Checks if a word contains a number.

    Args:
      string_val: (string) input word.

    Returns:
      (bool).
    """
    for char in string_val:
        try:
            int(char)
        except ValueError:
            continue
        else:
            return True
    return False


def pre_process_nltk(text, additional_stopwords=None):
    """Pre-processing text using the NLTK library.

    Args:
      text: (string) input text.
      additional_stopwords: (list) stopwords to add on top of the default ones.

    Returns:
      (string). The processed text.
    """
    text = text.lower()
    stop_words = set(stopwords.words('english'))
    if additional_stopwords is not None:
        stop_words = stop_words.union(set(additional_stopwords))

    word_tokens = word_tokenize(text)
    word_tokens = [w for w in word_tokens if not w in stop_words]
    stemmer = stem.PorterStemmer()
    stemmed_text = [stemmer.stem(w) for w in word_tokens]
    filtered_text = [w for w in stemmed_text if not w in stop_words]
    return " ".join(filtered_text)


def get_all_figure_pairs(article_id_1, article_id_2, figure_identifiers):
    """Get all figure pairs of two articles.

    Args:
      article_id_1: (string) first article id.
      article_id_2: (string) second article id.
      figure_identifiers: (list) the list of all figure identifiers.

    Returns:
      (list). All possible figure pairs.
    """
    figures_1 = [figure_id for figure_id in figure_identifiers if article_id_1 in figure_id]
    figures_2 = [figure_id for figure_id in figure_identifiers if article_id_2 in figure_id]

    pairs_list = []
    for figure_1 in figures_1:
        for figure_2 in figures_2:
            pairs_list.append((figure_identifiers.index(figure_1), figure_identifiers.index(figure_2)))
    return pairs_list


def min_max_norm(result_list):
    """MinMax normalization of a result list.
    Args:
        result_list: (list) a result list.

    Returns:
       Dictionary with normalized scores.
    """
    normalized_res = {}
    for test_id in result_list:
        normalized_res[test_id] = {}
        min_val = min([i[1] for i in result_list[test_id]])
        max_val = max([i[1] for i in result_list[test_id]])
        for element in result_list[test_id]:
            if min_val == max_val:
                normalized_res[test_id][element[0]] = element[1]
            else:
                normalized_res[test_id][element[0]] = (element[1] - min_val) / (max_val - min_val)
    return normalized_res


def load_embeddings(embeddings_dir):
    """Loading the embedding vectors.

    Args:
        embeddings_dir: (string) a directory for the embeddings.
    Returns:
        An embeddings matrix.
    """
    all_embeddings = []

    with open(embeddings_dir, 'r') as input_file:
        for line in input_file:
            all_embeddings.append([float(i) for i in line.rstrip('\n').split()])
    all_embeddings = np.asarray(all_embeddings)
    return all_embeddings