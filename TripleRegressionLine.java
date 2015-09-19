/** TripleRegressionLine.java */

package edu.washington.apl.aganse.dataTools;

import java.util.*;

/**
 * TripleRegressionLine computes and returns a three-phase linear regression
 * fit done on a DataSeries object.
 * It calculates slope, y-intercept, and total residual sum of
 * squares for three cojoined lines best-fitted to the data with least-
 * squares.
 * This 
 * The math used to do this in this class is called "three-phase linear
 * regression", and is based on the following article which I found in
 * the journal <I>Biometrics</I> at the
 * <A HREF="http://www.lib.washington.edu">
 * University of Washington Libraries</A>.
 * <BLOCKQUOTE>
 * Williams, D.A.  "Discrimination between regression models to determine
 * the pattern of enzyme synthesis in synchronous cell cultures".
 * <I>Biometrics</I>,  v.26, March 1970.  pp23-32.
 * </BLOCKQUOTE>
 * Note when comparing to that article, that the X axis here corresponds
 * to the T axis in the article, and variables denoted with an appended
 * asterisk in the article have the string "lines" appended here.
 * @author <A HREF="mailto:aganse@apl.washington.edu">Andy Ganse</A>,<BR>
 * <A HREF="http://www.apl.washington.edu">Applied Physics Laboratory</A>,<BR>
 * <A HREF="http://www.washington.edu">University of Washington</A>.
 * @version 18 Sep 2015 (initial version 25 Oct 2002)
 * @see <A HREF="DataSeries.html">DataSeries</A>
 * @see <A HREF="SingleRegressionLine.html">SingleRegressionLine</A>
 */
public class TripleRegressionLine {

    // handle to data vector, just so I don't have to keep passing it around
    DataSeries data;

    // for passing data from constructor to "get-" methods :
    double _R;                        // total resid sum of sq of fit
    double _slope1, _slope2, _slope3; // slopes of the three fit lines
    double _yint1, _yint2, _yint3;    // y-intercepts of the three fit lines
    double _x1, _x2;                  // x-values of intersections of fit lines
    double _avgSigma;                 // mean of the three stdDevs of residuals
                                      // = (R1/(N1-1)+R2/(N2-1)+R3/(N3-1))/3
    
    /** Most of the actual calculation done here in constructor, most of
     *  the other methods just return the results */
    public TripleRegressionLine(DataSeries newdata) {
        
        double Rmin;                      // Min total resid sum of sq for fit
        double Rnew;                      // Temp var for R when finding
                                          // next iteration of Rmin
        double Rlines;                    // Total resid sum of squares from
                                          //   proposed fit-lines formed by
                                          //   X1 and X2 choices
        double avgSigma;                  // mean of 3 stdDevs of residuals
        double B1lines=0;                 // Slopes of the three proposed
        double B2lines=0;                 //   fit-lines formed by choices
        double B3lines=0;                 //   of X1 and X2
                                          //   (Blines1 has lowest x value)
        double yint1=0;                   // Y-intercepts of the three
        double yint2=0;                   //   proposed fit-lines 
        double yint3=0;                   //   (yint1 has lowest x value)
        double X1lines=0;                 // X values of intersections
        double X2lines=0;                 //   of proposed fit-lines
        double X1, X2;                    // X values of where to divide data
                                          //   into 3 segments, each of which
                                          //   will have a fit-line calculated
                                          //   via least-squares.
                                          //   The intersections of those lines
                                          //   will not necessarily be at X1
                                          //   and X2 - in fact, the best
                                          //   choice of X1, X2 is when the
                                          //   intersections of those fit-lines
                                          //   are as close as possible to
                                          //   X1 and X2.  That's what the
                                          //   calculation checks for.
        DataSeries subdata1, subdata2, subdata3; // current 3 trial sections
        int j;                            // Last data index before X1
        int k;                            // Last data index before X2
        DataSeries.Point pj, pk;          // temp vars to access x[j] and x[k]
        
        // attach global handle
        data = newdata;
        
        // First ort the data to get it in order of increasing x value.
        // The calculations in this TripleRegressionLine object REQUIRE the
        // data to be in sorted order on the xaxis.
        data.sort();
        
        // Setting initial X1 & X2 values at 1/3 total x range
        DataSeries.Point tmp1, tmp2;
        tmp1 = (DataSeries.Point)data.elementAt((int)(data.getNumPts()-1));
        tmp2 = (DataSeries.Point)data.elementAt(0);
        X1 = (tmp1.getX()-tmp2.getX())/3 + tmp2.getX();
        X2 = 2*(tmp1.getX()-tmp2.getX())/3 + tmp2.getX();
        
        // Initial Rmin based on initial X1 & X2 guesses
        Rmin = nextR(X1,X2);

        
        // Initial stat values for output, in case the initial guesses are the
        // best values and none of these get assigned below(!)
        TotalResidSumSq totalResidSumSq = new TotalResidSumSq();
        totalResidSumSq.calculate(X1,X2);
        _R=totalResidSumSq.getRlines();
        _avgSigma=totalResidSumSq.getAvgSigma();
        _slope1 = totalResidSumSq.getB1lines();
        _slope2 = totalResidSumSq.getB2lines();
        _slope3 = totalResidSumSq.getB3lines();
        _yint1 = totalResidSumSq.getYint1();
        _yint2 = totalResidSumSq.getYint2();
        _yint3 = totalResidSumSq.getYint3();
        _x1 = totalResidSumSq.getX1lines();
        _x2 = totalResidSumSq.getX2lines();
        
        // Iterate over (almost) all combinations of the two intersection
        // points, keeping minimum resid sum of squares R as we go, and the
        // X1 & X2 associated with it.  Note that numPts must be at least 7.
        for( j=1; j<data.getNumPts()-4; j++ ) {
            for( k=j+2; k<data.getNumPts()-2; k++ ) {
                pj = (DataSeries.Point)data.elementAt(j);
                pk = (DataSeries.Point)data.elementAt(k);
                // calc B*1, B*2, B*3, R* :
                totalResidSumSq.calculate(pj.getX(),pk.getX());
                Rlines=totalResidSumSq.getRlines();
                avgSigma=totalResidSumSq.getAvgSigma();
                X1lines = totalResidSumSq.getX1lines();
                X2lines = totalResidSumSq.getX2lines();
                B1lines = totalResidSumSq.getB1lines();
                B2lines = totalResidSumSq.getB2lines();
                B3lines = totalResidSumSq.getB3lines();
                yint1 = totalResidSumSq.getYint1();
                yint2 = totalResidSumSq.getYint2();
                yint3 = totalResidSumSq.getYint3();

                if( Rlines < Rmin && X1lines>=data.getMinX() &&
                    X2lines<=data.getMaxX()) {
                    if( liesInRectangle( X1lines, X2lines, pj.getX(), pk.getX() ) ) {
                        X1 = X1lines;
                        X2 = X2lines;
                        Rmin = Rlines;
                    } else {
                        subdata1 = totalResidSumSq.getSubData1();
                        subdata2 = totalResidSumSq.getSubData2();
                        subdata3 = totalResidSumSq.getSubData3();
                        Rnew = nextR( pj.getX(), pk.getX(),
                            Rlines, B1lines, B2lines, B3lines,
                            X1lines, X2lines,
                            subdata1, subdata2, subdata3);
                        if( Rnew < Rmin ) {
                            X1 = pj.getX();
                            X2 = pk.getX();
                            Rmin = Rnew;
                            _R=Rlines;
                            _avgSigma=avgSigma;
                            _slope1 = B1lines;
                            _slope2 = B2lines;
                            _slope3 = B3lines;
                            _yint1 = yint1;
                            _yint2 = yint2;
                            _yint3 = yint3;
                            _x1 = X1lines;
                            _x2 = X2lines;
                            
                        }
                    }
                }
            }
        }
    }
    
    /** Used in constructor to calculate and supply the total residual sum
     *  of squares and some related quantities based on the given
     *  intersection points for the three fit-lines */
    private class TotalResidSumSq {
        
        double Rlines, X1lines, X2lines, avgSigma;
        double B1lines, B2lines, B3lines, yint1, yint2, yint3;
        DataSeries subdata1, subdata2, subdata3; // current 3 trial sections
        DataSeries.Point p; // tmp point used while dividing into 3 sections
        SingleRegressionLine line1, line2, line3;
        
        public void calculate(double X1, double X2) {
            subdata1 = new DataSeries();
            subdata2 = new DataSeries();
            subdata3 = new DataSeries();
            // is there a better way to divide data into subdata1-3?
            for(Enumeration e=data.elements(); e.hasMoreElements(); ) {
                p=(DataSeries.Point)e.nextElement();
                if( p.getX()<=X1 ) subdata1.add(p);
                else if( p.getX()>X1 && p.getX()<=X2 ) subdata2.add(p);
                else subdata3.add(p);
            }
            line1 = new SingleRegressionLine(subdata1);
            line2 = new SingleRegressionLine(subdata2);
            line3 = new SingleRegressionLine(subdata3);
            B1lines = line1.getSlope();
            B2lines = line2.getSlope();
            B3lines = line3.getSlope();
            yint1 = line1.getYint();
            yint2 = line2.getYint();
            yint3 = line3.getYint();
            Rlines = line1.getR() + line2.getR() + line3.getR();
            avgSigma = (line1.getR()/(subdata1.getNumPts()-1) +
                        line2.getR()/(subdata2.getNumPts()-1) +
                        line3.getR()/(subdata3.getNumPts()-1) ) / 3;
            if(yint2==yint1 && B1lines==B2lines) {
                X1lines=data.getMinX();
            } else {
                X1lines = (yint2-yint1)/(B1lines-B2lines);
            }
            if(yint3==yint2 && B2lines==B3lines) {
                X2lines=data.getMaxX();
            } else {
                X2lines = (yint3-yint2)/(B2lines-B3lines);
            }
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
        public double getB3lines() {
            return B3lines;
        }
        public double getYint1() {
            return yint1;
        }
        public double getYint2() {
            return yint2;
        }
        public double getYint3() {
            return yint3;
        }
        public double getX1lines() {
            return X1lines;
        }
        public double getX2lines() {
            return X2lines;
        }
        public DataSeries getSubData1() {
            return subdata1;
        }
        public DataSeries getSubData2() {
            return subdata2;
        }
        public DataSeries getSubData3() {
            return subdata3;
        }
    }
    
    /** Used in constructor to calculate residual sum of squares R(X1,X2)
     *  about three fitted lines constrained to meet at (X1,X2) */
    private double nextR(double X1, double X2) {
        double Rlines, B1lines, B2lines, B3lines, X1lines, X2lines;
        DataSeries subdata1, subdata2, subdata3; // current 3 trial sections
        TotalResidSumSq totalResidSumSq = new TotalResidSumSq();
        totalResidSumSq.calculate(X1,X2);
        B1lines = totalResidSumSq.getB1lines();
        B2lines = totalResidSumSq.getB2lines();
        B3lines = totalResidSumSq.getB3lines();
        X1lines = totalResidSumSq.getX1lines();
        X2lines = totalResidSumSq.getX2lines();
        Rlines = totalResidSumSq.getRlines();
        subdata1 = totalResidSumSq.getSubData1();
        subdata2 = totalResidSumSq.getSubData2();
        subdata3 = totalResidSumSq.getSubData3();
        return nextR(X1, X2, Rlines, B1lines, B2lines, B3lines,
                     X1lines, X2lines, subdata1, subdata2, subdata3);
    }
    
    /** Used in constructor to calculate residual sum of squares R(X1,X2)
     *  about three fitted lines constrained to meet at (X1,X2)...
     *  (this version uses some extra predefined parameters.)
     *  Again refer to the William article for the source of these equations */
    private double nextR(double X1, double X2, double Rlines,
                         double B1lines, double B2lines, double B3lines,
                         double X1lines, double X2lines,
                         DataSeries subdata1, DataSeries subdata2,
                         DataSeries subdata3) {
        double mAm;  // represents the m'*inv(A)*m term in Williams' equation
        double m1,m2; // m matrix
        double a11,a12,a22; // A matrix, note a12=a21 so only a12 used here
        m1 = (B1lines-B2lines)*(X1-X1lines);
        m2 = (B2lines-B3lines)*(X2-X2lines);
        a11 = 1/subdata1.getNumPts() + 1/subdata2.getNumPts() +
            (subdata1.getXmean() - X1)*(subdata1.getXmean() - X1) /
            subdata1.getSxx() +
            (subdata2.getXmean() - X1)*(subdata2.getXmean() - X1) /
            subdata2.getSxx();
        a12 = -1/subdata2.getNumPts() - 
            (subdata2.getXmean() - X1)*(subdata2.getXmean() - X2) /
            subdata2.getSxx();
        a22 = 1/subdata2.getNumPts() + 1/subdata3.getNumPts() +
            (subdata2.getXmean() - X2)*(subdata2.getXmean() - X2) /
            subdata2.getSxx() +
            (subdata3.getXmean() - X2)*(subdata3.getXmean() - X2) /
            subdata3.getSxx();
        mAm = 1/(a11*a22-a12*a12) * ( m1*m1*a22 - 2*m1*m2*a12 + m2*m2*a11 );
        
        return Rlines + mAm;
    }
    
    /** Used in constructor to check whether X1lines and X2lines are
     *  close enough to X1 and X2 */
    private boolean liesInRectangle( double X1lines, double X2lines,
                                     double X1, double X2 ) {
        // first find the x values contained in the data vector that are
        // on either side of X1 and X2 ( = X1left, X1right, X2left, X2right)
        double lastx=0., X1left=0., X1right=0., X2left=0., X2right=0.;
        DataSeries.Point p;
        for(Enumeration e=data.elements(); e.hasMoreElements(); ) {
            p=(DataSeries.Point)e.nextElement();
            if( p.getX()>=X1 && lastx<=X1 ) {
                X1left = lastx;
                X1right = p.getX();
            }
            if( p.getX()>=X2 && lastx<=X2 ) {
                X2left = lastx;
                X2right = p.getX();
            }
            lastx = p.getX();
        }
        if( X1lines>=X1left && X1lines<=X1right &&
            X2lines>=X2left && X2lines<=X2right )
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
    /** Returns the mean of the 3 stdDevs of the residuals for this fitting  */
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
    /** Returns the slope of the third (greatest x value) fitted line */
    public double getSlope3() {
        return _slope3;
    }
    /** Returns the y-intercept of the third (greatest x value) fitted
     *  line */
    public double getYint3() {
        return _yint3;
    }
    /** Calculates and returns the first (lesser) intersection point
     *  (x-value) */
    public double getX1() {
        return _x1;
    }
    /** Calculates and returns the second (greater) intersection point
     *  (x-value) */
    public double getX2() {
        return _x2;
    }
    public DataSeries getEndPoints() {
        double min_x = data.getMinX();
        double max_x = data.getMaxX();
        DataSeries output = new DataSeries();
        output.add(min_x,getSlope1()*min_x+getYint1());
        output.add(getX1(),getSlope1()*getX1()+getYint1());
        output.add(getX2(),getSlope2()*getX2()+getYint2());
        output.add(max_x,getSlope3()*max_x+getYint3());
        return output;
    }
    /** return a String representation describing this 3-phase regression,
     *  reporting stats and endpoints of the fit-lines and so on. */
    public String toString() {
        DataSeries tmp = getEndPoints();
        return "TripleRegressionLine:\n" +
            "   Sum of the three sums-of-squares-of-residuals = " + _R + "\n" +
            "   Properties:\n" +
            "     Slope1 = " + _slope1 + "\n" +
            "      Yint1 = " + _yint1 + "\n" +
            "     Slope2 = " + _slope2 + "\n" +
            "      Yint2 = " + _yint2 + "\n" +
            "     Slope3 = " + _slope3 + "\n" +
            "      Yint3 = " + _yint3 + "\n" +
            "   Endpoints:\n" +
            "      " + tmp.getX(0) + ", " + tmp.getY(0) + "\n" +
            "      " + tmp.getX(1) + ", " + tmp.getY(1) + "\n" +
            "      " + tmp.getX(2) + ", " + tmp.getY(2) + "\n" +
            "      " + tmp.getX(3) + ", " + tmp.getY(3) + "\n";
    }
}



