/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.Engine;
import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.*;

/**
 * A toggle to switch the engine on and off.  Goes in the DocHeader.
 */

final class EyeButton extends CoolButton implements MouseListener {

    private final static Icon Icon =
        IconFactory.createIcon(EyeButton.class, "eye.png");

    private final static String OriginalTooltip =
        LOCALE.get("EyeToolTip");

    private Engine engine;

    EyeButton(Engine engine) {
        this.engine = engine;
        setIcon(Icon);
        setToolTipText(OriginalTooltip);
        addMouseListener(this);
    }

    // Make a disabled EyeButton, for the no-Document display mode:
    EyeButton() {
        setIcon(Icon);
        setToolTipText(OriginalTooltip);
        setEnabled(false);
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent event) {
        engine.setActive(false);
    }

    public void mouseReleased(MouseEvent event) {
        engine.setActive(true);
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public static void main(String[] args) {
        EyeButton disabled = new EyeButton();
        disabled.setEnabled(false);
        EyeButton enabled = new EyeButton();
        enabled.setEnabled(true);

        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(enabled);
        frame.getContentPane().add(disabled);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
