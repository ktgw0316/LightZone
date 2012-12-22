/*
 * $RCSfile: DeferredData.java,v $
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

import java.io.Serializable;
import java.util.Observable;

/**
 * Class to be used as a wrapper for data which will be calculated
 * at a later time.  For example, an instance of a subclass of this
 * class may be passed as a parameter in the <code>ParameterBlock</code>
 * set on a <code>RenderedOp</code> node.  The data wrapped by the
 * <code>DeferredData</code> object will not however actually be
 * requested until the node is rendered.
 *
 * @see DeferredProperty
 * @see RenderedOp
 *
 * @since JAI 1.1
 */
public abstract class DeferredData extends Observable implements Serializable {
    /**
     * The class of the wrapped data.
     */
    protected Class dataClass;

    /**
     * The data wrapped by this class.  This field is marked
     * <code>transient</code> so that subclasses are obligated to provide
     * specific serialization methods if they desire that this field be
     * serialized.
     */
    protected transient Object data;

    /**
     * Creates a <code>DeferredData</code> wrapper for an object of the
     * indicated class.  The parameter must be non-<code>null</code> or an
     * <code>IllegalArgumentException</code> will be thrown.
     *
     * @throws IllegalArgumentException if <code>dataClass</code> is
     *                                  <code>null</code>.
     */
    protected DeferredData(Class dataClass) {
        if(dataClass == null) {
            throw new IllegalArgumentException(JaiI18N.getString("DeferredData0"));
        }
        this.dataClass = dataClass;
    }

    /**
     * Returns the class of the object wrapped by this DeferredData.
     */
    public Class getDataClass() {
        return dataClass;
    }

    /**
     * Whether the data value has already been computed.
     *
     * @return <code>true</code> if and inly if the internal data value is
     *         non-<code>null</code>, i.e., has already been computed.
     */
    public boolean isValid() {
        return data != null;
    }

    /**
     * Computes the value of the data object wrapped by this object.
     * The returned object must be an instance of a class assignable
     * to the class specified at construction.
     */
    protected abstract Object computeData();

    /**
     * Returns the object wrapped by this <code>DeferredData</code>.  If
     * the data have not yet been computed, <code>computeData()</code>
     * will be invoked to provide the data.  If <code>computeData()</code>
     * is invoked, then <code>setData()</code> will be invoked with
     * its parameter set to the value returned by <code>computeData()</code>.
     */
    public synchronized final Object getData() {
        if(data == null) {
            setData(computeData());
        }
        return data;
    }

    /**
     * Sets the instance variable associated with the data to the supplied
     * parameter.  If the parameter is non-<code>null</code> and not an
     * instance of the class specified at construction, an
     * <code>IllegalArgumentException</code> will be thrown.  If the
     * supplied parameter differs from the current data value, then
     * <code>setChanged()</code> will be invoked and
     * <code>notifyObservers()</code> will be called with its argument set
     * to the previous value of the data object.  This implies that the
     * <code>update()</code> method of any registered <code>Observer</code>s
     * will be invoked with the <code>Observable</code> parameter set to this
     * <code>DeferredData</code> object and its <code>Object</code> parameter
     * set to the previous value of the <code>data</code> field of this
     * <code>DeferredData</code> instance.  The current value of the
     * object can be retrieved by an <code>Observer</code> by casting the
     * <code>Observable</code> parameter of <code>update()</code> to
     * <code>DeferredData</code> and invoking <code>getData()</code>.  To
     * avoid provoking computation of deferred data by calling
     * <code>getData()</code>, <code>isValid()</code> could first be called
     * to determine whether <code>data</code> is non-<code>null</code>.
     * If an <code>Observer</code> detects that the data of the observed
     * <code>DeferredData</code> object is invalid, i.e., <code>null</code>,
     * then this indicates that the data should be re-requested by invoking
     * <code>getData()</code>.
     *
     * @throws IllegalArgumentException if <code>data</code> is
     *         non-<code>null</code> and not an instance of the class
     *         specified at construction.
     */
    protected final void setData(Object data) {
        if(data != null && !dataClass.isInstance(data)) {
            throw new IllegalArgumentException(JaiI18N.getString("DeferredData1"));
        }
        if(this.data == null || !this.data.equals(data)) {
            Object oldData = this.data;
            this.data = data;
            setChanged();
            notifyObservers(oldData);
        }
    }
}
