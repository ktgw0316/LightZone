/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.swing;

import java.awt.*;

/**
 * A <code>RangeSelectorColorGradientTrack</code> paints a color gradient for
 * a {@link RangeSelector}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class RangeSelectorColorGradientTrack extends CommonRangeSelectorTrack
    implements RangeSelector.Track {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public void paintTrack( RangeSelector selector, Rectangle r,
                            Graphics2D g2d ) {
        final int w = r.width - 2;      // -2 for inset border
        final int y1 = r.y + 1;
        final int y2 = r.y + r.height - 1;

        final int lfvx = selector.getLowerThumbFeatheringValueX();
        final int lvx = selector.getLowerThumbValueX();
        final int uvx = selector.getUpperThumbValueX();
        final int ufvx = selector.getUpperThumbFeatheringValueX();

        final int offset = selector.getTrackValue();
        final float o = (float)offset / selector.getMaximumTrackValue();

        for ( int dx = 0; dx < w; ++dx ) {
            final int x = r.x + 1 + dx;
            final float h = (float)dx / w - o;
            final float b;

            if ( x > lvx && x < uvx )
                b = BRIGHTNESS_MAX;
            else if ( x < lfvx || x > ufvx )
                b = BRIGHTNESS_MIN;
            else
                b = BRIGHTNESS_MID;
            g2d.setColor( Color.getHSBColor( h, 1, b ) );
            g2d.drawLine( x, y1, x, y2 );
        }

        paintBorder( r, g2d );
    }

    ////////// private ////////////////////////////////////////////////////////

    private static final float BRIGHTNESS_MAX = 1.0F;
    private static final float BRIGHTNESS_MIN = 0.3F;
    private static final float BRIGHTNESS_MID =
        (BRIGHTNESS_MIN + BRIGHTNESS_MAX) / 2;
}
/* vim:set et sw=4 ts=4: */
