/*
 * $RCSfile: PropertySourceImpl.java,v $
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.lightcrafts.media.jai.util.PropertyUtil;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * A utility implementation of the <code>PropertySource</code> interface.
 * Properties are managed by three internal structures: one which maps
 * property names to values, a second which maps property names to
 * <code>PropertySource</code>s, and a third which tracks which entries
 * in the name-value mapping derived their respective values from a
 * <code>PropertySource</code> in the name-<code>PropertySource</code>
 * mapping.  The case of property names is retained for subsequent
 * retrieval but is ignored when the names are used as keys.
 *
 * @see CaselessStringKey
 * @see PropertySource
 * @see WritablePropertySource
 * @see WritablePropertySourceImpl
 *
 * @since JAI 1.1
 */
// NB A class of this name existed in JAI 1.0.2 but that class was renamed
// to what is now PropertyEnvironment.
public class PropertySourceImpl implements PropertySource, Serializable {
    /**
     * Mapping of <code>CaselessStringKey</code>s to values.
     * If this object is serialized, only those entries of which
     * the value is serializable will be retained.
     */
    protected transient Map properties;

    /**
     * Mapping of <code>CaselessStringKey</code>s to
     * <code>PropertySource</code>s.
     * If this object is serialized, only those entries of which
     * the value is serializable will be retained.
     */
    protected transient Map propertySources;

    /**
     * <code>CaselessStringKey</code>s corresponding to the keys of entries
     * in <code>properties</code> which derived their respective
     * values from a <code>PropertySource</code> in
     * <code>propertySources</code>.
     */
    protected Set cachedPropertyNames;

    /**
     * Constructs a <code>PropertySourceImpl</code> instance with
     * no properties set.
     */
    protected PropertySourceImpl() {
        properties = new Hashtable();
        propertySources = new Hashtable();
        cachedPropertyNames = Collections.synchronizedSet(new HashSet());
    }

    /**
     * Constructs a <code>PropertySourceImpl</code> instance which
     * will derive properties from one or both of the supplied parameters.
     * The <code>propertyMap</code> and <code>propertySource</code> parameters
     * will be used to initialize the name-value and
     * name-<code>PropertySource</code> mappings, respectively.
     * Entries in the <code>propertyMap</code> object will be assumed
     * to be properties if the key is a <code>String</code> or a
     * <code>CaselessStringKey</code>.  The <code>propertySource</code>
     * object will be queried for the names of properties that it emits
     * but requests for associated values will not be made at this time
     * so as to to defer any calculation that such requests might provoke.
     * The case of property names will be retained but will be ignored
     * insofar as the name is used as a key to the property value.
     *
     * @param propertyMap A <code>Map</code> from which to copy properties
     *        which have keys which are either <code>String</code>s or
     *        <code>CaselessStringKey</code>s.
     * @param propertySource A <code>PropertySource</code> from which to
     *        derive properties.
     *
     * @exception IllegalArgumentException if <code>propertyMap</code>
     *            and <code>propertySource</code> are both <code>null</code>
     *            and this constructor is not being invoked from within a
     *            subclass constructor.  When invoked from a subclass
     *            constructor both parameters may be <code>null</code>.
     */
    public PropertySourceImpl(Map propertyMap, PropertySource propertySource) {
        this();

        // If both parameters are null throw an exception if this constructor
        // is not invoked from within a subclass constructor.
        if(propertyMap == null && propertySource == null) {
            boolean throwException = false;
            try {
                Class rootClass =
                    Class.forName("com.lightcrafts.mediax.jai.PropertySourceImpl");
                throwException = this.getClass().equals(rootClass);
            } catch(Exception e) {
                // Ignore it.
            }
            if(throwException) {
                throw new
                    IllegalArgumentException(JaiI18N.getString("Generic0"));
            }
        }

        if(propertyMap != null) {
            Iterator keys = propertyMap.keySet().iterator();
            while(keys.hasNext()) {
                Object key = keys.next();
                if(key instanceof String) {
                    properties.put(new CaselessStringKey((String)key),
                                   propertyMap.get(key));
                } else if(key instanceof CaselessStringKey) {
                    properties.put((CaselessStringKey)key,
                                   propertyMap.get(key));
                }
            }
        }

        if(propertySource != null) {
            String[] names = propertySource.getPropertyNames();
            if(names != null) {
                int length = names.length;
                for(int i = 0; i < length; i++) {
                    propertySources.put(new CaselessStringKey(names[i]),
                                        propertySource);
                }
            }
        }
    }

    /**
     * Returns an array of <code>String</code>s recognized as names by
     * this property source.  The case of the property names is retained.
     * If no properties are available, <code>null</code> will be returned.
     *
     * @return an array of <code>String</code>s giving the valid
     *         property names or <code>null</code>.
     */
    public String[] getPropertyNames() {
        synchronized(properties) {
            if(properties.size() + propertySources.size() == 0) {
                return null;
            }

            // Create a set from the property name-value mapping.
            Set propertyNames =
            Collections.synchronizedSet(new HashSet(properties.keySet()));

        // Add all names not already in the set.
            propertyNames.addAll(propertySources.keySet());

            // Copy names to an array.
            int length = propertyNames.size();
            String[] names = new String[length];
            Iterator elements = propertyNames.iterator();
            int index = 0;
            while(elements.hasNext() && index < length) { // redundant test
                names[index++] = ((CaselessStringKey)elements.next()).getName();
            }

            return names;
        }
    }

    /**
     * Returns an array of <code>String</code>s recognized as names by
     * this property source that begin with the supplied prefix.  If
     * no property names match, <code>null</code> will be returned.
     * The comparison is done in a case-independent manner.
     *
     * @return an array of <code>String</code>s giving the valid
     * property names.
     *
     * @exception IllegalArgumentException if <code>prefix</code>
     *                                     is <code>null</code>.
     */
    public String[] getPropertyNames(String prefix) {
        return PropertyUtil.getPropertyNames(getPropertyNames(), prefix);
    }

    /**
     * Returns the class expected to be returned by a request for
     * the property with the specified name.  If this information
     * is unavailable, <code>null</code> will be returned.
     * This method queries only the name-value mapping so as to avoid
     * requesting a property value from a <code>PropertySource</code>
     * to which the name might refer via the name-<code>PropertySource</code>
     * mapping.  If it is known from <code>getPropertyNames()</code> that
     * the property is emitted by this <code>PropertySource</code> but this
     * method returns <code>null</code>, then <code>getProperty()</code>
     * will have to be invoked and the <code>Class</code> obtained from
     * the property value itself.
     *
     * @param propertyName the name of the property, as a <code>String</code>.
     *
     * @return The <code>Class</code> expected to be returneded by a
     *         request for the value of this property or <code>null</code>.
     *
     * @exception IllegalArgumentException if <code>propertyName</code>
     *                                     is <code>null</code>.
     */
    public Class getPropertyClass(String propertyName) {
        if(propertyName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        synchronized(properties) {
            Class propertyClass = null;
            Object value = properties.get(new CaselessStringKey(propertyName));
            if(value != null) {
                propertyClass = value.getClass();
            }
            return propertyClass;
        }
    }

    /**
     * Returns the value of a property.  If the property name is not
     * recognized, <code>java.awt.Image.UndefinedProperty</code> will
     * be returned.
     *
     * <p> If the requested name is found in the name-value mapping,
     * the corresponding value will be returned.  Otherwise the
     * name-<code>PropertySource</code> mapping will be queried and the
     * value will be derived from the found <code>PropertySource</code>,
     * if any.  If the value is derived from a <code>PropertySource</code>,
     * a record will be kept of this and this property will be referred to
     * as a "cached property".
     *
     * @param propertyName the name of the property, as a <code>String</code>.
     *
     * @return the value of the property, as an
     *         <code>Object</code>, or the value
     *         <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @exception IllegalArgumentException if <code>propertyName</code>
     *                                     is <code>null</code>.
     */
    public Object getProperty(String propertyName) {
        if(propertyName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        synchronized(properties) {
            CaselessStringKey key = new CaselessStringKey(propertyName);

            // Try to retrieve from value mapping.
            Object value = properties.get(key);

            if(value == null) {
                // Try to retrieve from PropertySource mapping.
                PropertySource propertySource =
                    (PropertySource)propertySources.get(key);
                if(propertySource != null) {
                    value = propertySource.getProperty(propertyName);
                    if(value != java.awt.Image.UndefinedProperty) {
                        // Cache the value and flag it as such.
                        properties.put(key, value);
                        cachedPropertyNames.add(key);
                    }
                } else { // No PropertySource: undefined property.
                    value = java.awt.Image.UndefinedProperty;
                }
            }

            return value;
        }
    }

    /**
     * Copies into a <code>Map</code> all properties currently available
     * via this <code>PropertySource</code>.  All property values are
     * copied by reference rather than by being cloned.  The keys in the
     * <code>Map</code> will be <code>String</code>s with the original
     * property name case intact.  Property values derived from the
     * name-value mapping will take precedence.  The names of properties
     * whose values are derived via the name-<code>PropertySource</code>
     * mapping will be recorded as "cached properties".
     *
     * @return A <code>Map</code> of all properties or <code>null</code> if
     *         none are defined.
     */
    public Map getProperties() {
        if(properties.size() + propertySources.size() == 0) {
            return null;
        }

        synchronized(properties) {
            Hashtable props = null;

            String[] propertyNames = getPropertyNames();
            if(propertyNames != null) {
                int length = propertyNames.length;
                props = new Hashtable(properties.size());
                for(int i = 0; i < length; i++) {
                    String name = propertyNames[i];
                    Object value = getProperty(name);
                    props.put(name, value);
                }
            }

            return props;
        }
    }

    /**
     * Serialize a <code>Map</code> which contains serializable keys.
     */
    private static void writeMap(ObjectOutputStream out, Map map)
        throws IOException{
        // Create an empty Hashtable.
        Hashtable table = new Hashtable();

        // Copy serializable properties to local table.
        Iterator keys = map.keySet().iterator();
        while(keys.hasNext()) {
            Object key = keys.next();
            Object value = map.get(key);
            if(value instanceof Serializable) {
                table.put(key, value);
            }
        }

        // Write serialized form to the stream.
        out.writeObject(table);
    }

    /**
     * Serialize the PropertySourceImpl.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Write serializable fields.
        out.defaultWriteObject();

        synchronized(properties) {
            // Write serializable forms of name-value and
            // name-PropertySource maps.
            writeMap(out, properties);
            writeMap(out, propertySources);
        }
    }

    /**
     * Deserialize the PropertySourceImpl.
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // Read serializable fields.
        in.defaultReadObject();

        // Read serializable forms of name-value and name-PropertySource maps.
        properties = (Map)in.readObject();
        propertySources = (Map)in.readObject();

        // Clean up cached names list: delete names not in deserialized
        // name-value map.
        Iterator names = cachedPropertyNames.iterator();
        Set propertyNames = properties.keySet();
        while(names.hasNext()) {
            if(!propertyNames.contains(names.next())) {
                names.remove(); // remove name from cachedPropertyNames.
            }
        }
    }
}
