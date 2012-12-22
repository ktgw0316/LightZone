/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.layout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.Preferences;

/**
 * A JSplitPane with a fixed style that respects the minimum and maximum sizes
 * of its children, can swap its two child Components, can swap its orientation,
 * can show and hide its children, and remembers these settings as well as its
 * divider location between VM instances.
 */
public class SmartSplitPane extends JSplitPane {

    // Remember the split pane divider locations
    private final static Preferences Prefs = Preferences.userNodeForPackage(
        SmartSplitPane.class
    );

    // Constants for constructing Preferences key Strings
    private final static String LocationKey = "Location";
    private final static String HideLeftKey = "HideLeft";
    private final static String HideRightKey = "HideRight";
    private final static String HiddenLocKey = "HiddenLocation";

    // A Preferences key for the divider location at initialization
    private String prefsKey;

    // Either of the children may be hidden, in the manner of the JSplitPane
    // oneTouchExpandable property.
    private boolean hideLeft;
    private boolean hideRight;
    private int hiddenDividerLoc;

    private Component hidden;

    // Either LEFT, RIGHT, TOP or BOTTOM, to indicate which of the two
    // children should have its maximum size be respected when the divider
    // location is constrained; or null, meaning that the minimum sizes of
    // both should be respected.
    private String respectedSide;

    /**
     * Define a SplitTreeNode that will use the given String key in reading
     * and writing Preference values.
     */
    public SmartSplitPane(String prefsKey, String respectedSide) {
        this.prefsKey = prefsKey;
        this.respectedSide = respectedSide;
        setContinuousLayout(true);
        setBorder(null);
        // This divider size matches values in PreviewSplit and TemplateSplit.
        setDividerSize(4);
        if (respectedSide != null) {
            // Add the daemon that adjusts minimum size of the opposite child
            // in order to enforce the maximum size of the respected child.
            addComponentListener();
        }
    }

    /**
     * This method from the base class is overridden so the argument may be
     * copied into Preferences for restoring later.
     */
    public void setDividerLocation(int i) {
        super.setDividerLocation(i);
        Prefs.putInt(prefsKey + LocationKey, i);
    }

    /**
     * Update the left child component, without moving the divider.
     */
    public void setLeftFrozenDivider(JComponent comp) {
        int loc = getDividerLocation();
        setLeftComponent( comp );
        super.setDividerLocation(loc);
    }

    /**
     * Update the right child component, without moving the divider.
     */
    public void setRightFrozenDivider(JComponent comp) {
        int loc = getDividerLocation();
        setRightComponent(comp);
        super.setDividerLocation(loc);
    }

    /**
     * Call this when it is the time to restore the orientation and divider
     * location from Preferences: after the SplitTreeNode has been set up,
     * had its children defined, resize weights adjusted, etc.
     */
    public void restoreFromPrefs(int defaultDivider, int defaultOrientation) {
        int dividerLoc =
            Prefs.getInt(prefsKey + LocationKey, defaultDivider);
        if (dividerLoc >= 0) {
            super.setDividerLocation(dividerLoc);
        }
        super.setOrientation(defaultOrientation);

        hideLeft = Prefs.getBoolean(prefsKey + HideLeftKey, false);
        hideRight = Prefs.getBoolean(prefsKey + HideRightKey, false);
        hiddenDividerLoc =
            Prefs.getInt(prefsKey + HiddenLocKey, defaultDivider);
        if (hideLeft) {
            hidden = getLeftComponent();
            remove(hidden);
        }
        if (hideRight) {
            hidden = getRightComponent();
            remove(hidden);
        }
        if (hideLeft || hideRight) {
            setDividerSize(0);
        }
    }

    // Make sure the min/max size constraints are satisfied by the current
    // divider position.  (These may become violated after restoring from
    // preferences or changes in properties of the respected component.)
    public void checkConstraints() {
        Component respected = getRespectedChild();
        if (respected == null) {
            // The respected child may be hidden.
            return;
        }
        if (respectedSide == null) {
            // We could be in the symmetrical configuration.
            return;
        }
        // Component respected = getRespectedChild();
        if (respected == null) {
            // The respected child may be hidden.
            return;
        }
        Dimension size = respected.getSize();
        Dimension min = respected.getMinimumSize();
        Dimension max = respected.getMaximumSize();
        switch (getOrientation()) {
            case VERTICAL_SPLIT:
                int minH = min.height;
                int maxH = max.height;
                if ((size.height < minH) || (size.height > maxH)) {
                    if (respected == getTopComponent()) {
                        setDividerLocation(minH + getDividerSize());
                    }
                    else {
                        setDividerLocation(
                            getSize().height - minH - getDividerSize()
                        );
                    }
                    updateOppositeMinSize();
                }
                break;
            case HORIZONTAL_SPLIT:
                int minW = min.width;
                int maxW = max.width;
                if ((size.width < minW) || (size.width > maxW)) {
                    if (respected == getLeftComponent()) {
                        setDividerLocation(minW + getDividerSize());
                    }
                    else {
                        setDividerLocation(
                            getSize().width - minW - getDividerSize()
                        );
                    }
                    updateOppositeMinSize();
                }
                break;
        }
    }

    /**
     * Find out whether the left component has been hidden.
     */
    public boolean isHiddenLeft() {
        return hideLeft;
    }

    /**
     * Find out whether the right component has been hidden.
     */
    public boolean isHiddenRight() {
        return hideRight;
    }

    /**
     * Set the split pane divider location so that the left component has
     * zero size, remembering the current location for unhide().
     */
    public void hideLeft() {
        if (hideLeft) {
            return;
        }
        if (hideRight) {
            unhide();
        }
        hiddenDividerLoc = getDividerLocation();

        hidden = getLeftComponent();
        remove(hidden);

        setDividerLocation(0d);
        setDividerSize(0);
        hideLeft = true;
        hideRight = false;

        Prefs.putBoolean(prefsKey + HideLeftKey, hideLeft);
        Prefs.putBoolean(prefsKey + HideRightKey, hideRight);
        Prefs.putInt(prefsKey + HiddenLocKey, hiddenDividerLoc);
    }

    /**
     * Set the split pane divider location so that the right component has
     * zero size, remembering the current location for unhide().
     */
    public void hideRight() {
        if (hideRight) {
            return;
        }
        if (hideLeft) {
            unhide();
        }
        hiddenDividerLoc = getDividerLocation();

        hidden = getRightComponent();
        remove(hidden);

        setDividerLocation(1d);
        setDividerSize(0);
        hideLeft = false;
        hideRight = true;

        Prefs.putBoolean(prefsKey + HideLeftKey, hideLeft);
        Prefs.putBoolean(prefsKey + HideRightKey, hideRight);
        Prefs.putInt(prefsKey + HiddenLocKey, hiddenDividerLoc);
    }

    /**
     * Undo the effects of hideLeft() and hideRight().
     */
    public void unhide() {
        if (hideLeft || hideRight) {
            if (hideRight) {
                setRightComponent(hidden);
            }
            if (hideLeft) {
                setLeftComponent(hidden);
            }
            setDividerLocation(hiddenDividerLoc);
            setDividerSize(4);
            hideLeft = false;
            hideRight = false;
            validate();
        }
        Prefs.putBoolean(prefsKey + HideLeftKey, hideLeft);
        Prefs.putBoolean(prefsKey + HideRightKey, hideRight);
        Prefs.putInt(prefsKey + HiddenLocKey, hiddenDividerLoc);
    }

    /**
     * If this is a horizontal split, make it vertical.  If it's vertical,
     * make it horizontal.  The new setting gets written to Preferences.
     */
    public void toggleOrientation() {
        int orientation = getOrientation();
        switch (orientation) {
            case HORIZONTAL_SPLIT:
                setOrientation(VERTICAL_SPLIT);
                break;
            case VERTICAL_SPLIT:
                setOrientation(HORIZONTAL_SPLIT);
                break;
        }
    }

    /**
     * Add the ComponentListener that monitors size changes of this split pane
     * and updates the minimum size of the "opposite" child so that the divider
     * location constraint will respect the maximum size of the "respected"
     * child.
     */
    private void addComponentListener() {
        addComponentListener(
            new ComponentAdapter() {
                public void componentResized(ComponentEvent event) {
                    updateOppositeMinSize();
                }
            }
        );
    }

    public void updateOppositeMinSize() {
        if (isHiddenLeft() || (isHiddenRight())) {
            return;
        }
        Component respected = getRespectedChild();
        Component opposite = getOppositeChild();
        Dimension size = getSize();
        Dimension min = opposite.getMinimumSize();
        Dimension max = respected.getMaximumSize();
        switch (getOrientation()) {
            case HORIZONTAL_SPLIT:
                min.width = size.width - max.width;
                opposite.setMinimumSize(min);
                break;
            case VERTICAL_SPLIT:
                min.height = size.height - max.height;
                opposite.setMinimumSize(min);
                break;
        }
    }

    /**
     * Find out which child should have its maximum size respected by the
     * split pane divider location constraint.
     */
    private Component getRespectedChild() {
        if (respectedSide == null) {
            return null;
        }
        if (respectedSide.equals(TOP)) {
            return getTopComponent();
        }
        if (respectedSide.equals(BOTTOM)) {
            return getBottomComponent();
        }
        if (respectedSide.equals(LEFT)) {
            return getLeftComponent();
        }
        if (respectedSide.equals(RIGHT)) {
            return getRightComponent();
        }
        assert false : "Illegal SmartSplitPane respectedSide: " + respectedSide;
        return null;
    }

    /**
     * Find out which child should allow its minimum size to be slaved to the
     * split pane divider location constraint.
     */
    private Component getOppositeChild() {
        if (respectedSide == null) {
            return null;
        }
        if (respectedSide.equals(TOP)) {
            return getBottomComponent();
        }
        if (respectedSide.equals(BOTTOM)) {
            return getTopComponent();
        }
        if (respectedSide.equals(LEFT)) {
            return getRightComponent();
        }
        if (respectedSide.equals(RIGHT)) {
            return getLeftComponent();
        }
        assert false : "Illegal SmartSplitPane respectedSide: " + respectedSide;
        return null;
    }
}
