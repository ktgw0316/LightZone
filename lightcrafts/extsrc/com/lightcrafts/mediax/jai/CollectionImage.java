/*
 * $RCSfile: CollectionImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:05 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.lightcrafts.media.jai.util.PropertyUtil;

/**
 * An abstract superclass for classes representing a <code>Collection</code>
 * of images.  It may be a <code>Collection</code> of
 * <code>RenderedImage</code>s or <code>RenderableImage</code>s, a
 * <code>Collection</code> of <code>Collection</code>s that include images.
 * In other words, this class supports nested <code>Collection</code>s, but
 * at the very bottom, there must be images associated with the
 * <code>Collection</code> objects.
 *
 *
 */
public abstract class CollectionImage implements ImageJAI, Collection {

    /**
     * A <code>Collection</code> of objects.  It may be a
     * <code>Collection</code> of images of the same type, a
     * <code>Collection</code> of objects of the same type, each
     * containing an image, or a <code>Collection</code> of
     * <code>Collection</code>s whose leaf objects are images
     * or objects that contain images.
     */
    protected Collection imageCollection;

    /**
     * The <code>CollectionImageFactory</code> which created this
     * <code>CollectionImage</code>; may be <code>null</code> which
     * implies that the <code>CollectionImage</code> was not created
     * by a <code>CollectionImageFactory</code>.
     *
     * @since JAI 1.1
     */
    protected CollectionImageFactory imageFactory;
    private Boolean isFactorySet = Boolean.FALSE;

    /**
     * A helper object to manage firing events.
     *
     * @since JAI 1.1
     */
    protected PropertyChangeSupportJAI eventManager = null;

    /**
     * A helper object to manage the image properties.
     *
     * @since JAI 1.1
     */
    protected WritablePropertySourceImpl properties = null;
    
    /**
     * A <code>Set</code> of <code>WeakReference</code>s to the
     * sinks of this <code>CollectionImage</code>.
     *
     * @since JAI 1.1
     */
    protected Set sinks;

    /**
     * Default constructor.  The <code>imageCollection</code> parameter is
     * <code>null</code>.  Subclasses that use this constructor must either
     * set the <code>imageCollection</code> parameter themselves, or override
     * the methods defined in the <code>Collection</code> interface.
     * Otherwise, a <code>NullPointerException</code> may be thrown at a later
     * time when methods which use to the <code>imageCollection</code>
     * instance variable are invoked.
     */
    protected CollectionImage() {
        eventManager = new PropertyChangeSupportJAI(this);
        properties = new WritablePropertySourceImpl(null, null, eventManager);
    }

    /**
     * Constructs a class that contains an image <code>Collection</code>.
     *
     * @param collection  A <code>Collection</code> of objects that
     * include images.
     *
     * @throws IllegalArgumentException if <code>collection</code> is
     *         <code>null</code>.
     */
    public CollectionImage(Collection collection) {
        this();


        if ( collection == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        imageCollection = collection;
    }

    /* ----- Element retrieval method. ----- */

    /**
     * Returns the element at the given index in <code>imageCollection</code>.
     * If <code>imageCollection</code> is a <code>List</code> then the call is
     * forwarded to <code>imageCollection</code>; otherwise an array is created
     * by applying <code>toArray()</code> to <code>imageCollection</code> and
     * the indicated element of that array is returned.  Note that in the
     * latter case no guarantee as to element ordering beyond that stated in
     * the specification of <code>Collection.toArray()</code>.
     *
     * @param index The index of the desired element.
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<code>index</code> &lt; 0 || <code>index</code> &ge;
     *         <code>imageCollection.size()</code>).
     *
     * @since JAI 1.1
     */
    public Object get(int index) {
        if(index < 0 || index >= imageCollection.size()) {
            throw new IndexOutOfBoundsException(); // No message needed.
        }

        if(imageCollection instanceof List) {
            return ((List)imageCollection).get(index);
        } else {
            return imageCollection.toArray((Object[])null)[index];
        }
    }

    /* ----- Image factory methods. ----- */

    /**
     * Sets the <code>imageFactory</code> instance variable to the supplied
     * value.  The parameter may be <code>null</code>.  It is recommended
     * that this method be invoked as soon as the <code>CollectionImage</code>
     * is constructed.
     *
     * @param imageFactory The creating <code>CollectionImageFactory</code> or
     *        <code>null</code>
     * @throws IllegalStateException if the corresponding instance variable
     *                               was already set.
     *
     * @since JAI 1.1
     */
    public void setImageFactory(CollectionImageFactory imageFactory) {
        synchronized(isFactorySet) {
            if(isFactorySet.booleanValue()) {
                throw new IllegalStateException();
            }
            this.imageFactory = imageFactory;
            isFactorySet = Boolean.TRUE;
        }
    }

    /**
     * If this <code>CollectionImage</code> was created by a
     * <code>CollectionImageFactory</code> then return a reference to
     * that factory; otherwise return <code>null</code>.
     *
     * @since JAI 1.1
     */
    public CollectionImageFactory getImageFactory() {
        synchronized(isFactorySet) {
            return imageFactory;
        }
    }

    /* ----- Sink methods. ----- */

    /**
     * Adds a sink to the set of sinks.
     *
     * @since JAI 1.1
     */
    public synchronized boolean addSink(Object sink) {
        if(sink == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if(sinks == null) {
            sinks = new HashSet();
        }

        return sinks.add(new WeakReference(sink));
    }

    /**
     * Removes a sink from the set of sinks.
     *
     * @return <code>true</code> if and only if the set of sinks
     * changed as a result of the call.
     *
     * @since JAI 1.1
     */
    public synchronized boolean removeSink(Object sink) {
        if (sink == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sinks == null) {
            return false;
        }

        boolean result = false;
        Iterator it = sinks.iterator();
        while(it.hasNext()) {
            Object referent = ((WeakReference)it.next()).get();
            if(referent == sink) {
                // Remove the sink.
                it.remove();
                result = true;
                // Do not break: could be more than one.
            } else if(referent == null) {
                // A cleared reference: might as well remove it.
                it.remove(); // ignore return value here.
            }
        }

        return result;

    }

    /**
     * Retrieves the set of sinks or <code>null</code> if
     * there are none.
     *
     * @since JAI 1.1
     */
    public synchronized Set getSinks() {
        Set v = null;

        if (sinks != null && sinks.size() > 0) {
            v = new HashSet(sinks.size());

            Iterator it = sinks.iterator();
            while(it.hasNext()) {
                Object o = ((WeakReference)it.next()).get();

                if (o != null) {
                    v.add(o);
                }
            }

            if (v.size() == 0) {
                v = null;
            }
        }

        return v;
    }

    /**
     * Removes all sinks from the set of sinks.
     *
     * @since JAI 1.1
     */
    public synchronized void removeSinks() {
        sinks = null;
    }

    /* ----- WritablePropertySource methods. ----- */

    /**
     * Returns an array of <code>String</code>s recognized as names by this
     * property source.  If no property names match, <code>null</code>
     * will be returned.
     *
     * @return An array of <code>String</code>s which are the valid
     * property names or <code>null</code> if there are none.
     */
    public String[] getPropertyNames() {
        return properties.getPropertyNames();
    }

    /**
     * Returns an array of <code>String</code>s recognized as names by
     * this property source that begin with the supplied prefix.  If
     * no property names are recognized, or no property names match,
     * <code>null</code> will be returned.
     * The comparison is done in a case-independent manner.
     *
     * <p> The default implementation calls
     * <code>getPropertyNames()</code> and searches the list of names
     * for matches.
     *
     * @return An array of <code>String</code>s giving the valid
     *         property names or <code>null</code> if there are none.
     *
     * @throws <code>IllegalArgumentException</code> if <code>prefix</code>
     * is <code>null</code>.
     */
    public String[] getPropertyNames(String prefix) {
        return PropertyUtil.getPropertyNames(getPropertyNames(), prefix);
    }

    /**
     * Returns the class expected to be returned by a request for
     * the property with the specified name.  If this information
     * is unavailable, <code>null</code> will be returned.
     *
     * @return The <code>Class</code> expected to be return by a
     *         request for the value of this property or <code>null</code>.
     *
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public Class getPropertyClass(String name) {
        return properties.getPropertyClass(name);
    }

    /**
     * Returns the specified property.  The default implementation
     * returns <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     */
    public Object getProperty(String name) {
        return properties.getProperty(name);
    }

    /**
     * Returns the specified property.  The default implementation
     * returns <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     * @deprecated as of JAI 1.1.
     */
    public Object getProperty(String name, Collection collection) {
        return Image.UndefinedProperty;
    }

    /**
     * Sets a property on a <code>CollectionImage</code>.  Some
     * <code>CollectionImage</code> subclasses may ignore attempts to set
     * properties.
     *
     * @param name a <code>String</code> containing the property's name.
     * @param value the property, as a general <code>Object</code>.
     *
     * @throws IllegalArgumentException  If <code>name</code> or 
     *         <code>value</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public void setProperty(String name, Object value) {
        properties.setProperty(name, value);
    }

    /**
     * Removes the named property from the <code>CollectionImage</code>.
     * Some <code>CollectionImage</code> subclasses may ignore attempts to
     * remove properties.
     *
     * @since JAI 1.1
     */
    public void removeProperty(String name) {
        properties.removeProperty(name);
    }

    /* ----- PropertyChangeEmitter methods. ----- */

    /**
     * Add a PropertyChangeListener to the listener list. The
     * listener is registered for all properties.
     *
     * @since JAI 1.1
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        eventManager.addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property. The
     * listener will be invoked only when a call on
     * firePropertyChange names that specific property.  The case of
     * the name is ignored.
     *
     * @since JAI 1.1
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        eventManager.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener from the listener list. This
     * removes a PropertyChangeListener that was registered for all
     * properties.
     *
     * @since JAI 1.1
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        eventManager.removePropertyChangeListener(listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.  The case
     * of the name is ignored.
     *
     * @since JAI 1.1
     */
    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener listener) {
        eventManager.removePropertyChangeListener(propertyName, listener);
    }

    /* ----- Collection methods. ----- */

    /** Returns the number of elements in this <code>Collection</code>. */
    public int size() {
        return imageCollection.size();
    }

    /**
     * Returns <code>true</code> if this <code>Collection</code>
     * contains no elements.
     */
    public boolean isEmpty() {
        return imageCollection.isEmpty();
    }

    /**
     * Returns <code>true</code> if this <code>Collection</code>
     * contains the specified object.
     */
    public boolean contains(Object o) {
        return imageCollection.contains(o);
    }

    /**
     * Returns an <code>Iterator</code> over the elements in this
     * <code>Collection</code>.
     */
    public Iterator iterator() {
        return imageCollection.iterator();
    }

    /**
     * Returns an array containing all of the elements in this
     * <code>Collection</code>.
     */
    public Object[] toArray() {
        return imageCollection.toArray();
    }

    /**
     * Returns an array containing all of the elements in this collection
     * whose runtime type is that of the specified array.
     *
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in this
     *         <code>Collection</code>.
     */
    public Object[] toArray(Object[] a) {
        return imageCollection.toArray(a);
    }

    /**
     * Adds the specified object to this <code>Collection</code>.
     *
     * @return <code>true</code> if and only if the parameter is added to the
     *         <code>Collection</code>.
     */
    public boolean add(Object o) {
        return imageCollection.add(o);
    }

    /**
     * Removes the specified object from this <code>Collection</code>.
     *
     * @return <code>true</code> if and only if the parameter is removed
     *         from the <code>Collection</code>.
     */
    public boolean remove(Object o) {
        return imageCollection.remove(o);
    }

    /**
     * Returns <code>true</code> if this <code>Collection</code> contains
     * all of the elements in the specified <code>Collection</code>.
     */
    public boolean containsAll(Collection c) {
        return imageCollection.containsAll(c);
    }

    /**
     * Adds all of the elements in the specified <code>Collection</code>
     * to this <code>Collection</code>.
     *
     * @return <code>true</code> if this <code>Collection</code> changed
     * as a result of the call.
     */
    public boolean addAll(Collection c) {
        return imageCollection.addAll(c);
    }

    /**
     * Removes all this collection's elements that are also contained in the
     * specified <code>Collection</code>.
     *
     * @return <code>true</code> if this <code>Collection</code> changed
     * as a result of the call.
     */
    public boolean removeAll(Collection c) {
        return imageCollection.removeAll(c);
    }

    /**
     * Retains only the elements in this <code>Collection</code> that are
     * contained in the specified <code>Collection</code>.
     *
     * @return <code>true</code> if this <code>Collection</code> changed
     * as a result of the call.
     */
    public boolean retainAll(Collection c) {
        return imageCollection.retainAll(c);
    }

    /** Removes all of the elements from this <code>Collection</code>. */
    public void clear() {
        imageCollection.clear();
    }
}
