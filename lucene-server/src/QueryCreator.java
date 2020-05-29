import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

public class QueryCreator {
	
	private Analyzer analyzer;
	private HashMap<String,Float> searchFields = new HashMap<>();
	
	public QueryCreator(HashMap<String,Float> searchFields)
	{
		CharArraySet stopwords = Utils.loadStopwords(Utils.paramsMap.get("stopwordDir"));
		analyzer = new EnglishKrovetzAnalyzer(stopwords);
		this.searchFields = searchFields;
	}
	
	public QueryCreator()
	{
		CharArraySet stopwords = Utils.loadStopwords(Utils.paramsMap.get("stopwordDir"));
		analyzer = new EnglishKrovetzAnalyzer(stopwords);
	}
	
	public static Query buildIdQuery(String id, String idField)
	{
		BooleanQuery.Builder termQueryBuilder = new BooleanQuery.Builder();		
		Term t = new Term(idField, id);
		BoostQuery boostQuery = new BoostQuery(new TermQuery(t), 1f);
		BooleanClause booleanClause;
		booleanClause = new BooleanClause(boostQuery, Occur.SHOULD);
		termQueryBuilder.add(booleanClause);
		return termQueryBuilder.build();
	}
	
	public Query buildFieldsQuery(HashMap<String,String> fieldValueMap)
	{
		BooleanQuery.Builder termQueryBuilder = new BooleanQuery.Builder();	
		for(Map.Entry<String, String> e : fieldValueMap.entrySet())
		{	
			Term t = new Term(e.getKey(), e.getValue());
			BoostQuery boostQuery = new BoostQuery(new TermQuery(t), 1f);
			BooleanClause booleanClause;
			booleanClause = new BooleanClause(boostQuery, Occur.MUST);
			termQueryBuilder.add(booleanClause);
		}
		return termQueryBuilder.build();
	}
	
	public Query buildFieldsWeightedQuery(ArrayList<String> terms, HashMap<String,Double> fieldValueMap) throws IOException
	{
		ArrayList<String> analyzedTerms = analyzeTerms(terms);
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
		for(String term : analyzedTerms)
		{
			BooleanQuery.Builder termQueryBuilder = new BooleanQuery.Builder();
			for(String searchField : fieldValueMap.keySet())
			{
				Term t = new Term(searchField, term);
				BoostQuery boostQuery = new BoostQuery(new TermQuery(t), fieldValueMap.get(searchField).floatValue());
				BooleanClause booleanClause;
				booleanClause = new BooleanClause(boostQuery, Occur.SHOULD);
				termQueryBuilder.add(booleanClause);
				
			}
			booleanQueryBuilder.add(termQueryBuilder.build(), Occur.SHOULD);
		}
		return booleanQueryBuilder.build();
	}
	
	public Query buildSingleFieldQuery(String text, String field) throws IOException
	{
		
		ArrayList<String> analyzedTerms = analyzeText(text);
		BooleanQuery.Builder termQueryBuilder = new BooleanQuery.Builder();
		for(String term : analyzedTerms)
		{
			Term t = new Term(field, term);
			BoostQuery boostQuery = new BoostQuery(new TermQuery(t), 1);
			BooleanClause booleanClause;
			booleanClause = new BooleanClause(boostQuery, Occur.SHOULD);
			termQueryBuilder.add(booleanClause);		
		}
		return termQueryBuilder.build();
	}
	
	public void setSearchFields(HashMap<String,Float> searchFields)
	{
		this.searchFields = searchFields;
	}
	
	public Query create(ArrayList<String> terms, String operator) throws IOException
	{
		ArrayList<String> analyzedTerms = analyzeTerms(terms);
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
		for(String term : analyzedTerms)
		{
			BooleanQuery.Builder termQueryBuilder = new BooleanQuery.Builder();
			for(String searchField : searchFields.keySet())
			{
				Term t = new Term(searchField, term);
				BoostQuery boostQuery = new BoostQuery(new TermQuery(t), searchFields.get(searchField));
				BooleanClause booleanClause;
				booleanClause = new BooleanClause(boostQuery, Occur.SHOULD);
				termQueryBuilder.add(booleanClause);
				
			}
			if (operator.equals("or"))
				booleanQueryBuilder.add(termQueryBuilder.build(), Occur.SHOULD);
			else
				booleanQueryBuilder.add(termQueryBuilder.build(), Occur.MUST);
		}
		return booleanQueryBuilder.build();
	}
	
	public ArrayList<String> analyzeTerms(ArrayList<String> terms) throws IOException
	{
		ArrayList<String> analyzedTerms = new ArrayList<>();
		for(String term : terms)
		{
			String analyzedTerm = null;
			TokenStream tokenStream = analyzer.tokenStream("temp", term);
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

			tokenStream.reset();
			while (tokenStream.incrementToken()) {
			    analyzedTerm = charTermAttribute.toString();
			}
			tokenStream.close();
			
			if(analyzedTerm != null)
				analyzedTerms.add(analyzedTerm);
		}
		return analyzedTerms;
	}
	
	public ArrayList<String> analyzeText(String text) throws IOException
	{
		ArrayList<String> analyzedTerms = new ArrayList<>();

		String analyzedTerm = null;
		TokenStream tokenStream = analyzer.tokenStream("temp", text);
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

		tokenStream.reset();
		while (tokenStream.incrementToken()) {
		    analyzedTerm = charTermAttribute.toString();
		    if(analyzedTerm != null)
		    		analyzedTerms.add(analyzedTerm);
		}
		tokenStream.close();
		return analyzedTerms;
	}
	
	public String analyzeTextToText(String text) throws IOException
	{
		String output = "";
		ArrayList<String> terms = analyzeText(text);
		for(String t : terms)
			output += t + " ";
		return output.trim();
	}
	
	public static void main(String[] args) throws IOException
	{
		QueryCreator creator = new QueryCreator(new HashMap<>());
		String text = "what's up man? I'm good just went out with the dog";
		System.out.println(creator.analyzeText(text));
		
	}

}
