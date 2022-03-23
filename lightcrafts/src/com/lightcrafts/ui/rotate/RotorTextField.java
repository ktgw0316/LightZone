/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.rotate;

import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import static com.lightcrafts.ui.rotate.Locale.LOCALE;

// A validating text field that synchronizes with the RotorControl sliders.

class RotorTextField
    extends JTextField implements DocumentListener, MouseWheelListener
 {
    private final static NumberFormat Format = new DecimalFormat("+0.00;-0.00");
    private final static String TooltipText = LOCALE.get("RotorTextToolTip");

    private RotorControl control;

    private boolean isUpdating;     // Suppress the DocumentListener

    RotorTextField(RotorControl control) {
        this.control = control;
        setHorizontalAlignment(RIGHT);
        setToolTipText(TooltipText);

        setText("+100.00");
        Dimension size = getPreferredSize();
        setMinimumSize(size);
        setPreferredSize(size);
        setMaximumSize(size);

        update();

        setInputVerifier(new AngleVerifier());
        Document doc = getDocument();
        doc.addDocumentListener(this);

        addMouseWheelListener(this);

        addFocusListener(
            new FocusAdapter() {
                public void focusGained(FocusEvent event) {
                    selectAll();
                }
            }
        );
    }

    void update() {
        double angle = control.getDegrees();
        String text = Format.format(angle);
        isUpdating = true;
        setText(text);
        isUpdating = false;
    }

    public void changedUpdate(DocumentEvent event) {
        handleDocumentChange();
    }

    public void insertUpdate(DocumentEvent event) {
        handleDocumentChange();
    }

    public void removeUpdate(DocumentEvent event) {
        handleDocumentChange();
    }

    // When the text changes, give verification feedback and maybe update
    // the control:
    private void handleDocumentChange() {
        if (isUpdating) {
            return;
        }
        InputVerifier verifier = getInputVerifier();
        boolean verified = verifier.verify(this);
        if (! verified) {
            setForeground(Color.red);
        }
        else {
            String text = getText();
            try {
                setForeground(LightZoneSkin.Colors.ToolPanesForeground);
                double value = Format.parse(text).doubleValue();
                control.setDegrees(value);
            }
            catch (ParseException e) {
                System.err.println("Unparsable verified text: " + text);
                setForeground(Color.red);
            }
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        InputVerifier verifier = getInputVerifier();
        boolean verified = verifier.verify(this);
        if (verified) {
            final int count = e.getWheelRotation();
            if (count == 0) return;
            String text = getText();
            try {
                double value = Format.parse(text).doubleValue();
                value -= 0.1 * count;
                value = Math.round(10 * value) / 10d;
                control.setDegrees(value);
                update();
                selectAll();
            }
            catch (ParseException e1) {
                System.err.println("Unparsable verified text: " + text);
                // do nothing, let the wheel have no effect
            }
        }
    }

    // An InputVerifier that checks the text is a number:

    private class AngleVerifier extends InputVerifier {

        public boolean verify(JComponent input) {
            String text = getText();
            try {
                Format.parse(text);
                return true;
            }
            catch (ParseException e) {
                return false;
            }
        }
    }
}
