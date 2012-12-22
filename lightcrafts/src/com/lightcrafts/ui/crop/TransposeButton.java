/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import static com.lightcrafts.ui.crop.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class TransposeButton extends CoolButton implements ActionListener {

    private static Icon Icon = IconFactory.createInvertedIcon(
        TransposeButton.class, "transpose.png"
    );
    private ConstraintModel constraints;

    TransposeButton(ConstraintModel constraints) {
        this.constraints = constraints;

        addActionListener(this);

        setIcon(Icon);
        setFocusable(false);

        setToolTipText(LOCALE.get("TransposeToolTip"));
    }
    
    public void actionPerformed(ActionEvent event) {
        constraints.transpose();
    }
}
