/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.ui.toolkit.TextAreaFactory;
import com.lightcrafts.utils.ErrorLogger;

import static com.lightcrafts.app.Locale.LOCALE;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionDialog extends JOptionPane {

    private final static String RegularMessage = LOCALE.get("CrashRegularMessage");
    private final static String MemoryMessage = LOCALE.get("CrashMemoryMessage");

    private final static String DetailsMessage = LOCALE.get("CrashDetailsMessage");

    private final static String SaveMessage = LOCALE.get("CrashSaveMessage");
    private final static String ContinueMessage = LOCALE.get("CrashContinueMessage");
    private final static String ExitMessage = LOCALE.get("CrashExitMessage");

    private static boolean isDisplayed; // Prevent more than one dialog at once

    private JTextArea message;
    private JTextArea trace;

    private JButton detailsButton;
    private JScrollPane detailsScroll;

    private JRadioButton saveButton;
    private JRadioButton contButton;
    private JRadioButton exitButton;

    public ExceptionDialog() {

        message = new JTextArea();
        message.setBackground(getBackground());
        message.setWrapStyleWord(true);
        message.setRows(1);
        message.setLineWrap(true);
        message.setEditable(false);
        message.setBorder(null);

        trace = new JTextArea();
        trace.setRows(20);
        trace.setColumns(100);
        trace.setEditable(true);
        trace.setLineWrap(false);
        trace.setEditable(false);

        Font font = new Font("Monospaced", Font.PLAIN, 12);
        trace.setFont(font);

        detailsButton = new JButton(DetailsMessage);
        detailsScroll = new JScrollPane(trace);

        saveButton = new JRadioButton(SaveMessage);
        contButton = new JRadioButton(ContinueMessage);
        exitButton = new JRadioButton(ExitMessage);

        saveButton.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(saveButton);
        group.add(contButton);
        group.add(exitButton);

        Box buttonBox = Box.createHorizontalBox();
        Box buttonSubBox = Box.createVerticalBox();
        buttonSubBox.add(saveButton);
        buttonSubBox.add(contButton);
        buttonSubBox.add(exitButton);
        buttonBox.add(buttonSubBox);
        buttonBox.add(Box.createHorizontalGlue());

        Box box = Box.createVerticalBox();
        box.add(message);
        box.add(Box.createVerticalStrut(12));
        box.add(buttonBox);
        box.add(Box.createVerticalStrut(12));
        box.add(Box.createVerticalStrut(12));

        setMessage(box);

        setOptions(new Object[] {detailsButton, LOCALE.get("CrashOk")});
        setInitialValue(LOCALE.get("CrashOk"));

        setMessageType(ERROR_MESSAGE);
    }

    public void handle(Throwable t) {
        t.printStackTrace();
        // Fabio and Paul have clammored for this (for developers only):
        if (System.getProperty("dieOnError") != null) {
            System.exit(-1);
        }
        showError(null, t);
    }

    private void setText(Throwable t) {
        if (t instanceof OutOfMemoryError) {
            message.setText(MemoryMessage);
        }
        else {
            message.setText(RegularMessage);
        }
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        t.printStackTrace(writer);
        trace.setText(buffer.toString());
        trace.revalidate();
    }

    static void installHandler() {
        System.setProperty(
            "sun.awt.exception.handler", ExceptionDialog.class.getName()
        );
    }

    static void uninstallHandler() {
        System.setProperty("sun.awt.exception.handler", null);
    }

    void showError(Component parent, Throwable t) {
        if (isDisplayed) {
            // Just drop errors that arrive during the error display:
            return;
        }
        setText(t);

        // Initialize scrolled to the top of the stack trace:
        trace.setCaretPosition(0);

        // Call createDialog() twice.  The first time, the message JTextArea
        // figures out its preferred size.  The second time, the dialog layout
        // will get to use it.
        createDialog(null, "");
        final JDialog display = createDialog(parent, LOCALE.get("CrashTitle"));

        ActionListener detailsAction = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                JOptionPane pane = new JOptionPane(detailsScroll);
                JDialog details = pane.createDialog(
                    display, LOCALE.get("CrashDetailTitle")
                );
                details.setVisible(true);
            }
        };
        detailsButton.addActionListener(detailsAction);
        isDisplayed = true;
        display.setVisible(true);
        isDisplayed = false;
        detailsButton.removeActionListener(detailsAction);

        if (saveButton.isSelected()) {
            // Ignore errors during the saveAll and quit:
            try {
                Application.saveAll();
                System.exit(-1);
            }
            catch (Throwable throwable) {
                System.exit(-1);
            }
        }
        else if (contButton.isSelected()) {
            // Do nothing.
        }
        else if (exitButton.isSelected()) {
            System.exit(-1);
        }
    }

    public static void main(String[] args) throws Exception {
        ExceptionDialog.installHandler();

        // Make sure this one gets displayed:
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    throw new RuntimeException("Display Me");
//                    throw new OutOfMemoryError();
                }
            }
        );
        // Make sure this one gets ignored:
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    throw new RuntimeException("Suppress Me");
                }
            }
        );
    }
}
