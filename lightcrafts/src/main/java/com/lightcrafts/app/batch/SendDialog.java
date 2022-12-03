/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.batch;

import static com.lightcrafts.app.batch.Locale.LOCALE;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ICC_Profile;
import java.awt.event.*;
import java.io.File;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * A dialog to configure a BatchConfig for the "send" operation, which is a
 * form of batch export with simplified options.
 * <p>
 * BatchConfigs that come from this dialog always specify JPEG output at
 * a fixed quality, the sRGB color profile and perceptual rendering
 * intent, always specifying to create new files, and with a fixed output
 * size selected from a narrow range of values.
 */
public class SendDialog extends JDialog {

    private final static String PrefsKey = "Send";

    private final static Map<String, Integer> Sizes =
        new LinkedHashMap<String, Integer>();
    static {
        Sizes.put("320x240", 320);
        Sizes.put("640x480", 640);
        Sizes.put("800x600", 800);
        Sizes.put("1024x768", 1024);
        Sizes.put("1280x960", 1280);
        Sizes.put("1920x1440", 1920);
        Sizes.put("2560x1920", 2560);
        Sizes.put("Don't limit", 0);
    }
    private BatchConfig conf;

    // A flag to indicate that "Send" was clicked, instead of "Cancel"
    private boolean started;

    private SendDialog(final Frame owner, String from, int count) {
        super(owner);

        conf = new BatchConfig();
        conf.restoreFromPrefs(PrefsKey);

        if (! (conf.export instanceof JPEGImageType.ExportOptions)) {
            conf.export = JPEGImageType.INSTANCE.newExportOptions();
        }
        JPEGImageType.ExportOptions export =
            (JPEGImageType.ExportOptions) conf.export;
        export.quality.setValue(85);
        export.renderingIntent.setValue(ICC_Profile.icPerceptual);
        export.blackPointCompensation.setValue(false);

        if (conf.directory == null) {
            conf.directory = new File(System.getProperty("user.home"));
        }
        // The combo box of allowed output image sizes
        JComboBox sizes = new JComboBox();
        for (String item : Sizes.keySet()) {
            sizes.addItem(item);
        }
        sizes.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        String name = (String) e.getItem();
                        int size = Sizes.get(name);
                        conf.export.resizeWidth.setValue(size);
                        conf.export.resizeHeight.setValue(size);
                    }
                }
            }
        );
        int size = conf.export.resizeWidth.getValue();
        String defaultItem = null;
        for (Map.Entry<String, Integer> entry : Sizes.entrySet()) {
            int value = entry.getValue();
            if (value == size) {
                defaultItem = entry.getKey();
            }
        }
        if (defaultItem != null) {
            sizes.setSelectedItem(defaultItem);
        }
        final JTextField dirText = new JTextField(conf.directory.getName());
        dirText.setEditable(false);

        JButton dirButton = new JButton(
            LOCALE.get("SendDialogChooserButton")
        );
        dirButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Platform platform = Platform.getPlatform();
                    FileChooser chooser = platform.getFileChooser();
                    File directory = chooser.chooseDirectory(
                        LOCALE.get("SendDialogChooserTitle"),
                        conf.directory, SendDialog.this, false
                    );
                    if (directory != null) {
                        conf.directory = directory;
                        String name = directory.getName();
                        dirText.setText(name);
                        if (dirText.getPreferredSize().width >
                            dirText.getSize().width
                        ) {
                            pack();
                        }
                    }
                }
            }
        );
        JButton start = new JButton(LOCALE.get("SendDialogStartButton"));
        start.setAlignmentX(.5f);

        JButton cancel = new JButton(LOCALE.get("SendDialogCancelButton"));
        cancel.setAlignmentX(.5f);

        ActionListener disposeAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        start.addActionListener(disposeAction);
        cancel.addActionListener(disposeAction);

        start.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    started = true;
                }
            }
        );
        JLabel header;
        if (count > 1) {
            header = new JLabel(
                LOCALE.get(
                    "SendDialogHeaderPlural",
                    Integer.toString(count),
                    '"' + from + '"'
                )
            );
        }
        else {
            header = new JLabel(
                LOCALE.get("SendDialogHeaderSingular", '"' + from + '"')
            );
        }
        HelpButton help = new HelpButton();

        help.setAlignmentX(0f);
        cancel.setAlignmentX(1f);
        start.setAlignmentX(1f);

        Box sizeBox = Box.createHorizontalBox();
        sizeBox.add(sizes);
        sizeBox.add(Box.createHorizontalGlue());

        Box buttons = Box.createHorizontalBox();
        buttons.add(help);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(start);
        buttons.setMaximumSize(buttons.getPreferredSize());

        JPanel content = new JPanel(new GridBagLayout());

        // Sometimes, you have to use a GridBag:

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 6, 6, 8);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        content.add(header, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        content.add(new JLabel(LOCALE.get("SendDialogOutputLabel")), c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        content.add(dirText, c);

        c.gridx = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        content.add(dirButton, c);

        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.EAST;
        content.add(new JLabel(LOCALE.get("SendDialogSizelabel")), c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        content.add(sizeBox, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        content.add(buttons, c);

        // Curse you, GridBag

        content.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));

        // Hook up the escape key:
        content.registerKeyboardAction(
            disposeAction,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        // Set up the dialog:
        setContentPane(content);
        getRootPane().setDefaultButton(start);
        Platform.getPlatform().makeModal(this);
        setTitle(LOCALE.get("SendDialogTitle"));
        pack();
        setResizable(false);
    }

    public static BatchConfig showDialog(Frame owner, String from, int count) {
        SendDialog dialog = new SendDialog(owner, from, count);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        if (dialog.started) {
            // Remember choices for next time:
            dialog.conf.saveToPrefs(PrefsKey);
            return dialog.conf;
        }
        else {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
        BatchConfig conf = showDialog(null, "Test Folder", 256);
        if (conf != null) {
            conf.writeDebug(System.out);
        }
        else {
            System.out.println("cancelled");
        }
        System.exit(0);
    }
}
