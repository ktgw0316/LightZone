/*
 * $RCSfile: CaselessStringKeyHashtable.java,v $
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
import java.util.Map;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * A Hashtable where the keys are <code>CaselessStringKey</code>s.
 */
public class CaselessStringKeyHashtable extends Hashtable
       implements Cloneable, java.io.Serializable {

    /**
     * Constructs a new, empty CaselessStringKeyHashtable.
     */
    public CaselessStringKeyHashtable() {
	super();
    }

    /**
     * Constructs a new CaselessStringKeyHashtable with the same
     * mappings as the given Map.
     *
     * @param t the map whose mappings are to be placed in this map.
     */
    public CaselessStringKeyHashtable(Map t) {
	super(t);
    }

    /**
     * Returns a clone of the <code>CaselessStringKeyHashtable</code>
     * as an <code>Object</code>.
     */
    public Object clone() {
	return super.clone();
    }

    /**
     * Tests if the specified <code>String</code> is a key in this
     * hashtable. The key is wrapped by a <code>CaselessStringKey</code>
     * before being looked up.
     *
     * @param   key   possible key.
     *
     * @return  <code>true</code> if and only if the specified object 
     *          is a key in this hashtable, as determined by the 
     *          <tt>equals</tt> method; <code>false</code> otherwise.
     */
    public boolean containsKey(String key) {
	return super.containsKey(new CaselessStringKey(key));
    }

    /**
     * Tests if the specified <code>CaselessStringKey</code> is a key in
     * this hashtable.
     *
     * @param   key   possible key.
     *
     * @return  <code>true</code> if and only if the specified object 
     *          is a key in this hashtable, as determined by the 
     *          <tt>equals</tt> method; <code>false</code> otherwise.
     */
    public boolean containsKey(CaselessStringKey key) {
	return super.containsKey(key);
    }

    /**
     * Allow only <code>String</code> and <code>CaselessStringKey</code>
     * keys to be looked up. This always throws an IllegalArgumentException.
     *
     * @param   key   possible key.
     *
     * @return  always throws IllegalArgumentException.
     */
    public boolean containsKey(Object key) {
	throw new IllegalArgumentException();
    }

    /**
     * Returns the value to which the specified key is mapped in this
     * hashtable. The key is wrapped by a <code>CaselessStringKey</code>
     * before being looked up.
     *
     * @param   key   a key in the hashtable.
     *
     * @return  the value to which the key is mapped in this hashtable;
     *          <code>null</code> if the key is not mapped to any value in
     *          this hashtable.
     * @see     #put(String, Object)
     */
    public Object get(String key) {
	return super.get(new CaselessStringKey(key));
    }

    /**
     * Returns the value to which the specified key is mapped in this
     * hashtable.
     *
     * @param   key   a key in the hashtable.
     *
     * @return  the value to which the key is mapped in this hashtable;
     *          <code>null</code> if the key is not mapped to any value in
     *          this hashtable.
     * @see     #put(CaselessStringKey, Object)
     */
    public Object get(CaselessStringKey key) {
	return super.get(key);
    }

    /**
     * Allow only String and CaselessStringKey keys to be looked up.
     * This always throws an IllegalArgumentException.
     *
     * @param   key   possible key.
     *
     * @return  always throws IllegalArgumentException.
     * @see     #put(Object, Object)
     */
    public Object get(Object key) {
	throw new IllegalArgumentException();
    }

    /**
     * Maps the specified <code>key</code> to the specified
     * <code>value</code> in this hashtable. Neither the key nor
     * the value can be <code>null</code>. The key is wrapped by a
     * <code>CaselessStringKey</code> before mapping the key to the
     * value. <p>
     *
     * The value can be retrieved by calling the <code>get</code> method 
     * with a key that is equal to the original key. 
     *
     * @param      key     the hashtable key.
     * @param      value   the value.
     * @return     the previous value of the specified key in this hashtable,
     *             or <code>null</code> if it did not have one.
     * @exception  IllegalArgumentException  if the key or value is
     *               <code>null</code>.
     * @see     Object#equals(Object)
     * @see     #get(String)
     */
    public Object put(String key, Object value) {
        if ( key == null || value == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

	return super.put(new CaselessStringKey(key), value);
    }

    /**
     * Maps the specified <code>key</code> to the specified
     * <code>value</code> in this hashtable. Neither the key nor the
     * value can be <code>null</code>. <p>
     *
     * The value can be retrieved by calling the <code>get</code> method 
     * with a key that is equal to the original key. 
     *
     * @param      key     the hashtable key.
     * @param      value   the value.
     * @return     the previous value of the specified key in this hashtable,
     *             or <code>null</code> if it did not have one.
     * @exception  IllegalArgumentException  if the key or value is
     *               <code>null</code>.
     * @see     Object#equals(Object)
     * @see     #get(CaselessStringKey)
     */
    public Object put(CaselessStringKey key, Object value) {
        if ( key == null || value == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

	return super.put(key, value);
    }

    /**
     * Allow only String and CaselessStringKey keys to be mapped.
     * This always throws an IllegalArgumentException.
     *
     * @param   key   possible key.
     * @param      value   the value.
     *
     * @return  always throws IllegalArgumentException.
     * @see     #get(Object)
     */
    public Object put(Object key, Object value) {
	throw new IllegalArgumentException();
    }

    /**
     * Removes the key (and its corresponding value) from this
     * hashtable. This method does nothing if the key is not in the
     * hashtable. The key is wrapped by a <code>CaselessStringKey</code>
     * before trying to remove the entry.
     *
     * @param   key   the key that needs to be removed.
     * @return  the value to which the key had been mapped in this hashtable,
     *          or <code>null</code> if the key did not have a mapping.
     */
    public Object remove(String key) {
	return super.remove(new CaselessStringKey(key));
    }

    /**
     * Removes the key (and its corresponding value) from this
     * hashtable. This method does nothing if the key is not in the
     * hashtable.
     *
     * @param   key   the key that needs to be removed.
     * @return  the value to which the key had been mapped in this hashtable,
     *          or <code>null</code> if the key did not have a mapping.
     */
    public Object remove(CaselessStringKey key) {
	return super.remove(key);
    }

    /**
     * Allow only String and CaselessStringKey keys to be removed.
     * This always throws an IllegalArgumentException.
     *
     * @param   key   possible key.
     *
     * @return  always throws IllegalArgumentException.
     * @see     #get(Object)
     */
    public Object remove(Object key) {
	throw new IllegalArgumentException();
    }
}
