/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import com.lightcrafts.utils.xml.XmlNode;
import com.lightcrafts.utils.xml.XMLException;

import java.text.NumberFormat;

/** A Scale is a model for a rational scaling of an image.  For convenience,
 * Scales may be defined by floating point numbers instead of ratios.
 * <p>
 * Query an Engine for its prefererred Scales, or make one up.  Preferred
 * Scales may work better.
 */
public class Scale implements Comparable {

    // Constants for serialization:
    private final static String NumeratorTag = "Numerator";
    private final static String DenominatorTag = "Denominator";
    private final static String FactorTag = "Factor";

    private int numerator;
    private int denominator;
    private float scale;

    /** Define a Scale by a ratio of integers.  A Scale deefined by integers
      * is a "rational" Scale.  Rational Scales may work better.
      * @param numerator An integer numerator of a scale ratio.
      * @param denominator An integer denominator of a scale ratio.
      */
    public Scale(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /** Restore a Scale that was saved by <code>save(XmlNode)</code>.
     */
    public Scale(XmlNode node) throws XMLException {
        String[] attrs = node.getAttributes();
        String key = attrs[0];
        String value = null;
        try {
            if (key.equals(FactorTag)) {
                value = node.getAttribute(key);
                scale = Float.parseFloat(value);
                return;
            }
            else if (key.equals(NumeratorTag) || key.equals(DenominatorTag)) {
                value = node.getAttribute(NumeratorTag);
                numerator = Integer.parseInt(value);
                value = node.getAttribute(DenominatorTag);
                denominator = Integer.parseInt(value);
                return;
            }
            else {
                throw new XMLException(
                    "Unexpected attribute: " + key
                );
            }
        }
        catch (NumberFormatException e) {
            throw new XMLException(
                "Not a number :\"" + value + "\"", e
            );
        }
    }

    /** Define a Scale as a simple floating point number.
      * @param scale The scale factor to use.
      */
    public Scale(float scale) {
        this.scale = scale;
    }

    /** Returns this Scale's scale factor: either the ratio "numerator /
     * denominator", or a copy of the argument to the constructor
     * Scale(float).
     */
    public float getFactor() {
        if (scale != 0) {
            return scale;
        }
        return numerator / (float) denominator;
    }

    /** If this Scale is rational, then return a String that looks like a
      * ratio.  Otherwise, return null.
      */
    public String toRationalString() {
        return "" + numerator + ":" + denominator;
    }

    /** Return this Scale as a percentage String, as in "50%" for a 1:2
     * Scale.
     */
    public String toPercentString() {
        NumberFormat format = NumberFormat.getPercentInstance();
        float s = getFactor();
        return format.format(s);
    }

    /** If this Scale is rational, return the result of toRationalString(),
      * otherwise return toPercentString().
      */
    public String toString() {
        if (scale != 0) {
            return toPercentString();
        }
        else {
            return toRationalString();
        }
    }

    /** A Scale is bigger than another if getFactor() returns a larger value.
      */
    public int compareTo(Object o) throws ClassCastException {
        Scale s = (Scale) o;
        float a = getFactor();
        float b = s.getFactor();
        if (a == b) {
            return 0;
        }
        return (a > b) ? 1 : -1;
    }

    /** Two Scales are equal if getFactor() returns equal values.
      */
    public boolean equals(Object o) {
        if (! (o instanceof Scale)) {
            return false;
        }
        Scale s = (Scale) o;
        return getFactor() == s.getFactor();
    }

    public void save(XmlNode node) {
        if (scale != 0) {
            node.setAttribute(FactorTag, Float.toString(getFactor()));
        }
        else {
            node.setAttribute(NumeratorTag, Integer.toString(numerator));
            node.setAttribute(DenominatorTag, Integer.toString(denominator));
        }
    }
}
