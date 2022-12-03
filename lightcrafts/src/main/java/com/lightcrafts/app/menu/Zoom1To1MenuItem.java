/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.model.Scale;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.editor.ScaleListener;
import com.lightcrafts.ui.editor.ScaleModel;

import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;

final class Zoom1To1MenuItem extends DocumentMenuItem implements ScaleListener {

    final static Scale Unity = new Scale(1, 1);

    WeakReference<Document> docRef = new WeakReference<Document>(null);

    Zoom1To1MenuItem(ComboFrame frame) {
        super(frame, "Zoom11");
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
                setEnabled(model.canScaleUp());
            }
            else {
                setEnabled(false);
            }
            docRef = new WeakReference<Document>(newDoc);
        }
    }

    public void actionPerformed(ActionEvent event) {
        performPreAction( event );
        Document doc = getDocument();
        ScaleModel model = doc.getScaleModel();
        model.setScale(Unity);
    }

    public void scaleChanged(Scale scale) {
        boolean isUnity = scale.equals(Unity);
        setEnabled(! isUnity);
    }
}
