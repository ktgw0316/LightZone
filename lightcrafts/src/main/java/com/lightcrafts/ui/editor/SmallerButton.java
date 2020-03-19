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
 * An image button that zooms out.
 */

final class SmallerButton
    extends CoolButton implements ActionListener, ScaleListener
 {
    private static Icon Icon =
        IconFactory.createInvertedIcon(SmallerButton.class, "smallscale.png");

    private final static String ToolTip = LOCALE.get("ZoomOutToolTip");

    private ScaleModel scale;

    SmallerButton(ScaleModel scale) {
        this.scale = scale;
        setStyle(ButtonStyle.RIGHT);
        setIcon(Icon);
        setToolTipText(ToolTip);
        addActionListener(this);
        scale.addScaleListener(this);
    }

    // A disabled button, for the no-Document display mode.

    SmallerButton() {
        setIcon(Icon);
        setToolTipText(ToolTip);
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent event) {
        scale.scaleDown();
    }

    public void scaleChanged(Scale s) {
        boolean canScaleDown = scale.canScaleDown();
        setEnabled(canScaleDown);
    }
}
