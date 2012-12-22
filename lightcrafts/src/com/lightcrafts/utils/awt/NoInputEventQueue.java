/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.awt;

import java.awt.*;
import java.awt.event.InputEvent;

/**
 * A <code>NoInputEventQueue</code> is-a {@link PoppableEventQueue} that
 * discards all {@link InputEvent}s.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class NoInputEventQueue extends PoppableEventQueue {

    public static final NoInputEventQueue INSTANCE = new NoInputEventQueue();

    protected void dispatchEvent( AWTEvent event ) {
        if ( !(event instanceof InputEvent) ) {
            try {
                super.dispatchEvent( event );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
/* vim:set et sw=4 ts=4: */
