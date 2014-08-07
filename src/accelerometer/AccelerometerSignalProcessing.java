package accelerometer;

import helper.CommonUtils;
import helper.Constants;
import helper.DetectionMethod;
import helper.RawReadingDuration;

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

import org.apache.commons.math3.analysis.function.Add;
import org.apache.commons.math3.analysis.function.Cos;
import org.math.io.StringPrintable;

import upactivity.UPActivitiesOfSameSource;
import upactivity.UPActivity;
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
	
	
	public static RawReadingDuration duration=new RawReadingDuration();
	public static void main(String[] args) {
		
		/**
		 * Parameters setup
		 */
		////2014_05_012
		String[] dateSeqs=new String[]{
				//test set
				"2014_04_200", "2014_04_201",
				"2014_05_129", "2014_05_1212", "2014_05_1216",
				"2014_05_1318", "2014_05_1321", "2014_05_1319",
				"2014_05_1422",
				"2014_05_1526",	"2014_05_1527", "2014_05_1528", "2014_05_1529",
				"2014_05_1631",
				"2014_05_1734",	"2014_05_1735",	"2014_05_1737",
				"2014_05_1838",
				"2014_05_1939",
				"2014_05_2042",
				"2014_05_227"
			};
		String[] rawAcclFilepaths=convertDateSeqToAccelerometerRawFilPath(dateSeqs);
		UPActivitiesOfSameSource allGroundtruth=EventDetection.readGroudTruthFromRawAccelerometerFile(rawAcclFilepaths);
		
		
		Config civConf=new Config(10, 3, 6);
		Config mstConf=new Config(10, 3); 
		ArrayList<DetectionMethod> performanceResults=new ArrayList<DetectionMethod>();

		/****************
		 * below specifying experiments
		 ********************/
		//testDetectionThresholdForCIVAndMSTWekaGoogle(dateSeqs,allGroundtruth, civConf, mstConf, 30, VaryingParameter.DETECTION_THRESHOLD);

		
		/*MST-Google*/
		DetectionMethod dmMstGoogle=detectByMST(dateSeqs, allGroundtruth, SOURCE.MST_GOOGLE );
		performanceResults.add( dmMstGoogle); 
		
		
		double detectionThresold=0.7;
		/*CIV*/
		performanceResults.add(  detectByCIV(dateSeqs,allGroundtruth, civConf, detectionThresold, 30));
		System.out.println("total duration = "+ duration.totalDuration());
//		if(true) return;
		
		/*MST-Weka*/
		generateMotionStatesUsingWekaClassifier(dateSeqs, mstConf); //intermediate files (motion states) output
		performanceResults.add(detectByMST(dateSeqs, allGroundtruth, SOURCE.MST_WEKA)  );
	
		/*CIV-MST-Weka*/
		DetectionMethod dmCIVMSTWeka=detectByCIVAndMSTWeka(dateSeqs, civConf, mstConf, detectionThresold, 30) ;
		performanceResults.add( dmCIVMSTWeka );
		
		
		/*CIV-MST-Weka-Google*/
		DetectionMethod dmFuseAll=detectByCIVAndMSTWekaGoogle(allGroundtruth, dmMstGoogle.detectedEvents, dmCIVMSTWeka, 60);
		performanceResults.add(dmFuseAll);

		
		System.out.println("******************* # of trips ="+ allGroundtruth.size()+" *****************************");
		for(DetectionMethod mo: performanceResults){
			System.out.println(mo.printResult());
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
	public static DetectionMethod detectByCIV(String[] dateSeqs, UPActivitiesOfSameSource allGroundtruth,
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
		return new DetectionMethod("CIV", 
				EventDetection.calculatePerformance( allDetected, allGroundtruth, matchTimeDiffUpperBound, config.slidingStep*config.scope/2)
				,allDetected);
	}
	
	
	public static DetectionMethod detectByCIVAndMSTWeka(String[] dateSeqs, Config civConfig, Config mstConfig, double detectionThreshold, int matchTimeDiffUpperBound){
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
		return new DetectionMethod("CIV_MST_WEKA",		
				EventDetection.calculatePerformance( allDetected, allGroundtruth, matchTimeDiffUpperBound, matchTimeDiffLowerBound)
				,allDetected);
	}

	
	/**
	 * 
	 * @param dateSeqs:  raw accelerometer files
	 * @param mstSource: google or weka (weka needs preprocessing of raw accelerometer files)
	 */
	public static DetectionMethod detectByMST(String[] dateSeqs, UPActivitiesOfSameSource allGroundtruth, SOURCE mstSource){
		UPActivitiesOfSameSource allDetected;
		String methodName="";
		switch(mstSource){
		case MST_GOOGLE:
			System.out.println("*********** MST Detection via Google API *****************");
			EventDetection.setupParametersForMST(SOURCE.MST_GOOGLE);
			allDetected=new UPActivitiesOfSameSource(SOURCE.MST_GOOGLE);//MATCH_TIME_DIFF_THRESHOLD=30
			methodName="MST_Google";
			break;
		case MST_WEKA:
			System.out.println("*********** MST Detection via Weka Classifier *****************");
			EventDetection.setupParametersForMST(SOURCE.MST_WEKA);
			allDetected=new UPActivitiesOfSameSource(SOURCE.MST_WEKA); //MATCH_TIME_DIFF_THRESHOLD=90
			methodName="MST_Weka";
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
		
		return new DetectionMethod(methodName, 
				EventDetection.calculatePerformance( allDetected, allGroundtruth),
				allDetected);
	}
	
	
	public static DetectionMethod detectByCIVAndMSTWekaGoogle(UPActivitiesOfSameSource allGroundtruth, 
			UPActivitiesOfSameSource detectedByMSTGoogle, 
			DetectionMethod dmCIVMSTWeka , int expiratioTime){
		//update google detected by removing detections that are not preceded by 
		//a detection generated by CIVMSTWeka within the "near past" (defined by the epiration time)  
		int[] actTypes={Constants.UNPARKING_ACTIVITY, Constants.PARKING_ACTIVITY};
		System.out.println("CIV-MST_WEKA_GOOGLE: Removed G Activities");
		for(int type: actTypes){
			ArrayList<UPActivity> g=detectedByMSTGoogle.get(type);
			ArrayList<UPActivity> civWeka=dmCIVMSTWeka.detectedEvents.get(type);
			
			ArrayList<UPActivity> toBeRemoved=new ArrayList<UPActivity>();
			for(UPActivity gact: g){
				int firstLaterActIdx=UPActivity.binarySearchFirstLater(civWeka, 
						gact.date+UPActivity.DATE_TIME_DELIMETER+CommonUtils.secondsToHMS(gact.timeInSecOfDay-expiratioTime) );
				if(firstLaterActIdx==civWeka.size()
				||civWeka.get(firstLaterActIdx).timeInSecOfDay>gact.timeInSecOfDay){
					toBeRemoved.add(gact);
				}
			}
			System.out.println((type==Constants.PARKING_ACTIVITY?"Parking: ":"Unparking: ")+toBeRemoved);
			for(UPActivity act: toBeRemoved) g.remove(act);
		}
		
		//calculate the new pre and rec
		EventDetection.setupParametersForMST(SOURCE.MST_GOOGLE);
		EventDetectionResult newEdr=EventDetection.calculatePerformance(detectedByMSTGoogle, allGroundtruth);
		System.out.println("newEdr:\n"+newEdr);
		//replacing the avgDelay
		for(int type: actTypes){
			newEdr.update(type, dmCIVMSTWeka.result.get(type).avgDelay);
		}
		return new DetectionMethod("CIV-MST_WEKA_GOOGLE", newEdr);
	}
	
	
	
	
	public enum VaryingParameter{
		DETECTION_THRESHOLD, SCOPE;
	}
	public static void testDetectionThresholdForCIVAndMSTWekaGoogle(
			String[] dateSeqs, UPActivitiesOfSameSource allGroundtruth,
			Config civConfig, Config mstConfig, int matchTimeDiffUpperBound,
			VaryingParameter para
			) {
		ArrayList<DetectionMethod> performanceResults = new ArrayList<DetectionMethod>();
		ArrayList<DetectionMethod> performanceResultsCIVWeka = new ArrayList<DetectionMethod>();
		
		switch (para) {
		case DETECTION_THRESHOLD:
			double[] detectionThresholdArray;
			// detectionThresholdArray=new double[]{0.5, 0.6, 0.7, 0.8};
			detectionThresholdArray = new double[9];
			for (int i = 0; i < detectionThresholdArray.length; i++)
				detectionThresholdArray[i] = 0 + i * 0.1;
			
			for (int i = 0; i < detectionThresholdArray.length; i++) {
				DetectionMethod dmMstGoogle=detectByMST(dateSeqs, allGroundtruth, SOURCE.MST_GOOGLE );
				DetectionMethod dmCIVMSTWeka=detectByCIVAndMSTWeka(dateSeqs, civConfig, mstConfig, detectionThresholdArray[i],matchTimeDiffUpperBound);
				
				DetectionMethod dmFuseAll=detectByCIVAndMSTWekaGoogle(allGroundtruth, dmMstGoogle.detectedEvents, dmCIVMSTWeka, 60);
				dmFuseAll.name+=" DT="+ detectionThresholdArray[i];
				performanceResults.add(dmFuseAll);
				performanceResultsCIVWeka.add(dmCIVMSTWeka);
			}
			break;
		case SCOPE:
			int[] scope=new int[]{2,4,6,8,10,12,14,18,20};
			for (int i = 0; i < scope.length; i++) {
				civConfig.scope=scope[i];
				mstConfig.scope=scope[i];
				DetectionMethod dmMstGoogle=detectByMST(dateSeqs, allGroundtruth, SOURCE.MST_GOOGLE );
				DetectionMethod dmCIVMSTWeka=detectByCIVAndMSTWeka(dateSeqs, civConfig, mstConfig, 0.5,matchTimeDiffUpperBound);
				
				DetectionMethod dmFuseAll=detectByCIVAndMSTWekaGoogle(allGroundtruth, dmMstGoogle.detectedEvents, dmCIVMSTWeka, 60);
				dmFuseAll.name+=" Scope="+ scope[i];
				performanceResults.add(dmFuseAll);
				performanceResultsCIVWeka.add(dmCIVMSTWeka);
			}
		default:
			break;
		}

		System.out.println("******************* # of trips ="+ allGroundtruth.size() + " *****************************");
		for(int i=0;i<performanceResults.size();i++){
			System.out.println(performanceResults.get(i).printResult());
			System.out.println(performanceResultsCIVWeka.get(i).printResult());
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











