/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop.test;

import com.lightcrafts.ui.crop.CropListener;
import com.lightcrafts.ui.crop.CropMode;
import com.lightcrafts.model.CropBounds;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.geom.AffineTransform;

class Dot extends JComponent {

    private AffineTransform xform = new AffineTransform();

    void setTransform(AffineTransform xform) {
        this.xform = xform;
        repaint();
    }

    protected void paintComponent(Graphics graphics) {
        Dimension size = getSize();
        int w = size.width;
        int h = size.height;

        Graphics2D g = (Graphics2D) graphics;
        g.setTransform(xform);

        Rectangle r;

        r = new Rectangle(- 5, - 5, 10, 10);
        g.fill(r);

        r = new Rectangle(w / 4 - 5, h / 4 - 5, 10, 10);
        g.fill(r);

        r = new Rectangle(w / 4 - 5, - h / 4 - 5, 10, 10);
        g.fill(r);

        r = new Rectangle(- w / 4 - 5, h / 4 - 5, 10, 10);
        g.fill(r);

        r = new Rectangle(- w / 4 - 5, - h / 4 - 5, 10, 10);
        g.fill(r);
    }
}

public class CropTest {

    static AffineTransform xform = new AffineTransform();

    static AffineTransform updateXForm(
        double angle, double scale, double x, double y
    ) {
        AffineTransform s = AffineTransform.getScaleInstance(scale, scale);
        AffineTransform r = AffineTransform.getRotateInstance(angle);
        AffineTransform t = AffineTransform.getTranslateInstance(x, y);

        AffineTransform at = t;
        at.concatenate(r);
        at.concatenate(s);

        return at;
    }

    public static void main(String[] args) {

        final CropMode mode = new CropMode(false);
        mode.addCropListener(
            new CropListener() {
                public void cropCommitted(CropBounds rect) {
                    System.out.println(rect);
                }
                public void unCrop() {
                    System.out.println("uncrop");
                }
            }
        );
        JComponent overlay = mode.getOverlay();
        final Dot dot = new Dot();

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(overlay);
//        panel.add(dot);

        final JSlider rotor = new JSlider(-45, 45);
        final JSlider scalor = new JSlider(-100, 100);

        rotor.addChangeListener(
            new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    int r = rotor.getValue();
                    double angle = r * Math.PI / 180d;
                    int s = scalor.getValue();
                    double scale = Math.pow(10, s / 100d);
                    int x = panel.getWidth() / 2;
                    int y = panel.getHeight() / 2;
                    xform = updateXForm(angle, scale, x, y);
                    mode.setTransform(xform);
                    dot.setTransform(xform);
                }
            }
        );
        scalor.addChangeListener(
            new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    int r = rotor.getValue();
                    double angle = r * Math.PI / 180d;
                    int s = scalor.getValue();
                    double scale = Math.pow(10, s / 100d);
                    int x = panel.getWidth() / 2;
                    int y = panel.getHeight() / 2;
                    xform = updateXForm(angle, scale, x, y);
                    mode.setTransform(xform);
                    dot.setTransform(xform);
                }
            }
        );
        Box ctrls = Box.createHorizontalBox();
        ctrls.add(rotor);
        ctrls.add(scalor);

        panel.add(ctrls, BorderLayout.SOUTH);

        JFrame frame = new JFrame("Test");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 400, 400);
        frame.setVisible(true);
    }
}
