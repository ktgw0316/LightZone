/*
 * $RCSfile: DeferredProperty.java,v $
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A subclass of <code>DeferredData</code> to be used to wrap JAI property
 * values which will be computed at a later time.  For example, an instance
 * of this class could be used to wrap a property emitted by an operation
 * node so that the actual computation of the property value was deferred
 * until it was actually needed.
 *
 * @see DeferredData
 * @see RenderedOp
 *
 * @since JAI 1.1
 */
public class DeferredProperty extends DeferredData
    implements PropertyChangeListener {

    /**
     * The <code>PropertySource</code> from which the value of the named
     * property is to be drawn.
     */
    protected transient PropertySource propertySource;

    /**
     * The name of the property the value of which is to be obtained.
     */
    protected String propertyName;

    /**
     * Creates a <code>DeferredProperty</code>.  If the specified
     * <code>PropertySource</code> is a <code>PropertyChangeEmitter</code>,
     * then this <code>DeferredProperty</code> object is registered as a
     * <code>PropertyChangeListener</code> of the <code>PropertySource</code>.
     *
     * @exception IllegalArgumentException if a parameter is <code>null</code>
     *            or if the <code>propertyName</code> is not among those
     *            emitted by the <code>PropertySource</code>.
     */
    public DeferredProperty(PropertySource propertySource,
                            String propertyName,
                            Class propertyClass) {
        super(propertyClass);

        if(propertySource == null || propertyName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("DeferredData0"));
        }

        String[] propertyNames = propertySource.getPropertyNames();
        boolean isPropertyEmitted = false;
        if(propertyNames != null) {
            int length = propertyNames.length;
            for(int i = 0; i < length; i++) {
                if(propertyName.equalsIgnoreCase(propertyNames[i])) {
                    isPropertyEmitted = true;
                    break;
                }
            }
        }

        if(!isPropertyEmitted) {
            throw new IllegalArgumentException(JaiI18N.getString("DeferredProperty0"));
        }

        if(propertySource instanceof PropertyChangeEmitter) {
            PropertyChangeEmitter pce =
                (PropertyChangeEmitter)propertySource;
            pce.addPropertyChangeListener(propertyName, this);
        }

        this.propertySource = propertySource; 
        this.propertyName = propertyName; 
    }

    /**
     * Returns the <code>PropertySource</code> of the property value.
     */
    public PropertySource getPropertySource() {
        return propertySource;
    }

    /**
     * Returns the name of the property the value of which is to be obtained.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the value of the image property associated with the
     * image and property name specified at construction.
     */
    protected Object computeData() {
        return propertySource.getProperty(propertyName);
    }

    /**
     * Tests whether the parameter equals this object.  Equality obtains
     * if the parameter object is a <code>DeferredProperty</code> and
     * the respective <code>propertySource</code> and
     * <code>dataClass</code> variables are equal according to their
     * respective <code>equals()</code> methods and the
     * <code>propertyName</code>s are equal ignoring case.  The wrapped
     * data object is not tested unless the <code>isValid()</code> of
     * both objects returns <code>true</code> because requesting it via
     * <code>getData()</code> may provoke computation of a deferred quantity.
     */
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof DeferredProperty)) {
            return false;
        }

        DeferredProperty dp = (DeferredProperty)obj;

        return propertyName.equalsIgnoreCase(dp.getPropertyName()) &&
            propertySource.equals(dp.getPropertySource()) &&
            (!isValid() || !dp.isValid() || data.equals(dp.getData()));
    }

    /**
     * Returns a hash code value for the object.
     */
    public int hashCode() {
        return propertySource.hashCode() ^
            propertyName.toLowerCase().hashCode();
    }

    /**
     * The implementation of <code>PropertyChangeListener</code>.  This
     * method responds to certain <code>PropertyChangeEvent</code>s generated
     * by the <code>PropertySource</code> used to construct this object.
     *
     * <p> If the <code>PropertyChangeEvent</code> is named "Rendering" and is
     * an instance of <code>com.lightcrafts.mediax.jai.RenderingChangeEvent</code>, then
     * the source of the event is checked to determine whether it equals the
     * <code>PropertySource</code> used to construct this object.  If this test
     * is passed then the <code>PropertySource</code> is a
     * <code>RenderedOp</code> the rendering of which has changed.  Therefore
     * setData() will be invoked with a null argument.  This will indicate to
     * any registered observers that the property data should be re-requested
     * by invoking getData() on this object.
     *
     * <p> If the <code>PropertyChangeEvent</code> was generated by the
     * <code>PropertySource</code> used to construct this object, has name
     * equal to the name of the deferred property, and is an instance of
     * <code>PropertySourceChangeEvent</code>, then the value returned by
     * the <code>getNewValue()</code> method of the event object will be
     * passed to <code>setData()</code> unless <code>getNewValue()</code>
     * returns <code>java.awt.Image.UndefinedProperty</code> in which case
     * <code>null</code> will be passed to <code>setData()</code>.  Registered
     * observers will be notified according to the specification of
     * <code>setData()</code>.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getSource() == propertySource) {
            if(evt instanceof RenderingChangeEvent) {
                setData(null);
            } else if(evt instanceof PropertySourceChangeEvent &&
                      propertyName.equalsIgnoreCase(evt.getPropertyName())) {
                Object newValue = evt.getNewValue();
                setData(newValue == java.awt.Image.UndefinedProperty ?
                        null : newValue);
            }
        }
    }
}
