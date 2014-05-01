package main;

public final class Constants {

	public static final int UNPARKING=0;
	public static final int PARKING=1;
	
	public static final String DATA_BASE_DIR="E:/workspace/android/UPDetector/data/";
	
	
	/**
	 * For accelerometer signal
	 */
	public static final int AXIS_NUMBER=4;//triaxies: x, y, z and the total 
	public static final int AXIS_X=0;
	public static final int AXIS_Y=1;
	public static final int AXIS_Z=2;
	public static final int AXIS_AGG=3;
	
	
	//dir setting	
	public static final String ACCELEROMETER_BASE_DIR=DATA_BASE_DIR+"accelerometer/";
	public static final String PLOTING_CAR_BACKING_BASE=ACCELEROMETER_BASE_DIR+"figs/data/car_backing/";
	public static final String ACCELEROMETER_RAW_DATA_DIR=ACCELEROMETER_BASE_DIR+"raw/";
	public static final String ACCELEROMTER_ACTIVITY_GOOGLE_DIR=ACCELEROMETER_BASE_DIR+"activity/GoogleAPI/";
	public static final String ACCELEROMTER_ACTIVITY_WEKA_DIR=ACCELEROMETER_BASE_DIR+"activity/Weka/";
	public static final String ACCELEROMETER_CIV_FEATURES_DIR=ACCELEROMETER_BASE_DIR+"change_in_variance/";
	public static final String ACCELEROMETER_STATE_FEATURES_DIR=ACCELEROMETER_BASE_DIR+"motion_state/";
	
	public static final String ACCELEROMETER_FUSION_DIR=ACCELEROMETER_BASE_DIR+"fusion/";
	
	
	/**
	 * External data source
	 */
	public static final String ACCELEROMETER_DAILY_ACTIVITY_EXTERNAL_DATA_BASE_DIR="E:/program_data/Activity/";
	public static final String ACCELEROMETER_DAILY_ACTIVITY_EXTERNAL_HAR_DIR=ACCELEROMETER_DAILY_ACTIVITY_EXTERNAL_DATA_BASE_DIR+"HAR_Dataset/";
	public static final String ACCELEROMETER_DAILY_ACTIVITY_EXTERNAL_HMP_DIR=ACCELEROMETER_DAILY_ACTIVITY_EXTERNAL_DATA_BASE_DIR+"HMP_Dataset/";
	
	
	/**
	 * Phone placement position
	 */
	public static final String[] PHONE_POSITIONS={
		"handbag", "knapsack", "legpocket", "coatpocket"
		,"handbag_indoor", "knapsack_indoor", "legpocket_indoor","coatpocket_indoor"
		, "all_position"};
	public static final int PHONE_POSITION_HANDBAG=0;
	public static final int PHONE_POSITION_KNAPSACK=1;
	public static final int PHONE_POSITION_LEGPOCKET=2;
	public static final int PHONE_POSITION_COATPOCKET=3;
	public static final int PHONE_POSITION_HANDBAG_INDOOR=4;
	public static final int PHONE_POSITION_KNAPSACK_INDOOR=5;
	public static final int PHONE_POSITION_LEGPOCKET_INDOOR=6;
	public static final int PHONE_POSITION_COATPOCKET_INDOOR=7;
	public static final int PHONE_POSITION_ALL=8;
	
	
	/**
	 * For audio signal
	 */
	public static final double HAMMING_WINDOW_ALPHA=0.53836;
	
	//dir setting
	public static final String AUDIO_BASE_DIR=DATA_BASE_DIR+"audio/";
	public static final String AUDIO_DATA_DIR=AUDIO_BASE_DIR+"raw/";
	public static final String AUDIO_FEATURES_DIR=AUDIO_BASE_DIR+"features/";
	public static final String AUDIO_FEATURES_TEST_DIR=AUDIO_FEATURES_DIR+"test/";
	public static final String AUDIO_FEATURES_TRAIN_DIR=AUDIO_FEATURES_DIR+"train/";
	
	/**
	 * For in/out door signal
	 */
	public static final String ENVIRONMENT_BASE_DIR=DATA_BASE_DIR+"environment/";
	
	
	/**
	 * data for ploting needs
	 */
	//dir setting

	
	/**
	 * Classifier setting
	 */
	public static final int CLASSIFIER_ACCEL_RAW=100;
	public static final int CLASSIFIER_ACCEL_CIV_FEATURE=101;
	public static final int CLASSIFIER_ACCEL_STATE=102;
	public static final int CLASSIFIER_AUDIO=103;
	public static final String[] CLASSIFIER_NAME = { "accelerometer_raw",	"accelerometer_CIV_feature",
		"accelerometer_state", "audio" };
	
	
	
}
