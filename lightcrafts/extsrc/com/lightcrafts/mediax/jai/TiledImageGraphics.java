/*
 * $RCSfile: TiledImageGraphics.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:23 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.ImageObserver;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.RenderableImage;
import java.lang.reflect.Method;
import java.text.AttributedCharacterIterator;
import java.util.Hashtable;
import java.util.Map;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * A concrete (i.e., non-abstract) class implementing all the methods
 * of <code>Graphics2D</code> (and thus of <code>Graphics</code>) with
 * a <code>TiledImage</code> as the implicit drawing canvas.
 * The actual implementation will use Java2D to do most of the work
 * by packaging up the image tiles in a form that Java2D can
 * understand.
 *
 * <p> Since the public methods of this class all derive from
 * <code>Graphics2D</code>, they are not commented individually.
 *
 * <p> The <code>ColorModel</code> for the canvas will be that of the
 * associated <code>TiledImage</code> unless that <code>ColorModel</code>
 * is null.  If the <code>TiledImage</code> <code>ColorModel</code> is null,
 * an attempt will first be made to deduce the <code>ColorModel</code> from
 * the <code>SampleModel</code> of the <code>TiledImage</code> using the
 * <code>createColorModel()</code> method of <code>PlanarImage</code>.
 * If the <code>ColorModel</code> is still null, the default RGB
 * <code>ColorModel</code> returned by the <code>getRGBdefault()</code>
 * method of <code>ColorModel</code> will be used if the
 * <code>TiledImage</code> has a compatible <code>SampleModel</code>.
 * If no acceptable <code>ColorModel</code> can be derived an
 * <code>UnsupportedOperationException</code> will be thrown.
 *
 * @see java.awt.Graphics
 * @see java.awt.Graphics2D
 * @see java.awt.image.ColorModel
 * @see java.awt.image.SampleModel
 * @see TiledImage
 *
 */
class TiledImageGraphics extends Graphics2D {

    // Constants
    private static final Class GRAPHICS2D_CLASS = Graphics2D.class;
    private static final int PAINT_MODE = 1;
    private static final int XOR_MODE = 2;

    // TiledImageGraphics state
    private TiledImage tiledImage;
    Hashtable properties;
    private RenderingHints renderingHints;

    // Cached variables available from the source TiledImage
    private int tileWidth;
    private int tileHeight;
    private int tileXMinimum;
    private int tileXMaximum;
    private int tileYMinimum;
    private int tileYMaximum;
    private ColorModel colorModel;

    // Graphics state information (from java.awt.Graphics)
    private Point origin;
    private Shape clip;
    private Color color;
    private Font font;
    private int paintMode = PAINT_MODE;
    private Color XORColor;

    // Graphics state information (from java.awt.Graphics2D)
    private Color background;
    private Composite composite;
    private Paint paint;
    private Stroke stroke;
    private AffineTransform transform;

    // ---------- Methods specific to TiledImageGraphics ----------

    /**
     * Determine the bounding box of the points represented by the supplied
     * arrays of X and Y coordinates.
     *
     * @param xPoints An array of <i>x</i> points.
     * @param yPoints An array of <i>y</i> points.
     * @param nPoints The total number of points.
     */
    private static final Rectangle getBoundingBox(int[] xPoints,
                                                  int[] yPoints,
                                                  int nPoints) {
        if(nPoints <= 0) return null;

        int minX;
        int maxX;
        int minY;
        int maxY;

        minX = maxX = xPoints[0];
        minY = maxY = yPoints[0];

        for(int i = 1; i < nPoints; i++) {
            minX = Math.min(minX, xPoints[i]);
            maxX = Math.max(maxX, xPoints[i]);
            minY = Math.min(minY, yPoints[i]);
            maxY = Math.max(maxY, yPoints[i]);
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /**
     * Construct a <code>TiledImageGraphics</code> object that draws onto a
     * particular <code>TiledImage</code>.  The <code>TiledImage</code>
     * parameter must be of integral data type or an
     * <code>UnsupportedOperationException</code> will be thrown. Likewise,
     * if no appropriate <code>ColorModel</code> can be derived an
     * <code>UnsupportedOperationException</code> will be thrown.
     *
     * @param im The <code>TiledImage</code> which will serve as the graphics
     * canvas.
     * @throws <code>UnsupportedOperationException</code> if no appropriate
     * <code>ColorModel</code> can be derived.
     */
    public TiledImageGraphics(TiledImage im) {
        // Check for non-integral data type.
        int dataType = im.getSampleModel().getTransferType();
        if(dataType != DataBuffer.TYPE_BYTE &&
           dataType != DataBuffer.TYPE_SHORT &&
           dataType != DataBuffer.TYPE_USHORT &&
           dataType != DataBuffer.TYPE_INT) {
            throw new UnsupportedOperationException(JaiI18N.getString("TiledImageGraphics0"));
        }

        // Cache the TiledImage.
        tiledImage = im;

        // Cache the tile dimensions and index extrema.
        tileWidth = im.getTileWidth();
        tileHeight = im.getTileHeight();
        tileXMinimum = im.getMinTileX();
        tileXMaximum = im.getMaxTileX();
        tileYMinimum = im.getMinTileY();
        tileYMaximum = im.getMaxTileY();

        // Attempt to derive an appropriate ColorModel.
        colorModel = getColorModel(tiledImage);

        // Obtain a Graphics2D object from which to derive state
        Graphics2D g = getBogusGraphics2D(false);

        // -- java.awt.Graphics state --
        origin = new Point(0, 0);
        setClip(tiledImage.getBounds());
        setColor(g.getColor());
        setFont(g.getFont());
        setPaintMode();

        // -- java.awt.Graphics2D state --
        setBackground(g.getBackground());
        setComposite(g.getComposite());
        setStroke(g.getStroke());
        setTransform(g.getTransform());

        // Dispose of the Graphics2D
        g.dispose();

        // -- TiledImageGraphics state --
        // Cache the Hashtable of properties.
        properties = tiledImage.getProperties();

        // Create RenderingHints from the properties.
        renderingHints = new RenderingHints(properties);
    }

    /**
     * Copy the graphics state of the current object to a
     * <code>Graphics2D</code> object.
     *
     * @param g2d The target <code>Graphics2D</code> object.
     */
    private void copyState(Graphics2D g2d) {
        // java.awt.Graphics state
        g2d.translate(origin.x, origin.y);
        setClip(getClip());
        g2d.setColor(getColor());
        if(paintMode == PAINT_MODE) {
            g2d.setPaintMode();
        } else if(XORColor != null) {
            g2d.setXORMode(XORColor);
        }
        g2d.setFont(getFont());

        // java.awt.Graphics2D state
        g2d.setBackground(getBackground());
        g2d.setComposite(getComposite());
        if(paint != null) g2d.setPaint(getPaint());
        g2d.setRenderingHints(renderingHints);
        g2d.setStroke(getStroke());
        g2d.setTransform(getTransform());
    }

    /**
     * Creates a bogus <code>Graphics2D</code> object to be used to retrieve
     * information dependent on system aspects which are image-independent.
     *
     * <p>The <code>dispose()</code> method of the <code>Graphics2D</code>
     * object returned should be called to free the associated resources as\
     * soon as possible.
     *
     * @param shouldCopyState Whether the state of the returned
     * <code>Graphics2D</code> should be initialized to that of the
     * current <code>TiledImageGraphics</code> object.
     *
     * @return A <code>Graphics2D</code> object.
     */
    private Graphics2D getBogusGraphics2D(boolean shouldCopyState) {
        Raster r =  tiledImage.getTile(tileXMinimum, tileYMinimum);
        WritableRaster wr = r.createCompatibleWritableRaster(1, 1);
        BufferedImage bi =
            new BufferedImage(colorModel, wr,
                              colorModel.isAlphaPremultiplied(), properties);

        Graphics2D bogusG2D = bi.createGraphics();
        if(shouldCopyState) {
            copyState(bogusG2D);
        }
        return bogusG2D;
    }

    /**
     * Derive an approriate <code>ColorModel</code> for use with the
     * underlying <code>BufferedImage</code> canvas.  If an appropriate
     * <code>ColorModel</code> cannot be derived an
     * <code>UnsupportedOperationException</code> will be thrown.
     *
     * @return An appropriate <code>ColorModel</code>.
     * @throws <code>UnsupportedOperationException</code> if no appropriate
     * <code>ColorModel</code> can be derived.
     */
    private static ColorModel getColorModel(TiledImage ti) {
        // Derive an appropriate ColorModel for the BufferedImage:
        // first try to use that of the TiledImage.
        ColorModel colorModel = ti.getColorModel();

        if(colorModel == null) {
            // Try to guess an appropriate ColorModel.
            if(colorModel == null) {
                // First try to deduce one from the TiledImage SampleModel.
                SampleModel sm = ti.getSampleModel();
                colorModel = PlanarImage.createColorModel(sm);
                if(colorModel == null) {
                    // ColorModel still null: try RGB default model.
                    ColorModel cm = ColorModel.getRGBdefault();
                    if(JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
                        colorModel = cm;
                    }
                }

                // ColorModel is still null: throw an exception.
                if(colorModel == null) {
                    throw new UnsupportedOperationException(JaiI18N.getString("TiledImageGraphics1"));
                }
            }
        }

        return colorModel;
    }

    /**
     * Effect a graphics operation on the <code>TiledImage</code> by
     * creating a <code>BufferedImage</code> for each tile in the affected
     * region and using the corresponding <code>Graphics2D</code> to
     * perform the equivalent operation on the tile.
     *
     * @param x The <i>x</i> coordinate of the upper left corner.
     * @param y The <i>y</i> coordinate of the upper left corner.
     * @param width The width of the region.
     * @param height The height of the region.
     * @param argTypes An array of the <code>Classes</code> of the arguments
     * of the specified operation.
     * @param args The arguments of the operation as an array of
     * <code>Object</code>s.
     */
    private boolean doGraphicsOp(int x, int y, int width, int height,
                                 String name, Class[] argTypes,
                                 Object[] args) {
        boolean returnValue = false;

        // Determine the Method object associated with the Graphics2D method
        // having the indicated name. The search begins with the Graphics2D
        // class and continues to its superclasses until the Method is found.
        Method method = null;
        try {
            method = GRAPHICS2D_CLASS.getMethod(name, argTypes);
        } catch(Exception e) {
            String message = JaiI18N.getString("TiledImageGraphics2") + name;
            sendExceptionToListener(message, new ImagingException(e));
//            throw new RuntimeException(e.getMessage());
        }

        // Transform requested area to obtain actual bounds.
        Rectangle bounds = new Rectangle(x, y, width, height);
        bounds = getTransform().createTransformedShape(bounds).getBounds();

        // Determine the range of tile indexes
        int minTileX = tiledImage.XToTileX(bounds.x);
        if(minTileX < tileXMinimum)
            minTileX = tileXMinimum;
        int minTileY = tiledImage.YToTileY(bounds.y);
        if(minTileY < tileYMinimum)
            minTileY = tileYMinimum;
        int maxTileX = tiledImage.XToTileX(bounds.x + bounds.width - 1);
        if(maxTileX > tileXMaximum)
            maxTileX = tileXMaximum;
        int maxTileY = tiledImage.YToTileY(bounds.y + bounds.height - 1);
        if(maxTileY > tileYMaximum)
            maxTileY = tileYMaximum;

        // Loop over the tiles
        for(int tileY = minTileY; tileY <= maxTileY; tileY++) {
            int tileMinY = tiledImage.tileYToY(tileY);
            for(int tileX = minTileX; tileX <= maxTileX; tileX++) {
                int tileMinX = tiledImage.tileXToX(tileX);

                // Get the WritableRaster of the current tile
                WritableRaster wr = tiledImage.getWritableTile(tileX, tileY);
                wr = wr.createWritableTranslatedChild(0, 0);

                // Create an equivalent BufferedImage
                BufferedImage bi =
                    new BufferedImage(colorModel, wr,
                                      colorModel.isAlphaPremultiplied(),
                                      properties);

                // Create the associated Graphics2D
                Graphics2D g2d = bi.createGraphics();

                // Initialize the Graphics2D state
                copyState(g2d);

                // Bias the tile origin so that the global coordinates
                // map correctly onto the tile.
                try {
                    Point2D origin2D =
                        g2d.getTransform().transform(new Point2D.Double(),
                                                    null);
                    Point pt = new Point((int)origin2D.getX() - tileMinX,
                                         (int)origin2D.getY() - tileMinY);
                    Point2D pt2D =
                        g2d.getTransform().inverseTransform(pt, null);
                    g2d.translate(pt2D.getX(), pt2D.getY());
                } catch(Exception e) {
                    String message = JaiI18N.getString("TiledImageGraphics3");
                    sendExceptionToListener(message, new ImagingException(e));
//                    throw new RuntimeException(e.getMessage());
                }

                // Perform the graphics operation
                try {
                    Object retVal = method.invoke(g2d, args);
                    if(retVal != null && retVal.getClass() == boolean.class) {
                        returnValue = ((Boolean)retVal).booleanValue();
                    }
                } catch(Exception e) {
                    String message =
                        JaiI18N.getString("TiledImageGraphics3") + " " + name;
                    sendExceptionToListener(message, new ImagingException(e));
//                    throw new RuntimeException(e.getMessage());
                }

                // Dispose of the Graphics2D
                g2d.dispose();

                // Notify the TiledImage that writing to the tile is complete
                tiledImage.releaseWritableTile(tileX, tileY);
            }
        }

        return returnValue;
    }

    /**
     * Effect a graphics operation on the <code>TiledImage</code> by
     * creating a <code>BufferedImage</code> for each tile in the affected
     * region and using the corresponding <code>Graphics2D</code> to
     * perform the equivalent operation on the tile.
     *
     * @param s The encompassing <code>Shape</code>.
     * @param argTypes An array of the <code>Classes</code> of the arguments
     * of the specified operation.
     * @param args The arguments of the operation as an array of
     * <code>Object</code>s.
     */
    private boolean doGraphicsOp(Shape s, String name, Class[] argTypes,
                                 Object[] args) {
        Rectangle r = s.getBounds();
        return doGraphicsOp(r.x, r.y, r.width, r.height, name, argTypes, args);
    }

    // ---------- Methods from java.awt.Graphics ----------

    public Graphics create() {
        // Construct the TiledImageGraphics object from the current TiledImage.
        TiledImageGraphics tig = new TiledImageGraphics(tiledImage);

        // Copy the state of the current TiledImageGraphics object.
        copyState(tig);

        return tig;
    }

    // public Graphics create(int x, int y, int width, int height)
    // -- implemented in Graphics superclass

    public Color getColor() {
        return color;
    }

    public void setColor(Color c) {
        color = c;
    }

    public void setPaintMode() {
        paintMode = PAINT_MODE;
    }

    public void setXORMode(Color c1) {
        paintMode = XOR_MODE;
        XORColor = c1;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public FontMetrics getFontMetrics(Font f) {
        Graphics2D g2d = getBogusGraphics2D(true);

        FontMetrics fontMetrics = g2d.getFontMetrics(f);

        g2d.dispose();

        return fontMetrics;
    }

    public Rectangle getClipBounds() {
        return clip.getBounds();
    }

    public void clipRect(int x, int y, int width, int height) {
        clip(new Rectangle(x, y, width, height));
    }

    public void setClip(int x, int y, int width, int height) {
        setClip(new Rectangle(x, y, width, height));
    }

    public Shape getClip() {
        return clip;
    }

    public void setClip(Shape clip) {
        this.clip = clip;
    }

    public void copyArea(int x, int y, int width, int height,
                         int dx, int dy) {
        Rectangle rect =
            getBoundingBox(new int[] {x, x+dx, x+width-1, x+width-1+dx},
                           new int[] {y, y+dy, y+height-1, y+height-1+dy},
                           4);

        doGraphicsOp(rect, "copyArea",
                     new Class[] {int.class, int.class, int.class,
                                      int.class, int.class, int.class},
                     new Object[] {new Integer(x), new Integer(y),
                                       new Integer(width), new Integer(height),
                                       new Integer(dx), new Integer(dy)});
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        Rectangle rect =
            getBoundingBox(new int[] {x1, x2}, new int[] {y1, y2}, 2);

        doGraphicsOp(rect, "drawLine",
                     new Class[] {int.class, int.class, int.class, int.class},
                     new Object[] {new Integer(x1), new Integer(y1),
                                       new Integer(x2), new Integer(y2)});
    }

    public void fillRect(int x, int y, int width, int height) {
        doGraphicsOp(x, y, width, height, "fillRect",
                     new Class[] {int.class, int.class, int.class, int.class},
                     new Object[] {new Integer(x), new Integer(y),
                                       new Integer(width), new Integer(height)});
    }

    // public void drawRect(int x, int y, int width, int height)
    // -- implemented in Graphics superclass

    public void clearRect(int x, int y, int width, int height) {
        doGraphicsOp(x, y, width, height, "clearRect",
                     new Class[] {int.class, int.class, int.class, int.class},
                     new Object[] {new Integer(x), new Integer(y),
                                       new Integer(width), new Integer(height)});
    }

    public void drawRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        doGraphicsOp(x-arcWidth, y-arcHeight,
                     width+2*arcWidth, height+2*arcHeight, "drawRoundRect",
                     new Class[] {int.class, int.class, int.class,
                                      int.class, int.class, int.class},
                     new Object[] {new Integer(x), new Integer(y),
                                       new Integer(width), new Integer(height),
                                       new Integer(arcWidth),
                                       new Integer(arcHeight)});
    }

    public void fillRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        doGraphicsOp(x-arcWidth, y-arcHeight,
                     width+2*arcWidth, height+2*arcHeight,
                     "fillRoundRect",
                     new Class[] {int.class, int.class, int.class,
                                      int.class, int.class, int.class},
                     new Object[] {new Integer(x), new Integer(y),
                                       new Integer(width), new Integer(height),
                                       new Integer(arcWidth),
                                       new Integer(arcHeight)});
    }

    // draw3DRect() is implemented in the Graphics superclass but is
    // overridden in Graphics2D and so must be implemented here.
    public void draw3DRect(int x, int y, int width, int height,
                           boolean raised) {
        doGraphicsOp(x, y, width, height, "draw3DRect",
                     new Class[] {int.class, int.class, int.class, int.class,
                                      boolean.class},
                     new Object[] {new Integer(x), new Integer(y),
                                       new Integer(width),
                                       new Integer(height),
                                       new Boolean(raised)});
    }

    // fill3DRect() is implemented in the Graphics superclass but is
    // overridden in Graphics2D and so must be implemented here.
    public void fill3DRect(int x, int y, int width, int height,
                           boolean raised) {
        doGraphicsOp(x, y, width, height, "fill3DRect",
                     new Class[] {int.class, int.class, int.class, int.class,
                                      boolean.class},
                     new Object[] {new Integer(x), new Integer(y),
                                       new Integer(width),
                                       new Integer(height),
                                       new Boolean(raised)});
    }

    public void drawOval(int x, int y, int width, int height) {
        doGraphicsOp(x, y, width, height, "drawOval",
                     new Class[] {int.class, int.class, int.class, int.class},
                     new Object[] {new Integer(x), new Integer(y),
                                       new Integer(width), new Integer(height)});
    }

    public void fillOval(int x, int y, int width, int height) {
        doGraphicsOp(x, y, width, height, "fillOval",
                     new Class[] {int.class, int.class, int.class, int.class},
                     new Object[] {new Integer(x), new Integer(y),
                                       new Integer(width), new Integer(height)});
    }

    public void drawArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        doGraphicsOp(x, y, width, height, "drawArc",
                     new Class[] {int.class, int.class, int.class,
                                      int.class, int.class, int.class},
                     new Object[] {new Integer(x), new Integer(y),
                                       new Integer(width), new Integer(height),
                                       new Integer(startAngle),
                                       new Integer(arcAngle)});
    }

    public void fillArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        doGraphicsOp(x, y, width, height, "fillArc",
                     new Class[] {int.class, int.class, int.class,
                                      int.class, int.class, int.class},
                     new Object[] {new Integer(x), new Integer(y),
                                       new Integer(width), new Integer(height),
                                       new Integer(startAngle),
                                       new Integer(arcAngle)});
    }

    public void drawPolyline(int xPoints[], int yPoints[], int nPoints) {
        Class intArrayClass = xPoints.getClass();
        Rectangle box = getBoundingBox(xPoints, yPoints, nPoints);

        if(box == null) return;

        doGraphicsOp(box, "drawPolyline",
                     new Class[] {intArrayClass, intArrayClass, int.class},
                     new Object[] {xPoints, yPoints, new Integer(nPoints)});
    }

    public void drawPolygon(int xPoints[], int yPoints[], int nPoints) {
        Class intArrayClass = xPoints.getClass();
        Rectangle box = getBoundingBox(xPoints, yPoints, nPoints);

        if(box == null) return;

        doGraphicsOp(box, "drawPolygon",
                     new Class[] {intArrayClass, intArrayClass, int.class},
                     new Object[] {xPoints, yPoints, new Integer(nPoints)});
    }

    // public void drawPolygon(Polygon) -- implemented in Graphics superclass

    public void fillPolygon(int xPoints[], int yPoints[], int nPoints) {
        Class intArrayClass = xPoints.getClass();
        Rectangle box = getBoundingBox(xPoints, yPoints, nPoints);

        if(box == null) return;

        doGraphicsOp(box, "fillPolygon",
                     new Class[] {intArrayClass, intArrayClass, int.class},
                     new Object[] {xPoints, yPoints, new Integer(nPoints)});
    }

    // public void fillPolygon(Polygon) -- implemented in Graphics superclass

    public void drawString(String str, int x, int y) {
        Rectangle2D r2d =
            getFontMetrics(getFont()
                           ).getStringBounds(str, this);

        r2d.setRect(x, y - r2d.getHeight() + 1,
                    r2d.getWidth(), r2d.getHeight());

        doGraphicsOp(r2d, "drawString",
                     new Class[] {java.lang.String.class,
                                      int.class, int.class},
                     new Object[] {str, new Integer(x), new Integer(y)});
    }

    // public void drawChars -- implemented in Graphics superclass
    // public void drawBytes -- implemented in Graphics superclass

    public boolean drawImage(Image img,
                             int x, int y,
                             ImageObserver observer) {
        return doGraphicsOp(x, y,
                            img.getWidth(observer),
                            img.getHeight(observer),
                            "drawImage",
                            new Class[] {java.awt.Image.class,
                                             int.class, int.class,
                                             java.awt.image.ImageObserver.class},
                            new Object[] {img, new Integer(x), new Integer(y),
                                              observer});
    }

    public void drawRenderedImage(RenderedImage im,
                                  AffineTransform transform) {
        Rectangle2D.Double srcRect = new Rectangle2D.Double(im.getMinX(),
                                                            im.getMinY(),
                                                            im.getWidth(),
                                                            im.getHeight());

        Rectangle2D dstRect =
            transform.createTransformedShape(srcRect).getBounds2D();

        doGraphicsOp(dstRect, "drawRenderedImage",
                     new Class[] {java.awt.image.RenderedImage.class,
                                      java.awt.geom.AffineTransform.class},
                     new Object[] {im, transform});
    }

    public void drawRenderableImage(RenderableImage img,
                                    AffineTransform xform) {
        Rectangle2D.Double srcRect = new Rectangle2D.Double(img.getMinX(),
                                                            img.getMinY(),
                                                            img.getWidth(),
                                                            img.getHeight());

        Rectangle2D dstRect =
            xform.createTransformedShape(srcRect).getBounds2D();

        doGraphicsOp(dstRect, "drawRenderableImage",
                     new Class[] {java.awt.image.renderable.RenderableImage.class,
                                      java.awt.geom.AffineTransform.class},
                     new Object[] {img, xform});
    }

    public boolean drawImage(Image img, int x, int y,
                             int width, int height,
                             ImageObserver observer) {
        return doGraphicsOp(x, y, width, height, "drawImage",
                            new Class[] {java.awt.Image.class,
                                             int.class, int.class, int.class, int.class,
                                             java.awt.image.ImageObserver.class},
                            new Object[] {img, new Integer(x), new Integer(y),
                                              new Integer(width), new Integer(height),
                                              observer});
    }

    public boolean drawImage(Image img, int x, int y,
                             Color bgcolor,
                             ImageObserver observer) {
        return doGraphicsOp(x, y,
                            img.getWidth(observer),
                            img.getHeight(observer), "drawImage",
                            new Class[] {java.awt.Image.class,
                                             int.class, int.class,
                                             java.awt.Color.class,
                                             java.awt.image.ImageObserver.class},
                            new Object[] {img, new Integer(x), new Integer(y),
                                              bgcolor, observer});
    }

    public boolean drawImage(Image img, int x, int y,
                             int width, int height,
                             Color bgcolor,
                             ImageObserver observer) {
        return doGraphicsOp(x, y, width, height, "drawImage",
                   new Class[] {java.awt.Image.class,
                                    int.class, int.class, int.class, int.class,
                                    java.awt.Color.class,
                                    java.awt.image.ImageObserver.class},
                   new Object[] {img, new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height),
                                     bgcolor, observer});
    }

    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             ImageObserver observer) {
        return doGraphicsOp(dx1, dy1, dx2-dx1+1, dy2-dy1+1,
                            "drawImage",
                   new Class[] {java.awt.Image.class,
                                    int.class, int.class, int.class, int.class,
                                    int.class, int.class, int.class, int.class,
                                    java.awt.image.ImageObserver.class},
                   new Object[] {img, new Integer(dx1), new Integer(dy1),
                                     new Integer(dx2), new Integer(dy2),
                                     new Integer(sx1), new Integer(sy1),
                                     new Integer(sx2), new Integer(sy2),
                                     observer});
    }

    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             Color bgcolor,
                             ImageObserver observer) {
        return doGraphicsOp(dx1, dy1, dx2-dx1+1, dy2-dy1+1,
                            "drawImage",
                            new Class[] {java.awt.Image.class,
                                             int.class, int.class, int.class, int.class,
                                             int.class, int.class, int.class, int.class,
                                             java.awt.Color.class,
                                             java.awt.image.ImageObserver.class},
                            new Object[] {img, new Integer(dx1), new Integer(dy1),
                                              new Integer(dx2), new Integer(dy2),
                                              new Integer(sx1), new Integer(sy1),
                                              new Integer(sx2), new Integer(sy2),
                                              bgcolor, observer});
    }

    public void dispose() {
        // No resources need to be released.
    }

    // public void finalize -- implemented in Graphics superclass
    // public String toString -- implemented in Graphics superclass

    // ---------- Methods from java.awt.Graphics2D ----------

    public void addRenderingHints(Map hints) {
        renderingHints.putAll(hints);
    }

    public void draw(Shape s) {
        doGraphicsOp(s.getBounds(),
                     "draw",
                     new Class[] {java.awt.Shape.class},
                     new Object[] {s});
    }

    public boolean drawImage(Image img,
                             AffineTransform xform,
                             ImageObserver obs) {
        Rectangle2D.Double srcRect =
            new Rectangle2D.Double(0, 0,
                                   img.getWidth(obs), img.getHeight(obs));

        Rectangle2D dstRect =
            transform.createTransformedShape(srcRect).getBounds2D();

        return doGraphicsOp(dstRect, "drawImage",
                            new Class[] {java.awt.Image.class,
                                             java.awt.geom.AffineTransform.class,
                                             java.awt.image.ImageObserver.class},
                            new Object[] {img, xform, obs});
    }

    public void drawImage(BufferedImage img,
                          BufferedImageOp op,
                          int x,
                          int y) {
        doGraphicsOp(op.getBounds2D(img),
                     "drawImage",
                     new Class[] {java.awt.image.BufferedImage.class,
                                      java.awt.image.BufferedImageOp.class,
                                      int.class, int.class},
                     new Object[] {img, op, new Integer(x), new Integer(y)});
    }

    public void drawString(String s,
                           float x,
                           float y) {
        Rectangle2D r2d =
            getFontMetrics(getFont()
                           ).getStringBounds(s, this);

        r2d.setRect(x, y - r2d.getHeight() + 1,
                    r2d.getWidth(), r2d.getHeight());

        doGraphicsOp(r2d,
                     "drawString",
                     new Class[] {java.lang.String.class,
                                      float.class, float.class},
                     new Object[] {s, new Float(x), new Float(y)});
    }

    public void drawString(AttributedCharacterIterator iterator,
                           int x, int y) {
        Rectangle2D r2d =
            getFontMetrics(getFont()
                           ).getStringBounds(iterator,
                                             iterator.getBeginIndex(),
                                             iterator.getEndIndex(),
                                             this);

        r2d.setRect(x, y - r2d.getHeight() + 1,
                    r2d.getWidth(), r2d.getHeight());

        doGraphicsOp(r2d,
                     "drawString",
                     new Class[] {java.text.AttributedCharacterIterator.class,
                                      int.class, int.class},
                     new Object[] {iterator, new Integer(x), new Integer(y)});
    }

    public void drawString(AttributedCharacterIterator iterator,
                           float x, float y) {
        Rectangle2D r2d =
            getFontMetrics(getFont()
                           ).getStringBounds(iterator,
                                             iterator.getBeginIndex(),
                                             iterator.getEndIndex(),
                                             this);

        r2d.setRect(x, y - r2d.getHeight() + 1,
                    r2d.getWidth(), r2d.getHeight());

        doGraphicsOp(r2d,
                     "drawString",
                     new Class[] {java.text.AttributedCharacterIterator.class,
                                      float.class, float.class},
                     new Object[] {iterator, new Float(x), new Float(y)});
    }

    public void drawGlyphVector(GlyphVector g,
                                float x,
                                float y) {
        doGraphicsOp(g.getVisualBounds(),
                     "drawGlyphVector",
                     new Class[] {java.awt.font.GlyphVector.class,
                                      float.class, float.class},
                     new Object[] {g, new Float(x), new Float(y)});
    }

    public void fill(Shape s) {
        doGraphicsOp(s.getBounds(),
                     "fill",
                     new Class[] {java.awt.Shape.class},
                     new Object[] {s});
    }

    public boolean hit(Rectangle rect,
                       Shape s,
                       boolean onStroke) {
        Graphics2D g2d = getBogusGraphics2D(true);

        boolean hitTarget = g2d.hit(rect, s, onStroke);

        g2d.dispose();

        return hitTarget;
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        Graphics2D g2d = getBogusGraphics2D(true);

        GraphicsConfiguration gConf = g2d.getDeviceConfiguration();

        g2d.dispose();

        return gConf;
    }

    public void setComposite(Composite comp) {
        composite = comp;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public void setStroke(Stroke s) {
        stroke = s;
    }

    public void setRenderingHint(RenderingHints.Key hintKey,
                                 Object hintValue) {
        renderingHints.put(hintKey, hintValue);
    }

    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return renderingHints.get(hintKey);
    }

    public void setRenderingHints(Map hints) {
        renderingHints.putAll(hints);
    }

    public RenderingHints getRenderingHints() {
        return renderingHints;
    }

    public void translate(int x, int y) {
        origin = new Point(x, y);
        transform.translate((double)x, (double)y);
    }

    public void translate(double x, double y) {
        transform.translate(x, y);
    }

    public void rotate(double theta) {
        transform.rotate(theta);
    }

    public void rotate(double theta, double x, double y) {
        transform.rotate(theta, x, y);
    }

    public void scale(double sx, double sy) {
        transform.scale(sx, sy);
    }

    public void shear(double shx, double shy) {
        transform.shear(shx, shy);
    }

    public void transform(AffineTransform Tx) {
        transform.concatenate(Tx);
    }

    public void setTransform(AffineTransform Tx) {
        transform = Tx;
    }

    public AffineTransform getTransform() {
        return transform;
    }

    public Paint getPaint() {
        return paint;
    }

    public Composite getComposite() {
        return composite;
    }

    public void setBackground(Color color) {
        background = color;
    }

    public Color getBackground() {
        return background;
    }

    public Stroke getStroke() {
        return stroke;
    }

    public void clip(Shape s) {
        if(clip == null) {
            clip = s;
        } else {
            Area clipArea = (clip instanceof Area ?
                             (Area)clip : new Area(clip));
            clipArea.intersect(s instanceof Area ? (Area)s : new Area(s));
            clip = clipArea;
        }
    }

    public FontRenderContext getFontRenderContext() {
        Graphics2D g2d = getBogusGraphics2D(true);

        FontRenderContext fontRenderContext = g2d.getFontRenderContext();

        g2d.dispose();

        return fontRenderContext;
    }

    void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener = null;
        if (renderingHints != null)
            listener =
                (ImagingListener)renderingHints.get(JAI.KEY_IMAGING_LISTENER);

        if (listener == null)
            listener = JAI.getDefaultInstance().getImagingListener();
        listener.errorOccurred(message, e, this, false);
    }
}
