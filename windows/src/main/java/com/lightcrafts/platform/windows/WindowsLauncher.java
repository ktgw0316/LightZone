/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.platform.windows;

import com.lightcrafts.platform.Launcher;

/**
 * Launch LightZone for Windows.
 */
public final class WindowsLauncher extends Launcher {

    ////////// public /////////////////////////////////////////////////////////

    public static void main(String[] args) {
        final Launcher launcher = new WindowsLauncher();
        launcher.init(args);
    }

    ////////// protected //////////////////////////////////////////////////////

    @Override
    protected void startForkDaemon() {
        // Do nothing.
    }
}
/* vim:set et sw=4 ts=4: */
