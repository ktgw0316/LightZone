/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.swing;

import java.awt.*;
import javax.swing.Icon;

/**
 * A <code>NoIcon</code> is an {@link Icon} that takes up no space.  It is used
 * in places where an {@link Icon} needs to be passed but you really don't want
 * an icon at all.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class NoIcon implements Icon {

    /** The singleton static instance. */
    public static final NoIcon INSTANCE = new NoIcon();

    /**
     * {@inheritDoc
     */
    public int getIconHeight() {
        return 0;
    }

    /**
     * {@inheritDoc
     */
    public int getIconWidth() {
        return 0;
    }

    /**
     * {@inheritDoc
     */
    public void paintIcon( Component c, Graphics g, int x, int y ) {
        // do nothing
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton static instance.
     */
    private NoIcon() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */