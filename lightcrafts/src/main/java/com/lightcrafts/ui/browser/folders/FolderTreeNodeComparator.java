/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import java.text.Collator;
import java.util.Comparator;

/**
 * A <code>FolderTreeNodeComparator</code> is-a {@link Comparator} for
 * comparing {@link FolderTreeNode}s for the purposes of sorting.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @author Fabio RIccardi [fabio@lightcrafts.com]
 */
final class FolderTreeNodeComparator implements Comparator<FolderTreeNode> {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Compares two {@link FolderTreeNode}s' filesystem names in a
     * case-insensitive manner.
     *
     * @param f1 The first {@link FolderTreeNode}.
     * @param f2 The second {@link FolderTreeNode}.
     * @return a negative integer, zero, or a positive integer as the first
     * {@link FolderTreeNode} is less than, equal to, or greater than the
     * second.
     */
    public int compare( FolderTreeNode f1, FolderTreeNode f2 ) {
        final String s1 = f1.toString().toLowerCase();
        final String s2 = f2.toString().toLowerCase();
        return Collator.getInstance().compare( s1, s2 );
    }

    ////////// package ////////////////////////////////////////////////////////

    /** The singleton instance. */
    static final FolderTreeNodeComparator INSTANCE =
        new FolderTreeNodeComparator();

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of
     * <code>FolderTreeNodeComparator</code>.
     */
    private FolderTreeNodeComparator() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */