/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.export;

import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.image.export.ResolutionOption;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;

/**
 * An editable text field with key event bindings for increment and decrement,
 * a select-all behavior on focus gained, and color-coded text input validation.
 */
class PpiField extends JTextField {

    ResolutionOption resolution;

    PpiField(ImageFileExportOptions options) {
        super(4);        
        resolution = options.resolution;

        int ppi = resolution.getValue();
        setText(Integer.toString(ppi));

        setInputVerifier(Verifier);
        setHorizontalAlignment(RIGHT);
        addFocusListener(FocusSelector);

        registerKeyboardAction(
            UpAction,
            "Increment",
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
            WHEN_FOCUSED
        );

        registerKeyboardAction(
            DownAction,
            "Decrement",
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
            WHEN_FOCUSED
        );
        Document doc = getDocument();
        doc.addDocumentListener(TextUpdater);

        Dimension size = getPreferredSize();
        setMinimumSize(size);
        setPreferredSize(size);
        setMaximumSize(size);
    }

    // Select-all when one of the text fields gains focus:

    private final static FocusListener FocusSelector = new FocusAdapter() {
        public void focusGained(FocusEvent event) {
            JTextField text = (JTextField) event.getSource();
            text.selectAll();
        }
    };

    // Increment whenever the up arrow is pressed:

    private final static ActionListener UpAction = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            final JTextField source = (JTextField) event.getSource();
            InputVerifier verifier = source.getInputVerifier();
            if (! verifier.verify(source)) {
                return;
            }
            final int number = Integer.parseInt(source.getText());
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        source.setText(Integer.toString(number + 1));
                    }
                }
            );
        }
    };

    // Decrement whenever the down arrow is pressed:

    private final static ActionListener DownAction = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            final JTextField source = (JTextField) event.getSource();
            InputVerifier verifier = source.getInputVerifier();
            if (! verifier.verify(source)) {
                return;
            }
            final int number = Integer.parseInt(source.getText());
            if (number > 1) {
                EventQueue.invokeLater(
                    new Runnable() {
                        public void run() {
                            source.setText(Integer.toString(number - 1));
                        }
                    }
                );
            }
        }
    };

    private final static InputVerifier Verifier = new InputVerifier() {
        public boolean verify(JComponent input) {
            String text = ((JTextField) input).getText();
            double x;
            try {
                x = Integer.parseInt(text);
            }
            catch (NumberFormatException e) {
                return false;
            }
            return x > 0;
        }
    };

    private final DocumentListener TextUpdater = new DocumentListener() {

        public void changedUpdate(DocumentEvent e) {
            handleDocumentChange();
        }
        public void insertUpdate(DocumentEvent e) {
            handleDocumentChange();
        }
        public void removeUpdate(DocumentEvent e) {
            handleDocumentChange();
        }
        private void handleDocumentChange() {
            InputVerifier verifier = getInputVerifier();
            boolean verified = verifier.verify(PpiField.this);
            if (! verified) {
                setForeground(Color.red);
            }
            else {
                setForeground(LightZoneSkin.Colors.ToolPanesForeground);
                int value = Integer.parseInt(getText());
                resolution.setValue(value);
            }
        }
    };
}
