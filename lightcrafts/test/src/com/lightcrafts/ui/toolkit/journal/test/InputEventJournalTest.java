/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit.journal.test;

import com.lightcrafts.ui.toolkit.journal.JournalDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InputEventJournalTest {

    public static void main(String[] args) {
        JFrame frame = new JFrame("InputEventJournal Test");

        JPanel panel = new JPanel(new FlowLayout());
        frame.setContentPane(panel);

        for (int n=0; n<10; n++) {
            final JButton button = new JButton(Integer.toString(n));
            panel.add(button);
            button.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        System.out.println(button.getText());
                    }
                }
            );
        }
        JTextField text = new JTextField(20);
        text.setEditable(true);
        panel.add(text);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 400, 400);
        frame.setVisible(true);

        JournalDialog.showJournalDialog(frame);
    }
}
