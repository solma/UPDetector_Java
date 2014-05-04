package accelerometer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import main.Constants;
import main.EventClassifier;
import accelerometer.feature.WindowFeature;
import accelerometer.feature.WindowFeatureExtraction;

public class MotionStateClassifierTraining {
	public static void main(String[] args){
		//extractMotionStateFeaturesForSingleState();
		
		EventClassifier.run(true, Constants.CLASSIFIER_ACCEL_STATE,
				new String[]{Constants.ACCELEROMETER_BASE_DIR+"motion_state/state_combined.arff"} ,
				"RandomForest", -1);
	
		
	}
	
	
	
	/**
	 * produce motion state vectors of one single state
	 */
	public static void extractMotionStateFeaturesForSingleState(){
				//"2013_09_191","2013_09_1244", "2013_08_2710", "2013_08_295", "2013_08_2689", "2013_08_283", "2013_08_2311"
		//"2013_08_283", "2013_08_2311","2013_08_2710","2013_09_191",
		
		String[] fns={"2014_05_034"};	
		
		/**
		 * for motion state feature extraction
		 */
		Config config=new Config(5, 5, true);
		//config.singleMotionStateOnly=true;	
		
		FileWriter fw;
		try {
			for (int i = 0; i < fns.length; i++) {
				String fn = Constants.ACCELEROMETER_STATE_FEATURES_DIR+ "still/ACCELEROMETER_RAW_" + fns[i] + ".log";
				WindowFeatureExtraction fe = new WindowFeatureExtraction(
						config, new File(fn));
				fw = new FileWriter(fn.replaceAll("RAW", "MOTION_STATE"));
				for(WindowFeature wf:	fe.run()){
					fw.write(wf.asMotionStateFeatures()+",Still\n");
				}
				fw.close();
				System.out.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
