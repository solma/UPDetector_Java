package accelerometer.learning;

import helper.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import upactivity.UPActivitiesOfSameSource;
import upactivity.UPActivity;

import accelerometer.AccelerometerFileProcessing;
import accelerometer.Config;
import accelerometer.EventDetection;
import accelerometer.windowfeature.WindowFeature;
import accelerometer.windowfeature.WindowFeatureExtraction;

public class MotionStateClassifierTraining {
	public static void main(String[] args){
		
		Config config=new Config(5, 5);
		
		//extractMotionStateForDrivingAndWalkingFromGroundTruthLabeldRawFiles(config);
		
		//extractMotionStateFeaturesForSingleState(config, "Standing");
		
		
		/**
		 *  train the new classifier for motion states
		 */
		EventClassifier.run(true, Constants.CLASSIFIER_ACCEL_STATE, new String[]{Constants.ACCELEROMETER_BASE_DIR+"motion_state/state_combined.arff"},"RandomForest", -1);
	}
	
	public static void extractMotionStateForDrivingAndWalkingFromGroundTruthLabeldRawFiles(Config config){
		try {
			String[] fns = { 	
					"2013_09_191","2013_09_1244", "2013_08_2710", "2013_08_295", "2013_08_2689", "2013_08_283", "2013_08_2311"
					//"2013_09_1243","2013_09_1244","2013_08_2689"
					//"2014_04_200", "2014_04_201"
					};
			ArrayList<UPActivity> groundtruth;
			FileWriter fw = new FileWriter(Constants.ACCELEROMETER_MOTION_STATE_DIR+"driving_and_walking.arff");
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
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * produce motion state vectors of one single state
	 */
	public static void extractMotionStateFeaturesForSingleState(Config config, String motionState){
		try {
			String[] fns = { "2014_05_046_in_hand", "2014_05_047" };
			
			String outputFilePath=Constants.ACCELEROMETER_MOTION_STATE_DIR + motionState +"/"+ motionState +".arff";
			FileWriter fw = new FileWriter(outputFilePath);
			AccelerometerFileProcessing.writeMotionStateArffHeader(fw, Constants.ACCELEROMETER_MOTION_STATE_DIR+"state_arff_header.txt");
			
			for (int i = 0; i < fns.length; i++) {
				String fn = Constants.ACCELEROMETER_MOTION_STATE_DIR + motionState + "/ACCELEROMETER_RAW_" + fns[i] + ".log";
				WindowFeatureExtraction fe = new WindowFeatureExtraction(config, new File(fn));

				for (WindowFeature wf : fe.run()) {
					if(motionState.equals("Standing")) fw.write(wf.asMotionStateFeatures() + ",Still\n");
					else fw.write(wf.asMotionStateFeatures() + "," + motionState + "\n");
				}
			}
			fw.close();
			//System.out.println(System.getProperty("user.dir"));
			System.out.println("File output to "+outputFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
