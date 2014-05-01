package accelerometer;

import java.awt.print.Printable;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

import javax.naming.directory.DirContext;

import org.apache.commons.math3.analysis.function.Cos;
import org.math.io.StringPrintable;

import weka.attributeSelection.GainRatioAttributeEval;
import weka.filters.supervised.instance.StratifiedRemoveFolds;
import weka.filters.unsupervised.attribute.MergeTwoValues;

import com.google.common.io.Files;

import fusion.ProcessingForFusion;

import main.CommonUtils;
import main.Constants;
import main.EventClassifier;
import accelerometer.feature.AccelerometerFeature;
import accelerometer.feature.FeatureExtraction;

public class AccelerometerSignalProcessing {

	//TODO variable
	public static ArrayList<ArrayList<String>> detectedEvents=new ArrayList<ArrayList<String>>();
	
	
	public static void main(String[] args) {
		
		//extractMotionStateFeaturesForDrivingState();
				
		
		String folderOfAllRawFiles=Constants.ACCELEROMETER_RAW_DATA_DIR
				+Constants.PHONE_POSITIONS[Constants.PHONE_POSITION_ALL]+"/test/";
		String[] allRawFiles=getPathForAllFiles(
				new File(folderOfAllRawFiles));		
		
		
		String[] singleFile=new String[1]; 
		//singleFile[0]=folderOfAllRawFiles+"ACCELEROMETER_RAW_2013_11_011.log";
		singleFile[0]=Constants.ACCELEROMETER_BASE_DIR+"04202014/ACCELEROMETER_RAW_2014_04_200.log";
		
		//generateVectorsOfMSTIndicator(singleFile);
		
		detectedEvents.clear();
		detectedEvents.add(new ArrayList<String>());
		detectedEvents.add(new ArrayList<String>());
		EventDetection.readGroudTruth(new String[]{Constants.ACCELEROMETER_BASE_DIR+"04202014/ACCELEROMETER_RAW_2014_04_200.log"});
		detectByMSTViaGoogleAPI("2014_04_200");
		
		
		//generateVectorsOfCIVIndicator(Constants.ACCELEROMETER_BASE_DIR+"04202014/ACCELEROMETER_RAW_2014_04_200.log", 10, 3);
		
		//generateVectorsOfCIVIndicator(Constants.PHONE_POSITION_ALL, 10, 3);
		//testWindowSizeOrScopeForCIVIndicator(Constants.PHONE_POSITION_ALL, new int[]{2,6,10,14,18,22,26});
	}
	
	public static ArrayList<ArrayList<Double>> scopePreAndRecall=new ArrayList<ArrayList<Double>>();//testWindowSizeOrScopeForCIVIndicator
	
	
	public static void generateVectorsOfMSTIndicator(String[] pathForFilesToBeClassified){
		/**
		 * for motion state feature extraction
		 */
		Config config=new Config();
		config.motionStateFeature=true;
		config.windowSize=5;
		config.slidingStep=5;
		config.minNoOfSamplesInWholeWindow=1;//0;
		detectedEvents.clear();
		detectedEvents.add(new ArrayList<String>());
		detectedEvents.add(new ArrayList<String>());
		
		
		ArrayList<String> vectors=null;
		
		//read the ground truth		
		EventDetection.readGroudTruth(pathForFilesToBeClassified);		
		
		
		ArrayList<String> labeldVectors=new ArrayList<String>(); 
		for(String filepath: pathForFilesToBeClassified){
			System.out.println("********* "+ filepath +" ***************");

			// step 1: extract motion state features
			FeatureExtraction fe=new FeatureExtraction(config, new File(filepath) );
			System.out.println("step 1:  --> Extract Features and output the ARFF File");
			ArrayList<AccelerometerFeature> features=fe.run();
			
			//step 2: classify motion state and record detected events;
			String inputFileNameInDateFormat=CommonUtils.getFileName(filepath).replace("ACCELEROMETER_RAW_", "").replace(".log", "");
			vectors=EventClassifier.classifyMotionStates(inputFileNameInDateFormat, features);
			System.out.println();
			
			ArrayList<String> labeledVectorsOfThisFile;
			
			//option 1: label vectors of the MST indicator, i.e. indicating which vector is generated for parking/unparking  *************/
			//labeledVectorsOfThisFile=labelMSTVectors(vectors, EventDetection.groundTruth, inputFileNameInDateFormat);
			
			//option 2: for test purpose, simply labeling all MST vectors as "n" 
			labeledVectorsOfThisFile=new ArrayList<String>();
			for(String v:vectors){
				labeledVectorsOfThisFile.add(v+",n");
			}
			
			
			labeldVectors.addAll(labeledVectorsOfThisFile);			
			//output labeled vectors to a file
			try{
				String vectorOfMSTIndicator=Constants.ACCELEROMETER_FUSION_DIR+inputFileNameInDateFormat+"_MST.log";
				FileWriter fw=new FileWriter(vectorOfMSTIndicator);
				for(String lv:labeledVectorsOfThisFile){
					fw.write(lv+"\n");
				}
				System.out.println("vectors of MST indicator written to "+vectorOfMSTIndicator);
				fw.close();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		
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
		}
		
		
		//System.out.println(detectedEvents);
		
		//Step 3:
		/*** detect parking/unparking events via state transitions **/ 
		//EventDetection.calculatePerformance(detectedEvents);
	}
	
	
	
	
	public static ArrayList<String> allTPs=new ArrayList<String>();
	public static double[] tps=new double[4];
	
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
	
	public static void generateVectorsOfCIVIndicator(String[] filePaths, Config config){

		//no need of this step if the feature files exist
		for(String filepath: filePaths ){
			FeatureExtraction fe=new FeatureExtraction(config, new File(filepath));
			fe.run(); //convert the raw data to arff files 
			System.out.println();
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
	
	public static void extractMotionStateFeaturesForDrivingState(){
				//"2013_09_191","2013_09_1244", "2013_08_2710", "2013_08_295", "2013_08_2689", "2013_08_283", "2013_08_2311"
		//"2013_08_283", "2013_08_2311","2013_08_2710","2013_09_191",
	
		
		// process all files in a given array
		String[] fns={"ACCELEROMETER_RAW_2013_12_051"
				//,"ACCELEROMETER_RAW_2013_11_012"
				//,"ACCELEROMETER_RAW_2013_10_241"
				//,"ACCELEROMETER_RAW_2013_10_242"
				//,"ACCELEROMETER_RAW_2013_10_251"
				//,"ACCELEROMETER_RAW_2013_09_211"
		};	
		
		/**
		 * for motion state feature extraction
		 */
		Config config=new Config();
		config.motionStateFeature=true;
		config.minNoOfSamplesInWholeWindow=200;
		config.singleMotionStateOnly=true;	
		for(int i=0;i<fns.length; i++){
			//String fn=Constants.ACCELEROMETER_RAW_DATA_DIR+"all/"+fns[i]+".log";
			String fn="C:/Users/Shuo/Desktop/"+fns[i]+".log";
			FeatureExtraction fe=new FeatureExtraction(config, new File(fn) );
			System.out.println("step 1:  --> Extract Features and output the ARFF File");
			fe.run();	
			System.out.println();
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
			EventDetection.readGroudTruth(pathForFilesToBeClassified);
			
			detectedEvents.clear();
			detectedEvents.add(new ArrayList<String>());
			detectedEvents.add(new ArrayList<String>());
			//classify the files
			EventClassifier.run(false,Constants.CLASSIFIER_ACCEL_CIV_FEATURE, pathForFilesToBeClassified, classifierModel, phonePositionDirIdx);
			//System.out.println(detectedEvents);	
			
			//EventDetection.printGroundTruth();			
			EventDetection.calculatePerformance(detectedEvents);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * Weka trained method
	 * This classifier treats each accelerometer reading as an instance
	 */	
	//step 0: Minimally Labeled Accelerometer File	
	public static void activityTransitionByWekaAccelerometer(String fn){
		System.out.println("Weka Accelerometer");
		System.out.println("step 1:  --> Complete Labeled Accelerometer File");
		AccelerometerFileProcessing.createWekaInputFile(
		Constants.ACCELEROMETER_RAW_DATA_DIR+"accelerometer"+fn+".log"
		, Constants.ACCELEROMETER_RAW_DATA_DIR+"accelerometer"+fn+".arff"
		, false);
		System.out.println();
		
		System.out.println("step 2:  --> EventClassifier to output classified activities"); 
		EventClassifier.run(false,Constants.CLASSIFIER_ACCEL_RAW, new String[]{"accelerometer"+fn+".arff"}, "RandomForest", 0);
		System.out.println();
		
		System.out.println("step 3:  --> EventDetection to output events");
		if(EventDetection.setupFolderAndFieldIdx("Weka"))
			EventDetection.detectEvents(fn, EventDetection.groundTruth);
		System.out.println();
	}
	
	/*
	 * GoogleAPI  method
	 */
	
	public static void detectByMSTViaGoogleAPI(String fileNameAsDateSeq){
		System.out.println("Google API");
		System.out.println("step 1:  --> EventDetection to output events");
		if(EventDetection.setupFolderAndFieldIdx("google"))
			EventDetection.detectEvents(fileNameAsDateSeq, EventDetection.groundTruth);
		System.out.println();
	}
	
	
	
	

	
	

}
