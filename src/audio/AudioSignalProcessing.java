package audio;

import helper.Constants;
import jAudioFeatureExtractor.CommandLineThread;
import jAudioFeatureExtractor.DataModel;
import jAudioFeatureExtractor.ACE.DataTypes.Batch;
import jAudioFeatureExtractor.ACE.XMLParsers.XMLDocumentParser;
import jAudioFeatureExtractor.AudioFeatures.AreaMoments;
import jAudioFeatureExtractor.DataTypes.RecordingInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import accelerometer.learning.EventClassifier;


public class AudioSignalProcessing {
	
	public static void extractFeatures(String inputAudioFileName){
		//references:  jAudio.JAudioCommandLine
		
		//config files of jAudio
		String settingFilePath=Constants.AUDIO_BASE_DIR+"setting.xml";
		
		//read and parse the setting
		Object[] data = null;
		DataModel dm=null;
		try {
			InputStream ins=new FileInputStream(Constants.AUDIO_BASE_DIR+"features.xml");
			dm=new DataModel(ins, null);
			//DataModel dm = new DataModel(Constants.AUDIO_BASE_DIR+"features.xml",null);
			
			
			//data = (Object[]) XMLDocumentParser.parseXMLDocument(new FileInputStream(settingFilePath),"save_settings");
						
		} catch (Exception e) {
			System.out.println("Error encountered parsing the settings file");
			e.printStackTrace();
			System.exit(3);
		}

		
		int windowLength = 512;
		double offset = 0.0;
		double samplingRate;
		boolean saveWindows;
		boolean saveOverall;
		boolean normalise;
		int outputType;
		
		
		samplingRate = 16000;
		normalise = false;
		saveWindows = false;
		saveOverall = true;
		String outputFormat = "ARFF";
		outputType = 1; //"ARFF"
		
		/*try {
			System.out.println((String) data[0]); 
			windowLength = Integer.parseInt((String) data[0]);
		} catch (NumberFormatException e) {
			System.out.println("Error in settings file");
			System.out.println("Window length of settings must be an integer");
			System.exit(4);
		}
		try {
			System.out.println((String) data[1]); 
			offset = Double.parseDouble((String) data[1]);
		} catch (NumberFormatException e) {
			System.out.println("Error in settings file");
			System.out
					.println("Window offset of settings must be an double between 0 and 1");
			System.exit(4);
		}*/
		
		/*System.out.println((Double) data[2]); 
		samplingRate = ((Double) data[2]).doubleValue();
		
		System.out.println((Boolean) data[3]); 
		normalise = ((Boolean) data[3]).booleanValue();
		
		System.out.println((Boolean) data[4]);
		saveWindows = ((Boolean) data[4]).booleanValue();
		
		System.out.println((Boolean) data[5]);
		saveOverall = ((Boolean) data[5]).booleanValue();
		
		System.out.println((String) data[6]);
		String outputFormat = ((String) data[6]);
		if (outputFormat.equals("ACE")) {
			outputType = 0;
		} else {
			outputType = 1;
		}*/
	
		OutputStream destinationFK = null;
		OutputStream destinationFV = null;
		String outputFileName=Constants.AUDIO_FEATURES_DIR+inputAudioFileName + ".arff";
		try{		
			if (outputType == 0) {//if the output type is ACE
				destinationFK = new FileOutputStream(new File(Constants.AUDIO_FEATURES_DIR+inputAudioFileName + "FK.xml"));
				destinationFV = new FileOutputStream(new File(Constants.AUDIO_FEATURES_DIR+inputAudioFileName + "FV.xml"));
			} else { //type if ARFF
				destinationFK = new FileOutputStream(new File(Constants.AUDIO_FEATURES_DIR+"definition.arff"));  //definitions
				destinationFV = new FileOutputStream(new File(outputFileName));  //values
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}

		HashMap<String, Boolean> active=new HashMap<>();
		active.put("Derivative of Running Mean of Spectral Rolloff Point",false);
		active.put("Strongest Beat",true);
		active.put("Derivative of Standard Deviation of Spectral Flux",false);
		active.put("Spectral Centroid",true);
		active.put("Running Mean of Fraction Of Low Energy Windows",false);
		active.put("Derivative of Standard Deviation of Spectral Rolloff Point",false);
		active.put("Derivative of Strongest Beat",false);
		active.put("Derivative of Running Mean of Strongest Frequency Via FFT Maximum",false);
		active.put("Derivative of Standard Deviation of Method of Moments",false);
		active.put("Running Mean of Zero Crossings",false);
		active.put("Derivative of Running Mean of LPC",false);
		active.put("LPC",true);
		active.put("Derivative of Standard Deviation of Fraction Of Low Energy Windows",false);
		active.put("Running Mean of MFCC",false);
		active.put("Derivative of Standard Deviation of Strongest Frequency Via FFT Maximum",false);
		active.put("Derivative of Running Mean of Method of Moments",false);
		active.put("Derivative of Running Mean of Strongest Frequency Via Zero Crossings",false);
		active.put("Standard Deviation of Area Method of Moments",false);
		active.put("Derivative of Relative Difference Function",false);
		active.put("Derivative of Compactness",false);
		active.put("Derivative of Fraction Of Low Energy Windows",false);
		active.put("Derivative of Standard Deviation of Spectral Variability",false);
		active.put("Running Mean of Spectral Flux",false);
		active.put("Compactness",true);
		active.put("Derivative of Standard Deviation of Spectral Centroid",false);
		active.put("Derivative of Method of Moments",false);
		active.put("Beat Histogram",false);
		active.put("Derivative of Running Mean of Compactness",false);
		active.put("Standard Deviation of Spectral Flux",false);
		active.put("Running Mean of LPC",false);
		active.put("Standard Deviation of Beat Sum",false);
		active.put("Standard Deviation of Zero Crossings",false);
		active.put("Standard Deviation of Root Mean Square",false);
		active.put("MFCC",true);
		active.put("Zero Crossings",true);
		active.put("Fraction Of Low Energy Windows",true);
		active.put("Derivative of Spectral Centroid",false);
		active.put("Derivative of Spectral Flux",false);
		active.put("Derivative of Strength Of Strongest Beat",false);
		active.put("Running Mean of Relative Difference Function",false);
		active.put("Standard Deviation of MFCC",false);
		active.put("Running Mean of Area Method of Moments",false);
		active.put("Standard Deviation of Strongest Frequency Via Zero Crossings",false);
		active.put("Standard Deviation of Partial Based Spectral Centroid",false);
		active.put("Derivative of Running Mean of Fraction Of Low Energy Windows",false);
		active.put("Derivative of Root Mean Square",false);
		active.put("FFT Bin Frequency Labels",false);
		active.put("Derivative of Strongest Frequency Via Zero Crossings",false);
		active.put("Derivative of Standard Deviation of Strength Of Strongest Beat",false);
		active.put("Derivative of Running Mean of Relative Difference Function",false);
		active.put("Derivative of Standard Deviation of Relative Difference Function",false);
		active.put("Standard Deviation of Strength Of Strongest Beat",false);
		active.put("Derivative of Partial Based Spectral Flux",false);
		active.put("Strongest Frequency Via Spectral Centroid",false);
		active.put("Derivative of Area Method of Moments",false);
		active.put("Standard Deviation of Spectral Variability",false);
		active.put("Running Mean of Strongest Frequency Via FFT Maximum",false);
		active.put("Running Mean of Strength Of Strongest Beat",false);
		active.put("Running Mean of Beat Sum",false);
		active.put("Derivative of Spectral Variability",false);
		active.put("Derivative of Running Mean of Zero Crossings",false);
		active.put("Derivative of Running Mean of Spectral Flux",false);
		active.put("Strength Of Strongest Beat",true);
		active.put("Derivative of Standard Deviation of LPC",false);
		active.put("Derivative of LPC",false);
		active.put("Derivative of Standard Deviation of Compactness",false);
		active.put("Derivative of Zero Crossings",false);
		active.put("Standard Deviation of Strongest Frequency Via Spectral Centroid",false);
		active.put("Partial Based Spectral Centroid",false);
		active.put("Running Mean of Compactness",false);
		active.put("Running Mean of Strongest Beat",false);
		active.put("Derivative of Standard Deviation of Strongest Frequency Via Zero Crossings",false);
		active.put("Standard Deviation of Spectral Rolloff Point",false);
		active.put("Spectral Variability",true);
		active.put("Peak Based Spectral Smoothness",false);
		active.put("Running Mean of Method of Moments",false);
		active.put("Strongest Frequency Via Zero Crossings",false);
		active.put("Beat Histogram Bin Labels",false);
		active.put("Method of Moments",true);
		active.put("Standard Deviation of Fraction Of Low Energy Windows",false);
		active.put("Magnitude Spectrum",false);
		active.put("Derivative of Standard Deviation of Beat Sum",false);
		active.put("Derivative of Standard Deviation of Area Method of Moments",false);
		active.put("Running Mean of Peak Based Spectral Smoothness",false);
		active.put("Relative Difference Function",false);
		active.put("Derivative of Standard Deviation of Strongest Beat",false);
		active.put("Running Mean of Root Mean Square",false);
		active.put("Running Mean of Partial Based Spectral Centroid",false);
		active.put("Derivative of Running Mean of MFCC",false);
		active.put("Derivative of Running Mean of Strength Of Strongest Beat",false);
		active.put("Standard Deviation of Peak Based Spectral Smoothness",false);
		active.put("Spectral Flux",true);
		active.put("Running Mean of Partial Based Spectral Flux",false);
		active.put("Derivative of Standard Deviation of Peak Based Spectral Smoothness",false);
		active.put("Derivative of Running Mean of Spectral Variability",false);
		active.put("Derivative of Standard Deviation of Zero Crossings",false);
		active.put("Derivative of Running Mean of Beat Sum",false);
		active.put("Standard Deviation of Relative Difference Function",false);
		active.put("Derivative of Running Mean of Peak Based Spectral Smoothness",false);
		active.put("Beat Sum",true);
		active.put("Derivative of Running Mean of Spectral Centroid",false);
		active.put("Standard Deviation of Strongest Beat",false);
		active.put("Derivative of Running Mean of Partial Based Spectral Centroid",false);
		active.put("Standard Deviation of Partial Based Spectral Flux",false);
		active.put("Derivative of Running Mean of Root Mean Square",false);
		active.put("Running Mean of Spectral Centroid",false);
		active.put("Derivative of Standard Deviation of Partial Based Spectral Flux",false);
		active.put("Derivative of Running Mean of Strongest Beat",false);
		active.put("Derivative of Spectral Rolloff Point",false);
		active.put("Derivative of Running Mean of Area Method of Moments",false);
		active.put("Standard Deviation of Strongest Frequency Via FFT Maximum",false);
		active.put("Derivative of Standard Deviation of Root Mean Square",false);
		active.put("Standard Deviation of Spectral Centroid",false);
		active.put("Derivative of Strongest Frequency Via FFT Maximum",false);
		active.put("Peak Detection",false);
		active.put("Running Mean of Spectral Variability",false);
		active.put("Derivative of Peak Based Spectral Smoothness",false);
		active.put("Derivative of Strongest Frequency Via Spectral Centroid",false);
		active.put("Derivative of Partial Based Spectral Centroid",false);
		active.put("Strongest Frequency Via FFT Maximum",false);
		active.put("Derivative of Standard Deviation of MFCC",false);
		active.put("Running Mean of Spectral Rolloff Point",false);
		active.put("Standard Deviation of Method of Moments",false);
		active.put("Spectral Rolloff Point",true);
		active.put("Derivative of MFCC",false);
		active.put("Standard Deviation of Compactness",false);
		active.put("Derivative of Standard Deviation of Partial Based Spectral Centroid",false);
		active.put("Running Mean of Strongest Frequency Via Spectral Centroid",false);
		active.put("Root Mean Square",true);
		active.put("Derivative of Running Mean of Strongest Frequency Via Spectral Centroid",false);
		active.put("Area Method of Moments",false);
		active.put("Partial Based Spectral Flux",false);
		active.put("Running Mean of Strongest Frequency Via Zero Crossings",false);
		active.put("Derivative of Standard Deviation of Strongest Frequency Via Spectral Centroid",false);
		active.put("Derivative of Running Mean of Partial Based Spectral Flux",false);
		active.put("Standard Deviation of LPC",false);
		active.put("Derivative of Beat Sum",false);
		active.put("Power Spectrum",false);
		/*active= (HashMap<String, Boolean>) data[7];
		for(String key: active.keySet()){
			//System.out.println("active.put(\""+key+"\","+active.get(key)+");");
		}*/
		
		HashMap<String, String[]> attribute=new HashMap<String, String[]>();
		String[] attributeVales= new String[]{"0.85","100"};
		attribute.put("Derivative of Running Mean of Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Spectral Flux",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{"0.85","100"};
		attribute.put("Derivative of Standard Deviation of Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Zero Crossings",attributeVales);
		attributeVales= new String[]{"0.0","10","100"};
		attribute.put("Derivative of Running Mean of LPC",attributeVales);
		attributeVales= new String[]{"0.0","10"};
		attribute.put("LPC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{"13","100"};
		attribute.put("Running Mean of MFCC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{"10","100"};
		attribute.put("Standard Deviation of Area Method of Moments",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Relative Difference Function",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Compactness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Spectral Variability",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Spectral Flux",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Compactness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Method of Moments",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Beat Histogram",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Compactness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Spectral Flux",attributeVales);
		attributeVales= new String[]{"0.0","10","100"};
		attribute.put("Running Mean of LPC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Beat Sum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Root Mean Square",attributeVales);
		attributeVales= new String[]{"13"};
		attribute.put("MFCC",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Zero Crossings",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Spectral Flux",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Relative Difference Function",attributeVales);
		attributeVales= new String[]{"13","100"};
		attribute.put("Standard Deviation of MFCC",attributeVales);
		attributeVales= new String[]{"10","100"};
		attribute.put("Running Mean of Area Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Root Mean Square",attributeVales);
		attributeVales= new String[]{};
		attribute.put("FFT Bin Frequency Labels",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Relative Difference Function",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Relative Difference Function",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{"10"};
		attribute.put("Derivative of Area Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Spectral Variability",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Beat Sum",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Spectral Variability",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Spectral Flux",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{"0.0","10","100"};
		attribute.put("Derivative of Standard Deviation of LPC",attributeVales);
		attributeVales= new String[]{"0.0","10"};
		attribute.put("Derivative of LPC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Compactness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Compactness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{"0.85","100"};
		attribute.put("Standard Deviation of Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Spectral Variability",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Method of Moments",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Beat Histogram Bin Labels",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Magnitude Spectrum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Beat Sum",attributeVales);
		attributeVales= new String[]{"10","100"};
		attribute.put("Derivative of Standard Deviation of Area Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Relative Difference Function",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Root Mean Square",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{"13","100"};
		attribute.put("Derivative of Running Mean of MFCC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Spectral Flux",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Spectral Variability",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Beat Sum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Relative Difference Function",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Beat Sum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Root Mean Square",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Strongest Beat",attributeVales);
		attributeVales= new String[]{"0.85"};
		attribute.put("Derivative of Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{"10","100"};
		attribute.put("Derivative of Running Mean of Area Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Root Mean Square",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"10"};
		attribute.put("Peak Detection",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Spectral Variability",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"13","100"};
		attribute.put("Derivative of Standard Deviation of MFCC",attributeVales);
		attributeVales= new String[]{"0.85","100"};
		attribute.put("Running Mean of Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Method of Moments",attributeVales);
		attributeVales= new String[]{"0.85"};
		attribute.put("Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{"13"};
		attribute.put("Derivative of MFCC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Compactness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Root Mean Square",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{"10"};
		attribute.put("Area Method of Moments",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{"0.0","10","100"};
		attribute.put("Standard Deviation of LPC",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Beat Sum",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Power Spectrum",attributeVales);
		/*attribute= (HashMap<String, String[]>) data[8];
		for(String key: active. keySet()){
			System.out.print("attributeVales= new String[]{");
			String[] array=attribute.get(key);
			for(int i=0;i<array.length;i++){
				if(i>0) System.out.print(",");
				System.out.print("\""+array[i]+"\"");
			}
			System.out.println("};");
			System.out.println("attribute.put(\""+key+"\",attributeVales);");
		}*/
		
//		for (int i = 0; i < dm.features.length; ++i) {
//			String name = dm.features[i].getFeatureDefinition().name;
//			if (attribute.containsKey(name)) {
//				dm.defaults[i] = active.get(name);
//				String[] att = attribute.get(name);
//				for (int j = 0; j < att.length; ++j) {
//					try {
//						dm.features[i].setElement(j, att[j]);
//					} catch (Exception e) {
//						System.out.println("Feature " + name
//								+ "failed apply its " + j + " attribute");
//						e.printStackTrace();
//					}
//				}
//			} else {
//				dm.defaults[i] = false;
//			}
//		}

		// now process the aggregators
		String[] aggNames = {"Standard Deviation", "Mean"};
		String[][] aggFeatures ={{},{}};
		String[][] aggParameters ={{},{}};
		
		/*System.out.println((LinkedList<String>)data[9]);
		aggNames = ((LinkedList<String>)data[9]).toArray(new String[]{});
		
		aggFeatures = ((LinkedList<String[]>)data[10]).toArray(new String[][]{});
		for(String[] array: aggFeatures){
			System.out.println(Arrays.toString(array));
		}
		
		aggParameters = ((LinkedList<String[]>)data[11]).toArray(new String[][]{});
		for(String[] array: aggParameters){
			System.out.println(Arrays.toString(array));
		}*/
		
		//		LinkedList<Aggregator> aggregator = new LinkedList<Aggregator>();
//		for(int i=0;i<aggNames.length;++i){
//			if(dm.aggregatorMap.containsKey(aggNames[i])){
//				Aggregator tmp = dm.aggregatorMap.get(aggNames[i]);
//				if(!tmp.getAggregatorDefinition().generic){
//					tmp.setParameters(aggFeatures[i],aggParameters[i]);
//				}
//				aggregator.add(tmp);
//			}
//		}
//		dm.aggregators = aggregator.toArray(new Aggregator[]{});

		// now process the files
		File[] names = new File[1];
		for (int i = 0; i < names.length; ++i) {
			names[i] = new File(Constants.AUDIO_DATA_DIR+inputAudioFileName+".wav");
		}
		
		// Go through the files one by one
		RecordingInfo[] recording_info = new RecordingInfo[1];
		for (int i = 0; i < names.length; i++) {
			// Assume file is invalid as first guess
			recording_info[i] = new RecordingInfo(names[i].getName(), names[i]
					.getPath(), null, false);
		}// for i in names

		try {
			dm.featureKey = destinationFK;
			dm.featureValue = destinationFV;
			Batch b = new Batch();
			b.setDataModel(dm);
			b.setWindowSize(windowLength);
			b.setWindowOverlap(offset);
			b.setSamplingRate(samplingRate);
			b.setNormalise(normalise);
			b.setPerWindow(saveWindows);
			b.setOverall(saveOverall);
			b.setRecording(recording_info);
			b.setOutputType(outputType);
			b.setFeatures(active,attribute);
			b.setAggregators(aggNames,aggFeatures,aggParameters);

			CommandLineThread clt = new CommandLineThread(b);
			clt.start();
			while(clt.isAlive()){
				if(System.in.available()>0){
					clt.cancel();
				}
				clt.join(1000);
			}
			
			System.out.println("\nFeatures extracted and save to "+ outputFileName);
		} catch (Exception e) {
			System.out.println("Error extracting features - aborting");
			System.out.println(e.getMessage());
			System.exit(5);
		}
		
	}
	
	
	//@createArffDefFile: if true then only create the arff def file
	public static void addClassLabel(String inputAudioFileName, String audioClass, boolean createArffDefFile){
		try{
			Scanner sc=new Scanner(new File(Constants.AUDIO_FEATURES_DIR+inputAudioFileName+".arff"));
			String outputFileName;
			if(!createArffDefFile) outputFileName=Constants.AUDIO_FEATURES_DIR+"value_"+inputAudioFileName+".arff";
			else outputFileName=Constants.AUDIO_FEATURES_DIR+"arff_header.txt";
			FileWriter fw=new FileWriter(outputFileName);
			String line;
			while(sc.hasNextLine()){
				line=sc.nextLine().trim();
				if(line.startsWith("@")){ //a header line
					if(line.startsWith("@DATA")){
						fw.write("@attribute class {on,off,openclose, open, close,  bus, noise}\n"); //add the class attribute
					}
					fw.write(line+"\n");
				}else{ // a data line
					if(!createArffDefFile){
						fw.write(line+","+audioClass+"\n");
					}
				}
			}
			sc.close();
			fw.close();
			System.out.println("Arff file saved to "+ outputFileName);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	
	public void main(){
		//obsoleted code: how to use the WavFile and DFT class
				/*System.out.println("Step 1: extract ampltitude data from audio files");		
				String inputAudioFile=Constants.AUDIO_BASE_DIR+"ted-talk"+".wav";
				double[] data= WavFile.readAllFrames(inputAudioFile) ;
				
				System.out.println("Step 2: FFT transform");
				DFT dft=new DFT(data.length);
				double[] Y=dft.forward(data);
				int n=Y.length;
				System.out.println(n);		
				System.out.println(Arrays.toString(Arrays.copyOfRange(Y, n-10, n)));*/
	}
	
	public static void extractFeatures(){
		//key: class ; value: audio file names
		HashMap<String, String[]> audioFiles=new HashMap<String, String[]>();
		
		//audioFiles.put("on", new String[]{"engineOn6","engineOn7"});//,"engineOn2-3","engineOn1","engineOn2","engineOn3","engineOn4","engineOn5"});
		//audioFiles.put("off", new String[]{"engineOff1"});
		
		//audioFiles.put("bus", new String[]{"bus6", "bus7","bus8", "bus9", "bus10"});
		
		//audioFiles.put("openclose", new String[]{"doorOpenAndClosed8","doorOpenAndClosed9","doorOpenAndClosed10","doorOpenAndClosed11","doorOpenAndClosed12"});
		//audioFiles.put("open", new String[]{"doorOpen9","doorOpen10","doorOpen11","doorOpen12","doorOpen13","doorOpen14","doorOpen15","doorOpen16"});
		audioFiles.put("close", new String[]{"doorClose11","doorClose12"});

		//audioFiles.put("noise", new String[]{"snoring", "snoring2"});		
		
		for(String audioClass: audioFiles.keySet()){
			for(String inputAudioFileName: audioFiles.get(audioClass)){
				//extrac the features
				extractFeatures(inputAudioFileName);
				
				//add the class for the file				
				addClassLabel(inputAudioFileName, audioClass, false);
				
				
			}
		}
	}
	
	public static void testClassifier(){
		File folder = new File(Constants.AUDIO_FEATURES_TEST_DIR);
		File[] listOfFiles = folder.listFiles();

		String[] models={"RandomForest","NBTree" , "J48", "NaiveBayes","RandomCommittee"};
		String classifierModel=models[0];		
		
		
		EventClassifier.run(false, Constants.CLASSIFIER_AUDIO, new String[]{"combined.arff"}, classifierModel, 0);
	    
		/*for (int i = 0; i < listOfFiles.length; i++) {
	      if (listOfFiles[i].isFile()) {
	    	//classify it
	    	//System.out.println(listOfFiles[i].getName());
	      } 
	    }*/

	}
	
	public static void parseFeatureListFile(){
		try{
			Scanner sc=new Scanner(new File(Constants.AUDIO_BASE_DIR+"features.xml"));
			String line;
			boolean afterFeatureStartElement=false, featureOn=false;
			while(sc.hasNextLine()){
				line=sc.nextLine().trim();
				if(line.equals("<feature>")){
					afterFeatureStartElement=true;
					if(!featureOn){
						System.out.println("def.add(false);");
					}	
					featureOn=false;
					continue;
				}
				if(line.equals("</feature>")){
					afterFeatureStartElement=false;
					continue;
				}
				if(afterFeatureStartElement){
					if(line.contains("."))
						System.out.println("extractors.add(new "+line.substring(line.lastIndexOf(".")+1, line.lastIndexOf("<"))+"());");
					else{
						featureOn=true;
						System.out.println("def.add(true);");
					}
				}
				if(line.startsWith("<aggregator>")){
					System.out.println("aggs.add(new " +line.substring(line.indexOf(">")+1, line.lastIndexOf("<"))+
						"() );");
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	
	

	public static void main(String[] args) {
		//create the arff header file;
		//addClassLabel("snoring", null, true);
		
		extractFeatures();
				
		//testClassifier();
		
		//parseFeatureListFile();
	}
	
	

}
