package helper;

import java.util.HashMap;

public class RawReadingDuration{
	HashMap<String, Integer> dateSeqDuration;
	public RawReadingDuration(){
		dateSeqDuration=new HashMap<String, Integer>();
	}
	public void add(String dateSeq, int duration){
		dateSeqDuration.put(dateSeq, duration);
	}
	
	public int totalDuration(){
		int sum=0;
		for(int dur: dateSeqDuration.values()){
			sum+=dur;
		}
		return sum;
	}
}
