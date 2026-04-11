/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;

/**
 * The top-level container for the metadata interface, a scroll pane holding
 * a stack of MetadataSections.
 */
public class MetadataScroll extends JScrollPane {

    private static final Logger logger = LoggerFactory.getLogger(MetadataScroll.class);

    private MetadataStack stack;
    private ImageInfo info;

    public MetadataScroll(ImageInfo info) {
        this();
        setImage(info);
    }

    public MetadataScroll() {
        setOpaque(true);
        getViewport().setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setBorder(LightZoneSkin.getPaneBorder());
    }

    public void setImage(ImageInfo info) {
        // If someone just wants to refresh this component, and we're in the
        // middle of editing, then ignore the refresh.
        if ((stack != null) && stack.isEditing()) {
            if ((this.info != null) && (info != null)) {
                File file = info.getFile();
                File thisFile = this.info.getFile();
                if (file.equals(thisFile)) {
                    return;
                }
            }
        }
        endEditing();

        this.info = info;

        if (info != null) {
            if (stack != null) {
                stack.setImage(info);
            }
            else {
                stack = new MetadataStack(info);
            }
            getViewport().add(stack);
        }
        else {
            getViewport().removeAll();
            repaint();
        }
    }

    // The control refreshes itself whenever cell editing ends.
    // Called from MetadataTable.editingStopped().
    public void refresh() {
        if (info != null) {
            File file = info.getFile();
            if (file != null) {
                info = ImageInfo.getInstanceFor(file);
                setImage(info);
            }
        }
    }

    public void endEditing() {
        if ((stack != null) && stack.isEditing()) {
            stack.endEditing();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            logger.warn("usage: MetadataScroll (file)");
            System.exit(1);
        }

        System.setProperty( "apple.awt.antialiasing"    , "false" );
        System.setProperty( "apple.awt.showGrowBox"     , "true" );
        System.setProperty( "apple.awt.textantialiasing", "true" );
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );
        System.setProperty( "swing.aatext", "true" );

        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
        File file = new File(args[0]);
        ImageInfo info = ImageInfo.getInstanceFor(file);
        MetadataScroll scroll = new MetadataScroll(info);

        scroll.getViewport().setBackground(LightZoneSkin.Colors.ToolPanesBackground);

        JFrame frame = new JFrame("MetadataScroll Test");
        frame.setContentPane(scroll);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
