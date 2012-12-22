/*
 * $RCSfile: CanvasJAI.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:05 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;

/**
 * An extension of <code>java.awt.Canvas</code> for use with JAI.
 * <code>CanvasJAI</code> automatically returns an instance of
 * <code>GraphicsJAI</code> from its <code>getGraphics()</code>
 * method.  This guarantees that the <code>update(Graphics g)</code>
 * and <code>paint(Graphics g)</code> methods will receive a
 * <code>GraphicsJAI</code> instance for accelerated rendering of
 * <code>JAI</code> images.
 *
 * <p> In circumstances where it is not possible to use
 * <code>CanvasJAI</code>, a similar effect may be obtained by
 * manually calling <code>GraphicsJAI.createGraphicsJAI()</code> to
 * "wrap" a <code>Graphics2D</code> object.
 *
 * @see GraphicsJAI
 */
public class CanvasJAI extends Canvas {

    /**
     * Constructs an instance of <code>CanvasJAI</code> using the
     * given <code>GraphicsConfiguration</code>.
     */
    public CanvasJAI(GraphicsConfiguration config) {
        super(config);
    }

    /**
     * Returns an instance of <code>GraphicsJAI</code> for drawing to
     * this canvas.
     */
    public Graphics getGraphics() {
        Graphics2D g = (Graphics2D)super.getGraphics();
        return GraphicsJAI.createGraphicsJAI(g, this);
    }
}
