package plot;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.math.plot.Plot2DPanel;


public class Plot {
	public static void main(String[] args) {
		//new Plot().main();
		// define your data
		
		
		
		double[] x = { 0, 1, 3, 5, 5, 8, 7, 9, 2, 5, 7, 8, 9, 2, 1 };
		int noOfBins = 10;
		for (int i = 0; i < 3; i++){
			try {
				histogram("Log Normal population", x, noOfBins, "figs/tmp"+i+".png");
				TimeUnit.SECONDS.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public static void histogram(String title, double[] values, int noOfBins, String saveFilePath) {
		try {
			// create your PlotPanel (you can use it as a JPanel)
			Plot2DPanel plot = new Plot2DPanel();
			// add the histogram of x to the PlotPanel
			//plot.addHistogramPlot(title, values, noOfBins);
			
			plot.addHistogramPlot(title, new double[][]{{-5,10,1},{5,7,1}});
			
			Max max=new Max();
			plot.setFixedBounds(0, 0, max.evaluate(values)+1);
			
			// put the PlotPanel in a JFrame like a JPanel
			JFrame frame = new JFrame(title);
			frame.setSize(600, 600);
			//frame.setLayout(new FlowLayout());
			//frame.add(plot);
			frame.setContentPane(plot);
			frame.setVisible(true);
			
			//File savedFile=new File(saveFilePath);
			//System.out.println(savedFile.getCanonicalPath());
			//plot.toGraphicFile(savedFile);//need a frame to run
						
			//frame.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
