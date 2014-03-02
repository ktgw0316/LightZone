/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.colorbalance;

import com.lightcrafts.ui.layout.Box;
import com.lightcrafts.ui.layout.BoxLayout;
import static com.lightcrafts.ui.operation.colorbalance.Locale.LOCALE;

import javax.swing.*;
import java.awt.*;

class ColorText extends Box {

    private Box labels;
    private Box values;

    ColorText(Color color) {
        super(BoxLayout.X_AXIS);

        labels = Box.createVerticalBox();
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
        label.setAlignmentX(1f);
        return label;
    }

    public static void main(String[] args) {
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new ColorText(new Color(1, 2, 3)));
        JFrame frame = new JFrame();
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocation(100, 100);
        frame.setVisible(true);
    }
}
