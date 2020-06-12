# FigExplorer
A System for Retrieval and Exploration of Figures from Collections of Research Articles.
A demo of the system can be found [here](http://figuresearch.web.illinois.edu/).
For more elaborate information please refer to:
*FigExplorer: A System for Retrieval and Exploration of Figures from Collections of Research Articles. Saar Kuzi, ChengXiang Zhai, Yin Tian, and Haichuan Tang. In Proceedings of SIGIR 2020.*


The goal of this system is to facilitate the exploration of research article collections using figures and includes the following main functionalities:

* Searching for figures using keyword queries.
* Searching for figures using different textual representations of a figure and different retrieval models.
* Collecting user feedback in order to build test collections.
* Exploring related figures for a seed figures using figure embeddings.

In order to set up the system, the following steps should be followed:
1. Building a collection of figures from a set of pdf files.
2. Learning figure representations and inferring reladness relations between figures (optional).
3. Building a figure index.
3. Setting up the web-application.
