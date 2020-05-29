import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

public class Evaluation {
	
	public static String citationsDir = "/Users/saarkuzi/Documents/PycharmProjects/figure-embeddings/data/raw_data/out_paper_pairs.txt";
	
	
	private static HashMap<String, ArrayList<String>> parseCitations() {
		HashMap<String, ArrayList<String>> citations = new HashMap<>();
		ArrayList<String> lines = Utils.readLinesFromFile(citationsDir);
		for(String line : lines) {
			String[] args = line.split(",");
			if(!citations.containsKey(args[0])) {
				citations.put(args[0], new ArrayList<>());
			}
			if(!citations.containsKey(args[1])) {
				citations.put(args[1], new ArrayList<>());
			}
			citations.get(args[0]).add(args[1]);
			citations.get(args[1]).add(args[0]);
		}
		return citations;
	}
	
	public static void evaluate(HashMap<String, HashMap<String, Float>> resultLists, String outputDir) throws IOException
	{
		Float successAt1 = 0f;
		Float successAt3 = 0f;
		Float successAt5 = 0f;
		Float successAt10 = 0f;
		Float mrr = 0f;
		
		ArrayList<String> ranksList = new ArrayList<>();
		
		Float numQueries = 0f;
		for(String qid : resultLists.keySet())
		{
			numQueries++;
			HashMap<String, Float> resultList = resultLists.get(qid);

			Map<String,Float> sortedResult = Utils.sortByValue(resultList);
			//Map<String,Float> sortedResult = resultList;
			
			Float rank = 1f;
			boolean existFlag = false;
			for(String docId : sortedResult.keySet())
			{				
				if(docId.equals(qid))
				{
					existFlag = true;
					break;
				}
				rank++;
			}
			
			if(existFlag)
				ranksList.add(qid + " " + rank);
			else
				ranksList.add(qid + " 0");
			
			if(rank == 1)
				successAt1 += 1;
			if(rank <= 3)
				successAt3 += 1;
			if(rank <= 5)
				successAt5 += 1;
			if(rank <= 10)
				successAt10 += 1;
			if(existFlag)
				mrr += 1f / rank;
		}
		
		ranksList.add("success@1 " + successAt1 / numQueries);
		ranksList.add("success@3 " + successAt3 / numQueries);
		ranksList.add("success@5 " + successAt5 / numQueries);
		ranksList.add("success@10 " + successAt10 / numQueries);
		ranksList.add("mrr " + mrr / numQueries);

		Utils.writeLinesToFile(ranksList, outputDir);
		
		
	}
	
	public static void evaluateRelated(HashMap<String, HashMap<String, Float>> resultLists, String outputDir) throws IOException
	{
		HashMap<String, ArrayList<String>> citations = parseCitations();
		Float p3_out = 0f;
		Float p3_in = 0f;
		Float p3_both = 0f;
		Float p5_out = 0f;
		Float p5_in = 0f;
		Float p5_both = 0f;
				
		Float numQueries = 0f;
		for(String qid : resultLists.keySet())
		{
			numQueries++;
			HashMap<String, Float> resultList = resultLists.get(qid);
			Map<String,Float> sortedResult = Utils.sortByValue(resultList);
			
			float in_paper_counter = 0f;
			float out_paper_counter = 0f;
			int rank = 1;
			
			boolean citationFlag = true;
			if(!citations.containsKey(qid)) {
				citationFlag = false;
			}
			
			for(String docId : sortedResult.keySet())
			{
				if(citationFlag) {
					if(citations.get(qid).contains(docId)) {
						out_paper_counter += 1f;
					}
				}
				
				if(qid.split("_")[0].equals(docId.split("_")[0])) {
					in_paper_counter += 1f;
				}
				
				if(rank == 3) {
					p3_out += out_paper_counter / 3f;
					p3_in += in_paper_counter / 3f;
					p3_both += (out_paper_counter + in_paper_counter) / 3f;
				}
				
				if(rank == 5)  {
					p5_out += out_paper_counter / 5f;
					p5_in += in_paper_counter / 5f;
					p5_both += (out_paper_counter + in_paper_counter) / 5f;
					break;
				}
				rank++;
			}
		}

		System.out.println("p@3 in: " + p3_in / numQueries);
		System.out.println("p@3 out: " + p3_out / numQueries);
		System.out.println("p@3 both: " + p3_both / numQueries);
		System.out.println("p@5 in: " + p5_in / numQueries);
		System.out.println("p@5 out: " + p5_out / numQueries);
		System.out.println("p@5 both: " + p5_both / numQueries);
	}
	
	public static int getFigureRank(ScoreDoc[] results, int docId)
	{
		int rank = 0;		
		for(int i = 0 ; i < results.length ; i++)
		{
			if(docId == results[i].doc)
			{
				rank = i + 1;
				break;
			}
		}
		return rank;
	}
	
	public static void evaluateInitial(HashMap<String, ArrayList<String>> resultLists, String outputDir) throws IOException
	{
		Float successAt1 = 0f;
		Float successAt3 = 0f;
		Float successAt5 = 0f;
		Float successAt10 = 0f;
		Float successAt100 = 0f;
		Float mrr = 0f;
		
		ArrayList<String> ranksList = new ArrayList<>();
		
		Float numQueries = 0f;
		for(String qid : resultLists.keySet())
		{
			numQueries++;
			ArrayList<String> resultList = resultLists.get(qid);

			Float rank = 1f;
			boolean existFlag = false;
			for(String docId : resultList)
			{
				if(docId.equals(qid))
				{
					existFlag = true;
					break;
				}
				rank++;
			}
			
			if(existFlag)
				ranksList.add(qid + " " + rank);
			else
				ranksList.add(qid + " 0");
			
			if(rank == 1)
				successAt1 += 1;
			if(rank <= 3)
				successAt3 += 1;
			if(rank <= 5)
				successAt5 += 1;
			if(rank <= 10)
				successAt10 += 1;
			if(rank <= 100)
				successAt100 += 1;
			if(existFlag)
				mrr += 1f / rank;
		}
		
		ranksList.add("success@1 " + successAt1 / numQueries);
		ranksList.add("success@3 " + successAt3 / numQueries);
		ranksList.add("success@5 " + successAt5 / numQueries);
		ranksList.add("success@10 " + successAt10 / numQueries);
		ranksList.add("success@100 " + successAt100 / numQueries);
		ranksList.add("mrr " + mrr / numQueries);

		Utils.writeLinesToFile(ranksList, outputDir);
	}

	public static int getFigureRankId(ScoreDoc[] results, String figId, SearchEngine se) throws IOException
	{
		int rank = 1;
		for(ScoreDoc sd : results)
		{
			Document hitDoc = se.reader.document(sd.doc);
			String id = hitDoc.get(Utils.PAPER_ID_FIELD) + ".tei.xml_" + hitDoc.get(Utils.ID_FIELD);
			//String id = hitDoc.get(Utils.PAPER_ID_FIELD) + "_" + hitDoc.get(Utils.ID_FIELD);
			//System.out.println(id + " " + figId.replace("-", ""));
			if(id.equals(figId.replace("-", ""))) return rank;
			//if(id.equals(figId)) return rank;
			rank++;
		}
		return 0;
	}
	
	public static void main(String[] args)
	{
		HashMap<String,Float> temp = new HashMap<>();
		temp.put("a", 2f);
		temp.put("b", 3f);
		temp.put("d", 100f);
		temp.put("c", 1f);
		
		Map<String,Float> sorted = Utils.sortByValue(temp);
		for(String id : sorted.keySet())
		{
			System.out.println(id);
		}
	}
}
