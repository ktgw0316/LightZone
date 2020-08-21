/* Copyright (C) 2019- Masahiro Kitagawa */

package com.lightcrafts.prefs;

import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.MouseEvent;

import static com.lightcrafts.prefs.LocalePresenter.LanguageItemConverter.availableLanguageItems;

class LocaleItem extends PreferencesItem implements LocaleContract.View {

    private JComboBox<String> combo;
    private LocalePresenter presenter;

    LocaleItem(JTextArea help) {
        super(help);
        combo = new JComboBox<>();
        availableLanguageItems.forEach((item, lang) -> combo.addItem(item));
        combo.setEditable(false);
        addHelpListeners();

        presenter = new LocalePresenter();
        presenter.attachView(this);
    }

    @Override
    public String getLabel() {
        return "Language"; // Do not localize this.
    }

    @Override
    public String getHelp(MouseEvent e) {
        return "Change the user interface language."; // Do not localize this.
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
        presenter.commit();
    }

    @Override
    public void restore() {
        presenter.restore();
    }

    @Override
    public void setSelectedItem(@NotNull String item) {
        combo.setSelectedItem(item);
    }

    @Override
    public String getSelectedItem() {
        return (String) combo.getSelectedItem();
    }
}
