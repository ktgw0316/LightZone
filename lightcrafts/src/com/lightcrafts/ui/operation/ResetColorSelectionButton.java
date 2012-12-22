/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import java.awt.event.ActionListener;
import javax.swing.*;

import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import static com.lightcrafts.ui.operation.Locale.LOCALE;

/**
 * A <code>ResetColorSelectionButton</code> is the button that is used to reset
 * the color/brightness selection to encompass all colors at all brightnesses.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class ResetColorSelectionButton extends CoolButton {

    ////////// package ////////////////////////////////////////////////////////

    ResetColorSelectionButton( ActionListener listener ) {
        setIcon( ICON );
        setToolTipText( TOOL_TIP );
        addActionListener( listener );
    }

    ////////// private ////////////////////////////////////////////////////////

    private final static Icon ICON =
        IconFactory.createInvertedIcon( ResetColorSelectionButton.class, "reset_cs.png" );

    private final static String TOOL_TIP =
        LOCALE.get( "ResetColorSelectionToolTip" );
}
/* vim:set et sw=4 ts=4: */
