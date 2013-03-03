/*
 * $RCSfile: ImagePyramid.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:09 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.image.RenderedImage;
import java.util.Vector;

/**
 * A class implementing the "Pyramid" operation on a
 * <code>RenderedImage</code>.  Given a <code>RenderedImage</code>
 * which represents the image at the highest resolution level,
 * the images at lower resolution levels may be derived by
 * performing a specific chain of operations to downsample the
 * image at the higher resolution level repeatedly.  Similarly,
 * once an image at a lower resolution level is obtained, the images
 * at higher resolution levels may be retrieved by performing a
 * specific chain of operations to upsample the image at the lower
 * resolution level repeatedly.
 *
 * <p> When an image is downsampled, the image at the higher resolution
 * level is lost.  However, the difference image between the original image
 * and the image obtained by up sampling the downsampled result image is
 * saved.  This difference image, combined with the up sampling operations
 * is used to retrieve the image at a higher resolution level from the
 * image at a lower resolution level.
 *
 * <p> This is a bi-directional operation.  A user may request an image
 * at any resolution level greater than or equal to the highest
 * resolution level, which is defined as level 0.
 *
 * <p> The <code>downSampler</code> is a chain of operations that is
 * used to derive the image at the next lower resolution level from
 * the image at the current resolution level.  That is, given an image
 * at resolution level <code>i</code>, <code>downSampler</code> is
 * used to obtain the image at resolution level <code>i+1</code>.
 * The chain may contain one or more operation nodes; however, each
 * node must be a <code>RenderedOp</code>.  The parameter points to the
 * last node in the chain.  The very first node in the chain must be
 * a <code>RenderedOp</code> that takes one <code>RenderedImage</code>
 * as its source.  All other nodes may have multiple sources.  When
 * traversing back up the chain, if a node has more than one source,
 * the first source, <code>source0</code>, is used to move up the
 * chain.  This parameter is saved by reference.
 *
 * <p> The <code>upSampler</code> is a chain of operations that is
 * used to derive the image at the next higher resolution level from
 * the image at the current resolution level.  That is, given an image
 * at resolution level <code>i</code>, <code>upSampler</code> is
 * used to obtain the image at resolution level <code>i-1</code>.
 * The requirement for this parameter is identical to that of the
 * <code>downSampler</code> parameter.
 *
 * <p> The <code>differencer</code> is a chain of operations that is used
 * to find the difference between an image at a particular resolution
 * level and the image obtained by first down sampling that image
 * then up sampling the result image of the down sampling operations.
 * The chain may contain one or more operation nodes; however, each
 * node must be a <code>RenderedOp</code>.  The parameter points to the
 * last node in the chain.  The very first node in the chain must be
 * a <code>RenderedOp</code> that takes two <code>RenderedImage</code>s
 * as its sources.  When traversing back up the chain, if a node has
 * more than one source, the first source, <code>source0</code>, is
 * used to move up the chain.  This parameter is saved by reference.
 *
 * <p> The <code>combiner</code> is a chain of operations that is
 * used to combine the result image of the up sampling operations
 * and the difference image saved to retrieve an image at a higher
 * resolution level.  The requirement for this parameter is identical
 * to that of the <code>differencer</code> parameter.
 *
 * <p> Reference:
 * "The Laplacian Pyramid as a Compact Image Code"
 * Peter J. Burt and Edward H. Adelson
 * IEEE Transactions on Communications, Vol. COM-31, No. 4, April 1983
 *
 * @see ImageMIPMap
 *
 */
public class ImagePyramid extends ImageMIPMap {

    /** The operation chain used to derive the higher resolution images. */
    protected RenderedOp upSampler;

    /** The operation chain used to differ two images. */
    protected RenderedOp differencer;

    /** The operation chain used to combine two images. */
    protected RenderedOp combiner;

    /** The default constructor. */
    protected ImagePyramid() {}

    /** The saved difference images. */
    private Vector diffImages = new Vector();

    /**
     * Constructor.  The <code>RenderedOp</code> parameters point to
     * the last operation node in each chain.  The first operation in
     * each chain must not have any source images specified; that is,
     * its number of sources must be 0.  All input parameters are saved
     * by reference.
     *
     * @param image  The image with the highest resolution.
     * @param downSampler  The operation chain used to derive the lower
     *        resolution images.
     * @param upSampler  The operation chain used to derive the higher
     *        resolution images.
     * @param differencer  The operation chain used to differ two images.
     * @param combiner  The operation chain used to combine two images.
     *
     * @throws IllegalArgumentException if <code>image</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>downSampler</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>upSampler</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>differencer</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>combiner</code> is
     *         <code>null</code>.
     */
    public ImagePyramid(RenderedImage image,
                        RenderedOp downSampler,
                        RenderedOp upSampler,
                        RenderedOp differencer,
                        RenderedOp combiner) {
        super(image, downSampler);

        if (upSampler == null || differencer == null || combiner == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.upSampler = upSampler;
        this.differencer = differencer;
        this.combiner = combiner;
    }

    /**
     * Constructor.  The <code>RenderedOp</code> parameters point to
     * the last operation node in each chain.  The first operation in
     * the <code>downSampler</code> chain must have the image with
     * the highest resolution as its source.  The first operation in
     * all other chains must not have any source images specified;
     * that is, its number of sources must be 0.  All input parameters
     * are saved by reference.
     *
     * @param downSampler  The operation chain used to derive the lower
     *        resolution images.
     * @param upSampler  The operation chain used to derive the higher
     *        resolution images.
     * @param differencer  The operation chain used to differ two images.
     * @param combiner  The operation chain used to combine two images.
     *
     * @throws IllegalArgumentException if <code>downSampler</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>upSampler</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>differencer</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>combiner</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>downSampler</code>
     *         has no sources.
     * @throws IllegalArgumentException if an object other than a
     *         <code>RenderedImage</code> is found in the
     *         <code>downSampler</code> chain.
     */
    public ImagePyramid(RenderedOp downSampler,
                        RenderedOp upSampler,
                        RenderedOp differencer,
                        RenderedOp combiner) {
        super(downSampler);

        if (upSampler == null || differencer == null || combiner == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.upSampler = upSampler;
        this.differencer = differencer;
        this.combiner = combiner;
    }

    /**
     * Returns the image at the specified resolution level.  The
     * requested level must be greater than or equal to 0 or
     * <code>null</code> will be returned.  The image is obtained
     * by either down sampling or up sampling the current image.
     *
     * @param level The specified resolution level.
     */
    public RenderedImage getImage(int level) {
        if (level < 0) {
            return null;
        }

        while (currentLevel < level) {
            getDownImage();
        }
        while (currentLevel > level) {
            getUpImage();
        }

        return currentImage;
    }

    /**
     * Returns the image at the next lower resolution level,
     * obtained by applying the <code>downSampler</code> on the
     * image at the current resolution level.
     */
    public RenderedImage getDownImage() {
        currentLevel++;

        // Duplicate the downSampler op chain.

        RenderedOp downOp = duplicate(downSampler, vectorize(currentImage));

        // Save the difference image.
        RenderedOp upOp = duplicate(upSampler, vectorize(downOp.getRendering()));
        RenderedOp diffOp = duplicate(differencer,
                                      vectorize(currentImage, upOp.getRendering()));
        diffImages.add(diffOp.getRendering());

        currentImage = downOp.getRendering();
        return currentImage;
    }

    /**
     * Returns the image at the previous higher resolution level,
     * If the current image is already at level 0, then the current
     * image will be returned without further up sampling.
     *
     * <p> The image is obtained by first up sampling the current
     * image, then combine the result image with the previously saved
     * difference image using the <code>combiner</code> op chain.
     */
    public RenderedImage getUpImage() {
        if (currentLevel > 0) {
            currentLevel--;

            // Duplicate the upSampler op chain.
            RenderedOp upOp = duplicate(upSampler, vectorize(currentImage));

            // Retrieve diff image for this level.
            RenderedImage diffImage =
                (RenderedImage)diffImages.elementAt(currentLevel);
            diffImages.removeElementAt(currentLevel);

            RenderedOp combOp = duplicate(combiner,
                                          vectorize(upOp.getRendering(), diffImage));
            currentImage = combOp.getRendering();
        }

        return currentImage;
    }

    /**
     * Returns the difference image between the current image and the
     * image obtained by first down sampling the current image then up
     * sampling the result image of down sampling.  This is done using
     * the <code>differencer</code> op chain.  The current level and
     * current image will not be changed.
     */
    public RenderedImage getDiffImage() {
        // First downsample.
        RenderedOp downOp = duplicate(downSampler, vectorize(currentImage));

        // Then upsample.
        RenderedOp upOp = duplicate(upSampler, vectorize(downOp.getRendering()));

        // Find the difference image.
        RenderedOp diffOp = duplicate(differencer,
                                      vectorize(currentImage, upOp.getRendering()));

        return diffOp.getRendering();
    }
}
