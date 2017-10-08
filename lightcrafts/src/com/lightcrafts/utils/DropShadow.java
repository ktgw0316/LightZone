/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.JAIContext;

import javax.media.jai.JAI;
import javax.media.jai.BorderExtender;
import javax.media.jai.KernelJAI;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.Map;

public class DropShadow {
    protected Dimension dimension;

    protected float angle = 30;
    protected int distance = 5;

    protected int shadowSize = 5;
    protected float shadowOpacity = 0.5f;
    protected Color shadowColor = new Color(0x000000);
    protected Color backgroundColor = Color.white;

    public boolean equals(Object obj) {
	if (obj instanceof DropShadow) {
	    DropShadow ds = (DropShadow)obj;
	    return dimension.equals(ds.dimension) &&
                   angle == ds.angle &&
                   distance == ds.distance &&
                   shadowSize == ds.shadowSize &&
                   shadowColor.equals(ds.shadowColor) &&
                   backgroundColor.equals(ds.backgroundColor);
	}
	return false;
    }

    public int hashCode() {
        return (int) (dimension.hashCode() + shadowColor.hashCode() + backgroundColor.hashCode() +
                      angle + distance + shadowSize + shadowOpacity);
    }

    // cached values for fast painting
    protected int distance_x = 0;
    protected int distance_y = 0;

    public DropShadow(Dimension d) {
        computeShadowPosition();

        if (d != null) {
            dimension = d;
        } else {
            dimension = null;
        }
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
        computeShadowPosition();
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
        computeShadowPosition();
    }

    public Color getShadowColor() {
        return shadowColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setShadowColor(Color shadowColor) {
        if (shadowColor != null) {
            this.shadowColor = shadowColor;
        }
    }

    public void setBackgroundColor(Color backgroundColor) {
        if (backgroundColor != null) {
            this.backgroundColor = backgroundColor;
        }
    }

    public float getShadowOpacity() {
        return shadowOpacity;
    }

    public void setShadowOpacity(float shadowOpacity) {
        this.shadowOpacity = shadowOpacity;
    }

    public int getShadowSize() {
        return shadowSize;
    }

    public void setShadowSize(int shadowSize) {
        this.shadowSize = shadowSize;
    }

    private void computeShadowPosition() {
        double angleRadians = Math.toRadians(angle);
        distance_x = (int) (Math.cos(angleRadians) * distance);
        distance_y = (int) (Math.sin(angleRadians) * distance);
    }

    private static Map shadowMap = new SoftValueHashMap(); // Soft references stay around longer

    private synchronized BufferedImage createDropShadow() {
        BufferedImage shadow = (BufferedImage) shadowMap.get(this);
        if (shadow == null) {
            if ((shadow = (BufferedImage) shadowMap.get(this)) == null) {
                BufferedImage shadowMask = shadowOpacity == 1 ?
                                           createShadowMask(dimension) :
                                           createTranslucentShadowMask(dimension);

                shadow = getGaussianBlur(shadowSize, shadowMask);

                shadowMap.put(this, shadow);
            }
        }
        return shadow;
    }

    private BufferedImage createTranslucentShadowMask(Dimension d) {
        BufferedImage mask = new BufferedImage(d.width + shadowSize * 2, d.height + shadowSize * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = mask.createGraphics();
        g2d.fillRect(shadowSize, shadowSize, d.width, d.height);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, shadowOpacity));
        g2d.setColor(shadowColor);
        g2d.fillRect(0, 0, d.width + shadowSize * 2, d.height + shadowSize * 2);
        g2d.dispose();
        return mask;
    }

    private BufferedImage createShadowMask(Dimension d) {
        BufferedImage mask = new BufferedImage(d.width + shadowSize * 2, d.height + shadowSize * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = mask.createGraphics();
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, d.width + shadowSize * 2, d.height + shadowSize * 2);
        g2d.setColor(shadowColor);
        g2d.fillRect(shadowSize, shadowSize, d.width, d.height);
        g2d.dispose();
        return mask;
    }

    private BufferedImage getGaussianBlur(int size, BufferedImage image) {
        KernelJAI kernel = Functions.getGaussKernel(size / 3.0);
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(kernel);
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                  BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        hints.add(JAIContext.noCacheHint);
        return JAI.create("LCSeparableConvolve", pb, hints).getAsBufferedImage();
    }

    public BufferedImage getShadow() {
        return createDropShadow();
    }

    public AffineTransform getShadowTransform(int x, int y) {
        return AffineTransform.getTranslateInstance(x - shadowSize + distance_x, y - shadowSize + distance_y);
    }
}
