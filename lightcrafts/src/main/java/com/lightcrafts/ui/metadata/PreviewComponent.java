/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.ui.ExceptionDisplay;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.net.URL;

public class PreviewComponent extends JPanel {

    private static Image SadSmiley;     // If no preview (and no error)
    private static Dimension SadSize = new Dimension(98, 98);

    static {
        URL url = PreviewComponent.class.getResource(
            "resources/sadSmiley.jpg"
        );
        SadSmiley = Toolkit.getDefaultToolkit().createImage(url);
    }

    private ExceptionDisplay ex;        // If there was an error

    private RenderedImage image;        // If everything worked out

    public PreviewComponent(ImageInfo info) {
        setOpaque(false);

        Throwable t = null;
        try {
            image = info.getPreviewImage();
        }
        catch (Throwable e) {
            t = e;
        }
        if (t != null) {
            setLayout(new BorderLayout());
            ex = new ExceptionDisplay(t);
            add(ex);
        }
        setOpaque(false);
    }

    public Dimension getPreferredSize() {
        if (image != null) {
            return new Dimension(image.getWidth(), image.getHeight());
        }
        else if (ex != null) {
            return super.getPreferredSize();
        }
        else {
            return SadSize;
        }
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (image != null) {
            g2d.drawRenderedImage(image, new AffineTransform());
            // g.drawImage(image, 0, 0, this);
        }
        else if (ex == null) {
            g.drawImage(SadSmiley, 0, 0, this);
        }
    }
}
