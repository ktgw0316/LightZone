/*
 * $RCSfile: PropertyChangeEmitter.java,v $
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

import java.beans.PropertyChangeListener;

/**
 * A class which emits <code>PropertyChangeEvent</code>s.
 * This abstraction permits objects of disparate types to be recognized
 * as sources of <code>PropertyChangeEvent</code>s.
 * <code>PropertyChangeEvent</code>s emitted by JAI objects will be
 * <code>PropertyChangeEventJAI</code> instances.
 *
 * <p> Note that the case of property names used in this context is
 * significant.
 *
 * @see PropertyChangeEventJAI
 *
 * @since JAI 1.1
 */
public interface PropertyChangeEmitter {

    /**
     * Add a PropertyChangeListener to the listener list. The
     * listener is registered for all properties.
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Add a PropertyChangeListener for a specific property. The
     * listener will be invoked only when a call on
     * firePropertyChange names that specific property.
     *
     * @throws IllegalArgumentException for null <code>propertyName</code>.
     */
    void addPropertyChangeListener(String propertyName,
                                   PropertyChangeListener listener);

    /**
     * Remove a PropertyChangeListener from the listener list. This
     * removes a PropertyChangeListener that was registered for all
     * properties.
     */
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @throws IllegalArgumentException for null <code>propertyName</code>.
     */
    void removePropertyChangeListener(String propertyName,
                                      PropertyChangeListener listener);
}
