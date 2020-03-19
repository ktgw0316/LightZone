/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.ui.toolkit.CoolButton;

import javax.swing.*;

/**
 * A button that does something at the ComboFrame level, interacting with
 * top-level components and displayed in a LayoutHeader.
 */
abstract class FrameButton extends CoolButton {

    private ComboFrame frame;

    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    protected FrameButton(ComboFrame frame, String text) {
        setText(text);
        this.frame = frame;
        updateButton();
    }

    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    protected FrameButton(ComboFrame frame, Icon icon) {
        setIcon(icon);
        this.frame = frame;
        updateButton();
    }

    public ComboFrame getComboFrame() {
        return frame;
    }

    // Called when things in the frame change (folder, document, etc.)
    abstract void updateButton();
}
