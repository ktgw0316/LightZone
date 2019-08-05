/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.model.Engine;
import com.lightcrafts.model.EngineListener;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.utils.awt.geom.HiDpi;
import com.lightcrafts.utils.SoftValueHashMap;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.TileCache;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImageEditorDisplay extends JPanel {
    @Getter
    private PlanarImage source;

    private int epoch = 0;

    private static TileManager tileManager = new TileManager();

    @Setter
    private LinkedList<EngineListener> engineListeners = null;

    @Setter
    private PaintListener paintListener = null;

    private boolean synchronizedImage;

    private LCTileHandler tileHandler = new LCTileHandler();

    private ProgressNotifyer progressNotifyer = new ProgressNotifyer();

    @Setter(AccessLevel.PACKAGE)
    private RenderedImage backgroundImage;

    private SoftValueHashMap<CacheKey, BufferedImage> backgroundCache = null;

    // Workaround for unreliable ComponentListener.componentResized() callbacks.
    private ConcurrentLinkedQueue<ComponentListener> compListeners =
            new ConcurrentLinkedQueue<>();

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

    Engine engine;

    public ImageEditorDisplay(Engine engine, PlanarImage image) {
        super(null);

        this.engine = engine;

        source = image;
        if (source != null) {
            backgroundCache = new SoftValueHashMap<>();
            source.addTileComputationListener(tileManager);
            // Swing geometry
            setPreferredSize(HiDpi.userSpaceDimensionFrom(source));
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
            backgroundCache = new SoftValueHashMap<>();

            // Swing geometry
            setPreferredSize(HiDpi.userSpaceDimensionFrom(source));
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
        if (ctx.isCancelled() || ctx.getImage() != source) {
            return;
        }

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

    private Raster[] availableTiles(Point... tileIndices) {
        final OpImage ro;
        if (source instanceof OpImage) {
            ro = (OpImage) source;
        } else {
            PlanarImage rendering = ((RenderedOp) source).getCurrentRendering();
            ro = (OpImage) rendering;
        }
        TileCache cache = ro.getTileCache();
        return cache.getTiles(ro, tileIndices);
    }

    private static final ColorModel sRGBColorModel = new ComponentColorModel(
            JAIContext.sRGBColorSpace, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

    private BufferedImage getBackgroundTile(@NotNull WritableRaster tile, int x, int y) {
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

        HiDpi.resetTransformScaleOf(g2d);

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

        val tileIndices = source.getTileIndices(g2d.getClipBounds());
        if (tileIndices == null) {
            return;
        }

        if (ASYNCH_REPAINT) {
            asyncRepaint(g2d, tileIndices);
            progressNotifyer.setTiles(tileManager.pendingTiles(source, epoch));
        } else {
            // fetching tiles explicitly allows to schedule them on separate threads,
            // this is good if we have multiple CPUs
            progressNotifyer.setTiles(1);
            source.getTiles(tileIndices); // this blocks until the tiles are all available
            g2d.drawRenderedImage(source, identityTransform);
            progressNotifyer.setTiles(0);
        }
    }

    private void asyncRepaint(Graphics2D g2d, Point[] tileIndices) {
        val originalClipBounds = g2d.getClipBounds();
        val tiles = availableTiles(tileIndices);

        for (int i = 0; i < tileIndices.length; i++) {
            val tileIndex = tileIndices[i];
            val tile = (tiles == null) ? null : (WritableRaster) tiles[i];

            val tileClipRect = new Rectangle(
                    tileIndex.x * JAIContext.TILE_WIDTH, tileIndex.y * JAIContext.TILE_HEIGHT,
                    JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT);
            g2d.setClip(tileClipRect.intersection(originalClipBounds));

            drawBackgroundTile(g2d, tileIndex, tileClipRect, tile);
        }
        g2d.setClip(originalClipBounds); // reset the clip rect

        updateTileComputingStatus(tileIndices, originalClipBounds);
    }

    private void drawBackgroundTile(Graphics2D g2d, Point tileIndex, Rectangle tileClipRect,
                                    WritableRaster tile) {
        val tx = tileIndex.x;
        val ty = tileIndex.y;

        if (!validImageBackground[tx][ty] && tile != null) {
            validImageBackground[tx][ty] = true;
            g2d.drawImage(getBackgroundTile(tile, tx, ty), null, tile.getMinX(), tile.getMinY());
            return;
        }

        // if we don't have a fresh tile, try and see if we have an old one around
        val backgroundTileCache = backgroundCache.get(new CacheKey(tx, ty));
        if (backgroundTileCache != null) {
            // Recycle the background tile
            g2d.drawImage(backgroundTileCache, null, source.tileXToX(tx), source.tileYToY(ty));
            return;
        }

        val cachedTiles = availableTiles(new Point(tx, ty));
        if (cachedTiles.length == 1 && cachedTiles[0] != null) {
            val cachedTile = (WritableRaster) cachedTiles[0];
            g2d.drawImage(getBackgroundTile(cachedTile, tx, ty), null,
                    cachedTile.getMinX(), cachedTile.getMinY());
            return;
        }

        if (backgroundImage instanceof BufferedImage) {
            g2d.drawImage((BufferedImage) backgroundImage, null, tileClipRect.x, tileClipRect.y);
            return;
        }

        // If all fails paint the default background color
        g2d.setColor(backgroundColor);
        g2d.fillRect(tileClipRect.x, tileClipRect.y, tileClipRect.width, tileClipRect.height);
    }

    private void updateTileComputingStatus(Point[] tileIndices, Rectangle clipBounds) {
        val tileComparator = new TileComparator(
                (clipBounds.x + clipBounds.width / 2) / source.getTileWidth(),
                (clipBounds.y + clipBounds.height / 2) / source.getTileHeight());
        val dirtyTiles = Stream.of(tileIndices)
                .filter(tileIndex -> !validImageBackground[tileIndex.x][tileIndex.y])
                .sorted(tileComparator)
                .collect(Collectors.toList());
        if (!dirtyTiles.isEmpty()) {
            startGetTiles = System.currentTimeMillis();
            computingTiles = true;
            tileManager.queueTiles(source, epoch, dirtyTiles, synchronizedImage, false, tileHandler);
        } else if (tileManager.pendingTiles(source, epoch) == 0) {
            if (paintListener != null) {
                val endGetTiles = System.currentTimeMillis();
                val time = (computingTiles && synchronizedImage && startGetTiles > 0)
                        ? endGetTiles - startGetTiles
                        : 0;
                paintListener.paintDone(source, HiDpi.imageSpaceRectFrom(getVisibleRect()), synchronizedImage, time);
            }
            startGetTiles = -1;
            computingTiles = false;
        }
    }
}
