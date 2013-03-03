/*
 * $RCSfile: OperationNode.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:13 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;

/**
 * A class which is a node in a chain of operations.  This interface
 * aggregates the minimal set of methods which would be expected to be
 * implemented by such a class.
 *
 * <p> Accessors and mutators of the critical attributes of the node
 * are provided:
 *
 * <ul>
 * <li> The name of the operation as a <code>String</code>;
 * <li> The <code>OperationRegistry</code> to be used to resolve the
 *      operation name into an image factory which renders the node;
 * <li> The lists of sources and parameters of the node as a
 *      <code>ParameterBlock</code>; and
 * <li> The mapping of hints to be used when rendering the node.
 * </ul>
 *
 * Whether an implementing class maintains these critical attributes by
 * reference or by copying or cloning is left to the discretion of the
 * implementation.
 *
 * <p> <code>OperationNode</code>s should fire a
 * <code>PropertyChangeEventJAI</code> when any of the critical attributes of
 * the node is modified.  These events should be named "OperationName",
 * "OperationRegistry", "ParameterBlock", and "RenderingHints" corresponding
 * to the respective critical attributes.  Events named "Sources" and
 * "Parameters" may instead be fired if it can be determined that a
 * <code>ParameterBlock</code> modification has affected only the sources
 * or parameters of the node, respectively.  Nodes which implement convenience
 * methods to edit individual node sources, parameters, or hints should
 * still fire an event for the attribute as a whole.  Note that this might
 * require cloning the respective object.  <code>OperationNode</code>s are
 * also required to fire <code>PropertySourceChangeEvent</code>s by virtue
 * of their being a <code>PropertySource</code> as well as a
 * <code>PropertyChangeEmitter</code>.
 *
 * <p> Methods are also provided to modify the local property environment
 * of the node.  The global property environment is maintained by the
 * associated <code>OperationRegistry</code> and used to initialize the
 * local property environment.  Methods are provided to:
 *
 * <ul>
 * <li> Add a <code>PropertyGenerator</code>;
 * <li> Direct that a given property be copied from a certain source; and
 * <li> Suppress the emission of a certain property.
 * </ul>
 *
 * Invocation of these methods would not affect the global property
 * environment of the operation as maintained by the
 * <code>OperationRegistry</code>.
 *
 * @since JAI 1.1
 */
public interface OperationNode extends PropertySource, PropertyChangeEmitter {

    /**
     * Returns the name of the <code>RegistryMode</code> corresponding to
     * this <code>OperationNode</code>.  This value should be immutable
     * for a given node.
     */
    String getRegistryModeName();

    /** 
     * Returns the name of the operation this node represents as
     * a <code>String</code>.
     */
    String getOperationName();

    /**
     * Sets the name of the operation this node represents.
     *
     * <p> If the operation name changes according to a case-insensitive
     * comparison by <code>equals()</code> of the old and new names,
     * a <code>PropertyChangeEventJAI</code> named "OperationName"
     * should be fired with
     * source equal to this node and old and new values set to the old
     * and new values of the operation name, respectively.
     *
     * @param opName  The new operation name to be set.
     *
     * @throws IllegalArgumentException if <code>opName</code> is
     * <code>null</code>.
     */
    void setOperationName(String opName);

    /**
     * Returns the <code>OperationRegistry</code> that is used
     * by this node.  If the registry is not set, the default
     * registry is returned.
     */
    OperationRegistry getRegistry();

    /**
     * Sets the <code>OperationRegistry</code> that is used by
     * this node.  If the specified registry is <code>null</code>, the
     * default registry is used.
     *
     * <p> If the registry changes according to a direct comparison
     * of the old and new registry references,
     * a <code>PropertyChangeEventJAI</code> named "OperationRegistry"
     * should be fired with
     * source equal to this node and old and new values set to the old
     * and new values of the registry, respectively.
     *
     * @param registry  The new <code>OperationRegistry</code> to be set;
     *        it may be <code>null</code>.
     */
    void setRegistry(OperationRegistry registry);

    /**
     * Returns the <code>ParameterBlock</code> of this node.
     */
    ParameterBlock getParameterBlock();

    /**
     * Sets the <code>ParameterBlock</code> of this node.  If the specified
     * new <code>ParameterBlock</code> is <code>null</code>, it is assumed
     * that this node has no input sources and parameters.
     *
     * <p> This method does not validate the content of the supplied
     * <code>ParameterBlock</code>.  The caller should ensure that
     * the sources and parameters in the <code>ParameterBlock</code>
     * are suitable for the operation this node represents; otherwise
     * some form of error or exception may occur at the time of rendering.
     *
     * <p> If the <code>ParameterBlock</code> changes according to a
     * comparison of the sources and parameters <code>Vector</code>s of the
     * old and new <code>ParameterBlock</code>s using <code>equals()</code>,
     * a <code>PropertyChangeEventJAI</code> named "ParameterBlock"
     * should be fired with
     * source equal to this node and old and new values set to the old
     * and new values of the <code>ParameterBlock</code>, respectively.
     * A <code>PropertyChangeEventJAI</code> named "Sources" or
     * "Parameters" may instead be fired if it can be determined that the
     * <code>ParameterBlock</code> modification has affected only the sources
     * or parameters of the node, respectively.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when the node is
     * rendered.  Any <code>Observable</code> events generated by such
     * <code>DeferredData</code> parameters will be trapped by the node
     * and acted upon.
     *
     * @param pb  The new <code>ParameterBlock</code> to be set;
     *        it may be <code>null</code>.
     */
    void setParameterBlock(ParameterBlock pb);

    /**
     * Returns the <code>RenderingHints</code> of this node.
     * It may be <code>null</code>.
     */
    RenderingHints getRenderingHints();

    /**
     * Sets the <code>RenderingHints</code> of this node.  It is legal
     * for nodes to ignore <code>RenderingHints</code> set on them by
     * this mechanism.
     *
     * <p> If the <code>RenderingHints</code> changes according to a
     * comparison by <code>equals()</code> of the old and new hints,
     * a <code>PropertyChangeEventJAI</code> named "RenderingHints"
     * should be fired with
     * source equal to this node and old and new values set to the old
     * and new values of the hints, respectively.
     *
     * @param hints The new <code>RenderingHints</code> to be set;
     *        it may be <code>null</code>.
     */
    void setRenderingHints(RenderingHints hints);

    /**
     * Returns the property associated with the specified property name,
     * or <code>java.awt.Image.UndefinedProperty</code> if the specified
     * property is not set on the image.  This method is dynamic in the
     * sense that subsequent invocations of this method on the same object
     * may return different values as a function of changes in the property
     * environment of the node, e.g., a change in which
     * <code>PropertyGenerator</code>s are registered or in the values
     * associated with properties of node sources.  The case of the property
     * name passed to this method is ignored.
     *
     * @param name A <code>String</code> naming the property.
     *
     * @throws IllegalArgumentException if 
     *         <code>name</code> is <code>null</code>.
     */
    Object getDynamicProperty(String name);

    /**
     * Adds a <code>PropertyGenerator</code> to the node.  The property values
     * emitted by this property generator override any previous
     * definitions.
     *
     * @param pg A <code>PropertyGenerator</code> to be added to this node's
     *        property environment.
     *
     * @throws IllegalArgumentException if 
     * <code>pg</code> is <code>null</code>.
     */
    void addPropertyGenerator(PropertyGenerator pg);

    /**
     * Forces a property to be copied from the specified source node.
     * By default, a property is copied from the first source node
     * that emits it.  The result of specifying an invalid source is
     * undefined.
     *
     * @param propertyName the name of the property to be copied.
     * @param sourceIndex the index of the from which to copy the property.
     * @throws IllegalArgumentException if <code>propertyName</code> is
     *         <code>null</code>.
     */
    void copyPropertyFromSource(String propertyName,
                                int sourceIndex);

    /**
     * Removes a named property from the property environment of this
     * node.  Unless the property is stored locally either due
     * to having been set explicitly or to having been cached for property
     * synchronization purposes, subsequent calls to
     * <code>getProperty(name)</code> will return
     * <code>java.awt.Image.UndefinedProperty</code>, and <code>name</code> 
     * will not appear on the list of properties emitted by
     * <code>getPropertyNames()</code>.
     *
     * @param name A <code>String</code> naming the property to be suppressed.
     *     
     * @throws IllegalArgumentException if 
     * <code>name</code> is <code>null</code>.
     */
    void suppressProperty(String name);
}
