import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Building an index of figures using text data representation.
 */
public class IndexBuilder {
	private String dataDir;
	private String indexDir;
	private String similarityFunctionName;
	private CharArraySet stopwords;
	private Analyzer analyzer;
	
	private int numSuccessfulFiles = 0;
	private int numFailedFiles = 0;
	private int numIndexedFigures = 0;
	private int numIndexedPapers = 0;
	
	public IndexBuilder()
	{
		dataDir = Utils.indexParamsMap.get(Utils.DATA_CONFIG_PARAM);
		indexDir = Utils.indexParamsMap.get(Utils.INDEX_CONFIG_PARAM);
		similarityFunctionName = Utils.indexParamsMap.get(Utils.SIMILARITY_CONFIG_PARAM);
		

		if(Utils.indexParamsMap.containsKey(Utils.STOPWORDS_CONFIG_PARAM)) {
			stopwords = Utils.loadStopwords(Utils.indexParamsMap.get(Utils.STOPWORDS_CONFIG_PARAM));
			analyzer = new EnglishKrovetzAnalyzer(stopwords);
		} else {
			analyzer = new EnglishKrovetzAnalyzer();
		}
				
		File tmpIndexDir = new File(indexDir);
		if (tmpIndexDir.exists()) {
			String duplicationError = "WARNING: the index directory \"" + indexDir + "\" already exists.\nWriting an ";
			duplicationError += "index to a directory that already has an existing index in it can cause duplications!\n";
			duplicationError += "Please consider emptying the folder and indexing again.";
			System.err.println(duplicationError);
		}
	}
	
	/**
	 * Building a Lucene document for a single figure.
	 * @param fields		the figure's fields to be indexed
	 * @param figureId	the figure number
	 * @param paperId	an identifier for the figure's paper
	 * @return a Lucene document for indexing
	 */
	public Document buildDocument(ArrayList<Field> fields, String figureId, String paperId) {
		Document doc = new Document();
		Field figureField = new StringField(Utils.FIGURE_ID_FIELD, figureId, Field.Store.YES);
		Field paperField = new StringField(Utils.PAPER_ID_FIELD, paperId, Field.Store.YES);
		doc.add(figureField);
		doc.add(paperField);
		for(Field f : fields) {
			doc.add(f);
		}
		return doc;
	}
	
	/**
	 * Takes a single input file which contains a set of figures and create document objects for them.
	 * @param singleFile		the file with the figures (a single file corresponds to a single paper)
	 * @return a list of documents for indexing
	 */
	public ArrayList<Document> parseSingleFile(File singleFile) throws IOException {
		String paperId = singleFile.getName().split("\\.")[0];
		ArrayList<Document> figures = new ArrayList<>();
		FileReader fileReader = new FileReader(singleFile);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		String figureId = "";
		ArrayList<Field> figureFields = new ArrayList<>();
		boolean minLengthFlag = true;

		while ((line = bufferedReader.readLine()) != null) 
		{
			line = line.replaceAll("\n", "");
			String fieldName = line.split(">")[0].split("<")[1];
			String content = line.split(">")[1].split("<")[0];
			
			if(fieldName.equals(Utils.FIGURE_ID_FIELD)) {
				
				if(figureFields.size() > 0 && minLengthFlag) {
					Document document = buildDocument(figureFields, figureId, paperId);
					figures.add(document);
				}
				figureId = content;
				minLengthFlag = true;
				figureFields = new ArrayList<Field>();
			}
									
			if(!Utils.fieldInfo.containsKey(fieldName))
				continue;
			
			int minLength = Integer.parseInt(Utils.fieldInfo.get(fieldName).get(Utils.FIELD_LENGTH_POSITION));
			if(content.split(" ").length < minLength) {
				minLengthFlag = false;
			}
			
			Field.Store store;
			if(Utils.fieldInfo.get(fieldName).get(Utils.FIELD_STORE_POSITION).equals(Utils.STORE_FIELD))
				store = Field.Store.YES;
			else
				store = Field.Store.NO;
			
			Field field;
			String fieldType = Utils.fieldInfo.get(fieldName).get(Utils.FIELD_TYPE_POSITION);
			
			if(fieldType.equals(Utils.STRING_FIELD)) {
				field = new StringField(fieldName, content, store);
			}
			else if (fieldType.equals(Utils.TEXT_FIELD))
			{
				FieldType fieldTypeObject = new FieldType();
				fieldTypeObject.setStored(true);
				fieldTypeObject.setStoreTermVectors(true);
				fieldTypeObject.setStoreTermVectorPositions(true);
				fieldTypeObject.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);		
				field = new Field(fieldName, content, fieldTypeObject);
			} else {
				field = new StringField(fieldName, content, store);
				System.out.println("The field type " + fieldType + " is not defined; setting to String field instead.");
			}
			figureFields.add(field);
		}
		
	    if (figureFields.size() > 0 && !figureId.equals("") && minLengthFlag){
			Document document = buildDocument(figureFields, figureId, paperId);
			figures.add(document);
	    }
	    
		bufferedReader.close();
		return figures;
	}
	
	/**
	 * Builds the figures' index using a directory with all figures text data.
	 */
 	public void buildIndex() throws IOException
	{
		Path p = Paths.get(indexDir);
		Directory indexDirectory = FSDirectory.open(p);
		IndexWriterConfig configuration = new IndexWriterConfig(analyzer);
		
		Similarity similarity;
		if (similarityFunctionName.equals(Utils.BM25_SIMILARITY)) {
			similarity = new BM25Similarity();
		} else if (similarityFunctionName.equals(Utils.DIRICHLET_SIMILARITY)) {
			similarity = new LMDirichletSimilarity(Utils.DIRICHLET_MU);
		} else if (similarityFunctionName.equals(Utils.JELINEK_MERCER_SIMILARITY)) {
			similarity = new LMJelinekMercerSimilarity(Utils.JELINEK_MERCER_WEIGHT);
		} else {
			similarity = new BM25Similarity();
			System.out.println("The similarity function " + similarityFunctionName + " is not defined; setting to BM25 instead.");
		}
		configuration.setSimilarity(similarity);
		
		IndexWriter writer = new IndexWriter(indexDirectory, configuration);
		File folder = new File(dataDir);
		File[] listOfFiles = folder.listFiles();
	    for (int i = 0; i < listOfFiles.length; i++) 
	    {
	    		File singleFile = listOfFiles[i];
	    		ArrayList<Document> figures = new ArrayList<>();
	    		
	    		try {
	    			figures = parseSingleFile(singleFile);
	    		} catch (Exception e) {
	    			numFailedFiles += 1;
	    			continue;
	    		}
	    		numSuccessfulFiles += 1;
	    		
	    		for(Document figure : figures) {
	    			writer.addDocument(figure);
	    			numIndexedFigures += 1;
	    		}
	    		
	    		if(figures.size() > 0) {
	    			numIndexedPapers += 1;
	    		}
	    }
	    writer.close();
	    System.out.println("Number of successfuly parsed files: " + numSuccessfulFiles);
	    System.out.println("Number of skipped files: " + numFailedFiles);
	    System.out.println("Number of indexed figures/papers: " + numIndexedFigures + "/" + numIndexedPapers);
	}
 	
	public static void main(String[] args) throws IOException
	{
		IndexBuilder builder = new IndexBuilder();
		builder.buildIndex();
	}
}