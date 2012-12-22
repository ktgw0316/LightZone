/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

/**
 * Notify listeners of changes to the selection state in an OpStack.
 */

public interface OpStackListener {

    /**
     * A new OpControl has appeared.
     * @param control
     */
    void opAdded(OpControl control);

    /**
     * A preexisting OpControl has become selected.
     * @param control The selected OpControl.
     */
    void opChanged(OpControl control);

    /**
     * The current selection is not an OpControl.
     * @param control The current SelectedControl.
     */
    void opChanged(SelectableControl control);

    /**
     * The current selection has toggled between locked and unlocked.
     */
    void opLockChanged(OpControl control);

    /**
     * An OpControl has been deleted.
     */
    void opRemoved(OpControl control);
}
