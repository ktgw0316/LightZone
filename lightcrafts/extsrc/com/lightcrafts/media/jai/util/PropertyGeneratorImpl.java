/*
 * $RCSfile: PropertyGeneratorImpl.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:01 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.util;

import com.lightcrafts.mediax.jai.PropertyGenerator;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;

/**
 * This utility class provides an implementation of the
 * <code>PropertyGenerator</code> interface that is suitable for
 * extending.  In addition to providing a no-arg constructor which
 * passes the appropriate arrays, subclasses need only override the
 * method <code>getProperty(String,Object)</code> in which the method
 * <code>validate()</code> should be invoked as the first statement.
 *
 * @see PropertyGenerator
 */
public abstract class PropertyGeneratorImpl implements PropertyGenerator {

    private String[] propertyNames;

    private Class[] propertyClasses;

    private Class[] supportedOpClasses;

    /**
     * Constructs a <code>PropertyGeneratorImpl</code>.  All parameters are
     * saved by reference.
     *
     * @param propertyNames Names of emitted properties.
     * @param propertyClasses Classes of emitted properties.
     * @param supportedOpClasses Classes of supported op nodes.
     * @exception IllegalArgumentException if any of the array parameters
     *            is <code>null</code>.
     * @exception IllegalArgumentException if any of the array parameters
     *            has length zero.
     * @exception IllegalArgumentException if the lengths of the arrays
     *            <code>propertyNames</code> and <code>propertyClasses</code>
     *            are unequal.
     */
    protected PropertyGeneratorImpl(String[] propertyNames,
                                    Class[] propertyClasses,
                                    Class[] supportedOpClasses) {
        if(propertyNames == null ||
           propertyClasses == null ||
           supportedOpClasses == null) {
            throw new IllegalArgumentException(JaiI18N.getString("PropertyGeneratorImpl0"));
        } else if(propertyNames.length == 0 ||
                  propertyClasses.length == 0 ||
                  supportedOpClasses.length == 0) {
            throw new IllegalArgumentException(JaiI18N.getString("PropertyGeneratorImpl1"));
        } else if(propertyNames.length != propertyClasses.length) {
            throw new IllegalArgumentException(JaiI18N.getString("PropertyGeneratorImpl2"));
        }

        for(int i = 0; i < propertyClasses.length; i++) {
            if(propertyClasses[i].isPrimitive()) {
                throw new IllegalArgumentException(JaiI18N.getString("PropertyGeneratorImpl4"));
            }
        }

        this.propertyNames = propertyNames;
        this.propertyClasses = propertyClasses;
        this.supportedOpClasses = supportedOpClasses;
    }

    /**
     * Returns an array of <code>String</code>s naming properties emitted
     * by this property generator.
     *
     * @return an array of <code>String</code>s that may be passed as parameter
     *         names to the <code>getProperty()</code> method.
     */
    public String[] getPropertyNames() {
        return propertyNames;
    }

    /**
     * Returns the class expected to be returned by a request for
     * the property with the specified name.  If this information
     * is unavailable, <code>null</code> will be returned indicating
     * that <code>getProperty(propertyName).getClass()</code> should
     * be executed instead.  A <code>null</code> value might
     * be returned for example to prevent generating the value of
     * a deferred property solely to obtain its class.
     *
     * @return The <code>Class</code> expected to be return by a
     *         request for the value of this property or <code>null</code>.
     * @exception IllegalArgumentException if <code>propertyName</code>
     *         is <code>null</code>.
     */
    public Class getClass(String propertyName) {
        if(propertyName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("PropertyGeneratorImpl0"));
        }

        // Linear search as there are likely few properties.
        int numProperties = propertyNames.length;
        for(int i = 0; i < numProperties; i++) {
            if(propertyName.equalsIgnoreCase(propertyNames[i])) {
                return propertyClasses[i];
            }
        }

        // XXX Should an IllegalArgumentException be thrown if this property
        // is not emitted by this PropertyGenerator?
        return null;
    }

    /**
     * Determines whether the specified <code>Object</code> will
     * be recognized by <code>getProperty(String,Object)</code>.
     *
     * @exception IllegalArgumentException if <code>opNode</code>
     *         is <code>null</code>.
     */
    public boolean canGenerateProperties(Object opNode) {
        if(opNode == null) {
            throw new IllegalArgumentException(JaiI18N.getString("PropertyGeneratorImpl0"));
        }

        int numClasses = supportedOpClasses.length;
        if(numClasses == 1) {
            return supportedOpClasses[0].isInstance(opNode);
        } else {
            // Linear search as there are likely few supported classes.
            for(int i = 0; i < numClasses; i++) {
                if(supportedOpClasses[i].isInstance(opNode)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Computes the value of a property relative to an environment
     * of pre-existing properties.  The case of the supplied
     * <code>String</code> is ignored.
     *
     * <p> In the case of an <code>OperationNode</code> in a chain of
     * operations these properties may be emitted by the sources of the
     * node in a chain or the parameters of that operation.  The information
     * requisite to compute the requested property must be available via the
     * supplied <code>OperationNode</code>.  It is legal to call
     * <code>getProperty()</code> on the operation's sources.
     *
     * @param name the name of the property, as a <code>String</code>.
     * @param op the <code>Object</code> from which properties will
     *           be generated.
     * @return the value of the property, as an <code>Object</code> or the
     *         value <code>java.awt.Image.UndefinedProperty</code>.
     * @exception IllegalArgumentException if <code>name</code> or
     *         <code>opNode</code> is <code>null</code>.
     * @exception IllegalArgumentException if <code>opNode</code> is
     *            not an instance of a supported class for this method, i.e.,
     *            <code>canGenerateProperties(opNode)</code> returns
     *            <code>false</code>.
     */
    public abstract Object getProperty(String name, Object opNode);

    /**
     * Computes the value of a property relative to an environment
     * of pre-existing properties emitted by the sources of
     * a <code>RenderedOp</code>, and the parameters of that operation.
     *
     * <p> The operation name, sources, and <code>ParameterBlock</code>
     * of the <code>RenderedOp</code> being processed may be obtained by
     * means of the <code>op.getOperationName</code>, 
     * <code>op.getSources()</code>, and <code>op.getParameterBlock()</code>
     * methods.  It is legal to call <code>getProperty()</code> on the
     * operation's sources.
     *
     * @param name the name of the property, as a <code>String</code>.
     * @param op the <code>RenderedOp</code> representing the operation.
     * @return the value of the property, as an <code>Object</code> or the
     *         value <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @deprecated as of Java(tm) Advanced Imaging 1.1. Use
     *             <code>getProperty(String,Object)</code> instead.
     * @exception IllegalArgumentException if <code>name</code> or
     *         <code>op</code> is <code>null</code>.
     */
    public Object getProperty(String name, 
                              RenderedOp op) {
        return getProperty(name, (Object)op);
    }

    /**
     * Computes the value of a property relative to an environment
     * of pre-existing properties emitted by the sources of
     * a <code>RenderableOp</code>, and the parameters of that operation.
     *
     * <p> The operation sources and <code>ParameterBlock</code> of the
     * <code>RenderableOp</code> being processed may be obtained by
     * means of the <code>op.getSources()</code> and
     * <code>op.getParameterBlock()</code> methods. It is legal to call
     * <code>getProperty()</code> on the operation's sources.
     *
     * @param name the name of the property, as a <code>String</code>.
     * @param op the <code>RenderableOp</code> representing the operation.
     * @return the value of the property, as an <code>Object</code> or the
     *         value <code>java.awt.Image.UndefinedProperty</code>.
     * @exception IllegalArgumentException if <code>name</code> or
     *         <code>op</code> is <code>null</code>.
     *
     * @deprecated as of Java(tm) Advanced Imaging 1.1. Use
     *             <code>getProperty(String,Object)</code> instead.
     */
    public Object getProperty(String name, 
                              RenderableOp op) {
        return getProperty(name, (Object)op);
    }

    /**
     * Throws an exception if the arguments are illegal for
     * <code>getProperty(String,Object)</code>.
     *
     * @param name the name of the property, as a <code>String</code>.
     * @param op the <code>Object</code> from which properties will
     *           be generated.
     * @exception IllegalArgumentException if <code>name</code> or
     *         <code>opNode</code> is <code>null</code>.
     * @exception IllegalArgumentException if <code>opNode</code> is
     *            not an instance of a supported class for this method, i.e.,
     *            <code>canGenerateProperties(opNode)</code> returns
     *            <code>false</code>.
     */
    protected void validate(String name, Object opNode) {
        if(name == null) {
            throw new IllegalArgumentException(JaiI18N.getString("PropertyGeneratorImpl0"));
        } else if(!canGenerateProperties(opNode)) {
            throw new IllegalArgumentException(JaiI18N.getString("PropertyGeneratorImpl3"));
        }
    }
}
