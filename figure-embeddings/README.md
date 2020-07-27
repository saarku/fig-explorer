# Figure Embeddings
To learn an embedding model we use a Siamese neural network. That is, we feed the network with pairs of figures that are semantically related and calculate the semantic similarity between them using the cosine similarity. In this toolkit, we assume that two figures are related if they are either in the same paper or in citing paper.


## Building the Data for Embedding Learning/Inference
The data for the model includes three components: (1) feature vectors, (2) figure identifiers for the vectors, (3) pairs of figures for training.
Feature vectors are numbers that correspond to words where we capture the original order. The figure identifiers help us understand to which figure each vector belongs to. Finally, we have pairs of figures that are associated with a binary relevance label and also a relevance score. For your convenience, please find an example for those files [here](https://github.com/saarku/fig-explorer/tree/master/figure-embeddings/example_embedding_dataset). 

To build those file we need the outputs of the Grodid OCR toolkit. Please refer to [here](https://github.com/saarku/fig-explorer/tree/master/pre-processing) for information on how to generate them.
To build the data for the embeddings, just run the following command:
```
python build_data.py
```
It is currently set up to use the example data set in this repository. The directories can be easily changed in the main function.

## Learning/Inference of Embeddings Model
Using the data that was generated in the previous step, we can learn an embedding model or use an existing model to generate figure embeddings using the following command:
```
learn_and_predict.py
```
Inside the main function, you can decide what operation should be performed (either learning or inference). 


## Using Embeddings to Find Related Figures
Finally, we can use the embedding to find for each figure its most semantically related figures. To do this, we first perform a retrieval phase using a tf.idf model and the result is then re-ranked using the embedding-based similarity. The output of this function will be a file that can be used directly in the web-based app (take a look [here](https://github.com/saarku/fig-explorer/tree/master/web-application) for more details about it). To run this function, use the following command:
```
python get_related_figures.py
```
