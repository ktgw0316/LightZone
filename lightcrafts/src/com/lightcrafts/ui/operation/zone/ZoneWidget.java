/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import com.lightcrafts.model.ZoneOperation;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import static com.lightcrafts.ui.operation.zone.Locale.LOCALE;

import javax.swing.*;
import java.awt.*;

/** We use a special JPanel to control the layout of Zones and their
 * controls.  It decides the vertical distribution of GradientFills based on
 * a ZoneModel.  It also uses a DragListener to let the Spacers in between
 * get dragged around and trigger updates to the layout.
 * <p>
 * This class uses z-order and non-opaque listener Components to figure out
 * mouse events and do direct manipulation.  Be careful about tampering with
 * Container child indices.
 */

class ZoneWidget extends JPanel implements ZoneModelListener {

    private ZoneModel model;
    private DragListener dragListener;

    private GradientFill[] fills;
    private Spacer[] spacers;

    private OpControl.OpControlUndoSupport undoSupport;
    private boolean batchEdit;      // Suppress undoSupport while true

    ZoneWidget(ZoneOperation op, OpControl.OpControlUndoSupport undoSupport) {
        this.undoSupport = undoSupport;

        Zone[] zones = Zone.getZones();

        model = new ZoneModel(op, zones.length);
        model.addZoneModelListener(this);

        dragListener = new DragListener();
        dragListener.setModel(model);

        setLayout(null);

        fills = new GradientFill[zones.length];
        spacers = new Spacer[zones.length + 1];
        Spacer.SpacerHandle knobHandle = new Spacer.SpacerHandle();
        for (int n=0; n<zones.length; n++) {
            Zone zone = zones[n];
            GradientFill fill = new GradientFill(zone);
            fills[n] = fill;
            if (n == 0) {
                // First time through, add the bottom-most Spacer:
                Spacer spacer = new Spacer(model, 0, knobHandle);
                spacers[0] = spacer;
                add(spacer, 0);
                dragListener.addSelfTo(spacer);
            }
            // Add the Component at the end:
            add(fill, -1);

            // Add a Spacer at the end:
            Spacer spacer = new Spacer(model, n + 1, knobHandle);
            spacers[n+1] = spacer;
            add(spacer, n + 1);
            dragListener.addSelfTo(spacer);
        }
        spacers[zones.length / 2].setFocusedKnob();

        setFocusCycleRoot(true);
        FocusTraversalPolicy focus = new SpacerTraversalPolicy();
        setFocusTraversalPolicy(focus);

        // Pass Spacer focus changes to the ZoneOperation:
        new SpacerMouseListener(op, spacers);
    }

    void operationChanged(ZoneOperation op) {
        batchEdit = true;
        model.operationChanged(op);
        batchEdit = false;
    }

    // Turn off mouse event handling in the Spacers, and fix the Spacer layout.
    // This is an irreversible mutation.
    void makeReference() {
        setEnabled(false);
        int count = (getComponentCount() + 1) / 2;
        for (int n=0; n<count; n++) {
            Spacer spacer = getSpacer(n);
            spacer.setEnabled(false);
            dragListener.removeSelfFrom(spacer);
        }
        revalidate();
        repaint();
    }

    public void zoneModelBatchStart(ZoneModelEvent event) {
        batchEdit = true;
    }

    public void zoneModelChanged(ZoneModelEvent event) {
        // layout depends on control points
        doLayout();
        repaint();
        if (! batchEdit) {
            undoSupport.postEdit(LOCALE.get("ZoneEditName"));
        }
    }

    public void zoneModelBatchEnd(ZoneModelEvent event) {
        undoSupport.postEdit(LOCALE.get("ZoneEditName"));
        batchEdit = false;
    }

    public void doLayout() {
        // First set the Spacer locations, which are critical:
        updateSpacerLayout();
        // Then slave the other Components to the Spacers:
        updateGradientFillLayout();
    }

    void save(XmlNode node) {
        model.save(node);
    }

    void restore(XmlNode node) throws XMLException {
        batchEdit = true;
        model.restore(node);
        batchEdit = false;
    }

    private void updateSpacerLayout() {
        int width = getSize().width - Spacer.SpacerOutcrop;
        for (int n=0; n<spacers.length; n++) {
            Spacer spacer = getSpacer(n);
            int outcrop = spacer.getOutcrop();
            spacer.setSize(width + outcrop, Spacer.SpacerHeight);
            int index = spacer.getIndex();
            double y;
            if (isEnabled()) {
                y = model.getValueAt(index);
            }
            else {
                // Someone called makeReference():
                y = index / ((double) spacers.length - 1);
            }
            ComponentScaler.scaleToComponent(
                spacer, Spacer.SpacerHeight / 2, y
            );
        }
    }

    private void updateGradientFillLayout() {
        int width = getSize().width;
        for (int n=0; n<fills.length; n++) {
            Component c = getGradientFill(n);
            int yMin = getGradientFillTop(n);
            int yMax = getGradientFillBottom(n);
            c.setLocation(0, yMin);
            if (isEnabled()) {
                c.setSize(width - Spacer.SpacerOutcrop, yMax - yMin);
            }
            else {
                c.setSize(width, yMax - yMin);
            }
        }
    }

    // Spacers are numbered like boundaries, including the outermost
    // boundaries, starting at zero.  There is a Spacer Component for index
    // zero, and also for the maximum index.

    private Spacer getSpacer(int index) {
        return spacers[index];
    }

    private int getSpacerMiddle(int index) {
        Spacer spacer = getSpacer(index);
        return spacer.getLocation().y + spacer.getSize().height / 2;
    }

    private GradientFill getGradientFill(int index) {
        return fills[index];
    }

    private int getGradientFillBottom(int index) {
        // The following "+1" makes these Components overlap a little
        // vertically, so the border of one clobbers the border of the other.
        // This achieves a one-pixel boundary between GradientFills:
        return getSpacerMiddle(index) + 1;
    }

    private int getGradientFillTop(int index) {
        return getSpacerMiddle(index + 1);
    }
}
