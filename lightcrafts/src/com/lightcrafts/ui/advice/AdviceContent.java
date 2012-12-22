/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.advice;

import com.lightcrafts.ui.toolkit.TextAreaFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class AdviceContent extends JPanel {

    private final static Color TextColor = Color.white;
    private final static Color Background = new Color(88, 88, 88);

    private final static float TextFontSize = 11f;

    private final static Border Border =
        BorderFactory.createEmptyBorder(6, 2, 6, 8);
    
    private UnclickableTextArea message;
    private JButton button;
    private Box buttonBox;
    private Component strut;

    AdviceContent(Advice advice) {

        String text = advice.getMessage();
        JTextArea textArea = TextAreaFactory.createTextArea(text, 20);
        textArea.setEditable(false);
        textArea.setBorder(null);
        textArea.setForeground(TextColor);
        textArea.setBackground(Background);

        message = new UnclickableTextArea(textArea);

        Font font = message.getFont();
        font = font.deriveFont(TextFontSize);
        message.setFont(font);

        button = new CloseButton();
        button.setAlignmentY(0f);

        buttonBox = Box.createVerticalBox();
        buttonBox.add(button);
        buttonBox.add(Box.createVerticalGlue());

        strut = Box.createHorizontalStrut(8);

        setOpaque(true);
        setBackground(Background);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(buttonBox);
        add(strut);
        add(message);

        setBorder(Border);
        
        addDisposeListener();
    }

    void setCursorRecurse(Cursor cursor) {
        buttonBox.setCursor(cursor);
        strut.setCursor(cursor);
        // Set the cursor redundantly on the text area, because its component
        // UI's cursor masks the cursor on the parent.
        message.setCursor(cursor);
        // Don't set the cursor on the button, which works like a button.
        button.setCursor(Cursor.getDefaultCursor());
    }

    void addMouseInputListenerRecurse(MouseInputListener listener) {
        addMouseListener(listener);
        addMouseMotionListener(listener);
        // Add the mouse listener redundantly on the text area, because its
        // component UI's mouse listeners mask the listener on the parent.
        message.addMouseInputListenerRecurse(listener);
    }

    private void addDisposeListener() {
        button.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JDialog dialog =
                        (JDialog) SwingUtilities.getAncestorOfClass(
                            JDialog.class, button
                        );
                    if (dialog != null) {
                        dialog.dispose();
                    }
                }
            }
        );
    }
}
