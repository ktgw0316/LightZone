/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.text.ParseException;

// A validating text field for setting the "scale" number in a
// PrintLayoutModel.

class ScaleTextField extends JTextField implements DocumentListener {

    interface Listener {
        void scaleChanged(double scale);
    }

    // Select-all when the text field gain focus:
    private static final FocusListener FocusSelector = new FocusAdapter() {
        public void focusGained(FocusEvent event) {
            ScaleTextField text = (ScaleTextField) event.getSource();
            text.selectAll();
        }
    };

    private final static NumberFormat PercentFormat =
        NumberFormat.getPercentInstance();

    private final static NumberFormat IntegerFormat =
        NumberFormat.getIntegerInstance();

    private Listener listener;

    ScaleTextField() {
        setInputVerifier(new NumberVerifier());
        Document doc = getDocument();
        doc.addDocumentListener(this);
        setFixedSize();
        setHorizontalAlignment(RIGHT);
        addFocusListener(FocusSelector);
        registerKeyboardActions();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setScale(double scale) {
        String text = PercentFormat.format(scale);
        setText(text);
    }

    double getScale() {
        InputVerifier verifier = getInputVerifier();
        boolean verified = verifier.verify(this);
        if (! verified) {
            throw new IllegalStateException("Invalid scale text");
        }
        String text = getText();
        double scale = 1;
        try {
            Number number = PercentFormat.parse(text);
            scale = number.doubleValue();
        }
        catch (ParseException e) {
            try {
                Number number = IntegerFormat.parse(text);
                scale = number.doubleValue() / 100d;
            }
            catch (ParseException f) {
                // Can't happen (we just ran the verifier).
            }
        }
        return scale;
    }

    private void setFixedSize() {
        setText("1000%");
        Dimension size = getPreferredSize();
        setMinimumSize(size);
        setPreferredSize(size);
        setMaximumSize(size);
    }

    private void registerKeyboardActions() {

        // Increment and decrement by an amount dependent on the current
        // unit whenever up or down arrow is pressed.

        ActionListener upAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                InputVerifier verifier = getInputVerifier();
                if (! verifier.verify(ScaleTextField.this)) {
                    return;
                }
                double scale = getScale();
                scale += .01;
                if (verify(scale)) {
                    setScale(scale);
                }
            }
        };
        ActionListener downAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                InputVerifier verifier = getInputVerifier();
                if (! verifier.verify(ScaleTextField.this)) {
                    return;
                }
                double scale = getScale();
                scale -= .01;
                if (verify(scale)) {
                    setScale(scale);
                }
                else if (scale < 0) {
                    setScale(0);
                }
            }
        };
        registerKeyboardAction(
            upAction,
            "Increment",
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
            WHEN_FOCUSED
        );
        registerKeyboardAction(
            downAction,
            "Decrement",
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
            WHEN_FOCUSED
        );
    }

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
        boolean verified = verifier.verify(this);
        if (! verified) {
            setForeground(Color.red);
        }
        else {
            setForeground(LightZoneSkin.Colors.ToolPanesForeground);
            if (listener != null) {
                double x = getScale();
                listener.scaleChanged(x);
            }
        }
    }

    private class NumberVerifier extends InputVerifier {

        public boolean verify(JComponent input) {
            String text = getText();
            double x;
            try {
                Number number = PercentFormat.parse(text);
                x = number.doubleValue();
            }
            catch (ParseException e) {
                try {
                    // Maybe some just omitted the "%" character:
                    Number number = IntegerFormat.parse(text);
                    x = number.doubleValue() / 100d;
                }
                catch (ParseException f) {
                    return false;
                }
            }
            return ScaleTextField.verify(x);
        }
    }

    private static boolean verify(double x) {
        return x >= 0d;
    }
}
