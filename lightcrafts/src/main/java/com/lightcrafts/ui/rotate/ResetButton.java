/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.rotate;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;

import static com.lightcrafts.ui.rotate.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;

class ResetButton extends CoolButton implements ActionListener {

    private RotorControl control;

    ResetButton(RotorControl control) {
        setText(LOCALE.get("ResetButton"));
        this.control = control;
        addActionListener(this);
        setToolTipText(LOCALE.get("ResetToolTip"));

        // Shave off some of the width padded around the button text by the L&F.
        Dimension size = getPreferredSize();
        size.width -= 22;
        setMinimumSize(size);
        setPreferredSize(size);
        setMaximumSize(size);
    }

    public void actionPerformed(ActionEvent event) {
        control.setAngleInternal(0);
        control.notifyListenersReset();
    }

    public static void main(String[] args) {
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new ResetButton(null));

        JFrame frame = new JFrame();
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 400, 400);
        frame.setVisible(true);
    }
}
