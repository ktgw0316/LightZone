/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.swing;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;

/**
 * A <code>RangeSelectorZoneTrack</code> paints a 16-zone track for a
 * {@link RangeSelector}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class RangeSelectorZoneTrack
    extends CommonRangeSelectorTrack implements RangeSelector.Track {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public void paintTrack( RangeSelector selector, Rectangle r,
                            Graphics2D g2d ) {
        final int w = r.width - 2;      // -2 for inset border
        final int h = r.height - 2;
        final int y = r.y + 1;
        final float zoneWidth = (float)w / ZONES;
        final float[] colorArray = new float[3];
        final Rectangle2D r2d = new Rectangle2D.Float();

        for ( int z = 0; z < ZONES; ++z ) {
            final float[] c = zoneToColor( z, colorArray );
            g2d.setColor( new Color( c[0], c[1], c[2] ) );
            final float x = r.x + 1 + z * zoneWidth;
            r2d.setRect( x, y, zoneWidth, h );
            g2d.fill( r2d );
        }

        paintBorder( r, g2d );
    }

    /**
     * Convert a zone to a color.
     *
     * @param zone The zone in the range [0,15].
     * @param rgb An array to use for temporary storage or <code>null</code> if
     * none.
     * @return Returns the RGB values (in the range [0,1]) for the zone.
     */
    public static float[] zoneToColor( int zone, float[] rgb ) {
        assert zone >= 0 && zone < ZONES;
        if ( rgb == null )
            rgb = new float[3];

        final float c =
            (float)((Math.pow( 2, zone * 8.0 / (ZONES - 1) ) - 1) / 255.);
        rgb[0] = rgb[1] = rgb[2] = c;

        return Functions.fromLinearToCS( JAIContext.systemColorSpace, rgb );
    }

    ////////// private ////////////////////////////////////////////////////////

    private static final int ZONES = 16;
}
/* vim:set et sw=4 ts=4: */
