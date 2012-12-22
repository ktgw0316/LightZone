/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;

final class RotateButtons extends Box {

    private final static Icon LeftIcon = IconFactory.createInvertedIcon(
        RotateButtons.class, "rotateLeft.png"
    );
    private final static Icon RightIcon = IconFactory.createInvertedIcon(
        RotateButtons.class, "rotateRight.png"
    );

    private final static String LeftTip = LOCALE.get("RotateLeftToolTip");
    private final static String RightTip = LOCALE.get("RotateRightToolTip");

    RotateButtons(Action leftAction, Action rightAction) {
        super(BoxLayout.X_AXIS);

        final CoolButton leftButton = new CoolButton();
        final CoolButton rightButton = new CoolButton();

        leftButton.setIcon( LeftIcon );
        rightButton.setIcon( RightIcon );

        leftButton.setToolTipText( LeftTip );
        rightButton.setToolTipText( RightTip );

        leftButton.setStyle( CoolButton.ButtonStyle.LEFT );
        rightButton.setStyle( CoolButton.ButtonStyle.RIGHT );

        leftButton.addActionListener( leftAction );
        rightButton.addActionListener( rightAction );

        add( leftButton );
        add( rightButton );
    }

    // For the no-Document display mode.
    RotateButtons() {
        super( BoxLayout.X_AXIS );

        final CoolButton leftButton = new CoolButton();
        final CoolButton rightButton = new CoolButton();

        leftButton.setIcon( LeftIcon );
        rightButton.setIcon( RightIcon );

        leftButton.setStyle( CoolButton.ButtonStyle.LEFT );
        rightButton.setStyle( CoolButton.ButtonStyle.RIGHT );

        leftButton.setEnabled( false );
        rightButton.setEnabled( false );

        add( leftButton );
        add( rightButton );
    }
}
