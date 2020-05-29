import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

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
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;

import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class BasicIndexBuilder {
	
	private String dataDir;
	private String indexDir;
	private CharArraySet stopwords;
	public int counter = 0;
	
 	public BasicIndexBuilder(String configDir) throws IOException
	{
		dataDir = Utils.paramsMap.get("dataDir");
		indexDir = Utils.paramsMap.get("indexDir");
		stopwords = Utils.loadStopwords(Utils.paramsMap.get("stopwordDir"));
	}
	
 	public void build() throws IOException
	{
 		int numDocs = 0;
 		int numFigs = 0;
 		int numTabs = 0;
 		int numFiles = 0;
 		
		Path p = Paths.get(indexDir);
		Directory indexDirectory = FSDirectory.open(p);
		Analyzer analyzer = new EnglishKrovetzAnalyzer(stopwords);
		IndexWriterConfig conf = new IndexWriterConfig(analyzer);
		//Similarity sim = new BM25Similarity();
		//Similarity sim = new LMJelinekMercerSimilarity(0.1f);
		Similarity sim = new LMDirichletSimilarity(100);
		
		
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
			    		
			    		if(paperId.split("\\-").length == 2) 
			    			paperId = paperId.split("\\-")[0] + paperId.split("\\-")[1];
			    		
					FileReader fileReader = new FileReader(singleFile);
					BufferedReader bufferedReader = new BufferedReader(fileReader);
					String line;
					
					String elementNum = "";
					String elementType = "";
					HashMap<Integer,String> mentions = new HashMap<>();
					String captions = "";
					String imageDir = "";
					String abstractField = "";
					String titleField = "";
					String introField = ""; 
					String lines3Field = "";
					String lines5Field = "";
					String snippet3Field = "";
					String snippet5Field = "";
					
					while ((line = bufferedReader.readLine()) != null) 
					{
						line = line.replaceAll("\n", "");
						
						if (line.contains("<table>") || line.contains("<figure>"))
						{
							if(!elementNum.equals("") && mentions.size() != 0 &&
								mentions.get(10).split(" ").length >= 2 &&
								elementType == Utils.FIGURE_TYPE)
							{
								Document doc = createDocument(mentions, captions, elementNum, 
										singleFile.getName(), imageDir, elementType,titleField, 
										abstractField, introField, lines3Field, lines5Field,
										snippet3Field, snippet5Field);
								
								writer.addDocument(doc);
								
								if(elementType == Utils.TABLE_TYPE)
									numTabs += 1;
								else
									numFigs +=1;
								
								if(!imageDir.equals(""))
									numFiles++;
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
						else if(line.contains(Utils.INTRO_TAG))
						{
							introField = Utils.getContent(Utils.INTRO_TAG, line);
						}
						else if(line.contains(Utils.ABSTRACT_TAG))
						{
							abstractField = Utils.getContent(Utils.ABSTRACT_TAG, line);
						}
						else if(line.contains(Utils.TITLE_TAG))
						{
							titleField = Utils.getContent(Utils.TITLE_TAG, line);
						}
						else if(line.contains("<lines3>"))
						{
							lines3Field = Utils.getContent("<lines3>", line);
						}
						else if(line.contains("<lines5>"))
						{
							lines5Field = Utils.getContent("<lines5>", line);
						}
						else if(line.contains("<snippet3>"))
						{
							snippet3Field = Utils.getContent("<snippet3>", line);
						}
						else if(line.contains("<snippet5>"))
						{
							snippet5Field = Utils.getContent("<snippet5>", line);
						}
					}
					
					if(!elementNum.equals("") && mentions.size() != 0 &&
							mentions.get(10).split(" ").length >= 2 &&
							elementType == Utils.FIGURE_TYPE)
					{
						Document doc = createDocument(mentions, captions, elementNum, 
								singleFile.getName(), imageDir, elementType, titleField, 
								abstractField, introField, lines3Field, lines5Field,
								snippet3Field, snippet5Field);
						writer.addDocument(doc);
						if(elementType == Utils.TABLE_TYPE)
							numTabs += 1;
						else
							numFigs +=1;
						
						if(!imageDir.equals(""))
							numFiles++;
					}	
					fileReader.close();
			    }
			    
				writer.close();
				System.out.println("num articles: " + numDocs + ", num figs: " + numFigs + 
						", num tables: " + numTabs + "num files: "+ numFiles);
				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
 		
	public Document createDocument(HashMap<Integer,String> mentions, String captions, String elementNum, 
			String paperId, String imageDir, String elementType, String title, 
			String abs, String intro, String lines3, String lines5, String snippet3, String snippet5)
	{
		Document document = new Document();
		TextField captionField = new TextField(Utils.CAPTION_FIELD, captions, Field.Store.YES);
		TextField mention10Field = new TextField(Utils.MENTION_FIELD + "10", mentions.get(10), Field.Store.YES);
		TextField mention20Field = new TextField(Utils.MENTION_FIELD + "20", mentions.get(20), Field.Store.YES);
		TextField mention50Field = new TextField(Utils.MENTION_FIELD + "50", mentions.get(50), Field.Store.YES);
		StringField idField = new StringField(Utils.ID_FIELD, elementNum, Field.Store.YES);
		StringField paperIdField = new StringField(Utils.PAPER_ID_FIELD, paperId, Field.Store.YES);
		StringField fileField = new StringField(Utils.IMAGE_FIELD, imageDir, Field.Store.YES);
		StringField typeField = new StringField(Utils.TYPE_FIELD, elementType, Field.Store.YES);
		TextField titleField = new TextField(Utils.TITLE_FIELD, title, Field.Store.YES);
		TextField absField = new TextField(Utils.ABSTRACT_FIELD, abs, Field.Store.YES);
		TextField introField = new TextField(Utils.INTRO_FIELD, intro, Field.Store.YES);
		
		TextField lines3Field = new TextField("lines3", lines3, Field.Store.YES);
		TextField lines5Field = new TextField("lines5", lines5, Field.Store.YES);
		TextField snippet3Field = new TextField("snippet3", snippet3, Field.Store.YES);
		TextField snippet5Field = new TextField("snippet5", snippet5, Field.Store.YES);
		
		document.add(paperIdField);
		document.add(idField);
		document.add(typeField);
		document.add(captionField);
		document.add(mention10Field);
		document.add(mention20Field);
		document.add(mention50Field);
		document.add(fileField);
		document.add(titleField);
		document.add(absField);
		document.add(introField);
		document.add(lines3Field);
		document.add(lines5Field);
		document.add(snippet3Field);
		document.add(snippet5Field);
				
		return document;
	}
	
 	public void checkIndex() throws IOException
 	{
		Path p = Paths.get(Utils.paramsMap.get("indexDir"));
		Directory indexDirectory = FSDirectory.open(p);
		IndexReader reader = DirectoryReader.open(indexDirectory);
		System.out.println(reader.maxDoc());
		IndexSearcher searcher = new IndexSearcher(reader);
		Document HitDoc = searcher.doc(10);
		
		List<IndexableField> fields = HitDoc.getFields();
		for(IndexableField f : fields)
		{
			String fieldName = f.name();
			String content = HitDoc.get(fieldName);
			System.out.println(fieldName + ": " + content);
		}
 	}
 	
	public static void main(String[] args) throws IOException
	{
		BasicIndexBuilder builder = new BasicIndexBuilder("index.config");
		//builder.build();
		builder.checkIndex();
	}
}