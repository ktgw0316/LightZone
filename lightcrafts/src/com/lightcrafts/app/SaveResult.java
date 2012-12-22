/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

/**
 * A little model for the outcome of a save action.
 */
enum SaveResult {
    Saved,          // The user said save, and save happened
    DontSave,       // The user said don't save, so the action should continue
    CouldntSave,    // The user said save, but save didn't work out
    Cancelled       // The user said to abort the action that led to a save
}
