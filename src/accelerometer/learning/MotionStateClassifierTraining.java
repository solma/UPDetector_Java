package accelerometer.learning;

import helper.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.common.base.CaseFormat;

import upactivity.UPActivitiesOfSameSource;
import upactivity.UPActivity;

import accelerometer.AccelerometerFileProcessing;
import accelerometer.AccelerometerSignalProcessing;
import accelerometer.Config;
import accelerometer.EventDetection;
import accelerometer.AccelerometerSignalProcessing.MST_SOURCE;
import accelerometer.EventDetectionResult;
import accelerometer.windowfeature.WindowFeature;
import accelerometer.windowfeature.WindowFeatureExtraction;

public class MotionStateClassifierTraining {
	public static void main(String[] args){
		
		//tuneParasForMSTClassifier();
		
		Config mstConf=new Config(10, 3);
		trainNewMotionStateClassifierModelInOneMethod(mstConf);
		
		//extractMotionStateForDrivingAndWalkingFromGroundTruthLabeldRawFiles(mstConf);
		
		//extractMotionStateFeaturesForSingleState(mstConf, "Standing");
		//extractMotionStateFeaturesForSingleState(mstConf, "Walking");
		//extractMotionStateFeaturesForSingleState(mstConf, "Still");
		
		
		/**
		 *  train the new classifier for motion states
		 */
		//EventClassifier.run(true, Constants.CLASSIFIER_ACCEL_STATE, new String[]{Constants.ACCELEROMETER_BASE_DIR+"motion_state/state_combined.arff"},"RandomForest", -1);
	}
	
	public static void tuneParasForMSTClassifier(){
		String[] dateSeqs=new String[]{
				//test set
				//"2013_09_1243","2013_09_1244","2013_08_2689"
				"2014_04_200", "2014_04_201"
			};
		ArrayList<EventDetectionResult> performanceResults=new ArrayList<EventDetectionResult>();
		
		
		int[][] mstConfParas=new int[][]{{10, 5}, {10, 3}, {10, 2}, {5,5}};
		
		Config mstConf;
		for(int[] paras: mstConfParas){
			mstConf=new Config(paras[0], paras[1]);
			MotionStateClassifierTraining.trainNewMotionStateClassifierModelInOneMethod(mstConf);
			
			AccelerometerSignalProcessing.generateMotionStatesUsingWekaClassifier(dateSeqs, mstConf); 
			performanceResults.add( AccelerometerSignalProcessing.detectByMST(dateSeqs, MST_SOURCE.WEKA_CLASSIFIER) );
		}
		
		for(int i=0;i<mstConfParas.length;i++){
			System.out.println(Arrays.toString(mstConfParas[i]));
			System.out.println(performanceResults.get(i));
		}
	}
	
	
	public static void extractMotionStateForDrivingAndWalkingFromGroundTruthLabeldRawFiles(Config config, FileWriter fw){
		try {
			String[] fns = { 	
					"2013_09_191","2013_09_1244", "2013_08_2710", "2013_08_295", "2013_08_2689", "2013_08_283", "2013_08_2311"
					,"2013_09_1243","2013_09_1244","2013_08_2689"
					//"2014_04_200", "2014_04_201"
					};
			ArrayList<UPActivity> groundtruth;
			UPActivity act1, act2;
			String motionState="";
			for (int i = 0; i < fns.length; i++) {
				String fn = Constants.ACCELEROMETER_RAW_DATA_DIR + "all_position/ACCELEROMETER_RAW_" + fns[i] + ".log";
				System.out.println("*************** "+fn+" ***************");
				
				groundtruth=EventDetection.readGroudTruthFromRawAccelerometerFile(new String[]{fn}).toOneSingleSortedList();
				WindowFeatureExtraction fe = new WindowFeatureExtraction(config, new File(fn));
				
				for (WindowFeature wf : fe.run()) {
					int[] timeWindow=wf.temporalWindow;
					
					//if the window feature is in one single state
					act1=UPActivity.binarySearchLastSmaller(groundtruth, timeWindow[0]);
					act2=UPActivity.binarySearchLastSmaller(groundtruth, timeWindow[1]);
					if(act1!=null&&act2!=null&&	act1.equals(act2)){
						switch (act1.type) {
						case Constants.PARKING_ACTIVITY:
							motionState="Walking";
							break;
						case Constants.UNPARKING_ACTIVITY:
							motionState="Driving";
							break;
						default:
							break;
						}
						fw.write(wf.asMotionStateFeatures() + "," +motionState+ "\n");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void extractMotionStateForDrivingAndWalkingFromGroundTruthLabeldRawFiles(Config config){
		try {
			FileWriter fw = new FileWriter(Constants.ACCELEROMETER_MOTION_STATE_DIR+"driving_and_walking.arff");
			extractMotionStateForDrivingAndWalkingFromGroundTruthLabeldRawFiles(config, fw);
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static String[] setupFilesForSingleMotionState(String motionState){
		String[] fns=null;
		switch (motionState) {
		case "Still":
			fns=new String[]{"2014_05_034"};
			break;
		case "Walking":
			fns=new String[]{"2014_05_045_in_hand"};
			break;
		case "Standing":
			fns=new String[]{"2014_05_046_in_hand", "2014_05_047"};
			break;
		default:
			break;
		}
		return fns;
	}
	
	/**
	 * do not close filewrite in this method
	 * @param config
	 * @param motionState
	 * @param fw
	 */
	public static void extractMotionStateFeaturesForSingleState(Config config, String motionState, FileWriter fw){
		try {
			String[] fns=setupFilesForSingleMotionState(motionState);
			for (int i = 0; i < fns.length; i++) {
				String fn = Constants.ACCELEROMETER_MOTION_STATE_DIR + motionState + "/ACCELEROMETER_RAW_" + fns[i] + ".log";
				WindowFeatureExtraction fe = new WindowFeatureExtraction(config, new File(fn));
				for (WindowFeature wf : fe.run()) {
					//if(motionState.equals("Standing")) fw.write(wf.asMotionStateFeatures() + ",Still\n");
//					//else 
						fw.write(wf.asMotionStateFeatures() + "," + motionState + "\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * produce motion state vectors of one single state
	 */
	public static void extractMotionStateFeaturesForSingleState(Config config, String motionState){
		try {
			String[] fns=setupFilesForSingleMotionState(motionState);
						
			String outputFilePath=Constants.ACCELEROMETER_MOTION_STATE_DIR + motionState +"/"+ motionState +".arff";
			FileWriter fw=new FileWriter(outputFilePath);
			AccelerometerFileProcessing.writeMotionStateArffHeader(fw, Constants.ACCELEROMETER_MOTION_STATE_DIR+"state_arff_header.txt");
			extractMotionStateFeaturesForSingleState(config, motionState, fw);
			fw.close();
			
			//System.out.println(System.getProperty("user.dir"));
			System.out.println("File output to "+outputFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void trainNewMotionStateClassifierModelInOneMethod(Config config){
		//prepare the training file
		try {
			FileWriter fw=new FileWriter(Constants.ACCELEROMETER_MOTION_STATE_DIR+"state_combined.arff");
			AccelerometerFileProcessing.writeMotionStateArffHeader(fw, Constants.ACCELEROMETER_MOTION_STATE_DIR+"state_arff_header.txt");
			
			//Extract Driving and Walking
			extractMotionStateForDrivingAndWalkingFromGroundTruthLabeldRawFiles(config,fw);
			//Extract Single State
			String[] motionStates={"Walking", "Still", "Standing"};
			for(String ms: motionStates){
				extractMotionStateFeaturesForSingleState(config, ms, fw);
			}
			fw.close();
			
			//train the model
			EventClassifier.run(true, Constants.CLASSIFIER_ACCEL_STATE, new String[]{Constants.ACCELEROMETER_BASE_DIR+"motion_state/state_combined.arff"},"RandomForest", -1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
	}
}
