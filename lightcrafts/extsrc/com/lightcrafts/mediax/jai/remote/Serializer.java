/*
 * $RCSfile: Serializer.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:53 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.remote;

import java.awt.RenderingHints;

/**
 * An interface to be implemented by classes which are capable of
 * converting an instance of a supported class into a state representation
 * of the object which is serializable.  Supported classes may in fact be
 * classes or interfaces.
 *
 * <p> When possible, classes (not interfaces) should be supported explicitly,
 * i.e., the <code>getObjectClass()</code> method of the
 * <code>SerializableState</code> returned by <code>getState(Object)</code>
 * should return the same value which would be returned by invoking
 * <code>getClass()</code> on the object passed to
 * <code>getState(Object)</code>.  In particular, whenever feasible a
 * <code>Serializer</code> should not be used to serialize a given object
 * into a form which when deserialized would yield an instance of the
 * superclass of the class of which the object is an instance.  When it is
 * not possible to provide a class-specific <code>Serializer</code>, such
 * as when a factory class generate subclasses of itself via
 * factory methods, then <code>permitsSubclasses()</code> should return
 * <code>true</code>; in the case of class-specific <code>Serializer</code>s
 * it should return <code>false</code>.
 *
 * @see SerializableState
 * @see java.io.Serializable
 *
 * @since JAI 1.1
 */
public interface Serializer {

    /**
     * Returns the class or interface which is supported by this
     * <code>Serializer</code>.
     *
     * @return The supported <code>Class</code>.
     */
    Class getSupportedClass();

    /**
     * Returns <code>true</code> if and only if it is legal for this
     * <code>Serializer</code> to be used to serialize a subclass.  In
     * general this method should return <code>false</code>, i.e., a specific
     * <code>Serializer</code> should be registered explicitly for each class.
     * In some cases this is however not expedient.  An example of this is a
     * <code>Serializer</code> of classes created by a factory class: the
     * exact subclasses may not be known but sufficient information may be
     * able to be extracted from the subclass instance to permit its
     * serialization by the <code>Serializer</code> registered for the factory
     * class.
     */
    boolean permitsSubclasses();

    /**
     * Converts an object into a state-preserving object which may be
     * serialized.  If the value returned by <code>getSupportedClass()</code>
     * is a class, i.e., not an interface, then the parameter of
     * <code>getState()</code> should be an instance of that class; if
     * <code>getSupportedClass()</code> returns an interface, then the
     * parameter of <code>getState()</code> should implement that interface.
     * If the apposite condition is not satisfied then an
     * <code>IllegalArgumentException</code> will be thrown.
     *
     * <p> If the class of the parameter is supported explicitly, i.e., is an
     * instance of the class returned by <code>getSupportedClass()</code>, then
     * the object returned by the <code>getObject()</code> method of the
     * generated <code>SerializableState</code> will be an instance of the
     * same class.  If <code>getSupportedClass()</code> returns an interface
     * which the class of the parameter implements, then the object returned
     * by the <code>getObject()</code> method of the generated
     * <code>SerializableState</code> will be an instance of an unspecified
     * class which implements that interface.
     *
     * @param o The object to be converted into a serializable form.
     * @param h Configuration parameters the exact nature of which is
     *          <code>Serializer</code>-dependent.  If <code>null</code>,
     *          reasonable default settings should be used.
     * @return A serializable form of the supplied object.
     * @exception IllegalArgumentException if <code>o</code> is
     *            <code>null</code>, the supported class is an interface
     *            not implemented by the class of which <code>o</code> is
     *            an instance, or the supported class is not an interface
     *            and <code>getSupportedClass().equals(o.getClass())</code>
     *            returns <code>false</code>.
     */
    SerializableState getState(Object o, RenderingHints h);
}
