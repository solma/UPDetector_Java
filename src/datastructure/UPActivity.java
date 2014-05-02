package datastructure;

import java.util.ArrayList;

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
		GROUND_TRUTH("Groundtruth"), GOOGLE_API("GoogleAPI"),
		FUSION_DETECTION("Fusion_Result"), 
		FUSION_AND_GOOGLE_API("Fusion_Result_Confirmed_By_GoogleAPI");
		
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
	
	public UPActivity(SOURCE source, int type, int secs){
		this(source, type);
		this.timeInSecOfDay=secs;
		this.timeInHMS=CommonUtils.secondsToHMS(secs);
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
	public UPActivity matchToGroundtruthEvent(ArrayList<UPActivity> groudtruh) {
		if(source==SOURCE.GROUND_TRUTH){
			System.err.println("Error: source cannot be groundtruth");
			return null;
		}
		int l=-1, r=groudtruh.size();
		while(l+1!=r){
			int m=l+(r-l)/2;
			if(compareTo(groudtruh.get(m))<0) r=m;
			else l=m;
		}
		if(l>=0&&timeDiffWithinThreshold(groudtruh.get(l), 120)){
			System.out.println("Matched to groundtruth event: "+groudtruh.get(l));
			return groudtruh.get(l);
		}
		return null;
		
	}
	
	
	public String toString(){
		return date+" "+timeInHMS;
	}
	
	public String toDetailString(){
		return source+" "+(type==Constants.PARKING_ACTIVITY?"Parking ":"Unparking ")
				+date+" "+timeInHMS;
	}
	
}
