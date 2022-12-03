/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import java.util.Arrays;

/**
 * This is an editable combo box that remembers its most values, verifies its
 * text, and colors its input red and black.
 */
class PpiComboBox
    extends JComboBox implements ActionListener, DocumentListener
{
    // Keep track of recently entered PPI numbers
    private final static Preferences Prefs =
        Preferences.userNodeForPackage(PpiComboBox.class);

    private final static String RecentPpiTag = "RecentPpi";
    private final static String CurrentPpiTag = "CurrentPpi";

    private final static int MaxItemCount = 5;

    PpiComboBox() {
        setEditable(true);
        addActionListener(this);

        JTextComponent text = getTextComponent();
        Document doc = text.getDocument();
        doc.addDocumentListener(this);

        Font font = (new JTextField()).getFont();
        setFont(font);
        text.setFont(font);
        
        text.addFocusListener(
            new FocusListener() {
                public void focusGained(FocusEvent e) {
                    JTextComponent text = (JTextComponent) e.getSource();
                    text.selectAll();
                }
                public void focusLost(FocusEvent e) {
                    JTextComponent text = (JTextComponent) e.getSource();
                    text.select(0, 0);
                }
            }
        );
        text.setInputVerifier(
            new InputVerifier() {
                public boolean verify(JComponent comp) {
                    return PpiComboBox.this.verify();
                }
            }
        );
        addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        savePrefs();
                    }
                }
            }
        );
        Dimension size = getPreferredSize();
        setPreferredSize(new Dimension(30, size.height));

        restorePrefs();
    }

    public void actionPerformed(ActionEvent e) {
        boolean verified = verify();
        if (! verified) {
            return;
        }
        String text = (String) editor.getItem();
        setItem(text);
    }

    void setPpi(int ppi) {
        String text = Integer.toString(ppi);
        setItem(text);
    }

    int getPpi() {
        String text = (String) getSelectedItem();
        if (text != null) {
            return Integer.parseInt(text);
        }
        return 0;
    }

    private void setItem(String text) {
        for (int n=0; n<getItemCount(); n++) {
            String item = (String) getItemAt(n);
            if (item.equals(text)) {
                setSelectedItem(item);
                savePrefs();
                return;
            }
        }
        addItem(text);
        while (getItemCount() > MaxItemCount) {
            removeItemAt(0);
        }
        setSelectedItem(text);
        savePrefs();
    }

    public void insertUpdate(DocumentEvent e) {
        handleDocumentChange();
    }

    public void removeUpdate(DocumentEvent e) {
        handleDocumentChange();
    }

    public void changedUpdate(DocumentEvent e) {
        handleDocumentChange();
    }

    private void handleDocumentChange() {
        boolean verified = verify();
        JTextComponent text = getTextComponent();
        if (! verified) {
            text.setForeground(Color.red);
        }
        else {
            text.setForeground(LightZoneSkin.Colors.ToolPanesForeground);
        }
    }

    private JTextComponent getTextComponent() {
        return (JTextComponent) editor.getEditorComponent();
    }

    private boolean verify() {
        ComboBoxEditor editor = getEditor();
        String text = (String) editor.getItem();
        try {
            int ppi = Integer.parseInt(text);
            return (ppi > 0);
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isRestoring;

    private void savePrefs() {
        if (! isRestoring) {
            for (int n=0; n<getItemCount(); n++) {
                String item = (String) getItemAt(n);
                Prefs.put(RecentPpiTag + n, item);
            }
            String item = (String) getSelectedItem();
            if (item != null) {
                Prefs.put(CurrentPpiTag, item);
            }
            else {
                Prefs.remove(CurrentPpiTag);
            }
        }
    }

    private void restorePrefs() {
        isRestoring = true;
        try {
            String[] keys = Prefs.keys();
            Arrays.sort(keys);
            for (String key : keys) {
                if (key.startsWith(RecentPpiTag)) {
                    String item = Prefs.get(key, null);
                    if (item != null) {
                        addItem(item);
                    }
                }
            }
            String item = Prefs.get(CurrentPpiTag, null);
            if (item != null) {
                setSelectedItem(item);
            }
        }
        catch (BackingStoreException e) {
            System.err.println(
                "Error restoring PpiComboBox values: " + e.getMessage()
            );
            // Just leave things uninitialized
        }
        isRestoring = false;
    }

    public static void main(String[] args) {
        JPanel panel = new JPanel();
        panel.add(new PpiComboBox());
        panel.add(new JButton("x"));
        JFrame frame = new JFrame();
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
