from web_utils import read_lines_from_file
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity


class MLServer:
    """ A server which is responsible for (1) embedding-based similarity of figures,
        (2) mapping to related figures based on different algorithm.
    """

    def __init__(self, web_utils, search_engine_interface):
        self.identifiers_dict = {}
        self.embeddings_dict = {}
        self.related_figures = {}

        embeddings_dir = web_utils.configurations_dict.get(web_utils.EMBEDDINGS_DIR, [])
        for embeddings_tuple in embeddings_dir:
            self.identifiers_dict[embeddings_tuple[0]] = read_lines_from_file(embeddings_tuple[1])
            self.embeddings_dict[embeddings_tuple[0]] = self.load_embeddings(embeddings_tuple[2])

        related_figures_dir = web_utils.configurations_dict.get(web_utils.RELATED_FIGURES_DIR, [])
        for related_tuple in related_figures_dir:
            collection_name = related_tuple[0]
            if collection_name not in self.related_figures:
                self.related_figures[collection_name] = {}
            self.related_figures[collection_name][related_tuple[1]] = self.load_related_figures(related_tuple[2])

        for collection_name in search_engine_interface.same_article_figures:
            if collection_name not in self.related_figures:
                self.related_figures[collection_name] = {}
            self.related_figures[collection_name]['Same Paper Figures'] = \
                search_engine_interface.same_article_figures[collection_name]

    @staticmethod
    def load_embeddings(embeddings_dir):
        """Parse a file with embedding vectors into a numpy matrix.

        Args:
          embeddings_dir: (string) the directory with the embedding vectors.
        Returns:
          (numpy matrix). the embedding matrix.
        """
        all_embeddings = []

        with open(embeddings_dir, 'r') as input_file:
            for line in input_file:
                all_embeddings.append([float(i) for i in line.rstrip('\n').split()])
        all_embeddings = np.asarray(all_embeddings)
        return all_embeddings

    @staticmethod
    def load_related_figures(input_dir):
        """Loading a file with mapping between figures and their related figures.

        Args:
          input_dir: (string) a file with mapping from a figure to its related figures.
        Returns:
          (dictionary). a dictionary with figure id as key and a list of figures as value.
        """
        output = {}
        with open(input_dir, 'r') as input_file:
            for line in input_file:
                args = line.rstrip('\n').split(',')
                output[args[0]] = []
                for i in range(1, len(args)):
                    output[args[0]] += [args[i]]
        return output

    def get_embeddings_similarity(self, figure_id_1, figure_id_2, collection):
        """Getting the cosine similarity between two figures in the embedding space.

        Args:
          figure_id_1: (string) a figure identifier.
          figure_id_2: (string) a figure identifier.
          collection: (string) the name of the collection.
        Returns:
          (float). cosine similarity.
        """
        identifiers = self.identifiers_dict.get(collection, [])
        embeddings = self.embeddings_dict.get(collection, [])
        if figure_id_1 in identifiers and figure_id_2 in identifiers:
            vec_1 = embeddings[identifiers.index(figure_id_1)].reshape(1, -1)
            vec_2 = embeddings[identifiers.index(figure_id_2)].reshape(1, -1)
            return cosine_similarity(vec_1, vec_2)[0,0]
        else:
            return 0