/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.scroll;

import javax.swing.*;
import java.awt.*;

class CenteringViewport extends JViewport {

    protected LayoutManager createLayoutManager() {
        return createCenteringLayoutManager();
    }

    LayoutManager createCenteringLayoutManager() {
        return new CenteringViewportLayout();
    }

    LayoutManager createDefaultLayoutManager() {
        return super.createLayoutManager();
    }

    public void setView(Component view) {
        if (view instanceof Scrollable) {
            super.setView(view);
        }
        else if (view instanceof JComponent) {
            JComponent comp = (JComponent) view;
            ImageScrollable scrollable = new ImageScrollable(comp);
            super.setView(scrollable);
        }
    }
}
