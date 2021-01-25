/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2019-     Masahiro Kitagawa */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.CropBounds;
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
import com.lightcrafts.utils.awt.geom.HiDpi;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handle switching among all the Modes.  Lots of special mode-transition
 * logic lives here, plus the affine transform updates for Modes.
 * <p>
 * ModeManager sets and resets the crop when CropMode starts and ends.
 * The pan mode is entered on a key press and exited on release.  The
 * temporary Modes defined by OpControls are added and removed here too.
 */

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ModeManager
    implements OpStackListener, OpControlModeListener, XFormListener
{
    // Look for the special key events to enter and exit the pan mode,
    // taking care to filter out auto-repeat events:

    private static final Map<Integer, Long> PanKeyCodeAndTime = new HashMap<>() {{
        put(KeyEvent.VK_SPACE, 0L);
        put(KeyEvent.VK_META, 0L);
        put(KeyEvent.VK_CONTROL, 0L);
    }};

    private final KeyEventPostProcessor panModeKeyProcessor =
        new KeyEventPostProcessor() {
            private boolean isPanMode;

            @Override
            public boolean postProcessKeyEvent(KeyEvent e) {
                val wasPanMode = (overlay.peekMode() == transientPanMode);
                val keyCode = e.getKeyCode();
                if (PanKeyCodeAndTime.containsKey(keyCode)) {
                    switch (e.getID()) {
                        case KeyEvent.KEY_PRESSED:
                            PanKeyCodeAndTime.replace(keyCode, e.getWhen());
                            isPanMode = true;
                            break;
                        case KeyEvent.KEY_RELEASED:
                            // Detect and ignore auto-repeat release events
                            val lastPressed = PanKeyCodeAndTime.get(keyCode);
                            if (e.getWhen() > lastPressed + 1) {
                                PanKeyCodeAndTime.replace(keyCode, 0L);
                                isPanMode = PanKeyCodeAndTime.values().stream()
                                        .anyMatch(t -> t > 0L);
                            }
                            break;
                    }
                }
                if (! wasPanMode && isPanMode) {
                    overlay.pushMode(transientPanMode);
                }
                else if (wasPanMode && ! isPanMode) {
                    overlay.popMode();
                }
                return false;   // these key events have other interpretations
            }
        };

    private AffineTransform xform;

    private final Mode regionMode;
    private final CropMode cropMode;
    private final AbstractMode transientPanMode; // works with other modes (space key)
    private final CropMode rotateMode;

    private final ModeOverlay overlay;

    // The "extras" component for the region mode
    private final JComponent regionExtras;

    private final Document doc;

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
        AbstractMode transientPanMode, // After space-bar
        AbstractMode permanentPanMode, // present in "no mode" (arrow cursor)
        CropMode rotateMode,
        ModeOverlay overlay,
        Component underlay,
        JComponent regionExtras,    // The curve-type buttons
        Document doc                // For zoom-to-fit around the crop mode
    ) {
        this(regionMode, cropMode, transientPanMode, rotateMode, overlay,
                regionExtras, doc);
        xform = new AffineTransform();

        // The AffineTransform in the Modes sometimes depends on the size
        // of the ModeOverlay:
        overlay.addComponentListener(
            new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    setTransform(xform);
                }
            }
        );
        // Modes also require notification about the location and size of
        // their underlay:
        underlay.addComponentListener(
            new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    Rectangle bounds = event.getComponent().getBounds();
                    setUnderlayBounds(bounds);
                }
                @Override
                public void componentMoved(ComponentEvent event) {
                    Rectangle bounds = event.getComponent().getBounds();
                    setUnderlayBounds(bounds);
                }
            }
        );
        // The crop and rotate modes likes to end themselves:
        cropMode.addCropListener(
            new CropListener() {
                @Override
                public void cropCommitted(CropBounds bounds) {
                    // This may be called because someone already switched
                    // away from the crop mode, but it may also be called
                    // from the "Commit" crop mode popup menu item, in which
                    // case we must handle the mode switch ourselves.
                    EventQueue.invokeLater(() -> {
                        if (modeButtons != null && modeButtons.isCropSelected()) {
                            setEditorMode( EditorMode.ARROW );
                        }
                    });
                }
                @Override
                public void unCrop() {
                }
            }
        );
        rotateMode.addCropListener(
            new CropListener() {
                @Override
                public void cropCommitted(CropBounds bounds) {
                    // This may be called because someone already switched
                    // away from the rotate mode, but it may also be called
                    // from the "Commit" crop mode popup menu item, in which
                    // case we must handle the mode switch ourselves.
                    EventQueue.invokeLater(() -> {
                        if (modeButtons != null && modeButtons.isRotateSelected()) {
                            setEditorMode( EditorMode.ARROW );
                        }
                    });
                }
                @Override
                public void unCrop() {
                }
            }
        );
        registerPanModeListener();

        setMode(permanentPanMode);
    }

    public EditorMode getMode() {
        return m_editorMode;
    }

    void setModeButtons(ModeButtons buttons) {
        modeButtons = buttons;
    }

    // Start OpStackListener:

    @Override
    public void opAdded(OpControl control) {
    }

    @Override
    public void opChanged(OpControl control) {
        if (isControlModeActive()) {
            exitMode(null);
        }
        if (this.control != null) {
            this.control.removeModeListener(this);
        } else {
            setEditorMode(EditorMode.ARROW);
        }
        this.control = control;
        if (control == null) {
            modeButtons.setRegionsEnabled(true);
            return;
        }
        if (! control.isLocked()) {
            control.addModeListener(this);
        }
        // Some OpControls don't get regions.
        val isLocked = control.isLocked();
        val isRaw = control.isRawCorrection();
        modeButtons.setRegionsEnabled(! (isLocked || isRaw));

        val op = control.getOperation();
        val prefMode = op.getPreferredMode();
        if (isLocked || isRaw ||
                prefMode != EditorMode.ARROW ||
                m_editorMode == EditorMode.ARROW ) {
            setEditorMode(prefMode);
        }
    }

    @Override
    public void opChanged(SelectableControl control) {
        if (this.control != null) {
            this.control.removeModeListener(this);
            this.control = null;
        }
        if (control == null || control instanceof ProofSelectableControl) {
            setMode(null);
        }
    }

    @Override
    public void opLockChanged(OpControl control) {
        if (this.control == control) {
            opChanged(control);
        }
    }

    @Override
    public void opRemoved(OpControl control) {
        // Since region mode interacts with the tool stack selection,
        // be sure to pop out of that mode if it's the current mode at
        // the time a tool is deleted.
        if (overlay.peekMode() == regionMode && this.control != null) {
            val op = this.control.getOperation();
            if (op.getPreferredMode() != EditorMode.REGION) {
                EventQueue.invokeLater(() -> setEditorMode(EditorMode.ARROW));
            }
        }
    }

    // End OpStackListener.

    void setEditorMode(EditorMode mode) {
        if ( mode == m_editorMode )
            return;
        if ( modeButtons != null ) {
            switch (mode) {
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
        }
        m_editorMode = mode;
    }

    // Start OpControlModeListener:

    @Override
    public void enterMode(Mode mode) {
        mode.enter();
        assert (! isControlModeActive());
        controlMode = mode;
        controlMode.setTransform(getOverlayTransform());
        overlay.pushMode(controlMode);
    }

    @Override
    public void exitMode(Mode mode) {
        assert isControlModeActive();
        if ( mode != null )
            mode.exit();
        overlay.popMode();
        controlMode = null;
    }

    // End OpControlModeListener.

    // Start XFormListener:

    @Override
    public void xFormChanged(AffineTransform xform) {
        // First, validate from the overlay's validate root, because the
        // overlay bounds partly determine the overlay affine transform.
        val scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(
            JScrollPane.class, overlay
        );
        if (scroll != null) {
            overlay.invalidate();   // mark the overlay as needing to update
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
        // The given AffineTransform maps original image coordinates to the
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

        val overlayTransform = getOverlayTransform();

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
        val overlayXform = overlay.getTransform();
        val totalXform = (AffineTransform) xform.clone();
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
        val crop = cropMode.getCrop();
        val initCrop = getInitialCropBounds();
        if (crop == null) {
            cropMode.setCropWithConstraints(initCrop);
        }
        cropMode.setResetCropBounds(initCrop);
    }

    private boolean isFitMode() {
        return doc.popFitMode();
    }

    private boolean wasCrop() {
        val peekMode = overlay.peekMode();
        return peekMode == cropMode || peekMode == rotateMode;
    }

    JComponent setNoMode() {
        setMode(null);
        m_editorMode = EditorMode.ARROW;
        if (wasCrop() && !isFitMode()) {
            doc.zoomToFit();
        }
        return null;
    }

    private EditorMode m_editorMode;

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
        val crop = cropMode.getCrop();
        rotateMode.setCrop(crop);
        rotateMode.setResetCropBounds(crop);
        return rotateMode.getControl().getResetButton();
    }

    JComponent setRegionMode() {
        setMode(regionMode);
        m_editorMode = EditorMode.REGION;
        if (wasCrop() && !isFitMode()) {
            doc.zoomToFit();
        }
        return regionExtras;
    }

    private void setMode(final Mode newMode) {
        // Peel off any temporary Modes we may have pushed.
        while (isControlModeActive() || isTransientPanModeActive()) {
            overlay.popMode();
            controlMode = null;
        }
        // See if that did the trick, to avoid duplicate pushes.
        val oldMode = overlay.peekMode();
        if (oldMode == newMode) return;

        if (oldMode == cropMode) transitFromCropMode(newMode);
        else if (oldMode == rotateMode) transitFromRotateMode(newMode);
        else if (oldMode == regionMode) transitFromRegionMode(newMode);
        else transit(null, newMode);
    }

    private void transitFromRegionMode(final Mode newMode) {
        // If we are in region mode make sure we exit edit mode
        if (regionMode instanceof RegionOverlay) {
            ((RegionOverlay) regionMode).finishEditingCurve();
        }
        overlay.popMode();
        transit(regionMode, newMode);
    }

    private void transitFromCropMode(final Mode newMode) {
        overlay.popMode();
        cropMode.doCrop();
        transit(cropMode, newMode);

        // The CropModes need to be kept in sync with each other:
        rotateMode.setCrop(cropMode.getCrop());
    }

    private void transitFromRotateMode(final Mode newMode) {
        overlay.popMode();
        rotateMode.doCrop();
        transit(rotateMode, newMode);

        // The CropModes need to be kept in sync with each other:
        cropMode.setCrop(rotateMode.getCrop());
    }

    private void transit(final Mode oldMode, final Mode newMode) {
        if (oldMode != null) {
            oldMode.exit();
        }
        if (newMode != null) {
            overlay.pushMode(newMode);
            newMode.enter();

            // The CropModes need setup and teardown:
            if (newMode == cropMode || newMode == rotateMode) {
                ((CropMode) newMode).resetCrop();
            }
        }
    }

    // Get a default CropBounds for the CropMode overlay, in image
    // coordinates.
    private CropBounds getInitialCropBounds() {
        val inset = new Rectangle(underlayBounds);
        val crop = new CropBounds(HiDpi.imageSpaceRectFrom(inset));
        try {
            val inverse = getOverlayTransform().createInverse();
            return CropBounds.transform(inverse, crop);
        }
        catch (NoninvertibleTransformException e) {
            // Leave the crop overlay uninitialized.
        }
        return null;
    }

    // Don't rely on keyboard focus to switch the pan mode.
    // Instead, postprocess unclaimed key events:

    private void registerPanModeListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventPostProcessor(panModeKeyProcessor);
    }

    void dispose() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removeKeyEventPostProcessor(panModeKeyProcessor);
    }
}
