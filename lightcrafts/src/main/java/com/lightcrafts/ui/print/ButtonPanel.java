/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

import static com.lightcrafts.ui.print.Locale.LOCALE;

class ButtonPanel extends Box {

    private JButton print;
    private JButton cancel;
    private JButton done;
    private JButton setup;

    ButtonPanel() {
        super(BoxLayout.X_AXIS);

        print = new JButton(LOCALE.get("PrintButton"));
        cancel = new JButton(LOCALE.get("CancelButton"));
        done = new JButton(LOCALE.get("DoneButton"));
        setup = new JButton(LOCALE.get("SetupButton"));

        int width = print.getPreferredSize().width;
        int maxWidth = 0;
        maxWidth = (width > maxWidth) ? width : maxWidth;

        width = cancel.getPreferredSize().width;
        maxWidth = (width > maxWidth) ? width : maxWidth;

        width = done.getPreferredSize().width;
        maxWidth = (width > maxWidth) ? width : maxWidth;

        width = setup.getPreferredSize().width;
        maxWidth = (width > maxWidth) ? width : maxWidth;

        setFixedSize(print, maxWidth);
        setFixedSize(cancel, maxWidth);
        setFixedSize(done, maxWidth);
        setFixedSize(setup, maxWidth);

        Box subBox = Box.createVerticalBox();

        subBox.add(print);
        subBox.add(Box.createVerticalStrut(3));
        subBox.add(cancel);
        subBox.add(Box.createVerticalStrut(3));
        subBox.add(done);
        subBox.add(Box.createVerticalStrut(3));
        subBox.add(setup);

        add(Box.createHorizontalGlue());
        add(subBox);
    }

    void addPrintAction(ActionListener action) {
        print.addActionListener(action);
    }

    void addCancelAction(ActionListener action) {
        cancel.addActionListener(action);
    }

    void addDoneAction(ActionListener action) {
        done.addActionListener(action);
    }

    void addPrintSetupAction(ActionListener action) {
        setup.addActionListener(action);
    }

    // Called from keyboard actions in PrintLayoutPanel.
    void doCancelActions() {
        cancel.doClick();
    }

    // Called from keyboard actions in PrintLayoutPanel.
    void doDoneActions() {
        done.doClick();
    }

    private static void setFixedSize(JButton button, int width) {
        Dimension size = button.getPreferredSize();
        size = new Dimension(width, size.height);
        button.setMinimumSize(size);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
    }

    public static void main(String[] args) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new ButtonPanel());
        JFrame frame = new JFrame("ButtonPanel Test");
        frame.setContentPane(panel);
        frame.setLocation(100, 100);
        frame.pack();
        frame.setVisible(true);
    }
}
