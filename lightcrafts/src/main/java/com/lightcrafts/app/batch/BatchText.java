/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.batch;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * A regular JTextArea with extra formatting available, to make the
 * start/end/error nature of batch output more readable, and to handle
 * the rethreading of updates from the batch thread.
 */
class BatchText extends JTextArea {

    private int tabSize;

    BatchText() {
        setEditable(false);
    }

    void appendStart(final String message) {
        if (! EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        appendStart(message);
                    }
                }
            );
            return;
        }
        append(message);

        int newTabSize = getTabEquivalent(message);
        if (newTabSize > tabSize) {
            tabSize = newTabSize;
            setTabSize(tabSize);
        }
    }

    void appendEnd(final String message) {
        if (! EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        appendEnd(message);
                    }
                }
            );
            return;
        }
        append("\t -- ");
        append(message);
        append("\n");
    }

    void appendError(final String message) {
        // add emphasis (bold? italics? color? asterisks?)
        appendEnd(message);
    }

    // Compute the value for setTabSize() that would ensure that the first tab
    // stop is a short distance to the right of the right edge of the given
    // text.
    private int getTabEquivalent(String text) {
        Font font = getFont();
        FontMetrics metrics = getFontMetrics(font);
        // Javadocs for JTextArea suggest that setTabSize() multiplies its
        // argument by the maximum advance of the component's font, but the
        // actual multiplier is clearly less.  We use 13, which works right
        // with the current L&F.
//        int advance = metrics.getMaxAdvance();
        int advance = 12;
        Graphics2D g = (Graphics2D) getGraphics();
        Rectangle2D bounds = metrics.getStringBounds(text, g);
        double width = bounds.getX() + bounds.getWidth();
        return (int) Math.ceil(width / advance);
    }
}
