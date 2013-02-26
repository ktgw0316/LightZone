/*
 * $RCSfile: NegotiableCapabilitySet.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:51 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * This class is an aggregation of <code>NegotiableCapability</code> objects.
 * This class can be used to represent all the capabilities of a machine.
 * 
 * <p> This class can be classified as either a preference or a non-preference.
 * For an explanation of the concept of preference, refer to the class
 * comments on <code>com.lightcrafts.mediax.jai.remote.NegotiableCapability</code>.
 *
 * <p> If multiple <code>NegotiableCapability</code> objects with the
 * same category and capability name are added to this class, the
 * <code>NegotiableCapability</code> added earliest has the highest
 * preference.
 *
 * <p>All names are treated in a case-retentive and case-insensitive manner.
 *
 * @since JAI 1.1
 */
public class NegotiableCapabilitySet implements Serializable {
    
    // Implementation specific data structures. Each entry into this 
    // Hashtable is a SequentialMap object hashed by the category of the
    // NegotiableCapability. The SequentialMap stores NegotiableCapability
    // objects for the same category but different capabilityNames in
    // separate bins. Within each bin, the NegotiableCapability that was
    // added first will always be the first one to be accessed.
    private Hashtable categories = new Hashtable();

    // Whether this NegotiableCapabilitySet is a preference or not.
    private boolean isPreference = false;

    /**
     * Creates a <code>NegotiableCapabilitySet</code>. The 
     * <code>isPreference</code> argument specifies whether this 
     * <code>NegotiableCapabilitySet</code> should be treated as a preference
     * or non-preference. If this <code>NegotiableCapabilitySet</code> is
     * specified to be a non-preference, only non-preference 
     * <code>NegotiableCapability</code>'s will be allowed to be added to it,
     * otherwise an <code>IllegalArgumentException</code> will be thrown
     * at the time of addition. Similarly only preference 
     * <code>NegotiableCapability</code> objects can be added to this 
     * <code>NegotiableCapabilitySet</code> if <code>isPreference</code>
     * is true.
     *
     * @param isPreference a boolean that specifies whether the component
     *        <code>NegotiableCapability</code>'s are preferences.
     */
    public NegotiableCapabilitySet(boolean isPreference) {
	this.isPreference = isPreference;
    }

    /**
     * Returns true if this <code>NegotiableCapabilitySet</code> is an
     * aggregation of preference <code>NegotiableCapability</code>'s,
     * false otherwise.
     */
    public boolean isPreference() {
	return isPreference;
    }

    /**
     * Adds a new <code>NegotiableCapability</code> to this 
     * <code>NegotiableCapabilitySet</code>. If a 
     * <code>NegotiableCapability</code> with the same category and same 
     * capability name as the one currently being added has been added
     * previously, the previous one will have a higher preference.
     *
     * @param capability The <code>NegotiableCapability</code> to be added.
     * @throws IllegalArgumentException if capability is null.
     * @throws IllegalArgumentException if <code>isPreference()</code>
     * returns false, and capability is a preference
     * <code>NegotiableCapability</code>.
     * @throws IllegalArgumentException if <code>isPreference()</code>
     * returns true, and capability is a non-preference
     * <code>NegotiableCapability</code>.
     */
    public void add(NegotiableCapability capability) {

	if (capability == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapabilitySet0"));
	}

	if (isPreference != capability.isPreference()) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapabilitySet1"));
	}

	SequentialMap map = getCategoryMap(capability.getCategory());
	map.put(capability);
    }

    /**
     * Removes the specified <code>NegotiableCapability</code> from this 
     * <code>NegotiableCapabilitySet</code>. If the specified 
     * <code>NegotiableCapability</code> was not added previously, an
     * <code>IllegalArgumentException</code> will be thrown.
     *
     * @param capability The <code>NegotiableCapability</code> to be removed.
     * @throws IllegalArgumentException if capability is null.
     * @throws IllegalArgumentException if the specified
     * <code>NegotiableCapabilitySet</code> was not added previously.
     */
    public void remove(NegotiableCapability capability) {
	
	if (capability == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapabilitySet0"));
	}

	SequentialMap map = getCategoryMap(capability.getCategory());
	map.remove(capability);
    }

    /**
     * Returns all the <code>NegotiableCapability</code>s with the given
     * category and capability name added previously, as a <code>List</code>.
     * If none were added, returns an empty <code>List</code>.
     *
     * @param category   The category of the <code>NegotiableCapability</code>.
     * @param capabilityName The name of the <code>NegotiableCapability</code>.
     *
     * @throws IllegalArgumentException if category is null.
     * @throws IllegalArgumentException if capabilityName is null.
     */
    public List get(String category, String capabilityName) {

	if (category == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapabilitySet3"));
	}

	if (capabilityName == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapabilitySet4"));
	}

	SequentialMap map = getCategoryMap(category);
	return map.getNCList(capabilityName);
    }

    /**
     * Returns all the <code>NegotiableCapability</code>s with the given
     * category as a <code>List</code>. Returns an empty <code>List</code>
     * if no such <code>NegotiableCapability</code>s were added.
     *
     * @param category The category of the <code>NegotiableCapability</code>s
     *                 to return.
     * @throws IllegalArgumentException if category is null.
     */
    public List get(String category) {

	if (category == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapabilitySet3"));
	}

	SequentialMap map = getCategoryMap(category);
	Vector capNames = map.getCapabilityNames();

	Vector curr, allNC = new Vector();
	Object obj;
	for (Iterator e = capNames.iterator(); e.hasNext(); ) {
	    // Get the next vector of NCs
	    curr = (Vector)map.getNCList((String)e.next());
	    for (Iterator i=curr.iterator(); i.hasNext(); ) {
		// Get the elements of the Vector
		obj = i.next();

		// If it isn't already present in the resultant Vector, add it
		if (!(allNC.contains(obj))) {
		    allNC.add(obj);
		}
	    }
	}
	
	return (List)allNC;
    }

    /**
     * Returns the category of all the <code>NegotiableCapability</code> 
     * objects that have been added previously, as a <code>List</code>.
     * Returns an empty <code>List</code>, if no 
     * <code>NegotiableCapability</code> objects have been added.
     * 
     * <p> The returned <code>List</code> does not contain any 
     * duplicates. 
     */
    public List getCategories() {

	CaselessStringKey key;
	Vector v = new Vector();
	for (Enumeration e = categories.keys(); e.hasMoreElements(); ) {
	    key = (CaselessStringKey)e.nextElement();
	    v.add(key.toString());
	}
	
	return (List)v;
    }

    /**
     * Returns the capability names of all the
     * <code>NegotiableCapability</code> objects that have been added
     * previously, as a <code>List</code>. Returns an empty
     * <code>List</code> if no such <code>NegotiableCapability</code>
     * objects have been added.
     * 
     * <p> The returned <code>List</code> does not contain any 
     * duplicates. 
     *
     * @param category   The category of the <code>NegotiableCapability</code>.
     * @throws IllegalArgumentException if category is null.
     */
    public List getCapabilityNames(String category) {

	if (category == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapabilitySet3"));
	}

	SequentialMap map = getCategoryMap(category);
	Vector names = map.getCapabilityNames();
	return (List)names;
    }

    /**
     * Returns the common subset supported by this 
     * <code>NegotiableCapabilitySet</code> and the given 
     * <code>NegotiableCapabilitySet</code>, if the negotiation succeeds.
     * This method returns null if negotiation fails for all categories.
     *
     * <p> For those categories that are common to both the 
     * <code>NegotiableCapabilitySet</code> objects, negotiation is
     * performed on a per category basis. Within each category, negotiation
     * is performed on a per capabilityName basis. The categories which exist
     * only in one or the other <code>NegotiableCapabilitySet</code> are
     * not negotiated upon and do not show up in the resultant 
     * <code>NegotiableCapabilitySet</code>, if the negotiation is successful.
     * If this class contains 'm' <code>NegotiableCapability</code> objects
     * for the same category and capabilityName for which the 
     * <code>NegotiableCapabilitySet</code> being negotiated with contains
     * 'n' <code>NegotiableCapability</code> objects, then the negotiation 
     * for this category and capabilityName will require m x n number of
     * negotiations between two <code>NegotiableCapability</code> objects.
     * The ones that succeed will produce new <code>NegotiableCapability</code>
     * objects which will be added to the returned 
     * <code>NegotiableCapabilitySet</code>.
     *
     * <p> If the supplied <code>NegotiableCapabilitySet</code> is null,
     * then the negotiation will fail, and null will be returned.
     *
     * @param other The <code>NegotiableCapabilitySet</code> to negotiate with.
     */
    public NegotiableCapabilitySet negotiate(NegotiableCapabilitySet other) {

	if (other == null)
	    return null;

	NegotiableCapabilitySet negotiated = 
	    new NegotiableCapabilitySet(isPreference & other.isPreference());

	// Get only the common categories
	Vector commonCategories = new Vector(getCategories());
	commonCategories.retainAll(other.getCategories());

	String capName, otherCapName;
	NegotiableCapability thisCap, otherCap, negCap;
	List thisCapabilities, otherCapabilities;

	for (Iterator c = commonCategories.iterator(); c.hasNext(); ) {
	    String currCategory = (String)c.next();
	    
	    thisCapabilities = get(currCategory);
	    otherCapabilities = other.get(currCategory);

	    for (Iterator t=thisCapabilities.iterator(); t.hasNext(); ) {

		thisCap = (NegotiableCapability)t.next();

		for (Iterator o=otherCapabilities.iterator(); o.hasNext(); ) {
		    
		    otherCap = (NegotiableCapability)o.next();
		    negCap = thisCap.negotiate(otherCap);
		    if (negCap != null)
			negotiated.add(negCap);
		}
	    }
	}

	if (negotiated.isEmpty()) {
	    return null;
	}

	return negotiated;
    }
    
    /**
     * Returns the single <code>NegotiableCapability</code> that is the
     * negotiated result for the given category from the current class. 
     * Returns null if negotiation for this category failed. If the
     * negotiation is successful, then this method will return the most
     * prefered (the one that was added first i.e. the one that is first 
     * in the list) <code>NegotiableCapability</code> from the list of 
     * <code>NegotiableCapability</code> that are valid for this category. 
     *
     * @param category  The category to find the negotiated result for.
     * @throws IllegalArgumentException if category is null.
     */
    public NegotiableCapability getNegotiatedValue(String category) {

	if (category == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapabilitySet3"));
	}
	
	List thisCapabilities = get(category);
	if (thisCapabilities.isEmpty()) 
	    return null;
	else 
	    return (NegotiableCapability)(thisCapabilities.get(0));
    }

    /**
     * Performs negotiation with the given <code>NegotiableCapabilitySet</code>
     * for the specified category and returns the single
     * <code>NegotiableCapability</code> that is the negotiated result for
     * the given category, if the negotiation succeeds, null
     * otherwise.
     *
     * <p> Negotiation is only performed for the specified category. For
     * the specified category, if there are 'm' 
     * <code>NegotiableCapability</code> objects for which the 
     * <code>NegotiableCapabilitySet</code> being negotiated with contains
     * 'n' <code>NegotiableCapability</code> objects, then the negotiation 
     * for this category may require m x n number of negotiations at a 
     * maximum and one negotiation at a minimum as the negotiation process
     * stops as soon as a negotiation is successful. The results of this 
     * successful negotiation are then returned. If all the m x n
     * negotiations fail, null is returned.
     *
     * <p> If the supplied <code>NegotiableCapabilitySet</code> is null,
     * then the negotiation will fail and null will be returned.
     *
     * @param other The <code>NegotiableCapabilitySet</code> to negotiate with.
     * @param category The category to negotiate for.
     * @throws IllegalArgumentException if category is null.
     */
    public NegotiableCapability getNegotiatedValue(NegotiableCapabilitySet
						   other, String category) {
	if (other == null)
	    return null;

	if (category == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapabilitySet3"));
	}

	List thisCapabilities = get(category);
	List otherCapabilities = other.get(category);

	NegotiableCapability thisCap, otherCap, negCap;

	for (Iterator t=thisCapabilities.iterator(); t.hasNext(); ) {
	    
	    thisCap = (NegotiableCapability)t.next();
	    
	    for (Iterator o=otherCapabilities.iterator(); o.hasNext(); ) {
		
		otherCap = (NegotiableCapability)o.next();		
		negCap = thisCap.negotiate(otherCap);

		// The first negotiation to succeed is returned
		if (negCap != null)
		    return negCap;
	    }
	}

	return null;
    }

    /**
     * Returns true if no <code>NegotiableCapability</code> objects have been
     * added.
     */
    public boolean isEmpty() {
	return categories.isEmpty();
    }

    // Method to get the SequentialMap for a particular category, creating
    // one if necessary    
    private SequentialMap getCategoryMap(String category) {

	CaselessStringKey categoryKey = new CaselessStringKey(category);
	SequentialMap map = (SequentialMap)categories.get(categoryKey);

	if (map == null) {
	    map = new SequentialMap();
	    categories.put(categoryKey, map);
	}

	return map;
    }


    /**
     * A Class to manage storage of NegotiableCapability objects by category
     * and within that by capabilityName. This class also maintains the
     * order of NegotiableCapability in which they were added under a
     * particular category and capabilityName.
     */
    class SequentialMap implements Serializable {
	
	// Vector of CaselessStringKey objects, will be the capabilityNames.
	Vector keys;
	// Vector of vectors, each vector containing all the NCs for a 
	// particular capabilityName.
	Vector values;

	/**
	 * Creates a new SequentialMap.
	 */
	SequentialMap() {
	    keys = new Vector();
	    values = new Vector();
	}

	/**
	 * Add a capability to this SequentialMap.
	 */
	void put(NegotiableCapability capability) {
	    
	    CaselessStringKey capNameKey = 
		new CaselessStringKey(capability.getCapabilityName());

	    int index = keys.indexOf(capNameKey);

	    Vector v;
	    if (index == -1) {
		keys.add(capNameKey);
		v = new Vector();
		v.add(capability);
		values.add(v);
	    } else {
		v = (Vector)values.elementAt(index);
		if (v == null) 
		    v = new Vector();
		v.add(capability);
	    }
	}

	/**
	 * Let a List of all NegotiableCapability objects stored for the
	 * given capabilityName.
	 */
	List getNCList(String capabilityName) {

	    CaselessStringKey capNameKey = 
		new CaselessStringKey(capabilityName);

	    int index = keys.indexOf(capNameKey);
	    
	    Vector v;
	    if (index == -1) {
		v = new Vector();
		return (List)v;
	    } else {
		v = (Vector)values.elementAt(index);
		return (List)v;
	    }
	}

	/**
	 * Remove a NegotiableCapability from this SequentialMap.
	 */
	void remove(NegotiableCapability capability) {

	    CaselessStringKey capNameKey = 
		new CaselessStringKey(capability.getCapabilityName());

	    int index = keys.indexOf(capNameKey);
	    
	    if (index == -1) {
		throw new IllegalArgumentException(
			   JaiI18N.getString("NegotiableCapabilitySet2"));
	    }
	    
	    Vector v = (Vector)values.elementAt(index);
	    if (v.remove(capability) == false) {
		throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapabilitySet2"));
	    }
	    
	    // If this was the only element in the capabilityName Vector
	    if (v.isEmpty()) {
		keys.remove(capNameKey);
		values.remove(index);
	    }

	    if (keys.isEmpty())
		categories.remove(new CaselessStringKey(
						    capability.getCategory()));
	}

	/**
	 * Get capability names of all NegotiableCapabilitySets in this
	 * SequentialMap.
	 */
	Vector getCapabilityNames() {

	    Vector v = new Vector();
	    CaselessStringKey name;
	    for (Iterator i = keys.iterator(); i.hasNext(); ) {
		name = (CaselessStringKey)i.next();
		v.add(name.getName());
	    }

	    return v;
	}
    }
}

