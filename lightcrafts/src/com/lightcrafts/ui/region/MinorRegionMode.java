/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

/**
 * An abstract RegionMode serving as a marker base class to indicate that
 * everything a RegionMode does occurs between balanced batch calls for
 * undoable edit and change notification.
 * <p>
 * Extended by ClonePointMode, InnerCurveMode, FollowMouseOnceMode,
 * MoveCurveMode, and MoveEditingCurveMode.
 */

abstract class MinorRegionMode extends RegionMode {

    MinorRegionMode(RegionMode mode) {
        super(mode);
    }
}
