/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolToggleButton;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

class BrowseEditButtons extends Box {

    private final static String BrowseText = LOCALE.get("BrowseModeButtonText");
    private final static String EditText = LOCALE.get("EditModeButtonText");

    private final static String BrowseTip = LOCALE.get("BrowseModeButtonToolTip");
    private final static String EditTip = LOCALE.get("EditModeButtonToolTip");

    private JToggleButton browse;
    private JToggleButton edit;

    // The ComboFrame can update the button states without switching modes,
    // to synchronize the buttons with the layout.
    private boolean isProgrammaticChange;

    BrowseEditButtons(final ComboFrame frame) {
        super(BoxLayout.X_AXIS);

        browse = new CoolToggleButton(CoolButton.ButtonStyle.LEFT);
        browse.setText(BrowseText);
        browse.setToolTipText(BrowseTip);
        Font font = browse.getFont();
        font = font.deriveFont(16f);
        browse.setFont(font);

        edit = new CoolToggleButton(CoolButton.ButtonStyle.RIGHT);
        edit.setText(EditText);
        edit.setToolTipText(EditTip);
        edit.setFont(font);

        ButtonGroup group = new ButtonGroup();
        group.add(browse);
        group.add(edit);

        browse.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (! isProgrammaticChange) {
                            isProgrammaticChange = true;
                            boolean switched = frame.showBrowserPerspective();
                            if (! switched) {
                                EventQueue.invokeLater(
                                    new Runnable() {
                                        public void run() {
                                            isProgrammaticChange = true;
                                            edit.doClick();
                                            isProgrammaticChange = false;
                                        }
                                    }
                                );
                            }
                            isProgrammaticChange = false;
                        }
                        browse.setForeground(LightZoneSkin.Colors.LZOrange);
                    } else
                        browse.setForeground(Color.lightGray);
                }
            }
        );
        edit.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (! isProgrammaticChange) {
                            isProgrammaticChange = true;
                            boolean switched = frame.openSelected();
                            if (! switched) {
                                EventQueue.invokeLater(
                                    new Runnable() {
                                        public void run() {
                                            isProgrammaticChange = true;
                                            browse.doClick();
                                            isProgrammaticChange = false;
                                        }
                                    }
                                );
                            }
                            isProgrammaticChange = false;
                        }
                        edit.setForeground(LightZoneSkin.Colors.LZOrange);
                    } else
                        edit.setForeground(Color.lightGray);
                }
            }
        );
        add(browse);
        add(edit);

        // Don't let the layout get stretched.
//        Dimension size = getPreferredSize();
//        setPreferredSize(size);
//        setMinimumSize(size);
//        setMaximumSize(size);

        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    void setEditSelected() {
        isProgrammaticChange = true;
        edit.setSelected(true);
        isProgrammaticChange = false;
    }

    void setBrowseSelected() {
        isProgrammaticChange = true;
        browse.setSelected(true);
        isProgrammaticChange = false;
    }
}
