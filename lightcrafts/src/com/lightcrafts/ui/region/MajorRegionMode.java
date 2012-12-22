/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.utils.xml.XmlNode;
import com.lightcrafts.utils.xml.XMLException;

/**
 * An abstract base class for RegionModes that can be the target of undo
 * or redo transitions.  These must be able to save their state, so it can be
 * restored later.
 */
abstract class MajorRegionMode extends RegionMode {

    /**
     * For use only by the NewCurveMode constructor.
     */
    MajorRegionMode(RegionMode mode) {
        super(mode);
    }

    MajorRegionMode(RegionModel model, CurveComponent comp) {
        super(model, comp);
    }

    /**
     * Useful for registering key bindings and controlling batch levels on
     * transitions.
     */
    abstract void modeEntered();

    /**
     * Useful for unregistering key bindings and controlling batch levels on
     * transitions.
     */
    abstract void modeExited();

    /**
     * Save the state of this MajorRegionMode before posting an undoable
     * edit.
     */
    abstract void save(XmlNode node);

    /**
     * Restore the state of this MajorRegionMode when undoing or redoing
     * an edit.
     * <p>
     * As part of undo, all the RegionModel's Curves are
     * reinstantiated.  If this MajorRegionMode is holding references to
     * Curves or Curve control points, these references MUST be updated to
     * instances in the current RegionModel.  See
     * RegionModel.getRestoredCurve().
     */
    abstract void restore(XmlNode node) throws XMLException;

    /**
     * The "editing Curve" in the RegionModel may need updating on
     * transitions among MajorRegionModes.
     */
    abstract Curve getEditingCurve();
}
