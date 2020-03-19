/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import static com.lightcrafts.ui.toolkit.Locale.LOCALE;

// See ColorDropperControl and ColorPickerDropperControl, where this button
// is used, to understand its action.

public final class DropperButton
    extends CoolToggleButton implements ItemListener {

    public DropperButton() {
        setIcon( Icon );
        setToolTipText( m_startToolTip );
        addItemListener( this );
    }

    public void setToolTips( String start, String end ) {
        m_startToolTip = start;
        m_endToolTip = end;
        itemStateChanged( null );
    }

    public void itemStateChanged( ItemEvent event ) {
        setToolTipText( isSelected() ? m_endToolTip : m_startToolTip );
    }

    ////////// private ////////////////////////////////////////////////////////

    private static final String StartToolTipText =
        LOCALE.get( "DropperStartToolTip" );
    private static final String EndToolTipText =
        LOCALE.get( "DropperEndToolTip" );

    private final static Icon Icon =
        IconFactory.createInvertedIcon( DropperButton.class, "dropper.png" );

    private String m_startToolTip = StartToolTipText;
    private String m_endToolTip = EndToolTipText;
}
/* vim:set et sw=4 ts=4: */
