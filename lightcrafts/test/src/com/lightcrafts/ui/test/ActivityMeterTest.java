/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.test;

import com.lightcrafts.ui.ActivityMeter;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

class ImageComponent extends JComponent {

    private RenderedImage image;

    ImageComponent(RenderedImage image) {
        this.image = image;
    }

    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        g.drawRenderedImage(image, null);
    }

    public Dimension getPreferredSize() {
        return new Dimension(image.getWidth(), image.getHeight());
    }
}

public class ActivityMeterTest {

    public static void main(String[] args) throws IOException {

        RenderedImage image = ImageIO.read(new File(args[0]));
        JComponent comp = new ImageComponent(image);

        final ActivityMeter meter = new ActivityMeter(comp);

        Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    try {
                        for (int n=100; n>=0; n--) {
                            Thread.sleep(100);
                            meter.engineActive(n);
                        }
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        );
        thread.setDaemon(true);

        JFrame frame = new JFrame();
        frame.setContentPane(meter);
        frame.setLocation(100, 100);
        frame.pack();
        frame.setVisible(true);

        thread.start();
    }
}
