/*
 * $RCSfile: InterfaceProxy.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:51 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * A class which acts as a proxy for an object which is serialized as an
 * amalgam of <code>Serializer</code>s for the various interfaces that
 * it implements.
 *
 * @since 1.1
 */
public final class InterfaceProxy extends Proxy {
    public InterfaceProxy(InvocationHandler h) {
        super(h);
    }
}
