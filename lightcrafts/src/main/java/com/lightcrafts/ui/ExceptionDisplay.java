/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.StringWriter;

/** Display a Throwable as a text area.
  */

public class ExceptionDisplay extends Box implements Scrollable {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionDisplay.class);

    private JTextArea trace;

    public ExceptionDisplay(Throwable t) {
        super(BoxLayout.Y_AXIS);

        trace = new JTextArea();
        trace.setRows(20);
        trace.setColumns(100);
        trace.setEditable(true);
        trace.setLineWrap(false);
        trace.setEditable(false);
        Font font = new Font("Monospaced", Font.PLAIN, 12);
        trace.setFont(font);

        StringWriter buffer = new StringWriter();
        buffer.append(buildStackTraceText(t));
        trace.setText(buffer.toString());
        add(trace);

        logger.error("Exception display", t);

        add(Box.createVerticalGlue());
    }

    // Passthrough Scrollable implementation from DirectoryStack and JTextArea:

    public boolean getScrollableTracksViewportHeight() {
        return trace.getScrollableTracksViewportHeight();
    }

    public boolean getScrollableTracksViewportWidth() {
        return trace.getScrollableTracksViewportWidth();
    }

    public Dimension getPreferredScrollableViewportSize() {
        return trace.getPreferredSize();
    }

    public int getScrollableBlockIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        return trace.getScrollableBlockIncrement(
            visibleRect, orientation, direction
        );
    }

    public int getScrollableUnitIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        return trace.getScrollableUnitIncrement(
            visibleRect, orientation, direction
        );
    }

    private static String buildStackTraceText(Throwable t) {
        final StringBuilder sb = new StringBuilder();
        Throwable current = t;
        while (current != null) {
            sb.append(current).append('\n');
            for (StackTraceElement element : current.getStackTrace()) {
                sb.append("\tat ").append(element).append('\n');
            }
            current = current.getCause();
            if (current != null) {
                sb.append("Caused by: ");
            }
        }
        return sb.toString();
    }
}
