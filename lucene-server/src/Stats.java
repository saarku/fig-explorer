import java.util.ArrayList;
import org.apache.commons.math3.stat.inference.TTest;

public class Stats {
	
	public static void ttest(String dir1, String dir2)
	{
		ArrayList<String> list1 = Utils.readLinesFromFile(dir1);
		ArrayList<String> list2 = Utils.readLinesFromFile(dir2);
		ArrayList<Double> numbers1 = new ArrayList<>();
		ArrayList<Double> numbers2 = new ArrayList<>();
		
		for(int i = 0 ; i < 5; i++)
		{
			list1.remove(list1.size() - 1 - i);
			list2.remove(list2.size() - 1 - i);
		}
		
		for(int j =0; j < list1.size(); j++)
		{
			double rr1 = Double.valueOf(list1.get(j).split(" ")[1]);
			if(rr1 > 0)
				rr1 = 1.0 / rr1;
			numbers1.add(rr1);
			
			double rr2 = Double.valueOf(list2.get(j).split(" ")[1]);
			if(rr2 > 0)
				rr2 = 1.0 / rr2;
			numbers2.add(rr2);	
		}

		TTest t = new TTest();
		double pval = t.tTest(toArray(numbers1),toArray(numbers2));
		System.out.println(pval);
	}
	
	public static double[] toArray(ArrayList<Double> input)
	{
		int length = input.size();
		double[] output = new double[length];
		for(int i=0; i < length; i++)
		{
			output[i] = input.get(i);
		}
		return output;
	}
	
	public static void main(String[] args)
	{
		String baseline = "eval/figures.w2v.123456811121314151618.eval";
		String other = "eval/figures.w2v.111213.eval"; 
		Stats.ttest(baseline, other);
	}
}
