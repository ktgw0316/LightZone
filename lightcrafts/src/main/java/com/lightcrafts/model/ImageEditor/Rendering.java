/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.CachedImage;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.model.CropBounds;
import com.lightcrafts.model.Operation;
import lombok.Getter;
import lombok.val;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.util.LinkedList;

public class Rendering implements Cloneable {
    @Getter
    private float scaleFactor = 1;

    @Getter
    private CropBounds cropBounds = new CropBounds();

    @Getter
    private ImageEditorEngine engine;

    private AffineTransform inputTransform = new AffineTransform();
    private AffineTransform transform = new AffineTransform();
    private final PlanarImage sourceImage;
    private PlanarImage xformedSourceImage;

    private LinkedList<Operation> pipeline = new LinkedList<Operation>();
    private ImagePyramid pyramid;

    public boolean cheapScale = false;

    private static final int MIP_SCALE_RATIO = 2;

    @Override
    public Rendering clone() /* throws CloneNotSupportedException */ {
        try {
            val object = (Rendering) super.clone();
            object.engine = null;
            object.inputTransform = buildTransform(true);
            object.transform = buildTransform(false);
            object.xformedSourceImage = null;
            object.pipeline = new LinkedList<Operation>();
            for (val op : pipeline) {
                object.pipeline.add(((BlendedOperation) op).clone(object));
            }
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

    public Rendering(PlanarImage sourceImage, ImageEditorEngine engine) {
        this.sourceImage = sourceImage;
        this.engine = engine;
        pyramid = new ImagePyramid(sourceImage, MIP_SCALE_RATIO);
        xformedSourceImage = null;
        inputTransform = buildTransform(true);
        transform = buildTransform(false);
    }

    public Rendering(PlanarImage sourceImage) {
        this(sourceImage, null);
    }

    public void update(OperationImpl op, boolean isLive) {
        if (engine != null) {
            engine.update(op, isLive);
        }
    }

    public AffineTransform getInputTransform() {
        return new AffineTransform(inputTransform);
    }

    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    public void setCropBounds(CropBounds cropBounds) {
        setCropAndScale(cropBounds, this.scaleFactor);
    }

    public void setScaleFactor(float scaleFactor) {
        setCropAndScale(null, scaleFactor);
    }

    public void setCropAndScale(CropBounds cropBounds, float scaleFactor) {
        val shouldUpdatBounds = (cropBounds != null && !cropBounds.equals(this.cropBounds));
        val shouldUpdateScale = (scaleFactor != this.scaleFactor);

        if (shouldUpdatBounds || shouldUpdateScale) {
            if (shouldUpdatBounds) {
                this.cropBounds = cropBounds;
            }
            if (shouldUpdateScale) {
                this.scaleFactor = scaleFactor;
            }
            inputTransform = buildTransform(true);
            transform = buildTransform(false);
            if (xformedSourceImage != null) {
                xformedSourceImage.dispose();
                xformedSourceImage = null;
            }
        }
    }

    private PlanarImage getXformedSourceImage() {
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

        for (val op : pipeline) {
            val operation = (OperationImpl) op;
            if (stopBefore-- == 0)
                break;

            if (operation.isActive() && !(inactive && operation.isDeactivatable())) {
                val result = operation.render(processedImage, scaleFactor < 1 ? scaleFactor : 1);
                if (result != null)
                    processedImage = result;
            }
        }
        return cropSourceImage(processedImage);
    }

    public void prefetch(Rectangle area) {
        if (pipeline == null) {
            System.out.println("Rendering.renderPipeline: null pipeline?");
            return;
        }

        PlanarImage processedImage = cropSourceImage(getXformedSourceImage());

        for (val operation : pipeline) {
            if (!operation.isActive()) {
                continue;
            }
            PlanarImage result = ((OperationImpl)operation).render(processedImage, scaleFactor < 1 ? scaleFactor : 1);
            if (result == null) {
                continue;
            }
            val indices = result.getTileIndices(area);
            if (indices != null) {
                val cachedResult = new CachedImage(new ImageLayout(result), JAIContext.fileCache);

                result.prefetchTiles(indices);
                for (val tile : indices) {
                    Raster newTile = result.getTile(tile.x, tile.y);
                    WritableRaster cachedTile = cachedResult.getWritableTile(tile.x, tile.y);
                    Functions.copyData(cachedTile, newTile);
                }
                result = cachedResult;
            }
            processedImage = result;
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

    public Rectangle getSourceBounds() {
        // NOTE: we must clone PlanarImage.getBounds() since it returns a reference to an object
        return new Rectangle(sourceImage.getBounds());
    }

    public Dimension getRenderingSize() {
        if (cropBounds.isAngleOnly()) {
            Rectangle sourceBounds = getSourceBounds();

            if (cropBounds.getAngle() != 0) {
                val center = new Point2D.Double(sourceBounds.getCenterX(), sourceBounds.getCenterY());

                sourceBounds = AffineTransform.getRotateInstance(-cropBounds.getAngle(),
                                                                 center.getX(),
                                                                 center.getY())
                        .createTransformedShape(sourceBounds).getBounds();
            }
            return new Dimension(sourceBounds.width, sourceBounds.height);
        }
        return new Dimension((int) cropBounds.getWidth(), (int) cropBounds.getHeight());
    }

    private AffineTransform buildTransform(boolean isInputTransform) {
        val sourceBounds = getSourceBounds();
        val transform = new AffineTransform();

        // Scale
        if (scaleFactor < 1 || !isInputTransform) {
            val w = sourceBounds.width;
            val h = sourceBounds.height;
            val scaleX = Math.round(scaleFactor * w) / (double) w;
            val scaleY = Math.round(scaleFactor * h) / (double) h;
            val scale = Math.min(scaleX, scaleY); // To avoid wrong cropping ratio.
            transform.preConcatenate(AffineTransform.getScaleInstance(scale, scale));
        }

        // Rotate
        if (cropBounds.getAngle() != 0) {
            // Rotate
            {
                val bounds = transform.createTransformedShape(sourceBounds).getBounds2D();
                val center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
                transform.preConcatenate(AffineTransform.getRotateInstance(-cropBounds.getAngle(),
                        center.getX(),
                        center.getY()));
            }
            // Re-crop
            {
                val bounds = transform.createTransformedShape(sourceBounds).getBounds2D();
                transform.preConcatenate(AffineTransform.getTranslateInstance(-bounds.getMinX(),
                        -bounds.getMinY()));
            }
        }

        // Crop
        if (!cropBounds.isAngleOnly()) {
            val actualCropBounds = CropBounds.transform(transform, cropBounds);
            val cropUpperLeft = actualCropBounds.getUpperLeft();
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

            val zero = transform.transform(new Point2D.Double(0, 0), null);
            val one = transform.transform(new Point2D.Double(1, 1), null);

            val dx = one.getX() - zero.getX();
            val dy = one.getY() - zero.getY();
            double scale = Math.sqrt((dx*dx + dy*dy) / 2.0);

            if (!cheapScale && scale <= 0.5) {
                int level = 0;
                while(scale <= 1/(double) MIP_SCALE_RATIO) {
                    scale *= MIP_SCALE_RATIO;
                    level++;
                }
                image = (PlanarImage) pyramid.getImage(level);
                transform = new AffineTransform(transform);
                transform.concatenate(AffineTransform.getScaleInstance(
                        sourceImage.getWidth() / (double)image.getWidth(),
                        sourceImage.getHeight() / (double)image.getHeight()));
            }

            if (!transform.isIdentity()) {
                val extenderHints = new RenderingHints(
                        JAI.KEY_BORDER_EXTENDER,
                        BorderExtender.createInstance(BorderExtender.BORDER_COPY));
                val params = new ParameterBlock();
                params.addSource(image);
                params.add(transform);
                params.add(Interpolation.getInstance(
                        cheapScale ? Interpolation.INTERP_BILINEAR : Interpolation.INTERP_BICUBIC));
                xformedSourceImage = JAI.create("Affine", params, extenderHints);
            }
            else {
                xformedSourceImage = image;
            }
        }

        // We explicitly cache this
        xformedSourceImage = Functions.toUShortLinear(xformedSourceImage, null);

        if (xformedSourceImage instanceof RenderedOp) {
            xformedSourceImage.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
        }

        return xformedSourceImage;
    }

    float getScaleToFit(Dimension bounds) {
        val newDimension = cropBounds.getDimensionToFit(bounds);
        val dimension = getRenderingSize();
        return (float) Math.min(
                newDimension.getWidth() / dimension.getWidth(),
                newDimension.getHeight() / dimension.getHeight());
    }

    private PlanarImage cropSourceImage(PlanarImage xformedSourceImage) {
        if (!cropBounds.isAngleOnly()) {
            val actualCropBounds = CropBounds.transform(inputTransform, cropBounds);
            val bounds = new Rectangle(
                    xformedSourceImage.getMinX(), xformedSourceImage.getMinY(),
                    xformedSourceImage.getWidth(), xformedSourceImage.getHeight());

            // Calculate inner width and height for actualCropBounds,
            // while keeping the actualCropBound's aspect ratio as precisely as possible.
            val actualWidth  = actualCropBounds.getWidth();
            val actualHeight = actualCropBounds.getHeight();
            int intWidth  = (int) Math.round(actualWidth);
            int intHeight = (int) Math.round(actualHeight);

            val finalBounds = bounds.intersection(new Rectangle(0, 0, intWidth, intHeight));

            if (finalBounds.width > 0 && finalBounds.height > 0) {
                val ratio = actualWidth / actualHeight;
                if (intWidth > finalBounds.width) {
                    finalBounds.height = (int) (finalBounds.width / ratio);
                }
                if (intHeight > finalBounds.height) {
                    finalBounds.width = (int) (finalBounds.height * ratio);
                }
                xformedSourceImage = Functions.crop(
                        xformedSourceImage,
                        finalBounds.x, finalBounds.y,
                        finalBounds.width, finalBounds.height, null);
            }
        }
        val hFlip = cropBounds.isFlippedHorizontally();
        val vFlip = cropBounds.isFlippedVertically();
        if (hFlip || vFlip) {
            xformedSourceImage = Functions.flip(
                    xformedSourceImage, hFlip, vFlip, null);
        }
        return xformedSourceImage;
    }
}
