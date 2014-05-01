package audio;
import java.util.Random;

import main.CommonUtils;
import main.Constants;
//reference: http://coding.ihunda.com/voicelocktrac/browser/Android/Unlocker/src/com/ihunda/android/sco

// compare between JTransforms and Apache Commons Math (ACM): https://sites.google.com/site/musicaudiohp/performance/jtransforms-vs-apache-commons-math
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;//http://incanter.org/docs/parallelcolt/api/allclasses-noframe.html

public class DFT {	
	public static void main(String[] args){
		
		int SIZE=8371;
		double[] input = new double[SIZE];
		Random rand=new Random();
		for(int i=0;i<SIZE;i++) input[i]=rand.nextDouble();
		
        DoubleFFT_1D fftDo = new DoubleFFT_1D(input.length);
//       double[] fft = new double[input.length * 2];
//       System.arraycopy(input, 0, fft, 0, input.length);
        fftDo.realForward(input);
 
/*        for(double d: input) {
            System.out.println(d);
        }*/
	}
	
	/*
	 * Compute real fft of input data
	 * data length must be equal to fft size n
	 * Data comes out a follow
	 * a[2*k] = Re[k], 0<=k<n/2
	 * a[2*k+1] = Im[k], 0<k<n/2
	 * a[1] = Re[n/2]	
	 */

	private static final double M_2PI = 2*Math.PI;
	/** 
	 * Length of the DFT
	 * */
	int n;
	private  DoubleFFT_1D fft;
	private double[] placeholder;
	private double[] magn_res;
	private double[] HammingWindow;
	private int res_len;

	public DFT(int n) {
		this.n = n;
		fft = new DoubleFFT_1D(n);
		
		HammingWindow = new double[n];
		for (int i=0; i<n; i++) {
			HammingWindow[i] = Constants.HAMMING_WINDOW_ALPHA - (1-Constants.HAMMING_WINDOW_ALPHA)*Math.cos(M_2PI*(double)i/(double)n);
		}
		res_len = n;
		placeholder = new double[res_len];
		magn_res = new double[n/2+1];
	}

	/*
	 * Compute real fft of input data
	 * data length must be equal to fft size n
	 * Data comes out a follow
	 * a[2*k] = Re[k], 0<=k<n/2
     * a[2*k+1] = Im[k], 0<k<n/2
     * a[1] = Re[n/2]
 
	 */
	/* (non-Javadoc)
	 * @see com.ihunda.audio.DFTi#forward(double[])
	 */
	public double[] forward(double[] in) {
		for (int i=0;i<n;i++)
			placeholder[i] = in[i];
		fft.realForward(placeholder);
		return placeholder;
	}
	
	/* (non-Javadoc)
	 * @see com.ihunda.audio.DFTi#forward(float[])
	 */
	public double[] forward(float[] in) {
		return forward(CommonUtils.floatToDoubleArray(in));
	}
	
	/* (non-Javadoc)
	 * @see com.ihunda.audio.DFTi#forwardHanning(double[])
	 */
	public double[] forwardHanning(double[] in) {
		
		for (int i=0; i<n; i++) {
			placeholder[i] = in[i] * HammingWindow[i];
		}
		fft.realForward(placeholder);
		return placeholder;
	}
	
	/* (non-Javadoc)
	 * @see com.ihunda.audio.DFTi#forwardHanning(float[])
	 */
	public double[] forwardHanning(float[] in) {
		for (int i=0; i<n; i++) {
			placeholder[i] =  in[i] * HammingWindow[i];
		}
		fft.realForward(placeholder);
		return placeholder;
	}
	
	/* (non-Javadoc)
	 * @see com.ihunda.audio.DFTi#reverse()
	 */
	public double[] reverse() {
	   fft.realInverse(placeholder, true);
	   return placeholder;
	}
	
	/* (non-Javadoc)
	 * @see com.ihunda.audio.DFTi#reverse(double[])
	 */
	public double[] reverse(double[] d) {
		for (int i=0; i<res_len; i++) {
			placeholder[i] =  d[i];
		}
		
		fft.realInverse(placeholder, true);
		return placeholder;
	}
	
	/* (non-Javadoc)
	 * @see com.ihunda.audio.DFTi#reverseNoCopy(double[])
	 */
	public double[] reverseNoCopy(double[] d) {
		fft.realInverse(d, true);
		return d;
	}
	
	/* (non-Javadoc)
	 * @see com.ihunda.audio.DFTi#magnitude()
	 */
	public double[] magnitude() {
		magn_res[0] = placeholder[0];
		magn_res[n/2] = placeholder[1];
		
		for (int i=1; i<n/2; i+=1) {
			magn_res[i] = Math.sqrt(placeholder[2*i]*placeholder[2*i] + placeholder[2*i+1]*placeholder[2*i+1]);
		}
		
		return magn_res;
	}
	
	/* (non-Javadoc)
	 * @see com.ihunda.audio.DFTi#magnitude(double[])
	 */
	public double[] magnitude(double[]  d) {
		placeholder = d;
		return magnitude();
	}
}
