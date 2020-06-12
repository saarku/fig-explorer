# Lucene Server

This module is used for (1) building an index (the IndexBuilder.java code) and (2) setting up a java search engine server that is used by the application (the LuceneServer.java code).

## How to build an index?

We assume the following input ([example](https://github.com/saarku/fig-explorer/tree/master/small_dataset/acl_figures)). A folder which contains files where each file corresponds to a research article and contains information about the figures contained in this article. The name of the file will be considered as the article identifier and the combination of a figure number and the article identifier serves as a unique figure identifier.
Each file is composed of lines where in each line there is a single figure field for indexing; an XML tag contains the field name. The "<figure>" tag includes the figure number. Thus the "<figure>" tag marks the begging of fields corresponding to the specific figure (until we reach the next "<figure>" tag). For assistance in creating these files from the pdf collection, please take a look [here](https://github.com/saarku/fig-explorer/tree/master/pre-processing).

The next step will be to edit the configuration file for indexing ([index.builder.config](https://github.com/saarku/fig-explorer/blob/master/lucene-server/index.builder.config)):
* fields - the figure fields that we want to index where the fields are separated with ";" and the parameters of each field are separated with ",". A field has 4 parameters: field name, field type, store information, and minimum length.
  * field name- the name of the field as in the input file.
  * field type- the type of data to be indexed (currently "text" and "string" are supported).
  * store information- put "store" in order to also index the raw data (can be used for presentation purposes), and put "no-store" otherwise
  * minimum length- the minimal number of words in a field that is required to index the figure.
* indexDir - the directory for the index (please use an empty/non-existent directory).
* stopwordDir - a file with a stopword in each line (optional).
* dataDir - the folder with the input data.
* similarityFunctionName - the name of the retrieval model (currently supports "Jelinek-Mercer", "Dirichlet", and "BM25".
Note that each combination of a collection and a similarity function requires a separate index. Hence, when setting an Application, you might expect to build several indexes.

After the data folder is ready and the configuration file was modified, use the following commands to build the index: \
javac -cp "./:src/:jar/*" src/IndexBuilder.java \
java -cp "./:src/:jar/*" IndexBuilder

## How to start a search engine server?

The first step is to set up the configuration file [fig.explorer.config](https://github.com/saarku/fig-explorer/blob/master/fig.explorer.config). This file contains configurations that are used both by the LuceneServer code and the web application code. Here we explain the parameters that are needed for the LuceneServer code (i.e., for setting the search engine server).
* indexDir- all of the indexes that will be used in the application. The indexes are separated by ";" and each index has 3 parameters separated by ",":
  * The name of the collection.
  * The name of the similarity function (a combination of a collection and a similarity function serves as a unique identifier for an index).
  * The directory of the index.
* stopwordDir- a file with a stopword in each line (optional).
* lucenePortNumber- the port number in which the search engine server "listens".

Finally, use the following two commands to start the engine:\
javac -cp "./:src/:jar/*" src/LuceneServer.java \
java -cp "./:src/:jar/*" LuceneServer
