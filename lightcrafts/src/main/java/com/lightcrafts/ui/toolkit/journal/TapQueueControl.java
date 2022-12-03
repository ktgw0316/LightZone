/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit.journal;

import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.*;

class TapQueueControl implements AWTEventListener {

    // Signal events for the AWTEventListener, to tell about changes
    // in the state of the tap:
    static AWTEvent TapStartEvent = new AWTEvent(TapQueueControl.class, 0) {};
    static AWTEvent TapEndEvent = new AWTEvent(TapQueueControl.class, 1) {};

    private TapEventQueue queue;
    private AWTEventListener listener;

    private int controlKeyCode;
    private boolean isControlOn;

    TapQueueControl(AWTEventListener listener, int keyCode) {
        this.listener = listener;
        queue = new TapEventQueue(this);
        controlKeyCode = keyCode;
        queue.tapStart();
    }

    boolean isTapped() {
        return queue.isTapped();
    }

    void dispose() {
        queue.tapEnd();
    }

    public void eventDispatched(AWTEvent event) {
        updateControlState(event);
        if (isControlOn && ! isControlEvent(event)) {
            listener.eventDispatched(event);
        }
    }

    private void updateControlState(AWTEvent event) {
        if (event instanceof KeyEvent) {
            KeyEvent key = (KeyEvent) event;
            int code = key.getKeyCode();
            if (code == controlKeyCode) {
                if (key.getID() == KeyEvent.KEY_PRESSED) {
                    isControlOn = ! isControlOn;
                    if (isControlOn) {
                        listener.eventDispatched(TapStartEvent);
                    }
                    else {
                        listener.eventDispatched(TapEndEvent);
                    }
                    System.out.println(
                        "*** " +
                        "TapQueueControl " + (isControlOn ? "ON" : "OFF") +
                        " ***"
                    );
                }
            }
        }
    }

    private boolean isControlEvent(AWTEvent event) {
        return (event instanceof KeyEvent) &&
               ((KeyEvent) event).getKeyCode() == controlKeyCode;
    }
}
