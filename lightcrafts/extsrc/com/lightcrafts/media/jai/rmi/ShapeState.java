/*
 * $RCSfile: ShapeState.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:55 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * This class is a serializable proxy for a Shape.
 *
 *
 * @since 1.1
 */
public class ShapeState extends SerializableStateImpl {
    private final static int SHAPE_UNKNOWN	    =	    0;
    private final static int SHAPE_AREA		    =	    1;
    private final static int SHAPE_ARC_DOUBLE	    =	    2;
    private final static int SHAPE_ARC_FLOAT	    =	    3;
    private final static int SHAPE_CUBICCURVE_DOUBLE=	    4;
    private final static int SHAPE_CUBICCURVE_FLOAT =	    5;
    private final static int SHAPE_ELLIPSE_DOUBLE   =	    6;
    private final static int SHAPE_ELLIPSE_FLOAT    =	    7;
    private final static int SHAPE_GENERALPATH	    =	    8;
    private final static int SHAPE_LINE_DOUBLE	    =	    9;
    private final static int SHAPE_LINE_FLOAT	    =	    10;
    private final static int SHAPE_QUADCURVE_DOUBLE =	    11;
    private final static int SHAPE_QUADCURVE_FLOAT  =	    12;
    private final static int SHAPE_ROUNDRECTANGLE_DOUBLE  = 13;
    private final static int SHAPE_ROUNDRECTANGLE_FLOAT   = 14;
    private final static int SHAPE_RECTANGLE_DOUBLE	  = 15;
    private final static int SHAPE_RECTANGLE_FLOAT	  = 16;

    public static Class[] getSupportedClasses() {
        return new Class[] {Shape.class};
    }

    /**
      * Constructs a <code>ShapeState</code> from a
      * <code>Shape</code>.
      *
      * @param c The class of the object to be serialized.
      * @param o The <code>Shape</code> to be serialized.
      * @param h The <code>RenderingHints</code> (ignored).
      */
    public ShapeState(Class c, Object o, RenderingHints h) {
        super(c, o, h);
    }

    /**
      * Serialize the <code>ShapeState</code>.
      *
      * @param out The <code>ObjectOutputStream</code>.
      */
    private void writeObject(ObjectOutputStream out) throws IOException {
	boolean serializable = false;
	Object object = theObject;

	// if the specific Shape is Serializable, then write itself;
	// if the specific Shape has a proxy itself, use that proxy;
	// for the regular Shapes, such as Arc, Ellipse and etc, write
	// the parameters;
	// for an Area, write the path and recover the Area;
	// for the instance of GeneralPath or unknown Shape, write the path;
	//
	if (object instanceof Serializable)
	    serializable = true;

	// deal with Serializable Shape such as Polygon and Rectangle or
	// the Shape has its own proxy.
	//
	out.writeBoolean(serializable);
	if (serializable) {
	    out.writeObject(object);
	    return;
	}
	  
	Object dataArray = null;
	Object otherData = null;
	int type = SHAPE_UNKNOWN;

	// deal with the regular Shapes 
	if (theObject instanceof Area)
	    type = SHAPE_AREA;
	else if (theObject instanceof Arc2D.Double) {
	    Arc2D.Double ad = (Arc2D.Double)theObject;
	    dataArray = new double[]{ad.x, ad.y, ad.width, ad.height, 
				     ad.start, ad.extent};
	    type = SHAPE_ARC_DOUBLE;
	    otherData = new Integer(ad.getArcType());
	} else if (theObject instanceof Arc2D.Float) {
	    Arc2D.Float af = (Arc2D.Float)theObject;
            dataArray = new float[]{af.x, af.y, af.width, af.height, 
                                     af.start, af.extent};
            type = SHAPE_ARC_FLOAT;
	    otherData = new Integer(af.getArcType());
	} else if (theObject instanceof CubicCurve2D.Double) {
	    CubicCurve2D.Double cd = (CubicCurve2D.Double)theObject;
            dataArray = new double[]{cd.x1, cd.y1, cd.ctrlx1, cd.ctrly1,
				     cd.ctrlx2, cd.ctrly2, cd.x2, cd.y2};
            type = SHAPE_CUBICCURVE_DOUBLE;
	} else if (theObject instanceof CubicCurve2D.Float) {
            CubicCurve2D.Float cf = (CubicCurve2D.Float)theObject;
            dataArray = new float[]{cf.x1, cf.y1, cf.ctrlx1, cf.ctrly1,
                                     cf.ctrlx2, cf.ctrly2, cf.x2, cf.y2};
            type = SHAPE_CUBICCURVE_FLOAT;
        } else if (theObject instanceof Ellipse2D.Double) {
            Ellipse2D.Double ed = (Ellipse2D.Double)theObject;
            dataArray = new double[]{ed.x, ed.y, ed.width, ed.height};
            type = SHAPE_ELLIPSE_DOUBLE;
        } else if (theObject instanceof Ellipse2D.Float) {
            Ellipse2D.Float ef = (Ellipse2D.Float)theObject;
            dataArray = new float[]{ef.x, ef.y, ef.width, ef.height};
            type = SHAPE_ELLIPSE_FLOAT;
        } else if (theObject instanceof GeneralPath) 
	    type = SHAPE_GENERALPATH;
	else if (theObject instanceof Line2D.Double) {
	    Line2D.Double ld = (Line2D.Double)theObject;
	    dataArray = new double[]{ld.x1, ld.y1, ld.x2, ld.y2};
	    type = SHAPE_LINE_DOUBLE;
	} else if (theObject instanceof Line2D.Float) {
            Line2D.Float lf = (Line2D.Float)theObject;
            dataArray = new float[]{lf.x1, lf.y1, lf.x2, lf.y2};
            type = SHAPE_LINE_FLOAT;
	} else if (theObject instanceof QuadCurve2D.Double) {
	    QuadCurve2D.Double qd = (QuadCurve2D.Double)theObject;
	    dataArray = new double[]{qd.x1, qd.y1, qd.ctrlx, qd.ctrly, qd.x2, qd.y2};
	    type = SHAPE_QUADCURVE_DOUBLE;
        } else if (theObject instanceof QuadCurve2D.Float) {
            QuadCurve2D.Float qf = (QuadCurve2D.Float)theObject;
	    dataArray = new float[]{qf.x1, qf.y1, qf.ctrlx, qf.ctrly, qf.x2, qf.y2};
            type = SHAPE_QUADCURVE_FLOAT;
        } else if (theObject instanceof RoundRectangle2D.Double) {
	    RoundRectangle2D.Double rrd = (RoundRectangle2D.Double)theObject;
	    dataArray = new double[]{rrd.x, rrd.y, rrd.width, rrd.height,
				     rrd.arcwidth, rrd.archeight};
	    type = SHAPE_ROUNDRECTANGLE_DOUBLE;
        } else if (theObject instanceof RoundRectangle2D.Float) {
	    RoundRectangle2D.Float rrf = (RoundRectangle2D.Float)theObject;
	    dataArray = new float[]{rrf.x, rrf.y, rrf.width, rrf.height,
                                     rrf.arcwidth, rrf.archeight};
            type = SHAPE_ROUNDRECTANGLE_FLOAT;
	} else if (theObject instanceof Rectangle2D.Double) {
            Rectangle2D.Double rd = (Rectangle2D.Double)theObject;
            dataArray = new double[]{rd.x, rd.y, rd.width, rd.height};
            type = SHAPE_RECTANGLE_DOUBLE;
        } else if (theObject instanceof Rectangle2D.Float) {
            Rectangle2D.Float rf = (Rectangle2D.Float)theObject;
            dataArray = new float[]{rf.x, rf.y, rf.width, rf.height};
            type = SHAPE_RECTANGLE_FLOAT;
        }
	// write Shape belonging to : GenrealPath, Area and unknown classes
	out.writeInt(type);
	if (dataArray != null) {
	    out.writeObject(dataArray);
	    if (otherData != null)
		out.writeObject(otherData);
	    return;
	}

	PathIterator pathIterator 
	    = ((Shape)theObject).getPathIterator(null);

	// obtain and write the winding rule 
	int rule = pathIterator.getWindingRule();
	out.writeInt(rule);

	float[] coordinates = new float[6];

	// iteratively write isDone, segment type and segment coordinates
	boolean isDone = pathIterator.isDone();
	while (!isDone){
	    int segmentType = pathIterator.currentSegment(coordinates);
	    out.writeBoolean(isDone);
	    out.writeInt(segmentType);
	    for (int i = 0; i < 6; i++)
		out.writeFloat(coordinates[i]) ;
	    pathIterator.next();
	    isDone = pathIterator.isDone();
	}
	out.writeBoolean(isDone);
    }

    /**
      * Deserialize the <code>ShapeState</code>.
      *
      * @param out The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

	boolean serializable = in.readBoolean();

	// if Serializable or has a specific proxy
	if (serializable) {
	    theObject = in.readObject();
	    return;
	}

	// read the type of the wrapped Shape
	int type = in.readInt();

	//for regular shapes, read the parameters and recover it
        switch (type) {
	case SHAPE_ARC_DOUBLE:
        {
	    double[] data = (double[])in.readObject();
	    int arcType = ((Integer)in.readObject()).intValue();
	    theObject = new Arc2D.Double(data[0], data[1], data[2], data[3],
				         data[4], data[5], arcType);
	    return;
	}
        case SHAPE_ARC_FLOAT:
        {
            float[] data = (float[])in.readObject();
	    int arcType = ((Integer)in.readObject()).intValue();
            theObject = new Arc2D.Float(data[0], data[1], data[2], data[3],
                                        data[4], data[5], arcType);
            return;
	}
	case SHAPE_CUBICCURVE_DOUBLE:
        {
	    double[] data = (double[])in.readObject();
	    theObject = new CubicCurve2D.Double(data[0], data[1], data[2],
						data[3], data[4], data[5],
						data[6], data[7]);
	    return;
	}
        case SHAPE_CUBICCURVE_FLOAT:
        {
            float[] data = (float[])in.readObject();
            theObject = new CubicCurve2D.Float(data[0], data[1], data[2],
                                                data[3], data[4], data[5],
                                                data[6], data[7]);
            return;
	}
	case SHAPE_ELLIPSE_DOUBLE:
        {
	    double[] data = (double[])in.readObject();
            theObject = new Ellipse2D.Double(data[0], data[1], data[2], data[3]);
            return;
	}
        case SHAPE_ELLIPSE_FLOAT:
        {
            float[] data = (float[])in.readObject();
            theObject = new Ellipse2D.Float(data[0], data[1], data[2], data[3]);
            return;
	}

	case SHAPE_LINE_DOUBLE:
        {
	    double[] data = (double[])in.readObject();
	    theObject = new Line2D.Double(data[0], data[1], data[2], data[3]);
	    return;
	}
        case SHAPE_LINE_FLOAT:
        {
            float[] data = (float[])in.readObject();
            theObject = new Line2D.Float(data[0], data[1], data[2], data[3]);
            return;
	}
	case SHAPE_QUADCURVE_DOUBLE:
        {
	    double[] data = (double[])in.readObject();
	    theObject = new QuadCurve2D.Double(data[0], data[1], data[2],
					       data[3], data[4], data[5]);
	    return;
	}
        case SHAPE_QUADCURVE_FLOAT:
        {
            float[] data = (float[])in.readObject();
            theObject = new QuadCurve2D.Float(data[0], data[1], data[2],
                                              data[3], data[4], data[5]);
            
	    return;
	}
	case SHAPE_ROUNDRECTANGLE_DOUBLE:
        {
	    double[] data = (double[])in.readObject();
            theObject = new RoundRectangle2D.Double(data[0], data[1], data[2],
                                                    data[3], data[4], data[5]);
            return;
	}
	case SHAPE_ROUNDRECTANGLE_FLOAT:
        {
	    float[] data = (float[])in.readObject();
            theObject = new RoundRectangle2D.Float(data[0], data[1], data[2],
                                                   data[3], data[4], data[5]);
            return;
	}
        case SHAPE_RECTANGLE_DOUBLE:
        {
            double[] data = (double[])in.readObject();
            theObject = new Rectangle2D.Double(data[0], data[1], data[2],
                                               data[3]);
            return;
        }
        case SHAPE_RECTANGLE_FLOAT:
        {
            float[] data = (float[])in.readObject();
            theObject = new Rectangle2D.Float(data[0], data[1], data[2],
                                              data[3]);
            return;
        }
	}

	//read the winding rule
	int rule = in.readInt();

	GeneralPath path = new GeneralPath(rule);
	float[] coordinates = new float[6];

	//read the path
	while (!in.readBoolean()) {
	    int segmentType = in.readInt();
	    for (int i = 0; i < 6; i++)
		coordinates[i] = in.readFloat();

	    switch (segmentType) {
		case PathIterator.SEG_MOVETO:
		    path.moveTo(coordinates[0], coordinates[1]);
            	    break;

	    	case PathIterator.SEG_LINETO:
		    path.lineTo(coordinates[0], coordinates[1]);
        	    break;

		case PathIterator.SEG_QUADTO:
		    path.quadTo(coordinates[0], coordinates[1],
				coordinates[2], coordinates[3]);
		    break;

		case PathIterator.SEG_CUBICTO:
		    path.curveTo(coordinates[0], coordinates[1],
				 coordinates[2], coordinates[3],
				 coordinates[4], coordinates[5]);
        	    break;

		case PathIterator.SEG_CLOSE:
		    path.closePath();
        	    break;
		default:
		    break;
	    }
	}

	//recover Area instance
	switch (type) {
	case SHAPE_AREA:
	    theObject = new Area(path);
	    break;
	default:
	    theObject = path;
	    break;
	}
    }
}

