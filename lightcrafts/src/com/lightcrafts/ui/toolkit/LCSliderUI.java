/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Dictionary;
import java.util.Enumeration;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class LCSliderUI extends BasicSliderUI {

    public LCSliderUI( JSlider jslider ) {
        super( jslider );
        //jslider.setBorder( BorderFactory.createLineBorder( Color.red ) );
        m_isDragging = false;
    }

    public static ComponentUI createUI( JComponent comp ) {
        return new LCSliderUI( (JSlider)comp );
    }

    public Dimension getMinimumHorizontalSize() {
        return MINIMUM_HORIZONTAL_SIZE;
    }

    public Dimension getMinimumVerticalSize() {
        return MINIMUM_VERTICAL_SIZE;
    }

    public Dimension getPreferredHorizontalSize() {
        return PREFERRED_HORIZONTAL_SIZE;
    }

    public Dimension getPreferredVerticalSize() {
        return PREFERRED_VERTICAL_SIZE;
    }

    public void installUI( JComponent comp ) {
        final boolean flag = comp.isOpaque();
        super.installUI( comp );
        slider.setOpaque( flag );
        updateFont();
    }

    public void paint( Graphics g, JComponent unused ) {
        recalculateIfInsetsChanged();
        final Rectangle clipRect = g.getClipBounds();
        if ( slider.getPaintTrack() ) {
            if ( !clipRect.intersects( trackRect ) )
                calculateGeometry();
            if ( clipRect.intersects( trackRect ) ||
                 clipRect.intersects( thumbRect ) )
                paintTrack( g );
        }
        if ( slider.getPaintTicks() && clipRect.intersects( tickRect ) )
            paintTicks( g );
        if ( slider.getPaintLabels() && clipRect.intersects( labelRect ) )
            paintLabels( g );
        if ( clipRect.intersects( thumbRect ) )
            paintThumb( g );
    }

    public void paintFocus( Graphics g ) {
    }

    public void paintLabels( Graphics g ) {
        final Color color = slider.isEnabled() ? Color.black : Color.gray;
        final Dictionary labelTable = slider.getLabelTable();
        if ( labelTable == null )
            return;
        final Enumeration keys = labelTable.keys();
        final int min = slider.getMinimum();
        final int max = slider.getMaximum();
        while ( keys.hasMoreElements() ) {
            final int integer = (Integer)keys.nextElement();
            if ( integer >= min && integer <= max ) {
                final Component comp = (Component)labelTable.get( integer );
                comp.setForeground( color );
                switch ( slider.getOrientation() ) {
                    case JSlider.HORIZONTAL:
                        g.translate( 0, labelRect.y );
                        paintHorizontalLabel( g, integer, comp );
                        g.translate( 0, -labelRect.y );
                        break;
                    case JSlider.VERTICAL:
                        final int dx = Utils.isLeftToRight( slider ) ?
                            0 : labelRect.width - comp.getPreferredSize().width;
                        g.translate( labelRect.x + dx, 0 );
                        paintVerticalLabel( g, integer, comp );
                        g.translate( -labelRect.x - dx, 0 );
                        break;
                    default:
                        throw new IllegalStateException( "bad orientation" );
                }
            }
        }
    }

    public void paintTicks( Graphics g ) {
        final Graphics2D g2d = (Graphics2D)g;
        final Object hint = Utils.beginFont( g2d );
        superPaintTicks( g );
        Utils.endFont( g2d, hint );
    }

    public void paintTrack( Graphics g ) {
        final boolean isEnabled = slider.isEnabled();
        final boolean isHorizontal =
            slider.getOrientation() == JSlider.HORIZONTAL;
        final Graphics2D g2d = (Graphics2D) g;
        if ( g2d == null )
            return;

        final Color oldColor = g2d.getColor();
        final Stroke oldStroke = g2d.getStroke();
        g2d.setStroke(TRACK_STROKE);

        for ( int i = 0; i < 3; ++i ) {
            if ( isEnabled )
                g2d.setColor(
                    new Color(
                        TRACK_COLORS[i],
                        TRACK_COLORS[i],
                        TRACK_COLORS[i]
                    )
                );
            else
                g2d.setColor( Color.lightGray );

            final int lOff = i == 1 ? 0 : -1;
            if ( isHorizontal )
                g2d.drawLine(
                    (int)trackRect.getMinX() - lOff,
                    (int)trackRect.getCenterY() + i - 1,
                    (int)trackRect.getMaxX() + lOff,
                    (int)trackRect.getCenterY() + i - 1
                );
            else
                g2d.drawLine(
                    (int)trackRect.getCenterX() + i - 1,
                    (int)trackRect.getMinY() - lOff,
                    (int)trackRect.getCenterX() + i - 1,
                    (int)trackRect.getMaxY() + lOff
                );
        }

        g2d.setStroke( oldStroke );
        g2d.setColor( oldColor );
    }

    private static final BufferedImage triangleThumb;

    private static final Color lightThumbColor = new Color(185, 185, 185);
    private static final Color darkThumbColor = new Color(110, 110, 110);
    private static final Color shadowThumbColor = new Color(25, 25, 25);

    static {
        final int width = 14;
        final int height = 9;

        final Polygon triangle = new Polygon(
            new int[]{ 0, width / 2, width },
            new int[]{ 0, height, 0 },
            3
        );

        final BufferedImage buf =
            new BufferedImage(width, height+2, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = (Graphics2D) buf.getGraphics();
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        // g2d.setColor(new Color(85, 85, 85));
        g2d.setPaint(new GradientPaint(0, 0, lightThumbColor, width/2, 0, darkThumbColor));
        g2d.fill(triangle);
        g2d.dispose();
        final ShadowFactory sf = new ShadowFactory(4, 0.6f, shadowThumbColor);
        sf.setRenderingHint(ShadowFactory.KEY_BLUR_QUALITY, ShadowFactory.VALUE_BLUR_QUALITY_HIGH);
        triangleThumb = sf.createShadow(buf);
        g2d = (Graphics2D) triangleThumb.getGraphics();
        g2d.drawRenderedImage(buf, AffineTransform.getTranslateInstance(3, 2));
        g2d.dispose();
    }

    public void paintThumb( Graphics g ) {
        final Graphics2D g2d = (Graphics2D) g;
        if ( g2d == null )
            return;
        g2d.drawImage(triangleThumb, null, thumbRect.x-1, thumbRect.y);
    }

    ////////// protected //////////////////////////////////////////////////////

    protected void calculateContentRect() {
        super.calculateContentRect();
    }

    protected void calculateThumbLocation() {
        super.calculateThumbLocation();
        switch ( slider.getOrientation() ) {
            case JSlider.HORIZONTAL:
                thumbRect.y = trackRect.y + /*(shouldUseArrowThumb() ?*/ 3 /*: 0)*/;
                break;
            case JSlider.VERTICAL:
                thumbRect.x = trackRect.x + /*(shouldUseArrowThumb() ?*/ 2 /*: 0)*/;
                break;
            default:
                throw new IllegalStateException( "bad orientation" );
        }
    }

    protected void calculateThumbSize() {
        final Dimension size = getThumbSize();
        thumbRect.setSize( size.width + 4, size.height + 4 );
    }

    protected void calculateTickRect() {
        if ( slider.getOrientation() == JSlider.HORIZONTAL ) {
            tickRect.height = slider.getPaintTicks() ? getTickLength() : 0;
            tickRect.x = trackRect.x + trackBuffer;
            tickRect.y = (trackRect.y + trackRect.height) - tickRect.height / 2;
            tickRect.width = trackRect.width - trackBuffer * 2;
        } else {
            tickRect.width = slider.getPaintTicks() ? getTickLength() : 0;
            tickRect.x = (trackRect.x + trackRect.width) - tickRect.width / 2;
            tickRect.y = trackRect.y + trackBuffer;
            tickRect.height = trackRect.height - trackBuffer * 2;
        }
    }

    protected void calculateTrackRect() {
        switch ( slider.getOrientation() ) {

            case JSlider.HORIZONTAL:
                int height = thumbRect.height;
                if ( slider.getPaintTicks() )
                    height += getTickLength();
                if ( slider.getPaintLabels() )
                    height += getHeightOfTallestLabel();
                trackRect.x = contentRect.x + trackBuffer;
                trackRect.y = contentRect.y + (contentRect.height - height - 1) / 2;
                trackRect.width = contentRect.width - trackBuffer * 2;
                trackRect.height = thumbRect.height;
                break;

            case JSlider.VERTICAL:
                int width = thumbRect.width;
                if ( Utils.isLeftToRight( slider ) ) {
                    if ( slider.getPaintTicks() )
                        width += getTickLength();
                    if ( slider.getPaintLabels() )
                        width += getWidthOfWidestLabel();
                } else {
                    if ( slider.getPaintTicks() )
                        width -= getTickLength();
                    if ( slider.getPaintLabels() )
                        width -= getWidthOfWidestLabel();
                }
                trackRect.x = contentRect.x + (contentRect.width - width - 1) / 2;
                trackRect.y = contentRect.y + trackBuffer;
                trackRect.width = thumbRect.width;
                trackRect.height = contentRect.height - trackBuffer * 2;
                break;

            default:
                throw new IllegalStateException( "bad orientation" );
        }
    }

    protected ChangeListener createChangeListener( JSlider unused ) {
        return new MyChangeListener();
    }

    protected PropertyChangeListener createPropertyChangeListener( JSlider unused ) {
        return new MyPropertyChangeHandler();
    }

    protected TrackListener createTrackListener( JSlider unused ) {
        return new MyTrackListener();
    }

    protected Dimension getThumbSize() {
        return new Dimension( 13, 14 );
    }

    protected boolean shouldUseArrowThumb() {
        return slider.getPaintTicks() || slider.getPaintLabels();
    }

    ////////// private ////////////////////////////////////////////////////////

    private static class Utils {

        static Object beginFont( Graphics2D g2d ) {
            final Object hint =
                g2d.getRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING );
            g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
            return hint;
        }

        static void endFont( Graphics2D g2d, Object hint ) {
            g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, hint
            );
        }

        static boolean isLeftToRight(Component component) {
            return component.getComponentOrientation().isLeftToRight();
        }
    }

    private class MyChangeListener implements ChangeListener {
        public void stateChanged( ChangeEvent unused ) {
            if ( !m_isDragging ) {
                calculateThumbLocation();
                slider.repaint();
            }
        }
    }

    private class MyPropertyChangeHandler extends PropertyChangeHandler {
        public void propertyChange( PropertyChangeEvent event ) {
            final String propName = event.getPropertyName();
            if ( propName.equals( "font" ) )
                updateFont();
            else if ( propName.equals( "Frame.active" ) )
                slider.repaint();
            else
                super.propertyChange( event );
        }
    }

    private class MyTrackListener extends TrackListener {

        public void mouseDragged( MouseEvent event ) {
            if ( !slider.isEnabled() )
                return;
            currentMouseX = event.getX();
            currentMouseY = event.getY();
            if ( !m_isDragging )
                return;
            slider.setValueIsAdjusting( true );
            switch ( slider.getOrientation() ) {

                case JSlider.HORIZONTAL:
                    final int halfWidth = thumbRect.width / 2;
                    int xo = event.getX() - offset;
                    int x1 = trackRect.x;
                    int x2 = trackRect.x + (trackRect.width - 1);
                    final int xv = xPositionForValue(
                        slider.getMaximum() - slider.getExtent()
                    );
                    if ( drawInverted() )
                        x1 = xv;
                    else
                        x2 = xv;
                    xo = Math.max( xo, x1 - halfWidth );
                    xo = Math.min( xo, x2 - halfWidth );
                    setThumbLocation( xo, thumbRect.y );
                    slider.setValue( valueForXPosition( xo + halfWidth ) );
                    break;

                case JSlider.VERTICAL:
                    final int halfHeight = thumbRect.height / 2;
                    int yo = event.getY() - offset;
                    int y1 = trackRect.y;
                    int y2 = trackRect.y + (trackRect.height - 1);
                    final int yv = yPositionForValue(
                        slider.getMaximum() - slider.getExtent()
                    );
                    if ( drawInverted() )
                        y2 = yv;
                    else
                        y1 = yv;
                    yo = Math.max( yo, y1 - halfHeight );
                    yo = Math.min( yo, y2 - halfHeight );
                    setThumbLocation( thumbRect.x, yo );
                    slider.setValue( valueForYPosition( yo + halfHeight ) );
                    break;

                default:
                    throw new IllegalStateException( "bad orientation" );
            }
        }

        public void mouseMoved( MouseEvent unused ) {
        }

        public void mousePressed( MouseEvent event ) {
            if ( !slider.isEnabled() )
                return;
            currentMouseX = event.getX();
            currentMouseY = event.getY();
            if ( slider.isRequestFocusEnabled() )
                slider.requestFocus();
            if ( thumbRect.contains( currentMouseX, currentMouseY ) ) {
                switch ( slider.getOrientation() ) {
                    case JSlider.HORIZONTAL:
                        offset = currentMouseX - thumbRect.x;
                        break;
                    case JSlider.VERTICAL:
                        offset = currentMouseY - thumbRect.y;
                        break;
                    default:
                        throw new IllegalStateException( "bad orientation" );
                }
                m_isDragging = true;
                return;
            }
            m_isDragging = false;
            slider.setValueIsAdjusting( true );
            final Dimension sliderSize = slider.getSize();
            final byte direction;
            switch ( slider.getOrientation() ) {

                case JSlider.HORIZONTAL:
                    if ( thumbRect.isEmpty() ) {
                        final int halfWidth = sliderSize.width / 2;
                        if ( drawInverted() )
                            direction = (byte)(currentMouseX >= halfWidth ? -1 : 1);
                        else
                            direction = (byte)(currentMouseX >= halfWidth ? 1 : -1);
                        break;
                    }
                    final int l = thumbRect.x;
                    if ( drawInverted() )
                        direction = (byte)(currentMouseX >= l ? -1 : 1);
                    else
                        direction = (byte)(currentMouseX >= l ? 1 : -1);
                    break;

                case JSlider.VERTICAL:
                    if ( thumbRect.isEmpty() ) {
                        final int halfHeight = sliderSize.height / 2;
                        if ( drawInverted() )
                            direction = (byte)(currentMouseY >= halfHeight ? 1 : -1);
                        else
                            direction = (byte)(currentMouseY >= halfHeight ? -1 : 1);
                        break;
                    }
                    final int j = thumbRect.y;
                    if ( !drawInverted() )
                        direction = (byte)(currentMouseY >= j ? -1 : 1);
                    else
                        direction = (byte)(currentMouseY >= j ? 1 : -1);
                    break;

                default:
                    throw new IllegalStateException( "bad orientation" );
            }
            scrollDueToClickInTrack( direction );
            if ( !thumbRect.contains( currentMouseX, currentMouseY ) &&
                 shouldScroll( direction ) ) {
                scrollTimer.stop();
                scrollListener.setDirection( direction );
                scrollTimer.start();
            }
        }

        public void mouseReleased( MouseEvent mouseevent ) {
            if ( !slider.isEnabled() )
                return;
            offset = 0;
            scrollTimer.stop();
            if ( slider.getSnapToTicks() ) {
                m_isDragging = false;
                slider.setValueIsAdjusting( false );
            } else {
                slider.setValueIsAdjusting( false );
                m_isDragging = false;
            }
            slider.repaint();
        }

        public boolean shouldScroll( int direction ) {
            if ( slider.getOrientation() == JSlider.VERTICAL ) {
                if ( drawInverted() ? direction < 0 : direction > 0 ) {
                    if ( thumbRect.y + thumbRect.height <= currentMouseY )
                        return false;
                } else if ( thumbRect.y >= currentMouseY )
                    return false;
            } else if ( drawInverted() ? direction < 0 : direction > 0 ) {
                if ( thumbRect.x + thumbRect.width >= currentMouseX )
                    return false;
            } else if ( thumbRect.x <= currentMouseX )
                return false;
            if ( direction > 0 &&
                 slider.getValue() + slider.getExtent() >= slider.getMaximum() )
                return false;
            return direction >= 0 || slider.getValue() > slider.getMinimum();
        }
    }

    private void superPaintTicks( Graphics g ) {
        if ( slider.isEnabled() )
            g.setColor( Color.gray );
        else
            g.setColor( Color.lightGray );

        switch ( slider.getOrientation() ) {

            case JSlider.HORIZONTAL:
                g.translate( 0, tickRect.y );
                if ( slider.getMinorTickSpacing() > 0 )
                    for ( int i = slider.getMinimum(); i <= slider.getMaximum();
                          i += slider.getMinorTickSpacing() ) {
                        final int x = xPositionForValue( i );
                        paintMinorTickForHorizSlider( g, tickRect, x );
                    }
                if ( slider.getMajorTickSpacing() > 0 ) {
                    for ( int i = slider.getMinimum(); i <= slider.getMaximum();
                          i += slider.getMajorTickSpacing() ) {
                        final int x = xPositionForValue( i );
                        paintMajorTickForHorizSlider( g, tickRect, x );
                    }

                }
                g.translate( 0, -tickRect.y );
                break;

            case JSlider.VERTICAL:
                g.translate( tickRect.x, 0 );
                if ( slider.getMinorTickSpacing() > 0 ) {
                    int dx = 0;
                    if ( !Utils.isLeftToRight( slider ) ) {
                        dx = tickRect.width - tickRect.width / 2;
                        g.translate( dx, 0 );
                    }
                    for ( int i = slider.getMinimum(); i <= slider.getMaximum();
                          i += slider.getMinorTickSpacing() ) {
                        final int y = yPositionForValue( i );
                        paintMinorTickForVertSlider( g, tickRect, y );
                    }
                    if ( !Utils.isLeftToRight( slider ) )
                        g.translate( -dx, 0 );
                }
                if ( slider.getMajorTickSpacing() > 0 ) {
                    if ( !Utils.isLeftToRight( slider ) )
                        g.translate( 2, 0 );
                    for ( int i = slider.getMinimum(); i <= slider.getMaximum();
                          i += slider.getMajorTickSpacing() ) {
                        final int y = yPositionForValue( i );
                        paintMajorTickForVertSlider( g, tickRect, y );
                    }
                    if ( !Utils.isLeftToRight( slider ) )
                        g.translate( -2, 0 );
                }
                g.translate( -tickRect.x, 0 );
                break;

            default:
                throw new IllegalStateException();
        }
    }

    private void updateFont() {
        final Font font = slider.getFont();

        if ( font == null )
            return;

        calculateGeometry();
        slider.repaint();
    }

    private static final int[] TRACK_COLORS = { 45, 90, 120 };
    private static final Stroke TRACK_STROKE = new BasicStroke( 1F );

    private static final Dimension MINIMUM_HORIZONTAL_SIZE =
        new Dimension( 36, 15 );
    private static final Dimension MINIMUM_VERTICAL_SIZE =
        new Dimension( 15, 36 );
    private static final Dimension PREFERRED_HORIZONTAL_SIZE =
        new Dimension( 190, 15 );
    private static final Dimension PREFERRED_VERTICAL_SIZE =
        new Dimension( 15, 190 );

    private boolean m_isDragging;
}
/* vim:set et sw=4 ts=4: */
