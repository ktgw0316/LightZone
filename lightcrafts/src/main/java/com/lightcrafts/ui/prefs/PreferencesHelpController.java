package com.lightcrafts.ui.prefs;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.lightcrafts.ui.prefs.Locale.LOCALE;

final class PreferencesHelpController {
    private final HelpArea helpArea;

    PreferencesHelpController(HelpArea helpArea) {
        this.helpArea = helpArea;
    }

    void register(JComponent component, HelpMetadata metadata) {
        var listener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                String text = metadata.text();
                if (metadata.requiresRestart()) {
                    text += "  " + LOCALE.get("PreferencesRestartMessage");
                }
                helpArea.setText(text);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                helpArea.setText("");
            }
        };
        addListenerRecursively(component, listener);
    }

    private static void addListenerRecursively(
        Component component, MouseAdapter listener
    ) {
        component.addMouseListener(listener);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                addListenerRecursively(child, listener);
            }
        }
    }

    record HelpMetadata(String text, boolean requiresRestart) {
    }
}
