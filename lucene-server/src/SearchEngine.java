import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * A search engine of figures which are represented using text data.
 */
public class SearchEngine {
	
	public IndexSearcher searcher;
	public IndexReader reader;
	public QueryCreator queryCreator;

	public SearchEngine(String indexDir, String similarityFunctionName) throws IOException
	{
		Path p = Paths.get(indexDir);
		Directory indexDirectory = FSDirectory.open(p);
		reader = DirectoryReader.open(indexDirectory);
		searcher = new IndexSearcher(reader);
		
		Analyzer analyzer;
		if(Utils.searcherParamsMap.containsKey(Utils.STOPWORDS_CONFIG_PARAM)) {
			CharArraySet stopwords = Utils.loadStopwords(Utils.searcherParamsMap.get(Utils.STOPWORDS_CONFIG_PARAM));
			analyzer = new EnglishKrovetzAnalyzer(stopwords);
		} else {
			analyzer = new EnglishKrovetzAnalyzer();
		}
		queryCreator = new QueryCreator(analyzer);
		
		if (similarityFunctionName.equals(Utils.BM25_SIMILARITY)) {
			searcher.setSimilarity(new BM25Similarity());
		} else if (similarityFunctionName.equals(Utils.JELINEK_MERCER_SIMILARITY)) {
			searcher.setSimilarity(new LMJelinekMercerSimilarity(Utils.JELINEK_MERCER_WEIGHT));
		} else if (similarityFunctionName.equals(Utils.DIRICHLET_SIMILARITY)) {
			searcher.setSimilarity(new LMDirichletSimilarity(Utils.DIRICHLET_MU));
		}
	}
	
	/**
	 * A simple search over the collection using a Lucene query.
	 * @param q		query in Lucene language
	 * @param numResults		number of figures to include in the result list
	 * @return result list
	 */
	public ScoreDoc[] search(Query q, int numResults) throws IOException
	{
		TopDocs docs = searcher.search(q, numResults);	
		return docs.scoreDocs;
	}

	/**
	 * A search over the collection including the construction of the query.
	 * @param keywords		the query text from the user
	 * @param numResults		number of figures to include in the result list
	 * @param fieldValueMap	the fields to use for the query and their weights
	 * @return result list
	 */
	public ScoreDoc[] searchWithFields(String keywords, int numResults, HashMap<String,Double> fieldValueMap) throws IOException
	{
		ArrayList<String> keywordsList = new ArrayList<String>(Arrays.asList(keywords.split(" ")));
		Query query = queryCreator.buildFieldsWeightedQuery(keywordsList, fieldValueMap);
		TopDocs docs = searcher.search(query, numResults);
		return docs.scoreDocs;
	}
			
	/**
	 * Get the content of fields for the figures in a result list (usually to be presented to the user).
	 * @param result		the result list of figures
	 * @param fields		the fields for which we extract the content
	 * @return a list of map where in each map we have the content of a specific figure (ordered as in the result list)
	 */
	public ArrayList<HashMap<String,String>> getResultListContent(ScoreDoc[] result, ArrayList<String> fields) throws IOException
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
			singleMap.put(Utils.SCORE_FIELD, Float.toString(result[i].score));
			output.add(singleMap);
		}
		return output;
	}
	
	/**
	 * Get the content of fields for figures in a list (based on the figure id).
	 * @param figureIds		identifiers of figures for the content extraction (identifier in the form of paperId_figureId)
	 * @return a list of map where in each map we have the content of a specific figure (ordered as in the figures list)
	 */
	public ArrayList<HashMap<String,String>> getFiguresSetContent(ArrayList<String> figureIds) throws IOException {
		ArrayList<HashMap<String,String>> output = new ArrayList<>();
		for(String figureId : figureIds) {
			HashMap<String, String> fields = getFieldsOfSingleFigure(figureId);
			if(fields != null)
				output.add(fields);
		}
		return output;	
	}
	
	/**
	 * Get the content of a single figure.
	 * @param id		figure identifier in the form of paperId_figureId
	 * @return a map where with the content of a specific figure
	 */
	public HashMap<String, String> getFieldsOfSingleFigure(String id) throws IOException
	{
		HashMap<String, String> fields = new HashMap<>();
		String[] args = id.split("_");
		HashMap<String, String> fieldValueMap = new HashMap<>();
		
		fieldValueMap.put(Utils.PAPER_ID_FIELD, args[0]);
		fieldValueMap.put(Utils.FIGURE_ID_FIELD, args[1]);
		Query idQuery = queryCreator.buildFieldsQuery(fieldValueMap);
		ScoreDoc[] paperIdResult = search(idQuery, 1);
		if (paperIdResult.length != 1) return null;
		Document hitDoc = searcher.doc(paperIdResult[0].doc);
		for(IndexableField field : hitDoc.getFields())
			fields.put(field.name(), field.stringValue());
		return fields;
	}
}
