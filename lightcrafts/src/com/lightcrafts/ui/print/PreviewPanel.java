/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;

class PreviewPanel extends JPanel implements PrintLayoutModelListener {

    private PreviewComponent component;

    PreviewPanel(BufferedImage image, PrintLayoutModel model) {
        PageFormat page = model.getPageFormat();
        component = new PreviewComponent(image, page);
        layoutChanged(model);
        setLayout(new BorderLayout());
        add(component);
        Border border = BorderFactory.createTitledBorder("Preview");
        setBorder(border);
        model.addListener(this);
    }

    public void layoutChanged(PrintLayoutModel model) {
        PageFormat page = model.getPageFormat();
        component.setPageFormat(page);
        Rectangle2D rect = model.getImageRect();
        component.setImageRect(rect);
    }
}
