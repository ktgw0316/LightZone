/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor.assoc;

import java.io.File;

/**
 * Listeners may find out when the set of Document files associated with a
 * particular image File has changed.
 */

public interface DocumentDatabaseListener {

    void docFilesChanged(File imageFile);
}
