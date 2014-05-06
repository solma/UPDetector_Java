package accelerometer;

import helper.Constants;

import java.util.ArrayList;

public class EventDetectionResult {

	public ArrayList<Double> parkingResult;
	
	public ArrayList<Double> unparkingResult;

	public EventDetectionResult(){
		parkingResult=new ArrayList<Double>();
		unparkingResult=new ArrayList<Double>();
	}
	
	public void add(int eventType, double prec, double rec, double avgDelay){
		ArrayList<Double> list;
		if(eventType==Constants.PARKING_ACTIVITY) list=parkingResult;
		else list=unparkingResult;
		list.add(prec);
		list.add(rec);
		list.add(avgDelay);
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("Parking: "+parkingResult.toString()+"\n");
		sb.append("Unparking: "+unparkingResult.toString()+"\n");
		return sb.toString();
	}
}
