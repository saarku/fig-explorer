# Building a Collection of Figures from a Collection of Research Articles
The input for this module would be a folder with PDF files of articles, and the output would be textual files that can be indexed using [Lucene](https://github.com/saarku/fig-explorer/tree/master/lucene-server), and also the image files of the figures.
For the first step of information extraction from the PDF files, we use existing OCR tools. In this documentation we include some guidance on how we used these tools. After these tools were used, we perform various steps of processing to generate the different textual fields of a figure textual.
To facilitate the deployment of this code, and to avoid mistakes in the data format, we use sample data which can be found [here](https://github.com/saarku/fig-explorer/tree/master/small_dataset).

## Extracting Text from PDF
To extract text from the PDF files, we use the Grobid toolkit ([code](https://github.com/kermitt2/grobid),[documentation](https://grobid.readthedocs.io/en/latest/)).
We built the code to get a jar file, and then run the following command in the main folder of the toolkit (was used in Linux):
```
java -Xmx4G -jar grobid-core/build/libs/grobid-core-0.6.0-SNAPSHOT-onejar.jar -gH grobid-home -dIn folder-with-pdfs -dOut output-folder -exe processFullText 
```
You should specify the output directory folder and the input folder with the PDF files. Example output can be found [here](https://github.com/saarku/fig-explorer/tree/master/small_dataset/grobid_files).

## Extracting Figures from PDF
To extract the figures (captions and images) from the PDF files, we use the [Pdffigures](https://github.com/allenai/pdffigures2) toolkit. We use the option of CLI tools to run the toolkit. From the main folder of the toolkit, we run the following command:
```
sbt "run-main org.allenai.pdffigures2.FigureExtractorBatchCli input-dir -s stat_file.json -m images-output-dir -d json-data-dir"
```
Where input-dir is a folder with the PDF files, images-output-dir is a folder for the output images, and json-data-dir is the folder with the meta-data and the figure caption. The json file will be used in the next step and you can see an example for them [here](https://github.com/saarku/fig-explorer/tree/master/small_dataset/pdffigures_files).
Tip: when using this toolkit, try to process a single PDF file at a time. This is becasue when a single file fails (which can happen pretty often), it stops the processing for the entire folder.

## Generating Textual Fields for Figures
The final step is to use the outputs of the OCR toolkits to build the data to be indexed by Lucene.
To do this, run the following command:
```
python generate_indexable_figures.py
```
It is currently set up to process the small data set that is included in this repo, but it can be easily be adapted in the main function.
