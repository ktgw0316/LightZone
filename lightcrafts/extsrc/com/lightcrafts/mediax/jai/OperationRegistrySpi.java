/*
 * $RCSfile: OperationRegistrySpi.java,v $
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

// This uses the ImageIO idea of "services" to look for
// concrete class that implement this interface. These concrete
// classes must have been registered/listed in the
// META-INF/services/com.lightcrafts.mediax.jai.OperationRegistrySpi file.

/**
 * <p> An interface definition to aid in the automatic loading of
 * user-defined JAI operations.
 *
 * <p> All concrete classes that implement this
 * interface can register by listing themselves in the
 * "<code>META-INF/services/com.lightcrafts.mediax.jai.OperationRegistrySpi</code>"
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
 * <code>updateRegistry()</code> method of all such registered
 * classes will be called with the default instance of the JAI
 * <code>OperationRegistry</code> after it has been initialized with the
 * default JAI registry file (META-INF/com.lightcrafts.mediax.jai.registryFile.jai)
 * and once all "META-INF/registryFile.jai"s found in the
 * classpath are loaded. There is no guarantee of the order
 * in which the <code>updateRegistry()</code> method of each
 * <code>OperationRegistrySpi</code> instance will be invoked.
 *
 * <p>The <code>OperationRegistrySpi</code> could also be used to for
 * the registration of other JAI related objects done through static
 * methods such as the <code>Serializer</code> objects and image
 * codecs.
 *
 * @see com.lightcrafts.mediax.jai.remote.Serializer
 * @see com.lightcrafts.mediax.jai.OperationRegistry
 * @see com.lightcrafts.mediax.jai.OperationRegistry#writeExternal
 *
 * @since JAI 1.1
 */
public interface OperationRegistrySpi {

    /**
     * This method will be called for all registered "service-providers"
     * of this interface just after the default registry has been
     * read-in.
     */
    public void updateRegistry(OperationRegistry registry);
}
