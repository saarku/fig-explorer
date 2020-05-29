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
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IndexBuilderCamera {
	
	private String dataDir;
	private String indexDir;
	private CharArraySet stopwords;
	public ArrayList<String> figureFields = new ArrayList<>();
	public ArrayList<String> paperFields = new ArrayList<>();
	
 	public IndexBuilderCamera(String configDir) throws IOException
	{
		dataDir = Utils.paramsMap.get("dataDir");
		indexDir = Utils.paramsMap.get("indexDir");
		stopwords = Utils.loadStopwords(Utils.paramsMap.get("stopwordDir"));	
		paperFields.add(Utils.TITLE_FIELD);
		paperFields.add(Utils.ABSTRACT_FIELD);
		paperFields.add(Utils.INTRO_FIELD);
		figureFields.add(Utils.MENTION_FIELD + "10");
		figureFields.add(Utils.MENTION_FIELD + "20");
		figureFields.add(Utils.MENTION_FIELD + "50");
		figureFields.add(Utils.CAPTION_FIELD);
		figureFields.add(Utils.ABSTRACT_SEN_FIELD);
	}
	
 	public void build() throws IOException, ParserConfigurationException, SAXException
	{
 		int numDocs = 0;
 		int numFigs = 0;
		Path p = Paths.get(indexDir);
		Directory indexDirectory = FSDirectory.open(p);
		Analyzer analyzer = new EnglishKrovetzAnalyzer(stopwords);
		IndexWriterConfig conf = new IndexWriterConfig(analyzer);
		Similarity sim = new BM25Similarity();
		conf.setSimilarity(sim);
		IndexWriter writer = new IndexWriter(indexDirectory, conf);

		File folder = new File(dataDir);
		File[] listOfFiles = folder.listFiles();
	    for (int i = 0; i < listOfFiles.length; i++) 
	    {
	    		HashMap<String,String> paperText = new HashMap<>();
	    		numDocs += 1;
	    		File inputFile = listOfFiles[i];
    			String paperId = inputFile.getName().split("\\.")[0];
    			paperId = paperId.split("\\-")[0] + paperId.split("\\-")[1];
	    		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	        org.w3c.dom.Document doc = dBuilder.parse(inputFile);
	        doc.getDocumentElement().normalize();
	        
	        for(String f : paperFields)
	        {
	        		String fieldName = f;
	        		if(f.equals(Utils.INTRO_FIELD))
	        			fieldName = "introduction";
		        NodeList nListPaper = doc.getElementsByTagName(fieldName);
	            Node nNodePaper = nListPaper.item(0);
	            Element eElementPaper = (Element) nNodePaper;
	            paperText.put(f, eElementPaper.getTextContent());
	        }
	        
	        NodeList nList = doc.getElementsByTagName("figure");
	        for (int temp = 0; temp < nList.getLength(); temp++) 
	        {
	        		Document document = new Document();
	        		document.add(new TextField(Utils.PAPER_ID_FIELD, paperId, Field.Store.YES));
	        		for(Map.Entry<String, String> e : paperText.entrySet())
	        			document.add(new TextField(e.getKey(), e.getValue(), Field.Store.YES));
	        		
	        		Node nNode = nList.item(temp);
	        		Element eElement = (Element) nNode;
	        		String figureId = eElement.getAttribute("id");
	        		document.add(new StringField(Utils.ID_FIELD, figureId, Field.Store.YES));
	        		document.add(new StringField(Utils.TYPE_FIELD, Utils.FIGURE_TYPE, Field.Store.YES));
	        		for(String f : figureFields)
	        		{
	        			NodeList fieldTextNodes = eElement.getElementsByTagName(f);
	        			Element fieldElement = (Element) fieldTextNodes.item(0);
	        			document.add(new TextField(f, fieldElement.getTextContent(), Field.Store.YES));
	        		}
	        		writer.addDocument(document);
	        		numFigs++;
	        }
	    }
	    System.out.println("num docs: " + numDocs + ", num figures: " + numFigs);
	    	writer.close();   
	}
			  
 	public void checkIndex() throws IOException
 	{
		Path p = Paths.get(Utils.paramsMap.get("indexDir"));
		Directory indexDirectory = FSDirectory.open(p);
		IndexReader reader = DirectoryReader.open(indexDirectory);
		System.out.println(reader.maxDoc());
		IndexSearcher searcher = new IndexSearcher(reader);
		
		int maxDoc = reader.maxDoc();
		System.out.println("num docs: " + maxDoc);
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
		
		Document HitDoc1 = searcher.doc(100);
		
		for(IndexableField f : HitDoc1.getFields())
		{
			String fieldName = f.name();
			String content = HitDoc1.get(fieldName);
			System.out.println(fieldName + ": " + content);
		}
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
				if(singleDoc.get(Utils.CAPTION_FIELD).contains("Table"))
				{
					tableCounter++;
					continue;
				}
				outputLines.add("<figure id=\"" + singleDoc.get(Utils.ID_FIELD) + "\">");
				if(!ids.contains(singleDoc.get(Utils.ID_FIELD)))
				{
					ids.add(singleDoc.get(Utils.ID_FIELD));
				}
				else
				{
					uniqueCounter++;
				}
				
				for(String fieldName : figureFields)
				{
					outputLines.add("<"+fieldName+">");
					String content = singleDoc.get(fieldName);
					content = content.replace("&", "&amp;");
					content = content.replace("&ampamp;", "&amp;");					
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
				Utils.writeLinesToFile(outputLines, "/Users/saarkuzi/acl_data/papers_temp/" + paperId.replace(".tei", ""));
		}
		System.out.println(counter);
		System.out.println(uniqueCounter);
		System.out.println(tableCounter);
		Utils.writeLinesToFile(filesList, "files_temp.txt");
 	}

 	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException
 	{
 		IndexBuilderCamera builder = new IndexBuilderCamera("index.config");
 		builder.checkIndex();
 	}
}
