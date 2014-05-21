package accelerometer.windowfeature;

import helper.CommonUtils;
import helper.Constants;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;


import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import accelerometer.AccelerometerFileProcessing;
import accelerometer.AccelerometerSignalProcessing;
import accelerometer.Config;

import com.google.common.primitives.Doubles;

import fusion.Fusion;
import fusion.IndicatorVector;


public class WindowFeatureExtraction {
	public static void main(String[] args){
		Config config=new Config();
		config.scope=50;
		//config.minNoOfSamplesInWholeWindow=200;
		WindowFeatureExtraction fe=new WindowFeatureExtraction(config, new File(Constants.ACCELEROMETER_RAW_DATA_DIR+"all_position/ACCELEROMETER_RAW_2013_10_251.log") );
		System.out.println("step 1:  --> Extract Features and output the ARFF File");
		
	}
	
	public Config conf;
	
	//input minimally-labeled accelerometer file 
	public File inputFile;
	
	//store labeld lines in the input minimally-labeld accelerometer file
	public HashMap<Integer, String> eventsTimestamps;
	
	private static Variance var=new Variance();//obj to calculate var
	private static Mean mean=new Mean();//obj to calculate mean
	
	public WindowFeatureExtraction(Config conf, File inputFile){
		this.inputFile=inputFile;
		this.conf=conf;
		eventsTimestamps=new HashMap<Integer, String>();
	}	

	/*if(conf.singleMotionStateOnly){
		outputFilePath=Constants.ACCELEROMETER_STATE_FEATURES_DIR+"driving.arff";
	}else{
		outputFilePath=Constants.ACCELEROMETER_STATE_FEATURES_DIR+inputFile.getName().replace("RAW", "STATE_FEATURE").replace(".log", ".arff");;
	}
	
	try{				
		if(conf.singleMotionStateOnly){
			//save features to a file
			FileWriter fw=new FileWriter(outputFilePath, true);
			for(WindowFeature feature: features){
				fw.append(feature.asMotionStateFeatures()+",Still\n");
			}
			fw.close();
		}
	}catch(Exception ex){
		ex.printStackTrace();
	}*/

	public void saveIndicatorVectorToFile(ArrayList<IndicatorVector> indicatorVectors, String dateSeq){
		String outputFilePath=Constants.ACCELEROMETER_FUSION_DIR;
		/*//outputFilePath=Constants.ACCELEROMETER_CIV_FEATURES_DIR+Constants.PHONE_POSITIONS[conf.phonePlacementPosition]+"/";
		//determine if this file is in the test folder or train folder
		System.out.println(outputFilePath+"test/"+outputFileName);
		if(new File(outputFilePath+"test/"+outputFileName).exists()){
			outputFilePath+="test/";
		}else{
			if(new File(outputFilePath+"train/"+outputFileName).exists()){
				outputFilePath+="train/";
			}
		}*/
		
		outputFilePath+=dateSeq+"_CIV.log";			
		
		//ArrayList<IndicatorVector> indicatorVectors=outputCIVVectors(features, outputFilePath, inputFile);
		try{
			FileWriter fw=new FileWriter(outputFilePath);
			for(IndicatorVector iv: indicatorVectors){
				fw.write(iv.toString()+"\n");
				//System.out.println(iv);
			}
			fw.close();
			System.out.println("indicator vectors save to "+outputFilePath);
		}catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	public class Window{
		//temporal boundary of the window
		int[] bounds;
		//recording the content in the window
		ArrayList<ArrayList<Double>> acclReadings;
		//recording the no of samples for each time index, i.e. second in the window
		ArrayList<ArrayList<Integer>> noOfSamplesInASecond; //1st no. is time index, 2nd no. is cnt
		
		public Window(){
			bounds=new int[]{-1, -1};
			acclReadings=new ArrayList<ArrayList<Double>>(Constants.AXIS_NUMBER);
			for(int i=0;i<Constants.AXIS_NUMBER;i++) acclReadings.add(new ArrayList<Double>());
			noOfSamplesInASecond=new ArrayList<ArrayList<Integer>> ();
		}
		
		public void setBounds(int[] bounds){
			this.bounds[0]=bounds[0];
			this.bounds[1]=bounds[1];			
		}
	}

	
	/**
	 * 
	 * @param inputFile: raw accelerometer file
	 * @return
	 */
	public ArrayList<WindowFeature> run(){
		ArrayList<WindowFeature> features=new ArrayList<WindowFeature>();
		try{
			Scanner sc=new Scanner(inputFile);
			
			Window window=new Window();	
			int[][] driving_periods=new int[2][];//starting and ending timestamps of the driving periods
			while(sc.hasNextLine()){
				String line=sc.nextLine().trim();
				String[] fields=line.split(" ");
				
				if(fields.length<3) continue;
					
				if(!line.startsWith("@")){//not ground truth lines
					int curTime=CommonUtils.HMSToSeconds(fields[1]);
					
					if(conf.singleMotionStateOnly==true){
						boolean isDrivingline=false;
						//skip all line that is not in driving state
						for(int i=0;i<driving_periods.length;i++){
							if(curTime>driving_periods[0][i]+1&&curTime<driving_periods[1][i]-1){
								isDrivingline=true;
								break;
							}							
						}
						if(isDrivingline==false) continue;
					}
					
					//labeled line
					if(fields.length==7){
						eventsTimestamps.put(CommonUtils.HMSToSeconds(fields[1]), fields[fields.length-1]);
					}
					
					//if window not initialized
					if(window.bounds[0]==-1){
						window.setBounds(new int[]{curTime, curTime+conf.windowSize});
					}
					
					//update the window
					slideWindow(window, curTime,features, fields);
				}else{//ground truth lines
					//store and print the groudtruth periods for driving state
					if(line.contains("in_vehicle")){
						System.out.println("************"+inputFile.getName()+"*****************");
						String[] periods=line.trim().split(" ");
						int offset=2;
						driving_periods[0]=new int[periods.length-offset];
						driving_periods[1]=new int[periods.length-offset];
						for(int i=0;i<periods.length;i++){
							String duration=periods[i];
							if(duration.contains("~")){
								String[] stamps=duration.split("~");
								for(int j=0;j<2;j++){
									driving_periods[j][i-offset]=CommonUtils.HMSToSeconds(stamps[j]);
									//System.out.println(stamps[j]+" "+driving_periods[j][i-offset]);									
								}
							}
						}
						//System.out.println();
					}
				}
				
			}
			sc.close();
			AccelerometerSignalProcessing.duration.add(inputFile.getCanonicalPath(), features.get(features.size()-1).timeIndex-features.get(0).timeIndex);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		System.out.println(features.size()+" windows extracted. ");
		
		return features;
	}
	
	/**
	 * @param window: 
	 * @param curTime: time in seconds
	 * @param features: save the new feature to the list 
	 * @param fields: the readings of the three axes
	 * @return the extracted feature for the current window
	 */	
	private WindowFeature slideWindow(Window window, int curTime, ArrayList<WindowFeature> features, String[] fields){
		WindowFeature newFeature=null;
	
		if(curTime-window.bounds[0]>conf.windowSize){	//if the window slides
			//System.out.println("Window: "+ Arrays.toString(window.bounds));
			//System.out.println(window.acclReadings.get(3).toString()+"\n");
			
			//extract the feature for the current window
			newFeature=extractFeatureForCurrentWindow(window);
			int noOfFeatures=features.size();
			if(newFeature!=null&& (noOfFeatures==0||!features.get(noOfFeatures-1).equals(newFeature)) ){
				features.add(newFeature);
			}
			
			//update the bounds
			//TODO Options
				/****** old sliding method ****/
				//window.setBounds(new int[]{curTime-conf.windowSize, curTime});
					/** equvalent codes ***/
				/*	if(curTime-window.bounds[1]>1) window.setBounds(new int[]{curTime-conf.windowSize, curTime});
					else  window.setBounds(new int[]{window.bounds[0]+1, window.bounds[1]+1});*/
				
				/****** new sliding method ****/
				if(curTime-window.bounds[1]>conf.slidingStep) window.setBounds(new int[]{curTime-conf.windowSize, curTime});
				else  window.setBounds(new int[]{window.bounds[0]+conf.slidingStep, window.bounds[1]+conf.slidingStep});
				
			//remove expired readings
			int noOfRemoveTimestamps=0;
			for(ArrayList<Integer> count: window.noOfSamplesInASecond){
				int timeIdx=count.get(0);
				int cnt=count.get(1);
				if(timeIdx>=window.bounds[0]) break;
				
				noOfRemoveTimestamps+=1;
				for(int axisIdx=0;axisIdx<Constants.AXIS_NUMBER; axisIdx++){
					for(int j=0;j<cnt;j++){
						window.acclReadings.get(axisIdx).remove(0);
					}
				}
			}
			for (int i = 0; i < noOfRemoveTimestamps; i++) {
				window.noOfSamplesInASecond.remove(0);
			}
		}
		
		//add the new accelerometer readings
		for(int axisIdx=0;axisIdx<Constants.AXIS_NUMBER;axisIdx++){
			window.acclReadings.get(axisIdx).add(Double.parseDouble(fields[2+axisIdx]));
		}		
		
		//count the no of samples for this time index
		int idxOfLastTimestamp=window.noOfSamplesInASecond.size()-1;
		//if a new time index
		if(idxOfLastTimestamp==-1||window.noOfSamplesInASecond.get(idxOfLastTimestamp).get(0)!=curTime){
			window.noOfSamplesInASecond.add(new ArrayList<Integer>()); 
			idxOfLastTimestamp=window.noOfSamplesInASecond.size()-1; 
			window.noOfSamplesInASecond.get(idxOfLastTimestamp).add(curTime); //add timestamp
			window.noOfSamplesInASecond.get(idxOfLastTimestamp).add(1); //only 1 sample in this second
		}else{ // a old time index
			int cnt=window.noOfSamplesInASecond.get(idxOfLastTimestamp).get(1);
			//update the count
			window.noOfSamplesInASecond.get(idxOfLastTimestamp).remove(1);
			window.noOfSamplesInASecond.get(idxOfLastTimestamp).add(cnt+1);
		}
		return newFeature;		
	}
	
	private  WindowFeature extractFeatureForCurrentWindow(Window window){
		//initialize a feature
		WindowFeature feature=new WindowFeature(window.bounds);
		//add the content to the feature 
		for(int axisIdx=0;axisIdx<Constants.AXIS_NUMBER; axisIdx++){
			ArrayList<Double> axisAccl=window.acclReadings.get(axisIdx);
			if(axisAccl.size()<conf.minNoOfSamplesInWholeWindow) return null;
			
			int centerTimeIdxOftheWindow=feature.timeIndex;
			int noOfSamplesInFirstHalf=0;
			for(ArrayList<Integer> cnt: window.noOfSamplesInASecond){
				if(cnt.get(0)>centerTimeIdxOftheWindow) break;
				noOfSamplesInFirstHalf+=cnt.get(1);
			}			
			//add the first half average
			List<Double> firstHalfOfWindow=axisAccl.subList(0, noOfSamplesInFirstHalf); 
				//if the first half of window is empty; then discard this window
				if(firstHalfOfWindow.size()<conf.minNoOfSamplesInHalfWindow) return null; 
				double avgValue=mean.evaluate(Doubles.toArray(firstHalfOfWindow));
				feature.averageSeries.get(axisIdx).add(avgValue);
				//add the first half variance
				double varValue=var.evaluate(Doubles.toArray(firstHalfOfWindow), avgValue);
				feature.varianceSeries.get(axisIdx).add(varValue);
			
			//add the second half average
			List<Double> secondHalfOfWindow=axisAccl.subList(noOfSamplesInFirstHalf, axisAccl.size());
				//if the second half of window is empty; then discard this window
				if(secondHalfOfWindow.size()<conf.minNoOfSamplesInHalfWindow) return null;
				avgValue=mean.evaluate(Doubles.toArray(secondHalfOfWindow));
				feature.averageSeries.get(axisIdx).add(avgValue);
				//add the second half variance
				varValue=var.evaluate(Doubles.toArray(secondHalfOfWindow), avgValue);
				feature.varianceSeries.get(axisIdx).add(varValue);
				
			//add the whole window
			avgValue=mean.evaluate(Doubles.toArray(axisAccl));
			feature.averageSeries.get(axisIdx).add(avgValue);
			varValue=var.evaluate(Doubles.toArray(axisAccl), avgValue);
			feature.varianceSeries.get(axisIdx).add(varValue);
			
			/**
			 * calculate additional fields of feature for classifying motion states
			 */
			//binPecents
			double max=Integer.MIN_VALUE, min=Integer.MAX_VALUE;
			for(Double d: axisAccl){
				max=Math.max(max, d);
				min=Math.min(min, d);
			}
			double[] lowerBoundOfBins=new double[conf.noOfBins];
			double step=(max-min)/lowerBoundOfBins.length;
			for(int i=0;i<lowerBoundOfBins.length;i++){
				lowerBoundOfBins[i]=min+step*i;
			}
			double[] cntOfBins=new double[lowerBoundOfBins.length];
			for(Double d: axisAccl){
				//find which bin this value belongs to
				int l=-1, r=lowerBoundOfBins.length, m;
				while(l+1!=r){
					m=l+(r-l)/2;
					if(d<lowerBoundOfBins[m]) r=m;
					else l=m;
				}
				if(l>=0) cntOfBins[l]+=1;
			}
			for(int i=0;i<cntOfBins.length;i++){
				feature.binPercents.get(axisIdx).add(cntOfBins[i]/axisAccl.size());
			}
			
		}
		return feature;
	}
	
	
	
	/*
	 * @filename: path to a feature file:
	 * also output the features out to a file at accelerometer/fusion/ folder
	 */
	public ArrayList<IndicatorVector> outputCIVVectors(ArrayList<WindowFeature> features, String dateSeq, String labeldFilePath) {
		
		ArrayList<IndicatorVector> indicatorVectors=new ArrayList<IndicatorVector>();
		try {
			//read the labeled lines from the old feature file
			ArrayList<ArrayList<Integer>> labeledLines=AccelerometerFileProcessing.keepCIVClass(labeldFilePath);
						
			String line, classString="";
			ArrayList<Double> featureValues=new ArrayList<Double>();
			
			// read the groundtruth
			/*
			if(inputRawFile!=null){
				Scanner sc=new Scanner(inputRawFile);
				line=sc.nextLine().trim();
				if(line.startsWith("@")){
					//fw.write(line+"\n");
				}
				sc.close();
			}*/
			
			// write the header first			
			/*sc = new Scanner(new File(Constants.ACCELEROMETER_CIV_FEATURES_DIR+"arff_header.txt"));
			while(sc.hasNextLine()){
				fw.append(sc.nextLine()+"\n");
			}
			sc.close();	*/		
			
			// write the vectors to a file
			FileWriter fw=new FileWriter(Constants.ACCELEROMETER_FUSION_DIR+dateSeq+"_CIV.log");		
						
			Mean mean=new Mean();
			Variance variance=new Variance();			
			int scope=conf.scope; // K/2 preceding windows and K/2 trailing windows
			
			ArrayList<WindowFeature> previousWindows=new ArrayList<WindowFeature>();
			String timeInHMS;
			for(WindowFeature curWindow: features){
				featureValues.clear();
				previousWindows.add(curWindow);
				if(previousWindows.size()==scope+1){
					
					timeInHMS=CommonUtils.secondsToHMS(previousWindows.get(previousWindows.size()-1).temporalWindow[1]);
					
					line=timeInHMS;
					int[] precedingOrTrailing={0, scope/2+1};
					for(int j=0;j<precedingOrTrailing.length;j++){
						double[] variDiff=new double[scope/2];
						double[] avgAccel=new double[scope/2];
						//add the mean and variance of the values of the neighboring K/2 windows
						for(int i=precedingOrTrailing[j];i<precedingOrTrailing[j]+scope/2;i++){
							//the variance diff. between two halves of the window
							WindowFeature window=previousWindows.get(i);
							ArrayList<Double> acclList=window.varianceSeries.get(Constants.AXIS_AGG);
							variDiff[i-precedingOrTrailing[j]]=acclList.get(1)-acclList.get(0);
							avgAccel[i-precedingOrTrailing[j]]=acclList.get(2);
						}
						double avg=mean.evaluate(variDiff);
						line+=","+String.format("%7.3f", avg); //add the avg. of the variDiff
						featureValues.add(avg);
						double variOfVari=variance.evaluate(variDiff, avg);
						line+=","+String.format("%7.3f", variOfVari); // add the var. of the variDiff
						featureValues.add(variOfVari);
												
						avg=mean.evaluate(avgAccel);
						line+=","+String.format("%7.3f", avg); //add the avg. of the avg
						featureValues.add(avg);
						double variOfAvg=variance.evaluate(avgAccel, avg);
						line+=","+String.format("%7.3f", variOfAvg); // add the var. of the avg
						featureValues.add(variOfAvg);
						
						if(j==0){//add the value of the center window
							ArrayList<Double> acclList=previousWindows.get(scope/2).varianceSeries.get(Constants.AXIS_AGG);
							line+=","+String.format("%7.3f", acclList.get(1)-acclList.get(0));
							featureValues.add(acclList.get(1)-acclList.get(0));
						}
					}
					//add the label to the line
					boolean labeled=false;
					for(int i=0;i<2;i++){
						if(labeledLines.get(i).contains(previousWindows.get(scope/2).timeIndex)){
							if(i==Constants.UNPARKING_ACTIVITY){
								line+=",u\n";
								classString="u";
							}
							else{
								line+=",p\n";
								classString="p";
							}
							
							labeled=true;
							break;
						}
					}
					if(labeled==false){
						line+=",n\n";
						classString="n";
					}
					
					fw.append(line);
					//timeInHMS is the time stamp of the closing bound of the last window, i.e. when the vector is output
					indicatorVectors.add(new IndicatorVector(timeInHMS, 
							CommonUtils.doubleListToDoubleArray(featureValues),
							Fusion.INDICATOR_CIV, classString));
					previousWindows.remove(0); // remove the first window
				}			
			}
			fw.close();
			System.out.println(indicatorVectors.size()+" CIV indicator vectors extracted.");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return indicatorVectors;
	}
	
	
	//output the features to a file
	public void outputToAFile(ArrayList<WindowFeature> features, String outputFileName){
		try{
			FileWriter fw=new FileWriter(outputFileName);
			for(WindowFeature feature: features){
				int timestamp=feature.timeIndex;
				
				//if this timestamp (or its neighbor) is labeled in the feature file
				int key=0;
				int[] offset=new int[]{0, 1, -1, 2, -2, 3, -3};
				for(int i=0;i<offset.length;i++){
					if(eventsTimestamps.containsKey(timestamp+offset[i])){
						key=timestamp+offset[i];
						break;
					}
				}
				if(key!=0){
					String event="";
					String label=eventsTimestamps.get(key);
					if(label.equals("o")) event="p";
					if(label.equals("i")) event="u";
					fw.write(feature.toString(4)+event+"\n");
				}else{
					fw.write(feature.toString(4)+"\n");
				}
			}
			fw.close();
			System.out.println("Features are writen to "+outputFileName);
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
	
	}

}
