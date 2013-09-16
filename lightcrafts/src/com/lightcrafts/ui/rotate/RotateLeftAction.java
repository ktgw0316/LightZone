/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.rotate;

import javax.swing.*;
import java.awt.event.ActionEvent;

import com.lightcrafts.ui.editor.Editor;
import com.lightcrafts.ui.editor.EditorMode;

import static com.lightcrafts.ui.rotate.Locale.LOCALE;

final class RotateLeftAction extends AbstractAction {

    private RotorControl control;

    RotateLeftAction( RotorControl control ) {
        super(LOCALE.get("RotateLeftActionName"));
        putValue(Action.SHORT_DESCRIPTION, LOCALE.get("RotateLeftToolTip"));
        this.control = control;
    }

    public void actionPerformed(ActionEvent event) {
        if ( m_editor != null )
            m_editor.setMode( EditorMode.ARROW );

        double angle = control.getAngle();
        angle -= Math.PI / 2;
        angle = Math.IEEEremainder(angle, 2 * Math.PI);
        control.setAngleInternal(angle);
        control.notifyListenersNinetyDegrees();
    }

    public void setEditor( Editor editor ) {
        m_editor = editor;
    }

    private Editor m_editor;
}
