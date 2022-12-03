/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import javax.swing.*;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import static com.lightcrafts.ui.print.Locale.LOCALE;

class CenteredCheckBox extends JCheckBox implements PrintLayoutModelListener {

    CenteredCheckBox(final PrintLayoutModel model) {
        super(LOCALE.get("CenteredCheck"));
        boolean centered = model.isKeepCentered();
        setSelected(centered);
        addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    boolean doCenter =
                        (event.getStateChange() == ItemEvent.SELECTED);
                    model.setKeepCentered(doCenter);
                }
            }
        );
        model.addListener(this);
    }

    public void layoutChanged(PrintLayoutModel source) {
        boolean selected = source.isKeepCentered();
        setSelected(selected);
    }
}
