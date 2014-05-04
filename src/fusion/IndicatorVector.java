package fusion;
import helper.CommonUtils;


public class IndicatorVector{
	int secondOfDay;
	String timeInHMS; 
	double[] features;
	int indicator;
	
	String classifierClass;
	
	public IndicatorVector(double[] features, int indicator){
		this.features=features;
		this.indicator=indicator;
	}
	
	public IndicatorVector(String timeInHMS, double[] features, int indicator){
		this(features, indicator);
		this.timeInHMS=timeInHMS;
		this.secondOfDay=CommonUtils.HMSToSeconds(timeInHMS);
	}
	
	public IndicatorVector(int timeInSec, double[] features, int indicator){
		this(features, indicator);
		this.secondOfDay=timeInSec;
		this.timeInHMS=CommonUtils.secondsToHMS(timeInSec);
	}
	
	public IndicatorVector(String timeInHMS, double[] features, int indicator, String cls){
		this(timeInHMS, features, indicator);
		this.classifierClass=cls;
	}
	
	public String toString(){
		String ret=timeInHMS;
		for(double d:features){
			ret+=","+String.format("%7.3f", d);
		}
		ret+=","+classifierClass;
		return ret;
	}
}