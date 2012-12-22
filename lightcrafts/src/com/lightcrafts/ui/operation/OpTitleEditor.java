/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.ui.operation.OpControl.OpControlUndoSupport;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

/**
 * Listen for double-clicks on OpTitle labels, provide an editor for them,
 * and update OpTitles with the results.
 */

class OpTitleEditor extends MouseAdapter {

    private final static Border on =
        BorderFactory.createLineBorder(Color.lightGray);
    private final static Border off =
        BorderFactory.createEmptyBorder(1, 1, 1, 1);

    private OpTitle title;
    private JTextField label;

    // We add multiple exit listeners to the editor, and we only want to
    // exit once.
    private boolean isEditing;

    private OpControlUndoSupport undo;

    OpTitleEditor(OpTitle title, OpControlUndoSupport undo) {
        this.title = title;
        this.undo = undo;
    }

    void setLabel(JTextField label) {
        if (this.label != null) {
            this.label.removeMouseListener(this);
        }
        this.label = label;
        if (label != null) {
            label.addMouseListener(this);
            label.setBorder(off);
        }
    }

    // Show a rollover highlight:
    public void mouseEntered(MouseEvent event) {
        label.setBorder(on);
        label.repaint();
    }

    // Hide the rollover highlight:
    public void mouseExited(MouseEvent event) {
        label.setBorder(off);
        label.repaint();
    }
    // On mouse double-click, launch the editor:
    public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() != 2) {
            return;
        }
        final JTextField editor = new JTextField();
        final String oldText = label.getText();
        editor.setText(oldText);
        Border border = BorderFactory.createLineBorder(Color.gray);
        editor.setBorder(border);

        Dimension size = editor.getPreferredSize();
        editor.setPreferredSize(new Dimension(145, size.height));

        // All these invokeLater's fix glitches when the layout
        // must revalidate in the event handlers.

        // On focus lost, unlaunch the editor:
        editor.addFocusListener(
            new FocusAdapter() {
                public void focusLost(FocusEvent event) {
                    if (isEditing) {
                        final String text = editor.getText();
                        EventQueue.invokeLater(
                            new Runnable() {
                                public void run() {
                                    title.resetTitle(text);
                                    undo.postEdit("Change Tool Name");
                                }
                            }
                        );
                    }
                }
            }
        );
        // On action performed, unlaunch the editor:
        editor.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    if (isEditing) {
                        final String text = editor.getText();
                        EventQueue.invokeLater(
                            new Runnable() {
                                public void run() {
                                    title.resetTitle(text);
                                    undo.postEdit("Change Tool Name");
                                    isEditing = false;
                                }
                            }
                        );
                    }
                }
            }
        );
        // On the ESC key, unlaunch the editor without committing changes:
        editor.addKeyListener(
            new KeyListener() {
                public void keyPressed(KeyEvent e) {
                    if (isEditing) {
                        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            EventQueue.invokeLater(
                                new Runnable() {
                                    public void run() {
                                        title.resetTitle(oldText);
                                        isEditing = false;
                                    }
                                }
                            );
                        }
                    }
                }
                public void keyReleased(KeyEvent e) {
                }
                public void keyTyped(KeyEvent e) {
                }
            }
        );
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    title.setTitle(editor);
                    editor.requestFocusInWindow();
                    isEditing = true;
                }
            }
        );
    }
}
