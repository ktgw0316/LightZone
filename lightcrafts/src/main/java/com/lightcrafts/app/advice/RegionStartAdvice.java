/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.advice;

import static com.lightcrafts.app.advice.Locale.LOCALE;
import com.lightcrafts.ui.advice.AbstractAdvice;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.editor.Editor;
import com.lightcrafts.app.ComboFrame;

import javax.swing.*;
import java.awt.*;

class RegionStartAdvice extends AbstractAdvice {

    private ComboFrame frame;

    RegionStartAdvice(ComboFrame frame) {
        super(frame);
        this.frame = frame;
    }

    public String getMessage() {
        return LOCALE.get("StartRegionAdvice");
    }

    public int getMaxCount() {
        return 3;
    }

    public Point getLocation() {
        Document doc = frame.getDocument();
        Editor editor = doc.getEditor();
        JComponent image = editor.getImage();
        Point loc = image.getLocationOnScreen();
        Dimension size = image.getSize();
        Point p = new Point(
            loc.x + size.width - 200, loc.y + size.height - 200
        );
        return p;
    }
}
