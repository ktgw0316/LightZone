/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.*;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.ui.operation.OpStackListener;
import com.lightcrafts.ui.operation.SelectableControl;
import com.lightcrafts.ui.region.*;
import com.lightcrafts.ui.action.ToggleAction;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.event.*;
import java.util.List;

/** This class mediates between an OpStack and a RegionOverlay.  It
  * listens to the OpStack for changes to the selected control and then
  * updates the visible regions; and it listens for changes to the Regions and
  * stuffs the updates into the selected control's Operation.
  */

public class RegionManager implements OpStackListener, RegionListener {

    private RegionOverlay overlay;
    private OpControl control;
    private AffineTransform xform;

    private CurveTypeButtons buttons;

    private ToggleAction showHideAction;

    RegionManager() {
        overlay = new RegionOverlay();
        overlay.addRegionListener(this);
        buttons = new CurveTypeButtons(this);
        showHideAction = overlay.getShowHideAction();
        eeRegister();

        // If the user clicks on the regions overlay and the regions are not
        // visible, then make them visible:
        overlay.addMouseListener(
            new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    boolean regionsVisible = showHideAction.getState();
                    if (! regionsVisible) {
                        showHideAction.actionPerformed(
                            new ActionEvent(overlay, 0, "Show Regions")
                        );
                    }
                }
            }
        );
    }

    RegionOverlay getMode() {
        return overlay;
    }

    CurveTypeButtons getCurveTypeButtons() {
        return buttons;
    }

    public void addRegionListener(RegionListener listener) {
        overlay.addRegionListener(listener);
    }

    public void removeRegionListener(RegionListener listener) {
        overlay.removeRegionListener(listener);
    }

    public AbstractAction getShowHideAction() {
        return showHideAction;
    }

    public void opAdded(OpControl control) {
        // Preexisting regions may exist for this OpControl.
        // (Like after undo.)
        if (overlay.hasCookie(control)) {
            Region region = overlay.getRegion(control);
            control.getOperation().setRegion(region);
            control.setRegionIndicator(region!=null);
        }
    }

    public void opChanged(OpControl control) {
        // Must handle null to mean "no current cookie":
        if (control != null) {
            Operation op = control.getOperation();
            boolean isClone = (op instanceof CloneOperation);
            boolean isSpot = (op instanceof SpotOperation);
            boolean isRedEye = (op instanceof RedEyeOperation);
            overlay.setCookie(control, isClone || isSpot, isSpot || isRedEye);
        }
        else {
            overlay.setCookie(null);
        }
        this.control = control;
    }

    public void opChanged(SelectableControl control) {
        overlay.setCookie(null);
        this.control = null;        
    }

    public void opLockChanged(OpControl control) {
        if (this.control == control) {
            opChanged(control);
        }
    }

    public void opRemoved(OpControl control) {
        // TODO Tell the overlay, maybe trim the RegionModel (helps LZN).
    }

    public void regionBatchStart(Object cookie) {
        OpControl control = (OpControl) cookie;
        control.getOperation().changeBatchStarted();
    }

    public void regionChanged(Object cookie, SharedShape shape) {
        OpControl control = (OpControl) cookie;
        Region region = overlay.getRegion(control);
        control.getOperation().setRegion(region);
        control.setRegionIndicator(region!=null);
    }

    public void regionBatchEnd(Object cookie) {
        OpControl control = (OpControl) cookie;
        control.getOperation().changeBatchEnded();
    }

    public void addSelectionListener(CurveSelection.Listener listener) {
        overlay.addSelectionListener(listener);
    }

    public void removeSelectionListener(CurveSelection.Listener listener) {
        overlay.removeSelectionListener(listener);
    }

    public void shareShape(SharedShape shape, boolean clone) {
        if (control != null) {
            overlay.shareShape(control, shape, clone);
        }
    }

    public void shareShapes(CurveIterator shapes, boolean clone) {
        if (control != null) {
            shapes.reset();
            while (shapes.hasNext()) {
                overlay.shareShape(control, shapes.nextCurve(), clone);
            }
        }
    }

    public void unShareShape(SharedShape shape) {
        if (control != null) {
            overlay.unShareShape(control, shape);
        }
    }

    public void unShareShapes(CurveIterator shapes) {
        if (control != null) {
            shapes.reset();
            while (shapes.hasNext()) {
                overlay.unShareShape(control, shapes.nextCurve());
            }
        }
    }

    void save(List cookies, XmlNode node) {
        overlay.save(cookies, node);
    }

    void restore(List cookies, XmlNode node) throws XMLException {
        overlay.restore(cookies, node);
    }

    void addSaved(List cookies, XmlNode node) throws XMLException {
        overlay.addSaved(cookies, node);
    }

    void setXForm(AffineTransform xform) {
        // For EeRegion.create() in eeRegister():
        this.xform = xform;
    }

    private Dimension getInferredImageSize() {
        Dimension size = overlay.getUnderlayBounds().getSize();
        if (xform == null) {
            return size;
        }
        Point2D ul = new Point(0, 0);
        Point2D ll = new Point(0, size.height);
        Point2D ur = new Point(size.width, 0);
        try {
            ul = xform.inverseTransform(ul, null);
            ll = xform.inverseTransform(ll, null);
            ur = xform.inverseTransform(ur, null);
        }
        catch (NoninvertibleTransformException e) {
            return size;
        }
        double width = Math.sqrt(
            (ur.getX() - ul.getX()) * (ur.getX() - ul.getX()) +
            (ur.getY() - ul.getY()) * (ur.getY() - ul.getY())
        );
        double height = Math.sqrt(
            (ll.getX() - ul.getX()) * (ll.getX() - ul.getX()) +
            (ll.getY() - ul.getY()) * (ll.getY() - ul.getY())
        );
        return new Dimension(
            (int) Math.round(width), (int) Math.round(height)
        );
    }

    private void eeRegister() {
        ActionListener action = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                if (control != null) {
                    Dimension size = getInferredImageSize();
                    Graphics2D g = (Graphics2D) overlay.getGraphics();
                    Region ee = EeRegion.create(g, size);
                    control.getOperation().setRegion(ee);
                    control.setRegionIndicator(ee!=null);
                }
            }
        };
        int mask = InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK;
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, mask);
        overlay.registerKeyboardAction(
            action, key, JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    public void addUndoableEditListener(UndoableEditListener listener) {
        overlay.addUndoableEditListener(listener);
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
        overlay.removeUndoableEditListener(listener);
    }

    public void setCurveType(int type) {
        overlay.setCurveType(type);
        buttons.updateFromFactory();
    }

    public int getCurveType() {
        return overlay.getCurveType();
    }
}
