/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollPaneUI;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * We customize the ComponentUI of the JScrollPane which holds the
 * AbstractImageBrowser.  The sole purpose of this UI is to undo mouse wheel
 * event multipliers introduced by the operating system, and force every
 * physical wheel click to translate into one scroll unit increment, which
 * the browser interprets as one row of browser thumbnails.
 * <p>
 * On Mac, BasicScrollPaneUI sems to posess this virtue for free.
 */

class BrowserScrollPaneUI extends BasicScrollPaneUI {

    static void scrollByUnits(JScrollBar scrollbar, int direction, int units) {
        // This method is called from BasicScrollPaneUI to implement wheel
        // scrolling, as well as from scrollByUnit().
        int delta;

        for (int i=0; i<units; i++) {
            if (direction > 0) {
                delta = scrollbar.getUnitIncrement(direction);
            }
            else {
                delta = -scrollbar.getUnitIncrement(direction);
            }

            int oldValue = scrollbar.getValue();
            int newValue = oldValue + delta;

            // Check for overflow.
            if (delta > 0 && newValue < oldValue) {
                newValue = scrollbar.getMaximum();
            }
            else if (delta < 0 && newValue > oldValue) {
                newValue = scrollbar.getMinimum();
            }
            if (oldValue == newValue) {
                break;
            }
            scrollbar.setValue(newValue);
        }
    }

    static void scrollByBlock(JScrollBar scrollbar, int direction) {
        // This method is called from BasicScrollPaneUI to implement wheel
        // scrolling, and also from scrollByBlock().
            int oldValue = scrollbar.getValue();
            int blockIncrement = scrollbar.getBlockIncrement(direction);
            int delta = blockIncrement * ((direction > 0) ? +1 : -1);
            int newValue = oldValue + delta;

            // Check for overflow.
            if (delta > 0 && newValue < oldValue) {
                newValue = scrollbar.getMaximum();
            }
            else if (delta < 0 && newValue > oldValue) {
                newValue = scrollbar.getMinimum();
            }

            scrollbar.setValue(newValue);
    }

    class BrowserMouseWheelHandler implements MouseWheelListener {
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (scrollpane.isWheelScrollingEnabled() &&
                e.getScrollAmount() != 0) {
                JScrollBar toScroll = scrollpane.getVerticalScrollBar();
                int direction;
                // find which scrollbar to scroll, or return if none
                if (toScroll == null || !toScroll.isVisible()) {
                    toScroll = scrollpane.getHorizontalScrollBar();
                    if (toScroll == null || !toScroll.isVisible()) {
                        return;
                    }
                }
                direction = e.getWheelRotation() < 0 ? -1 : 1;
                if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                    // For Windows, lock the scrollAmount to 1
                    scrollByUnits(toScroll, direction, 1 /*e.getScrollAmount()*/);
                    // System.out.println("scrollByUnits " + direction + " " + e.getScrollAmount());
                } else if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
                    scrollByBlock(toScroll, direction);
                    // System.out.println("scrollByUnits " + direction);
                }
            }
        }
    }

    protected MouseWheelListener createMouseWheelListener() {
        return new BrowserScrollPaneUI.BrowserMouseWheelHandler();
    }
}
