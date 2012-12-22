/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata.test;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.ui.metadata.PreviewComponent;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.*;
import java.io.File;

// Listen for selection changes on the MetadataExplorer tree, and maybe
// update a preview display.

class PreviewUpdater extends Box implements TreeSelectionListener {

    private JComponent preview;
    private JCheckBox active;

    PreviewUpdater(final JTree tree) {
        super(BoxLayout.Y_AXIS);

        Border border = BorderFactory.createLineBorder(Color.gray);
        setBorder(border);

        active = new JCheckBox("Preview");
        active.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    boolean selected =
                        (event.getStateChange() == ItemEvent.SELECTED);
                    if (selected) {
                        tree.addTreeSelectionListener(PreviewUpdater.this);
                        TreePath path = tree.getSelectionPath();
                        if (path != null) {
                            updateImage(path);
                        }
                    }
                    else {
                        tree.removeTreeSelectionListener(PreviewUpdater.this);
                    }
                }
            }
        );
        add(active);

        preview = new JPanel();
        add(preview);

        TreePath path = tree.getSelectionPath();
        if (path != null) {
            updateImage(path);
        }
    }

    public void valueChanged(TreeSelectionEvent event) {
        TreePath path = event.getPath();
        if (path != null) {
            updateImage(path);
        }
    }

    protected void paintComponent(Graphics g) {
        Rectangle clip = g.getClipBounds();
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
    }

    private void updateImage(TreePath path) {
        try {
            FileNode node = (FileNode) path.getLastPathComponent();
            File file = node.getFile();
            ImageInfo info = ImageInfo.getInstanceFor(file);
            ImageMetadata meta = info.getMetadata();
            if (meta != null) {
                if (preview != null) {
                    remove(preview);
                }
                preview = new PreviewComponent(info);
                add(preview);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        JFrame frame = (JFrame)
            SwingUtilities.getAncestorOfClass(JFrame.class, this);
        frame.pack();
    }
}
