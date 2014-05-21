package upactivity;

import helper.CommonUtils;
import helper.Constants;

import java.util.ArrayList;
import java.util.HashMap;


import com.google.common.base.Objects.ToStringHelper;

/**
 * abstraction of parking/unparking activities
 * @author Sol
 *
 */
public class UPActivity implements Comparable<UPActivity> {
	
	public enum SOURCE{
		GROUND_TRUTH("Groundtruth"), 
		MST_GOOGLE("MST_Google"), MST_WEKA("MST_WEKA"),
		CIV("CIV"),
		CIV_MST_WEKA("CIV_MST_WEKA"), 
		CIV_MST_WEKA_AND_GOOGGLE("CIV_MST_WEKA_AND_GOOGGLE");
		
		String name;
		
		SOURCE(String name){
			this.name=name;
		}
		
		public String toString(){
			return name;
		}
	}
	
	public int type;
	public SOURCE source;
	
	public String date;
	public String timeInHMS;
	public int timeInSecOfDay;
	public String dateTime;
	
	public double confidence;
	
	public UPActivity(SOURCE source, int type){
		this.source=source;
		this.type=type;
	}
	
	
	public static String DATE_TIME_DELIMETER="-";
	
	public UPActivity(SOURCE source, int type, String date, String timeInHMS){
		this(source, type);
		this.date=date;
		this.timeInHMS=timeInHMS;
		this.dateTime=date+DATE_TIME_DELIMETER+timeInHMS;
		this.timeInSecOfDay=CommonUtils.HMSToSeconds(timeInHMS);
	}
	
	public UPActivity(SOURCE source, int type, String date, String timeInHMS, double conf){
		this(source, type, date, timeInHMS);
		this.confidence=conf;
	}
	
	public UPActivity(SOURCE source, int type, String date, int secs){
		this(source, type, date, CommonUtils.secondsToHMS(secs));
	}

	@Override
	public int compareTo(UPActivity o) {
		return dateTime.compareTo(o.dateTime);
	}
	
	public boolean equals(UPActivity o){
		boolean equal=false;
		switch(o.source){
		case GROUND_TRUTH:
			equal=date.equals(o.date)&&timeDiffWithinThreshold(o, 5); // within 5 secs considere equal
			break;
		default:
			break;
		}
		return equal;
	}
	
	
	public boolean timeDiffWithinThreshold(UPActivity o, int diffThreshold){
		if(Math.abs(timeInSecOfDay-o.timeInSecOfDay)<diffThreshold) return true;
		else return false;
	}
	
	
	/**
	 * 
	 * @param groudtruh: sorted based on dateTime
	 * @return find the ground truth event that happens in most recent past defined by 
	 * temporal interval (curtime-upperbound, curtime-lowerbound)
	 */
	public UPActivity matchToGroundtruthEvent(ArrayList<UPActivity> groudtruth, int timeDiffUpperBound, int timeDiffLowerBound) {
		if(source==SOURCE.GROUND_TRUTH){
			System.err.println("Error: source cannot be groundtruth");
			return null;
		}
		int idxOfLastSmaller=binarySearchLastEarlier(groudtruth, this.dateTime);
		
		UPActivity matchedGTEvent;
		int idx=idxOfLastSmaller;
		while(idx<groudtruth.size()&&idx>=0){
			matchedGTEvent=groudtruth.get(idx);
			if(this.timeInSecOfDay-matchedGTEvent.timeInSecOfDay>timeDiffUpperBound) break;
			if(this.timeInSecOfDay-matchedGTEvent.timeInSecOfDay>=timeDiffLowerBound){
				//System.out.println(this+" matched to groundtruth event: "+matchedGTEvent);
				return matchedGTEvent;
			}
			idx--;
		}
		return null;
	}
	

	/**
	 * 
	 * @param activities: a list of activities
	 * @param timeOfSec: to be searched
	 * @return: the last activity whose timestamp is smaller or equal than timeOfSec
	 */
	public static int binarySearchLastEarlier(ArrayList<UPActivity> activities, String dateTime){
		int l=-1, r=activities.size();
		while(l+1!=r){
			int m=l+(r-l)/2;
			UPActivity act=activities.get(m);
			if(act.dateTime.compareTo(dateTime)>0) r=m;
			else l=m;
		}
		return l;
	}
	
	public static int binarySearchFirstLater(ArrayList<UPActivity> activities, String dateTime){
		int l=-1, r=activities.size();
		while(l+1!=r){
			int m=l+(r-l)/2;
			UPActivity act=activities.get(m);
			if(act.dateTime.compareTo(dateTime)<0) l=m;
			else r=m;
		}
		return r;
	}
	


	public String toString(){
		return date+"-"+timeInHMS;
	}
	
	public String toDetailString(){
		return source+"-"+(type==Constants.PARKING_ACTIVITY?"Parking":"Unparking")+"-"
				+date+"-"+timeInHMS;
	}
	
}
