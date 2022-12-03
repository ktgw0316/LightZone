/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import com.lightcrafts.model.ZoneOperation;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import java.util.Iterator;
import java.util.LinkedList;

/** The model for ZoneControl.  The state of a ZoneControl is defined by a
 * ZoneOperation, which accepts control points as a double[] and returns
 * interpolated values in the same format.
 * <p>
 * A ZoneModel extends the ZoneOperation model in these ways:
 * <ul>
 * <li>more flexible access to the ZoneOperation, like setting and clearing
 *     individual control points;</li>
 * <li>change notifications;</li>
 * <li>guarantees monotonicity;</li>
 * <li>save/restore.</li>
 * </ul>
 */

class ZoneModel {

    private ZoneOperation op;
    private int size;               // number of control points (zones+1)
    private double[] points;        // 0...size, -1 means uncontrolled
    private LinkedList listeners;

    private int batch;              // batch change depth counter

    ZoneModel(ZoneOperation op, int size) {
        this.op = op;
        this.size = size;
        listeners = new LinkedList();
        reset();
    }

    void operationChanged(ZoneOperation op) {
        this.op = op;
        push();
    }

    void addZoneModelListener(ZoneModelListener listener) {
        listeners.add(listener);
    }

    void removeZoneModelListener(ZoneModelListener listener) {
        listeners.remove(listener);
    }

    int getSize() {
        return size;
    }

    void reset() {
        points = new double[size + 1];
        for (int n=0; n<=size; n++) {
            points[n] = -1;
        }
        push();
    }

    /** Bind an index between zero and the size (inclusive) to a value
     * between zero and one (inclusive).  Note that values in this model must
     * be a monotonically decreasing function of the indices.  See
     * <code>checkValue()</code>.
      */
    void setPoint(int index, double value) {
        checkValue(index, value);
        points[index] = value;
        push();
    }

    void removePoint(int index) {
        points[index] = -1;
        push();
    }

    boolean containsPoint(int index) {
        return points[index] >= 0;
    }

    double getValueAt(int index) {
        return op.getControlPoint(index);
    }

    /** See if a given (index, value) pair is within legal bounds for the
      * model.  Legal bounds for "index" are 0 to "size" inclusive.  Legal
      * values for "value" are zero to one inclusive.  Also, there is a
      * global constraint that "value" must always be a monotonically
      * decreasing function of "index".
      * <p>
      * If bad values are submitted to <code>setPoint()</code>, then that
      * method will throw an IllegalArgumentException, just like this method
      * does.
      */
    void checkValue(int index, double value) {
        if ((index < 0) || (index > size)) {
            throw new IllegalArgumentException("Illegal index: " + index);
        }
        if ((value < 0) || (value > 1)) {
            throw new IllegalArgumentException("Illegal value: " + value);
        }
        // Ensure that the model remains monotonic:
        double max = 0;
        for (int n=0; n<=size; n++) {
            if (n == index) {
                if (value < max) {
                    throw new IllegalArgumentException(
                        "Non-monotonic pair: (" + index + ", " + value + ")"
                    );
                }
                max = value;
            }
            else if (points[n] >= 0) {
                if (points[n] < max) {
                    throw new IllegalArgumentException(
                        "Non-monotonic pair: (" + index + ", " + value + ")"
                    );
                }
                max = points[n];
            }
        }
    }

    void batchStart() {
        if (batch++ == 0) {
            notifyListenersStart();
        }
    }

    void batchEnd() {
        if (--batch == 0) {
            notifyListenersEnd();
        }
    }

    private void push() {
        double[] copy = new double[points.length];
        System.arraycopy(points, 0, copy, 0, points.length);
        op.setControlPoints(copy);
        notifyListenersChanged();
    }

    private void notifyListenersChanged() {
        ZoneModelEvent event = new ZoneModelEvent(this);
        for (Iterator i=listeners.iterator(); i.hasNext(); ) {
            ZoneModelListener listener = (ZoneModelListener) i.next();
            listener.zoneModelChanged(event);
        }
    }

    private void notifyListenersStart() {
        op.changeBatchStarted();
        ZoneModelEvent event = new ZoneModelEvent(this);
        for (Iterator i=listeners.iterator(); i.hasNext(); ) {
            ZoneModelListener listener = (ZoneModelListener) i.next();
            listener.zoneModelBatchStart(event);
        }
    }

    private void notifyListenersEnd() {
        op.changeBatchEnded();
        ZoneModelEvent event = new ZoneModelEvent(this);
        for (Iterator i=listeners.iterator(); i.hasNext(); ) {
            ZoneModelListener listener = (ZoneModelListener) i.next();
            listener.zoneModelBatchEnd(event);
        }
    }

    private final static String SizeTag = "Size";
    private final static String PointsTag = "Points";
    private final static String PointTag = "Point";
    private final static String XTag = "X";
    private final static String YTag = "Y";

    void save(XmlNode node) {
        XmlNode ptsNode = node.addChild(PointsTag);
        ptsNode.setAttribute(SizeTag, Integer.toString(size));
        for (int n=0; n<points.length; n++) {
            if (points[n] >= 0) {
                Integer key = n;
                Double value = points[n];
                XmlNode ptNode = ptsNode.addChild(PointTag);
                ptNode.setAttribute(XTag, key.toString());
                ptNode.setAttribute(YTag, value.toString());
            }
        }
    }

    void restore(XmlNode node) throws XMLException {
        XmlNode ptsNode = node.getChild(PointsTag);
        try {
            int s = Integer.parseInt(ptsNode.getAttribute(SizeTag));
            if (s != size) {
                throw new XMLException("Unsupported size change");
            }
            reset();
        }
        catch (NumberFormatException e) {
            throw new XMLException(
                "Not an integer: \"" + ptsNode.getAttribute(SizeTag) + "\"", e
            );
        }
        XmlNode[] ptNodes = ptsNode.getChildren(PointTag);
        for (XmlNode ptNode : ptNodes) {
            Integer key;
            Double value;
            try {
                key = Integer.valueOf(ptNode.getAttribute(XTag));
            }
            catch (NumberFormatException e) {
                throw new XMLException(
                    "Not an integer: \"" + ptNode.getAttribute(XTag) + "\"", e
                );
            }
            try {
                value = Double.valueOf(ptNode.getAttribute(YTag));
            }
            catch (NumberFormatException e) {
                throw new XMLException(
                    "Not a number: \"" + ptNode.getAttribute(YTag) + "\"", e
                );
            }
            try {
                setPoint(key, value);
            }
            catch (IllegalArgumentException e) {
                throw new XMLException(
                    "Invalid zone mapping: (" + key + ", " + value + ")"
                );
            }
        }
        notifyListenersChanged();
    }
}
