/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.generic;

import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.ParseException;

/** This is a JTextField that listens on a ConfiguredBoundedRangeModel,
  * updating its text when the model changes and pushing validated numeric
  * text back into the model.
  */

class ConfiguredTextField
    extends JTextField
    implements MouseWheelListener, ChangeListener, DocumentListener
{
    // Select-all and handle mouse wheel events when text fields gain focus:
    private static final FocusListener FocusSelector = new FocusAdapter() {
        public void focusGained(FocusEvent event) {
            ConfiguredTextField text = (ConfiguredTextField) event.getSource();
            text.selectAll();
            text.addMouseWheelListener(text);
        }
        public void focusLost(FocusEvent event) {
            ConfiguredTextField text = (ConfiguredTextField) event.getSource();
            text.select(0, 0);
            text.removeMouseWheelListener(text);
        }
    };

    private ConfiguredBoundedRangeModel model;
    private NumberFormat format;

    private double min;     // The minimum numeric value for the text
    private double max;     // The maximum numeric value for the text

    private double inc;     // An increment/decrement amount, for keystrokes

    private boolean isUpdating; // A flag to detect our own model changes

    ConfiguredTextField(
        ConfiguredBoundedRangeModel model, DecimalFormat format
    ) {
        this.model = model;
        this.format = format;
        min = model.getConfiguredMinimum();
        max = model.getConfiguredMaximum();
        inc = model.getConfiguredIncrement();
        setInputVerifier(new IntervalVerifier(min, max));
        setHorizontalAlignment(RIGHT);

        setFont(getFont()); // Figure out the maxiumum text width

        addListeners();

        updateFromModel();
    }

    public void setFont(Font font) {
        super.setFont(font);
        if (model == null) {
            // called from base class constructor
            return;
        }
        // Adjust our size to allow for the maximum value:

        double max = model.getConfiguredMaximum();
        int widest = getWidestNumber(max);

        String tempText = Integer.toString(widest);
        int places = format.getMaximumFractionDigits();
        if (places > 0) {
            tempText += ".";
            for (int n=0; n<places; n++) {
                tempText += "0";
            }
        }
        String text = getText();
        setText(tempText);
        setPreferredSize(null); // wipe previous settings
        Dimension size = getPreferredSize();
        setText(text);

        setMinimumSize(size);
        setPreferredSize(size);
    }

    // When the model changes, update our text:
    public void stateChanged(ChangeEvent event) {
        if (isUpdating) {
            return;
        }
        Object source = event.getSource();
        if (! source.equals(model)) {
            return;
        }
        updateFromModel();
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

    private void updateFromModel() {
        double value = model.getConfiguredValue();
        String text = format.format(value);
        if (! text.equals(getText())) {
            setText(text);
            selectAll();
        }
    }

    // When the document changes, give verification feedback and maybe update
    // the model:
    private void handleDocumentChange() {
        InputVerifier verifier = getInputVerifier();
        String text = getText();
        boolean verified = verifier.verify(this);
        if (! verified) {
            setForeground(Color.red);
        }
        else {
            try {
                double value = format.parse(text).doubleValue();
                setForeground(LightZoneSkin.Colors.ToolPanesForeground);
                isUpdating = true;
                model.setConfiguredValue(value);
                isUpdating = false;
            }
            catch (ParseException e) {
                // Should never happen, because of the verifier.
                System.err.println("Unparsable verified text: " + text);
                setForeground(Color.red);
            }
        }
    }

    private void addListeners() {

        // Respond to document changes with verification and model changes:
        Document doc = getDocument();
        doc.addDocumentListener(this);

        // Update text when the model changes:
        model.addChangeListener(this);

        // Select-all when we gain focus, select-none when we lose:
        addFocusListener(FocusSelector);

        // Override the default input map for spacebar, because that key stroke
        // is used globally to access the editor's pan mode.
        InputMap input = getInputMap(WHEN_FOCUSED);
        KeyStroke space = KeyStroke.getKeyStroke(new Character(' '), 0);
        input.put(space, "none");

        // Increment by largeInc on up arrow events:
        registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    double value = model.getConfiguredValue();
                    value = getNextRoundValueUp(value);
                    if ((value >= min) && (value <= max)) {
                        model.setConfiguredValue(value);
                    }
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
            WHEN_FOCUSED
        );

        // Decrement by largeInc on down arrow events:
        registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    double value = model.getConfiguredValue();
                    value = getNextRoundValueDown(value);
                    if ((value >= min) && (value <= max)) {
                        model.setConfiguredValue(value);
                    }
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
            WHEN_FOCUSED
        );
    }

    // Increment/decrement by smallInc on mouse wheel events, if we're focused:
    public void mouseWheelMoved(MouseWheelEvent event) {
        double value = model.getConfiguredValue();
        int count = event.getWheelRotation();
        int sign = (count > 0) ? 1 : -1;
        for (int n=0; n<sign*count; n++) {
            if (sign > 0) {
                value = getNextRoundValueDown(value);
            }
            else {
                value = getNextRoundValueUp(value);
            }
            if ((value >= min) && (value <= max)) {
                model.setConfiguredValue(value);
            }
            else {
                break;
            }
        }
    }

    private double getNextRoundValueDown(double value) {
        return getRoundValue(value - inc);
    }

    private double getNextRoundValueUp(double value) {
        return getRoundValue(value + inc);
    }

    private double getRoundValue(double value) {
        return inc * Math.round(value / inc);
    }

    private static int getWidestNumber(double max) {
        double powTen = Math.pow(10, Math.ceil(Math.log(max) / Math.log(10)));
        int widest = (int) Math.round(powTen);
        if (widest == 1) {
            // the one case where a power of ten is narrower than a
            // smaller nonnegative integer
            widest = 100;
        }
        return widest;
    }

    // An InputVerifier that checks our text is compatible with a number range:

    private class IntervalVerifier extends InputVerifier {

        private double min;
        private double max;

        IntervalVerifier(double min, double max) {
            this.min = min;
            this.max = max;
        }

        public boolean verify(JComponent input) {
            JTextField textField = (JTextField) input;
            String text = textField.getText();
            double x;
            try {
                x = format.parse(text).doubleValue();
            }
            catch (ParseException e) {
                return false;
            }
            return ((x >= min) && (x <= max));
        }
    }
}
