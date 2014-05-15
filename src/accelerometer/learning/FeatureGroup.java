package accelerometer.learning;

import java.util.ArrayList;


public class FeatureGroup{
	public int idx;
	//0: parking; 1:unparking; 2:none
	public static int PARKING=0;
	public static int UNPARKING=1;
	public static int NONE=2;
	
	public ArrayList<ArrayList<Double>> values;
	public FeatureGroup(int idx){
		this.idx=idx;
		values=new ArrayList<ArrayList<Double>>();
	}
}
