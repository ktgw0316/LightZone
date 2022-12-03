/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.editor;

import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFontFactory;

import javax.swing.*;

import static com.lightcrafts.ui.editor.Locale.LOCALE;

final class RotateButtons extends Box {
    private final static Icon LeftIcon = IconFontFactory.buildIcon("rotateLeft");
    private final static Icon RightIcon = IconFontFactory.buildIcon("rotateRight");
    private final static Icon HorizontalIcon = IconFontFactory.buildIcon("flipHoriz");
    private final static Icon VerticalIcon = IconFontFactory.buildIcon("flipVert");

    private final static String LeftTip = LOCALE.get("RotateLeftToolTip");
    private final static String RightTip = LOCALE.get("RotateRightToolTip");
    private final static String HorizontalToolTip = LOCALE.get("FlipHorizontalToolTip");
    private final static String VerticalToolTip = LOCALE.get("FlipVerticalToolTip");

    RotateButtons(Action leftAction, Action rightAction,
                  Action horizontalAction, Action verticalAction) {
        super(BoxLayout.X_AXIS);

        CoolButton leftButton = new CoolButton();
        CoolButton rightButton = new CoolButton();
        CoolButton horizontalButton = new CoolButton();
        CoolButton verticalButton = new CoolButton();

        leftButton.setIcon( LeftIcon );
        rightButton.setIcon( RightIcon );
        horizontalButton.setIcon( HorizontalIcon );
        verticalButton.setIcon( VerticalIcon );

        leftButton.setToolTipText( LeftTip );
        rightButton.setToolTipText( RightTip );
        horizontalButton.setToolTipText( HorizontalToolTip );
        verticalButton.setToolTipText( VerticalToolTip );

        leftButton.setStyle( CoolButton.ButtonStyle.LEFT );
        rightButton.setStyle( CoolButton.ButtonStyle.CENTER );
        horizontalButton.setStyle( CoolButton.ButtonStyle.CENTER );
        verticalButton.setStyle( CoolButton.ButtonStyle.RIGHT );

        leftButton.addActionListener( leftAction );
        rightButton.addActionListener( rightAction );
        horizontalButton.addActionListener( horizontalAction );
        verticalButton.addActionListener( verticalAction );

        add( leftButton );
        add( rightButton );
        add( horizontalButton );
        add( verticalButton );
    }

    // For the no-Document display mode.
    RotateButtons() {
        super( BoxLayout.X_AXIS );

        CoolButton leftButton = new CoolButton();
        CoolButton rightButton = new CoolButton();
        CoolButton horizontalButton = new CoolButton();
        CoolButton verticalButton = new CoolButton();

        leftButton.setIcon( LeftIcon );
        rightButton.setIcon( RightIcon );
        horizontalButton.setIcon( HorizontalIcon );
        verticalButton.setIcon( VerticalIcon );

        leftButton.setStyle( CoolButton.ButtonStyle.LEFT );
        rightButton.setStyle( CoolButton.ButtonStyle.CENTER );
        horizontalButton.setStyle( CoolButton.ButtonStyle.CENTER );
        verticalButton.setStyle( CoolButton.ButtonStyle.RIGHT );

        leftButton.setEnabled( false );
        rightButton.setEnabled( false );
        horizontalButton.setEnabled( false );
        verticalButton.setEnabled( false );

        add( leftButton );
        add( rightButton );
        add( horizontalButton );
        add( verticalButton );
    }
}
