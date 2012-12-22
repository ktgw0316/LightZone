/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.CropBounds;
import com.lightcrafts.model.Engine;
import com.lightcrafts.ui.crop.CropListener;
import com.lightcrafts.ui.crop.CropMode;
import com.lightcrafts.ui.rotate.RotorListener;
import com.lightcrafts.ui.rotate.RotorMode;
import com.lightcrafts.ui.rotate.RotorControl;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import static com.lightcrafts.ui.editor.Locale.LOCALE;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.*;

// Logic for crop and rotate: undo/redo, save/restore, and Engine interaction.

class CropRotateManager implements RotorListener {

    private CropMode rotorMode;     // A CropMode with its rotate-only flag set
    private CropMode cropMode;

    private RotorMode hiddenRotorMode;  // Legacy class, for 90-degree actions

    private CropBounds bounds;
    private boolean hasInvertedAspect;  // At rotate-reset, maybe invert bounds

    private Engine engine;
    private XFormModel xform;

    private UndoableEditSupport undo;

    public void setEditor( Editor editor ) {
        hiddenRotorMode.setEditor( editor );
    }

    private class LocalCropListener implements CropListener {

        LocalCropListener( String s ) {
            m_opName = s;
        }

        public void cropCommitted(CropBounds crop) {
            CropBounds newBounds = (crop != null) ? crop : new CropBounds();
            // Don't allow rotations past 45 degrees through the crop mode.
            newBounds = limitRotation(newBounds);
            engine.setCropBounds(newBounds);
            double angle = newBounds.getAngle();
            // Note this sign change:
            hiddenRotorMode.setAngle(- angle);
            // The rotate package applies the convention that positive angles
            // are positive in screen coordinates, meaning clockwise on the
            // screen, which is the opposite of the convention in CropBounds.
            xform.update();
            postEdit(newBounds, LOCALE.get( m_opName + "EditName"), true );
        }

        public void unCrop() {
            CropBounds newBounds = getUncroppedBounds();
            engine.setCropBounds(newBounds);
            xform.update();
            // This method gets called when crop and rotate modes are entered,
            // to reveal the whole image.  We used to post an insignificant
            // undoable edit at this time, but it's not necessary for undo
            // and redo to work, and it has the bad side effect of leaving the
            // image uncropped when a crop is undone.
//            postEdit(newBounds, LOCALE.get("Un" + m_opName + "EditName"), false );
        }

        private final String m_opName;
    }

    CropRotateManager(Engine engine, XFormModel xform) {
        this.engine = engine;
        this.xform = xform;
        rotorMode = new CropMode(true);
        cropMode = new CropMode(false);
        hiddenRotorMode = new RotorMode();
        bounds = new CropBounds();
        undo = new UndoableEditSupport();
        cropMode.addCropListener( new LocalCropListener( "Crop" ) );
        rotorMode.addCropListener( new LocalCropListener( "Rotate" ) );
        hiddenRotorMode.addRotorListener(this);
    }

    CropMode getRotorMode() {
        return rotorMode;
    }

    CropMode getCropMode() {
        return cropMode;
    }

    Action getLeftAction() {
        RotorControl control = hiddenRotorMode.getControl();
        return control.getLeftAction();
    }

    Action getRightAction() {
        RotorControl control = hiddenRotorMode.getControl();
        return control.getRightAction();
    }

    void addUndoableEditListener(UndoableEditListener listener) {
        undo.addUndoableEditListener(listener);
    }

    void removeUndoableEditListener(UndoableEditListener listener) {
        undo.removeUndoableEditListener(listener);
    }

    public void angleChanged(
        double angle, boolean isChanging, boolean isNinetyDegrees
    ) {
        // Note this sign change:
        angle = - angle;
        // The rotate package applies the convention that positive angles
        // are positive in screen coordinates, meaning clockwise on the
        // screen, which is the opposite of the convention in CropBounds.

        if (! isChanging) {
            CropBounds newBounds = getNewBounds(angle, isNinetyDegrees);
            engine.setCropBounds(newBounds);
            if (! newBounds.isAngleOnly()) {
                cropMode.setCrop(newBounds);
            }
            xform.update();
            postEdit(newBounds, LOCALE.get("RotateEditName"), true );
        }
    }

    public void angleReset() {
        angleChanged(0, false, hasInvertedAspect);
    }

    private CropBounds getNewBounds(double angle, boolean invertAspect) {
        if (bounds.isAngleOnly()) {
            return new CropBounds(angle);
        }
        else if (invertAspect) {
            CropBounds inverted = bounds.createInvertedAspect();
            hasInvertedAspect = ! hasInvertedAspect;
            return new CropBounds(inverted, angle);
        }
        else {
            return new CropBounds(bounds, angle);
        }
    }

    private void postEdit( final CropBounds newBounds, final String name,
                           final boolean significant ) {
        final CropBounds oldBounds = bounds;
        bounds = newBounds;
        UndoableEdit edit = new AbstractUndoableEdit() {
            public String getPresentationName() {
                return name;
            }
            public void undo() {
                super.undo();
                bounds = oldBounds;
                engine.setCropBounds(bounds);
                xform.update();
                // Note this sign change:
                hiddenRotorMode.setAngle(- bounds.getAngle());
                // The rotate package applies the convention that positive angles
                // are positive in screen coordinates, meaning clockwise on the
                // screen, which is the opposite of the convention in CropBounds.
                cropMode.setCrop(bounds);
            }
            public void redo() {
                super.redo();
                bounds = newBounds;
                engine.setCropBounds(bounds);
                xform.update();
                // Note this sign change:
                hiddenRotorMode.setAngle(- bounds.getAngle());
                // The rotate package applies the convention that positive angles
                // are positive in screen coordinates, meaning clockwise on the
                // screen, which is the opposite of the convention in CropBounds.
                cropMode.setCrop(bounds);
            }
            public boolean isSignificant() {
                return significant;
            }
        };
        undo.postEdit(edit);
    }

    // Compare the angle in the given CropBounds with the current angle in
    // our member bounds, and if the angle has changed by more than
    // 45 degrees, then reinterpret the crop as a different crop with a
    // closer angle and maybe transposed.
    private CropBounds limitRotation(CropBounds crop) {
        double oldAngle = (bounds != null) ? bounds.getAngle() : 0;
        double newAngle = (crop != null) ? crop.getAngle() : 0;
        while (newAngle - oldAngle > Math.PI / 4) {
            Point2D center = crop.getCenter();
            double width = crop.getWidth();
            double height = crop.getHeight();
            double angle = crop.getAngle();
            crop = new CropBounds(center, height, width, angle - Math.PI / 2);
            newAngle = crop.getAngle();
        }
        while (newAngle - oldAngle < - Math.PI / 4) {
            Point2D center = crop.getCenter();
            double width = crop.getWidth();
            double height = crop.getHeight();
            double angle = crop.getAngle();
            crop = new CropBounds(center, height, width, angle + Math.PI / 2);
            newAngle = crop.getAngle();
        }
        return crop;
    }

    // When crop mode starts, or when the user asks to reset the current crop,
    // use these CropBounds.  They are angle-only bounds, set to the nearest
    // multiple of 90 degrees.
    private CropBounds getUncroppedBounds() {
        if (bounds == null) {
            return new CropBounds();
        }
        double angle = bounds.getAngle();
        angle = (Math.PI / 2) * Math.round(angle / (Math.PI / 2));
        return new CropBounds(angle);
    }

    private final static String AngleTag = "Angle";

    private final static String CropTag = "Crop";

    private final static String ULtag = "UpperLeft";
    private final static String URtag = "UpperRight";
    private final static String LLtag = "LowerLeft";
    private final static String LRtag = "LowerRight";

    private final static String Xtag = "X";
    private final static String Ytag = "Y";

    public void save(XmlNode node) {
        node = node.addChild(CropTag);

        if (bounds.isAngleOnly()) {
            double angle = bounds.getAngle();
            node.setAttribute(AngleTag, Double.toString(angle));
        }
        else {
            XmlNode ulNode = node.addChild(ULtag);
            Point2D ul = bounds.getUpperLeft();
            ulNode.setAttribute(Xtag, Double.toString(ul.getX()));
            ulNode.setAttribute(Ytag, Double.toString(ul.getY()));

            XmlNode urNode = node.addChild(URtag);
            Point2D ur = bounds.getUpperRight();
            urNode.setAttribute(Xtag, Double.toString(ur.getX()));
            urNode.setAttribute(Ytag, Double.toString(ur.getY()));

            XmlNode llNode = node.addChild(LLtag);
            Point2D ll = bounds.getLowerLeft();
            llNode.setAttribute(Xtag, Double.toString(ll.getX()));
            llNode.setAttribute(Ytag, Double.toString(ll.getY()));

            XmlNode lrNode = node.addChild(LRtag);
            Point2D lr = bounds.getLowerRight();
            lrNode.setAttribute(Xtag, Double.toString(lr.getX()));
            lrNode.setAttribute(Ytag, Double.toString(lr.getY()));
        }
    }

    public void restore(XmlNode node) throws XMLException {
        try {
            node = node.getChild(CropTag);

            double x, y;
            Point2D.Double ul, ur, ll, lr;

            try {
                XmlNode ulNode = node.getChild(ULtag);
                x = Double.parseDouble(ulNode.getAttribute(Xtag));
                y = Double.parseDouble(ulNode.getAttribute(Ytag));
                ul = new Point2D.Double(x, y);

                XmlNode urNode = node.getChild(URtag);
                x = Double.parseDouble(urNode.getAttribute(Xtag));
                y = Double.parseDouble(urNode.getAttribute(Ytag));
                ur = new Point2D.Double(x, y);

                XmlNode llNode = node.getChild(LLtag);
                x = Double.parseDouble(llNode.getAttribute(Xtag));
                y = Double.parseDouble(llNode.getAttribute(Ytag));
                ll = new Point2D.Double(x, y);

                XmlNode lrNode = node.getChild(LRtag);
                x = Double.parseDouble(lrNode.getAttribute(Xtag));
                y = Double.parseDouble(lrNode.getAttribute(Ytag));
                lr = new Point2D.Double(x, y);
            }
            catch (NumberFormatException e) {
                throw new XMLException("Invalid crop coordinates", e);
            }
            bounds = new CropBounds(ul, ur, ll, lr);
        }
        catch (XMLException e1) {
            try {
                double angle = Double.parseDouble(node.getAttribute(AngleTag));

                Dimension size = engine.getNaturalSize();
                Rectangle2D imageBounds = new Rectangle2D.Double(0, 0, size.width, size.height);

                bounds = new CropBounds(imageBounds, angle);
                // bounds = new CropBounds(angle);
            }
            catch (XMLException e2) {
                throw new XMLException(
                    "No valid crop or rotate data: " +
                    e1.getMessage() + ", " + e2.getMessage()
                );
            }
        }

        engine.setCropBounds(bounds);
        cropMode.setCrop(bounds);
        // Note this sign change:
        hiddenRotorMode.setAngle(- bounds.getAngle());
        // The rotate package applies the convention that positive angles
        // are positive in screen coordinates, meaning clockwise on the
        // screen, which is the opposite of the convention in CropBounds.
        xform.update();
    }

    void dispose() {
        if (rotorMode != null)
            rotorMode.dispose();
        if (cropMode!= null)
            cropMode.dispose();
        if (hiddenRotorMode != null)
            hiddenRotorMode.dispose();
    }
}
