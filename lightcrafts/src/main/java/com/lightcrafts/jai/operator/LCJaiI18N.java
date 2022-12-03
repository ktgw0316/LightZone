/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: LCJaiI18N.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/03/14 08:14:58 $
 * $State: Exp $
 */
package com.lightcrafts.jai.operator;

import java.util.ResourceBundle;

class LCJaiI18N {
    static String packageName = "com.lightcrafts.jai.operator";

    private final static ResourceBundle Resources = ResourceBundle.getBundle(
        "com/lightcrafts/jai/operator/LCJaiI18N"
    );

    public static String getString(String key) {
        return Resources.getString(key);
    }
}
