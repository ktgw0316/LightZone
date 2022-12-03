/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.model.Contour;
import com.lightcrafts.model.Region;
import com.lightcrafts.ui.mode.Mode;
import com.lightcrafts.ui.action.ToggleAction;
import static com.lightcrafts.ui.region.Locale.LOCALE;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import javax.swing.event.UndoableEditListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

/** A container for two kinds of Component: CurveComponents, and a single
  * given underlay.  The CurveComponent regions are drawn on top but are not
  * opaque.  All child Components have bounds set to the preferred size of the
  * underlay.
  * <p>
  * A cookie mechanism is provided so that an arbitrary number of different
  * CurveComponents can be remembered and swapped around.  Only one can be
  * active at a time.
  * <p>
  * The current overlay CurveComponent will intercept all MouseEvents.  There
  * is no hope of the underlay getting any user events.
  */

public class RegionOverlay extends JPanel implements Mode {

    private CurveComponent overlay;
    private Map overlays;
    private Rectangle underlayBounds;
    private RegionModel model;
    private RegionChangeMulticaster multicaster;
    private AffineTransform xform;
    private boolean m_entered;

    private Collection mouseListeners;  // External MouseMotionListeners

    private ShowHideRegionsAction showHideToggle; // Can show/hide the overlays

    public RegionOverlay() {
        setLayout(null);
        overlays = new HashMap();
        model = new RegionModel(this);
        showHideToggle = new ShowHideRegionsAction(this);
        multicaster = new RegionChangeMulticaster();
        mouseListeners = new LinkedList();
        addPopupListener();
    }

    public void enter() {
        m_entered = true;
    }

    public void exit() {
        m_entered = false;
    }

    public boolean isIn() {
        return m_entered;
    }
    
    public ToggleAction getShowHideAction() {
        return showHideToggle;
    }

    public void finishEditingCurve() {
        if (overlay != null)
            overlay.finishEditingCurve();
    }

    // Should be called only from ShowHideRegionsAction, to maintain
    // consistency among all the controls for this feature.
    void setRegionsVisible(boolean visible) {
        if (visible) {
            addOverlay();
        }
        else {
            removeOverlay();
        }
    }

    public void setCurveType(int type) {
        model.setNewCurveType(type);
    }

    // Accessors for states that are hooked to menu items:

    public int getCurveType() {
        return model.getNewCurveType();
    }

    // End accessors for states hooked to menu items.

    public void setCookie(Object cookie) {
        setCookie(cookie, false, false);
    }

    public void setCookie(Object cookie, boolean clonePts, boolean spotCurves) {
        if (overlay != null) {
            if (showHideToggle.isRegionsVisible()) {
                model.setMajorMode(new NewCurveMode(model, overlay));
                removeOverlay();
            }
        }
        CurveSelection selection = model.getSelection();
        selection.setCookie(cookie);
        if (cookie == null) {
            overlay = null;
            return;
        }
        if (overlays.containsKey(cookie)) {
            overlay = (CurveComponent) overlays.get(cookie);
        }
        else {
            overlay = new CurveComponent(model, clonePts, spotCurves);
            overlay.setOpaque(false);
            overlays.put(cookie, overlay);
            overlay.setCookie(cookie);
            model.addComponent(overlay);
        }
        model.setMajorMode(new NewCurveMode(model, overlay));

        if (xform != null) {
            overlay.setTransform(xform);
        }
        if (showHideToggle.isRegionsVisible()) {
            addOverlay();
        }
    }

    public boolean hasCookie(Object cookie) {
        return overlays.containsKey(cookie);
    }

    public boolean isCookie(Object cookie) {
        return cookie.equals(overlay.getCookie());
    }

    public Region getRegion(Object cookie) {
        if (cookie != null) {
            CurveComponent comp = (CurveComponent) overlays.get(cookie);
            Set curves = model.getCurves(comp);
            if (! curves.isEmpty()) {
                ArrayList contours = new ArrayList();
                for (Iterator i=curves.iterator(); i.hasNext(); ) {
                    Curve curve = (Curve) i.next();
                    Contour contour = model.getContour(curve);
                    contours.add(contour);
                }
                Region region = new ContourRegion(contours);
                return region;
            }
        }
        return null;
    }

    public Collection getShapes(Object cookie) {
        if (cookie == null) {
            return null;
        }
        CurveComponent comp = (CurveComponent) overlays.get(cookie);
        Set curves = model.getCurves(comp);
        Set shapes = new HashSet();
        for (Iterator i=curves.iterator(); i.hasNext(); ) {
            Curve curve = (Curve) i.next();
            if (curve.isValidShape()) {
                shapes.add(curve);
            }
        }
        return shapes;
    }

    public Collection getAllShapes() {
        Set curves = model.getAllCurves();
        Set shapes = new HashSet();
        for (Iterator i=curves.iterator(); i.hasNext(); ) {
            Curve curve = (Curve) i.next();
            if (curve.isValidShape()) {
                shapes.add(curve);
            }
        }
        return shapes;
    }

    public void shareShape(Object cookie, SharedShape shape, boolean clone) {
        Curve curve = (Curve) shape;
        if (clone) {
            curve = (Curve) curve.clone();
        }
        CurveComponent comp = (CurveComponent) overlays.get(cookie);
        model.addCurve(comp, curve);
    }

    public void unShareShape(Object cookie, SharedShape shape) {
        Curve curve = (Curve) shape;
        CurveComponent comp = (CurveComponent) overlays.get(cookie);
        model.removeCurve(comp, curve);
    }

    public void removeShape(SharedShape shape) {
        model.removeCurve((Curve) shape);
    }

    /** This method saves CubicCurves and their associations with cookie
      * Objects as identified by their order in the argument.  It saves no
      * information about the cookies.
      */
    public void save(List cookies, XmlNode node) {
        LinkedList comps = new LinkedList();
        for (Iterator i=cookies.iterator(); i.hasNext(); ) {
            CurveComponent comp = (CurveComponent) overlays.get(i.next());
            comps.add(comp);
        }
        model.save(comps, node);
    }

    /**
     * This method restores CubicCurves and reforms their assocations with
     * cookie Objects as identified by their order in the argument.  The
     * cookies must already be restored and added to the overlay before this
     * is called.
     */
    public void restore(List cookies, XmlNode node) throws XMLException {
        LinkedList comps = new LinkedList();
        for (Iterator i=cookies.iterator(); i.hasNext(); ) {
            CurveComponent comp = (CurveComponent) overlays.get(i.next());
            comps.add(comp);
        }
        model.restore(comps, node);
    }

    /**
     * This works like restore(), except it doesn't clear out existing cookies
     * or region data.
     */
    public void addSaved(List cookies, XmlNode node) throws XMLException {
        LinkedList comps = new LinkedList();
        for (Iterator i=cookies.iterator(); i.hasNext(); ) {
            CurveComponent comp = (CurveComponent) overlays.get(i.next());
            comps.add(comp);
        }
        model.addSaved(comps, node);
    }

    public void setTransform(AffineTransform xform) {
        this.xform = xform;
        if (overlay != null) {
            overlay.setTransform(xform);
        }
    }

    public void setUnderlayBounds(Rectangle bounds) {
        underlayBounds = bounds;
    }

    public Rectangle getUnderlayBounds() {
        return underlayBounds;
    }

    public void doLayout() {
        if (overlay != null) {
            Dimension size = getSize();
            overlay.setLocation(0, 0);
            overlay.setSize(size);
        }
    }

    void regionBatchStart(Collection comps) {
        Collection cookies = getCookies(comps);
        multicaster.regionBatchStart(cookies);
    }

    void regionChanged(Collection comps, Curve curve) {
        if (curve.isValidShape()) {
            Collection cookies = getCookies(comps);
            multicaster.regionChanged(cookies, curve);
        }
    }

    void regionsBatchEnd(Collection comps) {
        Collection cookies = getCookies(comps);
        multicaster.regionBatchEnd(cookies);
    }

    private Collection getCookies(Collection comps) {
        Set cookies = new HashSet();
        for (Iterator i=comps.iterator(); i.hasNext(); ) {
            CurveComponent comp = (CurveComponent) i.next();
            Object cookie = comp.getCookie();
            cookies.add(cookie);
        }
        return cookies;
    }

    public void addRegionListener(RegionListener listener) {
        multicaster.add(listener);
    }

    public void removeRegionListener(RegionListener listener) {
        multicaster.remove(listener);
    }

    public void addSelectionListener(CurveSelection.Listener listener) {
        CurveSelection selection = model.getSelection();
        selection.addSelectionListener(listener);
    }

    public void removeSelectionListener(CurveSelection.Listener listener) {
        CurveSelection selection = model.getSelection();
        selection.removeSelectionListener(listener);
    }

    public void addUndoableEditListener(UndoableEditListener listener) {
        model.addUndoableEditListener(listener);
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
        model.removeUndoableEditListener(listener);
    }

    private void addOverlay() {
        if (overlay != null) {
            add(overlay);
            for (Iterator i=mouseListeners.iterator(); i.hasNext(); ) {
                MouseInputListener listener = (MouseInputListener) i.next();
                overlay.addMouseListener(listener);
                overlay.addMouseMotionListener(listener);
            }
            validate();
            repaint();
        }
    }

    private void removeOverlay() {
        if (overlay != null) {
            remove(overlay);
            for (Iterator i=mouseListeners.iterator(); i.hasNext(); ) {
                MouseInputListener listener = (MouseInputListener) i.next();
                overlay.removeMouseListener(listener);
                overlay.removeMouseMotionListener(listener);
            }
            Rectangle bounds = overlay.getBounds();
            repaint(bounds);
        }
    }

    // A mouse listener for our popup, which just offers to unhide the overlay.
    private void addPopupListener() {
        addMouseListener(
            new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    if (event.isPopupTrigger()) {
                        showPopup(event);
                    }
                }
                public void mouseReleased(MouseEvent event) {
                    if (event.isPopupTrigger()) {
                        showPopup(event);
                    }
                }
            }
        );
    }

    private void showPopup(MouseEvent event) {
        if (event.isPopupTrigger() && ! showHideToggle.getState()) {
            JMenuItem item = new JMenuItem(LOCALE.get("ShowRegionsMenuItemText"));
            item.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        showHideToggle.setState(true);
                    }
                }
            );
            JPopupMenu popup = new JPopupMenu();
            popup.add(item);
            popup.show(event.getComponent(), event.getX(), event.getY());
        }
    }

    public JComponent getOverlay() {
        return this;
    }

    public void addMouseInputListener(MouseInputListener listener) {
        mouseListeners.add(listener);
        if (overlay != null) {
            overlay.addMouseListener(listener);
            overlay.addMouseMotionListener(listener);
        }
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    public void removeMouseInputListener(MouseInputListener listener) {
        mouseListeners.remove(listener);
        if (overlay != null) {
            overlay.removeMouseListener(listener);
            overlay.removeMouseMotionListener(listener);
        }
        removeMouseListener(listener);
        removeMouseMotionListener(listener);
    }

    public boolean wantsAutocroll() {
        return true;
    }

    public void dispose() {
        // nothing to dispose of
    }
}
