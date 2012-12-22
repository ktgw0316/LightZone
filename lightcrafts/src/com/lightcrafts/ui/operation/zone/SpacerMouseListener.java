/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import com.lightcrafts.model.ZoneOperation;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Listen for focus changes on an array of Spacers, moderate mouse events
// on all of them, and tell the ZoneOperation where the focus lies.
// For ZoneOperation.setControlFocus().

class SpacerMouseListener extends MouseAdapter {
    
    private ZoneOperation op;
    private Spacer[] spacers;
    private Spacer focused;
    private boolean inSpacer;
    private boolean mousePressed;

    SpacerMouseListener(ZoneOperation op, Spacer[] spacers) {
        this.op = op;
        this.spacers = spacers;
        for (Spacer spacer : spacers) {
            spacer.addMouseListener(this);
        }
    }

    public void mouseEntered(MouseEvent event) {
        inSpacer = true;
        Spacer spacer = (Spacer) event.getComponent();
        if ((spacer != focused) && (! mousePressed)) {
            focused = spacer;
            int index = getIndexOf(focused);
            op.setFocusPoint(index);
        }
    }

    public void mouseExited(MouseEvent event) {
        inSpacer = false;
        setNoFocus();
    }

    public void mousePressed(MouseEvent event) {
        mousePressed = true;
        setNoFocus();
    }

    public void mouseReleased(MouseEvent event) {
        mousePressed = false;
        if (inSpacer) {
            Spacer spacer = (Spacer) event.getComponent();
            setFocus(spacer);
        }
    }

    private void setFocus(Spacer spacer) {
        if (focused != spacer) {
            focused = spacer;
            int index = getIndexOf(focused);
            op.setFocusPoint(index);
        }
    }

    private void setNoFocus() {
        if (focused != null) {
            focused = null;
            op.setFocusPoint(-1);
        }
    }

    private int getIndexOf(Spacer spacer) {
        for (int n=0; n<spacers.length; n++) {
            if (spacers[n] == spacer) {
                return n;
            }
        }
        return -1;
    }
}
