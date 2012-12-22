/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.advice;

import com.lightcrafts.app.ComboFrame;
import static com.lightcrafts.app.advice.Locale.LOCALE;
import com.lightcrafts.ui.advice.AbstractAdvice;

import java.awt.*;

/**
 * Advice to show when there is a browser with no images in it.  See
 * ComboFrame.initImages().
 */
class EmptyFolderAdvice extends AbstractAdvice {

    EmptyFolderAdvice(ComboFrame frame) {
        super(frame);
    }

    public String getMessage() {
        return LOCALE.get("EmptyFolderAdvice");
    }

    public int getMaxCount() {
        return 3;
    }

    public Point getLocation() {
        // The advisor dialog will appear centered relative to the frame.
        return null;
    }
}
