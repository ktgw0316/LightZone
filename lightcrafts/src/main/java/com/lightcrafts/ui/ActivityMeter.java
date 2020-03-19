/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui;

import com.lightcrafts.model.EngineListener;

import javax.swing.*;
import java.awt.*;

/**
 * Paints a progress bar-style indicator of Engine activity on top of any
 * JComponent using EngineListener.engineActive().
 * <p>
 * The indicator does not show all the time.  It includes logic to predict
 * how much longer the Engine will be active, and shows only when it's going
 * to be busy a while.  Once it's shown, it stays visible until the Engine
 * activity declines to zero.
 * <p>
 * A flag can be set in showWait() to force some static wait text to appear
 * instead of the progress indicator, bypassing the timing logic until the
 * next EngineListener update.
 */

public class ActivityMeter extends JLayeredPane implements EngineListener {

    /**
     * A JComponent rendered in the ActivityMeter's palette layer.  It uses a
     * bounded range model to work like a progress meter, but with custom
     * rendering.
     */
    class BarDisplay extends JComponent {

        private final Color FrameColor = new Color(200, 200, 200, 128);
        private final Color BackgroundColor = new Color(150, 150, 150, 128);
        private final Color ForegroundColor = new Color(255, 255, 255, 128);

        // The maximum number of little white blocks to show:
        private final static int MaxBlocks = 7;

        private int min, max, value;

        private boolean showWait;

        private String waitText;

        int getValue() {
            return value;
        }

        void setValue(int value) {
            int oldBlocks = getBlocks();
            this.value = value;
            if (getBlocks() != oldBlocks)
                repaint();
        }

        int getMinimum() {
            return min;
        }

        void setMinimum(int min) {
            this.min = min;
            repaint();
        }

        int getMaximum() {
            return max;
        }

        void setMaximum(int max) {
            this.max = max;
            repaint();
        }

        void setShowWait(String text) {
            showWait = text != null;
            waitText = text;
        }

        private int getBlocks() {
            return (int) Math.ceil(
                MaxBlocks * (value - min) / (double) (max - min)
            );
        }

        public Dimension getPreferredSize() {
            return new Dimension(90, 20);
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics;

            RenderingHints oldHints = g.getRenderingHints();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );
            int diameter = 8;

            Dimension size = getSize();

            g.setColor(FrameColor);
            g.fillRoundRect(
                0, 0, size.width-1, size.height-1, diameter, diameter
            );
            g.setColor(BackgroundColor);
            g.fillRoundRect(
                1, 1, size.width-2, size.height-2, diameter, diameter
            );
            g.setRenderingHints(oldHints);

            if (showWait) {
                JLabel text = new JLabel(waitText);
                text.setForeground(ForegroundColor);
                text.setHorizontalAlignment(SwingConstants.CENTER);
                text.setSize(getSize());
                text.paint(graphics);
            }
            else {
                g.setColor(ForegroundColor);
                int count = MaxBlocks - getBlocks() + 1;
                for (int n=0; n<count; n++) {
                    drawOneRectangle(g, n);
                }
            }
        }

        // Makes one of the little white rectangles, at the given position.
        private void drawOneRectangle(Graphics g, int n) {
            int inset = 4;
            Dimension size = getSize();
            int width = (size.width - 2 * inset) / (2 * MaxBlocks - 1);
            int left = 2 + inset + 2 * n * width;
            int right = 2 + inset + (2 * n + 1) * width;
            g.fillRoundRect(
                left, inset, right - left - 1, size.height - 2 * inset - 1, 4, 4
            );
        }
    }

    // If the predicted wait until Engine activity completes is greater
    // than this many milliseconds, the activity meter will display.
    public static final long WaitThreshold = 250;

    // Running average time to compute one tile, in milliseconds:
    private double avgTileWait;

    // Clock time of the last tile count update, in milliseconds:
    private long lastTileTime;

    // Predicted clock time when all tiles will be done, in milliseconds:
    private long doneTime;

    // The JComponent to render as the indicator:
    private BarDisplay progress;

    // The JComponent underneath the indicator:
    private JComponent underlay;

    public ActivityMeter(JComponent underlay) {
        this.underlay = underlay;
        progress = new BarDisplay();
        setLayout(null);
        add(underlay, JLayeredPane.DEFAULT_LAYER);
        avgTileWait = 100;  // initial estimate of milliseconds per tile
        lastTileTime = System.currentTimeMillis();
        doneTime = lastTileTime;
    }

    public Dimension getPreferredSize() {
        return underlay.getPreferredSize();
    }

    // This method could be invoked on any thread.  So it is synchronized,
    // and it rethreads itself to the event dispatch thread.
    public synchronized void engineActive(final int newValue) {
        if (! EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        engineActive(newValue);
                    }
                }
            );
        }
        boolean isVisible = (progress.getParent() != null);

        double oldValue = progress.getValue();
        long oldTime = lastTileTime;
        long newTime = System.currentTimeMillis();

        if (newValue < oldValue) {
            double tileWait =
                (newTime - oldTime) / (double) (oldValue - newValue);
            avgTileWait = (avgTileWait + tileWait) / 2;
        }
        lastTileTime = newTime;
        doneTime = Math.round(newTime + newValue * avgTileWait);

        if (newValue > progress.getMaximum()) {
            progress.setMaximum(newValue);
        }
        progress.setValue(newValue);

        if (newValue > 0) {
            progress.setShowWait(null);
        }
        if ((getTimeRemaining() >= WaitThreshold) && ! isVisible) {
            add(progress, JLayeredPane.PALETTE_LAYER);
            doProgressLayout();
            progress.repaint();
        }
        else if ((newValue == 0) && isVisible && (progress.waitText == null)) {
            remove(progress);
            repaint(progress.getBounds());
        }
    }

    // This causes the BarDisplay to become visible and to show its wait text
    // until the next time an engineActive() callback arrives or a call to
    // hideWait().
    public void showWait(String text) {
        progress.setShowWait(text);
        boolean isVisible = (progress.getParent() != null);
        if (! isVisible) {
            add(progress, JLayeredPane.PALETTE_LAYER);
            doProgressLayout();
        }
        progress.repaint();
    }

    // If the crrent progress level is zero, then hide
    public void hideWait() {
        progress.setShowWait(null);
        boolean isVisible = (progress.getParent() != null);
        if (isVisible) {
            repaint(progress.getBounds());
            remove(progress);
        }
    }

    public long getTimeRemaining() {
        long remaining = doneTime - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    public void doLayout() {
        Dimension size = getSize();
        underlay.setBounds(0, 0, size.width, size.height);
        doProgressLayout();
    }

    private void doProgressLayout() {
        Dimension size = getSize();
        int centerX = size.width / 2;
        int highY = (int) Math.round(0.10 * size.height);
        Dimension displaySize = progress.getPreferredSize();
        int width = displaySize.width;
        int height = displaySize.height;
        progress.setBounds(
            centerX - width / 2, highY - height / 2, width, height
        );
    }
}
