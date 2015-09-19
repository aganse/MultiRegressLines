/** TripleRegressionLine.java */

package edu.washington.apl.aganse.dataTools;

import java.util.*;

/**
 * DoubleRegressionLine computes and returns a two-phase linear regression
 * fit done on a DataSeries object.
 * It calculates slope, y-intercept, and total residual sum of
 * squares for two cojoined lines best-fitted to the data with least-
 * squares.
 * @author <A HREF="mailto:aganse@apl.washington.edu">Andy Ganse</A>,<BR>
 * <A HREF="http://www.apl.washington.edu">Applied Physics Laboratory</A>,<BR>
 * <A HREF="http://www.washington.edu">University of Washington</A>.
 * @version 18 Sep 2015 (initial version 25 Oct 2002)
 * @see DataSeries
 * @see <A HREF="SingleRegressionLine.html">SingleRegressionLine</A>
 * @see <A HREF="TripleRegressionLine.html">TripleRegressionLine</A>
 */
public class DoubleRegressionLine {

    // handle to data vector, just so I don't have to keep passing it around
    DataSeries data;

    // for passing data from constructor to "get-" methods :
    double _R;                        // total resid sum of sq of fit
    double _slope1, _slope2;          // slopes of the fit lines
    double _yint1, _yint2;            // y-intercepts of the fit lines
    double _x;                        // x-values of intersections of fit lines
    double _avgSigma;                 // mean of stdDevs of resids for lines

    /** Most of the actual calculation done here in constructor, most of
     *  the other methods just return the results */
    public DoubleRegressionLine(DataSeries newdata) {

        double Rmin;                      // Min total resid sum of sq for fit
        double Rlines;                    // Total resid sum of squares from
                                          //   proposed fit-lines
        double avgSigma;                  // mean of stdDevs of resids
        double B1lines=0;                 // Slopes of the proposed fit-lines
        double B2lines=0;                 //   formed by choices of X1 
                                          //   (Blines1 has lowest x value)
        double yint1=0;                   // Y-intercepts of proposed fit-lines
        double yint2=0;                   //   (yint1 has lowest x value)
        double X1lines=0;                 // X values of intersections
                                          //   of proposed fit-lines
        double X1;                        // X values of where to divide data
                                          //   into 2 segments, each of which
                                          //   will have a fit-line calculated
                                          //   via least-squares.
                                          //   The intersection of those lines
                                          //   will not necessarily be at X1
                                          //   in fact, the best choice of X1
                                          //   is when the intersection of
                                          //   the fit-lines is as close as
                                          //   possible to X1. That's what the
                                          //   calculation checks for.
        DataSeries subdata1, subdata2;    // current 2 trial sections
        int j;                            // Last data index before X1
        DataSeries.Point pj;              // temp var to access x[j]
        
        // attach global handle
        data = newdata;
        
        // First sort the data to get it in order of increasing x value.
        // The calculations in this DoubleRegressionLine object REQUIRE the
        // data to be in sorted order on the xaxis.
        data.sort();
        
        // Setting initial X1 value at x data midpoint
        DataSeries.Point tmp1, tmp2;
        tmp1 = (DataSeries.Point)data.elementAt((int)(data.getNumPts()-1));
        tmp2 = (DataSeries.Point)data.elementAt(0);
        X1 = (tmp1.getX()-tmp2.getX())/2 + tmp2.getX();
        //X1=((DataSeries.Point)data.elementAt(2)).getX();
        System.out.println("X1="+X1);
        
        // Initial stat values for output, in case the initial guesses are the
        // best values and none of these get assigned below(!)
        TotalResidSumSq totalResidSumSq = new TotalResidSumSq();
        totalResidSumSq.calculate(X1);
        _R=Rmin=totalResidSumSq.getRlines();
        _avgSigma=totalResidSumSq.getAvgSigma();
        _slope1 = totalResidSumSq.getB1lines();
        _slope2 = totalResidSumSq.getB2lines();
        _yint1 = totalResidSumSq.getYint1();
        _yint2 = totalResidSumSq.getYint2();
        _x = totalResidSumSq.getX1lines();
		if(_x<=data.getMinX() || _x>=data.getMaxX()) {
			_x=X1;
			_R=1.0e16;  // ie want any Rlines to be less than this
		}
        
        // Iterate over (almost) all combinations of the two intersection
        // points, keeping minimum resid sum of squares R as we go, and the
        // X1 associated with it.  Note that numPts must be at least 3.
        for( j=1; j<data.getNumPts()-2; j++ ) {  // note starting w/ 2nd datapt
            pj = (DataSeries.Point)data.elementAt(j);
            
            // calc B*1, B*2, B*3, R* :
            totalResidSumSq.calculate(pj.getX()+0.5);
            Rlines=totalResidSumSq.getRlines();
            avgSigma=totalResidSumSq.getAvgSigma();
            X1lines = totalResidSumSq.getX1lines();
            B1lines = totalResidSumSq.getB1lines();
            B2lines = totalResidSumSq.getB2lines();
            yint1 = totalResidSumSq.getYint1();
            yint2 = totalResidSumSq.getYint2();
            if( Rlines<Rmin && X1lines>data.getMinX() && X1lines<data.getMaxX()) {
                if( liesInRectangle( X1lines, pj.getX() ) ) {
                System.out.println("in rectangle");
                    X1 = X1lines;
                    Rmin = Rlines;
                    _R=Rlines;
                    _avgSigma=avgSigma;
                    _slope1 = B1lines;
                    _slope2 = B2lines;
                    _yint1 = yint1;
                    _yint2 = yint2;
                    _x = X1lines;
                }
            }
        }
    }
    
    /** Used in constructor to calculate and supply the total residual sum
     *  of squares and some related quantities based on the given
     *  intersection points for the two fit-lines */
    private class TotalResidSumSq {
        
        double Rlines, X1lines, avgSigma;
        double B1lines, B2lines, yint1, yint2;
        DataSeries subdata1, subdata2; // current 2 trial sections
        DataSeries.Point p; // tmp point used while dividing into 2 sections
        SingleRegressionLine line1, line2;
        
        public void calculate(double X1) {
            subdata1 = new DataSeries();
            subdata2 = new DataSeries();
            // is there a better way to divide data into subdata1&2?
            for(Enumeration e=data.elements(); e.hasMoreElements(); ) {
                p=(DataSeries.Point)e.nextElement();
                if( p.getX()<=X1 ) subdata1.add(p);
                else subdata2.add(p);
            }
            line1 = new SingleRegressionLine(subdata1);
            line2 = new SingleRegressionLine(subdata2);
            B1lines = line1.getSlope();
            B2lines = line2.getSlope();
            yint1 = line1.getYint();
            yint2 = line2.getYint();
            Rlines = line1.getR() + line2.getR();
            avgSigma = (line1.getR()/(subdata1.getNumPts()-1) +
                        line2.getR()/(subdata2.getNumPts()-1) ) / 2;
            X1lines = (yint2-yint1)/(B1lines-B2lines);
            System.out.println("X1lines in loop = "+X1lines);
        }
        public double getRlines() {
            return Rlines;
        }
        public double getAvgSigma() {
            return avgSigma;
        }
        public double getB1lines() {
            return B1lines;
        }
        public double getB2lines() {
            return B2lines;
        }
        public double getYint1() {
            return yint1;
        }
        public double getYint2() {
            return yint2;
        }
        public double getX1lines() {
            return X1lines;
        }
        public DataSeries getSubData1() {
            return subdata1;
        }
        public DataSeries getSubData2() {
            return subdata2;
        }
    }
    
    
    /** Used in constructor to check whether X1lines is
     * close enough to X1 */
    private boolean liesInRectangle( double X1lines, double X1 ) {
        // first find the x values contained in the data vector that are
        // on either side of X1
        double lastx=0., X1left=0., X1right=0.;
        DataSeries.Point p;
        for(Enumeration e=data.elements(); e.hasMoreElements(); ) {
            p=(DataSeries.Point)e.nextElement();
            if( p.getX()>=X1 && lastx<=X1 ) {
                X1left = lastx;
                X1right = p.getX();
            }
            lastx = p.getX();
        }

        if( X1lines>=X1left && X1lines<=X1right )
            return true;
        else
            return false;
    }
    
    /** Returns the total residual sum of squares for this fitting.
     *  Notation warning: note this is SSres, not correlation coefficient!
     */
    public double getR() {
        return _R;
    }
    /** Returns the mean of the 2 stdDevs of the residuals for this fitting  */
    public double getAvgSigma() {
        return _avgSigma;
    }
    /** Returns the slope of the first (least x value) fitted line */
    public double getSlope1() {
        return _slope1;
    }
    /** Returns the y-intercept of the first (least x value) fitted line */
    public double getYint1() {
        return _yint1;
    }
    /** Returns the slope of the second (middle) fitted line */
    public double getSlope2() {
        return _slope2;
    }
    /** Returns the y-intercept of the second (middle) fitted line */
    public double getYint2() {
        return _yint2;
    }
    /** Calculates and returns the first (lesser) intersection point
     *  (x-value) */
    public double getX1() {
        return _x;
    }
    public DataSeries getEndPoints() {
        double min_x = data.getMinX();
        double max_x = data.getMaxX();
        DataSeries output = new DataSeries();
        output.add(min_x,getSlope1()*min_x+getYint1());
        output.add(getX1(),getSlope1()*getX1()+getYint1());
        output.add(max_x,getSlope2()*max_x+getYint2());
        return output;
    }
    /** return a String representation describing this 2-phase regression,
     *  reporting stats and endpoints of the fit-lines and so on. */
    public String toString() {
        DataSeries tmp = getEndPoints();
        return "DoubleRegressionLine:\n" +
            "   Sum of the two sums-of-squares-of-residuals = " + _R + "\n" +
            "   Properties:\n" +
            "      Slope1 = " + _slope1 + "\n" +
            "       Yint1 = " + _yint1 + "\n" +
            "      Slope2 = " + _slope2 + "\n" +
            "       Yint2 = " + _yint2 + "\n" +
            "   Endpoints:\n" + 
            "      " + tmp.getX(0) + ", " + tmp.getY(0) + "\n" +
            "      " + tmp.getX(1) + ", " + tmp.getY(1) + "\n" +
            "      " + tmp.getX(2) + ", " + tmp.getY(2) + "\n";
    }
}



