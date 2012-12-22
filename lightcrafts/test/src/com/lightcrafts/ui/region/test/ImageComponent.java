/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.test;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;

class ImageComponent extends JComponent {

    private Image image;
    private int width;
    private int height;

    ImageComponent(Image image) {
        this.image = image;
        width = image.getWidth(this);
        height = image.getHeight(this);
        System.out.println("w, h = " + width + ", " + height);
    }

    protected void paintComponent(Graphics g) {
        g.drawImage(image, 0, 0, this);
    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public boolean imageUpdate(
        Image img, final int infoflags, int x, int y, final int w, final int h
    ) {
        boolean result = super.imageUpdate(img, infoflags, x, y, w, h);
        if (img.equals(image)) {
            if ((infoflags & (WIDTH | HEIGHT)) != 0) {
                width = w;
                height = h;
                System.out.println("w, h = " + width + ", " + height);
                revalidate();
                repaint();
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
		Image image = ImageIO.read(new File(args[0]));
        ImageComponent ic = new ImageComponent(image);
        JFrame frame = new JFrame("Test");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(ic);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 400, 400);
        frame.setVisible(true);
    }
}
