"""Keras data generator. We do not want to load the full data since the pairs have many repeating figures in them."""
import numpy as np
import keras


class DataGenerator(keras.utils.Sequence):

    def __init__(self, feature_vectors_dir, figure_pairs_dir, losses, batch_size=64, sequence_length=100,
                 vocabulary_size=1000):

        self.batch_size = batch_size
        self.sequence_length = sequence_length
        self.vocabulary_size = vocabulary_size
        self.losses = losses
        self.indexes = None
        self.shape = [self.batch_size, self.sequence_length]
        self.vectors_dict = self.load_vectors(feature_vectors_dir)
        self.pair_ids, self.labels = self.load_pairs_and_labels(figure_pairs_dir)
        self.on_epoch_end()

    def __len__(self):
        """Returns the number of batches"""
        return int(np.floor(len(self.pair_ids) / self.batch_size))

    def __getitem__(self, index):
        """Returns a batch of data"""
        indexes = self.indexes[index*self.batch_size:(index+1)*self.batch_size]
        list_ids_temp = [self.pair_ids[k] for k in indexes]
        x, y = self.__data_generation(list_ids_temp)
        return x, y

    def on_epoch_end(self):
        """Updates indexes after each epoch"""
        self.indexes = np.arange(len(self.pair_ids))
        np.random.shuffle(self.indexes)

    @staticmethod
    def load_pairs_and_labels(pairs_dir):
        """Loads all data pairs and their labels (identifiers only!).

        Args:
          pairs_dir: (string) the file with the pairs.

        Returns:
          (list, list). ids of pairs and their labels.
        """
        pair_ids = []
        labels = {}
        with open(pairs_dir, 'r') as pairs_file:
            for line in pairs_file:
                args = line.rstrip('\n').split(',')
                pair_ids.append((int(args[0]), int(args[1])))
                relevance_score = float(args[3])
                relevance_label = int(args[2])
                labels[int(args[0]), int(args[1])] = (relevance_label, relevance_score)
        return pair_ids, labels

    def load_vectors(self, vectors_dir):
        """Loading the feature vectors to all figures.

        Args:
          vectors_dir: (string) the file with feature vectors.

        Returns:
          (dictionary). mapping between sample id and its feature vector.
        """
        vectors_dict = {}
        with open(vectors_dir, 'r') as vector_file:
            for i, line in enumerate(vector_file):
                # adding zero padding to sequences shorter than 'sequence length'
                padding = [0] * self.sequence_length
                padded_line = [int(j) for j in line.split()] + padding

                # replacing infrequent terms with OOV token
                padded_line = np.asarray(padded_line[0:self.sequence_length])
                padded_line[padded_line > self.vocabulary_size] = self.vocabulary_size + 1
                vectors_dict[i] = np.asarray(padded_line)
        return vectors_dict

    def __data_generation(self, list_ids_temp):
        """Generates data containing batch_size samples' # X : (n_samples, sequence_length)"""

        x_left = np.empty((self.batch_size, self.sequence_length))
        x_right = np.empty((self.batch_size, self.sequence_length))

        # Generate data
        y_labels = []
        y_scores = []
        y = []
        for i, ID in enumerate(list_ids_temp):
            x_left[i,] = self.vectors_dict[ID[0]]
            x_right[i,] = self.vectors_dict[ID[1]]
            y_labels += [self.labels[ID][0]]
            y_scores += [self.labels[ID][1]]

        if 'binary_crossentropy' in self.losses:
            y.append(y_labels)
        if 'mse' in self.losses:
            y.append(y_scores)

        return [x_left, x_right], y