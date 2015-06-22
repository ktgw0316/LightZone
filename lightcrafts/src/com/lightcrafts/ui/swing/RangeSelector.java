/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.swing;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.toolkit.ShadowFactory;

/**
 * A <code>RangeSelector</code> is like a {@link JSlider} except that it has
 * two knobs.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class RangeSelector extends JComponent {

    /**
     * A <code>Track</code> is used to paint the track of a
     * {@link RangeSelector}.
     */
    public interface Track {

        /**
         * Paints the track of a {@link RangeSelector}.
         *
         * @param s The {@link RangeSelector} to paint the track of.
         * @param r The {@link Rectangle} to paint within.
         * @param g The graphics context to use.
         */
        void paintTrack( RangeSelector s, Rectangle r, Graphics2D g );
    }

    /**
     * Constructs a <code>RangeSelector</code>.
     */
    public RangeSelector() {
        this( new RangeSelectorModel(), false );
    }

    /**
     * Constructs a <code>RangeSelector</code>.
     *
     * @param minimumThumbValue The minimum thumb value.
     * @param maximumThumbValue The maximum thumb value.
     */
    public RangeSelector( int minimumThumbValue, int maximumThumbValue ) {
        this(
            minimumThumbValue, maximumThumbValue,
            minimumThumbValue, minimumThumbValue,
            maximumThumbValue, maximumThumbValue
        );
    }

    /**
     * Constructs a <code>RangeSelector</code>.
     *
     * @param minimumThumbValue The minimum thumb value.
     * @param maximumThumbValue The maximum thumb value.
     * @param lowerThumbValue The lower thumb value.
     * @param lowerFeatheringValue The lower thumb feathering value.
     * @param upperThumbValue The upper thumb value.
     * @param upperThumbFeatheringValue The upper thumb feathering value.
     */
    public RangeSelector( int minimumThumbValue, int maximumThumbValue,
                          int lowerThumbValue, int lowerFeatheringValue,
                          int upperThumbValue, int upperThumbFeatheringValue ) {
        this(
            new RangeSelectorModel(
                minimumThumbValue, maximumThumbValue,
                lowerThumbValue, lowerFeatheringValue,
                upperThumbValue, upperThumbFeatheringValue,
                0, Integer.MAX_VALUE, 0
            ),
            false
        );
    }

    /**
     * Constructs a <code>RangeSelector</code>.
     *
     * @param minimumThumbValue The minimum thumb value.
     * @param maximumThumbValue The maximum thumb value.
     * @param lowerThumbValue The lower thumb value.
     * @param lowerFeatheringValue The lower thumb feathering value.
     * @param upperThumbValue The upper thumb value.
     * @param upperThumbFeatheringValue The upper thumb feathering value.
     */
    public RangeSelector( int minimumThumbValue, int maximumThumbValue,
                          int lowerThumbValue, int lowerFeatheringValue,
                          int upperThumbValue, int upperThumbFeatheringValue,
                          int minimumTrackValue, int maximumTrackValue,
                          int trackValue, boolean trackValueWraps ) {
        this(
            new RangeSelectorModel(
                minimumThumbValue, maximumThumbValue,
                lowerThumbValue, lowerFeatheringValue,
                upperThumbValue, upperThumbFeatheringValue,
                minimumTrackValue, maximumTrackValue, trackValue
            ),
            trackValueWraps
        );
    }

    /**
     * Constructs a <code>RangeSelector</code>.
     *
     * @param model The {@link RangeSelectorModel} to use.
     * @param trackValueWraps If <code>true</code>, the track value is allowed
     * to wrap around.
     */
    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    public RangeSelector( RangeSelectorModel model, boolean trackValueWraps ) {
        m_model = model;
        m_trackValueWraps = trackValueWraps;
        addComponentListener(
            new ComponentAdapter() {
                public void componentResized( ComponentEvent e ) {
                    stateChanged();
                }
            }
        );
        setFocusable( false );
        final LocalMouseListener listener = new LocalMouseListener();
        addMouseListener( listener );
        addMouseMotionListener( listener );
    }

    /**
     * Adds a {@link ChangeListener} to this component.
     *
     * @param listener The {@link ChangeListener} to add.
     * @see #removeChangeListener(ChangeListener)
     */
    public void addChangeListener( ChangeListener listener ) {
        listenerList.add( ChangeListener.class, listener );
    }

    /**
     * Gets the track value.
     *
     * @return Returns said value.
     * @see #setTrackValue(int)
     */
    public int getTrackValue() {
        return m_model.getTrackValue();
    }

    /**
     * Gets the track modulo value.
     *
     * @return Returns said value.
     * @see #getTrackValue()
     */
    public boolean getTrackValueWraps() {
        return m_trackValueWraps;
    }

    /**
     * Returns an array of all the {@link ChangeListener}s added to this
     * component with {@link #addChangeListener(ChangeListener)}.
     *
     * @return Returns all of the {@link ChangeListener}s added or an empty
     * array if no listeners have been added.
     */
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners( ChangeListener.class );
    }

    /**
     * Gets the lower thumb feathering value.
     *
     * @return Returns said value.
     * @see #getLowerThumbFeatheringValueX()
     * @see #getUpperThumbFeatheringValue()
     * @see #getLowerThumbValue()
     */
    public int getLowerThumbFeatheringValue() {
        return m_model.getLowerThumbFeatheringValue();
    }

    /**
     * Gets the X coordinate of the lower thumb feathering value.
     *
     * @return Returns said coordinate.
     * @see #getLowerThumbFeatheringValue()
     * @see #getUpperThumbFeatheringValueX()
     */
    public int getLowerThumbFeatheringValueX() {
        return m_polygons[ LOWER_FEATHERING_THUMB ].xpoints[0];
    }

    /**
     * Gets the lower thumb value.
     *
     * @return Returns said value.
     * @see #getLowerThumbFeatheringValueX()
     * @see #getUpperThumbValue()
     */
    public int getLowerThumbValue() {
        return m_model.getLowerThumbValue();
    }

    /**
     * Gets the X coordinate of the lower thumb value.
     *
     * @return Returns said coordinate.
     * @see #getLowerThumbFeatheringValueX()
     * @see #getLowerThumbValue()
     * @see #getUpperThumbValueX()
     */
    public int getLowerThumbValueX() {
        return m_polygons[ LOWER_THUMB ].xpoints[0];
    }

    /**
     * Gets the maximum thumb value.
     *
     * @return Returns said value.
     * @see #getMinimumThumbValue()
     */
    public int getMaximumThumbValue() {
        return m_model.getMaximumThumbValue();
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getMaximumSize() {
        final Insets in = getInsets();
        return new Dimension(
            Integer.MAX_VALUE,
            in.top + in.bottom + Math.max( ARROW_HEIGHT, THUMB_HEIGHT )
        );
    }

    /**
     * Gets the maximum track value.
     *
     * @return Returns said value.
     * @see #getMinimumTrackValue()
     */
    public int getMaximumTrackValue() {
        return m_model.getMaximumTrackValue();
    }

    /**
     * Gets the minimum thumb value.
     *
     * @return Returns said value.
     * @see #getMaximumThumbValue()
     */
    public int getMinimumThumbValue() {
        return m_model.getMinimumThumbValue();
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getMinimumSize() {
        final Insets in = getInsets();
        return new Dimension(
            100,
            in.top + in.bottom + Math.max( ARROW_HEIGHT, THUMB_HEIGHT )
        );
    }

    /**
     * Gets the minimum track value.
     *
     * @return Returns said value.
     * @see #getMaximumTrackValue()
     */
    public int getMinimumTrackValue() {
        return m_model.getMinimumTrackValue();
    }

    /**
     * Gets the {@link RangeSelectorModel} is use.
     *
     * @return Returns said model.
     * @see #setModel(RangeSelectorModel)
     */
    public RangeSelectorModel getModel() {
        return m_model;
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getPreferredSize() {
        final int modelWidth =
            m_model.getMaximumThumbValue() - m_model.getMinimumThumbValue();
        final Insets in = getInsets();
        return new Dimension(
            modelWidth + in.left + in.right + ARROW_WIDTH * 2 + THUMB_WIDTH,
            in.top + in.bottom + Math.max( ARROW_HEIGHT, THUMB_HEIGHT )
        );
    }

    /**
     * Gets the current {@link Track}.
     *
     * @return Returns said {@link Track}.
     * @see #setTrack(Track)
     */
    public Track getTrack() {
        return m_track;
    }

    /**
     * Gets the upper thumb feathering value.
     *
     * @return Returns said value.
     * @see #getLowerThumbFeatheringValue()
     * @see #getUpperThumbFeatheringValueX()
     */
    public int getUpperThumbFeatheringValue() {
        return m_model.getUpperThumbFeatheringValue();
    }

    /**
     * Gets the X coordinate of the upper thumb feathering value.
     *
     * @return Returns said coordinate.
     * @see #getLowerThumbValueX()
     * @see #getUpperThumbFeatheringValue()
     * @see #getUpperThumbValueX()
     */
    public int getUpperThumbFeatheringValueX() {
        return m_polygons[ UPPER_FEATHERING_THUMB ].xpoints[0];
    }

    /**
     * Gets the upper thumb value.
     *
     * @return Returns said value.
     * @see #getLowerThumbValue()
     * @see #getUpperThumbValueX()
     */
    public int getUpperThumbValue() {
        return m_model.getUpperThumbValue();
    }

    /**
     * Gets the X coordinate of the upper thumb value.
     *
     * @return Returns said coordinate.
     * @see #getLowerThumbValueX()
     * @see #getUpperThumbFeatheringValueX()
     * @see #getUpperThumbValue()
     */
    public int getUpperThumbValueX() {
        return m_polygons[ UPPER_THUMB ].xpoints[0];
    }

    /**
     * Removes a {@link ChangeListener} from this component.
     *
     * @param listener The {@link ChangeListener} to remove.
     * @see #addChangeListener(ChangeListener)
     */
    public void removeChangeListener( ChangeListener listener ) {
        listenerList.remove( ChangeListener.class, listener );
    }

    /**
     * Sets the lower thumb feathering value.  The following constraints must
     * always hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     * @param newValue The new lower feathering value.
     * @throws IllegalArgumentException if any constraint is violated.
     * @see #getLowerThumbFeatheringValue()
     * @see #setLowerThumbValue(int)
     */
    public void setLowerThumbFeatheringValue( int newValue ) {
        if ( m_model.setLowerFeatheringValue( newValue ) )
            fireStateChanged();
    }

    /**
     * Sets the lower thumb value.  The following constraints must always hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     * @param newValue The new lower thumb value.
     * @throws IllegalArgumentException if any constraint is violated.
     * @see #getLowerThumbValue()
     * @see #setLowerThumbFeatheringValue(int)
     */
    public void setLowerThumbValue( int newValue ) {
        if ( m_model.setLowerThumbValue( newValue ) )
            fireStateChanged();
    }

    /**
     * Sets the maximum thumb value.  The following constraints must always
     * hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     *
     * @param newValue The new maximum thumb value.
     * @throws IllegalArgumentException if any constraint is violated.
     * @see #setMinimumThumbValue(int)
     */
    public void setMaximumThumbValue( int newValue ) {
        if ( m_model.setMaximumThumbValue( newValue ) )
            fireStateChanged();
    }

    /**
     * Sets the minimum thumb value.  The following constraints must always
     * hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     *
     * @param newValue The new minimum thumb value.
     * @throws IllegalArgumentException if any constraint is violated.
     * @see #setMaximumThumbValue(int)
     */
    public void setMinimumThumbValue( int newValue ) {
        if ( m_model.setMinimumThumbValue( newValue ) )
            fireStateChanged();
    }

    /**
     * Sets the model to use.
     *
     * @param newModel The new {@link RangeSelectorModel}.
     * @see #getModel()
     */
    public void setModel( RangeSelectorModel newModel ) {
        if ( newModel != m_model ) {
            m_model = newModel;
            fireStateChanged();
        }
    }

    /**
     * Sets the various properties of the selector.  The following constraints
     * must always hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     *
     * @param minimumThumbValue The minimum thumb value.
     * @param maximumThumbValue The maximum thumb value.
     * @param lowerThumbValue The lower thumb value.
     * @param lowerThumbFeatheringValue The lower thumb feathering value.
     * @param upperThumbValue The upper thumb value.
     * @param upperThumbFeatheringValue The upper thumb feathering value.
     * @param minimumTrackValue The minimum track value.
     * @param maximumTrackValue The maximum track value.
     * @param trackValue The track value.
     * @param trackValueWraps The modulo value to mod the track value by.
     * @throws IllegalArgumentException if any constraint is violated.
     */
    public void setProperties( int minimumThumbValue, int maximumThumbValue,
                               int lowerThumbValue,
                               int lowerThumbFeatheringValue,
                               int upperThumbValue,
                               int upperThumbFeatheringValue,
                               int minimumTrackValue, int maximumTrackValue,
                               int trackValue, boolean trackValueWraps ) {
        boolean changed = m_model.setThumbProperties(
            minimumThumbValue, maximumThumbValue,
            lowerThumbValue, lowerThumbFeatheringValue,
            upperThumbValue, upperThumbFeatheringValue
        );
        changed = m_model.setTrackProperties(
            minimumTrackValue, maximumTrackValue, trackValue
        ) || changed;
        if ( changed )
            fireStateChanged();
    }

    /**
     * Sets the {@link Track} to use.
     *
     * @param t The new {@link Track}.
     * @see #getTrack()
     */
    public void setTrack( Track t ) {
        m_track = t;
    }

    /**
     * Sets the track value.
     *
     * @param newValue The new track value.
     * @see #getTrackValue()
     */
    public void setTrackValue( int newValue ) {
        if ( m_model.setTrackValue( newValue ) )
            fireStateChanged();
    }

    /**
     * Sets the upper thumb feathering value.  The following constraints must
     * always hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     *
     * @param newValue The new upper thumb feathering value.
     * @throws IllegalArgumentException if any constraint is violated.
     * @see #getUpperThumbFeatheringValue()
     * @see #setUpperThumbValue(int)
     */
    public void setUpperThumbFeatheringValue( int newValue ) {
        if ( m_model.setUpperThumbFeatheringValue( newValue ) )
            fireStateChanged();
    }

    /**
     * Sets the upper thumb value.  The following constraints must always hold:
     * <blockquote>
     * min &lt;= LTFV &lt;= LTV &lt;= UTV &lt;= UTFV &lt;= max
     * </blockquote>
     * @param newValue The new upper thumb value.
     * @throws IllegalArgumentException if any constraint is violated.
     * @see #getUpperThumbValue()
     * @see #setUpperThumbFeatheringValue(int)
     */
    public void setUpperThumbValue( int newValue ) {
        if ( m_model.setUpperThumbValue( newValue ) )
            fireStateChanged();
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected void paintComponent( Graphics g ) {
        if ( m_polygons == null ) {
            positionPolygons();
            if ( m_polygons == null )
                return;
        }

        final Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
        );

        if ( isOpaque() ) {
            g2d.setColor( getBackground() );
            g2d.fillRect( 0, 0, getWidth(), getHeight() );
        }

        //
        // Paint the track.
        //
        final Insets in = getInsets();
        final Rectangle trackRect = new Rectangle(
            getMinimumThumbX(),
            in.top,
            getWidth() - in.left - in.right - ARROW_WIDTH * 2 - THUMB_WIDTH,
            getHeight() - in.top - in.bottom
        );
        m_track.paintTrack( this, trackRect, g2d );

        //
        // Paint the thumbs.
        //
        g2d.setColor( Color.BLACK );
        for ( int i = THUMB_INDEX_START; i <= THUMB_INDEX_END; ++i ) {
            final Rectangle r = m_polygons[i].getBounds();
            g2d.drawImage( m_images[ i % 2 ], null, r.x, r.y );
        }

        //
        // Paint the arrows.
        //
        if ( m_trackValueWraps ) {
            g2d.setColor( ARROW_COLOR );
            g2d.fill( m_polygons[ LOWER_SCROLL_ARROW ] );
            g2d.fill( m_polygons[ UPPER_SCROLL_ARROW ] );
        }

        //
        // Highlight the polygon the mouse is hovering over, if any.
        //
        for ( Shape s : m_polygons )
            if ( s == m_hoverPolygon ) {
                g2d.setColor( HOVER_COLOR );
                g2d.draw( s );
                break;
            }
    }

    /**
     * Only one {@link ChangeEvent} is needed per slider instance since the
     * event's only (read-only) state is the source property.  The source of
     * events generated here is always "this". The event is lazily created the
     * first time that an event notification is fired.
     *
     * @see #fireStateChanged()
     */
    protected transient ChangeEvent m_changeEvent;

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A <code>LocalMouseListener</code> handles mouse events for the
     * {@link RangeSelector}.
     */
    private final class LocalMouseListener extends MouseAdapter
        implements MouseMotionListener
    {
        ////////// MouseAdapter methods ///////////////////////////////////////

        /**
         * Handle a mouse-pressed event: either the start of a drag or a click
         * in one of the arrows.
         *
         * @param me The {@link MouseEvent}.
         */
        public void mousePressed( MouseEvent me ) {
            if ( me.isPopupTrigger() || ignoreMouseEvent() )
                return;
            final Point p = me.getPoint();
            m_mousePointX = p.x;
            m_foundPolygonIndex = findPolygonIndex( p );
            switch ( m_foundPolygonIndex ) {
                case LOWER_SCROLL_ARROW:
                case UPPER_SCROLL_ARROW:
                    scrollOnce( m_foundPolygonIndex );
                    startScrollTimer( m_foundPolygonIndex );
                    break;
            }
        }

       /**
         * Handle a mouse-released event: kill the scroll timer, if any.
         *
         * @param me The {@link MouseEvent}.
         */
        public void mouseReleased( MouseEvent me ) {
            killScrollTimer();
        }

        ////////// MouseMotionListener methods ////////////////////////////////

        /**
         * {@inheritDoc}
         */
        public void mouseDragged( MouseEvent me ) {
            if ( ignoreMouseEvent() || m_foundPolygonIndex == -1 )
                return;
            final int newDragPointX = me.getPoint().x;
            m_inDrag = true;
            try {
                switch ( m_foundPolygonIndex ) {
                    case LOWER_SCROLL_ARROW:
                    case UPPER_SCROLL_ARROW:
                        // do nothing
                        break;
                    case TRACK:
                        dragAllThumbs( newDragPointX );
                        break;
                    default:
                        dragThumb( newDragPointX );
                }
            }
            catch ( IllegalArgumentException e ) {
                // ignore
            }
            m_inDrag = false;
/*
            if ( m_stateChanged )
                fireStateChanged();
*/
        }

        /**
         * {@inheritDoc}
         */
        public void mouseExited( MouseEvent me ) {
            if ( m_hoverPolygon != null ) {
                m_hoverPolygon = null;
                repaint();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void mouseMoved( MouseEvent me ) {
            if ( ignoreMouseEvent() )
                return;
            final Shape oldHoverPolygon = m_hoverPolygon;
            m_hoverPolygon = findPolygon( me.getPoint() );
            if ( m_hoverPolygon != oldHoverPolygon )
                repaint();
        }

        ////////// private ////////////////////////////////////////////////////

        /**
         * Drag all of the thumbs.
         *
         * @param newDragPointX The X coordinate of the mouse while dragging.
         */
        private void dragAllThumbs( int newDragPointX ) {
            final int minX = getMinimumThumbX();
            final int maxX = getMaximumThumbX();
            final int lfvx = getLowerThumbFeatheringValueX();
            final int lvx = getLowerThumbValueX();
            final int uvx = getUpperThumbValueX();
            final int ufvx = getUpperThumbFeatheringValueX();

            int dx = newDragPointX - m_mousePointX;
            final int[] newX = new int[5];

            if ( dx < 0 ) {             ////////// Dragging to the left
                newX[ LOWER_FEATHERING_THUMB ] = lfvx + dx;
                if ( newX[ LOWER_FEATHERING_THUMB ] < minX ) {
                    //
                    // Prevent the thumb from being dragged below the minimum
                    // X.
                    //
                    newX[ LOWER_FEATHERING_THUMB ] = minX;
                    dx = minX - lfvx;
                    newDragPointX = m_mousePointX + dx;
                }
                newX[ LOWER_THUMB ] = lvx + dx;
                newX[ UPPER_THUMB ] = uvx + dx;
                newX[ UPPER_FEATHERING_THUMB ] = ufvx + dx;
            } else {                    ////////// Dragging to the right
                newX[ UPPER_FEATHERING_THUMB ] = ufvx + dx;
                if ( newX[ UPPER_FEATHERING_THUMB ] > maxX ) {
                    //
                    // Prevent the thumb from being dragged above the maximum
                    // X.
                    //
                    newX[ UPPER_FEATHERING_THUMB ] = maxX;
                    dx = maxX - ufvx;
                    newDragPointX = m_mousePointX + dx;
                }
                newX[ LOWER_FEATHERING_THUMB ] = lfvx + dx;
                newX[ LOWER_THUMB ] = lvx + dx;
                newX[ UPPER_THUMB ] = uvx + dx;
            }

            final int[] newV = new int[4];
            for ( int i = THUMB_INDEX_START; i <= THUMB_INDEX_END; ++i ) {
                newV[i] = mapXToThumbValue( newX[i] );
                movePolygonTo( i, newX[i] );
            }

            setProperties(
                getMinimumThumbValue(), getMaximumThumbValue(),
                newV[ LOWER_THUMB ],
                newV[ LOWER_FEATHERING_THUMB ],
                newV[ UPPER_THUMB ],
                newV[ UPPER_FEATHERING_THUMB ],
                getMinimumTrackValue(), getMaximumTrackValue(),
                getTrackValue(), getTrackValueWraps()
            );
            m_mousePointX = newDragPointX;
        }

        /**
         * Drag one of the thumbs.
         *
         * @param newDragPointX The X coordinate of the mouse while dragging.
         */
        private void dragThumb( int newDragPointX ) {
            final int minX = getMinimumThumbX();
            final int maxX = getMaximumThumbX();
            final int lfvx = getLowerThumbFeatheringValueX();
            final int lvx = getLowerThumbValueX();
            final int uvx = getUpperThumbValueX();
            final int ufvx = getUpperThumbFeatheringValueX();

            int dx = newDragPointX - m_mousePointX;
            int newX = 0;
            int partnerIndex = -1;
            int partnerNewX = 0;

            if ( dx < 0 ) {             ////////// Dragging to the left

                switch ( m_foundPolygonIndex ) {

                    case LOWER_FEATHERING_THUMB: {
                        newX = lfvx + dx;
                        if ( newX < minX ) {
                            //
                            // Prevent the thumb from being dragged below the
                            // minimum X.
                            //
                            newX = newDragPointX = minX;
                        }
                        final int lfv2 = mapXToThumbValue( newX );
                        setLowerThumbFeatheringValue( lfv2 );
                        break;
                    }

                    case LOWER_THUMB: {
                        newX = lvx + dx;
                        if ( newX < minX ) {
                            //
                            // Prevent the thumb from being dragged below the
                            // minimum X.
                            //
                            newX = newDragPointX = minX;
                            dx = minX - lvx;
                        }
                        final int lv2 = mapXToThumbValue( newX );
                        if ( lfvx > minX ) {
                            //
                            // Dragging the lower thumb drags the lower
                            // feathering thumb also.
                            //
                            partnerNewX = lfvx + dx;
                            if ( partnerNewX < minX )
                                partnerNewX = minX;
                            final int lfv2 = mapXToThumbValue( partnerNewX );
                            try {
                                setLowerThumbFeatheringValue( lfv2 );
                                partnerIndex = LOWER_FEATHERING_THUMB;
                            }
                            catch ( IllegalArgumentException e ) {
                                // ignore
                            }
                        }

                        if ( getLowerThumbFeatheringValue() > lv2 ) {
                            setLowerThumbFeatheringValue( lv2 );
                            partnerIndex = LOWER_FEATHERING_THUMB;
                        }

                        setLowerThumbValue( lv2 );
                        break;
                    }

                    case UPPER_THUMB: {
                        newX = uvx + dx;
                        if ( newX < lvx + THUMB_WIDTH ) {
                            //
                            // Prevent the upper thumb from hitting the lower
                            // thumb.
                            //
                            newX = newDragPointX = lvx + THUMB_WIDTH + 1;
                            dx = newX - uvx;
                        }
                        //
                        // Dragging the upper thumb drags the upper feathering
                        // thumb also.
                        //
                        partnerIndex = UPPER_FEATHERING_THUMB;
                        partnerNewX = ufvx + dx;
                        final int uv2 = mapXToThumbValue( newX );
                        final int ufv2 = mapXToThumbValue( partnerNewX );
                        setUpperThumbValue( uv2 );
                        setUpperThumbFeatheringValue( ufv2 );
                        break;
                    }

                    case UPPER_FEATHERING_THUMB: {
                        newX = ufvx + dx;
                        int ufv2 = mapXToThumbValue( newX );
                        if ( newX <= uvx ) {
                            //
                            // The upper feathering thumb bumped into the upper
                            // thumb: "push" the upper thumb along.
                            //
                            if ( newX < lvx + THUMB_WIDTH ) {
                                //
                                // Prevent the upper thumb from hitting the
                                // lower thumb.
                                //
                                newX = lvx + THUMB_WIDTH + 1;
                                newDragPointX = newX;
                                ufv2 = mapXToThumbValue( newX );
                            }
                            setUpperThumbValue( ufv2 );
                            partnerIndex = UPPER_THUMB;
                            partnerNewX = newX;
                        }
                        setUpperThumbFeatheringValue( ufv2 );
                        break;
                    }

                }

            } else {                    ////////// Dragging to the right

                switch ( m_foundPolygonIndex ) {

                    case LOWER_FEATHERING_THUMB: {
                        newX = lfvx + dx;
                        int lfv2 = mapXToThumbValue( newX );
                        if ( newX >= lvx ) {
                            //
                            // The lower feathering thumb bumped into the lower
                            // thumb: "push" the lower thumb along.
                            //
                            if ( newX >= uvx - THUMB_WIDTH ) {
                                //
                                // Prevent the lower thumb from hitting the
                                // upper thumb.
                                //
                                newX = uvx - THUMB_WIDTH - 1;
                                newDragPointX = newX;
                                lfv2 = mapXToThumbValue( newX );
                            }
                            setLowerThumbValue( lfv2 );
                            partnerIndex = LOWER_THUMB;
                            partnerNewX = newX;
                        }
                        setLowerThumbFeatheringValue( lfv2 );
                        break;
                    }

                    case LOWER_THUMB: {
                        newX = lvx + dx;
                        if ( newX > uvx - THUMB_WIDTH ) {
                            //
                            // Prevent the lower thumb from hitting the upper
                            // thumb.
                            //
                            newX = newDragPointX = uvx - THUMB_WIDTH - 1;
                            dx = newX - lvx;
                        }
                        //
                        // Dragging the lower thumb drags the lower feathering
                        // thumb also.
                        //
                        partnerIndex = LOWER_FEATHERING_THUMB;
                        partnerNewX = lfvx + dx;
                        final int lv2 = mapXToThumbValue( newX );
                        final int lfv2 = mapXToThumbValue( partnerNewX );
                        setLowerThumbValue( lv2 );
                        setLowerThumbFeatheringValue( lfv2 );
                        break;
                    }

                    case UPPER_THUMB: {
                        newX = uvx + dx;
                        if ( newX > maxX ) {
                            //
                            // Prevent the thumb from being dragged above the
                            // maximum X.
                            //
                            newX = newDragPointX = maxX;
                            dx = maxX - uvx;
                        }
                        final int uv2 = mapXToThumbValue( newX );
                        if ( ufvx < maxX ) {
                            //
                            // Dragging the upper thumb drags the upper
                            // feathering thumb also.
                            //
                            partnerNewX = ufvx + dx;
                            if ( partnerNewX > maxX )
                                partnerNewX = maxX;
                            final int ufv2 = mapXToThumbValue( partnerNewX );
                            try {
                                setUpperThumbFeatheringValue( ufv2 );
                                partnerIndex = UPPER_FEATHERING_THUMB;
                            }
                            catch ( IllegalArgumentException e ) {
                                // ignore
                            }
                        }

                        if ( getUpperThumbFeatheringValue() < uv2 ) {
                            setUpperThumbFeatheringValue( uv2 );
                            partnerIndex = UPPER_FEATHERING_THUMB;
                        }

                        setUpperThumbValue( uv2 );
                        break;
                    }

                    case UPPER_FEATHERING_THUMB: {
                        newX = ufvx + dx;
                        if ( newX > maxX ) {
                            //
                            // Prevent the thumb from being dragged above the
                            // maximum X.
                            //
                            newX = newDragPointX = maxX;
                        }
                        final int ufv2 = mapXToThumbValue( newX );
                        setUpperThumbFeatheringValue( ufv2 );
                        break;
                    }

                }
            }

            movePolygonTo( m_foundPolygonIndex, newX );
            if ( partnerIndex != -1 )
                movePolygonTo( partnerIndex, partnerNewX );
            m_mousePointX = newDragPointX;
        }

        /**
         * Checks whether we should ignore all mouse events.
         *
         * @return Returns <code>true</code> only if all mouse events should be
         * ignored.
         */
        private boolean ignoreMouseEvent() {
            return !isEnabled() || m_polygons == null;
        }

        /**
         * Kill the scroll timer.
         *
         * @see #startScrollTimer(int)
         */
        private void killScrollTimer() {
            if ( m_scrollTimer != null ) {
                m_scrollTimer.stop();
                m_scrollTimer = null;
            }
        }

        /**
         * Scroll the track once.
         *
         * @param polygonIndex The index of the polygon of the arrow the user
         * clicked on.
         */
        private void scrollOnce( int polygonIndex ) {
            final int tMin = getMinimumTrackValue();
            final int tMax = getMaximumTrackValue();
            int tv = getTrackValue();
            switch ( polygonIndex ) {
                case LOWER_SCROLL_ARROW:
                    tv += SCROLL_TRACK_DELTA;
                    if ( tv > tMax )
                        tv -= tMax;
                    break;
                case UPPER_SCROLL_ARROW:
                    tv -= SCROLL_TRACK_DELTA;
                    if ( tv < tMin )
                        tv += tMax;
                    break;
            }
            setTrackValue( tv );
        }

        /**
         * Start a timer first to wait to see if the user is clicking and
         * holding the mouse down on one of the arrows (during which time,
         * nothing happens) and then to start and continue scrolling until the
         * user releases the mouse.
         *
         * @param polygonIndex The index of the polygon of the arrow the user
         * clicked on.
         * @see #killScrollTimer()
         */
        private void startScrollTimer( final int polygonIndex ) {
            final ActionListener listener = new ActionListener() {
                boolean m_firstTime = true;
                public void actionPerformed( ActionEvent ae ) {
                    if ( m_firstTime )
                        m_firstTime = false;
                    else
                        scrollOnce( polygonIndex );
                }
            };
            m_scrollTimer = new Timer( INTER_SCROLL_DELAY, listener );
            m_scrollTimer.setCoalesce( true );
            m_scrollTimer.setInitialDelay( CLICK_DELAY );
            m_scrollTimer.start();
        }

        /**
         * The recent X coordinate of the mouse.
         */
        private int m_mousePointX;

        /**
         * The index into the <code>m_polygons</code> array of the polygon
         * that the mouse is in or -1 if none.
         */
        private int m_foundPolygonIndex = -1;

        /**
         * A {@link Timer} used for scrolling the track.
         */
        private Timer m_scrollTimer;

        /**
         * The number of milliseconds to wait starting from when the user first
         * clicks to start scrolling (assuming the user keeps holding the mouse
         * down).
         */
        private static final int CLICK_DELAY = 500;

        /**
         * The number of milliseconds to wait between scrolling the track by
         * {@link #SCROLL_TRACK_DELTA}.
         */
        private static final int INTER_SCROLL_DELAY = 50;

        /**
         * The amount by which to change the track value per scroll.
         */
        private static final int SCROLL_TRACK_DELTA = 5;
    }

    /**
     * Creates the polygons.
     */
    private static Polygon[] createPolygons() {
        final Polygon[] p = new Polygon[7];
        //
        // The polygons for the lower feathering value and the upper value use
        // negative coordinates so that the side of the polygon where a zero
        // value would be (the right side) is at X coordinate zero.
        //
        p[ LOWER_FEATHERING_THUMB ] = new Polygon(
            new int[]{ 0,            0, -THUMB_WIDTH / 2 },
            new int[]{ 0, THUMB_HEIGHT,                0 },
            3
        );

        p[ LOWER_THUMB ] = new Polygon(
            new int[]{ 0,            0, THUMB_WIDTH / 2 },
            new int[]{ 0, THUMB_HEIGHT,               0 },
            3
        );

        p[ UPPER_THUMB ] = new Polygon(
            new int[]{ 0,            0, -THUMB_WIDTH / 2 },
            new int[]{ 0, THUMB_HEIGHT,                0 },
            3
        );

        p[ UPPER_FEATHERING_THUMB ] = new Polygon(
            new int[]{ 0,            0, THUMB_WIDTH / 2 },
            new int[]{ 0, THUMB_HEIGHT,               0 },
            3
        );

        p[ LOWER_SCROLL_ARROW ] = new Polygon(
            new int[]{ 0, ARROW_WIDTH, ARROW_WIDTH },
            new int[]{ ARROW_HEIGHT / 2, 0, ARROW_HEIGHT },
            3
        );

        p[ UPPER_SCROLL_ARROW ] = new Polygon(
            new int[]{ 0, -ARROW_WIDTH, -ARROW_WIDTH },
            new int[]{ ARROW_HEIGHT / 2, 0, ARROW_HEIGHT },
            3
        );

        return p;
    }

    /**
     * Find the polygon the given point is within.
     *
     * @param p The {@link Point} to check.
     * @return Returns said {@link Shape} or <code>null</code> if the point is
     * not within any polygon.
     * @see #findPolygonIndex(Point)
     */
    private Shape findPolygon( Point p ) {
        final int i = findPolygonIndex( p );
        return i >= 0 ? m_polygons[i] : null;
    }

    /**
     * Find the index of the polygon the given point is within.
     *
     * @param p The {@link Point} to check.
     * @return Returns an integer &gt; 0 if the point is within a polygon or -1
     * if not.
     * @see #findPolygon(Point)
     */
    private int findPolygonIndex( Point p ) {
        for ( int i = 0; i < m_polygons.length; ++i )
            if ( m_polygons[i].getBounds().contains( p ) ) {
                switch ( i ) {
                    case LOWER_SCROLL_ARROW:
                    case UPPER_SCROLL_ARROW:
                        if ( !m_trackValueWraps )
                            break;
                        // no break;
                    default:
                        return i;
                }
            }
        return -1;
    }

    /**
     * Sends a {@link ChangeEvent}, whose source is this component, to each
     * listener.  This method method is called each time a {@link ChangeEvent}
     * is received from the model.
     *
     * @see #addChangeListener(ChangeListener)
     */
    private void fireStateChanged() {
/*
        if ( m_inDrag ) {
            m_stateChanged = true;
            return;
        }
*/
        final Object[] listeners = listenerList.getListenerList();
        for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
            if ( listeners[i] == ChangeListener.class ) {
                if ( m_changeEvent == null )
                    m_changeEvent = new ChangeEvent( this );
                final ChangeListener listener = (ChangeListener)listeners[i+1];
                listener.stateChanged( m_changeEvent );
            }
        }
        stateChanged();
    }

    /**
     * Gets the maximum X coordinate for a thumb.
     *
     * @return Returns said coordinate.
     * @see #getMinimumThumbX()
     */
    private int getMaximumThumbX() {
        final Insets in = getInsets();
        return getWidth() - in.right - ARROW_WIDTH - THUMB_WIDTH / 2;
    }

    /**
     * Gets the minimum X coordinate for a thumb.
     *
     * @return Returns said coordinate.
     * @see #getMaximumThumbX()
     */
    private int getMinimumThumbX() {
        final Insets in = getInsets();
        return in.left + ARROW_WIDTH + THUMB_WIDTH / 2;
    }

    /**
     * Gets the X scaling factor for the thumbs.
     *
     * @return Returns said factor.
     */
    private double getThumbXScale() {
        final double modelWidth =
            m_model.getMaximumThumbValue() - m_model.getMinimumThumbValue();
        final Insets in = getInsets();
        return  (getWidth() - in.left - in.right
                - ARROW_WIDTH * 2 - THUMB_WIDTH) / modelWidth;
    }

    /**
     * Initialize the images for the thumbs.
     */
    private static void initImages() {
        final Polygon triangle = new Polygon(
            new int[]{ 0, THUMB_WIDTH, THUMB_WIDTH / 2 },
            new int[]{ 0,           0,    THUMB_HEIGHT },
            3
        );
        final BufferedImage buf = new BufferedImage(
            THUMB_WIDTH, THUMB_HEIGHT + SHADOW_FUDGE_Y,
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = (Graphics2D)buf.getGraphics();
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        final GradientPaint p = new GradientPaint(
            0, 0, THUMB_LIGHT_COLOR, THUMB_WIDTH / 2, 0, THUMB_DARK_COLOR
        );
        g2d.setPaint( p );
        g2d.fill( triangle );
        g2d.dispose();

        final Raster[] raster = new Raster[2];
        raster[0] = buf.getData(
            new Rectangle( 0, 0, THUMB_WIDTH / 2, THUMB_HEIGHT )
        );
        raster[1] = buf.getData(
            new Rectangle( THUMB_WIDTH / 2, 0, THUMB_WIDTH / 2, THUMB_HEIGHT )
        );
        raster[1] = raster[1].createTranslatedChild( 0, 0 );

        final ShadowFactory sf =
            new ShadowFactory( 4, 0.6F, THUMB_SHADOW_COLOR );
        sf.setRenderingHint(
            ShadowFactory.KEY_BLUR_QUALITY,
            ShadowFactory.VALUE_BLUR_QUALITY_HIGH
        );

        for ( int i = 0; i < 2; ++i ) {
            m_images[i] = new BufferedImage(
                buf.getColorModel(), (WritableRaster)raster[i], false, null
            );
            BufferedImage temp = sf.createShadow( m_images[i] );
            temp = temp.getSubimage(
                SHADOW_FUDGE_X,
                SHADOW_FUDGE_Y,
                temp.getWidth() - SHADOW_FUDGE_X,
                temp.getHeight() - SHADOW_FUDGE_Y
            );
            g2d = (Graphics2D)temp.getGraphics();
            g2d.drawRenderedImage( m_images[i], new AffineTransform() );
            m_images[i] = temp;
            g2d.dispose();
        }
    }

    /**
     * Map a thumb value to an X coordinate.
     *
     * @param value The value to map.
     * @return Returns the X coordinate derived from the slider value.
     * @see #mapXToThumbValue(int)
     */
    private int mapThumbValueToX( int value ) {
        final int minV = getMinimumThumbValue();
        return  getMinimumThumbX() +
                (int)((value - minV) * getThumbXScale() + 0.5);
    }

    /**
     * Map an X coordinate to a thumb value.
     *
     * @param x The X coordinate to map.
     * @return Returns the value derived from the X coordinate.
     * @see #mapThumbValueToX(int)
     */
    private int mapXToThumbValue( int x ) {
        final int minV = getMinimumThumbValue();
        final int maxV = getMaximumThumbValue();
        final int minX = getMinimumThumbX();
        final int maxX = getMaximumThumbX();

        if ( x < minX )
            return minV - 1;
        if ( x > maxX )
            return maxV + 1;
        return minV + (int)((x - minX) / getThumbXScale() + 0.5);
    }

    /**
     * Move a {@link Polygon} at a given index to an X coordinate.
     *
     * @param polygonIndex The index of the polygon to move.
     * @param x The X coordinate to move the polygon to.
     * @see #movePolygonTo(Polygon,int)
     */
    private void movePolygonTo( int polygonIndex, int x ) {
        movePolygonTo( m_polygons[ polygonIndex ], x );
    }

    /**
     * Move a {@link Polygon} to an X coordinate.  The polygon's first vertex
     * is the one moved to the specified coordinate; the remaining vertices are
     * moved the same relative amount.
     *
     * @param p The {@link Polygon} to move.
     * @param x The X coordinate to move the polygon to.
     */
    private static void movePolygonTo( Polygon p, int x ) {
        p.translate( x - p.xpoints[0], 0 );
    }

    /**
     * Creates and positions the polygons based on the values in the model.
     */
    private void positionPolygons() {
        m_polygons = null;
        if ( getWidth() == 0 )
            return;

        final int ltfv = m_model.getLowerThumbFeatheringValue();
        final int ltv = m_model.getLowerThumbValue();
        final int utv = m_model.getUpperThumbValue();
        final int utfv = m_model.getUpperThumbFeatheringValue();

        final int ltfvx = mapThumbValueToX( ltfv );
        final int ltvx = mapThumbValueToX( ltv );
        final int utvx = mapThumbValueToX( utv );
        final int utfvx = mapThumbValueToX( utfv );
        final int y = getHeight() / 2 - THUMB_HEIGHT / 2;

        m_polygons = createPolygons();
        m_polygons[ LOWER_FEATHERING_THUMB ].translate( ltfvx, y );
        m_polygons[ LOWER_THUMB ].translate( ltvx, y );
        m_polygons[ UPPER_THUMB ].translate( utvx, y );
        m_polygons[ UPPER_FEATHERING_THUMB ].translate( utfvx, y );

        final Insets in = getInsets();
        final int minX = getMinimumThumbX();
        final int maxX = getMaximumThumbX();
        final int minY = in.top;
        final int maxY = minY + THUMB_HEIGHT - 1;

        m_polygons[ TRACK ] = new Polygon(
            new int[]{ minX, maxX, maxX, minX },
            new int[]{ minY, minY, maxY, maxY },
            4
        );

        m_polygons[ LOWER_SCROLL_ARROW ].translate( in.left, y );
        m_polygons[ UPPER_SCROLL_ARROW ].translate( getWidth() - in.right, y );
    }

    /**
     * The state changed: reinitialize stuff.
     */
    private void stateChanged() {
        positionPolygons();
        repaint();
    }

    /**
     * One of the {@link #m_polygons} the mouse is hovering over or
     * <code>null</code> if none.
     */
    private Shape m_hoverPolygon;

    /**
     * TODO
     */
    private boolean m_inDrag;
    private boolean m_stateChanged;

    /**
     * The {@link RangeSelectorModel} in use.
     */
    private RangeSelectorModel m_model;

    /**
     * These polygons are used for the various parts of the component that the
     * user can manipulate (click, drag, etc.).
     */
    private Polygon[] m_polygons;

    /**
     * The {@link Track} in use or <code>null</code> if none.
     */
    private Track m_track;

    /**
     * If <code>true</code>, then the track value wraps around when dragged
     * past the ends.
     */
    private boolean m_trackValueWraps;

    private static final BufferedImage[] m_images = new BufferedImage[2];

    private static final Color ARROW_COLOR = Color.LIGHT_GRAY;

    /**
     * The height of an arrow.
     */
    private static final int ARROW_HEIGHT = 9;

    /**
     * The width of an arrow.
     */
    private static final int ARROW_WIDTH = 5;

    /**
     * The color used to draw the highlight of the polygon the mouse is
     * hovering over.
     */
    private static final Color HOVER_COLOR = new Color(
        Color.LIGHT_GRAY.getRed(),
        Color.LIGHT_GRAY.getGreen(),
        Color.LIGHT_GRAY.getBlue(),
        128
    );

    private static final int SHADOW_FUDGE_X = 3;
    private static final int SHADOW_FUDGE_Y = 2;

    private static final Color THUMB_DARK_COLOR = new Color( 110, 110, 110 );
    private static final Color THUMB_LIGHT_COLOR = new Color( 185, 185, 185 );
    private static final Color THUMB_SHADOW_COLOR = new Color( 25, 25, 25 );

    /**
     * The height of a thumb.
     */
    private static final int THUMB_HEIGHT = 9;

    /**
     * This is the combined width of both halves of a thumb, e.g., the lower
     * feathering thumb and the lower thumb together; therefore, it should
     * always be an even number.
     */
    private static final int THUMB_WIDTH = 14;

    private static final int THUMB_INDEX_START = 0;
    private static final int THUMB_INDEX_END = 3;

    //
    // Indicies into the m_polygons array.
    //
    private static final int LOWER_FEATHERING_THUMB = 0;
    private static final int LOWER_THUMB = 1;
    private static final int UPPER_THUMB = 2;
    private static final int UPPER_FEATHERING_THUMB = 3;
    private static final int TRACK = 4;
    private static final int LOWER_SCROLL_ARROW = 5;
    private static final int UPPER_SCROLL_ARROW = 6;

    static {
        initImages();
    }

    ////////// main() for testing /////////////////////////////////////////////

    public static void main( String[] args ) throws Exception {
        UIManager.setLookAndFeel( Platform.getPlatform().getLookAndFeel() );

/*
        final RepaintManager rm = RepaintManager.currentManager( null );
        rm.setDoubleBufferingEnabled( false );
*/

        final RangeSelector rs =
            new RangeSelector( 0, 100, 0, 0, 100, 100, 0, 100, 0, true );
        rs.setTrack( new RangeSelectorColorGradientTrack() );
        //rs.setTrack( new RangeSelectorZoneTrack() );

        final JPanel customLayout = new JPanel( null ) {
            public Dimension getPreferredSize() {
                return rs.getPreferredSize();
            }

            public void doLayout() {
                final Dimension size = getSize();
                final int midY = size.height / 2;
                final Dimension pSize = rs.getPreferredSize();
                final int pHeight = pSize.height;
                final int y = midY - pHeight / 2;
                rs.setLocation( 0, y );
                rs.setSize( size.width, pHeight );
            }
        };
        customLayout.add( rs );
        customLayout.setBackground( LightZoneSkin.Colors.ToolPanesBackground );

        final JFrame frame = new JFrame( "Test" );
        frame.getContentPane().add( customLayout );
        frame.setSize( 600, 100 );
        frame.setLocationRelativeTo( null );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible( true );
    }
}
/* vim:set et sw=4 ts=4: */
