import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class SearchEngine {
	
	public IndexSearcher searcher;
	public IndexReader reader;
	public QueryCreator queryCreator;
	public HashMap<String, Float> searchFields = new HashMap<>();

	public SearchEngine(String indexDir, HashMap<String,Float> searchFields, String similarity) throws IOException
	{
		Path p = Paths.get(indexDir);
		Directory indexDirectory = FSDirectory.open(p);
		this.searchFields = searchFields;
		reader = DirectoryReader.open(indexDirectory);
		searcher = new IndexSearcher(reader);
		
		if (similarity.equals("BM25")) {
			searcher.setSimilarity(new BM25Similarity());
		} else if (similarity.equals("JM01")) {
			searcher.setSimilarity(new LMJelinekMercerSimilarity(0.1f));
		} else if (similarity.equals("DIR100")) {
			searcher.setSimilarity(new LMDirichletSimilarity(100));
		}
		queryCreator = new QueryCreator(searchFields);
	}
	
	public SearchEngine(String indexDir) throws IOException
	{
		Path p = Paths.get(indexDir);
		Directory indexDirectory = FSDirectory.open(p);
		reader = DirectoryReader.open(indexDirectory);
		searcher = new IndexSearcher(reader);
		searcher.setSimilarity(new BM25Similarity());
		queryCreator = new QueryCreator();
	}
	
	public void setSearchFields(HashMap<String,Float> searchFields)
	{
		this.searchFields = searchFields;
		queryCreator.setSearchFields(searchFields);
	}
	
	public ScoreDoc[] search(String keywords, int numResults, String operator) throws IOException
	{
		ArrayList<String> keywordsList = new ArrayList<String>(Arrays.asList(keywords.split(" ")));
		Query query = queryCreator.create(keywordsList, operator);
		TopDocs docs = searcher.search(query, numResults);
		return docs.scoreDocs;
	}
	
	public ScoreDoc[] searchWithFields(String keywords, int numResults, HashMap<String,Double> fieldValueMap) throws IOException
	{
		ArrayList<String> keywordsList = new ArrayList<String>(Arrays.asList(keywords.split(" ")));
		Query query = queryCreator.buildFieldsWeightedQuery(keywordsList, fieldValueMap);
		TopDocs docs = searcher.search(query, numResults);
		return docs.scoreDocs;
	}
	
	public ScoreDoc[] search(Query q, int numResults) throws IOException
	{
		TopDocs docs = searcher.search(q, numResults);	
		return docs.scoreDocs;
	}
	
	public String printResult(ScoreDoc[] result) throws IOException
	{
		String output = "";
		for(int i = 0; i < result.length; i++)
		{
			int rank = i+1;
			Document HitDoc = searcher.doc(result[i].doc);
			List<IndexableField> fields = HitDoc.getFields();
			output += "=================" + rank + "=================\n";
			for(IndexableField f : fields)
			{	
				String fieldName = f.name();
				String content = HitDoc.get(fieldName);
				output += fieldName + ": " + content + "\n";
			}
		}
		return output;
	}
	
	public ArrayList<HashMap<String,String>> getContent(ScoreDoc[] result) throws IOException
	{	
		ArrayList<HashMap<String,String>> output = new ArrayList<>();
		for(int i = 0; i < result.length; i++)
		{
			HashMap<String,String> singleMap = new HashMap<>();
			Document HitDoc = searcher.doc(result[i].doc);
			List<IndexableField> fields = HitDoc.getFields();
			for(IndexableField f : fields)
			{	
				String fieldName = f.name();
				String content = HitDoc.get(fieldName);
				singleMap.put(fieldName, content);
			}
			output.add(singleMap);
		}
		return output;
	}
	
	public ArrayList<HashMap<String,String>> getContentSubset(ScoreDoc[] result, ArrayList<String> fields) throws IOException
	{	
		ArrayList<HashMap<String,String>> output = new ArrayList<>();
		for(int i = 0; i < result.length; i++)
		{
			HashMap<String,String> singleMap = new HashMap<>();
			Document HitDoc = searcher.doc(result[i].doc);
			for(String fieldName : fields)
			{	
				String content = HitDoc.get(fieldName);
				singleMap.put(fieldName, content);
			}
			singleMap.put("score", Float.toString(result[i].score));
			output.add(singleMap);
		}
		return output;
	}
	
	public ArrayList<HashMap<String,String>> getContentById(ArrayList<String> figureIds) throws IOException {
		ArrayList<HashMap<String,String>> output = new ArrayList<>();
		
		for(String figureId : figureIds) {
			HashMap<String, String> fields = getFieldsFromId(figureId, Utils.FIGURE_TYPE);
			if(fields != null)
				output.add(fields);
		}
		return output;	
	}
	
 	public HashMap<String,Float> getTfIdfVector(String text, ArrayList<String> fields) throws IOException
	{
		HashMap<String,Float> tfIdfVec = new HashMap<>();
		HashMap<String,Float> idfVec = new HashMap<>();
		ArrayList<String> terms = queryCreator.analyzeText(text);
		for(String term : terms)
		{
			if(!idfVec.containsKey(term))
				idfVec.put(term, getIdf(term, fields));
			
			tfIdfVec.put(term, tfIdfVec.getOrDefault(term, 0f) + idfVec.get(term));
		}
		return tfIdfVec;	
	}
	
	public Float getIdf(String term, ArrayList<String> fields) throws IOException
	{
		Float docCount = 0f;
		Float numDocs = 0f;
		for(String field : fields)
		{
			Term termInstance = new Term(field, term);                              
			docCount += (float) Math.max(reader.docFreq(termInstance), 0.01);
			numDocs += (float) reader.numDocs();
		}
		return (float) Math.log(numDocs/docCount);
	}
	
	public String getQueryTextFromId(String id, boolean preprocessFlag, String elementType) throws IOException
	{
		String[] args = id.split("_");
		HashMap<String, String> fieldValueMap = new HashMap<>();
		fieldValueMap.put(Utils.PAPER_ID_FIELD, args[0]);
		fieldValueMap.put(Utils.ID_FIELD, args[1]);
		fieldValueMap.put(Utils.TYPE_FIELD, elementType);
		
		Query idQuery = queryCreator.buildFieldsQuery(fieldValueMap);
		
		ScoreDoc[] paperIdResult = search(idQuery, 1);
		assert(paperIdResult.length == 1);
		Document hitDoc = searcher.doc(paperIdResult[0].doc);
		String captionText = hitDoc.get(Utils.CAPTION_FIELD);
		if(preprocessFlag)
			captionText = preprocessCaption(captionText);
		return captionText;
	}
	
	public HashMap<String, String> getFieldsFromId(String id, String elementType) throws IOException
	{
		HashMap<String, String> fields = new HashMap<>();
		String[] args = id.split("_");
		HashMap<String, String> fieldValueMap = new HashMap<>();
		
		
		ArrayList<String> paperArgs = new ArrayList<>();
		for(int i =0; i < args.length-1; i++) {
			paperArgs.add(args[i]);
		}
		String paperId = String.join("_", paperArgs);
		fieldValueMap.put(Utils.PAPER_ID_FIELD, paperId);
		fieldValueMap.put(Utils.ID_FIELD, args[args.length-1]);
		fieldValueMap.put(Utils.TYPE_FIELD, elementType);
		Query idQuery = queryCreator.buildFieldsQuery(fieldValueMap);
		ScoreDoc[] paperIdResult = search(idQuery, 1);
		if (paperIdResult.length != 1) return null;
		Document hitDoc = searcher.doc(paperIdResult[0].doc);
		for(IndexableField field : hitDoc.getFields())
			fields.put(field.name(), field.stringValue());
		return fields;
	}
	
	public String preprocessCaption(String inputCaption) throws IOException
	{
		String outputCaption = "";
		ArrayList<String> keywordsList = new ArrayList<String>(Arrays.asList(inputCaption.split(" ")));
		keywordsList = queryCreator.analyzeTerms(keywordsList);
		for(String keyword : keywordsList)
		{
			if(Utils.isNumeric(keyword)) continue;
			if(keyword.toLowerCase().equals("table")) continue;
			if(keyword.toLowerCase().equals("figure")) continue;
			if(keyword.toLowerCase().equals("fig")) continue;
			outputCaption += keyword + " ";
		}
		return outputCaption.trim();
	}
	
	public void getInPaperFigures(String outputDir) throws IOException {

		int numDocs = reader.maxDoc();
		ArrayList<String> lines = new ArrayList<>();
		for(int i=0; i<numDocs ; i++) {
			Document HitDoc = searcher.doc(i);
			String paperId = HitDoc.get(Utils.PAPER_ID_FIELD);
			String elementId = HitDoc.get(Utils.ID_FIELD);
			String line = paperId + "_" + elementId;
			HashMap<String, String> fieldValueMap = new HashMap<>();
			fieldValueMap.put(Utils.PAPER_ID_FIELD, paperId);
			Query idQuery = queryCreator.buildFieldsQuery(fieldValueMap);
			ScoreDoc[] paperIdResult = search(idQuery, 100);
			
			for(int j = 0; j < paperIdResult.length; j++) {
				Document d = searcher.doc(paperIdResult[j].doc);
				String currId = d.get(Utils.ID_FIELD);
				if(!currId.equals(elementId)) {
					line += "," + paperId + "_" + currId;
				}
			}
			lines.add(line);
		}
		Utils.writeLinesToFile(lines, outputDir);
	}
	
	public static void main(String[] args) throws IOException
	{
		HashMap<String,Float> searchFields = new HashMap<>();
		searchFields.put(Utils.CAPTION_FIELD, 1f);
		searchFields.put(Utils.MENTION_FIELD + "50", 1f);
		SearchEngine se = new SearchEngine("/Users/saarkuzi/acl_data/acl_fig_index/", searchFields, "BM25");
		//SearchEngine se = new SearchEngine("/Users/saarkuzi/Documents/PhDResearch/research_figures/mechanical_index_dir100/", searchFields, "BM25");
		ArrayList<String> figureIds = new ArrayList<>();
		figureIds.add("W14-2511.tei.xml_3");
		figureIds.add("S18-1166.tei.xml_2");
		
		se.getInPaperFigures("acl_inPaperFigs.txt");
	}
}
