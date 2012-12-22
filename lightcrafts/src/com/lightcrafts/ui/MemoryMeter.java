/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

/**
 * Make a progress bar-style indicator of used and available heap space that
 * updates automatically.
 */

public class MemoryMeter extends JComponent {

    private final static long UpdateInterval = 1000;    // milliseconds
    private final static Dimension PreferredSize = new Dimension(110, 20);
    private final static Color MemoryColor = new Color(100, 168, 242);

    private final static Runtime RtInstance = Runtime.getRuntime();
    private final static long MaxMemory = RtInstance.maxMemory();

    private static long usedMemory;

    private final static List<MemoryMeter> Instances =
        new LinkedList<MemoryMeter>();

    private final static Thread Updater = new Thread(
        new Runnable() {
            public void run() {
                String text = null;
                while (true) {
                    try {
                        Thread.sleep(UpdateInterval);
                    }
                    catch (InterruptedException e) {
                    }
                    usedMemory =
                        RtInstance.totalMemory() - RtInstance.freeMemory();
                    if (getText().equals(text)) {
                        // Update only when the text changes:
                        continue;
                    }
                    text = getText();
                    synchronized(Instances) {
                        for (MemoryMeter instance : Instances) {
                            if (instance.isShowing()) {
                                instance.repaint();
                            }
                        }
                    }
                }
            }
        },
        "Memory Meter Updater"
    );

    static {
        Updater.setDaemon(true);
        Updater.start();
    }

    public MemoryMeter() {
        synchronized(Instances) {
            Instances.add(this);
        }
    }

    public Dimension getPreferredSize() {
        return PreferredSize;
    }

    protected void paintComponent(Graphics graphics) {
        Dimension size = getSize();
        Insets insets = getInsets();

        int x = insets.left;
        int y = insets.top;
        int w = size.width - insets.left - insets.right;
        int h = size.height - insets.top - insets.bottom;

        Paint paint = new GradientPaint(
            x, y, Color.white, 0, h / 2, MemoryColor, true
        );
        int mem
            = (int) Math.round(size.width * usedMemory / (double) MaxMemory);

        Graphics2D g = (Graphics2D) graphics;
        Paint oldPaint = g.getPaint();
        g.setPaint(paint);
        g.fillRect(x, y, mem, h);
        g.setPaint(oldPaint);

        String text = getText();

        Font font = g.getFont();
        FontRenderContext fontContext = g.getFontRenderContext();
        TextLayout layout = new TextLayout(text, font, fontContext);
        Rectangle2D textBounds = layout.getBounds();

        int textH = x + (int) Math.round(w - textBounds.getWidth()) / 2;
        int textV = y + (int) Math.round(h + textBounds.getHeight()) / 2;
        g.drawString(text, textH, textV);
    }

    /** All MemoryMeter instances are updated on a single Thread.  This
      * method removes the reference to this MemoryMeter from the Thread.
      */
    public void dispose() {
        synchronized(Instances) {
            Instances.remove(this);
        }
    }

    private static String getText() {
        long usedMegs = usedMemory >> 20;
        long maxMegs = MaxMemory >> 20;
        String text = "" + usedMegs + "M of " + maxMegs + "M";
        return text;
    }
}
