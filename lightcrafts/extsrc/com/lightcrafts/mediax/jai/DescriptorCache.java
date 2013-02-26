/*
 * $RCSfile: DescriptorCache.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:07 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * A class to manage the descriptors belong to a certain
 * <code>RegistryMode</code>
 *
 * The <code>RegistryElementDescriptor</code> names are used in a
 * case-insensitive manner.
 */
class DescriptorCache {

    /**
     * The name of the mode for which the cache of descriptors is 
     * being maintained.
     */
    final String modeName;

    /**
      * Cache the RegistryMode since it is bound to get used
      * many, many times.
      */
    final RegistryMode mode;

    /** Does the registry mode for this cache support preferences. */
    final boolean arePreferencesSupported;

    /** Does the registry mode for this cache support properties. */
    final boolean arePropertiesSupported;

    /**
     * A Hashtable of all the <code>RegistryElementDescriptor</code>s,
     * hashed by their name.
     */
    private Hashtable descriptorNames;
    
    /**
     * A Hashtable of all the products, hashed by the
     * name of the <code>RegistryElementDescriptor</code> to which they belong.
     */
    private Hashtable products;

    /**
     * A Hashtable of all the product preferences, hashed by the
     * descriptor name that the products belong to. The product
     * preferences are stored in a <code>Vector</code>. The elements
     * of the <code>Vector</code> consist of <code>String[2]</code>
     * objects, each storing the preferred product's name, and the other
     * product's name within it.
     */
    private Hashtable productPrefs;

    //
    // Property related tables...
    //
    
    /**
     * A Hashtable of <code>Vector</code>s containing all the 
     * <code>PropertyGenerator</code>s, hashed by the descriptor
     * name that the <code>PropertyGenerator</code>s belong to.
     */
    private Hashtable properties;

    /**
     * A Hashtable of <code>Vector</code>s containing the names 
     * of all the properties to be suppressed, hashed by the descriptor
     * name whose properties are to be suppressed.
     */
    private Hashtable suppressed;

    /**
     * A Hashtable containing information about which source a property
     * should be copied from, hashed by the descriptor name to which the
     * property belongs. The information about which source a property
     * should be copied from is stored in a Hashtable containing the
     * index of the source, hashed by the property name.
     */
    private Hashtable sourceForProp;

    /**
     * A Hashtable that stores the <code>PropertySource</code>s for all
     * the properties, hashed by the descriptor name that the properties
     * belong to. The <code>PropertySource</code>s are stored in a
     * Hashtable, hashed by the name of the property.
     */
    private Hashtable propNames;

    /**
     * The Constructor. Create a <code>RegistryElementDescriptor</code>
     * cache for maintaining descriptors for the specified mode.
     *
     * @param modeName the registry mode name.
     *
     */
    DescriptorCache(String modeName) {

	this.modeName = modeName;
	this.mode     = RegistryMode.getMode(modeName);

	arePreferencesSupported = mode.arePreferencesSupported();
	arePropertiesSupported  = mode.arePropertiesSupported();

	descriptorNames = new Hashtable();
	products	= new Hashtable();

	if (arePreferencesSupported)
	    productPrefs = new Hashtable();

	// Property related tables.
	properties    = new Hashtable();
	suppressed    = new Hashtable();
	sourceForProp = new Hashtable();
	propNames     = new Hashtable();
    }

    /**
     * Adds a <code>RegistryElementDescriptor</code> to the cache.
     *
     * <p> An <code>RegistryElementDescriptor</code> cannot be added against a
     * descriptor name under which another <code>RegistryElementDescriptor</code> 
     * was added previously.
     *
     * @param rdesc an <code>RegistryElementDescriptor</code> containing
     *		    information about the descriptor.
     *
     * @return false, if one already existed. true otherwise.
     *
     * @throws IllegalArgumentException is rdesc in null
     * @throws IllegalArgumentException if the descriptor has
     *		already been registered in this cache.
     */
    boolean addDescriptor(RegistryElementDescriptor rdesc) {

        if (rdesc == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

	String descriptorName = rdesc.getName();

	// Use a caseless version of the key.
	CaselessStringKey key = new CaselessStringKey(descriptorName);

	// If the key has already been added bail out ...
	if (descriptorNames.containsKey(key) == true) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache0",
		    new Object[] {descriptorName, modeName}));
	}

	// Store the RegistryElementDescriptor hashed by its global name
	descriptorNames.put(key, rdesc);

	// Store the ProductOperationGraph hashed by the caseless
	// descriptor name
	if (arePreferencesSupported)
	    products.put(key, new ProductOperationGraph());

	// if properties arent supported by this descriptor we are done.
	if (rdesc.arePropertiesSupported() == false)
	    return true;

	// Store the Property Generators associated with this descriptor
	// for the specified mode.
	PropertyGenerator props[] = rdesc.getPropertyGenerators(modeName);

	if (props != null) {
	    for (int i=0; i<props.length; i++) {

		Vector v = (Vector)properties.get(key);
		if (v == null) {
		    v = new Vector();
		    v.addElement(props[i]);
		    properties.put(key, v);
		} else {
		    v.addElement(props[i]);
		}
		
		v = (Vector)suppressed.get(key);
		Hashtable h = (Hashtable)sourceForProp.get(key);
		String names[] = props[i].getPropertyNames();
		
		for (int j=0; j<names.length; j++) {
		    CaselessStringKey name = new CaselessStringKey(names[j]);

		    if (v != null) v.remove(name);
		    if (h != null) h.remove(name);
		}
	    }
	}

	return true;
    }

    /**
     * Removes a <code>RegistryElementDescriptor</code> from the cache.
     *
     * @param descriptorName the descriptor name as a String.
     *
     * @return false, if one wasnt previously registered, true otherwise.
     *
     * @throws IllegalArgumentException if descriptorName is null or
     *		was not previously registered.
     * @throws IllegalArgumentException if any of the 
     *         <code>PropertyGenerator</code>s associated with the 
     *         <code>RegistryElementDescriptor</code> to be removed is null.
     */
    boolean removeDescriptor(String descriptorName) {

	// Use a caseless version of the key.
	CaselessStringKey key = new CaselessStringKey(descriptorName);

	// If it is not present in the cache already, then return false.
	if (descriptorNames.containsKey(key) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache1",
		    new Object[] {descriptorName, modeName}));
	}

	RegistryElementDescriptor rdesc = 
		(RegistryElementDescriptor)descriptorNames.get(key);

	PropertyGenerator props[] = null;

	// if properties arent supported by this descriptor we are done.
	if (rdesc.arePropertiesSupported() == true)
	    props = rdesc.getPropertyGenerators(modeName);

	// Remove the Property Generators associated with this descriptor
	if (props != null) {
	    for (int i=0; i<props.length; i++) {

		if (props[i] == null) {
		    throw new IllegalArgumentException(
			JaiI18N.formatMsg("DescriptorCache2",
			    new Object[] {
				new Integer(i),
				descriptorName, modeName}));
		}
		
		Vector v = (Vector)properties.get(key);
		if (v != null) {
		    v.removeElement(props[i]);
		}
	    }
	}

	// Remove the RegistryElementDescriptor hashed by its global name
	descriptorNames.remove(key);

	if (arePreferencesSupported)
	    products.remove(key);

	return true;
    }
	
    /**
     * Removes a <code>RegistryElementDescriptor</code> from the cache.
     *
     * @param rdesc an <code>RegistryElementDescriptor</code> to be removed.
     *
     * @return false, if one wasnt previously registered, true otherwise.
     *
     * @throws IllegalArgumentException if rdesc is null.
     * @throws IllegalArgumentException if rdesc was not
     *		previously registered.
     * @throws IllegalArgumentException if any of the 
     *         <code>PropertyGenerator</code>s associated with the 
     *         <code>RegistryElementDescriptor</code> to be removed is null.
     */
    boolean removeDescriptor(RegistryElementDescriptor rdesc) {
        if (rdesc == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
	return removeDescriptor(rdesc.getName());
    }

    /**
     * Returns the <code>RegistryElementDescriptor</code> that is
     * currently registered under the given name, or null if none
     * exists.
     *
     * @param descriptorName the String to be queried.
     *
     * @return an <code>RegistryElementDescriptor</code>.
     *
     * @throws IllegalArgumentException if descriptorName is null
     */
    RegistryElementDescriptor getDescriptor(String descriptorName) {
	// Use a caseless version of the key.
	CaselessStringKey key = new CaselessStringKey(descriptorName);

	return (RegistryElementDescriptor)descriptorNames.get(key);
    }
	
    /**
     * Returns a <code>List</code> of all currently registered 
     * <code>RegistryElementDescriptor</code>s.
     *
     * @return a List of <code>RegistryElementDescriptor</code>s.
     */
    List getDescriptors() {

	ArrayList list = new ArrayList();

	for (Enumeration en = descriptorNames.elements();
			 en.hasMoreElements(); ) {
	    list.add(en.nextElement());
	}

	return list;
    }

    /**
     * Returns a list of names under which all the 
     * <code>RegistryElementDescriptor</code>s in the registry are registered.
     *
     * @return a list of currently existing descriptor names.
     */
    String[] getDescriptorNames() {

	Enumeration e = descriptorNames.keys();
	int size = descriptorNames.size();
	String names[] = new String[size];

	for (int i = 0; i < size; i++) {
	    CaselessStringKey key = (CaselessStringKey)e.nextElement();
	    names[i] = key.getName();
	}
		
	return names;
    }

    /**
     * Registers a product name against a descriptor. The descriptor
     * must already exist in the cache. If the product already existed
     * under the descriptor, the old one is returned without adding
     * another.
     *
     * @param descriptorName the descriptor name as a String
     * @param productName the product name as a String.
     *
     * @return null, if the descriptor or the product did not exist or the product
     *
     * @throws IllegalArgumentException if descriptorName is null
     */
    OperationGraph addProduct(String descriptorName,
			      String productName) {
	// Use a caseless version of the key.
	CaselessStringKey key = new CaselessStringKey(descriptorName);

	if (productName == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	ProductOperationGraph pog =
		    (ProductOperationGraph)products.get(key);

	if (pog == null)
	    return null;

	PartialOrderNode pon = pog.lookupOp(productName);

	if (pon == null) {
	    pog.addProduct(productName);

	    pon = pog.lookupOp(productName);
	}

	return (OperationGraph)pon.getData();
    }

    /**
     * Unregisters a product name against a descriptor. The
     * descriptor must already exist in the cache and the procduct
     * must have been registered against the descriptor
     *
     * @param descriptorName the descriptor name as a String
     * @param productName the product name as a String.
     *
     * @return false, if the descriptor did not exist or the product
     *		      was not already registered.
     *
     * @throws IllegalArgumentException if descriptorName is null
     */
    boolean removeProduct(String descriptorName, String productName) {
	// Use a caseless version of the key.
	CaselessStringKey key = new CaselessStringKey(descriptorName);

	if (productName == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	ProductOperationGraph pog =
		    (ProductOperationGraph)products.get(key);

	if (pog == null)
	    return false;

	PartialOrderNode pon = pog.lookupOp(productName);

	if (pon == null)
	    return false;

	pog.removeOp(productName);

	return true;
    }

    /**
     * Looks up a product name against a descriptor.
     *
     * @param descriptorName the descriptor name as a String
     * @param productName the product name as a String.
     *
     * @return null, if the descriptor did not exist or the product
     *	       was not already registered. Otherwise returns the
     *	       <code>PartialOrderNode</code> corresponding to this product
     *
     * @throws IllegalArgumentException if descriptorName is null
     */
    OperationGraph lookupProduct(String descriptorName,
				 String productName) {
	// Use a caseless version of the key.
	CaselessStringKey key = new CaselessStringKey(descriptorName);

	if (productName == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	ProductOperationGraph pog =
		    (ProductOperationGraph)products.get(key);

	if (pog == null)
	    return null;

	PartialOrderNode pon = pog.lookupOp(productName);

	if (pon == null)
	    return null;

	return (OperationGraph)pon.getData();
    }

    /**
     * Sets a preference between two products registered under
     * a common <code>RegistryElementDescriptor</code>.
     * if the descriptor was not registered previously and no preference
     * will be set. Any attempt to set a preference between a product
     * and itself will be ignored.
     *
     * @param descriptorName the operation name as a String. 
     * @param preferredProductName the product to be preferred.
     * @param otherProductName the other product.
     *
     * @return false, if the descriptor was not registered previously
     *		      or if either if the products were not already
     *		      added against the descriptor.
     *
     * @throws IllegalArgumentException if this registry mode does
     *		not support preferences.
     * @throws IllegalArgumentException if any of the args is null
     * @throws IllegalArgumentException if descriptorName or either of
     *		the products were not previously registered.
     */
    boolean setProductPreference(String descriptorName,
				 String preferredProductName,
				 String otherProductName) {

	if (!arePreferencesSupported) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache6",
		    new Object[] {modeName}));
	}

	if ((descriptorName == null) || (preferredProductName == null) ||
	    (otherProductName == null))
	    throw new IllegalArgumentException(
			JaiI18N.getString("Generic0"));

	// Attempt to set preference of a product with itself, do nothing.
	if (preferredProductName.equalsIgnoreCase(otherProductName)) {
	    return false;
	}
	
	// Use a caseless version of the key.
	CaselessStringKey key = new CaselessStringKey(descriptorName);
	
	if (descriptorNames.containsKey(key) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache1",
		    new Object[] {descriptorName, modeName}));
	} 
		
	ProductOperationGraph og = (ProductOperationGraph)products.get(key);
		
	if (og == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache3",
		    new Object[] {descriptorName, modeName}));
	}

	if (og.lookupOp(preferredProductName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache4",
		    new Object[]
			{descriptorName, modeName, preferredProductName}));
	} 

	if (og.lookupOp(otherProductName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache4",
		    new Object[]
			{descriptorName, modeName, otherProductName}));
	}

	og.setPreference(preferredProductName, otherProductName);
		
	String[] prefs = { preferredProductName, otherProductName };

	// Update structures to reflect this new product preference.
	if (productPrefs.containsKey(key) == false) {
	    Vector v = new Vector();
	    v.addElement(prefs);

	    productPrefs.put(key, v);

	} else {
	    Vector v = (Vector)productPrefs.get(key);
	    v.addElement(prefs);
	}

	return true;
    }

    /**
     * Removes a preference between two products registered under
     * a common <code>RegistryElementDescriptor</code>. An error message will
     * be printed out if the operation was not registered previously.
     *
     * @param descriptorName the operation name as a String. 
     * @param preferredProductName the product formerly preferred.
     * @param otherProductName the other product.
     *
     * @return false, if the descriptor was not registered previously
     *		      or if either if the products were not already
     *		      added against the descriptor.
     *
     * @throws IllegalArgumentException if this registry mode does
     *		not support preferences.
     * @throws IllegalArgumentException if any of the args is null
     * @throws IllegalArgumentException if descriptorName or either of
     *		the products were not previously registered.
     */
    boolean unsetProductPreference(String descriptorName,
				   String preferredProductName,
				   String otherProductName) {

	if (!arePreferencesSupported) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache6",
		    new Object[] {modeName}));
	}

	if ((descriptorName == null) || (preferredProductName == null) ||
	    (otherProductName == null))
	    throw new IllegalArgumentException(
			JaiI18N.getString("Generic0"));

	// Attempt to unset preference of a product with itself, do nothing.
	if (preferredProductName.equalsIgnoreCase(otherProductName)) {
	    return false;
	}

	// Use a caseless version of the key.
	CaselessStringKey key = new CaselessStringKey(descriptorName);
		
	if (descriptorNames.containsKey(key) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache1",
		    new Object[] {descriptorName, modeName}));
	} 

	ProductOperationGraph og = (ProductOperationGraph)products.get(key);

	if (og == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache3",
		    new Object[] {descriptorName, modeName}));
	}

	if (og.lookupOp(preferredProductName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache4",
		    new Object[]
			{descriptorName, modeName, preferredProductName}));
	} 

	if (og.lookupOp(otherProductName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache4",
		    new Object[]
			{descriptorName, modeName, otherProductName}));
	}

	og.unsetPreference(preferredProductName,
			   otherProductName);
		
	// Update structures to reflect removal of this product preference.
	if (productPrefs.containsKey(key) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache5",
		    new Object[] {descriptorName, modeName}));
	} 
		
	Vector v = (Vector)productPrefs.get(key);
	Iterator it = v.iterator();
	while(it.hasNext()) {
	    String[] prefs = (String[])it.next();

	    if (prefs[0].equalsIgnoreCase(preferredProductName) &&
		prefs[1].equalsIgnoreCase(otherProductName)) {
		it.remove();
		break;
	    }
	}

	return true;
    }

    /**
     * Removes all preferences between products registered under
     * a common <code>RegistryElementDescriptor</code>. An error message will
     * be printed out if the operation was not registered previously.
     *
     * @param descriptorName the operation name as a String. 
     *
     * @throws IllegalArgumentException if this registry mode does
     *		not support preferences.
     * @throws IllegalArgumentException if descriptorName is null.
     */
    boolean clearProductPreferences(String descriptorName) {
	
	if (!arePreferencesSupported) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache6",
		    new Object[] {modeName}));
	}

	// Use a caseless version of the key.
	CaselessStringKey key = new CaselessStringKey(descriptorName);

	if (descriptorNames.containsKey(key) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache1",
		    new Object[] {descriptorName, modeName}));
	} 

	ProductOperationGraph og = (ProductOperationGraph)products.get(key);

	if (og == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache3",
		    new Object[] {descriptorName, modeName}));
	}
				
	// if there are no preferences to clear..
	if (productPrefs.containsKey(key) == false)
	    return true;

	Vector v = (Vector)productPrefs.get(key);
	Enumeration e = v.elements();

	while(e.hasMoreElements()) {
	    String prefs[] = (String[])e.nextElement();
			    
	    String pref  = prefs[0];
	    String other = prefs[1];
			    
	    if (og.lookupOp(pref) == null) {
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("DescriptorCache4",
			new Object[]
			    {descriptorName, modeName, pref}));
	    } 

	    if (og.lookupOp(other) == null) {
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("DescriptorCache4",
			new Object[]
			    {descriptorName, modeName, other}));
	    }

	    og.unsetPreference(pref, other);
	}
	productPrefs.remove(key);
	return true;
    }

    /**
     * Returns a list of the pairwise product preferences
     * under a particular <code>RegistryElementDescriptor</code>. If no product
     * preferences have been set, returns null.
     *
     * @param descriptorName the operation name as a String. 
     * @return an array of 2-element arrays of Strings.
     *
     * @throws IllegalArgumentException if this registry mode does
     *		not support preferences.
     * @throws IllegalArgumentException if descriptorName is null
     */
    String[][] getProductPreferences(String descriptorName) {
	
	if (!arePreferencesSupported) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache6",
		    new Object[] {modeName}));
	}

	// Use a caseless version of the key.
	CaselessStringKey key = new CaselessStringKey(descriptorName);

	Vector v;

	if (productPrefs.containsKey(key) == false) {
	    // No product preferences have been set.
	    return null;
	} else {
	    v = (Vector)productPrefs.get(key);
	    int s = v.size();
	    if (s == 0) {
		return null;
	    }
	    String productPreferences[][] = new String[s][2];
	    int count = 0;
	    Enumeration e = v.elements();
	    while(e.hasMoreElements()) {
		String[] o = (String[])e.nextElement();
		productPreferences[count][0]   = o[0];
		productPreferences[count++][1] = o[1];
	    }
	    
	    return productPreferences;
	}
    }

    /**
     * Returns a list of the products registered under a particular
     * <code>RegistryElementDescriptor</code>, in an ordering that
     * satisfies all of the pairwise preferences that have been
     * set. Returns <code>null</code> if cycles exist. Returns
     * <code>null</code> if no <code>RegistryElementDescriptor</code>
     * has been registered under this descriptorName, or if no products
     * exist for this operation.
     *
     * @param descriptorName the operation name as a String. 
     *
     * @return a Vector of Strings representing product names. returns
     *	       null if this registry mode does not support preferences.
     *
     * @throws IllegalArgumentException if descriptorName is null.
     */
    Vector getOrderedProductList(String descriptorName) {
	
	if (!arePreferencesSupported)
	    return null;

	// Use a caseless version of the key.
	CaselessStringKey key = new CaselessStringKey(descriptorName);
	
	if (descriptorNames.containsKey(key) == false) {
	    return null;
	}

	ProductOperationGraph productGraph =
	    (ProductOperationGraph)products.get(key);

	// If no products exist under this Operation Name
	if (productGraph == null) {
	    return null;
	}

	// Get the ordered vector of PartialOrderNodes
	Vector v1 = productGraph.getOrderedOperationList();

	if (v1 == null)
	    return null;

	int size = v1.size();

	if (size == 0) {	// no element
	    return null;
	} else {
	    Vector v2 = new Vector();
	    for (int i = 0; i < size; i++) {
		v2.addElement(((PartialOrderNode)v1.elementAt(i)).getName());
	    }
	    
	    return v2;
	}
    }

    // Property management

    private boolean arePropertiesSupported(String descriptorName) {
    
	CaselessStringKey key = new CaselessStringKey(descriptorName);

	RegistryElementDescriptor rdesc = 
		(RegistryElementDescriptor)descriptorNames.get(key);

	if (rdesc == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache1",
		    new Object[] {descriptorName, modeName}));
	}

	return arePropertiesSupported;
    }

    /**
     * Removes all property associated information from this
     * <code>DescriptorCache</code>.
     */
    void clearPropertyState() {				

	if (arePropertiesSupported == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache7",
				    new Object[] {modeName}));
	}

	properties = new Hashtable();
	suppressed = new Hashtable();					
    }
	
    /**
     * Adds a <code>PropertyGenerator</code> to the a particular 
     * descriptor.
     *
     * @param descriptorName the operation name as a String.
     * @param generator the <code>PropertyGenerator</code> to be added.
     */
    void addPropertyGenerator(String descriptorName,
			      PropertyGenerator generator) {

	if ((descriptorName == null) || (generator == null))
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	if (arePropertiesSupported(descriptorName) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache7",
				    new Object[] {modeName}));
	}

	CaselessStringKey key = new CaselessStringKey(descriptorName);
	
	Vector v = (Vector)properties.get(key);

	if (v == null) {
	    v = new Vector();
	    properties.put(key, v);
	}

	v.addElement(generator);

	v = (Vector)suppressed.get(key);
	Hashtable h = (Hashtable)sourceForProp.get(key);

	String names[] = generator.getPropertyNames();

	for (int j=0; j<names.length; j++) {
	    CaselessStringKey name = new CaselessStringKey(names[j]);

	    if (v != null) v.remove(name);
	    if (h != null) h.remove(name);
	}
    }

    private void hashNames(String descriptorName) {
	CaselessStringKey key = new CaselessStringKey(descriptorName);

	Vector c = (Vector)properties.get(key);
	Vector s = (Vector)suppressed.get(key);	

	Hashtable h = new Hashtable();
	propNames.put(key, h);
		
	if (c != null) {
	    PropertyGenerator pg;
	    String names[];

	    for (Iterator it = c.iterator(); it.hasNext(); ) {
		pg = (PropertyGenerator)it.next();
		names = pg.getPropertyNames();

		for (int i=0; i<names.length; i++) {
		    CaselessStringKey name =
			    new CaselessStringKey(names[i]);

		    // Don't add a property that was suppressed
		    if ((s == null) || !s.contains(name)) {
			h.put(name, pg);
		    }
		}
	    }
	}

	Hashtable htable = (Hashtable)sourceForProp.get(key);

	if (htable != null) {
	    for (Enumeration e = htable.keys(); e.hasMoreElements(); ) {
		CaselessStringKey name = (CaselessStringKey)e.nextElement();

		int i = ((Integer)htable.get(name)).intValue();

		PropertyGenerator generator = new
		    PropertyGeneratorFromSource(i, name.getName());

		h.put(name, generator);
	    }
	}
    }
	
    /**
     * Removes a <code>PropertyGenerator</code> from its association
     * with a particular descriptor. If the generator was not associated
     * with the descriptor, nothing happens.
     *
     * @param descriptorName the operation name as a String.
     * @param generator the <code>PropertyGenerator</code> to be removed.
     *
     * @throws IllegalArgumentException if descriptorName is null.
     * @throws IllegalArgumentException if generator is null.
     */
    void removePropertyGenerator(String descriptorName,
				 PropertyGenerator generator) {

        if ((descriptorName == null) || (generator == null)) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

	if (arePropertiesSupported(descriptorName) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache7",
				    new Object[] {modeName}));
	}

	CaselessStringKey key = new CaselessStringKey(descriptorName);
	
	Vector v = (Vector)properties.get(key);

	if (v != null) {
	    v.removeElement(generator);
	}		
    }

    /**
     * Forces a particular property to be suppressed by nodes associated
     * with a particular descriptor. By default, properties are passed
     * through unchanged.
     *
     * @param descriptorName the operation name as a String.
     * @param propertyName the name of the property to be suppressed.
     *
     * @throws IllegalArgumentException if descriptorName is null.
     * @throws IllegalArgumentException if propertyName is null.
     */
    void suppressProperty(String descriptorName,
			  String propertyName) {
	
        if ((descriptorName == null) || (propertyName == null)) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

	if (arePropertiesSupported(descriptorName) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache7",
				    new Object[] {modeName}));
	}

	CaselessStringKey key = new CaselessStringKey(descriptorName);
	CaselessStringKey propertyKey = new CaselessStringKey(propertyName);

	// Mark the property name as suppressed.  
	Vector v = (Vector)suppressed.get(key);

	if (v == null) {
	    v = new Vector();
	    suppressed.put(key, v);
	}

	v.addElement(propertyKey);

	Hashtable h = (Hashtable)sourceForProp.get(key);

	if (h != null) {
	    h.remove(propertyKey);
	}
    }

    /**
     * Forces all properties to be suppressed by nodes associated with a
     * particular descriptor. By default, properties are passed through
     * unchanged.
     *
     * @param descriptorName the operation name as a String.
     *
     * @throws IllegalArgumentException if descriptorName is null.
     */
    void suppressAllProperties(String descriptorName) {	

	if (arePropertiesSupported(descriptorName) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache7",
				    new Object[] {modeName}));
	}

	// In this method synchronized takes care of the fact that all the
	// operations take place in a sequential fashion, while
	// suppressProperty's writeLock insures that all changes are
	// made by only one thread.
	CaselessStringKey key = new CaselessStringKey(descriptorName);
	
	// Get names of all properties that this descriptorName
	// is associated with
	Vector v = (Vector)properties.get(key);

	if (v != null) {
	    PropertyGenerator pg;

	    for (Iterator it = v.iterator(); it.hasNext(); ) {
		pg = (PropertyGenerator)it.next();
		String propertyNames[] = pg.getPropertyNames();
		for (int i=0; i<propertyNames.length; i++) {
		    suppressProperty(descriptorName, propertyNames[i]);
		}
	    }
	}
    }

    /**
     * Forces a property to be copied from the specified source index
     * by nodes associated with a particular descriptor. By default, a
     * property is copied from the first source node that emits it. The
     * result of specifying an invalid source is undefined.
     *
     * @param descriptorName the operation name as a String.
     * @param propertyName the name of the property to be copied.
     * @param sourceIndex the index of the source to copy the property from.
     *
     * @throws IllegalArgumentException if descriptorName is null.
     * @throws IllegalArgumentException if propertyName is null.
     */
    void copyPropertyFromSource(String descriptorName,
				String propertyName,
				int sourceIndex) {

        if ((descriptorName == null) || (propertyName == null)) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

	if (arePropertiesSupported(descriptorName) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache7",
				    new Object[] {modeName}));
	}

	CaselessStringKey key = new CaselessStringKey(descriptorName);
	CaselessStringKey propertyKey = new CaselessStringKey(propertyName);

	Hashtable h = (Hashtable)sourceForProp.get(key);

	if (h == null) {
	    h = new Hashtable();
	    sourceForProp.put(key, h);
	}

	h.put(propertyKey, new Integer(sourceIndex));

	Vector v = (Vector)suppressed.get(key);

	if (v != null) {
	    v.remove(propertyKey);
	}
    }

    /**
     * Returns a list of the properties generated by nodes implementing
     * the functionality associated with a particular descriptor.
     * Returns null if no properties are generated.
     *
     * @param descriptorName the operation name as a String.
     * @return an array of Strings.
     *
     * @throws IllegalArgumentException if descriptorName is null.
     */
    String[] getGeneratedPropertyNames(String descriptorName) {

	if (arePropertiesSupported(descriptorName) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache7",
				    new Object[] {modeName}));
	}

	CaselessStringKey key = new CaselessStringKey(descriptorName);

	hashNames(descriptorName);
		
	Hashtable h = (Hashtable)propNames.get(key);

	if(h != null && h.size() > 0) {
	    String names[] = new String[h.size()];
	    int count = 0;
	    for (Enumeration e = h.keys(); e.hasMoreElements(); ) {
		CaselessStringKey str = (CaselessStringKey)e.nextElement();
		names[count++] = str.getName();
	    }				
	    
	    return count > 0 ? names : null;
	}
		
	return null;
    }

    /**
      * Merge mode-specific property environment with mode-independent
      * property environment of the descriptor. Array elements of
      * "sources" are expected to be in the same ordering as referenced
      * by the "sourceIndex" parameter of copyPropertyFromSource().
      *
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param sources the <code>PropertySource</code>s corresponding to
      *     the sources of the object representing the named descriptor
      *     in the indicated mode.
      * @param op the <code>Object</code> from which properties will
      *           be generated.
      *
      * @return A <code>PropertySource</code> which encapsulates
      *     the global property environment for the object representing
      *     the named descriptor in the indicated mode.
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      * @throws IllegalArgumentException if the specified mode does not
      *             support properties.
      *
      * @since JAI 1.1
      */
    PropertySource getPropertySource(String descriptorName,
				     Object op,
                                     Vector sources) {
	
        if ((descriptorName == null) || (op == null) || (sources == null)) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

	if (arePropertiesSupported(descriptorName) == false) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("DescriptorCache7",
				    new Object[] {modeName}));
	}

	CaselessStringKey key = new CaselessStringKey(descriptorName);

	Vector pg = (Vector)properties.get(key);
	Vector sp = (Vector)suppressed.get(key);

	Hashtable sfp = (Hashtable)sourceForProp.get(key);
		
	return new PropertyEnvironment(sources, pg, sp, sfp, op);
    }
}
