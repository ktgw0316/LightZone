/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import com.lightcrafts.model.Scale;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.app.ComboFrame;

import static com.lightcrafts.ui.editor.Locale.LOCALE;

/**
 * An image button that zooms in.
 */

final class OneToOneButton
    extends CoolButton implements ActionListener, ScaleListener
{
     final static Scale Unity = new Scale(1, 1);

     private static Icon Icon =
         IconFactory.createInvertedIcon(OneToOneButton.class, "1to1.png");

     private final static String ToolTip = LOCALE.get("OneToOneToolTip");

     private ScaleModel scale;

     OneToOneButton(ScaleModel scale) {
         this.scale = scale;
         setStyle(ButtonStyle.LEFT);
         setIcon(Icon);
         setToolTipText(ToolTip);
         addActionListener(this);
         scale.addScaleListener(this);
     }

     // A disabled button, for the no-Document display mode.

     OneToOneButton() {
         setIcon(Icon);
         setToolTipText(ToolTip);
         setEnabled(false);
     }

     public void actionPerformed(ActionEvent event) {
         scale.setScale(Unity);
     }

     public void scaleChanged(Scale scale) {
         boolean isUnity = scale.equals(Unity);
         setEnabled(! isUnity);
     }
}
