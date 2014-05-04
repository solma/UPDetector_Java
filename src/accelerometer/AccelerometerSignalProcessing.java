package accelerometer;

import helper.CommonUtils;
import helper.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

import javax.naming.directory.DirContext;

import org.apache.commons.math3.analysis.function.Cos;
import org.math.io.StringPrintable;

import upactivity.UPActivitiesOfSameSource;
import upactivity.UPActivity.SOURCE;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.filters.supervised.instance.StratifiedRemoveFolds;
import weka.filters.unsupervised.attribute.MergeTwoValues;

import com.google.common.io.Files;


import fusion.Fusion;
import fusion.IndicatorVector;

import accelerometer.learning.EventClassifier;
import accelerometer.windowfeature.WindowFeature;
import accelerometer.windowfeature.WindowFeatureExtraction;

//test112
public class AccelerometerSignalProcessing {

	//TODO variable
	
	public static ArrayList<String> allTPs=new ArrayList<String>();
	public static double[] tps=new double[4];
	
	public static void main(String[] args) {
		
		//extractMotionStateFeaturesForDrivingState();
				
		
		String folderOfAllRawFiles=Constants.ACCELEROMETER_RAW_DATA_DIR
				+Constants.PHONE_POSITIONS[Constants.PHONE_POSITION_ALL]+"/test/";
		String[] allRawFiles=getPathForAllFiles(new File(folderOfAllRawFiles));		
		
		
		
		////2014_05_012
		String[] dateSeqs=new String[]{"2014_04_200", "2014_04_201"};
		//detectByMST(dateSeqs, MST_SOURCE.GOOGLE_API);
		
		detectByCIV(dateSeqs, new Config(10, 3));
		
		detectByCIVAndMSTWeka(dateSeqs, new Config(10, 3), new Config(5, 5, true));
		
		//MST-Weka
		//generateMotionStatesUsingWekaClassifier(dateSeqs, new Config(5, 5, true));
		//detectByMST(dateSeqs, MST_SOURCE.WEKA_CLASSIFIER);
		
		
		//generateVectorsOfCIVIndicator(Constants.ACCELEROMETER_BASE_DIR+"04202014/ACCELEROMETER_RAW_2014_04_200.log", 10, 3);
		
		
		//testWindowSizeOrScopeForCIVIndicator(Constants.PHONE_POSITION_ALL, new int[]{2,6,10,14,18,22,26});
	}
	
	public static ArrayList<ArrayList<Double>> scopePreAndRecall=new ArrayList<ArrayList<Double>>();//testWindowSizeOrScopeForCIVIndicator
	
	
	
	public static String[] convertDateSeqToAccelerometerRawFilPath(String[] dateSeqs){
		String[] filepaths=new String[dateSeqs.length];
		for(int i=0;i<dateSeqs.length;i++){
			filepaths[i]=Constants.ACCELEROMETER_RAW_DATA_DIR+"/all_position/ACCELEROMETER_RAW_"+dateSeqs[i]+".log";
		}
		return filepaths;
	}
	
	/**
	 * 
	 * @param dateSeqs: raw accelerometer files
	 * @param config: window feature extraction configuration
	 */
	public static void detectByCIV(String[] dateSeqs, Config config){
		String[] rawAcclFilepaths=convertDateSeqToAccelerometerRawFilPath(dateSeqs);
		UPActivitiesOfSameSource allGroundtruth=EventDetection.readGroudTruthFromRawAccelerometerFile(rawAcclFilepaths);
		
		UPActivitiesOfSameSource allDetected=new UPActivitiesOfSameSource(SOURCE.CIV);
		for(int i=0;i<dateSeqs.length;i++){
			String filepath=rawAcclFilepaths[i];
			String dateSeq=dateSeqs[i];
			
			WindowFeatureExtraction wfe=new WindowFeatureExtraction(config, new File(filepath));
			ArrayList<IndicatorVector> indicatorVectors=wfe.outputCIVVectors(wfe.run(), null, null);
			//wfe.saveIndicatorVectorToFile(indicatorVectors, dateSeq);
			allDetected.addAll(Fusion.detectByIndicatorVectors(indicatorVectors, dateSeq.substring(0, 10), 0.6,  SOURCE.CIV));
		}
		EventDetection.calculatePerformance( allDetected, allGroundtruth, 30);
	}
	
	
	public static void detectByCIVAndMSTWeka(String[] dateSeqs, Config civConfig, Config mstConfig){
		String[] rawAcclFilepaths=convertDateSeqToAccelerometerRawFilPath(dateSeqs);
		UPActivitiesOfSameSource allGroundtruth=EventDetection.readGroudTruthFromRawAccelerometerFile(rawAcclFilepaths);
		
		UPActivitiesOfSameSource allDetected=new UPActivitiesOfSameSource(SOURCE.CIV_MST_WEKA);
		for(int i=0;i<dateSeqs.length;i++){
			String filepath=rawAcclFilepaths[i];
			String dateSeq=dateSeqs[i];
			
			//CIV vectors
			WindowFeatureExtraction wfe=new WindowFeatureExtraction(civConfig, new File(filepath));
			ArrayList<IndicatorVector> indicatorVectors=wfe.outputCIVVectors(wfe.run(), null, null);
			
			//MST vectors
			WindowFeatureExtraction fe=new WindowFeatureExtraction(mstConfig, new File(filepath) );
			indicatorVectors.addAll( EventClassifier.classifyMotionStatesAndReturnMSTVectors(dateSeq, fe.run()) );
			
			//sort vectors
			Collections.sort(indicatorVectors);
			
			//wfe.saveIndicatorVectorToFile(indicatorVectors, dateSeq);
			allDetected.addAll(Fusion.detectByIndicatorVectors(indicatorVectors, dateSeq.substring(0, 10), 0.6,  SOURCE.CIV_MST_WEKA));
		}
		EventDetection.calculatePerformance( allDetected, allGroundtruth, 30);
	}
	
	
	/*
	 * GoogleAPI  method
	 */
	public enum MST_SOURCE{
		GOOGLE_API, WEKA_CLASSIFIER;
	}
	
	/**
	 * 
	 * @param dateSeqs:  raw accelerometer files
	 * @param mstSource: google or weka (weka needs preprocessing of raw accelerometer files)
	 */
	public static void detectByMST(String[] dateSeqs, MST_SOURCE mstSource){
		UPActivitiesOfSameSource allDetected;
		switch(mstSource){
		case GOOGLE_API:
			System.out.println("*********** MST Detection via Google API *****************");
			EventDetection.setupParametersForMST("google");
			allDetected=new UPActivitiesOfSameSource(SOURCE.MST_GOOGLE);
			break;
		case WEKA_CLASSIFIER:
			System.out.println("*********** MST Detection via Weka Classifier *****************");
			EventDetection.setupParametersForMST("weka");
			allDetected=new UPActivitiesOfSameSource(SOURCE.MST_WEKA);
			break;
		default:
			System.err.println("Error: Unknown MST source");
			if(true) return;
			break;
		}
		
		System.out.println("******  "+Arrays.toString(dateSeqs)+" ******");
		String[] rawAcclFilepaths=convertDateSeqToAccelerometerRawFilPath(dateSeqs);
		UPActivitiesOfSameSource allGroundtruth=EventDetection.readGroudTruthFromRawAccelerometerFile(rawAcclFilepaths);
		
		for(String dateSeq: dateSeqs){
			allDetected.addAll(EventDetection.findMotionStateTransition(dateSeq));			
		}
		EventDetection.calculatePerformance( allDetected, allGroundtruth);
		
		System.out.println();
	}
	
	
	public static void generateMotionStatesUsingWekaClassifier(String[] dateSeqs, Config config){
		//convert dateseq to filepaths
		String[] rawAcclFilepaths=convertDateSeqToAccelerometerRawFilPath(dateSeqs);
		
		for(int i=0;i<rawAcclFilepaths.length;i++){
			String dateSeq=dateSeqs[i];
			String filepath=rawAcclFilepaths[i];
			System.out.println("********* "+ filepath +" ***************");
			WindowFeatureExtraction fe=new WindowFeatureExtraction(config, new File(filepath) );
			ArrayList<IndicatorVector> mstVectors=EventClassifier.classifyMotionStatesAndReturnMSTVectors(dateSeq, fe.run());
			System.out.println(mstVectors.size()+"  motion states classified");
		}
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
		
		//Step 3:
		/*** detect parking/unparking events via state transitions **/ 
		//EventDetection.calculatePerformance(detectedEvents);
	
	
	
	
	

	
	public static void generateVectorsOfCIVIndicator(int phonePosDirIdx, int scope, int slidingStep){
		String[] inputFiles=getPathForAllFiles(new File(Constants.ACCELEROMETER_RAW_DATA_DIR+
				Constants.PHONE_POSITIONS[phonePosDirIdx]) );
		Config.SLIDING_STEP=slidingStep;
		Config.SCOPE=scope;
		Config config=new Config(phonePosDirIdx);
		generateVectorsOfCIVIndicator(inputFiles, config);
	}
	
	
	public static void generateVectorsOfCIVIndicator(String singleRawFilePath, int scope, int slidingStep){
		String[] inputFiles={singleRawFilePath};
		Config.SLIDING_STEP=slidingStep;
		Config.SCOPE=scope;
		Config config=new Config();
		generateVectorsOfCIVIndicator(inputFiles, config);
	}
	
	
	
	public static void testWindowSizeOrScopeForCIVIndicator(int phonePosDirIdx, int[] scopeArray){
		/*int[] windowSize={6, 10, 20, 30};
		//test the windowSize
		for(int i=0;i<windowSize.length;i++){
			Config.WINDOW_SIZE=windowSize[i];
			preprocess(); //convert the raw data to arff files 
			classifyingFeature(POSITION_DIR_IDX);//classify the files in the "test" folder under the position folder
			allTPs.add("windowSize="+windowSize[i]+"  :  "+Arrays.toString(tps));
		
		}*/
		for(int i=0;i<scopeArray.length;i++){
			int scope=scopeArray[i];
		
			scopePreAndRecall.add(new ArrayList<Double>());
			scopePreAndRecall.get(scopePreAndRecall.size()-1).add(scope+0.0);
			
			generateVectorsOfCIVIndicator(phonePosDirIdx, scopeArray[i], 3);
			
		}
		for(ArrayList<Double> res:scopePreAndRecall){
			System.out.println(res);
		}
		
	}
	
	
	
	
	/**
	 * @param: folder:
	 * return all log files in a the given folder
	 */
	public static String[] getPathForAllFiles(File folder){
		// process all log files in a folder
		File[] files=folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if(name.startsWith("ACCELEROMETER_RAW_")&&name.endsWith(".log")){
					return true;
				}
				return false;
			}
		});
		String[] fns=new String[files.length];
		for(int i=0;i<fns.length;i++) fns[i]=files[i].getAbsolutePath().replaceAll("\\\\", "/");
		return fns;
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
	
	
	public static void detectByCIVClassifier(int phonePositionDirIdx){
		try{			
			System.out.println("step 1: --> EventClassifier to train the classifier model");	
			String trainFolder=Constants.ACCELEROMETER_CIV_FEATURES_DIR+
					Constants.PHONE_POSITIONS[phonePositionDirIdx]+"/train/";
			
			//merge all arff files in the train folder
				FileWriter fw=new FileWriter(new File(trainFolder+"combined.arff"));
				Scanner sc=new Scanner(new File(Constants.ACCELEROMETER_CIV_FEATURES_DIR+"arff_header.txt"));
				String line;
				while(sc.hasNextLine()){
					fw.write(sc.nextLine()+"\n");
				}
				sc.close();
				
				File dir=new File(trainFolder);
				File[] files=dir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						if(name.startsWith("ACCELEROMETER_FEATURE_")&&name.endsWith(".arff")){
							return true;
						}
						return false;
					}
				});
				FileWriter listFile=new FileWriter(trainFolder+"training_file_list.txt");
				for(File f: files){
					sc=new Scanner(f);
					listFile.write(f.getName()+"\n");
					while(sc.hasNextLine()){
						line=sc.nextLine();
						if(line.startsWith("@")) continue;
						fw.write(line+"\n");
					}
				}
				listFile.close();
				fw.close();
				sc.close();	
			
			//train the model first 
			EventClassifier.run(true, Constants.CLASSIFIER_ACCEL_CIV_FEATURE,new String[]{trainFolder+"combined.arff"}, "RandomForest",  phonePositionDirIdx );
			
			/*
			 * 
			 */
			System.out.println("step 2: --> EventClassifier to output classified activities");		
			String[] models={"RandomForest", "NBTree" , "J48","RandomCommittee"};
			String classifierModel=models[0];
			
			// define the files to be classified
			files=new File(Constants.ACCELEROMETER_CIV_FEATURES_DIR+
					Constants.PHONE_POSITIONS[phonePositionDirIdx]+"/test/").listFiles();
			
			String[] pathForFilesToBeClassified=new String[files.length];
			for(int i=0;i<files.length;i++) pathForFilesToBeClassified[i]=files[i].getAbsolutePath().replace("\\", "/");
			//System.out.println(Arrays.toString(pathForFilesToBeClassified));
			
			//read the ground truth		
			EventDetection.readGroudTruthFromRawAccelerometerFile(pathForFilesToBeClassified);
			
			//classify the files
			EventClassifier.run(false,Constants.CLASSIFIER_ACCEL_CIV_FEATURE, pathForFilesToBeClassified, classifierModel, phonePositionDirIdx);
			//System.out.println(detectedEvents);	
			
			//EventDetection.printGroundTruth();			
			//EventDetection.calculatePrecisionAndRecall(detectedEvents);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * take raw acclerometer file as input
	 * output civ vector
	 */
	public static void generateVectorsOfCIVIndicator(String[] dateSeqs, Config config){
		for(String dateSeq: dateSeqs ){
			String filepath=Constants.ACCELEROMETER_RAW_DATA_DIR+"all_position/ACCELEROMETER_RAW_"+dateSeq+".log";
			WindowFeatureExtraction fe=new WindowFeatureExtraction(config, new File(dateSeq));
			ArrayList<WindowFeature> vectorsOfCIV=fe.run(); 
			
		}
		//ProcessingForFusion.fusion();
		//detectByCIVClassifier(phonePosDirIdx);//classify the files in the "test" folder under the position folder
		//allTPs.add("neighborSize="+neighborSize[i]+"  :  "+Arrays.toString(tps));
	}
	
	/**
	 * take raw accelerometer file
	 * return mst vectors that are generated by motion state classifier
	 */
	public static void generateVectorsOfMSTWekaClassifier(String[] dateSeqs){
		System.out.println("Weka Accelerometer");
		for(String fn: dateSeqs){
			System.out.println("step 1:  --> Complete Labeled Accelerometer File");
			AccelerometerFileProcessing.createWekaInputFile(
			Constants.ACCELEROMETER_RAW_DATA_DIR+"accelerometer"+fn+".log"
			, Constants.ACCELEROMETER_RAW_DATA_DIR+"accelerometer"+fn+".arff"
			, false);
			System.out.println();
			
			System.out.println("step 2:  --> EventClassifier to output classified activities"); 
			EventClassifier.run(false,Constants.CLASSIFIER_ACCEL_RAW, new String[]{"accelerometer"+fn+".arff"}, "RandomForest", 0);
			System.out.println();
		
			System.out.println();
		}
	}
	
	
	
	
	

	
	

}
