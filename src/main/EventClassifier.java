package main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.invoke.SwitchPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.misc.SerializedClassifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.SerializationHelper;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AddClassification;
import accelerometer.AccelerometerSignalProcessing;
import accelerometer.EventDetection;
import accelerometer.feature.AccelerometerFeature;

//reference: https://svn.cms.waikato.ac.nz/svn/weka/branches/stable-3-6/wekaexamples/src/main/java/wekaexamples/classifiers/WekaDemo.java
public class EventClassifier {
	/** input data format: either features or accelerometer*/	
	 protected static String fileDirectory;
	protected static String modelDirectory;
	
	 /** the classifier used internally */
	  protected static Classifier m_Classifier = null;
	  
	  /** the filter to use */
	  protected static Filter m_Filter = null;

	  /** the training files */
	  public static ArrayList<String> m_TrainingFiles=new ArrayList<String>();

	  /** the training instances */
	  protected static Instances m_Training = null;
	  

	  /** the test files */
	  public static ArrayList<String> m_TestFiles=new ArrayList<String>();
	  /** the test instances */
	  protected static Instances m_Test = null;

	  /** for evaluating the classifier */
	  protected static Evaluation m_Evaluation = null;
	  
	  

	  /**
	   * initializes the demo
	   */
	  public EventClassifier() {
	    super();
	  }

	  /**
	   * sets the classifier to use
	   * @param name        the classname of the classifier
	   * @param options     the options for the classifier
	   */
	  public static void setClassifier(String name, String[] options) throws Exception {
	    m_Classifier = Classifier.forName(name, options);
	    
	  }

	  /**
	   * sets the filter to use
	   * @param name        the classname of the filter
	   * @param options     the options for the filter
	   */
	  public static void setFilter(String name, String[] options){
		try{
		    m_Filter = (Filter) Class.forName(name).newInstance();
		    if (m_Filter instanceof OptionHandler)
		      ((OptionHandler) m_Filter).setOptions(options);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	  }

	  /**
	   * sets the file to use for training
	   */
	  public static void setTraining(ArrayList<String> trainingFiles) throws Exception {
	    for(String f: trainingFiles){
		    Instances ins   = new Instances( new BufferedReader(new FileReader(f)));
		    if(m_Training==null) m_Training=ins;
		    else{
		    	for(int i=0;i<ins.numInstances();i++) m_Training.add(ins.instance(i));
		    }
	    }
	    m_Training.setClassIndex(m_Training.numAttributes() - 1);
	  }
	  
	  /**
	   * sets the file to use for test
	   * @param noOfPrecedingLins: no of lines precede the "@relation" line
	   */
	  public static void setTest(ArrayList<String> testFiles) throws Exception {
		 for(String f: testFiles){
			//System.out.println(f);
			 BufferedReader br=new BufferedReader(new FileReader(f));
		 
			 br.readLine();//read off lines preceding @relation
			 Instances instances = new Instances(br);
			
			if(m_Test==null) m_Test=instances;
			else{ 
				for(int i=0;i<instances.numInstances();i++) m_Test.add(instances.instance(i));
			}
		 }
		 m_Test.setClassIndex(m_Test.numAttributes() - 1);
	  }
	  
	  /*
	   * build the classifier
	   */
	public static void runForTraining(Classifier mClassifier, Filter mFilter, Instances mInstances, String outputModelFilePath)
			throws Exception {
		
		//Code section 1 has the same effect as code section 2
		
		// Code section 1
		Instances filtered;
		if(mFilter!=null){
			mFilter.setInputFormat(mInstances);
			filtered = Filter.useFilter(mInstances, mFilter);		
		}else{
			filtered=mInstances;
		}
		System.out.println("# of classes in the training set is  : "+mInstances.numClasses()+"\n they are: "+mInstances.classAttribute().toString());
		
		//build and save the classifier		
		mClassifier.buildClassifier(filtered);	
		
		//Code section 2
		/*FilteredClassifier filteredClassifier = new FilteredClassifier(); 
		filteredClassifier.setFilter(m_Filter); 
		filteredClassifier.setClassifier(m_Classifier); 
		filteredClassifier.buildClassifier(m_Training); */
		
		
		SerializationHelper.write(outputModelFilePath, mClassifier);
	    System.out.println("File saved to "+ outputModelFilePath+"\n");
	    
		
		//10fold CV with seed=1
		/*m_Evaluation = new Evaluation(filtered);
	    m_Evaluation.crossValidateModel(m_Classifier, filtered, 10, m_Training.getRandomNumberGenerator(1));
	    */
	}	  
	
	
	
	public static void identifyIncorrectlyClassifiedInstances(Instances instances){
		try {
			int count=0;
			for (int i = 0; i < instances.numInstances(); i++) {
				Instance in = instances.instance(i);
				double pred = in.value(instances.numAttributes()-1);
                double actual = in.value(instances.numAttributes()-2);
                if(pred!=actual) count+=1;
			}
			System.out.println("Num of incorrectly classified instances "+ count);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	  /**
	   * execute the classifier
	   * @param classifierAlgorithm: e.g. RandomForest
	   * @param pathOfFeatureFile: path of feature File to be classified
	   */
	  public static void runForClassifying(int classifierType, String classfierAlgorithm, String pathOfFeatureFile) throws Exception {
	   
		String classfierModelPath = modelDirectory + classfierAlgorithm+ ".model";
		
		System.out.println("********"+pathOfFeatureFile+"********");
		System.out.println("m_Test size = "+m_Test.numInstances()+"  # of attrs="+ m_Test.numAttributes()+"  cls attr idx=" + m_Test.classIndex());
		//System.out.println(m_Filter.toString());
		Instances filteredTestInstances;
		if(m_Filter!=null){
			m_Filter.setInputFormat(m_Test);
			filteredTestInstances = Filter.useFilter(m_Test, m_Filter);
		}else{
			filteredTestInstances=m_Test;
		}
		Classifier mClassifier = (Classifier)SerializationHelper.read(classfierModelPath);
		
		
		Scanner sc;
		Instances copy;
		Attribute classAttr;
		switch(classifierType){
		case Constants.CLASSIFIER_ACCEL_RAW:
			// String evalationOptions="-l "+classfierModelPath+" -T "+m_TestFiles.get(0);
			// System.out.println(evalationOptions);
			// Evaluation.evaluateModel("weka.classifiers.trees.RandomForest",
			// evalationOptions.split(" "));
			
			
			String activityFile=Constants.ACCELEROMTER_ACTIVITY_WEKA_DIR
					+ CommonUtils.getFileName(m_TestFiles.get(0)).replaceAll(
							"accelerometer", "activity");
			System.out.println(activityFile);
			outputClassificationResult(activityFile, filteredTestInstances, m_Test);
			break;
		case Constants.CLASSIFIER_ACCEL_STATE:
			break;
		case Constants.CLASSIFIER_ACCEL_CIV_FEATURE:
			System.out.println("# of attrs="+ filteredTestInstances.numAttributes()+"  cls attr idx=" + filteredTestInstances.classIndex());
			
			/*AddClassification addClass = new AddClassification();
			addClass.setClassifier(classifier);
			addClass.setOutputClassification(true);
			addClass.setOutputErrorFlag(true);
			*/
			
			/*FilteredClassifier filteredClassifier = new FilteredClassifier(); 
			filteredClassifier.setFilter(m_Filter); 
			filteredClassifier.setClassifier(classifier); */
			
			sc=new Scanner(new File(pathOfFeatureFile));
			copy=new Instances(m_Test);
			copy.delete();
			//m_Test.delete();
			classAttr=m_Test.attribute(m_Test.numAttributes()-1);
			
			
			String dateSeq="";
			while(sc.hasNextLine()){
				String features=sc.nextLine().trim();
				if(!features.contains("@")&&features.contains(",")){//a feature line
					String[] fields=features.split(",");
					int n=fields.length;		
					
					Instance in = new Instance(n);//class 
					// Set instance's values
					for(int i=0;i<n-1;i++){
						in.setValue(i, Double.parseDouble(fields[i].trim()) );
					}		
					//in.setDataset(copy);
					//in.setClassMissing();//set the class attribute missing
					
					//in.setClassValue(fields[n-1]);
					//m_Test.delete();// remove the last instance
					
					Instances ins=new Instances(copy);
					ins.add(in);
					//System.out.println("Before Filter: "+in.toString());
					
					//m_Filter.setInputFormat(ins);
					Instances filterInstances=Filter.useFilter(ins, m_Filter);
					Instance filteredInst=filterInstances.instance(0);
					//System.out.println("After Filter: "+filteredInst.toString());
					
					//addClass.setInputFormat(filterInstances);
					//filterInstances=Filter.useFilter(removedFilteredTestInstances, addClass);
					//String pred=filteredInst.stringValue(filteredInst.numAttributes()-2);
					//double[] fDistribution = classifier.distributionForInstance(filteredInst);
					//String pred=removedFilteredTestInstances.classAttribute().value((int)classifier.classifyInstance(filteredInst));
					String pred=classAttr.value((int)mClassifier.classifyInstance(filteredInst));
					
					if(!pred.equals("n")){
						//System.out.println(CommonUtils.secondsToHMS((int)in.value(0))+" pred="+pred+" filteredInst="+filteredInst.toString());
					}
					
					int secondsOfADay= (int) in.value(0);
	                
					//System.out.println(secondsOfADay+"  "+pred);
					dateSeq=CommonUtils.getFileName(pathOfFeatureFile).replace("ACCELEROMETER_FEATURE_", "").replace(".arff", "");
					if(pred.equals("p") ){
	                	AccelerometerSignalProcessing.detectedEvents.get(Constants.PARKING).add(dateSeq+"-"+CommonUtils.secondsToHMS(secondsOfADay));
					}else{
						if(pred.equals("u")){
							//System.out.println(ParkSense.detectedEvents.size());
							AccelerometerSignalProcessing.detectedEvents.get(Constants.UNPARKING).add(dateSeq+"-"+CommonUtils.secondsToHMS(secondsOfADay));
						}
					}
					
				}
			}
			sc.close();
			
			// output the classification class
			/*AddClassification addClass = new AddClassification();
			addClass.setClassifier(classifier);
			addClass.setOutputClassification(true);
			addClass.setInputFormat(removedFilteredTestInstances);
			removedFilteredTestInstances=Filter.useFilter(removedFilteredTestInstances, addClass);
			for (int i = 0; i < removedFilteredTestInstances.numInstances(); i++) {
				Instance in= removedFilteredTestInstances.instance(i);
				String pred=in.stringValue(in.numAttributes()-1);
			
				if(!pred.equals("n"))
					System.out.println("pred="+pred+" filteredInst="+in.toString());
				
				
				int secondsOfADay= (int) m_Test.instance(i).value(0);
	            //System.out.println(secondsOfADay+"  "+pred);
				if(pred.equals("p") ){
	            	AccelerometerSignalProcessing.detectedEvents.get(Constants.PARKING).add(fileNameInDateFormat+"-"+CommonUtils.secondsToStringTime(secondsOfADay));
				}else{
					if(pred.equals("u")){
						//System.out.println(ParkSense.detectedEvents.size());
						AccelerometerSignalProcessing.detectedEvents.get(Constants.UNPARKING).add(fileNameInDateFormat+"-"+CommonUtils.secondsToStringTime(secondsOfADay));
					}
				}
			}*/
			
			if(EventDetection.groundTruth.get(dateSeq)!=null){
				//Print out the ground truth and detected events
				for(int i=0;i<2;i++){
					// ground truth 
					ArrayList<String> timestamps=EventDetection.groundTruth.get(dateSeq).get(i);
					if(i==Constants.PARKING) System.out.println("Groundtruth Parking events:");
					else System.out.println("Groundtruth Unparking events:");
					for(String timestamp: timestamps){
						String time=timestamp.split("-")[1];
						System.out.printf("%s(%5d), ", time, CommonUtils.HMSToSeconds(time));
					}
					System.out.println();
					
					//detected
					if(i==Constants.PARKING) System.out.println("Deteced Parking events:");
					else System.out.println("Deteced Unparking events:");
					for(String timestamp: AccelerometerSignalProcessing.detectedEvents.get(i)){
						if(timestamp.contains(dateSeq)){
							String time=timestamp.split("-")[1];
							System.out.printf("%s(%5d), ", time, CommonUtils.HMSToSeconds(time));
						}
					}
					System.out.println();
				}
			}
			
			break;
		case Constants.CLASSIFIER_AUDIO:
			break;
		default:
			System.err.println("Invalid option");
			break;
		}
		

	  
	  }
	  
	  private static void outputClassificationResult(String outputFile, Instances outputAfterClassification, Instances inputBeforeFilter){
		  try{
			FileWriter fw = new FileWriter(outputFile);
			for (int i = 0; i < outputAfterClassification.numInstances(); i++) {
				Instance in = outputAfterClassification.instance(i);
				Instance original= inputBeforeFilter.instance(i);
				fw.write(original.stringValue(0)+","+original.stringValue(1)+","+in.toString()+"\n");
			}
			fw.close();
		  }catch(Exception ex){
			  ex.printStackTrace();
		  }
	  }

	  
	  private static String getAllFileNamesOnly(ArrayList<String> files){
		  StringBuilder sBuilder=new StringBuilder();
		  for(String path: files) sBuilder.append(CommonUtils.getFileName(path)+"\t");
		  return sBuilder.toString();
	  }
	  
	  /**
	   * outputs some data about the classifier
	   */
	  public static String printClassifierModel(String classifierModel) {
	    StringBuffer  result;

	    result = new StringBuffer();
	    result.append("\n****************Info. About the Trained/Imported Classifier Model*******\n===========\n\n");

	    result.append("Classifier...: " 
	        + classifierModel+ "\n");
/*	        + m_Classifier.getClass().getName() + " " 
	        + Utils.joinOptions(m_Classifier.getOptions()) + "\n");*/
		if (m_Filter != null) {
			if (m_Filter instanceof OptionHandler)
				result.append("Filter.......: "
						+ m_Filter.getClass().getName()
						+ " "
						+ Utils.joinOptions(((OptionHandler) m_Filter)
								.getOptions()) + "\n");
			else
				result.append("Filter.......: " + m_Filter.getClass().getName()+ "\n");
		}
	    result.append("Training files: " 
	        + getAllFileNamesOnly(m_TrainingFiles)+ "\n");
	    result.append("Test files: " 
		        + getAllFileNamesOnly(m_TestFiles) + "\n");

	    //result.append(m_Classifier.toString() + "\n");

		try {
			if(m_Evaluation!=null){
				result.append(m_Evaluation.toSummaryString() + "\n");
				result.append(m_Evaluation.toMatrixString() + "\n");
				result.append(m_Evaluation.toClassDetailsString() + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		result.append("*************************************************************************************\n");
	    return result.toString();
	  }
	  
	  
	public static HashMap<String, String> wekaClassifiers = new HashMap<String, String>();
	 
	/**
	 * @param training: 
	 * 			true: training
	 * 			false: classifying
	 * @param classifierType: define the type of features of the classifier
	 * @param filePaths:  paths for files that are used to be trained or classified
	 * @param classifierAlgorithm: e.g. RandomForest
	 * @param phonePositionDirIdx: only required when classifierType==Constants.CLASSIFIER_ACCEL_CIV_FEATURE
	 */
	
	public static void run(boolean training, int classifierType, 
			String[] filePaths, String classifierAlgorithm, int phonePositionDirIdx){
		// specify the classifier
		if (wekaClassifiers.size() == 0) {
			wekaClassifiers.put("RandomForest", "weka.classifiers.trees.RandomForest");
			wekaClassifiers.put("NBTree", "weka.classifiers.trees.NBTree");
			wekaClassifiers.put("J48", "weka.classifiers.trees.J48");
			wekaClassifiers.put("NaiveBayes","weka.classifiers.bayes.NaiveBayes");
			wekaClassifiers.put("RandomCommittee", "weka.classifiers.meta.RandomCommittee");
		}
		
		
		//setup
		String filter=null, filterOption=null;
		String outputModelFilePath="";		
		
		switch(classifierType){
		case Constants.CLASSIFIER_ACCEL_RAW:
			fileDirectory=Constants.ACCELEROMETER_RAW_DATA_DIR;
			filter = "weka.filters.unsupervised.attribute.Remove";
			filterOption= "-R 1,2";
			break;
		case Constants.CLASSIFIER_ACCEL_CIV_FEATURE: //accelerometer CIV features
			fileDirectory=Constants.ACCELEROMETER_CIV_FEATURES_DIR+Constants.PHONE_POSITIONS[phonePositionDirIdx]+"/";
			filter = "weka.filters.unsupervised.attribute.Remove";
			filterOption="-R 1,4,5,9,10";
			break;
		case Constants.CLASSIFIER_ACCEL_STATE:
			fileDirectory=Constants.ACCELEROMETER_STATE_FEATURES_DIR;
			filter = "weka.filters.unsupervised.attribute.Remove";
			filterOption="-R 1";
			break;
		case Constants.CLASSIFIER_AUDIO:
			fileDirectory=Constants.AUDIO_FEATURES_DIR;
			break;
		default:
			System.err.println(classifierType+" is not a valid option");
			break;
		}
		modelDirectory=fileDirectory+"model/";
		outputModelFilePath=modelDirectory+classifierAlgorithm+".model";
		
		//System.out.println(fileDirectory);
		if(training) fileDirectory+="train/";
		else fileDirectory+="test/";
		
		String wekaClassifierClass=wekaClassifiers.get(classifierAlgorithm);		
		String classifierOption ="";			
				
		// run
		try{
			setClassifier(wekaClassifierClass, classifierOption.split(" "));
			if(filter!=null) setFilter(filter, filterOption.split(" "));
			
			if(training){//training
				m_Training=null;
				for (String f : filePaths){
					m_TrainingFiles.clear();
					m_TrainingFiles.add(f); 
				}
				setTraining(m_TrainingFiles);
				runForTraining(m_Classifier, m_Filter, m_Training, outputModelFilePath);
			}else{//classify
				switch(classifierType){
				case Constants.CLASSIFIER_AUDIO:
					runForClassifying(classifierType, classifierAlgorithm, null);
					break;
				case Constants.CLASSIFIER_ACCEL_CIV_FEATURE:
				case Constants.CLASSIFIER_ACCEL_STATE:
					//classify all files
					for(String filepath: filePaths){
						
						//set the test instances
						m_TestFiles.clear(); //clear 
						System.out.println(CommonUtils.getFileName(filepath));
						m_TestFiles.add(filepath);
						m_Test=null; //clear
						setTest(m_TestFiles);
						runForClassifying(classifierType, classifierAlgorithm, filepath);
					}
					break;
				}
				
			}
			//System.out.println(printClassifierModel(classfierModel));
		}catch(Exception ex){
			ex.printStackTrace();
		}		
	}
		
	public static void compareMultipleClassifyingAlgoritms(int classifierType, int phonePositionDirIdx, String trainingFilePath) {
		String[] models = { "RandomForest" };// , "NBTree", "J48", "NaiveBayes",
												// "RandomCommittee" };
		String classifierModel;

		for (int i = 0; i < models.length; i++) {
			classifierModel = models[i];
			System.out
					.println("********** " + classifierModel + " ********** ");
			run(true, classifierType, new String[] { trainingFilePath }, classifierModel, phonePositionDirIdx);
		}
	}


	public static void classifySingleInstance(int classifierType){
		
		
		try {
			String inputModelFile=null;
			String inputHeaderFile=null;
			String inst="";
			Filter  filter=null;
			
			
			switch(classifierType){
			case Constants.CLASSIFIER_ACCEL_CIV_FEATURE:
				inst="67309,-0.081,-2.83,-0.071,-1.355,5.963,0.942,-9.871,5.957,-9.902";
				inputModelFile=Constants.ACCELEROMETER_CIV_FEATURES_DIR+"all_position/model/RandomForest.model";
				inputHeaderFile=Constants.ACCELEROMETER_CIV_FEATURES_DIR+"arff_header.txt";
				filter = (Filter) Class.forName("weka.filters.unsupervised.attribute.Remove").newInstance();
				if (filter instanceof OptionHandler)
			      ((OptionHandler) filter).setOptions("-R 1".split(" "));
				break;
			case Constants.CLASSIFIER_ACCEL_STATE:
				inst="0.13,0.25,0.29,0.24,0.09,0.01,0.09,0.38,0.42,0.11,0.00,0.08,0.77,0.14,0.00,0.89,1.63,9.23,1.19,0.47,0.84,7.61";
				inputModelFile=Constants.ACCELEROMETER_BASE_DIR+"motion_state/model/RandomForest.model";
				inputHeaderFile=Constants.ACCELEROMETER_BASE_DIR+"motion_state/state_arff_header.txt";
			}
			
			Classifier classifier = (Classifier) SerializationHelper.read(new FileInputStream(inputModelFile));
			Instances instances=new Instances(new BufferedReader(new FileReader(inputHeaderFile)));			
			instances.setClassIndex(instances.numAttributes()-1);
			
			classifySingleInstance(classifier, instances, filter, inst);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param inputFileNameInDateFormat
	 * @param features
	 * @return vectors of MotionStateTransition Indicator
	 */
	public static ArrayList<String> classifyMotionStates(String inputFileNameInDateFormat,ArrayList<AccelerometerFeature> features){
		ArrayList<String> vectorsOfMSTIndicator=new ArrayList<String>();
		try{
			//build an instance and classify
			Classifier mClassifier = (Classifier)SerializationHelper.read(
					Constants.ACCELEROMETER_STATE_FEATURES_DIR+"model/RandomForest.model");
			System.out.println("Loaded Classifier:");
			System.out.println(mClassifier.toString());
			
			Instances template=new Instances(new BufferedReader(
					new FileReader(Constants.ACCELEROMETER_STATE_FEATURES_DIR+"state_arff_header.txt")));
			template.setClassIndex(template.numAttributes()-1);
			Attribute classAttr=template.classAttribute();
			System.out.println("class attribute:"+classAttr.toString());
			//System.out.println("no of attrs="+template.numAttributes()+" " +"classattr="+classAttr);
			
			String prevState="";
			double[] probDistri=null, prevProbDistri=null;
			String vectorOfIndicator;
			
			
			for(AccelerometerFeature feature: features){
				String[] fields=feature.asStringForMotationState().split(",");
				int n=fields.length;
				
				Instance in = new Instance(n+1);//class
				// Set instance's values
				for(int i=0;i<n;i++){
					in.setValue(i, Double.parseDouble(fields[i].trim()) );
				}		
				in.setDataset(template);
				//in.setClassMissing();//set the class attribute missing
				
				//System.out.println("# of classes for this instance is "+in.numClasses());
				int secondsOfADay=feature.timeIndex;
				
				
				int classIdx=(int)mClassifier.classifyInstance(in);
				String curState=classAttr.value(classIdx);
				probDistri=mClassifier.distributionForInstance(in);
				/*System.out.println("time : " +CommonUtils.secondsToHMS(secondsOfADay)
						+" most likely state : "+curState+" last State : "+prevState
						+" distribution: "+Arrays.toString(probDistri) );
				*/
				
				if(prevProbDistri!=null){
					vectorOfIndicator=CommonUtils.secondsToHMS(secondsOfADay)
							+","+prevProbDistri[0]+","+prevProbDistri[1]
							+","+probDistri[0]+","+probDistri[1];				
					//System.out.println(vectorOfIndicator);
					vectorsOfMSTIndicator.add(vectorOfIndicator);
				}
				prevProbDistri=Arrays.copyOf(probDistri, probDistri.length);
				
				
				
				//System.out.println(secondsOfADay+"  "+pred);
				HashSet<String> drivingStateClass=new HashSet<String>();
				drivingStateClass.add("Driving");
				//drivingStateClass.add("Still");
				
				HashSet<String> walkingStateClass=new HashSet<String>();
				//walkingStateClass.add("Jogging");
				walkingStateClass.add("Walking");
				//walkingStateClass.add("Upstairs");
				//walkingStateClass.add("Downstairs");
				
				//add detected events
				if(walkingStateClass.contains(curState)&&drivingStateClass.contains(prevState)){
						System.out.println("Parking 	time:"+CommonUtils.secondsToHMS(secondsOfADay)+"	"+prevState+"--->"+curState);
						AccelerometerSignalProcessing.detectedEvents.get(Constants.PARKING).add(inputFileNameInDateFormat+"-"+CommonUtils.secondsToHMS(secondsOfADay));
				}else{
					if(drivingStateClass.contains(curState)&&walkingStateClass.contains(prevState)){
						System.out.println("Unparking 	time:"+CommonUtils.secondsToHMS(secondsOfADay)+"	"+prevState+"--->"+curState);
						AccelerometerSignalProcessing.detectedEvents.get(Constants.UNPARKING).add(inputFileNameInDateFormat+"-"+CommonUtils.secondsToHMS(secondsOfADay));
					}
				}
				prevState=curState;
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}	
		return vectorsOfMSTIndicator;
	}
	
	/**
	 * @param mClassifier:
	 * @param mInstances:
	 * @param mFilter:   
	 * @param features:  string representation of features without class field
	 */
	public static void classifySingleInstance(Classifier mClassifier, Instances mInstances, Filter mFilter, String features){
		String[] fields=features.split(",");
		int n=fields.length;		
		
		//Log.e(LOG_TAG, "n="+n+"  "+features);
		
		Instance in = new Instance(n+1);//class 
		in.setDataset(mInstances); //necessary under android; removable under Windows
		// Set instance's values
		for(int i=0;i<n;i++){
			//Attribute attr=mInstances.attribute(i);
			in.setValue(i, Double.parseDouble(fields[i].trim()) );
			//System.out.println(Double.parseDouble(fields[i])+" "+inst.value(i));
		}		
		//in.setClassMissing();
		
		Instances ins;
		ins=new Instances(mInstances);
		ins.add(in); //add the new instance

		try {
			Instances filterInstances;
			Instance filteredInst;
			if(mFilter!=null){
				mFilter.setInputFormat(mInstances);	//necessary under android; removable under Windows
				filterInstances = Filter.useFilter(ins, mFilter);
				filteredInst=filterInstances.instance(0);
			}else {
				filterInstances=ins;
				filteredInst=in;
			}
			
			//Log.e(LOG_TAG, "after fileter:  # of attributes: " + filterInstances.numAttributes()+"  cls attr idx=" + filterInstances.classIndex());
			//Log.e(LOG_TAG, "after fileter classification inst= "+ filteredInst.toString()); 
			
			int predClass=(int)mClassifier.classifyInstance(filteredInst);
		
			double[] probs=mClassifier.distributionForInstance(filteredInst);
			System.out.println(Arrays.toString(probs) );
			System.out.println(predClass+" "+mInstances.classAttribute().value(predClass));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	/**
	 * classify a single file
	 */
	public static void classifySingleFile(){
		
		AccelerometerSignalProcessing.detectedEvents.add(new ArrayList<String>());
		AccelerometerSignalProcessing.detectedEvents.add(new ArrayList<String>());
		int phonePosIdx=Constants.PHONE_POSITION_ALL;
		run(false,Constants.CLASSIFIER_ACCEL_CIV_FEATURE, 
				new String[]{"ACCELEROMETER_FEATURE_2013_11_201.arff"},
				"RandomForest",
			phonePosIdx);		
	}
	
	
	public static void main(String[] args) throws Exception {
		/**
		 * train a classifier
		 */
		run(true, Constants.CLASSIFIER_ACCEL_STATE,
				new String[]{Constants.ACCELEROMETER_BASE_DIR+"motion_state/state_combined.arff"} ,
				"RandomForest", -1);
	
		
		//classifySingleFile();

		//classifySingleInstance(Constants.CLASSIFIER_ACCEL_CIV_FEATURE);//Constants.CLASSIFIER_ACCEL_STATE);
		
		/*String className="weka.classifiers.trees.RandomForest";		
		System.out.println(
				SerializationHelper.getUID(className)+
				" "+SerializationHelper.hasUID(className)+
				" "+SerializationHelper.needsUID(className));		
		long uid=-1129603696879962476L;
		System.out.println(uid);*/
	}


}
