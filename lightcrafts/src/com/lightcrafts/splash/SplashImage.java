/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.splash;

import com.lightcrafts.utils.Version;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * A BufferedImage made from another BufferedImage with some text drawn on
 * top.  Suitable for annotating a splash image with dynamic data, like
 * release tags.
 */
public final class SplashImage extends BufferedImage {

    ////////// public /////////////////////////////////////////////////////////

    public SplashImage( String text ) {
        this( new String[]{ text } );
    }

    public SplashImage( String[] texts ) {
        super(
            backgroundImage().getWidth(), backgroundImage().getHeight(), TYPE_INT_ARGB
        );
        m_staticText = texts;
        update( "" );
    }

    public StartupProgress getStartupProgress() {
        return new StartupProgress() {
            public void startupMessage( String text ) {
                super.startupMessage( text );
                update( text );
                SplashWindow.repaintInstance();
            }
        };
    }

    /**
     * Get the test that is to be overprinted onto the splash screen.
     */
    public static String[] getDefaultSplashText( String licenseText ) {
        final String name = Version.getVersionName();
        final String revision = Version.getRevisionNumber();
        final String versionText = "Version " + name + " (" + revision + ')';
        if ( licenseText != null )
            return new String[]{ versionText, licenseText };
        return new String[]{ versionText };
    }

    ////////// private ////////////////////////////////////////////////////////

    private static BufferedImage m_splashImage = null;

    private String[] m_staticText;      // There is also dynamic text.

    private static final Color m_textColor = new Color( 204, 204, 204 );
    private static final Font m_textFont = new Font( "SansSerif", Font.PLAIN, 12 );
    private static final Point m_textLoc = new Point( 57, 107 );

    static synchronized BufferedImage backgroundImage() {
        if (m_splashImage == null) {
            URL url = SplashImage.class.getResource("resources/Splash.png");
            try {
                m_splashImage = ImageIO.read(url);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return m_splashImage;
    }

    private void update( String dynamicText ) {
        // Copy the background image:
        final Graphics2D g = (Graphics2D)getGraphics();
        g.drawImage( backgroundImage(), new AffineTransform(), null );

        // Draw the text:
        g.setColor( m_textColor );
        g.setFont( m_textFont );
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        int y = m_textLoc.y;
        final FontMetrics metrics = g.getFontMetrics( m_textFont );
        final int dy = 0 - metrics.getHeight() - 5;
        for ( int i = 0; i < m_staticText.length; ++i ) {
            g.drawString( m_staticText[i], m_textLoc.x, y );
            y += dy;
        }
        g.drawString( dynamicText, m_textLoc.x, y );
    }
}
/* vim:set et sw=4 ts=4: */
