import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.lucene.analysis.CharArraySet;
/*
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
*/

public class Utils {
	
	public static final String configDir = "index.config";
	public static final String FIGURE_TAG = "<figure>";
	public static final String TABLE_TAG = "<table>";
	public static final String MENTION_TAG = "<mention>";
	public static final String MENTION_10_TAG = "<mention10>";
	public static final String MENTION_20_TAG = "<mention20>";
	public static final String MENTION_50_TAG = "<mention50>";
	public static final String CAPTION_TAG = "<caption>";
	public static final String FILE_TAG = "<file>";
	public static final String ABSTRACT_TAG = "<abstract>";
	public static final String TITLE_TAG = "<title>";
	public static final String INTRO_TAG = "<intro>";
	
	public static final String FIGURE_TYPE = "figure";
	public static final String TABLE_TYPE = "table";
	
	public static final String CAPTION_FIELD = "caption";
	public static final String MENTION_FIELD = "mention";
	public static final String ID_FIELD = "elementNum";
	public static final String PAPER_ID_FIELD = "paper_id";
	public static final String IMAGE_FIELD = "fileName";
	public static final String TYPE_FIELD = "type";
	public static final String TITLE_FIELD = "title";
	public static final String ABSTRACT_FIELD = "abstract";
	public static final String ABSTRACT_SEN_FIELD = "absSentence";
	public static final String INTRO_FIELD = "intro";
	public static final String CONC_FIELD = "intro";
	public static final String TEXT_FIELD = "text";

	
	public static final HashMap<String, String> paramsMap = parseParams(configDir);
		
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
			e.printStackTrace();
		}
		
		CharArraySet c = new CharArraySet(stopwordsList.size(), true);
		for(String word : stopwordsList)
			c.add(word);
		
		return c;
	}
	
	public static String getContent(String tag, String content)
	{
		String output = content.replaceAll(tag, "");
		output = output.replaceAll(tag.replaceAll("<", "</"), "");
		return output;
	}
	/*
    public static ArrayList<String> getSentences(String input)
    {
        ArrayList<String> sentenceList = new ArrayList<>();
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation document = new Annotation(input);
        pipeline.annotate(document);
        ArrayList<CoreMap> sentences = (ArrayList<CoreMap>) document.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence : sentences)
        {
        		String originalString = "";
	        	for (CoreLabel token: sentence.get(TokensAnnotation.class)) 
	        		originalString += token.get(TextAnnotation.class) + " ";
	        	sentenceList.add(originalString);
        }
        return sentenceList;
    }*/
    
    public static double getCosine(HashMap<String,Float> vec1, HashMap<String,Float> vec2)
    {
    		double norm1 = 0f;
    		double norm2 = 0f;
    		double enumerator = 0f;
    		for(String term1 : vec1.keySet())
    		{
    			norm1 += Math.pow(vec1.get(term1), 2);
    			enumerator += vec1.get(term1) * vec2.getOrDefault(term1, 0f);
    		}
    		
    		for(String term2 : vec2.keySet())
    			norm2 += Math.pow(vec2.get(term2), 2);
    		
    		norm1 = Math.sqrt(norm1);
    		norm2 = Math.sqrt(norm2);
    		
    		return enumerator / (norm1 * norm2);
    }
    
	public static void writeLinesToFile(ArrayList<String> lines, String outputDir) throws IOException
	{
		File fout = new File(outputDir);
		FileOutputStream fos = new FileOutputStream(fout);
	 
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
	 
		
		for (int i = 0; i< lines.size(); i++) {
			bw.write(lines.get(i));
			bw.newLine();
		}
		bw.close();
	}
	
	public static ArrayList<String> readLinesFromFile(String linesDir)
	{
		ArrayList<String> output = new ArrayList<>();
		try {
			File file = new File(linesDir);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				output.add(line);
			}
			fileReader.close();	
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}
	
	public static Map sortByValue(Map unsortedMap)
	{
		Map sortedMap = new TreeMap(new ValueComparator(unsortedMap));
		sortedMap.putAll(unsortedMap);
		return sortedMap;
	}
	
	public static boolean isNumeric(String str)  
	{  
	  try  
	  {  
	    Double.parseDouble(str);  
	  }  
	  catch(NumberFormatException nfe)  
	  {  
	    return false;  
	  }  
	  return true;  
	}
	
    public static void main(String[] args)
    {
    		String text = "the dog eat food. the cat goes home. the world is round";
    }
}

class ValueComparator implements Comparator {
	Map map;
	public ValueComparator(Map map) {
		this.map = map;
	}
	
	public int compare(Object keyA, Object keyB) {
		Comparable valueA = (Comparable) map.get(keyA);
		Comparable valueB = (Comparable) map.get(keyB);
		return valueB.compareTo(valueA);
	}
}



