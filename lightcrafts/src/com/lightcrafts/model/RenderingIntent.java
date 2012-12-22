/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

/**
 * TODO.
 */
public final class RenderingIntent {

    public static final RenderingIntent ABSOLUTE_COLORIMETRIC =
        new RenderingIntent( "Absolute Colorimetric" );

    public static final RenderingIntent PERCEPTUAL =
        new RenderingIntent( "Perceptual" );

    public static final RenderingIntent RELATIVE_COLORIMETRIC =
        new RenderingIntent( "Relative Colorimetric" );

    public static final RenderingIntent RELATIVE_COLORIMETRIC_BP =
        new RenderingIntent( "Relative Colorimetric with BPC" );

    public static final RenderingIntent SATURATION =
        new RenderingIntent( "Saturation" );

    public boolean equals( Object o ) {
        if ( !(o instanceof RenderingIntent) )
            return false;
        final RenderingIntent r = (RenderingIntent)o;
        return r.m_name.equals( m_name );
    }

    public static RenderingIntent[] getAll() {
        return new RenderingIntent[] {
            ABSOLUTE_COLORIMETRIC,
            PERCEPTUAL,
            RELATIVE_COLORIMETRIC,
            RELATIVE_COLORIMETRIC_BP,
            SATURATION,
        };
    }

    public int hashCode() {
        return m_name.hashCode() + 1;
    }

    public String toString() {
        return m_name;
    }

    ////////// private ////////////////////////////////////////////////////////

    private RenderingIntent( String name ) {
        m_name = name;
    }

    private final String m_name;
}
/* vim:set et sw=4 ts=4: */
