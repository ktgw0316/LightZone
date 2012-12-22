/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import javax.swing.*;

/**
 * A static utility to configure JTextAreas in a standard way.  It takes a
 * text String and a column width, and returns a JTextArea that has wrapped
 * lines with word breaks, has its preferred size correctly computed, and is
 * not editable.
 */
public class TextAreaFactory {

    /**
     * Generate a JTextArea holding the given text and of width corresponding
     * to the given column count, and initialized in a standard way.
     * @param text The message used to fill the text area and determine its
     * layout.
     * @param columns A number to specify the width of the text area, in the
     * units of JTextArea.setColumns().
     * @return The configured JTextArea, suitable for user presentation.
     */
    public static JTextArea createTextArea(String text, int columns) {
        JTextArea area = new JTextArea(text);
        area.setColumns(columns);
        area.setRows(0);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);

        // Here is how you make a JTextArea discover its correct preferred size:
        JDialog layoutHack = new JDialog();
        layoutHack.getContentPane().add(area);
        layoutHack.pack();
        layoutHack.remove(area);

        return area;
    }
}
