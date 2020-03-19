/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.toolkit.IconFontFactory;
import com.lightcrafts.ui.toolkit.ImageOnlyButton;

import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.prefs.Preferences;

class SortUpDownCtrl extends JToggleButton implements ItemListener {

    // Remember the up/down option between instances.
    private final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/ui/browser/ctrls"
    );
    private final static String InvertedKey = "SortInverted";

    private final static Icon UpIcon = IconFontFactory.buildIcon("sort_up");
    private final static Icon DownIcon = IconFontFactory.buildIcon("sort_down");

    private AbstractImageBrowser images;

    SortUpDownCtrl(AbstractImageBrowser images) {
        this.images = images;
        setIcon(DownIcon);
        setToolTipText(LOCALE.get("SortUpToolTip"));
        ImageOnlyButton.setStyle(this);
        addItemListener(this);
        boolean inverted = Prefs.getBoolean(InvertedKey, false);
        if (inverted) {
            setSelected(true);
        }
    }

    public void itemStateChanged(ItemEvent event) {
        boolean selected =
            (event.getStateChange() == ItemEvent.SELECTED);
        images.setSortInverted(selected);
        if (selected) {
            setToolTipText(LOCALE.get("SortDownToolTip"));
            setIcon(UpIcon);
        }
        else {
            setToolTipText(LOCALE.get("SortUpToolTip"));
            setIcon(DownIcon);
        }
        Prefs.putBoolean(InvertedKey, selected);
    }
}
