/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import java.io.File;
import java.util.List;

/**
 * Listeners may subscribe on a FolderBrowserPane to receive notifications
 * of changes to the lead selection in a FolderTree.
 */
public interface FolderTreeListener {

    void folderSelectionChanged(File folder);

    void folderDropAccepted(List<File> files, File folder);
}
