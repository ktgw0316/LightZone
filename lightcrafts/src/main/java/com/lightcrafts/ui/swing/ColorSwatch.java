/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.swing;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

import com.lightcrafts.ui.toolkit.ShadowFactory;
import com.lightcrafts.utils.LCMS;
import com.lightcrafts.jai.JAIContext;

public final class ColorSwatch extends JComponent {

    ////////// public /////////////////////////////////////////////////////////

    public ColorSwatch( Color color ) {
        m_color = color;
        setFocusable( false );
    }

    public Color getColor() {
        return m_color;
    }

    public Dimension getMinimumSize() {
        return (Dimension)PREFERRED_SIZE.clone();
    }

    public Dimension getMaximumSize() {
        return (Dimension)PREFERRED_SIZE.clone();
    }

    public Dimension getPreferredSize() {
        return (Dimension)PREFERRED_SIZE.clone();
    }

    public void setColor( Color color ) {
        m_color = color;
        repaint();
    }

    ////////// protected //////////////////////////////////////////////////////

    private static LCMS.Transform ts = new LCMS.Transform(
        new LCMS.Profile( JAIContext.linearProfile ), LCMS.TYPE_RGB_8,
        new LCMS.Profile( JAIContext.systemProfile ), LCMS.TYPE_RGB_8,
        LCMS.INTENT_PERCEPTUAL, 0
    );

    protected void paintComponent( Graphics graphics ) {
        final Dimension size = getSize();

        final byte[] systemColor = new byte[3];
        ts.doTransform(
            new byte[]{
                (byte)m_color.getRed(),
                (byte)m_color.getGreen(),
                (byte)m_color.getBlue()
            },
            systemColor
        );

        final BufferedImage image = new BufferedImage(
            size.width - 6, size.height - 6, BufferedImage.TYPE_INT_RGB
        );

        Graphics g = image.getGraphics();
        g.setColor( new Color(0xff & systemColor[0], 0xff & systemColor[1], 0xff & systemColor[2]) );
        g.fillRect( 0, 0, size.width - 6, size.height - 6 );
        g.dispose();

        final ShadowFactory shadow = new ShadowFactory( 3, 1.0f, Color.gray );
        shadow.setRenderingHint(
            ShadowFactory.KEY_BLUR_QUALITY,
            ShadowFactory.VALUE_BLUR_QUALITY_HIGH
        );
        final BufferedImage shadowImage = shadow.createShadow( image );
        g = shadowImage.getGraphics();
        g.drawImage( image, 3, 2, null );
        g.dispose();

        graphics.drawImage( shadowImage, 0, 0, null );
        graphics.setColor( Color.DARK_GRAY );
        graphics.drawRect( 3, 2, size.width - 6, size.height - 6 );
    }

    ////////// private ////////////////////////////////////////////////////////

    private Color m_color;

    private final static Dimension PREFERRED_SIZE = new Dimension( 35, 35 );
}
/* vim:set et sw=4 ts=4: */
