/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.Engine;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.Preview;
import com.lightcrafts.ui.ActivityMeter;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.crop.CropMode;
import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.layout.ToggleTitleBorder;
import com.lightcrafts.ui.mode.ModeOverlay;
import com.lightcrafts.ui.mode.AbstractMode;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.ui.region.RegionOverlay;
import com.lightcrafts.ui.scroll.CenteringScrollPane;
import com.lightcrafts.ui.scroll.PannerOverlay;
import com.lightcrafts.ui.scroll.ScrollMode;
import com.lightcrafts.ui.toolkit.BoxedButton;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.List;

/**
 * This class initializes the various Document editor controls and
 * interconnects their behaviors.  The controls are: an image component with
 * mode overlays, activity meter, and scrollbars (<code>getImage()</code>); a
 * stack of Operation controls (<code>getTools()</code>); and a toolbar of
 * miscellaneous buttons (<code>getToolBar()</code>).
 * <p>
 * These components may be placed into a suitable layout using EditorLayout,
 * or they may be rearranged in other contexts.
 * <p>
 * See Document.getEditor().
 */

public class Editor {

    private Engine engine;
    private ScaleModel scale;

    // Modes are managed here:
    private RegionManager regions;
    private CropRotateManager crop;
    private ModeOverlay overlay;
    private ModeManager modes;

    // These are the Components of an Editor:
    private EditorControls opControls;
    private Box toolbar;
    private ActivityMeter imagePane;

    // Keep a reference to the scroll, for zoom-to-fit:
    private CenteringScrollPane imageScroll;

    // Switch the zoom-to-fit mode on and off during mouse modes:
    private FitButton fitButton;

    private Action proofAction;     // show/hide the proof tool

    private ScrollMode transientPanMode;
    private MiniScrollMode permanentPanMode;
    private CropMode cropMode;
    private CropMode rotorMode;
    private RegionOverlay regionMode;

    static void scrollByUnits(JScrollBar scrollbar, int direction, int units) {
        // This method is called from BasicScrollPaneUI to implement wheel
        // scrolling, as well as from scrollByUnit().
        int delta;

        for (int i = 0; i < units; i++) {
            if (direction > 0) {
                delta = scrollbar.getUnitIncrement(direction);
            } else {
                delta = -scrollbar.getUnitIncrement(direction);
            }

            int oldValue = scrollbar.getValue();
            int newValue = oldValue + delta;

            // Check for overflow.
            if (delta > 0 && newValue < oldValue) {
                newValue = scrollbar.getMaximum();
            } else if (delta < 0 && newValue > oldValue) {
                newValue = scrollbar.getMinimum();
            }
            if (oldValue == newValue) {
                break;
            }
            scrollbar.setValue(newValue);
        }
    }

    static void scrollByBlock(JScrollBar scrollbar, int direction) {
        // This method is called from BasicScrollPaneUI to implement wheel
        // scrolling, and also from scrollByBlock().
        int oldValue = scrollbar.getValue();
        int blockIncrement = scrollbar.getBlockIncrement(direction);
        int delta = blockIncrement * ((direction > 0) ? +1 : -1);
        int newValue = oldValue + delta;

        // Check for overflow.
        if (delta > 0 && newValue < oldValue) {
            newValue = scrollbar.getMaximum();
        } else if (delta < 0 && newValue > oldValue) {
            newValue = scrollbar.getMinimum();
        }

        scrollbar.setValue(newValue);
    }

    public void horizontalMouseWheelMoved(MouseWheelEvent e) {
        if (transientPanMode != null) {
            transientPanMode.getOverlay().getMouseWheelListeners()[0].mouseWheelMoved(e);
        }
    }

    public EditorMode getMode() {
        if ( modes != null )
            return modes.getMode();
        return null;
    }
    
    public void setMode( EditorMode mode ) {
        if ( modes != null )
            modes.setEditorMode( mode );
    }

    private static class MiniScrollMode extends AbstractMode {
        private JPanel overlay;     // just something to setCursor() on

        public MiniScrollMode() {
            overlay = new JPanel();
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
    }

    class PanningMouseWheelListener implements MouseWheelListener {
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (imageScroll.isWheelScrollingEnabled() && e.getScrollAmount() != 0) {
                int direction = e.getWheelRotation() < 0 ? -1 : 1;
                if (e.isControlDown()) {
                    scale.scaleUpDown(-direction);
                } else {
                    JScrollBar toScroll = (e.getScrollType() < 2 && ! e.isShiftDown())
                                          ? imageScroll.getVerticalScrollBar()
                                          : imageScroll.getHorizontalScrollBar();
                    if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                        scrollByUnits(toScroll, direction, e.getScrollAmount());
                    } else if (e.getScrollType() ==
                               MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
                        scrollByBlock(toScroll, direction);
                    } else {
                        scrollByUnits(toScroll, -direction, Math.abs(e.getWheelRotation()));
                    }
                }
            }
        }
    }

    Editor(
        final Engine engine,
        final ScaleModel scale,
        final XFormModel xform,
        RegionManager regions,
        CropRotateManager crop,
        Document doc                // for zoom-to-fit
    ) {
        this.engine = engine;
        this.scale = scale;
        this.regions = regions;
        this.crop = crop;

        // Instantiate the UI elements:

        Component image = engine.getComponent();
        overlay = new ModeOverlay(image);

        imageScroll = new CenteringScrollPane(overlay);
        imageScroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        imageScroll.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_NEVER
        );

        imageScroll.setBorder(LightZoneSkin.getImageBorder());

        imagePane = new ActivityMeter(new PannerOverlay(imageScroll));
        engine.addEngineListener(imagePane);

        // Mode initializations:

        regionMode = regions.getMode();

        // Two pan modes, so you can pan in the arrow-cursor mode and
        // also in any other mode when you press spacebar.
        PanningMouseWheelListener mouseWheelListener = new PanningMouseWheelListener();
        transientPanMode = new ScrollMode(imageScroll);
        transientPanMode.getOverlay().addMouseWheelListener(mouseWheelListener);
        permanentPanMode = new MiniScrollMode();
        permanentPanMode.getOverlay().addMouseWheelListener(mouseWheelListener);

        cropMode = crop.getCropMode();

        rotorMode = crop.getRotorMode();

        opControls = new EditorControls(engine);
        opControls.addOpStackListener(regions);

        // Plug in dropper calls, so previews can update:

        overlay.addMouseInputListener(
            new MouseInputAdapter() {
                public void mouseDragged(MouseEvent e) {
                    updateDropper(e);
                }
                public void mouseMoved(MouseEvent e) {
                    updateDropper(e);
                }
                public void mouseExited(MouseEvent e) {
                    opControls.setDropper(null);
                }
                private void updateDropper(MouseEvent e) {
                    Point p = e.getPoint();
                    Point q = new Point();
                    AffineTransform xform = modes.getOverlayTransform();
                    try {
                        xform.inverseTransform(p, q);
                        opControls.setDropper(q);
                    }
                    catch (NoninvertibleTransformException f) {
                        // Can't happen, because Engine transforms are never
                        // singular.
                        System.err.println(
                            "DocPanel Preview dropper update: " + f.getMessage()
                        );
                    }
                }
            }
        );

        // Mode-switching logic can be intricate:
        CurveTypeButtons regionButtons = regions.getCurveTypeButtons();
        modes = new ModeManager(
            regionMode,
            cropMode,
            transientPanMode,
            permanentPanMode,
            rotorMode,
            overlay,
            image,
            regionButtons,
            doc
        );
        opControls.addOpStackListener(modes);

        // Synchronize all the Modes with the current AffineTransform:
        xform.addXFormListener(modes);

        // Orientation buttons:
        final Action leftAction = crop.getLeftAction();
        final Action rightAction = crop.getRightAction();
        RotateButtons rotors = new RotateButtons(leftAction, rightAction);
        ToggleTitleBorder.setBorder(rotors, LOCALE.get("RotateBorderTitle"));

        // Stuff some Mode controls into a SelectableControl in EditorControls:
        ProofSelectableControl proofOp = new ProofSelectableControl(engine);

        ProofButton pb = new ProofButton(opControls, proofOp);
        BoxedButton proofButton = new BoxedButton(LOCALE.get("SoftProofBorderTitle"), pb);

        proofAction = pb.getShowHideAction();

        // More header controls:
        BoxedButton eyeButton = new BoxedButton(LOCALE.get("EyeBorderTitle"), new EyeButton(engine));

        OneToOneButton oneToOneButton = new OneToOneButton(scale);
        fitButton = new FitButton(this, engine, scale);
        LargerButton largerButton = new LargerButton(scale);
        SmallerButton smallerButton = new SmallerButton(scale);

        // Hook up the zoom-to-fit mode:
        imageScroll.getViewport().addComponentListener(
            new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    if (fitButton.isSelected()) {
                        fitButton.doZoomToFit();
                    }
                }
            }
        );
        Box zoomBox = Box.createHorizontalBox();
        zoomBox.add(oneToOneButton);
        zoomBox.add(fitButton);
        zoomBox.add(largerButton);
        zoomBox.add(smallerButton);
        ToggleTitleBorder.setBorder(zoomBox, LOCALE.get("ZoomBorderTitle"));

        ModeButtons modeBox = new ModeButtons(modes);
        modes.setModeButtons(modeBox);

        toolbar = Box.createHorizontalBox();
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(eyeButton.box);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(proofButton.box);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(rotors);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(zoomBox);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(new Separator());
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(modeBox);
        toolbar.add(Box.createHorizontalStrut(8));

        // Add space above and below, to tune the layout:
        Border border = BorderFactory.createEmptyBorder(3, 0, 3, 0);
        toolbar.setBorder(border);
    }

    static class Separator extends JSeparator {
        Separator() {
            super(SwingConstants.VERTICAL);
            setMaximumSize(new Dimension(3, 32));
        }
    }

    // Make a disabled set of controls, for the no-Document display mode.
    //
    // This method was cloned from the regular Editor constructor, with
    // dummy data substituted for Document data.

    Editor() {
        final Component image = new JLabel();     // JLabel is not opaque
        overlay = new ModeOverlay(image);

        imageScroll = new CenteringScrollPane(overlay);
        imageScroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        imageScroll.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_NEVER
        );
        imagePane = new ActivityMeter(new PannerOverlay(imageScroll));

        opControls = new EditorControls();

        // Need a RegionManager to initialize the (disabled) regionButton:
        regions = new RegionManager();
        final Action showHideAction = regions.getShowHideAction();
        // Cause the button to appear unselected:
        showHideAction.actionPerformed(null);
        showHideAction.setEnabled(false);

        final RotateButtons rotors = new RotateButtons();
        ToggleTitleBorder.setBorder(rotors, LOCALE.get("RotateBorderTitle"));

        final ProofButton proofButton = new ProofButton();
        final JComponent eyeButton = new EyeButton();

        // Lots of titles:
        ToggleTitleBorder.setBorder(
            proofButton, LOCALE.get("SoftProofBorderTitle")
        );
        ToggleTitleBorder.setBorder(
            eyeButton, LOCALE.get("EyeBorderTitle")
        );
        final JComponent oneToOneButton = new OneToOneButton();
        final JComponent fitButton = new FitButton();
        final JComponent largerButton = new LargerButton();
        final JComponent smallerButton = new SmallerButton();

        final Box zoomBox = Box.createHorizontalBox();
        zoomBox.add(oneToOneButton);
        zoomBox.add(fitButton);
        zoomBox.add(largerButton);
        zoomBox.add(smallerButton);
        ToggleTitleBorder.setBorder(zoomBox, LOCALE.get("ZoomBorderTitle"));

        final ModeButtons modeBox = new ModeButtons();
        ToggleTitleBorder.setBorder(modeBox, LOCALE.get("ModeBorderTitle"));

        toolbar = Box.createHorizontalBox();
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(zoomBox);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(proofButton);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(eyeButton);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(rotors);
        toolbar.add(new Separator());
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(modeBox);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(new Separator());
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(Box.createHorizontalGlue());

        // Add space above and below, to tune the Mac layout:
        Border border = BorderFactory.createEmptyBorder(3, 0, 3, 0);
        toolbar.setBorder(border);
    }

    private boolean wasFitMode;

    public void pushFitMode() {
        wasFitMode = fitButton.isSelected();
        fitButton.setSelected(false);
    }

    public boolean popFitMode() {
        fitButton.setSelected(wasFitMode);
        return wasFitMode;
    }

    public Engine getEngine() {
        return engine;
    }

    public ModeManager getModeManager() {
        return modes;
    }

    /**
     * The tools JComponent is the Engine tool stack, where most editing work
     * is conducted.
     */
    public EditorControls getToolStack() {
        return opControls;
    }

    /**
     * The tool bar JComponent is a row of miscellaneous buttons, normally
     * displayed above the image.
     */
    public JComponent getToolBar() {
        return toolbar;
    }

    /**
     * The image JComponent shows the image under edit, with lots of stuff
     * layered on top.
     */
    public JComponent getImage() {
        return imagePane;
    }

    public void setPreview(Preview preview) {
        opControls.setPreview(preview);
    }

    /**
     * Add a "Loading..." indication in the ActivityMeter, to show that
     * something is about to happen.
     */
    public void showWait(String text) {
        imagePane.showWait(text);
    }

    /**
     * Remove the "Loading..." indication in the ActivityMeter.
     */
    public void hideWait() {
        imagePane.hideWait();
    }

    /**
     * Set some text to show in the tool stack area in case this Editor is
     * disabled.
     */
    public void setDisabledText(String text) {
        // Do nothing; this editor is active
    }

    public List<OpControl> addControls(XmlNode node) throws XMLException {
        List<OpControl> controls = opControls.addControls(node);
        // Tell the RegionOverlay about its new cookies:
        for (OpControl control : controls) {
            regions.opChanged(control);
        }
        // Add the curve data:
        regions.addSaved(controls, node);

        // Ignore any crop or rotate commands in this context.

        return controls;
    }

    public OpControl addControl(OperationType type, int index) {
        return opControls.addControl(type, 0);
    }

    public void removeControls(List<OpControl> controls) {
        opControls.removeControls(controls);
    }

    void addUndoableEditListener(UndoableEditListener listener) {
        crop.addUndoableEditListener(listener);
        opControls.addUndoableEditListener(listener);
        regions.addUndoableEditListener(listener);
    }

    void removeUndoableEditListener(UndoableEditListener listener) {
        crop.removeUndoableEditListener(listener);
        opControls.removeUndoableEditListener(listener);
        regions.removeUndoableEditListener(listener);
    }

    // Used for zoomToFit() in Document and FitButton:
    Rectangle getMaxImageBounds() {
        JViewport viewport = imageScroll.getViewport();
        Rectangle bounds = viewport.getVisibleRect();
        bounds = ModeOverlay.insetPercent(bounds);
        return bounds;
    }

    List<Action> getOperations() {
        return opControls.getOperations();
    }

    Action getProofAction() {
        return proofAction;
    }

    boolean hasRawAdjustments() {
        return opControls.hasRawAdjustments();
    }

    void dispose() {
        // The ModeManager needs to remove a key binding:
        if (modes != null) {
            modes.dispose();
        }
        ToggleTitleBorder.unsetAllBorders(toolbar);

        if (transientPanMode != null)
            transientPanMode.dispose();
        if (permanentPanMode != null)
            permanentPanMode.dispose();
        if (cropMode != null)
            cropMode.dispose();
        if (rotorMode != null)
            rotorMode.dispose();
        if (regionMode != null)
            regionMode.dispose();
    }

    // Tell this Editor that save is occuring, and so it should commit any
    // crop or rotate state from its modes.
    public TemporaryEditorCommitState saveStart() {
        boolean isCropActive = (overlay.peekMode() == cropMode);
        boolean isRotateActive = (overlay.peekMode() == rotorMode);
        if (isCropActive || isRotateActive) {
            modes.setNoMode();
        }
        TemporaryEditorCommitState state = new TemporaryEditorCommitState(
            isCropActive, isRotateActive
        );
        return state;
    }

    public void saveEnd(TemporaryEditorCommitState state) {
        if (state.isCropActive) {
            modes.setCropMode();
        }
        else if (state.isRotateActive) {
            modes.setRotateMode();
        }
    }

    void save(XmlNode node) {
        opControls.save(node);
        List opControls = this.opControls.getOpControls();
        regions.save(opControls, node);
        crop.save(node);
    }

    void restore(XmlNode node) throws XMLException {
        try {
            opControls.restore(node);
        } catch (XMLException e) {
            dispose();
            throw e;
        }
        // First tell the RegionOverlay about all its cookies:
        List<OpControl> opControls = this.opControls.getOpControls();
        for (OpControl control : opControls) {
            regions.opChanged(control);
        }
        // Then restore all the curve data:
        regions.restore(opControls, node);

        crop.restore(node);
    }
}
