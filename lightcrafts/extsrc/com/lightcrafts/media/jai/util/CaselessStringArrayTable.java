/*
 * $RCSfile: CaselessStringArrayTable.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:59 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.util;

import java.util.Hashtable;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * A class that maps an array of <code>String</code>s or 
 * <code>CaselessStringKey</code>s into the array indices and
 * vice versa (all in a caseless fashion).
 *
 * This is used to map source names and parameter names to their
 * indices in a case insensitive fashion.
 */
public class CaselessStringArrayTable implements java.io.Serializable {

    private CaselessStringKey[] keys;
    private Hashtable indices;

    /**
     * Constructor for an array of <code>CaselessStringKey</code>s.
     */
    public CaselessStringArrayTable() {
	this((CaselessStringKey[])null);
    }

    /**
     * Constructor for an array of <code>CaselessStringKey</code>s.
     */
    public CaselessStringArrayTable(CaselessStringKey[] keys) {

	this.keys = keys;
	this.indices = new Hashtable();

	if (keys != null)
	    for (int i = 0; i < keys.length; i++) {
		this.indices.put(keys[i], new Integer(i));
	    }
    }

    /**
     * Constructor for an array of <code>String</code>s.
     */
    public CaselessStringArrayTable(String[] keys) {
	this(toCaselessStringKey(keys));
    }

    /**
     * Map an array of <code>String</code>s to <code>CaselessStringKey</code>s.
     */
    private static CaselessStringKey[] toCaselessStringKey(String strings[]) {
	if (strings == null)
	    return null;

	CaselessStringKey[] keys = new CaselessStringKey[strings.length];

	for (int i = 0; i < strings.length; i++)
	    keys[i] = new CaselessStringKey(strings[i]);
				
	return keys;
    }

    /**
     * Get the index of the specified key.
     *
     * @throws IllegalArgumentException if the key is <code>null or
     *	if the key is not found.
     */
    public int indexOf(CaselessStringKey key) {
	if (key == null) {
	    throw new IllegalArgumentException(
		      JaiI18N.getString("CaselessStringArrayTable0"));
	}

	Integer i = (Integer)indices.get(key);

	if (i == null) {
	    throw new IllegalArgumentException(key.getName() + " - " +
		      JaiI18N.getString("CaselessStringArrayTable1"));
	}

	return i.intValue();
    }

    /**
     * Get the index of the specified key.
     *
     * @throws IllegalArgumentException if the key is <code>null or
     *	if the key is not found.
     */
    public int indexOf(String key) {
	return indexOf(new CaselessStringKey(key));
    }

    /**
     * Get the <code>String</code> corresponding to the index <code>i</code>.
     *
     * @throws ArrayIndexOutOfBoundsException if <code>i</code> is out of range.
     */
    public String getName(int i) {
	if (keys == null)
	    throw new ArrayIndexOutOfBoundsException();

	return keys[i].getName();
    }

    /**
     * Get the <code>CaselessStringKey</code> corresponding to the
     * index <code>i</code>.
     *
     * @throws ArrayIndexOutOfBoundsException if <code>i</code> is out of range.
     */
    public CaselessStringKey get(int i) {
	if (keys == null)
	    throw new ArrayIndexOutOfBoundsException();

	return keys[i];
    }

    /**
     * Tests if this table contains the specified key.
     *
     * @return true if the key is present. false otherwise.
     */
    public boolean contains(CaselessStringKey key) {
	if (key == null) {
	    throw new IllegalArgumentException(
		      JaiI18N.getString("CaselessStringArrayTable0"));
	}

	return indices.get(key) != null;
    }

    /**
     * Tests if this table contains the specified key.
     *
     * @return true if the key is present. false otherwise.
     */
    public boolean contains(String key) {
	return contains(new CaselessStringKey(key));
    }
}
