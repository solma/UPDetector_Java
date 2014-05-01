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
	private static ArrayList<String> calNoOfDetectedEvents(ArrayList<String> detectedEvents){
		int cnt=0;
		int prevTime=0;
		ArrayList<String> clusteredEvents=new ArrayList<String>();
		for(String dateTime: detectedEvents){
			String[] fields=dateTime.split("-");
			int timeOfSecond=CommonUtils.HMSToSeconds(fields[1]);
			if(Math.abs(timeOfSecond-prevTime)>3){//if move out the neighboring area, then count as a new event
				cnt+=1;
				clusteredEvents.add(dateTime);
			}
			prevTime=timeOfSecond;
		}
		return clusteredEvents;
	}
	
	//detection results:  one parking list: one unparking list
	// each timestamp format: date-hms
	public static double[][] calculatePerformance(ArrayList<ArrayList<String>> detectionResults){
		
		System.out.println("************************   Detection Performance:  ***************************");
		int tp,fp, fn;
		
		int[] offsets={0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5};
		
		double[][] preAndRecall=new double[2][2];
		for(int i=0;i<2;i++){
			tp=0; fp=0; fn=0;			
			
			//get the ground-truth for parking/unparking events
			ArrayList<String> truth=new ArrayList<String>();
			for(String date: groundTruth.keySet()){
				truth.addAll(groundTruth.get(date).get(i));
			}
			//sort the truth by the date la
			Collections.sort(truth, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					String[] os={o1, o2};
					int[][] nums=new int[2][5];
					for(int i=0;i<os.length;i++){
						String[] fields=os[i].split("-");
						String[] dateParts=fields[0].split("_");
						for(int j=0;j<dateParts.length;j++){
							if(j<dateParts.length-1) nums[i][j]=Integer.parseInt(dateParts[j]);
							else{
								nums[i][j]=Integer.parseInt(dateParts[j].substring(0,2));
								nums[i][j+1]=Integer.parseInt(dateParts[j].substring(2));
							}
						}
						nums[i][nums[0].length-1]=CommonUtils.HMSToSeconds(fields[1]);
					}
					for(int i=0;i<nums[0].length;i++){
						int diff=nums[0][i]-nums[1][i];
						if(diff!=0) return diff;						
					}
					return 0;
				}
			});
			
			//convert the detection results to a set
			HashSet<String> detected=new HashSet<String>();
			for(String dateTime: detectionResults.get(i)){
				String[] fields=dateTime.split("-");
				for(int j=0;j<offsets.length;j++){
					int timeOfSecond=CommonUtils.HMSToSeconds(fields[1])+offsets[j];
					detected.add(fields[0]+"-"+CommonUtils.secondsToHMS(timeOfSecond) ) ;
				}
			}
			
			
			ArrayList<String> falseNegative=new ArrayList<String>();
			for(String dateTime: truth){
				if(detected.contains(dateTime)){
					tp+=1;
				}else{
					falseNegative.add(dateTime);
				}
			}
			
			
			//fp=classificationResults.get(i).size()-tp;
			ArrayList<String> clusteredEvents=calNoOfDetectedEvents(detectionResults.get(i));
			fp= clusteredEvents.size()-tp;
			fn=truth.size()-tp;
			
			if(i==Constants.PARKING){
				System.out.println("********    Parking Events: **********");
			}
			else{
				System.out.println("********    Unparking Events: **********");
			}
			Collections.sort(truth);
			Collections.sort(clusteredEvents);
			System.out.println("               Truth: "+ truth.size()+"  "+truth);
			System.out.println("(Clusted)   Detected: "+ clusteredEvents.size()+"  "+clusteredEvents);
			System.out.println("(Unclusted) Detected: "+ detectionResults.get(i).size()+"  "+detectionResults.get(i));
			System.out.println(" False Negatives are:     "+ falseNegative);
			System.out.println("TP="+tp+"  FP="+fp+"  FN="+fn);
			
			
			AccelerometerSignalProcessing.tps[i*2]=Math.round(tp/(tp+fp+0.0)*1000.0)/1000.0;
			AccelerometerSignalProcessing.tps[i*2+1]=Math.round(tp/(tp+fn+0.0)*1000.0)/1000.0;
			
			
			double precision=((int)(tp/(tp+fp+0.0)*1000))/1000.0;
			double recall=((int)(tp/(tp+fn+0.0)*1000))/1000.0;
			System.out.println("Precision="+precision+"		Recall="+recall);
			System.out.println();
		
			preAndRecall[i][0]=precision;preAndRecall[i][1]=recall;
		}
		return preAndRecall;
	}
	
	//@fn date
	public static void detectEvents(String dateSeq ,HashMap<String, ArrayList<ArrayList<String>> > groundTruth){
		ArrayList<ArrayList<String>> detectedTransitions= findMotionStateTransition(dateSeq);
		
		for(int i=0;i<detectedTransitions.size();i++){
			//add the parking/unparking events
			for(String timestamp: detectedTransitions.get(i)){
				String seconds=CommonUtils.cutField(timestamp, ":", 0, 3, ":");
				if(!AccelerometerSignalProcessing.detectedEvents.get(i).contains(dateSeq+"-"+seconds)){
					AccelerometerSignalProcessing.detectedEvents.get(i).add(dateSeq+"-"+seconds );
				}
			}
		}
		
		for(int i=0;i<2;i++){
			if(i==0) System.out.println("Groudtruth Parking events:");
			else System.out.println("Groudtruth Unparking events:");
			System.out.println(groundTruth.get(dateSeq).get(i));
			for(String timestamp:  groundTruth.get(dateSeq).get(i)){
				System.out.print(CommonUtils.HMSToSeconds(timestamp.split("-")[1])+",");
			}
			System.out.println();		
			
			if(i==0) System.out.println("Deteced Parking events:");
			else System.out.println("Deteced Unparking events:");
			System.out.println(AccelerometerSignalProcessing.detectedEvents.get(i));
			//print the timestamp of the events in seconds
			for(String timestamp: AccelerometerSignalProcessing.detectedEvents.get(i)){
				System.out.print(CommonUtils.HMSToSeconds(timestamp.split("-")[1])+",");
			}
			System.out.println();
			System.out.println();
		}
		
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
	public static ArrayList<ArrayList<String>> findMotionStateTransition(String fileNameAsDateSeq){
		
		String fileName=ACTIVITY_DIR_Name+"GOOGLE_ACTIVITY_"+fileNameAsDateSeq+FILE_EXTENSION;
		//Constants.ACCELEROMETER_BASE_DIR+"04202014/GOOGLE_ACTIVITY_UPDATE_2014_04_200.log";
		
		ArrayList<ArrayList<String>> events=new ArrayList<ArrayList<String>>();
		try{
			Scanner sc=new Scanner(new File(fileName));

			events.add(new ArrayList<String>()); //parking events
			events.add(new ArrayList<String>()); //unparking events
			
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
					if(!events.get(0).contains(fields[TIMESTAMP_IDX])){
						events.get(0).add(fields[TIMESTAMP_IDX]);
					}
				}
				
				//unparking
				if(isFromOnFoottoInVehicle(newOnFootOrInVehicleActivity, lastOnFootOrInVehicleActivity)){
					if(!events.get(1).contains(fields[TIMESTAMP_IDX])){
						events.get(1).add(fields[TIMESTAMP_IDX]);
					}
				}
				lastOnFootOrInVehicleActivity=newOnFootOrInVehicleActivity;
				
			}
			
			sc.close();
			
			Mean mean=new Mean();
			System.out.println(String.format("average activity update interval is %.2f", mean.evaluate(Doubles.toArray(intervals)) )+ " secs\n");
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return events;
	}
	
	
		//string: date, ArrayList<ArrayList<String>>: ground-truth events
		public static HashMap<String, ArrayList<ArrayList<String>> > groundTruth=new HashMap<String, ArrayList<ArrayList<String>>>(); 
		
		
		/**
		 * 
		 * @param pathOfRawFiles
		 * @return
		 */
		public static HashMap<String, ArrayList<ArrayList<String>> > readGroudTruth(String[] pathOfRawFiles){
			groundTruth.clear();			
			try{
				for(String filepath: pathOfRawFiles ){
					String dateSeq;
					if(filepath.contains("FEATURE"))
						dateSeq=CommonUtils.getFileName(filepath).replace("ACCELEROMETER_FEATURE_", "").replace(".arff", "");
					else
						dateSeq=CommonUtils.getFileName(filepath).replace("ACCELEROMETER_RAW_", "").replace(".log", "");
					//System.out.println(dateSeq);
					
					if(!groundTruth.containsKey(filepath)){
						groundTruth.put(dateSeq, new ArrayList<ArrayList<String>>());
						groundTruth.get(dateSeq).add(new ArrayList<String>()); //parking events
						groundTruth.get(dateSeq).add(new ArrayList<String>()); //unparking events										
					}
					
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
									groundTruth.get(dateSeq).get(Constants.UNPARKING).add(dateSeq+"-"+timestamps[0]); //read time to second only
								}
								groundTruth.get(dateSeq).get(Constants.PARKING).add(dateSeq+"-"+timestamps[1]);
							}
							
							break;
						}
					}
					sc.close();
				}

				//printGroundTruth();			
				
			}catch(Exception ex){
				ex.printStackTrace();
			}
				
			return groundTruth;
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
