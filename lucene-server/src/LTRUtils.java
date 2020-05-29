import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class LTRUtils {
	
	private static String svmDir = "./svm_rank/";
	
	public static void learnSVMModel(String dataDir, String modelDir, Float c, int kernel, int numSamples, ArrayList<String> features) throws IOException, InterruptedException
	{
		//kernel options- 0:linear, 1:polynomial, 2:radial, 3:sigmoid tanh, 4:user defined
		System.out.println("preparing data for learning...");
		LTRUtils.modifyData(dataDir, "data.temp", numSamples, features, true);
		String cmd = svmDir + "svm_rank_learn -c " + c + " -t " + kernel;
		cmd += " data.temp " + modelDir;
		System.out.println("learning a model...");
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(cmd);
		pr.waitFor();
		System.out.println("done.");
		pr = rt.exec("rm data.temp");
	}
	
	public static void learnRankLib(String dataDir, String modelDir, 
			int ranker, ArrayList<String> features, String norm, String metric) throws IOException, InterruptedException
	{
		// ranker:  0:MART, 1:RankNet, 2:RankBoost, 3:AdaRank, 4:CoordinateAscent, 6:LambdaMART, 7:ListNet, 8:RandomForests
		// norm: none, sum, zscore, linear
		// metric: MAP, NDCG@k, DCG@k, P@k, RR@k, ERR@k
		
		String normCmd = "";
		if(!norm.equals("none"))
			normCmd = " -norm " + norm;
		
		Utils.writeLinesToFile(features, "features.temp");
		String cmd = "java -jar jar/RankLib-2.10.jar -train " + dataDir + " -ranker " + ranker;
		cmd += " -feature features.temp -metric2t " + metric + normCmd + " -save " + modelDir;
		Runtime rt = Runtime.getRuntime();
		System.out.println(cmd);
		System.out.println("Learning model..");
		Process pr = rt.exec(cmd);
		pr.waitFor();
		pr = rt.exec("rm features.temp");
		System.out.println("Finished learning.");
	}
	
	public static ArrayList<String> predictSVM(String modelDir, String dataDir, ArrayList<String> features) throws InterruptedException, IOException
	{
		LTRUtils.modifyData(dataDir, "data.temp", 1000, features, false);
		String dataName = "data.temp";
		//dataName = dataDir;
		String cmd = svmDir + "svm_rank_classify " + dataName + " " + modelDir + " temp.prediction";
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(cmd);
		pr.waitFor();
		ArrayList<String> predictions = Utils.readLinesFromFile("temp.prediction");
		pr = rt.exec("rm temp.prediction data.temp");
		pr.waitFor();
		return predictions;
	}
	
	public static ArrayList<String> predictRankLib(String modelDir, String dataDir, String norm) throws InterruptedException, IOException
	{
		String cmd = "java -jar jar/RankLib-2.10.jar -load " + modelDir;
		cmd += " -rank " + dataDir + " -score  temp.prediction -norm " + norm;
		
		System.out.println(cmd);
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(cmd);
		pr.waitFor();
		ArrayList<String> lines = Utils.readLinesFromFile("temp.prediction");
		ArrayList<String> predictions = new ArrayList<>();
		for(String line : lines)
			predictions.add(line.split("\\t")[2]);
		pr = rt.exec("rm temp.prediction");
		return predictions;
	}
	
	public static ArrayList<String> getSingleFeatPredictions(String dataDir, String featNum) throws InterruptedException, IOException
	{
		ArrayList<String> lines = Utils.readLinesFromFile(dataDir);
		ArrayList<String> predictions = new ArrayList<>();
		for(String line : lines)
		{
			String[] args = line.split("#")[0].split(" ");
			for(int i = 2 ; i < args.length ; i++)
			{
				if(featNum.equals(args[i].split(":")[0]))
					predictions.add(args[i].split(":")[1]);
			}
		}
		return predictions;
	}
	
	public static void modifyData(String dataDir, String outputDir, int numSamples, ArrayList<String> features, boolean shuffleFlag) throws IOException
	{
		ArrayList<String> dataLines = Utils.readLinesFromFile(dataDir);
		ArrayList<String> beforeList = new ArrayList<>();
		ArrayList<String> afterList = new ArrayList<>();
		ArrayList<String> modifiedList = new ArrayList<>();
		
		boolean beforeFlag = true;
		String queryLine = "";
		String currentQid = dataLines.get(0).split(" ")[1];
		
		for(String line : dataLines)
		{
			String[] args = line.split(" ");
			String qid = args[1];
			if(!qid.equals(currentQid))
			{
				if(shuffleFlag)
				{
					Collections.shuffle(afterList);
					Collections.shuffle(beforeList);
				}
				for(int i=0 ; i < numSamples+1; i++)
				{
					if(beforeList.size() > i)
						modifiedList.add(modifyLine(beforeList.get(i), features));
				}
				if(!queryLine.equals("")) modifiedList.add(queryLine);
				
				for(int i=0 ; i < numSamples+1; i++)
				{
					if(afterList.size() > i)
						modifiedList.add(modifyLine(afterList.get(i), features));
				}
				
				afterList = new ArrayList<>();
				beforeList = new ArrayList<>();
				beforeFlag = true;
				queryLine = "";
				currentQid = qid;
			}
			
			String rel = args[0];
			if(rel.equals("1")) 
			{
				beforeFlag = false;
				//modifiedList.add(modifyLine(line, features));
				queryLine = modifyLine(line, features);
				continue;
			}
			
			if(beforeFlag)
				beforeList.add(line);
			else
				afterList.add(line);	
		}
		
		if(shuffleFlag)
		{
			Collections.shuffle(afterList);
			Collections.shuffle(beforeList);
		}
		
		for(int i=0 ; i < numSamples; i++)
		{
			if(beforeList.size() > i)
				modifiedList.add(modifyLine(beforeList.get(i), features));
		}
		if(!queryLine.equals("")) modifiedList.add(queryLine);
		
		for(int i=0 ; i < numSamples; i++)
		{
			if(afterList.size() > i)
				modifiedList.add(modifyLine(afterList.get(i), features));
		}
		Utils.writeLinesToFile(modifiedList, outputDir);
	}
	
	public static String modifyLine(String line, ArrayList<String> features)
	{
		String newLine = "";
		String comment = "#" + line.split("#")[1];
		String[] args = line.split("#")[0].split(" ");
		for(String arg : args)
		{
			if(!arg.contains(":") || arg.contains("qid"))
			{
				newLine += arg + " ";
				continue;
			}
			
			String featNum = arg.split(":")[0];
			if(features.contains(featNum))
				newLine += arg + " ";
		}
		newLine = newLine.trim() + comment;
		return newLine;
	}
	
	public static HashMap<String, HashMap<String, Float>> parsePredictions(ArrayList<String> predictionsList, String dataDir) throws NumberFormatException, IOException
	{
		
		HashMap<String, HashMap<String, Float>> resultList = new HashMap<>();
		File file = new File(dataDir);
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		int i = 0;
		while ((line = bufferedReader.readLine()) != null) {
			String[] comment = line.split("#")[1].split(" ");
			String qid = comment[1].split("qid:")[1];
			if(!resultList.containsKey(qid)) resultList.put(qid, new HashMap<>());
			String docid = comment[2].split("docid:")[1];
			
			if (predictionsList.get(i).equals("nan"))
				System.out.println(line);
			Float score = Float.parseFloat(predictionsList.get(i));
			
			resultList.get(qid).put(docid, score);
			i++;
		}
		bufferedReader.close();
		return resultList;
	}
	
	public static HashMap<String, ArrayList<String>> parseInitial(String dataDir)
	{
		HashMap<String, ArrayList<String>> resultList = new HashMap<>();
		ArrayList<String> dataList = Utils.readLinesFromFile(dataDir);
		
		for(String line : dataList)
		{
			String[] comment = line.split("#")[1].split(" ");
			String qid = comment[1].split("qid:")[1];
			if(!resultList.containsKey(qid)) resultList.put(qid, new ArrayList<>());
			String docid = comment[2].split("docid:")[1];
			resultList.get(qid).add(docid);
		}
		return resultList;
	}
	
	public static HashMap<String, HashMap<String,Float>> minMaxNorm(HashMap<String, HashMap<String,Float>> resultList) {
		HashMap<String, HashMap<String,Float>> output = new HashMap<>();
		
		for(String qid : resultList.keySet()) {
			output.put(qid, new HashMap<>());
			float maxVal = Collections.max(resultList.get(qid).values());
			float minVal = Collections.min(resultList.get(qid).values());
			float denominator = maxVal - minVal;
			if(denominator == 0) {
				denominator = 1;
			}
			
			for(String docid : resultList.get(qid).keySet())  {
				float normScore = (resultList.get(qid).get(docid) - minVal) / denominator;
				output.get(qid).put(docid, normScore);
			}
		}
		return output;
	}
	
	public static HashMap<String, HashMap<String,Float>> mergeLists(HashMap<String, HashMap<String,Float>> result, 
			HashMap<String, HashMap<String,Float>> singleRes, float weight) {
		
		HashMap<String, HashMap<String,Float>> mergedList = new HashMap<>();
		for(String qid : result.keySet()) {
			mergedList.put(qid, new HashMap<>());
			for(String docid : result.get(qid).keySet()) {
				float score = weight * result.get(qid).get(docid) + (1-weight) * singleRes.get(qid).get(docid);
				mergedList.get(qid).put(docid, score);
			}
		}
		return mergedList;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		
		String trainDataDir = "svm_data/data/figures.w2v.train.svm.embed";
		String testDataDir = "svm_data/data/figures.w2v.train.svm.embed";
		
		String modelDir = "svm_data/sigir_models/segembed.model";
		//ArrayList<String> predictions = SVMUtils.predict(modelDir, trainDataDir);
		//System.out.println(predictions);
		
		//full text:20,   segments:21,22,23,24 other: "1","2","3","4","5","6","8","11","12","13","14","15","16","18"
		String[] f = new String[] {"21","22","23","24"};
		ArrayList<String> features = new ArrayList(Arrays.asList(f));
		
		learnRankLib(trainDataDir, modelDir, 6, features, "zscore", "ERR@100");
	}
}
