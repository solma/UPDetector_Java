package accelerometer;

public class Config {
	
	/*
	 * Parameters for feature extraction
	 */	
		
		//length of window in secs
		public int windowSize;  
		
		public int slidingStep; //the stpe size at which the window slides
		
		public int noOfBins;
		
		public int scope; //# of preceding and succeeding windows
		
		//if the no. of accelerometer readings in any half is below the threshold, the window is not output for a feature
		public int minNoOfSamplesInHalfWindow;
		
		//if the # of readings in the window below the threshold, the window is not output for a feature
		public int minNoOfSamplesInWholeWindow;

		public boolean motionStateFeature; //true if extracting features for motion state classifying 
		public boolean singleMotionStateOnly; //true if extracting motion state features for driving state only
		
		// used which axes, 0:X, 1:Y, 2:Z, 3:Aggregate
		public int[] AXES;
		
		
		//IO directory settings
		public int phonePlacementPosition;
	/*
	 * 	
	 */
		
	public static int WINDOW_SIZE=10;
	public static int SCOPE=6; //to be same as in android app
	public static int NO_OF_BINS=5;// for motion state classification features
	public static int SLIDING_STEP=3; //in seconds	
	
	
	public Config(int phonePlacementPosition){
		this();		
		this.phonePlacementPosition=phonePlacementPosition;
	}
	
	public Config(){
		//TODO change those variables
		windowSize=WINDOW_SIZE; 		
		scope=SCOPE;
		slidingStep=SLIDING_STEP;
		
		noOfBins=NO_OF_BINS;
		
		minNoOfSamplesInHalfWindow=1;
		minNoOfSamplesInWholeWindow=minNoOfSamplesInHalfWindow;
		
		AXES=new int[]{0, 1, 2, 3};
	}
	
	public Config(int windowSize, int slidingStep){
		this();
		this.windowSize=windowSize;
		this.slidingStep=slidingStep;
	}
	
	public Config(int windowSize, int slidingStep, boolean motionStateFeature){
		this(windowSize, slidingStep);
		this.motionStateFeature=motionStateFeature;
	}
}
