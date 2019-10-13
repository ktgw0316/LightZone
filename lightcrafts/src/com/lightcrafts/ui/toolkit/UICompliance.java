/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import com.lightcrafts.platform.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;

public class UICompliance {
    public static int showOptionDialog(Component parentComponent,
        Object message, String title, int optionType, int messageType,
        Icon icon, final Object[] options, Object initialValue, int destructive)
        throws HeadlessException {
        final JOptionPane pane = new JOptionPane(message, messageType,
                                                 optionType, icon,
                                                 options, initialValue);

        pane.setInitialValue(initialValue);
        pane.setComponentOrientation(((parentComponent == null) ?
	    JOptionPane.getRootFrame() : parentComponent).getComponentOrientation());

        JDialog dialog = pane.createDialog(parentComponent, title);

        for (int i = 2; i < options.length; i++) {
            final int o = i;
            Action action = new AbstractAction() {
                public void actionPerformed(ActionEvent event) {
                    pane.setValue(options[o]);
                }
            };
            KeyStroke stroke = KeyStroke.getKeyStroke(((String)options[i]).charAt(0),
                                                      Platform.isMac()
                                                      ? InputEvent.META_MASK
                                                      : InputEvent.CTRL_MASK);
            dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, action);
            dialog.getRootPane().getActionMap().put(action, action);
        }
        pane.selectInitialValue();
        dialog.setVisible(true);
        dialog.dispose();

        Object selectedValue = pane.getValue();

        if(selectedValue == null)
            return JOptionPane.CLOSED_OPTION;
        for(int counter = 0, maxCounter = options.length;
            counter < maxCounter; counter++) {
            if(options[counter].equals(selectedValue))
                return counter;
        }
        return JOptionPane.CLOSED_OPTION;
    }
}
