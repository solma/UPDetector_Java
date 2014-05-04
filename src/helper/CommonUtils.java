package helper;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

import org.apache.commons.math3.analysis.solvers.NewtonRaphsonSolver;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import com.google.common.primitives.Doubles;


public class CommonUtils {
	public static void main(String[] args){

		
		Calendar c = Calendar.getInstance(); 
		System.out.println(
				c.get(Calendar.YEAR)+"-"+c.get(Calendar.MONTH)+"-"+c.get(Calendar.DAY_OF_MONTH)
				+" "+c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND)+":"+c.get(Calendar.MILLISECOND));
		
		System.out.println(DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()));
		
		//new Date()
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd kk:mm:ss:SSS", Locale.US);
		//sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		System.out.println(sdf.format(Calendar.getInstance().getTime()));
		
		try {
			System.out.println(new SimpleDateFormat("hh:mm:ss", Locale.US).parse("02:11:30"));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		System.out.println(calculatePDFOfNormalDistribution(0.06, 0.08, -0.001));
		
		System.out.println(new NormalDistribution(0.06, 0.08).density(-0.001));
		
		System.out.println(HMSToSeconds("16:10:30"));
		//System.out.println(secondsToStringTime(65181));
		//System.out.println(getDirectory("E:/workspace/android/ActivityRecognition/logs/weka/input/features2013_08_295.arff"));
		//checkAccelerometerTrainingFile();
		
		//System.out.println(String.format("%-6.3f%-6.3f", 3.31, -1.12));
		
		//System.out.println(new Date(12363992261L));
		
		/*String groundTruth="10:35:20~10:36:47 10:38:00~10:39:35 10:41:08~10:42:35 10:44:22~10:45:50 10:47:35~10:48:46";
		for(String duration: groundTruth.split(" ")){
			for(String timestamp: duration.split("~")){
				System.out.println(HMSToSeconds(timestamp));
				
			}
		}
		
		groundTruth="41893 49475 49476 49568 49653 49761";
		for(String timestamp: groundTruth.split(" ")){
			System.out.println(secondsToHMS(Integer.parseInt((timestamp)) ));			
		}
		System.out.println(secondsToHMS(67442));*/
		
		//System.out.println(calPDFOfNormalDistribution(0.1, 0.4, 0.7));
		
		//Double[][]lists=new Double[][]{{-3.162, -0.049, 0.120 }, { -1.387,  0.327,  4.264}};
		
		//System.out.println(calculatePearsonCorrelation(Arrays.asList(lists[0]), Arrays.asList(lists[1])));
		//System.out.println(calculateCosineSimilarity(Arrays.asList(lists[0]), Arrays.asList(lists[1])));
	}
	
	public static double[] maxAndMin(double[] values){
		if(values.length==0) return null; 
		double max=values[0],min=values[0];
		for(int i=1;i<values.length;i++){
			max=Math.max(max, values[i]);
			min=Math.min(min, values[i]);
		}
		return new double[]{min, max};
	}

	
	public static double max(double[] values){
		if(values.length==0) return Integer.MIN_VALUE; 
		double max=values[0];
		for(int i=1;i<values.length;i++){
			max=Math.max(max, values[i]);
		}
		return max;
	}
	
	public static double min(double[] values){
		if(values.length==0) return Integer.MIN_VALUE; 
		double min=values[0];
		for(int i=1;i<values.length;i++){
			min=Math.min(min, values[i]);
		}
		return min;
	}
	
	public static double[] calculateHistogram(List<Double> values, double[] normalizedInterval, int noOfBins){		
		double[] doubles=new double[values.size()];
		for(int i=0;i<values.size();i++) doubles[i]=values.get(i);
		return calculateHistogram(doubles, normalizedInterval, noOfBins);
	}
	
	public static double[] calculateHistogram(double[] values, double[] normalizedInterval, int noOfBins){		
		if(values.length<1) return null;
		if(noOfBins<2) return new double[]{1};
		
		double min=normalizedInterval[0], max=normalizedInterval[1];
		double[] probs=new double[noOfBins];
		for(int i=0;i<values.length;i++){
			probs[(int)((values[i]-min)*noOfBins/(max-min))]+=1;
		}
		for(int i=0;i<probs.length;i++){
			probs[i]/=values.length;
		}
		return probs;
	}
	
	public static double calculatePearsonCorrelation(List<Double> list1, List<Double> list2){
		if(list1.size()!=list2.size()){
			System.err.println("Two lists must have the same dimensionality.");
			return 0;
		}
		double mean1=calculateMean(list1);
		double mean2=calculateMean(list2);
		
		double std1=Math.sqrt(calculateVariance(list1, mean1));
		double std2=Math.sqrt(calculateVariance(list2, mean2));
		
		double dividend=0;
		for(int i=0;i<list1.size();i++){
			dividend+=(list1.get(i)-mean1)*(list2.get(i)-mean2);
		}
		dividend/=list1.size()-1;
			
		//System.out.println(mean1+" "+std1+" "+mean2+" "+std2+" "+dividend);
		return dividend/(std1*std2);
	}
	
	public static double calculateCosineSimilarity(List<Double> list1, List<Double> list2){
		if(list1.size()!=list2.size()){
			System.err.println("Two lists must have the same dimensionality.");
			return 0;
		}
		double dividend=0, divisor1=0, divisor2=0;
	
		for(int i=0;i<list1.size();i++){
			dividend+=list1.get(i)*list2.get(i);
			divisor1+=Math.pow(list1.get(i),2);
			divisor2+=Math.pow(list2.get(i),2);
		}
		//System.out.println(dividend+" "+divisor1+" "+divisor2);
		return dividend/(Math.sqrt(divisor1)*Math.sqrt(divisor2));		
	}
	
	public static double calculatePDFOfNormalDistribution(double mean, double std, double value){
		double prob=Math.pow(Math.E, -Math.pow(value-mean, 2)/2/Math.pow(std, 2))/(Math.sqrt(Math.PI*2)*std);
		if(prob>1) System.out.println(mean+" "+std+" "+value);
		return prob;
	}
	
	public static double calculateMean(List<Double> list){
		if(list==null||list.size()==0) return Double.MAX_VALUE;
		double sum=0;		
		for(double num: list) sum+=num;
		return sum/list.size();		
	}
	
	public static double calculateVariance(List<Double> list, double mean){
		if(mean==Double.MAX_VALUE) return mean;
		if(list.size()==1) return 0;
		double sum=0;
		for(double num: list){
			sum+=Math.pow(num-mean, 2);
		}
		return sum/(list.size()-1);
	}
	
	public static double[] doubleListToDoubleArray(List<Double> values){
		if(values==null) return null;
		double[] ret=new double[values.size()];
		for(int i=0; i<ret.length;i++) ret[i]=values.get(i);
		return ret;
	}
	
	public static String getFileName(String path){
		int idx = path.lastIndexOf("/");
		return idx >= 0 ? path.substring(idx + 1) : path;
	}
	
	public static String getDirectory(String path){
		int idx = path.lastIndexOf("/");
		return idx >= 0 ? path.substring(0,idx + 1) : path;
	}
	
	public static int HMSToSeconds(String time){
		String[] fields=time.split(":");
		int secs=Integer.parseInt(fields[0])*3600+Integer.parseInt(fields[1])*60;
		if (fields.length>2) secs+=Integer.parseInt(fields[2]);
		return secs;
	}
	
	public static long HMSSToMillseconds(String time){
		String[] fields=time.split(":");
		long millisecs=(Integer.parseInt(fields[0])*3600+Integer.parseInt(fields[1])*60)*1000;
		if (fields.length>2) millisecs+=Integer.parseInt(fields[2])*1000;	
		if (fields.length>3) millisecs+=Integer.parseInt(fields[3]);
		return millisecs;
	}
	
	

	
	
	public static String secondsToHMS(int secs){
		StringBuilder  sb=new StringBuilder();
		int[] hourMinSec=new int[3];
		hourMinSec[0]=secs/3600;
		hourMinSec[1]=(secs-hourMinSec[0]*3600)/60;
		hourMinSec[2]=secs-hourMinSec[0]*3600-hourMinSec[1]*60;
		for(int i=0;i<hourMinSec.length;i++){			
			//if(i==hourMinSec.length-1&&hourMinSec[i]==0) continue;
			if(i>0) sb.append(":");
			sb.append(hourMinSec[i]>=10?hourMinSec[i]:("0"+hourMinSec[i]));

		}
		return sb.toString();
	}
	
	
	/*
	 * @start: index of the starting field (inclusive)
	 * @end: index of the ending field (exclusive)
	 */
	public static String join(String[] fields, int start, int end, String connectingDelimeter){
		try{
			StringBuilder sb=new StringBuilder();
			for(int i=start;i<end;i++){
				if(i!=start) sb.append(connectingDelimeter);
				sb.append(fields[i]);
			}
			return sb.toString();
		}catch(Exception ex){
			ex.printStackTrace();
			return null;
		}
	}
	
	
	
	/*
	 * return a substring consisting of fields of the given string at the given
	 * indices  
	 * @start: index of the starting field (inclusive)
	 * @end: index of the ending field (exclusive)
	 */
	public static String cutField(String s, String delimeter, int start, int end, String connectingDelimeter){
		String[] fields=s.split(delimeter);
		return join(fields, start, end, connectingDelimeter);
	}
	
	public static String cutField(String s, String delimeter, int start){
		return cutField(s, delimeter, start, s.split(delimeter).length, " ");
	}
	
	
	/*
	 * A snippet to help automatically minimally-label a new feature file using the old labeled feature file
	 */
	public static void automaticConvert(String date) {
		try{
			String line;
			String[] fields;
			
			HashMap<String, String> eventsTimestamps=new HashMap<String, String>();
			//read the old feature file to record the timestamps
			String oldFeatureLogFile=Constants.ACCELEROMETER_BASE_DIR+"features"+date+".log";
			Scanner sc=new Scanner(new File(oldFeatureLogFile));
			while(sc.hasNextLine()){
				line=sc.nextLine().trim();
				fields=line.split(" ");
				if(fields.length==6){ //labeled line in the old feature file has 7 fields
					eventsTimestamps.put(fields[0], fields[5]);
				}
			}
			sc.close();
			
			//update the new file, save the new files under model folder temporally
			String newFeatureLogFile=Constants.ACCELEROMETER_CIV_FEATURES_DIR+"features"+date+".log";
			sc=new Scanner(new File(newFeatureLogFile));
			
			FileWriter fw=new FileWriter(Constants.ACCELEROMETER_CIV_FEATURES_DIR+"model/features"+date+".log");
			while(sc.hasNextLine()){
				line=sc.nextLine().trim();
				fields=line.split(" ");
				
				if(eventsTimestamps.containsKey(fields[0])){
					fw.write(line+" "+eventsTimestamps.get(fields[0])+"\n");
				}else fw.write(line+"\n");
			}
			sc.close();
			fw.close();
			
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
	
	}
	
	public static double[] intToDoubleArray(int[] arr){
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double)arr[i];
		}
		return ret;
	}
	
	public static float[] doubleToFloatArray(double[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (float)arr[i];
		}
		return ret;
	}

	public static double[] floatToDoubleArray(float[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double)arr[i];
		}
		return ret;
	}
	
	/**
	 * 
	 * @param filename: the name of the accelerometer training file
	 */
	public static void checkAccelerometerTrainingFile(){
		try{
			String trainFileName=Constants.ACCELEROMETER_CIV_FEATURES_DIR+"/train/combined.arff";
			Scanner sc=new Scanner(new File(trainFileName));
			String line;
			String[] fields;
			int lineNo=0;
			
			Variance var=new Variance();//obj to calculate var
			Mean mean=new Mean();//obj to calculate mean
			
			
			//first pass to get the stat
			ArrayList<ArrayList<ArrayList<Double>>> nums=new ArrayList<ArrayList<ArrayList<Double>>>();
			
			String[] labelClasses={"p", "u", "n"};
			ArrayList<ArrayList<Double>> avgs=new ArrayList<ArrayList<Double>>();
			for(int i=0;i<labelClasses.length;i++) avgs.add(new ArrayList<Double>());
			ArrayList<ArrayList<Double>> vars=new ArrayList<ArrayList<Double>>();
			for(int i=0;i<labelClasses.length;i++) vars.add(new ArrayList<Double>());

			int noOfNumericFields=9;
			for(int j=0;j<labelClasses.length;j++){
				nums.add(new ArrayList<ArrayList<Double>>());
				for(int i=0;i<noOfNumericFields;i++){
					nums.get(j).add(new ArrayList<Double>());
				}
			}
			while(sc.hasNextLine()){
				line=sc.nextLine();
				lineNo+=1;
				if(line.contains("@")) continue;
				fields=line.split(",");
				int n=fields.length;
				for(int j=0;j<labelClasses.length;j++){
					if( fields[n-1].equals(labelClasses[j]) ){
						for(int i=0;i<n-2;i++){
							nums.get(j).get(i).add(Double.parseDouble(fields[i+1]));
						}
						break;
					}					
				}
				
			}
			
			System.out.println(nums.size()+" "+nums.get(0).size());
			double avg, vari;
			for(int j=0;j<labelClasses.length;j++){
				for(int i=0;i<noOfNumericFields;i++){
					avg=mean.evaluate(Doubles.toArray(nums.get(j).get(i)));
					avgs.get(j).add( avg);
					vari=var.evaluate(Doubles.toArray(nums.get(j).get(i)) );
					vars.get(j).add( vari);
				}
			}
			//print the stats
			System.out.println();
			for(int j=0;j<labelClasses.length;j++){
				System.out.println(labelClasses[j]);
				System.out.println(avgs.get(j));
				System.out.println(vars.get(j));
			}
			System.out.println();
			
			// second pass 
			sc=new Scanner(new File(trainFileName));
			while(sc.hasNextLine()){
				line=sc.nextLine();
				lineNo+=1;
				if(line.contains("@")) continue;
				fields=line.split(",");
				//abnormal "p" or "u" lines or "n" lines
				for(int j=0;j<labelClasses.length;j++){
					if(fields[fields.length-1].equals(labelClasses[j]) ){
						int firstAbnormalField=isAbnormal(fields, vars, avgs, j);
						if(firstAbnormalField>0 && !labelClasses[j].equals("n")){
							System.out.println(lineNo+" "+firstAbnormalField+" "+line);
						}
						break;
					}
				}
				
			}

		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param fields
	 * @param vars
	 * @param avgs
	 * @param j
	 * @return the first field that out of the 3\delta range
	 */
	private static int  isAbnormal(String [] fields, ArrayList<ArrayList<Double>> vars, ArrayList<ArrayList<Double>> avgs, int j){
		int[] indices={2,3, 5, 6, 7 ,8};
		for(int i=0;i<indices.length;i++){
			if(Math.abs(Double.parseDouble(fields[indices[i]])-avgs.get(j).get(indices[i]-1))>=3*Math.sqrt(vars.get(j).get(indices[i]-1)) ){
				return i;
			}
		}
		//System.out.println(isAbnormal);
		return -1;
	}
	
}
