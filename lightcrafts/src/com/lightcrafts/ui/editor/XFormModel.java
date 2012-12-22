/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.Engine;

import java.awt.geom.AffineTransform;
import java.util.LinkedList;

/**
 * This class just broadcasts AffineTransform changes from an Engine.  The
 * Modes use this to handle overlays correctly.
 */

class XFormModel {

    private Engine engine;
    private LinkedList<XFormListener> listeners;

    XFormModel(Engine engine) {
        this.engine = engine;
        listeners = new LinkedList<XFormListener>();
    }

    void addXFormListener(XFormListener listener) {
        listeners.add(listener);
    }

    void removeXFormListener(XFormListener listener) {
        listeners.remove(listener);
    }

    void update() {
        notifyListeners(engine.getTransform());
    }

    private void notifyListeners(AffineTransform xform) {
        for (XFormListener listener : listeners) {
            listener.xFormChanged(xform);
        }
    }
}
