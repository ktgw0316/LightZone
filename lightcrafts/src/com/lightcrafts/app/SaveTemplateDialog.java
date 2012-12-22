/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.RawImageType;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.templates.TemplateKey;
import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The save-template dialog is a lot like a JOptionPane with a combo box,
 * except it uses its own dialog instead of the JOptionPane showXxxDialog()
 * methods so it can set the initial focus on the combo box instead of a
 * button.
 */
class SaveTemplateDialog extends JDialog {

    private XmlDocument originalTemplate;
    private JComboBox namespaceField;
    private JComboBox nameField;
    private TemplateToolSelector selector;
    private boolean isOkSelected;
    private boolean isDefaultSelected;

    TemplateKey showDialog(
        ImageMetadata meta,     // offer the always-apply option for RAW images
        XmlDocument template,   // the template LZN to save
        String nsDefault,       // the default template namespace
        JFrame parent           // parent for this dialog
    ) {
        originalTemplate = template;
        nameField = new JComboBox();
        namespaceField = new JComboBox();
        try {
            // Find all distinct namespaces from the TemplateDocuments.
            List<TemplateKey> keys = TemplateDatabase.getTemplateKeys();
            Set<String> namespaces = new HashSet<String>();
            for (TemplateKey key : keys) {
                namespaces.add(key.getNamespace());
            }
            for (String namespace : namespaces) {
                namespaceField.addItem(namespace);
            }
            // Update the template names when the namespace changes.
            namespaceField.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Object name = nameField.getSelectedItem();
                        updateNameField();
                        nameField.setSelectedItem(name);
                    }
                }
            );
        }
        catch (TemplateDatabase.TemplateException e) {
            // Just don't show existing template names.
        }
        try {
            selector = new TemplateToolSelector(template);
        }
        catch (XMLException e) {
            // Don't include the selector to the layout below.
            System.err.println("Couldn't initialize TemplateToolSelector");
            e.printStackTrace();
        }
        // Please excuse the GridBag.  I'm just lining up text fields and
        // labels into columns.
        JPanel textGrid = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        textGrid.add(new JLabel(LOCALE.get("TemplateNameLabel") + ':'), c);
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        textGrid.add(nameField, c);
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        textGrid.add(new JLabel(LOCALE.get("TemplateNamespaceLabel") + ':'), c);
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        textGrid.add(namespaceField, c);

        // If a camera model is known, then offer the option to associate
        // all images from this camera with the current template:
        String camera = meta.getCameraMake(true);
        boolean isRaw = (meta.getImageType() instanceof RawImageType);
        JCheckBox defaultCheck;
        if (isRaw && (camera != null)) {
            defaultCheck = new JCheckBox(
                LOCALE.get("TemplateAlwaysLabel", camera)
            );
        }
        else {
            defaultCheck = new JCheckBox(
                LOCALE.get("TemplateAlwaysDisabledLabel")
            );
            defaultCheck.setEnabled(false);
        }
        defaultCheck.setAlignmentX(1f);

        Box defaultBox = Box.createHorizontalBox();
        defaultBox.add(defaultCheck);
        defaultBox.add(Box.createHorizontalGlue());

        // Initialize the text fields with a default template name:
        if (nsDefault == null) {
            nsDefault = LOCALE.get("TemplateDefaultNamespace");
        }
        int n = 1;
        String defaultName = null;
        do {
            String name = LOCALE.get(
                "TemplateDefaultNamePattern", Integer.toString(n++)
            );
            TemplateKey key = new TemplateKey(nsDefault, name);
            try {
                TemplateDatabase.getTemplateDocument(key);
            }
            catch (TemplateDatabase.TemplateException e) {
                // No Template with this name has been defined.
                defaultName = name;
            }
        } while (defaultName == null);

        namespaceField.addItem(nsDefault);
        namespaceField.setSelectedItem(nsDefault);
        namespaceField.setEditable(true);
        nameField.setPreferredSize(
            new Dimension(160, namespaceField.getPreferredSize().height)
        );
        nameField.getEditor().selectAll();
        
        nameField.addItem(defaultName);
        nameField.setSelectedItem(defaultName);
        nameField.setEditable(true);
        nameField.setPreferredSize(
            new Dimension(160, nameField.getPreferredSize().height)
        );
        nameField.getEditor().selectAll();

        Box messageBox = Box.createVerticalBox();
        messageBox.add(textGrid);
        messageBox.add(defaultBox);
        messageBox.add(Box.createVerticalStrut(8));
        if (selector != null) {
            messageBox.add(selector);
        }
        // We use our own OK button, instead of the one generated by
        // JOptionPane, so we can set it to be the default button for the
        // dialog.
        JButton okButton = new JButton(LOCALE.get("TemplateSaveOption"));
        JButton cancelButton = new JButton(LOCALE.get("TemplateCancelOption"));
        JOptionPane pane = new JOptionPane(
            messageBox,
            JOptionPane.QUESTION_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION,
            null,
            new Object[] { okButton, cancelButton },
            null
        );
        // Set a minimum width for the dialog.
        Dimension size = pane.getPreferredSize();
        size = new Dimension(Math.max(size.width, 400), size.height);
        pane.setPreferredSize(size);

        // Use a custom dialog instead of the JOptionPane showXxxDialog()
        // methods so that we can set the initial focus on the combo box
        // instead of the OK button.
        final JDialog dialog = new JDialog(parent);
        dialog.setTitle(LOCALE.get("TemplateSaveDialogTitle"));
        dialog.setContentPane(pane);
        dialog.getRootPane().setDefaultButton(okButton);

        // This causes the dialog to go away when the user presses "ESCAPE".
        pane.registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        // This causes the dialog to go away when the user clicks "OK".
        okButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                    // In case this happens while the combo box editor is
                    // active, commit the current text to the combo box model:
                    ComboBoxEditor editor = nameField.getEditor();
                    JTextField text = (JTextField) editor.getEditorComponent();
                    String name = text.getText();
                    nameField.setSelectedItem(name);

                    isOkSelected = true;
                    dialog.setVisible(false);
                }
            }
        );
        // This causes the dialog to go away when the user clicks "Cancel".
        cancelButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    dialog.setVisible(false);
                }
            }
        );
        dialog.setModal(true);
        dialog.setResizable(true);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        dialog.setVisible(true);

        isDefaultSelected = defaultCheck.isSelected();

        if (! isOkSelected) {
            // The dialog was disposed, or the user clicked Cancel.
            return null;
        }
        if (nameField.getEditor().getEditorComponent().isFocusOwner()) {
            KeyboardFocusManager focus =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
            focus.upFocusCycle();
        }
        return new TemplateKey(getNamespaceText(), getNameText());
    }

    String getNameText() {
        Object selected = nameField.getSelectedItem();
        return (selected != null) ? selected.toString() : "";
    }

    String getNamespaceText() {
        Object selected = namespaceField.getSelectedItem();
        return (selected != null) ? selected.toString() : "";
    }

    boolean isDefaultSelected() {
        return isDefaultSelected;
    }

    XmlDocument getModifiedTemplate() {
        if (selector != null) {
            return selector.getModifiedTemplate();
        }
        else {
            return new XmlDocument(originalTemplate);
        }
    }

    // When the namespace changes, update the combo box of Template names.
    private void updateNameField() {
        String namespace = getNamespaceText();
        nameField.removeAllItems();
        try {
            List<TemplateKey> descs = TemplateDatabase.getTemplateKeys();
            for (TemplateKey desc : descs) {
                if (namespace.equals(desc.getNamespace())) {
                    nameField.addItem(desc.getName());
                }
            }
        }
        catch (TemplateDatabase.TemplateException e) {
            // Just leave the name field blank.
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());

        XmlDocument doc = new XmlDocument(
            new FileInputStream(
                "/Users/anton/test/1/test.lzn"
            )
        );
        ImageInfo info = ImageInfo.getInstanceFor(
            new File("/Users/anton/test/1/test.crw")
        );
        ImageMetadata meta = info.getMetadata();

        SaveTemplateDialog dialog = new SaveTemplateDialog();
        dialog.showDialog(meta, doc, null, null);

        System.out.println("name=" + dialog.getNameText());
        System.out.println("namespace=" + dialog.getNamespaceText());

        System.exit(0);
    }
}
