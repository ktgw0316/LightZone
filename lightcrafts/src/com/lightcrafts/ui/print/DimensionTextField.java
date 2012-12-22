/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.ParseException;

import com.lightcrafts.ui.print.PrintLayoutModel.LengthUnit;
import com.lightcrafts.ui.LightZoneSkin;

// A validating text field, to ensure that margin and size numbers entered
// into the print layout dialog are kosher.

class DimensionTextField extends JTextField implements DocumentListener {

    interface Listener {
        void dimensionChanged(double dimension);
    }

    // Select-all when the text field gain focus:
    private static final FocusListener FocusSelector = new FocusAdapter() {
        public void focusGained(FocusEvent event) {
            DimensionTextField text = (DimensionTextField) event.getSource();
            text.selectAll();
        }
    };

    private final static NumberFormat InchFormat = new DecimalFormat("0.00");
    private final static NumberFormat CmFormat = new DecimalFormat("0.0");
    private final static NumberFormat PointFormat = new DecimalFormat("0");

    private Listener listener;
    private NumberFormat format;

    DimensionTextField() {
        setInputVerifier(new NumberVerifier());
        Document doc = getDocument();
        doc.addDocumentListener(this);
        format = InchFormat;
        setFixedSize();
        setHorizontalAlignment(RIGHT);
        addFocusListener(FocusSelector);
        registerKeyboardActions();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setUnit(LengthUnit unit) {
        if (unit == LengthUnit.CM) {
            format = CmFormat;
        }
        else if (unit == LengthUnit.INCH) {
            format = InchFormat;
        }
        else if (unit == LengthUnit.POINT) {
            format = PointFormat;
        }
        else {
            throw new IllegalArgumentException("Unrecognized LengthUnit");
        }
    }

    LengthUnit getUnit() {
        if (format == CmFormat) {
            return LengthUnit.CM;
        }
        else if (format == InchFormat) {
            return LengthUnit.INCH;
        }
        else if (format == PointFormat) {
            return LengthUnit.POINT;
        }
        else {
            throw new IllegalStateException("Unknown LengthUnit");
        }
    }

    void setDimension(double dimension) {
        String text = format.format(dimension);
        setText(text);
    }

    double getDimension() {
        InputVerifier verifier = getInputVerifier();
        boolean verified = verifier.verify(this);
        if (! verified) {
            throw new IllegalStateException("Unverified dimension text");
        }
        String text = getText();
        try {
            return format.parse(text).doubleValue();
        }
        catch (ParseException e) {
            throw new IllegalStateException("Unparsable dimension text");
        }
    }

    private void setFixedSize() {
        setText("0.000");
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
                if (! verifier.verify(DimensionTextField.this)) {
                    return;
                }
                LengthUnit unit = getUnit();
                final double delta = (unit == LengthUnit.POINT) ? 1 : .1;
                double dim = getDimension();
                dim += delta;
                if (dim >= 0) {
                    setDimension(dim);
                }
            }
        };
        ActionListener downAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                InputVerifier verifier = getInputVerifier();
                if (! verifier.verify(DimensionTextField.this)) {
                    return;
                }
                LengthUnit unit = getUnit();
                final double delta = (unit == LengthUnit.POINT) ? 1 : .1;
                double dim = getDimension();
                dim -= delta;
                if (dim >= 0) {
                    setDimension(dim);
                }
                else {
                    setDimension(0);
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
                double x = getDimension();
                listener.dimensionChanged(x);
            }
        }
    }

    private class NumberVerifier extends InputVerifier {

        public boolean verify(JComponent input) {
            String text = getText();
            double x;
            try {
                x = format.parse(text).doubleValue();
            }
            catch (ParseException e) {
                return false;
            }
            return x >= 0d;
        }
    }
}
