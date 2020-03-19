/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.layout;

import javax.swing.*;

/**
 * A fading tab is specified by a triplet: a component to show and hide,
 * a name for the button text, and tooltip text
 */
public class FadingTabConfiguration {

    public JComponent comp;
    public String name;
    public String tip;

    public FadingTabConfiguration(
        JComponent comp, String name, String tip
    ) {
        this.comp = comp;
        this.name = name;
        this.tip = tip;
    }
}
