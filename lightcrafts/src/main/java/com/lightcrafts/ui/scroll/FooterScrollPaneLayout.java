/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.scroll;

import javax.swing.*;
import java.awt.*;

class FooterScrollPaneLayout extends ScrollPaneLayout {

    private JComponent footer;

    void setFooter(JComponent footer) {
        this.footer = footer;
    }

    JComponent getFooter() {
        return footer;
    }

    public void layoutContainer(Container parent) {
        super.layoutContainer(parent);
        if (footer != null) {
            Point hsbLoc = hsb.getLocation();
            Dimension hsbSize = hsb.getSize();
            Dimension footerSize = footer.getPreferredSize();

            // Shrink the horizontal scrollbar horizontally:
            hsb.setSize(hsbSize.width - footerSize.width, hsbSize.height);

            // Align the footer's height with the scrollbar:
            footer.setSize(footerSize);

            // Place the footer right of the scrollbar, centered vertically:
            footer.setLocation(
                hsbLoc.x + hsbSize.width - footerSize.width,
                hsbLoc.y - (footerSize.height - hsbSize.height) / 2
            );
        }
    }
}
