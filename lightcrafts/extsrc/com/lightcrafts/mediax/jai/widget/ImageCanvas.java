/*
 * $RCSfile: ImageCanvas.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:58 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.widget;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.HashSet;
import java.util.Iterator;

import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.PlanarImage;


/**
 * A simple output widget for a RenderedImage.  ImageCanvas subclasses
 * java.awt.Canvas, and can be used in any context that calls for a
 * Canvas.  It monitors resize and update events and automatically
 * requests tiles from its source on demand.  Any displayed area outside
 * the image is displayed in grey.
 *
 * <p> There is currently no policy regarding what sorts of widgets,
 * if any, will be part of JAI.
 *
 * <p> Due to the limitations of BufferedImage, only TYPE_BYTE of band
 * 1, 2, 3, 4, and TYPE_USHORT of band 1, 2, 3 images can be displayed
 * using this widget.
 *
 *
 * <p>
 * This class has been deprecated.  The source
 * code has been moved to the samples/widget
 * directory.  These widgets are no longer
 * supported.
 *
 * @deprecated as of JAI 1.1
 */
public class ImageCanvas extends Canvas {
    
    /** The source RenderedImage. */
    protected RenderedImage im;
    /** The image's SampleModel. */
    protected SampleModel sampleModel;
    /** The image's ColorModel or one we supply. */
    protected ColorModel colorModel;

    /** The image's min X tile. */
    protected int minTileX;
    /** The image's max X tile. */
    protected int maxTileX;
    /** The image's min Y tile. */
    protected int minTileY;
    /** The image's max Y tile. */
    protected int maxTileY;
    /** The image's tile width. */
    protected int tileWidth;
    /** The image's tile height. */
    protected int tileHeight;
    /** The image's tile grid X offset. */
    protected int tileGridXOffset;
    /** The image's tile grid Y offset. */
    protected int tileGridYOffset;

    protected int imWidth;
    protected int imHeight;

    /** used to center image in it's container */
    protected int padX;
    protected int padY;

    protected boolean drawBorder = false;

    /** The pixel to display in the upper left corner or the canvas. */ 
    protected int originX;
    /** The pixel to display in the upper left corner or the canvas. */ 
    protected int originY;
    /** The width of the canvas. */
    protected int canvasWidth = 0;
    /** The height of the canvas. */
    protected int canvasHeight = 0;

    private Color grayColor = new Color(192, 192, 192);
    
    private Color backgroundColor = null;

    /** Initializes the ImageCanvas. */
    private synchronized void initialize() {
        int mx = im.getMinX();
        int my = im.getMinY();
        if ((mx < 0) || (my < 0)) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(im);
            pb.add((float)Math.max(-mx, 0));
            pb.add((float)Math.max(-my, 0));
            pb.add(new InterpolationNearest());
            im = JAI.create("translate", pb, null);
        }

        this.sampleModel = im.getSampleModel();

	// First check whether the opimage has already set a suitable ColorModel
	this.colorModel = im.getColorModel();
	if (this.colorModel == null) {
	    // If not, then create one.
	    this.colorModel = PlanarImage.createColorModel(im.getSampleModel());
	    if (this.colorModel == null) {
		throw new IllegalArgumentException(JaiI18N.getString("ImageCanvas0"));
	    }
	}

        Object col = im.getProperty("background_color");
        if (col != Image.UndefinedProperty) {
            backgroundColor = (Color)col;
        }

        minTileX = im.getMinTileX();
        maxTileX = im.getMinTileX() + im.getNumXTiles() - 1;
        minTileY = im.getMinTileY();
        maxTileY = im.getMinTileY() + im.getNumYTiles() - 1;
        tileWidth  = im.getTileWidth();
        tileHeight = im.getTileHeight();
        tileGridXOffset = im.getTileGridXOffset();
        tileGridYOffset = im.getTileGridYOffset();

        imWidth  = im.getMinX() + im.getWidth();
        imHeight = im.getMinY() + im.getHeight();

        originX = originY = 0;
    }

    /** 
     * Constructs an ImageCanvas to display a RenderedImage.
     *
     * @param im a RenderedImage to be displayed.
     * @param drawBorder true if a raised border is desired.
     */
    public ImageCanvas(RenderedImage im, boolean drawBorder) {
        this.im = im;
        this.drawBorder = drawBorder;
        initialize();
    }

    /** 
     * Constructs an ImageCanvas to display a RenderedImage.
     *
     * @param im a RenderedImage to be displayed.
     */
    public ImageCanvas(RenderedImage im) {
        this(im, false);
    }

    public void addNotify() {
        super.addNotify();
        initialize();
    }

    /** Changes the source image to a new RenderedImage. */
    public synchronized void set(RenderedImage im) {
        this.im = im;
        initialize();
        repaint();
    }
    
    /** Changes the pixel to set Origin at x,y */
    public void setOrigin(int x, int y) {
        padX = 0;
        padY = 0;
        originX = x;
        originY = y;
        repaint();
    }
   
    public int getXOrigin() {
        return originX;
    }

    public int getYOrigin() {
        return originY;
    }

    public int getXPad() {
        return padX;
    }

    public int getYPad() {
        return padY;
    }

    public Dimension getMinimumSize() {
        return new Dimension(im.getMinX() + im.getWidth() +
                             (drawBorder ? 4 : 0),
                             im.getMinY() + im.getHeight() +
                             (drawBorder ? 4 : 0));
    }

    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    public Dimension getMaximumSize() {
        return getMinimumSize();
    }

    /** Records a new size.  Called by the AWT. */
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        canvasWidth  = width;
        canvasHeight = height;

        padX = Math.max((canvasWidth  - imWidth  - (drawBorder ? 4 : 0))/2, 0);
        padY = Math.max((canvasHeight - imHeight - (drawBorder ? 4 : 0))/2, 0);
    }

    private int XtoTileX(int x) {
        return (int) Math.floor((double) (x - tileGridXOffset)/tileWidth);
    }

    private int YtoTileY(int y) {
        return (int) Math.floor((double) (y - tileGridYOffset)/tileHeight);
    }
    
    private int TileXtoX(int tx) {
        return tx*tileWidth + tileGridXOffset;
    }
    
    private int TileYtoY(int ty) {
        return ty*tileHeight + tileGridYOffset;
    }
    
    /**
     * There is no need to erase prior to drawing, so we override the
     * default update method to simply call paint().
     */
    public void update(Graphics g) {
        paint(g);
    }

    /**
     * Paint the image onto a Graphics object.  The painting is
     * performed tile-by-tile, and includes a grey region covering the
     * unused portion of image tiles as well as the general
     * background.
     */
    public synchronized void paint(Graphics g) {
        if (im == null) {
            return;
        }

        Graphics2D g2D = null;
        if (g instanceof Graphics2D) {
            g2D = (Graphics2D)g;
        } else {
            System.err.println(JaiI18N.getString("ImageCanvas1"));
            return;
        }

        Color saveColor = g2D.getColor();

        if (drawBorder) {
            g.setColor(new Color(171, 171, 171));
            g.draw3DRect(padX, padY,
                         imWidth + 3,
                         imHeight + 3,
                         true);
            g.draw3DRect(padX + 1, padY + 1,
                         imWidth + 1,
                         imHeight + 1,
                         true);
        }

        // Get the clipping rectangle and translate it into image coordinates. 
        Rectangle clipBounds = g.getClipBounds();
        if (clipBounds == null) {
            clipBounds = new Rectangle(0, 0, canvasWidth, canvasHeight);
        }

        int border = drawBorder ? 2 : 0;
        int transX = padX + border - originX;
        int transY = padY + border - originY;

        clipBounds.translate(-transX, -transY);

        // Determine the extent of the clipping region in tile coordinates.
        int txmin, txmax, tymin, tymax;
        
        txmin = XtoTileX(clipBounds.x);
        txmin = Math.max(txmin, minTileX);
        txmin = Math.min(txmin, maxTileX);

        txmax = XtoTileX(clipBounds.x + clipBounds.width - 1);
        txmax = Math.max(txmax, minTileX);
        txmax = Math.min(txmax, maxTileX);

        tymin = YtoTileY(clipBounds.y);
        tymin = Math.max(tymin, minTileY);
        tymin = Math.min(tymin, maxTileY);

        tymax = YtoTileY(clipBounds.y + clipBounds.height - 1);
        tymax = Math.max(tymax, minTileY);
        tymax = Math.min(tymax, maxTileY);

        if (backgroundColor != null) {
            g2D.setColor(backgroundColor);
        } else {
            // Draw grey over unused area
            g2D.setColor(grayColor);
        }

        int xmin = im.getMinX();
        int xmax = im.getMinX()+im.getWidth();
        int ymin = im.getMinY();
        int ymax = im.getMinY()+im.getHeight();
        int screenX = clipBounds.x + clipBounds.width;
        int screenY = clipBounds.y + clipBounds.height;

        // Left
        if (xmin > clipBounds.x) {
            g2D.fillRect(clipBounds.x + transX,
                         clipBounds.y + transY,
                         xmin - clipBounds.x,
                         clipBounds.height);
        }

        // Right
        if (xmax < screenX) {
            g2D.fillRect(xmax + transX,
                         clipBounds.y + transY,
                         screenX - xmax,
                         clipBounds.height);
        }

        // Top
        if (ymin > clipBounds.y) {
            g2D.fillRect(xmin + transX,
                         clipBounds.y + transY,
                         xmax - xmin,
                         ymin - clipBounds.y);
        }

        // Bottom
        if (ymax < screenY) {
            g2D.fillRect(xmin + transX,
                         ymax + transY,
                         xmax - xmin,
                         screenY - ymax);
        }

        // needed for clipping (crop op)
        g2D.setClip(new Rectangle(transX + im.getMinX(),
                                  transY + im.getMinY(),
                                  im.getWidth(),
                                  im.getHeight()));

        // Get all tiles which overlap the clipping region.
        Point[] tileIndices = new Point[(txmax-txmin+1)*(tymax-tymin+1)];
        int index = 0;
        for(int tj = tymin; tj <= tymax; tj++) {
            for(int ti = txmin; ti <= txmax; ti++) {
                tileIndices[index++] = new Point(ti, tj);
            }
        }
        Raster[] tiles =
            PlanarImage.wrapRenderedImage(im).getTiles(tileIndices);

        // Loop over tiles within the clipping region
        int numTiles = tiles.length;
        for (int tileNum = 0; tileNum < numTiles; tileNum++) {
            Raster tile = tiles[tileNum];

            int tx = tile.getMinX();
            int ty = tile.getMinY();

            if ( tile != null ) {
                WritableRaster wr =
                    tile instanceof WritableRaster ?
                    ((WritableRaster)tile).createWritableTranslatedChild(0, 0) :
                    tile.createWritableRaster(sampleModel,
                                              tile.getDataBuffer(),
                                              new Point(0, 0));

                BufferedImage bi = 
                    new BufferedImage(colorModel,
                                      wr,
                                      colorModel.isAlphaPremultiplied(), 
                                      null);

                AffineTransform transform =
                    AffineTransform.getTranslateInstance(tx + transX,
                                                         ty + transY);
                if (backgroundColor != null) {
                    g2D.fillRect(tx + transX, ty + transY,
                                 tileWidth, tileHeight);
                }
                g2D.drawRenderedImage(bi, transform);
            }
        }

        // Restore color
        g2D.setColor(saveColor);
	notifyPaintListeners(g2D);
    }

    /**
     * An interface used to notify listeners during a <code>paint</code>
     * just after the image has been painted on the image canvas. This
     * allows registered listeners to draw additional graphics on top
     * of the image.
     *
     * @since JAI 1.1
     */
    public interface PaintListener {

	/**
	 * Called from <code>ImageCanvas.paint</code> just after
	 * the image has been drawn on the canvas.
	 */
	public void paint(ImageCanvas ic, Graphics g);
    }

    private HashSet paintListeners = new HashSet();

    /**
     * Adds the specified <code>PaintListener</code> to the canvas.
     *
     * @since JAI 1.1
     */
    public void addPaintListener(PaintListener pl) {
	paintListeners.add(pl);
    }

    /**
     * Removes the specified <code>PaintListener</code> from the canvas.
     *
     * @since JAI 1.1
     */    
    public void removePaintListener(PaintListener pl) {
	paintListeners.remove(pl);
    }

    private void notifyPaintListeners(Graphics g) {

	Iterator it = paintListeners.iterator();

	while (it.hasNext()) {
	    ((PaintListener)it.next()).paint(this, g);
	}
    }
}
