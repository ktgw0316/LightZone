/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit.journal;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;

class TapEventQueue extends EventQueue {

    private AWTEventListener listener;
    private boolean isPushed;

    TapEventQueue(AWTEventListener listener) {
        this.listener = listener;
    }

    void tapStart() {
        if (! isPushed) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            EventQueue queue = toolkit.getSystemEventQueue();
            queue.push(this);
            isPushed = true;
        }
    }

    boolean isTapped() {
        return isPushed;
    }

    void tapEnd() {
        if (isPushed) {
            pop();
            isPushed = false;
        }
    }

    protected void dispatchEvent(AWTEvent event) {
        if (event instanceof InputEvent) {
            listener.eventDispatched(event);
        }
        super.dispatchEvent(event);
    }
}
