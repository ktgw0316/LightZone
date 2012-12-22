/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.io.IOException;

/**
 * Paint the fading knobs on Spacers that are indicate focus and mouseover.
 */
class KnobPainter implements ActionListener {

    // This features can be turned off, since it's experimental.
    private final static boolean KnobPainterOn = true;

    private static BufferedImage KnobImage;
    {
        URL url = Spacer.class.getResource("resources/spacerknob.png");
        try {
            KnobImage = ImageIO.read(url);
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't initialize Spacer.Knob");
        }
    }
    private final static int Delay = 25;    // ms for fade-in and fade-out

    private Spacer spacer;
    private boolean isOn;          // Headed towards opaque or invisible
    private float alpha;           // Current blend, 0 to 1
    private Timer timer;

    KnobPainter(Spacer spacer) {
        this.spacer = spacer;
        timer = new Timer(Delay, this);
    }

    void knobOn(boolean immediate) {
        if (! KnobPainterOn) {
            return;
        }
        isOn = true;
        if (! timer.isRunning() && ! immediate) {
            timer.restart();
        }
        else if (immediate) {
            alpha = 1;
            spacer.repaint();
        }
    }

    void knobOff(boolean immediate) {
        if (! KnobPainterOn) {
            return;
        }
        isOn = false;
        if (! timer.isRunning() && ! immediate) {
            timer.restart();
        }
        else if (immediate) {
            alpha = 0;
            spacer.repaint();
        }
    }

    void paint(Graphics2D g) {
        if (! KnobPainterOn) {
            return;
        }
        if (alpha == 0) {
            return;
        }
        Dimension size = spacer.getSize();
        int centerX = (size.width - spacer.getOutcrop()) / 2;
        int centerY = size.height / 2;
        int knobW = KnobImage.getWidth();
        int knobH = KnobImage.getHeight();

        AffineTransform xform = AffineTransform.getTranslateInstance(
            centerX - knobW / 2, centerY - knobH / 2
        );
        Composite oldComposite = g.getComposite();
        AlphaComposite blend = AlphaComposite.getInstance(
            AlphaComposite.SRC_ATOP, alpha
        );
        g.setComposite(blend);
        g.drawRenderedImage(KnobImage, xform);
        g.setComposite(oldComposite);
    }

    public void actionPerformed(ActionEvent event) {
        if (isOn) {
            alpha += .1;
            if (alpha >= 1) {
                alpha = 1;
                timer.stop();
            }
        }
        else {
            alpha -= .1;
            if (alpha <= 0) {
                alpha = 0;
                timer.stop();
            }
        }
        spacer.repaint();
    }
}
