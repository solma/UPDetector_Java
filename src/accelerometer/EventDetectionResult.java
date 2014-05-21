package accelerometer;

import helper.Constants;

import java.util.ArrayList;
import java.util.HashMap;

public class EventDetectionResult {

	public class Measures{
		double precision;
		double recall;
		double avgDelay;
		
		public Measures(double prec, double rec, double avgDelay){
			precision=prec;
			recall=rec;
			this.avgDelay=avgDelay;
		}
		
		public String toString(){
			return precision+" "+recall+" "+avgDelay;
		}
	}
	
	public HashMap<Integer, Measures> results;

	public EventDetectionResult(){
		results=new HashMap<Integer, Measures>();
	}
	
	
	public Measures get(int eventType){
		return results.get(eventType);
	}
	
	public void update(int eventType, double prec, double rec){
		Measures m=get(eventType);
		m.precision=prec;
		m.recall=rec;
	}	
	
	public void update(int eventType, double avgDelay){
		Measures m=get(eventType);
		m.avgDelay=avgDelay;
	}
	
	public void add(int eventType, double prec, double rec, double avgDelay){
		results.put(eventType, new Measures(prec, rec, avgDelay));
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("Parking: "+get(Constants.PARKING_ACTIVITY)+"\n");
		sb.append("Unparking: "+get(Constants.UNPARKING_ACTIVITY)+"\n");
		return sb.toString();
	}
}
