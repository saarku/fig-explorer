import java.io.IOException;
import java.util.HashMap;

import py4j.GatewayServer;

/**
 * A search engine server which receives request from some port.
 */
public class LuceneServer {
	
	public HashMap<String,SearchEngine> engines;
	
	public SearchEngine getSearchEngine(String name)
	{
		return engines.get(name);
	}
	
	public LuceneServer() throws IOException
	{		
		engines = new HashMap<>();
		String[] indexDirs = Utils.searcherParamsMap.get(Utils.INDEX_CONFIG_PARAM).split(",");
		String[] similarityNames = Utils.searcherParamsMap.get(Utils.SIMILARITY_CONFIG_PARAM).split(",");
		String[] collectionNames = Utils.searcherParamsMap.get(Utils.COLLECTION_CONFIG_PARAM).split(",");
		
		for(int i=0; i < indexDirs.length; i++) {
			SearchEngine singleEngine = new SearchEngine(indexDirs[i], similarityNames[i]);
			String engineName = collectionNames[i] + "_" + similarityNames[i];
			System.out.println("Index \"" + engineName + "\" Started Successfully");
			engines.put(engineName, singleEngine);
		}
	}
	
 	public static void main(String[] args) throws IOException
 	{
 		int portNumber = Integer.parseInt(Utils.searcherParamsMap.get(Utils.PORT_CONFIG_PARAM));
		GatewayServer gatewayServer = new GatewayServer(new LuceneServer(), portNumber);
        gatewayServer.start();
        System.out.println("Lucene Server Started");
 	}
}