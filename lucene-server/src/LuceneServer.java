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
		String[] indexs = Utils.searcherParamsMap.get(Utils.INDEX_CONFIG_PARAM).split(";");

		
		for(int i=0; i < indexs.length; i++) {
			String[] indexArgs = indexs[i].split(",");
			SearchEngine singleEngine = new SearchEngine(indexArgs[2], indexArgs[1]);
			String engineName = indexArgs[0] + "_" + indexArgs[1];
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