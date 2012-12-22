/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit.journal;

interface JournalListener {

    void journalStarted(boolean replay);

    void journalEvent(int count, boolean replay);

    void journalEnded(boolean replay);
}
