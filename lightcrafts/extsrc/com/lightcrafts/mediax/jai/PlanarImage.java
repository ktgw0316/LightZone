/*
 * $RCSfile: PlanarImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.3 $
 * $Date: 2006/06/16 19:55:56 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.media.jai.util.DataBufferUtils;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import com.lightcrafts.media.jai.util.PropertyUtil;

/**
 * A <code>RenderedImage</code> is expressed as a collection of pixels.
 * A pixel is defined as a 1-by-1 square; its origin is the top-left
 * corner of the square (0, 0), and its energy center is located at the
 * center of the square (0.5, 0.5).
 *
 * <p> This is the fundamental base class of Java Advanced Imaging (JAI)
 * that represents a two-dimensional <code>RenderedImage</code>.
 *
 * <p> This class provides a home for the information and functionalities
 * common to all the JAI classes that implement the
 * <code>RenderedImage</code> interface, such as the image's layout,
 * sources, properties, etc.  The image layout, sources, and properties
 * may be set either at construction or subsequently using one of the
 * mutator methods supplied for the respective attribute.  In general
 * this class does not perform sanity checking on the state of its
 * variables so it is very important that subclasses set them correctly.
 * This is of particular importance with respect to the image layout.
 *
 * <p> The layout of a <code>PlanarImage</code> is specified by variables
 * <code>minX</code>, <code>minY</code>, <code>width</code>,
 * <code>height</code>, <code>tileGridXOffset</code>,
 * <code>tileGridYOffset</code>, <code>tileWidth</code>,
 * <code>tileHeight</code>, <code>sampleModel</code>, and
 * <code>colorModel</code>.  These variables do not have any default settings
 * so subclasses must set the appropriate ones at construction via the
 * <code>ImageLayout</code> argument or subsequently using
 * <code>setImageLayout()</code>.  Otherwise, unexpected errors may occur.
 * Although these variables have <code>protected</code> access, it is
 * strongly recommended that subclasses not set the values of these variables
 * directly but rather via <code>setImageLayout()</code> which performs a
 * certain few initializations based on the layout values.  The variables
 * are defined to have <code>protected</code> access for convenience.
 *
 * <p> A <code>PlanarImage</code> may have any number of
 * <code>RenderedImage</code> sources or no source at all.
 *
 * <p> All non-JAI <code>RenderedImage</code> instances must be
 * converted into <code>PlanarImage</code>s by means of the
 * <code>RenderedImageAdapter</code> and
 * <code>WritableRenderedImageAdapter</code> classes.  The
 * <code>wrapRenderedImage</code> method provides a convenient interface
 * to both add a wrapper and take a snapshot if the image is writable.
 * All of the <code>PlanarImage</code> constructors perform this wrapping
 * automatically.  Images that already extend <code>PlanarImage</code>
 * will be returned unchanged by <code>wrapRenderedImage</code>; that
 * is, it is idempotent.
 *
 * <p> Going in the other direction, existing code that makes use of
 * the <code>RenderedImage</code> interface will be able to use
 * <code>PlanarImage</code>s directly, without any changes or
 * recompilation.  Therefore, within JAI, two-dimensional images are
 * returned from methods as <code>PlanarImage</code>s, even though
 * incoming <code>RenderedImages</code> are accepted as arguments directly.
 *
 * <p> A <code>PlanarImage</code> may also have any number of properties
 * of any type.  If or how a property is used depends on the individual
 * subclass.  This class only stores the property information.  If any
 * <code>PropertyChangeListener</code>s are registered they will receive
 * a <code>PropertySourceChangeEvent</code> for each change in an image
 * property.
 *
 * <p> In general, methods in this class are implemented such that they
 * use any class variables directly instead of through their accessors for
 * performance reasons.  Subclasses need to be careful when overriding this
 * class' variable accessors that other appropriate methods are overriden
 * as well.
 *
 * <p> <code>PlanarImage</code> implements a <code>createSnapshot</code>
 * method that produces a new, immutable image with a copy of this
 * image's current contents.  In practice, this snapshot is only a
 * virtual copy; it is managed by the <code>SnapshotImage</code> class
 * in such a way as to minimize copying and memory footprint generally.
 * Multiple calls to <code>createSnapshot</code> make use of a single
 * <code>SnapshotImage</code> per <code>PlanarImage</code> in order to
 * centralize version management.  These mechanisms are transparent to
 * the API user and are discussed here only for edification.
 *
 * <p> The source and sink lists have the effect of creating a graph
 * structure between a set of <code>PlanarImage</code>s.  Note that
 * the practice of making such bidirectional connections between
 * images means that the garbage collector will not inform us when all
 * user references to a node are lost, since there will still be
 * internal references up until the point where the entire graph is
 * detached from user space.  A solution is available in the form of
 * <em>Reference Objects</em>; see <a
 * href="http://java.sun.com/j2se/1.5.0/docs/guide/refobs/">
 * http://java.sun.com/j2se/1.5.0/docs/guide/refobs/</a> for
 * more information.  These classes include <em>weak references</em>
 * that allow the Garbage Collector (GC) to collect objects they
 * reference, setting the reference to <code>null</code> in the process.
 *
 * <p> The reference problem requires us to be careful about how we
 * define the <i>reachability</i> of directed acyclic graph (DAG) nodes.
 * If we were to allow nodes to be reached by arbitrary graph traversal,
 * we would be unable to garbage collect any subgraphs of an active
 * graph at all since any node may be reached from any other.  Instead,
 * we define the set of reachable nodes as those that may be accessed
 * directly from a reference in user code, or that are the source (not
 * sink) of a reachable node.  Reachable nodes are always accessible,
 * whether they are reached by traversing upwards or downwards in the DAG.
 *
 * <p> A DAG may also contain nodes that are not reachable, that is,
 * they require a downward traversal at some point.  For example,
 * assume a node <code>A</code> is reachable, and a call to
 * <code>A.getSinks()</code> yields a <code>Vector</code> containing a
 * reference to a previously unreachable node <code>B</code>.  The
 * node <code>B</code> naturally becomes reachable by virtue of the
 * new user reference pointing to it.  However, if the user were to
 * relinquish that reference, the node might be garbage collected, and
 * a future call to <code>A.getSinks()</code> might no longer include
 * <code>B</code> in its return value.
 *
 * <p> Because the set of sinks of a node is inherently unstable, only
 * the <code>getSinks</code> method is provided for external access to
 * the sink vector at a node.  A hypothetical method such as
 * <code>getSink</code> or <code>getNumSinks</code> would produce
 * confusing results should a sink be garbage collected between that
 * call and a subsequent call to <code>getSinks</code>.
 *
 * @see java.awt.image.RenderedImage
 * @see java.lang.ref.Reference
 * @see java.lang.ref.WeakReference
 * @see ImageJAI
 * @see OpImage
 * @see RenderedImageAdapter
 * @see SnapshotImage
 * @see TiledImage
 */
public abstract class PlanarImage implements ImageJAI, RenderedImage {

    /** The UID for this image. */
    private Object UID;

    /**
     * The X coordinate of the image's top-left pixel.
     */
    protected int minX;

    /**
     * The Y coordinate of the image's top-left pixel.
     */
    protected int minY;

    /**
     * The image's width in number of pixels.
     */
    protected int width;

    /**
     * The image's height in number of pixels.
     */
    protected int height;

    /** The image's bounds. */
    // Initialize to an empty Rectangle so this object may always
    // be used as a mutual exclusion lock in getBounds().
    // private Rectangle bounds = new Rectangle();

    /**
     * The X coordinate of the top-left pixel of tile (0, 0).
     */
    protected int tileGridXOffset;

    /**
     * The Y coordinate of the top-left pixel of tile (0, 0).
     */
    protected int tileGridYOffset;

    /**
     * The width of a tile in number of pixels.
     */
    protected int tileWidth;

    /**
     * The height of a tile in number of pixels.
     */
    protected int tileHeight;

    /**
     * The image's <code>SampleModel</code>.
     */
    protected SampleModel sampleModel = null;

    /**
     * The image's <code>ColorModel</code>.
     */
    protected ColorModel colorModel = null;

    /**
     * A <code>TileFactory</code> for use in
     * {@link #createWritableRaster(SampleModel,Point)}.
     * This field will be <code>null</code> unless initialized via the
     * configuration properties passed to
     * {@link #PlanarImage(ImageLayout,Vector,Map)}.
     *
     * @since JAI 1.1.2
     */
    protected TileFactory tileFactory = null;

    /** The <code>PlanarImage</code> sources of the image. */
    private Vector sources = null;

    /** A set of <code>WeakReference</code>s to the sinks of the image. */
    private Vector sinks = null;

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

    /**
     * A <code>SnapshotImage</code> that will centralize tile
     * versioning for this image.
     */
    private SnapshotImage snapshot = null;

    /** A <code>WeakReference</code> to this image. */
    private WeakReference weakThis;

    /** Cache of registered <code>TileComputationListener</code>s. */
    private Set tileListeners = null;

    private boolean disposed = false;

    /** Array copy size, used by "cobble" methods. */
    private static final int MIN_ARRAYCOPY_SIZE = 64;

    /**
     * The default constructor.
     *
     * <p> The <code>eventManager</code> and <code>properties</code>
     * helper fields are initialized by this constructor; no other
     * non-private fields are set.
     */
    public PlanarImage() {
        this.weakThis = new WeakReference(this);

        // Create an event manager.
        eventManager = new PropertyChangeSupportJAI(this);

        // Copy the properties by reference.
        this.properties = new WritablePropertySourceImpl(null,
                                                         null,
                                                         eventManager);
        this.UID = ImageUtil.generateID(this);
    }

    /**
     * Constructor.
     *
     * <p> The image's layout is encapsulated in the <code>layout</code>
     * argument.  Note that no verification is performed to determine whether
     * the image layout has been set either at construction or subsequently.
     *
     * <p> This constructor does not provide any default settings for
     * the layout variables so all of those that will be used later must
     * be set in the <code>layout</code> argument or subsequently via
     * <code>setImageLayout()</code> before the values are used.
     * Otherwise, unexpected errors may occur.

     * <p> If the <code>SampleModel</code> is non-<code>null</code> and the
     * supplied tile dimensions are positive, then if the dimensions of the
     * supplied <code>SampleModel</code> differ from the tile dimensions, a
     * new <code>SampleModel</code> will be created for the image from the
     * supplied <code>SampleModel</code> but with dimensions equal to those
     * of a tile.
     *
     * <p> If both the <code>SampleModel</code> and the <code>ColorModel</code>
     * in the supplied <code>ImageLayout</code> are non-<code>null</code>
     * they will be tested for compatibility.  If the test fails an
     * exception will be thrown.  The test is that
     *
     * <ul>
     * <li> <code>ColorModel.isCompatibleSampleModel()</code> invoked on
     * the <code>SampleModel</code> must return <code>true</code>, and
     * <li> if the <code>ColorModel</code> is a
     * <code>ComponentColorModel</code> then:
     * <ul>
     * <li>the number of bands of the <code>SampleModel</code> must equal
     * the number of components of the <code>ColorModel</code>, and
     * <li><code>SampleModel.getSampleSize(b) >= ColorModel.getComponentSize(b)</code>
     * for all bands <code>b</code>.
     * </ul>
     * </ul>
     *
     * <p> The <code>sources</code> parameter contains a list of immediate
     * sources of this image none of which may be <code>null</code>.  All
     * <code>RenderedImage</code>s in the list are automatically converted
     * into <code>PlanarImage</code>s when necessary.  If this image has
     * no source, this argument should be <code>null</code>.
     *
     * <p> The <code>properties</code> parameter contains a mapping of image
     * properties.  All map entries which have a key which is either a
     * <code>String</code> or a <code>CaselessStringKey</code> are interpreted
     * as image properties and will be copied to the property database of
     * this image.  This parameter may be <code>null</code>.
     *
     * <p>If a {@link TileFactory}-valued mapping of the key
     * {@link JAI#KEY_TILE_FACTORY} is present in
     * <code>properties</code>, then set the instance variable
     * <code>tileFactory</code> to the specified <code>TileFactory</code>.
     * This <code>TileFactory</code> will be used by
     * {@link #createWritableRaster(SampleModel,Point)} to create
     * <code>Raster</code>s, notably in {@link #getData(Rectangle)},
     * {@link #copyData(WritableRaster)}, and
     * {@link #getExtendedData(Rectangle,BorderExtender)}.</p>
     *
     * <p> The event and property helper fields are initialized by this
     * constructor.
     *
     * @param layout  The layout of this image or <code>null</code>.
     * @param sources  The immediate sources of this image or
     *                 <code>null</code>.
     * @param properties  A <code>Map</code> containing the properties of
     *                    this image or <code>null</code>.
     *
     * @throws IllegalArgumentException if a
     *         <code>ColorModel</code> is specified in the layout and it is
     *         incompatible with the <code>SampleModel</code>
     * @throws IllegalArgumentException  If <code>sources</code>
     *         is non-<code>null</code> and any object in
     *         <code>sources</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public PlanarImage(ImageLayout layout,
                       Vector sources,
                       Map properties) {
        this();

        // Set the image layout.
        if(layout != null) {
            setImageLayout(layout);
        }

        // Set the image sources. All source Vector elements must be non-null.
        // If any source is a RenderedImage it is converted to a PlanarImage
        // before being set.
        if (sources != null) {
            setSources(sources);
        }

        if(properties != null) {
            // Add properties from parameter.
            this.properties.addProperties(properties);

            // Set tileFactory if key present.
            if(properties.containsKey(JAI.KEY_TILE_FACTORY)) {
                Object factoryValue = properties.get(JAI.KEY_TILE_FACTORY);

                // Check the class type in case 'properties' is not
                // an instance of RenderingHints.
                if(factoryValue instanceof TileFactory) {
                    this.tileFactory = (TileFactory)factoryValue;
                }
            }
        }
    }

    /**
     * Sets the image bounds, tile grid layout,
     * <code>SampleModel</code> and <code>ColorModel</code> using
     * values from an <code>ImageLayout</code> object.
     *
     * <p> If either of the tile dimensions is not set in the passed in
     * <code>ImageLayout</code> object, then the tile dimension in question
     * will be set to the corresponding image dimension.
     *
     * <p> If either of the tile grid offsets is not set in the passed in
     * <code>ImageLayout</code> object, then the tile grid offset in
     * question will be set to 0. The same is true for the <code>minX</code>
     * , <code>minY</code>, <code>width</code> and <code>height</code>
     * fields, if no value is set in the passed in <code>ImageLayout</code>
     * object, they will be set to 0.
     *
     * <p> If the <code>SampleModel</code> is non-<code>null</code> and the
     * supplied tile dimensions are positive, then if the dimensions of the
     * supplied <code>SampleModel</code> differ from the tile dimensions, a
     * new <code>SampleModel</code> will be created for the image from the
     * supplied <code>SampleModel</code> but with dimensions equal to those
     * of a tile.
     *
     * <p> If both the <code>SampleModel</code> and the <code>ColorModel</code>
     * in the supplied <code>ImageLayout</code> are non-<code>null</code>
     * they will be tested for compatibility.  If the test fails an
     * exception will be thrown.  The test is that
     *
     * <ul>
     * <li> <code>ColorModel.isCompatibleSampleModel()</code> invoked on
     * the <code>SampleModel</code> must return <code>true</code>, and
     * <li> if the <code>ColorModel</code> is a
     * <code>ComponentColorModel</code> then:
     * <ul>
     * <li>the number of bands of the <code>SampleModel</code> must equal
     * the number of components of the <code>ColorModel</code>, and
     * <li><code>SampleModel.getSampleSize(b) >= ColorModel.getComponentSize(b)</code>
     * for all bands <code>b</code>.
     * </ul>
     * </ul>
     *
     * @param layout an ImageLayout that is used to selectively
     *        override the image's layout, <code>SampleModel</code>,
     *        and <code>ColorModel</code>.  Only valid fields, i.e.,
     *        those for which <code>ImageLayout.isValid()</code> returns
     *        <code>true</code> for the appropriate mask, are used.
     *
     * @throws <code>IllegalArgumentException</code> if <code>layout</code>
     *         is <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if a
     *         <code>ColorModel</code> is specified in the layout and it is
     *         incompatible with the <code>SampleModel</code>
     *
     * @since JAI 1.1
     */
    protected void setImageLayout(ImageLayout layout) {
        if(layout == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else {
            // Set image bounds.
            if(layout.isValid(ImageLayout.MIN_X_MASK)) {
                minX = layout.getMinX(null);
            }
            if(layout.isValid(ImageLayout.MIN_Y_MASK)) {
                minY = layout.getMinY(null);
            }
            if(layout.isValid(ImageLayout.WIDTH_MASK)) {
                width = layout.getWidth(null);
            }
            if(layout.isValid(ImageLayout.HEIGHT_MASK)) {
                height = layout.getHeight(null);
            }

            // Set tile grid parameters.
            if(layout.isValid(ImageLayout.TILE_GRID_X_OFFSET_MASK)) {
                tileGridXOffset = layout.getTileGridXOffset(null);
            }
            if(layout.isValid(ImageLayout.TILE_GRID_Y_OFFSET_MASK)) {
                tileGridYOffset = layout.getTileGridYOffset(null);
            }
            if(layout.isValid(ImageLayout.TILE_WIDTH_MASK)) {
                tileWidth = layout.getTileWidth(null);
            } else {
                tileWidth = width;
            }
            if(layout.isValid(ImageLayout.TILE_HEIGHT_MASK)) {
                tileHeight = layout.getTileHeight(null);
            } else {
                tileHeight = height;
            }

            // Set SampleModel.
            if(layout.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
                sampleModel = layout.getSampleModel(null);
            }

            // Make the SampleModel dimensions equal to those of a tile.
            if(sampleModel != null && tileWidth > 0 && tileHeight > 0 &&
               (sampleModel.getWidth() != tileWidth ||
                sampleModel.getHeight() != tileHeight)) {
                sampleModel =
                    sampleModel.createCompatibleSampleModel(tileWidth,
                                                            tileHeight);
            }

            // Set ColorModel.
            if(layout.isValid(ImageLayout.COLOR_MODEL_MASK)) {
                colorModel = layout.getColorModel(null);
            }
            if(colorModel != null && sampleModel != null) {
                if(!JDKWorkarounds.areCompatibleDataModels(sampleModel,
                                                           colorModel)) {
                    throw new IllegalArgumentException(JaiI18N.getString("PlanarImage5"));
                    /* XXX Begin debugging statements: to be deleted
                    System.err.println("\n----- ERROR: "+
                                       JaiI18N.getString("PlanarImage5"));
                    System.err.println(getClass().getName());
                    System.err.println(sampleModel.getClass().getName()+": "+
                                       sampleModel);
                    System.err.println("Transfer type = "+
                                       sampleModel.getTransferType());
                    System.err.println(colorModel.getClass().getName()+": "+
                                       colorModel);
                    System.err.println("");
                    XXX End debugging statements */
                }
            }
        }
    }

    /**
     * Wraps an arbitrary <code>RenderedImage</code> to produce a
     * <code>PlanarImage</code>.  <code>PlanarImage</code> adds
     * various properties to an image, such as source and sink vectors
     * and the ability to produce snapshots, that are necessary for
     * JAI.
     *
     * <p> If the image is already a <code>PlanarImage</code>, it is
     * simply returned unchanged.  Otherwise, the image is wrapped in
     * a <code>RenderedImageAdapter</code> or
     * <code>WritableRenderedImageAdapter</code> as appropriate.
     *
     * @param image  The <code>RenderedImage</code> to be converted into
     *        a <code>PlanarImage</code>.
     *
     * @return A <code>PlanarImage</code> containing <code>image</code>'s
     *         pixel data.
     *
     * @throws IllegalArgumentException  If <code>image</code> is
     *         <code>null</code>.
     */
    public static PlanarImage wrapRenderedImage(RenderedImage image) {
        if (image == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

	if (image instanceof PlanarImage) {
	    return (PlanarImage)image;
	} else if (image instanceof WritableRenderedImage) {
	    return new WritableRenderedImageAdapter(
                           (WritableRenderedImage)image);
	} else {
	    return new RenderedImageAdapter(image);
        }
    }

    /**
     * Creates a snapshot, that is, a virtual copy of the image's
     * current contents.  If the image is not a
     * <code>WritableRenderedImage</code>, it is returned unchanged.
     * Otherwise, a <code>SnapshotImage</code> is created and the
     * result of calling its <code>createSnapshot()</code> is
     * returned.
     *
     * @return A <code>PlanarImage</code> with immutable contents.
     */
    public PlanarImage createSnapshot() {
	if (this instanceof WritableRenderedImage) {
            if (snapshot == null) {
                synchronized (this) {
                    snapshot = new SnapshotImage(this);
                }
            }
            return snapshot.createSnapshot();

	} else {
            return this;
        }
    }

    /**
     * Returns the X coordinate of the left-most column of the image.
     * The default implementation returns the corresponding instance variable.
     */
    public int getMinX() {
	return minX;
    }

    /**
     * Returns the X coordinate of the column immediately to the right
     * of the right-most column of the image.
     *
     * <p> This method is implemented in terms of <code>getMinX()</code>
     * and <code>getWidth()</code> so that subclasses which override
     * those methods do not need to override this one.
     */
    public int getMaxX() {
	return getMinX() + getWidth();
    }

    /**
     * Returns the Y coordinate of the top-most row of the image.
     * The default implementation returns the corresponding instance variable.
     */
    public int getMinY() {
	return minY;
    }

    /**
     * Returns the Y coordinate of the row immediately below the
     * bottom-most row of the image.
     *
     * <p> This method is implemented in terms of <code>getMinY()</code>
     * and <code>getHeight()</code> so that subclasses which override
     * those methods do not need to override this one.
     */
    public int getMaxY() {
	return getMinY() + getHeight();
    }

    /**
     * Returns the width of the image in number of pixels.
     * The default implementation returns the corresponding instance variable.
     */
    public int getWidth() {
	return width;
    }

    /**
     * Returns the height of the image in number of pixels.
     * The default implementation returns the corresponding instance variable.
     */
    public int getHeight() {
	return height;
    }

    /**
     * Retrieve the number of image bands.  Note that this will not equal
     * the number of color components if the image has an
     * <code>IndexColorModel</code>.  This is equivalent to calling
     * <code>getSampleModel().getNumBands()</code>.
     *
     * @since JAI 1.1
     */
    public int getNumBands() {
        return getSampleModel().getNumBands();
    }

    /**
     * Returns the image's bounds as a <code>Rectangle</code>.
     *
     * <p> The image's bounds are defined by the values returned by
     * <code>getMinX()</code>, <code>getMinY()</code>,
     * <code>getWidth()</code>, and <code>getHeight()</code>.
     * A <code>Rectangle</code> is created based on these four methods and
     * cached in this class.  Each time that this method is invoked, the
     * bounds of this <code>Rectangle</code> are updated with the values
     * returned by the four aforementioned accessors.
     *
     * <p> Because this method returns the <code>bounds</code> variable
     * by reference, the caller should not change the settings of the
     * <code>Rectangle</code>.  Otherwise, unexpected errors may occur.
     * Likewise, if the caller expects this variable to be immutable it
     * should clone the returned <code>Rectangle</code> if there is any
     * possibility that it might be changed by the <code>PlanarImage</code>.
     * This may generally occur only for instances of <code>RenderedOp</code>.
     */
    /* public Rectangle getBounds() {
        synchronized(bounds) {
            bounds.setBounds(getMinX(), getMinY(), getWidth(), getHeight());
        }

	return bounds;
    } */

    public Rectangle getBounds() {
        return new Rectangle(getMinX(), getMinY(), getWidth(), getHeight());
    }

    /**
     * Returns the X coordinate of the top-left pixel of tile (0, 0).
     * The default implementation returns the corresponding instance variable.
     */
    public int getTileGridXOffset() {
	return tileGridXOffset;
    }

    /**
     * Returns the Y coordinate of the top-left pixel of tile (0, 0).
     * The default implementation returns the corresponding instance variable.
     */
    public int getTileGridYOffset() {
	return tileGridYOffset;
    }

    /**
     * Returns the width of a tile of this image in number of pixels.
     * The default implementation returns the corresponding instance variable.
     */
    public int getTileWidth() {
	return tileWidth;
    }

    /**
     * Returns the height of a tile of this image in number of pixels.
     * The default implementation returns the corresponding instance variable.
     */
    public int getTileHeight() {
	return tileHeight;
    }

    /**
     * Returns the horizontal index of the left-most column of tiles.
     *
     * <p> This method is implemented in terms of the static method
     * <code>XToTileX()</code> applied to the values returned by primitive
     * layout accessors and so does not need to be implemented by subclasses.
     */
    public int getMinTileX() {
	return XToTileX(getMinX(), getTileGridXOffset(), getTileWidth());
    }

    /**
     * Returns the horizontal index of the right-most column of tiles.
     *
     * <p> This method is implemented in terms of the static method
     * <code>XToTileX()</code> applied to the values returned by primitive
     * layout accessors and so does not need to be implemented by subclasses.
     */
    public int getMaxTileX() {
	return XToTileX(getMinX() + getWidth() - 1,
                        getTileGridXOffset(), getTileWidth());
    }

    /**
     * Returns the number of tiles along the tile grid in the
     * horizontal direction.
     *
     * <p> This method is implemented in terms of the static method
     * <code>XToTileX()</code> applied to the values returned by primitive
     * layout accessors and so does not need to be implemented by subclasses.
     */
    public int getNumXTiles() {
        int x = getMinX();
        int tx = getTileGridXOffset();
        int tw = getTileWidth();
	return XToTileX(x + getWidth() - 1, tx, tw) - XToTileX(x, tx, tw) + 1;
    }

    /**
     * Returns the vertical index of the top-most row of tiles.
     *
     * <p> This method is implemented in terms of the static method
     * <code>YToTileY()</code> applied to the values returned by primitive
     * layout accessors and so does not need to be implemented by subclasses.
     */
    public int getMinTileY() {
	return YToTileY(getMinY(), getTileGridYOffset(), getTileHeight());
    }

    /**
     * Returns the vertical index of the bottom-most row of tiles.
     *
     * <p> This method is implemented in terms of the static method
     * <code>YToTileY()</code> applied to the values returned by primitive
     * layout accessors and so does not need to be implemented by subclasses.
     */
    public int getMaxTileY() {
	return YToTileY(getMinY() + getHeight() - 1,
                        getTileGridYOffset(), getTileHeight());
    }

    /**
     * Returns the number of tiles along the tile grid in the vertical
     * direction.
     *
     * <p> This method is implemented in terms of the static method
     * <code>YToTileY()</code> applied to the values returned by primitive
     * layout accessors and so does not need to be implemented by subclasses.
     */
    public int getNumYTiles() {
        int y = getMinY();
        int ty = getTileGridYOffset();
        int th = getTileHeight();
	return YToTileY(y + getHeight() - 1, ty, th) - YToTileY(y, ty, th) + 1;
    }

    /**
     * Converts a pixel's X coordinate into a horizontal tile index
     * relative to a given tile grid layout specified by its X offset
     * and tile width.
     *
     * <p> If <code>tileWidth < 0</code>, the results of this method
     * are undefined.  If <code>tileWidth == 0</code>, an
     * <code>ArithmeticException</code> will be thrown.
     *
     * @throws ArithmeticException  If <code>tileWidth == 0</code>.
     */
    public static int XToTileX(int x, int tileGridXOffset, int tileWidth) {
        x -= tileGridXOffset;
        if (x < 0) {
            x += 1 - tileWidth;		// force round to -infinity (ceiling)
        }
        return x/tileWidth;
    }

    /**
     * Converts a pixel's Y coordinate into a vertical tile index
     * relative to a given tile grid layout specified by its Y offset
     * and tile height.
     *
     * <p> If <code>tileHeight < 0</code>, the results of this method
     * are undefined.  If <code>tileHeight == 0</code>, an
     * <code>ArithmeticException</code> will be thrown.
     *
     * @throws ArithmeticException  If <code>tileHeight == 0</code>.
     */
    public static int YToTileY(int y, int tileGridYOffset, int tileHeight) {
        y -= tileGridYOffset;
        if (y < 0) {
            y += 1 - tileHeight;	 // force round to -infinity (ceiling)
        }
        return y/tileHeight;
    }

    /**
     * Converts a pixel's X coordinate into a horizontal tile index.
     * No attempt is made to detect out-of-range coordinates.
     *
     * <p> This method is implemented in terms of the static method
     * <code>XToTileX()</code> applied to the values returned by primitive
     * layout accessors and so does not need to be implemented by subclasses.
     *
     * @param x the X coordinate of a pixel.
     *
     * @return the X index of the tile containing the pixel.
     *
     * @throws ArithmeticException  If the tile width of this image is 0.
     */
    public int XToTileX(int x) {
        return XToTileX(x, getTileGridXOffset(), getTileWidth());
    }

    /**
     * Converts a pixel's Y coordinate into a vertical tile index.  No
     * attempt is made to detect out-of-range coordinates.
     *
     * <p> This method is implemented in terms of the static method
     * <code>YToTileY()</code> applied to the values returned by primitive
     * layout accessors and so does not need to be implemented by subclasses.
     *
     * @param y the Y coordinate of a pixel.
     *
     * @return the Y index of the tile containing the pixel.
     *
     * @throws ArithmeticException  If the tile height of this image is 0.
     */
    public int YToTileY(int y) {
        return YToTileY(y, getTileGridYOffset(), getTileHeight());
    }

    /**
     * Converts a horizontal tile index into the X coordinate of its
     * upper left pixel relative to a given tile grid layout specified
     * by its X offset and tile width.
     */
    public static int tileXToX(int tx, int tileGridXOffset, int tileWidth) {
        return tx * tileWidth + tileGridXOffset;
    }

    /**
     * Converts a vertical tile index into the Y coordinate of
     * its upper left pixel relative to a given tile grid layout
     * specified by its Y offset and tile height.
     */
    public static int tileYToY(int ty, int tileGridYOffset, int tileHeight) {
        return ty * tileHeight + tileGridYOffset;
    }

    /**
     * Converts a horizontal tile index into the X coordinate of its
     * upper left pixel.  No attempt is made to detect out-of-range
     * indices.
     *
     * <p> This method is implemented in terms of the static method
     * <code>tileXToX()</code> applied to the values returned by primitive
     * layout accessors and so does not need to be implemented by subclasses.
     *
     * @param tx the horizontal index of a tile.
     * @return the X coordinate of the tile's upper left pixel.
     */
    public int tileXToX(int tx) {
        return tileXToX(tx, getTileGridXOffset(), getTileWidth());
    }

    /**
     * Converts a vertical tile index into the Y coordinate of its
     * upper left pixel.  No attempt is made to detect out-of-range
     * indices.
     *
     * <p> This method is implemented in terms of the static method
     * <code>tileYToY()</code> applied to the values returned by primitive
     * layout accessors and so does not need to be implemented by subclasses.
     *
     * @param ty the vertical index of a tile.
     * @return the Y coordinate of the tile's upper left pixel.
     */
    public int tileYToY(int ty) {
        return tileYToY(ty, getTileGridYOffset(), getTileHeight());
    }

    /**
     * Returns a <code>Rectangle</code> indicating the active area of
     * a given tile.  The <code>Rectangle</code> is defined as the
     * intersection of the tile area and the image bounds.  No attempt
     * is made to detect out-of-range indices; tile indices lying
     * completely outside of the image will result in returning an
     * empty <code>Rectangle</code> (width and/or height less than or
     * equal to 0).
     *
     * <p> This method is implemented in terms of the primitive layout
     * accessors and so does not need to be implemented by subclasses.
     *
     * @param tileX  The X index of the tile.
     * @param tileY  The Y index of the tile.
     *
     * @return A <code>Rectangle</code>
     */
    public Rectangle getTileRect(int tileX, int tileY) {
        return getBounds().intersection(new Rectangle(
               tileXToX(tileX), tileYToY(tileY),
               getTileWidth(), getTileHeight()));
    }

    /**
     * Returns the <code>SampleModel</code> of the image.
     * The default implementation returns the corresponding instance variable.
     */
    public SampleModel getSampleModel() {
        return sampleModel;
    }

    /**
     * Returns the <code>ColorModel</code> of the image.
     * The default implementation returns the corresponding instance variable.
     */
    public ColorModel getColorModel() {
	return colorModel;
    }

    /**
     * Returns a <code>ComponentColorModel</code> created based on
     * the indicated <code>dataType</code> and <code>numBands</code>.
     *
     * <p> The <code>dataType</code> must be one of <code>DataBuffer</code>'s
     * <code>TYPE_BYTE</code>, <code>TYPE_USHORT</code>,
     * <code>TYPE_INT</code>, <code>TYPE_FLOAT</code>, or
     * <code>TYPE_DOUBLE</code>.
     *
     * <p> The <code>numBands</code> may range from 1 to 4, with the
     * following <code>ColorSpace</code> and alpha settings:
     * <ul>
     * <li> <code>numBands = 1</code>: <code>CS_GRAY</code> without alpha;
     * <li> <code>numBands = 2</code>: <code>CS_GRAY</code> with alpha;
     * <li> <code>numBands = 3</code>: <code>CS_sRGB</code> without alpha;
     * <li> <code>numBands = 4</code>: <code>CS_sRGB</code> with alpha.
     * </ul>
     * The transparency is set to <code>Transparency.TRANSLUCENT</code> if
     * alpha is used and to <code>Transparency.OPAQUE</code> otherwise.
     *
     * <p> All other inputs result in a <code>null</code> return value.
     *
     * @param dataType  The data type of the <code>ColorModel</code>.
     * @param numBands  The number of bands of the pixels the created
     *        <code>ColorModel</code> is going to work with.
     *
     * @since JAI 1.1
     */
    public static ColorModel getDefaultColorModel(int dataType,
                                                  int numBands) {
        if (dataType < DataBuffer.TYPE_BYTE ||
            dataType == DataBuffer.TYPE_SHORT ||
            dataType > DataBuffer.TYPE_DOUBLE ||
            numBands < 1 || numBands > 4) {
            return null;
        }

        ColorSpace cs = numBands <= 2 ?
                        ColorSpace.getInstance(ColorSpace.CS_GRAY) :
                        ColorSpace.getInstance(ColorSpace.CS_sRGB);

        boolean useAlpha = (numBands == 2) || (numBands == 4);
        int transparency = useAlpha ?
                           Transparency.TRANSLUCENT : Transparency.OPAQUE;

        return RasterFactory.createComponentColorModel(dataType,
                                                       cs,
                                                       useAlpha,
                                                       false,
                                                       transparency);

    }

    /**
     * Creates a <code>ColorModel</code> that may be used with the
     * specified <code>SampleModel</code>.  If a suitable
     * <code>ColorModel</code> cannot be found, this method returns
     * <code>null</code>.
     *
     * <p> Suitable <code>ColorModel</code>s are guaranteed to exist
     * for all instances of <code>ComponentSampleModel</code> whose
     * <code>dataType</code> is not <code>DataBuffer.TYPE_SHORT</code>
     * and with no more than 4 bands.  A <code>ComponentColorModel</code>
     * of either type CS_GRAY or CS_sRGB is returned.
     *
     * <p> For 1- and 3- banded <code>SampleModel</code>s, the returned
     * <code>ColorModel</code> will be opaque.  For 2- and 4-banded
     * <code>SampleModel</code>s, the output will use alpha transparency.
     *
     * <p> Additionally, an instance of <code>DirectColorModel</code>
     * will be created for instances of
     * <code>SinglePixelPackedSampleModel</code> with no more than 4 bands.
     *
     * <p> Finally, an instance of <code>IndexColorModel</code>
     * will be created for instances of
     * <code>MultiPixelPackedSampleModel</code> with a single band and a
     * pixel bit stride of unity.  This represents the case of binary data.
     *
     * <p> This method is intended as an useful utility for the creation
     * of simple <code>ColorModel</code>s for some common cases.
     * In more complex situations, it may be necessary to instantiate
     * the appropriate <code>ColorModel</code>s directly.
     *
     * @return An instance of <code>ColorModel</code> that is suitable for
     *         the supplied <code>SampleModel</code>, or <code>null</code>.
     *
     * @throws IllegalArgumentException  If <code>sm</code> is
     *         <code>null</code>.
     */
    public static ColorModel createColorModel(SampleModel sm) {
        if(sm == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        int bands = sm.getNumBands();
        if (bands < 1 || bands > 4) {
            return null;
        }

        if (sm instanceof ComponentSampleModel) {
            return getDefaultColorModel(sm.getDataType(), bands);

        } else if (sm instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm =
                (SinglePixelPackedSampleModel)sm;

            int[] bitMasks = sppsm.getBitMasks();
            int rmask = 0;
            int gmask = 0;
            int bmask = 0;
            int amask = 0;

            int numBands = bitMasks.length;
            if (numBands <= 2) {
                rmask = gmask = bmask = bitMasks[0];
                if (numBands == 2) {
                    amask = bitMasks[1];
                }
            } else {
                rmask = bitMasks[0];
                gmask = bitMasks[1];
                bmask = bitMasks[2];
                if (numBands == 4) {
                    amask = bitMasks[3];
                }
            }

            int[] sampleSize = sppsm.getSampleSize();
            int bits = 0;
            for (int i = 0; i < sampleSize.length; i++) {
                bits += sampleSize[i];
            }

            return new DirectColorModel(bits, rmask, gmask, bmask, amask);

        } else if (ImageUtil.isBinary(sm)) {
            byte[] comp = new byte[] { (byte)0x00, (byte)0xFF };

            return new IndexColorModel(1, 2, comp, comp, comp);

        } else {	// unable to create an suitable ColorModel
            return null;
        }
    }

    /**
     * Returns the value of the instance variable <code>tileFactory</code>.
     *
     * @since JAI 1.1.2
     */
    public TileFactory getTileFactory() {
        return tileFactory;
    }

    /**
     * Returns the number of immediate <code>PlanarImage</code> sources
     * this image has.  If this image has no source, this method returns
     * 0.
     */
    public int getNumSources() {
        return sources == null ? 0 : sources.size();
    }

    /**
     * Returns this image's immediate source(s) in a <code>Vector</code>.
     * If this image has no source, this method returns <code>null</code>.
     */
    public Vector getSources() {
        if (getNumSources() == 0) {
            return null;
        } else {
            synchronized (sources) {
                return (Vector)sources.clone();
            }
        }
    }

    /**
     * Returns the immediate source indicated by the index.  If there
     * is no source corresponding to the specified index, this method
     * throws an exception.
     *
     * @param index  The index of the desired source.
     *
     * @return A <code>PlanarImage</code> source.
     *
     * @throws ArrayIndexOutOfBoundsException  If this image has no
     *         immediate source, or if the index is negative or greater
     *         than the maximum source index.
     *
     * @deprecated as of JAI 1.1. Use <code>getSourceImage()</code>.
     * @see PlanarImage#getSourceImage(int)
     */
    public PlanarImage getSource(int index) {
        if (sources == null) {
            throw new ArrayIndexOutOfBoundsException(
                      JaiI18N.getString("PlanarImage0"));
        }

        synchronized (sources) {
            return (PlanarImage)sources.get(index);
        }
    }

    /**
     * Sets the list of sources from a given <code>List</code> of
     * <code>PlanarImage</code>s.  All of the existing sources are
     * discarded.  Any <code>RenderedImage</code> sources in the supplied
     * list are wrapped using <code>wrapRenderedImage()</code>.  The list
     * of sinks of each prior <code>PlanarImage</code> source and of each
     * current unwrapped <code>PlanarImage</code> source is adjusted as
     * necessary such that this image is a sink of all such current sources
     * but is removed as a sink of all such prior sources which are not
     * also current.
     *
     * @param sourceList a <code>List</code> of <code>PlanarImage</code>s.
     *
     * @throws IllegalArgumentException  If <code>sourceList</code> is
     *         <code>null</code> or contains any <code>null</code> elements.
     */
    protected void setSources(List sourceList) {
        if(sourceList == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        int size = sourceList.size();

        synchronized (this) {
            if(sources != null) {
                // Remove this image as a sink of prior PlanarImage sources.
                Iterator it = sources.iterator();
                while(it.hasNext()) {
                    Object src = it.next();
                    if(src instanceof PlanarImage) {
                        ((PlanarImage)src).removeSink(this);
                    }
                }
            }
            sources = new Vector(size);
        }

        synchronized (sources) {
            for (int i = 0; i < size; i++) {
                Object sourceElement = sourceList.get(i);
                if(sourceElement == null) {
                    throw new IllegalArgumentException(JaiI18N.getString("PlanarImage7"));
                }

                sources.add(sourceElement instanceof RenderedImage ?
                            wrapRenderedImage((RenderedImage)sourceElement) :
                            sourceElement);

                // Add as a sink of any PlanarImage source.
                if(sourceElement instanceof PlanarImage) {
                    ((PlanarImage)sourceElement).addSink(this);
                }
            }
        }
    }

    /**
     * Removes all the sources of this image.  This image is removed from
     * the list of sinks of any prior <code>PlanarImage</code>s sources.
     */
    protected void removeSources() {
        if (sources != null) {
            synchronized (this) {
                if(sources != null) {
                    // Remove this image as a sink of prior PlanarImage sources.
                    Iterator it = sources.iterator();
                    while(it.hasNext()) {
                        Object src = it.next();
                        if(src instanceof PlanarImage) {
                            ((PlanarImage)src).removeSink(this);
                        }
                    }
                }
                sources = null;
            }
        }
    }

    /**
     * Returns the immediate source indicated by the index.  If there
     * is no source corresponding to the specified index, this method
     * throws an exception.
     *
     * @param index  The index of the desired source.
     *
     * @return A <code>PlanarImage</code> source.
     *
     * @throws ArrayIndexOutOfBoundsException  If this image has no
     *         immediate source, or if the index is negative or greater
     *         than the maximum source index.
     *
     * @since JAI 1.1
     */
    public PlanarImage getSourceImage(int index) {
        if (sources == null) {
            throw new ArrayIndexOutOfBoundsException(
                      JaiI18N.getString("PlanarImage0"));
        }

        synchronized (sources) {
            return (PlanarImage)sources.get(index);
        }
    }

    /**
     * Returns the immediate source indicated by the index.  If there
     * is no source corresponding to the specified index, this method
     * throws an exception.
     *
     * @param index  The index of the desired source.
     *
     * @return An <code>Object</code> source.
     *
     * @throws ArrayIndexOutOfBoundsException  If this image has no
     *         immediate source, or if the index is negative or greater
     *         than the maximum source index.
     *
     * @since JAI 1.1
     */
    public Object getSourceObject(int index) {
        if (sources == null) {
            throw new ArrayIndexOutOfBoundsException(
                      JaiI18N.getString("PlanarImage0"));
        }

        synchronized (sources) {
            return sources.get(index);
        }
    }

    /**
     * Adds an <code>Object</code> source to the list of sources.
     * If the source is a <code>RenderedImage</code> it is wrapped using
     * <code>wrapRenderedImage()</code>.  If the unwrapped source is a
     * <code>PlanarImage</code> then this image is added to its list of sinks.
     *
     * @param source An <code>Object</code> to be added as an
     *        immediate source of this image.
     *
     * @throws IllegalArgumentException  If <code>source</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    protected void addSource(Object source) {
        if(source == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sources == null) {
            synchronized (this) {
                sources = new Vector();
            }
        }

        synchronized (sources) {
            // Add the source wrapping it if necessary.
            sources.add(source instanceof RenderedImage ?
                        wrapRenderedImage((RenderedImage)source) :
                        source);
        }

        if(source instanceof PlanarImage) {
            ((PlanarImage)source).addSink(this);
        }
    }

    /**
     * Sets an immediate source of this image.  The source to be replaced
     * with the new input <code>Object</code> is referred to by its
     * index.  This image must already have a source corresponding to the
     * specified index.  If the source is a <code>RenderedImage</code> it is
     * wrapped using <code>wrapRenderedImage()</code>.  If the unwrapped
     * source is a <code>PlanarImage</code> then this image is added to its
     * list of sinks.  If a <code>PlanarImage</code> source previously
     * existed at this index, this image is removed from its list of sinks.
     *
     * @param source  A <code>Object</code> source to be set.
     * @param index  The index of the source to be set.
     *
     * @throws ArrayIndexOutOfBoundsException  If this image has no
     *         immediate source, or if there is no source corresponding
     *         to the index value.
     * @throws IllegalArgumentException  If <code>source</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    protected void setSource(Object source, int index) {
        if(source == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sources == null) {
            throw new ArrayIndexOutOfBoundsException(
                      JaiI18N.getString("PlanarImage0"));
        }

        synchronized (sources) {
            if(index < sources.size() &&
               sources.get(index) instanceof PlanarImage) {
                getSourceImage(index).removeSink(this);
            }
            sources.set(index,
                        source instanceof RenderedImage ?
                        wrapRenderedImage((RenderedImage)source) : source);
        }
        if(source instanceof PlanarImage) {
            ((PlanarImage)source).addSink(this);
        }
    }

    /**
     * Removes an <code>Object</code> source from the list of sources.
     * If the source is a <code>PlanarImage</code> then this image
     * is removed from its list of sinks.
     *
     * @param source  The <code>Object</code> source to be removed.
     *
     * @return <code>true</code> if the element was present,
     *         <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException  If <code>source</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    protected boolean removeSource(Object source) {
        if(source == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sources == null) {
            return false;
        }

        synchronized (sources) {
            if(source instanceof PlanarImage) {
                ((PlanarImage)source).removeSink(this);
            }
            return sources.remove(source);
        }
    }

    /**
     * Returns a <code>Vector</code> containing the currently available
     * <code>PlanarImage</code> sinks of this image (images for which
     * this image is a source), or <code>null</code> if no sinks are
     * present.
     *
     * <p> Sinks are stored using weak references.  This means that
     * the set of sinks may change between calls to
     * <code>getSinks()</code> if the garbage collector happens to
     * identify a sink as not otherwise reachable (reachability is
     * discussed in the class comments for this class).
     *
     * <p> Since the pool of sinks may change as garbage collection
     * occurs, <code>PlanarImage</code> does not implement either a
     * <code>getSink(int index)</code> or a <code>getNumSinks()</code>
     * method.  Instead, the caller must call <code>getSinks()</code>,
     * which returns a Vector of normal references.  As long as the
     * returned <code>Vector</code> is referenced from user code, the
     * images it references are reachable and may be reliably
     * accessed.
     */
    public Vector getSinks() {
        Vector v = null;

        if (sinks != null) {
            synchronized (sinks) {
		int size = sinks.size();
		v = new Vector(size);
                for (int i = 0; i < size; i++) {
                    Object o = ((WeakReference)sinks.get(i)).get();

                    if (o != null) {
                        v.add(o);
                    }
                }
            }

            if (v.size() == 0) {
                v = null;
            }
        }
        return v;
    }

    /**
     * Adds an <code>Object</code> sink to the list of sinks.
     *
     * @return <code>true</code> if the element was added,
     *         <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException if
     * <code>sink</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public synchronized boolean addSink(Object sink) {
        if (sink == null) {
	  throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sinks == null) {
            sinks = new Vector();
        }

        boolean result = false;
        if(sink instanceof PlanarImage) {
	    result = sinks.add(((PlanarImage)sink).weakThis);
        } else {
            result = sinks.add(new WeakReference(sink));
        }

        return result;
    }

    /**
     * Removes an <code>Object</code> sink from the list of sinks.
     *
     * @return <code>true</code> if the element was present,
     *         <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException if
     * <code>sink</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public synchronized boolean removeSink(Object sink) {
        if (sink == null) {
	  throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sinks == null) {
            return false;
        }

        boolean result = false;
        if(sink instanceof PlanarImage) {
	    result = sinks.remove(((PlanarImage)sink).weakThis);
        } else {
            Iterator it = sinks.iterator();
            while(it.hasNext()) {
                Object referent = ((WeakReference)it.next()).get();
                if(referent == sink) {
                    // Remove the sink.
                    it.remove();
                    result = true;
                    // Do not break: could be more than one.
                } else if(referent == null) {
                    // A cleared reference: might as well remove it.
                    it.remove(); // ignore return value here.
                }
            }
        }

        return result;
    }

    /**
     * Adds a <code>PlanarImage</code> sink to the list of sinks.
     *
     * @param sink A <code>PlanarImage</code> to be added as a sink.
     *
     * @throws IllegalArgumentException  If <code>sink</code> is
     *         <code>null</code>.
     *
     * @deprecated as of JAI 1.1. Use <code>addSink(Object)</code> instead.
     */
    protected void addSink(PlanarImage sink) {
        if(sink == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sinks == null) {
            synchronized (this) {
                sinks = new Vector();
            }
        }

        synchronized (sinks) {
	    sinks.add(sink.weakThis);
        }
    }

    /**
     * Removes a <code>PlanarImage</code> sink from the list of sinks.
     *
     * @param sink  A <code>PlanarImage</code> sink to be removed.
     *
     * @return <code>true</code> if the element was present,
     *         <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException  If <code>sink</code> is
     *         <code>null</code>.
     * @throws IndexOutOfBoundsException  If <code>sink</code> is not
     *         in the sink list.
     *
     * @deprecated as of JAI 1.1. Use <code>removeSink(Object)</code> instead.
     */
    protected boolean removeSink(PlanarImage sink) {
        if(sink == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sinks == null) {
            return false;
        }

        synchronized (sinks) {
            return sinks.remove(sink.weakThis);
        }
    }

    /** Removes all the sinks of this image. */
    public void removeSinks() {
        if (sinks != null) {
            synchronized (this) {
                sinks = null;
            }
        }
    }

    /**
     * Returns the internal <code>Hashtable</code> containing the
     * image properties by reference.
     */
    protected Hashtable getProperties() {
        return (Hashtable)properties.getProperties();
    }

    /**
     * Sets the <code>Hashtable</code> containing the image properties
     * to a given <code>Hashtable</code>.  The <code>Hashtable</code>
     * is incorporated by reference and must not be altered by other
     * classes after this method is called.
     */
    protected void setProperties(Hashtable properties) {
        this.properties.addProperties(properties);
    }

    /**
     * Gets a property from the property set of this image.  If the
     * property name is not recognized,
     * <code>java.awt.Image.UndefinedProperty</code> will be returned.
     *
     * @param name the name of the property to get, as a <code>String</code>.
     *
     * @return A reference to the property <code>Object</code>, or the value
     *         <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @exception IllegalArgumentException if <code>propertyName</code>
     *                                     is <code>null</code>.
     */
    public Object getProperty(String name) {
        return properties.getProperty(name);
    }

    /**
     * Returns the class expected to be returned by a request for
     * the property with the specified name.  If this information
     * is unavailable, <code>null</code> will be returned.
     *
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     *
     * @return The <code>Class</code> expected to be return by a
     *         request for the value of this property or <code>null</code>.
     *
     * @since JAI 1.1
     */
    public Class getPropertyClass(String name) {
        return properties.getPropertyClass(name);
    }

    /**
     * Sets a property on a <code>PlanarImage</code>.  Some
     * <code>PlanarImage</code> subclasses may ignore attempts to set
     * properties.
     *
     * @param name a <code>String</code> containing the property's name.
     * @param value the property, as a general <code>Object</code>.
     *
     * @throws IllegalArgumentException  If <code>name</code> or
     *         <code>value</code> is <code>null</code>.
     */
    public void setProperty(String name, Object value) {
        properties.setProperty(name, value);
    }

    /**
     * Removes the named property from the <code>PlanarImage</code>.
     * Some <code>PlanarImage</code> subclasses may ignore attempts to
     * remove properties.
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
     * Returns a list of property names that are recognized by this image
     * or <code>null</code> if none are recognized.
     *
     * @return an array of <code>String</code>s containing valid
     *         property names or <code>null</code>.
     */
    public String[] getPropertyNames() {
        return properties.getPropertyNames();
    }

    /**
     * Returns an array of <code>String</code>s recognized as names by
     * this property source that begin with the supplied prefix.  If
     * no property names match, <code>null</code> will be returned.
     * The comparison is done in a case-independent manner.
     *
     * <p> The default implementation calls
     * <code>getPropertyNames()</code> and searches the list of names
     * for matches.
     *
     * @return an array of <code>String</code>s giving the valid
     *         property names.
     *
     * @throws IllegalArgumentException  If <code>prefix</code> is
     *         <code>null</code>.
     */
    public String[] getPropertyNames(String prefix) {
        return PropertyUtil.getPropertyNames(getPropertyNames(), prefix);
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
        eventManager.addPropertyChangeListener(propertyName.toLowerCase(),
                                               listener);
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
        eventManager.removePropertyChangeListener(propertyName.toLowerCase(),
                                                  listener);
    }

    private synchronized Set getTileComputationListeners(boolean createIfNull) {
        if(createIfNull && tileListeners == null) {
            tileListeners = Collections.synchronizedSet(new HashSet());
        }
        return tileListeners;
    }

    /**
     * Adds a <code>TileComputationListener</code> to the list of
     * registered <code>TileComputationListener</code>s.  This listener
     * will be notified when tiles requested via <code>queueTiles()</code>
     * have been computed.
     *
     * @param listener The <code>TileComputationListener</code> to register.
     * @throws IllegalArgumentException if <code>listener</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public synchronized void
        addTileComputationListener(TileComputationListener listener) {
        if(listener == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Set listeners = getTileComputationListeners(true);

        listeners.add(listener);
    }

    /**
     * Removes a <code>TileComputationListener</code> from the list of
     * registered <code>TileComputationListener</code>s.
     *
     * @param listener The <code>TileComputationListener</code> to unregister.
     * @throws IllegalArgumentException if <code>listener</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public synchronized void
        removeTileComputationListener(TileComputationListener listener) {
        if(listener == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Set listeners = getTileComputationListeners(false);

        if(listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Retrieves a snapshot of the set of all registered
     * <code>TileComputationListener</code>s as of the moment this
     * method is invoked.
     *
     * @return All <code>TileComputationListener</code>s or
     *         <code>null</code> if there are none.
     *
     * @since JAI 1.1
     */
    public TileComputationListener[] getTileComputationListeners() {

        Set listeners = getTileComputationListeners(false);

        if(listeners == null) {
            return null;
        }

        return (TileComputationListener[])listeners.toArray(new TileComputationListener[listeners.size()]);
    }

    /**
     * Within a given rectangle, store the list of tile seams of both
     * X and Y directions into the corresponding split sequence.
     *
     * @param xSplits An <code>IntegerSequence</code> to which the
     *        tile seams in the X direction are to be added.
     * @param ySplits An <code>IntegerSequence</code> to which the
     *        tile seams in the Y direction are to be added.
     * @param rect The rectangular region of interest.
     *
     * @throws IllegalArgumentException  If <code>xSplits</code>,
     *         <code>ySplits</code>, or <code>rect</code>
     *         is <code>null</code>.
     */
    public void getSplits(IntegerSequence xSplits,
                          IntegerSequence ySplits,
                          Rectangle rect) {
        if(xSplits == null || ySplits == null || rect == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        int minTileX = XToTileX(rect.x);
        int maxTileX = XToTileX(rect.x + rect.width - 1);
        int xTilePos = tileXToX(minTileX);
        for (int i = minTileX; i <= maxTileX; i++) {
            xSplits.insert(xTilePos);
            xTilePos += tileWidth;
        }

        int minTileY = YToTileY(rect.y);
        int maxTileY = YToTileY(rect.y + rect.height - 1);
        int yTilePos = tileYToY(minTileY);
        for (int i = minTileY; i <= maxTileY; i++) {
            ySplits.insert(yTilePos);
            yTilePos += tileHeight;
        }
    }

    /**
     * Returns an array containing the indices of all tiles which overlap
     * the specified <code>Rectangle</code>.  If the <code>Rectangle</code>
     * does not intersect the image bounds then <code>null</code> will be
     * returned.  If an array is returned, it will be ordered in terms of
     * the row major ordering of its contained tile indices.  If the
     * specified <code>Rectangle</code> is <code>null</code>, the tile
     * indicies for the entire image will be returned.
     *
     * @param region The <code>Rectangle</code> of interest.
     * @return An array of the indices of overlapping tiles or
     *         <code>null</code> if <code>region</code> does not intersect
     *         the image bounds.
     *
     * @since JAI 1.1
     */
    public Point[] getTileIndices(Rectangle region) {
        if(region == null) {
            region = (Rectangle) getBounds().clone();
        } else if(!region.intersects(getBounds())) {
            return null;
        } else {
            region = region.intersection(getBounds());
            if(region.isEmpty()) {
                return null;
            }
        }

        if(region == null) {
            region = getBounds();
        } else {
            Rectangle r = new Rectangle(getMinX(), getMinY(),
                                        getWidth() + 1, getHeight() + 1);
            if(!region.intersects(r)) {
                return null;
            } else {
                region = region.intersection(r);
            }
        }

        int minTileX = XToTileX(region.x);
        int maxTileX = XToTileX(region.x + region.width - 1);
        int minTileY = YToTileY(region.y);
        int maxTileY = YToTileY(region.y + region.height - 1);

        Point[] tileIndices =
            new Point[(maxTileY-minTileY+1)*(maxTileX-minTileX+1)];

        int tileIndexOffset = 0;
        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                tileIndices[tileIndexOffset++] = new Point(tx, ty);
            }
        }

        return tileIndices;
    }

    /**
     * Returns <code>true</code> if and only if the intersection of
     * the specified <code>Rectangle</code> with the image bounds
     * overlaps more than one tile.
     *
     * @throws IllegalArgumentException if <code>rect</code> is
     * <code>null</code>.
     */
     public boolean overlapsMultipleTiles(Rectangle rect) {
        if(rect == null) {
            throw new IllegalArgumentException("rect == null!");
        }

        Rectangle xsect = rect.intersection(getBounds());

        // 'true' if and only if non-empty and more than one tile in
        // either horizontal or vertical direction.
        return !xsect.isEmpty() &&
            (XToTileX(xsect.x) != XToTileX(xsect.x + xsect.width - 1) ||
             YToTileY(xsect.y) != YToTileY(xsect.y + xsect.height - 1));
     }

    /**
     * Creates a <code>WritableRaster</code> with the specified
     * <code>SampleModel</code> and location.  If <code>tileFactory</code>
     * is non-<code>null</code>, it will be used to create the
     * <code>WritableRaster</code>; otherwise
     * {@link RasterFactory#createWritableRaster(SampleModel,Point)}
     * will be used.
     *
     * @param sampleModel The <code>SampleModel</code> to use.
     * @param location The origin of the <code>WritableRaster</code>; if
     * <code>null</code>, <code>(0,&nbsp;0)</code> will be used.
     *
     * @throws IllegalArgumentException if <code>sampleModel</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    protected final WritableRaster
        createWritableRaster(SampleModel sampleModel, Point location) {

        if(sampleModel == null) {
            throw new IllegalArgumentException("sampleModel == null!");
        }

        return tileFactory != null ?
            tileFactory.createTile(sampleModel, location) :
            RasterFactory.createWritableRaster(sampleModel, location);
    }

    /**
     * Returns the entire image in a single <code>Raster</code>.  For
     * images with multiple tiles this will require creating a new
     * <code>Raster</code> and copying data from multiple tiles into
     * it ("cobbling").
     *
     * <p>The returned <code>Raster</code> is semantically a copy.
     * This means that subsequent updates to this image will not be
     * reflected in the returned <code>Raster</code>. For non-writable
     * (immutable) images, the returned value may be a reference to the
     * image's internal data. The returned <code>Raster</code> should
     * be considered non-writable; any attempt to alter its pixel data
     * (such as by casting it to a <code>WritableRaster</code> or obtaining
     * and modifying its <code>DataBuffer</code>) may result in undefined
     * behavior. The <code>copyData</code> method should be used if the
     * returned <code>Raster</code> is to be modified.
     *
     * <p> For a very large image, more than
     * <code>Integer.MAX_VALUE</code> entries could be required in the
     * returned <code>Raster</code>'s underlying data array.  Since
     * the Java language does not permit such an array, an
     * <code>IllegalArgumentException</code> will be thrown.
     *
     * @return A <code>Raster</code> containing the entire image data.
     *
     * @throws IllegalArgumentException  If the size of the returned data
     *         is too large to be stored in a single <code>Raster</code>.
     */
    public Raster getData() {
        return getData(null);
    }

    /**
     * Returns a specified region of this image in a <code>Raster</code>.
     *
     * <p> The returned <code>Raster</code> is semantically a copy.
     * This means that subsequent updates to this image will not be
     * reflected in the returned <code>Raster</code>. For non-writable
     * (immutable) images, the returned value may be a reference to the
     * image's internal data. The returned <code>Raster</code> should
     * be considered non-writable; any attempt to alter its pixel data
     * (such as by casting it to a <code>WritableRaster</code> or obtaining
     * and modifying its <code>DataBuffer</code>) may result in undefined
     * behavior. The <code>copyData</code> method should be used if the
     * returned <code>Raster</code> is to be modified.
     *
     * <p> The region of the image to be returned is specified by a
     * <code>Rectangle</code>. This region may go beyond this image's
     * boundary. If so, the pixels in the areas outside this image's
     * boundary are left unset.  Use <code>getExtendedData</code> if
     * a specific extension policy is required.
     *
     * <p> The <code>region</code> parameter may also be
     * <code>null</code>, in which case the entire image data is
     * returned in the <code>Raster</code>.
     *
     * <p> If <code>region</code> is non-<code>null</code> but does
     * not intersect the image bounds at all, an
     * <code>IllegalArgumentException</code> will be thrown.
     *
     * <p> It is possible to request a region of an image that would
     * require more than <code>Integer.MAX_VALUE</code> entries
     * in the returned <code>Raster</code>'s underlying data array.
     * Since the Java language does not permit such an array,
     * an <code>IllegalArgumentException</code> will be thrown.
     *
     * @param region The rectangular region of this image to be
     * returned, or <code>null</code>.
     *
     * @return A <code>Raster</code> containing the specified image data.
     *
     * @throws IllegalArgumentException  If the region does not
     *         intersect the image bounds.
     * @throws IllegalArgumentException  If the size of the returned data
     *         is too large to be stored in a single <code>Raster</code>.
     */
    public Raster getData(Rectangle region) {
        Rectangle b = getBounds();	// image's bounds

        if (region == null) {
            region = b;
        } else if (!region.intersects(b)) {
            throw new IllegalArgumentException(
                JaiI18N.getString("PlanarImage4"));
        }

        // Get the intersection of the region and the image bounds.
        Rectangle xsect = region == b ? region : region.intersection(b);

        // Compute tile indices over the intersection.
        int startTileX = XToTileX(xsect.x);
        int startTileY = YToTileY(xsect.y);
        int endTileX = XToTileX(xsect.x + xsect.width - 1);
        int endTileY = YToTileY(xsect.y + xsect.height - 1);

        if (startTileX == endTileX && startTileY == endTileY &&
            getTileRect(startTileX, startTileY).contains(region)) {
            // Requested region is within a single tile.
            Raster tile = getTile(startTileX, startTileY);

            if(this instanceof WritableRenderedImage) {
                // Returned Raster must not change if the corresponding
                // image data are modified so if this image is mutable
                // a copy must be created.
                SampleModel sm = tile.getSampleModel();
                if(sm.getWidth() != region.width ||
                   sm.getHeight() != region.height) {
                    sm = sm.createCompatibleSampleModel(region.width,
                                                        region.height);
                }
                WritableRaster destinationRaster =
                    createWritableRaster(sm, region.getLocation());
                Raster sourceRaster =
                    tile.getBounds().equals(region) ?
                    tile : tile.createChild(region.x, region.y,
                                            region.width, region.height,
                                            region.x, region.y,
                                            null);
                JDKWorkarounds.setRect(destinationRaster, sourceRaster);
                return destinationRaster;
            } else {
                // Image is immutable so returning the tile or a child
                // thereof is acceptable.
                return tile.getBounds().equals(region) ?
                    tile : tile.createChild(region.x, region.y,
                                            region.width, region.height,
                                            region.x, region.y,
                                            null);
            }
        } else {
            // Extract a region crossing tiles into a new WritableRaster
            WritableRaster dstRaster;
            SampleModel srcSM = getSampleModel();
            int dataType = srcSM.getDataType();
            int nbands = srcSM.getNumBands();
            boolean isBandChild = false;

            ComponentSampleModel csm = null;
            int[] bandOffs = null;

            boolean fastCobblePossible = false;
            if (srcSM instanceof ComponentSampleModel) {
                csm = (ComponentSampleModel)srcSM;
                int ps = csm.getPixelStride();
                boolean isBandInt = (ps == 1 && nbands > 1);
                isBandChild = (ps > 1 && nbands != ps);
                if ( (!isBandChild) && (!isBandInt)) {
                    bandOffs = csm.getBandOffsets();
                    int i;
                    for (i=0; i<nbands; i++) {
                        if (bandOffs[i] >= nbands) {
                            break;
                        }
                    }
                    if (i == nbands) {
                        fastCobblePossible = true;
                    }
                }
            }

            if (fastCobblePossible) {
                // For acceptable cases of ComponentSampleModel,
                // use an optimized cobbler which directly accesses the
                // tile DataBuffers, using arraycopy whenever possible.
                try {
                    SampleModel interleavedSM =
                        RasterFactory.createPixelInterleavedSampleModel(
                            dataType,
                            region.width,
                            region.height,
                            nbands,
                            region.width*nbands,
                            bandOffs);
                    dstRaster = createWritableRaster(interleavedSM,
                                                     region.getLocation());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        JaiI18N.getString("PlanarImage2"));
                }

                switch (dataType) {
                  case DataBuffer.TYPE_BYTE:
                    cobbleByte(region, dstRaster);
                    break;
                  case DataBuffer.TYPE_SHORT:
                    cobbleShort(region, dstRaster);
                    break;
                  case DataBuffer.TYPE_USHORT:
                    cobbleUShort(region, dstRaster);
                    break;
                  case DataBuffer.TYPE_INT:
                    cobbleInt(region, dstRaster);
                    break;
                  case DataBuffer.TYPE_FLOAT:
                    cobbleFloat(region, dstRaster);
                    break;
                  case DataBuffer.TYPE_DOUBLE:
                    cobbleDouble(region, dstRaster);
                    break;
                  default:
                    break;
                }
            } else {
                SampleModel sm = sampleModel;
                if(sm.getWidth() != region.width ||
                   sm.getHeight() != region.height) {
                    sm = sm.createCompatibleSampleModel(region.width,
                                                        region.height);
                }

                try {
                    dstRaster = createWritableRaster(sm,
                                                     region.getLocation());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        JaiI18N.getString("PlanarImage2"));
                }

                for (int j = startTileY; j <= endTileY; j++) {
                    for (int i = startTileX; i <= endTileX; i++) {
                        Raster tile = getTile(i, j);

                        Rectangle subRegion = region.intersection(
                                              tile.getBounds());
                        Raster subRaster =
                            tile.createChild(subRegion.x,
                                             subRegion.y,
                                             subRegion.width,
                                             subRegion.height,
                                             subRegion.x,
                                             subRegion.y,
                                             null);

			if (sm instanceof ComponentSampleModel &&
                            isBandChild) {
                            // Need to handle this case specially, since
                            // setDataElements will not copy band child images
                            switch (sm.getDataType()) {
                              case DataBuffer.TYPE_FLOAT:
                                dstRaster.setPixels(
                                  subRegion.x,
                                  subRegion.y,
                                  subRegion.width,
                                  subRegion.height,
                                  subRaster.getPixels(
                                    subRegion.x,
                                    subRegion.y,
                                    subRegion.width,
                                    subRegion.height,
                                    new float[nbands*subRegion.width*subRegion.height]));
                                break;
                              case DataBuffer.TYPE_DOUBLE:
                                dstRaster.setPixels(
                                  subRegion.x,
                                  subRegion.y,
                                  subRegion.width,
                                  subRegion.height,
                                  subRaster.getPixels(
                                    subRegion.x,
                                    subRegion.y,
                                    subRegion.width,
                                    subRegion.height,
                                    new double[nbands*subRegion.width*subRegion.height]));
                                break;
                              default:
                                dstRaster.setPixels(
                                  subRegion.x,
                                  subRegion.y,
                                  subRegion.width,
                                  subRegion.height,
                                  subRaster.getPixels(
                                    subRegion.x,
                                    subRegion.y,
                                    subRegion.width,
                                    subRegion.height,
                                    new int[nbands*subRegion.width*subRegion.height]));
                                break;
                            }
                        } else {
                            JDKWorkarounds.setRect(dstRaster, subRaster);
                        }

                    }
                }
            }

            return dstRaster;
        }
    }

    /** Copies the entire image into a single raster. */
    public WritableRaster copyData() {
        return copyData(null);
    }

    /**
     * Copies an arbitrary rectangular region of this image's pixel
     * data into a caller-supplied <code>WritableRaster</code>.
     * The region to be copied is defined as the boundary of the
     * <code>WritableRaster</code>, which can be obtained by calling
     * <code>WritableRaster.getBounds()</code>.
     *
     * <p>The supplied <code>WritableRaster</code> may have a region
     * that extends beyond this image's boundary, in which case only
     * pixels in the part of the region that intersects this image
     * are copied. The areas outside of this image's boundary are left
     * untouched.
     *
     * <p>The supplied <code>WritableRaster</code> may also be
     * <code>null</code>, in which case the entire image is copied
     * into a newly-created <code>WritableRaster</code> with a
     * <code>SampleModel</code> that is compatible with that of
     * this image.
     *
     * @param raster  A <code>WritableRaster</code> to hold the copied
     *        pixel data of this image.
     *
     * @return A reference to the supplied <code>WritableRaster</code>,
     *         or to a new <code>WritableRaster</code> if the supplied
     *         one was <code>null</code>.
     */
    public WritableRaster copyData(WritableRaster raster) {
        Rectangle region;               // the region to be copied
        if (raster == null) {           // copy the entire image
            region = getBounds();

            SampleModel sm = getSampleModel();
            if(sm.getWidth() != region.width ||
               sm.getHeight() != region.height) {
                sm = sm.createCompatibleSampleModel(region.width,
                                                    region.height);
            }
            raster = createWritableRaster(sm, region.getLocation());
        } else {
            region = raster.getBounds().intersection(getBounds());

            if (region.isEmpty()) {     // Raster is outside of image's boundary
                return raster;
            }
        }

        int startTileX = XToTileX(region.x);
        int startTileY = YToTileY(region.y);
        int endTileX = XToTileX(region.x + region.width - 1);
        int endTileY = YToTileY(region.y + region.height - 1);

        SampleModel[] sampleModels = { getSampleModel() };
        int tagID = RasterAccessor.findCompatibleTag(sampleModels,
                                                     raster.getSampleModel());

        RasterFormatTag srcTag = new RasterFormatTag(getSampleModel(),tagID);
        RasterFormatTag dstTag =
            new RasterFormatTag(raster.getSampleModel(),tagID);

        for (int ty = startTileY; ty <= endTileY; ty++) {
            for (int tx = startTileX; tx <= endTileX; tx++) {
                Raster tile = getTile(tx, ty);
                Rectangle subRegion = region.intersection(tile.getBounds());

                RasterAccessor s = new RasterAccessor(tile, subRegion,
                                                      srcTag, getColorModel());
                RasterAccessor d = new RasterAccessor(raster, subRegion,
                                                      dstTag, null);

                if (getSampleModel() instanceof ComponentSampleModel &&
                    raster.getSampleModel() instanceof ComponentSampleModel) {
                    ComponentSampleModel ssm = (ComponentSampleModel) getSampleModel();

                    if (ssm.getPixelStride() == ssm.getNumBands() &&
                        getSampleModel().getNumBands() == raster.getSampleModel().getNumBands())
                        fastCopyRaster(s, d);
                    else
                        ImageUtil.copyRaster(s, d);
                } else
                    ImageUtil.copyRaster(s, d);
            }
        }
        return raster;
    }

    private static final void fastCopyRaster(RasterAccessor src,
                                        RasterAccessor dst) {
        int srcPixelStride = src.getPixelStride();
        int srcLineStride = src.getScanlineStride();
        int[] srcBandOffsets = src.getBandOffsets();

        int dstPixelStride = dst.getPixelStride();
        int dstLineStride = dst.getScanlineStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        int width = dst.getWidth() * dstPixelStride;
        int height = dst.getHeight() * dstLineStride;

        int dataType = src.getDataType();

        final Object s, d;

        if (dataType == DataBuffer.TYPE_BYTE) {
            s = src.getByteDataArray(0);
            d = dst.getByteDataArray(0);
        } else if (dataType == DataBuffer.TYPE_SHORT ||
                   dataType == DataBuffer.TYPE_USHORT) {
            s = src.getShortDataArray(0);
            d = dst.getShortDataArray(0);
        } else if (dataType == DataBuffer.TYPE_INT) {
            s = src.getIntDataArray(0);
            d = dst.getIntDataArray(0);
        } else if (dataType == DataBuffer.TYPE_FLOAT) {
            s = src.getFloatDataArray(0);
            d = dst.getFloatDataArray(0);
        } else if (dataType == DataBuffer.TYPE_DOUBLE) {
            s = src.getDoubleDataArray(0);
            d = dst.getDoubleDataArray(0);
        } else
            throw new IllegalArgumentException();

        int srcOffset = Integer.MAX_VALUE;
        for (int offset : srcBandOffsets)
            if (offset < srcOffset)
                srcOffset = offset;
        int dstOffset = Integer.MAX_VALUE;
        for (int offset : dstBandOffsets)
            if (offset < dstOffset)
                dstOffset = offset;

        int heightEnd = dstOffset + height;

        for (int dstLineOffset = dstOffset,
             srcLineOffset = srcOffset;
             dstLineOffset < heightEnd;
             dstLineOffset += dstLineStride,
             srcLineOffset += srcLineStride) {

             System.arraycopy(s, srcLineOffset, d, dstLineOffset, width);
        }
    }

    /**
     * Copies an arbitrary rectangular region of the
     * <code>RenderedImage</code> into a caller-supplied
     * <code>WritableRaster</code>.  The portion of the supplied
     * <code>WritableRaster</code> that lies outside of the bounds of
     * the image is computed by calling the given
     * <code>BorderExtender</code>.  The supplied
     * <code>WritableRaster</code> must have a
     * <code>SampleModel</code> that is compatible with that of the
     * image.
     *
     * @param dest a <code>WritableRaster</code> to hold the returned
     * portion of the image.
     * @param extender an instance of <code>BorderExtender</code>.
     *
     * @throws IllegalArgumentException  If <code>dest</code> or
     *         <code>extender</code> is <code>null</code>.
     */
    public void copyExtendedData(WritableRaster dest,
                                 BorderExtender extender) {
        if(dest == null || extender == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // If the Raster is within the image just copy directly.
        Rectangle destBounds = dest.getBounds();
        Rectangle imageBounds = getBounds();
        if(imageBounds.contains(destBounds)) {
            copyData(dest);
            return;
        }

        // Get the intersection of the Raster and image bounds.
        Rectangle isect = imageBounds.intersection(destBounds);

        if(!isect.isEmpty()) {
            // Copy image data into the dest Raster.
            WritableRaster isectRaster = dest.createWritableChild(
                                             isect.x, isect.y,
                                             isect.width, isect.height,
                                             isect.x, isect.y,
                                             null);
            copyData(isectRaster);
        }

        // Extend the Raster.
        extender.extend(dest, this);
    }

    /**
     * Returns a copy of an arbitrary rectangular region of this image
     * in a <code>Raster</code>.  The portion of the rectangle of
     * interest ouside the bounds of the image will be computed by
     * calling the given <code>BorderExtender</code>.  If the region
     * falls entirely within the image, <code>extender</code> will not
     * be used in any way.  Thus it is possible to use a
     * <code>null</code> value for <code>extender</code> when it is
     * known that no actual extension will be required.
     *
     * <p> The returned <code>Raster</code> should be considered
     * non-writable; any attempt to alter its pixel data (such as by
     * casting it to a <code>WritableRaster</code> or obtaining and
     * modifying its <code>DataBuffer</code>) may result in undefined
     * behavior. The <code>copyExtendedData</code> method should be
     * used if the returned <code>Raster</code> is to be modified.
     *
     * @param region the region of the image to be returned.
     * @param extender an instance of <code>BorderExtender</code>,
     *        used only if the region exceeds the image bounds,
     *        or <code>null</code>.
     * @return a <code>Raster</code> containing the extended data.
     *
     * @throws IllegalArgumentException  If <code>region</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException  If the region exceeds the image
     *         bounds and <code>extender</code> is <code>null</code>.
     */
    public Raster getExtendedData(Rectangle region,
                                  BorderExtender extender) {
        if(region == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (getBounds().contains(region)) {
            return getData(region);
        }

        if(extender == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Create a WritableRaster of the desired size
        SampleModel destSM = getSampleModel();
        if(destSM.getWidth() != region.width ||
           destSM.getHeight() != region.height) {
            destSM = destSM.createCompatibleSampleModel(region.width,
                                                        region.height);
        }

        // Translate it
        WritableRaster dest = createWritableRaster(destSM,
                                                   region.getLocation());

        copyExtendedData(dest, extender);
        return dest;
    }

    /**
     * Returns a copy of this image as a <code>BufferedImage</code>.
     * A subarea of the image may be copied by supplying a
     * <code>Rectangle</code> parameter; if it is set to
     * <code>null</code>, the entire image is copied.  The supplied
     * Rectangle will be clipped to the image bounds.  The image's
     * <code>ColorModel</code> may be overridden by supplying a
     * non-<code>null</code> second argument.  The resulting
     * <code>ColorModel</code> must be non-<code>null</code> and
     * appropriate for the image's <code>SampleModel</code>.
     *
     * <p> The resulting <code>BufferedImage</code> will contain the
     * full requested area, but will always have its top-left corner
     * translated (0, 0) as required by the <code>BufferedImage</code>
     * interface.
     *
     * @param rect  The <code>Rectangle</code> of the image to be
     *              copied, or <code>null</code> to indicate that the
     *              entire image is to be copied.
     *
     * @param cm  A <code>ColorModel</code> used to override
     *        this image's <code>ColorModel</code>, or <code>null</code>.
     *        The caller is responsible for supplying a
     *        <code>ColorModel</code> that is compatible with the image's
     *        <code>SampleModel</code>.
     *
     * @throws IllegalArgumentException  If an incompatible, non-null
     *         <code>ColorModel</code> is supplied.
     * @throws IllegalArgumentException  If no <code>ColorModel</code> is
     *         supplied, and the image <code>ColorModel</code> is
     *         <code>null</code>.
     */
    public BufferedImage getAsBufferedImage(Rectangle rect,
                                            ColorModel cm) {
        if (cm == null) {
            cm = getColorModel();
            if(cm == null) {
                throw new IllegalArgumentException(
                    JaiI18N.getString("PlanarImage6"));
            }
        }

        if (!JDKWorkarounds.areCompatibleDataModels(sampleModel, cm)) {
            throw new IllegalArgumentException(
                JaiI18N.getString("PlanarImage3"));
        }

        if (rect == null) {
            rect = getBounds();
        } else {
            rect = getBounds().intersection(rect);
        }


        SampleModel sm =
            sampleModel.getWidth() != rect.width ||
            sampleModel.getHeight() != rect.height ?
            sampleModel.createCompatibleSampleModel(rect.width,
                                                    rect.height) :
            sampleModel;

	WritableRaster ras = createWritableRaster(sm, rect.getLocation());
	copyData(ras);

	if (rect.x != 0 || rect.y != 0) {
	    // Move Raster to (0, 0)
	    ras = RasterFactory.createWritableChild(ras, rect.x, rect.y,
						    rect.width, rect.height,
						    0, 0, null);
	}

        return new BufferedImage(cm, ras, cm.isAlphaPremultiplied(), null);
    }

    /**
     * Returns a copy of the entire image as a
     * <code>BufferedImage</code>.  The image's
     * <code>ColorModel</code> must be non-<code>null</code>, and
     * appropriate for the image's <code>SampleModel</code>.
     *
     * @see java.awt.image.BufferedImage
     */
    public BufferedImage getAsBufferedImage() {
        return getAsBufferedImage(null, null);
    }

    /**
     * Returns a <code>Graphics</code> object that may be used to draw
     * into this image.  By default, an
     * <code>IllegalAccessError</code> is thrown.  Subclasses that
     * support such drawing, such as <code>TiledImage</code>, may
     * override this method to return a suitable <code>Graphics</code>
     * object.
     */
    public Graphics getGraphics() {
       throw new IllegalAccessError(JaiI18N.getString("PlanarImage1"));
    }

    /**
     * Returns tile (<code>tileX</code>, <code>tileY</code>) as a
     * <code>Raster</code>.  Note that <code>tileX</code> and
     * <code>tileY</code> are indices into the tile array, not pixel
     * locations.
     *
     * <p> Subclasses must override this method to return a
     * non-<code>null</code> value for all tile indices between
     * <code>getMinTile{X,Y}</code> and <code>getMaxTile{X,Y}</code>,
     * inclusive.  Tile indices outside of this region should result
     * in a return value of <code>null</code>.
     *
     * @param tileX  The X index of the requested tile in the tile array.
     * @param tileY  The Y index of the requested tile in the tile array.
     */
    public abstract Raster getTile(int tileX, int tileY);

    /**
     * Returns the <code>Raster</code>s indicated by the
     * <code>tileIndices</code> array.  This call allows certain
     * <code>PlanarImage</code> subclasses such as
     * <code>OpImage</code> to take advantage of the knowledge that
     * multiple tiles are requested at once.
     *
     * @param tileIndices  An array of Points representing tile indices.
     *
     * @return An array of <code>Raster</code> containing the tiles
     *         corresponding to the given tile indices.
     *
     * @throws IllegalArgumentException  If <code>tileIndices</code> is
     *         <code>null</code>.
     */
    public Raster[] getTiles(Point[] tileIndices) {
        if(tileIndices == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        int size = tileIndices.length;
        Raster tiles[] = new Raster[size];

        for (int i = 0; i < tileIndices.length; i++) {
            Point p = tileIndices[i];
            tiles[i] = getTile(p.x,p.y);
        }

        return tiles;
    }

    /**
     * Computes and returns all tiles in the image.  The tiles are returned
     * in a sequence corresponding to the row-major order of their respective
     * tile indices.  The returned array may of course be ignored, e.g., in
     * the case of a subclass which caches the tiles and the intent is to
     * force their computation.
     */
    public Raster[] getTiles() {
        return getTiles(getTileIndices(getBounds()));
    }

    /**
     * Queues a list of tiles for computation.  Registered listeners
     * will be notified after each tile has been computed.
     *
     * <p> The <code>TileScheduler</code> of the default instance of the
     * <code>JAI</code> class is used to process the tiles.  If this
     * <code>TileScheduler</code> has a positive parallelism this
     * method will be non-blocking.  The event source parameter passed to
     * such listeners will be the <code>TileScheduler</code> and the image
     * parameter will be this image.
     *
     * @param tileIndices A list of tile indices indicating which tiles
     *        to schedule for computation.
     * @throws IllegalArgumentException  If <code>tileIndices</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public TileRequest queueTiles(Point[] tileIndices) {
        if(tileIndices == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        TileComputationListener[] listeners = getTileComputationListeners();
        return JAI.getDefaultInstance().getTileScheduler().scheduleTiles(this,
                                                                  tileIndices,
                                                                  listeners);
    }

    /**
     * Issue an advisory cancellation request to nullify processing of
     * the indicated tiles.  It is legal to implement this method as a no-op.
     *
     * <p> The cancellation request is forwarded to the
     * <code>TileScheduler</code> of the default instance of the
     * <code>JAI</code> class.
     *
     * @param request The request for which tiles are to be cancelled.
     * @param tileIndices The tiles to be cancelled; may be <code>null</code>.
     *        Any tiles not actually in the <code>TileRequest</code> will be
     *        ignored.
     * @throws IllegalArgumentException  If <code>request</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public void cancelTiles(TileRequest request, Point[] tileIndices) {
        if (request == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("Generic4"));
        }

        JAI.getDefaultInstance().getTileScheduler().cancelTiles(request,
                                                                tileIndices);
    }

    /**
     * Hints that the given tiles might be needed in the near future.
     * Some implementations may spawn a thread or threads
     * to compute the tiles while others may ignore the hint.
     *
     * <p> The <code>TileScheduler</code> of the default instance of the
     * <code>JAI</code> class is used to prefetch the tiles.  If this
     * <code>TileScheduler</code> has a positive prefetch parallelism
     * this method will be non-blocking.
     *
     * @param tileIndices A list of tile indices indicating which tiles
     *        to prefetch.
     *
     * @throws IllegalArgumentException  If <code>tileIndices</code> is
     *         <code>null</code>.
     */
    public void prefetchTiles(Point[] tileIndices) {
        if(tileIndices == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        JAI.getDefaultInstance().getTileScheduler().prefetchTiles(this,
                                                                  tileIndices);
    }

    /**
     * Provides a hint that an image will no longer be accessed from a
     * reference in user space.  The results are equivalent to those
     * that occur when the program loses its last reference to this
     * image, the garbage collector discovers this, and finalize is
     * called.  This can be used as a hint in situations where waiting
     * for garbage collection would be overly conservative.
     *
     * <p> <code>PlanarImage</code> defines this method to remove the
     * image being disposed from the list of sinks in all of its
     * source images.  Subclasses should call
     * <code>super.dispose()</code> in their <code>dispose</code>
     * methods, if any.
     *
     * <p> The results of referencing an image after a call to
     * <code>dispose()</code> are undefined.
     */
    public synchronized void dispose() {
        // Do nothing if dispose() has been called previously
        if (disposed) {
            return;
        }
        disposed = true;

        // Retrieve the sources as a Vector rather than using getSource()
        // to enable compatibility with subclasses which may have sources
        // which are not PlanarImages, e.g., as in RenderedOp.
        Vector srcs = getSources();
        if(srcs != null) {
            int numSources = srcs.size();
            for (int i = 0; i < numSources; i++) {
                Object src = srcs.get(i);
                if(src instanceof PlanarImage) {
                    ((PlanarImage)src).removeSink(this);
                }
            }
        }
    }

    /**
     * Performs cleanup prior to garbage collection.
     *
     * <p> <code>PlanarImage</code> defines this method to invoke
     * the <code>dispose()</code> method.</p>
     *
     * @exception <code>Throwable</code> if an error occurs in the
     *            garbage collector.
     */
    /* protected void finalize() throws Throwable {
        dispose();
    } */

    /** For debugging. */
    private void printBounds() {
        System.out.println("Bounds: [x=" + getMinX() +
    		           ", y=" + getMinY() +
  		           ", width=" + getWidth() +
  		           ", height=" + getHeight() +
  		           "]");
    }

    /** For debugging. */
    private void printTile(int i, int j) {
        int xmin = i*getTileWidth() + getTileGridXOffset();
        int ymin = j*getTileHeight() + getTileGridYOffset();

        Rectangle imageBounds = getBounds();
        Rectangle tileBounds = new Rectangle(xmin, ymin,
  					     getTileWidth(),
  					     getTileHeight());
        tileBounds = tileBounds.intersection(imageBounds);

        Raster tile = getTile(i, j);

        Rectangle realTileBounds = new Rectangle(tile.getMinX(),
  					         tile.getMinY(),
  					         tile.getWidth(),
  					         tile.getHeight());
        System.out.println("Tile bounds (actual)   = " + realTileBounds);
        System.out.println("Tile bounds (computed) = " + tileBounds);

        xmin = tileBounds.x;
        ymin = tileBounds.y;
        int xmax = tileBounds.x + tileBounds.width - 1;
        int ymax = tileBounds.y + tileBounds.height - 1;
        int numBands = getSampleModel().getNumBands();
        int[] val = new int[numBands];
        int pi, pj;

        for (pj = ymin; pj <= ymax; pj++) {
            for (pi = xmin; pi <= xmax; pi++) {
  	        tile.getPixel(pi, pj, val);
  	        if (numBands == 1) {
  	            System.out.print("(" + val[0] + ") ");
  	        } else if (numBands == 3) {
  	            System.out.print("(" + val[0] + "," + val[1] +
                                     "," + val[2] + ") ");
  	        }
            }
            System.out.println();
        }
    }

    /**
     * Returns a <code>String</code> which includes the basic information
     * of this image.
     *
     * @since JAI 1.1
     */
    public String toString() {
        return "PlanarImage[" +
               "minX=" + minX + " minY=" + minY +
               " width=" + width + " height=" + height +
               " tileGridXOffset=" + tileGridXOffset +
               " tileGridYOffset=" + tileGridYOffset +
               " tileWidth=" + tileWidth + " tileHeight=" + tileHeight +
               " sampleModel=" + sampleModel +
               " colorModel=" + colorModel +
               "]";
    }

    private void cobbleByte(Rectangle bounds,
                            Raster dstRaster) {

        ComponentSampleModel dstSM =
            (ComponentSampleModel)dstRaster.getSampleModel();

        int startX = XToTileX(bounds.x);
        int startY = YToTileY(bounds.y);
        int rectXend = bounds.x + bounds.width - 1;
        int rectYend = bounds.y + bounds.height - 1;
        int endX = XToTileX(rectXend);
        int endY = YToTileY(rectYend);

        //
        //  Get parameters of destination raster
        //
        DataBufferByte dstDB = (DataBufferByte)dstRaster.getDataBuffer();
        byte[] dst           = dstDB.getData();
        int dstPS            = dstSM.getPixelStride();
        int dstSS            = dstSM.getScanlineStride();

        boolean tileParamsSet = false;
        ComponentSampleModel srcSM = null;
        int srcPS=0, srcSS=0;
        int xOrg, yOrg;
        int srcX1, srcY1, srcX2, srcY2, srcW, srcH;

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                Raster tile = getTile(x, y);
                if (tile == null) {
                    //
                    // Out-of-bounds tile. Zero fill will be supplied
                    // since dstRaster is initialized to zero
                    //
                    continue;
                }

                if (! tileParamsSet) {
                    //
                    // These are constant for all tiles,
                    // so only set them once.
                    //
                    srcSM = (ComponentSampleModel)tile.getSampleModel();
                    srcPS = srcSM.getPixelStride();
                    srcSS = srcSM.getScanlineStride();
                    tileParamsSet = true;
                }

                //
                //  Intersect the tile and the rectangle
                //  Avoid use of Math.min/max
                //
                yOrg  = y*tileHeight + tileGridYOffset;
                srcY1 = yOrg;
                srcY2 = srcY1 + tileHeight - 1;
                if (bounds.y > srcY1) srcY1 = bounds.y;
                if (rectYend < srcY2) srcY2 = rectYend;
                srcH = srcY2 - srcY1 + 1;

                xOrg  = x*tileWidth + tileGridXOffset;
                srcX1 = xOrg;
                srcX2 = srcX1 + tileWidth - 1;
                if (bounds.x > srcX1) srcX1 = bounds.x;
                if (rectXend < srcX2) srcX2 = rectXend;
                srcW = srcX2 - srcX1 + 1;

                int dstX = srcX1 - bounds.x;
                int dstY = srcY1 - bounds.y;

                // Get the actual data array
                DataBufferByte srcDB = (DataBufferByte)tile.getDataBuffer();
                byte[] src = srcDB.getData();

                int nsamps = srcW * srcPS;
                boolean useArrayCopy = (nsamps >= MIN_ARRAYCOPY_SIZE);

                int ySrcIdx = (srcY1 - yOrg)*srcSS + (srcX1 - xOrg)*srcPS;
                int yDstIdx = dstY*dstSS + dstX*dstPS;
                if (useArrayCopy) {
                    for (int row = 0; row < srcH; row++) {
                        System.arraycopy(src, ySrcIdx, dst, yDstIdx, nsamps);
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                } else {
                    for (int row = 0; row < srcH; row++) {
                        int xSrcIdx = ySrcIdx;
                        int xDstIdx = yDstIdx;
                        int xEnd = xDstIdx + nsamps;
                        while (xDstIdx < xEnd) {
                            dst[xDstIdx++] = src[xSrcIdx++];
                        }
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                }
            }
        }
    }

    private void cobbleShort(Rectangle bounds,
                            Raster dstRaster) {

        ComponentSampleModel dstSM =
            (ComponentSampleModel)dstRaster.getSampleModel();

        int startX = XToTileX(bounds.x);
        int startY = YToTileY(bounds.y);
        int rectXend = bounds.x + bounds.width - 1;
        int rectYend = bounds.y + bounds.height - 1;
        int endX = XToTileX(rectXend);
        int endY = YToTileY(rectYend);

        //
        //  Get parameters of destination raster
        //
        DataBufferShort dstDB = (DataBufferShort)dstRaster.getDataBuffer();
        short[] dst           = dstDB.getData();
        int dstPS            = dstSM.getPixelStride();
        int dstSS            = dstSM.getScanlineStride();

        boolean tileParamsSet = false;
        ComponentSampleModel srcSM = null;
        int srcPS=0, srcSS=0;
        int xOrg, yOrg;
        int srcX1, srcY1, srcX2, srcY2, srcW, srcH;

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                Raster tile = getTile(x, y);
                if (tile == null) {
                    //
                    // Out-of-bounds tile. Zero fill will be supplied
                    // since dstRaster is initialized to zero
                    //
                    continue;
                }

                if (! tileParamsSet) {
                    //
                    // These are constant for all tiles,
                    // so only set them once.
                    //
                    srcSM = (ComponentSampleModel)tile.getSampleModel();
                    srcPS = srcSM.getPixelStride();
                    srcSS = srcSM.getScanlineStride();
                    tileParamsSet = true;
                }

                //
                //  Intersect the tile and the rectangle
                //  Avoid use of Math.min/max
                //
                yOrg  = y*tileHeight + tileGridYOffset;
                srcY1 = yOrg;
                srcY2 = srcY1 + tileHeight - 1;
                if (bounds.y > srcY1) srcY1 = bounds.y;
                if (rectYend < srcY2) srcY2 = rectYend;
                srcH = srcY2 - srcY1 + 1;

                xOrg  = x*tileWidth + tileGridXOffset;
                srcX1 = xOrg;
                srcX2 = srcX1 + tileWidth - 1;
                if (bounds.x > srcX1) srcX1 = bounds.x;
                if (rectXend < srcX2) srcX2 = rectXend;
                srcW = srcX2 - srcX1 + 1;

                int dstX = srcX1 - bounds.x;
                int dstY = srcY1 - bounds.y;

                // Get the actual data array
                DataBufferShort srcDB = (DataBufferShort)tile.getDataBuffer();
                short[] src = srcDB.getData();

                int nsamps = srcW * srcPS;
                boolean useArrayCopy = (nsamps >= MIN_ARRAYCOPY_SIZE);

                int ySrcIdx = (srcY1 - yOrg)*srcSS + (srcX1 - xOrg)*srcPS;
                int yDstIdx = dstY*dstSS + dstX*dstPS;
                if (useArrayCopy) {
                    for (int row = 0; row < srcH; row++) {
                        System.arraycopy(src, ySrcIdx, dst, yDstIdx, nsamps);
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                } else {
                    for (int row = 0; row < srcH; row++) {
                        int xSrcIdx = ySrcIdx;
                        int xDstIdx = yDstIdx;
                        int xEnd = xDstIdx + nsamps;
                        while (xDstIdx < xEnd) {
                            dst[xDstIdx++] = src[xSrcIdx++];
                        }
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                }
            }
        }
    }

    private void cobbleUShort(Rectangle bounds,
                            Raster dstRaster) {

        ComponentSampleModel dstSM =
            (ComponentSampleModel)dstRaster.getSampleModel();

        int startX = XToTileX(bounds.x);
        int startY = YToTileY(bounds.y);
        int rectXend = bounds.x + bounds.width - 1;
        int rectYend = bounds.y + bounds.height - 1;
        int endX = XToTileX(rectXend);
        int endY = YToTileY(rectYend);

        //
        //  Get parameters of destination raster
        //
        DataBufferUShort dstDB = (DataBufferUShort)dstRaster.getDataBuffer();
        short[] dst           = dstDB.getData();
        int dstPS            = dstSM.getPixelStride();
        int dstSS            = dstSM.getScanlineStride();

        boolean tileParamsSet = false;
        ComponentSampleModel srcSM = null;
        int srcPS=0, srcSS=0;
        int xOrg, yOrg;
        int srcX1, srcY1, srcX2, srcY2, srcW, srcH;

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                Raster tile = getTile(x, y);
                if (tile == null) {
                    //
                    // Out-of-bounds tile. Zero fill will be supplied
                    // since dstRaster is initialized to zero
                    //
                    continue;
                }

                if (! tileParamsSet) {
                    //
                    // These are constant for all tiles,
                    // so only set them once.
                    //
                    srcSM = (ComponentSampleModel)tile.getSampleModel();
                    srcPS = srcSM.getPixelStride();
                    srcSS = srcSM.getScanlineStride();
                    tileParamsSet = true;
                }

                //
                //  Intersect the tile and the rectangle
                //  Avoid use of Math.min/max
                //
                yOrg  = y*tileHeight + tileGridYOffset;
                srcY1 = yOrg;
                srcY2 = srcY1 + tileHeight - 1;
                if (bounds.y > srcY1) srcY1 = bounds.y;
                if (rectYend < srcY2) srcY2 = rectYend;
                srcH = srcY2 - srcY1 + 1;

                xOrg  = x*tileWidth + tileGridXOffset;
                srcX1 = xOrg;
                srcX2 = srcX1 + tileWidth - 1;
                if (bounds.x > srcX1) srcX1 = bounds.x;
                if (rectXend < srcX2) srcX2 = rectXend;
                srcW = srcX2 - srcX1 + 1;

                int dstX = srcX1 - bounds.x;
                int dstY = srcY1 - bounds.y;

                // Get the actual data array
                DataBufferUShort srcDB = (DataBufferUShort)tile.getDataBuffer();
                short[] src = srcDB.getData();

                int nsamps = srcW * srcPS;
                boolean useArrayCopy = (nsamps >= MIN_ARRAYCOPY_SIZE);

                int ySrcIdx = (srcY1 - yOrg)*srcSS + (srcX1 - xOrg)*srcPS;
                int yDstIdx = dstY*dstSS + dstX*dstPS;
                if (useArrayCopy) {
                    for (int row = 0; row < srcH; row++) {
                        System.arraycopy(src, ySrcIdx, dst, yDstIdx, nsamps);
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                } else {
                    for (int row = 0; row < srcH; row++) {
                        int xSrcIdx = ySrcIdx;
                        int xDstIdx = yDstIdx;
                        int xEnd = xDstIdx + nsamps;
                        while (xDstIdx < xEnd) {
                            dst[xDstIdx++] = src[xSrcIdx++];
                        }
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                }
            }
        }
    }

    private void cobbleInt(Rectangle bounds,
                            Raster dstRaster) {

        ComponentSampleModel dstSM =
            (ComponentSampleModel)dstRaster.getSampleModel();

        int startX = XToTileX(bounds.x);
        int startY = YToTileY(bounds.y);
        int rectXend = bounds.x + bounds.width - 1;
        int rectYend = bounds.y + bounds.height - 1;
        int endX = XToTileX(rectXend);
        int endY = YToTileY(rectYend);

        //
        //  Get parameters of destination raster
        //
        DataBufferInt dstDB = (DataBufferInt)dstRaster.getDataBuffer();
        int[] dst           = dstDB.getData();
        int dstPS            = dstSM.getPixelStride();
        int dstSS            = dstSM.getScanlineStride();

        boolean tileParamsSet = false;
        ComponentSampleModel srcSM = null;
        int srcPS=0, srcSS=0;
        int xOrg, yOrg;
        int srcX1, srcY1, srcX2, srcY2, srcW, srcH;

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                Raster tile = getTile(x, y);
                if (tile == null) {
                    //
                    // Out-of-bounds tile. Zero fill will be supplied
                    // since dstRaster is initialized to zero
                    //
                    continue;
                }

                if (! tileParamsSet) {
                    //
                    // These are constant for all tiles,
                    // so only set them once.
                    //
                    srcSM = (ComponentSampleModel)tile.getSampleModel();
                    srcPS = srcSM.getPixelStride();
                    srcSS = srcSM.getScanlineStride();
                    tileParamsSet = true;
                }

                //
                //  Intersect the tile and the rectangle
                //  Avoid use of Math.min/max
                //
                yOrg  = y*tileHeight + tileGridYOffset;
                srcY1 = yOrg;
                srcY2 = srcY1 + tileHeight - 1;
                if (bounds.y > srcY1) srcY1 = bounds.y;
                if (rectYend < srcY2) srcY2 = rectYend;
                srcH = srcY2 - srcY1 + 1;

                xOrg  = x*tileWidth + tileGridXOffset;
                srcX1 = xOrg;
                srcX2 = srcX1 + tileWidth - 1;
                if (bounds.x > srcX1) srcX1 = bounds.x;
                if (rectXend < srcX2) srcX2 = rectXend;
                srcW = srcX2 - srcX1 + 1;

                int dstX = srcX1 - bounds.x;
                int dstY = srcY1 - bounds.y;

                // Get the actual data array
                DataBufferInt srcDB = (DataBufferInt)tile.getDataBuffer();
                int[] src = srcDB.getData();

                int nsamps = srcW * srcPS;
                boolean useArrayCopy = (nsamps >= MIN_ARRAYCOPY_SIZE);

                int ySrcIdx = (srcY1 - yOrg)*srcSS + (srcX1 - xOrg)*srcPS;
                int yDstIdx = dstY*dstSS + dstX*dstPS;
                if (useArrayCopy) {
                    for (int row = 0; row < srcH; row++) {
                        System.arraycopy(src, ySrcIdx, dst, yDstIdx, nsamps);
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                } else {
                    for (int row = 0; row < srcH; row++) {
                        int xSrcIdx = ySrcIdx;
                        int xDstIdx = yDstIdx;
                        int xEnd = xDstIdx + nsamps;
                        while (xDstIdx < xEnd) {
                            dst[xDstIdx++] = src[xSrcIdx++];
                        }
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                }
            }
        }
    }

    private void cobbleFloat(Rectangle bounds,
                            Raster dstRaster) {

        ComponentSampleModel dstSM =
            (ComponentSampleModel)dstRaster.getSampleModel();

        int startX = XToTileX(bounds.x);
        int startY = YToTileY(bounds.y);
        int rectXend = bounds.x + bounds.width - 1;
        int rectYend = bounds.y + bounds.height - 1;
        int endX = XToTileX(rectXend);
        int endY = YToTileY(rectYend);

        //
        //  Get parameters of destination raster
        //
        DataBuffer dstDB = dstRaster.getDataBuffer();
        float[] dst           = DataBufferUtils.getDataFloat(dstDB);
        int dstPS            = dstSM.getPixelStride();
        int dstSS            = dstSM.getScanlineStride();

        boolean tileParamsSet = false;
        ComponentSampleModel srcSM = null;
        int srcPS=0, srcSS=0;
        int xOrg, yOrg;
        int srcX1, srcY1, srcX2, srcY2, srcW, srcH;

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                Raster tile = getTile(x, y);
                if (tile == null) {
                    //
                    // Out-of-bounds tile. Zero fill will be supplied
                    // since dstRaster is initialized to zero
                    //
                    continue;
                }

                if (! tileParamsSet) {
                    //
                    // These are constant for all tiles,
                    // so only set them once.
                    //
                    srcSM = (ComponentSampleModel)tile.getSampleModel();
                    srcPS = srcSM.getPixelStride();
                    srcSS = srcSM.getScanlineStride();
                    tileParamsSet = true;
                }

                //
                //  Intersect the tile and the rectangle
                //  Avoid use of Math.min/max
                //
                yOrg  = y*tileHeight + tileGridYOffset;
                srcY1 = yOrg;
                srcY2 = srcY1 + tileHeight - 1;
                if (bounds.y > srcY1) srcY1 = bounds.y;
                if (rectYend < srcY2) srcY2 = rectYend;
                srcH = srcY2 - srcY1 + 1;

                xOrg  = x*tileWidth + tileGridXOffset;
                srcX1 = xOrg;
                srcX2 = srcX1 + tileWidth - 1;
                if (bounds.x > srcX1) srcX1 = bounds.x;
                if (rectXend < srcX2) srcX2 = rectXend;
                srcW = srcX2 - srcX1 + 1;

                int dstX = srcX1 - bounds.x;
                int dstY = srcY1 - bounds.y;

                // Get the actual data array
                DataBuffer srcDB = tile.getDataBuffer();
                float[] src = DataBufferUtils.getDataFloat(srcDB);

                int nsamps = srcW * srcPS;
                boolean useArrayCopy = (nsamps >= MIN_ARRAYCOPY_SIZE);

                int ySrcIdx = (srcY1 - yOrg)*srcSS + (srcX1 - xOrg)*srcPS;
                int yDstIdx = dstY*dstSS + dstX*dstPS;
                if (useArrayCopy) {
                    for (int row = 0; row < srcH; row++) {
                        System.arraycopy(src, ySrcIdx, dst, yDstIdx, nsamps);
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                } else {
                    for (int row = 0; row < srcH; row++) {
                        int xSrcIdx = ySrcIdx;
                        int xDstIdx = yDstIdx;
                        int xEnd = xDstIdx + nsamps;
                        while (xDstIdx < xEnd) {
                            dst[xDstIdx++] = src[xSrcIdx++];
                        }
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                }
            }
        }
    }

    private void cobbleDouble(Rectangle bounds,
                            Raster dstRaster) {

        ComponentSampleModel dstSM =
            (ComponentSampleModel)dstRaster.getSampleModel();

        int startX = XToTileX(bounds.x);
        int startY = YToTileY(bounds.y);
        int rectXend = bounds.x + bounds.width - 1;
        int rectYend = bounds.y + bounds.height - 1;
        int endX = XToTileX(rectXend);
        int endY = YToTileY(rectYend);

        //
        //  Get parameters of destination raster
        //
        DataBuffer dstDB = dstRaster.getDataBuffer();
        double[] dst           = DataBufferUtils.getDataDouble(dstDB);
        int dstPS            = dstSM.getPixelStride();
        int dstSS            = dstSM.getScanlineStride();

        boolean tileParamsSet = false;
        ComponentSampleModel srcSM = null;
        int srcPS=0, srcSS=0;
        int xOrg, yOrg;
        int srcX1, srcY1, srcX2, srcY2, srcW, srcH;

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                Raster tile = getTile(x, y);
                if (tile == null) {
                    //
                    // Out-of-bounds tile. Zero fill will be supplied
                    // since dstRaster is initialized to zero
                    //
                    continue;
                }

                if (! tileParamsSet) {
                    //
                    // These are constant for all tiles,
                    // so only set them once.
                    //
                    srcSM = (ComponentSampleModel)tile.getSampleModel();
                    srcPS = srcSM.getPixelStride();
                    srcSS = srcSM.getScanlineStride();
                    tileParamsSet = true;
                }

                //
                //  Intersect the tile and the rectangle
                //  Avoid use of Math.min/max
                //
                yOrg  = y*tileHeight + tileGridYOffset;
                srcY1 = yOrg;
                srcY2 = srcY1 + tileHeight - 1;
                if (bounds.y > srcY1) srcY1 = bounds.y;
                if (rectYend < srcY2) srcY2 = rectYend;
                srcH = srcY2 - srcY1 + 1;

                xOrg  = x*tileWidth + tileGridXOffset;
                srcX1 = xOrg;
                srcX2 = srcX1 + tileWidth - 1;
                if (bounds.x > srcX1) srcX1 = bounds.x;
                if (rectXend < srcX2) srcX2 = rectXend;
                srcW = srcX2 - srcX1 + 1;

                int dstX = srcX1 - bounds.x;
                int dstY = srcY1 - bounds.y;

                // Get the actual data array
                DataBuffer srcDB = tile.getDataBuffer();
                double[] src = DataBufferUtils.getDataDouble(srcDB);

                int nsamps = srcW * srcPS;
                boolean useArrayCopy = (nsamps >= MIN_ARRAYCOPY_SIZE);

                int ySrcIdx = (srcY1 - yOrg)*srcSS + (srcX1 - xOrg)*srcPS;
                int yDstIdx = dstY*dstSS + dstX*dstPS;
                if (useArrayCopy) {
                    for (int row = 0; row < srcH; row++) {
                        System.arraycopy(src, ySrcIdx, dst, yDstIdx, nsamps);
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                } else {
                    for (int row = 0; row < srcH; row++) {
                        int xSrcIdx = ySrcIdx;
                        int xDstIdx = yDstIdx;
                        int xEnd = xDstIdx + nsamps;
                        while (xDstIdx < xEnd) {
                            dst[xDstIdx++] = src[xSrcIdx++];
                        }
                        ySrcIdx += srcSS;
                        yDstIdx += dstSS;
                    }
                }
            }
        }
    }

    /**
     * Returns a unique identifier (UID) for this <code>PlanarImage</code>.
     * This UID may be used when the potential redundancy of the value
     * returned by the <code>hashCode()</code> method is unacceptable.
     * An example of this is in generating a key for storing image tiles
     * in a cache.
     */
    public Object getImageID() {
        return UID;
    }
}
