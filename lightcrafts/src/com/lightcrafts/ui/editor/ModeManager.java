/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.CropBounds;
import com.lightcrafts.model.Operation;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.crop.CropListener;
import com.lightcrafts.ui.crop.CropMode;
import com.lightcrafts.ui.mode.AbstractMode;
import com.lightcrafts.ui.mode.Mode;
import com.lightcrafts.ui.mode.ModeOverlay;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.ui.operation.OpControlModeListener;
import com.lightcrafts.ui.operation.OpStackListener;
import com.lightcrafts.ui.operation.SelectableControl;
import com.lightcrafts.ui.region.RegionOverlay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;

/**
 * Handle switching among all the Modes.  Lots of special mode-transition
 * logic lives here, plus the affine transform updates for Modes.
 * <p>
 * ModeManager sets and resets the crop when CropMode starts and ends.
 * The pan mode is entered on a key press and exited on release.  The
 * temporary Modes defined by OpControls are added and removed here too.
 */

public class ModeManager
    implements OpStackListener, OpControlModeListener, XFormListener
{
    // Look for the special key events to enter and exit the pan mode,
    // taking care to filter out auto-repeat events:

    private static int PanKeyCode = Platform.isMac()
            ? KeyEvent.VK_META
            : KeyEvent.VK_CONTROL;

    private KeyEventPostProcessor panModeKeyProcessor =
        new KeyEventPostProcessor() {
            private boolean isPanMode;
            public boolean postProcessKeyEvent(KeyEvent e) {
                final boolean wasPanMode = (overlay.peekMode() == transientPanMode);
                if (e.getKeyCode() == PanKeyCode) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        isPanMode = true;
                    }
                    if (e.getID() == KeyEvent.KEY_RELEASED) {
                        if (Platform.isMac()) {
                            isPanMode = false;
                        }
                        else {
                            // Detect and ignore auto-repeat release events
                            final boolean isReallyPressed =
                                Platform.getPlatform().isKeyPressed(
                                    PanKeyCode
                                );
                            isPanMode = isReallyPressed;
                        }
                    }
                }
                if (isPanMode && ! wasPanMode) {
                    overlay.pushMode(transientPanMode);
                }
                if (wasPanMode && ! isPanMode) {
                    overlay.popMode();
                }
                return false;   // these key events have other interpretations
            }
        };

    private Document doc;

    private ModeOverlay overlay;

    private AffineTransform xform;

    private Mode regionMode;
    private CropMode cropMode;
    private AbstractMode transientPanMode; // works with other modes (space key)
    private AbstractMode permanentPanMode; // present in "no mode" (arrow cursor)
    private CropMode rotateMode;

    // The "extras" component for the region mode
    private JComponent regionExtras;

    // If someone clicks "Commit" in crop mode, we must update the mode buttons
    private ModeButtons modeButtons;

    // OpControls can ask for short-term Mode push/pops:
    private OpControl control;                  // add and remove the listener
    private Mode controlMode;                   // update the AffineTransform

    // Keep track of where the underlay is, so the crop overlay can be
    // initialized the first time it's entered.
    private Rectangle underlayBounds;

    ModeManager(
        Mode regionMode,
        CropMode cropMode,
        AbstractMode transientPanMode,// After spacebar
        AbstractMode permanentPanMode,// Always-on
        CropMode rotateMode,
        ModeOverlay overlay,
        Component underlay,
        JComponent regionExtras,    // The curve-type buttons
        Document doc                // For zoom-to-fit around the crop mode
    ) {
        this.overlay = overlay;
        this.regionExtras = regionExtras;
        this.doc = doc;

        xform = new AffineTransform();

        this.regionMode = regionMode;
        this.cropMode = cropMode;
        this.transientPanMode = transientPanMode;
        this.permanentPanMode = permanentPanMode;
        this.rotateMode = rotateMode;

        // The AffineTransform in the Modes sometimes depends on the size
        // of the ModeOverlay:
        overlay.addComponentListener(
            new ComponentAdapter() {
                public void componentResized(ComponentEvent event) {
                    setTransform(xform);
                }
            }
        );
        // Modes also require notification about the location and size of
        // their underlay:
        underlay.addComponentListener(
            new ComponentAdapter() {
                public void componentResized(ComponentEvent event) {
                    Rectangle bounds = event.getComponent().getBounds();
                    setUnderlayBounds(bounds);
                }
                public void componentMoved(ComponentEvent event) {
                    Rectangle bounds = event.getComponent().getBounds();
                    setUnderlayBounds(bounds);
                }
            }
        );
        // The crop and rotate modes likes to end themselves:
        cropMode.addCropListener(
            new CropListener() {
                public void cropCommitted(CropBounds bounds) {
                    // This may be called because someone already switched
                    // away from the crop mode, but it may also be called
                    // from the "Commit" crop mode popup menu item, in which
                    // case we must handle the mode switch ourselves.
                    EventQueue.invokeLater(
                        new Runnable() {
                            public void run() {
                                if (modeButtons != null) {
                                    if (modeButtons.isCropSelected()) {
                                        setEditorMode( EditorMode.ARROW );
                                    }
                                }
                            }
                        }
                    );
                }
                public void unCrop() {
                }
            }
        );
        rotateMode.addCropListener(
            new CropListener() {
                public void cropCommitted(CropBounds bounds) {
                    // This may be called because someone already switched
                    // away from the rotate mode, but it may also be called
                    // from the "Commit" crop mode popup menu item, in which
                    // case we must handle the mode switch ourselves.
                    EventQueue.invokeLater(
                        new Runnable() {
                            public void run() {
                                if (modeButtons != null) {
                                    if (modeButtons.isRotateSelected()) {
                                        setEditorMode( EditorMode.ARROW );
                                    }
                                }
                            }
                        }
                    );
                }
                public void unCrop() {
                }
            }
        );
        registerPanModeListener();

        setMode(this.permanentPanMode);
    }

    public EditorMode getMode() {
        return m_editorMode;
    }

    void setModeButtons(ModeButtons buttons) {
        modeButtons = buttons;
    }

    // Start OpStackListener:

    public void opAdded(OpControl control) {
    }

    public void opChanged(OpControl control) {
        if (isControlModeActive()) {
            exitMode(null);
        }
        if (this.control != null) {
            this.control.removeModeListener(this);
        } else
            setEditorMode( EditorMode.ARROW );
        this.control = control;

        if (control != null && ! control.isLocked()) {
            control.addModeListener(this);
        }
        // Some OpControls don't get regions.
        final boolean isLocked = (control != null) && control.isLocked();
        final boolean isRaw = (control != null) && control.isRawCorrection();
        if (isLocked || isRaw) {
            modeButtons.setRegionsEnabled(false);
        } else {
            modeButtons.setRegionsEnabled(true);
        }

        if ( control != null ) {
            final Operation op = control.getOperation();
            final EditorMode prefMode = op.getPreferredMode();
            if ( isLocked || isRaw ||
                 prefMode != EditorMode.ARROW ||
                 m_editorMode == EditorMode.ARROW )
                setEditorMode( prefMode );
        }
    }

    public void opChanged(SelectableControl control) {
        if (this.control != null) {
            this.control.removeModeListener(this);
            this.control = null;
        }
        if ( control instanceof ProofSelectableControl )
            setMode( null );
        if ( control == null )
            setMode( null );
    }

    public void opLockChanged(OpControl control) {
        if (this.control == control) {
            opChanged(control);
        }
    }

    public void opRemoved(OpControl control) {
        // Since region mode interacts with the tool stack selection,
        // be sure to pop out of that mode if it's the current mode at
        // the time a tool is deleted.
        if (overlay.peekMode() == regionMode) {
            if (this.control != null) {
                Operation op = this.control.getOperation();
                if (op.getPreferredMode() != EditorMode.REGION) {
                    EventQueue.invokeLater(
                        new Runnable() {
                            public void run() {
                                setEditorMode( EditorMode.ARROW );
                            }
                        }
                    );
                }
            }
        }
    }

    // End OpStackListener.

    public void setEditorMode( EditorMode mode ) {
        if ( mode == m_editorMode )
            return;
        if ( modeButtons != null )
            switch ( mode ) {
                case ARROW:
                    modeButtons.clickNoMode();
                    break;
                case CROP:
                    modeButtons.clickCropButton();
                    break;
                case REGION:
                    modeButtons.clickRegionButton();
                    break;
                case ROTATE:
                    modeButtons.clickRotateButton();
            }
        m_editorMode = mode;
    }

    // Start OpControlModeListener:

    public void enterMode(Mode mode) {
        mode.enter();
        assert (! isControlModeActive());
        controlMode = mode;
        controlMode.setTransform(getOverlayTransform());
        overlay.pushMode(controlMode);
    }

    public void exitMode(Mode mode) {
        assert isControlModeActive();
        if ( mode != null )
            mode.exit();
        overlay.popMode();
        controlMode = null;
    }

    // End OpControlModeListener.

    // Start XFormListener:

    public void xFormChanged(AffineTransform xform) {
        // First, validate from the overlay's validate root, because the
        // overlay bounds partly determine the overlay affine transform.
        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(
            JScrollPane.class, overlay
        );
        if (scroll != null){
            overlay.invalidate();   // mark the overay as needing to update
            scroll.validate();      // update the layout under the scroll pane
        }
        setTransform(xform);
    }

    // End XFormListener.

    private boolean isControlModeActive() {
        return (controlMode != null) && overlay.peekMode().equals(controlMode);
    }

    private boolean isTransientPanModeActive() {
        return overlay.peekMode().equals(transientPanMode);
    }

    private void setTransform(AffineTransform xform) {
        //
        // The given AffineTranform maps original image coordinates to the
        // coordinates of the image component on the screen.  The Modes
        // require an AffineTransform mapping original image coordinates to
        // coordinates of the Mode overlay component.
        //
        // Referring to ModeOverlay, the origin of a Mode's overlay component
        // does not necessarily coincide with the origin of the image
        // component.  If the image is zoomed out to a size smaller than an
        // enclosing scroll pane viewport, then the Mode overlay component
        // will be larger than the image, filling the viewport.
        //
        // So, depending on this viewport condition, we may need to add a
        // translation to the transform:
        //
        this.xform = xform;

        AffineTransform overlayTransform = getOverlayTransform();

        regionMode.setTransform(overlayTransform);
        cropMode.setTransform(overlayTransform);
        transientPanMode.setTransform(overlayTransform);
        rotateMode.setTransform(overlayTransform);
        if (controlMode != null) {
            controlMode.setTransform(overlayTransform);
        }
    }

    // Also used from DocPanel to forward mouse motion to the Previews.
    AffineTransform getOverlayTransform() {
        AffineTransform overlayXform = overlay.getTransform();
        AffineTransform totalXform = (AffineTransform) xform.clone();
        totalXform.preConcatenate(overlayXform);
        return totalXform;
    }

    private void setUnderlayBounds(Rectangle bounds) {
        if (bounds.equals(underlayBounds)) {
            return;
        }
        underlayBounds = bounds;
        regionMode.setUnderlayBounds(bounds);
        cropMode.setUnderlayBounds(bounds);
        transientPanMode.setUnderlayBounds(bounds);
        rotateMode.setUnderlayBounds(bounds);
        if (controlMode != null) {
            controlMode.setUnderlayBounds(bounds);
        }
        // The default crop bounds depend on the underlay bounds.
        CropBounds crop = cropMode.getCrop();
        CropBounds initCrop = getInitialCropBounds();
        if (crop == null) {
            cropMode.setCropWithConstraints(initCrop);
        }
        cropMode.setResetCropBounds(initCrop);
    }

    JComponent setNoMode() {
        boolean wasCrop =
            ((overlay.peekMode() == cropMode) ||
             (overlay.peekMode() == rotateMode));
        setMode(null);
        m_editorMode = EditorMode.ARROW;
        if (wasCrop) {
            boolean isFitMode = doc.popFitMode();
            if (! isFitMode) {
                doc.zoomToFit();
            }
        }
        return null;
    }

    EditorMode m_editorMode;

    JComponent setCropMode() {
        doc.markDirty();
        setMode(cropMode);
        m_editorMode = EditorMode.CROP;
        doc.pushFitMode();
        doc.zoomToFit();
        return cropMode.getControl();
    }

    JComponent setRotateMode() {
        doc.markDirty();
        setMode(rotateMode);
        m_editorMode = EditorMode.ROTATE;
        doc.pushFitMode();
        doc.zoomToFit();
        // The "reset" button in rotate mode depends on the initial crop:
        CropBounds crop = cropMode.getCrop();
        rotateMode.setCrop(crop);
        rotateMode.setResetCropBounds(crop);
        return rotateMode.getControl().getResetButton();
    }

    JComponent setRegionMode() {
        boolean wasCrop =
            ((overlay.peekMode() == cropMode) ||
             (overlay.peekMode() == rotateMode));
        setMode(regionMode);
        m_editorMode = EditorMode.REGION;
        if (wasCrop) {
            boolean isFitMode = doc.popFitMode();
            if (! isFitMode) {
                doc.zoomToFit();
            }
        }
        return regionExtras;
    }

    private void setMode(Mode newMode) {
        // Peel off any temporary Modes we may have pushed.
        while (isControlModeActive() || isTransientPanModeActive()) {
            overlay.popMode();
            controlMode = null;
        }
        // See if that did the trick, to avoid duplicate pushes.
        Mode oldMode = overlay.peekMode();
        if (oldMode == newMode) {
            return;
        }
        // If the current Mode is one of ours, pop it before pushing the next:
        if (oldMode == regionMode ||
            oldMode == cropMode ||
            oldMode == rotateMode) {
            // If we are in region mode make sure we exit edit mode
            if (oldMode == regionMode && regionMode instanceof RegionOverlay)
                ((RegionOverlay) regionMode).finishEditingCurve();

            overlay.popMode();

            // The CropModes need setup and teardown:
            if ((oldMode == cropMode) || (oldMode == rotateMode)) {
                CropMode crop = (CropMode) oldMode;
                crop.doCrop();
            }
            oldMode.exit();
        }
        if (newMode != null) {
            overlay.pushMode(newMode);
            newMode.enter();
        }
        // The CropModes need setup and teardown:
        if ((newMode == cropMode) || (newMode == rotateMode)) {
            CropMode crop = (CropMode) newMode;
            crop.resetCrop();
        }
        // The CropModes need to be kept in sync with each other:
        if (oldMode == cropMode) {
            rotateMode.setCrop(cropMode.getCrop());
        }
        if (oldMode == rotateMode) {
            cropMode.setCrop(rotateMode.getCrop());
        }
    }

    // Get a default CropBounds for the CropMode overlay, in image
    // coordinates.
    private CropBounds getInitialCropBounds() {
        double x = underlayBounds.getX();
        double y = underlayBounds.getY();
        double width = underlayBounds.getWidth();
        double height = underlayBounds.getHeight();
        Rectangle2D inset = new Rectangle2D.Double(
            x, y, width, height
        );
        CropBounds crop = new CropBounds(inset);
        try {
            AffineTransform inverse = getOverlayTransform().createInverse();
            crop = CropBounds.transform(inverse, crop);
            return crop;
        }
        catch (NoninvertibleTransformException e) {
            // Leave the crop overlay uninitialized.
        }
        return null;
    }

    // Don't rely on keyboard focus to switch the pan mode.
    // Instead, postprocess unclaimed key events:

    private void registerPanModeListener() {
        KeyboardFocusManager focus =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focus.addKeyEventPostProcessor(panModeKeyProcessor);
    }

    void dispose() {
        KeyboardFocusManager focus =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focus.removeKeyEventPostProcessor(panModeKeyProcessor);
    }
}
