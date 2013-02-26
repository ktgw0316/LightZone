/*
 * $RCSfile: PropertySourceChangeEvent.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:17 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

/**
 * A class instances of which represent JAI properties as emitted for
 * example by a <code>PropertySource</code> but in the guise of an event
 * as defined for Java Beans.  This class definition adds no functionality
 * to that provided by the superclass per se.  The significance of the
 * derivation is that instances of this event by definition refer to properties
 * in the JAI sense of the term. Otherwise put, this class provides an extra
 * level of indirection.
 *
 * @see PropertyChangeEventJAI
 * @see PropertySource
 *
 * @since JAI 1.1
 */
public class PropertySourceChangeEvent extends PropertyChangeEventJAI {
    /**
     * Constructs a <code>PropertySourceChangeEvent</code>.
     * <code>propertyName</code> is forced to lower case; all other
     * parameters are passed unmodified to the superclass constructor.
     * If <code>oldValue</code> or <code>newValue</code> is to indicate
     * a property for which no value is defined, then the object
     * <code>java.awt.Image.UndefinedProperty</code> should be passed.
     *
     * @exception NullPointerException if <code>propertyName</code> is
     *            <code>null</code>.
     * @exception IllegalArgumentException if <code>source</code>,
     *            <code>oldValue</code> or <code>newValue</code> is
     *            <code>null</code>.
     */
    public PropertySourceChangeEvent(Object source,
				     String propertyName,
				     Object oldValue,
				     Object newValue) {
        super(source, propertyName, oldValue, newValue);

        // Note: source and propertyName are checked for null in superclass.

        if(oldValue == null) {
            throw new IllegalArgumentException(JaiI18N.getString("PropertySourceChangeEvent0"));
        } else if(newValue == null) {
            throw new IllegalArgumentException(JaiI18N.getString("PropertySourceChangeEvent1"));
        }
    }
}
