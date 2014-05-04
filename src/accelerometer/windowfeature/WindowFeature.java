package accelerometer.windowfeature;

import helper.CommonUtils;
import helper.Constants;

import java.util.ArrayList;

import accelerometer.Config;


public class WindowFeature {

	public int[] temporalWindow;//the start and end time index of the window
	public int timeIndex; //the middle time index of the window
	
	//each axis has three values: variance of the values in 1st and 2nd half window and the whole window, respectively
	public ArrayList<ArrayList<Double>> varianceSeries; 
	
	//each axis has three values: average of the values in 1st and 2nd half window and the whole window, respectively
	public ArrayList<ArrayList<Double>> averageSeries;
	
	/**
	 * additional fields for classifying motion states
	 * reference: http://www.cis.fordham.edu/wisdm/public_files/sensorKDD-2010.pdf
	 * */
	// For X, Y, Z and aggregate axis
	public ArrayList<ArrayList<Double>> timeIntervalBtwPeaks;
	public ArrayList<ArrayList<Double>> binPercents;
	
	
	public WindowFeature(){
		temporalWindow=new int[2];
		varianceSeries=new ArrayList<ArrayList<Double>>(Constants.AXIS_NUMBER); 
		for(int i=0;i<Constants.AXIS_NUMBER;i++) varianceSeries.add(new ArrayList<Double>());
		
		averageSeries=new ArrayList<ArrayList<Double>>(Constants.AXIS_NUMBER); 
		for(int i=0;i<Constants.AXIS_NUMBER;i++) averageSeries.add(new ArrayList<Double>());
		
		binPercents=new ArrayList<ArrayList<Double>>(Constants.AXIS_NUMBER); 
		for(int i=0;i<Constants.AXIS_NUMBER;i++) binPercents.add(new ArrayList<Double>());
	}
	
	public WindowFeature(int[] window){
		this();
		for(int i=0;i<window.length; i++) temporalWindow[i]=window[i];
		this.timeIndex=temporalWindow[0]+(temporalWindow[1]-temporalWindow[0])/2;
	}
	
	
	
	public boolean equals(Object other){
		WindowFeature feature=(WindowFeature)other;
		return toString().equals(feature.toString());
	}
	
	
	/**
	 * @return a string feature for motion state classification
	 */
	public String asMotionStateFeatures(){
		StringBuilder sb=new StringBuilder();
		
		//format needs to be compatible WISDM_Act_v1.1 dataset
		String dFormat="%.2f";
		for(int axisIdx=0;axisIdx<3;axisIdx++){
			for(int i=0;i<Config.NO_OF_BINS;i++){
				sb.append(String.format(dFormat, binPercents.get(axisIdx).get(i))+",");
			}
		}
		for(int axisIdx=0;axisIdx<3;axisIdx++){
			sb.append(String.format(dFormat, averageSeries.get(axisIdx).get(2))+",");	
		}
		for(int axisIdx=0;axisIdx<3;axisIdx++){
			//note it is standard deviation not variance
			//in order to be compatible WISDM_Act_v1.1 dataset
			sb.append(String.format(dFormat, Math.sqrt(varianceSeries.get(axisIdx).get(2)) )+",");
		}
		sb.append(String.format(dFormat,averageSeries.get(3).get(2))) ;//append the resultant
		
		return sb.toString();
	}
	
	
	public String toString(int noOfAxes){
		StringBuilder sb=new StringBuilder();
		sb.append(CommonUtils.secondsToHMS(timeIndex)+" ");
		
		for(int axisIdx=0;axisIdx<noOfAxes;axisIdx++){
			for(int i=axisIdx;i<Math.min(axisIdx+1, Constants.AXIS_NUMBER);i++){
				ArrayList<Double> values=varianceSeries.get(i);
				//output the variance
				sb.append(String.format("%.3f", values.get(0))+" "+String.format("%.3f", values.get(1)));
				//output average
				values=averageSeries.get(i);
				sb.append(" "+String.format("%.3f", values.get(0))+" "+String.format("%.3f", values.get(1))+" ");
			}
		}
		
		//sb.append("\n");
		return sb.toString();
	}
	

}
