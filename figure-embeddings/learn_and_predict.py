from keras.models import Model
from keras.layers import Input, Embedding, LSTM, Dot, Activation, Dropout, Dense, Bidirectional
import os
from data_generator import DataGenerator
from keras.callbacks import ModelCheckpoint
import numpy as np
from keras import backend as K


class NeuralModel:
    def __init__(self, sequence_length=100, n_hidden_lstm=50, n_hidden_embeddings=100):
        self.n_hidden_lstm = n_hidden_lstm
        self.n_hidden_embeddings = n_hidden_embeddings
        self.sequence_length = sequence_length
        self.vocab_size = 1000

    def create_model(self, losses, weights_dir=None):
        """Define and build a model.

        Args:
          losses: (list) the loss function to use (based on what's available on Keras).
          weights_dir: (string) directory for the model weights in the case of inference only.

        Returns:
          (Keras model).
        """
        left_input = Input(shape=(self.sequence_length,), dtype='int32')
        right_input = Input(shape=(self.sequence_length,), dtype='int32')

        embedding_layer = Embedding(self.vocab_size + 2, self.n_hidden_embeddings, input_length=self.sequence_length,
                                    trainable=True)
        encoded_left = embedding_layer(left_input)
        encoded_right = embedding_layer(right_input)

        shared_lstm = LSTM(self.n_hidden_lstm)
        left_output = shared_lstm(encoded_left)
        right_output = shared_lstm(encoded_right)

        dot_layer_1 = Dot(1)
        dot_layer_output = dot_layer_1([left_output, right_output])
        sigmoid_layer = Activation(activation='sigmoid')
        distance_1 = sigmoid_layer(dot_layer_output)
        dot_layer_2 = Dot(1)
        distance_2 = dot_layer_2([left_output, right_output])

        outputs = []
        training_losses = []
        weights = []
        if 'binary_crossentropy' in losses:
            outputs += [distance_1]
            training_losses += ['binary_crossentropy']
            weights += [1]

        if 'mse' in losses:
            outputs += [distance_2]
            training_losses += ['mse']
            weights += [1]

        model = Model([left_input, right_input], outputs)

        if weights_dir is not None:
            model.load_weights(weights_dir)

        model.compile(loss=training_losses, optimizer='adam', metrics=['accuracy'], loss_weights=weights)
        return model


def learn_model(model_name, pairs_dir, feature_vectors_dir, losses, num_epochs=3, batch_size=64, sequence_length=100):
    """Learn an embedding model.

    Args:
      model_name: (string) a name for the model (a folder with this name will be created).
      pairs_dir: (string) directory for the figure pairs for training.
      feature_vectors_dir: (string) the file with the figure feature vectors.
      losses: (list) the loss function to use (the options are: mse and binary_crossentropy).
      num_epochs: (int) number of training epochs.
      batch_size: (int) the batch size.
      sequence_length: (int) the number of first tokens to consider.

    Returns:
      None. Outputs the model in each epoch to the model folder.
    """
    if not os.path.exists(model_name):
        os.mkdir(model_name)

    training_generator = DataGenerator(feature_vectors_dir, pairs_dir, losses, batch_size=batch_size,
                                       sequence_length=sequence_length)
    model = NeuralModel()
    compiled_model = model.create_model(losses)
    checkpoints = ModelCheckpoint(model_name + "/ckpt-{epoch:02d}.hdf5")
    compiled_model.fit_generator(generator=training_generator, epochs=num_epochs, callbacks=[checkpoints])
    K.clear_session()


def generate_embeddings(model_dir, model_epoch, losses, feature_vectors_dir, output_dir, vocabulary_size=1000,
                        sequence_length=100):
    """Generate figure embeddings using a model.

    Args:
      model_dir: (string) the folder with the models in each epoch.
      feature_vectors_dir: (string) the file with the figure feature vectors.
      losses: (list) the loss function to use (the options are: mse and binary_crossentropy).
      model_epoch: (int) the epoch to consider for the model.
      sequence_length: (int) the number of first tokens to consider.
      losses: (list): the loss function to use (based on what's available on Keras).
      output_dir: (string) directory for the embeddings output file.
      vocabulary_size: (int) the size of the vocabulary.

    Returns:
      None. Outputs the embeddings into a .txt file.
    """

    data_matrix = []
    with open(feature_vectors_dir, 'r') as input_file:
        for i, line in enumerate(input_file):
            args = line.rstrip('\n').split()
            padding = [0] * sequence_length
            padded_line = [int(j) for j in args] + padding
            padded_line = np.asarray(padded_line[0:sequence_length])
            padded_line[padded_line > vocabulary_size] = vocabulary_size + 1
            data_matrix.append(list(padded_line))
    data_matrix = np.asarray(data_matrix)

    model = NeuralModel()
    compiled_model = model.create_model(losses, weights_dir=model_dir+'/ckpt-0'+str(model_epoch)+'.hdf5')
    intermediate_layer_model = Model(inputs=compiled_model.get_layer('input_1').output,
                                     outputs=compiled_model.get_layer('lstm_1').get_output_at(0))
    intermediate_output = intermediate_layer_model.predict(data_matrix)

    with open(output_dir, 'w+') as output_file:
        for line_num in range(intermediate_output.shape[0]):
            output_file.write(' '.join([str(i) for i in list(intermediate_output[line_num, :])]) + '\n')

    K.clear_session()


def main():
    model_name = 'example_model'
    pairs_dir = 'example_embedding_dataset/pairs.txt'
    feature_vectors_dir = 'example_embedding_dataset/vectors.txt'
    embeddings_output_dir = model_name + '/embeddings.txt'
    losses = ['binary_crossentropy', 'mse']
    batch_size = 2  # very small value just for the example data.

    learn_flag = True
    predict_flag = True

    if learn_flag:
        learn_model(model_name, pairs_dir, feature_vectors_dir, losses, batch_size=batch_size)
    if predict_flag:
        generate_embeddings(model_name, 3, losses, feature_vectors_dir, embeddings_output_dir)


if __name__ == '__main__':
    main()

