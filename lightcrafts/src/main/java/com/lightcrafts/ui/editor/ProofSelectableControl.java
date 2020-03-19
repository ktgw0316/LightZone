/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.model.Engine;
import com.lightcrafts.model.PrintSettings;
import com.lightcrafts.model.RenderingIntent;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.help.HelpConstants;
import com.lightcrafts.ui.operation.SelectableControl;
import com.lightcrafts.ui.toolkit.WidePopupComboBox;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.color.ICC_Profile;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Collection;
import java.util.List;
import java.util.prefs.Preferences;

import static com.lightcrafts.ui.editor.Locale.LOCALE;

/**
 * A SelectableControl to control the argument to Engine.preview(),
 * a PrintSettings object holding a color profile and a rendering intent
 * (or null).
 */
final class ProofSelectableControl extends SelectableControl {

    private static Collection<ColorProfileInfo> PrinterProfiles =
        Platform.getPlatform().getPrinterProfiles();

    private static Preferences Prefs =
        Preferences.userRoot().node("/com/lightcrafts/ui/editor");

    private final static String ProofProfileKey = "ProofColorProfile";
    private final static String ProofIntentKey = "ProofRenderingIntent";

    private final static int PreferredComboWidth = 150;

    // The model for this control:
    private PrintSettings settings;

    // The Engine controlled by this control:
    private Engine engine;

    private JComboBox printerProfile;
    private JComboBox renderingIntent;

    private JPanel content;

    ProofSelectableControl(Engine engine) {
        this.engine = engine;

        setTitle(LOCALE.get("ProofControlTitle"));

        settings = new PrintSettings();

        initPrinterProfile();
        initRenderingIntent();

        initContent();

        setContent(content);
    }

    public void addNotify() {
        super.addNotify();
        engine.preview(settings);
    }

    public void removeNotify() {
        super.removeNotify();
        engine.preview(null);
    }

    private void initPrinterProfile() {
        printerProfile = new WidePopupComboBox();

        if (PrinterProfiles != null) {
            final List<ColorProfileInfo> profiles =
                ColorProfileInfo.arrangeForMenu(PrinterProfiles);
            for (ColorProfileInfo profile : profiles) {
                printerProfile.addItem(profile);
            }
            // Initialize the printerProfile selection from Preferences:
            final String initProfile = Prefs.get(ProofProfileKey, null);
            if (initProfile != null) {
                for (ColorProfileInfo profile : profiles) {
                    if (profile != null) {
                        // Don't require strict equality for ColorProfileInfo's.
                        // Just ask that the names match:
                        if (profile.getName().equals(initProfile)) {
                            printerProfile.setSelectedItem(profile);
                            settings.setColorProfile(profile.getICCProfile());
                        }
                    }
                }
            }
        }
        // Avoid scroll bars in menus, even if some items may not be accessible:
        printerProfile.setMaximumRowCount(25);

        printerProfile.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                ColorProfileInfo cpi =
                    (ColorProfileInfo) printerProfile.getSelectedItem();
                ICC_Profile profile = cpi.getICCProfile();
                settings.setColorProfile(profile);
                engine.preview(settings);
                Prefs.put(ProofProfileKey, cpi.getName());
            }
        });
        printerProfile.addMouseWheelListener(e -> {
            JComboBox source = (JComboBox) e.getComponent();
            if (!source.hasFocus()) {
                return;
            }
            final int rot = e.getWheelRotation();
            if (rot == 0) return;
            final int ni = source.getSelectedIndex() + rot;
            if (ni >= 0 && ni < source.getItemCount()) {
                source.setSelectedIndex(ni);
            }
        });
    }

    private void initRenderingIntent() {
        renderingIntent = new WidePopupComboBox();

        RenderingIntent[] intents = RenderingIntent.getAll();
        for (RenderingIntent intent : intents) {
            renderingIntent.addItem(intent);
        }
        // Initialize the renderingIntent selection from Preferences:
        String initIntent = Prefs.get(ProofIntentKey, null);
        if (initIntent != null) {
            for (RenderingIntent intent : intents) {
                if (intent.toString().equals(initIntent)) {
                    renderingIntent.setSelectedItem(intent);
                    settings.setRenderingIntent(intent);
                }
            }
        }
        renderingIntent.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        RenderingIntent intent =
                            (RenderingIntent) renderingIntent.getSelectedItem();
                        settings.setRenderingIntent(intent);
                        engine.preview(settings);
                        Prefs.put(ProofIntentKey, intent.toString());
                    }
                }
            }
        );
        renderingIntent.addMouseWheelListener(
            new MouseWheelListener() {
                public void mouseWheelMoved(MouseWheelEvent e) {
                    JComboBox source = (JComboBox) e.getComponent();
                    if (!source.hasFocus()) {
                        return;
                    }
                    final int rot = e.getWheelRotation();
                    if (rot == 0) return;
                    final int ni = source.getSelectedIndex() + rot;
                    if (ni >= 0 && ni < source.getItemCount()) {
                        source.setSelectedIndex(ni);
                    }
                }
            }
        );
    }

    private void initContent() {
        content = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel profileLabel = new JLabel(LOCALE.get("ProofProfileLabel") + ": ");
        profileLabel.setMinimumSize(profileLabel.getPreferredSize());
        profileLabel.setFont(getFont());
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        content.add(profileLabel, c);

        printerProfile.setFont(getFont());
        setFixedSize(printerProfile);
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        content.add(printerProfile, c);

        JLabel intentLabel = new JLabel(LOCALE.get("ProofIntentLabel") + ": ");
        intentLabel.setMinimumSize(intentLabel.getPreferredSize());
        intentLabel.setFont(getFont());
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        content.add(intentLabel, c);

        renderingIntent.setFont(getFont());
        setFixedSize(renderingIntent);
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        content.add(renderingIntent, c);

        Border border = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        content.setBorder(border);
    }

    private static void setFixedSize(JComboBox combo) {
        Dimension size = combo.getPreferredSize();
        size = new Dimension(PreferredComboWidth, size.height);
        combo.setMinimumSize(size);
        combo.setPreferredSize(size);
        combo.setMaximumSize(size);
    }

    protected String getHelpTopic() {
        return HelpConstants.HELP_TOOL_PROOF;
    }
}
