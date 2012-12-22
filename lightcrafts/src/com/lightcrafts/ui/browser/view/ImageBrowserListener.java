/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

/**
 * Learn about mouse interaction with images in an AbstractImageBrowser.
 */
public interface ImageBrowserListener {

    /**
     * The user clicked on an entry.
     */
    void selectionChanged(ImageBrowserEvent event);

    /**
     * The user double-clicked on an entry.
     */
    void imageDoubleClicked(ImageBrowserEvent event);

    /**
     * Something went wrong that the user should know about.
     */
    void browserError(String message);
}
