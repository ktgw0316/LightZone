/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.utils.awt.geom.HiDpi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Iterator;
import java.util.Set;

import static com.lightcrafts.ui.region.Locale.LOCALE;

/** A JComponent that provides direct manipulation for Curves.
  * It maintains an optional AffineTransform, which it applies to
  * painting, mouse event dispatch, and bounds calculations.
  */

class CurveComponent extends JComponent
    implements CurveSelection.Listener, RegionModel.Listener
{
    private RegionModel model;      // Defines our Curves
    private CurveSelection selection; // Defines the selected Curve
    private AffineTransform xform;  // Can draw transformed

    private Curve updatingCurve;    // Track a Curve undergoing batch changes

    private boolean showClonePts;     // Show "special points" (clone tool)
    private boolean makeSpotCurves;   // Only elliptical "spots" (spot tool)

    private Object cookie;          // Associate this with a user Object

    CurveComponent(RegionModel model, boolean clonePts, boolean spotCurves) {
        this.model = model;
        this.showClonePts = clonePts;
        this.makeSpotCurves = spotCurves;
        model.addListener(this);
        selection = model.getSelection();
        selection.addSelectionListener(this);
        addMouseListener(
            new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    requestFocusInWindow();
                    if (event.isPopupTrigger()) {
                        handlePopup(event);
                    }
                }
                public void mouseReleased(MouseEvent event) {
                    if (event.isPopupTrigger()) {
                        handlePopup(event);
                    }
                }
            }
        );
        // Register and unregister arrow keys for nudging selected curves
        // so arrow keys can be used for ancestor scroll panes when the
        // selection is empty.
        final CurveNudger nudger = new CurveNudger(model, this);
        selection.addSelectionListener(
            new CurveSelection.Listener() {
                public void selectionChanged(
                    CurveIterator oldSelection, CurveIterator newSelection
                ) {
                    if (newSelection.hasNext()) {
                        nudger.registerKeys();
                    }
                    else {
                        nudger.unregisterKeys();
                    }
                }
            }
        );
    }

    void finishEditingCurve() {
        model.setEditingCurve(null);
    }

    Curve addCurve() {
        Curve curve = model.createCurve(this);
        // no painting, because a curve with no points is invisible
        return curve;
    }

    Curve addCloneCurve(Point2D p) {
        p = invertPoint(p);
        Curve curve = model.createCurve(this, p);
        Rectangle2D bounds = getPaintBounds(curve);
        repaint(bounds);
        return curve;
    }

    Curve addSpotCurve(Point2D p) {
        p = invertPoint(p);
        Curve curve = model.createSpotCurve(this, p);
        Rectangle2D bounds = getPaintBounds(curve);
        repaint(bounds);
        return curve;
    }

    // This removeCurve() method removes a Curve, enqueues a repaint, and
    // also maybe changes the current RegionMode, if the Curve being deleted
    // was the curve being edited.
    void removeCurve(Curve curve) {
        Rectangle2D bounds = getPaintBounds(curve);
        if (model.isEditingCurve(curve)) {
            model.editStart();
            model.removeCurve(this, curve);
            model.setMajorMode(new NewCurveMode(model, this));
            model.editEnd();
        }
        else {
            model.removeCurve(this, curve);
        }
        repaint(bounds);
    }

    // This removeCurve() method just removes a Curve and enqueues a repaint.
    // It doesn't alter the RegionMode.  It's needed in FolowMouseMode, in
    // case that RegionMode wants to back out its changes when it exits.
    void removeCurveWithoutModeChange(Curve curve) {
        Rectangle2D bounds = getPaintBounds(curve);
        model.removeCurve(this, curve);
        repaint(bounds);
    }

    void removeAllCurves() {
        Rectangle2D bounds = null;
        Set curves = model.getCurves(this);
        for (Iterator i=curves.iterator(); i.hasNext(); ) {
            Curve curve = (Curve) i.next();
            if (bounds == null) {
                bounds = getPaintBounds(curve);
            }
            else {
                bounds.add(getPaintBounds(curve));
            }
            model.removeCurve(this, curve);
        }
        if (bounds != null) {
            repaint(bounds);
        }
    }

    int addPoint(Curve curve, Point2D p) {
        p = invertPoint(p);
        Rectangle2D oldBounds = curve.getPaintBounds();
        int index = curve.addPoint(p);
        Rectangle2D newBounds = curve.getPaintBounds();
        model.notifyChanged(curve);
        repaint(oldBounds, newBounds);
        return index;
    }

    void insertPoint(Curve curve, int index, Point2D p) {
        p = invertPoint(p);
        Rectangle2D oldBounds = curve.getPaintBounds();
        curve.insertPoint(index, p);
        Rectangle2D newBounds = curve.getPaintBounds();
        model.notifyChanged(curve);
        repaint(oldBounds, newBounds);
    }

    void removePoint(Curve curve, int index) {
        Rectangle2D oldBounds = curve.getPaintBounds();
        model.editStart();
        curve.removePoint(index);
        if (curve.isValidShape()) {
            model.notifyChanged(curve);
        }
        else {
            oldBounds = getPaintBounds(curve);
            removeCurve(curve);
        }
        model.editEnd();
        Rectangle2D newBounds = curve.getPaintBounds();
        repaint(oldBounds, newBounds);
    }

    void movePoint(Curve curve, int index, Point2D p, boolean isMoving) {
        if (isMoving) {
            updatingCurve = curve;
        }
        else {
            updatingCurve = null;
        }
        movePoint(curve, index, p);
    }

    void movePoint(Curve curve, int index, Point2D p) {
        p = invertPoint(p);
        Rectangle2D oldBounds = curve.getPaintBounds();
        curve.movePoint(index, p);
        Rectangle2D newBounds = curve.getPaintBounds();
        model.notifyChanged(curve);
        repaint(oldBounds, newBounds);
    }

    void moveCurve(Curve curve, Point2D p1, Point2D p2, boolean isMoving) {
        if (isMoving) {
            updatingCurve = curve;
        }
        else {
            updatingCurve = null;
        }
        moveCurve(curve, p1, p2);
    }

    void moveCurve(Curve curve, Point2D p1, Point2D p2) {
        Rectangle2D oldBounds = curve.getPaintBounds();
        p1 = invertPoint(p1);
        p2 = invertPoint(p2);
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        curve.translate(dx, dy);
        Rectangle2D newBounds = curve.getPaintBounds();
        model.notifyTranslated(curve, dx, dy);
        repaint(oldBounds, newBounds);
    }

    Curve getCurveAt(Point2D p) {
        p = invertPoint(p);
        Set curves = model.getCurves(this);
        for (Iterator i=curves.iterator(); i.hasNext(); ) {
            Curve curve = (Curve) i.next();
            if (curve.isOnCurve(p)) {
                return curve;
            }
        }
        return null;
    }

    Curve getCurveAround(Point2D p) {
        p = invertPoint(p);
        Set curves = model.getCurves(this);
        for (Iterator i=curves.iterator(); i.hasNext(); ) {
            Curve curve = (Curve) i.next();
            if (curve.isValidShape() && curve.contains(p)) {
                return curve;
            }
        }
        return null;
    }

    Set getCurves() {
        return model.getCurves(this);
    }

    int getPointAt(Curve curve, Point2D p) {
        p = invertPoint(p);
        return curve.getPointAt(p);
    }

    int getSegmentAt(Curve curve, Point2D p) {
        p = invertPoint(p);
        return curve.getSegmentAt(p);
    }

    boolean isInnerShapeAt(Curve curve, Point2D p) {
        p = invertPoint(p);
        return curve.isInnerShape(p);
    }

    void setInnerShapeAt(Curve curve, Point2D p, boolean isChanging) {
        if (isChanging) {
            updatingCurve = curve;
        }
        else {
            updatingCurve = null;
        }
        setInnerShapeAt(curve, p);
    }

    void setInnerShapeAt(Curve curve, Point2D p) {
        if (p != null) {
            p = invertPoint(p);
        }
        curve.setInnerShape(p);
        model.notifyChanged(curve);
        Rectangle2D bounds = curve.getPaintBounds();
        repaint(bounds);
    }

    boolean isClonePointAt(Curve curve, Point2D p) {
        p = invertPoint(p);
        return curve.isClonePoint(p);
    }

    boolean showsClonePoints() {
        return showClonePts;
    }

    boolean makesSpotCurves() {
        return makeSpotCurves;
    }

    void setClonePoint(Curve curve, Point2D p1, Point2D p2, boolean isMoving) {
        if (isMoving) {
            updatingCurve = curve;
        }
        else {
            updatingCurve = null;
        }
        setClonePoint(curve, p1, p2);
    }

    void setClonePoint(Curve curve, Point2D p1, Point2D p2) {
        Point2D clonePt = model.getClonePoint(curve);
        Rectangle2D oldBounds = curve.getPaintBounds();
        p1 = invertPoint(p1);
        p2 = invertPoint(p2);
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        clonePt.setLocation(clonePt.getX() + dx, clonePt.getY() + dy);
        curve.setClonePoint(clonePt);
        Rectangle2D newBounds = curve.getPaintBounds();
        repaint(oldBounds, newBounds);
        model.notifyChanged(curve);
    }

    Shape getShape() {
        Set curves = model.getCurves(this);
        if (curves.isEmpty()) {
            return null;
        }
        Area area = new Area();
        boolean isValid = false;
        for (Iterator i=curves.iterator(); i.hasNext(); ) {
            Curve curve = (Curve) i.next();
            if (curve.isValidShape()) {
                Area a = new Area(curve);
                area.add(a);
                isValid = true;
            }
        }
        if (! isValid) {
            return null;
        }
        return area;
    }

    void setTransform(AffineTransform xform) {
        this.xform = xform;
        repaint();
    }

    public void selectionChanged(
        CurveIterator oldSelection, CurveIterator newSelection
    ) {
        Rectangle2D oldBounds = null;
        Rectangle2D newBounds = null;
        if (oldSelection.hasNext()) {
            Curve oldCurve = oldSelection.nextCurve();
            oldBounds = oldCurve.getPaintBounds();
            while (oldSelection.hasNext()) {
                oldCurve = oldSelection.nextCurve();
                oldBounds.add(oldCurve.getPaintBounds());
            }
        }
        Set curves = model.getCurves(this);
        for (Iterator i=curves.iterator(); i.hasNext(); ) {
            Curve c = (Curve) i.next();
            c.resetHighlights();
        }
        if (newSelection.hasNext()) {
            Curve newCurve = newSelection.nextCurve();
            newCurve.highlightAllSegments();
            newBounds = newCurve.getPaintBounds();
            while (newSelection.hasNext()) {
                newCurve = newSelection.nextCurve();
                newCurve.highlightAllSegments();
                newBounds.add(newCurve.getPaintBounds());
            }
        }
        if ((oldBounds != null) || (newBounds != null)) {
            repaint(oldBounds, newBounds);
        }
    }

    void setCookie(Object cookie) {
        this.cookie = cookie;
    }

    Object getCookie() {
        return cookie;
    }

    private void repaint(Rectangle2D oldBounds, Rectangle2D newBounds) {
        if (oldBounds == null) {
            repaint(newBounds);
        }
        else if (newBounds == null) {
            repaint(oldBounds);
        }
        else {
            Rectangle2D union = oldBounds.createUnion(newBounds);
            repaint(union);
        }
    }

    private void repaint(Shape shape) {
        if (xform != null) {
            shape = xform.createTransformedShape(shape);
        }
        Rectangle bounds = shape.getBounds();
        // Pad the integer bounds by 1, to encompass extra pixels stroked
        // by antialiasing:
        repaint(0, bounds.x, bounds.y, bounds.width + 1, bounds.height + 1);
    }

    void repaint(Curve curve) {
        Rectangle2D bounds = curve.getPaintBounds();
        repaint(bounds);
    }

    private Rectangle2D getPaintBounds(Curve curve) {
        return curve.getPaintBounds();
    }

    private Point2D invertPoint(Point2D p) {
        if (xform == null) {
            return (Point2D) p.clone();
        }
        try {
            return xform.inverseTransform(p, null);
        }
        catch (NoninvertibleTransformException e) {
            throw new RuntimeException("Scaling failed", e);
        }
    }

    private void handlePopup(MouseEvent event) {
        final Point p0 = event.getPoint();
        final Point p = HiDpi.imageSpacePointFrom(p0);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = null;

        Curve curveAt = getCurveAt(p);
        Curve curveAround = getCurveAround(p);
        final Curve curve = (curveAt != null) ? curveAt : curveAround;

        item = new JMenuItem(LOCALE.get("HideRegionsMenuItem"));
        item.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    RegionOverlay overlay = (RegionOverlay) getParent();
                    AbstractAction showHideAction = overlay.getShowHideAction();
                    showHideAction.actionPerformed(event);
                }
            }
        );
        menu.add(item);
        if (curve != null) {

            if (model.isEditingCurve(curve)) {
                // Show/hide the blur width affordance:
                if (curve.getShowInnerShape()) {
                    item = new JMenuItem(LOCALE.get("HideInnerRegionMenuItem"));
                }
                else {
                    item = new JMenuItem(LOCALE.get("ShowInnerRegionMenuItem"));
                }
                item.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            boolean showHide = curve.getShowInnerShape();
                            curve.setShowInnerShape(! showHide);
                            repaint(curve);
                        }
                    }
                );
                menu.add(item);
            }
            menu.add(new JSeparator());

            final int index = getPointAt(curve, p);
            if ((index >= 0) && curve.allowsAddRemovePoints()) {
                item = new JMenuItem(LOCALE.get("DeletePointMenuItem"));
                item.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            removePoint(curve, index);
                        }
                    }
                );
                menu.add(item);
            }
            if (curve != null) {
                // Delete-curve button:
                item = new JMenuItem(LOCALE.get("DeleteRegionMenuItem"));
                item.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            removeCurve(curve);
                        }
                    }
                );
                menu.add(item);
            }
        }
        menu.show(this, p0.x, p0.y);
    }

    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        RenderingHints oldHints = g.getRenderingHints();
        Composite oldComposite = g.getComposite();
        AffineTransform oldTransform = g.getTransform();
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
        );
        if (xform != null) {
            AffineTransform newTransform =
                (AffineTransform) oldTransform.clone();
            newTransform.concatenate(HiDpi.inverseDefaultTransform);
            newTransform.concatenate(xform);
            g.setTransform(newTransform);
        }
        Composite updatingBlend = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, .25f
        );
        Composite normalBlend = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, .75f
        );
        Set curves = model.getCurves(this);
        for (Iterator i=curves.iterator(); i.hasNext(); ) {
            Curve curve = (Curve) i.next();
            if (curve == updatingCurve) {
                // Shade Curves during brief interactive updates.
                g.setComposite(updatingBlend);
            }
            else {
                g.setComposite(normalBlend);
            }
            boolean showClonePt =
                showClonePts &&
                model.isEditingCurve(curve) &&
                model.hasClonePoint(curve);
            curve.paint(g, showClonePt);
        }
        g.setTransform(oldTransform);
        g.setComposite(oldComposite);
        g.setRenderingHints(oldHints);
    }

    public Dimension getPreferredSize() {
        return new Dimension(400, 400);
    }

    public void editingCurveChanged(Curve oldCurve, Curve newCurve) {
        Set curves = getCurves();
        if (curves.contains(newCurve)) {
            newCurve.highlightAll();
            newCurve.setShowInnerShape(true);
            Rectangle2D bounds = getPaintBounds(newCurve);
            if (bounds != null) {
                repaint(bounds);
            }
        }
        if (curves.contains(oldCurve)) {
            oldCurve.resetHighlights();
            oldCurve.setShowInnerShape(false);
            if (selection.isSelected(oldCurve)) {
                oldCurve.highlightAllSegments();
            }
            Rectangle2D bounds = getPaintBounds(oldCurve);
            if (bounds != null) {
                repaint(bounds);
            }
        }
    }

    public void modeChanged(RegionMode oldMode, RegionMode newMode) {
        if (oldMode != null) {
            removeMouseListener(oldMode);
            removeMouseMotionListener(oldMode);
        }
        if (newMode != null) {
            addMouseListener(newMode);
            addMouseMotionListener(newMode);
        }
    }
}
