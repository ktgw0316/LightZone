/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.Application;
import com.lightcrafts.app.ComboFrame;

import java.awt.event.ActionEvent;

/**
 * The Exit menu item for Windows and Linux.  (Mac OS X has native Cocoa code
 * call <code>MacOSXLauncher.quit()</code> instead.)
 */
final class ExitMenuItem extends UpdatableMenuItem {

    ExitMenuItem(ComboFrame frame) {
        super(frame, "Exit");
    }

    public void actionPerformed(ActionEvent event) {
        performPreAction( event );
        Application.quit();
    }
}
