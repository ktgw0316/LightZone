/*
 * $RCSfile: NegotiableNumeric.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:51 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

/**
 * A class that wraps a numeric primitive data type or a subclass of 
 * <code>Number</code> to implement the <code>Negotiable</code> interface. 
 * <code>NegotiableNumeric</code> is a convenience class to specify a
 * <code>Negotiable</code> value for a parameter which has a single
 * valid numeric value.
 *
 * @since JAI 1.1
 */
public class NegotiableNumeric implements Negotiable {

    // The numeric value stored as a Number subclass;
    Number number;

    // The Class of the Number.    
    Class elementClass;
    
    /**
     * Creates a <code>NegotiableNumeric</code> given a <code>byte</code>.
     *
     * @param b The <code>byte</code> to be wrapped to implement 
     *          <code>Negotiable</code>.
     */
    public NegotiableNumeric(byte b) {
	number = new Byte(b);
	elementClass = number.getClass();
    }
    
    /**
     * Creates a <code>NegotiableNumeric</code> given a <code>short</code>.
     *
     * @param s The <code>short</code> to be wrapped to implement 
     *          <code>Negotiable</code>.
     */
    public NegotiableNumeric(short s) {
	number = new Short(s);
	elementClass = number.getClass();
    }
    
    /**
     * Creates a <code>NegotiableNumeric</code> given an <code>int</code>.
     *
     * @param i The <code>int</code> to be wrapped to implement 
     *          <code>Negotiable</code>.
     */
    public NegotiableNumeric(int i) {
	number = new Integer(i);
	elementClass = number.getClass();
    }
    
    /**
     * Creates a <code>NegotiableNumeric</code> given a <code>long</code>.
     *
     * @param l The <code>long</code> to be wrapped to implement 
     *          <code>Negotiable</code>.
     */
    public NegotiableNumeric(long l) {
	number = new Long(l);
	elementClass = number.getClass();
    }
    
    /**
     * Creates a <code>NegotiableNumeric</code> given a <code>float</code>.
     *
     * @param f The <code>float</code> to be wrapped to implement 
     *          <code>Negotiable</code>.
     */
    public NegotiableNumeric(float f) {
	number = new Float(f);
	elementClass = number.getClass();
    }
    
    /**
     * Creates a <code>NegotiableNumeric</code> given a <code>double</code>.
     *
     * @param d The <code>double</code> to be wrapped to implement 
     *          <code>Negotiable</code>.
     */
    public NegotiableNumeric(double d) {
	number = new Double(d);
	elementClass = number.getClass();
    }
    
    /**
     * Creates a <code>NegotiableNumeric</code> given a <code>Number</code>.
     *
     * @param n The <code>Number</code> to be wrapped to implement 
     *          <code>Negotiable</code>.
     *
     * @throws IllegalArgumentException if n is null.
     */
    public NegotiableNumeric(Number n) {

	if (n == null) {
	    throw new IllegalArgumentException(
				     JaiI18N.getString("NegotiableNumeric0"));
	}

	number = n;
	elementClass = number.getClass();
    }

    /**
     * Returns the <code>Number</code> that is currently the valid value
     * for this class. A valid primitive data type value, such as int, 
     * will be returned as a member of the corresponding wrapper class, 
     * such as <code>Integer</code>.
     */
    public Number getNumber() {
	return number;
    }

    /**
     * Returns a <code>NegotiableNumeric</code> that contains the value
     * that is common to this <code>NegotiableNumeric</code>
     * and the one supplied, i.e the <code>Number</code> encapsulated in
     * both the <code>NegotiableNumeric</code> are equal. If the supplied
     * <code>Negotiable</code> is not a <code>NegotiableNumeric</code> with
     * its element being of the same <code>Class</code> as this class', or
     * if there is no common value (i.e the values are not equal), the
     * negotiation will fail and <code>null</code> will be returned.
     *
     * @param other The <code>Negotiable</code> to negotiate with.
     */
    public Negotiable negotiate(Negotiable other) {
	if (other == null)
	    return null;

	if (!(other instanceof NegotiableNumeric) ||
	    other.getNegotiatedValueClass() != elementClass) {
	    return null;
	}

	NegotiableNumeric otherNN = (NegotiableNumeric)other;

	if (number.equals(otherNN.getNumber())) {
	    return new NegotiableNumeric(number);
	} else {
	    return null;
	}
    }

    /**
     * Returns the result of the negotiation as a <code>Number</code> 
     * subclass. Values belonging to a base type, such as <code>int</code>, 
     * will be returned as a member of the corresponding <code>Number</code>
     * subclass, such as <code>Integer</code>.
     */
    public Object getNegotiatedValue() {
	return number;
    }

    /**
     * Returns the <code>Class</code> of the negotiated value. Values
     * belonging to a base type, such as <code>int</code>, will be returned
     * as a member of the corresponding <code>Number</code> subclass, such as
     * <code>Integer</code>. The <code>Class</code> returned similarly will be
     * a <code>Number</code> subclass.
     */
    public Class getNegotiatedValueClass() {
	return elementClass;
    }

    /**
     * A convenience method to return the single negotiated value as a
     * <code>byte</code>. 
     *
     * @throws ClassCastException if the value is of a different Class type.
     */
    public byte getNegotiatedValueAsByte() {
	if (elementClass != Byte.class) 
	    throw new ClassCastException(
				    JaiI18N.getString("NegotiableNumeric1"));
	return number.byteValue();
    }
    
    /**
     * A convenience method to return the single negotiated value as a 
     * <code>short</code>. 
     *
     * @throws ClassCastException if the value is of a different Class type.
     */
    public short getNegotiatedValueAsShort() {
	if (elementClass != Short.class) 
	    throw new ClassCastException(
				    JaiI18N.getString("NegotiableNumeric1"));
	return number.shortValue();
    }

    /**
     * A convenience method to return the single negotiated value as a
     * <code>int</code>. 
     *
     * @throws ClassCastException if the value is of a different Class type.
     */
    public int getNegotiatedValueAsInt() {
	if (elementClass != Integer.class) 
	    throw new ClassCastException(
				    JaiI18N.getString("NegotiableNumeric1"));
	return number.intValue();
    }

    /**
     * A convenience method to return the single negotiated value as a
     * <code>long</code>. 
     *
     * @throws ClassCastException if the value is of a different Class type.
     */
    public long getNegotiatedValueAsLong() {
	if (elementClass != Long.class) 
	    throw new ClassCastException(
				    JaiI18N.getString("NegotiableNumeric1"));
	return number.longValue();
    }

    /**
     * A convenience method to return the single negotiated value as a 
     * <code>float</code>. 
     *
     * @throws ClassCastException if the value is of a different Class type.
     */
    public float getNegotiatedValueAsFloat() {
	if (elementClass != Float.class) 
	    throw new ClassCastException(
				    JaiI18N.getString("NegotiableNumeric1"));
	return number.floatValue();
    }

    /**
     * A convenience method to return the single negotiated value as a
     * <code>double</code>. 
     *
     * @throws ClassCastException if the value is of a different Class type.
     */
    public double getNegotiatedValueAsDouble() {
	if (elementClass != Double.class) 
	    throw new ClassCastException(
				    JaiI18N.getString("NegotiableNumeric1"));
	return number.doubleValue();
    }
}
