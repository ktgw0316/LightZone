/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.advice;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;

/**
 * A JPanel that holds a JTextArea and overlays it with an invisible
 * component whose only purpose is to absorb mouse events.
 */
class UnclickableTextArea extends JPanel {

    private JTextArea text;
    private JPanel overlay;

    UnclickableTextArea(JTextArea text) {
        this.text = text;

        overlay = new JPanel();
        overlay.setOpaque(false);
        overlay.addMouseListener(new MouseAdapter() {});
        overlay.addMouseMotionListener(new MouseMotionAdapter() {});

        setLayout(null);
        add(overlay);
        add(text);
    }

    public Dimension getPreferredSize() {
        return text.getPreferredSize();
    }

    public void doLayout() {
        Dimension size = getSize();
        text.setSize(size);
        overlay.setSize(size);
    }

    void addMouseInputListenerRecurse(MouseInputListener listener) {
        addMouseListener(listener);
        addMouseMotionListener(listener);
        overlay.addMouseListener(listener);
        overlay.addMouseMotionListener(listener);
    }

    public static void main(String[] args) {

        JTextArea text = new JTextArea("hello world");
        UnclickableTextArea unclick = new UnclickableTextArea(text);

        JFrame frame = new JFrame("Test");
        frame.setContentPane(unclick);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
