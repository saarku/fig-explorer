import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class IndexBuilder {
	
	private String dataDir;
	private String indexDir;
	private CharArraySet stopwords;
	private SearchEngine refSearchEngine;
	private ArrayList<String> additionalFields = new ArrayList<>();
	private HashMap<String,String> absSentences;
	public int counter = 0;
	
 	public IndexBuilder(String configDir) throws IOException
	{
		dataDir = Utils.paramsMap.get("dataDir");
		indexDir = Utils.paramsMap.get("indexDir");
		stopwords = Utils.loadStopwords(Utils.paramsMap.get("stopwordDir"));
		refSearchEngine = new SearchEngine(Utils.paramsMap.get("referenceIndexDir"));
		absSentences = parseAbsSentences("absSentences.txt");
				
		additionalFields.add(Utils.TITLE_FIELD);
		additionalFields.add(Utils.TEXT_FIELD);
		additionalFields.add(Utils.ABSTRACT_FIELD);
		additionalFields.add(Utils.INTRO_FIELD);
		additionalFields.add(Utils.CONC_FIELD);
	}
	
 	public void build() throws IOException
	{
 		int numDocs = 0;
 		int numFigs = 0;
 		int numTabs = 0;
		Path p = Paths.get(indexDir);
		Directory indexDirectory = FSDirectory.open(p);
		Analyzer analyzer = new EnglishKrovetzAnalyzer(stopwords);
		IndexWriterConfig conf = new IndexWriterConfig(analyzer);
		Similarity sim = new BM25Similarity();
		conf.setSimilarity(sim);
		
		IndexWriter writer = new IndexWriter(indexDirectory, conf);
		
		try {
				File folder = new File(dataDir);
				File[] listOfFiles = folder.listFiles();
			    for (int i = 0; i < listOfFiles.length; i++) 
			    {
			    		numDocs += 1;
			    		File singleFile = listOfFiles[i];
			    		String paperId = singleFile.getName().split("\\.")[0];
			    		paperId = paperId.split("\\-")[0] + paperId.split("\\-")[1];
			    		HashMap<String, String> additionalField = getAdditionalFields(paperId);
			    		if(additionalField.size() == 0)
			    			counter++;
			    		
					FileReader fileReader = new FileReader(singleFile);
					BufferedReader bufferedReader = new BufferedReader(fileReader);
					String line;
					
					String elementNum = "";
					String elementType = "";
					HashMap<Integer,String> mentions = new HashMap<>();
					String captions = "";
					String imageDir = "";
					
					while ((line = bufferedReader.readLine()) != null) 
					{
						line = line.replaceAll("\n", "");

						if (line.contains("<table>") || line.contains("<figure>"))
						{
							if(!elementNum.equals("") && mentions.size() != 0)
							{
								if(mentions.get(10).split(" ").length < 2) continue;
								Document doc = createDocument(mentions, captions, elementNum, 
										singleFile.getName(), imageDir, elementType, additionalField);
								writer.addDocument(doc);
								if(elementType == Utils.TABLE_TYPE)
									numTabs += 1;
								else
									numFigs +=1;
							}
							
							mentions = new HashMap<>();
							captions = "";
							imageDir = "";
							
							if(line.contains(Utils.FIGURE_TAG))
							{
								elementNum = Utils.getContent(Utils.FIGURE_TAG, line);
								elementType = Utils.FIGURE_TYPE;
							}
							else
							{
								elementNum = Utils.getContent(Utils.TABLE_TAG, line);
								elementType = Utils.TABLE_TYPE;
							}
						}
						
						else if (line.contains(Utils.CAPTION_TAG))
						{
							captions = Utils.getContent(Utils.CAPTION_TAG, line);
						}
						else if(line.contains(Utils.MENTION_10_TAG))
						{
							mentions.put(10, Utils.getContent(Utils.MENTION_10_TAG, line));
						}
						else if(line.contains(Utils.MENTION_20_TAG))
						{
							mentions.put(20, Utils.getContent(Utils.MENTION_20_TAG, line));
						}
						else if(line.contains(Utils.MENTION_50_TAG))
						{
							mentions.put(50, Utils.getContent(Utils.MENTION_50_TAG, line));
						}
						else if(line.contains(Utils.FILE_TAG))
						{
							imageDir = Utils.getContent(Utils.FILE_TAG, line);
						}
					}
					
					if(!elementNum.equals("") && mentions.size() !=0)
					{
						if(mentions.get(10).split(" ").length >= 2)
						{
							Document doc = createDocument(mentions, captions, elementNum, 
									singleFile.getName(), imageDir, elementType, additionalField);
							writer.addDocument(doc);
							if(elementType == Utils.TABLE_TYPE)
								numTabs += 1;
							else
								numFigs +=1;
						}
					}
					
					fileReader.close();
			    }
					writer.close();
					System.out.println("num articles: " + numDocs + ", num figs: " + numFigs + 
							", num tables: " + numTabs);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
 		
	public Document createDocument(HashMap<Integer,String> mentions, String captions, String elementNum, 
			String paperId, String imageDir, String elementType, HashMap<String, String> additionalFields)
	{
		Document document = new Document();
		TextField captionField = new TextField(Utils.CAPTION_FIELD, captions, Field.Store.YES);
		TextField mention10Field = new TextField(Utils.MENTION_FIELD + "10", mentions.get(10), Field.Store.YES);
		TextField mention20Field = new TextField(Utils.MENTION_FIELD + "20", mentions.get(20), Field.Store.YES);
		TextField mention50Field = new TextField(Utils.MENTION_FIELD + "50", mentions.get(50), Field.Store.YES);
		
		StringField idField = new StringField(Utils.ID_FIELD, elementNum, Field.Store.YES);
		StringField titleField = new StringField(Utils.PAPER_ID_FIELD, paperId, Field.Store.YES);
		StringField fileField = new StringField(Utils.IMAGE_FIELD, imageDir, Field.Store.YES);
		StringField typeField = new StringField(Utils.TYPE_FIELD, elementType, Field.Store.YES);

		document.add(titleField);
		document.add(idField);
		document.add(typeField);
		document.add(captionField);
		document.add(mention10Field);
		document.add(mention20Field);
		document.add(mention50Field);
		document.add(fileField);
		
		for(Map.Entry<String, String> e : additionalFields.entrySet())
		{
			TextField additionalField = new TextField(e.getKey(), e.getValue(), Field.Store.YES);
			document.add(additionalField);
		}
		
		String absKey = paperId + elementNum + elementType;
		String absSentence = absSentences.getOrDefault(absKey, "");
		if (absSentence.equals("")) counter++;
		document.add(new TextField(Utils.ABSTRACT_SEN_FIELD, absSentence, Field.Store.YES));
		
		return document;
	}
	
	public HashMap<String, String> getAdditionalFields(String paperId) throws IOException
	{
		HashMap<String,String> fieldValues = new HashMap<>();
		Query idQuery = refSearchEngine.queryCreator.buildIdQuery(paperId, "id");
		ScoreDoc[] paperIdResult = refSearchEngine.search(idQuery, 1);
		if(paperIdResult.length == 0)
			return fieldValues;
		
		Document HitDoc = refSearchEngine.searcher.doc(paperIdResult[0].doc);
		for(String fieldName : additionalFields)
		{
			fieldValues.put(fieldName, HitDoc.get(fieldName));
		}
		return fieldValues;
	}
	
	public HashMap<String,String> parseAbsSentences(String fileDir)
	{
		HashMap<String,String> sentenceMap = new HashMap<>();
		ArrayList<String> lines = Utils.readLinesFromFile(fileDir);
		for(String line : lines)
		{
			String[] args = line.split(",");
			String paperId = args[0];
			String elementId = args[1];
			String elementType = args[2];
			String key = paperId + elementId + elementType;
			String val = args[3];
			sentenceMap.put(key, val);
		}	
		return sentenceMap;
	}
	
 	public void checkIndex() throws IOException
 	{
		Path p = Paths.get(Utils.paramsMap.get("indexDir"));
		Directory indexDirectory = FSDirectory.open(p);
		IndexReader reader = DirectoryReader.open(indexDirectory);
		System.out.println(reader.maxDoc());
		IndexSearcher searcher = new IndexSearcher(reader);
		
		int maxDoc = reader.maxDoc();
		int counter = 0;
		for(int i=0; i < maxDoc; i++)
		{
			Document HitDoc = searcher.doc(i);
			String content = HitDoc.get(Utils.TYPE_FIELD);
			if(content.equals(Utils.FIGURE_TYPE))
			{
				//System.out.println(content);
				counter += 1;
			}
		}
		System.out.println(counter);
		
		/*
		List<IndexableField> fields = HitDoc.getFields();
		for(IndexableField f : fields)
		{
			String fieldName = f.name();
			String content = HitDoc.get(fieldName);
			System.out.println(fieldName + ": " + content);
		}*/
		
 	}
 	
 	public void cameraReady() throws IOException
 	{
 		ArrayList<String> filesList = new ArrayList<>();
 		int counter = 0;
 		ArrayList<String> figureFields = new ArrayList<>();
 		figureFields.add(Utils.CAPTION_FIELD);
 		figureFields.add(Utils.MENTION_FIELD + "10");
 		figureFields.add(Utils.MENTION_FIELD + "20");
 		figureFields.add(Utils.MENTION_FIELD + "50");
 		figureFields.add(Utils.ABSTRACT_SEN_FIELD);
 		int uniqueCounter = 0;
 		int tableCounter = 0;
 		
 		ArrayList<String> paperIds = new ArrayList<>();
		Path p = Paths.get(Utils.paramsMap.get("indexDir"));
		Directory indexDirectory = FSDirectory.open(p);
		
		IndexReader reader = DirectoryReader.open(indexDirectory);
		IndexSearcher searcher = new IndexSearcher(reader);
		int maxDoc = reader.maxDoc();
		
		for(int i = 0; i<maxDoc ; i++)
		{
			Document HitDoc = searcher.doc(i);
			String paperId = HitDoc.get(Utils.PAPER_ID_FIELD);
			if(!paperIds.contains(paperId))
				paperIds.add(paperId);
		}
		
		for(String paperId : paperIds)
		{
			ArrayList<String> outputLines = new ArrayList<>();
			outputLines.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
			outputLines.add("<paper id=\"" + paperId.replace(".tei.xml", "") + "\">");
			
			Query q = QueryCreator.buildIdQuery(paperId, Utils.PAPER_ID_FIELD);
			TopDocs docs = searcher.search(q, 1000);
			ScoreDoc[] sdocs = docs.scoreDocs;
			Document paperDoc = searcher.doc(sdocs[0].doc);
			
			String title = paperDoc.get(Utils.TITLE_FIELD);
			if(title==null) title = "";
			String abstractField = paperDoc.get(Utils.ABSTRACT_FIELD);
			if(abstractField==null) abstractField = "";
			String intro = paperDoc.get(Utils.INTRO_FIELD);
			if(intro==null) intro = "";
			
			outputLines.add("<title>");
			outputLines.add(title);
			outputLines.add("</title>");
			outputLines.add("<abstract>");
			outputLines.add(abstractField);
			outputLines.add("</abstract>");
			outputLines.add("<introduction>");
			outputLines.add(intro);
			outputLines.add("</introduction>");
			Boolean outputFlag = false;
			
			ArrayList<String> ids = new ArrayList<>();
			for(ScoreDoc d : sdocs)
			{	
				Document singleDoc = searcher.doc(d.doc);
				String type = singleDoc.get(Utils.TYPE_FIELD);
				if(type.equals(Utils.TABLE_TYPE)) continue;
				outputLines.add("<figure id=\"" + singleDoc.get(Utils.ID_FIELD) + "\">");
				if(!ids.contains(singleDoc.get(Utils.ID_FIELD)))
				{
					ids.add(singleDoc.get(Utils.ID_FIELD));
				}
				else
				{
					uniqueCounter++;
				}
				
				if(singleDoc.get(Utils.CAPTION_FIELD).contains("Table"))
					tableCounter++;
				
				for(String fieldName : figureFields)
				{
					outputLines.add("<"+fieldName+">");
					String content = singleDoc.get(fieldName);
					content = content.replace("&", "&amp;");
					content = content.replace("&ampamp;", "&amp;");
					
					//content = content.replace("<figcaptions>", " ");
					//content = content.replace("<labels>", " ");
					//content = content.replace("<h-cpos=?; s-cpos=?; m-gender=?>", " ");
					//content = content.replace("<OAS>", " ");
					//content = content.replace(" < ", " &lt; ");
					//content = content.replace(" > ", " &gt; ");
					//content = content.replace("p<", "p&gt;");
					//content = content.replace("<bold underlined>", " ");
					//content = content.replace("<np>", " ");
					//content = content.replace("<tablecaptions>", " ");
					//content = content.replace("<:", " ");
					
					//content = content.replace("<dobj>", " ");
					//content = content.replace("<AUXMOD", " ");
					//content = content.replace(" <<< ", " &lt;&lt;&lt;");
					//content = content.replace(" >>> ", " &gt;&gt;&gt;");
					
					//content = content.replace(" << ", " &lt;&lt;");
					//content = content.replace(" >> ", " &gt;&gt;");
					
					//content = content.replace("<STnAEyA <lY", " ");
					//content = content.replace("<drug, disease>", "(drug, disease)");
					//content = content.replace("<0.05", "&lt;0.05");
					//content = content.replace("(<", "(&lt;");
					//content = content.replace("<T>", " ");
					
					content = content.replace("<", "&lt;");
					content = content.replace("<", "&gt;");
					
					outputLines.add(content);
					outputLines.add("</"+fieldName+">");
					outputFlag = true;
				}
				String fileName = singleDoc.get(Utils.IMAGE_FIELD);
				if(!fileName.equals("") && !fileName.contains("Table"))
				{
					filesList.add(fileName);
					outputLines.add("<"+Utils.IMAGE_FIELD+">");
					outputLines.add(fileName);
					outputLines.add("</"+Utils.IMAGE_FIELD+">");
					
				}
				outputLines.add("</figure>");
				counter += 1;
			}
			outputLines.add("</paper>");
			if(outputFlag)
				Utils.writeLinesToFile(outputLines, "/Users/saarkuzi/acl_data/papers/" + paperId.replace(".tei", ""));
		}
		System.out.println(counter);
		System.out.println(uniqueCounter);
		System.out.println(tableCounter);
		Utils.writeLinesToFile(filesList, "files.txt");
 	}
 	
	public static void main(String[] args) throws IOException
	{
		IndexBuilder builder = new IndexBuilder("index.config");
		builder.cameraReady();
	}
}