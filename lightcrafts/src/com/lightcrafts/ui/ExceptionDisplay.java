/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Display a Throwable as a text area.
  */

public class ExceptionDisplay extends Box implements Scrollable {

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
        PrintWriter writer = new PrintWriter(buffer);
        t.printStackTrace(writer);
        trace.setText(buffer.toString());
        add(trace);

        t.printStackTrace();

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
}
