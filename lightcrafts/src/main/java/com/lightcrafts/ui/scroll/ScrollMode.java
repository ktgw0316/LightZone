/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.scroll;

import com.lightcrafts.ui.mode.AbstractMode;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.net.URL;

public class ScrollMode extends AbstractMode {

    static Cursor OpenHand;
    static Cursor ClosedHand;

    static {

        // Load the icon and cursor images from resources:

        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image;
        String path;
        URL url;

        final Point hot = new Point(8, 8);

        path = "resources/OpenHand.png";
        url = ScrollMode.class.getResource(path);
        image = toolkit.createImage(url);
        OpenHand = toolkit.createCustomCursor(
            image, hot, "Pan"
        );

        path = "resources/ClosedHand.png";
        url = ScrollMode.class.getResource(path);
        image = toolkit.createImage(url);
        ClosedHand = toolkit.createCustomCursor(
            image, hot, "Panning"
        );
    }

    private JScrollPane scroll; // gets scrolled on mouseDragged()
    private JPanel overlay;     // just something to setCursor() on

    public ScrollMode(JScrollPane scroll) {
        this.scroll = scroll;
        overlay = new JPanel();
        final ScrollListener listener = new ScrollListener();
        overlay.setCursor(OpenHand);
        overlay.addMouseListener(listener);
        overlay.addMouseMotionListener(listener);
    }

    public JComponent getOverlay() {
        return overlay;
    }

    public void addMouseInputListener(MouseInputListener listener) {
        overlay.addMouseListener(listener);
        overlay.addMouseMotionListener(listener);
    }

    public void removeMouseInputListener(MouseInputListener listener) {
        overlay.removeMouseListener(listener);
        overlay.removeMouseMotionListener(listener);
    }

    private class ScrollListener extends MouseInputAdapter {

        private Point cursorStart;
        private Point viewportStart;

        public void mousePressed(MouseEvent e) {
            overlay.setCursor(ClosedHand);
            e = SwingUtilities.convertMouseEvent(
                e.getComponent(), e, scroll
            );
            cursorStart = e.getPoint();
            viewportStart = scroll.getViewport().getViewPosition();
        }

        public void mouseReleased(MouseEvent e) {
            overlay.setCursor(OpenHand);
            cursorStart = null;
            viewportStart = null;
        }

        public void mouseDragged(MouseEvent e) {
            e = SwingUtilities.convertMouseEvent(
                e.getComponent(), e, scroll
            );
            final Point cursor = e.getPoint();
            final int dx = cursor.x - cursorStart.x;
            final int dy = cursor.y - cursorStart.y;
            final Point q =
                new Point(viewportStart.x - dx, viewportStart.y - dy);

            // Enforce all the restirctions to prevent scrolling the view
            // outside its viewport:

            final JViewport viewport = scroll.getViewport();
            final Dimension viewSize = viewport.getViewSize();
            final Dimension extentSize = viewport.getExtentSize();

            if (q.x < 0) {
                q.x = 0;
            }
            else if (extentSize.width >= viewSize.width) {
                q.x = 0;
            }
            else if (q.x > viewSize.width - extentSize.width) {
                q.x = viewSize.width - extentSize.width;
            }
            if (q.y < 0) {
                q.y = 0;
            }
            else if (extentSize.height >= viewSize.height) {
                q.y = 0;
            }
            else if (q.y > viewSize.height - extentSize.height) {
                q.y = viewSize.height - extentSize.height;
            }
            viewport.setViewPosition(q);
        }
    }
}
