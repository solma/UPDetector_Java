package fusion;

import helper.CommonUtils;
import helper.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import java_cup.runtime.lr_parser;


import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import upactivity.UPActivitiesOfSameSource;
import upactivity.UPActivity;
import upactivity.UPActivity.SOURCE;



import accelerometer.AccelerometerSignalProcessing;
import accelerometer.EventDetection;
import accelerometer.learning.ConditionalProbability;
import accelerometer.learning.FeatureGroup;


public class Fusion {
    public static final int HIGH_LEVEL_ACTIVITY_UPARKING=9009;
    public static final int HIGH_LEVEL_ACTIVITY_IODOOR=9008;
	
	
    /**
     * Enums for environments
     */
    public static final int ENVIRON_UNKNOWN=500;
    public static final int ENVIRON_INDOOR=501;
    public static final int ENVIRON_OUTDOOR=502;
    public static final int ENVIRON_SEMI_OUTDOOR=503;
    public static final int INDICATOR_LIGHT_DAY=1009;
    public static final int INDICATOR_LIGHT_NIGHT=1008;
    public static final int INDICATOR_RSS=1007;
    public static final int INDICATOR_MAGNETIC=1006;
	
	public static final int INDICATOR_CIV=1001;
	public static final int INDICATOR_IODOOR=1002;
	public static final int INDICATOR_BLUETOOTH=1003;
	public static final int INDICATOR_ENGINE_START=1004;
	public static final int INDICATOR_MST=1005;
	
	static int[] indicators={INDICATOR_CIV, INDICATOR_IODOOR,INDICATOR_BLUETOOTH,INDICATOR_ENGINE_START
		,INDICATOR_MST};
	
	public static double NOTIFICATION_THRESHOLD=0.9;
		
	public static final int OUTCOME_NONE=100;
	public static final int OUTCOME_PARKING=101;
	public static final int OUTCOME_UNPARKING=102;
	static int[] outcomes={OUTCOME_NONE, OUTCOME_PARKING, OUTCOME_UNPARKING};
	
	
	public static HashMap<Integer, Double> PRIOR_PROBABILITY=new HashMap<Integer, Double>();
	public static HashMap<String, ConditionalProbability> CONDITIONAL_PROBABILITY=new HashMap<String, ConditionalProbability>();
	


	public static boolean DEBUG_ON=false;
	public static ArrayList<ArrayList<String>> detectedEvents=new ArrayList<ArrayList<String>>();
	
	
	/**
	 * Initialize conditional probabilities
	 */
	static{
		//equal prob makes CIV have higher recall lower prec, and vice versa
		PRIOR_PROBABILITY.put(OUTCOME_PARKING, .1); //
		PRIOR_PROBABILITY.put(OUTCOME_UNPARKING, .1); //
		PRIOR_PROBABILITY.put(OUTCOME_NONE, .3); //
	
		/*double[][][][] histograms={
  				//none
  				{ 
  					//CIV
  					{{0.01,0.05,0.89,0.04,0.01},	{0.98,0.02,0.00,0.00,0.00},	{0.00,0.10,0.90,0.00,0.00},	{0.01,0.03,0.89,0.06,0.01},	{0.97,0.02,0.00,0.00,0.00}}
  					//Indoor/Outdoor
  					,{{0.93,0.05,0.01,0.01,0.00},	{1.00,0.00,0.00,0.00,0.00},	{0.90,0.00,0.00,0.00,0.10}}
  				},
  				//parking
  				{
  					//CIV
  					{{0.02,0.02,0.08,0.64,0.24},	{0.68,0.24,0.06,0.00,0.02},	{0.62,0.18,0.10,0.06,0.04},	{0.28,0.56,0.12,0.00,0.04},	{0.70,0.26,0.02,0.00,0.02}}
  					//Indoor/Outdoor
  					,{{0.50,0.27,0.10,0.10,0.03},	{1.00,0.00,0.00,0.00,0.00},	{0.20,0.00,0.00,0.00,0.80}}
  				},
  				//unparking
  				{
  					//CIV
  					{{0.29,0.26,0.26,0.07,0.12},	{0.95,0.00,0.02,0.00,0.02},	{0.10,0.02,0.02,0.26,0.60},	{0.02,0.00,0.05,0.48,0.45},	{0.98,0.00,0.00,0.00,0.02}}
  					//Indoor/Outdoor
  					,{{0.50,0.27,0.10,0.10,0.03},	{1.00,0.00,0.00,0.00,0.00},	{0.20,0.00,0.00,0.00,0.80}}
  				}  				
  		};*/

		double[][][][] observedMinAndMax={
				//none
  				{ 
  					//CIV
  					{{-6.515 ,6.715 },	{0.000 ,363.417 },	{0.000 ,25.238 },	{0.000 ,134.836 },	{-41.293 ,58.983 },	{-7.297 ,6.715 },	{0.000 ,363.417 },	{0.000 ,25.238 },	{0.000 ,134.836 }}
  					//{{-6.515 ,6.715 },	{-41.293 ,58.983 },	{-7.297 ,6.715 }}
  					//Indoor/Outdoor
  					,{{0.000 ,10403.000 },	{0.000 ,0.000 },	{-1.000 ,1.000 }}
  					//bluetooth
  					,{{-1,1}}
  				//engine_start
  					,{{2.242 ,25.880 },	{0.012 ,0.235 },	{0.000 ,0.015 },	{168.200 ,376.900 },	{0.000 ,0.005 },	{0.013 ,0.188 },	{0.000 ,0.045 },	{5.681 ,41.690 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{10.120 ,146.400 },	{2.645 ,8.421 },	{1.669 ,4.579 },	{1.415 ,3.004 },	{1.431 ,3.755 },	{1.235 ,2.268 },	{1.267 ,2.528 },	{1.069 ,2.658 },	{1.091 ,2.627 },	{1.054 ,1.545 },	{0.950 ,2.506 },	{0.840 ,1.900 },	{0.876 ,1.290 },	{0.062 ,0.187 },	{0.138 ,0.418 },	{0.118 ,0.217 },	{0.070 ,0.205 },	{0.067 ,0.155 },	{0.054 ,0.150 },	{0.052 ,0.104 },	{0.053 ,0.149 },	{0.053 ,0.121 },	{0.000 ,0.000 },	{0.019 ,0.437 },	{6.257 ,21.940 },	{391.100 ,1329.000 },	{38300.000 ,125200.000 },	{6827000.000 ,22640000.000 },	{8.780 ,42.440 },	{0.047 ,0.343 },	{0.000 ,0.010 },	{1382.000 ,1535.000 },	{0.000 ,0.007 },	{0.010 ,0.259 },	{0.000 ,0.891 },	{20.180 ,109.200 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{-186.300 ,-108.000 },	{0.247 ,19.810 },	{-2.461 ,4.957 },	{-3.318 ,1.516 },	{-2.231 ,1.538 },	{-3.273 ,1.030 },	{-1.821 ,0.607 },	{-2.242 ,0.378 },	{-1.328 ,1.360 },	{-1.887 ,0.036 },	{-1.121 ,-0.046 },	{-1.222 ,0.811 },	{-1.427 ,0.129 },	{-0.977 ,-0.728 },	{-0.094 ,0.763 },	{-0.245 ,0.189 },	{0.026 ,0.304 },	{-0.189 ,0.122 },	{-0.099 ,0.142 },	{-0.171 ,0.077 },	{-0.010 ,0.133 },	{-0.123 ,0.051 },	{0.000 ,0.000 },	{0.020 ,0.514 },	{22.330 ,78.060 },	{1267.000 ,4809.000 },	{139700.000 ,372800.000 },	{22710000.000 ,69170000.000 }}
  					
  					//MST //does not matter much
  					//,{{0.000 ,0.800 },	{0.000 ,1.000 },	{0.000 ,0.800 },	{0.000 ,1.000 }}
  					//,{{0.000 ,0.700 },	{0.000 ,1.000 },	{0.000 ,0.700 },	{0.000 ,1.000 }}
  					,{{0.0 ,1.0 },	{0.0 ,1.0 },	{0.0 ,1.0 },	{0.0 ,1.0 }}
  					
  				},
  				//parking
  				{
  					//CIV
  					{{-0.259 ,0.135 },	{0.000 ,0.414 },	{0.001 ,0.467 },	{0.000 ,0.178 },	{1.005 ,7.324 },	{-0.971 ,5.559 },	{0.449 ,132.937 },	{0.706 ,19.986 },	{0.350 ,95.262 }}
  					//{{-0.259 ,0.135 },	{1.005 ,7.324 },	{-0.971 ,5.559 }}
  					//Indoor/Outdoor
  					,{{0.000 ,10403.000 },	{0.000 ,0.000 },	{-1.000 ,1.000 }}
  				//bluetooth
  					,{{-1,1}}
  				//engine_start
  					,{{19.190 ,46.270 },	{0.160 ,0.267 },	{0.000 ,0.001 },	{167.400 ,312.200 },	{0.000 ,0.001 },	{0.007 ,0.031 },	{0.000 ,0.109 },	{27.080 ,69.420 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{12.260 ,114.600 },	{3.386 ,6.181 },	{2.183 ,3.117 },	{1.839 ,2.839 },	{1.840 ,2.387 },	{1.582 ,2.440 },	{1.627 ,2.323 },	{1.400 ,2.914 },	{1.290 ,2.020 },	{1.239 ,1.624 },	{1.205 ,2.120 },	{1.245 ,2.012 },	{1.027 ,1.972 },	{0.160 ,0.425 },	{0.177 ,0.475 },	{0.155 ,0.264 },	{0.108 ,0.175 },	{0.097 ,0.152 },	{0.090 ,0.197 },	{0.085 ,0.145 },	{0.076 ,0.135 },	{0.070 ,0.134 },	{0.000 ,0.000 },	{0.023 ,0.154 },	{13.340 ,28.690 },	{627.100 ,1311.000 },	{76310.000 ,154100.000 },	{7208000.000 ,16810000.000 },	{35.210 ,74.100 },	{0.250 ,0.472 },	{0.000 ,0.000 },	{1455.000 ,1526.000 },	{0.000 ,0.001 },	{0.006 ,0.023 },	{0.000 ,0.796 },	{91.480 ,152.200 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{-180.400 ,-162.100 },	{-2.617 ,1.613 },	{-0.800 ,2.656 },	{-1.464 ,1.131 },	{-2.109 ,-0.446 },	{-1.363 ,-0.607 },	{-0.387 ,1.456 },	{-1.835 ,0.856 },	{-1.048 ,0.864 },	{-0.971 ,-0.171 },	{-0.985 ,-0.046 },	{-0.577 ,0.988 },	{-0.906 ,0.447 },	{-0.801 ,-0.452 },	{0.017 ,0.261 },	{-0.245 ,-0.028 },	{0.026 ,0.187 },	{-0.175 ,-0.066 },	{-0.020 ,0.198 },	{-0.224 ,0.013 },	{0.027 ,0.200 },	{-0.130 ,-0.027 },	{0.000 ,0.000 },	{0.020 ,0.102 },	{72.040 ,92.680 },	{3836.000 ,4634.000 },	{116600.000 ,242600.000 },	{42020000.000 ,53930000.000 }}
  				
  					//MST
  					//,{{0.200 ,0.700 },	{0.000 ,0.300 },	{0.000 ,0.600 },	{0.000 ,0.800 }}
  					//,{{0.000 ,0.700 },	{0.000 ,0.400 },	{0.000 ,0.300 },	{0.100 ,0.700 }}
  					,{{0.0 ,1.0 },	{0.0 ,1.0 },	{0.0 ,1.0 },	{0.0 ,1.0 }}
  				},
  				//unparking
  				{
  					//CIV
  					{{-2.371 ,3.029 },	{1.466 ,361.423 },	{0.913 ,18.554 },	{0.300 ,95.261 },	{-10.081 ,-1.037 },	{-0.540 ,0.104 },	{0.000 ,2.901 },	{0.001 ,0.720 },	{0.000 ,0.762 }}
  					//{{-2.371 ,3.029 },	{-10.081 ,-1.037 },	{-0.540 ,0.104 }}
  					//Indoor/Outdoor
  					,{{0.000 ,10403.000 },	{0.000 ,0.000 },	{-1.000 ,1.000 }}
  				//bluetooth
  					,{{-1,1}}
  				//engine_start
  					,{{2.242 ,23.780 },	{0.012 ,0.235 },	{0.000 ,0.015 },	{180.400 ,376.900 },	{0.000 ,0.005 },	{0.013 ,0.188 },	{0.000 ,0.045 },	{5.681 ,41.690 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{23.010 ,146.400 },	{2.645 ,8.421 },	{1.669 ,4.579 },	{1.415 ,3.004 },	{1.431 ,2.487 },	{1.235 ,2.268 },	{1.267 ,1.791 },	{1.069 ,1.801 },	{1.091 ,1.763 },	{1.054 ,1.545 },	{0.950 ,1.458 },	{0.840 ,1.465 },	{0.876 ,1.290 },	{0.062 ,0.187 },	{0.138 ,0.274 },	{0.118 ,0.217 },	{0.070 ,0.181 },	{0.067 ,0.134 },	{0.054 ,0.120 },	{0.052 ,0.104 },	{0.053 ,0.104 },	{0.053 ,0.095 },	{0.000 ,0.000 },	{0.035 ,0.437 },	{6.257 ,21.940 },	{391.100 ,1308.000 },	{38300.000 ,125200.000 },	{6827000.000 ,22640000.000 },	{8.780 ,42.440 },	{0.047 ,0.343 },	{0.000 ,0.010 },	{1382.000 ,1504.000 },	{0.000 ,0.007 },	{0.010 ,0.259 },	{0.000 ,0.891 },	{20.180 ,109.200 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{0.000 ,0.000 },	{-186.300 ,-108.000 },	{0.247 ,19.810 },	{-2.461 ,4.957 },	{-3.318 ,1.516 },	{-2.091 ,1.538 },	{-3.273 ,1.030 },	{-1.821 ,0.607 },	{-1.164 ,0.378 },	{-1.328 ,0.136 },	{-1.887 ,0.036 },	{-1.066 ,-0.046 },	{-1.222 ,0.131 },	{-1.427 ,0.129 },	{-0.977 ,-0.728 },	{-0.094 ,0.763 },	{-0.245 ,0.189 },	{0.026 ,0.304 },	{-0.107 ,0.122 },	{-0.099 ,0.142 },	{-0.171 ,0.077 },	{-0.010 ,0.133 },	{-0.123 ,0.051 },	{0.000 ,0.000 },	{0.028 ,0.514 },	{22.330 ,78.060 },	{1267.000 ,4809.000 },	{139700.000 ,372800.000 },	{22710000.000 ,69170000.000 }}
  					
  					//MST
  					//,{{0.000 ,0.700 },	{0.000 ,0.800 },	{0.100 ,0.700 },	{0.000 ,0.400 }}
  					//,{{0.000 ,0.500 },	{0.100 ,0.700 },	{0.000 ,0.700 },	{0.000 ,0.800 }}
  					,{{0.0 ,1.0 },	{0.0 ,1.0 },	{0.0 ,1.0 },	{0.0 ,1.0 }}
  				}		
  		};		
		
		double[][][][] meansAndStds={
			//none
			{ 
				//CIV
				{ {0.042, 1.004},	{7.700, 23.441},	{1.808, 3.116},	{4.387, 13.485},	{0.044, 2.941},	{0.028, 1.045},	{8.012, 24.184},	{1.885, 3.260},	{4.780, 14.264}}
				//{{0.042, 1.004},	{0.044, 2.941},	{0.028, 1.045}}
				
				//Indoor/Outdoor
				,{{402.161, 1152.593},	{0.020, 0.02},	{-0.801, 0.599}}
				//bluetooth
				,{{0, 0.32}}
				//engine_start
				,{{9.932, 5.787},	{0.079, 0.049},	{0.004, 0.004},	{288.400, 46.927},	{0.002, 0.001},	{0.069, 0.036},	{0.005, 0.012},	{22.568, 8.697},	{0.000, 0.000},	{0.000, 0.000},	{0.000, 0.000},	{107.190, 29.523},	{4.686, 1.617},	{2.759, 0.783},	{2.181, 0.415},	{1.938, 0.420},	{1.635, 0.190},	{1.519, 0.235},	{1.407, 0.267},	{1.379, 0.258},	{1.275, 0.137},	{1.236, 0.256},	{1.157, 0.178},	{1.092, 0.099},	{0.123, 0.025},	{0.218, 0.053},	{0.164, 0.028},	{0.113, 0.029},	{0.093, 0.020},	{0.083, 0.021},	{0.075, 0.014},	{0.075, 0.018},	{0.069, 0.014},	{0.000, 0.000},	{0.183, 0.099},	{14.768, 3.574},	{1008.126, 229.294},	{81757.353, 20264.732},	{14364970.588, 3746182.473},	{20.412, 8.703},	{0.130, 0.068},	{0.002, 0.002},	{1451.206, 30.078},	{0.001, 0.002},	{0.061, 0.067},	{0.280, 0.369},	{54.560, 22.439},	{0.000, 0.000},	{0.000, 0.000},	{0.000, 0.000},	{-160.474, 22.244},	{8.044, 4.745},	{0.976, 2.182},	{-0.035, 1.071},	{0.304, 0.907},	{-0.199, 0.876},	{-0.255, 0.543},	{-0.580, 0.463},	{-0.477, 0.461},	{-0.713, 0.449},	{-0.574, 0.292},	{-0.641, 0.432},	{-0.706, 0.310},	{-0.907, 0.056},	{0.346, 0.202},	{0.007, 0.114},	{0.115, 0.061},	{0.029, 0.072},	{0.043, 0.052},	{-0.032, 0.048},	{0.038, 0.027},	{-0.012, 0.031},	{0.000, 0.000},	{0.140, 0.140},	{49.585, 15.142},	{3233.471, 1060.064},	{277470.588, 68800.716},	{51232941.176, 13647687.678}}
				
				//MST
				//,{{0.508, 0.154},	{0.088, 0.126},	{0.508, 0.153},	{0.088, 0.126}}//windowsize=2
				,{{0.432, 0.191},	{0.148, 0.187},	{0.434, 0.190},	{0.147, 0.186}}//windowsize=5
				//,{{0.416, 0.189},	{0.145, 0.190},	{0.418, 0.189},	{0.145, 0.189}}//windowsize=10
			},
			//parking
			{
				//CIV
				{ {0.017, 0.065},	{0.060, 0.080},	{0.068, 0.082},	{0.020, 0.035},	{2.490, 1.502},	{0.941, 1.212},	{21.832, 22.699},	{5.308, 3.361},	{10.549, 16.561}}
				//{{0.017, 0.065},		{2.490, 1.502},	{0.941, 1.212}}
				
				//Indoor/Outdoor
				,{{2498.867, 2833.061},	{0.01, 0.01},	{0.600, 0.814}}
				//bluetooth
				,{{-0.3, 0.48}}
				//engine_start
				,{{9.449, 5.133},	{0.077, 0.048},	{0.004, 0.004},	{292.042, 42.494},	{0.002, 0.001},	{0.071, 0.035},	{0.004, 0.012},	{22.140, 8.460},	{0.000, 0.000},	{0.000, 0.000},	{0.000, 0.000},	{110.131, 24.402},	{4.718, 1.631},	{2.789, 0.775},	{2.173, 0.418},	{1.883, 0.275},	{1.640, 0.191},	{1.488, 0.156},	{1.369, 0.152},	{1.341, 0.136},	{1.273, 0.139},	{1.197, 0.125},	{1.135, 0.122},	{1.088, 0.099},	{0.121, 0.024},	{0.212, 0.040},	{0.163, 0.028},	{0.110, 0.024},	{0.091, 0.018},	{0.081, 0.017},	{0.075, 0.014},	{0.072, 0.012},	{0.068, 0.010},	{0.000, 0.000},	{0.188, 0.096},	{14.788, 3.627},	{998.403, 225.619},	{80634.848, 19475.964},	{14256939.394, 3750098.223},	{19.872, 8.241},	{0.128, 0.068},	{0.002, 0.002},	{1448.667, 26.588},	{0.002, 0.002},	{0.063, 0.067},	{0.270, 0.369},	{53.613, 22.087},	{0.000, 0.000},	{0.000, 0.000},	{0.000, 0.000},	{-159.867, 22.301},	{8.147, 4.780},	{1.005, 2.210},	{-0.046, 1.085},	{0.381, 0.801},	{-0.162, 0.863},	{-0.275, 0.539},	{-0.530, 0.363},	{-0.533, 0.332},	{-0.724, 0.452},	{-0.557, 0.280},	{-0.685, 0.353},	{-0.697, 0.311},	{-0.909, 0.055},	{0.343, 0.204},	{0.008, 0.115},	{0.114, 0.062},	{0.036, 0.061},	{0.041, 0.051},	{-0.028, 0.043},	{0.037, 0.027},	{-0.012, 0.032},	{0.000, 0.000},	{0.144, 0.140},	{49.269, 15.263},	{3241.152, 1075.539},	{279406.061, 68921.082},	{51623939.394, 13664539.633}}
			
				//MST
				//,{{0.378, 0.192},	{0.122, 0.139},	{0.300, 0.141},	{0.178, 0.156}}
				,{{0.514, 0.131},	{0.047, 0.094},	{0.197, 0.163},	{0.344, 0.255}}
				//,{ {0.474, 0.155},	{0.064, 0.106},	{0.151, 0.102},	{0.418, 0.188}}
			},
			//unparking
			{
				//CIV
				{{-0.312, 1.376},	{33.108, 62.395},	{5.522, 3.727},	{13.903, 22.833},	{-3.162, 2.434},	{-0.049, 0.095},	{0.120, 0.449},	{0.066, 0.113},	{0.032, 0.118}}
				//{{-0.312, 1.376},	{-3.162, 2.434},	{-0.049, 0.095}}
				
				//Indoor/Outdoor
				,{{2498.867, 2833.061},	{0.01, 0.01},	{0.600, 0.814}}
				//bluetooth
				,{{0.3, 0.48}}
				//engine_start
				,{{28.860, 9.178},	{0.207, 0.039},	{0.000, 0.000},	{225.075, 45.855},	{0.000, 0.000},	{0.015, 0.007},	{0.039, 0.034},	{46.334, 13.615},	{0.000, 0.000},	{0.000, 0.000},	{0.000, 0.000},	{35.736, 40.507},	{4.737, 0.995},	{2.573, 0.354},	{2.297, 0.377},	{2.156, 0.232},	{1.864, 0.262},	{1.964, 0.276},	{2.091, 0.600},	{1.754, 0.304},	{1.469, 0.137},	{1.635, 0.322},	{1.549, 0.249},	{1.241, 0.306},	{0.254, 0.093},	{0.302, 0.114},	{0.198, 0.034},	{0.138, 0.027},	{0.114, 0.018},	{0.130, 0.035},	{0.105, 0.021},	{0.109, 0.021},	{0.097, 0.022},	{0.000, 0.000},	{0.053, 0.043},	{19.295, 5.166},	{1000.838, 264.629},	{105728.750, 28006.173},	{12287625.000, 3238794.482},	{52.424, 11.648},	{0.368, 0.070},	{0.000, 0.000},	{1478.000, 25.049},	{0.000, 0.000},	{0.012, 0.005},	{0.522, 0.327},	{121.373, 18.735},	{0.000, 0.000},	{0.000, 0.000},	{0.000, 0.000},	{-170.638, 6.238},	{-0.527, 1.394},	{0.827, 1.239},	{-0.323, 0.790},	{-1.013, 0.608},	{-0.830, 0.237},	{0.239, 0.565},	{-0.720, 1.071},	{-0.279, 0.545},	{-0.599, 0.261},	{-0.387, 0.283},	{0.227, 0.447},	{-0.458, 0.424},	{-0.656, 0.104},	{0.170, 0.082},	{-0.136, 0.076},	{0.133, 0.055},	{-0.132, 0.040},	{0.094, 0.069},	{-0.118, 0.080},	{0.116, 0.057},	{-0.096, 0.034},	{0.000, 0.000},	{0.040, 0.027},	{80.993, 6.868},	{4253.625, 256.105},	{187212.500, 39050.386},	{46065000.000, 4241627.046}}
				
				//MST
				//,{{0.330, 0.157},	{0.200, 0.183},	{0.330, 0.226},	{0.150, 0.158}}
				,{{0.230, 0.158},	{0.338, 0.205},	{0.462, 0.172},	{0.084, 0.117}}
				//{{0.167, 0.123},	{0.397, 0.161},	{0.388, 0.188},	{0.120, 0.203}}
			}
		};  		
		
		for(int i=0;i<outcomes.length;i++){
			int outcome=outcomes[i];
			for(int j=0;j<indicators.length;j++){
				int indicator=indicators[j];
				for(int k=0;k<meansAndStds[i][j].length;k++){
					int featureIdx=k;
					CONDITIONAL_PROBABILITY.put(outcome+"-"+indicator+"-"+featureIdx, 
							/*new ConditionalProbability(outcome, indicator, featureIdx, 
								histograms[i][j][k],
						upperAndLowerBounds[i][j][k][0], upperAndLowerBounds[i][j][k][1])*/
							new ConditionalProbability(outcome, indicator, featureIdx, 
									meansAndStds[i][j][k][0],meansAndStds[i][j][k][1],
									observedMinAndMax[i][j][k][0],observedMinAndMax[i][j][k][1]) 
					);
				}
			}
		}
		
		
		/**
		 * set for environment
		 */
		PRIOR_PROBABILITY.put(ENVIRON_INDOOR, 0.6);
		PRIOR_PROBABILITY.put(ENVIRON_OUTDOOR, 0.4);
		System.out.println(PRIOR_PROBABILITY);
		
		
		int[] environOutcomes=new int[]{ENVIRON_INDOOR, ENVIRON_OUTDOOR};
  		indicators=new int[]{INDICATOR_LIGHT_DAY,INDICATOR_LIGHT_NIGHT, INDICATOR_RSS,INDICATOR_MAGNETIC};
  		
  		observedMinAndMax=new double[][][][]{
  				//indoor
  				{
  					//light-day
  					{{0, 300}},
  					//light-night
  					{{0, 150}},
  					//RSS
  					{{0, 70}},
  					//magnetic
  					{{0,1}}
  				},  				
  				//outdoor
  				{
  					//light-day
  					{{0, 3000}},
  					//light-night
  					{{0, 30}},
  					//RSS
  					{{30, 100}},
  					//magnetic
  					{{0,1}}
  				}
  		};
  		
  		meansAndStds=new double[][][][]{
  				//indoor
  				{
  					//light-day
  					{{40, 40}},
  					//light-night
  					{{30, 30}},
  					//RSS
  					{{55, 10}},
  					//magnetic
  					{{0, 1}}
  				},  				
  				//outdoor
  				{
  					//light-day
  					{{150, 100}},
  					//light-night
  					{{10, 10}},
  					//RSS
  					{{65, 10}},
  					//magnetic
  					{{0, 1}}
  				}
  		};
  		for(int i=0;i<environOutcomes.length;i++){
			int outcome=environOutcomes[i];
			for(int j=0;j<indicators.length;j++){
				int indicator=indicators[j];
				for(int k=0;k<meansAndStds[i][j].length;k++){
					int featureIdx=k;
					CONDITIONAL_PROBABILITY.put(outcome+"-"+indicator+"-"+featureIdx, 
						new ConditionalProbability(outcome, indicator, featureIdx, 
						//histograms[i][j][k], upperAndLowerBounds[i][j][k][0], upperAndLowerBounds[i][j][k][1]
						meansAndStds[i][j][k][0], meansAndStds[i][j][k][1]
					));
				}
			}
		}
	}
	
	
	double[][] featureVectors={
			/*
			//CIV
			 { 0.010,  0.005,  1.082,  1.294,  0.514}//parking
			,{-0.620,  3.746, -1.086, -0.029,  0.021}	//unparking
			,{ -0.001,  0.000,  0.983,  0.022, 17.238}//none
			*/
				
			//engine_start
				/*{3.207E1,1.922E-1,1.006E-3,3.122E2,5.74E-4,3.075E-2,0E0,5.593E1,0E0,0E0,0E0,1.146E2,5.468E0,2.757E0,1.839E0,1.874E0,1.777E0,1.875E0,1.635E0,1.674E0,1.49E0,1.513E0,1.609E0,1.081E0,2.82E-1,1.852E-1,2.11E-1,1.697E-1,1.185E-1,1.265E-1,9.124E-2,1.347E-1,1.209E-1,0E0,1.537E-1,2.108E1,8.358E2,1.31E5,1.681E7,5.395E1,3.776E-1,2.598E-4,1.455E3,4.12E-4,2.251E-2,0E0,1.264E2,0E0,0E0,0E0,-1.621E2,-1.425E0,2.298E0,-3.339E-1,-5.606E-1,-7.03E-1,-3.865E-1,-4.94E-1,-1.048E0,-5.441E-1,-3.183E-1,1.342E-1,-9.061E-1,-6.235E-1,2.158E-1,-2.274E-1,7.22E-2,-7.982E-2,5.538E-2,-1.815E-2,4.018E-2,-9.941E-2,0E0,1.016E-1,8.244E1,4.243E3,1.971E5,4.863E7}//parking
				,{7.346E0,4.662E-2,1.867E-3,2.885E2,1.103E-3,5.408E-2,0E0,1.93E1,0E0,0E0,0E0,1.224E2,3.122E0,1.865E0,1.657E0,1.676E0,1.687E0,1.487E0,1.371E0,1.254E0,1.082E0,1.112E0,1.051E0,1.145E0,1.149E-1,1.741E-1,1.315E-1,1.028E-1,7.43E-2,5.369E-2,6.249E-2,6.009E-2,5.402E-2,0E0,1.051E-1,1.343E1,9.853E2,7.853E4,1.479E7,2.381E1,1.478E-1,3.928E-4,1.456E3,3.315E-4,1.608E-2,0E0,6.319E1,0E0,0E0,0E0,-1.832E2,5.093E0,-2.289E0,1.263E0,4.524E-1,4.785E-2,-6.677E-1,-1.078E0,-7.26E-1,-9.864E-1,-7.362E-1,-9.606E-1,-6.277E-1,-8.95E-1,2.608E-1,1.832E-1,5.362E-2,-9.407E-2,1.087E-2,-3.491E-2,2.184E-2,2.699E-3,0E0,3.761E-2,5.807E1,4E3,3.666E5,6.648E7}//none
				,{9.594E0,6.867E-2,2.312E-3,3.23E2,1.477E-3,8.302E-2,0E0,2.144E1,0E0,0E0,0E0,1.379E2,3.717E0,1.936E0,2.102E0,1.766E0,1.235E0,1.48E0,1.355E0,1.11E0,1.207E0,1.122E0,1.184E0,1.06E0,1.316E-1,1.747E-1,1.177E-1,8.829E-2,8.139E-2,6.096E-2,6.767E-2,8.422E-2,6.593E-2,0E0,1.483E-1,1.465E1,9.542E2,7.327E4,1.342E7,2.281E1,1.461E-1,6.114E-4,1.454E3,5.436E-4,2.795E-2,0E0,6.247E1,0E0,0E0,0E0,-1.753E2,4.755E0,1.765E-1,-3.445E-1,-4.646E-1,3.879E-2,5.626E-2,-1.109E0,1.361E-1,-3.071E-1,-8.847E-1,-3.441E-1,-5.553E-1,-8.966E-1,3.06E-1,-8.4E-2,5.157E-2,4.698E-2,4.622E-2,-6.962E-2,5.517E-2,2.113E-2,0E0,6.326E-2,5.593E1,3.581E3,3.009E5,5.465E7}//none
*/	
		//bluetooth
				{-1} //parking, disconnection
				,{0} //none
				,{1} //unparking, connection
	};
	
	public static void validateExistingPriorProbUsingSpecificVector(){
		
		double[] features=new double[]{0.020,  1.621,  0.980,  0.013, -1.158, -0.022,  0.000,  0.023,  0.000};
		double[] outcomeProbs=new double[outcomes.length];
		
		for(int i=0;i<outcomes.length;i++){
			outcomeProbs[i]=conditionalProbabilityProduct(features, outcomes[i], INDICATOR_CIV, HIGH_LEVEL_ACTIVITY_UPARKING);
			//outcomeProbs[i]*=PRIOR_PROBABILITY.get(outcomes[i]);
		}

		double[] res=getMostLikelyOutcomeAndItsProb(normalizeProbabilities(outcomeProbs));
		System.out.println("Most Likely outcome is "+outcomes[(int)res[0]]+" prob="+res[1]);
	}
	
	
	
	//TODO main method
	public static void main(String[] args){
		validateExistingPriorProbUsingSpecificVector();
		//if(true) return;

		//fusion();
		
		//testNotificationThreshold();
	
		//tuneIndoorOutdoorFusion();
	}
		
	public static double[][] fusion(){
		detectedEvents.clear();
		detectedEvents.add(new ArrayList<String>());
		detectedEvents.add(new ArrayList<String>());		
		
		// certainly thrown away //"2013_10_014","2013_10_242"
		//  maybe  //
		String[] dates={
				//"2013_08_283","2013_08_295","2013_08_2311","2013_08_2710",
				//"2013_09_191",
				"2014_04_200"
				//"2013_09_211","2013_10_241", "2013_10_251","2013_11_011", "2013_11_012",
				//test set
				//"2013_09_1243","2013_09_1244","2013_08_2689"
		};
		//dates=new String[]{"2013_11_011", "2013_11_012"};
		
		//testPerformanceForEachFile(dates);
		double[][] precAneRecall=performance(dates,false, false, true);
		
		if(AccelerometerSignalProcessing.scopePreAndRecall.size()>0){
			ArrayList<Double> res=AccelerometerSignalProcessing.scopePreAndRecall.get(AccelerometerSignalProcessing.scopePreAndRecall.size()-1);
			res.add(precAneRecall[0][0]);res.add(precAneRecall[0][1]);res.add(precAneRecall[1][0]);res.add(precAneRecall[1][1]);
		}
		return precAneRecall;
	}
	
	public static void testNotificationThreshold(){
		double[] thresholds={0.7,0.75,0.8,0.85,0.9, 0.95};
		ArrayList<ArrayList<Double>> thrPreandRecall=new ArrayList< ArrayList<Double>>();
		for(double thre:thresholds){
			NOTIFICATION_THRESHOLD=thre;
			
			thrPreandRecall.add(new ArrayList<Double>());
			thrPreandRecall.get(thrPreandRecall.size()-1).add(thre);
			
			double[][] res=fusion();
			
			ArrayList<Double> last=thrPreandRecall.get(thrPreandRecall.size()-1);
			last.add(res[0][0]);last.add(res[0][1]);last.add(res[1][0]);last.add(res[1][1]);
		}
		
		for(ArrayList<Double> thr: thrPreandRecall){
			System.out.println(thr);
		}
	}
	
	public static void testPerformanceForEachFile(String[] dates){
		double[][][] results=new double[dates.length][][];
		for(int i=0;i<dates.length;i++){
			String[] file=new String[1]; file[0]=dates[i];
			results[i]=performance(file,true,false,false);
		}
		for(int i=0;i<dates.length;i++){
			String date=dates[i];
			double[][] preAndRecall=results[i];
			System.out.println(date+" : "+preAndRecall[0][0]+" "+preAndRecall[0][1]+" "+preAndRecall[1][0]+" "+preAndRecall[1][1]);
		}
	}
	
	/**
	 * 
	 * @param dates
	 * @param CIV: detect use only CIV
	 * @param MST: detect use only MST
	 * @param fusion: detect use fusion
	 * @return
	 */
	public static double[][] performance(String[] dates, boolean CIV, boolean MST, boolean fusion){
		String[] pathOfRawDataFiles=new String[dates.length];
		
		for(int i=0;i<dates.length;i++){
			/*pathOfRawDataFiles[i]=Constants.ACCELEROMETER_RAW_DATA_DIR
					+Constants.PHONE_POSITIONS[Constants.PHONE_POSITION_ALL]
							+"/test/ACCELEROMETER_RAW_"+dates[i]+".log";*/
			pathOfRawDataFiles[i]=Constants.ACCELEROMETER_BASE_DIR+"04202014/ACCELEROMETER_RAW_2014_04_200.log";
		}
		
		EventDetection.readGroudTruthFromRawAccelerometerFile(pathOfRawDataFiles);
		
		if(CIV){
			detectedEvents.get(0).clear();
			detectedEvents.get(1).clear();
			for(String date: dates){
				calculateOutcomeLikelihood(Constants.ACCELEROMETER_FUSION_DIR+date+"_CIV.log", date);
			}
			//System.out.println(detectedEvents);
			//EventDetection.calculatePrecisionAndRecall(detectedEvents);
		}

		if(MST){
			detectedEvents.get(0).clear();
			detectedEvents.get(1).clear();
			for(String date: dates){
				calculateOutcomeLikelihood(Constants.ACCELEROMETER_FUSION_DIR+date+"_MST.log", date);
			}
			//System.out.println(detectedEvents);
			//EventDetection.calculatePrecisionAndRecall(detectedEvents);
		}
		
		
		if(fusion){
			detectedEvents.get(0).clear();
			detectedEvents.get(1).clear();
			for(String date: dates){	
				calculateOutcomeLikelihoodWithFusion(date);
			}
			//return EventDetection.calculatePrecisionAndRecall(detectedEvents);
			//System.out.println(detectedEvents);
		}
		return null;
	}
	
	
	
	
	
	//read all feature vectors in 
	public static double[][] readVectors(String filePath, int indicator){
		ArrayList<Double []> featureVectors=new ArrayList<Double []>();
		try{
			Scanner sc=new Scanner(new File(filePath));
			String line;
			String[] lineFields;
			while(sc.hasNextLine()){
				line=sc.nextLine();
				if(line.startsWith("@")) continue;
				lineFields=line.split(",");
				//System.out.println(line);
				if(indicator==INDICATOR_CIV){
					featureVectors.add(new Double[3]);
					featureVectors.get(featureVectors.size()-1)[0]=Double.parseDouble(lineFields[1]);
					featureVectors.get(featureVectors.size()-1)[1]=Double.parseDouble(lineFields[2]);
					featureVectors.get(featureVectors.size()-1)[2]=Double.parseDouble(lineFields[3]);
				}else{
					featureVectors.add(new Double[lineFields.length-2]);
					for(int i=0;i<lineFields.length-2;i++){
						featureVectors.get(featureVectors.size()-1)[i]=Double.parseDouble(lineFields[i+1]);
					}
				}

			}
			sc.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
		double[][] vectors=new double[featureVectors.size()][];
		for(int i=0;i<featureVectors.size();i++){
			vectors[i]=new double[featureVectors.get(i).length];
			for(int j=0;j<featureVectors.get(i).length;j++){
				vectors[i][j]=featureVectors.get(i)[j];
			}
		}
		return vectors;
	}
	
	/**
	 * normalize the likelihood
	 */
	public static double[] normalizeProbabilities(double[] outcomeLikelihood){
		double sum = 0;
		double[] normalized=new double[outcomeLikelihood.length];
		for (int i = 0; i < outcomeLikelihood.length; i++)
			sum += outcomeLikelihood[i];
		if (DEBUG_ON){
			System.out.println(sum + "  "+ Arrays.toString(outcomeLikelihood));
		}
		for (int i = 0; i < outcomeLikelihood.length; i++)
			normalized[i] = (int) ((outcomeLikelihood[i] / sum) * 1000) / 1000.0;
		if (DEBUG_ON){
			System.out.println("normalized outcome likelihoods: "+ Arrays.toString(normalized));
		}
		return normalized;
	}
	
	public static double[] getMostLikelyOutcomeAndItsProb(double[] outcomeLikelihood){
		int mostLikelyOutcome=0;
		double maxProb=outcomeLikelihood[mostLikelyOutcome];
		for(int i=1;i<outcomeLikelihood.length;i++){
			if(outcomeLikelihood[i]>=maxProb){
				mostLikelyOutcome=i;
				maxProb=outcomeLikelihood[i];
			}
		}
		return new double[]{mostLikelyOutcome, maxProb};
	}
	
	/**
	 * 
	 * @param indicatorVectors: sorted based on time
	 * @param date
	 * @param detectionThreshold: 
	 * @param source: SOURCE
	 * @return
	 */
	public static UPActivitiesOfSameSource detectByIndicatorVectors(
			ArrayList<IndicatorVector> indicatorVectors, String date,
			double detectionThreshold, SOURCE source) {
		UPActivitiesOfSameSource detected = new UPActivitiesOfSameSource(source);
		double curProb;
		double[] outcomeLikelihood = new double[outcomes.length];
		double[] condProbs=new double[outcomeLikelihood.length];
		
		IndicatorVectorSet ivs=new IndicatorVectorSet();
		
		int[] indicators={INDICATOR_CIV, INDICATOR_MST};
		
		for (IndicatorVector niv : indicatorVectors) {
			ivs.add(niv);
		
			if(niv.timeInHMS.equals("15:23:30")){
				System.out.println();
			}
			
			
			int[] mostLikelyOutcomes=new int[ivs.vectorSet.size()];//most likely outcome for each indicator
			int indicatorCnt=0;
			for(int indiDix=0;indiDix<indicators.length;indiDix++){
				int indicator=indicators[indiDix];
				
				if(ivs.vectorSet.containsKey(indicator)){
					for (int i = 0; i < outcomes.length; i++) {
						int outcome = outcomes[i];
						IndicatorVector iv=ivs.vectorSet.get(indicator);
						
						if(iv.timeInHMS.contains("17:45:15")){
							System.out.println();
						}
						
						condProbs[i]=conditionalProbabilityProduct(iv.features, outcome, iv.indicator, HIGH_LEVEL_ACTIVITY_UPARKING);
						mostLikelyOutcomes[indicatorCnt]=(int)getMostLikelyOutcomeAndItsProb(condProbs)[0];
						
						if(indicatorCnt==0) outcomeLikelihood[i]=1;//initialize
						
						if(//true
							indicatorCnt==0 || 
							(mostLikelyOutcomes[indicatorCnt]==mostLikelyOutcomes[indicatorCnt-1]
									//&&mostLikelyOutcomes[indicatorCnt]!=OUTCOME_NONE //no clear impact
							)
						){
							outcomeLikelihood[i] *= condProbs[i]; // precision is 3
						}
					}
					indicatorCnt++;
				}
			}
			
			//multiply prior prob
			for (int i = 0; i < outcomes.length; i++) {
				outcomeLikelihood[i] *= PRIOR_PROBABILITY.get(outcomes[i]);
			}
			outcomeLikelihood=normalizeProbabilities(outcomeLikelihood);
			double[] mostLikelyOutcomeAndProb=getMostLikelyOutcomeAndItsProb(outcomeLikelihood);
			int mostLikelyOutcome=(int)mostLikelyOutcomeAndProb[0];
			double mostLikelyOutcomeProb=mostLikelyOutcomeAndProb[1];
			switch(mostLikelyOutcome){
			case 1:
				if(mostLikelyOutcomeProb>detectionThreshold){
					detected.get(Constants.PARKING_ACTIVITY).add(
							new UPActivity(source,	Constants.PARKING_ACTIVITY, date, niv.timeInHMS, mostLikelyOutcomeProb));
				}
				break;
			case 2:
				if (mostLikelyOutcomeProb > detectionThreshold) {
					detected.get(Constants.UNPARKING_ACTIVITY).add(
							new UPActivity(source,Constants.UNPARKING_ACTIVITY, date,niv.timeInHMS, mostLikelyOutcomeProb));
				}
				break;
			default:
				break;
			}			
		}
		System.out.println(detected);
		return detected;
	}
	
	public static double conditionalProbabilityProduct(double[] features, int outcome, int indicator, int highLevelActivity){
		double ret=1;
		HashSet<Integer> usedFeatures=new HashSet<Integer>();
		switch (indicator) {
		case INDICATOR_MST:
			usedFeatures.add(0);
			usedFeatures.add(1);
			usedFeatures.add(2);
			usedFeatures.add(3);
			break;
		case INDICATOR_CIV:
			usedFeatures.add(2);//0,2
			usedFeatures.add(4);
			usedFeatures.add(7);//5,7
		default:
			break;
		}

		
		for(int featureIdx=0;featureIdx<features.length;featureIdx++){
			if(!usedFeatures.contains(featureIdx)) continue;
			String identifier=outcome+"-"+indicator+"-"+featureIdx;
			if(CONDITIONAL_PROBABILITY.containsKey(identifier)){
				double featureValue=features[featureIdx];
				ConditionalProbability cp=CONDITIONAL_PROBABILITY.get(identifier);
				double featureCondProb;  						
				double delta=ConditionalProbability.getDelta(cp, highLevelActivity);
				featureCondProb=cp.getProbNormalDistr(featureValue	,delta);
				ret*=featureCondProb;
				String indi="";
				switch(indicator){
				case INDICATOR_LIGHT_DAY:
					indi="LIGHT-DAY";
					break;
				case INDICATOR_LIGHT_NIGHT:
					indi="LIGHT-NIGHT";
					break;
				case INDICATOR_RSS:
					indi="RSS";
					break;
				default:
					break;
				}
				//System.out.println(indi+" "+identifier+" "+featureCondProb);
				String logMsg=identifier+" "+featureValue+" "+delta+" "+featureCondProb+" "+ret;
				if(DEBUG_ON){
					System.out.println(logMsg);
				}
			}
		}
		return ret;
	}
	
	
	public static void tuneIndoorOutdoorFusion(){
		int[] outcomes={Fusion.ENVIRON_INDOOR, Fusion.ENVIRON_OUTDOOR};
		double[] probs={0.6, 0.4};
		
		
		//int[] indicators={INDICATOR_LIGHT_DAY, INDICATOR_RSS};
		int[] indicators={INDICATOR_LIGHT_NIGHT, INDICATOR_RSS};
		
		double[][] features={
				//light
				{74},
				//RSS
				{59}
		};
		
		
		double prob;
		for(int i=0;i<outcomes.length;i++){
			int outcome=outcomes[i];
			prob=probs[i];
			for(int indicator: indicators){
				prob*=conditionalProbabilityProduct(features[i], outcome, indicator, HIGH_LEVEL_ACTIVITY_IODOOR);
			}
			String environ;
			if(outcome==ENVIRON_INDOOR) environ="Indoor";
			else environ="outdoor";
			System.out.println(environ+" "+prob);
		}

	}
	
	
	
	
	/***
	 * Legacy codes 
	 */
	/**
	 * 
	 * @param featureVectors
	 * @param inputFileOutcome
	 * @param mingling
	 * @return: int[][]---> int[0]: w/o threshold, witht threshold
	 */
	public static int[][] calculateOutcomeLikelihood(String vectorFile, String date){
		
		
		double[] outcomeLikelihood=new double[outcomes.length]; 
		System.out.println("outcomes = "+Arrays.toString(outcomes));
		double curProb;  	
	
		int[][] outcomeCnts=new int[2][outcomes.length];
		double[] avgOutComeProb=new double[outcomes.length];
		
		
		
		ArrayList<IndicatorVector> vectors=readVectors(vectorFile);
		if(DEBUG_ON) System.out.println("single indicator: ");
		
		for(IndicatorVector iv:vectors){
			for(int i=0;i<outcomes.length;i++){
				int outcome=outcomes[i];
				curProb=PRIOR_PROBABILITY.get(outcome);
			
				curProb*=conditionalProbabilityProduct(iv.features, outcome, iv.indicator, HIGH_LEVEL_ACTIVITY_UPARKING);
				/**
				 * inject vectors of bluetooth and engine start
				 */
				/*if(mingling){
					double[] bluetoothVector=null;
					double[] engineStartVector=null;
					switch(inputFileOutcome){
					case OUTCOME_PARKING:
						bluetoothVector=new double[]{-1};
						break;
					case OUTCOME_UNPARKING:
						bluetoothVector=new double[]{1};
						engineStartVector=new double[]{
								2.111E1,1.889E-1,3.512E-4,1.933E2,2.86E-4,1.207E-2,1.093E-1,3.488E1,0E0,0E0,0E0,1.517E1,4.515E0,2.183E0,1.928E0,1.84E0,1.739E0,2.097E0,1.578E0,1.29E0,1.385E0,1.36E0,1.297E0,1.027E0,1.619E-1,1.979E-1,1.592E-1,1.262E-1,9.69E-2,9.961E-2,9.965E-2,7.601E-2,6.956E-2,0E0,4.765E-2,1.446E1,6.538E2,8.224E4,9.033E6,3.521E1,2.497E-1,5.313E-5,1.471E3,1.325E-4,6.411E-3,7.1E-1,9.148E1,0E0,0E0,0E0,-1.723E2,1.613E0,7.638E-1,-8.833E-1,-1.756E0,-9.215E-1,1.368E-1,8.559E-1,-1.775E-1,-5.982E-1,-2.854E-1,-5.766E-1,-2.631E-1,-8.008E-1,8.953E-2,-1.183E-1,1.691E-1,-1.33E-1,1.609E-1,-1.094E-1,1.995E-1,-1.006E-1,0E0,2.486E-2,7.204E1,4.341E3,2.426E5,4.897E7
						};
						break;
					default:
						break;
					}
					//multiple with bluetoothVector
					if(bluetoothVector!=null){
						ConditionalProbability cp=CONDITIONAL_PROBABILITY.get(outcome+"-"+INDICATOR_BLUETOOTH+"-0");
						double delta=getDelta(cp);
						double featureCondProb=cp.getProbNormalDistr(bluetoothVector[0],delta);
						curProb*=featureCondProb;
						String logMsg=outcome+" "+bluetoothVector[0]+" "+delta+" "+featureCondProb+" "+curProb+"\n";
						if(DEBUG_ON) System.out.println(logMsg);
					}
					if(engineStartVector!=null){
						for(int ii=0;ii<engineStartVector.length;ii++){
							ConditionalProbability cp=CONDITIONAL_PROBABILITY.get(outcome+"-"+INDICATOR_ENGINE_START+"-"+ii);
							double delta=getDelta(cp);
							double featureCondProb=cp.getProbNormalDistr(engineStartVector[ii],delta);
							curProb*=featureCondProb;
							String logMsg=outcome+" "+engineStartVector[ii]+" "+delta+" "+featureCondProb+" "+curProb+"\n";
							if(DEBUG_ON) System.out.println(logMsg);
						}
					}
				}	*/
				outcomeLikelihood[i]=curProb; //precision is 3
			}  		
			countOutcomes(outcomeLikelihood, avgOutComeProb, outcomeCnts, iv.timeInHMS, date);
		}
		
		for(int i=0;i<avgOutComeProb.length;i++){
			avgOutComeProb[i]=((int)(avgOutComeProb[i]/outcomeCnts[0][i]*1000))/1000.0;
		}
		
		if(DEBUG_ON){
			for(int j=0;j<outcomeCnts.length;j++){
				System.out.println("outcome counts: "+Arrays.toString(outcomeCnts[j]));
				if(j==0) System.out.println("avg outcome probability: "+Arrays.toString(avgOutComeProb));
				else System.out.println();
			}
		}
		
		return outcomeCnts;		
	}
	
	//outcomelikelihood not normalized
	public static void countOutcomes(double[] outcomeLikelihood, double[] avgOutComeProb, int[][] outcomeCnts, String timeOfTheVector, String date){
		/**
		 * normalize the likelihood
		 */
			double sum=0;
		  	for(int i=0;i<outcomeLikelihood.length;i++) sum+=outcomeLikelihood[i];
		  	if(DEBUG_ON) System.out.println(sum+"  "+ Arrays.toString(outcomeLikelihood));  	
		  	
		  	for(int i=0;i<outcomeLikelihood.length;i++) outcomeLikelihood[i]= (int)((outcomeLikelihood[i]/sum)*1000)/1000.0;
		  	if(DEBUG_ON) System.out.println("normalized outcome likelihoods: "+ Arrays.toString(outcomeLikelihood));
	  	
	  	//System.out.println("outcome likelihood: "+Arrays.toString(outcomeLikelihood) );
	  	if(outcomeLikelihood[0]>outcomeLikelihood[1]&&outcomeLikelihood[0]>outcomeLikelihood[2]){
	  		outcomeCnts[0][0]++;
	  		avgOutComeProb[0]+=outcomeLikelihood[0];
	  		if(outcomeLikelihood[0]>NOTIFICATION_THRESHOLD){
	  			outcomeCnts[1][0]+=1;
	  		}
	  	}else{
	  		if(outcomeLikelihood[1]>outcomeLikelihood[2]){
	  			outcomeCnts[0][1]++;
	  			avgOutComeProb[1]+=outcomeLikelihood[1];
	  			if(outcomeLikelihood[1]>NOTIFICATION_THRESHOLD){
	  				outcomeCnts[1][1]+=1;
	  				detectedEvents.get(Constants.PARKING_ACTIVITY).add(date+"-"+timeOfTheVector);
	  				if(DEBUG_ON) System.out.println("Parking at "+timeOfTheVector+"\n");
	  			}
	  		}
	  		else{
	  			outcomeCnts[0][2]++;
	  			avgOutComeProb[2]+=outcomeLikelihood[2];
	  			if(outcomeLikelihood[2]>NOTIFICATION_THRESHOLD){
	  				outcomeCnts[1][2]+=1;
	  				detectedEvents.get(Constants.UNPARKING_ACTIVITY).add(date+"-"+timeOfTheVector);
	  				if(DEBUG_ON) System.out.println("Unparking at "+timeOfTheVector+"\n");
	  			}
	  		}
	  	}	
	}
	
	
	public static void calculateOutcomeLikelihoodWithFusion(String date){
		double[] outcomeLikelihood=new double[outcomes.length]; 
		double curProb;  	
	
		int[][] outcomeCnts=new int[2][outcomes.length];
		double[] avgOutComeProb=new double[outcomes.length];
		
		if(DEBUG_ON) System.out.println("fusing indicators: ");
		
		//merge vectors of CIV and MST indicator
			ArrayList<IndicatorVector> vectorsOfCIV=readVectors(Constants.ACCELEROMETER_FUSION_DIR+date+"_CIV.log");
			ArrayList<IndicatorVector> vectorsOfMST=readVectors(Constants.ACCELEROMETER_FUSION_DIR+date+"_MST.log");
			int idxCIV=0, idxMST=0;
			
			ArrayList<IndicatorVector> fused=new ArrayList<IndicatorVector>();
			while(idxCIV<vectorsOfCIV.size()&&idxMST<vectorsOfMST.size()){
				if(vectorsOfCIV.get(idxCIV).secondOfDay<=vectorsOfMST.get(idxMST).secondOfDay){
					fused.add(vectorsOfCIV.get(idxCIV));
					idxCIV++;				
				}else{
					fused.add(vectorsOfMST.get(idxMST));
					idxMST++;				
				}
			}
			if(idxCIV<vectorsOfCIV.size()){
				while(idxCIV<vectorsOfCIV.size()){
					fused.add(vectorsOfCIV.get(idxCIV++));
				}
			}
			if(idxMST<vectorsOfMST.size()){
				while(idxMST<vectorsOfMST.size()){
					fused.add(vectorsOfMST.get(idxMST++));
				}
			}
		
		//for(TimestampedVector tv: fused) System.out.println(tv);
		IndicatorVector prevVector=null;
		for(IndicatorVector tv: fused){
			for(int i=0;i<outcomes.length;i++){
				int outcome=outcomes[i];
				curProb=PRIOR_PROBABILITY.get(outcome);
				curProb*=conditionalProbabilityProduct(tv.features, outcome, tv.indicator, HIGH_LEVEL_ACTIVITY_UPARKING);
				//fusion
				if(prevVector!=null&&tv.indicator!=prevVector.indicator){
					curProb*=conditionalProbabilityProduct(prevVector.features, outcome, prevVector.indicator, HIGH_LEVEL_ACTIVITY_UPARKING);
				}
				outcomeLikelihood[i]=curProb; //precision is 3
			}
			prevVector=tv;			
			countOutcomes(outcomeLikelihood, avgOutComeProb, outcomeCnts, tv.timeInHMS, date);	
		}
		for(int i=0;i<avgOutComeProb.length;i++){
			avgOutComeProb[i]=((int)(avgOutComeProb[i]/outcomeCnts[0][i]*1000))/1000.0;
		}
		if(DEBUG_ON){
			for(int j=0;j<outcomeCnts.length;j++){
				System.out.println("outcome counts: "+Arrays.toString(outcomeCnts[j]));
				if(j==0) System.out.println("avg outcome probability: "+Arrays.toString(avgOutComeProb));
				else System.out.println();
			}
		}
	}
	
	
	public static ArrayList<IndicatorVector> readVectors(String filepath){
		ArrayList<IndicatorVector> vectors=new ArrayList<IndicatorVector>();
		try{
			int indicator;
			if(filepath.contains("CIV")) indicator=INDICATOR_CIV;
			else indicator=INDICATOR_MST;
				
			Scanner sc=new Scanner(new File(filepath));
			while(sc.hasNextLine()){
				String line=sc.nextLine();
				if(line.startsWith("@")) continue;
				String[] fields=line.split(",");
				double[] features=new double[fields.length-2];
				for(int i=1;i<fields.length-1;i++) features[i-1]=Double.parseDouble(fields[i]);
				vectors.add(new IndicatorVector(fields[0], features, indicator));
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return vectors;
	}
	
	public static double getDelta(ConditionalProbability cp){
		//getSmallestIntervalBetweenObservedMaxAndMinOfTheSameFeature
		double interval=cp.observedMax-cp.observedMin;
		String identifier=cp.indicatorID+"-"+cp.featureIdx;
		for(int outcome:outcomes){
			if(outcome==cp.outcomeID) continue;
			ConditionalProbability counterpart=CONDITIONAL_PROBABILITY.get(outcome+"-"+cp.indicatorID+"-"+cp.featureIdx);
			interval=Math.min(interval, counterpart.observedMax-counterpart.observedMin);
		}
		//System.out.println(identifier+" "+interval/5);
		//return 0.1;
		return Math.max(interval/20, 0.1);
	}
	
	
	/*public static void peformance(){
		boolean[] fusions={false, true};
		
		for(boolean fusion:fusions){
			//0: w/o threshold, 1: w threshold
			//0:parking, 1:unparking
			int[][] tp=new int[2][2]; 
			int[][] fp=new int[2][2];
			int[][] fn=new int[2][2];
			
			double[] pre=new double[2];
			double[] recall=new double[2];
			
			if(fusion) System.out.println("************* With Fusion *************");
			else System.out.println("************* W/O Fusion *************");
			
			for(int i=0;i<3;i++){
				String filename=filenames[i];
				//double[][] featureVectors = readVectors(CIV_FOLDER + filename, INDICATOR_CIV);
				int[][] outcomeCnts = calculateOutcomeLikelihood(CIV_FOLDER + filename );//;outcomes[i], fusion);
				switch (filename) {
				case "parking.txt":
					for(int j=0;j<outcomeCnts.length;j++){
						tp[j][0]+=outcomeCnts[j][1];
						fn[j][0]+=outcomeCnts[j][0]+outcomeCnts[j][2];
						fp[j][1]+=outcomeCnts[j][2];
					}
					break;
				case "unparking.txt":
					for(int j=0;j<outcomeCnts.length;j++){
						tp[j][1]+=outcomeCnts[j][2];
						fn[j][1]+=outcomeCnts[j][0]+outcomeCnts[j][1];
						fp[j][0]+=outcomeCnts[j][1];
					}
					break;
				case "none.txt":
					for(int j=0;j<outcomeCnts.length;j++){
						fp[j][0]+=outcomeCnts[j][1];
						fp[j][1]+=outcomeCnts[j][2];
					}
					break;
				default:
					break;
				}
			}
			
			for(int j=0;j<2;j++){
				if(j==0) System.out.println("Without threshold:");
				else System.out.println("With threshold:");
				for(int i=0;i<2;i++){
					pre[i]=tp[j][i]/(tp[0][i]+fp[0][i]+0.0);
					recall[i]=tp[j][i]/(tp[0][i]+fn[0][i]+0.0);
					if(i==0) System.out.print("Parking:  ");
					else System.out.print("Unparking:  ");
					System.out.println(String.format("%.3f", pre[i])+"  "+String.format("%.3f", recall[i]));
				}
			}
		}		
	}*/
}




