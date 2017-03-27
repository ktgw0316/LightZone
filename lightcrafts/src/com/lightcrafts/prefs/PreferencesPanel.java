/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import com.lightcrafts.platform.Platform;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

public class PreferencesPanel extends JPanel {

    private ArrayList<PreferencesItem> items;
    private HelpArea help;

    PreferencesPanel() {
        setLayout(new GridBagLayout());
        items = new ArrayList<PreferencesItem>();
        help = new HelpArea();
        addItems();
        addFootnote();
    }

    private void addItems() {
        addItem(this, items, new HeapLimitItem(help));
        addItem(this, items, new UpdateInteractiveItem(help));
        addItem(this, items, new ScratchFileItem(help));
        if (Platform.isLinux()) {
            addItem(this, items, new DisplayProfileItem(help));
        }
        addItem(this, items, new BrowserCacheItem(help));
//        addItem(this, items, new CheckForUpdateItem(help));
        addItem(this, items, new SaveDirectoryItem(help));
        addItem(this, items, new AutoSaveItem(help));
        addItem(this, items, new OtherAppIntegrationItem(help));
        addItem(this, items, new ShowToolTitlesItem(help));
        addItem(this, items, new ClearPrefsItem(help));
//        addItem(this, items, new VideoLearningCenterItem(help));
    }

    private static void addItem(
        Container container,
        Collection<PreferencesItem> items,
        PreferencesItem item
    ) {
        GridBagConstraints c = new GridBagConstraints();

        String name = item.getLabel();
        JLabel label = new JLabel(name + ":");
        label.setHorizontalAlignment(SwingConstants.RIGHT);

        JComponent comp = item.getComponent();

        c.gridy = items.size();
        c.ipadx = 2;
        c.ipady = 4;
        c.insets = new Insets(2, 2, 2, 2);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.anchor = GridBagConstraints.EAST;
        c.gridx = 0;
        container.add(label, c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        container.add(comp, c);

        item.restore();

        items.add(item);
    }

    private void addFootnote() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = items.size() + 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 5, 0, 5);

        Dimension size = help.getPreferredSize();
        size = new Dimension(size.width, 70);
        help.setPreferredSize(size);

        add(help, c);
    }

    void commit() {
        for (PreferencesItem item : items) {
            item.commit();
        }
    }
}
