/** MultiRegressDemo.java */

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*; 
import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import edu.washington.apl.aganse.dataTools.*;

/**
* MultiRegressDemo - interactive plot applet to demo the multiphase linear
* regression functions of my DataSeries class.  Note the data and layout are
* for depth profiles, as this algorithm was originally made for oceanic data,
* so the x axis is the vertical (depth) one and the y axis is the horizontal
* (soundspeed) one.  Of course the idea is the same though for the more
* standard case where the x axis is the horizontal.  If MultiRegressDemo is started
* in applet mode on a webpage or in appletviewer, one example data set 
* Numerical values seen in the display are in meters (depth) and meters
* per second (soundspeed).
* @author <A HREF="mailto:aganse@apl.washington.edu">Andy Ganse</A>, APL-UW
* @version 18 Jan 2000
* @see edu.washington.apl.aganse.DataSeries
*/
public class MultiRegressDemo extends JApplet {

    DataSeries data = new DataSeries();
    double[] regLine;
    //double sigTol1=0.5, sigTol2=1.0, sigTol3=4.0;
    double sigTol1=3.0, sigTol2=9.0, sigTol3=30.0;
    static BufferedReader inFile;
    String dataFileLine;
    boolean useBuiltInData=false;

    /** Initialize applet (just puts together the GUI) */
    public void init() {

	/** create label panel */
	JPanel lp = new JPanel();
        JTextArea lpText = 
	    new JTextArea("Click your own data points into the graph, " +
			  "or click the \"Example Data\" button to view a preset " +
			  "data profile, or click the Clear button to clear the graph.",
			  3 /*rows*/, 30 /*cols*/);
	lpText.setLineWrap(true);
	lpText.setWrapStyleWord(true);
	lpText.setEditable(false);
	lpText.setBackground(new Color(0,0,0,0));
	lp.setBorder(BorderFactory.createEtchedBorder());
	lp.add(lpText);

	/** create result panel to show numerical results
	 * (currently this is only stddev) */
	ResultPanel rp = new ResultPanel();

	/** create depthplot panel which will go onto graph panel */
	InteractiveDepthPlot dp=new InteractiveDepthPlot(0,1000,1400,1600,rp);

	/** create control panel */
	ControlPanel cp = new ControlPanel(dp);

	/** create graph panel */
	JPanel gp = new JPanel();
	gp.setBorder(BorderFactory.createCompoundBorder(
		BorderFactory.createEtchedBorder(),
		BorderFactory.createEmptyBorder(5,5,5,5)));
	gp.setLayout(new BorderLayout());
	gp.add(dp, BorderLayout.CENTER);

	/** add panels to applet */
	getContentPane().add(lp, BorderLayout.NORTH);
	getContentPane().add(gp, BorderLayout.CENTER);
	getContentPane().add(cp, BorderLayout.SOUTH);
	getContentPane().add(rp, BorderLayout.EAST);

    }


    /** ControlPanel is the panel at the bottom of the applet with the
	'clear' and 'example data' buttons on it */
    private class ControlPanel extends JPanel {
	InteractiveDepthPlot dp;
	public ControlPanel(InteractiveDepthPlot dp) {
	    this.dp = dp;
		JButton exampleDataBtn = new JButton("Example Profile Data");
		add(exampleDataBtn);
		exampleDataBtn.addActionListener(new EBlstnr());
	    JButton clearBtn = new JButton("Clear");
	    add(clearBtn);
	    clearBtn.addActionListener(new CBlstnr());
	    setBorder(BorderFactory.createEtchedBorder());
	}
	/** Button listener for exampleData button: */
	class EBlstnr implements ActionListener {
	    public void actionPerformed(ActionEvent e) {
		    data.clear();
		    data.addExampleData();
		    dp.repaint();
	   }
	}
	/** Button listener for clear button: */
	class CBlstnr implements ActionListener {
	    public void actionPerformed(ActionEvent e) {
		    data.clear();
		    regLine=null;
		    dp.repaint();
	    }
	}	
    }

    /** ResultPanel is the panel at the right of the applet with the
	numerical results */
    private class ResultPanel extends JPanel {
	JTextField stdDevValue = new JTextField(7);
	JLabel warningLabel = new JLabel(".");
	public ResultPanel() {
	    stdDevValue.setEditable(false);
	    stdDevValue.setHorizontalAlignment(JTextField.CENTER);
	    JLabel stdDevLabel = new JLabel("StdDev:");

		// all this GridBagLayout stuff is here because I originally
		// had a bunch of other output fields here.  As is, this could
		// be handled okay with just the default FlowLayout...
	    GridBagLayout gridbag = new GridBagLayout();
	    GridBagConstraints c = new GridBagConstraints();
	    setLayout(gridbag);
	    c.fill = GridBagConstraints.VERTICAL; 
	    c.gridx = 0;
	    c.ipadx = 5;
	    c.gridwidth = GridBagConstraints.REMAINDER;
	    c.insets = new Insets(5,0,0,0);  //top padding
	    gridbag.setConstraints(stdDevLabel, c); add(stdDevLabel);
	    c.insets = new Insets(0,0,0,0);  //remove padding
	    gridbag.setConstraints(stdDevValue, c); add(stdDevValue);
	    c.insets = new Insets(0,0,30,0);  //bottom padding
	    gridbag.setConstraints(warningLabel, c); add(warningLabel);
	    c.ipady = 70;
	    c.ipadx = 70;
	    c.weighty = 1.0;   //request any extra vertical space
	    c.insets = new Insets(0,0,5,0);  //bottom padding
	    setBorder(BorderFactory.createEtchedBorder());
	}
	public void paintComponent(Graphics g) {
	    DecimalFormat fixed = new DecimalFormat("#####.##");
	    if(regLine!=null) {
			stdDevValue.setText(fixed.format(regLine[0]));
			if(regLine.length==1 ||
			   (regLine.length==9 && regLine[0]>sigTol3) ) {
				warningLabel.setEnabled(true);
				warningLabel.setText("Bad Line Fit!");
			} else {
				warningLabel.setEnabled(false);
				warningLabel.setText(".");
			}

		} else {
			stdDevValue.setText("(no data)");
			warningLabel.setEnabled(false);
			warningLabel.setText(".");
		}
		super.paintComponent(g);
	}
    }


    /** InteractiveDepthPlot contains the actual graph plot itself, which
	allows new points to be clicked in via mouse, and displays the data
	points as well as the regression line.  InteractiveDepthPlot assumes
	positive depth in DataSeries object. */
    private class InteractiveDepthPlot extends Canvas
		implements MouseListener {

	Dimension r;
	DataSeries.Point p;
	DS2PanelXY grid;
	Dimension outerAxisBorder = new Dimension(5,5); //15,15 with axislabels
	Dimension innerAxisBorder = new Dimension(15,15); //25,25 with axislab.
	ResultPanel rp;

	/** note constructor requires plot bounds (in depth & sndspd) */
	public InteractiveDepthPlot(double dMin, double dMax,
				double ssMin, double ssMax, ResultPanel rp) {
	    this.rp=rp;

	    setBackground(Color.white);
	    grid = new DS2PanelXY(dMin, dMax, ssMin, ssMax);
	    addMouseListener(this);  //  for clicking to add new data points
	}

	/** catch mouse click and if location is valid, add data point */
	public void mousePressed(MouseEvent e) {
	    e.consume();
	    int tmpx=e.getX();
	    int tmpy=e.getY();
	    if(tmpx>=innerAxisBorder.width &&
	       tmpx<=r.width-outerAxisBorder.width &&
     	       tmpy>=innerAxisBorder.height &&
	       tmpy<=r.height-outerAxisBorder.height) {
		data.add(grid.getD(tmpy),grid.getSS(tmpx));
		repaint();
	    }
	}
	/** This method is declared but empty because I had to declare the
	 *  rest of the methods in the implemented MouseListener interface. */
 	public void mouseReleased(MouseEvent e) {
	}
	/** This method is declared but empty because I had to declare the
	 *  rest of the methods in the implemented MouseListener interface. */
	public void mouseEntered(MouseEvent e) {
	}
	/** This method is declared but empty because I had to declare the
	 *  rest of the methods in the implemented MouseListener interface. */
	public void mouseExited(MouseEvent e) {

	}
	/** This method is declared but empty because I had to declare the
	 *  rest of the methods in the implemented MouseListener interface. */
	public void mouseClicked(MouseEvent e) {
	}

	public void paint(Graphics g) {
	    rp.repaint();
	    r = getSize();  /** get size of canvas */
	    grid.update(r); /** tell size of canvas to 'grid' so it knows how
						 *  to convert plot locations to/from data points */
	    g.setColor(Color.black);
	    /** Draw x-axes */
	    g.drawLine(outerAxisBorder.width,
		       innerAxisBorder.height,
		       r.width-outerAxisBorder.width,
		       innerAxisBorder.height);
	    /** Draw arrow at tip of x-axis: */
	    g.drawLine(r.width-outerAxisBorder.width,
		       innerAxisBorder.height,
		       r.width-outerAxisBorder.width-5,
		       innerAxisBorder.height-5);
	    g.drawLine(r.width-outerAxisBorder.width,
		       innerAxisBorder.height,
		       r.width-outerAxisBorder.width-5,
		       innerAxisBorder.height+5);
	    /** Draw y-axes */
	    g.drawLine(innerAxisBorder.width,
		       outerAxisBorder.height,
		       innerAxisBorder.width,
		       r.height-outerAxisBorder.height);
	    /** Draw arrow at tip of y-axis: */
	    g.drawLine(innerAxisBorder.width,
		       r.height-outerAxisBorder.height,
		       innerAxisBorder.width-5,
		       r.height-outerAxisBorder.height-5);
	    g.drawLine(innerAxisBorder.width,
		       r.height-outerAxisBorder.height,
		       innerAxisBorder.width+5,
		       r.height-outerAxisBorder.height-5);
	    /** Draw data points */
	    for (Enumeration e = data.elements(); e.hasMoreElements(); ) {
		g.setColor(Color.blue);
		p = (DataSeries.Point)e.nextElement();
		/** In the fillOval line below, the x's and y's are not swapped
		 * accidentally.  In InteractiveDepthPlot, depth is the
		 * independent variable, but is shown per convention on the 
		 * y-axis.  In DataSeries.Point (note object p) the x-axis is
		 * the dependent axis, hence the swap here.  Sorry for the
		 * confusion, but I'm trying to match convention in both. */
		g.fillOval( grid.getX( p.getY() /* ie SS */ ) - 2,
			    grid.getY( p.getX() /* ie depth */) - 2,
			    4, 4);
	    }
	    /** Draw regression line (if enough pts) */
	    if (data.getNumPts() > 1){
		regLine = data.bestRegressionLine(sigTol1,sigTol2,sigTol3);
		if (regLine.length==5) {
			/** if 5 args in regLine then it's a single regression
			 *  line: stdDev,x1,y1,x2,y2 */
		    g.setColor(Color.red);
		    g.drawLine(grid.getX(regLine[2]), grid.getY(regLine[1]),
			       grid.getX(regLine[4]), grid.getY(regLine[3]));
		}
		else if (regLine.length==7) {
		    /** if 7 args in regLine then it's a 2-phase regression
			 *  line: stdDev,x1,y1,x2,y2,x3,y3 */
		    g.setColor(Color.cyan);
		    g.drawLine(grid.getX(regLine[2]), grid.getY(regLine[1]),
			       grid.getX(regLine[4]), grid.getY(regLine[3]));
		    g.drawLine(grid.getX(regLine[4]), grid.getY(regLine[3]),
			       grid.getX(regLine[6]), grid.getY(regLine[5]));
		}
 		else if (regLine.length==9) {
		    /** if 9 args in regLine then it's a 3-phase regression
			 * line: stdDev,x1,y1,x2,y2,x3,y3,x4,y4 */
		    g.setColor(Color.green);
		    g.drawLine(grid.getX(regLine[2]), grid.getY(regLine[1]),
			       grid.getX(regLine[4]), grid.getY(regLine[3]));
		    g.drawLine(grid.getX(regLine[4]), grid.getY(regLine[3]),
			       grid.getX(regLine[6]), grid.getY(regLine[5]));
		    g.drawLine(grid.getX(regLine[6]), grid.getY(regLine[5]),
			       grid.getX(regLine[8]), grid.getY(regLine[7]));
		}
	    }
	}

	private class DS2PanelXY {
	    double dMin, dMax, ssMin, ssMax;
	    Dimension r;
	    public DS2PanelXY(double dMin, double dMax,
				  double ssMin, double ssMax) {
		this.dMin=dMin;
		this.dMax=dMax;
		if(dMin>=dMax) this.dMax=this.dMin+10;
		this.ssMin=ssMin;
		this.ssMax=ssMax;
		if(ssMin>=ssMax) this.ssMax=this.ssMin+10;
	    }
	    public void update(Dimension r) {
		this.r = r;
	    }
	    public double getDMin() {
		return dMin;
	    }
	    public double getDMax() {
		return dMax;
	    }
	    public double getSSMin() {
		return ssMin;
	    }
	    public double getSSMax() {
		return ssMax;
	    }
	    public int getX(double ss) {
		return innerAxisBorder.width + 
		 (int)( (ss-ssMin)/(ssMax-ssMin) * 
		 (r.width - innerAxisBorder.width - outerAxisBorder.width));
	    }
	    public int getY(double depth) {
		return innerAxisBorder.height + 
		 (int)( (depth-dMin)/(dMax-dMin) * 
	         (r.height - innerAxisBorder.height - outerAxisBorder.height));
	    }
	    public double getSS(int x) {
		return ssMin + (
		   (ssMax-ssMin) * (double)(x-innerAxisBorder.width) /
		   (double)
		   (r.width-innerAxisBorder.width-outerAxisBorder.width) );
	    }
	    public double getD(int y) {
		return dMin + (
		   (dMax-dMin) * (double)(y-innerAxisBorder.height) /
		   (double)
		   (r.height-innerAxisBorder.height-outerAxisBorder.height) );
	    }
	}
    }
    

    /** To run applet in its own frame when run as a separate application */
    public static void main(String[] args) {

	/** put applet into application window frame */
	MultiRegressDemo applet = new MultiRegressDemo();
	Frame myFrame = new Frame("MultiPhase Regression Demo Applet");
	/** add window-closing control so can close application */
    myFrame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
            System.exit(0);
        }
    });
	myFrame.add(applet, BorderLayout.CENTER);
	myFrame.setSize(420,500);
	applet.init();
	applet.start();
	myFrame.setVisible(true);
    }

}
