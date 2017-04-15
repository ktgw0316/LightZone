/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.generic;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/** This is a Container to handle the particular layout constraints of groups
  * of GenericSliders.  It aligns the labels, sliders, and text fields of
  * these Components into rows and columns.
  * <p>
  * Add GenericSliders to this Container by calling
  * <code>addGenericSlider()</code>.  This method will remove all children
  * from its argument before adding them into the GenericSliderContainer.
  */

class GenericSliderContainer extends JPanel {

    private int count;

    GenericSliderContainer() {
        super(new GridBagLayout());
        Border border = BorderFactory.createEmptyBorder(0, 3, 0, 3);
        setBorder(border);
    }

    void addGenericSlider(GenericSlider slider) {
        slider.removeAll();

        Component left = slider.getLabel();
        Component center = slider.getSlider();
        Component text = slider.getText();

        Box right = Box.createVerticalBox();
        right.add(Box.createVerticalGlue());
        right.add(text);
        right.add(Box.createVerticalGlue());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridy = count;
        c.weighty = 1.;

        c.gridx = 0;
        c.weightx = 0.;
        add(left, c);

        c.gridx = 1;
        c.weightx = 1.;
        add(center, c);

        c.gridx = 2;
        c.weightx = 0.;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(right, c);

        count++;
    }
}
