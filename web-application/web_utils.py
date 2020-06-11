
def read_lines_from_file(input_dir):
    """Read a text file and put lines in a list.

    Args:
      input_dir: (string) the input text file.
    Returns:
      (list). the file lines.
    """
    lines = []
    with open(input_dir, 'r') as input_file:
        for line in input_file:
            lines += [line.rstrip('\n')]
    return lines


class WebUtils:
    """Different utilities that are used by the web server."""

    def __init__(self, configuration_dir):
        self.configurations_dict = self.parse_parameters_file(configuration_dir)

        # The names of different figure fields that are used by the web server.
        self.CAPTION_FIELD = self.configurations_dict.get('captionField', [('caption',)])[0][0]
        self.FIGURE_FIELD = self.configurations_dict.get('figureIdField', [('figure',)])[0][0]
        self.PAPER_FIELD = self.configurations_dict.get('paperIdField', [('paper',)])[0][0]
        self.FILE_FIELD = self.configurations_dict.get('imageFileField', [('file',)])[0][0]
        self.SNIPPET_FIELD = self.configurations_dict.get('snippetField', [('snippet5',)])[0][0]
        self.TITLE_FIELD = self.configurations_dict.get('titleField', [('title',)])[0][0]
        self.ABSTRACT_FIELD = self.configurations_dict.get('abstractField', [('abstract',)])[0][0]
        self.SCORE_FIELD = 'score'

        # The names of the different fields in the configuration file.
        self.WEB_APP_PORT = 'webApplicationPortNumber'
        self.LUCENE_PORT_NUM = 'lucenePortNumber'
        self.WEB_APP_HOST = 'webApplicationHost'
        self.APP_URL = 'applicationUrl'
        self.IMAGE_FILE_DIR = 'imageFilesDir'
        self.EMBEDDINGS_DIR = 'embeddingsDir'
        self.RELATED_FIGURES_DIR = 'relatedFiguresDir'
        self.INDEX_DIR = 'indexDir'
        self.PDF_DIR = 'pdfFolderDir'

    @staticmethod
    def parse_parameters_file(input_dir):
        """Parsing the configuration file for the App.

        Args:
          input_dir: (string) the configuration file.
        Returns:
          (dictionary). a mapping between parameter names and their values.
        """
        parameter_dict = {}
        lines = read_lines_from_file(input_dir)
        for line in lines:
            args = line.rstrip('\n').split('=')
            if len(args) == 2:
                parameter_dict[args[0]] = [tuple(element.split(',')) for element in args[1].split(';')]
        return parameter_dict

    def build_settings_menu(self):
        """Building the "settings" menu in the App based on the configuration file.

        Returns:
          (string). html code of the "settings" menu.
          (string). the names of figure fields (separated with comma).
          (string). the names of initial figure weights (separated with comma).
          (string). the names of similarity functions (separated with comma).
        """
        field_names, field_weights, field_checked, similarity_functions = [], [], [], []
        raw_fields_data = self.configurations_dict.get('fields', [])
        raw_index_data = self.configurations_dict.get('indexDir', [])

        assert len(raw_fields_data) > 0, 'You must specify at least one search field.'
        assert len(raw_index_data) > 0, 'You must specify at least one Lucene index.'

        for field_tuple in raw_fields_data:
            assert len(field_tuple) == 3, 'Wrong format for the \"fields\" parameter in the configuration file.'
            field_names.append(field_tuple[0])
            field_weights.append(field_tuple[1])
            if field_tuple[2] == 'checked':
                field_checked.append(True)
            else:
                field_checked.append(False)

        for index_tuple in raw_index_data:
            assert len(index_tuple) == 3, 'Wrong format for the \"indexDir\" parameter in the configuration file.'
            similarity_functions.append(index_tuple[1])
        similarity_functions = list(set(similarity_functions))

        html_menu = '<hr><center>'
        html_menu += '<h5>Retrieval Models:</h5><p>'
        html_menu += '<input type=\"radio\" name=\"model\" value=\"' + similarity_functions[0] + '\" checked> '
        html_menu += similarity_functions[0]
        for i in range(1, len(similarity_functions)):
            html_menu += '&nbsp;&nbsp;<input type=\"radio\" name=\"model\" value=\"' + similarity_functions[i] + '\"> '
            html_menu += similarity_functions[i] + '&nbsp;'
        html_menu += '</p>'

        html_menu += '<h5>Figure Fields:</h5><p>'
        for i in range(len(field_names)):
            checked_string = ''
            if field_checked[i]:
                checked_string = 'checked'

            html_menu += '&nbsp;&nbsp;<input type=\"checkbox\" name=\"' + field_names[i] + '\" value=\"' + field_names[i] + '\" '
            html_menu += checked_string + '>' + field_names[i] + '<input id=\"' + field_names[i]
            html_menu += 'Val\" type=\"number\" size=\"1\" value=\"' + field_weights[i] + '\" min=\"0\" max=\"10\"/>'
        html_menu += '</p><button type=\"button\" id=\"min\" class=\"mybutton\" border=\"none\">'
        html_menu += '<font color=\"#FF8033\"> Minimize ^</font></button></center><hr>'

        return html_menu, ','.join(field_names), ','.join(field_weights), ','.join(similarity_functions)

    def build_collection_menu(self):
        """Building the drop down list of collections that is displayed in the App.

        Returns:
          (string). html code of the menu.
          (string). the names of figure fields (separated with comma).
          (string). two query examples that will be presented in the App for each collection.
        """
        raw_index_data = self.configurations_dict.get('indexDir', [])
        raw_example_query_data = self.configurations_dict.get('exampleQueries', [])
        assert len(raw_index_data) > 0, 'You must specify at least one Lucene index.'
        collections = []

        for index_tuple in raw_index_data:
            assert len(index_tuple) == 3, 'Wrong format for the \"indexDir\" parameter in the configuration file.'
            collections.append(index_tuple[0])
        collections = list(set(collections))

        queries_map = {}
        for query_tuple in raw_example_query_data:
            assert len(query_tuple) == 3, 'Wrong format for the \"exampleQueries\" parameter in the configuration file.'
            queries_map[query_tuple[0]] = (query_tuple[1], query_tuple[2])

        collection_menu = ''
        collection_menu += '<font size=\"2\">Collection:</font>&nbsp <div class=\"selectWrapper\">'
        collection_menu += '<select id=\"drop\" class=\"myclass\">'

        for i, collection_name in enumerate(collections):
            collection_menu += '<option value=\"' + collection_name + '\"><font size=\"2\">' + collection_name
            collection_menu += '</font></option>'

        collection_menu += '</select></div><br>'

        query_examples_1 = ''
        query_examples_2 = ''

        for collection in collections:
            query_examples_1 += queries_map.get(collection, ['example1', 'example2'])[0] + ','
            query_examples_2 += queries_map.get(collection, ['example1', 'example2'])[1] + ','

        return collection_menu, ','.join(collections), query_examples_1.rstrip(','), query_examples_2.rstrip(',')

    def build_top_menu(self):
        """Building the navigation menu at the top of the App.

        Returns:
          (string). html code of the menu.
        """
        application_url = self.configurations_dict.get('applicationUrl', [])
        url = ""
        if len(application_url) > 0:
            url = application_url[0][0]
        top_menu = '<a class="active" href=\"' + url + '/\" style="color:#100F12">FigExplorer</a>'
        top_menu += '<object align=\"right\">'
        top_menu += '<a href=\"' + url + 'about/\" style="color:#FF8033;">About</a>&nbsp;&nbsp;</object>'
        return top_menu
