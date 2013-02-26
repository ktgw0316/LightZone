/*
 * $RCSfile: SerializableState.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:53 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.remote;

import java.io.Serializable;

/**
 * An interface to be implemented by classes instances of which act as
 * serializable proxies for instances of non-serializable classes.
 *
 * @see java.io.Serializable
 *
 * @since JAI 1.1
 */
public interface SerializableState extends Serializable {

    /**
     * Retrieve the class of the object which would be returned by
     * invoking <code>getObject()</code>.
     *
     * @return The class of the object which would be returned by
     * <code>getObject()</code>.
     */
    Class getObjectClass();

    /**
     * Reconstitutes an object from a serializable version of its state
     * wrapped by an implementation of this interface.
     *
     * @return Deserialized form of the state.
     */
    Object getObject();
}
