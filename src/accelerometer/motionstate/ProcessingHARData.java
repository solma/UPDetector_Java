package accelerometer.motionstate;

import helper.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;


/*
1 WALKING
2 WALKING_UPSTAIRS
3 WALKING_DOWNSTAIRS
4 SITTING
5 STANDING
6 LAYING
*/

public class ProcessingHARData {
	public static String BASE_DIR=Constants.ACCELEROMETER_DAILY_ACTIVITY_EXTERNAL_HAR_DIR;
	
	public static void main(String[] args) {
		recreateRawAccelerometerReadings("C:/Users/Shuo/Desktop/temp.txt");
	}
	
	/**
	 * vector: indexed by the line no.
	 * 
	 */
	public static HashMap<Integer, Vector> loadLabeledVectors(String filepath){
		HashMap<Integer,Vector> vectors=new HashMap< Integer,Vector>();
		try {
			Scanner sc=new Scanner(new File(filepath));
			int lineCnt=1;
			while(sc.hasNextLine()){
				vectors.put(lineCnt++, new Vector(lineCnt, Integer.parseInt(sc.nextLine())) );
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return vectors;
		
	}
	
	/**
	 * Combine the train\Inertial_Signals\total_x, y, z to create original accelerometer readings
	 */
	public static void recreateRawAccelerometerReadings(String outputFilePath){
		String[] axis = { "x", "y", "z" };
		HashMap<Integer, Vector> vectors = loadLabeledVectors(BASE_DIR
				+ "train/y_train.txt");

		ArrayList<ArrayList<Double>> rawReadings = new ArrayList<ArrayList<Double>>();
		Scanner sc;
		try {
			for (String ax : axis) {
				String filepath = BASE_DIR
						+ "/train/Inertial_Signals/total_acc_" + ax
						+ "_train.txt";
				String line;

				sc = new Scanner(new File(filepath));
				int lineCnt = 0;
				int walkingLineCnt = 0;
				while (sc.hasNextLine()) {
					line = sc.nextLine().trim().replaceAll("\\s+", " ");
					lineCnt++;
					if (vectors.get(lineCnt).activityType != 1) {// not walking
																	// state
						continue;
					}
					String[] readings = line.split(" ");
					if (readings.length != 128){
						System.out.println(ax+" "+readings.length);
						continue;
					}

					for (int i = 0; i < readings.length; i++) {
						if (ax.equals("x")) {
							rawReadings.add(new ArrayList<Double>());
						}
						rawReadings.get(walkingLineCnt * 128 + i).add(
								Double.parseDouble(readings[i]) * 9.8);
					}
					walkingLineCnt++;

				}

			}

			FileWriter fw = new FileWriter(outputFilePath);
			for (ArrayList<Double> readings : rawReadings) {
				for(int i=0;i<readings.size();i++){
					if(i>0) fw.write(",");					
					fw.write(String.format("%.3f", readings.get(i)) );
				}
				fw.write("\n");
			}
			fw.close();
			System.out.println("# of lines extracted = " + rawReadings.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

class Vector{
	public int lineIdx;
	public int activityType;
	public double[][] accel_array;//each vector has 128 readings/elements
	public Vector(int idx, int activity){
		this.lineIdx=idx;
		this.activityType=activity;
		accel_array=new double[3][128];
	}
}
