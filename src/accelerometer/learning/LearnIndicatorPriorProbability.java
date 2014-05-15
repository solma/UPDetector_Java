package accelerometer.learning;

import helper.CommonUtils;
import helper.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 * Learn the prior probability for CIV and MST vector
 * @author Shuo
 */

public class LearnIndicatorPriorProbability {
	/**
	 * 
	 * conditional probability folders of vectors for different indicators under different outcomes
	 */
	public static String MST_FOLDER=Constants.ACCELEROMETER_MOTION_STATE_DIR+"/ConditionalProbability/";
	public static String CIV_FOLDER=Constants.ACCELEROMETER_CIV_FEATURES_DIR+"/"+Constants.PHONE_POSITIONS[Constants.PHONE_POSITION_ALL]+"/";
	public static String IODOOR_FOLDER=Constants.ENVIRONMENT_BASE_DIR;
	public static String ENGINE_START_FOLDER=Constants.AUDIO_BASE_DIR+"conditional_probability/engine_start/";
	
	
	public static void main(String[] args) {

	}
	
	
	public static ArrayList<String> labelMSTVectors(ArrayList<String> vectors,HashMap<String, ArrayList<ArrayList<String>> >  groundTruth,
			String date){
		System.out.println(date);
		//System.out.println(groundTruth.keySet());
		ArrayList<ArrayList<String>> groundTruthsForTheDay=groundTruth.get(date);
				
 		 int idxOfNextUnparkingEvent=0,idxOfNextParkingEvent=0;
 		 boolean nextGTEventiSUnparking=true; //true:unparking false: parking
 		 //next GT event is unparking
 		 int timeOfNextGTEvent=
		 CommonUtils.HMSToSeconds(groundTruthsForTheDay.get(0).get(idxOfNextUnparkingEvent).split("-")[1]);
		 
 		 ArrayList<String> labeledVectors=new ArrayList<String>();
 		  
		for(String vector: vectors){
			String labeledVector=vector;

			int secondsOfTheDay=CommonUtils.HMSToSeconds(vector.split(",")[0]);
			if(secondsOfTheDay>=timeOfNextGTEvent){
				if(nextGTEventiSUnparking){
					labeledVector+=",u";
					nextGTEventiSUnparking=false;
					idxOfNextUnparkingEvent+=1;
					if(idxOfNextParkingEvent<groundTruthsForTheDay.get(1).size())
						timeOfNextGTEvent=CommonUtils.HMSToSeconds(groundTruthsForTheDay.get(1).get(idxOfNextParkingEvent).split("-")[1]);
					else 
						timeOfNextGTEvent=Integer.MAX_VALUE;					
				}
				else{
					labeledVector+=",p";
					nextGTEventiSUnparking=true;
					idxOfNextParkingEvent+=1;
					if(idxOfNextUnparkingEvent<groundTruthsForTheDay.get(0).size())
						timeOfNextGTEvent=CommonUtils.HMSToSeconds(groundTruthsForTheDay.get(0).get(idxOfNextUnparkingEvent).split("-")[1]);
					else 
						timeOfNextGTEvent=Integer.MAX_VALUE;	
				}
			}else{
				labeledVector+=",n";
			}
			
			labeledVectors.add(labeledVector);
		}
		
		return labeledVectors;
		
	}
	
	/*ArrayList<String> labeledVectorsOfThisFile;
	
	//option 1: label vectors of the MST indicator, i.e. indicating which vector is generated for parking/unparking 
	//labeledVectorsOfThisFile=labelMSTVectors(vectors, EventDetection.groundTruth, inputFileNameInDateFormat);
	
	//option 2: for test purpose, simply labeling all MST vectors as "n" 
	labeledVectorsOfThisFile=new ArrayList<String>();
	for(String v:mstVectors){
		labeledVectorsOfThisFile.add(v+",n");
	}
	

	//output labeled vectors to a file
	labeldVectors.addAll(labeledVectorsOfThisFile);
	try{
		String vectorOfMSTIndicator=Constants.ACCELEROMETER_FUSION_DIR+dateSeq+"_MST.log";
		FileWriter fw=new FileWriter(vectorOfMSTIndicator);
		for(String lv:labeledVectorsOfThisFile){
			fw.write(lv+"\n");
		}
		System.out.println("vectors of MST indicator written to "+vectorOfMSTIndicator);
		fw.close();
	}catch(Exception ex){
		ex.printStackTrace();
	}
	
	
	//if(true) return;
	
	//output to the label vectors in order to learn conditional probability for MST indicator
	try{
		String condfolder=Constants.ACCELEROMETER_STATE_FEATURES_DIR+"/ConditionalProbability/";
		FileWriter parking=new FileWriter(condfolder+"parking.txt");
		FileWriter unparking=new FileWriter(condfolder+"unparking.txt");
		FileWriter none=new FileWriter(condfolder+"none.txt");
		for(String labeledVector: labeldVectors){
			String[] fields=labeledVector.split(",");
			String vec=CommonUtils.cutField(labeledVector, ",", 1, fields.length-1, ",")+"\n";
			switch (fields[fields.length-1]) {
			case "u":
				unparking.write(vec);
				break;
			case "p":
				parking.write(vec);
				break;
			default:
				none.write(vec);
				break;
			}			
		}
		parking.close();
		unparking.close();
		none.close();
	}catch(Exception ex){
		ex.printStackTrace();
	}*/
	
	
	//System.out.println(detectedEvents);
	
	

	/**
	 * read from files and compute the histogram distribution
	 */
	public static void calculateAverageAndStdForCIVandMSTVector(String folder){
		Scanner sc;
		
		String[] filenames={
			"none.txt",
			"parking.txt",
			"unparking.txt"
		};
		for(int i=0;i<filenames.length;i++){
			filenames[i]=folder+filenames[i];
		}
		HashMap<Integer, FeatureGroup> featureGroups=new HashMap<Integer, FeatureGroup>();
		
		try{			
			for(String filepath: filenames){
				sc=new Scanner(new File(filepath));
				ArrayList<ArrayList<Double>> features=new ArrayList<ArrayList<Double>>();
				String line;
				String[] lineFields;
				while(sc.hasNextLine()){
					line=sc.nextLine();
					lineFields=line.split(",");
					if(features.size()==0){//read the first line to determine the # of features
						for(int i=0;i<lineFields.length;i++){
							features.add(new ArrayList<Double>());
							if(!featureGroups.containsKey(i)){
								featureGroups.put(i, new FeatureGroup(i));
							}
						}
					}
					for(int i=0;i<lineFields.length;i++){
						double value=Double.parseDouble(lineFields[i].trim());
						features.get(i).add(value);
					}
				}
				sc.close();
				
				String fileNameWithExt=CommonUtils.getFileName(filepath);
				String fileNameWOExt=fileNameWithExt.substring(0, fileNameWithExt.indexOf('.'));
				System.out.print(String.format("%13s",fileNameWOExt+":  "));
				
				for(int i=0;i<features.size();i++){
					ArrayList<Double> values=features.get(i);
					//System.out.println(fileNameWOExt+" "+scalarGroups.get(i).values.size());
					featureGroups.get(i).values.add(values);
					
					/**
					 * Histogram approach
					 */
					/*double[] normalizedInterval=CommonUtils.maxAndMin(CommonUtils.doubleListToDoubleArray(values));
					normalizedInterval[1]+=0.0001;					
					double[] distr=CommonUtils.calculateHistogram(values,normalizedInterval, 5);
					if(i>0) System.out.print(",");
					System.out.print("	{");
						*//**
						 * Print out bin probabilities
						 *//*
						for(int j=0;j<distr.length;j++){
							if(j>0) System.out.print(",");
							System.out.print(String.format("%.2f", distr[j]));
						}					
						*//**
						 * print out the observed max and min
						 *//*
						for(int j=0;j<normalizedInterval.length;j++){
							if(j>0) System.out.print(",");
							System.out.print(String.format("%.3f ",normalizedInterval[j]));
						}
					System.out.print("}");*/
					
					/**
					 * Normal Distribution approach
					 */
					double mean=CommonUtils.calculateMean(values);		
					double std=Math.sqrt(CommonUtils.calculateVariance(values, mean));
					System.out.print("{"+String.format("%.3f", mean)+", "+String.format("%.3f},\t", std ));
				
				}
				System.out.println();
				
				/**
				 * Correlation between features
				 */
				PearsonsCorrelation pc=new PearsonsCorrelation();
				for(int i=0;i<features.size();i++){
					for(int j=i+1;j<features.size();j++){
						double cor=pc.correlation(CommonUtils.doubleListToDoubleArray(features.get(i)),
								CommonUtils.doubleListToDoubleArray(features.get(j)) );
						System.out.println(i+","+j+" : "+cor);
					}
				}
			}

			
			/*for(int idx:scalarGroups.keySet()){
				ArrayList<ArrayList<Double>> allValues=scalarGroups.get(idx).values;

				
				//get the max and min of the current scalar
				ArrayList<Double> groupedValues=new ArrayList<Double>();
				for(ArrayList<Double> values: allValues){
					groupedValues.addAll(values);
				}				
				double[] normalizedInterval=CommonUtils.maxAndMin(CommonUtils.fromDoubleArrayListToArray(groupedValues));
				normalizedInterval[1]+=0.0001;
				
				for(int i=0;i<allValues.size();i++){
					ArrayList<Double> values=allValues.get(i);
					
					double[] distr=CommonUtils.calculateHistogram(values,normalizedInterval, 5);
					if(i>0) System.out.print(",");
					System.out.print("	{");
					for(int j=0;j<distr.length;j++){
						if(j>0) System.out.print(",");
						System.out.print(String.format("%.2f", distr[j]));
					}
					System.out.print("}");
				}
				System.out.println();
			}*/
		}catch (Exception ex) {
				ex.printStackTrace();
		}
	}
}
