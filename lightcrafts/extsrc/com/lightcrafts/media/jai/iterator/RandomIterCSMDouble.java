/*
 * $RCSfile: RandomIterCSMDouble.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:42 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.iterator;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;

/**
 * @since EA2
 */
public class RandomIterCSMDouble extends RandomIterCSM {

    public RandomIterCSMDouble(RenderedImage im, Rectangle bounds) {
        super(im, bounds);
    }

    public final int getSample(int x, int y, int b) {
        return (int)getSampleDouble(x, y, b);
    }

    public final float getSampleFloat(int x, int y, int b) {
        return (float)getSampleDouble(x, y, b);
    }

    public final double getSampleDouble(int x, int y, int b) {
        return 0.0;
    }
}
