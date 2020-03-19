/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;

/**
 * A validating text field for entering positive integers and handling dynamic
 * updates.  It gives validation feedback by setting the text color to black or
 * red, handles arrow keyboard actions by incrementing and decrementing, and
 * respects the locale.
 */

public class NumberTextField extends JTextField implements DocumentListener {

    /**
      * A single listener can be notified when the validated number changes.
     */
    public static interface Listener {
        void numberChanged(int number);
    }

    /**
     * This focus listener handles select-all when the text field gains focus.
     */
    private final static FocusListener FocusSelector = new FocusAdapter() {
        public void focusGained(FocusEvent event) {
            NumberTextField text = (NumberTextField) event.getSource();
            text.selectAll();
        }
    };

    /**
     * We use a NumberFormat to validate entered text, and also to format
     * the text when the number is set programmatically.
     */
    private final static NumberFormat IntegerFormat =
        NumberFormat.getIntegerInstance();
    static {
        // This determines how liberal the input validation will be.
        IntegerFormat.setParseIntegerOnly(false);
    }

    private Listener listener;

    public NumberTextField(Listener listener) {
        fixSize();
        setNumber(0);
        this.listener = listener;
        setHorizontalAlignment(RIGHT);
        setInputVerifier(new PositiveNumberVerifier());
        Document doc = getDocument();
        doc.addDocumentListener(this);
        addFocusListener(FocusSelector);
        registerKeyboardActions();
    }

    public void setNumber(int n) {
        String text = IntegerFormat.format(n);
        setText(text);
    }

    /**
     * Implementing DocumentListener
     */
    public void changedUpdate(DocumentEvent e) {
        handleDocumentChange();
    }

    /**
     * Implementing DocumentListener
     */
    public void insertUpdate(DocumentEvent e) {
        handleDocumentChange();
    }

    /**
     * Implementing DocumentListener
     */
    public void removeUpdate(DocumentEvent e) {
        handleDocumentChange();
    }

    /**
     * Get the number value fo the current text, or throw IllegalStateException
     * if the current text is not a verifiable number.
     */
    public int getNumber() {
        InputVerifier verifier = getInputVerifier();
        boolean verified = verifier.verify(this);
        if (! verified) {
            throw new IllegalStateException("Invalid number text");
        }
        String text = getText();
        int n = 0;
        try {
            Number number = IntegerFormat.parse(text);
            n = number.intValue();
        }
        catch (ParseException e) {
            // Can't happen (we just ran the verifier).
            System.err.println(e.getMessage());
        }
        return n;
    }

    /**
     * Increment and decrement the current number in response to arrow keys.
     */
    private void registerKeyboardActions() {
        ActionListener upAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                InputVerifier verifier = getInputVerifier();
                if (! verifier.verify(NumberTextField.this)) {
                    return;
                }
                int n = getNumber();
                setNumber(++n);
            }
        };
        ActionListener downAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                InputVerifier verifier = getInputVerifier();
                if (! verifier.verify(NumberTextField.this)) {
                    return;
                }
                int n = getNumber();
                if (n > 1) {
                    setNumber(--n);
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

    /**
     * On every document change, check if the current text is a valid number
     * and update the text color accordingly.  Then, if the number is valid,
     * notify the listener.
     */
    private void handleDocumentChange() {
        InputVerifier verifier = getInputVerifier();
        boolean verified = verifier.verify(this);
        if (! verified) {
            setForeground(Color.red);
        }
        else {
            setForeground(LightZoneSkin.Colors.ToolPanesForeground);
            if (listener != null) {
                int n = getNumber();
                listener.numberChanged(n);
            }
        }
    }

    /**
     * To initialize the size of NumberTextFields, put in a big number and
     * note the preferred size.
     */
    private void fixSize() {
        setText("100");
        Dimension size = getPreferredSize();
        setMinimumSize(size);
        setPreferredSize(size);
        setMaximumSize(size);
    }

    /**
     * An InputVerifier that checks whether this NumberTextField's
     * text can be parsed as an integer and that the integer is positive.
     */
    private class PositiveNumberVerifier extends InputVerifier {
        public boolean verify(JComponent input) {
            String text = getText();
            ParsePosition pos = new ParsePosition(0);
            IntegerFormat.parse(text, pos);
            int index = pos.getIndex();
            if ((index <= 0) || (index != text.length())) {
                return false;
            }
            try {
                Number number = IntegerFormat.parse(text);
                int n = number.intValue();
                return (n > 0);
            }
            catch (ParseException e) {
                // Can't happen (we just ran the verifier).
                return false;
            }
        }
    }

    public static void main(String[] args) {
        NumberTextField text = new NumberTextField(
            new Listener() {
                public void numberChanged(int number) {
                    System.out.println(number);
                }
            }
        );
        JFrame frame = new JFrame("NumberTextField Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(text);
        frame.pack();
        frame.setLocation(100, 100);
        frame.setVisible(true);
    }
}
