/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.scroll.CenteringScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * This is a scroll pane customized for holding an AbstractImageBrowser.
 * <p>
 * Customizations include:
 * <ul>
 * <li>A specialized scroll pane UI for standardizing mouse wheel behavior.</li>
 * <li>A call to cancel background browser thumbnail tasks for off-viewport
 * thumbnails when scrollbar adjustment ends.
 * <li>Repaint trickery to reorder thumbnail tasks so refreshes happen in order
 * in the viewport (hopefully).</li>
 * <li>Snap-to-grid behavior when scrollbar adjustment ends.
 * <li>The Quaqua visualMargin client property.</li>
 * </ul>
 */
public class ImageBrowserScrollPane extends CenteringScrollPane {

    private AbstractImageBrowser browser;

    public ImageBrowserScrollPane() {
        // Enforce cross-platform mouse wheel behavior on the browser scroll:
        setUI(new BrowserScrollPaneUI());

        setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        );
        setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        setBorder(LightZoneSkin.getPaneBorder());

        // Let a click on the viewport clear the browser's selection.
        getViewport().addMouseListener(
            new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    if (browser != null) {
                        browser.clearSelection();
                    }
                }
            }
        );
        // This is a trick to reorder the thumbnail tasks
        // so refreshes happen in order in the viewport.
        getVerticalScrollBar().addAdjustmentListener(
            new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    if (! e.getValueIsAdjusting()) {
                        EventQueue.invokeLater(
                            new Runnable() {
                                public void run() {
                                    // May be run after dispose().
                                    if (browser != null)
                                        browser.repaint();
                                }
                            }
                        );
                    }
                }
            }
        );
        // When scrolling ceases, bump the scroll position so that browser
        // features align with the viewport:
        getVerticalScrollBar().addAdjustmentListener(
            new AdjustmentListener() {
                boolean isSnapping;
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    if ((! e.getValueIsAdjusting()) && (browser != null)) {
                        // Don't recurse.
                        if (! isSnapping) {
                            isSnapping = true;
                            // May be run after dispose().
                            snapBrowserScroll();
                            isSnapping = false;
                        }
                        // Figure out what thumbnails are NOT visible, and
                        // make sure all of them are idle.
                        cancelBrowserTasks();
                    }
                }
            }
        );
    }

    public void setBrowser(AbstractImageBrowser browser) {
        this.browser = browser;
        JViewport viewport = getViewport();
        viewport.setBackground(AbstractImageBrowser.Background);
        viewport.setView(browser);
        validate();
    }

    public AbstractImageBrowser getBrowser() {
        return browser;
    }

    // Adjust the browser scroll position so that things remain aligned with
    // the viewport.  Called from an AdjustmentListener defined in the
    // ComboFrame constructor.
    private void snapBrowserScroll() {
        int inc = browser.getScrollableUnitIncrement(
            null, 0, 0
        );
        JViewport viewport = getViewport();
        Rectangle viewRect = viewport.getViewRect();

        // Four numbers:
        //
        //  amount up   to align top
        //  amount down to align top
        //  amount up   to align bottom
        //  amount down to align bottom
        //
        // Jump up or down by the smallest distance, respecting whether we
        // are currently aligning at the top or bottom.

        int upTop = - viewRect.y % inc;

        int downTop = (inc + upTop);

        int upBottom = - (viewRect.y + viewRect.height) % inc;

        int downBottom = (inc + upBottom);

        int min = Integer.MAX_VALUE;
        int scrollToY = 0;

        if (Math.abs(min) > Math.abs(upTop)) {
            scrollToY = viewRect.y + upTop;
            min = upTop;
        }
        if (Math.abs(min) > Math.abs(downTop)) {
            scrollToY = viewRect.y + viewRect.height + downTop;
            min = downTop;
        }
        if (Math.abs(min) > Math.abs(upBottom)) {
            scrollToY = viewRect.y + upBottom;
            min = upBottom;
        }
        if (Math.abs(min) > Math.abs(downBottom)) {
            scrollToY = viewRect.y + viewRect.height + downBottom;
        }
        Rectangle rect = new Rectangle(0, scrollToY, 0, 0);

        browser.scrollRectToVisible(rect);
    }

    // Figure out which thumbnails are NOT visible in the scroll viewport,
    // and make sure their background tasks are dequeued.  Called from an
    // AdjustmentListener defined in the ComboFrame constructor.
    private void cancelBrowserTasks() {
        if (browser != null) {
            JViewport viewport = getViewport();
            Rectangle visible = viewport.getViewRect();
            browser.cancelTasks(visible);
        }
    }
}
