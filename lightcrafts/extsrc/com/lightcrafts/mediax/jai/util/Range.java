/*
 * $RCSfile: Range.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:57 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.util;

import java.io.Serializable;

/**
 * A class to represent ranges of values. A range is defined to contain 
 * all the values between the minimum and maximum values, where 
 * the minimum/maximum value can be considered either included or excluded 
 * from the range. 
 *
 * <p> This example creates a range of <code>Integer</code>s whose minimum
 * value is 1 and the maximum value is 5. The range is inclusive at both
 * ends:
 *
 * <p><code>
 * Range intRange = new Range(Integer.class, new Integer(1), new Integer(5)); 
 * </code>
 *
 * <p> A <code>Range</code> can be unbounded at either or both of its ends.
 * An unbounded end is specified by passing null for the value of that end.
 * A <code>Range</code> unbounded at both of its ends represents a range of
 * all possible values for the <code>Class</code> of elements in that
 * <code>Range</code>. The <code>isMinIncluded()</code> method will always
 * return true for a <code>Range</code> unbounded on the minimum side and
 * correspondingly the <code>isMaxIncluded()</code> method will always
 * return true for a <code>Range</code> unbounded on the maximum side.
 *
 * <p> An empty range is defined as a <code>Range</code> whose minimum value
 * is greater than it's maximum value if the ends are included, or as a
 * <code>Range</code> whose minimum value is greater than or equal to it's
 * maximum value, if the minimum or the maximum value is excluded.
 *
 * @since JAI 1.1
 */
public class Range implements Serializable {
    
    // The class of the elements in this Range.
    private Class elementClass;

    // The minimum and maximum values of the range.
    private Comparable minValue, maxValue;

    // The the minimum/maximum value is included in the range.i
    // The default value is true, that is, included.
    private boolean isMinIncluded =true, isMaxIncluded = true ;

    /**
     * Constructs a <code>Range</code> object given the <code>Class</code>
     * of the elements in the <code>Range</code>, the minimum value and 
     * the maximum value. The minimum and the maximum value are considered
     * inclusive.
     *
     * <p> An unbounded range can be specified by passing in a null for
     * either of the two values, in which case the <code>Range</code> is
     * unbounded on one side, or for both, in which case the
     * <code>Range</code> represents an all inclusive set.
     * 
     * @param elementClass  The <code>Class</code> of the <code>Range</code>
     *                      elements.
     * @param minValue      The lowest value included in the <code>Range</code>.
     * @param maxValue      The highest value included in the <code>Range</code>.
     *
     * @throws IllegalArgumentException if minValue and maxValue are both null,
     *					and elementClass is not one of the 
     *					subclasses of <code>Comparable</code>.
     * @throws IllegalArgumentException if minValue is not the same 
     *                                  <code>Class</code> as elementClass.
     * @throws IllegalArgumentException if maxValue is not the same 
     *                                  <code>Class</code> as elementClass.
     */
    public Range(Class elementClass, Comparable minValue, Comparable maxValue) {

        // If both minValue and maxValue are null, check whether elementClass
        // is an instanceof Comparable.
        if ((minValue == null) && (maxValue == null)) {
            Class c = null ;
            try {
                c = Class.forName("java.lang.Comparable") ;
            }
            catch (ClassNotFoundException e) {
            }

            if (!c.isAssignableFrom(elementClass))
                throw new IllegalArgumentException(JaiI18N.getString("Range0") ) ;
        }

	this.elementClass = elementClass;

	if (minValue != null && minValue.getClass() != this.elementClass) {
	    throw new IllegalArgumentException(JaiI18N.getString("Range1")) ;
	}

	this.minValue = minValue;

	if (maxValue != null && maxValue.getClass() != this.elementClass) {
	    throw new IllegalArgumentException(JaiI18N.getString("Range2")) ;
	}

	this.maxValue = maxValue;
    }

    /**
     * Constructs a <code>Range</code> object given the <code>Class</code>
     * of the elements in the <code>Range</code>, the minimum value and
     * the maximum value. Whether the minimum value and the maximum value
     * are considered inclusive is specified via the 
     * <code>isMinIncluded</code> and <code>isMaxIncluded</code> variables.
     *     
     * <p> An unbounded range can be specified by passing in a null for
     * either of the two values, in which case the <code>Range</code> is
     * unbounded at one end, or for both, in which case the
     * <code>Range</code> represents an all inclusive set. If null is passed
     * in for either variable, the <code>boolean</code> variables have
     * no effect.
     *     
     * @param elementClass  The <code>Class</code> of the <code>Range</code>
     *                      elements.
     * @param minValue      The lowest value for the <code>Range</code>.
     * @param isMinIncluded A boolean that defines whether the minimum value is
     *                      included in the <code>Range</code>.
     * @param maxValue      The highest value for the <code>Range</code>.
     * @param isMaxIncluded A boolean that defines whether the maximum value is
     *                      included in the <code>Range</code>.
     *
     * @throws IllegalArgumentException if minValue and maxValue are both null,
     *                                  and elementClass is not one of the
     *                                  subclasses of <code>Comparable</code>.
     * @throws IllegalArgumentException if minValue is not the same 
     *                                  <code>Class</code> as elementClass.
     * @throws IllegalArgumentException if maxValue is not the same 
     *                                  <code>Class</code> as elementClass.
     */
    public Range(Class elementClass, 
		 Comparable minValue, 
		 boolean isMinIncluded,
		 Comparable maxValue,
		 boolean isMaxIncluded) {
	this(elementClass, minValue, maxValue) ;
	this.isMinIncluded = isMinIncluded ;
	this.isMaxIncluded = isMaxIncluded ;
    }

    /**
     * Returns true if the minimum value is included within this
     * <code>Range</code>. If the range is unbounded at this end, this
     * method will return true.
     */
    public boolean isMinIncluded() {
	if (this.minValue == null)
	    return true ;

	return isMinIncluded ;
    }

    /**
     * Returns true if the maximum value is included within this
     * <code>Range</code>. If the range is unbounded at this end, this
     * method will return true.
     */
    public boolean isMaxIncluded() {
	if (this.maxValue == null)
	    return true ;

	return isMaxIncluded ;
    }

    /**
     * Returns the <code>Class</code> of the elements of this <code>Range</code>.
     */
    public Class getElementClass() {
	return elementClass;
    }

    /**
     * Returns the minimum value of this <code>Range</code>.
     * Returns null if the <code>Range</code> is unbounded at this end.
     */
    public Comparable getMinValue() {
	return minValue;
    }

    /**
     * Returns the maximum value of this <code>Range</code>.
     * Returns null if the <code>Range</code> is unbounded at this end.
     */
    public Comparable getMaxValue() {
	return maxValue;
    }

    /**
     * Returns true if the specified value is within this <code>Range</code>,
     * i.e. is either equal to or greater than the minimum value of this
     * <code>Range</code> and is either lesser than or equal to the maximum
     * value of this <code>Range</code>.    
     *
     * @param value  The value to be checked for being within this 
     *               <code>Range</code>.
     * @throws IllegalArgumentException if the <code>Class</code> of the value
     * parameter is not the same as the elementClass of this <code>Range</code>.
     */
    public boolean contains(Comparable value) {

        if (value != null && value.getClass() != elementClass) {
            throw new IllegalArgumentException( JaiI18N.getString("Range3"));
        }

	// First check if the Range is empty
	if (isEmpty() == true)
	    return false;

	// check both bounds.
	return isUnderUpperBound(value) && isOverLowerBound(value) ;
    }

    /**
     * Return true if the specific value is smaller than the maximum of
     * this range. If this range is unbounded at the maximum end, return true;
     * if the specific value is null and the maximum end is bounded, return 
     * false, that is, suppose this null is the "positive infinite". 
     */
    private boolean isUnderUpperBound(Comparable value ) {
	// if the maximum side is unbounded, return true
	if (this.maxValue == null)
	    return true;

	// if the object passed in is null, return false: suppose it is
	// the "positive infinite". So be care when use this method.
	if (value == null)
	    return false;

	if (isMaxIncluded) 
	    return maxValue.compareTo(value) >= 0;
	return maxValue.compareTo(value) > 0;
    }

    /** Return true if the specific value is larger than the minimum of
     * this range. If this range is unbounded at the minimum end, return true;
     * if the specific value is null, return false; 
     */
    private boolean isOverLowerBound( Comparable value ) {
	// if the minimum side is unbounded, return true
	if (this.minValue == null)
	    return true;

	// if the object passed in is null, return false: suppose it is
        // the "negative infinite". So be care when use this method.
	if (value == null)
	    return false;

	if (isMinIncluded)
	    return minValue.compareTo(value) <= 0;
	else return minValue.compareTo(value) < 0;
    }

    /**
     * Returns true if the supplied <code>Range</code> is fully contained 
     * within this <code>Range</code>. Fully contained is defined as having
     * the minimum and maximum values of the fully contained range lie 
     * within the range of values of the containing <code>Range</code>.
     *
     * @throws IllegalArgumentException if the <code>Class</code> of the
     * elements of the given <code>Range</code> is not the same as the 
     * <code>Class</code> of the elements of this <code>Range</code>.
     * @throws IllegalArgumentException if the given <code>Range</code> is null
     */
    public boolean contains(Range range) {

        if (range == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Range5"));
	if (elementClass != range.getElementClass())
	    throw new IllegalArgumentException(JaiI18N.getString("Range4"));

	if (range.isEmpty())
	    return true;

	Comparable min = range.getMinValue();
	Comparable max = range.getMaxValue();
	boolean maxSide, minSide;

	if (max == null)
	    maxSide = (maxValue == null);
	else 
	    maxSide = isUnderUpperBound(max)
		      || (isMaxIncluded == range.isMaxIncluded() &&
			  max.equals(maxValue)); 
	
	if (min == null)
	    minSide = (minValue == null);
	else
	    minSide = isOverLowerBound(min)
		      || (isMinIncluded == range.isMinIncluded() &&
			  min.equals(minValue));
	return minSide && maxSide;
    }

    /**
     * Returns true if this <code>Range</code> intersects the 
     * given <code>Range</code>.
     *
     * @throws IllegalArgumentException if the <code>Class</code> of the
     * elements of the given <code>Range</code> is not the same as the 
     * <code>Class</code> of the elements of this <code>Range</code>.
     * @throws IllegalArgumentException if the given <code>Range</code> is null
     */
    public boolean intersects(Range range) {
        if (range == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Range5"));
        if (elementClass != range.getElementClass())
            throw new IllegalArgumentException(JaiI18N.getString("Range4"));

	return !intersect( range ).isEmpty();
    }

    /**
     * Returns the union of this <code>Range</code> with the given 
     * <code>Range</code>. If this <code>Range</code> and the given
     * <code>Range</code> are disjoint, the <code>Range</code> returned
     * as a result of the union will have a minimum value set to the
     * minimum of the two disjoint range's minimum values, and the maximum
     * set to the maximum of the two disjoint range's maximum values, thus
     * including the disjoint range within it.
     *
     * @throws IllegalArgumentException if the <code>Class</code> of the
     * elements of the given <code>Range</code> is not the same as the 
     * <code>Class</code> of the elements of this <code>Range</code>.
     * @throws IllegalArgumentException if the given <code>Range</code> is null
     */
    public Range union(Range range) {

	if (range == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Range5"));
        if (elementClass != range.getElementClass())
            throw new IllegalArgumentException(JaiI18N.getString("Range4"));

	if (this.isEmpty()) 
	    return new Range(elementClass, range.getMinValue(),
		range.isMinIncluded(), range.getMaxValue(), 
		range.isMaxIncluded());

	if (range.isEmpty())
	    return new Range(elementClass, this.minValue, this.isMinIncluded,
		this.maxValue, this.isMaxIncluded);

	boolean containMin = !isOverLowerBound(range.getMinValue());
	boolean containMax = !isUnderUpperBound(range.getMaxValue());

	// If the minimum of this range is contained in the given range, the
	// minimum of the union is the minimum of the given range; otherwise
	// it is the minimum of this range. So does the boolean isMinIncluded.
	// Similar for the maximum end
	Comparable minValue = containMin ? range.getMinValue() : this.minValue;
	Comparable maxValue = containMax ? range.getMaxValue() : this.maxValue;
	boolean isMinIncluded = containMin 
				? range.isMinIncluded() : this.isMinIncluded;
        boolean isMaxIncluded = containMax 
                                ? range.isMaxIncluded() : this.isMaxIncluded;
	return new Range(elementClass, minValue, isMinIncluded, maxValue, 
			isMaxIncluded);
    }

    /**
     * Returns the intersection of this <code>Range</code> with the
     * given <code>Range</code>.
     *
     * @throws IllegalArgumentException if the <code>Class</code> of the
     * elements of the given <code>Range</code> is not the same as the 
     * <code>Class</code> of the elements of this <code>Range</code>.
     * @throws IllegalArgumentException if the given <code>Range</code> is null
     */
    public Range intersect(Range range) {

	if (range == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Range5"));
        if (elementClass != range.getElementClass())
            throw new IllegalArgumentException(JaiI18N.getString("Range4"));
	
	if (this.isEmpty()) {
            Comparable temp = this.minValue;
            if (temp == null) 
                temp = this.maxValue;

            // get a non-null object to create an empty Range
            // because the range is empty, so temp will not be
            // null 
            return new Range(elementClass, temp, false, temp, false);
	}

	if (range.isEmpty()) {
	    Comparable temp = range.getMinValue();
	    if (temp == null)
		temp = range.getMaxValue();

	    // get a non-null object to create an empty Range
	    // because the range is empty, so temp will not be
	    // null 
	    return new Range(elementClass, temp, false, temp, false);
	}

        boolean containMin = !isOverLowerBound(range.getMinValue());
        boolean containMax = !isUnderUpperBound(range.getMaxValue());

	// If the minimum of this range is contained in the given range, the
	// minimum of the intersect range should be the one of this range;
	// similarly, we can get the maximum and the booleans. 
        Comparable minValue = containMin ? this.minValue : range.getMinValue();
        Comparable maxValue = containMax ? this.maxValue : range.getMaxValue();
        boolean isMinIncluded = containMin 
                                ? this.isMinIncluded : range.isMinIncluded();
        boolean isMaxIncluded = containMax 
                                ? this.isMaxIncluded : range.isMaxIncluded();
        return new Range(elementClass, minValue,
			 isMinIncluded, maxValue, isMaxIncluded);
    }

    /**
     * Returns the <code>Range</code> of values that are in this
     * <code>Range</code> but not in the given <code>Range</code>. If 
     * the subtraction results in two disjoint <code>Range</code>s, they
     * will be returned as two elements of a <code>Range</code> array, 
     * otherwise the resultant <code>Range</code> will be returned as the
     * first element of a one element array.
     *
     * When this <code>Range</code> and the given <code>Range</code> are
     * both unbounded at both the ends (i.e both the <code>Range</code>s
     * are all inclusive), this method will return null as the first
     * element of one element array, as a result of the subtraction.
     *
     * When this <code>Range</code> is completely contained in the
     * given <code>Range</code>, an empty <code>Range</code> is returned.
     *
     * @throws IllegalArgumentException if the <code>Class</code> of the
     * elements of the given <code>Range</code> is not the same as the 
     * <code>Class</code> of the elements of this <code>Range</code>.
     */
    public Range[] subtract(Range range) {

	if (range == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Range5"));
        if (elementClass != range.getElementClass())
            throw new IllegalArgumentException(JaiI18N.getString("Range4"));

	// if this range is empty, return an empty range by copying this range;
	// if the given range is empty, return this range
	if (this.isEmpty() || range.isEmpty()) {
	    Range[] ra = {new Range(elementClass, this.minValue, 
		this.isMinIncluded, this.maxValue, this.isMaxIncluded)};
	    return ra; 
	}

	Comparable min = range.getMinValue();
        Comparable max = range.getMaxValue();
	boolean minIn = range.isMinIncluded();
	boolean maxIn = range.isMaxIncluded();

	if (this.minValue == null && this.maxValue == null 
	    && min == null && max == null) {
	    Range[] ra = {null};
	    return ra;
	}

        boolean containMin = this.contains(min);
        boolean containMax = this.contains(max);

	// this range may be a full range [null, null]
	if (containMin && containMax) {
	    Range r1 = new Range(elementClass, this.minValue, 
				 this.isMinIncluded, min, !minIn);
	    Range r2 = new Range(elementClass, max, !maxIn, 
				 this.maxValue, this.isMaxIncluded);

	    // if r1 is empty , return the second section only;
	    // or if this range and the given range are all unbounded
	    // at the minimum end, r1 is [null, null] but 
	    // should be empty. so we need to treat it as a special case; 
	    // otherwise, a full range is returned
	    if (r1.isEmpty() || (this.minValue == null && min == null)){
		Range[] ra = {r2};
		return ra;
	    }
	    // similar to above
	    if (r2.isEmpty() || (this.maxValue == null && max == null)) {
	        Range[] ra = {r1};
		return ra;
	    }
	    Range[] ra = {r1, r2};
	    return ra; 
	}
	// if the max of the given range is in this range, return the range
	// from max of given range to the max of this range
	else if (containMax) {
	    Range[] ra = {new Range(elementClass, max, !maxIn, this.maxValue,
				    this.isMaxIncluded)};
	    return ra;
	}
	// if the min of the given range is in this range, return the range
	// from the min of this range to the min of the given range
	else if (containMin){
	    Range[] ra = {new Range(elementClass, this.minValue,
				    this.isMinIncluded, min, !minIn)};
	    return ra; 
	}

	// no overlap, just copy this range
	if ((min != null && !isUnderUpperBound(min)) 
	    || (max != null && !isOverLowerBound(max))) {
	    Range[] ra = {new Range(elementClass, this.minValue,
			  this.isMinIncluded, this.maxValue, this.isMaxIncluded)};
	    return ra;
	}

	// this range is contained in the given range, return an empty range
	min = (this.minValue == null) ? this.maxValue : this.minValue;
	Range[] ra = {new Range(elementClass, min, false, min, false)};
	return ra;
    }

    /**
     * Compute a hash code value for this <code>Range</code> object. 
     *
     * @return  a hash code value for this <code>Range</code> object. 
     */
 
    public int hashCode() {

	int code = this.elementClass.hashCode();

	if (isEmpty())
	    return code;

	code ^= Integer.MAX_VALUE;

	if (this.minValue != null) {
	    code ^= this.minValue.hashCode();
	    if (this.isMinIncluded)
		code ^= 0xFFFF0000;
	}

	if (this.maxValue != null) {
	    code ^= this.maxValue.hashCode() * 31;
	    if (this.isMaxIncluded)
		code ^= 0xFFFF;
	}
	
	return code;
    }

    /**
     * Returns true if this <code>Range</code> and the given 
     * <code>Range</code> both have elements of the same <code>Class</code>,
     * their minimum and maximum values are the same, and their isMinIncluded()
     * and isMaxIncluded() methods return the same values.
     *
     * If this <code>Range</code> and the given <code>Range</code> are both
     * empty and the <code>Class</code> of their elements is the same, they
     * will be found to be equal and true will be returned.
     */
    public boolean equals(Object other) {

	// this range is not null, so if the given object is null, 
	// return false
	if (other == null)
	    return false;

	// if the given object is not a range, return false
	if (!(other instanceof Range))
	    return false;
	
	Range r = (Range)other ;

	// if the element class is not same, return false
	if (this.elementClass != r.getElementClass())
	    return false;

	// two empty ranges are equal
        if (this.isEmpty() && r.isEmpty())
            return true;

	Comparable min = r.getMinValue() ;
	// if the minimum is not null, compare both minValue and the boolean
	if (this.minValue != null) {
	    if (!this.minValue.equals(min))
		return false;

	    if (this.isMinIncluded != r.isMinIncluded())
	        return false;
	}
	// if the minimum is unbounded, just check the given range is bounded
	// or not
	else if (min != null)
	    return false;

	Comparable max = r.getMaxValue() ;
	// if the maximum is not null, compare both maxValue and the boolean
	if (this.maxValue != null) {
	    if (!this.maxValue.equals(max))
		return false;

	    if (this.isMaxIncluded != r.isMaxIncluded())
		return false;
	}
        // if the maximum is unbounded, just check the given range is bounded
        // or not
	else if (max != null)
	    return false;

	return true;
    }

    /**
     * Returns true if this <code>Range</code> is empty, i.e. if the minimum
     * value is greater than the maximum value, if both are included, or if
     * the minimum value is greater than equal to the maximum value if either
     * the minimum or maximum value is excluded.
     */
    public boolean isEmpty() {

	// an unbounded range is not empty
	if (minValue == null || maxValue == null)
	    return false;

	int cmp = this.minValue.compareTo(this.maxValue);

	// if the minimum is larger than the maximum, this range is empty
	if (cmp > 0)
	    return true;

	// if the minimum equals to the maximum and one side is not 
	// inclusive, then it is empty
	if (cmp == 0)
	    return !(isMinIncluded & isMaxIncluded) ;

	// otherwise, not empty
	return false ;
    }

    /**
     * Returns a <code>String</code> representation of this <code>Range</code>.
     */
    public String toString() {

	// if inclusive, display '[' otherwise display '('
	char c1 = isMinIncluded ? '[' : '(' ;
	char c2 = isMaxIncluded ? ']' : ')' ;

	// if both ends are bounded
	if (minValue != null && maxValue != null)
	    return new String(c1 + this.minValue.toString() + ", " +
			      this.maxValue.toString() + c2);

	// if the maximum end is bounded
	if (maxValue != null)
	    return new String(c1 + "---, " + this.maxValue.toString() + c2) ;

	// if the minimum end is bounded
	if (minValue != null)
	    return new String(c1+ this.minValue.toString() + ", " + "---" + c2);

	// both ends are unbounded
	return new String(c1 + "---, ---" + c2);
    }
}
