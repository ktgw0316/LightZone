/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.export;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.export.IntegerExportOption;
import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.event.*;
import java.awt.*;

/**
 * A pair of editable text fields that validate input as positive integers
 * and update each other to preserve their ratio.
 */

class ExportSizeFields {

    private JTextField xText;
    private JTextField yText;

    private double ratio;

    // Prevent update loops:
    private boolean isXUpdating;
    private boolean isYUpdating;

    ExportSizeFields(ImageExportOptions options) {
        final ImageFileExportOptions fileOptions =
            (ImageFileExportOptions)options;

        IntegerExportOption widthOption = fileOptions.resizeWidth;
        IntegerExportOption heightOption = fileOptions.resizeHeight;

        int x = widthOption.getValue();
        int y = heightOption.getValue();

        xText = createTextField(x);
        yText = createTextField(y);

	int width = fileOptions.originalWidth.getValue();
	int height = fileOptions.originalHeight.getValue();

        ratio = width / (double) height;

        // Push valid text updates into the export options:
        new ExportUpdater(widthOption, xText);
        new ExportUpdater(heightOption, yText);
    }

    JTextField getXText() {
        return xText;
    }

    JTextField getYText() {
        return yText;
    }

    private static JTextField createTextField(int value) {
        JTextField text = new JTextField(4);
        text.setText(Integer.toString(value));
        text.setInputVerifier(Verifier);
        text.addFocusListener(FocusSelector);
        text.setHorizontalAlignment(SwingConstants.RIGHT);
        text.registerKeyboardAction(
            UpAction,
            "Increment",
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
            JComponent.WHEN_FOCUSED
        );
        text.registerKeyboardAction(
            DownAction,
            "Decrement",
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
            JComponent.WHEN_FOCUSED
        );
        return text;
    }

    private void xUpdated() {
        if (! isYUpdating) {
            isXUpdating = true;
            int x = Integer.parseInt(xText.getText());
            int y = (int) Math.round(x / ratio);
            yText.setText(Integer.toString(y));
            isXUpdating = false;
        }
    }

    private void yUpdated() {
        if (! isXUpdating) {
            isYUpdating = true;
            int y = Integer.parseInt(yText.getText());
            int x = (int) Math.round(y * ratio);
            xText.setText(Integer.toString(x));
            isYUpdating = false;
        }
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

    // Verify positive integer text:

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

    // Repsond to valid text changes with color changes and updates to the
    // ImageExportOptions object:

    private final class ExportUpdater implements DocumentListener {

        private IntegerExportOption option;
        private JTextField text;

        ExportUpdater(IntegerExportOption option, JTextField text) {
            this.option = option;
            this.text = text;
            Document doc = text.getDocument();
            doc.addDocumentListener(this);
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
            InputVerifier verifier = text.getInputVerifier();
            boolean verified = verifier.verify(text);
            if (! verified) {
                text.setForeground(Color.red);
            }
            else {
                text.setForeground(LightZoneSkin.Colors.ToolPanesForeground);
                int value = Integer.parseInt(text.getText());
                option.setValue(value);

                // Trigger the text field synchronizations:

                if (text == xText) {
                    xUpdated();
                }
                if (text == yText) {
                    yUpdated();
                }
            }
        }
    }
}
