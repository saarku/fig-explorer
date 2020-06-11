import re
import nltk.data


class SearchEngineInterface:
    """This class is in charge of communicating with the Lucene search engine."""

    def __init__(self, server, web_utils):

        self.server = server
        self.sentence_splitter = nltk.data.load('tokenizers/punkt/english.pickle')
        self.search_engines = {}
        self.configurations = web_utils.configurations_dict
        self.web_utils = web_utils
        self.same_article_figures = {}

        for index_tuple in web_utils.configurations_dict[web_utils.INDEX_DIR]:
            index_name = index_tuple[0] + '_' + index_tuple[1]
            self.search_engines[index_name] = server.getSearchEngine(index_name)

        self.get_same_article_figures()

    def get_same_article_figures(self):
        for index_name in self.search_engines.keys():
            collection_name = index_name.split('_')[0]
            if collection_name not in self.same_article_figures:
                self.same_article_figures[collection_name] = self.search_engines[index_name].getArticleFigures()

    @staticmethod
    def check_duplications(mention, caption, mentions, captions):
        """Checking if a figure is already in the result list.

        Args:
          mention: (string) the text describing the figure.
          caption: (string) the caption of the figure.
          mentions: (list) the list of figure texts in the result list.
          captions: (list) the list of figure captions in the result list.
        Returns:
          (boolean). True if there is no duplication.
        """
        for i in range(len(captions)):
            curr_mention = mentions[i]
            curr_caption = captions[i]
            if curr_caption == caption and curr_mention == mention:
                return False
        return True

    @staticmethod
    def trim_snippet(snippet):
        """Styling the figure snippet for better presentation.

        Args:
          snippet: (string) the figure snippet.
        Returns:
          (string). The output snippet for display.
        """
        limit = min(len(snippet), 500)
        snippet = snippet.replace('1n', 'In').replace('1t', 'It')

        try:
            i = re.search('[A-Z]', snippet).start()
            snippet = snippet[i:len(snippet)]
            short_text = snippet[0:limit].rstrip('.')
            args = short_text.split()
            short_text = ' '.join(args[0:len(args)-1]) + ' ...'
            return short_text
        except:
            return '...'

    @staticmethod
    def extract_number(word):
        """Extracting a number from a string to be highlighted in the snippet.

        Args:
          word: (string) the input string.
        Returns:
          (string). The string of the number.
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

    def search(self, query, collection, model, field_weights):
        """Searching for figures using a text query.

        Args:
          query: (string) the keyword query.
          collection: (string) the name of the collection.
          model: (string) the name of the ranking model.
          field_weights: (dictionary) a mapping between figure fields and their weights.
        Returns:
          (java ScoreDoc[]). The java result list.
          (java SearchEngine). The java search engine which was used.
        """
        search_engine = self.search_engines[collection + '_' + model]
        results = search_engine.searchWithFields(query, 1000, field_weights)
        return results, search_engine

    def get_figures_content(self, figure_ids, collection, model):
        """Get the figure fields content of a set of figures.

        Args:
          figure_ids: (list) a list of figure identifiers.
          collection: (string) the name of the collection.
          model: (string) the name of the ranking model.
        Returns:
          (java HashMap). A map with the content.
        """
        content = self.search_engines[collection + '_' + model].getFiguresSetContent(figure_ids)
        return content

    def get_pdf_url(self, paper_name):
        """Get the figure fields content of a set of figures.

        Args:
          paper_name: (string) the name of a research article.
        Returns:
          (string). The url of the article's name.
        """
        pdf_name = paper_name + '.pdf'
        return self.configurations[self.web_utils.APP_URL][0][0] + 'pdf/' + pdf_name

    def boldface_mention(self, text, query, figure_num):
        """Bold-facing the mentions of a figure and the query terms in the snippet.

        Args:
          text: (string) the text of the snippet.
          query: (string) the keyword query.
          figure_num: (string) the figure number.

        Returns:
          (string). The url of the article's name.
        """
        keywords = [w.lower() for w in query.split()]
        names = ['fig', 'figure', 'fig.', 'figure.']
        words = text.split()

        for i, word in enumerate(words):
            second_word_predicate = False
            for name in names:
                if name in word.lower():
                    number = self.extract_number(word)
                    if len(number) == 0 and i + 1 < len(words):
                        number = self.extract_number(words[i+1])
                        second_word_predicate = True
                    if number == figure_num:
                        words[i] = '<b>' + word + '</b>'
                        if i + 1 < len(words) and second_word_predicate:
                            words[i + 1] = '<b>' + words[i + 1] + '</b>'

            for keyword in keywords:
                if keyword in word.lower():
                    words[i] = '<b>' + word + '</b>'
                    break

        return ' '.join(words).rstrip("\"").rstrip(".") + "."

    def process_caption(self, caption):
        """Pre-processing the figure caption to display in the App.

        Args:
          caption: (string) the figure caption.

        Returns:
          (string). The figure caption for display.
        """
        sentences = self.sentence_splitter.tokenize(caption.strip())
        try:
            limit = min(len(sentences), 2)
            return ' '.join(sentences[0:limit]).rstrip('.') + '.'
        except:
            return ''
