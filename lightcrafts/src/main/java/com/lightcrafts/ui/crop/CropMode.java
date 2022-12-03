/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import com.lightcrafts.model.CropBounds;
import com.lightcrafts.ui.mode.AbstractMode;
import com.lightcrafts.ui.crop.CropOverlay;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Dimension2D;
import java.util.LinkedList;

public class CropMode extends AbstractMode {

    private CropOverlay overlay;
    private AffineTransform xform;
    private CropControl control;
    private LinkedList<CropListener> listeners;

    public CropMode(boolean isRotateOnly) {
        listeners = new LinkedList<CropListener>();

        ResetAction reset = new ResetAction();

        control = new CropControl(reset,isRotateOnly);
        overlay = new CropOverlay(isRotateOnly);

        // The control imposes aspect ratio constraints on the overlay:
        control.setOverlay(overlay);

        xform = new AffineTransform();

        overlay.addMouseListener(new CropPopupMenu(this, reset));

        // Let Enter commit the crop and ESC cancel it.
        overlay.registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doCrop();
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        overlay.registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    CropBounds resetBounds = getResetCropBounds();
                    setCropWithConstraints(resetBounds);
                    doCrop();
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        overlay.registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    overlay.changeOrientation();
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

    }

    public void enter() {
        super.enter();
        overlay.modeEntered();
        control.unlock();
    }

    public void setResetCropBounds(CropBounds crop) {
        if (crop != null) {
            crop = CropBounds.transform(xform, crop);
        }
        control.setResetValue(crop);
    }

    public CropBounds getResetCropBounds() {
        CropBounds crop = control.getResetValue();
        if (crop != null) {
            try {
                final AffineTransform inverse = xform.createInverse();
                crop = CropBounds.transform(inverse, crop);
            }
            catch (NoninvertibleTransformException e) {
                crop = null;
            }
        }
        return crop;
    }

    Dimension2D getRotateLimitDimensions() {
        Dimension2D limit = overlay.getRotateLimitDimensions();
        if (limit != null) {
            double scale = xform.getScaleX();
            limit.setSize(
                limit.getWidth() / scale, limit.getHeight() / scale
            );
        }
        return limit;
    }

    void setRotateLimitDimensions(Dimension2D limit) {
        if (limit != null) {
            double scale = xform.getScaleX();
            limit.setSize(limit.getWidth() * scale, limit.getHeight() * scale);
            overlay.setRotateLimitDimensions(limit);
        }
    }

    public void addCropListener(CropListener listener) {
        listeners.add(listener);
    }

    public void removeCropListener(CropListener listener) {
        listeners.remove(listener);
    }

    public CropControl getControl() {
        return control;
    }

    /**
     * Get the current crop, in image coordinates.
     */
    public CropBounds getCrop() {
        final CropBounds rect = overlay.getCrop();
        if (rect != null) {
            try {
                final AffineTransform inverse = xform.createInverse();
                return CropBounds.transform(inverse, rect);
            }
            catch (NoninvertibleTransformException e) {
                // An uninvertible transform would create bigger problems
                // than this.
                e.printStackTrace();
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Set the current CropBounds, in image coordinates.
     * This is intended for undo/redo.
     */
    public void setCrop(CropBounds crop) {
        if (crop != null) {
            crop = CropBounds.transform(xform, crop);
        }
        overlay.setCrop(crop);
    }

    public void setCropWithConstraints(CropBounds crop) {
        if (crop != null) {
            crop = CropBounds.transform(xform, crop);
        }
        overlay.setCropWithConstraints(crop);
    }

    public void resetCrop() {
        for (CropListener listener : listeners) {
            listener.unCrop();
        }
    }

    public void doCrop() {
        if (isIn()) {
            final CropBounds crop = getCrop();
            for (CropListener listener : listeners) {
                listener.cropCommitted(crop);
            }
        }
    }

    public JComponent getOverlay() {
        return overlay;
    }

    public void addMouseInputListener(MouseInputListener listener) {
        overlay.addMouseListener(listener);
        overlay.addMouseMotionListener(listener);
    }

    public void removeMouseInputListener(MouseInputListener listener) {
        overlay.removeMouseListener(listener);
        overlay.removeMouseMotionListener(listener);
    }

    public void setTransform(AffineTransform xform) {
        // Reinitialize the crop, so it scales with the image and follows
        // rotations:
        final CropBounds crop = getCrop();
        final CropBounds reset = getResetCropBounds();
        Dimension2D rotateLimitDimensions = getRotateLimitDimensions();
        this.xform = xform;
        setCrop(crop);
        setResetCropBounds(reset);
        setRotateLimitDimensions(rotateLimitDimensions);
    }

    public void setUnderlayBounds(Rectangle bounds) {
        overlay.setUnderlayRect(bounds);
    }

    public boolean wantsAutocroll() {
        return true;
    }

    public void dispose() {
        overlay.dispose();
    }
}
