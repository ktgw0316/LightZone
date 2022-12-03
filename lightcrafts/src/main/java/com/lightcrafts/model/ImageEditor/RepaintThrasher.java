/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;

/**
 * Show a frame with a control that thrashes repaint on a JComponent and
 * gives feedback about the frequency.
 */

class RepaintThrasher extends JFrame implements ChangeListener {

    private JComponent comp;    // the JComponent to repaint
    private JButton button;
    private JTextField counter;
    private int count;

    private boolean run;    // true while the button is pressed

    RepaintThrasher(JComponent comp) {
        super("Repaint Thrasher");
        this.comp = comp;

        button = new JButton("Repaint");
        button.addChangeListener(this);

        counter = new JTextField();
        counter.setColumns(4);

        JPanel panel = new JPanel();
        panel.add(button);
        panel.add(counter);
        setContentPane(panel);
    }

    private void increment() {
        counter.setText(Integer.toString(++count));
    }

    private void reset() {
        count = 0;
        counter.setText(Integer.toString(count));
    }

    private void queueOneRecursive() {
        if (run) {
            EventQueue.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        comp.repaint();
                        increment();
                        EventQueue.invokeLater(
                            new Runnable() {
                                public void run() {
                                    queueOneRecursive();
                                }
                            }
                        );
                    }
                }
            );
        }
    }

    // JButton ChangeListener:

    @Override
    public void stateChanged(ChangeEvent event) {
        boolean pressed = button.getModel().isPressed();
        if (pressed && ! run) {
            run = true;
            reset();
            queueOneRecursive();
        }
        else if (run && ! pressed) {
            run = false;
        }
    }
}
