/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.browser.ctrls.NavigationPane;
import com.lightcrafts.ui.datatips.xswing.DataTipManager;
import com.lightcrafts.ui.toolkit.MenuButton;
import com.lightcrafts.utils.directory.DirectoryMonitor;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Stack;

/**
 * A container for FolderTree stuff, including: selection navigation logic;
 * tree path save and restore access; sticky tree selection behavior; and a
 * scroll pane which holds the tree component itself.
 */
public class FolderBrowserPane
    extends JScrollPane implements TreeSelectionListener
{
    private FolderTree tree;

    private Stack<FolderTreeNode> backStack;

    private Stack<FolderTreeNode> forwardStack;

    private MenuButton pathButton;

    private boolean inSyntheticSelectionChange; // prevent recursion

    public FolderBrowserPane() {
        pathButton = new MenuButton();
        tree = new FolderTree();
        backStack = new Stack<FolderTreeNode>();
        forwardStack = new Stack<FolderTreeNode>();

        // setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LightZoneSkin.Colors.FrameBackground));
        setBorder(null);
        
        TreePath path = FolderPathsSaver.getLatestPath(tree);

        FolderPathsSaver.addToTree(tree);

        final Font treeFont = tree.getFont();
        tree.setFont(
            treeFont.deriveFont( (float)(treeFont.getSize() - 1) )
        );
        tree.setExpandsSelectedPaths( true );

        tree.addTreeSelectionListener(this);

        tree.setSelectionPath(path);

        tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        getViewport().setView(tree);
        
        if (Platform.getType() != Platform.MacOSX) {
            DataTipManager.get().register(tree);
        }
    }

    public boolean goToPicturesFolder() {
        return tree.goToPicturesFolder();
    }

    /**
     * Dispose of this <code>FolderBrowserPane</code>.
     */
    public synchronized void dispose() {
        if (Platform.getType() != Platform.MacOSX) {
            DataTipManager.get().unregister(tree);
        }
        tree.dispose();
        tree = null;
    }

    public DirectoryMonitor getDirectoryMonitor() {
        return ((FolderTreeModel) tree.getModel()).getDirectoryMonitor();
    }

    public MenuButton getPathPopupMenu() {
        return pathButton;
    }

    public void goBack() {
        if ( isBackAvailable() ) {
            FolderTreeNode node = tree.getSelectedNode();
            forwardStack.push(node);
            setSelectedNode(backStack.pop());
        }
    }

    public void goForward() {
        if ( isForwardAvailable() ) {
            FolderTreeNode node = tree.getSelectedNode();
            backStack.push(node);
            setSelectedNode(forwardStack.pop());
        }
    }

    public boolean isBackAvailable() {
        return ! backStack.empty();
    }

    public boolean isForwardAvailable() {
        return ! forwardStack.empty();
    }

    // The tree selection changed, so update the navigation controls and
    // notify listeners.
    public void valueChanged(TreeSelectionEvent e) {
        TreePath oldPath = e.getOldLeadSelectionPath();
        FolderTreeNode oldNode = (oldPath != null) ?
            (FolderTreeNode) oldPath.getLastPathComponent() : null;

        TreePath newPath = e.getNewLeadSelectionPath();
        FolderTreeNode newNode = (newPath != null) ?
            (FolderTreeNode) newPath.getLastPathComponent() : null;

        pathButton.clear();

        FolderTreeNode node = newNode;
        while (node != null) {
            String name = node.toString();
            Icon icon = node.getIcon();
            JMenuItem menuItem = new JMenuItem(name, icon);

            final FolderTreeNode menuCurrent = newNode;
            final FolderTreeNode menuSelection = node;
            final boolean modifyStack = newNode != node;
            menuItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (modifyStack) {
                            backStack.push(menuCurrent);
                            forwardStack.clear();
                        }
                        setSelectedNode(menuSelection);
                    }
                }
            );
            pathButton.add(menuItem);
            node = (FolderTreeNode) node.getParent();
        }
        if (! inSyntheticSelectionChange) {
            backStack.push(oldNode);
            forwardStack.clear();
        }
    }
    
    public void addSelectionListener(FolderTreeListener listener) {
        tree.addFolderTreeListener(listener);
    }

    public void removeSelectionListener(FolderTreeListener listener) {
        tree.removeFolderTreeListener(listener);
    }

    public File getSelectedFile() {
        FolderTreeNode node = tree.getSelectedNode();
        return (node != null) ? node.getFile() : null;
    }

    public void savePath(String key) {
        FolderPathsSaver.savePath(tree, key);
    }

    public void restorePath(String key) {
        FolderPathsSaver.restorePath(tree, key);
    }

    public static void clearPath(String key) {
        FolderPathsSaver.clearPath(key);
    }

    // Programmatically select the given tree node.  Used by the navigation
    // controls (forward, backward, and the path control).
    private void setSelectedNode(FolderTreeNode node) {
        inSyntheticSelectionChange = true;

        if (node != null) {
            TreePath path = node.getTreePath();
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        }
        else {
            tree.clearSelection();
        }
        inSyntheticSelectionChange = false;
    }

    public static void main(String[] args) {
        FolderBrowserPane browser = new FolderBrowserPane();
        browser.goToPicturesFolder();
        
        NavigationPane buttons = new NavigationPane(browser); // new NavigationButtons(browser);

        JFrame frame = new JFrame("Test");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(browser);
        frame.getContentPane().add(buttons, BorderLayout.NORTH);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        DirectoryMonitor monitor = browser.getDirectoryMonitor();
        monitor.resume(false);
    }
}
/* vim:set et sw=4 ts=4: */
