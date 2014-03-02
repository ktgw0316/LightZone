/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.browser.model.ImageDatumComparator;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;

import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.prefs.Preferences;

class SortCombo extends JComboBox implements ItemListener {

    // Remember the comparator choice between instances.
    private final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/ui/browser/ctrls"
    );
    private final static String ComparatorKey = "SortComparator";

    private AbstractImageBrowser images;

    SortCombo(AbstractImageBrowser images) {
        this.images = images;
        ImageDatumComparator[] comparators = ImageDatumComparator.getAll();
        for (ImageDatumComparator comparator : comparators) {
            addItem(comparator);
        }
        addItemListener(this);
        setToolTipText(LOCALE.get("SortToolTip"));
        setFocusable(false);
        setFont(new Font("SansSerif", Font.PLAIN, 12));
        setFixedSize();
        String compName = Prefs.get(ComparatorKey, null);
        for (ImageDatumComparator comparator : comparators) {
            if (comparator.toString().equals(compName)) {
                setSelectedItem(comparator);
            }
        }
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            ImageDatumComparator comp = (ImageDatumComparator) e.getItem();
            images.setSort(comp);
            Prefs.put(ComparatorKey, comp.toString());
        }
    }

    private void setFixedSize() {
        Dimension size = getPreferredSize();
        setMinimumSize(size);
        setPreferredSize(size);
        setMaximumSize(size);
    }
}
