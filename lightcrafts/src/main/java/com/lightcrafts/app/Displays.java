/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import java.awt.*;

/**
 * A simple model to provide information about displays (monitors) to the frame
 * initialization logic in the Application class.
 */
class Displays {

    // Iterate over all GraphicsConfigurations in the system.  If any of them
    // has bounds not at (0, 0), then there are multiple Displays.
    static boolean isMultipleDisplays() {
        GraphicsEnvironment env =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devs = env.getScreenDevices();
        for (GraphicsDevice dev : devs) {
            GraphicsConfiguration[] confs = dev.getConfigurations();
            for (GraphicsConfiguration conf : confs) {
                Rectangle bounds = conf.getBounds();
                if ((bounds.x != 0) || (bounds.y != 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Get the bounds of the virtual coordinate system that includes all
    // displays.
    static Rectangle getVirtualBounds() {
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment env =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devs = env.getScreenDevices();
        for (GraphicsDevice dev : devs) {
            GraphicsConfiguration[] confs = dev.getConfigurations();
            for (GraphicsConfiguration conf : confs) {
                virtualBounds = virtualBounds.union(conf.getBounds());
            }
        }
        return virtualBounds;
    }
}
