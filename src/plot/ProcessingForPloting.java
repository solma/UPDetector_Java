package plot;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

import javax.xml.datatype.Duration;

import org.apache.commons.math3.analysis.solvers.BracketedUnivariateSolver;

import com.google.common.base.Objects.ToStringHelper;

import main.CommonUtils;
import main.Constants;

/**
 * a class contains miscellaneous I/O functions
 * that process raw sensor readings for the purpose plotting
 */
public class ProcessingForPloting {
	
	public static void main(String[] args){
		/*String date = "2013_11_23";
		fuseOrientationAndLinearAcceleration(
				Constants.PLOTING_BACK_AND_FORTH_MOVEMENT_BASE + date + "/"	+ date + "1_Orientation_Sensor.log",
				Constants.PLOTING_BACK_AND_FORTH_MOVEMENT_BASE + date + "/"	+ date + "1_Linear_Acceleration_Sensor.log");*/
		countNoOfSamplesAndComputeAverage();
	}
	
	/**
	 * @param: orientationFile: path to the raw orientation sensor readings
	 * @param: accelFile: path to the raw linear acceleration sensor readings
	 * phone is placed vertically (i.e. Y axis perpendicular to the ground) such that 
	 * the front screen (i.e. Z axis) faces the front end of the car.
	 * The car is facing North, so the readings of orientation sensor on Z axis should be around 360/0 degree. 
	 * @return: 
	 * The orientation of acceleration on Z axis
	 */
	public static void fuseOrientationAndLinearAcceleration(String orientationFile, String accelFile){
		try{
			//save the Z axis readings and its timestamp
			//if Z axis is positive, then true; otherwise false
			HashMap<Long, Boolean> accelOnZAxis=new HashMap<Long, Boolean>();
			int Z_AXIS_FIELD_IDX=4;
			Scanner sc=new Scanner(new File(accelFile));
			while(sc.hasNextLine()){
				String[] fields=sc.nextLine().trim().split(" ");
				long timestamp=CommonUtils.HMSSToMillseconds(fields[1]);
				accelOnZAxis.put(timestamp, Float.parseFloat(fields[Z_AXIS_FIELD_IDX])>0);
			}
			sc.close();
			//System.out.println(accelOnZAxis.size());
			
			sc=new Scanner(new File(orientationFile));
			FileWriter fw=new FileWriter(orientationFile.replace("_Orientation_Sensor.log", "_Acceleration_Orientation.log"));
			int AZIMUTH_FIELD_IDX=2;
			while(sc.hasNextLine()){
				
				String line=sc.nextLine().trim();
				if(line.startsWith("@")) continue;
				String[] fields=line.split(" ");
				
				long timestamp=CommonUtils.HMSSToMillseconds(fields[1]);
				//System.out.println(timestamp+" ");
				
				int MAX_OFFSET=5;// 5 millisecs
				boolean flag=false;
				for(int j=0;j<MAX_OFFSET;j++){
					for(int i=0;i<2;i++){
						long offsetTimestamp=timestamp;
						if(i>0) offsetTimestamp+=j;
						else offsetTimestamp-=j;
						//System.out.println(offsetTimestamp+" ");
						if(accelOnZAxis.containsKey(offsetTimestamp)){
							if(!accelOnZAxis.get(offsetTimestamp)){
								float azimuth=Float.parseFloat(fields[AZIMUTH_FIELD_IDX]);
								if(azimuth<180) azimuth+=180; // this case
								else azimuth-=180;
								//reverse the orientation
								fields[AZIMUTH_FIELD_IDX]=String.valueOf(azimuth);
								System.out.println(offsetTimestamp+" "+azimuth);
							}
							flag=true;
							break;
						}
					}
					if(flag) break;
				}
				
				fw.write(CommonUtils.join(fields, 0, fields.length, " ")+"\n");
				
			}
			fw.close();
			sc.close();
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * @input: acceleration orientation file, i.e. output of fuseOrientationAndLinearAcceleration()
	 * @output: draw the histograms for samples and compute the average orientation 
	 * in each in_vehicle period
	 */
	public static void countNoOfSamplesAndComputeAverage(){
		try{
			/**
			 * Input variables
			 */
			/*String inputFile=Constants.PLOTING_BACK_AND_FORTH_MOVEMENT_BASE 
					+  "2013_11_23/2013_11_231_Acceleration_Orientation.log";
			int orientationFieldIdx=2;
			int demarkation=200;*/
			
						
			String date="2013_11_20";
			String inputFile=Constants.PLOTING_CAR_BACKING_BASE 
					+  date+"/ACCELEROMETER_RAW_"+date+"1.log";
			
			String outputFile=Constants.PLOTING_CAR_BACKING_BASE+date+"/"+date+"_output.txt";
			FileWriter fw=new FileWriter(outputFile);
			
			int idxOfFieldOfInterest=4;
			int demarkation=0;
			/**
			 * End of input variables
			 */
			
			Scanner sc=new Scanner(new File(inputFile));
			String line;
			String[] fields;
			
			HashMap<Integer, GroundTruthPeriod> durations=new HashMap<Integer, GroundTruthPeriod>();
			ArrayList<Integer> startTimes=new ArrayList<Integer>();
			int startTimeIdx=0;
			boolean firstLine=true;
			
			while(sc.hasNextLine()){
				line=sc.nextLine().trim();
				fields=line.split(" ");
				if(fields.length<2) continue;
				if(line.startsWith("@")){//a ground truth line
					for(int i=2;i<fields.length;i++){
						String[] timestamps=fields[i].split("~");
						int start=CommonUtils.HMSToSeconds(timestamps[0]);
						int end=CommonUtils.HMSToSeconds(timestamps[1]);
						int type=-1;
						//System.out.println(fields[1]);
						switch(fields[1]){
						case "forward:":
							type=GroundTruthPeriod.FORWORD;
							break;
						case "backward:":
							type=GroundTruthPeriod.BACKWORD;
							break;
						case "normal_drive:":
							type=GroundTruthPeriod.NORMAL_DRIVING;
							break;
						}
						durations.put(start, new GroundTruthPeriod(start, end, type) );
						startTimes.add(start);
					}
				}else{//sensor reading line
					if(firstLine){
						firstLine=false;
						Collections.sort(startTimes);
					}
					int timeStamp=CommonUtils.HMSToSeconds(fields[1]);
					if(startTimeIdx<startTimes.size()&&timeStamp>startTimes.get(startTimeIdx)){
						startTimeIdx++;
					}					
					if(startTimeIdx<1) continue;
					
					GroundTruthPeriod gtp=durations.get( startTimes.get(startTimeIdx-1) );
					double orientation=Double.parseDouble(fields[idxOfFieldOfInterest]);
					gtp.avgOrientation+=orientation;
					if(orientation>demarkation) gtp.noOfSamplesAboveThreshold+=1;
					else gtp.noOfSamplesBelowThreshold+=1;
				}
			}
			for(int startTime: startTimes){
				GroundTruthPeriod gtp=durations.get(startTime);
				gtp.noOfTotalSamples=(gtp.noOfSamplesAboveThreshold+gtp.noOfSamplesBelowThreshold);
				gtp.avgOrientation=gtp.avgOrientation/gtp.noOfTotalSamples;
				String msg=gtp.toString();
				System.out.println(msg);
				fw.write(msg+"\n");
			}
			fw.close();
			sc.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
}


class GroundTruthPeriod{
	//type enums
	public static final int FORWORD=0;
	public static final int BACKWORD=1;
	public static final int NORMAL_DRIVING=2;
	
	public int startTime;
	public int endTime;
	public double avgOrientation;
	public int noOfSamplesAboveThreshold;
	public int noOfSamplesBelowThreshold;
	public int noOfTotalSamples;
	public int type;
	public GroundTruthPeriod(int start, int end, int t){
		startTime=start;
		endTime=end;
		type=t;
	}
	
	public String header(){
		return "type,start_time,end_time,PercentOfAbove,PercentOfBelow,total#,avg";
	}
	
	public String toString(){
		String tp="";
		switch(type){
		case FORWORD:
			tp="forward";
			break;
		case BACKWORD:
			tp="backward";
			break;
		case NORMAL_DRIVING:
			tp="normal_drive";
			break;
		}
		return String.format("%13s", tp)+","
		+CommonUtils.secondsToHMS(startTime)+","+CommonUtils.secondsToHMS(endTime)
		+","+String.format("%.2f", (noOfSamplesAboveThreshold+0.0)/noOfTotalSamples)
		+","+String.format("%.2f", (noOfSamplesBelowThreshold+0.0)/noOfTotalSamples)
		+","+noOfTotalSamples
		+","+String.format("%.2f", avgOrientation);
	}
}
