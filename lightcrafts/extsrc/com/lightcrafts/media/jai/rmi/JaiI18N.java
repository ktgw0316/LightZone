/*
 * $RCSfile: JaiI18N.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:52 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;
import com.lightcrafts.media.jai.util.PropertyUtil;

class JaiI18N {
    static String packageName = "com.lightcrafts.media.jai.rmi";

    public static String getString(String key) {
        return PropertyUtil.getString(packageName, key);
    }
}
