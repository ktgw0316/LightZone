/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.*;

import static com.lightcrafts.prefs.Locale.LOCALE;

/**
 * The PreferencesPanel is built out of PreferenceItems.  Every PreferenceItem
 * provides help text, and this base implementation ensures that the help
 * text gets displayed at the right times.
 */
abstract class PreferencesItem {

    private JTextArea help;

    PreferencesItem(JTextArea help) {
        this.help = help;
    }

    /**
     * Derived classes must call this after their component is initialized
     * to switch on help updates.
     */
    void addHelpListeners() {
        MouseListener listener = new MouseAdapter() {
            public void mouseEntered(MouseEvent event) {
                String text = getHelp(event);
                if (requiresRestart()) {
                    text += "  " + LOCALE.get("PreferencesRestartMessage");
                }
                help.setText(text);
            }
            public void mouseExited(MouseEvent event) {
                help.setText("");
            }
        };
        JComponent comp = getComponent();
        addListenerRecurse(comp, listener);
    }

    private void addListenerRecurse(Component comp, MouseListener listener) {
        comp.addMouseListener(listener);
        if (comp instanceof Container) {
            Component[] children = ((Container) comp).getComponents();
            for (Component child : children) {
                addListenerRecurse(child, listener);
            }
        }
    }

    abstract String getHelp(MouseEvent event);

    /**
     * A user-presentable text label for this item, shown right-justified to
     * the left of the JComponent and with a trailing colon.
     */
    abstract String getLabel();

    /**
     * Determine whether this preference change takes immediate effect, or
     * is effective after restart.
     */
    abstract boolean requiresRestart();

    /**
     * The actual preference control, shown next to the label.  This control
     * should be initialized from preference values in restore() and should
     * push its values to preferences in commit(), but otherwise should not
     * alter preference values at all.
     */
    abstract JComponent getComponent();

    /**
     * Take the current values from the JComponent control and push them to
     * preferences.
     */
    abstract void commit();

    /**
     * Read the current preference values and use them to initialize the
     * JComponent control.
     */
    abstract void restore();
}
