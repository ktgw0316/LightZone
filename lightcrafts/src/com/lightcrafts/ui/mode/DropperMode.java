/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.mode;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.MouseInputListener;

import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.utils.WeakHashSet;

/**
 * A Mode that draws nothing but handles mouse events, designed to go with
 * tools that sample from an image.
 */

public class DropperMode extends AbstractMode {

    /**
     * An interface for listeners that want to hear when the user selects
     * a point by a mouse press.
     */
    public static interface Listener {
        /**
         * The mouse has been pressed somewhere.
         *
         * @param p The location where the mouse was pressed, in image
         * coordinates.
         */
        void pointSelected( Point2D p );

        /**
         * The Mode was ended (as determined by removeMouseInputListener())
         * before a point was selected.
         */
        void modeCancelled();
    }

    public DropperMode( OpControl opControl ) {
        m_opControl = opControl;
        m_dropperModes.add( this );
        m_overlay = new DropperOverlay();
        m_overlay.setCursor( DropperCursor );
        m_overlay.addMouseListener(
            new MouseAdapter() {
                public void mousePressed( MouseEvent me ) {
                    notifyPointSelected( me.getPoint() );
                }
            }
        );
        m_listeners = new LinkedList<Listener>();
    }

    public void addListener( Listener listener ) {
        m_listeners.add( listener );
    }

    public void enter() {
        for ( DropperMode dm : m_dropperModes )
            if ( dm != this && dm.m_opControl == m_opControl && dm.isIn() )
                m_opControl.notifyListenersExitMode( dm );
        super.enter();
    }

    public JComponent getOverlay() {
        return m_overlay;
    }

    public void removeListener( Listener listener ) {
        m_listeners.remove( listener );
    }

    public void addMouseInputListener( MouseInputListener listener ) {
        m_overlay.addMouseListener( listener );
        m_overlay.addMouseMotionListener( listener );
    }

    public void removeMouseInputListener( MouseInputListener listener ) {
        m_overlay.removeMouseListener( listener );
        m_overlay.removeMouseMotionListener( listener );
        notifyModeCancelled();
    }

    public void setTransform( AffineTransform xform ) {
        m_xform = xform;
    }

    public boolean wantsAutocroll() {
        return true;
    }

    ////////// private ////////////////////////////////////////////////////////

    // This Mode never displays anything.  It just sets the cursor and
    // handles mouse events.
    private static final class DropperOverlay extends JComponent {
        protected void paintComponent( Graphics g ) {
            // do nothing
        }
    }

    private Point2D invertPoint( Point2D p ) {
        if ( m_xform == null )
            return (Point2D)p.clone();
        try {
            return m_xform.inverseTransform( p, null );
        }
        catch ( NoninvertibleTransformException e ) {
            throw new RuntimeException( "Scaling failed", e );
        }
    }

    private void notifyModeCancelled() {
        for ( Listener listener : m_listeners )
            listener.modeCancelled();
    }

    private void notifyPointSelected( Point2D p ) {
        p = invertPoint( p );
        for ( Listener listener : m_listeners )
            listener.pointSelected( p );
    }

    private final List<Listener> m_listeners;
    private final JComponent m_overlay;
    private AffineTransform m_xform;

    private final OpControl m_opControl;

    private final static Set<DropperMode> m_dropperModes =
        new WeakHashSet<DropperMode>();

    private static final Cursor DropperCursor;
    private static final Point DropperHotPoint = new Point( 3, 19 );

    static {
        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final String path = "resources/dropper_cursor.png";
        final URL url = DropperMode.class.getResource( path );
        final Image image = toolkit.createImage( url );
        DropperCursor = toolkit.createCustomCursor(
            image, DropperHotPoint, "Dropper"
        );
    }
}
/* vim:set et sw=4 ts=4: */
