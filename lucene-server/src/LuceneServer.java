import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import py4j.GatewayServer;

public class LuceneServer {
	
	public HashMap<String,SearchEngine> engines;
	
	public SearchEngine getSearchEngine(String name)
	{
		return engines.get(name);
	}
	
	public LuceneServer() throws IOException
	{		
		engines = new HashMap<>();
		HashMap<String,Float> searchFields = new HashMap<>();
		searchFields.put(Utils.CAPTION_FIELD, 1f);
		searchFields.put(Utils.MENTION_FIELD + "50", 1f);
		
		engines.put("acl_bm25", new SearchEngine(Utils.paramsMap.get("indexDirAclBM25"), searchFields, "BM25"));
		engines.put("acl_jm01", new SearchEngine(Utils.paramsMap.get("indexDirAclJM01"), searchFields, "JM01"));
		engines.put("acl_dir100", new SearchEngine(Utils.paramsMap.get("indexDirAclDIR100"), searchFields, "DIR100"));
		
		engines.put("railway_bm25", new SearchEngine(Utils.paramsMap.get("indexDirRailwayBM25"), searchFields, "BM25"));
		engines.put("railway_jm01", new SearchEngine(Utils.paramsMap.get("indexDirRailwayJM01"), searchFields, "JM01"));
		engines.put("railway_dir100", new SearchEngine(Utils.paramsMap.get("indexDirRailwayDIR100"), searchFields, "DIR100"));
	}
	
 	public static void main(String[] args) throws IOException
 	{
		GatewayServer gatewayServer = new GatewayServer(new LuceneServer(), 25000);
        gatewayServer.start();
        System.out.println("Gateway Server Started");
 	}
}