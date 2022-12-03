/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

// Make a label with a collapse/expand icon, and hook it to a DirectoryStack
// to add and remove a ReadonlyTable when it gets mouse-pressed.

class DirectoryLabel extends Box {

    private static Icon CollapsedIcon;
    private static Icon ExpandedIcon;
    private static Icon CollapsedHighlightIcon;
    private static Icon ExpandedHighlightIcon;

    static {
        URL url;
        Image image;
        Toolkit toolkit = Toolkit.getDefaultToolkit();

        url = DirectoryLabel.class.getResource("resources/RightArrow.png");
        image = toolkit.createImage(url);
        CollapsedIcon = new ImageIcon(image);

        url = DirectoryLabel.class.getResource("resources/DownArrow.png");
        image = toolkit.createImage(url);
        ExpandedIcon = new ImageIcon(image);

        url = DirectoryLabel.class.getResource(
            "resources/RightArrowHighlight.png"
        );
        image = toolkit.createImage(url);
        CollapsedHighlightIcon = new ImageIcon(image);

        url = DirectoryLabel.class.getResource(
            "resources/DownArrowHighlight.png"
        );
        image = toolkit.createImage(url);
        ExpandedHighlightIcon = new ImageIcon(image);
    }

    private DirectoryStack stack;
    private ImageMetadataDirectory dir;
    private JLabel label;
    private boolean isExpanded;

    DirectoryLabel(
        ImageMetadataDirectory dir,
        String name,
        DirectoryStack stack
    ) {
        super(BoxLayout.X_AXIS);
        this.dir = dir;
        this.stack = stack;

        label = new JLabel(name);
        add(label);
        add(Box.createHorizontalGlue());

        addMouseListener(
            new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    if (isExpanded) {
                        doCollapse();
                    }
                    else {
                        doExpand();
                    }
                }
                public void mouseEntered(MouseEvent event) {
                    if (isExpanded) {
                        label.setIcon(ExpandedHighlightIcon);
                    }
                    else {
                        label.setIcon(CollapsedHighlightIcon);
                    }
                }
                public void mouseExited(MouseEvent event) {
                    if (isExpanded) {
                        label.setIcon(ExpandedIcon);
                    }
                    else {
                        label.setIcon(CollapsedIcon);
                    }
                }
            }
        );
        setAlignmentX(1f);
        if (stack.isDirectoryExpanded(dir)) {
            doExpand();
        }
        else {
            doCollapse();
        }
    }

    void doExpand() {
        stack.showDirectory(dir);
        label.setIcon(ExpandedIcon);
        isExpanded = true;
        stack.expandDirectory(dir);
    }

    void doCollapse() {
        stack.hideDirectory(dir);
        label.setIcon(CollapsedIcon);
        isExpanded = false;
        stack.collapseDirectory(dir);
    }
}
