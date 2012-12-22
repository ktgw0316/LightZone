/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

/**
 * When the application decides to save a document, if the editor's crop or
 * rotate mode is active, then the temporary mode state (the crop bounds the
 * user sees) must be committed to the engine while the save is happening, to
 * make sure that the thumbnail, preview, and LZN all match what the user
 * sees in the mode.
 * <p>
 * When the application notifies the editor that save is occurring, the
 * editor commits any active crop or rotate mode and returns what its state
 * was before the commit in a TemporaryEditorCommitState.  Then the
 * application submits this same TemporaryEditorCommitState when it notifies
 * the editor that the save is done.
 */
public class TemporaryEditorCommitState {

    boolean isCropActive;

    boolean isRotateActive;

    TemporaryEditorCommitState(boolean rotate, boolean crop) {
        isRotateActive = rotate;
        isCropActive = crop;
    }
}
