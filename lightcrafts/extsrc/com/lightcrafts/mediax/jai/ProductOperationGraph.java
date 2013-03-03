/*
 * $RCSfile: ProductOperationGraph.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:16 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

/**
 * ProductOperationGraph manages a list of descriptors belonging to a
 * particular product. The descriptors have pairwise preferences between
 * them.
 *
 * This class extends "OperationGraph" which provide the
 * other operations such as remove, lookup, set/unset preference etc.
 *
 * <p> This class is used by the implementation of the OperationRegistry
 * class and is not intended to be part of the API.
 *
 * @see OperationGraph
 *
 *	    - Moved most of the functionality to "OperationGraph"
 *	      which has been generalized to maintain product as well
 *	      as factory tree
 */
final class ProductOperationGraph extends OperationGraph
				  implements java.io.Serializable {

    /** Constructs an <code>ProductOperationGraph</code>. */
    ProductOperationGraph() {
	// Use the name of the PartialOrderNode for comparisions
	super(true);
    }
	
    /**
     * Adds a product to an <code>ProductOperationGraph</code>.  A new 
     * <code>PartialOrderNode</code> is constructed to hold the product
     * and its graph adjacency information.
     */
    void addProduct(String productName) {
	addOp(new PartialOrderNode(new OperationGraph(), productName));
    }
}
