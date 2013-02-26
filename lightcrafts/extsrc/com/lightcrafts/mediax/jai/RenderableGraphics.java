/*
 * $RCSfile: RenderableGraphics.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:20 $
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
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderContext;
import java.lang.reflect.Method;
import java.text.AttributedCharacterIterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * An implementation of <code>Graphics2D</code> with
 * <code>RenderableImage</code> semantics. In other words, content may be
 * drawn into the image using the <code>Graphics2D</code> interface and later
 * be turned into <code>RenderedImage</code>s with different resolutions and
 * characteristics.
 *
 * <p> A <code>RenderableGraphics</code> occupies a region of the plane
 * specified at the time of construction.
 *
 * <p> The contents of <code>RenderableImage</code>s that are drawn onto a
 * <code>RenderableGraphics</code> are accessed only at the time of rendering,
 * not the time of drawing.
 *
 * <p> Since the methods of this class all derive from <code>Graphics2D</code>
 * and <code>RenderableImage</code>, they are not all commented individually.
 *
 * @see java.awt.Graphics2D
 * @see java.awt.image.renderable.RenderableImage
 */
public class RenderableGraphics extends Graphics2D implements RenderableImage {

    // Constants
    private static final Class GRAPHICS2D_CLASS = Graphics2D.class;

    // Bounding rectangle
    private Rectangle2D dimensions;

    // Linked list of (Method, Object[]) pairs.
    private LinkedList opArgList;

    // Graphics state information
    private Point origin;
    private Shape clip;
    private Color color;
    private Font font;

    // Graphics2D state information
    private Color background;
    private Composite composite;
    private Paint paint;
    private Stroke stroke;
    private RenderingHints renderingHints = new RenderingHints(null);
    private AffineTransform transform;

    /**
     * Constructs a <code>RenderableGraphics</code> given a bounding
     * <code>Rectangle2D</code>.
     *
     * @param dimensions The bounding <code>Rectangle2D</code>.
     */
    public RenderableGraphics(Rectangle2D dimensions) {
        this(dimensions, new LinkedList(), new Point(0, 0), null);
    }

    /**
     * Constructs a <code>RenderableGraphics</code> given a bounding
     * <code>Rectangle2D</code>, an origin, and a <code>Graphics2D</code>
     * object from which to initialize the <code>RenderableGraphics</code>
     * state. The <code>Graphics2D</code> may be null.
     *
     * @param dimensions The bounding <code>Rectangle2D</code>.
     * @param opArgList  The list of operations and arguments.
     * @param dimensions The origin.
     * @param dimensions The <code>Graphics2D</code> state source; may be null.
     */
    private RenderableGraphics(Rectangle2D dimensions, LinkedList opArgList,
                               Point origin, Graphics2D g) {
        if(dimensions.isEmpty()) {
            throw new RuntimeException(JaiI18N.getString("RenderableGraphics0"));
        }

        // -- RenderableGraphics state --
        this.dimensions = dimensions;
        this.opArgList = opArgList;

        // Use the Graphics2D passed in or create one.
        Graphics2D g2d = g;
        if(g2d == null) {
            g2d = getBogusGraphics2D();
        }

        // -- java.awt.Graphics state --
        this.origin = (Point)origin.clone();
        setClip(g2d.getClip());
        setColor(g2d.getColor());
        setFont(g2d.getFont());

        // -- java.awt.Graphics2D state --
        setBackground(g2d.getBackground());
        setComposite(g2d.getComposite());
        setRenderingHints(g2d.getRenderingHints());
        setStroke(g2d.getStroke());
        setTransform(g2d.getTransform());

        // Dispose of the Graphics2D if it was created within this method.
        if(g == null) g2d.dispose();
    }

    /**
     * Creates a bogus <code>Graphics2D</code> object to be used to retrieve
     * information dependent on system aspects which are image-independent.
     *
     * <p>The <code>dispose()</code> method of the <code>Graphics2D</code>
     * object returned should be called to free the associated resources as\
     * soon as possible.
     *
     * @return A <code>Graphics2D</code> object.
     */
    private Graphics2D getBogusGraphics2D() {
        TiledImage ti = createTiledImage(renderingHints,
                                         dimensions.getBounds());

        return ti.createGraphics();
    }

    /**
     * Create a TiledImage to be used as the canvas.
     *
     * @param hints RenderingHints from which to derive an ImageLayout.
     * @param bounds The bounding box of the TiledImage.
     *
     * @return A TiledImage.
     */
    private TiledImage createTiledImage(RenderingHints hints,
                                        Rectangle bounds) {
        // Set the default tile size.
        int tileWidth = bounds.width;
        int tileHeight = bounds.height;

        // Retrieve layout information from the hints. The tile size hints
        // are ignored if a SampleModel hint is supplied. Set the hints
        // observed if any hints are used.
        SampleModel sm = null;
        ColorModel cm = null;
        RenderingHints hintsObserved = null;
        if(hints != null) {
            // Get the ImageLayout.
            ImageLayout layout = (ImageLayout)hints.get(JAI.KEY_IMAGE_LAYOUT);

            if(layout != null) {
                // Initialize the observed hint variables.
                hintsObserved = new RenderingHints(null);
                ImageLayout layoutObserved = new ImageLayout();

                // Get the SampleModel.
                if(layout.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
                    sm = layout.getSampleModel(null);
                    if(sm.getWidth() != tileWidth ||
                       sm.getHeight() != tileHeight) {
                        sm = sm.createCompatibleSampleModel(tileWidth,
                                                            tileHeight);
                    }
                    if(layoutObserved != null) {
                        layoutObserved.setSampleModel(sm);
                    }
                }

                // Get the ColorModel.
                if(layout.isValid(ImageLayout.COLOR_MODEL_MASK)) {
                    cm = layout.getColorModel(null);
                    if(layoutObserved != null) {
                        layoutObserved.setColorModel(cm);
                    }
                }

                // Get the tile dimensions.
                if(layout.isValid(ImageLayout.TILE_WIDTH_MASK)) {
                    tileWidth = layout.getTileWidth(null);
                    if(layoutObserved != null) {
                        layoutObserved.setTileWidth(tileWidth);
                    }
                } else if(sm != null) {
                    tileWidth = sm.getWidth();
                }
                if(layout.isValid(ImageLayout.TILE_HEIGHT_MASK)) {
                    tileHeight = layout.getTileHeight(null);
                    if(layoutObserved != null) {
                        layoutObserved.setTileHeight(tileHeight);
                    }
                } else if(sm != null) {
                    tileHeight = sm.getHeight();
                }

                // Set the observed hints layout.
                hintsObserved.put(JAI.KEY_IMAGE_LAYOUT, layoutObserved);
            } // layout != null
        } // hints != null

        // Ensure that the SampleModel is compatible with the tile size.
        if(sm != null &&
           (sm.getWidth() != tileWidth || sm.getHeight() != tileHeight)) {
            sm = sm.createCompatibleSampleModel(tileWidth, tileHeight);
        }

        // Attempt to derive compatible SampleModel/ColorModel combination.
        if(cm != null && (sm == null ||
                          !JDKWorkarounds.areCompatibleDataModels(sm, cm))) {
            // If the ColorModel is non-null and the SampleModel is null
            // or incompatible then create a new SampleModel.
            sm = cm.createCompatibleSampleModel(tileWidth, tileHeight);
        } else if(cm == null && sm != null) {
            // If the ColorModel is null but the SampleModel is not,
            // try to guess a reasonable ColorModel.
            cm = PlanarImage.createColorModel(sm);

            // If the ColorModel is still null, set it to the RGB default
            // ColorModel if the latter is compatible with the SampleModel.
            ColorModel cmRGB = ColorModel.getRGBdefault();
            if(cm == null &&
               JDKWorkarounds.areCompatibleDataModels(sm, cmRGB)) {
                cm = cmRGB;
            }
        }

        // Create the TiledImage.
        TiledImage ti = null;
        if(sm != null) {
            // Use the (derived) SampleModel and ColorModel.
            ti = new TiledImage(bounds.x, bounds.y,
                                bounds.width, bounds.height,
                                bounds.x, bounds.y,
                                sm, cm);
        } else {
            // Default to a PixelInterleaved TiledImage.
            ti = TiledImage.createInterleaved(bounds.x, bounds.y,
                                              bounds.width, bounds.height,
                                              3, DataBuffer.TYPE_BYTE,
                                              tileWidth, tileHeight,
                                              new int[] {0, 1, 2});
        }

        // Set the HINTS_OBSERVED property of the TiledImage.
        if(hintsObserved != null) {
            ti.setProperty("HINTS_OBSERVED", hintsObserved);
        }

        return ti;
    }

    /**
     * Queue a <code>Graphics2D</code> operation and its argument list in the
     * linked list of operations and arguments. The name of the operation and
     * the array of class types of its arguments are used to determine the
     * associated <code>Method</code> object. The <code>Method</code> object
     * and array of <code>Object</code> arguments are appended to the list as
     * an ordered pair of the form (<code>Method</code>,<code>Object</code>[]).
     *
     * @param name The name of the <code>Graphics2D</code> operation.
     * @param argTypes An array of the <code>Classes</code> of the arguments
     * of the specified operation.
     * @param args The arguments of the operation as an array of
     * <code>Object</code>s.
     */
    private void queueOpArg(String name, Class[] argTypes, Object[] args) {
        // Determine the Method object associated with the Graphics2D method
        // having the indicated name. The search begins with the Graphics2D
        // class and continues to its superclasses until the Method is found.
        Method method = null;
        try {
            method = GRAPHICS2D_CLASS.getMethod(name, argTypes);
        } catch(Exception e) {
            String message = JaiI18N.getString("TiledGraphicsGraphics2") + name;
            sendExceptionToListener(message, new ImagingException(e));
//            throw new RuntimeException(e.getMessage());
        }

        // Queue the Method object and the Object[] argument array as an
        // ordered pair (Method, Object[]).
        opArgList.addLast(method);
        opArgList.addLast(args);
    }

    /**
     * Evaulate the queue of <code>Graphics2D</code> operations on the
     * specified <code>Graphics2D</code> object.
     *
     * @param g2d The <code>Graphics2D</code> on which to evaluate the
     * operation queue.
     */
    private void evaluateOpList(Graphics2D g2d) {
        if(opArgList == null) {
            return;
        }

        ListIterator li = opArgList.listIterator(0);

        while(li.hasNext()) {
            Method method = (Method)li.next();
            Object[] args = (Object[])li.next();

            try {
                method.invoke(g2d, args);
            } catch(Exception e) {
                String message = JaiI18N.getString("TiledGraphicsGraphics4") + method;
                sendExceptionToListener(message, new ImagingException(e));
//                e.printStackTrace();
//                throw new RuntimeException(e.getMessage());
            }
        }
    }

    // ---------- Methods from java.awt.Graphics ----------

    public Graphics create() {
        return new RenderableGraphics(dimensions, opArgList, origin, this);
    }

    // public Graphics create(int x, int y, int width, int height)
    // -- implemented in Graphics superclass.

    public Color getColor() {
        return color;
    }

    public void setColor(Color c) {
        color = c;

        queueOpArg("setColor",
                   new Class[] {java.awt.Color.class},
                   new Object[] {c});
    }

    public void setPaintMode() {
        queueOpArg("setPaintMode", null, null);
    }

    public void setXORMode(Color c1) {
        queueOpArg("setXORMode",
                   new Class[] {java.awt.Color.class},
                   new Object[] {c1});
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;

        queueOpArg("setFont",
                   new Class[] {java.awt.Font.class},
                   new Object[] {font});
    }

    public FontMetrics getFontMetrics(Font f) {
        Graphics2D g2d = getBogusGraphics2D();

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
        clip = new Rectangle(x, y, width, height);

        queueOpArg("setClip",
                   new Class[] {int.class, int.class, int.class, int.class},
                   new Object[] {new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height)});
    }

    public Shape getClip() {
        return clip;
    }

    public void setClip(Shape clip) {
        this.clip = clip;

        queueOpArg("setClip",
                   new Class[] {java.awt.Shape.class},
                   new Object[] {clip});
    }

    public void copyArea(int x, int y, int width, int height,
                         int dx, int dy) {
        queueOpArg("copyArea",
                   new Class[] {int.class, int.class, int.class,
                                    int.class, int.class, int.class},
                   new Object[] {new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height),
                                     new Integer(dx), new Integer(dy)});
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        queueOpArg("drawLine",
                   new Class[] {int.class, int.class, int.class, int.class},
                   new Object[] {new Integer(x1), new Integer(y1),
                                     new Integer(x2), new Integer(y2)});
    }

    public void fillRect(int x, int y, int width, int height) {
        queueOpArg("fillRect",
                   new Class[] {int.class, int.class, int.class, int.class},
                   new Object[] {new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height)});
    }

    // public void drawRect(int x, int y, int width, int height)
    // -- implemented in Graphics superclass

    public void clearRect(int x, int y, int width, int height) {
        queueOpArg("clearRect",
                   new Class[] {int.class, int.class, int.class, int.class},
                   new Object[] {new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height)});
    }

    public void drawRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        queueOpArg("drawRoundRect",
                   new Class[] {int.class, int.class, int.class,
                                    int.class, int.class, int.class},
                   new Object[] {new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height),
                                     new Integer(arcWidth),
                                     new Integer(arcHeight)});
    }

    public void fillRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        queueOpArg("fillRoundRect",
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
        queueOpArg("draw3DRect",
                   new Class[] {int.class, int.class, int.class, int.class,
                                    boolean.class},
                   new Object[] {new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height),
                                     new Boolean(raised)});
    }

    // fill3DRect() is implemented in the Graphics superclass but is
    // overridden in Graphics2D and so must be implemented here.
    public void fill3DRect(int x, int y, int width, int height,
                           boolean raised) {
        queueOpArg("fill3DRect",
                   new Class[] {int.class, int.class, int.class, int.class,
                                    boolean.class},
                   new Object[] {new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height),
                                     new Boolean(raised)});
    }

    public void drawOval(int x, int y, int width, int height) {
        queueOpArg("drawOval",
                   new Class[] {int.class, int.class, int.class, int.class},
                   new Object[] {new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height)});
    }

    public void fillOval(int x, int y, int width, int height) {
        queueOpArg("fillOval",
                   new Class[] {int.class, int.class, int.class, int.class},
                   new Object[] {new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height)});
    }

    public void drawArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        queueOpArg("drawArc",
                   new Class[] {int.class, int.class, int.class,
                                    int.class, int.class, int.class},
                   new Object[] {new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height),
                                     new Integer(startAngle),
                                     new Integer(arcAngle)});
    }

    public void fillArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        queueOpArg("fillArc",
                   new Class[] {int.class, int.class, int.class,
                                    int.class, int.class, int.class},
                   new Object[] {new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height),
                                     new Integer(startAngle),
                                     new Integer(arcAngle)});
    }

    public void drawPolyline(int xPoints[], int yPoints[], int nPoints) {
        Class intArrayClass = xPoints.getClass();
        queueOpArg("drawPolyline",
                   new Class[] {intArrayClass, intArrayClass, int.class},
                   new Object[] {xPoints, yPoints, new Integer(nPoints)});
    }

    public void drawPolygon(int xPoints[], int yPoints[], int nPoints) {
        Class intArrayClass = xPoints.getClass();
        queueOpArg("drawPolygon",
                   new Class[] {intArrayClass, intArrayClass, int.class},
                   new Object[] {xPoints, yPoints, new Integer(nPoints)});
    }

    // public void drawPolygon -- implemented in Graphics superclass

    public void fillPolygon(int xPoints[], int yPoints[], int nPoints) {
        Class intArrayClass = xPoints.getClass();
        queueOpArg("fillPolygon",
                   new Class[] {intArrayClass, intArrayClass, int.class},
                   new Object[] {xPoints, yPoints, new Integer(nPoints)});
    }

    // public void fillPolygon -- implemented in Graphics superclass

    public void drawString(String str, int x, int y) {
        queueOpArg("drawString",
                   new Class[] {java.lang.String.class,
                                    int.class, int.class},
                   new Object[] {str, new Integer(x), new Integer(y)});
    }

    // public void drawChars -- implemented in Graphics superclass
    // public void drawBytes -- implemented in Graphics superclass

    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        queueOpArg("drawImage",
                   new Class[] {java.awt.Image.class,
                                    int.class, int.class,
                                    java.awt.image.ImageObserver.class},
                   new Object[] {img, new Integer(x), new Integer(y),
                                     observer});
        return true;
    }

    public boolean drawImage(Image img, int x, int y,
                             int width, int height,
                             ImageObserver observer) {
        queueOpArg("drawImage",
                   new Class[] {java.awt.Image.class,
                                    int.class, int.class, int.class, int.class,
                                    java.awt.image.ImageObserver.class},
                   new Object[] {img, new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height),
                                     observer});
        return true;
    }

    public boolean drawImage(Image img, int x, int y,
                             Color bgcolor,
                             ImageObserver observer) {
        queueOpArg("drawImage",
                   new Class[] {java.awt.Image.class,
                                    int.class, int.class,
                                    java.awt.Color.class,
                                    java.awt.image.ImageObserver.class},
                   new Object[] {img, new Integer(x), new Integer(y),
                                     bgcolor, observer});
        return true;
    }

    public boolean drawImage(Image img, int x, int y,
                             int width, int height,
                             Color bgcolor,
                             ImageObserver observer) {
        queueOpArg("drawImage",
                   new Class[] {java.awt.Image.class,
                                    int.class, int.class, int.class, int.class,
                                    java.awt.Color.class,
                                    java.awt.image.ImageObserver.class},
                   new Object[] {img, new Integer(x), new Integer(y),
                                     new Integer(width), new Integer(height),
                                     bgcolor, observer});
        return true;
    }

    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             ImageObserver observer) {
        queueOpArg("drawImage",
                   new Class[] {java.awt.Image.class,
                                    int.class, int.class, int.class, int.class,
                                    int.class, int.class, int.class, int.class,
                                    java.awt.image.ImageObserver.class},
                   new Object[] {img, new Integer(dx1), new Integer(dy1),
                                     new Integer(dx2), new Integer(dy2),
                                     new Integer(sx1), new Integer(sy1),
                                     new Integer(sx2), new Integer(sy2),
                                     observer});
        return true;
    }

    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             Color bgcolor,
                             ImageObserver observer) {
        queueOpArg("drawImage",
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
        return true;
    }

    public void dispose() {
        queueOpArg("dispose", null, null);
    }

    // public void finalize -- implemented in Graphics superclass
    // public String toString -- implemented in Graphics superclass

    // ---------- Methods from java.awt.Graphics2D ----------

    public void addRenderingHints(Map hints) {
        renderingHints.putAll(hints);

        queueOpArg("addRenderingHints",
                   new Class[] {java.util.Map.class},
                   new Object[] {hints});
    }

    public void draw(Shape s) {
        queueOpArg("draw",
                   new Class[] {java.awt.Shape.class},
                   new Object[] {s});
    }

    public boolean drawImage(Image img,
                             AffineTransform xform,
                             ImageObserver obs) {
        queueOpArg("drawImage",
                   new Class[] {java.awt.Image.class,
                                    java.awt.geom.AffineTransform.class,
                                    java.awt.image.ImageObserver.class},
                   new Object[] {img, xform, obs});
        return true;
    }

    public void drawRenderedImage(RenderedImage img,
                                  AffineTransform xform) {

        queueOpArg("drawRenderedImage",
                   new Class[] {java.awt.image.RenderedImage.class,
                                java.awt.geom.AffineTransform.class},
                   new Object[] {img, xform});
    }

    public void drawRenderableImage(RenderableImage img,
                                    AffineTransform xform) {
        queueOpArg("drawRenderableImage",
                   new Class[] {java.awt.image.renderable.RenderableImage.class,
                                java.awt.geom.AffineTransform.class},
                   new Object[] {img, xform});
    }

    public void drawImage(BufferedImage img,
                          BufferedImageOp op,
                          int x,
                          int y) {
        queueOpArg("drawImage",
                   new Class[] {java.awt.image.BufferedImage.class,
                                    java.awt.image.BufferedImageOp.class,
                                    int.class, int.class},
                   new Object[] {img, op, new Integer(x), new Integer(y)});
    }

    public void drawString(String s,
                           float x,
                           float y) {
        queueOpArg("drawString",
                   new Class[] {java.lang.String.class,
                                    float.class, float.class},
                   new Object[] {s, new Float(x), new Float(y)});
    }

    public void drawString(AttributedCharacterIterator iterator,
                           int x, int y) {
        queueOpArg("drawString",
                   new Class[] {java.text.AttributedCharacterIterator.class,
                                    int.class, int.class},
                   new Object[] {iterator, new Integer(x), new Integer(y)});
    }

    public void drawString(AttributedCharacterIterator iterator,
                           float x, float y) {
        queueOpArg("drawString",
                   new Class[] {java.text.AttributedCharacterIterator.class,
                                    float.class, float.class},
                   new Object[] {iterator, new Float(x), new Float(y)});
    }

    public void drawGlyphVector(GlyphVector v,
                                float x,
                                float y) {
        queueOpArg("drawGlyphVector",
                   new Class[] {java.awt.font.GlyphVector.class,
                                    float.class, float.class},
                   new Object[] {v, new Float(x), new Float(y)});

    }

    public void fill(Shape s) {
        queueOpArg("fill",
                   new Class[] {java.awt.Shape.class},
                   new Object[] {s});
    }

    public boolean hit(Rectangle rect,
                       Shape s,
                       boolean onStroke) {
        Graphics2D g2d = getBogusGraphics2D();

        boolean hitTarget = g2d.hit(rect, s, onStroke);

        g2d.dispose();

        return hitTarget;
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        Graphics2D g2d = getBogusGraphics2D();

        GraphicsConfiguration gConf = g2d.getDeviceConfiguration();

        g2d.dispose();

        return gConf;
    }

    public FontRenderContext getFontRenderContext() {
        Graphics2D g2d = getBogusGraphics2D();

        FontRenderContext fontRenderContext = g2d.getFontRenderContext();

        g2d.dispose();

        return fontRenderContext;
    }

    public void setComposite(Composite comp) {
        composite = comp;
        queueOpArg("setComposite",
                   new Class[] {java.awt.Composite.class},
                   new Object[] {comp});
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
        queueOpArg("setPaint",
                   new Class[] {java.awt.Paint.class},
                   new Object[] {paint});
    }

    public void setStroke(Stroke s) {
        stroke = s;
        queueOpArg("setStroke",
                   new Class[] {java.awt.Stroke.class},
                   new Object[] {s});
    }

    public void setRenderingHint(RenderingHints.Key hintKey,
                                 Object hintValue) {
        renderingHints.put(hintKey, hintValue);

        queueOpArg("setRenderingHint",
                   new Class[] {java.awt.RenderingHints.Key.class,
                                    java.lang.Object.class},
                   new Object[] {hintKey, hintValue});
    }

    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return renderingHints.get(hintKey);
    }

    public void setRenderingHints(Map hints) {
        renderingHints.putAll(hints);

        queueOpArg("setRenderingHints",
                   new Class[] {java.util.Map.class},
                   new Object[] {hints});
    }

    public RenderingHints getRenderingHints() {
        return renderingHints;
    }

    public void translate(int x, int y) {
        origin = new Point(x, y);
        transform.translate((double)x, (double)y);

        queueOpArg("translate",
                   new Class[] {int.class, int.class},
                   new Object[] {new Integer(x), new Integer(y)});
    }

    public void translate(double x, double y) {
        transform.translate(x, y);

        queueOpArg("translate",
                   new Class[] {double.class, double.class},
                   new Object[] {new Double(x), new Double(y)});
    }

    public void rotate(double theta) {
        transform.rotate(theta);

        queueOpArg("rotate",
                   new Class[] {double.class},
                   new Object[] {new Double(theta)});
    }

    public void rotate(double theta, double x, double y) {
        transform.rotate(theta, x, y);

        queueOpArg("rotate",
                   new Class[] {double.class, double.class, double.class},
                   new Object[] {new Double(theta),
                                     new Double(x), new Double(y)});
    }

    public void scale(double sx, double sy) {
        transform.scale(sx, sy);

        queueOpArg("scale",
                   new Class[] {double.class, double.class},
                   new Object[] {new Double(sx), new Double(sy)});
    }

    public void shear(double shx, double shy) {
        transform.shear(shx, shy);

        queueOpArg("shear",
                   new Class[] {double.class, double.class},
                   new Object[] {new Double(shx), new Double(shy)});
    }

    public void transform(AffineTransform Tx) {
        transform.concatenate(Tx);

        queueOpArg("transform",
                   new Class[] {java.awt.geom.AffineTransform.class},
                   new Object[] {Tx});
    }

    public void setTransform(AffineTransform Tx) {
        transform = Tx;

        queueOpArg("setTransform",
                   new Class[] {java.awt.geom.AffineTransform.class},
                   new Object[] {Tx});
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

        queueOpArg("setBackground",
                   new Class[] {java.awt.Color.class},
                   new Object[] {color});
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

        queueOpArg("clip",
                   new Class[] {java.awt.Shape.class},
                   new Object[] {s});
    }

    // ---------- Methods from RenderableImage ----------

    public Vector getSources() {
        return null;
    }

    public Object getProperty(String name) {
        return Image.UndefinedProperty;
    }

    public String[] getPropertyNames() {
        return null;
    }

    public boolean isDynamic() {
        return false;
    }

    public float getWidth() {
        return (float)dimensions.getWidth();
    }

    public float getHeight() {
        return (float)dimensions.getHeight();
    }

    public float getMinX() {
        return (float)dimensions.getMinX();
    }

    public float getMinY() {
        return (float)dimensions.getMinY();
    }

    public RenderedImage createScaledRendering(int w, int h,
                                               RenderingHints hints) {
        if(w <= 0 && h <= 0) {
            throw new IllegalArgumentException(JaiI18N.getString("RenderableGraphics1"));
        } else if(w <= 0) {
            w = (int)Math.round(h*dimensions.getWidth()/dimensions.getHeight());
        } else if(h <= 0) {
            h = (int)Math.round(w*dimensions.getHeight()/dimensions.getWidth());
        }

        double sx = (double)w/dimensions.getWidth();
        double sy = (double)h/dimensions.getHeight();
        AffineTransform usr2dev = new AffineTransform();
        usr2dev.setToScale(sx, sy);

        return createRendering(new RenderContext(usr2dev, hints));
    }

    public RenderedImage createDefaultRendering() {
        return createRendering(new RenderContext(new AffineTransform()));
    }

    /**
     * Creates a RenderedImage that represents a rendering of this image
     * using a given RenderContext.  This is the most general way to obtain a
     * rendering of a RenderableImage.
     *
     * <p> The created RenderedImage may have a property identified
     * by the String HINTS_OBSERVED to indicate which RenderingHints
     * (from the RenderContext) were used to create the image.  In addition
     * any RenderedImages that are obtained via the getSources() method on
     * the created RenderedImage may have such a property.
     *
     * <p> The bounds of the <code>RenderedImage</code> are determined from
     * the dimensions parameter passed to the <code>RenderableGraphics</code>
     * constructor.  These bounds will be transformed by any
     * <code>AffineTransform</code> from the <code>RenderContext</code>.
     * The <code>RenderingHints</code> from the <code>RenderContext</code> may
     * be used to specify the tile width and height, <code>SampleModel</code>,
     * and <code>ColorModel</code> by supplying an <code>ImageLayout</code>
     * hint.  The precedence for determining tile width and height is to use
     * firstly values provided explicitly via the <code>ImageLayout</code>,
     * secondly the width and height of the <code>SampleModel</code> in the
     * hint, and thirdly the bounds of the <code>RenderableGraphics</code>
     * object after transformation.
     *
     * <p> If either the <code>SampleModel</code> or <code>ColorModel</code>
     * is null, an attempt will be made to derive a compatible value for the
     * null object from the non-null object.  If they are both null, a 3-band
     * byte <code>TiledImage</code> with a null <code>ColorModel</code> and a
     * <code>PixelInterleavedSampleModel</code> will be created.
     *
     * @param renderContext the RenderContext to use to produce the rendering.
     * @return a RenderedImage containing the rendered data.
     */
    public RenderedImage createRendering(RenderContext renderContext) {
        // Unpack the RenderContext and set local variables based thereon
        AffineTransform usr2dev = renderContext.getTransform();
        if(usr2dev == null) {
            usr2dev = new AffineTransform();
        }
        RenderingHints hints = renderContext.getRenderingHints();
        Shape aoi = renderContext.getAreaOfInterest();
        if(aoi == null) {
            aoi = dimensions.getBounds();
        }

        // Transform the area of interest.
        Shape transformedAOI = usr2dev.createTransformedShape(aoi);

        // Create a TiledImage to be used as the canvas.
        TiledImage ti = createTiledImage(hints, transformedAOI.getBounds());

        // Create the associated Graphics2D object and translate it to
        // account for the initial origin specified in the RenderableGraphics
        // constructor.
        Graphics2D g2d = ti.createGraphics();

        // NOTE: There is no need to copy the state to the TiledImageGraphics
        // object here as all modifications are contained in the opArgList.

        // Set Graphics2D state variables according to the RenderContext
        if(!usr2dev.isIdentity()) {
            AffineTransform tf = getTransform();
            tf.concatenate(usr2dev);
            g2d.setTransform(tf);
        }
        if(hints != null) {
            g2d.addRenderingHints(hints);
        }
        g2d.setClip(aoi);

        // Evaluate the operation queue
        evaluateOpList(g2d);

        // Free Graphics2D resources
        g2d.dispose();

        return ti;
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
