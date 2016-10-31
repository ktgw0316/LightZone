/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

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
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.*;
import javax.swing.Timer;

import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.ui.LightZoneSkin;

import lombok.*;

public class ImageEditorDisplay extends JPanel {
    @Getter
    private PlanarImage source = null;

    private int epoch = 0;

    private static TileManager tileManager = new TileManager();

    @Setter
    private LinkedList<EngineListener> engineListeners = null;

    @Setter
    private PaintListener paintListener = null;

    private boolean synchronizedImage = false;

    private LCTileHandler tileHandler = new LCTileHandler();

    private ProgressNotifyer progressNotifyer = new ProgressNotifyer();

    @Setter(AccessLevel.PACKAGE)
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
        val event = new ComponentEvent(
            this, ComponentEvent.COMPONENT_RESIZED
        );
        for (val listener : compListeners) {
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

    @RequiredArgsConstructor
    @EqualsAndHashCode
    private static class CacheKey {
        final int tileX;
        final int tileY;
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

    private class LCTileHandler implements TileHandler {
        @Override
        public void handle(int tileX, int tileY, PaintContext ctx) {
            EventQueue.invokeLater(new AsynchronousRepainter(tileX, tileY, ctx));
        }
    }

    @RequiredArgsConstructor
    private class AsynchronousRepainter implements Runnable {
        private final int tileX;
        private final int tileY;
        private final PaintContext ctx;

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

    private synchronized void repaintTile(PaintContext ctx, int tileX, int tileY) {
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
            } else {
                progressNotifyer.setTiles(tileManager.pendingTiles(source, epoch));
            }
        }
    }

    private class ProgressNotifyer {
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

    @RequiredArgsConstructor
    private static class TileComparator implements Comparator<Point> {
        final int x;
        final int y;

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

    private static final ColorModel sRGBColorModel = new ComponentColorModel(
            JAIContext.sRGBColorSpace, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

    private BufferedImage getBackgroundTile(WritableRaster tile, int x, int y) {
        val key = new CacheKey(x, y);
        BufferedImage image = backgroundCache.get(key);
        val tileImage = new BufferedImage(sRGBColorModel,
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

    private long startGetTiles;
    private boolean computingTiles = false;

    private static final Color backgroundColor = LightZoneSkin.Colors.EditorBackground;
    private static final AffineTransform identityTransform = new AffineTransform();
    private static final boolean ADVANCED_REPAINT = true;
    private static final boolean ASYNCH_REPAINT = true;

    @Override
    public synchronized void paintComponent(Graphics g) {
        if (firstTime) {
            if (!paintTimer.isRunning())
                paintTimer.start();
            return;
        }

        val g2d = (Graphics2D)g;
        g2d.setBackground(backgroundColor);
        g2d.clearRect(0, 0, getWidth(), getHeight());

        // empty component (no image)
        if (source == null) {
            return;
        }

        if (!ADVANCED_REPAINT) {
            progressNotifyer.setTiles(1);
            g2d.drawRenderedImage(source, identityTransform);
            progressNotifyer.setTiles(0);
            return;
        }

        val clipBounds = g2d.getClipBounds();

        val tileIndices = source.getTileIndices(clipBounds);
        if (tileIndices == null) {
            return;
        }

        // fetching tiles explicitly allows to schedule them on separate threads,
        // this is good if we have multiple CPUs
        if (!ASYNCH_REPAINT) {
            progressNotifyer.setTiles(1);
            source.getTiles(tileIndices); // this blocks until the tiles are all available
            g2d.drawRenderedImage(source, identityTransform);
            progressNotifyer.setTiles(0);
            return;
        }

        val dirtyTiles = new LinkedList<Point>();
        val tiles = availableTiles(tileIndices);

        for (int i = 0; i < tileIndices.length; i++) {
            final Point tileIndex = tileIndices[i];
            final WritableRaster tile = (tiles == null) ? null : (WritableRaster) tiles[i];

            final Rectangle tileClipRect = new Rectangle(tileIndex.x * JAIContext.TILE_WIDTH,
                                                   tileIndex.y * JAIContext.TILE_HEIGHT,
                                                   JAIContext.TILE_WIDTH,
                                                   JAIContext.TILE_HEIGHT);
            g2d.setClip(tileClipRect.intersection(clipBounds));

            final BufferedImage backgroundTile;
            final int xOffset;
            final int yOffset;

            if (validImageBackground[tileIndex.x][tileIndex.y] || tile == null) {
                if (!validImageBackground[tileIndex.x][tileIndex.y])
                    dirtyTiles.add(tileIndex);

                // if we don't have a fresh tile, try and see if we have an old one around
                val backgroundTileCache = backgroundCache.get(new CacheKey(tileIndex.x, tileIndex.y));

                if (backgroundTileCache != null) {
                    // Recycle the background tile
                    backgroundTile = backgroundTileCache;
                    xOffset = source.tileXToX(tileIndex.x);
                    yOffset = source.tileYToY(tileIndex.y);
                } else {
                    val cachedTiles = availableTiles(new Point(tileIndex.x, tileIndex.y));
                    if (cachedTiles.length == 1 && cachedTiles[0] != null) {
                        val cachedTile = (WritableRaster) cachedTiles[0];
                        xOffset = cachedTile.getMinX();
                        yOffset = cachedTile.getMinY();
                        backgroundTile = getBackgroundTile(cachedTile, tileIndex.x, tileIndex.y);
                    } else {
                        xOffset = tileClipRect.x;
                        yOffset = tileClipRect.y;
                        backgroundTile = (backgroundImage instanceof BufferedImage)
                                ? (BufferedImage) backgroundImage
                                : null;
                    }
                }
            } else {
                xOffset = tile.getMinX();
                yOffset = tile.getMinY();
                backgroundTile = getBackgroundTile(tile, tileIndex.x, tileIndex.y);
                validImageBackground[tileIndex.x][tileIndex.y] = true;
            }

            try {
                g2d.drawImage(backgroundTile, null, xOffset, yOffset);
            } catch (RuntimeException e) {
                e.printStackTrace();
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
