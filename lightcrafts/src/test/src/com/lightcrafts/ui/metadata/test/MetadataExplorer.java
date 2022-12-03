/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata.test;

import com.lightcrafts.platform.Platform;

import javax.swing.*;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.io.File;

public class MetadataExplorer {

    public static void main(String[] args) {
        // Find a file system root to explore:
        File root;
        if (args.length > 0) {
            root = new File(args[0]);
        }
        else {
            String dir = System.getProperty("user.dir");
            root = new File(dir);
        }
        // The ImageInfo thumbnails mechanism needs dcraw for raw files:
        System.loadLibrary("DCRaw");
        if ( Platform.isMac() ) {
            System.loadLibrary("MacOSX");
        }

        // Make a JTree to represent the files:
        TreeNode rootNode = new FileNode(root);
        JTree tree = new JTree(rootNode);

        // Enforce a single-row selection model on the tree:
        TreeSelectionModel selectModel = new DefaultTreeSelectionModel();
        selectModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setSelectionModel(selectModel);

        // The metadata display:
        DirectoryUpdater dirs = new DirectoryUpdater(tree);

        // The thumbnail display:
        ThumbnailUpdater thumbs = new ThumbnailUpdater(tree);

        // The preview display:
        PreviewUpdater previews = new PreviewUpdater(tree);

        // The file tree and the metadata tables go in a split pane:
        JSplitPane split = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), dirs
        );

        // The thumbnails and previews go in their own frames:
        JFrame thumbFrame = new JFrame("Thumbnails");
        thumbFrame.setContentPane(thumbs);
        thumbFrame.setLocation(700, 20);
        thumbFrame.pack();
        JFrame previewFrame = new JFrame("Previews");
        previewFrame.setContentPane(previews);
        previewFrame.setLocation(700, 200);
        previewFrame.pack();

        // Show a frame holding all this stuff:
        JFrame frame = new JFrame("Metadata Explorer");
        frame.setContentPane(split);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(0, 20);
        frame.setSize(600, 800);

        previewFrame.setVisible(true);
        thumbFrame.setVisible(true);
        frame.setVisible(true);
    }
}
