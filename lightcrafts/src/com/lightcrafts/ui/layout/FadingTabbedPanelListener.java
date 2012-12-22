/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.layout;

/**
 * Find out when a FadingTabbedPanel transitions from someting selected to
 * nothing selected; and from nothing to something.
 */
public interface FadingTabbedPanelListener {

    void somethingSelected();

    void tabSelected(String name);

    void nothingSelected();
}
