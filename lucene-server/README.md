# Lucene Server

This module is used for (1) building an index (the IndexBuilder.java code) and (2) setting up a search engine java servers which is used by the application (the LuceneServer.java code).

## How to build and index?

We assume the following input ([example](https://github.com/saarku/fig-explorer/tree/master/small_dataset/acl_figures)). A folder which contains files where each files corresponds to a research article and contains information about the figures contained in this article. The name of the file will be considered as the article identifier and the combination of a figure number and the article identifier serve as a unique figure identifier.
Each file is composed of lines where in each line there is a single figure field for indexing; an XML tag contains the field name. The "<figure>" tag includes the figure number. Thus the "<figure>" tag marks the begging of fields corresponding to the specific figure (until we reach the next "<figure>" tag). For assitance in creating these files from the pdf collection, please take a look [here](https://github.com/saarku/fig-explorer/tree/master/pre-processing).

The next step will be to edit the configuration file for indexing (index.builder.config):
* "fields" - the figure fields that we want to index where the fields are separated with ";" and the parameters of each fields are seperated with ",". A field has 4 parameters: field name, field type, store information, and minimum length.
  ** field name- the name of the field as in the input file.
  ** field type- the type of data to be indexed (currently "text" and "string" are supported).
  ** store information- put "store" in order to also index the raw data (can be used for presentation purposes), and put "nostore" otherwise
  ** minimum length- the minimal number of words in a field that is required to index the figure.
* "indexDir" - the directory for the index (please use an empty/non-existent directory).
* "stopwordDir" - a file with a stopword in each line (optional).
* "dataDir" - the folder with the input data.
* "similarityFunctionName" - the name of the retrieval model (currently supports "Jelinek-Mercer", "Dirichlet", and "BM25".
Note the each combination of a collection and a similarity function requires a seperate index. Hence, when setting an Application, you might expect building several indexes.

After the data folder is ready and the configuration file was modified, use the following commands to build the index:
javac -cp "./:src/:jar/*" src/IndexBuilder.java
java -cp "./:src/:jar/*" IndexBuilder
