package accelerometer;


import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import javax.sound.sampled.Line;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import main.Constants;
import main.CommonUtils;
import com.google.common.base.*;
/*
 * A class that process accelerometer data for differing purposes
 */


public class AccelerometerFileProcessing {

	public static void main(String[] args) throws Exception{
		File dir=new File(Constants.ACCELEROMETER_RAW_DATA_DIR);
		
		createTrainingFileForMotionStateClassifier();
		
		String fn;
		for(File f: dir.listFiles()){ 
			if(f.getName().contains(".log")){
				//fn=f.getName();
				//discardDuplicateReadings(Constants.ACCELEROMETER_DIR+fn, Constants.BASE_DIR+fn);
				//createWekaInputFile(Constants.ACCELEROMETER_DIR+fn, Constants.ACCELEROMETER_DIR+fn.replaceAll(".log", ".arff"));
			}
		}
		
		//fn="accelerometer2013_08_295.log";
		fn="ACCELEROMETER_FEATURE_2013_10_151.log";
		
		//discardDuplicateReadings(Constants.ACCELEROMETER_DIR+fn, Constants.BASE_DIR+fn);
		//createWekaInputFile(Constants.ACCELEROMETER_FEATURES_DIR+fn, Constants.ACCELEROMETER_FEATURES_DIR+fn.replaceAll(".log", ".arff"), false);
		
		//readWeatherSignalLogs("C:/Users/Shuo/Desktop/WeatherLog_Oct_21,_2013_75906_PM.csv");
		
		/*extractInVehicleDurationFromLabeledFeatureFile(Constants.ACCELEROMETER_FEATURES_DIR
				+Constants.PHONE_POSITIONS[AccelerometerSignalProcessing.POSITION_DIR_IDX]+"/all/"
				+"ACCELEROMETER_FEATURE_2013_11_011.arff");*/
	}
	
	/*
	 * This method reads a minimally (i.e. only at the edge points) activity (i.e. on_foot/in_vehicle) labeled 
	 * accelerometer data file and automatically add activity labels for unlabeled readings. 
	 * "o": on_foot
	 * "i": in_vehicle
	 * "s": still
	 * "h": hold_in_hand_on_foot
	 * @removeFilter: if set true, the output file cuts the date-time field of the input file
	 */
	public static void createWekaInputFile(String inputFile, String outputFile, boolean removeFilter){
		try{
			String lastActivity="o";
			
			int FIELD_LENGTH_OF_LABELED_RECORD=7;
			
			Scanner sc=new Scanner(new File(Constants.ACCELEROMETER_RAW_DATA_DIR+"arff_header.txt"));
			FileWriter fw=new FileWriter(outputFile);
			
			//write the arff header
			while(sc.hasNextLine()){
				String line=sc.nextLine().trim();
				if(removeFilter){
					if(line.contains("date")||line.contains("time"))
						continue;
				}
				fw.write(line+"\n");
			}
			sc.close();		
			
			//complete the activity label
			sc=new Scanner(new File(inputFile));
			
			while(sc.hasNextLine()){
				
				String line=sc.nextLine().trim();
				
				//empty or groud_truth lines				
				if(!line.contains(":")||line.startsWith("#")) continue; 

				String[] fields=line.split(" ");
				
				int startFieldIdx;
				if(removeFilter) startFieldIdx=2;
				else startFieldIdx=0;
				
				//if not activity labeled
				if(fields.length<FIELD_LENGTH_OF_LABELED_RECORD)
					fw.write(CommonUtils.cutField(line, " ", startFieldIdx).replaceAll(" ", ",")+","+lastActivity+"\n");
				else{
					fw.write(CommonUtils.cutField(line, " ", startFieldIdx).replaceAll(" ", ",")+"\n" );
					lastActivity=fields[FIELD_LENGTH_OF_LABELED_RECORD-1];
				}
			}
			sc.close();
			fw.close();
			
			System.out.println("Complete activity labeld file was written to \""+CommonUtils.getFileName(outputFile)+"\"");
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	
	
	
	/*
	 * In the case when features are changed
	 * Read u/p labels from the labeled file with old features 
	 */
	public static ArrayList<ArrayList<Integer>> transferLabeledFeatureFiles(String labeledFeatureFilePath){
		ArrayList<ArrayList<Integer>> labeledLines=new ArrayList<ArrayList<Integer>>();
		labeledLines.add(new ArrayList<Integer>()); 
		labeledLines.add(new ArrayList<Integer>()); 
		try{
			File inputFile=new File(labeledFeatureFilePath);
			if(!inputFile.exists()) return labeledLines;
		
			Scanner sc=new Scanner(inputFile);
			String line;
			String[] fields;
			while(sc.hasNextLine()){
				line=sc.nextLine().trim();
				if(line.startsWith("@") | line.startsWith("#") | !line.contains(",")) continue;
				
				fields=line.split(",");
				String label=fields[fields.length-1];
				if(label.equals("u")||label.equals("p")){
					int time=Integer.parseInt(fields[0]);
					if(label.equals("u")) labeledLines.get(Constants.UNPARKING_ACTIVITY).add(time); //add a unparking
					else labeledLines.get(Constants.PARKING_ACTIVITY).add(time); //add a parking
				}
			}
			sc.close();
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return labeledLines;
	}
	
	
	/*
	 * extract In_Vehicle durations from labeled feature files
	 * and then update the corresponding line the raw file, since there may be annotation error in raw file
	 */
	public static String extractInVehicleDurationFromLabeledFeatureFile(String labeledFeatureFilePath){
		String updateDurationsString="";
		String lineInSecs="";
		try{
			ArrayList<ArrayList<Integer>> labeledLines=transferLabeledFeatureFiles(labeledFeatureFilePath);
			
			//scan through labelLines, remove lines that have close timestamps
			for(int i=0;i<2;i++){
				ArrayList<Integer> timestamps=new ArrayList<Integer>();
				for(Integer timestamp: labeledLines.get(i)){
					if(timestamps.size()==0||timestamp-timestamps.get(timestamps.size()-1)>10) timestamps.add(timestamp);
				}
				labeledLines.remove(i);//remove the old timestamps
				labeledLines.add(i, timestamps);// add the new timestamps;
			}
			
			
			if(labeledLines.get(Constants.UNPARKING_ACTIVITY).size()>0){
				updateDurationsString="@ in_vehicle:";
				lineInSecs=updateDurationsString;
			}
			for(int i=0;i<labeledLines.get(Constants.UNPARKING_ACTIVITY).size();i++){
				updateDurationsString+=" "+
						CommonUtils.secondsToHMS(labeledLines.get(Constants.UNPARKING_ACTIVITY).get(i));
				lineInSecs+=" "+labeledLines.get(Constants.UNPARKING_ACTIVITY).get(i);
				if(labeledLines.get(Constants.PARKING_ACTIVITY).size()>i){
					updateDurationsString+="~"+CommonUtils.secondsToHMS(labeledLines.get(Constants.PARKING_ACTIVITY).get(i));
					lineInSecs+="~"+labeledLines.get(Constants.PARKING_ACTIVITY).get(i);
				}
			}
			System.out.println(updateDurationsString);
			System.out.println(lineInSecs);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		if(updateDurationsString.contains("~")) return updateDurationsString+"\n"+lineInSecs;
		else return null;
	}
	
	
	
	/*
	 * This method is used to correct the "old" accelerometer files
	 * which contain duplicated accelerometer records.
	 * (Two accelerometer records are duplicated if they are consecutive in time 
	 * and have the same readings along all three axis.)
	 */	
	public static void discardDuplicateReadings(String inputFile, String outputFile){
		try{
			Scanner sc=new Scanner(new File(inputFile));
			String[] lastReadings=new String[]{"0", "0", "0"};
			int lastTime=0;
			
			FileWriter fw=new FileWriter(outputFile);
			while(sc.hasNextLine()){
				
				String line=sc.nextLine();
				if(!line.contains(":")) continue; //empty lines
				
				if(line.startsWith("#")){
					fw.write(line+"\n");
					continue;
				}

				String[] fields=line.split(" ");
				int curTime=CommonUtils.HMSToSeconds(fields[1]);
				
				//write to the output file
				if (curTime==lastTime
				&&lastReadings[0].equals(fields[2])
				&&lastReadings[1].equals(fields[3])
				&&lastReadings[2].equals(fields[4]) ) {
					//System.out.println(fields[1]);
					continue;
				}
				fw.write(line+"\n");
				lastTime=curTime;
				lastReadings[0]=fields[2];
				lastReadings[1]=fields[3];
				lastReadings[2]=fields[4];	
			}
			sc.close();
			fw.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	
	public static void readWeatherSignalLogs(String filePath){
		try{
			Scanner sc=new Scanner(new File(filePath));
			String line;
			String [] fieldNames=sc.nextLine().trim().split(",");
			String [] fields;
			double curTime,prevTime=0, timeInterval;
			ArrayList<Double> timeIntervals=new ArrayList<Double>();
			while(sc.hasNextLine()){
				line=sc.nextLine();
				fields=line.trim().split(",");
				curTime=Long.parseLong(fields[2].substring(1, fields[2].length()-1));
				if(prevTime>0){
					timeInterval=(curTime-prevTime+0.0)/1000;
					if(timeInterval<10) timeIntervals.add(timeInterval);
				}
				prevTime=curTime;
			}
			Mean mean=new Mean();
			
			System.out.println(timeIntervals);
			System.out.println("avg interval is: "+mean.evaluate(CommonUtils.doubleListToDoubleArray(timeIntervals))+" secs");
		}catch (Exception ex) {
			ex.printStackTrace();
			System.out.println();
		}
	}
	
	/*
	 * need to reduce the # of bins to 5 otherwise the file is too big for android to train
	 * also limits # of jogging and walking instances
	 */
	public static void createTrainingFileForMotionStateClassifier(){
		try{
			String folder=Constants.ACCELEROMETER_BASE_DIR+File.separator+"motion_state"+File.separator;
			Scanner sc=new Scanner(new File(folder+"state_arff_header.txt"));
			String line;
			String outputFile=folder+"state_combined.arff";
			FileWriter fw=new FileWriter(outputFile);
			//append the arff header first
			while(sc.hasNextLine()){
				line=sc.nextLine();
				fw.write(line+"\n");
			}
			
			String[] fields;
			//append instances from WISDM_Act_v1.1 dataset		

			sc=new Scanner(new File("E:/program_data/Activity/WISDM_ar_v1.1/WISDM_ar_v1.1_transformed.arff"));
			int jogCnt=0, walkCnt=0;
			while(sc.hasNextLine()){
				line=sc.nextLine().trim();
				if(line.startsWith("@")||!line.contains(",")) continue;
				fields=line.split(",");
				int[] omitFieldIdx={0,1,35,36,37,38,39,40};
				int j=0;
				
				
				String state=fields[fields.length-1];
				if(walkCnt==1000) continue;
				if(jogCnt==500) continue;
				
				double sum=0;
				for(int i=0;i<fields.length;i++){
					if(j<omitFieldIdx.length&&i==omitFieldIdx[j]){
						j++;
						continue;
					}
					
					if(i<=31&&i%2==0){
						sum=Double.parseDouble(fields[i]); 
					}
					else{
						if(i<=31){
							sum+=Double.parseDouble(fields[i]);
							fw.write(String.format("%.2f",sum) );
						}else{
							if(fields[i].equals("Walking")) walkCnt+=1;
							if(fields[i].equals("Jogging")) jogCnt+=1;
							fw.write(fields[i]);
						}
						
						if(i!=fields.length-1) fw.write(",");
						else fw.write("\n");
					}
				}
			}
			System.out.println("Training file created for motion state classifier at "+outputFile);
			sc.close();
			fw.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	

}
