/*
 * $RCSfile: JaiI18N.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:58 $
 * $State: Exp $
 */
/**
 * <p>
 * This class has been deprecated.  The source
 * code has been moved to the samples/widget
 * directory.  These widgets are no longer
 * supported.
 *
 * @deprecated as of JAI 1.1
 */

package com.lightcrafts.mediax.jai.widget;
import com.lightcrafts.media.jai.util.PropertyUtil;

class JaiI18N {
    static String packageName = "com.lightcrafts.mediax.jai.widget";

    public static String getString(String key) {
        return PropertyUtil.getString(packageName, key);
    }
}
