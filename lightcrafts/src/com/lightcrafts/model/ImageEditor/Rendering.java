/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.CropBounds;
import com.lightcrafts.model.Operation;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.CachedImage;

import com.lightcrafts.mediax.jai.*;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.*;
import java.util.LinkedList;
import java.util.Vector;

public class Rendering implements Cloneable {
    private float scaleFactor = 1;
    private CropBounds cropBounds = new CropBounds();
    private AffineTransform inputTransform = new AffineTransform();
    private AffineTransform transform = new AffineTransform();
    private final PlanarImage sourceImage;
    private PlanarImage xformedSourceImage;
    private ImageEditorEngine engine;
    private LinkedList<Operation> pipeline = new LinkedList<Operation>();
    private ImagePyramid pyramid;

    public boolean cheapScale = false;

    public Rendering clone() /* throws CloneNotSupportedException */ {
        try {
            Rendering object = (Rendering) super.clone();
            object.engine = null;

            RenderedOp downSampler = createDownScaleOp(sourceImage, MIP_SCALE_RATIO);
            downSampler.removeSources();

            object.inputTransform = buildTransform(true);
            object.transform = buildTransform(false);
            object.xformedSourceImage = null;

            object.pipeline = new LinkedList<Operation>();
            for ( Operation op : pipeline )
                object.pipeline.add(((BlendedOperation) op).clone(object));
            return object;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    void dispose() {
        if (pipeline != null) {
            while (!pipeline.isEmpty())
                pipeline.removeLast().dispose();
            pipeline = null;
        }
    }

    void addOperation(int position, Operation op) {
        pipeline.add(position, op);
    }

    Operation removeOperation(int position) {
        return pipeline.remove(position);
    }

    public int indexOf(Operation op) {
        return pipeline.indexOf(op);
    }

    public Operation getOperation(int index) {
        return pipeline.get(index);
    }

    protected RenderedOp createDownScaleOp(RenderedImage src, int ratio) {
        KernelJAI kernel = Functions.getLanczos2Kernel(ratio);
        int ko = kernel.getXOrigin();
        float kdata[] = kernel.getHorizontalKernelData();
        float qsFilterArray[] = new float[kdata.length - ko];
        System.arraycopy(kdata, ko, qsFilterArray, 0, qsFilterArray.length);

        ParameterBlock params = new ParameterBlock();
        params.addSource(src);
        params.add(ratio);
        params.add(ratio);
        params.add(qsFilterArray);
        params.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        return JAI.create("FilteredSubsample", params,
                          new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                             BorderExtender.createInstance(BorderExtender.BORDER_COPY)));
    }

    private static final int MIP_SCALE_RATIO = 2;

    class ImagePyramid {
        private RenderedImage currentImage;
        private int currentLevel = 0;

        Vector<RenderedImage> renderings = new Vector<RenderedImage>();

        ImagePyramid(RenderedImage image) {
            currentImage = image;
            renderings.addElement(currentImage);
        }

        public RenderedImage getUpImage() {
            if (currentLevel > 0) {
                currentLevel--;

                currentImage = renderings.get(currentLevel);
            }

            return currentImage;
        }

        public RenderedImage getDownImage() {
            currentLevel++;
            if (renderings.size() <= currentLevel) {
                RenderedOp smaller = createDownScaleOp(currentImage, MIP_SCALE_RATIO);
                smaller.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
                renderings.addElement(smaller);
                return currentImage = smaller;
            } else {
                return currentImage = renderings.get(currentLevel);
            }
        }

        public RenderedImage getImage(int level) {
            if (level < 0)
                return null;

            while (currentLevel < level)
                getDownImage();
            while (currentLevel > level)
                getUpImage();

            return currentImage;
        }
    }

    public Rendering(PlanarImage sourceImage, ImageEditorEngine engine) {
        this.sourceImage = sourceImage;
        this.engine = engine;

        RenderedOp downSampler = createDownScaleOp(sourceImage, MIP_SCALE_RATIO);
        downSampler.removeSources();

        pyramid = new ImagePyramid(sourceImage);

        xformedSourceImage = null;
        inputTransform = buildTransform(true);
        transform = buildTransform(false);
    }

    public Rendering(PlanarImage sourceImage) {
        this(sourceImage, null);
    }

    ImageEditorEngine getEngine() {
        return engine;
    }

    public void update(OperationImpl op, boolean isLive) {
        if (engine != null)
            engine.update(op, isLive);
    }

    public AffineTransform getInputTransform() {
        return new AffineTransform(inputTransform);
    }

    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    public void setCropBounds(CropBounds cropBounds) {
        if (!cropBounds.equals(this.cropBounds)) {
            this.cropBounds = cropBounds;
            inputTransform = buildTransform(true);
            transform = buildTransform(false);
            if (xformedSourceImage != null) {
                xformedSourceImage.dispose();
                xformedSourceImage = null;
            }
        }
    }

    public CropBounds getCropBounds() {
        return cropBounds;
    }

    public void setScaleFactor(float scaleFactor) {
        if (scaleFactor != this.scaleFactor) {
            this.scaleFactor = scaleFactor;
            inputTransform = buildTransform(true);
            transform = buildTransform(false);
            if (xformedSourceImage != null) {
                xformedSourceImage.dispose();
                xformedSourceImage = null;
            }
        }
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public void setCropAndScale(CropBounds cropBounds, float scaleFactor) {
        if (!cropBounds.equals(this.cropBounds) || scaleFactor != this.scaleFactor) {
            this.cropBounds = cropBounds;
            this.scaleFactor = scaleFactor;
            inputTransform = buildTransform(true);
            transform = buildTransform(false);
            if (xformedSourceImage != null) {
                xformedSourceImage.dispose();
                xformedSourceImage = null;
            }
        }
    }

    public PlanarImage getXformedSourceImage() {
        if (xformedSourceImage == null)
            xformedSourceImage = transformSourceImage();
        return xformedSourceImage;
    }

    public PlanarImage getRendering(boolean inactive, int stopBefore) {
        PlanarImage processedImage = getXformedSourceImage();

        if (pipeline == null) {
            System.out.println("Rendering.renderPipeline: null pipeline?");
            return processedImage;
        }

        int index = 0;
        for (Operation op : pipeline) {
            OperationImpl operation = (OperationImpl) op;
            if (index == stopBefore)
                break;

            if (operation.isActive() && !(inactive && operation.isDeactivatable())) {
                PlanarImage result = operation.render(processedImage, scaleFactor < 1 ? scaleFactor : 1);
                if (result != null)
                    processedImage = result;
            }

            index++;
        }

        return processedImage;
    }

    public void prefetch(Rectangle area) {
        if (pipeline == null) {
            System.out.println("Rendering.renderPipeline: null pipeline?");
            return;
        }

        PlanarImage processedImage = getXformedSourceImage();

        int index = 0;
        for (Operation operation : pipeline) {
            if (operation.isActive() /* && !(inactive && operation.isDeactivatable()) */) {
                PlanarImage result = ((OperationImpl)operation).render(processedImage, scaleFactor < 1 ? scaleFactor : 1);
                if (result != null) {
                    Point[] indices = result.getTileIndices(area);
                    if (indices != null) {
                        CachedImage cachedResult = new CachedImage(new ImageLayout(result), JAIContext.fileCache);

                        result.prefetchTiles(indices);
                        for (Point tile : indices) {
                            Raster newTile = result.getTile(tile.x, tile.y);
                            WritableRaster cachedTile = cachedResult.getWritableTile(tile.x, tile.y);
                            Functions.copyData(cachedTile, newTile);
                        }

                        result = cachedResult;
                    }
                    System.out.println("Rendered layer " + index);

                    processedImage = result;
                }
            }

            index++;
        }
    }

    public PlanarImage getRendering() {
        return getRendering(false, -1);
    }

    public PlanarImage getRendering(boolean inactive) {
        return getRendering(inactive, -1);
    }

    public PlanarImage getRendering(int stopBefore) {
        return getRendering(false, stopBefore);
    }

    public Dimension getRenderingSize() {
        if (cropBounds.isAngleOnly()) {
            // NOTE: we must clone PlanarImage.getBounds() since it returns a reference to an object
            Rectangle sourceBounds = new Rectangle(sourceImage.getBounds());

            if (cropBounds.getAngle() != 0) {
                Point2D center = new Point2D.Double(sourceBounds.getCenterX(), sourceBounds.getCenterY());

                sourceBounds = AffineTransform.getRotateInstance(-cropBounds.getAngle(),
                                                                 center.getX(),
                                                                 center.getY()).createTransformedShape(sourceBounds).getBounds();
            }

            return new Dimension(sourceBounds.width,
                                 sourceBounds.height);
        }

        return new Dimension((int) cropBounds.getWidth(),
                             (int) cropBounds.getHeight());
    }

    private AffineTransform buildTransform(boolean isInputTransform) {
        // NOTE: we must clone PlanarImage.getBounds() since it returns a reference to an object
        Rectangle sourceBounds = new Rectangle(sourceImage.getBounds());

        AffineTransform transform = new AffineTransform();

        // Scale
        if (scaleFactor < 1 || !isInputTransform) {
            double scaleX = Math.round(scaleFactor * sourceBounds.width) / (double) sourceBounds.width;
            double scaleY = Math.round(scaleFactor * sourceBounds.height) / (double) sourceBounds.height;
            double scale = Math.min(scaleX, scaleY); // To avoid wrong cropping ratio.
            transform.preConcatenate(AffineTransform.getScaleInstance(scale, scale));
        }

        // Rotate
        if (cropBounds.getAngle() != 0) {
            Rectangle2D bounds = transform.createTransformedShape(sourceBounds).getBounds2D();

            Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());

            transform.preConcatenate(AffineTransform.getRotateInstance(-cropBounds.getAngle(),
                                                                       center.getX(),
                                                                       center.getY()));

            bounds = transform.createTransformedShape(sourceBounds).getBounds2D();
            transform.preConcatenate(AffineTransform.getTranslateInstance(-bounds.getMinX(), -bounds.getMinY()));
        }

        // Crop
        if (!cropBounds.isAngleOnly()) {
            CropBounds actualCropBounds = CropBounds.transform(transform, cropBounds);

            Point2D cropUpperLeft = actualCropBounds.getUpperLeft();

            transform.preConcatenate(AffineTransform.getTranslateInstance(-cropUpperLeft.getX(),
                                                                          -cropUpperLeft.getY()));
        }

        return transform;
    }

    private PlanarImage transformSourceImage() {
        PlanarImage image = sourceImage;

        PlanarImage xformedSourceImage = image;

        AffineTransform completeInputTransform = inputTransform;

        if (!completeInputTransform.isIdentity()) {
            AffineTransform transform = completeInputTransform;

            Point2D zero = transform.transform(new Point2D.Double(0, 0), null);
            Point2D one = transform.transform(new Point2D.Double(1, 1), null);

            double dx = one.getX() - zero.getX();
            double dy = one.getY() - zero.getY();
            double scale = Math.sqrt((dx*dx + dy*dy) / 2.0);

            if (!cheapScale && scale <= 0.5) {
                int level = 0;
                while(scale <= 1/(double) MIP_SCALE_RATIO) {
                    scale *= MIP_SCALE_RATIO;
                    level++;
                }
                image = (PlanarImage) pyramid.getImage(level);
                transform = new AffineTransform(transform);
                transform.concatenate(AffineTransform.getScaleInstance(sourceImage.getWidth() / (double)image.getWidth(),
                                                                       sourceImage.getHeight() / (double)image.getHeight()));
            }

            if (!transform.isIdentity()) {
                RenderingHints extenderHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                                  BorderExtender.createInstance(BorderExtender.BORDER_COPY));
                ParameterBlock params = new ParameterBlock();
                params.addSource(image);
                params.add(transform);
                params.add(Interpolation.getInstance(cheapScale ? Interpolation.INTERP_BILINEAR : Interpolation.INTERP_BICUBIC));
                // params.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
                xformedSourceImage = JAI.create("Affine", params, extenderHints);
            } else
                xformedSourceImage = image;
        }

        if (!cropBounds.isAngleOnly()) {
            CropBounds actualCropBounds = CropBounds.transform(completeInputTransform, cropBounds);

            Rectangle bounds = new Rectangle(xformedSourceImage.getMinX(), xformedSourceImage.getMinY(),
                                             xformedSourceImage.getWidth(), xformedSourceImage.getHeight());
            Rectangle finalBounds = bounds.intersection(new Rectangle(0, 0,
                                                                      (int) Math.round(actualCropBounds.getWidth()),
                                                                      (int) Math.round(actualCropBounds.getHeight())));
            if (finalBounds.width > 0 && finalBounds.height > 0)
                xformedSourceImage = Functions.crop(xformedSourceImage,
                                                    finalBounds.x, finalBounds.y,
                                                    finalBounds.width, finalBounds.height, null);
        }

        // We explicitly cache this
        xformedSourceImage = Functions.toUShortLinear(xformedSourceImage, null);

        if (xformedSourceImage instanceof RenderedOp)
            xformedSourceImage.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);

        return xformedSourceImage;
    }
}
