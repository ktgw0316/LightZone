/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.swing;

import java.awt.*;

/**
 * A <code>CommonRangeSelectorTrack</code> is an abstract base class for other
 * implementors of {@link RangeSelector.Track}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
abstract class CommonRangeSelectorTrack {

    ////////// protected //////////////////////////////////////////////////////

    protected static void paintBorder( Rectangle r, Graphics2D g2d ) {
        final int x2 = r.x + r.width - 1;
        final int y2 = r.y + r.height - 1;
        g2d.setColor( BORDER_DARK_COLOR );
        g2d.drawLine( r.x, r.y, r.x, y2 - 1 );
        g2d.drawLine( r.x, r.y, x2, r.y );
        g2d.setColor( BORDER_LIGHT_COLOR );
        g2d.drawLine( r.x, y2, x2, y2 );
        g2d.drawLine( x2, r.y + 1, x2, y2 );
    }

    ////////// private ////////////////////////////////////////////////////////

    private static final Color BORDER_DARK_COLOR = Color.BLACK;
    private static final Color BORDER_LIGHT_COLOR = Color.LIGHT_GRAY;
}
/* vim:set et sw=4 ts=4: */
