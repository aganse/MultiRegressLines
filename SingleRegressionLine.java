/** SingleRegressionLine.java */

package edu.washington.apl.aganse.dataTools;

import java.util.*;

/**
 * SingleRegressionLine - a class that calculates slope, y-intercept, and
 * residual sum of squares of a single line best-fitted to data with
 * least-squares, and offers the resulting values in individual "get" methods.
 * Must use an object of class DataSeries to operate on.
 * @author <A HREF="mailto:aganse@apl.washington.edu">Andy Ganse</A>,<BR>
 * <A HREF="http://www.apl.washington.edu">Applied Physics Laboratory</A>,<BR>
 * <A HREF="http://www.washington.edu">University of Washington</A>.
 * @version 18 Sep 2015 (initial version 24 Jan 2000)
 * @see <A HREF="DataSeries.html">DataSeries</A>
 * @see <A HREF="DoubleRegressionLine.html">DoubleRegressionLine</A>
 * @see <A HREF="TripleRegressionLine.html">TripleRegressionLine</A>
 * @see <A HREF="MultiRegressLines.html">MultiRegressLines</A>
 */
public class SingleRegressionLine {
    double slope, yint, R;
    int numPts;

    // handle to data vector, just so I don't have to keep passing it around:
    DataSeries data;

    /** Actual calculation done here in constructor, other methods just
     *  return the results */
    public SingleRegressionLine(DataSeries newdata) {
		data=newdata;
		double Ry, Rsum_y=0, Rsum_yy=0;
		DataSeries.Point p;
		numPts = data.getNumPts();
		slope = data.getSxy()/data.getSxx();
		yint = ( data.getSumY() - slope*data.getSumX() ) / (double)numPts;
		// tallying and calculating resid sum of squares R :
		for(Enumeration e=data.elements(); e.hasMoreElements(); ) {
			p=(DataSeries.Point)e.nextElement();
			Ry = p.getY() - slope*p.getX() - yint;  // residual
			Rsum_y += Ry;  // sum of residuals
			Rsum_yy += Ry*Ry;  // sum of squares of residuals
		}
		R = Rsum_yy;  // - Rsum_y*Rsum_y/data.getNumPts();
    }
    /** Returns the residual sum of squares for this line fitting.
     *  Notation warning: note this is SSres, not correlation coefficient!
     */
    public double getR() {
	return R;
    }
    /** Returns stdDev of residuals for this line fitting */
    public double getSigma() {
	return Math.sqrt(R/(numPts-1));
    }
    /** Returns the slope of the fitted line */
    public double getSlope() {
	return slope;
    }
    /** Returns the y-intercept of the fitted line */
    public double getYint() {
	return yint;
    }
	public DataSeries getEndPoints() {
		double min_x = data.getMinX();
		double max_x = data.getMaxX();
		DataSeries output = new DataSeries();
		output.add(min_x,getSlope()*min_x+getYint());
		output.add(max_x,getSlope()*max_x+getYint());
		return output;
	}
    /** return a String representation describing this regression fit,
     *  reporting stats and endpoints of the fit-lines and so on. */
    public String toString() {
		DataSeries tmp = getEndPoints();
		return "SingleRegressionLine: " + "\n" +
			"   Sum-of-squares-of-residuals = " + R + "\n" +
			"   Properties:\n" +
			"      Slope = " + slope + "\n" +
			"      Yint = " + yint + "\n" +
			"   Endpoints:\n" +
			"      " + tmp.getX(0) + ", " + tmp.getY(0) + "\n" +
			"      " + tmp.getX(1) + ", " + tmp.getY(1) + "\n";
    }
}









