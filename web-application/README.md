# Web Application

To start the application, the [fig.explorer.config](https://github.com/saarku/fig-explorer/blob/master/fig.explorer.config) file should be modified first, and then the application can be launched by using the following command: *python server.py*. 
Note that the search engine server should be launched first (take a look [here](https://github.com/saarku/fig-explorer/tree/master/lucene-server) to see how to do that).

## The [fig.explorer.config](https://github.com/saarku/fig-explorer/blob/master/fig.explorer.config) Configuration File
The configuration file has the following syntax. Each line corresponds to a parameter (please avoid empty lines). Then, for each parameter, the parameter name is separated from its value using "=". The value of a parameter is, in the general case, a list of values, separated by ";". Furthermore, each value in the list can have several attributes, separated by "," (please avoid using any spaces). Here is the list of parameters that should be set up:

* indexDir- the list of indexes that will be used in the application. The attributes of each index are:
  * The name of the collection (use the same name throughout the different parameters in this file).
  * The name of the similarity function (based on the index that was built).
  * The directory of the index.
* fields- the fields to be used by the application (you should use only names of fields that were indexed based on the index configuration [file](https://github.com/saarku/fig-explorer/blob/master/lucene-server/index.builder.config). The attributes of each field are:
    * Field name- the name of the field as it was indexed.
    * Field weight- the initial (default) weight for a field; should be a non-negative integer.
    * Checked- put "checked" if this field should be used as a default in the application or "none" otherwise.
* exampleQueries: example queries that will be presented for each collection (optional). Each collection should have a single value at the most where the attributes are:
    * Collection name.
    * The first query text.
    * The second query text.
* stopwordDir- a file with a stopword in each line (optional).
* lucenePortNumber- the port number in which the search engine server "listens".
* webApplicationPortNumber- the port number for the web application (Flask) server.
* webApplicationHost- the hostname for the web application (usually 0.0.0.0 or localhost).
* applicationUrl- the URL of the application (use http://[webApplicationHost]:[webApplicationPortNumber]/ when running on local machine usually; in case of a server, the URL might be different).
* imageFilesDir- a directory with all image files of figures.
* pdfFolderDir- a directory with all pdf files of the articles.
* embeddingsDir- directories of figure embeddings for the different collections (each collection has a single value) with the following attributes:
    * Collection name.
    * Identifiers list: a file with a figure identifier (in the form of "paperName_figureNumber") in each line.
    * Embeddings: a file with a vector in each line (numbers separated with spaces) where each line corresponds to a figure in the identifiers list.
* relatedFiguresDir- Directories of related figures for the collections. Each collection can have multiple entries for using different algorithms to generate the related figures. Here are the attributes:
    * Collection name.
    * Algorithm name.
    * Directory of the related figures file. Each line is a list of figure identifiers, separated by ",". The first figure is the seed figure, and the rest of the figures are the ones related to it (a figure identifier is in the form of "paperName_figureNumber").
* captionField- the name of the figure field (from the index) that should be used for the figure caption in the application.
* imageFileField- the name of the figure field (from the index) that should be used for the figure image file directory.
* snippetField- the name of the figure field (from the index) that should be used for the figure snippet.
* titleField- the name of the figure field (from the index) that should be used for an article title.
* abstractField- the name of the figure field (from the index) that should be used for an article abstract.
