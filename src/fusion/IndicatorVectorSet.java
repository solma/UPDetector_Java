package fusion;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math3.analysis.function.Add;


public class IndicatorVectorSet {
	public HashMap<Integer, IndicatorVector> vectorSet;
	public static int FUSION_INDICATOR_TIME_INTERVAL=60; //in secs
	
	
	public IndicatorVectorSet(){
		vectorSet=new HashMap<Integer, IndicatorVector>();
	}
	
	public void add(IndicatorVector iv){
		vectorSet.put(iv.indicator, iv);
		//remove stale indicators
		ArrayList<Integer> keysToBeRemoved=new ArrayList<Integer>();
		for(Integer indicator: vectorSet.keySet()){
			if(indicator!=iv.indicator && iv.secondOfDay-vectorSet.get(indicator).secondOfDay>FUSION_INDICATOR_TIME_INTERVAL){
				keysToBeRemoved.add(indicator);
			}
		}
		for(Integer indicator: keysToBeRemoved){
			vectorSet.remove(indicator);
		}
	}
}
