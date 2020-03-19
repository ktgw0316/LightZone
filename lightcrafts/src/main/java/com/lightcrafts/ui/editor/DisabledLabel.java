/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

// Presents a user message in a disabled editor when there are no preview
// images to show.

class DisabledLabel extends Box {

    private final static Color TextColor =
        LightZoneSkin.Colors.BrowserBackground.darker();

    private final static Color BackgroundColor =
        LightZoneSkin.Colors.BrowserBackground.brighter();

    private final static Color BorderColor = TextColor;

    DisabledLabel(String text) {
        super(BoxLayout.Y_AXIS);

        JLabel label = new JLabel(text);
        label.setForeground(TextColor);
        label.setBackground(BackgroundColor);
        label.setOpaque(true);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        Border emptyBorder = BorderFactory.createEmptyBorder(20, 80, 20, 80);
        Border lineBorder = BorderFactory.createLineBorder(BorderColor);
        Border border =
            BorderFactory.createCompoundBorder(lineBorder, emptyBorder);
        label.setBorder(border);

        Box box = Box.createHorizontalBox();
        box.add(Box.createHorizontalGlue());
        box.add(label);
        box.add(Box.createHorizontalGlue());

        add(Box.createVerticalGlue());
        add(box);
        add(Box.createVerticalGlue());
    }
}
