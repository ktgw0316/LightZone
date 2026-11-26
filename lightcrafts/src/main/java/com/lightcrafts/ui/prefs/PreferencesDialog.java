/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.prefs;

import com.lightcrafts.application.prefs.PreferencesEffect;
import com.lightcrafts.application.prefs.PreferencesMessage;
import com.lightcrafts.application.prefs.PreferencesModel;
import com.lightcrafts.application.prefs.PreferencesUpdate;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.utils.Version;

import javax.swing.*;
import java.awt.*;

import static com.lightcrafts.ui.prefs.Locale.LOCALE;

public class PreferencesDialog extends JPanel {
    private Component parent;
    private PreferencesEffectHandler effectHandler;
    private PreferencesUi ui;
    private SavePrefsPanel save;
    private CopyrightPrefsPanel copyright;
    private PreferencesPanel general;
    private PreferencesModel model;

    public PreferencesDialog() {
    }

    private PreferencesDialog(
        Component parent,
        PreferencesModel initialModel,
        ImageExportOptions defaultExportOptions,
        PreferencesEffectHandler effectHandler,
        PreferencesUi ui
    ) {
        this.parent = parent;
        model = initialModel;
        this.effectHandler = effectHandler;
        this.ui = ui;
        save = new SavePrefsPanel(
            model.saveFormat(), defaultExportOptions, this::dispatch
        );
        copyright = new CopyrightPrefsPanel(
            model.copyright(), this::dispatch
        );
        general = new PreferencesPanel(model, this::dispatch);
    }

    public static void showDialog(
        Component parent,
        PreferencesModel model,
        ImageExportOptions defaultExportOptions,
        PreferencesEffectHandler effectHandler,
        PreferencesUi ui
    ) {
        new PreferencesDialog(
            parent, model, defaultExportOptions, effectHandler, ui
        ).displayDialog();
    }

    private void displayDialog() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(LOCALE.get("GeneralPreferencesTab"), general);
        tabs.addTab(LOCALE.get("SavePreferencesTab"), save);
        tabs.addTab(LOCALE.get("CopyrightPreferencesTab"), copyright);

        int result = JOptionPane.showOptionDialog(
            parent,
            tabs,
            Version.getApplicationName() + " " +
                LOCALE.get("PreferencesDialogTitle"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            new Object[] {
                LOCALE.get("PreferencesDialogOkButton"),
                LOCALE.get("PreferencesDialogCancelButton")
            },
            LOCALE.get("PreferencesDialogOkButton")
        );
        if (result == 0) {
            dispatch(new PreferencesMessage.Apply());
        }
    }

    private void dispatch(PreferencesMessage message) {
        PreferencesUpdate.Transition transition =
            PreferencesUpdate.update(model, message);
        model = transition.model();
        general.render(model);
        save.render(model.saveFormat());
        copyright.render(model.copyright());
        for (PreferencesEffect effect : transition.effects()) {
            effectHandler.handle(
                effect,
                save.getSelectedExportOptions(),
                this::dispatch,
                ui
            );
        }
    }

}
