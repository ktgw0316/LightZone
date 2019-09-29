/* Copyright (C) 2019- Masahiro Kitagawa */

package com.lightcrafts.prefs;

import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class LocaleItem extends PreferencesItem {

    private final static String Package = "/com/lightcrafts/app";
    private final static String Key = "Locale";

    private final static Preferences Prefs = Preferences.userRoot().node(Package);

    private final static String DefaultItem = "(System Default)";

    static final private Map<String, String> availableLanguageItems;

    static {
        val availableLanguages = Stream.of(
                "", // system default
                "en", "da", "de", "es", "fr", "hu", "it", "ja", "nl", "pl"
        );
        availableLanguageItems = availableLanguages
                .map(l -> Map.entry(languageToItem(l), l))
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    private JComboBox<String> combo;

    LocaleItem(JTextArea help) {
        super(help);
        combo = new JComboBox<>();
        availableLanguageItems.forEach((item, lang) -> combo.addItem(item));
        // combo.setSelectedItem(DefaultItem);
        combo.setEditable(false);
        addHelpListeners();
    }

    @Override
    public String getLabel() {
        return "Language"; // TODO: localize
    }

    @Override
    public String getHelp(MouseEvent e) {
        return "Change the user interface language."; // TODO: localize
    }

    @Override
    public boolean requiresRestart() {
        return true;
    }

    @Override
    public JComponent getComponent() {
        val box = Box.createHorizontalBox();
        box.add(combo);
        box.add(Box.createHorizontalGlue());
        return box;
    }

    @Override
    public void commit() {
        val item = (String) combo.getSelectedItem();
        if (item == null || item.equals(DefaultItem)) {
            Prefs.remove(Key);
        } else {
            val lang = availableLanguageItems.get(item);
            Prefs.put(Key, lang);
            Locale.setDefault(new Locale(lang));
        }
        ResourceBundle.clearCache();
    }

    @Override
    public void restore() {
        val lang = Prefs.get(Key, "");
        combo.setSelectedItem(languageToItem(lang));
        if (!lang.isEmpty()) {
            Locale.setDefault(new Locale(lang));
        }
    }

    @NotNull
    private static String languageToItem(@NotNull String language) {
        if (language.isEmpty()) {
            return DefaultItem;
        }
        val locale = new Locale(language);
        return locale.getDisplayLanguage(locale);
    }
}
