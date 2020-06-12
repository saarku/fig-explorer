# Web Application

To start the application, the [fig.explorer.config](https://github.com/saarku/fig-explorer/blob/master/fig.explorer.config) should be modified first, and then the application can be launched by using the following command: *python server.py*. 
Note that the search engine server should be launced first (take a look [here](https://github.com/saarku/fig-explorer/tree/master/lucene-server) on how to do that).

## The [fig.explorer.config](https://github.com/saarku/fig-explorer/blob/master/fig.explorer.config) Configuration File
The configuration file has the following syntax. Each line corresponds to a parameter (please avoid empty lines). Then, for each parameter, the parameter name is separated from its value using "=". The value of a parameter is, in the general case, a list of values, seperated by ";". Furthermore, each value can have different attributes, seperated by ",". Please avoid using any spaces. Here is the list of parameters:

* indexDir- all of the indexes that will be used in the application. The attributes of each index are:
  * The name of the collection.
  * The name of the similarity function (a combination of a collection and a similarity function serves as a unique identifier for an index).
  * The directory of the index.
* fields- the fields to be used by the application (you should use only name of fields that were indexed based on the index configuration [file](https://github.com/saarku/fig-explorer/blob/master/lucene-server/index.builder.config). The attributes of each field are:
    * field name- the name of the field as it was indexed.
    * field weight- the initial (default) weight for a field; should be a non-negative integer.
    * checked- put "checked" if this field should be used as default or "none" otherwise.
* exampleQueries: example queries that will be presented for each collection (optional). Each collection should have a single value at the most where the attributes are:
    * collection name.
    * the first query string.
    * the second query string.
* stopwordDir- a file with a stopword in each line (optional).
* lucenePortNumber- the port number in which the search engine server "listens".
* webApplicationPortNumber- the port number for the web application (Flask) server.
* webApplicationHost- the host name for the web application (usually 0.0.0.0 or localhost).
* applicationUrl- the url of the application (use http://[webApplicationHost]:[webApplicationPortNumber]/ when running on local machine; in case of a server, the url might be different).
* imageFilesDir- a directory with all image files of figures.
* pdfFolderDir- a directory with all pdf files of the articles.
* embeddingsDir- Directories of figure embeddings for the different collections (each collection has a single value) with the following attributes:
    * collection name.
    * identifiers list: a file with a figure identifier (paper name + "_" + figure number) in each line.
    * embeddings: a file with a vector in each line (nunber separated with space) where each line corresponds to the figure in the identifiers list.
* relatedFiguresDir- Directories of related figures for the collections. Each collection can have multiple entries for using different algorithms to generate the related figures. Here are the attributes:
    * collection name.
    * algorithm name.
    * directory of the related figures file. Each line is a list of figure identifiers, separated by ",". The first figure is the seed figure, and the rest of the figures are the ones related to it (a figure identifier is paper name + "_" + figure number).
* captionField- the name of the figure field (from the index) that should be used for the figure caption in the application.
* imageFileField- the name of the figure field (from the index) that should be used for the figure image file directory.
* snippetField- the name of the figure field (from the index) that should be used for the figure snippet.
* titleField- the name of the figure field (from the index) that should be used for an article title.
* abstractField- the name of the figure field (from the index) that should be used for an article abstract.
