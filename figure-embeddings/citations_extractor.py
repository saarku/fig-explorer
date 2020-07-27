"""Extracting citation relations between figures"""
import os


class CitationExtractor:

    def __init__(self, grobid_files_dir):
        self.grobid_files_dir = grobid_files_dir

    def extract_article_title(self, article_name):
        """Extracting the article's title.

        Args:
          article_name: (string) the name of the article (pdf file name).

        Returns:
          (string). The title.
        """
        file_flag = "<fileDesc>"
        file_close_flag = "</fileDesc>"
        title_flag = "<title level=\"a\" type=\"main\">"
        title_close_flag = "</title>"

        with open(self.grobid_files_dir + '/' + article_name) as input_file:
            file_flag_predicate = False
            title = ''

            for line in input_file:
                if file_close_flag in line:
                    break
                if file_flag in line:
                    file_flag_predicate = True
                if title_flag in line and file_flag_predicate:
                    file_flag_predicate = False
                    title = line.rstrip('\n').replace(title_close_flag, '').replace(title_flag, '')

        return title.lstrip().rstrip()

    def build_titles_dict(self):
        """Building a dictionary that maps article title to its name.

        Returns:
          (dictionary).
        """
        titles_dict = {}
        for file_name in os.listdir(self.grobid_files_dir):
            title = self.extract_article_title(file_name)
            titles_dict[title.lower()] = file_name
        return titles_dict

    def get_article_references(self, article_name):
        """Get the article references (titles).

        Args:
          article_name: (string) the name of the article (pdf file name).

        Returns:
          (list). List of the references' titles.
        """
        bib_flag = '<listBibl>'
        bib_end_flag= '</listBibl>'
        title_flag = '<title level=\"a\" type=\"main\">'
        title_end_flag = '</title>'

        with open(self.grobid_files_dir + '/' + article_name) as input_file:
            bib_flag_predicate = False
            titles = []

            for line in input_file:

                if bib_end_flag in line:
                    break

                if bib_flag in line:
                    bib_flag_predicate = True

                if title_flag in line and bib_flag_predicate:
                    titles += [line.rstrip('\n').replace(title_end_flag, '').replace(title_flag, '').lstrip().rstrip()]
        return titles

    def get_references_dict(self):
        """Get a dictionary mapping article names to their citations (paper titles).

        Returns:
          (dictionary).
        """
        ref_dict = {}
        for file_name in os.listdir(self.grobid_files_dir):
            ref_dict[file_name] = self.get_article_references(file_name)
        return ref_dict

    def get_all_citations(self):
        """Get the citation network.

        Returns:
          (dictionary). Mapping between articles and their references.
        """
        citations = {}
        titles = self.build_titles_dict()
        ref_dict = self.get_references_dict()

        for title in titles:
            file_name = titles[title]
            references = []
            for reference in ref_dict[file_name]:
                ref_id = titles.get(reference.lower(), '')
                if ref_id != '':
                    references += [ref_id.split('.')[0]]
            citations[file_name.split('.')[0]] = references
        return citations
