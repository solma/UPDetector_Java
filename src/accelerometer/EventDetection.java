package accelerometer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import javax.rmi.CORBA.Util;

import main.Constants;
import main.ParkSense;
import main.CommonUtils;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import com.google.common.primitives.Doubles;

import datastructure.UPActivitiesOfSameSource;
import datastructure.UPActivity;
import datastructure.UPActivity.SOURCE;


public class EventDetection {
	
	//Used in the logic for determining the transition from in_vehicle to on_foot
	private static final int MIN_NUMBER_OF_CONSECUTIVE_ON_FOOT_ACTIVITY=1; 

	/*
	 * parameters for Google API and Weka train activity files
	 */
	private static String ACTIVITY_DIR_Name;
	
	private static int ACTIVITY_IDX;
	private static int TIMESTAMP_IDX=1;
	
	private static String ON_FOOT_ACTIVITY;
	private static String HELD_IN_HAND_ON_FOOT_ACTIVITY="";
	
	private static String IN_VEHICLE_ACTIVITY;
	private static String DELEMITER;
	private static String FILE_EXTENSION;
		
	public static void main(String[] args) {
	}
	
	//purpose: so events with adjacent timestamp only count as 1
	private static ArrayList<UPActivity> clusterDetectedEvents(ArrayList<UPActivity> detectedEvents){
		ArrayList<UPActivity> clusteredEvents=new ArrayList<UPActivity>();
		UPActivity prev=null;
		for(UPActivity event: detectedEvents){
			if(prev==null||event.timeDiffWithinThreshold(prev)){//if move out the neighboring area, then count as a new event
				clusteredEvents.add(event);
			}
			prev=event;
		}
		return clusteredEvents;
	}
	
	//detection results:  one parking list: one unparking list
	// each timestamp format: date-hms
	public static double[][] calculatePrecisionAndRecall(UPActivitiesOfSameSource detectionResults, UPActivitiesOfSameSource groudtruth){
		
		System.out.println("************************   Detection Precision and Recall:  ***************************");
		int tp,fp, fn;
		
		int[] offsets={0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5};
		//get the ground-truth for parking/unparking events
		groudtruth.sort();
		
		
		double[][] preAndRecall=new double[2][2];
		ArrayList<UPActivity> truth;
		for(int i=0;i<2;i++){
			tp=0; fp=0; fn=0;			
			
			truth=groudtruth.get(i);
			double avgDelay=0;
			//calculate false negatives
			ArrayList<UPActivity> falseNegative=new ArrayList<UPActivity>();
			UPActivity matchedGroundtruthEvent;
			for(UPActivity event: detectionResults.get(i)){
				matchedGroundtruthEvent=event.matchToGroundtruthEvent(truth);
				if(matchedGroundtruthEvent!=null){
					tp+=1;
					avgDelay+=event.timeInSecOfDay-matchedGroundtruthEvent.timeInSecOfDay;
				}else{
					falseNegative.add(event);
				}
			}
			
			
			//fp=classificationResults.get(i).size()-tp;
			ArrayList<UPActivity> clusteredEvents=clusterDetectedEvents(detectionResults.get(i));
			fp= clusteredEvents.size()-tp;
			fn=truth.size()-tp;
			avgDelay/=tp;
			
			if(i==Constants.PARKING_ACTIVITY){
				System.out.println("********    Parking Events: **********");
			}
			else{
				System.out.println("********    Unparking Events: **********");
			}
			
			System.out.println("               Truth: "+ truth.size()+"  "+truth);
			System.out.println("(Clusted)   Detected: "+ clusteredEvents.size()+"  "+clusteredEvents);
			System.out.println("(Unclusted) Detected: "+ detectionResults.get(i).size()+"  "+detectionResults.get(i));
			System.out.println(" False Negatives are:     "+ falseNegative);
			System.out.println("TP="+tp+"  FP="+fp+"  FN="+fn);
			
			
			AccelerometerSignalProcessing.tps[i*2]=Math.round(tp/(tp+fp+0.0)*1000.0)/1000.0;
			AccelerometerSignalProcessing.tps[i*2+1]=Math.round(tp/(tp+fn+0.0)*1000.0)/1000.0;
			
			
			double precision=((int)(tp/(tp+fp+0.0)*1000))/1000.0;
			double recall=((int)(tp/(tp+fn+0.0)*1000))/1000.0;
			System.out.println("Precision="+precision+"		Recall="+recall+"		avgDelay="+String.format("%.2f secs", avgDelay));
			System.out.println();
		
			preAndRecall[i][0]=precision;preAndRecall[i][1]=recall;
		}
		return preAndRecall;
	}
	
	//@fn date
	public static UPActivitiesOfSameSource detectEvents(String dateSeq ,UPActivitiesOfSameSource groundTruth){
		//detect activities by discovering transitions
		UPActivitiesOfSameSource detectedTransitions= findMotionStateTransition(dateSeq);
		
		/*for(int i=0;i<detectedTransitions.size();i++){
			//add the parking/unparking events
			for(String timestamp: detectedTransitions.get(i)){
				String seconds=CommonUtils.cutField(timestamp, ":", 0, 3, ":");
				if(!AccelerometerSignalProcessing.detectedEvents.get(i).contains(dateSeq+"-"+seconds)){
					AccelerometerSignalProcessing.detectedEvents.get(i).add(dateSeq+"-"+seconds );
				}
			}
		}*/
		
		/*for(int i=0;i<2;i++){
			if(i==0) System.out.println("Groudtruth Parking events:");
			else System.out.println("Groudtruth Unparking events:");
			System.out.println(groundTruth.get(dateSeq).get(i));
			for(String timestamp:  groundTruth.get(dateSeq).get(i)){
				System.out.print(CommonUtils.HMSToSeconds(timestamp.split("-")[1])+",");
			}
			System.out.println();		
			
			if(i==Constants.PARKING_ACTIVITY) System.out.println("Deteced Parking events:");
			else System.out.println("Deteced Unparking events:");
			System.out.println(AccelerometerSignalProcessing.detectedEvents.get(i));
			//print the timestamp of the events in seconds
			for(String timestamp: AccelerometerSignalProcessing.detectedEvents.get(i)){
				System.out.print(CommonUtils.HMSToSeconds(timestamp.split("-")[1])+",");
			}
			System.out.println();
			System.out.println();
		}*/
		return detectedTransitions;
	}
	
	/*
	 * choose the direction and setup parameters
	 */
	public static boolean setupFolderAndFieldIdx(String wekaOrGoogle){
		if(wekaOrGoogle.toLowerCase().equals("weka") ){
			ACTIVITY_DIR_Name=Constants.ACCELEROMTER_ACTIVITY_WEKA_DIR;
		}else{
			if(wekaOrGoogle.toLowerCase().equals("google"))
				ACTIVITY_DIR_Name=Constants.ACCELEROMTER_ACTIVITY_GOOGLE_DIR;
			else{
				System.err.println("Invalid Option! Only 'weka' or 'google'. ");
				return false;
			}
		}
		if(ACTIVITY_DIR_Name==Constants.ACCELEROMTER_ACTIVITY_WEKA_DIR){
			ACTIVITY_IDX=7;
			ON_FOOT_ACTIVITY="o";
			IN_VEHICLE_ACTIVITY="i";
			HELD_IN_HAND_ON_FOOT_ACTIVITY="h";
			DELEMITER=",";
			FILE_EXTENSION=".arff";
		}else{
			TIMESTAMP_IDX=1;
			ACTIVITY_IDX=2;
			ON_FOOT_ACTIVITY="on_foot";
			IN_VEHICLE_ACTIVITY="in_vehicle";
			DELEMITER=" ";
			FILE_EXTENSION=".log";
		}
		return true;
	}

	
	private static int NO_Of_Consecutive_OnFootActivities;
	
	
	private static boolean isFromOnFoottoInVehicle(String newDetectedActivity, String lastDetectedActivity){
        
        if (newDetectedActivity.equals(IN_VEHICLE_ACTIVITY) 
        	&& ( lastDetectedActivity.equals(ON_FOOT_ACTIVITY) || lastDetectedActivity.equals(HELD_IN_HAND_ON_FOOT_ACTIVITY))){
            return true;
        }

    	return false;
    }
    
    private static boolean isFromInVehicletoOnFoot(String newDetectedActivity, String lastDetectedActivity){

    	//in_vehicle to on_foot
    	if( (newDetectedActivity.equals(ON_FOOT_ACTIVITY)|| newDetectedActivity.equals(HELD_IN_HAND_ON_FOOT_ACTIVITY))
    	&& lastDetectedActivity.equals(IN_VEHICLE_ACTIVITY) )
    	{    		
            NO_Of_Consecutive_OnFootActivities=1;   
            if(NO_Of_Consecutive_OnFootActivities==MIN_NUMBER_OF_CONSECUTIVE_ON_FOOT_ACTIVITY) return true;
    	}else{
    		//two consecutive on_foot
    		if(NO_Of_Consecutive_OnFootActivities>0
    		&& ( newDetectedActivity.equals(ON_FOOT_ACTIVITY)|| newDetectedActivity.equals(HELD_IN_HAND_ON_FOOT_ACTIVITY)) 
    		&& (lastDetectedActivity.equals(ON_FOOT_ACTIVITY) || lastDetectedActivity.equals(HELD_IN_HAND_ON_FOOT_ACTIVITY) ) ){
	    		
    			NO_Of_Consecutive_OnFootActivities+=1;
    			//found  consecutive on_foot activities
	    	    if(NO_Of_Consecutive_OnFootActivities==MIN_NUMBER_OF_CONSECUTIVE_ON_FOOT_ACTIVITY) return true;
    		}
    	}
    	return false;
    }
	
    /**
     * @return detected events 
     */
	public static UPActivitiesOfSameSource findMotionStateTransition(String fileNameAsDateSeq){
		
		String fileName=ACTIVITY_DIR_Name+"GOOGLE_ACTIVITY_"+fileNameAsDateSeq+FILE_EXTENSION;
		//Constants.ACCELEROMETER_BASE_DIR+"04202014/GOOGLE_ACTIVITY_UPDATE_2014_04_200.log";
		
		UPActivitiesOfSameSource eventsDetectedByGoogleAPI=new UPActivitiesOfSameSource(SOURCE.GOOGLE_API);
		try{
			Scanner sc=new Scanner(new File(fileName));

			NO_Of_Consecutive_OnFootActivities=0;

			String lastOnFootOrInVehicleActivity=""; 
		
			ArrayList<Integer> intervals=new ArrayList<Integer>();
			int lastTimestamp=0;
			while(sc.hasNextLine()){
				String line=sc.nextLine();
				String[] fields=line.split(DELEMITER);
				String newOnFootOrInVehicleActivity=fields[ACTIVITY_IDX].toLowerCase();
				
				int curTime=CommonUtils.HMSToSeconds(fields[TIMESTAMP_IDX]);
				if(lastTimestamp!=0) intervals.add(curTime-lastTimestamp);
				lastTimestamp=curTime;
				
				if(!newOnFootOrInVehicleActivity.equals(ON_FOOT_ACTIVITY)
				&&!newOnFootOrInVehicleActivity.equals(IN_VEHICLE_ACTIVITY)
				&&!newOnFootOrInVehicleActivity.equals(HELD_IN_HAND_ON_FOOT_ACTIVITY))
					continue;
				
				//parking
				if(isFromInVehicletoOnFoot(newOnFootOrInVehicleActivity, lastOnFootOrInVehicleActivity)) {
					eventsDetectedByGoogleAPI.add(new UPActivity(SOURCE.GOOGLE_API, 
							Constants.PARKING_ACTIVITY, fileNameAsDateSeq.substring(0, 10),
							CommonUtils.cutField(fields[TIMESTAMP_IDX], ":", 0, 3, ":")
							));
				}
				
				//unparking
				if(isFromOnFoottoInVehicle(newOnFootOrInVehicleActivity, lastOnFootOrInVehicleActivity)){
					eventsDetectedByGoogleAPI.add(new UPActivity(SOURCE.GOOGLE_API, 
							Constants.UNPARKING_ACTIVITY, fileNameAsDateSeq.substring(0, 10),
							CommonUtils.cutField(fields[TIMESTAMP_IDX], ":", 0, 3, ":")));
				}
				lastOnFootOrInVehicleActivity=newOnFootOrInVehicleActivity;
				
			}
			
			sc.close();
			
			Mean mean=new Mean();
			System.out.println(String.format("average activity update interval is %.2f", mean.evaluate(Doubles.toArray(intervals)) )+ " secs\n");
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return eventsDetectedByGoogleAPI;
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

							for(int i=2;i<fields.length;i++){
								String[] timestamps=fields[i].split("~");
								if(!dateSeq.equals("2013_08_2311")||!timestamps[0].equals("18:41:50")){
									//groundTruth.get(dateSeq).get(Constants.UNPARKING_ACTIVITY).add(dateSeq+"-"+timestamps[0]); //read time to second only
									groudtruth.add(new UPActivity(SOURCE.GROUND_TRUTH, Constants.UNPARKING_ACTIVITY, dateSeq.substring(0, 10), timestamps[0]));
								}
								//groundTruth.get(dateSeq).get(Constants.PARKING_ACTIVITY).add(dateSeq+"-"+timestamps[1]);
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
