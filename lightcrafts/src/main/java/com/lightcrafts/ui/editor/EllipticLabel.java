/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

// Presents a user message in a disabled editor indicating that some number
// of preview images have been suppressed.

class EllipticLabel extends Box {

    private final static Color TextColor =
        LightZoneSkin.Colors.BrowserBackground.darker();

    private final static Color BackgroundColor =
        LightZoneSkin.Colors.BrowserBackground.brighter();

    private final static Color BorderColor = TextColor;

    private JLabel label;

    private int count;

    EllipticLabel() {
        super(BoxLayout.Y_AXIS);

        label = new JLabel();
        label.setForeground(TextColor);
        label.setBackground(BackgroundColor);
        label.setOpaque(true);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        Border emptyBorder = BorderFactory.createEmptyBorder(20, 20, 20, 20);
        Border lineBorder = BorderFactory.createLineBorder(BorderColor);
        Border border =
            BorderFactory.createCompoundBorder(lineBorder, emptyBorder);
        label.setBorder(border);

        Font font = label.getFont();
        font = font.deriveFont(18f);
        label.setFont(font);

        Box box = Box.createHorizontalBox();
        box.add(Box.createHorizontalStrut(8));
        box.add(Box.createHorizontalGlue());
        box.add(label);
        box.add(Box.createHorizontalGlue());
        box.add(Box.createHorizontalStrut(8));

        add(Box.createVerticalGlue());
        add(box);
        add(Box.createVerticalGlue());
    }

    void increment() {
        label.setText(LOCALE.get("EllipsisMessage", ++count));
        repaint();
    }

    int getCount() {
        return count;
    }

    void reset() {
        count = 0;
        label.setText("");
    }
}
