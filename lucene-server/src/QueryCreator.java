import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

/**
 * Functions to help building lucene queries.
 */
public class QueryCreator {
	
	private Analyzer analyzer;
		
	public QueryCreator(Analyzer inputAnalyzer)
	{
		analyzer = inputAnalyzer;
	}
		
	/**
	 * Building a query over a set of fields with uniform field weights.
	 * @param fieldValueMap		a map between field and the associated content (i.e., query term)
	 * @return a Lucene query
	 */
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
	
	/**
	 * Building a query using a set of term over a set of fields with field weights.
	 * @param terms		the query keywords
	 * @param fieldValueMap		a mapping between fields and their weights
	 * @return a Lucene query
	 */
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
			
	/**
	 * Performing pre-processing (e.g., stopword removal and stemming) to query keywords.
	 * @param terms		the query keywords
	 * @return the processed terms
	 */
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
}
