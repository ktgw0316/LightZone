/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import javax.swing.*;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.EditorMode;
import com.lightcrafts.ui.editor.Editor;

/**
 * A base class for menu items that want to update something when properties
 * change on a ComboFrame.
 */
abstract class UpdatableMenuItem extends JMenuItem implements ActionListener {

    // Menu items leak on the Mac; no hard references allowed.
    private WeakReference<ComboFrame> frameRef;

    protected UpdatableMenuItem(ComboFrame frame, String key) {
        frameRef = new WeakReference<ComboFrame>(frame);
        MenuFactory.configureMenuItem(key, this);
        //noinspection OverridableMethodCallInConstructor
        addActionListener(this);
    }

    ComboFrame getComboFrame() {
        return frameRef.get();
    }

    void update() {
        // default is to do nothing
    }

    public abstract void actionPerformed(ActionEvent event);

    protected void performPreAction( ActionEvent event ) {
        final ComboFrame frame = getComboFrame();
        if ( frame != null ) {
            final Editor editor = frame.getEditor();
            editor.setMode( EditorMode.ARROW );
        }
    }
}
