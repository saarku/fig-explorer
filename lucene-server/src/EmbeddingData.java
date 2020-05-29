import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class EmbeddingData {
	
	public IndexReader reader;
	public IndexSearcher searcher;
	public SearchEngine se;
	
	public EmbeddingData() throws IOException {
		String indexDir = Utils.paramsMap.get("indexDir");
		Path p = Paths.get(indexDir);
		Directory indexDirectory = FSDirectory.open(p);
		reader = DirectoryReader.open(indexDirectory);
		searcher = new IndexSearcher(reader);
		HashMap<String,Float> searchFields = new HashMap<>();
		searchFields.put("caption", 0.5f);
		searchFields.put("lines3", 0.5f);
		se = new SearchEngine(indexDir, searchFields, "BM25");
	}
	
	public void buildData() throws IOException {
		int maxDoc = reader.maxDoc();
		ArrayList<String> lines = new ArrayList<>();
		
		for(int i=0; i < maxDoc; i++) {
			if(i%1000 == 0) {
				System.out.println(i);
			}
			
			Document hitDoc = searcher.doc(i);
			String caption = hitDoc.get("caption");
			String figureId =  hitDoc.get("paper_id") + "_" + hitDoc.get("elementNum");
			String fileName = hitDoc.get("fileName");
			
			if(fileName.equals("")) {
				continue;
			}
			
			ScoreDoc[] result = se.search(caption, 11, "or");
			
			for(int j=0; j < result.length; j++) {
				
				Document resultHitDoc = searcher.doc(result[j].doc);
				String resultId = resultHitDoc.get("paper_id") + "_" + resultHitDoc.get("elementNum");
				if(resultId.equals(figureId) || resultHitDoc.get("fileName").equals("")) {
					continue;
				}
				lines.add(figureId + "," + resultId + ",1,1");
			}
		}		
		Utils.writeLinesToFile(lines, "bm25_pairs.txt");
	}
	
	public void getFileNames() throws IOException {
		int maxDoc = reader.maxDoc();
		ArrayList<String> lines = new ArrayList<>();
		
		for(int i=0; i < maxDoc; i++) {
			Document hitDoc = searcher.doc(i);
			String figureId =  hitDoc.get("paper_id") + "_" + hitDoc.get("elementNum");
			String fileName = hitDoc.get("fileName");
			if(!fileName.equals("")) {
				lines.add(figureId + "," + fileName);
			}
		}		
		Utils.writeLinesToFile(lines, "file_names.txt");
	}
	
	public static void main(String[] args) throws IOException {
		EmbeddingData ed = new EmbeddingData();
		ed.buildData();
	}
}


