package accelerometer;

import fusion.Fusion;
import helper.CommonUtils;
import helper.Constants;

import java.io.File;
import java.io.ObjectInputStream.GetField;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import javax.rmi.CORBA.Util;


import org.apache.commons.math3.stat.descriptive.moment.Mean;

import upactivity.UPActivitiesOfSameSource;
import upactivity.UPActivity;
import upactivity.UPActivity.SOURCE;

import com.google.common.primitives.Doubles;



public class EventDetection {
	

	/*
	 * parameters for Google API and Weka train activity files
	 */
	private static String ACTIVITY_DIR_Name;
	
	private static int ACTIVITY_IDX;
	private static int TIMESTAMP_IDX=1;
	
	private static String ON_FOOT_ACTIVITY;
	private static String IN_VEHICLE_ACTIVITY;
	
	private static SOURCE SOURCE;
	private static String FILE_NAME_PREFIX;
	
	private static String DELEMITER;
	private static String FILE_EXTENSION;
	public static int MATCH_TIME_DIFF_UPPER_BOUND;
	public static int MATCH_TIME_DIFF_LOWER_BOUND; //detection delay
		
	public static void main(String[] args) {
	}
	
	//purpose: so events with adjacent timestamp only count as 1
	private static ArrayList<UPActivity> clusterDetectedEvents(ArrayList<UPActivity> detectedEvents){
		ArrayList<UPActivity> clusteredEvents=new ArrayList<UPActivity>();
		UPActivity prev=null;
		for(UPActivity event: detectedEvents){
			if(prev==null||!event.timeDiffWithinThreshold(prev, 5)){//if move out the neighboring area, then count as a new event
				clusteredEvents.add(event);
			}
			prev=event;
		}
		return clusteredEvents;
	}
	
	public static EventDetectionResult calculatePerformance(
			UPActivitiesOfSameSource detectionResults, UPActivitiesOfSameSource groudtruth, 
			int matchTimeDiffUpperBound, int matchTimeDiffLowerBound){
		MATCH_TIME_DIFF_UPPER_BOUND=matchTimeDiffUpperBound;
		MATCH_TIME_DIFF_LOWER_BOUND=matchTimeDiffLowerBound; 
		return calculatePerformance(detectionResults, groudtruth);
	}
	
	/**
	 * 
	 * @param detectionResults
	 * @param groudtruth
	 * @return pre, recal, delay 
	 */
	public static EventDetectionResult calculatePerformance(UPActivitiesOfSameSource detectionResults, UPActivitiesOfSameSource groudtruth){
		EventDetectionResult edr=new EventDetectionResult();
		System.out.println("************************   Detection Precision and Recall:  ***************************");
		int tp,fp, fn;
		
		//get the ground-truth for parking/unparking events
		groudtruth.sort();
		detectionResults.sort();
		
		ArrayList<UPActivity> truth, detected;
		for(int i=0;i<2;i++){
			if(i==Constants.PARKING_ACTIVITY){
				System.out.println("********    Parking Events: **********");
			}
			else{
				System.out.println("********    Unparking Events: **********");
			}
			
			tp=0; fp=0; fn=0;			
			
			truth=groudtruth.get(i);
			detected=detectionResults.get(i);
			double avgDelay=0;
			
			//key: ground truth; vale: matched detected events
			HashMap<UPActivity, ArrayList<UPActivity>> matches=new HashMap<UPActivity, ArrayList<UPActivity>>();
			ArrayList<UPActivity> unmatchedDetectedEvents=new ArrayList<UPActivity>();
			for(UPActivity detectedEvent: detected){
				/*if(detectedEvent.timeInHMS.contains("57:06")){
					System.out.println();
				}*/
				UPActivity matchedTruth=detectedEvent.matchToGroundtruthEvent(truth, MATCH_TIME_DIFF_UPPER_BOUND, MATCH_TIME_DIFF_LOWER_BOUND );
				if(matchedTruth!=null){
					if(!matches.containsKey(matchedTruth)){
						matches.put(matchedTruth, new ArrayList<UPActivity>());
					}
					matches.get(matchedTruth).add(detectedEvent);
				}else{
					unmatchedDetectedEvents.add(detectedEvent);
				}
			}
			tp=matches.size();
			fn=truth.size()-tp;

			ArrayList<UPActivity> clustedUnMatchedDetectedEvents=clusterDetectedEvents(unmatchedDetectedEvents);
			fp=clustedUnMatchedDetectedEvents.size();
			
			//calculate average detection delay of true positives
			//for each matched truth only count one detected event			
			for(UPActivity matchedTruth: matches.keySet()){
				ArrayList<UPActivity> detectedEventsMatchedToTheSameTruthEvent=matches.get(matchedTruth);
				Collections.sort(detectedEventsMatchedToTheSameTruthEvent);
				avgDelay+=detectedEventsMatchedToTheSameTruthEvent.get(0).timeInSecOfDay-matchedTruth.timeInSecOfDay;
			}
			avgDelay=((int)(avgDelay/tp*1000))/1000.0;
			
			//print ground truth
			System.out.print("               Truth: "+ truth.size()+"  ");
			for(UPActivity gtEvent: truth){
				System.out.print(gtEvent+"  ");
			}
			System.out.println();
			
			//print matched detection
			System.out.print("(Matched)   Detected: "+ matches.size()+"  ");
			for(UPActivity gtEvent: truth){
				if(matches.containsKey(gtEvent)){
					System.out.print(matches.get(gtEvent).get(0)+"  ");
				}else{
					System.out.print(String.format("%21s", " "));
				}
			}
			System.out.println();
			
			
			System.out.println("(all)       Detected: "+ detected.size()+"  "+detected);
			//System.out.println(" False Negatives are:     "+ falseNegative);
			System.out.println("TP="+tp+"  FP="+fp+"  FN="+fn);
			
			double precision=((int)(tp/(tp+fp+0.0)*1000))/1000.0;
			double recall=((int)(tp/(tp+fn+0.0)*1000))/1000.0;
			System.out.println("Precision="+precision+"		Recall="+recall+"		avgDelay="+String.format("%.2f secs", avgDelay));
			System.out.println();
		
			edr.add(i, precision, recall, avgDelay);
		}
		return edr;
	}
	
	
	/*
	 * choose the direction and setup parameters
	 */
	public static boolean setupParametersForMST(SOURCE source){
		switch (source) {
		case MST_GOOGLE:
			ACTIVITY_DIR_Name=Constants.ACCELEROMTER_ACTIVITY_GOOGLE_DIR;
			SOURCE=SOURCE.MST_GOOGLE;
			FILE_NAME_PREFIX="GOOGLE_ACTIVITY_";
			TIMESTAMP_IDX=1;
			ACTIVITY_IDX=2;
			ON_FOOT_ACTIVITY="on_foot";
			IN_VEHICLE_ACTIVITY="in_vehicle";
			DELEMITER=" ";
			MATCH_TIME_DIFF_UPPER_BOUND=300;
			MATCH_TIME_DIFF_LOWER_BOUND=0;
			FILE_EXTENSION=".log";
			break;
		case MST_WEKA:
			ACTIVITY_DIR_Name=Constants.ACCELEROMTER_ACTIVITY_WEKA_DIR;
			SOURCE=SOURCE.MST_WEKA;
			FILE_NAME_PREFIX="WEKA_";
			ACTIVITY_IDX=24; //2(w/o features) 24 with features
			ON_FOOT_ACTIVITY="walking";
			IN_VEHICLE_ACTIVITY="driving";
			DELEMITER=" ";
			FILE_EXTENSION=".arff";
			MATCH_TIME_DIFF_UPPER_BOUND=60;
			MATCH_TIME_DIFF_LOWER_BOUND=0;
			break;
		default:
			System.err.println("Invalid Option! Only 'weka' or 'google'. ");
			return false;
		}
		return true;
	}

	
	
	
	private static int SIZE_OF_PAST_RELEVANT_STATES=4;
	private static int isTransitioning(ArrayList<String> pastStates, SOURCE msSource){
		int size=pastStates.size();
		if(size!=SIZE_OF_PAST_RELEVANT_STATES) return Fusion.OUTCOME_NONE;
		String lastStateString=pastStates.get(size-1), 
				secondFromLast=pastStates.get(size-2), thirdFromLast=pastStates.get(size-3), forthFromLast=pastStates.get(size-4);
		if(lastStateString.equals(ON_FOOT_ACTIVITY)
				//&&secondFromLast.equals(ON_FOOT_ACTIVITY)
				&&secondFromLast.equals(IN_VEHICLE_ACTIVITY)
				&& (thirdFromLast.equals(IN_VEHICLE_ACTIVITY) || msSource==SOURCE.MST_GOOGLE ) 
				//&& (forthFromLast.equals(IN_VEHICLE_ACTIVITY) )// || msSource==SOURCE.MST_GOOGLE ) 
			)//parking
			return Fusion.OUTCOME_PARKING;
		if(lastStateString.equals(IN_VEHICLE_ACTIVITY)
				//&&secondFromLast.equals(IN_VEHICLE_ACTIVITY)
				&&secondFromLast.equals(ON_FOOT_ACTIVITY)
				&&thirdFromLast.equals(ON_FOOT_ACTIVITY)
				//&&forthFromLast.equals(ON_FOOT_ACTIVITY)
			)//unparking
			return Fusion.OUTCOME_UNPARKING;
		return Fusion.OUTCOME_NONE;
	}
	
	
    /**
     * @return detected events 
     */
	public static UPActivitiesOfSameSource findMotionStateTransition(String fileNameAsDateSeq, SOURCE source){
		
		String fileName=ACTIVITY_DIR_Name+FILE_NAME_PREFIX+fileNameAsDateSeq+FILE_EXTENSION;
		//Constants.ACCELEROMETER_BASE_DIR+"04202014/GOOGLE_ACTIVITY_UPDATE_2014_04_200.log";
		String line="";
		UPActivitiesOfSameSource detected=new UPActivitiesOfSameSource(SOURCE);
		try{
			Scanner sc=new Scanner(new File(fileName));

			ArrayList<String> pastStates=new ArrayList<String>();

			
			ArrayList<Integer> intervals=new ArrayList<Integer>();
			int lastTimestamp=0;
			while(sc.hasNextLine()){
				line=sc.nextLine();
				String[] fields=line.trim().split(DELEMITER);
				String newOnFootOrInVehicleActivity=fields[ACTIVITY_IDX].toLowerCase();
				
				int curTime=CommonUtils.HMSToSeconds(fields[TIMESTAMP_IDX]);
				if(lastTimestamp!=0) intervals.add(curTime-lastTimestamp);
				lastTimestamp=curTime;
				
				//ignore non-vehicle-or-foot states
				if(!newOnFootOrInVehicleActivity.equals(ON_FOOT_ACTIVITY)
					&&!newOnFootOrInVehicleActivity.equals(IN_VEHICLE_ACTIVITY)
				)
					continue;
				
				/*if(fields[TIMESTAMP_IDX].contains("16:07:24")){
					System.out.println();
				}*/
				pastStates.add(newOnFootOrInVehicleActivity);
				if(pastStates.size()==SIZE_OF_PAST_RELEVANT_STATES+1) pastStates.remove(0);
				
				int outcome=isTransitioning(pastStates, source);
				switch (outcome) {
				case Fusion.OUTCOME_PARKING:
					detected.add(new UPActivity(SOURCE, 
							Constants.PARKING_ACTIVITY, fileNameAsDateSeq.substring(0, 10),
							CommonUtils.cutField(fields[TIMESTAMP_IDX], ":", 0, 3, ":")
							));
					break;
				case Fusion.OUTCOME_UNPARKING:
					detected.add(new UPActivity(SOURCE, 
							Constants.UNPARKING_ACTIVITY, fileNameAsDateSeq.substring(0, 10),
							CommonUtils.cutField(fields[TIMESTAMP_IDX], ":", 0, 3, ":")));
					break;
				default:
					break;
				}
				
				
			}
			
			sc.close();
			System.out.println("Detected: "+ detected);
			Mean mean=new Mean();
			System.out.println(String.format("average activity update interval is %.2f", mean.evaluate(Doubles.toArray(intervals)) )+ " secs\n");
			
		}catch(Exception ex){
			ex.printStackTrace();
			System.out.println("error line: "+line);
		}
		return detected;
	}
	
	
		//string: date, ArrayList<ArrayList<String>>: ground-truth events
		public static HashMap<String, ArrayList<ArrayList<String>> > groundTruth=new HashMap<String, ArrayList<ArrayList<String>>>(); 
		
		
		/**
		 * 
		 * @param pathOfRawFiles
		 * @return
		 */
		public static UPActivitiesOfSameSource readGroudTruthFromRawAccelerometerFile(String[] pathOfRawFiles){
			UPActivitiesOfSameSource groudtruth=new UPActivitiesOfSameSource(SOURCE.GROUND_TRUTH);
			try{
				for(String filepath: pathOfRawFiles ){
					String dateSeq;
					if(filepath.contains("FEATURE"))
						dateSeq=CommonUtils.getFileName(filepath).replace("ACCELEROMETER_FEATURE_", "").replace(".arff", "");
					else
						dateSeq=CommonUtils.getFileName(filepath).replace("ACCELEROMETER_RAW_", "").replace(".log", "");
					//System.out.println(dateSeq);
					
					/*if(!groundTruth.containsKey(filepath)){
						groundTruth.put(dateSeq, new ArrayList<ArrayList<String>>());
						groundTruth.get(dateSeq).add(new ArrayList<String>()); //parking events
						groundTruth.get(dateSeq).add(new ArrayList<String>()); //unparking events										
					}*/
					
					Scanner sc=new Scanner(new File(filepath));
					
					while(sc.hasNextLine()){
						//read off the first line
						String line=sc.nextLine().trim();
						//the first line must contain in_vehicle state
						if(line.contains("in_vehicle")){
							String[] fields=line.split(" ");
							//works fine with files without parking/unparking activities
							for(int i=2;i<fields.length;i++){
								String[] timestamps=fields[i].split("~");
								if(!dateSeq.equals("2013_08_2311")||!timestamps[0].equals("18:41:50")){
									//groundTruth.get(dateSeq).get(Constants.UNPARKING_ACTIVITY).add(dateSeq+UPActivity.DATE_TIME_DELIMETER+timestamps[0]); //read time to second only
									groudtruth.add(new UPActivity(SOURCE.GROUND_TRUTH, Constants.UNPARKING_ACTIVITY, dateSeq.substring(0, 10), timestamps[0]));
								}
								//groundTruth.get(dateSeq).get(Constants.PARKING_ACTIVITY).add(dateSeq+UPActivity.DATE_TIME_DELIMETER+timestamps[1]);
								groudtruth.add(new UPActivity(SOURCE.GROUND_TRUTH, Constants.PARKING_ACTIVITY, dateSeq.substring(0, 10), timestamps[1]));
							}
							
							break;
						}
					}
					sc.close();
				}
				System.out.println(groudtruth);
				//printGroundTruth();			
				
			}catch(Exception ex){
				ex.printStackTrace();
			}
				
			return groudtruth;
		}
		
		public static void printGroundTruth(){
			System.out.println("*********Ground_truth***********");
			for(String key: groundTruth.keySet() ){
				ArrayList<ArrayList<String>> truth=groundTruth.get(key);
				for(int i=0;i<2;i++){
					if(i==0) System.out.print(key+" Unparking: ");
					else     System.out.print(key+"   Parking: ");
					for(String timestamp: truth.get(i)){
						System.out.print(timestamp.split("-")[1]+"  ");
					}
					System.out.println();
				}
			}
			System.out.println();
		}

}
