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
import org.jetbrains.annotations.Nullable;

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

    private static final TileManager tileManager = new TileManager();

    @Setter
    private LinkedList<EngineListener> engineListeners = null;

    @Setter
    private PaintListener paintListener = null;

    private boolean synchronizedImage;

    private LCTileHandler tileHandler = new LCTileHandler();

    private ProgressNotifier progressNotifier = new ProgressNotifier();

    @Setter(AccessLevel.PACKAGE)
    private RenderedImage backgroundImage;

    private SoftValueHashMap<CacheKey, BufferedImage> backgroundCache = null;

    // Workaround for unreliable ComponentListener.componentResized() callbacks.
    private final ConcurrentLinkedQueue<ComponentListener> compListeners =
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
        progressNotifier = null;
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
            progressNotifier.setTiles(tileManager.pendingTiles(source, epoch));
        }
    }

    private class ProgressNotifier {
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

    @NotNull
    private Raster[] availableTiles(Point... tileIndices) {
        final OpImage ro;
        if (source instanceof OpImage) {
            ro = (OpImage) source;
        } else {
            PlanarImage rendering = ((RenderedOp) source).getCurrentRendering();
            ro = (OpImage) rendering;
        }
        TileCache cache = ro.getTileCache();
        @Nullable val tiles = cache.getTiles(ro, tileIndices);
        return (tiles != null) ? tiles : new Raster[tileIndices.length];
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
    private int repaintCount = 0;

    private final Timer paintTimer = new Timer(100, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            firstTime = false;
            paintTimer.stop();
            repaintCount++;
            repaint();
//            System.out.println("repaintCount = " + repaintCount);
        }
    });

    void setFirstTime() {
        firstTime = true;
        repaintCount = 0;
    }

    private long startGetTiles;

    private static final Color backgroundColor = LightZoneSkin.Colors.EditorBackground;

    @Override
    public synchronized void paintComponent(Graphics g) {
        if (firstTime) {
            if (!paintTimer.isRunning())
                paintTimer.start();
            return;
        }

        val g2d = (Graphics2D)g;

        HiDpi.resetTransformScaleOf(g2d);

        // empty component (no image)
        if (source == null) {
            g2d.setBackground(backgroundColor);
            g2d.clearRect(0, 0, getWidth(), getHeight());
            return;
        }

        val tileIndices = source.getTileIndices(g2d.getClipBounds());
        if (tileIndices == null) {
            return;
        }

        final int MAX_REPAINT_COUNT = 2;
        if (repaintCount > MAX_REPAINT_COUNT) {
            System.err.println("asyncRepaint failed");
            // Fetch tiles explicitly, blocks until the tiles are all available
            source.getTiles(tileIndices);
        }

        val isCompleted = asyncRepaint(g2d, tileIndices);
        progressNotifier.setTiles(tileManager.pendingTiles(source, epoch));

        if (isCompleted) {
            repaintCount = 0;
        } else if (!paintTimer.isRunning()) {
            paintTimer.start();
        }
    }

    private boolean asyncRepaint(Graphics2D g2d, Point[] tileIndices) {
        val originalClipBounds = g2d.getClipBounds();
        val tiles = availableTiles(tileIndices);

        boolean isCompleted = true;
        for (int i = 0; i < tileIndices.length; i++) {
            val tileIndex = tileIndices[i];
            @Nullable val tile = (WritableRaster) tiles[i];
            val tileClipRect = new Rectangle(
                    tileIndex.x * JAIContext.TILE_WIDTH, tileIndex.y * JAIContext.TILE_HEIGHT,
                    JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT);
            g2d.setClip(tileClipRect.intersection(originalClipBounds));

            isCompleted &= drawBackgroundTile(g2d, tileIndex, tileClipRect, tile);
        }
        g2d.setClip(originalClipBounds); // reset the clip rect

        updateTileComputingStatus(tileIndices, originalClipBounds);
        return isCompleted;
    }

    private boolean drawBackgroundTile(Graphics2D g2d, Point tileIndex, Rectangle tileClipRect,
                                    @Nullable WritableRaster tile) {
        val tx = tileIndex.x;
        val ty = tileIndex.y;

        if (!validImageBackground[tx][ty] && tile != null) {
            validImageBackground[tx][ty] = true;
            return g2d.drawImage(getBackgroundTile(tile, tx, ty), tile.getMinX(), tile.getMinY(), this);
        }

        // if we don't have a fresh tile, try and see if we have an old one around
        val backgroundTileCache = backgroundCache.get(new CacheKey(tx, ty));
        if (backgroundTileCache != null) {
            // Recycle the background tile
            return g2d.drawImage(backgroundTileCache, source.tileXToX(tx), source.tileYToY(ty), this);
        }

        val cachedTiles = availableTiles(new Point(tx, ty));
        if (cachedTiles[0] != null) {
            val cachedTile = (WritableRaster) cachedTiles[0];
            return g2d.drawImage(getBackgroundTile(cachedTile, tx, ty),
                    cachedTile.getMinX(), cachedTile.getMinY(), this);
        }

        if (backgroundImage instanceof BufferedImage) {
            return g2d.drawImage((BufferedImage) backgroundImage, tileClipRect.x, tileClipRect.y, this);
        }

        // If all fails paint the default background color
        g2d.setColor(backgroundColor);
        g2d.fillRect(tileClipRect.x, tileClipRect.y, tileClipRect.width, tileClipRect.height);
        return false;
    }

    private boolean computingTiles = false;

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
