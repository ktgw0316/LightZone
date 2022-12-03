/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import java.awt.*;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: May 19, 2007
 * Time: 11:30:39 AM
 */
public class PaneTitle extends JPanel {
    public Box createLabelBox(String text) {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(JLabel.LEADING);
        Font font = label.getFont();
        font = font.deriveFont(20f);
        label.setFont(font);

        Box labelBox = Box.createHorizontalBox();
        labelBox.add(Box.createHorizontalStrut(8));
        labelBox.add(label);
        labelBox.add(Box.createHorizontalGlue());

        return labelBox;
    }

    public void assembleTitle(Box labelBox) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setBackground(
            LightZoneSkin.Colors.ToolPanesBackground
        );

        add(Box.createVerticalStrut(4));
        add(labelBox);
        add(Box.createVerticalStrut(4));
        add(new JSeparator());
        add(Box.createVerticalStrut(4));

        setMaximumSize(new Dimension(400, 24));
        setMinimumSize(new Dimension(200, 24));
    }

    public PaneTitle(String text) {
        Box labelBox = createLabelBox(text);
        assembleTitle(labelBox);
    }

    protected PaneTitle() {

    }
}
