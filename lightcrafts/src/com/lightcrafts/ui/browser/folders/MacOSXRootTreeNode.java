/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import static com.lightcrafts.ui.browser.folders.Locale.LOCALE;
import com.lightcrafts.utils.directory.DirectoryMonitor;

import java.io.File;
import java.util.List;

/**
 * A FolderTreeNode that represents the special-case of the root folder for
 * Mac.  It's "/Volumes", with a special name and user.home as an extra child.
 */
final class MacOSXRootTreeNode extends FolderTreeNode {

    // Returns a friendler name for "/Volumes".
    public String toString() {
        return LOCALE.get("MacOSXRootName");
    }

    MacOSXRootTreeNode(NodeFileIndex index, DirectoryMonitor monitor) {
        super(new File("/Volumes"), null, index, monitor);
    }

    /**
     * Insert the user's home directory as the first child, so it's easy to
     * find.
     */
    void updateChildren() {
        super.updateChildren();
        File home = new File(System.getProperty("user.home"));
        List<FolderTreeNode> children = getChildren();
        NodeFileIndex index = getIndex();
        DirectoryMonitor monitor = getDirectoryMonitor();
        children.add(0, new FolderTreeNode(home, this, index, monitor));
    }
}
/* vim:set et sw=4 ts=4: */
