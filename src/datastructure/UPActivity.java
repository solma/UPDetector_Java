package datastructure;

import java.util.ArrayList;
import java.util.HashMap;

import main.CommonUtils;
import main.Constants;

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
	
	public UPActivity(SOURCE source, int type, String date, String timeInHMS){
		this(source, type);
		this.date=date;
		this.timeInHMS=timeInHMS;
		this.dateTime=date+"-"+timeInHMS;
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
			equal=date.equals(o.date)&&timeDiffWithinThreshold(o);
			break;
		default:
			break;
		}
		return equal;
	}
	
	
	public static int DEFAULT_TIME_DIFF_THRSHOLD=5;//seconds
	public boolean timeDiffWithinThreshold(UPActivity o, int diffThreshold){
		if(Math.abs(timeInSecOfDay-o.timeInSecOfDay)<diffThreshold) return true;
		else return false;
	}
	
	public boolean timeDiffWithinThreshold(UPActivity o){
		return timeDiffWithinThreshold(o, DEFAULT_TIME_DIFF_THRSHOLD);
	}
	
	
	
	/**
	 * 
	 * @param groudtruh: sorted based on dateTime
	 * @return find the ground truth event that happens in most recent past (if the distance is larger than threshold, return null)
	 */
	public UPActivity matchToGroundtruthEvent(ArrayList<UPActivity> groudtruth, int matchTimeDiffThreshold) {
		if(source==SOURCE.GROUND_TRUTH){
			System.err.println("Error: source cannot be groundtruth");
			return null;
		}
		int l=-1, r=groudtruth.size();
		while(l+1!=r){
			int m=l+(r-l)/2;
			if(compareTo(groudtruth.get(m))<0) r=m;
			else l=m;
		}
		if(l>=0&&timeDiffWithinThreshold(groudtruth.get(l), matchTimeDiffThreshold)){
			System.out.println(this+" matched to groundtruth event: "+groudtruth.get(l));
			return groudtruth.get(l);
		}
		return null;
		
	}
	
	/**
	 * 
	 * @param detectedEvents
	 * @return
	 */
	public UPActivity matchToDetectedEvent(ArrayList<UPActivity> detectedEvents, int matchTimeDiffThreshold) {
		if(source!=SOURCE.GROUND_TRUTH){
			System.err.println("Error: source has to be groundtruth");
			return null;
		}
		int l=-1, r=detectedEvents.size();
		while(l+1!=r){
			int m=l+(r-l)/2;
			if(compareTo(detectedEvents.get(m))>0) l=m;
			else r=m;
		}
		if(r<=detectedEvents.size()&&timeDiffWithinThreshold(detectedEvents.get(l), matchTimeDiffThreshold)){
			System.out.println(this+" matched to groundtruth event: "+detectedEvents.get(l));
			return detectedEvents.get(l);
		}
		return null;
	}


	public String toString(){
		return date+"-"+timeInHMS;
	}
	
	public String toDetailString(){
		return source+"-"+(type==Constants.PARKING_ACTIVITY?"Parking":"Unparking")+"-"
				+date+"-"+timeInHMS;
	}
	
}
