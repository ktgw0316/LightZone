/*
 * $RCSfile: OperationGraph.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:13 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.util.Enumeration;
import java.util.Vector;

/**
 * OperationGraph manages a list of <code>PartialOrderNode</code>s
 * and pairwise preferences between them.
 *
 * The getOrderedOperationList method performs a topological sort.
 * The topological sort follows the algorithm described in Horowitz
 * and Sahni, <i>Fundamentals of Data Structures</i> (1976), p. 315.
 *
 * <p> Several minor changes are made to their implementation.  First,
 * nodes are represented as objects, not as integers.  The count
 * (in-degree) field is not used to link zero in-degree objects, but
 * instead a separate zeroLink field is used.  The neighbor lists are
 * stored as Vectors, not linked lists, and enumerations are used
 * to iterate over them.
 *
 * <p> This class is used by the implementation of the OperationRegistry
 * class and is not intended to be part of the API.
 *
 *	    - what was OperationGraph pre-JAI 1.1 is now FactoryOperationGraph
 *
 */
class OperationGraph implements java.io.Serializable {

    /** 
     * A Vector of <code>PartialOrderNode</code>s, each 
     * <code>PartialOrderNode</code> contains the name of the operation 
     * and the <code>OperationGraph</code> that contains the image 
     * factories implementing that operation. 
     */
    Vector operations = new Vector();

    /** A cached version of the ordered product list */
    Vector orderedOperations;

    /** Signifies whether the cached copy is out of date. */
    boolean isChanged = true;
	
    /**
      * If true, use a case-insensitive compare of the name of the
      * <code>PartialOrderNode</code> for lookups.
      */
    private boolean lookupByName = false;

    /**
     * Constructs an <code>OperationGraph</code>.
     * The default comparision for lookups is by object reference.
     */
    OperationGraph() {}

    /**
     * Specify the comparator used to compare the PartialOrderNode
     * with an object (used for lookupOp)
     *
     * @param lookupByName if true lookup does a case-insensitive
     *	      compare of the object being looked up with the
     *	      <code>PartialOrderNode</code> name. Needless to say,
     *	      this works only if the objects being looked
     *	      up are <code>String</code>s.
     */
    OperationGraph(boolean lookupByName) {
	this.lookupByName = lookupByName;
    }
	
    /**
     * The comparison used for lookups.
     */
    private boolean compare(PartialOrderNode poNode, Object op) {
	if (lookupByName)
	    return poNode.getName().equalsIgnoreCase((String)op);
	else
	    return poNode.getData() == op;
    }

    /**
     * Adds a PartialOrderNode to an <code>OperationGraph</code>.
     */
    void addOp(PartialOrderNode poNode) {
	
	operations.addElement(poNode);
	isChanged = true;	
    }
    
    /**
     * Removes the PartialOrderNode corresponding to the operation
     * from an <code>OperationGraph</code>.
     *
     * Does a "lookupOp" of the PartialOrderNode corresponding to "op"
     * and removes it.
     */
    synchronized boolean removeOp(Object op) {

	boolean retval = false;

	PartialOrderNode poNode = lookupOp(op);

	if (poNode != null) {
	    retval = operations.removeElement(poNode);

	    if (retval)
		isChanged = true;
	}

	return retval;
    }

    /**
     * Locates an operation from within the vector of PartialOrderNodes
     * using the object provided. 
     */
    PartialOrderNode lookupOp(Object op) {

	int num = operations.size();
		
	for (int i = 0; i < num; i++) {
	    PartialOrderNode poNode = (PartialOrderNode)operations.elementAt(i);

	    if (compare(poNode, op)) {
		PartialOrderNode tempNode = poNode;
		return tempNode;
	    }
	}
		
	return null;
    }

    /** Sets a preference between two operations. */
    synchronized boolean setPreference(Object preferred,
				    Object other) {
	boolean retval = false;

	if ((preferred == null) || (other == null)) {
	    throw new IllegalArgumentException(
			JaiI18N.getString("Generic0"));
	}

	if (preferred == other)
	    return retval;

	PartialOrderNode preferredPONode = lookupOp(preferred);
	PartialOrderNode otherPONode = lookupOp(other);
	
	if ((preferredPONode != null) && (otherPONode != null)) {
	    preferredPONode.addEdge(otherPONode);

	    retval = true;
	    isChanged = true;	
	}

	return retval;
    }

    /** Removes a preference between two operations. */
    synchronized boolean unsetPreference(Object preferred,
				      Object other) {
	boolean retval = false;

	if ((preferred == null) || (other == null)) {
	    throw new IllegalArgumentException(
			JaiI18N.getString("Generic0"));
	}

	if (preferred == other)
	    return retval;

	PartialOrderNode preferredPONode = lookupOp(preferred);
	PartialOrderNode otherPONode = lookupOp(other);

	if ((preferredPONode != null) && (otherPONode != null)) {
	    preferredPONode.removeEdge(otherPONode);

	    retval = true;
	    isChanged = true;
	}

	return retval;
    }

    /** 
     * Performs a topological sort on the set of 
     * <code>PartialOrderNodes</code>. 
     */
    public synchronized Vector getOrderedOperationList() {
		
	// If the cached copy is still current, return it
	if (isChanged == false) {

	    Vector ordered = orderedOperations;
	    return ordered;
	}

	int num = operations.size(); // The number of nodes in the digraph
	for (int i=0; i<num; i++) {
	    PartialOrderNode pon = (PartialOrderNode)operations.elementAt(i);
	    pon.setCopyInDegree(pon.getInDegree());
	}

	// Cache the ordered list in orderedOperations, and update the
	// isChanged variable to reflect that this is a newly computed list.
	orderedOperations = new Vector(num);
	isChanged = false;

	// A linked list of nodes with zero in-degree
	PartialOrderNode zeroList = null;
	PartialOrderNode poNode;
	int i;

	// Scan for elements with 0 in-degree
	for (i = 0; i < num; i++) {
	    poNode = (PartialOrderNode)operations.elementAt(i);
	    if (poNode.getCopyInDegree() == 0) {
		poNode.setZeroLink(zeroList);
		zeroList = poNode;
	    }
	}

	// Loop for each node
	for (i = 0; i < num; i++) {
	    // No free vertices, must be a cycle somewhere
	    if (zeroList == null) {
		orderedOperations = null;
		return null; // Cycle exists
	    }
			
	    // Set firstNode to a node from the free list
	    // and add it to the output.
	    PartialOrderNode firstNode = zeroList;

	    orderedOperations.addElement(firstNode);

	    // Bump the free list pointer
	    zeroList = zeroList.getZeroLink();
	    // For each neighbor of the output node, decrement its in-degree
	    Enumeration neighbors = firstNode.getNeighbors();
	    while (neighbors.hasMoreElements()) {
		poNode = (PartialOrderNode)neighbors.nextElement();
		poNode.decrementCopyInDegree();

		// If the in-degree has fallen to 0,
		// place the node on the free list.
		if (poNode.getCopyInDegree() == 0) {
		    poNode.setZeroLink(zeroList);
		    zeroList = poNode;
		}
	    }
	}
	
	Vector ordered = orderedOperations;
	return ordered;
    }
}
