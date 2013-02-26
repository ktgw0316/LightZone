/*
 * $RCSfile: PropertyEnvironment.java,v $
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

import com.lightcrafts.media.jai.util.CaselessStringKeyHashtable;
import com.lightcrafts.media.jai.util.PropertyUtil;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * A class that implements the <code>PropertySource</code> interface.
 * Property names are treated in a case-insensitive manner.
 *
 *
 * @since JAI 1.1
 */ 
// In JAI 1.0.2 this class was namex com.lightcrafts.mediax.jai.PropertySourceImpl
// and was package scope (as it is now).
class PropertyEnvironment implements PropertySource {

    /** The local PropertyGenerators of this PropertyEnvironment. */
    Vector pg;

    /**
     * The sources of the associated node.  The elements should
     * be PropertySources.
     */
    Vector sources;

    // Dummy value to associate with a key in "suppressed"
    private static final Object PRESENT = new Object();

    /** The names of suppressed properties. */
    CaselessStringKeyHashtable suppressed;

    /**
     * Sources of properties of this node.  The keys are property names
     * and the values are Integers which should be indexes into the Vector
     * of sources.
     */
    CaselessStringKeyHashtable sourceForProp;

    /** 
     * The associated node, which is either a RenderedOp or a RenderableOp.
     */
    private Object op;

    /**
     * Hash of property names. Values are either PropertyGenerators,
     * PropertySources, or Integers.
     */
    private CaselessStringKeyHashtable propNames;

    /**
     * Locally added default PropertySource which will override the default
     * property inheritance mechanism, viz., inheritance from the lowest
     * indexed source if no other means was specified.
     */
    private PropertySource defaultPropertySource = null;

    /**
     * Flag which is set to indicate that the default PropertySource
     * has not yet been mapped into the property name table.  Initially
     * true as there is nothing to map.
     */
    private boolean areDefaultsMapped = true;
    
    /**
     * Constructs a <code>PropertyEnvironment</code>
     *
     * @param sources <code>PropertySource</code>s in operation source order.
     * @param generators <code>PropertyGenerator</code>s.
     * @param suppressed Names of suppressed properties.
     * @param sourceForProp Hash by property name of indexes of
     *        <code>PropertySource</code>s in <code>sources</code> from
     *        which to derive properties.
     * @param op The operation node.
     */
    public PropertyEnvironment(Vector sources, Vector generators,
                               Vector suppressed, Hashtable sourceForProp,
                               Object op) {
	this.sources = sources;
	this.pg = generators == null ?
            null : (Vector)generators.clone();

        // "suppressed" should never be null
	this.suppressed = new CaselessStringKeyHashtable();

	if (suppressed != null) {
	    Enumeration e = suppressed.elements();

	    while (e.hasMoreElements()) {
		this.suppressed.put(e.nextElement(), PRESENT);
	    }
	}

	this.sourceForProp = (sourceForProp == null) ?
		null : new CaselessStringKeyHashtable(sourceForProp);

        this.op = op;

	hashNames();
    }

    /**
     * Returns an array of Strings recognized as names by this
     * property source.
     *
     * @return an array of Strings giving the valid property names.
     */
    public String[] getPropertyNames() {
        mapDefaults();

	int count = 0;
	String names[] = new String[propNames.size()];
	for (Enumeration e = propNames.keys(); e.hasMoreElements(); ) {
	    names[count++] = ((CaselessStringKey)e.nextElement()).getName();
	}

	return names;
    }

    /**
     * Returns an array of <code>String</code>s recognized as names by
     * this property source that begin with the supplied prefix.  If
     * no property names match, <code>null</code> will be returned.
     * The comparison is done in a case-independent manner.
     *
     * <p> The default implementation calls <code>getPropertyNames()</code>
     * and searches the list of names for matches.
     *
     * @return an array of <code>String</code>s giving the valid
     * property names.
     */
    public String[] getPropertyNames(String prefix) {
	// This gives us a list of all non-suppressed properties
	String[] propertyNames = getPropertyNames();
        return PropertyUtil.getPropertyNames(propertyNames, prefix);
    }

    /**
     * Returns the class expected to be returned by a request for
     * the property with the specified name.  If this information
     * is unavailable, <code>null</code> will be returned.
     *
     * <p> This implemention returns <code>null</code> to avoid
     * provoking deferred calculations.
     *
     * @return The <code>Class</code> expected to be return by a
     *         request for the value of this property or <code>null</code>.
     */
    public Class getPropertyClass(String propertyName) {
        if(propertyName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        return null;
    }

    /**
     * Returns the value of a property.
     *
     * @param name the name of the property, as a String.
     * @return the value of the property, as an Object.
     */
    public Object getProperty(String name) {
        if(name == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        mapDefaults();

	Object o = propNames.get(name);

        Object property = null;
	if (o == null) {
	    return java.awt.Image.UndefinedProperty;
	} else if (o instanceof PropertyGenerator) {
            property = ((PropertyGenerator)o).getProperty(name, op);
	} else if (o instanceof Integer) { // copy from source
            int srcIndex = ((Integer)o).intValue();
            PropertySource src = (PropertySource)sources.elementAt(srcIndex);
            property = src.getProperty(name);
        } else if (o instanceof PropertySource) {
            property = ((PropertySource)o).getProperty(name);
        }

	return property;
    }

    /** ---- Methods to modify the local property environment. ---- */

    public void copyPropertyFromSource(String propertyName, int sourceIndex) {
        PropertySource propertySource =
            (PropertySource)sources.elementAt(sourceIndex);
        propNames.put(propertyName, propertySource);
        suppressed.remove(propertyName);
    }

    public void suppressProperty(String propertyName) {
	suppressed.put(propertyName, PRESENT);
	hashNames();
    }

    public void addPropertyGenerator(PropertyGenerator generator) {
	if (pg == null) {
	    pg = new Vector();
	}
	pg.addElement(generator);

	// Remove suppressed status of any property being generated by
	// this PropertyGenerator
	removeSuppressedProps(generator);
	hashNames();
    }

    /**
     * Sets a PropertySource from which to derive all non-suppressed
     * properties emitted by the PropertySource if and only if neither
     * a PropertyGenerator nor a copy-from-source directive exists
     * for the requested property.  This PropertySource will supersede
     * automatic inheritance from any (operation) sources.  It should
     * be used, for example, when a rendered node wishes to derive
     * property values from its rendering without overriding any user
     * configuration settings in the property environment of the node.
     */
    public void setDefaultPropertySource(PropertySource ps) {
        // If no change just return.
        if(ps == defaultPropertySource) {
            return;
        }

        if(defaultPropertySource != null) {
            // Return the table to zero state if defaults existed before.
            hashNames();
        }

        // Unset the flag.
        areDefaultsMapped = false;

        // Cache the parameter.
        defaultPropertySource = ps;
    }

    /**
     * Updates "propNames" hash with the default PropertySource.  This
     * method allows deferred access to the default PropertySource so
     * as to postpone calculations which access thereto might incur.
     * Does nothing unless the default PropertySource has changed.
     */
    private void mapDefaults() {
        if(!areDefaultsMapped) {
            // Set the flag.
            areDefaultsMapped = true;

            // Update with default PropertySource only if non-null.
            if(defaultPropertySource != null) {
                String[] names = defaultPropertySource.getPropertyNames();
                if(names != null) {
                    int length = names.length;
                    for(int i = 0; i < length; i++) {
                        if(!suppressed.containsKey(names[i])) {
                            Object o = propNames.get(names[i]);
                            if(o == null ||            // undefined property
                               o instanceof Integer) { // default inheritance
                                // Add "defaultPropertySource" or
                                // replace default inheritance.
                                propNames.put(names[i], defaultPropertySource);
                            }
                        }
                    }
                }
            }
        }
    }

    private void removeSuppressedProps(PropertyGenerator generator) {
	String names[] = generator.getPropertyNames();
	for (int i=0; i<names.length; i++) {
            suppressed.remove(names[i]);
	}
    }

    private void hashNames() {
	propNames = new CaselessStringKeyHashtable();

	// Copy properties from sources. This is the default behavior if no
        // other property source is specified. The sources are represented
        // via Integer values in propNames.
	if (sources != null) {
	    // Then get property names from each source
	    for (int i = sources.size() - 1; i >= 0; i--) {
                Object o = sources.elementAt(i);

                if (o instanceof PropertySource) {
		    PropertySource source = (PropertySource)o;
		    String[] propertyNames = source.getPropertyNames();

		    if (propertyNames != null) {
                        for (int j = 0; j < propertyNames.length; j++) {
                            String name = propertyNames[j];
                            if (!suppressed.containsKey(name)) {
                                propNames.put(name, new Integer(i));
                            }
		        }
		    }
                }
	    }
	}
	
	// Get non-suppressed properties from PropertyGenerators.
        // The propNames values are PropertyGenerator instances.
	if (pg != null) {
	    PropertyGenerator generator;
	    
	    for (Iterator it = pg.iterator(); it.hasNext(); ) {
		generator = (PropertyGenerator)it.next();
                if(generator.canGenerateProperties(op)) {
                    String[] propertyNames = generator.getPropertyNames();
                    if (propertyNames != null) {
                        for (int i = 0; i < propertyNames.length; i++) {
                            String name = propertyNames[i];
                            if (!suppressed.containsKey(name)) {
                                propNames.put(name, generator);
                            }
                        }
                    }
		}
	    }
	}

	// Lastly, honor all the copyPropertyFromSource directives
        // The propNames values are PropertySource instances.
	if (sourceForProp != null) {
	    for (Enumeration e = sourceForProp.keys(); e.hasMoreElements(); ) {
		CaselessStringKey name = (CaselessStringKey)e.nextElement();

                if (!suppressed.containsKey(name)) {
                    Integer i = (Integer)sourceForProp.get(name);
                    PropertySource propertySource =
                        (PropertySource)sources.elementAt(i.intValue());
                    propNames.put(name, propertySource);
                }
	    }
	}

        // Unset the default mapping flag.
        areDefaultsMapped = false;
    }    
}
