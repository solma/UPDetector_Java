package upactivity;
import helper.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.math3.analysis.function.Add;

import upactivity.UPActivity.SOURCE;





public class UPActivitiesOfSameSource {
	SOURCE source;
	HashMap<Integer, ArrayList<UPActivity>> events;

	
	public UPActivitiesOfSameSource(SOURCE source){
		this.source=source;
		events=new HashMap<Integer, ArrayList<UPActivity>>();
		events.put(Constants.UNPARKING_ACTIVITY, new ArrayList<UPActivity>());
		events.put(Constants.PARKING_ACTIVITY, new ArrayList<UPActivity>());
	}
	
	public ArrayList<UPActivity> get(int type){
		if(!events.containsKey(type)){
			System.err.println("Error: Unknown key="+type);
			return null;
		}
		return events.get(type);
	}
	
	public void addAll(UPActivitiesOfSameSource other){
		if(source!=other.source){
			System.err.println("cannot add new list to the list: incompatible source");
			return;
		}
		for(int i=0;i<2;i++){
			events.get(i).addAll(other.events.get(i));
		}
		//sort();
	}
	
	public void add(UPActivity activity){
		if(source!=activity.source){
			System.err.println("cannot add new activity to the list: incompatible source");
			return;
		}
		events.get(activity.type).add(activity);
	}
	
	public void sort(){
		for(Integer key: events.keySet()){
			Collections.sort(events.get(key));
		}
	}
	
	public int size(){
		return events.get(0).size();
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("********* "+source+" ***********\n");
		
		for(Integer key: events.keySet()){
			sb.append( (key==Constants.PARKING_ACTIVITY?"Parking: ":"Unparking: "));
			for(UPActivity event: events.get(key)){
				sb.append(event.date+"-"+event.timeInHMS+" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	
}
