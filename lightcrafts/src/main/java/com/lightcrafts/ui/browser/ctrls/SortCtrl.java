/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.browser.model.ImageDatumComparator;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;

import javax.swing.*;
import java.awt.*;

public class SortCtrl extends JPanel {

    private SortCombo combo;
    private SortUpDownCtrl toggle;

    public SortCtrl(AbstractImageBrowser browser) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // add(new JLabel("Sort: "));

        combo = new SortCombo(browser);
        add(combo);

        add(Box.createHorizontalStrut(4));

        toggle = new SortUpDownCtrl(browser);
        add(toggle);

        Font font = getFont();
        font = font.deriveFont(10f);
        setFont(font);

        setFocusable(false);
    }

    public ImageDatumComparator getSort() {
        return (ImageDatumComparator) combo.getSelectedItem();
    }

    public boolean getSortInverted() {
        return toggle.isSelected();
    }
}
