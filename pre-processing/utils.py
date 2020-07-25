"""Different utilities for processing textual data"""
import re


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


def clean_xml_tags(text):
    """Remove XML tags from text.

    Args:
      text: (string) input text.
    Returns:
      (string). The same text without XML tags.
    """
    tag_re = re.compile(r'<[^>]+>')
    text = tag_re.sub('', text)
    return text


def process_line(line_str):
    """Processing of a single sentence.

    Args:
      line_str: (string) the sentence.
    Returns:
      (string). The processed sentence.
    """
    punctuation = [',', ':', ')']
    for mark in punctuation:
        line_str = line_str.replace(mark, ' ')
    line_str = line_str.replace('(', '')
    line_str = line_str.replace('III', '3').replace('II', '2').replace(' I ', '1')
    words = [w for w in line_str.split(' ') if w != '']
    return words


def extract_number(word):
    """Get a number that is concatenated with other text (e.g., "Figure9").

    Args:
      word: (string) the word that might contain the number.
    Returns:
      (string). The number stored as string.
    """
    number_flag = True
    number = ''
    word = word.rstrip('.').lstrip('.')
    for char in word:
        try:
            if char == '.' and number_flag:
                number += char
            else:
                int(char)
                if number_flag:
                    number += char
        except:
            if len(number) > 0:
                number_flag = False
            continue
    return number


def process_text(text):
    """Process text to make it look more presentable.

    Args:
      text: (string) the text.
    Returns:
      (string). The processed text.
    """
    fix_dict = {'fig.': 'fig', 'fig .': 'fig ', 'Fig.': 'Fig', 'Fig .': 'Fig ',
                'figure.': 'figure', 'figure .': 'figure ', 'Figure.': 'Fig', 'Figure .': 'Fig ',
                'et al.': 'et al', 'III': '3', 'II': '2', 'I': '1'}

    for old_pattern in fix_dict.keys():
        text = text.replace(old_pattern, fix_dict[old_pattern])
    return text


def roman_converter(number):
    """Convert roman number to regular number.

    Args:
      number: (string) the roman number.
    Returns:
      (string). The regular number.
    """
    num_dict = {'I': '1', 'II': '2', 'III': '3', 'IIII': '4', 'V': '5', 'VI': '6', 'VII': '7', 'VIII': '8', 'VIIII': '9', 'X': '10'}
    if number in num_dict:
        return num_dict[number]
    return number


def process_caption(caption, figure_num):
    """Process the figure caption to make it look nicer.

    Args:
      caption: (string) the caption text.
      figure_num: (string) the figure number.

    Returns:
      (string). The processed caption.
    """
    caption = caption.replace('III', '3').replace('II', '2').replace(' I ', '1').replace('I:', '1:')
    caption = caption.lower().split(figure_num)[1].split('figure')[0].split('fig')[0].split('table')[0]
    caption = caption.split('....')[0]
    caption = caption.lstrip(' .').lstrip(' :')
    return caption


def remove_list_element(input_list, indexes):
    """Remove specific indexes from a list.

    Args:
      input_list: (list).
      indexes: (list).

    Returns:
      (list). The list with the removed items.
    """
    new_list = []
    for i, element in enumerate(input_list):
        if i in indexes:
            continue
        new_list += [input_list[i]]
    return new_list
