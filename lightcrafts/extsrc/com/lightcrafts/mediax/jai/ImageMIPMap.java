/*
 * $RCSfile: ImageMIPMap.java,v $
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
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import java.beans.PropertyChangeListener;
import java.util.Vector;

/**
 * A class implementing the "MIP map" operation on a
 * <code>RenderedImage</code>.  Given a <code>RenderedImage</code>,
 * which represents the image at the highest resolution level, the
 * image at each lower resolution level may be derived by performing a
 * specific chain of operations to down sample the image at the next
 * higher resolution level repeatedly.  The highest resolution level is
 * defined as level 0.
 *
 * <p> The <code>downSampler</code> is a chain of operations that is
 * used to derive the image at the next lower resolution level from
 * the image at the current resolution level.  That is, given an image
 * at resolution level <code>i</code>, the <code>downSampler</code> is
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
 * @see ImagePyramid
 *
 */
public class ImageMIPMap implements ImageJAI {

    /** The image with the highest resolution. */
    protected RenderedImage highestImage;

    /** The image at the current resolution level. */
    protected RenderedImage currentImage;

    /** The current resolution level. */
    protected int currentLevel = 0;

    /** The operation chain used to derive the lower resolution images. */
    protected RenderedOp downSampler;

    /**
     * A helper object to manage firing events.
     *
     * @since JAI 1.1
     */
    protected PropertyChangeSupportJAI eventManager = null;

    /**
     * A helper object to manage the image properties.
     *
     * @since JAI 1.1
     */
    protected WritablePropertySourceImpl properties = null;

    /** The default constructor. */
    protected ImageMIPMap() {
        eventManager = new PropertyChangeSupportJAI(this);
        properties = new WritablePropertySourceImpl(null, null, eventManager);
    }

    /**
     * Constructor.  The down sampler is an "affine" operation that
     * uses the supplied <code>AffineTransform</code> and
     * <code>Interpolation</code> objects.
     * All input parameters are saved by reference.
     *
     * @param image  The image with the highest resolution.
     * @param transform  An affine matrix used with an "affine" operation
     *        to derive the lower resolution images.
     * @param interpolation  The interpolation method for the "affine"
     *        operation.  It may be <code>null</code>, in which case the
     *        default "nearest neighbor" interpolation method is used.
     *
     * @throws IllegalArgumentException if <code>image</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>transform</code> is
     *         <code>null</code>.
     */
    public ImageMIPMap(RenderedImage image,
                       AffineTransform transform,
                       Interpolation interpolation) {
        this();

        if ( image == null || transform == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(transform);
        pb.add(interpolation);

        downSampler = JAI.create("affine", pb);
        downSampler.removeSources();

        highestImage = image;
        currentImage = highestImage;
    }

    /**
     * Constructor.  The <code>downSampler</code> points to the last
     * operation node in the <code>RenderedOp</code> chain.  The very
     * first operation in the chain must not have any source images
     * specified; that is, its number of sources must be 0.  All input
     * parameters are saved by reference.
     *
     * @param image  The image with the highest resolution.
     * @param downSampler  The operation chain used to derive the lower
     *        resolution images.  No validation is done on the first
     *        operation in the chain.
     *
     * @throws IllegalArgumentException if <code>image</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>downSampler</code> is
     *         <code>null</code>.
     */
    public ImageMIPMap(RenderedImage image,
                       RenderedOp downSampler) {
        this();
        if (image == null || downSampler == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        highestImage = image;
        currentImage = highestImage;
        this.downSampler = downSampler;
    }

    /**
     * Constructs a new <code>ImageMIPMap</code> from a
     * <code>RenderedOp</code> chain.  The <code>downSampler</code>
     * points to the last operation node in the
     * <code>RenderedOp</code> chain.  The source image is determined
     * by traversing up the chain: starting at the bottom node, given by
     * the <code>downSample</code> parameter, we move to the first
     * source of the node and repeat until we find either a sourceless
     * <code>RenderedOp</code> or any other type of
     * <code>RenderedImage</code>.
     *
     * The <code>downSampler</code> parameter is saved by reference
     * and should not be modified during the lifetime of any
     * <code>ImageMIPMap</code> referring to it.
     *
     * @param downSampler  The operation chain used to derive the lower
     *        resolution images.  The source of the first node in this
     *        chain is taken as the image with the highest resolution.
     *
     * @throws IllegalArgumentException if <code>downSampler</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>downSampler</code>
     *         has no sources.
     * @throws IllegalArgumentException if an object other than a
     *         <code>RenderedImage</code> is found in the
     *         <code>downSampler</code> chain.
     */
    public ImageMIPMap(RenderedOp downSampler) {
        this();

        if ( downSampler == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (downSampler.getNumSources() == 0) {
            throw new IllegalArgumentException(
                      JaiI18N.getString("ImageMIPMap0"));
        }

        // Find the highest resolution image from the chain.
        RenderedOp op = downSampler;
        while (true) {
            Object src = op.getNodeSource(0);

            if (src instanceof RenderedOp) {
                RenderedOp srcOp = (RenderedOp)src;

                if (srcOp.getNumSources() == 0) {
                    highestImage = srcOp;
                    op.removeSources();
                    break;
                } else {
                    op = srcOp;
                }
            } else if (src instanceof RenderedImage) {
                highestImage = (RenderedImage)src;
                op.removeSources();
                break;
            } else {
                throw new IllegalArgumentException(
                          JaiI18N.getString("ImageMIPMap1"));
            }
        }

        currentImage = highestImage;
        this.downSampler = downSampler;
    }

    /**
     * Returns an array of <code>String</code>s recognized as names by
     * this property source.  If no property names match,
     * <code>null</code> will be returned.
     *
     * <p> The default implementation returns <code>null</code>, i.e.,
     * no property names are recognized.
     *
     * @return An array of <code>String</code>s giving the valid
     *         property names.
     */
    public String[] getPropertyNames() {
        return properties.getPropertyNames();
    }

    /**
     * Returns an array of <code>String</code>s recognized as names by
     * this property source that begin with the supplied prefix.  If
     * no property names are recognized, or no property names match,
     * <code>null</code> will be returned.
     * The comparison is done in a case-independent manner.
     *
     * @return An array of <code>String</code>s giving the valid
     *         property names.
     *
     * @param prefix the supplied prefix for the property source.
     *
     * @throws IllegalArgumentException if <code>prefix</code> is
     *                                  <code>null</code>.
     */
    public String[] getPropertyNames(String prefix) {
        return properties.getPropertyNames(prefix);
    }

    /**
     * Returns the class expected to be returned by a request for
     * the property with the specified name.  If this information
     * is unavailable, <code>null</code> will be returned.
     *
     * @return The <code>Class</code> expected to be return by a
     *         request for the value of this property or <code>null</code>.
     *
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public Class getPropertyClass(String name) {
        return properties.getPropertyClass(name);
    }

    /**
     * Returns the specified property.  The default implementation
     * returns <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @param name  The name of the property.
     *
     * @return The value of the property, as an Object.
     *
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     */
    public Object getProperty(String name) {
        return properties.getProperty(name);
    }

    /**
     * Sets a property on a <code>ImageMIPMap</code>.
     *
     * @param name a <code>String</code> containing the property's name.
     * @param value the property, as a general <code>Object</code>.
     *
     * @throws IllegalArgumentException  If <code>name</code> or 
     *         <code>value</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public void setProperty(String name, Object value) {
        properties.setProperty(name, value);
    }

    /**
     * Removes the named property from the <code>ImageMIPMap</code>.
     *
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public void removeProperty(String name) {
        properties.removeProperty(name);
    }

    /**
     * Add a PropertyChangeListener to the listener list. The
     * listener is registered for all properties.
     *
     * @since JAI 1.1
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        eventManager.addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property. The
     * listener will be invoked only when a call on
     * firePropertyChange names that specific property.  The case of
     * the name is ignored.
     *
     * @since JAI 1.1
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        eventManager.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener from the listener list. This
     * removes a PropertyChangeListener that was registered for all
     * properties.
     *
     * @since JAI 1.1
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        eventManager.removePropertyChangeListener(listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.  The case
     * of the name is ignored.
     *
     * @since JAI 1.1
     */
    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener listener) {
        eventManager.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Returns the current resolution level.  The highest resolution
     * level is defined as level 0.
     */
    public int getCurrentLevel() {
        return currentLevel;
    }

    /** Returns the image at the current resolution level. */
    public RenderedImage getCurrentImage() {
        return currentImage;
    }

    /**
     * Returns the image at the specified resolution level.  The
     * requested level must be greater than or equal to 0 or
     * <code>null</code> will be returned.
     *
     * @param level The specified level of resolution
     */
    public RenderedImage getImage(int level) {
        if (level < 0) {
            return null;
        }

        if (level < currentLevel) {	// restart from the highest image
            currentImage = highestImage;
            currentLevel = 0;
        }

        while (currentLevel < level) {
            getDownImage();
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

        /* Duplicate the downSampler op chain. */
        RenderedOp op = duplicate(downSampler, vectorize(currentImage));
        currentImage = op.getRendering();
        return currentImage;
    }

    /**
     * Duplicates a <code>RenderedOp</code> chain.  Each node in the
     * chain must be a <code>RenderedOp</code>.  The <code>op</code>
     * parameter points to the last <code>RenderedOp</code> in the chain.
     * The very first op in the chain must have no sources and its source
     * will be set to the supplied image vector.  When traversing up the
     * chain, if any node has more than one source, the first source will
     * be used.  The first source of each node is duplicated; all other
     * sources are copied by reference.
     *
     * @param op RenderedOp chain
     * @param vector of source images
     *
     * @throws IllegalArgumentException if <code>op</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>images</code> is <code>null</code>.
     */
    protected RenderedOp duplicate(RenderedOp op,
                                   Vector images) {
        if (images == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        //
        // Duplicates a RenderedOp with the original OperationRegistry,
        // OperationName, ParameterBlock, and RenderingHints copied over
        // by reference.  No property information is copied.
        //
        op = new RenderedOp(op.getRegistry(),
                            op.getOperationName(),
                            op.getParameterBlock(),
                            op.getRenderingHints());

        ParameterBlock pb = new ParameterBlock();
        pb.setParameters(op.getParameters());

        Vector srcs = op.getSources();
        int numSrcs = srcs.size();

        if (numSrcs == 0) {	// first op in the chain
            pb.setSources(images);

        } else {		// recursively duplicate source0
            pb.addSource(duplicate((RenderedOp)srcs.elementAt(0), images));

            for (int i = 1; i < numSrcs; i++) {
                pb.addSource(srcs.elementAt(i));
            }
        }

        op.setParameterBlock(pb);
        return op;
    }

    /**
     * Returns the current image as a <code>RenderableImage</code>.
     * This method returns a <code>MultiResolutionRenderableImage</code>.
     * The <code>numImages</code> parameter indicates the number of
     * <code>RenderedImage</code>s used to construct the
     * <code>MultiResolutionRenderableImage</code>.  Starting with the
     * current image, the images are obtained by finding the necessary
     * number of lower resolution images using the <code>downSampler</code>.
     * The current level and current image will not be changed.
     * If the width or height reaches 1, the downsampling will stop
     * and return the renderable image.
     *
     * <p> The <code>numImages</code> should be greater than or equal to 1.
     * If a value of less than 1 is specified, this method uses 1 image,
     * which is the current image.
     *
     * @param numImages The number of lower resolution images.
     * @param minX The minimum X coordinate of the Renderable, as a float.
     * @param minY The minimum Y coordinate of the Renderable, as a float.
     * @param height The height of the Renderable, as a float.
     *
     * @throws IllegalArgumentException if <code>height</code> is less than 0.
     *
     * @see MultiResolutionRenderableImage
     */
    public RenderableImage getAsRenderable(int numImages,
                                           float minX,
                                           float minY,
                                           float height) {
        Vector v = new Vector();
        v.add(currentImage);

        RenderedImage image = currentImage;
        for (int i = 1; i < numImages; i++) {
            RenderedOp op = duplicate(downSampler, vectorize(image));
            image = op.getRendering();

            if ( image.getWidth() <= 1 || image.getHeight() <= 1 ) {
                break;
            }

            v.add(image);
        }

        return new MultiResolutionRenderableImage(v, minX, minY, height);
    }

    /**
     * Returns the current image as a <code>RenderableImage</code>.
     * This method returns a <code>MultiResolutionRenderableImage</code>
     * with the current image as the only source image, minX and minY
     * set to 0.0, and height set to 1.0.
     *
     * @see MultiResolutionRenderableImage
     */
    public RenderableImage getAsRenderable() {
        return getAsRenderable(1, 0.0F, 0.0F, 1.0F);
    }

    // XXX - see OpImage vectorize and consolidate?
    //       could be public static in PlanarImage
    /**
     * Creates and returns a <code>Vector</code> containing a single
     * element equal to the supplied <code>RenderedImage</code>.
     *
     *
     * @since JAI 1.1
     */
    protected final Vector vectorize(RenderedImage image) {
        Vector v = new Vector(1);
        v.add(image);
        return v;
    }

    /**
     * Creates and returns a <code>Vector</code> containing two
     * elements equal to the supplied <code>RenderedImage</code>s
     * in the order given.
     *
     *
     * @since JAI 1.1
     */
    protected final Vector vectorize(RenderedImage im1, RenderedImage im2) {
        Vector v = new Vector(2);
        v.add(im1);
        v.add(im2);
        return v;
    }
}
