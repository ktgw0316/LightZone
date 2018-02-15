/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.model.Contour;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEditSupport;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import static com.lightcrafts.ui.region.Locale.LOCALE;

/**
 * A RegionModel maintains a collection of Curves that define various
 * Regions which may appear in the overlays.  The Regions are constructed by
 * unioning subsets of the Curves, and each subset has an associated
 * CurveComponent to render it.
 * <p>
 * This model can tell which Curves are rendered in a given CurveComponent,
 * and which CurveComponents render a given Curve.
 * <p>
 * The RegionModel maintains a Contour instance for each Curve.  Each
 * instance is replaced every time its Curve changes.  This is for building
 * Region implementations using ContourRegion.
 * <p>
 * In addition to Curves, Contours, and CurveComponents, this model also
 * keeps track of the current CurveSelection, which is a Collection of Curves
 * that are drawn highlighted to respond to copy and paste; and sometimes
 * also a single "editing" Curve, which is drawn with its control points
 * visible and with an extra layer of edit batching for listeners.
 * <p>
 * Every Curve may have an associated ClonePoint, to accomodate the Curves
 * used by the clone tool.
 * <p>
 * There is also one RegionMode per RegionModel, which determines the policies
 * for mouse event handling on CurveComponents, translating MouseEvents into
 * edits on Curve data.
 */

class RegionModel {

    // Learn about changes to the RegionModel that are only broadcast
    // inside this package.
    interface Listener {

        // When the editing Curve changes, find out what it was before
        // and what it is now.  Either may be null, but not both.
        void editingCurveChanged(Curve oldCurve, Curve newCurve);

        // After an undo or redo happens, this tells what the new RegionMode
        // should be.
        void modeChanged(RegionMode oldMode, RegionMode newMode);
    }

    // Switch on some printlines that show about edit batching and
    // change notification:
    private static final boolean Debug =
        System.getProperty("lightcrafts.debug.regions") != null;

    private LinkedList<Listener> listeners;

    private TwoWayMultiMap<Curve, CurveComponent> map;

    private RegionOverlay overlay;  // Forwards notifications to users

    private Map<Curve, Contour> contours;   // For Region impl

    private CurveFactory factory;   // Instantiates all Curves

    private CurveSelection selection;   // The selection model for Curves

    private Curve editingCurve;     // See intro comments, maybe null

    // Handle mouse events on CurveComponents:
    private MajorRegionMode majorMode;
    private MinorRegionMode minorMode;

    private UndoableEditSupport undoSupport;

    private RegionEdit currentEdit;

    private int batchEdit;          // Suppress undoSupport while positive

    private boolean isUndoing;      // Suppress undoSupport while undoing

    private boolean isRestoring;    // Suppress undoSupport while restoring

    private int batchNotify;        // Count notification depth, for dev.

    RegionModel(RegionOverlay overlay) {
        this.overlay = overlay;
        map = new TwoWayMultiMap<Curve, CurveComponent>();
        contours = new HashMap<Curve, Contour>();
        factory = new CurveFactory();
        selection = new CurveSelection();
        undoSupport = new UndoableEditSupport();
        listeners = new LinkedList<Listener>();
    }

    CurveSelection getSelection() {
        return selection;
    }

    void setEditingCurve(Curve curve) {
        if (editingCurve == curve) {
            return;
        }
        Curve oldCurve = editingCurve;
        Curve newCurve = curve;

        if (curve != null) {
            if (editingCurve != null) {
                setEditingCurve(null);
            }
            editingCurve = curve;
            selection.addCurve(editingCurve);
            notifyChangeStart(curve);
        }
        else {
            selection.removeCurve(editingCurve);
            notifyChangeEnd(editingCurve);
            editingCurve = null;
        }
        notifyListenersEditCurveChanged(oldCurve, newCurve);
    }

    boolean isEditingCurve(Curve curve) {
        return curve == editingCurve;
    }

    private RegionMode getMode() {
        return (minorMode != null) ? minorMode : majorMode;
    }

    void setMinorMode(MinorRegionMode newMode) {
        if (newMode == minorMode) {
            return;
        }
        RegionMode oldMode = getMode();
        if (oldMode == majorMode) {
            majorMode.modeExited();
        }
        minorMode = newMode;
        notifyModeChanged(oldMode, newMode);
    }

    void setMajorMode(MajorRegionMode newMode) {
        if (newMode == majorMode) {
            return;
        }
        RegionMode oldMode = getMode();
        if ((oldMode == majorMode) && (majorMode != null)) {
            majorMode.modeExited();
        }
        majorMode = newMode;
        minorMode = null;
        if (oldMode == null) {
            // If this is the first MajorRegionMode, kick off undoable edits.
            currentEdit = new RegionEdit();
        }
        newMode.modeEntered();
        setEditingCurve(newMode.getEditingCurve());
        notifyModeChanged(oldMode, newMode);
    }

    void setMajorModeWithoutExitOrEnter(MajorRegionMode newMode) {
        if (newMode == majorMode) {
            return;
        }
        RegionMode oldMode = getMode();
        majorMode = newMode;
        minorMode = null;
        setEditingCurve(newMode.getEditingCurve());
        notifyModeChanged(oldMode, newMode);
    }

    Set<CurveComponent> getComponents(Curve curve) {
        return map.getRight(curve);
    }

    Set<Curve> getCurves(CurveComponent comp) {
        return map.getLeft(comp);
    }

    Set<CurveComponent> getAllComponents() {
        return map.getAllRight();
    }

    Set<Curve> getAllCurves() {
        return map.getAllLeft();
    }

    void editStart() {
        batchEdit++;
        dumpEdit("edit start");
    }

    void editEnd() {
        dumpEdit("edit end");
        if (--batchEdit == 0) {
            postEdit();
        }
    }

    void editCancel() {
        dumpEdit("edit cancel");
        --batchEdit;
        // Don't call postEdit().
    }

    // Useful debug dump, to track timing and balancing of edit batches:
    private void dumpEdit(String s) {
        if (! Debug) {
            return;
        }
        System.out.print("|");
        for (int n=0; n<batchEdit;n++) {
            System.out.print("  ");
        }
        System.out.print(batchEdit);
        System.out.print(" ");
        System.out.println(s);
    }

    // Useful debug dump, to track timing and balancing of change notifications:
    private void dumpNotify(String s) {
        if (! Debug) {
            return;
        }
        System.out.print("|");
        for (int n=0; n<batchNotify;n++) {
            System.out.print("  ");
        }
        System.out.print(batchNotify);
        System.out.print(" ");
        System.out.println(s);
    }

    private void dumpModeChange(RegionMode oldMode, RegionMode newMode) {
        if (Debug) {
            System.out.println(
                "mode change: " +
                getNameOfMode(oldMode) + " -> " + getNameOfMode(newMode)
            );
        }
    }

    private static String getNameOfMode(RegionMode mode) {
        if (mode != null) {
            return mode.getClass().getName().replaceAll(".*\\.", "");
        }
        return "null";
    }

    void notifyChangeStart(Curve curve) {
        Set<CurveComponent> comps = map.getRight(curve);
        overlay.regionBatchStart(comps);
        batchNotify++;
        dumpNotify("change start");
    }

    void notifyChanged(Curve curve) {
        changeContour(curve);
        Set<CurveComponent> comps = map.getRight(curve);
        overlay.regionChanged(comps, curve);
        if (batchEdit == 0) {
            postEdit();
        }
    }

    void notifyTranslated(Curve curve, double dx, double dy) {
        translateContour(curve, dx, dy);
        Set<CurveComponent> comps = map.getRight(curve);
        overlay.regionChanged(comps, curve);
        if (batchEdit == 0) {
            postEdit();
        }
    }

    void notifyChangeEnd(Curve curve) {
        Set<CurveComponent> comps = map.getRight(curve);
        overlay.regionsBatchEnd(comps);
        dumpNotify("change end");
        batchNotify--;
    }

    void setNewCurveType(int type) {
        factory.setCurveType(type);
    }

    int getNewCurveType() {
        return factory.getCurveType();
    }

    Curve createCurve(CurveComponent comp) {
        Curve curve = factory.createCurve();
        addCurve(comp, curve);
        return curve;
    }

    Curve createCurve(CurveComponent comp, Point2D p) {
        Curve curve = factory.createCurve();
        Point2D q = new Point2D.Double(p.getX() - 20, p.getY() - 20);
        curve.setClonePoint(q);
        addCurve(comp, curve);
        return curve;
    }

    Curve createSpotCurve(CurveComponent comp, Point2D p) {
        Curve curve = factory.createSpotCurve(p);
        addCurve(comp, curve);
        return curve;
    }

    void addCurve(CurveComponent comp, Curve curve) {
        map.put(curve, comp);
        addContour(curve);
        // Maybe this CurveComponent shows clone points, but this Curve has
        // never had its clone point initialized.
        if (comp.showsClonePoints() && (curve.getClonePoint() == null)) {
            Rectangle2D bounds = curve.getBounds2D();
            Point2D center = new Point2D.Double(
                bounds.getCenterX(), bounds.getCenterY()
            );
            curve.setClonePoint(center);
            changeContour(curve);
        }
        Collection<CurveComponent> comps = Collections.singleton(comp);
        overlay.regionChanged(comps, curve);
        if (batchEdit == 0) {
            postEdit();
        }
    }

    void removeCurve(CurveComponent comp, Curve curve) {
        if (selection.isSelected(curve)) {
            selection.removeCurve(curve);
        }
        if (isEditingCurve(curve)) {
            setEditingCurve(null);
        }
        map.remove(curve, comp);

        Collection<CurveComponent> comps = Collections.singleton(comp);
        overlay.regionChanged(comps, curve);
        if (batchEdit == 0) {
            postEdit();
        }
    }

    void removeCurve(Curve curve) {
        // Don't just call map.removeLeft(curve), because we need to fire
        // all the notifications:
        Set<CurveComponent> comps = getComponents(curve);
        batchEdit++;
        for (CurveComponent comp : comps) {
            removeCurve(comp, curve);
        }
        map.removeLeft(curve);
        removeContour(curve);
        batchEdit--;
        if (batchEdit == 0) {
            postEdit();
        }
    }

    void removeCurves() {
        Set<Curve> curves = getAllCurves();
        batchEdit++;
        for (Curve curve : curves) {
            removeCurve(curve);
        }
        batchEdit--;
        if (batchEdit == 0) {
            postEdit();
        }
    }

    void addComponent(CurveComponent comp) {
        map.addRight(comp);
    }

    void removeComponent(CurveComponent comp) {
        map.removeRight(comp);
    }

    Contour getContour(Curve curve) {
        return (Contour) contours.get(curve);
    }

    void setClonePoint(Curve curve, Point2D p) {
        curve.setClonePoint(p);
    }

    Point2D getClonePoint(Curve curve) {
        return curve.getClonePoint();
    }

    boolean hasClonePoint(Curve curve) {
        return getClonePoint(curve) != null;
    }

    private void addContour(Curve curve) {
        if (! contours.containsKey(curve)) {
            Contour contour = new CurveContour(curve);
            contours.put(curve, contour);
        }
    }

    private void changeContour(Curve curve) {
        Contour contour = new CurveContour(curve);
        contours.put(curve, contour);
    }

    private void translateContour(Curve curve, double dx, double dy) {
        CurveContour contour = (CurveContour) contours.get(curve);
        contour.addTranslation(new Point2D.Double(dx, dy));
    }

    private void removeContour(Curve curve) {
        contours.remove(curve);
    }

    void addListener(Listener listener) {
        listeners.add(listener);
    }

    void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    void notifyListenersEditCurveChanged(Curve oldCurve, Curve newCurve) {
        for (Listener listener : listeners) {
            listener.editingCurveChanged(oldCurve, newCurve);
        }
    }

    void notifyModeChanged(RegionMode oldMode, RegionMode newMode) {
        dumpModeChange(oldMode, newMode);
        for (Listener listener : listeners) {
            listener.modeChanged(oldMode, newMode);
        }
    }

    void addUndoableEditListener(UndoableEditListener listener) {
        undoSupport.addUndoableEditListener(listener);
    }

    void removeUndoableEditListener(UndoableEditListener listener) {
        undoSupport.removeUndoableEditListener(listener);
    }

    private void postEdit() {
        if (isRestoring || isUndoing) {
            return;
        }
        if (Debug) {
            System.out.println("post edit");
        }
        currentEdit.end();
        undoSupport.postEdit(currentEdit);
        currentEdit = new RegionEdit();
    }

    // All Curve references need to be reset after undo and redo, because
    // undo and redo reinstantiate all the Curves.
    // 
    // Call this method to identify correspondences between stale Curves
    // and fresh ones.

    Curve getRestoredCurve(Curve old) {
        Set<Curve> curves = getAllCurves();
        for (Curve curve : curves) {
            if (curve.matches(old)) {
                return curve;
            }
        }
        return null;
    }

    // A state preserve/restore edit, like StateEdit but with XmlDocuments
    // instead of Hashtables.

    private class RegionEdit extends AbstractUndoableEdit {

        private final static String ModeTag = "mode";

        private XmlDocument beforeDoc = new XmlDocument("before");
        private XmlDocument afterDoc  = new XmlDocument("after");

        private List<CurveComponent> beforeComps = new LinkedList<CurveComponent>();
        private List<CurveComponent> afterComps  = new LinkedList<CurveComponent>();

        private MajorRegionMode beforeMode;
        private MajorRegionMode afterMode;

        private RegionEdit() {
            beforeComps = getCompsList();
            beforeMode = majorMode;
            XmlNode root = beforeDoc.getRoot();
            save(beforeComps, root);
            XmlNode modeNode = root.addChild(ModeTag);
            beforeMode.save(modeNode);
        }

        public void end() {
            afterComps = getCompsList();
            afterMode = majorMode;
            XmlNode root = afterDoc.getRoot();
            save(afterComps, root);
            XmlNode modeNode = root.addChild(ModeTag);
            afterMode.save(modeNode);
        }

        public String getPresentationName() {
            return LOCALE.get("RegionChangeEditName");
        }

        public void undo() {
            super.undo();
            if (minorMode != null) {
                throw new CannotUndoException();
            }
            try {
                restoreWithMode(beforeDoc, beforeComps, beforeMode);
                currentEdit = new RegionEdit();
            }
            catch (XMLException e) {
                CannotUndoException cue = new CannotUndoException();
                cue.initCause(e);
                throw cue;
            }
        }

        public void redo() {
            super.redo();
            if (minorMode != null) {
                throw new CannotRedoException();
            }
            try {
                restoreWithMode(afterDoc, afterComps, afterMode);
                currentEdit = new RegionEdit();
            }
            catch (XMLException e) {
                CannotRedoException cre = new CannotRedoException();
                cre.initCause(e);
                throw cre;
            }
        }

        // Make a List out of the CurveComponent Set, so that the save/restore
        // cycle will be deterministic:
        private List<CurveComponent> getCompsList() {
            Set<CurveComponent> comps = getAllComponents();
            List<CurveComponent> list = new LinkedList<CurveComponent>();
            for (CurveComponent comp : comps) {
                list.add(comp);
            }
            return list;
        }

        // We have our own version of setMinorMode() in RegionEdit, because
        // when there is undo/redo going on, the order of operations must be:
        // 1) old mode runs modeExited(); 2) restore occurs; 3) new mode
        // runs modeEntered().
        private void restoreWithMode(
            XmlDocument doc, List<CurveComponent> comps, MajorRegionMode newMode
        ) throws XMLException {
            isUndoing = true;

            XmlNode root = doc.getRoot();

            MajorRegionMode oldMode = majorMode;
            oldMode.modeExited();

            restore((List<CurveComponent>) comps, (XmlNode) root);

            XmlNode modeNode = root.getChild(ModeTag);
            newMode.restore(modeNode);

            majorMode = newMode;

            setEditingCurve(newMode.getEditingCurve());
            newMode.modeEntered();

            isUndoing = false;

            notifyModeChanged(oldMode, newMode);
        }
    }

    private final static String ComponentTag = "Region";
    private final static String CurveTag = "Contour";
    private final static String ReferenceTag = "Reference";
    private final static String IndexTag = "Index";

    // This save method preserves Curve information, but it saves nothing
    // about CurveComponents except their associations with Curves:

    void save(List<CurveComponent> comps, XmlNode node) {
        Set<Curve> curves = getAllCurves();
        Map<Curve, Integer> curveNumbers = new HashMap<Curve, Integer>(); // CubicCurves to Integers
        int count = 0;
        for (Curve curve : curves) {
            XmlNode curveNode = node.addChild(CurveTag);
            CurveFactory.save(curve, curveNode);
            curveNumbers.put(curve, count++);
        }
        for (CurveComponent comp : comps) {
            XmlNode compNode = node.addChild(ComponentTag);
            Set<Curve> compCurves = getCurves(comp);
            for (Curve compCurve : compCurves) {
                Integer index = curveNumbers.get(compCurve);
                XmlNode refNode = compNode.addChild(ReferenceTag);
                refNode.setAttribute(IndexTag, index.toString());
            }
        }
    }

    // This restore method assumes that the CurveComponents of this model
    // have already been consistently initialized by addComponent():

    void restore(List<CurveComponent> comps, XmlNode node) throws XMLException {
        restore(comps, node, true);
    }

    // This works just like restore() above, except it doesn't clear out
    // the preexisting model state.

    void addSaved(List<CurveComponent> comps, XmlNode node) throws XMLException {
        restore(comps, node, false);
    }

    private void restore(List<CurveComponent> comps, XmlNode node, boolean shouldRemoveCurves)
            throws XMLException {
        isRestoring = true;
        Map<Integer, Curve> curveNumbers = new HashMap<Integer, Curve>();
        int count = 0;
        XmlNode[] curveNodes = node.getChildren(CurveTag);
        for (XmlNode curveNode : curveNodes) {
            Curve curve = CurveFactory.restore(curveNode);
            curve.restore(curveNode);
            curveNumbers.put(count++, curve);
        }
        XmlNode[] compNodes = node.getChildren(ComponentTag);
        if (compNodes.length != comps.size()) {
            throw new XMLException(
                "The number of regions doesn't match the number of tools"
            );
        }
        if (shouldRemoveCurves) {
            removeCurves();
        }
        Iterator<CurveComponent> compIter = comps.iterator();
        for (XmlNode compNode : compNodes) {
            CurveComponent comp = compIter.next();
            XmlNode[] refNodes = compNode.getChildren(ReferenceTag);
            for (XmlNode refNode : refNodes) {
                String attr = refNode.getAttribute(IndexTag);
                Integer index;
                try {
                    index = Integer.valueOf(attr);
                }
                catch (NumberFormatException e) {
                    throw new XMLException("Expected a number: " + attr, e);
                }
                Curve curve = curveNumbers.get(index);
                if (curve == null) {
                    throw new XMLException("Not a valid curve index: " + attr);
                }
                addCurve(comp, curve);
            }
        }
        isRestoring = false;
    }
}
