/*
 * $RCSfile: JAIServerConfigurationSpi.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:49 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.remote;

import com.lightcrafts.mediax.jai.JAI;

// This uses the ImageIO idea of "services" to look for
// concrete class that implement this interface. These concrete
// classes must have been registered/listed in the
// META-INF/services/com.lightcrafts.media.jai.remote.JAIServerConfigurationSpi file.

/**
 * <p> An interface definition to aid in the automatic loading of
 * user-defined JAI based Remote Imaging server configuration logic.
 *
 * <p> All concrete classes that implement this
 * interface can register by listing themselves in the
 * "<code>META-INF/services/com.lightcrafts.media.jai.remote.JAIServerConfigurationSpi</code>"
 * file that can be found in the classpath (this file is often contained
 * in a jar file along with the class files). The file should contain
 * a list of fully-qualified concrete provider-class names, one per
 * line. Space and tab characters surrounding each name, as well as
 * blank lines, are ignored. The comment character is <tt>'#'</tt>
 * (<tt>0x23</tt>); on each line all characters following the first
 * comment character are ignored. The file must be encoded in UTF-8.
 *
 * <p> If a particular concrete provider class is named in more than one
 * configuration file, or is named in the same configuration file more
 * than once, then the duplicates will be ignored. The configuration
 * file naming a particular provider need not be in the same jar file or
 * other distribution unit as the provider itself. The provider must be
 * accessible from the same class loader that was initially queried to
 * locate the configuration file; note that this is not necessarily the
 * class loader that found the file.
 *
 * <p>All such concrete classes must have a zero-argument
 * constructor so that they may be instantiated during lookup. The
 * <code>updateServer()</code> method of all such registered
 * classes will be called with the default instance of the <code>JAI</code>
 * class. Note that this will take place after the JAI 
 * <code>OperationRegistry</code> has been initialized with the
 * default JAI registry file (META-INF/com.lightcrafts.mediax.jai.registryFile.jai),
 * once all "META-INF/registryFile.jai"s found in the
 * classpath are loaded and the <code>updateRegistry</code> method of each
 * <code>OperationRegistrySpi</code> instance has been executed. There is
 * no guarantee of the order in which the <code>updateServer()</code> method
 * of each <code>JAIServerConfigurationSpi</code> instance will be invoked.
 *
 * <p>It is possible to provide arguments to a class implementing this 
 * interface (or any other Service Provider Interface) using the standard
 * <code>Java</code> <code> -D<propertyName>=<value></code> mechanism on
 * the command line when starting an application.
 *
 * @see com.lightcrafts.mediax.jai.remote.JAIRMIDescriptor
 * @see com.lightcrafts.mediax.jai.OperationRegistry
 * @see com.lightcrafts.mediax.jai.OperationRegistry#writeExternal
 * @see com.lightcrafts.mediax.jai.OperationRegistrySpi
 *
 * @since JAI 1.1
 */
public interface JAIServerConfigurationSpi {

    /**
     * This method will be called for all registered "service-providers"
     * of this interface just after the default <code>JAI</code> instance
     * has been constructed.
     */
    public void updateServer(JAI jaiInstance);
}
