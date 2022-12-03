/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.prefs.Preferences;

/**
 * A <code>FolderPathSaver</code> is used to save and restore the path to the
 * selected folder so that the next time the application is launched, the
 * saved path is reselected.
 *
 * @author Anton Kast [anton@lightcrafts.com]
 */
class FolderPathsSaver {

    ////////// package ////////////////////////////////////////////////////////

    static void addToTree(JTree tree) {
        tree.addTreeSelectionListener(m_listener);
    }

    /**
     * Delete the bookmark with the given key.
     */
    static void clearPath(String pathKey) {
        String name;
        int n = 0;
        do {
            String key = PrefsKey + pathKey + n++;
            name = Prefs.get(key, null);
            if (name != null) {
                Prefs.remove(key);
            }
        } while (name != null);
    }

    static TreePath getLatestPath(FolderTree tree) {
        return getLatestPath(tree, "");
    }

    static void removeFromTree(JTree tree) {
        tree.removeTreeSelectionListener(m_listener);
    }

    /**
     * Restore to a bookmark previously saved in savePath().
     */
    static void restorePath(FolderTree tree, String key) {
        TreePath path = getLatestPath(tree, key);
        if (path != null) {
            tree.setSelectionPath(path);
        }
    }

    /**
     * Bookmark the currently selected path in the given tree.  If there is
     * no currently selected path, this does nothing.
     */
    static void savePath(FolderTree tree, String key) {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            updatePath(path, key);
        }
    }

    ////////// private ////////////////////////////////////////////////////////

    private static TreePath getLatestPath(FolderTree tree, String pathKey) {
        final LinkedList<String> names = new LinkedList<>();
        String name;
        int n = 0;
        do {
            name = Prefs.get(PrefsKey + pathKey + n++, null);
            if (name != null) {
                names.addFirst(name);
            }
        } while (name != null);

        if (names.isEmpty()) {
            return null;
        }
        final ArrayList<TreeNode> nodes = new ArrayList<>();
        FolderTreeNode node = tree.getRoot();
        final Iterator i = names.iterator();
        if (!node.toString().equals(i.next())) {
            return null;
        }
        nodes.add(node);
        while (i.hasNext()) {
            name = (String)i.next();
            node = node.getChildByName(name);
            if (node != null) {
                nodes.add(node);
            } else {
                break;
            }
        }
        return new TreePath(nodes.toArray());
    }

    private static void updatePath(TreePath path, String pathKey) {
        int n = 0;
        String key = PrefsKey + pathKey;
        while (path != null) {
            FolderTreeNode node = (FolderTreeNode)path.getLastPathComponent();
            Prefs.put(key + n++, node.toString());
            path = path.getParentPath();
        }
        // Clear out residual path from the last update
        key = PrefsKey + pathKey + n++;
        while (Prefs.get(key, null) != null) {
            Prefs.remove(key);
            key = PrefsKey + pathKey + n++;
        }
    }

    private final static Preferences Prefs =
        Preferences.userNodeForPackage(FolderPathsSaver.class);

    private final static String PrefsKey = "BrowserTreePath";

    private final static TreeSelectionListener m_listener =
            e -> updatePath(e.getPath(), "");
}
/* vim:set et sw=4 ts=4: */
