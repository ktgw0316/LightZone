/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.model.Scale;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.editor.ScaleListener;
import com.lightcrafts.ui.editor.ScaleModel;

import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;

final class ZoomOutMenuItem extends DocumentMenuItem implements ScaleListener {

    WeakReference<Document> docRef = new WeakReference<Document>(null);

    ZoomOutMenuItem(ComboFrame frame) {
        super(frame, "ZoomOut");
        setEnabled(false);
    }

    void update() {
        Document newDoc = getDocument();
        Document oldDoc = docRef.get();
        if (newDoc != oldDoc) {
            if (oldDoc != null) {
                ScaleModel model = oldDoc.getScaleModel();
                model.removeScaleListener(this);
            }
            if (newDoc != null) {
                ScaleModel model = newDoc.getScaleModel();
                model.addScaleListener(this);
                setEnabled(model.canScaleDown());
            }
            else {
                setEnabled(false);
            }
            docRef = new WeakReference<Document>(newDoc);
        }
    }

    public void actionPerformed(ActionEvent event) {
        Document doc = getDocument();
        ScaleModel model = doc.getScaleModel();
        model.scaleDown();
    }

    public void scaleChanged(Scale scale) {
        Document doc = getDocument();
        ScaleModel model = doc.getScaleModel();
        setEnabled(model.canScaleDown());
    }
}
