/*
 * $RCSfile: GraphicsJAI.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:08 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * A JAI wrapper for a Graphics2D object derived from a Component.
 * When drawing JAI images to a Component such as a Canvas, a new
 * GraphicsJAI may be constructed to wrap the Graphics2D object
 * provided by that Component.  This GraphicsJAI object may provide
 * acceleration for calls to drawRenderedImage(),
 * drawRenderableImage(), and possibly other methods.
 *
 * <p> If it is possible to use a CanvasJAI object instead of a
 * generic Canvas, or other Canvas subclass, then the Graphics objects
 * obtained from getGraphics() or received as an argument in paint()
 * will automatically be instances of GraphicsJAI.
 *
 * <p> The portion of the <code>GraphicsJAI</code> interface that
 * deals with adding and retrieving new hardware-specific implementations
 * has not been finalized and does not appear in the current API.
 *
 * @see CanvasJAI
 */
public class GraphicsJAI extends Graphics2D {

    Graphics2D g;
    Component component;

    /**
     * Constructs a new instance of <code>GraphicsJAI</code> that
     * wraps a given instance of <code>Graphics2D</code> for drawing
     * to a given <code>Component</code>.
     */
    protected GraphicsJAI(Graphics2D g, Component component) {
        this.g = g;
        this.component = component;
    }

    /**
     * Returns an instance of <code>GraphicsJAI</code> suitable
     * for rendering to the given <code>Component</code> via the
     * given <code>Graphics2D</code> instance.
     *
     * <p> If one is available, his method will select a hardware-specific
     * implementation, that is specialized for the display device containing
     * the component.
     */
    public static GraphicsJAI createGraphicsJAI(Graphics2D g,
                                                Component component) {
        return new GraphicsJAI(g, component);
    }

    /**
     * Creates a new <code>GraphicsJAI</code> object that is 
     * a copy of this <code>GraphicsJAI</code> object.
     *
     * @see java.awt.Graphics#create()
     */
    public Graphics create() {
        return new GraphicsJAI(g, component);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#getColor()
     */
    public Color getColor() {
        return g.getColor();
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#setColor(Color)
     */
    public void setColor(Color c) {
        g.setColor(c);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#setPaintMode()
     */
    public void setPaintMode() {
        g.setPaintMode();
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#setXORMode(Color)
     */
    public void setXORMode(Color c1) {
        g.setXORMode(c1);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#getFont
     */
    public Font getFont() {
        return g.getFont();
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#setFont(Font)
     */
    public void setFont(Font font) {
        g.setFont(font);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#getFontMetrics(Font)
     */
    public FontMetrics getFontMetrics(Font f) {
        return g.getFontMetrics(f);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#getClipBounds
     */
    public Rectangle getClipBounds() {
        return g.getClipBounds();
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#clipRect(int, int, int, int)
     */
    public void clipRect(int x, int y, int width, int height) {
        g.clipRect(x, y, width, height);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#setClip(int, int, int, int)
     */
    public void setClip(int x, int y, int width, int height) {
        g.setClip(x, y, width, height);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#getClip
     */
    public Shape getClip() {
        return g.getClip();
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#setClip(Shape)
     */
    public void setClip(Shape clip) {
        g.setClip(clip);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#copyArea(int, int, int, int, int, int)
     */
    public void copyArea(int x, int y, int width, int height,
                         int dx, int dy) {
        g.copyArea(x, y, width, height, dx, dy);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawLine(int, int, int, int)
     */
    public void drawLine(int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#fillRect(int, int, int, int)
     */
    public void fillRect(int x, int y, int width, int height) {
        g.fillRect(x, y, width, height);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#clearRect(int, int, int, int)
     */
    public void clearRect(int x, int y, int width, int height) {
        g.clearRect(x, y, width, height);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawRoundRect(int, int, int, int, int, int)
     */
    public void drawRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        g.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#fillRoundRect(int, int, int, int, int, int)
     */
    public void fillRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        g.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawOval(int, int, int, int)
     */
    public void drawOval(int x, int y, int width, int height) {
        g.drawOval(x, y, width, height);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#fillOval(int, int, int, int)
     */
    public void fillOval(int x, int y, int width, int height) {
        g.fillOval(x, y, width, height);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawArc(int, int, int, int, int, int)
     */
    public void drawArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        g.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#fillArc(int, int, int, int, int, int)
     */
    public void fillArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        g.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawPolyline(int[], int[], int)
     */
    public void drawPolyline(int xPoints[], int yPoints[],
                             int nPoints) {
        g.drawPolyline(xPoints, yPoints, nPoints);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawPolygon(int[], int[], int)
     */
    public void drawPolygon(int xPoints[], int yPoints[],
                            int nPoints) {
        g.drawPolygon(xPoints, yPoints, nPoints);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#fillPolygon(int[], int[], int)
     */
    public void fillPolygon(int xPoints[], int yPoints[],
                            int nPoints) {
        g.fillPolygon(xPoints, yPoints, nPoints);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawImage(Image, int, int, ImageObserver)
     */
    public boolean drawImage(Image img, int x, int y, 
                             ImageObserver observer) {
        return g.drawImage(img, x, y,
                           observer);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawImage(Image, int, int, int, int, ImageObserver)
     */
    public boolean drawImage(Image img, int x, int y,
                             int width, int height, 
                             ImageObserver observer) {
        return g.drawImage(img, x, y,
                           width, height,
                           observer);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawImage(Image, int, int, Color, ImageObserver)
     */
    public boolean drawImage(Image img, int x, int y, 
                             Color bgcolor,
                             ImageObserver observer) {
        return g.drawImage(img,
                           x, y,
                           bgcolor,
                           observer);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawImage(Image, int, int, int, int, Color, ImageObserver)
     */
    public boolean drawImage(Image img, int x, int y,
                             int width, int height, 
                             Color bgcolor,
                             ImageObserver observer) {
        return g.drawImage(img,
                           x, y, width, height,
                           bgcolor,
                           observer);
    }

    
    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawImage(Image, int, int, int, int, int, int, int, int, ImageObserver)
     */
    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             ImageObserver observer) {
        return g.drawImage(img,
                           dx1, dy1, dx2, dy2,
                           sx1, sy1, sx2, sy2,
                           observer);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#drawImage(Image, int, int, int, int, int, int, int, int, Color, ImageObserver)
     */
    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             Color bgcolor,
                             ImageObserver observer) {
        return g.drawImage(img,
                           dx1, dy1, dx2, dy2,
                           sx1, sy1, sx2, sy2,
                           bgcolor,
                           observer);
    }

    /**
     * See comments in java.awt.Graphics.
     *
     * @see java.awt.Graphics#dispose
     */
    public void dispose() {
        g.dispose();
    }

    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#draw(Shape)
     */
    public void draw(Shape s) {
        g.draw(s);
    }

    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#drawImage(Image, AffineTransform, ImageObserver)
     */
    public boolean drawImage(Image img,
                             AffineTransform xform,
                             ImageObserver obs) {
        return g.drawImage(img, xform, obs);
    }

    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#drawImage(BufferedImage, BufferedImageOp, int, int)
     */
    public void drawImage(BufferedImage img,
                          BufferedImageOp op,
                          int x,
                          int y) {
        g.drawImage(img, op, x, y);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#drawRenderedImage(RenderedImage, AffineTransform)
     */
    public void drawRenderedImage(RenderedImage img,
                                  AffineTransform xform) {
        g.drawRenderedImage(img, xform);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#drawRenderableImage(RenderableImage, AffineTransform)
     */
    public void drawRenderableImage(RenderableImage img,
                                    AffineTransform xform) {
        g.drawRenderableImage(img, xform);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#drawString(String, int, int)
     */
    public void drawString(String str, int x, int y) {
        g.drawString(str, x, y);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#drawString(String, float, float)
     */
    public void drawString(String s, float x, float y) {
        g.drawString(s, x, y);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#drawString(AttributedCharacterIterator, int, int)
     */
    public void drawString(AttributedCharacterIterator iterator,
                           int x, int y) {
        g.drawString(iterator, x, y);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#drawString(AttributedCharacterIterator, float, float)
     */
    public void drawString(AttributedCharacterIterator iterator,
                           float x, float y) {
        g.drawString(iterator, x, y);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#drawGlyphVector(GlyphVector, float, float)
     */
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        (this.g).drawGlyphVector(g, x, y);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#fill(Shape)
     */
    public void fill(Shape s) {
        g.fill(s);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#hit(Rectangle, Shape, boolean)
     */
    public boolean hit(Rectangle rect,
                       Shape s,
                       boolean onStroke) {
        return g.hit(rect, s, onStroke);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#getDeviceConfiguration
     */
    public GraphicsConfiguration getDeviceConfiguration() {
        return g.getDeviceConfiguration();
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#setComposite(Composite)
     */
    public void setComposite(Composite comp) {
        g.setComposite(comp);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#setPaint(Paint)
     */
    public void setPaint(Paint paint) {
        g.setPaint(paint);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#setStroke(Stroke)
     */
    public void setStroke(Stroke s) {
        g.setStroke(s);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#setRenderingHint(RenderingHints.Key, Object)
     */
    public void setRenderingHint(Key hintKey, Object hintValue) {
        g.setRenderingHint(hintKey, hintValue);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#getRenderingHint(RenderingHints.Key)
     */
    public Object getRenderingHint(Key hintKey) {
        return g.getRenderingHint(hintKey);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#setRenderingHints(Map)
     */
    public void setRenderingHints(Map hints) {
        g.setRenderingHints(hints);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#addRenderingHints(Map)
     */
    public void addRenderingHints(Map hints) {
        g.addRenderingHints(hints);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#getRenderingHints
     */
    public RenderingHints getRenderingHints() {
        return g.getRenderingHints();
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#translate(int, int)
     */
    public void translate(int x, int y) {
        g.translate(x, y);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#translate(double, double)
     */
    public void translate(double tx, double ty) {
        g.translate(tx, ty);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#rotate(double)
     */
    public void rotate(double theta) {
        g.rotate(theta);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#rotate(double, double, double)
     */
    public void rotate(double theta, double x, double y) {
        g.rotate(theta, x, y);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#scale(double, double)
     */
    public void scale(double sx, double sy) {
        g.scale(sx, sy);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#shear(double, double)
     */
    public void shear(double shx, double shy) {
        g.shear(shx, shy);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#transform(AffineTransform)
     */
    public void transform(AffineTransform Tx) {
        g.transform(Tx);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#setTransform(AffineTransform)
     */
    public void setTransform(AffineTransform Tx) {
        g.setTransform(Tx);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#getTransform
     */
    public AffineTransform getTransform() {
        return g.getTransform();
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#getPaint
     */
    public Paint getPaint() {
        return g.getPaint();
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#getComposite
     */
    public Composite getComposite() {
        return g.getComposite();
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#setBackground(Color)
     */
    public void setBackground(Color color) {
        g.setBackground(color);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#getBackground
     */
    public Color getBackground() {
        return g.getBackground();
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#getStroke
     */
    public Stroke getStroke() {
        return g.getStroke();
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#clip(Shape)
     */
    public void clip(Shape s) {
        g.clip(s);
    }
    
    /**
     * See comments in java.awt.Graphics2D.
     *
     * @see java.awt.Graphics2D#getFontRenderContext
     */
    public FontRenderContext getFontRenderContext() {
        return g.getFontRenderContext();
    }
}
