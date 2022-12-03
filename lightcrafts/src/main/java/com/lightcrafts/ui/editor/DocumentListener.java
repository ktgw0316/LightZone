/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

/**
 * A listener that can learn when a Document has become dirty or clean.
 * Useful for updating frame titles and the red close button on the Mac.
 */
public interface DocumentListener {

    void documentChanged(Document doc, boolean isDirty);
}
