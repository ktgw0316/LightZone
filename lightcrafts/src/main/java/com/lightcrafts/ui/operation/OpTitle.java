/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2024-     Masahiro Kitagawa */

package com.lightcrafts.ui.operation;

import com.formdev.flatlaf.icons.FlatInternalFrameCloseIcon;
import com.lightcrafts.model.Operation;
import static com.lightcrafts.ui.operation.Locale.LOCALE;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A title bar for OpControls, including a label for the Operation and
 * standard controls like show/hide, enable/disable, and remove.
 */

class OpTitle extends SelectableTitle {

    // Preferences are used to remember OpControl presets:
    private final static Preferences Prefs = Preferences.userNodeForPackage(OpTitle.class);
    private final static String PresetsKey = "Presets";

    private final OpControl control;
    private final OpTitleEditor editor;
    private JCheckBox activeCheckBox;
    private JButton removeButton;

    OpTitle(final OpControl control, final OpStack stack) {
        super(control);
        this.control = control;

        editor = new OpTitleEditor(this, control.undoSupport);
        editor.setLabel(label);

        Operation op = control.getOperation();
        BufferedImage image = OpActions.getIcon(op);
        setIcon(image);

        if (control.isRawCorrection()) {
            return;
        }

        activeCheckBox = new JCheckBox("", true);
        activeCheckBox.setToolTipText(LOCALE.get("DisableToolTip"));
        activeCheckBox.setRolloverEnabled(true);
        activeCheckBox.setSelected(true);
        activeCheckBox.addActionListener(event -> {
            boolean active = activeCheckBox.isSelected();
            String tip = active ? LOCALE.get("DisableToolTip") : LOCALE.get("EnableToolTip");
            activeCheckBox.setToolTipText(tip);
            control.setActivated(activeCheckBox.isSelected());
        });
        buttonBox.add(activeCheckBox);

        final Dimension removeButtonSize = activeCheckBox.getPreferredSize();
        removeButton = new JButton(new FlatInternalFrameCloseIcon());
        removeButton.setToolTipText(LOCALE.get("RemoveToolTip"));
        removeButton.setPreferredSize(removeButtonSize);
        removeButton.setMaximumSize(removeButtonSize);
        removeButton.setContentAreaFilled(false);
        removeButton.setBorder(null);
        removeButton.addActionListener(event -> stack.removeControl(control));
        buttonBox.add(removeButton);
    }

    void setActive(boolean active) {
        if (activeCheckBox != null) {
            // Won't generate an ActionEvent, so no risk of recursion with the
            // activeButton ActionListener:
            activeCheckBox.setSelected(active);
        }
    }

    @Override
    void resetTitle(String title) {
        super.resetTitle(title);
        if (editor != null) {
            // (Called from base class constructor, when editor may be null)
            editor.setLabel(label);
        }
    }

    @Override
    JPopupMenu getPopupMenu() {
        JPopupMenu menu = super.getPopupMenu();

        final boolean isLocked = control.isLocked();

        if (! control.isRawCorrection()) {
            final boolean isActivated = control.isActivated();

            final String activateText = isActivated ?
                LOCALE.get("DisableMenuItem") : LOCALE.get("EnableMenuItem");
            final var activateItem = new JMenuItem(activateText);
            activateItem.addActionListener(event -> control.setActivated(! isActivated));
            activateItem.setEnabled(! isLocked);
            menu.add(activateItem, 0);

            menu.add(new JSeparator(), 1);
        }

        final String lockUnlockText = isLocked ?
            LOCALE.get("UnlockMenuItem") : LOCALE.get("LockMenuItem");
        final var lockUnlockItem = new JMenuItem(lockUnlockText);
        lockUnlockItem.addActionListener(event -> control.setLocked(! isLocked));
        menu.add(lockUnlockItem, 2);

        menu.add(new JSeparator(), 3);

        final var savePresetItem = new JMenuItem(LOCALE.get("RememberPresetMenuItem"));
        savePresetItem.addActionListener(event -> {
            XmlDocument preset = new XmlDocument("Preset");
            control.save(preset.getRoot());
            writePreset(preset);
        });
        menu.add(savePresetItem, 4);

        final var applyPresetItem = new JMenuItem(LOCALE.get("ApplyPresetMenuItem"));
        final XmlDocument preset = readPreset();
        if ((preset == null) || isLocked) {
            applyPresetItem.setEnabled(false);
        }
        else {
            applyPresetItem.addActionListener(event -> {
                try {
                    // Like control.restore(), but with undo:
                    control.restorePresets(preset.getRoot());
                }
                catch (XMLException e) {
                    // No way to back out of this, so leave things as
                    // they are and hope the user can recover.
                    System.err.println("Error in preset restore: " + e.getMessage());
                }
            });
        }
        menu.add(applyPresetItem, 5);

        if (! control.isRawCorrection()) {
            final OpStack stack = findOpStack();

            final var deleteItem = new JMenuItem(LOCALE.get("DeleteMenuItem"));
            deleteItem.addActionListener(event -> stack.removeControl(control));
            deleteItem.setEnabled(! isLocked);

            menu.add(new JSeparator());
            menu.add(deleteItem);
        }
        return menu;
    }

    /**
     * Read in the preset for the current OpControl type from Preferences,
     * or null if no such preset exists.
     */
    private @Nullable XmlDocument readPreset() {
        String key = getPresetsKey();
        String text = Prefs.get(key, "");
        try {
            var in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
            return new XmlDocument(in);
        }
        catch (IOException e) {   // IOException or XMLException
            return null;
        }
    }

    /**
     * Save the given preset in Preferences for the current OpControl type.
     */
    private void writePreset(@NotNull XmlDocument preset) {
        String key = getPresetsKey();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            preset.write(out);
            String text = out.toString(StandardCharsets.UTF_8);
            Prefs.put(key, text);
            Prefs.sync();
        }
        catch (IOException | BackingStoreException e) {
            // Make sure the "Apply Preset" item doesn't get enabled:
            Prefs.remove(key);
        }
    }

    private @NotNull String getPresetsKey() {
        String name = control.getOperation().getType().getName();
        return PresetsKey + name;
    }
}
