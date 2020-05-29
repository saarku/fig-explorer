import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;

public class SearchExperiment {
	
	private SearchEngine se;
	private boolean constrain;
	
	public SearchExperiment(HashMap<String,Float> searchFields, 
			boolean constrain, String modelDir, String featuresDir) 
					throws IOException
	{
		if(!modelDir.equals("none"))
			searchFields = getSearchFields(modelDir, featuresDir);
		System.out.println(searchFields);
		se = new SearchEngine(Utils.paramsMap.get("indexDir"), searchFields, "BM25");
		this.constrain = constrain;
	}
	
	public static HashMap<String,Float> getSearchFields(String modelDir, String featuresDir)
	{
		HashMap<String,Float> fields = new HashMap<>();
		HashMap<String,Float> modelWeights = new HashMap<>();
		ArrayList<String> modelLines = Utils.readLinesFromFile(modelDir);
		String modelLine = modelLines.get(modelLines.size()-1);
		String[] args = modelLine.split(" ");
		for(int i = 1 ; i < args.length - 1; i++)
		{
			String featNum = args[i].split(":")[0];
			Float featVal = Float.valueOf(args[i].split(":")[1]);
			modelWeights.put(featNum, featVal);
		}

		HashMap<String,String> featureNames = new HashMap<>();
		ArrayList<String> featureLines = Utils.readLinesFromFile(featuresDir);
		for(String line : featureLines)
		{
			String featNum = line.split(":")[0];
			String featName = line.split(":")[1];
			featureNames.put(featNum, featName);
		}
		
		for(String featNum : modelWeights.keySet())
		{
			fields.put(featureNames.get(featNum), modelWeights.get(featNum));
		}
		return fields;
	}
	
	public HashMap<String,Float> runSingleExperiment(String type) throws IOException
	{
		HashMap<String,Float> measures =  new HashMap<>();
		Float successAt1 = 0f;
		Float successAt3 = 0f;
		Float successAt5 = 0f;
		Float successAt10 = 0f;
		Float mrr = 0f;
		
		int figureRank = 0;
		int numQueries = 0;
		int numDocs = se.reader.maxDoc();
		for(int docId = 0; docId < numDocs; docId++)
		{
			Document HitDoc = se.searcher.doc(docId);
			String elementType = HitDoc.get(Utils.TYPE_FIELD);
			if(!type.equals(elementType))	continue;
			
			if(numQueries%1000 == 0) System.out.println(type + "," + numQueries);
			numQueries++;
			String captionText = HitDoc.get(Utils.CAPTION_FIELD);
			captionText = se.preprocessCaption(captionText);
			
			ScoreDoc[] results;
			if(constrain)
			{
				Query q = createConstrainedQuery(captionText, elementType, "or");
				TopDocs docs = se.searcher.search(q, 100);	
				results = docs.scoreDocs;
			}
			else
			{
				results = se.search(captionText, 100, "or");
			}
			
			figureRank = Evaluation.getFigureRank(results, docId);
			
			if(figureRank > 0)

			if(figureRank == 1)
				successAt1 += 1;
			if(figureRank <= 3)
				successAt3 += 1;
			if(figureRank <= 5)
				successAt5 += 1;
			if(figureRank <= 10)
				successAt10 += 1;
			if(figureRank > 0)
				mrr += 1f / (float) figureRank;

		}
		
		measures.put("successAt1", successAt1);
		measures.put("successAt3", successAt3);
		measures.put("successAt5", successAt5);
		measures.put("successAt10", successAt10);
		measures.put("mrr", mrr);
		measures.put("numQueries", (float)numQueries);
		return measures;
	}
	
	public void runSVMRetrieval(String idListDir, String outputDir, String elementType) throws IOException
	{
		HashMap<String, ArrayList<String>> result = new HashMap<>();
		ArrayList<String> idsList = Utils.readLinesFromFile(idListDir);
		for(String elementId : idsList)
		{
			ArrayList<String> singleResult = new ArrayList<>();
			String captionText = se.getQueryTextFromId(elementId, true, "figure");
			Query q = createConstrainedQuery(captionText, elementType, "or");
			TopDocs docs = se.searcher.search(q, 100);	
			ScoreDoc[] results = docs.scoreDocs;
			
			for(ScoreDoc sd : results)
			{
				Document d = se.searcher.doc(sd.doc);
				String currPaperId = d.get(Utils.PAPER_ID_FIELD);
				String currElementId = d.get(Utils.ID_FIELD);
				singleResult.add(currPaperId + "_" + currElementId); 
			}
			result.put(elementId, singleResult);
			Evaluation.evaluateInitial(result, outputDir);
		}
	}
	
	public void runMultipleExperiments() throws IOException
	{
		HashMap<String,Float> figuresResult = runSingleExperiment("figure");
		HashMap<String,Float> tablesResult = runSingleExperiment("table");
		
		Float numFigures = figuresResult.get("numQueries");
		System.out.println("number of figures: " + numFigures);
		figuresResult.remove("numQueries");
		Float numTables = tablesResult.get("numQueries");
		System.out.println("number of tables: " + numTables);
		tablesResult.remove("numQueries");
		printResult(figuresResult, tablesResult, numTables, numFigures);
		
	}
	
	public void printResult(HashMap<String,Float> figuresResult, HashMap<String,Float> tablesResult,
			Float numTables, Float numFigures)
	{
		String output = "";
		ArrayList<String> measures = new ArrayList<>(Arrays.asList(new String[]{"mrr", "successAt1",
				"successAt3", "successAt5", "successAt10"}));
		
		for(String measure : measures)
			output += "," + measure;
		output += "\nfigures";
	
		for(String measure : measures)
			output += "," + figuresResult.get(measure)/numFigures;
		output += "\ntables";
		
		for(String measure : measures)
			output += "," + tablesResult.get(measure)/numTables;
		output += "\noverall";
		
		for(String measure : measures)
			output += "," + (figuresResult.get(measure) + tablesResult.get(measure))/(numFigures + numTables);
		
		System.out.print(output);
	}
	
	public Query createConstrainedQuery(String captionText, String elementType, String operator) throws IOException
	{
		ArrayList<String> keywordsList = new ArrayList<String>(Arrays.asList(captionText.split(" ")));
		ArrayList<String> analyzedTerms = se.queryCreator.analyzeTerms(keywordsList);
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
		for(String term : analyzedTerms)
		{
			BooleanQuery.Builder termQueryBuilder = new BooleanQuery.Builder();
			for(String searchField : se.searchFields.keySet())
			{
				Term t = new Term(searchField, term);
				BoostQuery boostQuery = new BoostQuery(new TermQuery(t), se.searchFields.get(searchField));
				BooleanClause booleanClause;
				booleanClause = new BooleanClause(boostQuery, Occur.SHOULD);
				termQueryBuilder.add(booleanClause);
				
			}
			if (operator.equals("or"))
				booleanQueryBuilder.add(termQueryBuilder.build(), Occur.SHOULD);
			else
				booleanQueryBuilder.add(termQueryBuilder.build(), Occur.MUST);
		}
		
		// add the type of the element required (table or figure) to constraint the search
		Term t = new Term(Utils.TYPE_FIELD, elementType);
		BoostQuery boostQuery = new BoostQuery(new TermQuery(t), 1f);
		BooleanClause booleanClause = new BooleanClause(boostQuery, Occur.MUST);
		booleanQueryBuilder.add(booleanClause);
		return booleanQueryBuilder.build();
	}
	/*
	public void getAbstractSentences() throws IOException
	{
		ArrayList<String> fields = new ArrayList<>();
		fields.add(Utils.ABSTRACT_FIELD);
		fields.add(Utils.MENTION_FIELD + "50");
		int numDocs = se.reader.numDocs();
		ArrayList<String> outputLines = new ArrayList<>(); 
 		
		for(int docId = 0 ; docId < numDocs ; docId++)
		{
			Document hitDoc = se.searcher.doc(docId);
			String mention = hitDoc.get(Utils.MENTION_FIELD + "50");
			String abs = hitDoc.get(Utils.ABSTRACT_FIELD);
			String paperId = hitDoc.get(Utils.PAPER_ID_FIELD);
			String elementNum = hitDoc.get(Utils.ID_FIELD); 
			String elementType = hitDoc.get(Utils.TYPE_FIELD); 
			if(abs == null) continue;
			//ArrayList<String> sentences = Utils.getSentences(abs);
						
			double maxSim = 0;
			String maxSentence = "";
			for(String sentence : sentences)
			{
				HashMap<String,Float> senVec = se.getTfIdfVector(sentence, fields);
				HashMap<String,Float> menVec = se.getTfIdfVector(mention, fields);
				double sim = Utils.getCosine(senVec, menVec);
				if(sim > maxSim)
				{
					maxSim = sim;
					maxSentence = sentence;
				}
			}
			outputLines.add(paperId + "," + elementNum + "," + elementType + "," + 
					maxSentence + "," + maxSim);
		}
		Utils.writeLinesToFile(outputLines, "absSentences.txt");
	}*/
	
	public void splitTrainTestSets(Float testPortion, String type, String outputDir) throws IOException
	{
		
		int maxDoc = se.reader.maxDoc();
		ArrayList<String> elementsList = new ArrayList<>();
		for(int docId = 0; docId < maxDoc; docId++)
		{
			Document hitDoc = se.searcher.doc(docId);
			String elementType = hitDoc.get(Utils.TYPE_FIELD);
			String captionText = se.preprocessCaption(hitDoc.get(Utils.CAPTION_FIELD));
			int numTerms = captionText.split(" ").length;
			
			if(!elementType.equals(type) || captionText.equals("")) continue;
			if(numTerms > 5 || numTerms <= 1) continue;
			
			String elementPaper = hitDoc.get(Utils.PAPER_ID_FIELD);
			String elementId = hitDoc.get(Utils.ID_FIELD);
			elementsList.add(elementPaper + "_" + elementId);
		}
		Collections.shuffle(elementsList);
		Integer numElements = elementsList.size();
		Integer numTest = (int) (numElements * testPortion);
		
		ArrayList<String> testList = new ArrayList<>();
		ArrayList<String> trainList = new ArrayList<>();
		for(int i=0 ; i < numTest; i++)
			testList.add(elementsList.get(i));
		for(int i=numTest; i < numElements; i++)
			trainList.add(elementsList.get(i));
		Utils.writeLinesToFile(trainList, outputDir+".train");
		Utils.writeLinesToFile(testList, outputDir+".test");
	}
	
	public void generateSVMData(String IdsFileDir, String outputDir, String elementType, 
			ArrayList<String> rerankFields) throws IOException
	{
		ArrayList<String> featureLines = new ArrayList<>();
		ArrayList<String> idsList = Utils.readLinesFromFile(IdsFileDir);
		int queryId = 1;
		for(String elementId : idsList)
		{
			String[] args = elementId.split("_");			
			String captionText = se.getQueryTextFromId(elementId, true, elementType);	
			Query q = createConstrainedQuery(captionText, elementType, "or");
			TopDocs docs = se.searcher.search(q, 100);	
			ScoreDoc[] results = docs.scoreDocs;
			
			for(ScoreDoc res : results)
			{
				int docId = res.doc;
				int featureId = 1;
				String relLabel = "0";

				Document d = se.searcher.doc(res.doc);
				String currPaperId = d.get(Utils.PAPER_ID_FIELD);
				String currElementId = d.get(Utils.ID_FIELD);
				if(currPaperId.equals(args[0]) && currElementId.equals(args[1]))
					relLabel = "1";
				String featureLine = relLabel + " qid:" + queryId;
				
				for(String field : rerankFields)
				{
					Query fieldQuery = se.queryCreator.buildSingleFieldQuery(captionText, field);
					Explanation explain = se.searcher.explain(fieldQuery, docId);
					Float score = explain.getValue();
					featureLine += " " + featureId + ":" + score;
					featureId++;
				}
				
				HashMap<String, String> fields = se.getFieldsFromId(elementId, "figure");
				String mentionField = fields.get(Utils.MENTION_FIELD + "10");
				int numMentions = mentionField.split("\\.\\.\\.").length-2;
				featureLine += " " + featureId + ":" + numMentions;
				featureLine += "# qid:" + elementId + " docid:" + currPaperId + "_" + currElementId;
				featureLines.add(featureLine);
			}
			queryId++;
		}
		
		Utils.writeLinesToFile(featureLines, outputDir);
		ArrayList<String> featureMapLines = new ArrayList<>();
		int fid = 1;
		for(String f : rerankFields)
		{
			featureMapLines.add(fid + ":" + f);
			fid++;
		}
		featureMapLines.add(fid + ":mentions");
		
		Utils.writeLinesToFile(featureMapLines, outputDir+".features");
	}
	
	public void evaluateSVM(String modelDir, String dataDir, String outputDir, 
			ArrayList<String> features, String modelType) 
					throws InterruptedException, IOException
	{
		System.out.println("getting prediction for test data..");
		ArrayList<String> predictionsList;
		if(modelType.equals("svm"))
			predictionsList = LTRUtils.predictSVM(modelDir, dataDir, features);
		else
			predictionsList = LTRUtils.predictRankLib(modelDir, dataDir, "zscore");
			
		System.out.println("parsing predictions..");
		HashMap<String, HashMap<String,Float>> result = LTRUtils.parsePredictions(predictionsList, dataDir);
		//Evaluation.evaluate(result, outputDir);
		Evaluation.evaluateRelated(result, outputDir);
		System.out.println("done.");
	}
	
	public void RerankSingle(String featNum, String dataDir, String outputDir) 
					throws InterruptedException, IOException
	{

		ArrayList<String> predictionsList = LTRUtils.getSingleFeatPredictions(dataDir, featNum);
		HashMap<String, HashMap<String,Float>> result = LTRUtils.parsePredictions(predictionsList, dataDir);
		Evaluation.evaluateRelated(result, outputDir);
	}
	
	public void evaluateSVMinitial(String dataDir, String outputDir) throws IOException, InterruptedException
	{
		HashMap<String, ArrayList<String>> initialResult = LTRUtils.parseInitial(dataDir);
		Evaluation.evaluateInitial(initialResult, outputDir);
	}
	
	public void generateW2VData(String outputDir) throws IOException
	{
		int numDocs = se.reader.maxDoc();
		ArrayList<String> papers = new ArrayList<>();
		ArrayList<String> outputLines = new ArrayList<>();
		
		for(int docId = 0; docId < numDocs; docId++)
		{
			Document hitDoc = se.searcher.doc(docId);
			String paperId = hitDoc.get(Utils.PAPER_ID_FIELD);
			if(!papers.contains(paperId))
			{
				papers.add(paperId);
				String fullText = hitDoc.get(Utils.TEXT_FIELD);
				if(fullText == null) continue;
				ArrayList<String> terms = se.queryCreator.analyzeText(fullText);
				String line = "";
				for(String t : terms)
					line += t + " ";
				outputLines.add(line);
			}
		}
		Utils.writeLinesToFile(outputLines, outputDir);
	}
	
	public void getQueryLength(String IdsFileDir, String outputDir) throws IOException
	{
		ArrayList<String> outputLines = new ArrayList<>();
		ArrayList<String> idsList = Utils.readLinesFromFile(IdsFileDir);
		
		for(String elementId : idsList)
		{
			String[] args = elementId.split("_");
			String captionText = se.getQueryTextFromId(elementId, true, "figure");	
			int numTerms = captionText.split(" ").length;
			outputLines.add(elementId + " " + numTerms);
		}
		Utils.writeLinesToFile(outputLines, outputDir);
	}
	
	public void getQueryText(String IdsFileDir, String outputDir) throws IOException
	{
		ArrayList<String> outputLines = new ArrayList<>();
		ArrayList<String> idsList = Utils.readLinesFromFile(IdsFileDir);
		
		for(String elementId : idsList)
		{
			String[] args = elementId.split("_");
			String captionText = se.getQueryTextFromId(elementId, true, "figure");	
			String captionTextRaw = se.getQueryTextFromId(elementId, false, "figure");	
			outputLines.add(elementId + "," + captionText + "," + captionTextRaw);
		}
		Utils.writeLinesToFile(outputLines, outputDir);
	}
	
	public void getNumMentions(String IdsFileDir, String outputDir) throws IOException
	{
		ArrayList<String> outputLines = new ArrayList<>();
		ArrayList<String> idsList = Utils.readLinesFromFile(IdsFileDir);
		
		for(String elementId : idsList)
		{
			HashMap<String, String> fields = se.getFieldsFromId(elementId, "figure");
			String mentionField = fields.get(Utils.MENTION_FIELD + "10");
			
			int numMentions = mentionField.split("\\.\\.\\.").length-2;
			outputLines.add(elementId + " " + numMentions);
		}
		Utils.writeLinesToFile(outputLines, outputDir);
	}
	
	public void getLDAdata(String trainIdsDir, String testIdsDir, String outputDir) throws IOException
	{
		ArrayList<String> outputLines = new ArrayList<>();
		ArrayList<String> trainIdsList = Utils.readLinesFromFile(trainIdsDir);
		ArrayList<String> testIdsList = Utils.readLinesFromFile(testIdsDir);
		
		for(String elementId : trainIdsList)
		{
			String captionText = se.getQueryTextFromId(elementId, true, "figure");		
			outputLines.add(captionText);
		}
		
		for(String elementId : testIdsList)
		{
			String captionText = se.getQueryTextFromId(elementId, true, "figure");		
			outputLines.add(captionText);
		}
		Utils.writeLinesToFile(outputLines, outputDir);
	}
	
	public HashMap<String,Float> runTestExperiment(String queriesDir) throws IOException
	{
		HashMap<String,Float> measures =  new HashMap<>();
		Float successAt1 = 0f;
		Float successAt3 = 0f;
		Float successAt5 = 0f;
		Float successAt10 = 0f;
		Float mrr = 0f;
		int figureRank = 0;
		int numQueries = 0;
		
		ArrayList<String> queries = Utils.readLinesFromFile(queriesDir);
		for(String queryLine: queries)
		{
			String[] args = queryLine.split("\t");
			String text = args[1];
			String figId = args[0];
			
			Query q = createConstrainedQuery(text, Utils.FIGURE_TYPE, "or");
			TopDocs docs = se.searcher.search(q, 100);	
			ScoreDoc[] results = docs.scoreDocs;
			
			//ScoreDoc[] results = se.search(text, 100, "or");
			numQueries++;
			
			figureRank = Evaluation.getFigureRankId(results, figId, se);
			//System.out.println(figureRank);
			if(figureRank > 0)
			{
				if(figureRank == 1)
					successAt1 += 1;
				if(figureRank <= 3)
					successAt3 += 1;
				if(figureRank <= 5)
					successAt5 += 1;
				if(figureRank <= 10)
					successAt10 += 1;
				if(figureRank > 0)
					mrr += 1f / (float) figureRank;
			}
		}
		
		measures.put("successAt1", successAt1 / numQueries);
		measures.put("successAt3", successAt3 / numQueries);
		measures.put("successAt5", successAt5 / numQueries);
		measures.put("successAt10", successAt10 / numQueries);
		measures.put("mrr", mrr / numQueries);
		measures.put("numQueries", (float)numQueries);
		return measures;
	}
	
	public void featureReRank(String dataDir, String modelDir, String featureNum, String outputDir) throws InterruptedException, IOException {
		ArrayList<String> predictionsList = LTRUtils.predictRankLib(modelDir, dataDir, "zscore");
		HashMap<String, HashMap<String,Float>> result = LTRUtils.parsePredictions(predictionsList, dataDir);
		result = LTRUtils.minMaxNorm(result);
		
		ArrayList<String> singlePredList = LTRUtils.getSingleFeatPredictions(dataDir, featureNum);
		HashMap<String, HashMap<String,Float>> singleRes = LTRUtils.parsePredictions(singlePredList, dataDir);
		singleRes = LTRUtils.minMaxNorm(singleRes);
		
		float[] vals = new float[] {0.2f,0.3f,0.4f,0.5f,0.6f,0.7f,0.8f,0.9f};
		for(float a : vals) {
			System.out.println(a);
			HashMap<String, HashMap<String,Float>> mergedList = LTRUtils.mergeLists(result, singleRes, a);
			Evaluation.evaluateRelated(mergedList, outputDir);
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		boolean constrain = false;
		boolean experimentFlag = false;
		boolean abstractFlag = false;
		boolean splitFlag = false;
		boolean dataFlag = false;
		boolean initEvalFlag = false;
		boolean svmLearnFlag = false;
		boolean svmRerankFlag = false;
		boolean svmRetrievalFlag = false;
		boolean analysisFlag = false;
		boolean featureRerankFlag = true;
		
		HashMap<String,Float> searchFields = new HashMap<>();
		searchFields.put(Utils.MENTION_FIELD + "50", 1f);
		//searchFields.put(Utils.MENTION_FIELD + "20", 0.048170377f);
		//searchFields.put(Utils.MENTION_FIELD + "10", 0.044926483f);
		//searchFields.put(Utils.ABSTRACT_FIELD, 0.1f);
		//searchFields.put("absSentence", 0.2f);
		//searchFields.put("intro", 0.1f);
		//searchFields.put("conclusion", 0.1f);
		//searchFields.put("title", 0.1f);
		//searchFields.put("text", 0.1f);
		
		ArrayList<String> rerankFields = new ArrayList<>();
		rerankFields.add(Utils.MENTION_FIELD + "50");
		rerankFields.add(Utils.MENTION_FIELD + "20");
		rerankFields.add(Utils.MENTION_FIELD + "10");
		rerankFields.add("absSentence");
		rerankFields.add(Utils.ABSTRACT_FIELD);
		rerankFields.add("title");
		rerankFields.add("conclusion");
		rerankFields.add("intro");
		rerankFields.add("text");
		
		ArrayList<String> features = 
				new ArrayList<>(Arrays.asList(new String[] 
						{"1","2","3","4","5","6","8","9","11","12","13","14","15","16","18","19"}));
		
		String modelName = "";
		for(String f : features)
			modelName += f;
		
		SearchExperiment sExp = new SearchExperiment(searchFields, constrain, 
				"none", 
				"svm_data/features/figures.test.svm.features");

		if(experimentFlag)
		{
			sExp.runMultipleExperiments();
		}
		if(abstractFlag)
		{
			//sExp.getAbstractSentences();
		}
		if(splitFlag)
		{
			sExp.splitTrainTestSets(0.5f, "figure", "figures");
		}
		if(dataFlag)
		{
			System.out.println("test");
			sExp.generateSVMData("figures.test", "figures.test.svm", "figure", rerankFields);	
		}
		if(initEvalFlag)
		{
			sExp.evaluateSVMinitial("svm_data/data/figures.test.svm", "eval/figures.2.eval");
		}
		if(svmLearnFlag)
		{
			//LTRUtils.learnSVMModel("svm_data/data/figures.w2v.train.svm", "svm_data/models/figures.w2v." + modelName + ".model", 0.01f, 0, numSamples, features);
			LTRUtils.learnRankLib("svm_data/data/figures.w2v.train.svm", 
					"svm_data/models/lambda_mart/figures.w2v." + modelName + ".model", 
					6, features, "zscore", "ERR@100");
		}
		if(svmRerankFlag)
		{
			sExp.evaluateSVM("svm_data/sigir_models/baseline.segembed.model", 
					"svm_data/data/figures.w2v.test.svm.embed", 
					"sigir_eval/fullembed.segembed.eval", features, "ranklib");
		}
		if(svmRetrievalFlag)
		{
			sExp.runSVMRetrieval("figures.test", "eval/figures." + modelName + ".eval", "figure");
		}
		if(analysisFlag)
		{
			sExp.RerankSingle("21", "svm_data/data/figures.w2v.test.svm.embed", "sigir_eval/mean.eval");
			//sExp.getNumMentions("figures.test", "figures.test.mentions");
			//sExp.getQueryText("figures.train", "figures.terms.train");
			//sExp.getLDAdata("figures.train", "figures.test", "lda.dat");
			//sExp.getQueryLength("figures.test", "figures.length.test");
			//HashMap<String, Float> results = sExp.runTestExperiment("/Users/saarkuzi/acl_data/camera_ready_data/queries.train.txt.temp");
			//System.out.println(results);
		}
		if(featureRerankFlag)
		{
			sExp.featureReRank("svm_data/data/figures.w2v.test.svm.embed", "svm_data/sigir_models/baseline.model", "23", "sigir_eval/baseline.rerank.segembedmax.eval");
		}
	}
}
