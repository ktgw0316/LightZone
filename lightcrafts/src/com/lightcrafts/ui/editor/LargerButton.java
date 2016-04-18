/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.Scale;
import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.app.ComboFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * An image button that zooms in.
 */

final class LargerButton
    extends CoolButton implements ActionListener, ScaleListener
{
    private static Icon Icon =
        IconFactory.createInvertedIcon(LargerButton.class, "largescale.png");

    private final static String ToolTip = LOCALE.get("ZoomInToolTip");

    private ScaleModel scale;

    LargerButton(ScaleModel scale) {
        this.scale = scale;
        setStyle(ButtonStyle.CENTER);
        setIcon(Icon);
        setToolTipText(ToolTip);
        addActionListener(this);
        scale.addScaleListener(this);
    }

    // A disabled button, for the no-Document display mode.

    LargerButton() {
        setIcon(Icon);
        setToolTipText(ToolTip);
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent event) {
        scale.scaleUp();
    }

    public void scaleChanged(Scale s) {
        boolean canScaleUp = scale.canScaleUp();
        setEnabled(canScaleUp);
    }
}
