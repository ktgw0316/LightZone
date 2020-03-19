/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

import static com.lightcrafts.prefs.Locale.LOCALE;

class UpdateInteractiveItem extends PreferencesItem {

    private final static String Package = "/com/lightcrafts/model/ImageEditor";
    private final static String Key = "InteractiveUpdate";

    private final static Preferences Prefs =
        Preferences.userRoot().node(Package);

    private final static String FrequentItem =
        LOCALE.get("UpdateFrequentLabel");
    private final static String NormalItem =
        LOCALE.get("UpdateNormalLabel");
    private final static String InfrequentItem =
        LOCALE.get("UpdateInfrequentLabel");

    private final static String DefaultItem = NormalItem;

    private JComboBox combo;

    UpdateInteractiveItem(JTextArea help) {
        super(help);
        combo = new JComboBox();
        combo.addItem(FrequentItem);
        combo.addItem(NormalItem);
        combo.addItem(InfrequentItem);
        combo.setSelectedItem(DefaultItem);
        combo.setEditable(false);
        addHelpListeners();
    }

    public String getLabel() {
        return LOCALE.get("UpdateInteractiveLabel");
    }

    public String getHelp(MouseEvent e) {
        return LOCALE.get("UpdateInteractiveHelp");
    }

    public boolean requiresRestart() {
        return false;
    }

    public JComponent getComponent() {
        Box box = Box.createHorizontalBox();
        box.add(combo);
        box.add(Box.createHorizontalGlue());
        return box;
    }

    public void commit() {
        String item = (String) combo.getSelectedItem();
        int i = itemToInt(item);
        Prefs.putInt(Key, i);
    }

    public void restore() {
        int defaultInt = itemToInt(DefaultItem);
        int i = Prefs.getInt(Key, defaultInt);
        String item = intToItem(i);
        combo.setSelectedItem(item);
    }

    private static String intToItem(int i) {
        switch (i) {
            case 0:
                return FrequentItem;
            case 1:
                return NormalItem;
            case 2:
                return InfrequentItem;
        }
        assert false : "Invalid interactive update option in intToItem()";
        return null;
    }

    private static int itemToInt(String item) {
        if (item.equals(FrequentItem)) {
            return 0;
        }
        if (item.equals(NormalItem)) {
            return 1;
        }
        if (item.equals(InfrequentItem)) {
            return 2;
        }
        assert false : "Invalid interactive update option in itemToInt()";
        return -1;
    }
}
