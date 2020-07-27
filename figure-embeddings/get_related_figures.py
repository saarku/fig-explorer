import utils
from sklearn.feature_extraction.text import CountVectorizer, TfidfTransformer
from sklearn.neighbors import NearestNeighbors
import numpy as np


class KnnSearcher:

    def __init__(self, embeddings_matrix, figure_identifiers, num_neighbors):
        self.embeddings_matrix = embeddings_matrix
        self.figure_identifiers = figure_identifiers
        num_neighbors = min(embeddings_matrix.shape[0]-1, num_neighbors)
        self.neighbors_model = NearestNeighbors(n_neighbors=num_neighbors + 1, algorithm='brute', metric='cosine').fit(
            self.embeddings_matrix)

    def get_test_embeddings(self, test_identifiers):
        """Get a matrix with the embeddings of the test figures.

        Args:
          test_identifiers: (list) the test figures.

        Returns:
          (numpy array). An embedding matrix.
        """
        test_embeddings = []
        for figure_id in test_identifiers:
            embedding_index = self.figure_identifiers.index(figure_id)
            embedding_vectors = self.embeddings_matrix[[embedding_index]]
            try:
                embedding_vectors = embedding_vectors.todense().tolist()
            except:
                embedding_vectors = embedding_vectors.tolist()
            test_embeddings += embedding_vectors

        test_embeddings = np.asarray(test_embeddings)
        return test_embeddings

    def perform_keyword_retrieval(self, test_figure_identifiers):
        """Perform retrieval with keyword matching using tf.idf vectors.

        Args:
          test_figure_identifiers: (list) the test figures.

        Returns:
          Dictionary. A result list.
        """
        test_embeddings = self.get_test_embeddings(test_figure_identifiers)
        distances, neighbor_indexes = self.neighbors_model.kneighbors(test_embeddings)
        final_result_list = {}

        for i, test_figure in enumerate(test_figure_identifiers):
            result_indexes = neighbor_indexes[i]
            result_similarities = 1 - distances[i]
            final_result_list[test_figure] = [(self.figure_identifiers[result_indexes[j]], result_similarities[j])
                                              for j in range(len(result_indexes))]
        return final_result_list

    def re_rank_result_list(self, result_list):
        """Re-rank an initial result list using embedding-based similarity.

        Args:
          result_list: (dictionary) the result list to be re-ranked.

        Returns:
          (Dictionary). A re-ranked result list.
        """
        re_ranked_result_list = {}

        for test_figure in result_list.keys():
            result_fig_ids = [result[0] for result in result_list[test_figure]]
            result_fig_indexes = []
            result_figure_identifiers = []
            for figure_id in result_fig_ids:
                result_fig_indexes.append(self.figure_identifiers.index(figure_id))
                result_figure_identifiers += [figure_id]

            test_neighbor_model = NearestNeighbors(n_neighbors=len(result_fig_indexes), algorithm='brute',
                                                   metric='cosine').fit(self.embeddings_matrix[result_fig_indexes, :])

            test_embeddings = self.get_test_embeddings([test_figure])
            distances, neighbor_indexes = test_neighbor_model.kneighbors(test_embeddings)
            result_indexes = neighbor_indexes[0]
            result_similarities = 1 - distances[0]
            re_ranked_result_list[test_figure] = [(result_figure_identifiers[result_indexes[i]],
                                                   result_similarities[i]) for i in range(len(result_indexes))]
        return re_ranked_result_list

    @staticmethod
    def get_tf_idf_embeddings(data_dir):
        """Get tf-idf matrix of the figures in the collection.

        Args:
          data_dir: (string) a file with the text data of the figures.

        Returns:
          tf.idf Matrix.
        """
        data_lines = utils.read_lines_from_file(data_dir)
        count_vector = CountVectorizer()
        tf_vectors = count_vector.fit_transform(data_lines)
        tf_idf_transformer = TfidfTransformer()
        tf_idf_vectors = tf_idf_transformer.fit_transform(tf_vectors)
        return tf_idf_vectors


def re_rank_with_embeddings(result_list, embeddings_matrix, figure_identifiers):
    """Re-rank an initial result list using embedding-based similarity.

    Args:
      result_list: (dictionary) the result list to be re-ranked.
      embeddings_matrix: (numpy array) the embedding matrix for re-ranking.
      figure_identifiers: (list) the identifiers of the figures in the embedding matrix.

    Returns:
      (Dictionary). A re-ranked result list.
    """
    re_ranker = KnnSearcher(embeddings_matrix, figure_identifiers, 0)
    re_ranked_result_list = re_ranker.re_rank_result_list(result_list)
    re_ranked_result_list = utils.min_max_norm(re_ranked_result_list)
    initial_result_list = utils.min_max_norm(result_list)

    final_result_list = {}
    for figure_test_id in initial_result_list:
        single_result_list = {}

        for res_id in initial_result_list[figure_test_id]:
            single_result_list[res_id] = initial_result_list[figure_test_id][res_id] + \
                                         re_ranked_result_list[figure_test_id][res_id]

        sorted_result = [(k, single_result_list[k]) for k in sorted(single_result_list, key=single_result_list.get,
                                                                    reverse=True)]
        final_result_list[figure_test_id] = sorted_result
    return final_result_list


def get_related_figures(identifiers_dir, text_data_dir, embeddings_dir, test_identifiers_dir, output_dir):
    """Get semantically related figures for a set of test figures.

    Args:
      identifiers_dir: (string) identifiers of all figures in the collection.
      text_data_dir: (string) the file with the text for each figure (for keyword retrieval purposes).
      embeddings_dir: (string) the embedding vectors for all figures in the collection.
      test_identifiers_dir: (string) the figures for which we want to find related figures (a subset of full collection)
      output_dir: (string) directory for the output data.

    Returns:
      None. Outputs the related figures to a file.
    """
    test_identifiers = utils.read_lines_from_file(test_identifiers_dir)
    all_identifiers = utils.read_lines_from_file(identifiers_dir)
    tf_idf_matrix = KnnSearcher.get_tf_idf_embeddings(text_data_dir)
    searcher = KnnSearcher(tf_idf_matrix, all_identifiers, 100)
    initial_result_list = searcher.perform_keyword_retrieval(test_identifiers)
    embedding_matrix = utils.load_embeddings(embeddings_dir)
    final_result_list = re_rank_with_embeddings(initial_result_list, embedding_matrix, all_identifiers)

    with open(output_dir, 'w+') as output_file:
        for figure_id in final_result_list:
            line = figure_id
            for other_figure in final_result_list[figure_id]:
                if other_figure[0] != figure_id:
                    line += ',' + other_figure[0]
            output_file.write(line + '\n')


def main():
    test_identifiers_dir = 'example_embedding_dataset/identifiers.txt'
    identifiers_dir = 'example_embedding_dataset/identifiers.txt'
    text_data_dir = 'example_embedding_dataset/raw_data.txt'
    embeddings_dir = 'example_model/embeddings.txt'

    get_related_figures(identifiers_dir, text_data_dir, embeddings_dir, test_identifiers_dir, 'related_figures.txt')


if __name__ == '__main__':
    main()
