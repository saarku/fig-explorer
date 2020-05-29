import java.io.IOException;
import java.util.HashMap;
import py4j.GatewayServer;

public class LuceneServerSimple {
	
	public SearchEngine engine;
	
	public LuceneServerSimple() throws IOException
	{		
		HashMap<String,Float> searchFields = new HashMap<>();
		searchFields.put(Utils.CAPTION_FIELD, 1f);
		searchFields.put(Utils.MENTION_FIELD + "50", 1f);
		engine = new SearchEngine(Utils.paramsMap.get("indexDir"), searchFields, "BM25");
	}
	
	public SearchEngine getSearchEngine()
	{
		return engine;
	}
	
 	public static void main(String[] args) throws IOException
 	{
		GatewayServer gatewayServer = new GatewayServer(new LuceneServerSimple(), 25000);
        gatewayServer.start();
        System.out.println("Gateway Server Started");
 	}
}