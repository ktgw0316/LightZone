/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import com.lightcrafts.model.RenderingIntent;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.toolkit.WidePopupComboBox;
import com.lightcrafts.image.color.ColorProfileInfo;

import static com.lightcrafts.ui.print.Locale.LOCALE;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.List;

class ColorManagementPanel extends JPanel {

    private static Collection<ColorProfileInfo> PrinterProfiles =
        Platform.getPlatform().getPrinterProfiles();

    private final static String APPLICATION_COLORS = LOCALE.get("AppColors");
    private final static String PRINTER_COLORS = LOCALE.get("PrinterColors");

    private final static int PreferredComboWidth = 400;

    private PrintLayoutModel model;

    private JComboBox colorHandling;
    private JComboBox printerProfile;
    private JComboBox renderingIntent;

    private JPanel titlePanel;  // intermediary container allows title borders

    ColorManagementPanel(PrintLayoutModel model) {
        this.model = model;

        titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        Border border = BorderFactory.createTitledBorder(
            LOCALE.get("ColorTitle")
        );
        titlePanel.setBorder(border);

        addColorHandling();
        titlePanel.add(Box.createVerticalStrut(3));
        addPrinterProfile();
        titlePanel.add(Box.createVerticalStrut(3));
        addRenderingIntent();

        // Initialize the color handling last, because changes here can trigger
        // updates in the other controls:
        ColorProfileInfo profile = model.getColorProfileInfo();
        Object selected =
            (profile != null) ? APPLICATION_COLORS : PRINTER_COLORS;
        colorHandling.setSelectedItem(selected);

        setLayout(new BorderLayout());
        add(titlePanel);
    }

    private void addColorHandling() {
        colorHandling = new JComboBox();
        colorHandling.addItem(APPLICATION_COLORS);
        colorHandling.addItem(PRINTER_COLORS);

        setFixedSize(colorHandling);

        Box box = createLabelCombo(LOCALE.get("HandlingLabel"), colorHandling);
        titlePanel.add(box);

        // Add the behavior where color items are enabled and disabled
        // in response to the "Color Handling" selection:
        colorHandling.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (e.getItem() == APPLICATION_COLORS) {
                            renderingIntent.setEnabled(true);
                            printerProfile.setEnabled(true);
//                            blackPointCompensation.setEnabled(true);

                            ColorProfileInfo cpi = null;
                            Object stuff = printerProfile.getSelectedItem();
                            if (stuff instanceof ColorProfileInfo) {
                                cpi = (ColorProfileInfo) stuff;
                            }
                            model.setColorProfile(cpi);
                        }
                        else {
                            renderingIntent.setEnabled(false);
                            printerProfile.setEnabled(false);

                            model.setColorProfile(null);
                        }
                    }
                }
            }
        );
        // Don't initialize the color handling combo here; wait for
        // all the other controls to be initialized first.
    }

    private void addPrinterProfile() {
        printerProfile = new WidePopupComboBox();

        setFixedSize(printerProfile);

        List<ColorProfileInfo> profiles =
            ColorProfileInfo.arrangeForMenu(PrinterProfiles);
        for (ColorProfileInfo profile : profiles) {
            printerProfile.addItem(profile);
        }
        // Initialize the printerProfile selection from the model:
        ColorProfileInfo initProfile = model.getColorProfileInfo();
        if (initProfile != null) {
            for (ColorProfileInfo profile : profiles) {
                if (profile != null) {
                    // Don't require strict equality for ColorProfileInfo's.
                    // Just ask that the names match:
                    if (profile.getName().equals(initProfile.getName())) {
                        printerProfile.setSelectedItem(profile);
                    }
                }
            }
        }
        // Avoid scroll bars in menus, even if some items may not be accessible:
        printerProfile.setMaximumRowCount(25);

        Box box = createLabelCombo(LOCALE.get("ProfileLabel"), printerProfile);
        titlePanel.add(box);

        printerProfile.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        final ColorProfileInfo cpi =
                            (ColorProfileInfo)printerProfile.getSelectedItem();
                        model.setColorProfile(cpi);
                    }
                }
            }
        );
    }

    private void addRenderingIntent() {
        renderingIntent = new JComboBox();

        setFixedSize(renderingIntent);

        RenderingIntent[] intents = RenderingIntent.getAll();
        for (RenderingIntent intent : intents) {
            renderingIntent.addItem(intent);
        }
        RenderingIntent intent = model.getRenderingIntent();
        renderingIntent.setSelectedItem(intent);

        Box box = createLabelCombo(LOCALE.get("IntentLabel"), renderingIntent);
        titlePanel.add(box);

        renderingIntent.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        RenderingIntent intent =
                            (RenderingIntent) renderingIntent.getSelectedItem();
                        model.setRenderingIntent(intent);
                    }
                }
            }
        );
    }

    private static void setFixedSize(JComboBox combo) {
        Dimension size = combo.getPreferredSize();
        size = new Dimension(PreferredComboWidth, size.height);
        combo.setMinimumSize(size);
        combo.setPreferredSize(size);
        combo.setMaximumSize(size);
    }

    private static Box createLabelCombo(String name, JComboBox combo) {
        Box box = Box.createHorizontalBox();
        box.add(Box.createHorizontalGlue());
        box.add(new JLabel(name + ':'));
        box.add(Box.createHorizontalStrut(3));
        box.add(combo);
        return box;
    }

    public static void main(String[] args) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new ColorManagementPanel(new PrintLayoutModel(100, 100)));
        JFrame frame = new JFrame("ColorManagementPanel Test");
        frame.setContentPane(panel);
        frame.setLocation(100, 100);
        frame.pack();
        frame.setVisible(true);
    }
}
/* vim:set et sw=4 ts=4: */
