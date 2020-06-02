import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.analysis.CharArraySet;

/**
 * General utilities for the project + static final variables (constants).
 */
public class Utils {
	
	public static final String INDEX_CONFIG_DIR = "index.builder.config";
	public static final String SEARCHER_CONFIG_DIR = "lucene.server.config";
	public static final float DIRICHLET_MU = 100;
	public static final float JELINEK_MERCER_WEIGHT = 0.1f;
	public static final String BM25_SIMILARITY = "BM25";
	public static final String DIRICHLET_SIMILARITY = "Dirichlet";
	public static final String JELINEK_MERCER_SIMILARITY = "Jelinek-Mercer";
	public static final String STORE_FIELD = "store";
	public static final String STRING_FIELD = "string";
	public static final String TEXT_FIELD = "text";
	public static final String SCORE_FIELD = "score";
	public static final String FIGURE_ID_FIELD = "figure";
	public static final String PAPER_ID_FIELD = "paper";
	public static final String STOPWORDS_CONFIG_PARAM = "stopwordDir";
	public static final String SIMILARITY_CONFIG_PARAM = "similarityFunctionName";
	public static final String INDEX_CONFIG_PARAM = "indexDir";
	public static final String DATA_CONFIG_PARAM = "dataDir";
	public static final String COLLECTION_CONFIG_PARAM = "collectionName";
	public static final String PORT_CONFIG_PARAM = "portNumber";
	
	public static final int FIELD_TYPE_POSITION = 0;
	public static final int FIELD_STORE_POSITION = 1;
	public static final int FIELD_LENGTH_POSITION = 2;
	
	public static final HashMap<String, String> indexParamsMap = parseParams(INDEX_CONFIG_DIR);
	public static final HashMap<String, String> searcherParamsMap = parseParams(SEARCHER_CONFIG_DIR);
	public static final HashMap<String, ArrayList<String>> fieldInfo = getFieldsInfo();
		
	/**
	 * Get the configuration of fields for the indexing process.
	 * @return a map with keys which are the field names and the values are 3-element lists [type,storeField,minLength]
	 */
	public static HashMap<String, ArrayList<String>> getFieldsInfo()
	{
		HashMap<String, ArrayList<String>> fieldsMap = new HashMap<>();
		
		String fieldsInfo = indexParamsMap.get("fields");
		if(fieldsInfo == null)
			return fieldsMap;
		
		for(String triple : fieldsInfo.split(";"))
		{	
			String[] singles = triple.split(",");
			
			if(singles.length == FIELD_LENGTH_POSITION + 2) {
				fieldsMap.put(singles[0], new ArrayList<>());
				fieldsMap.get(singles[0]).add(singles[FIELD_TYPE_POSITION + 1]);
				fieldsMap.get(singles[0]).add(singles[FIELD_STORE_POSITION + 1]);
				fieldsMap.get(singles[0]).add(singles[FIELD_LENGTH_POSITION + 1]);
			} else {
				System.err.println("Incorrect number of parameters for the fields in the configuration file.");
				System.exit(-1);
			}
		}
		return fieldsMap;
	}
	
	/**
	 * Parse the configuration file (for either search or indexing).
	 * @return a map between configuration parameters and their values
	 */
	public static HashMap<String, String> parseParams(String paramsDir)
	{
		HashMap<String,String> params = new HashMap<>();
		try {
			File file = new File(paramsDir);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				String[] args = line.split("=");
				params.put(args[0], args[1]);
			}
				fileReader.close();	
			} catch (IOException e) {
				e.printStackTrace();
			}
		return params;
	}
	
	/**
	 * Loading a set of stopwords to be used for indexing/search.
	 * @param stopwordsFileDir	file with a stopword in each line
	 * @return a set of stopwords
	 */
	public static CharArraySet loadStopwords(String stopwordsFileDir)
	{
		ArrayList<String> stopwordsList = new ArrayList<>();
		
		try {
				File file = new File(stopwordsFileDir);
				FileReader fileReader = new FileReader(file);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					stopwordsList.add(line);
				}	
				fileReader.close();
				
		} catch (IOException e) {
			System.err.println("An error occured while parsing the stopword list file.");
			System.exit(-1);
		}
		
		CharArraySet c = new CharArraySet(stopwordsList.size(), true);
		for(String word : stopwordsList)
			c.add(word);
		
		return c;
	}
	
}