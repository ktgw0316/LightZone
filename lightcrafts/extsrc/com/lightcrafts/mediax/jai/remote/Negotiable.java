/*
 * $RCSfile: Negotiable.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:50 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.io.Serializable;

/**
 * An interface that defines objects that can be negotiated upon. 
 * Negotiation amongst objects is performed using the 
 * <code>negotiate()</code> method. This method can be used to 
 * perform a chaining of successful negotiations, i.e., the results 
 * of one successful negotiation can be used to negotiate with another
 * <code>Negotiable</code> and so on. In order to retrieve a single
 * negotiated value out of the <code>Negotiable</code>, the
 * <code>getNegotiatedValue()</code> method can be used at any point
 * during this series of negotiations.
 *
 * @since JAI 1.1
 */
public interface Negotiable extends Serializable {

    /**
     * Returns a <code>Negotiable</code> object that represents the
     * set intersection of this <code>Negotiable</code> with the 
     * given <code>Negotiable</code>. The returned <code>Negotiable</code>
     * represents the support that is common to both the 
     * <code>Negotiable</code>s. If the negotiation fails, i.e there is 
     * no common subset, null is returned.
     *
     * <p> If the supplied argument is null, negotiation will fail and
     * null will be returned, as it is not possible to negotiate with a
     * null value. It may, however, be noted that it is valid for
     * <code>getNegotiatedValue()</code> to return null, i.e the single
     * value result of the negotiation can be null.
     *
     * @param other The <code>Negotiable</code> object to negotiate with.
     * @returns     The <code>Negotiable</code> that represents the subset
     *              common to this and the given <code>Negotiable</code>. 
     *              <code>null</code> is returned if there is no common subset.
     */
    Negotiable negotiate(Negotiable other);

    /**
     * Returns a value that is valid for this <code>Negotiable</code>.
     * If more than one value is valid for this <code>Negotiable</code>,
     * this method can choose one arbitrarily, e.g. picking the first one
     * or by having static preferences amongst the valid values. Which of the
     * many valid values is returned is upto the particular implementation
     * of this method.
     *
     * @returns     The negotiated value.
     */
    Object getNegotiatedValue();

    /**
     * Returns the <code>Class</code> of the object that would be returned
     * from the <code>getNegotiatedValue</code> method on a successful
     * negotiation. This method can be used to learn what the
     * <code>Class</code> of the negotiated value will be if the negotiation
     * is successful. Implementing classes are encouraged to return an
     * accurate <code>Class</code> from this method if at all possible.
     * However it is permissible to return null, if the <code>Class</code>
     * of the negotiated value is indeterminate for any reason.
     *
     * @returns the <code>Class</code> of the negotiated value.
     */
    Class getNegotiatedValueClass();
}
