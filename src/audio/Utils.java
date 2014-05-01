package audio;

public class Utils {


	public final static double M_PI = Math.PI;
	public final static double M_2PI = 2*Math.PI;

	private static final double DB_NEG_INF = -100;
	
	
	
	public static double[] sinWave(int n, double volume, double freq, double sampleRate) {
		double[] ret = new double[n];
		double inc1 = M_2PI * freq / sampleRate;
		double angle1 = 0;

		for(int i = 0; i < n; i++)
		{
			ret[i] = Math.sin(angle1) * volume;
			angle1 += inc1;
		}

		return ret;
	}
	
	/*
	 * Add a sine wave to an existing array, the array is modified in place
	 */
	public static void addsinWave(double[] in, double volume, double freq, double sampleRate) {
		int n = in.length;
		double inc1 = M_2PI * freq / sampleRate;
		double angle1 = 0;

		for(int i = 0; i < n; i++)
		{
			in[i] += Math.sin(angle1) * volume;
			angle1 += inc1;
		}
	}

	public static double atan2(double x, double y)
	{
		double signx;
		if (x > 0.) signx = 1.;  
		else signx = -1.;

		if (x == 0.) return 0.;
		if (y == 0.) return signx * M_PI / 2.;

		return Math.atan2(x, y);
	}

	public static double HanningWindow(int k, int n) {
		return -.5*Math.cos(M_2PI*(double)k/(double)n)+.5;
	}
	
	/**
	 * 
	 * @param a Ratio between 0 .. 1
	 * @return between 0 .. 1 on a DB scale
	 */
	public static double MagtoDbNormalized(double a) {
		double db = 20*Math.log10(a);
		if (db < DB_NEG_INF)
			db = DB_NEG_INF;
		return (db - DB_NEG_INF)/-DB_NEG_INF;
	}
	
	public static double DbtoMagRatio(double db) {
		return Math.pow(10, db/20);
	}
	
	public static double CubicInterpolate(double y0, double y1, double y2, double y3, double x)
	{
	   double a, b, c, d;

	   a = y0 / -6.0 + y1 / 2.0 - y2 / 2.0 + y3 / 6.0;
	   b = y0 - 5.0 * y1 / 2.0 + 2.0 * y2 - y3 / 2.0;
	   c = -11.0 * y0 / 6.0 + 3.0 * y1 - 3.0 * y2 / 2.0 + y3 / 3.0;
	   d = y0;

	   double xx = x * x;
	   double xxx = xx * x;

	   return (a * xxx + b * xx + c * x + d);
	}
}
