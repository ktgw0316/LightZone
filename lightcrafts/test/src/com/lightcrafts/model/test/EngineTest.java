/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.test;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.model.Engine;
import com.lightcrafts.model.EngineFactory;
import com.lightcrafts.platform.Platform;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class EngineTest {

    public static void main(String[] args) throws Exception {

        if ( Platform.isMac() ) {
            System.loadLibrary( "MacOSX" );
        }

        File file = new File(args[0]);
        ImageInfo info = ImageInfo.getInstanceFor(file);
        ImageMetadata meta = info.getMetadata();
        Engine engine = EngineFactory.createEngine(meta, null, null);
        Component comp = engine.getComponent();

        JFrame frame;

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(comp);

        frame = new JFrame("Test") {
            public void dispose() {
                super.dispose();
                setContentPane(new JPanel());
            }
        };
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocation(100, 100);
        frame.pack();
        frame.setVisible(true);

        frame = new JFrame("last");
        frame.setVisible(true);
    }
}
