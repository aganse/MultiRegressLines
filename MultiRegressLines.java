/** MultiRegressLines.java - example/testapp for dataTools & DataPlotWindow */

import ptolemy.plot.*;
import edu.washington.apl.aganse.ptolemyUpdates.plot.*;
import edu.washington.apl.aganse.dataTools.*;

/** MultiRegressLines is a commandline-based program that implements 
 *  multi-phase regression - regression of up to three co-joined line segments.
 *  Enter "java -jar MultiRegressLines.jar" at the commandline to get its
 *  usage listing; overall the idea is a loads your ascii columnar data
 *  file of X,Y points, lists out statistics and endpoints of the
 *  one-, two-, and three-phase linear regression fits to that data,
 *  and also puts up a plot of the data and regression lines in a
 *  new window.<BR>
 *  The plotting capability currently comes from the PtPlot (v2.0) component of
 *  <A HREF="http:ptolemy.eecs.berkeley.edu">PtolemyII</A> (Berkeley),
 *  with some modifications by various folks at APL-UW.
 * @author <A HREF="mailto:aganse@apl.washington.edu">Andy Ganse</A>,<BR>
 * <A HREF="http://www.apl.washington.edu">Applied Physics Laboratory</A>,<BR>
 * <A HREF="http://www.washington.edu">University of Washington</A>.
 * @version 18 Sep 2015 (initial version 25 Oct 2002)
 * @see <A HREF="SingleRegressionLine.html">SingleRegressionLine</A>
 * @see <A HREF="DoubleRegressionLine.html">DoubleRegressionLine</A>
 * @see <A HREF="TripleRegressionLine.html">TripleRegressionLine</A>
 */
public class MultiRegressLines {

    /** MultiRegressLines is a separate program that is called from the commandline
     *  as "java -jar MultiRegressLines <mydatafile>". */
    public static void main(String[] args) {

        DataSeries mydata = new DataSeries();

		System.out.println("");
		System.out.println("MultiRegressLines: implementation of multi-phase linear regression routines.");
		System.out.println("Andy Ganse, APL-UW, 2002-2015, aganse@apl.washington.edu");
		System.out.println("(see http://staff.washington.edu/aganse/mpregression/mpregression.html for discussion)\n");

		if(args.length!=1) {
			System.out.println("Usage: java -jar MultiRegressLines.jar <datafilename>");
			System.out.println("       (where datafile is a two-column ascii file of x and y data)");
			System.out.println("   or: java -jar MultiRegressLines.jar -exampledata");
			System.exit(1);
		}
		else if(args[0].equalsIgnoreCase("-exampledata")) {
			System.out.println("...using built-in example data, actually an oceanic soundspeed profile"+
							   "\nwith x=depth(m) and y=speed(m/s) :  Calculating...");
			mydata.addExampleData();
		}
		else {
			System.out.println("Data file "+args[0]+":  Calculating...");
			mydata.loadFromFile(args[0]);
		}

		SingleRegressionLine line1 = new SingleRegressionLine(mydata);
		DoubleRegressionLine line2 = new DoubleRegressionLine(mydata);
		TripleRegressionLine line3 = new TripleRegressionLine(mydata);

		System.out.println("NumPts = "+mydata.getNumPts());
		System.out.println("Data Range = "+mydata.getMinX()+"/"+mydata.getMaxX()+"/"+
						   mydata.getMinY()+"/"+mydata.getMaxY()+"\n");
		System.out.println(line1);
		System.out.println(line2);
		System.out.println(line3);

		DataPlotWindow myplot = new DataPlotWindow(mydata);
		//DepthDataPlotWindow myplot = new DepthDataPlotWindow(mydata);

		myplot.addDataSeries(line1.getEndPoints());
		myplot.addDataSeries(line2.getEndPoints());
		myplot.addDataSeries(line3.getEndPoints());
		myplot.setTitle("SSres1="+String.format("%4.2f",line1.getR())+
			"     SSres2="+String.format("%4.2f",line2.getR())+
			"     SSres3="+String.format("%4.2f",line3.getR()));

    }
}
