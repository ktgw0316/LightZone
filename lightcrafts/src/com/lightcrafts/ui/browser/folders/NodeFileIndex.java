/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import java.io.File;
import java.util.*;

// A lookup table for inverting FolderTreeNode.getFile().  Useful for
// restoring selections from preferences, and identifying the changed node
// when a file system modification is detected.

class NodeFileIndex {

    private Map<File, FolderTreeNode> index;

    NodeFileIndex() {
        index = new HashMap<File, FolderTreeNode>();
    }

    void add(FolderTreeNode node) {
        File file = node.getFile();
        index.put(file, node);
    }

    FolderTreeNode get(File file) {
        return index.get(file);
    }

    void remove(FolderTreeNode node) {
        File file = node.getFile();
        index.remove(file);
    }
}
