/*
 * $RCSfile: RegistryElementDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:19 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

/**
 * An interface for all JAI descriptors that register themselves
 * with the <code>OperationRegistry</code>. Examples include
 * <code>OperationDescriptor</code>, <code>TileCodecDescriptor</code>,
 * <code>RemoteDescriptor</code> etc.
 *
 * @see OperationRegistry
 * @see RegistryMode
 *
 * @since JAI 1.1
 */
public interface RegistryElementDescriptor {

    /**
     * The name this descriptor will be registered under in the
     * <code>OperationRegistry</code>. Individual descriptors
     * implementing this interface will define what this name means
     * in their space. For example this would be "operation name" for
     * <code>OperationDescriptor</code> and "format name" for
     * <code>TileCodecDescriptor</code> etc. The descriptor
     * names are to be treated in a case-insensitive (but retentive) manner.
     */
    String getName();

    /**
     * The registry modes supported by this descriptor. Known modes
     * include those returned by <code>RegistryMode.getModes()</code>.
     *
     * @return an array of <code>String</code>s specifying the supported modes.
     *
     * @see RegistryMode
     */
    String[] getSupportedModes();

    /**
     * Whether this descriptor supports the specified registry mode.
     * The <code>modeName</code>s are to be treated in a case-insensitive
     * (but retentive) manner.
     *
     * @param modeName the registry mode name
     *
     * @return true, if the implementation of this descriptor supports
     *	       the specified mode. false otherwise.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null
     */
    boolean isModeSupported(String modeName);

    /**
     * Whether this descriptor supports JAI properties.
     *
     * @return <code>true</code>, if the implementation of this descriptor
     *	       supports JAI properties. <code>false</code> otherwise.
     *
     * @see PropertyGenerator
     */
    boolean arePropertiesSupported();

    /**
     * Returns an array of <code>PropertyGenerator</code>s implementing
     * the property inheritance for this descriptor.  They may be used
     * as a basis for the descriptor's property management.
     *
     * @param modeName the registry mode name
     *
     * @return  An array of <code>PropertyGenerator</code>s, or
     *          <code>null</code> if this operation does not have any of
     *          its own <code>PropertyGenerator</code>s.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null
     *		or if it is not one of the supported modes.
     * @throws UnsupportedOperationException if <code>arePropertiesSupported()</code>
     *		returns <code>false</code>
     */
    PropertyGenerator[] getPropertyGenerators(String modeName);

    /**
     * Returns the <code>ParameterListDescriptor</code> that describes
     * the associated parameters (<u>not</u> sources). This method returns
     * null if the specified modeName does not support parameters.
     * If the specified modeName supports parameters but the
     * implementing class does not have parameters, then this method
     * returns a non-null <code>ParameterListDescriptor</code> whose
     * <code>getNumParameters()</code> returns 0.
     *
     * @param modeName the registry mode name.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null
     *		or if it is not one of the supported modes.
     */
    ParameterListDescriptor getParameterListDescriptor(String modeName);
}
