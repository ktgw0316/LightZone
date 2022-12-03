/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.test;

import com.lightcrafts.ui.region.RegionOverlay;

import javax.swing.*;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegionOverlayTest {

    private static UndoManager undo;
    private static JButton r;
    private static JButton u;

    private static void updateButtons() {
        u.setEnabled(undo.canUndo());
        r.setEnabled(undo.canRedo());
    }

    public static void main(final String[] args) throws Exception {
        EventQueue.invokeLater(new Runnable() { public void run() {
            final RegionOverlay overlay = new RegionOverlay();
            overlay.setCookie(new Object(), false, true);
            Dimension size = new Dimension(400, 400);
            overlay.setPreferredSize(size);

//            // Useful for making icons out of screenshots:
//            overlay.setTransform(AffineTransform.getScaleInstance(4, 4));

            undo = new UndoManager() {
                public boolean addEdit(UndoableEdit edit) {
                    boolean result = super.addEdit(edit);
                    updateButtons();
                    return result;
                }
            };
            overlay.addUndoableEditListener(undo);

            u = new JButton("Undo");
            u.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        if (undo.canUndo()) {
                            undo.undo();
                            overlay.repaint();
                        }
                        updateButtons();
                    }
                }
            );

            r = new JButton("Redo");
            r.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        if (undo.canRedo()) {
                            undo.redo();
                            overlay.repaint();
                        }
                        updateButtons();
                    }
                }
            );

            updateButtons();

            Box buttons = Box.createHorizontalBox();
            buttons.add(Box.createHorizontalGlue());
            buttons.add(u);
            buttons.add(Box.createHorizontalGlue());
            buttons.add(r);
            buttons.add(Box.createHorizontalGlue());

            JFrame frame = new JFrame("Test");
            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add(overlay);
            frame.getContentPane().add(buttons, BorderLayout.SOUTH);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocation(100, 100);
            frame.setVisible(true);
        }});
    }
}
