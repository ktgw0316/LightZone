/*
 * $RCSfile: NegotiableCollection.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:51 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * A class that wraps an <code>Collection</code> to implement the 
 * <code>Negotiable</code> interface. <code>NegotiableCollection</code>
 * is a convenience class to specify a <code>Negotiable</code> value for
 * a parameter whose valid values are contained in an <code>Collection</code>.
 *
 * @since JAI 1.1
 */
public class NegotiableCollection implements Negotiable {

    private Vector elements;
    private Class elementClass;
    
    /**
     * Creates a <code>NegotiableCollection</code> given an
     * <code>Collection</code>.
     *
     * @throws IllegalArgumentException if collection is null.
     * @throws IllegalArgumentException if all the elements of collection 
     * are not of the same <code>Class</code> type.
     */
    public NegotiableCollection(Collection collection) {

	if (collection == null) {
	    throw new IllegalArgumentException(
				 JaiI18N.getString("NegotiableCollection0"));
	}

	elements = new Vector();
	Object obj;

	Iterator i = collection.iterator();
	if (i.hasNext()) {
	    obj = i.next();
	    elements.add(obj);
	    elementClass = obj.getClass();
	} else {
	    // no elements, so elementClass will be initialized to null,
	    // which is correct. elements will also be null, which is 
	    // also correct.
	}

	for ( ;i.hasNext(); ) {
	    obj = i.next();
	    if (obj.getClass() != elementClass) {
		throw new IllegalArgumentException(
				 JaiI18N.getString("NegotiableCollection1"));
	    }
	    elements.add(obj);
	}
    }

    /**
     * Creates a <code>NegotiableCollection</code> given an array of 
     * <code>Object</code>s. The elements of the <code>Object</code>
     * array are treated as being the elements of an <code>Collection</code>.
     *
     * @throws IllegalArgumentException if objects is null.
     * @throws IllegalArgumentException if all the elements of objects are not
     * of the same <code>Class</code> type.
     */
    public NegotiableCollection(Object objects[]) {

	if (objects == null) {
	    throw new IllegalArgumentException(
				 JaiI18N.getString("NegotiableCollection0"));
	}

	int length = objects.length;
	if (length != 0) {
	    elementClass = objects[0].getClass();
	} else {
	    // no elements, so elementClass will be initialized to null,
	    // which is correct. elements will also be null, which is 
	    // also correct.
	}

	elements = new Vector(length);
	for (int i=0; i<length; i++) {
	    if (objects[i].getClass() != elementClass) {
		throw new IllegalArgumentException(
				 JaiI18N.getString("NegotiableCollection1"));
	    }
	    elements.add(objects[i]);
	}
    }

    /**
     * Returns the <code>Collection</code> of values which are currently
     * valid for this class, null if there are no valid values.
     */
    public Collection getCollection() {
	if (elements.isEmpty())
	    return null;
	return elements;
    }

    /**
     * Returns a <code>NegotiableCollection</code> that contains those
     * elements that are common to this <code>NegotiableCollection</code>
     * and the one supplied. If the supplied <code>Negotiable</code> is not
     * a <code>NegotiableCollection</code> with its elements being of the 
     * same <code>Class</code> as this class', or if there are no common
     * elements, the negotiation will fail and <code>null</code> (signifying
     * the failure of the negotiation) will be returned.
     *
     * @param other The <code>Negotiable</code> to negotiate with.
     */
    public Negotiable negotiate(Negotiable other) {

	if (other == null) {
	    return null;
	}

	// if other is not an instance of NegotiableCollection
	if (!(other instanceof NegotiableCollection) || 
	    other.getNegotiatedValueClass() != elementClass) {
	    return null;
	}
	
	Object obj;
	Vector result = new Vector();

	Collection otherCollection =
	    ((NegotiableCollection)other).getCollection();

	// If the collection is null, i.e there are no valid values, then 
	// negotiation fails.
	if (otherCollection == null) 
	    return null;
	
	// Return a NegotiableCollection whose elements are those that
	// were common to both the collections.
	for (Iterator i=elements.iterator(); i.hasNext(); ) {
	    obj = i.next();
	    // If element is present in both the collections
	    if (otherCollection.contains(obj)) {
		// Do not insert duplicates
		if (!result.contains(obj)) {
		    result.add(obj);
		}
	    }
	}

	// If there are no common elements, negotiation failed.
	if (result.isEmpty()) {
	    return null;
	}

	return new NegotiableCollection(result);
    }

    /**
     * Returns a single value that is valid for this 
     * <code>NegotiableCollection</code>. The returned value is the first
     * element contained in this <code>NegotiableCollection</code>. Returns
     * <code>null</code> if there are no valid elements in this
     * <code>NegotiableCollection</code>.
     */
    public Object getNegotiatedValue() {
	
	// Return the first element in this NegotiableCollection
	// else return 
	// <code>null</code> 
	if (elements != null && elements.size() > 0) {
	    return elements.elementAt(0);
	} else {
	    return null;
	}
    }

    /**
     * Returns the <code>Class</code> of the Object returned as the result
     * of the negotiation. If the <code>Collection</code> used to construct
     * this <code>NegotiableCollection</code> was empty, i.e. had no
     * elements, the <code>Class</code> of the elements is indeterminate, 
     * therefore null will be returned from this method in such a case.
     */
    public Class getNegotiatedValueClass() {
	return elementClass;
    }
}
