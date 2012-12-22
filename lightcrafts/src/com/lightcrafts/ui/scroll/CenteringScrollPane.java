/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.scroll;

import javax.swing.*;
import java.awt.*;

/** A JScrollPane that uses a CenteringViewport and an ImageScrollable to
  * provide customized scrolling behavior suitable for viewing images.  This
  * behavior includes preserving the center of the visible rectangle when the
  * view's size changes, and putting the view in a Scrollable that specifies
  * scroll increments in terms of fractions of the visible rectangle.
  */

public class CenteringScrollPane extends JScrollPane {

    private JComponent footer;

    public CenteringScrollPane() {
        this(new JPanel());
    }

    public CenteringScrollPane(JComponent view) {
        CenteringViewport viewport = new CenteringViewport();
        viewport.setView(view);
        setViewport(viewport);
    }

    /**
     * Make a CenteringScrollPane that has an extra "footer" component.
     * This footer will be displayed right of the horizontal scroll bar
     * at its preferred width and at a height matching the height of the
     * scroll bar.
     */
    public CenteringScrollPane(JComponent view, JComponent footer) {
        this(view);
        FooterScrollPaneLayout layout = new FooterScrollPaneLayout();
        layout.setFooter(footer);
        setLayout(layout);
        add(footer);
        this.footer = footer;
    }

    public void setFooter(JComponent footer) {
        if (this.footer != null) {
            remove(this.footer);
            setLayout(new ScrollPaneLayout());
            this.footer = null;
        }
        if (footer != null) {
            add(footer);
            FooterScrollPaneLayout layout = new FooterScrollPaneLayout();
            setLayout(layout);
            layout.setFooter(footer);
            this.footer = footer;
        }
        repaint();
    }

    public void setCenteringLayout(boolean on) {
        CenteringViewport viewport = (CenteringViewport) getViewport();
        LayoutManager layout = viewport.getLayout();
        boolean isCentering = layout instanceof CenteringViewportLayout;
        if (on) {
            if (! isCentering) {
                layout = viewport.createCenteringLayoutManager();
                viewport.setLayout(layout);
            }
        }
        else {
            if (isCentering) {
                layout = viewport.createDefaultLayoutManager();
                viewport.setLayout(layout);
            }
        }
    }
}
