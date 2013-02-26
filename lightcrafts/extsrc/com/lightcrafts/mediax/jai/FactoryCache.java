/*
 * $RCSfile: FactoryCache.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:08 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
  * A class to manage the various instances of a descriptor factory.
  * This also manages preferences between factory instances for
  * a specified descriptor/product.
  *
  * @since JAI 1.1
  */
class FactoryCache {

    /** * The registry mode name. */
    final String modeName;

    /**
     * Cache the RegistryMode since it is bound to get used
     * many, many times.
     */
    final RegistryMode mode;

    /** The Class corresponding to the factory. */
    final Class factoryClass;

    /**
     * The name of the method in this factory used to
     * do a "create"
     */
    final Method factoryMethod;

    /**
     * does the factory support preferences both among products
     * and among multiple instances of the factory within the
     * same product ?
     */
    final boolean arePreferencesSupported;

    /**
     * A Hashtable of all the instances, hashed by a filename that
     * uniquely identifies each registered factory.
     */
    private Hashtable instances;

    /**
     * A Hashtable of all the unique factory filenames, hashed by
     * the factory they represent.
     */
    private Hashtable instancesByName;

    /** A count to give a number to each registered factory. */
    private int count = 0;

    /**
     * A Hashtable of a Hashtable of all the factory preferences, hashed
     * by the descriptor name first and then the product name that the
     * factory belongs to. Each element of the per product Hashtable is
     * a Vector which contains a list of pairwise factory preferences
     * stored as Vectors.
     */
    private Hashtable prefs;

    /**
     * Constructor. Create a FactoryCache to hold factory objects
     * for a specific mode.
     *
     * @param modeName the registry mode name.
     */
    FactoryCache(String modeName) {

	this.modeName = modeName;

	mode			= RegistryMode.getMode(modeName);
	factoryClass		= mode.getFactoryClass();
	factoryMethod		= mode.getFactoryMethod();
	arePreferencesSupported = mode.arePreferencesSupported();

	instances = new Hashtable();

	if (arePreferencesSupported) {
	    instancesByName = new Hashtable();
	    prefs = new Hashtable();
	}
    }

    /**
      * Invoke the create method of the given factory instance
      *
      * @param factoryInstance an instance of this factory
      * @param parameterValues the parameterValues to be passed in to the
      *			       the create method.
      *
      * @return the object created by the create method
      *
      * @throws IllegalArgumentException thrown by Method.invoke
      * @throws InvocationTargetException thrown by Method.invoke
      * @throws IllegalAccessException thrown by Method.invoke
      */
    Object invoke(Object factoryInstance, Object[] parameterValues)
				throws InvocationTargetException,
				       IllegalAccessException {

	return factoryMethod.invoke(factoryInstance, parameterValues);
    }

    /**
      * Add a factory instance to this factory. If the factory has
      * NO preferences add it to a table hashed by just the operation name.
      * Otherwise add it to two tables, one hashed by a unique filename
      * (modeName + count) and the other hashed by the factory interface
      * name.
      *
      * @param descriptorName operation that this factory instance implements
      * @param productName   product to which this factory instance belongs
      * @param factoryInstance the factory instance
      */
    void addFactory(String descriptorName, String productName,
		    Object factoryInstance) {

	checkInstance(factoryInstance);

	if (arePreferencesSupported) {

	    if (productName == null)
		throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	    // Update structures to reflect the addition of
	    // this factory instance.
	    Vector v = new Vector();

	    v.add(factoryInstance.getClass().getName());
	    v.add(productName);
	    v.add(descriptorName);

	    CaselessStringKey fileName =
		    new CaselessStringKey(modeName + count);

	    instancesByName.put(factoryInstance, fileName);
	    instances.put(fileName, v);
	    count++;

	} else
	    instances.put(
		new CaselessStringKey(descriptorName), factoryInstance);
    }

    /**
      * Remove a facory instance associated with the specified operation
      *
      * @param descriptorName operation that this factory instance implements
      * @param productName product to which this factory instance belongs
      * @param factoryInstance the factory instance
      *
      */
    void removeFactory(String descriptorName, String productName,
		       Object factoryInstance) {

	checkInstance(factoryInstance);
	checkRegistered(descriptorName, productName, factoryInstance);

	if (arePreferencesSupported) {

	    // Update structures to reflect the removal of
	    // this factoryInstance.
	    CaselessStringKey fileName =
		(CaselessStringKey)instancesByName.get(factoryInstance);

	    instancesByName.remove(factoryInstance);
	    instances.remove(fileName);
	    count--;
	} else {
	    instances.remove(new CaselessStringKey(descriptorName));
	}
    }

    /**
      * Sets a preference between two factory instances for the given
      * operation and product.
      *
      * @param descriptorName operation that this factory instance implements
      * @param productName   product to which this factory instance belongs
      * @param preferredOp the preferred factory instance
      * @param otherOp     the not-so preferred/other factory instance
      */
    void setPreference(String descriptorName,
		       String productName,
		       Object preferredOp,
		       Object otherOp) {

	if (!arePreferencesSupported) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("FactoryCache1",
		    new Object[] {modeName}));
	}

	if ((preferredOp == null) || (otherOp == null)) {
	    throw new IllegalArgumentException(
			JaiI18N.getString("Generic0"));
	}

	checkRegistered(descriptorName, productName, preferredOp);
	checkRegistered(descriptorName, productName, otherOp);

	if (preferredOp == otherOp)
	    return;

	checkInstance(preferredOp);
	checkInstance(otherOp);

	CaselessStringKey dn = new CaselessStringKey(descriptorName);
	CaselessStringKey pn = new CaselessStringKey(productName);

	Hashtable dht = (Hashtable)prefs.get(dn);

	if (dht == null) {
	    prefs.put(dn, dht = new Hashtable());
	}

	Vector pv = (Vector)dht.get(pn);

	if (pv == null) {
	    dht.put(pn, pv = new Vector());
	}

	pv.addElement(new Object[] { preferredOp, otherOp });
    }

    /**
      * Unets a preference between two factory instances for the given
      * operation and product.
      *
      * @param descriptorName operation that this factory instance implements
      * @param productName   product to which this factory instance belongs
      * @param preferredOp the preferred factory instance
      * @param otherOp     the not-so preferred/other factory instance
      */
    void unsetPreference(String descriptorName,
		         String productName,
		         Object preferredOp,
		         Object otherOp) {

	if (!arePreferencesSupported) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("FactoryCache1",
		    new Object[] {modeName}));
	}

	if ((preferredOp == null) || (otherOp == null)) {
	    throw new IllegalArgumentException(
			JaiI18N.getString("Generic0"));
	}

	checkRegistered(descriptorName, productName, preferredOp);
	checkRegistered(descriptorName, productName, otherOp);

	if (preferredOp == otherOp)
	    return;

	checkInstance(preferredOp);
	checkInstance(otherOp);

	// Update structures to reflect removal of this pref.
	Hashtable dht = (Hashtable)prefs.get(
			new CaselessStringKey(descriptorName));

	boolean found = false;

	if (dht != null) {

	    Vector pv = (Vector)dht.get(
			new CaselessStringKey(productName));

	    if (pv != null) {
		Iterator it = pv.iterator();

		while(it.hasNext()) {
		    Object[] objs = (Object[])it.next();

		    if ((objs[0] == preferredOp) &&
			(objs[1] == otherOp)) {

			it.remove();
			found = true;
		    }
		}

	    }
	}

	if (!found)
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("FactoryCache2",
		    new Object[] {
			preferredOp.getClass().getName(),
			otherOp.getClass().getName(),
			modeName, descriptorName, productName }));
    }

    /**
      * Gets an iterator over all preferences set between factory
      * instances for a given descriptor and product.
      *
      * @param descriptorName operation that this factory instance implements
      * @param productName   product to which this factory instance belongs
      */
    Object[][] getPreferences(String descriptorName,
			    String productName) {

	if (!arePreferencesSupported) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("FactoryCache1",
		    new Object[] {modeName}));
	}

	if ((descriptorName == null) || (productName == null))
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	// Update structures to reflect removal of this pref.
	Hashtable dht = (Hashtable)prefs.get(
			new CaselessStringKey(descriptorName));

	if (dht != null) {

	    Vector pv = (Vector)dht.get(
			new CaselessStringKey(productName));

	    if (pv != null) {
		return (Object[][])pv.toArray(new Object[0][]);
	    }
	}

	return null;
    }

    /**
      * Removes all preferences set between factory
      * instances for a given descriptor and product.
      *
      * @param descriptorName operation that this factory instance implements
      * @param productName   product to which this factory instance belongs
      */
    void clearPreferences(String descriptorName,
			  String productName) {

	if (!arePreferencesSupported) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("FactoryCache1",
		    new Object[] {modeName}));
	}

	// Update structures to reflect removal of this pref.
	Hashtable dht = (Hashtable)prefs.get(
			new CaselessStringKey(descriptorName));

	if (dht != null)
	    dht.remove(new CaselessStringKey(productName));
    }

    /**
     * Get a list of factory objects registered under the descriptor
     * and the product (in no particular order).
     */
    List getFactoryList(String descriptorName, String productName) {

	if (arePreferencesSupported) {

	    ArrayList list = new ArrayList();

	    Enumeration keys = instancesByName.keys();

	    while (keys.hasMoreElements()) {
		Object instance = keys.nextElement();
		CaselessStringKey fileName =
		    (CaselessStringKey)instancesByName.get(instance);

		Vector v = (Vector)instances.get(fileName);

		String dn = (String)v.get(2);
		String pn = (String)v.get(1);

		if (descriptorName.equalsIgnoreCase(dn) &&
		       productName.equalsIgnoreCase(pn))
		    list.add(instance);
	    }

	    return list;

	} else {
	    Object obj =
		instances.get(new CaselessStringKey(descriptorName));
	    
	    ArrayList list = new ArrayList(1);

	    list.add(obj);
	    return list;
	}
    }

    /**
     * Get the local name of a factoryInstance
     */
    String getLocalName(Object factoryInstance) {
	CaselessStringKey fileName =
	    (CaselessStringKey)instancesByName.get(factoryInstance);

	if (fileName != null)
	    return fileName.getName();

	return null;
    }

    /**
     * Check to see if the factoryInstance is valid object
     * of this registry mode.
     */
    private boolean checkInstance(Object factoryInstance) {

	if (!factoryClass.isInstance(factoryInstance))
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("FactoryCache0",
		    new Object[] {
			factoryInstance.getClass().getName(),
			modeName,
			factoryClass.getName()
		    }));

	return true;
    }

    /**
     * Check to see if <code>factoryInstance</code> is registered against
     * the specified <code>descriptorName</code> and <code>productName</code>.
     */
    private void checkRegistered(String descriptorName, String productName,
			         Object factoryInstance) {

	if (arePreferencesSupported) {

	    if (productName == null)
		throw new IllegalArgumentException(
		    "productName : " + JaiI18N.getString("Generic0"));

	    CaselessStringKey fileName =
		(CaselessStringKey)instancesByName.get(factoryInstance);

	    if (fileName != null) {

		Vector v = (Vector)instances.get(fileName);

		String pn = (String)v.get(1);
		String dn = (String)v.get(2);

		if ((dn != null) && dn.equalsIgnoreCase(descriptorName) &&
		    (pn != null) && pn.equalsIgnoreCase(productName)) {
		    return;
		}
	    }

	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("FactoryCache3",
		    new Object[] { factoryInstance.getClass().getName(),
				   descriptorName, productName}));
	} else {

	    CaselessStringKey key = new CaselessStringKey(descriptorName);

	    if (factoryInstance != instances.get(key)) {
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("FactoryCache4",
			new Object[] { factoryInstance.getClass().getName(),
				       descriptorName}));
	    }
	}
    }
}
