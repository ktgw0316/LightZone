/*
 * $RCSfile: CaselessStringKey.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2006/06/16 22:52:04 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.util;

import java.io.Serializable;
import java.util.Locale;

/**
 * Class to use as the key in a <code>java.util.Map</code>.
 * The case of the name is maintained but the <code>equals()</code>
 * method performs case-insensitive comparison.
 *
 * @see com.lightcrafts.mediax.jai.PropertySourceImpl
 * @see java.util.Map
 *
 * @since JAI 1.1
 */
public final class CaselessStringKey implements Cloneable, Serializable {

    private String name;
    private String lowerCaseName;

    /**
     * Creates a <code>CaselessStringKey</code> for the given name.
     * The parameter <code>name</code> is stored by reference.
     *
     * @throws IllegalArgumentException if <code>name</code> is
     *                                  <code>null</code>.
     */
    public CaselessStringKey(String name) {
        setName(name);
    }

    /**
     * Returns a hash code value for the <code>CaselessStringKey</code>.
     */
    public int hashCode() {
        return lowerCaseName.hashCode();
    }

    /**
     * Returns the internal name by reference.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a lower case version of the internal name.
     */
    private String getLowerCaseName() {
        return lowerCaseName;
    }

    /**
     * Stores the parameter by reference in the internal name.
     *
     * @throws IllegalArgumentException if <code>name</code> is
     *                                  <code>null</code>.
     */
    public void setName(String name) {
        if(name == null) {
            throw new IllegalArgumentException(JaiI18N.getString("CaselessStringKey0"));
        }
        this.name = name;
        lowerCaseName = name.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Returns a clone of the <code>CaselessStringKey</code> as an
     * <code>Object</code>.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since object is Cloneable
            throw new InternalError();
        }
    }

    /**
     * Whether another <code>Object</code> equals this one.  This will obtain
     * if and only if the parameter is non-<code>null</code> and is a
     * <code>CaselessStringKey</code> whose lower case name equals the
     * lower case name of this <code>CaselessStringKey</code>.
     */
    public boolean equals(Object o) {
        if(o != null && o instanceof CaselessStringKey) {
            return lowerCaseName.equals(((CaselessStringKey)o).getLowerCaseName());
        }
        return false;
    }

    /**
     * Returns the value returned by <code>getName()</code>.
     */
    public String toString() {
        return getName();
    }
}
