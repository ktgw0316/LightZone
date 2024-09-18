/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.colorbalance;

import static com.lightcrafts.ui.operation.colorbalance.Locale.LOCALE;

import javax.swing.*;
import java.awt.*;

class ColorText extends Box {

    private final Box values;

    ColorText(Color color) {
        super(BoxLayout.X_AXIS);

        final Box labels = Box.createVerticalBox();
        labels.add(createLabel(LOCALE.get("RedLabel") + ':'));
        labels.add(createLabel(LOCALE.get("GreenLabel") + ':'));
        labels.add(createLabel(LOCALE.get("BlueLabel") + ':'));
        add(labels);

        add(Box.createHorizontalStrut(6));

        values = Box.createVerticalBox();
        add(values);

        // Make sure we're wide enough for all colors, and don't resize when
        // the color text becomes shorter:
        setColor(Color.white);
        Dimension size = values.getPreferredSize();
        size.setSize(size.width + 1, size.height);
        values.setMinimumSize(size);
        values.setPreferredSize(size);
        values.setMaximumSize(size);

        setColor(color);
    }

    void setColor(Color color) {
        values.removeAll();
        values.add(createLabel(color.getRed()));
        values.add(createLabel(color.getGreen()));
        values.add(createLabel(color.getBlue()));
        validate();
        repaint();
    }

    private static JLabel createLabel(int number) {
        return createLabel(Integer.toString(number));
    }

    // TODO: this should be defined elsewhere
    public final static Font ControlFont = new Font("SansSerif", Font.PLAIN, 13);

    private static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ControlFont);
        label.setAlignmentX(Component.RIGHT_ALIGNMENT);
        label.setHorizontalAlignment(JLabel.TRAILING);
        return label;
    }
}
