package helper;

import accelerometer.EventDetectionResult;
import upactivity.UPActivitiesOfSameSource;


public class DetectionMethod{
	public String  name;
	public UPActivitiesOfSameSource detectedEvents;
	public EventDetectionResult result;
	
	public DetectionMethod(String name, EventDetectionResult edr){
		this.name=name;
		result=edr;
	}
	
	public DetectionMethod(String name, EventDetectionResult edr, UPActivitiesOfSameSource detectedEvents){
		this(name, edr);
		this.detectedEvents=detectedEvents;
	}
	
	public String printResult(){
		return name+"\n"+result.toString();
	}
}
