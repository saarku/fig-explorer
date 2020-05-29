import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;

public class Embeddings {
	
	private SearchEngine se;
	
	public Embeddings() throws IOException
	{
		se = new SearchEngine(Utils.paramsMap.get("indexDir"), new HashMap<>(), "BM25");
	}
	
	public void createQueriesFile(String listDir, String outputDir) throws IOException
	{
		ArrayList<String> queries = Utils.readLinesFromFile(listDir);
		ArrayList<String> outputLines = new ArrayList<>();
		for(String qid : queries)
		{
			String qText = se.getQueryTextFromId(qid, true, "figure");
			ArrayList<String> qTerms = se.queryCreator.analyzeText(qText);
			String line = "";
			for(String t : qTerms)
				line += t + " ";
			outputLines.add(qid + "," + line.trim());
		}
		Utils.writeLinesToFile(outputLines, outputDir);
	}
	
	public void createDocsFile(String testDataDir, String trainDataDir, String outputDir, 
			HashMap<String,String> features) throws IOException
	{
		ArrayList<String> docIdList = new ArrayList<>();		
		ArrayList<String> testLines = Utils.readLinesFromFile(testDataDir);
		ArrayList<String> trainLines = Utils.readLinesFromFile(trainDataDir);
		ArrayList<String> outputLines = new ArrayList<>();
		for(String line : testLines)
		{
			String docId = line.split("docid:")[1].trim();
			if(!docIdList.contains(docId))
				docIdList.add(docId);
		}
		for(String line : trainLines)
		{
			String docId = line.split("docid:")[1].trim();
			if(!docIdList.contains(docId))
				docIdList.add(docId);
		}
		
		for(String docId : docIdList)
		{
			HashMap<String,String> fields = se.getFieldsFromId(docId, "figure");
			if(fields == null) 
			{
				System.out.print("missing doc " + docId);
				continue;
			}
			
			for(String f : features.keySet())
			{
				String featureVal = fields.get(f);
				if(featureVal == null)
					featureVal = "";
				else
					featureVal = se.queryCreator.analyzeTextToText(featureVal);
				String outputLine = docId + "," + f + "," + featureVal;
				outputLines.add(outputLine);
			}
		}
		Utils.writeLinesToFile(outputLines, outputDir);
	}
	
	public void createIdfFile() throws IOException
	{
		ArrayList<String> idfFields = new ArrayList<>();
		ArrayList<String> outputLines = new ArrayList<>();
		idfFields.add(Utils.TEXT_FIELD);
		Fields fields = MultiFields.getFields(se.reader);
		Terms terms = fields.terms(Utils.TEXT_FIELD);
		TermsEnum termsEnum = terms.iterator();
		while(termsEnum.next() != null)
		{
			String t = termsEnum.term().utf8ToString();
			double idf = se.getIdf(t, idfFields);
			outputLines.add(t + " " + idf);
		}
		Utils.writeLinesToFile(outputLines, "figures.idf");
	}
	
	public static void main(String[] args) throws IOException
	{
		HashMap<String,String> features = new HashMap<>();
		features.put("mention20", "1");
		features.put("mention50", "2");
		features.put("mention10", "3");
		features.put("absSentence", "4");
		features.put("abstract", "5");
		features.put("title", "6");
		features.put("conclusion", "7");
		features.put("intro", "8");
		features.put("text", "9");
		
		Embeddings e = new Embeddings();
		e.createIdfFile();
		
		//e.createQueriesFile("figures.test", "figures.terms.test");
		e.createDocsFile("figures.test.svm", 
				"figures.train.svm", "figures.fields", features);
	}
}
