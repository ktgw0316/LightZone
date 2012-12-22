/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata.test;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.ui.ExceptionDisplay;
import com.lightcrafts.ui.metadata.DirectoryStack;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.io.File;

// Listen for selection changes on the MetadataExplorerer tree, and update
// a metadata display or show the exception.

class DirectoryUpdater extends JScrollPane implements TreeSelectionListener {

    private DirectoryStack dirs;

    DirectoryUpdater(final JTree tree) {
        tree.addTreeSelectionListener(this);
    }

    public void valueChanged(TreeSelectionEvent event) {
        TreePath path = event.getPath();
        getViewport().removeAll();
        if (path != null) {
            FileNode node = (FileNode) path.getLastPathComponent();
            File file = node.getFile();
            ImageInfo info = ImageInfo.getInstanceFor(file);
            ImageMetadata meta = null;
            Exception error = null;
            try {
                meta = info.getMetadata();
            }
            catch (Exception e) {
                error = e;
            }
            if (meta != null) {
                if (dirs == null) {
                    // Show metadata unfiltered, sorted, and with IDs:
                    dirs = new DirectoryStack(meta, false, true, true);
                }
                else {
                    dirs.setMetadata(meta);
                }
                getViewport().add(dirs);
            }
            else {
                ExceptionDisplay ex = new ExceptionDisplay(error);
                getViewport().add(ex);
            }
        }
        else {
            JLabel label = new JLabel("(no file selected)");
            getViewport().add(label);
        }
        getViewport().revalidate();
    }
}
