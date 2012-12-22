/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.metadata.ImageMetadata;

import javax.swing.*;
import java.awt.*;

/**
 * The top-level container for the metadata interface, a scroll pane holding
 * a directory stack.
 */
public class InfoPane extends JScrollPane {

    private DirectoryStack dirs;

    public InfoPane(ImageMetadata meta) {
        this();
        setOpaque(true);
        getViewport().setOpaque(false);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        setMetadata(meta);
    }

    public InfoPane() {
        setOpaque(true);
        getViewport().setOpaque(false);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        setPlaceholder();
    }

    public void setMetadata(ImageMetadata meta) {
        if (meta != null) {
            if (dirs != null) {
                dirs.setMetadata(meta);
            }
            else {
                dirs = new DirectoryStack(meta);
            }
            getViewport().add(dirs);
        }
        else {
            getViewport().removeAll();
            setPlaceholder();
        }
    }

    public void dispose() {
        if (dirs != null) {
            dirs.disposeTables();
        }
    }

    // Silly Aqua scrollbars, they draw wrong with an empty viewport,
    // or with a zero-size view:

    private void setPlaceholder() {
        JPanel placeholder = new JPanel();
        placeholder.setPreferredSize(new Dimension(1, 1));
        getViewport().setView(placeholder);
    }
}
