/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.EngineListener;
import com.lightcrafts.model.Engine;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.utils.SoftValueHashMap;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.*;
import javax.swing.Timer;

import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.ui.LightZoneSkin;

public class ImageEditorDisplay extends JPanel {
    private static final boolean ADVANCED_REPAINT = true;
    private static final boolean ASYNCH_REPAINT = true;

    private PlanarImage source = null;

    private int epoch = 0;

    private static TileManager tileManager = new TileManager();

    private LinkedList<EngineListener> engineListeners = null;

    private PaintListener paintListener = null;

    private boolean synchronizedImage = false;

    private LCTileHandler tileHandler = new LCTileHandler();

    private ProgressNotifyer progressNotifyer = new ProgressNotifyer();

    private RenderedImage backgroundImage;

    private SoftValueHashMap<CacheKey, BufferedImage> backgroundCache = null;

    // Workaround for unreliable ComponentListener.componentResized() callbacks.
    private ConcurrentLinkedQueue<ComponentListener> compListeners =
        new ConcurrentLinkedQueue<ComponentListener>();

    @SuppressWarnings("deprecation")
    @Override
    public void reshape(int x, int y, int w, int h) {
        super.reshape(x, y, w, h);

        // COMPONENT_RESIZED events are not reliably forwarded to listeners,
        // so we do so manually.
        ComponentEvent event = new ComponentEvent(
            this, ComponentEvent.COMPONENT_RESIZED
        );
        for (ComponentListener listener : compListeners) {
            listener.componentResized(event);
        }
    }

    @Override
    public void addComponentListener(ComponentListener listener) {
        if (listener == null) {
            return;
        }
        compListeners.add(listener);
        super.addComponentListener(listener);
    }

    @Override
    public void removeComponentListener(ComponentListener listener) {
        if (listener == null) {
            return;
        }
        compListeners.remove(listener);
        super.removeComponentListener(listener);
    }

    static class CacheKey {
        final int tileX;
        final int tileY;

        CacheKey(int _tileX, int _tileY) {
            tileX = _tileX;
            tileY = _tileY;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;

            final CacheKey cacheKey = (CacheKey) o;

            return tileX == cacheKey.tileX &&
                   tileY == cacheKey.tileY;
        }

        @Override
        public int hashCode() {
            int result;
            result = tileX;
            result = 29 * result + tileY;
            return result;
        }
    }

    private boolean[][] validImageBackground = null;

    Engine engine = null;

    public ImageEditorDisplay(Engine engine, PlanarImage image) {
        super(null);

        this.engine = engine;

        source = image;
        if (source != null) {
            backgroundCache = new SoftValueHashMap<CacheKey, BufferedImage>();
            source.addTileComputationListener(tileManager);
            // Swing geometry
            setPreferredSize(new Dimension(source.getWidth(), source.getHeight()));
        }
        setOpaque(true);

        synchronizedImage = false;
    }

    public void dispose() {
        if (source != null) {
            source.removeTileComputationListener(tileManager);
            tileManager.cancelTiles(source, epoch);
            source.dispose();
            source = null;
        }
        if (backgroundCache != null) {
            backgroundCache = null;
        }
        engineListeners = null;
        paintListener = null;
        tileHandler = null;
        progressNotifyer = null;
    }

    public synchronized void set(PlanarImage image, boolean synchronous) {
        if (image == null)
            throw new IllegalArgumentException("cannot set a null image!");

        if (source != null) {
            source.removeTileComputationListener(tileManager);
            tileManager.cancelTiles(source, epoch);
            source.dispose();
        }

        PlanarImage oldImage = source;

        source = image;
        epoch++;
        source.addTileComputationListener(tileManager);

        int maxTileX = source.getMaxTileX();
        int maxTileY = source.getMaxTileY();
        validImageBackground = new boolean[maxTileX+1][maxTileY+1];

        synchronizedImage = synchronous;

        if (oldImage == null || !oldImage.getBounds().equals(image.getBounds())) {
            backgroundCache = new SoftValueHashMap<CacheKey, BufferedImage>();

            // Swing geometry
            Dimension dim = new Dimension(source.getWidth(), source.getHeight());
            setPreferredSize(dim);
        }
        repaint();
    }

    public void setEngineListeners(LinkedList<EngineListener> engineListeners) {
        this.engineListeners = engineListeners;
    }

    public void setPaintListener(PaintListener listener) {
        paintListener = listener;
    }

    public RenderedImage getSource() {
        return source;
    }

    class LCTileHandler implements TileHandler {
        @Override
        public void handle(int tileX, int tileY, PaintContext ctx) {
            EventQueue.invokeLater(new AsynchronousRepainter(tileX, tileY, ctx));
        }
    }

    class AsynchronousRepainter implements Runnable {
        private int tileX;
        private int tileY;
        private PaintContext ctx;

        AsynchronousRepainter(int tileX, int tileY, PaintContext ctx) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.ctx = ctx;
        }

        @Override
        public void run() {
            repaintTile(ctx, tileX, tileY);
        }
    }

    /*
        Note: we repaint old tiles in a synchronized method and we restore the old image
        this avoids old tiles going out of step with the new image if the set method updates
        the image while tiles are being painted/computed...
    */

    synchronized void repaintTile(PaintContext ctx, int tileX, int tileY) {
        if (!ctx.isCancelled() && ctx.getImage() == source) {
            if (!ctx.isPrefetch()) {
                PlanarImage currentSource = source;
                boolean currentSynchronized = synchronizedImage;
                source = ctx.getImage();
                synchronizedImage = ctx.isSynchronous();
                paintImmediately(tileX * source.getTileWidth(),
                                 tileY * source.getTileHeight(),
                                 source.getTileWidth(),
                                 source.getTileHeight());
                synchronizedImage = currentSynchronized;
                source = currentSource;
            } else
                progressNotifyer.setTiles(tileManager.pendingTiles(source, epoch));
        }
    }

    class ProgressNotifyer {
        private int queuedTiles = 0;

        private void notifyListeners() {
            if (engineListeners != null) {
                for (EngineListener listener : engineListeners)
                    listener.engineActive(queuedTiles);
            }
        }

        public synchronized void setTiles(int numTiles) {
            queuedTiles = numTiles;
            notifyListeners();
        }
    }

    static class TileComparator implements Comparator<Point> {
        int x, y;

        TileComparator(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compare(Point t1, Point t2) {
            int dx1 = t1.x - x;
            int dy1 = t1.y - y;
            int dx2 = t2.x - x;
            int dy2 = t2.y - y;

            if (dx1 * dx1 + dy1 * dy1 > dx2 * dx2 + dy2 * dy2)
                return 1;
            else if (dx1 * dx1 + dy1 * dy1 < dx2 * dx2 + dy2 * dy2)
                return -1;
            return 0;
        }
    }

    private Raster[] availableTiles(Point tileIndex) {
        return availableTiles(new Point[] {tileIndex});
    }

    private Raster[] availableTiles(Point[] tileIndices) {
        OpImage ro;
        if (source instanceof OpImage) {
            ro = (OpImage) source;
        } else {
            PlanarImage rendering = ((RenderedOp) source).getCurrentRendering();
            ro = (OpImage) rendering;
        }
        TileCache cache = (ro).getTileCache();
        return cache.getTiles(ro, tileIndices);
    }

    private static final Color backgroundColor = LightZoneSkin.Colors.EditorBackground;

    // private static boolean windowsPlatform = Platform.getType() == Platform.Windows;

    private static final AffineTransform identityTransform = new AffineTransform();

    private long startGetTiles;

    private boolean computingTiles = false;

    private static final ColorModel sRGBColorModel = new ComponentColorModel(
            JAIContext.sRGBColorSpace, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

    BufferedImage getBackgroundTile(WritableRaster tile, int x, int y) {
        CacheKey key = new CacheKey(x, y);
        BufferedImage image = backgroundCache.get(key);
        BufferedImage tileImage = new BufferedImage(sRGBColorModel,
                                                    (WritableRaster) tile.createTranslatedChild(0, 0),
                                                    false, null);
        if (image != null
            && image.getWidth() == tile.getWidth()
            && image.getHeight() == tile.getHeight()) {
            Graphics2D big = (Graphics2D) image.getGraphics();
            big.drawRenderedImage(tileImage, new AffineTransform());
            big.dispose();
        } else {
            image = Functions.toFastBufferedImage(tileImage);
            backgroundCache.put(key, image);
        }
        return image;
    }

    private boolean firstTime;

    private final Timer paintTimer = new Timer(300, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            firstTime = false;
            paintTimer.stop();
            repaint();
        }
    });

    void setFirstTime() {
        firstTime = true;
    }

    void setBackgroundImage(RenderedImage image) {
        backgroundImage = image;
    }

    @Override
    public synchronized void paintComponent(Graphics g) {
        if (firstTime) {
            if (!paintTimer.isRunning())
                paintTimer.start();
            return;
        }

        Graphics2D g2d = (Graphics2D)g;

        // empty component (no image)
        if ( source == null ) {
            g2d.setColor(backgroundColor);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            return;
        }
        g2d.setColor(Color.blue);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        if (!ADVANCED_REPAINT) {
            g2d.drawRenderedImage(source, identityTransform);
            return;
        }

        Rectangle clipBounds = g2d.getClipBounds();

        Point[] tileIndices = source.getTileIndices(clipBounds);
        if (tileIndices == null) {
            return;
        }

        if (!ASYNCH_REPAINT) {
            // fetching tiles explicitly allows to schedule them on separate threads,
            // this is good if we have multiple CPUs
            source.getTiles(tileIndices); // this blocks until the tiles are all available
            g2d.drawRenderedImage(source, identityTransform);
            return;
        }

        List<Point> dirtyTiles = new LinkedList<Point>();

        final Raster[] tiles = availableTiles(tileIndices);

        for (int i = 0; i < tileIndices.length; i++) {
            final Point tileIndex = tileIndices[i];
            final WritableRaster tile = (tiles == null) ? null : (WritableRaster) tiles[i];

            final Rectangle tileClipRect = new Rectangle(tileIndex.x * JAIContext.TILE_WIDTH,
                                                   tileIndex.y * JAIContext.TILE_HEIGHT,
                                                   JAIContext.TILE_WIDTH,
                                                   JAIContext.TILE_HEIGHT);
            g2d.setClip(tileClipRect.intersection(clipBounds));

            if (validImageBackground[tileIndex.x][tileIndex.y] || tile == null) {
                if (!validImageBackground[tileIndex.x][tileIndex.y])
                    dirtyTiles.add(tileIndex);

                // if we don't have a fresh tile, try and see if we have an old one around
                final BufferedImage backgroundTile =
                        backgroundCache.get(new CacheKey(tileIndex.x, tileIndex.y));

                if (backgroundTile != null) {
                    final int xOffset = source.tileXToX(tileIndex.x);
                    final int yOffset = source.tileYToY(tileIndex.y);

                    try {
                        g2d.drawImage(backgroundTile, null, xOffset, yOffset);
                        // System.out.println("recycled background tile for: " + xOffset + ", " + yOffset);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Raster cachedTiles[] = availableTiles(new Point(tileIndex.x, tileIndex.y));
                    if (cachedTiles.length == 1 && cachedTiles[0] != null) {
                        WritableRaster cachedTile = (WritableRaster) cachedTiles[0];
                        BufferedImage image = getBackgroundTile(cachedTile, tileIndex.x, tileIndex.y);
                        g2d.drawImage(image, null, cachedTile.getMinX(), cachedTile.getMinY());
                    } else {
                        if (backgroundImage != null) {
                            g2d.drawRenderedImage(backgroundImage, new AffineTransform());
                        } else {
                            // if all fails paint the default background color
                            g2d.setColor(backgroundColor);
                            g2d.fillRect(tileClipRect.x,
                                         tileClipRect.y,
                                         tileClipRect.width,
                                         tileClipRect.height);
                        }
                    }
                }
            } else {
                try {
                    BufferedImage image = getBackgroundTile(tile, tileIndex.x, tileIndex.y);
                    g2d.drawImage(image, null, tile.getMinX(), tile.getMinY());
                    validImageBackground[tileIndex.x][tileIndex.y] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        g2d.setClip(clipBounds); // reset the clip rect

        if (!dirtyTiles.isEmpty()) {
            startGetTiles = System.currentTimeMillis();

            Collections.sort(dirtyTiles,
                    new TileComparator((clipBounds.x + clipBounds.width  / 2) / source.getTileWidth(),
                                       (clipBounds.y + clipBounds.height / 2) / source.getTileHeight()));

            computingTiles = true;
            tileManager.queueTiles(source, epoch, dirtyTiles, synchronizedImage, false, tileHandler);
        } else if (tileManager.pendingTiles(source, epoch) == 0) {
            final long endGetTiles = System.currentTimeMillis();
            final long time = (computingTiles && synchronizedImage && startGetTiles > 0) ?
                    endGetTiles - startGetTiles : 0;

            if (paintListener != null) {
                paintListener.paintDone(source, getVisibleRect(), synchronizedImage, time);
            }
            startGetTiles = -1;
            computingTiles = false;
        }
        progressNotifyer.setTiles(tileManager.pendingTiles(source, epoch));
    }
}
