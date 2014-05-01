package main;

import java.util.ArrayList;

public class DetectedActivity implements Comparable<DetectedActivity>{
	public enum Type{
		Parking, Unparking
	}
	
	public long timestamp;
	public String hms;
	
	public Type type;
	public DetectedActivity(Type type, long time){
		timestamp=time;
		this.type=type;
	}
	
	public int compareTo(DetectedActivity other){
		return (int)(this.timestamp-other.timestamp);
	}
}

class DetectedActivitySequence{
	public ArrayList<DetectedActivity> parkingActivities;
	public ArrayList<DetectedActivity> unparkingActivities;
	public DetectedActivitySequence(){
		parkingActivities=new ArrayList<DetectedActivity>();
		unparkingActivities=new ArrayList<DetectedActivity>();
	}
}

class FusionDetectedActivity extends DetectedActivity{
	public FusionDetectedActivity(Type type, long time, double prob) {
		super(type, time);
		this.probability=prob;
	}
	public static double DETECTION_THRESHOLD;
	public double probability;
}

class GoogleConfirmedActivity extends DetectedActivity{
	public GoogleConfirmedActivity(Type type, long time) {
		super(type, time);		
	}
	
	public FusionDetectedActivity fusionDetectedActivity;
	
}
