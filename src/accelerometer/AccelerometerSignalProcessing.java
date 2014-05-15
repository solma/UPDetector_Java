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
import accelerometer.learning.MotionStateClassifierTraining;
import accelerometer.windowfeature.WindowFeature;
import accelerometer.windowfeature.WindowFeatureExtraction;

//test112
public class AccelerometerSignalProcessing {

	//TODO variable
	
	public static ArrayList<String> allTPs=new ArrayList<String>();
	public static double[] tps=new double[4];
	
	public static void main(String[] args) {
		
		/**
		 * Parameters setup
		 */
		////2014_05_012
		String[] dateSeqs=new String[]{
				//test set
				//"2013_09_1243","2013_09_1244","2013_08_2689"
				"2014_04_200", "2014_04_201",
				"2014_05_129", "2014_05_1212", "2014_05_1216",
				"2014_05_1318", "2014_05_1321", "2014_05_1319",
				"2014_05_1422"
			};
		String[] rawAcclFilepaths=convertDateSeqToAccelerometerRawFilPath(dateSeqs);
		UPActivitiesOfSameSource allGroundtruth=EventDetection.readGroudTruthFromRawAccelerometerFile(rawAcclFilepaths);
		
		
		Config civConf=new Config(10, 3);
		Config mstConf=new Config(10, 3); //5, 5
		ArrayList<ExperimentRun> performanceResults=new ArrayList<ExperimentRun>();

		/****************
		 * below specifying experiments
		 ********************/
		//testDetectionThresholdForCIVAndMSTWeka(dateSeqs,allGroundtruth, civConf, mstConf, 30);
		
		performanceResults.add(new ExperimentRun("MST-Google", detectByMST(dateSeqs, allGroundtruth, SOURCE.MST_GOOGLE ) ) ); //MST-GOOGLE
				
		performanceResults.add(new ExperimentRun("CIV",  detectByCIV(dateSeqs,allGroundtruth, civConf, 0.9, 30) ) );
		//if(true) return;
		
		
		//MST-Weka
		generateMotionStatesUsingWekaClassifier(dateSeqs, mstConf); //intermediate files (motion states) output
		performanceResults.add(new ExperimentRun("MST-Weka", detectByMST(dateSeqs, allGroundtruth, SOURCE.MST_WEKA) ) );
	
		performanceResults.add(new ExperimentRun("CIV-MST-Weka", detectByCIVAndMSTWeka(dateSeqs, civConf, mstConf, 0.9, 30) ) );
		
		System.out.println("******************* # of trips ="+ allGroundtruth.size()+" *****************************");
		
		for(ExperimentRun mo: performanceResults){
			System.out.println(mo);
		}
		
		
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
	public static EventDetectionResult detectByCIV(String[] dateSeqs, UPActivitiesOfSameSource allGroundtruth,
			Config config,	double detectionThreshold, int matchTimeDiffUpperBound	){
		String[] rawAcclFilepaths=convertDateSeqToAccelerometerRawFilPath(dateSeqs);
		
		UPActivitiesOfSameSource allDetected=new UPActivitiesOfSameSource(SOURCE.CIV);
		for(int i=0;i<dateSeqs.length;i++){
			String filepath=rawAcclFilepaths[i];
			String dateSeq=dateSeqs[i];
			
			WindowFeatureExtraction wfe=new WindowFeatureExtraction(config, new File(filepath));
			ArrayList<IndicatorVector> indicatorVectors=wfe.outputCIVVectors(wfe.run(), dateSeq, null);
			//wfe.saveIndicatorVectorToFile(indicatorVectors, dateSeq);
			allDetected.addAll(Fusion.detectByIndicatorVectors(indicatorVectors, dateSeq.substring(0, 10), detectionThreshold,  SOURCE.CIV));
		}
		return EventDetection.calculatePerformance( allDetected, allGroundtruth, matchTimeDiffUpperBound, config.slidingStep*config.scope/2);
	}
	
	
	public static EventDetectionResult detectByCIVAndMSTWeka(String[] dateSeqs, Config civConfig, Config mstConfig, double detectionThreshold, int matchTimeDiffUpperBound){
		String[] rawAcclFilepaths=convertDateSeqToAccelerometerRawFilPath(dateSeqs);
		UPActivitiesOfSameSource allGroundtruth=EventDetection.readGroudTruthFromRawAccelerometerFile(rawAcclFilepaths);
		
		UPActivitiesOfSameSource allDetected=new UPActivitiesOfSameSource(SOURCE.CIV_MST_WEKA);
		for(int i=0;i<dateSeqs.length;i++){
			String filepath=rawAcclFilepaths[i];
			String dateSeq=dateSeqs[i];
			
			//CIV vectors
			WindowFeatureExtraction wfe=new WindowFeatureExtraction(civConfig, new File(filepath));
			ArrayList<IndicatorVector> indicatorVectors=wfe.outputCIVVectors(wfe.run(), dateSeq, null);
			
			//MST vectors
			WindowFeatureExtraction fe=new WindowFeatureExtraction(mstConfig, new File(filepath) );
			indicatorVectors.addAll( EventClassifier.classifyMotionStatesAndReturnMSTVectors(dateSeq, fe.run()) );
			
			//sort vectors
			Collections.sort(indicatorVectors);
			
			//wfe.saveIndicatorVectorToFile(indicatorVectors, dateSeq);
			allDetected.addAll(Fusion.detectByIndicatorVectors(indicatorVectors, dateSeq.substring(0, 10), detectionThreshold,  SOURCE.CIV_MST_WEKA));
		}
		
		
		int matchTimeDiffLowerBound=Math.max(civConfig.slidingStep*civConfig.scope/2, mstConfig.slidingStep*mstConfig.scope/2);
		return EventDetection.calculatePerformance( allDetected, allGroundtruth, matchTimeDiffUpperBound, matchTimeDiffLowerBound);
	}

	
	/**
	 * 
	 * @param dateSeqs:  raw accelerometer files
	 * @param mstSource: google or weka (weka needs preprocessing of raw accelerometer files)
	 */
	public static EventDetectionResult detectByMST(String[] dateSeqs, UPActivitiesOfSameSource allGroundtruth, SOURCE mstSource){
		UPActivitiesOfSameSource allDetected;
		switch(mstSource){
		case MST_GOOGLE:
			System.out.println("*********** MST Detection via Google API *****************");
			EventDetection.setupParametersForMST("google");
			allDetected=new UPActivitiesOfSameSource(SOURCE.MST_GOOGLE);//MATCH_TIME_DIFF_THRESHOLD=30
			break;
		case MST_WEKA:
			System.out.println("*********** MST Detection via Weka Classifier *****************");
			EventDetection.setupParametersForMST("weka");
			allDetected=new UPActivitiesOfSameSource(SOURCE.MST_WEKA); //MATCH_TIME_DIFF_THRESHOLD=90
			break;
		default:
			System.err.println("Error: Unknown MST source");
			if(true) return null;
			break;
		}
		
		System.out.println("******  "+Arrays.toString(dateSeqs)+" ******");
		
		for(String dateSeq: dateSeqs){
			allDetected.addAll(EventDetection.findMotionStateTransition(dateSeq, mstSource));			
		}
		System.out.println();
		
		return EventDetection.calculatePerformance( allDetected, allGroundtruth);
	}
	
	public static void testDetectionThresholdForCIVAndMSTWeka(
			String[] dateSeqs, UPActivitiesOfSameSource allGroundtruth,
			Config civConfig, Config mstConfig, int matchTimeDiffUpperBound) {
		double[] detectionThresholdArray;
		// detectionThresholdArray=new double[]{0.5, 0.6, 0.7, 0.8};
		detectionThresholdArray = new double[10];
		for (int i = 0; i < detectionThresholdArray.length; i++)
			detectionThresholdArray[i] = 0.5 + i * 0.05;

		ArrayList<ExperimentRun> performanceResults = new ArrayList<ExperimentRun>();
		for (int i = 0; i < detectionThresholdArray.length; i++) {
			performanceResults.add(new ExperimentRun("DT="
					+ detectionThresholdArray[i], detectByCIVAndMSTWeka(
					dateSeqs, civConfig, mstConfig, detectionThresholdArray[i],
					matchTimeDiffUpperBound)));
		}

		System.out.println("******************* # of trips ="+ allGroundtruth.size() + " *****************************");
		for (ExperimentRun mo : performanceResults) {
			System.out.println(mo);
		}
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
	
	
	
	
}

class ExperimentRun{
	String  name;
	EventDetectionResult result;
	public ExperimentRun(String name, EventDetectionResult edr){
		this.name=name;
		result=edr;
	}
	
	public String toString(){
		return name+"\n"+result;
	}
}
