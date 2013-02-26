/*
 * $RCSfile: JaiI18N.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:11 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import com.lightcrafts.media.jai.util.PropertyUtil;
import java.text.MessageFormat;
import java.util.Locale;

class JaiI18N {
    static String packageName = "com.lightcrafts.mediax.jai";

    public static String getString(String key) {
        return PropertyUtil.getString(packageName, key);
    }

    public static String formatMsg(String key, Object[] args) {
        MessageFormat mf = new MessageFormat(getString(key));
        mf.setLocale(Locale.getDefault());

        return mf.format(args);
    }
}
