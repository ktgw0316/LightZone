/*
 * $RCSfile: WritablePropertySourceImpl.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:25 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.Map;

import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * A utility implementation of the <code>WritablePropertySource</code>
 * interface.  The same internal superclass data structures are used and
 * are supplemented by a <code>PropertyChangeSupportJAI</code> to handle
 * property event firing.  All events fired by an instance of this class
 * will be <code>PropertySourceChangeEvent</code>s with an event source
 * equal to the object used to create the <code>PropertyChangeSupportJAI</code>
 * helper object.  This object is user-specifiable at construction time or
 * automatically created when a <code>PropertyChangeListener</code> is
 * added or removed and the <code>PropertyChangeSupportJAI</code> specified
 * at construction was <code>null</code>.
 *
 * @see CaselessStringKey
 * @see PropertySource
 * @see PropertySourceChangeEvent
 * @see PropertySourceImpl
 * @see WritablePropertySource
 * @see PropertyChangeEmitter
 * @see PropertyChangeSupportJAI
 *
 * @since JAI 1.1
 */
public class WritablePropertySourceImpl extends PropertySourceImpl
    implements WritablePropertySource {

    /**
     * Helper object for bean-style property events.  Its default
     * value is <code>null</code> which indicates that no events
     * are to be fired.
     */
    protected PropertyChangeSupportJAI manager = null;

    /**
     * Constructs a <code>WritablePropertySourceImpl</code> instance with
     * no properties set.
     */
    public WritablePropertySourceImpl() {
        super();
    }

    /**
     * Constructs a <code>WritablePropertySourceImpl</code> instance which
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
     * @param manager The object which will actually fire the events.
     *                May be <code>null</code> in which case a default
     *                event manager will be created as needed with this
     *                object as its event source.
     */
    public WritablePropertySourceImpl(Map propertyMap,
                                      PropertySource source,
                                      PropertyChangeSupportJAI manager) {
        super(propertyMap, source);
        this.manager = manager;
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
     * as a "cached property".  Derivation of the value from a
     * <code>PropertySource</code> rather than from the name-value
     * mapping may cause a <code>PropertySourceChangeEvent<code> to be fired.
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
            // Check whether a value mapping exists.
            boolean isMapped =
                properties.containsKey(new CaselessStringKey(propertyName));

            // Retrieve the value.
            Object value = super.getProperty(propertyName);

            // Fire an event if necessary, i.e., if there is an event manager
            // and the property value was just derived from a PropertySource in
            // the name-PropertySource mapping.
            if(manager != null &&
               !isMapped &&
               value != java.awt.Image.UndefinedProperty) {
                // Value was derived from a PropertySource -> event.
                Object eventSource = 
                    manager.getPropertyChangeEventSource();
                PropertySourceChangeEvent evt =
                    new PropertySourceChangeEvent(eventSource, propertyName,
                                                  java.awt.Image.UndefinedProperty,
                                                  value);
                manager.firePropertyChange(evt);
            }

            return value;
        }
    }

    /**
     * Adds the property value associated with the supplied name to
     * the <code>PropertySource</code>.  Values set by this means will
     * supersede any previously defined value of the property.
     *
     * <p> <code>PropertySourceChangeEvent<code>s may be fired
     * with a name set to that of the property (retaining case) and old and
     * new values set to the previous and current values of the property,
     * respectively.  The value returned by the <code>getSource()</code>
     * method of the event is determined by the value passed to the
     * constructor of the <code>PropertyChangeSupportJAI</code> parameter.
     * Neither the old nor the new value may be <code>null</code>: undefined
     * properties must as usual be indicated by the constant value
     * <code>java.awt.Image.UndefinedProperty</code>.  It is however
     * legal for either but not both of the old and new property values
     * to equal <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @param propertyName the name of the property, as a <code>String</code>.
     * @param propertyValue the property, as a general <code>Object</code>.
     *
     * @exception IllegalArgumentException if <code>propertyName</code>
     *                                     or <code>propertyValue</code>
     *                                     is <code>null</code>.
     */
    public void setProperty(String propertyName,
                            Object propertyValue) {
        if(propertyName == null || propertyValue == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        synchronized(properties) {
            CaselessStringKey key = new CaselessStringKey(propertyName);

            // Set the entry in the name-value mapping.
            Object oldValue = properties.put(key, propertyValue);
            if(oldValue == null) {
                oldValue = java.awt.Image.UndefinedProperty;
            }

            // Suppress the name if present in the cached properties listing.
            cachedPropertyNames.remove(key);

            if(manager != null && !oldValue.equals(propertyValue)) {
                Object eventSource = 
                    manager.getPropertyChangeEventSource();
                PropertySourceChangeEvent evt =
                    new PropertySourceChangeEvent(eventSource, propertyName,
                                                  oldValue, propertyValue);
                manager.firePropertyChange(evt);
            }
        }
    }

    /**
     * Removes the named property from the <code>PropertySource</code>.
     * <code>PropertySourceChangeEvent<code>s may be fired if the property
     * values is actually removed from the name-value mapping.  In this case
     * the new value of the property event will be
     * <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @param propertyName the name of the property, as a <code>String</code>.
     * @param propertyValue the property, as a general <code>Object</code>.
     *
     * @exception IllegalArgumentException if <code>propertyName</code>
     *                                     is <code>null</code>.
     */
    public void removeProperty(String propertyName) {
        if(propertyName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        synchronized(properties) {
            CaselessStringKey key = new CaselessStringKey(propertyName);

            // Remove the entry from the name-value mapping and save its value.
            Object oldValue = properties.remove(key);

            // Remove the entry from the name-PropertySource mapping and from
            // the list of cached properties.
            propertySources.remove(key);
            cachedPropertyNames.remove(key);

            if(manager != null && oldValue != null) {
                Object eventSource = 
                    manager.getPropertyChangeEventSource();
                PropertySourceChangeEvent evt =
                    new PropertySourceChangeEvent(eventSource, propertyName,
                                                  oldValue,
                                                  java.awt.Image.UndefinedProperty);
                manager.firePropertyChange(evt);
            }
        }
    }

    /**
     * Copies from the supplied <code>Map</code> to the property set all
     * entries wherein the key is an instance of <code>String</code>
     * or <code>CaselessStringKey</code>.  Values set by this means will
     * supersede any previously defined values of the respective properties.
     * All property values are copied by reference.  
     * <code>PropertySourceChangeEvent<code>s may be fired for each
     * property added.  If the property was not previously defined the
     * old value of the property event will be
     * <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @param propertyMap A <code>Map</code> from which to copy properties
     *        which have keys which are either <code>String</code>s or
     *        <code>CaselessStringKey</code>s.  If <code>null</code> no
     *        properties will be added.
     */
    public void addProperties(Map propertyMap) {
        if(propertyMap != null) {
            synchronized(properties) {
                Iterator keys = propertyMap.keySet().iterator();
                while(keys.hasNext()) {
                    Object key = keys.next();
                    if(key instanceof String) {
                        setProperty((String)key,
                                    propertyMap.get(key));
                    } else if(key instanceof CaselessStringKey) {
                        setProperty(((CaselessStringKey)key).getName(),
                                    propertyMap.get(key));
                    }
                }
            }
        }
    }

    /**
     * Adds a <code>PropertySource</code> to the
     * name-<code>PropertySource</code> mapping.  The
     * actual property values are not requested at this time but instead
     * an entry for the name of each property emitted by the
     * <code>PropertySource</code> is added to the
     * name-<code>PropertySource</code> mapping.  Properties defined by
     * this <code>PropertySource</code> supersede those of all other
     * previously added <code>PropertySource</code>s including that
     * specified at construction, if any.  Note that this method will not
     * provoke any events as no properties will actually have changed.
     *
     * @param propertySource A <code>PropertySource</code> from which to
     *        derive properties.  If <code>null</code> nothing is done.
     */
    public void addProperties(PropertySource propertySource) {
        if(propertySource != null) {
            synchronized(properties) {
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
    }

    /**
     * Clears all properties from the <code>PropertySource</code> by
     * invoking <code>removeProperty()</code> with all known names.
     * <code>PropertySourceChangeEvent<code>s may be fired
     * for each stored property removed.  In this case the new value
     * of the property event will be
     * <code>java.awt.Image.UndefinedProperty</code>.
     */
    public void clearProperties() {
        synchronized(properties) {
            String[] names = getPropertyNames();
            if(names != null) {
                int length = names.length;
                for(int i = 0; i < length; i++) {
                    removeProperty(names[i]);
                }
            }
        }
    }

    /**
     * Clears the name-value mapping of all properties.
     * <code>PropertySourceChangeEvent<code>s may be fired
     * for each stored property removed.  In this case the new value
     * of the property event will be
     * <code>java.awt.Image.UndefinedProperty</code>.
     */
    public void clearPropertyMap() {
        synchronized(properties) {
            Iterator keys = properties.keySet().iterator();
            while(keys.hasNext()) {
                CaselessStringKey key = (CaselessStringKey)keys.next();
                Object oldValue = properties.get(key);
                keys.remove();

                if(manager != null) {
                    Object eventSource = 
                        manager.getPropertyChangeEventSource();
                    PropertySourceChangeEvent evt =
                        new PropertySourceChangeEvent(eventSource,
                                                      key.getName(),
                                                      oldValue,
                                                      java.awt.Image.UndefinedProperty);
                    manager.firePropertyChange(evt);
                }
            }

            // Also clear cached names.
            cachedPropertyNames.clear();
        }
    }

    /**
     * Clears the name-<code>PropertySource</code> mapping.
     * No events will be fired.
     */
    public void clearPropertySourceMap() {
        synchronized(properties) {
            propertySources.clear();
        }
    }

    /**
     * Clears from the name-value mapping all properties which were
     * derived from a source in the name-<code>PropertySource</code> mapping.
     * <code>PropertySourceChangeEvent<code>s may be fired
     * for each stored property removed.  In this case the new value
     * of the property event will be
     * <code>java.awt.Image.UndefinedProperty</code>.
     */
    public void clearCachedProperties() {
        synchronized(properties) {
            Iterator names = cachedPropertyNames.iterator();
            while(names.hasNext()) {
                CaselessStringKey name = (CaselessStringKey)names.next();
                Object oldValue = properties.remove(name);
                names.remove(); // remove name from cachedPropertyNames.
                if(manager != null) {
                    Object eventSource = 
                        manager.getPropertyChangeEventSource();
                    PropertySourceChangeEvent evt =
                        new PropertySourceChangeEvent(eventSource,
                                                      name.getName(),
                                                      oldValue,
                                                      java.awt.Image.UndefinedProperty);
                    manager.firePropertyChange(evt);
                }
            }
        }
    }

    /**
     * Removes from the name-<code>PropertySource</code> mapping all entries
     * which refer to the supplied <code>PropertySource</code>.
     */
    public void removePropertySource(PropertySource propertySource) {
        synchronized(properties) {
            Iterator keys = propertySources.keySet().iterator();
            while(keys.hasNext()) {
                Object ps = propertySources.get(keys.next());
                if(ps.equals(propertySource)) {
                    keys.remove(); // remove this entry from propertySources.
                }
            }
        }
    }

    /**
     * Add a <code>PropertyChangeListener</code> to the listener list. The
     * listener is registered for all properties.
     *
     * <p> If the property event utility object was not set at construction,
     * then it will be initialized to a <code>PropertyChangeSupportJAI</code> 
     * whose property event source is this object.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getEventManager().addPropertyChangeListener(listener);
    }

    /**
     * Add a <code>PropertyChangeListener</code> for a specific property. The
     * listener will be invoked only when a call on
     * firePropertyChange names that specific property.
     *
     * <p> If the property event utility object was not set at construction,
     * then it will be initialized to a <code>PropertyChangeSupportJAI</code> 
     * whose property event source is this object.
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        getEventManager().addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a <code>PropertyChangeListener</code> from the listener list.
     * This removes a <code>PropertyChangeListener</code> that was registered
     * for all properties.
     *
     * <p> If the property event utility object was not set at construction,
     * then it will be initialized to a <code>PropertyChangeSupportJAI</code> 
     * whose property event source is this object.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        getEventManager().removePropertyChangeListener(listener);
    }

    /**
     * Remove a <code>PropertyChangeListener</code> for a specific property.
     *
     * <p> If the property event utility object was not set at construction,
     * then it will be initialized to a <code>PropertyChangeSupportJAI</code> 
     * whose property event source is this object.
     */
    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener listener) {
        getEventManager().removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Returns the utility property event manager.  If none has been set,
     * initialize it to one whose source is this object.
     */
    private PropertyChangeSupportJAI getEventManager() {
        if(manager == null) {
            synchronized(this) {
                manager = new PropertyChangeSupportJAI(this);
            }
        }
        return manager;
    }
}
