/*
 * $RCSfile: JAI.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.3 $
 * $Date: 2005/11/23 22:53:09 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import com.lightcrafts.media.jai.util.SunTileCache;
import com.lightcrafts.media.jai.util.SunTileScheduler;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import com.lightcrafts.mediax.jai.remote.NegotiableCapabilitySet;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.ImagingListenerImpl;
import com.lightcrafts.media.jai.util.PropertyUtil;

/**
 * A convenience class for instantiating operations.
 *
 * <p> This class allows programmers to use the syntax:
 *
 * <pre>
 * import com.lightcrafts.mediax.jai.JAI;
 * RenderedOp im = JAI.create("convolve", paramBlock, renderHints);
 * </pre>
 *
 * to create new images or collections by applying operators.
 * The <code>create()</code> method returns a <code>RenderedOp</code>
 * encapsulating the operation name, parameter block, and rendering
 * hints.  Additionally, it performs validity checking on the operation
 * parameters. Programmers may also refer to
 * <code>JAI.createCollection("opname", paramBlock, renderHints)</code>,
 * <code>JAI.createRenderable("opname", paramBlock, renderHints)</code>, and
 * <code>JAI.createRenderableCollection("opname", paramBlock,
 * renderHints)</code>.
 *
 * <p> If the <code>OperationDescriptor</code> associated with the
 * named operation returns <code>true</code> from its
 * <code>isImmediate()</code> method, the <code>JAI.createNS()</code>
 * method will ask the <code>RenderedOp</code> it constructs to render
 * itself immediately.  If this rendering is <code>null</code>,
 * <code>createNS()</code> will itself return <code>null</code>
 * rather than returning an instance of <code>RenderedOp</code> as it
 * normally does.
 *
 * <p> It is possible to create new instances of the<code>JAI</code>class in
 * order to control each instance's registry, tile cache, and tile scheduler
 * individually.  Most users will want to use only the static methods
 * of this class, which perform all operations on a default instance,
 * which in turn makes use of a default registry.  To create a new
 * image or collection on a non-default <code>JAI</code> instance,
 * the <code>createNS()</code> and <code>createCollectionNS</code>
 * (NS being short for "non-static") methods are used.
 *
 * <p> The <code>JAI</code> class contains convenience methods for a
 * number of common argument list formats.  These methods perform the
 * work of constructing a <code>ParameterBlock</code> automatically.
 * The convenience methods are available only in <code>static</code>
 * form and make use of the default instance.  When operating with a
 * specific instance, the general, non-static functions
 * <code>createNS()</code> and <code>createCollectionNS()</code>
 * should be used.  All of the convenience methods operate by calling
 * <code>createNS()</code> on the default <code>JAI</code> instance,
 * and thus inherit the semantics of that method with regard to immediate
 * rendering.
 *
 * <p> The registry being used by a particular instance may be
 * retrieved or set using the <code>getOperationRegistry()</code> and
 * <code>setOperationRegistry()</code> methods, respectively. Only
 * advanced users should attempt to set the registry.  The tile cache and
 * tile scheduler being used by a particular instance may likewise be set
 * or retrieved using the methods <code>setTileCache()</code>,
 * <code>setTileScheduler()</code>, <code>getTileCache()</code>, or
 * <code>getTileScheduler()</code>.
 *
 * <p> Each instance of <code>JAI</code> contains a set of rendering
 * hints which will be used for all image or collection creations.
 * These hints are merged with any hints supplied to the
 * <code>create</code> method; directly supplied hints take precedence
 * over the common hints.  When a new <code>JAI</code> instance is
 * constructed, its hints are initialized to a copy of the hints
 * associated with the default instance.  When the default instance is
 * constructed, hints for the default registry, tile cache, and tile
 * scheduler are added to the set of common rendering hints.  Similarly,
 * invoking <code>setOperationRegistry()</code>, <code>setTileCache()</code>,
 * or <code>setTileScheduler()</code> on a <code>JAI</code> instance will
 * cause the respective entity to be added to the common rendering hints.
 * The hints associated with any instance, including the default instance,
 * may be manipulated using the <code>getRenderingHints()</code>,
 * <code>setRenderingHints()</code>, and <code>clearRenderingHints()</code>
 * methods.
 *
 * <p> An <code>ImagingListener</code> will reside in each instance of
 * <code>JAI</code>.  It can be used to listen to (and
 * process) the exceptional situations that occur in the
 * operations and JAI.  A default <code>ImagingListener</code> is
 * initially registered which re-throws <code>RuntimeException</code>s
 * and prints the error message and
 * the stack trace of other types to <code>System.err</code>.  To override this
 * default behavior an instance of an alternate <code>ImagingListener</code>
 * implementation should be registered using
 * {@link JAI#setImagingListener}.
 *
 * <p> An <code>ImagingListener</code> also can be attached to a node as
 * a rendering hint, which maps the key <code>KEY_IMAGING_LISTENER</code>.
 * The <code>Throwable</code>s which arise in the creation
 * and the rendering of this node will be sent to this
 * <code>ImagingListener</code> (note that those thrown at the top levels
 * such as node creation failure will be handled by the listener registered
 * to the <code>JAI</code> instead.)  The default value for this hint will
 * be the one registered to the instance of <code>JAI</code>.</p>
 *
 * @see OperationRegistry
 * @see RenderingHints
 * @see TileScheduler
 * @see TileCache
 * @see ImagingListener
 */
public final class JAI {

    //
    // Private hint keys. Each of these keys must be assigned a unique value.
    //

    // JAI Core
    private static final int HINT_IMAGE_LAYOUT = 101;
    private static final int HINT_INTERPOLATION = 102;
    private static final int HINT_OPERATION_REGISTRY = 103;
    private static final int HINT_OPERATION_BOUND = 104;
    private static final int HINT_BORDER_EXTENDER = 105;
    private static final int HINT_TILE_CACHE = 106;
    private static final int HINT_TILE_SCHEDULER = 107;
    private static final int HINT_DEFAULT_COLOR_MODEL_ENABLED = 108;
    private static final int HINT_DEFAULT_COLOR_MODEL_METHOD = 109;
    private static final int HINT_TILE_CACHE_METRIC = 110;
    private static final int HINT_SERIALIZE_DEEP_COPY = 111;
    private static final int HINT_TILE_CODEC_FORMAT = 112;
    private static final int HINT_TILE_ENCODING_PARAM = 113;
    private static final int HINT_TILE_DECODING_PARAM = 114;
    private static final int HINT_RETRY_INTERVAL = 115;
    private static final int HINT_NUM_RETRIES = 116;
    private static final int HINT_NEGOTIATION_PREFERENCES = 117;
    private static final int HINT_DEFAULT_RENDERING_SIZE = 118;
    private static final int HINT_COLOR_MODEL_FACTORY = 119;
    private static final int HINT_REPLACE_INDEX_COLOR_MODEL = 120;
    private static final int HINT_TILE_FACTORY = 121;
    private static final int HINT_TILE_RECYCLER = 122;
    private static final int HINT_CACHED_TILE_RECYCLING_ENABLED = 123;
    private static final int HINT_TRANSFORM_ON_COLORMAP = 124;
    private static final int HINT_IMAGING_LISTENER = 125;

    //
    // Public keys
    //

    /**
     * Key for {@link ImageLayout} object values.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     */
    public static RenderingHints.Key KEY_IMAGE_LAYOUT =
    new RenderingKey(HINT_IMAGE_LAYOUT, ImageLayout.class);

    /**
     * Key for {@link Interpolation} object values.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     *
     * @see MultiResolutionRenderableImage#createScaledRendering
     * @see MultiResolutionRenderableImage#createRendering
     */
    public static RenderingHints.Key KEY_INTERPOLATION =
    new RenderingKey(HINT_INTERPOLATION, Interpolation.class);

    /**
     * Key for {@link OperationRegistry} object values.
     * The common <code>RenderingHints</code> by default contain a hint
     * corresponding to this key the value of which is equal to the value
     * returned by <code>getOperationRegistry()</code>.  The hint is
     * automatically set by <code>setOperationRegistry()</code>.
     */
    public static RenderingHints.Key KEY_OPERATION_REGISTRY =
    new RenderingKey(HINT_OPERATION_REGISTRY, OperationRegistry.class);

    /**
     * Key for <code>Integer</code> object values representing whether
     * the operation is compute, network, or I/O bound.  The values
     * come from the constants <code>OpImage.OP_COMPUTE_BOUND</code>,
     * <code>OpImage.OP_IO_BOUND</code>, and
     * <code>OpImage.OP_NETWORK_BOUND</code>.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     */
    public static RenderingHints.Key KEY_OPERATION_BOUND =
    new RenderingKey(HINT_OPERATION_BOUND, Integer.class);

    /**
     * Key for {@link BorderExtender} object values.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     */
    public static RenderingHints.Key KEY_BORDER_EXTENDER =
    new RenderingKey(HINT_BORDER_EXTENDER, BorderExtender.class);

    /**
     * Key for {@link TileCache} object values.
     * The common <code>RenderingHints</code> by default contain a hint
     * corresponding to this key the value of which is equal to the value
     * returned by <code>getTileCache()</code>.  The hint is
     * automatically set by <code>setTileCache()</code>.
     *
     * @see #createTileCache(long)
     * @see #createTileCache()
     *
     * @see OpImage#OpImage
     */
    public static RenderingHints.Key KEY_TILE_CACHE =
    new RenderingKey(HINT_TILE_CACHE, TileCache.class);

    /**
     * Key for Tile ordering metric.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     *
     * @see OpImage#OpImage
     * @see TileCache
     *
     * @since JAI 1.1
     */
    public static RenderingHints.Key KEY_TILE_CACHE_METRIC =
    new RenderingKey(HINT_TILE_CACHE_METRIC, Object.class);

    /**
     * Key for {@link TileScheduler} object values.
     * The common <code>RenderingHints</code> by default contain a hint
     * corresponding to this key the value of which is equal to the value
     * returned by <code>getTileScheduler()</code>.  The hint is
     * automatically set by <code>setTileScheduler()</code>.
     *
     * @see OpImage#OpImage
     *
     * @since JAI 1.1
     */
    public static RenderingHints.Key KEY_TILE_SCHEDULER =
    new RenderingKey(HINT_TILE_SCHEDULER, TileScheduler.class);

    /**
     * Key for enabling default <code>ColorModel</code> initialization
     * when a valid <code>ColorModel</code> may not be derived by
     * inheritance.  The corresponding object must be a <code>Boolean</code>.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key which is equivalent to its being
     * set to <code>TRUE</code>.
     *
     * @see OpImage#OpImage
     *
     * @since JAI 1.1
     */
    public static RenderingHints.Key KEY_DEFAULT_COLOR_MODEL_ENABLED =
    new RenderingKey(HINT_DEFAULT_COLOR_MODEL_ENABLED, Boolean.class);

    /**
     * Key for specifying a method to be used as for default
     * <code>ColorModel</code> initialization.  The corresponding object
     * must be a <code>java.lang.reflect.Method</code> which is static
     * and accepts a single <code>SampleModel</code> parameter and returns a
     * <code>ColorModel</code> or <code>null</code>.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key which is equivalent to its being
     * set to the <code>Method</code> corresponding to
     * {@link PlanarImage#createColorModel(SampleModel)}.
     *
     * @see OpImage#OpImage
     *
     * @since JAI 1.1
     */
    public static RenderingHints.Key KEY_DEFAULT_COLOR_MODEL_METHOD =
    new RenderingKey(HINT_DEFAULT_COLOR_MODEL_METHOD, Method.class);

    /**
     * Key for the dimensions of a <code>RenderedImage</code> created by
     * invoking <code>createDefaultRendering()</code> on a node of type
     * <code>RenderableOp</code> in a renderable processing chain.  The
     * type of the associated value is <code>java.awt.Dimension</code>.
     *
     * @see RenderableOp#createDefaultRendering()
     *
     * @since JAI 1.1
     */
    public static final RenderingHints.Key KEY_DEFAULT_RENDERING_SIZE =
    new RenderingKey(HINT_DEFAULT_RENDERING_SIZE, Dimension.class);

    /**
     * Key for {@link ColorModelFactory} object values.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     *
     * @see OpImage#OpImage
     *
     * @since JAI 1.1.2
     */
    public static RenderingHints.Key KEY_COLOR_MODEL_FACTORY =
    new RenderingKey(HINT_COLOR_MODEL_FACTORY, ColorModelFactory.class);

    /**
     * Key for enabling changing of destination image's <code>ColorModel</code>
     * to a <code>ComponentColorModel</code>, when the source image has
     * an <code>IndexColorModel</code>. The corresponding object must be
     * a <code>Boolean</code>. If the source image has an
     * <code>IndexColorModel</code>, and the user or the operation itself
     * does not set the <code>ColorModel</code> of the destination image,
     * the destination image's <code>ColorModel</code> would be copied
     * from the source and would therefore also be an
     * <code>IndexColorModel</code>. A <code>Boolean.TRUE</code> value
     * set for this key causes the destination image's
     * <code>ColorModel</code> to be changed to a
     * <code>ComponentColorModel</code>. The advantage of changing the
     * destination's <code>ColorModel</code> comes in the usage of
     * <code>RasterAccessor</code>. When a <code>RasterAccessor</code>
     * is created using this source and destination pair, the source
     * <code>IndexColorModel</code> will be automatically expanded,
     * allowing operations that depend on the pixel value (as opposed
     * to the index into the <code>ColorModel</code>) to function correctly.
     *
     * Note that the JAI provided dithering operations
     * (<code>errordiffusion</code>, <code>ordereddither</code>) along with
     * the color quantization operator, <code>colorquantizer</code> can be
     * used for the inverse operation, i.e. converting from an RGB image
     * to an indexed image.
     *
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key which is equivalent to its being
     * set to <code>FALSE</code>. Certain operators, however, change the
     * default for themselves to <code>Boolean.TRUE</code>.
     *
     * @see com.lightcrafts.mediax.jai.OpImage#OpImage
     * @see com.lightcrafts.mediax.jai.RasterAccessor
     * @see com.lightcrafts.mediax.jai.operator.ColorQuantizerDescriptor
     *
     * @since JAI 1.1.2
     */
    public static RenderingHints.Key KEY_REPLACE_INDEX_COLOR_MODEL =
    new RenderingKey(HINT_REPLACE_INDEX_COLOR_MODEL, Boolean.class);

    /**
     * Key for <code>TileFactory</code> object values.
     * The common <code>RenderingHints</code> contain a
     * {@link RecyclingTileFactory}-valued hint corresponding
     * to this key. The value is the same as that to which
     * {@link #KEY_TILE_RECYCLER} is initially mapped.
     *
     * @see PlanarImage#PlanarImage(ImageLayout,Vector,Map)
     * @see OpImage#OpImage(Vector,ImageLayout,Map,boolean)
     *
     * @since JAI 1.1.2
     */
    public static RenderingHints.Key KEY_TILE_FACTORY =
    new RenderingKey(HINT_TILE_FACTORY, TileFactory.class);

    /**
     * Key for <code>TileRecycler</code> object values.
     * The common <code>RenderingHints</code> contain a
     * {@link RecyclingTileFactory}-valued hint corresponding
     * to this key. The value is the same as that to which
     * {@link #KEY_TILE_FACTORY} is initially mapped.
     *
     * @see OpImage#OpImage(Vector,ImageLayout,Map,boolean)
     *
     * @since JAI 1.1.2
     */
    public static RenderingHints.Key KEY_TILE_RECYCLER =
    new RenderingKey(HINT_TILE_RECYCLER, TileRecycler.class);

    /**
     * Key for <code>Boolean</code> object values which specify
     * whether automatic recycling of application-visible tiles
     * should occur.  The common <code>RenderingHints</code> contain
     * a <code>FALSE</code>-valued hint corresponding to this key.
     *
     * @see OpImage#OpImage(Vector,ImageLayout,Map,boolean)
     *
     * @since JAI 1.1.2
     */
    public static RenderingHints.Key KEY_CACHED_TILE_RECYCLING_ENABLED =
    new RenderingKey(HINT_CACHED_TILE_RECYCLING_ENABLED, Boolean.class);

    /**
     * Key for specifying whether a deep copy of the image data should
     * be used when serializing images.  The corresponding
     * object must be a <code>Boolean</code>.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     *
     * @since JAI 1.1
     */
    public static RenderingHints.Key KEY_SERIALIZE_DEEP_COPY =
    new RenderingKey(HINT_SERIALIZE_DEEP_COPY, Boolean.class);

    /**
     * Key for specifying the default format to be used for tile
     * serialization via <code>TileCodec</code>s.  The corresponding
     * object must be a <code>String</code>.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     *
     * @since JAI 1.1
     */
    public static RenderingHints.Key KEY_TILE_CODEC_FORMAT =
    new RenderingKey(HINT_TILE_CODEC_FORMAT, String.class);

    /**
     * Key for specifying the default encoding parameters to be used for
     * tile serialization via <code>TileCodec</code>s.  The corresponding
     * object must be a <code>TileCodecParameterList</code>.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     *
     * @since JAI 1.1
     */
    public static RenderingHints.Key KEY_TILE_ENCODING_PARAM =
    new RenderingKey(HINT_TILE_ENCODING_PARAM, TileCodecParameterList.class);

    /**
     * Key for specifying the default decoding parameters to be used for
     * tile serialization via <code>TileCodec</code>s.  The corresponding
     * object must be a <code>TileCodecParameterList</code>.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     *
     * @since JAI 1.1
     */
    public static RenderingHints.Key KEY_TILE_DECODING_PARAM =
    new RenderingKey(HINT_TILE_DECODING_PARAM, TileCodecParameterList.class);

    /**
     * Key for the retry interval value to be used for dealing with
     * network errors during remote imaging. The corresponding
     * object must be an <code>Integer</code>.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     *
     * @see com.lightcrafts.mediax.jai.remote.RemoteJAI
     *
     * @since JAI 1.1
     */
    public static RenderingHints.Key KEY_RETRY_INTERVAL =
        new RenderingKey(HINT_RETRY_INTERVAL, Integer.class);

    /**
     * Key for the number of retries to be used for dealing with
     * network errors during remote imaging. The corresponding
     * object must be an <code>Integer</code>.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     *
     * @see com.lightcrafts.mediax.jai.remote.RemoteJAI
     *
     * @since JAI 1.1
     */
    public static RenderingHints.Key KEY_NUM_RETRIES =
        new RenderingKey(HINT_NUM_RETRIES, Integer.class);

    /**
     * Key for the negotiation preferences to be used to negotiate
     * capabilities to be used in the remote communication. The
     * corresponding object must be a <code>NegotiableCapabilitySet</code>.
     * The common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.
     *
     * @see com.lightcrafts.mediax.jai.remote.RemoteJAI
     *
     * @since JAI 1.1
     */
    public static RenderingHints.Key KEY_NEGOTIATION_PREFERENCES =
        new RenderingKey(HINT_NEGOTIATION_PREFERENCES,
                         NegotiableCapabilitySet.class);

    /**
     * Key that indicates whether the {@link ColormapOpImage}s do
     * the transform on the color map or on the pixels when the source
     * image and destination images are all color-indexed.  The
     * corresponding object must be a <code>Boolean</code>.  The
     * common <code>RenderingHints</code> do not contain a default
     * hint corresponding to this key.  The default behavior is
     * equivalent to setting a hint with a value of
     * <code>Boolean.TRUE</code>.
     *
     * @since JAI 1.1.2
     */
    public static RenderingHints.Key KEY_TRANSFORM_ON_COLORMAP =
	new RenderingKey(HINT_TRANSFORM_ON_COLORMAP,
			 Boolean.class);

    /**
     * Key for the {@link ImagingListener} registered to a rendering node.
     * The default mapping of this key in each JAI instance rethrows
     * <code>RuntimeException</code>s and prints the error message and
     * stack trace of other exceptions to <code>System.err</code>.
     *
     * @since JAI 1.1.2
     */
    public static RenderingHints.Key KEY_IMAGING_LISTENER =
	new RenderingKey(HINT_IMAGING_LISTENER,
			 ImagingListener.class);

    /**
     * Initial default tile size. Applies to both dimensions.
     */
    private static final int DEFAULT_TILE_SIZE = 512;

    /**
     * Default tile size. Null signifies no default.
     */
    private static Dimension defaultTileSize =
        new Dimension(DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE);

    /**
     * Default <code>RenderableOp</code> rendering size.
     * Null signifies no default.
     */
    private static Dimension defaultRenderingSize = new Dimension(0, 512);


    private OperationRegistry operationRegistry;
    private TileScheduler tileScheduler;
    private TileCache tileCache;
    private RenderingHints renderingHints;

    /**
     * A <code>ImagingListener</code> to listen and/or process the special
     * situations in the operations registered in this
     * <code>JAI</code>.
     *
     * @since JAI 1.1.2
     */
    private ImagingListener imagingListener = ImagingListenerImpl.getInstance();

    private static JAI defaultInstance =
        new JAI(OperationRegistry.initializeRegistry(),
                new SunTileScheduler(),
                new SunTileCache(),
                new RenderingHints(null));

    /** Returns a new instance of the JAI class. */
    private JAI(OperationRegistry operationRegistry,
                TileScheduler tileScheduler,
                TileCache tileCache,
                RenderingHints renderingHints) {
        this.operationRegistry = operationRegistry;
        this.tileScheduler = tileScheduler;
        this.tileCache = tileCache;
        this.renderingHints = renderingHints;

        this.renderingHints.put(KEY_OPERATION_REGISTRY, operationRegistry);
        this.renderingHints.put(KEY_TILE_CACHE, tileCache);
        this.renderingHints.put(KEY_TILE_SCHEDULER, tileScheduler);

        TileFactory rtf = new RecyclingTileFactory();
        this.renderingHints.put(KEY_TILE_FACTORY, rtf);
        this.renderingHints.put(KEY_TILE_RECYCLER, rtf);
        this.renderingHints.put(KEY_CACHED_TILE_RECYCLING_ENABLED,
                                Boolean.FALSE);
        this.renderingHints.put(KEY_IMAGING_LISTENER, imagingListener);
    }

    /**
     * Returns JAI version information as a <code>String</code>
     *
     * @since JAI 1.1
     */
    public static final String getBuildVersion() {
	try {
	    InputStream is = JAI.class.getResourceAsStream("buildVersion");
	    if (is == null) 
		is = PropertyUtil.getFileFromClasspath("javax/media/jai/buildVersion");

	    BufferedReader reader =
		new BufferedReader(new InputStreamReader(is));

	    StringWriter sw = new StringWriter();
	    BufferedWriter writer = new BufferedWriter(sw);

	    String str;
	    boolean append = false;

	    while ((str = reader.readLine()) != null) {
		if (append) writer.newLine();

		writer.write(str);
		append = true;
	    }

	    writer.close();
	    return sw.getBuffer().toString();

	} catch (Exception e) {
            return JaiI18N.getString("JAI13");
	}
    }

    /**
     * Disable use of default tile cache.  Tiles are not stored.
     *
     * @since JAI 1.1
     */
    public static final void disableDefaultTileCache() {
        TileCache tmp = defaultInstance.getTileCache();
        if ( tmp != null ) {
            tmp.flush();
        }
        defaultInstance.renderingHints.remove(KEY_TILE_CACHE);
    }

    /**
     * Enable use of default tile cache.  Tiles are stored.
     *
     * @since JAI 1.1
     */
    public static final void enableDefaultTileCache() {
        defaultInstance.renderingHints.put(KEY_TILE_CACHE,
                                           defaultInstance.getTileCache());
    }

    /**
     * Sets the default tile dimensions to the clone of the provided parameter.
     * If <code>null</code> there are no default dimensions.
     *
     * @param tileDimensions The default tile dimensions or <code>null</code>.
     *
     * @throws <code>IllegalArgumentException</code> if
     *         <code>tileDimensions</code> is non-<code>null</code> and
     *         has non-positive width or height.
     *
     * @since JAI 1.1
     */
    public static final void setDefaultTileSize(Dimension tileDimensions) {
        if(tileDimensions != null &&
           (tileDimensions.width <= 0 || tileDimensions.height <= 0)) {
            throw new IllegalArgumentException();
        }

        defaultTileSize = tileDimensions != null ?
            (Dimension)tileDimensions.clone() : null;
    }

    /**
     * Retrieves the clone of the default tile dimensions.
     * If <code>null</code> there are no default dimensions set.
     *
     * @return The default tile dimensions or <code>null</code>.
     *
     * @since JAI 1.1
     */
    public static final Dimension getDefaultTileSize() {
        return defaultTileSize != null ?
            (Dimension)defaultTileSize.clone() : null;
    }

    /**
     * Sets the default size of the rendering created by invoking
     * <code>createDefaultRendering()</code> on a <code>RenderableOp</code>.
     * This default size may be overruled by setting a hint with key
     * <code>KEY_DEFAULT_RENDERING_SIZE</code> on the node.
     * If <code>null</code> there are no default dimensions.
     * Either dimension may be non-positive in which case the other
     * dimension and the renderable aspect ratio will be used to compute
     * the rendered image size.  The intial value of this setting is
     * <pre>
     * new Dimension(0, 512)
     * </pre>
     * which produces a default rendering of height 512 and width
     * 512*aspect_ratio.
     *
     * @param defaultSize The default rendering size or <code>null</code>.
     *
     * @throws <code>IllegalArgumentException</code> if
     *         <code>defaultSize</code> is non-<code>null</code> and
     *         both the width and height are non-positive.
     *
     * @since JAI 1.1
     */
    public static final void setDefaultRenderingSize(Dimension defaultSize) {
        if(defaultSize != null &&
           defaultSize.width <= 0 &&
           defaultSize.height <= 0) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI8"));
        }

        defaultRenderingSize = defaultSize == null ?
            null : new Dimension(defaultSize);
    }

    /**
     * Retrieves a copy of the default rendering size.
     * If <code>null</code> there is no default size set.
     *
     * @return The default rendering size or <code>null</code>.
     *
     * @since JAI 1.1
     */
    public static final Dimension getDefaultRenderingSize(){
        return defaultRenderingSize == null ?
            null : new Dimension(defaultRenderingSize);
    }

    /**
     * Returns the default<code>JAI</code>instance.  This instance is used
     * by all of the static methods of this class.  It uses the default
     * <code>OperationRegistry</code> and, in the Sun Microsystems, Inc.
     * implementation, the Sun implementations of <code>TileCache</code> and
     * <code>TileScheduler</code>.  The <code>RenderingHints</code> will
     * contain hints only for these three entities.
     *
     * <p>Unless otherwise changed through a <code>setOperationRegistry
     * </code> the <code>OperationRegistry</code> used by the default
     * instance is thread-safe.
     */
    public static JAI getDefaultInstance() {
        return defaultInstance;
    }

    /**
     * Merge one <code>RenderingHints</code> into another.
     *
     * @param defaultHints The default <code>RenderingHints</code>.
     * @param hints The superseding <code>RenderingHints</code>; hints in
     *              this mapping take precedence over any in
     *              <code>defaultHints</code>.
     */
    static RenderingHints mergeRenderingHints(RenderingHints defaultHints,
                                              RenderingHints hints) {
        RenderingHints mergedHints;
        if (hints == null || hints.isEmpty()) {
            mergedHints = defaultHints;
        } else if(defaultHints == null || defaultHints.isEmpty()) {
            mergedHints = hints;
        } else { // Both parameters are non-null and non-empty.
            mergedHints = new RenderingHints((Map)defaultHints);
            mergedHints.add(hints);
        }

        return mergedHints;
    }

    /**
     * Returns a new instance of the<code>JAI</code>class.  The
     * <code>OperationRegistry</code>, <code>TileScheduler</code>, and
     * <code>TileCache</code> will initially be references to those of
     * the default instance.  The rendering hints will be set to a
     * clone of those of the default instance.
     */
    public JAI() {
        this.operationRegistry = defaultInstance.operationRegistry;
        this.tileScheduler = defaultInstance.tileScheduler;
        this.tileCache = defaultInstance.tileCache;
        this.renderingHints =
            (RenderingHints)defaultInstance.renderingHints.clone();
    }

    /**
     * Returns the<code>OperationRegistry</code> being used by
     * this<code>JAI</code>instance.
     *
     * <p>Unless otherwise changed through a <code>setOperationRegistry
     * </code> the <code>OperationRegistry</code> returned by <code>
     * getDefaultInstance().getOperationRegistry()</code> is thread-safe.
     */
    public OperationRegistry getOperationRegistry() {
        return operationRegistry;
    }

    /**
     * Sets the<code>OperationRegistry</code> to be used by this<code>JAI</code>instance.
     *
     *@throws IllegalArgumentException if <code>operationRegistry</code> is <code>null</code>.
     */
    public void setOperationRegistry(OperationRegistry operationRegistry) {
        if (operationRegistry == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        this.operationRegistry = operationRegistry;
        this.renderingHints.put(KEY_OPERATION_REGISTRY, operationRegistry);
    }

    /** Returns the <code>TileScheduler</code> being used by this<code>JAI</code>instance. */
    public TileScheduler getTileScheduler() {
        return tileScheduler;
    }

    /**
     * Sets the <code>TileScheduler</code> to be used by this<code>JAI</code>
     * instance.  The
     * <code>tileScheduler</code> parameter will be added to the
     * <code>RenderingHints</code> of this <code>JAI</code> instance.
     * @throws IllegalArgumentException if <code>tileScheduler</code> is <code>null</code>.
     */
    public void setTileScheduler(TileScheduler tileScheduler) {
        if (tileScheduler == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        this.tileScheduler = tileScheduler;
        renderingHints.put(KEY_TILE_SCHEDULER, tileScheduler);
    }

    /** Returns the <code>TileCache</code> being used by this<code>JAI</code>instance. */
    public TileCache getTileCache() {
        return tileCache;
    }

    /**
     * Sets the <code>TileCache</code> to be used by this<code>JAI</code>
     * instance.  The
     * <code>tileCache</code> parameter will be added to the
     * <code>RenderingHints</code> of this <code>JAI</code> instance.
     *
     * @throws IllegalArgumentException if <code>tileCache</code> is <code>null</code>.
     */
    public void setTileCache(TileCache tileCache) {
        if (tileCache == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        this.tileCache = tileCache;
        renderingHints.put(KEY_TILE_CACHE, tileCache);
    }

   /**
     * The default implementation constructs a <code>TileCache</code>
     * with the given memory capacity in bytes.  Users may supply an
     * instance of <code>TileCache</code> to an operation by
     * supplying a <code>RenderingHint</code> with a
     * JAI.KEY_TILE_CACHE key and the desired <code>TileCache</code>
     * instance as its value.  Note that the absence of a tile cache
     * hint will result in the use of the <code>TileCache</code>
     * belonging to the default<code>JAI</code>instance.  To force an operation
     * not to perform caching, a <code>TileCache</code> instance with
     * a tile capacity of 0 may be used.
     * An exception will be thrown if memCapacity is negative.
     * Attempting to set either value larger than the JVM size may result in an
     * OutOfMemory exception.
     *
     * @deprecated as of JAI 1.1 Refer to createTileCache(long memCapacity).
     */
    public static TileCache createTileCache(int tileCapacity,
                                            long memCapacity) {
        if (memCapacity < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI10"));
        }
        return new SunTileCache(memCapacity);
    }

    /**
     * Constructs a <code>TileCache</code> with the given memory capacity
     * in bytes.  Users may supply an instance of <code>TileCache</code>
     * to an operation by supplying a <code>RenderingHint</code> with a
     * JAI.KEY_TILE_CACHE key and the desired <code>TileCache</code>
     * instance as its value.  Note that the absence of a tile cache
     * hint will result in the use of the <code>TileCache</code>
     * belonging to the default<code>JAI</code>instance.  To force an operation
     * not to perform caching, a <code>TileCache</code> instance with
     * a tile capacity of 0 may be used.
     * An exception will be thrown if memCapacity is negative.
     * Attempting to set either value larger than the JVM size may result in an
     * OutOfMemory exception.
     *
     * @since JAI 1.1
     */
    public static TileCache createTileCache(long memCapacity) {
        if (memCapacity < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI10"));
        }
        return new SunTileCache(memCapacity);
    }

    /**
     * Constructs a <code>TileCache</code> with the default memory
     * capacity in bytes. Users may supply an instance of
     * <code>TileCache</code> to an operation by
     * supplying a <code>RenderingHint</code> with a
     * JAI.KEY_TILE_CACHE key and the desired <code>TileCache</code>
     * instance as its value.  Note that the absence of a tile cache
     * hint will result in the use of the <code>TileCache</code>
     * belonging to the default<code>JAI</code>instance.  To force an operation
     * not to perform caching, a <code>TileCache</code> instance with
     * a tile capacity of 0 may be used.
     */
    public static TileCache createTileCache() {
        return new SunTileCache();
    }

    /**
     * Constructs a <code>TileScheduler</code> with the default parallelism
     * and priorities.
     *
     * <p> In the Sun Microsystems reference implementation of TileScheduler
     * the default parallelism is 2, default priority is
     * <code>THREAD.NORM_PRIORITY</code>, default prefetch parallelism is 1,
     * and default prefetch priority is <code>THREAD.MIN_PRIORITY</code>.
     *
     * @since JAI 1.1
     */
    public static TileScheduler createTileScheduler() {
        return new SunTileScheduler();
    }


  // Create methods for Rendered mode.


    /**
     * Creates a <code>RenderedOp</code> which represents the named
     * operation, using the source(s) and/or parameter(s) specified in
     * the <code>ParameterBlock</code>, and applying the specified
     * hints to the destination.  This method should only be used when
     * the final result returned is a single
     * <code>RenderedImage</code>.
     *
     * <p> The default<code>JAI</code>instance is used as the source of the
     * registry and tile scheduler; that is, this method is equivalent
     * to <code>getDefaultInstance().createNS(opName, args, hints)</code>.
     * The functionality of this method is the same as its corresponding
     * non-static method <code>createNS()</code>.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     * @param hints  The hints for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the default operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the default operation registry does not
     *         support rendered image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.RenderedImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>RenderedOp</code> that represents the named
     *          operation, or <code>null</code> if the specified operation
     *          is in the "immediate" mode and the rendering of the
     *          <code>PlanarImage</code> failed.
     */
    public static RenderedOp create(String opName,
                                    ParameterBlock args,
                                    RenderingHints hints) {
        return defaultInstance.createNS(opName, args, hints);
    }

    /**
     * Creates a <code>RenderedOp</code> which represents the named
     * operation, using the source(s) and/or parameter(s) specified in
     * the <code>ParameterBlock</code>, and applying the specified
     * hints to the destination.  This method should only be used when
     * the final result returned is a single
     * <code>RenderedImage</code>.  However, the source(s) supplied
     * may be a collection of rendered images or a collection of
     * collections that at the very basic level include rendered
     * images.
     *
     * <p> The supplied operation name is validated against the
     * operation registry.  The source(s) and/or parameter(s) in the
     * <code>ParameterBlock</code> are validated against the named
     * operation's descriptor, both in their numbers and types.
     * Additional restrictions placed on the sources and parameters
     * by an individual operation are also validated by calling its
     * <code>OperationDescriptor.validateArguments()</code> method.
     *
     * <p><code>JAI</code>allows a parameter to have a <code>null</code> input
     * value, if that particular parameter has a default value specified
     * in its operation's descriptor.  In this case, the default value
     * will replace the <code>null</code> input.
     *
     * <p><code>JAI</code>also allows unspecified tailing parameters, if these
     * parameters have default values specified in the operation's
     * descriptor. However, if a parameter, which has a default value,
     * is followed by one or more parameters that
     * have no default values, this parameter must be specified in the
     * <code>ParameterBlock</code>, even if it only has a value of
     * code>null</code>.
     *
     * <p> The rendering hints associated with this instance of
     * <code>JAI</code> are overlaid with the hints passed to this
     * method.  That is, the set of keys will be the union of the
     * keys from the instance's hints and the hints parameter.
     * If the same key exists in both places, the value from the
     * hints parameter will be used.
     *
     * <p> This version of <code>create</code> is non-static; it may
     * be used with a specific instance of the<code>JAI</code>class.
     * All of the static <code>create()</code> methods ultimately call this
     * method, thus inheriting this method's error handling.
     *
     * <p> Since this method performs parameter checking, it may not
     * be suitable for creating <code>RenderedOp</code> nodes meant to
     * be passed to another host using the <code>RemoteImage</code>
     * interface.  For example, it might be necessary to refer to a
     * file that is present only on the remote host.  In such cases,
     * it is possible to instantiate a <code>RenderedOp</code>
     * directly, avoiding all checks.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     * @param hints  The hints for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the current operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the current operation registry does not
     *         support rendered image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.RenderedImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>RenderedOp</code> that represents the named
     *          operation, or <code>null</code> if the specified operation
     *          is in the "immediate" mode and the rendering of the
     *          <code>PlanarImage</code> failed.
     */
    public RenderedOp createNS(String opName,
                               ParameterBlock args,
                               RenderingHints hints) {

        if(opName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI14"));
        } else if (args == null) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI15"));
        }

	String modeName = "rendered";

        // Get the OperationDescriptor registered under the specified name.
        OperationDescriptor odesc = (OperationDescriptor)
            operationRegistry.getDescriptor(modeName, opName);

        if (odesc == null) {
            throw new IllegalArgumentException(opName + ": " +
                                               JaiI18N.getString("JAI0"));
        }

	if (!RenderedImage.class.isAssignableFrom(odesc.getDestClass(modeName))) {
	  throw new IllegalArgumentException(opName + ": " +
					     JaiI18N.getString("JAI2"));
        }


         // Validate input arguments. The ParameterBlock is cloned here
         // because OperationDescriptor.validateArguments() may change
         // its content.

        StringBuffer msg = new StringBuffer();
        args = (ParameterBlock)args.clone();
        if (!odesc.validateArguments(modeName, args, msg)) {
            throw new IllegalArgumentException(msg.toString());
        }

        // Merge rendering hints.  Hints passed in take precedence.
        RenderingHints mergedHints = mergeRenderingHints(renderingHints, hints);

        RenderedOp op = new RenderedOp(operationRegistry, opName,
                                       args, mergedHints);

        // If the operation requests immediate rendering, do so.
        if (odesc.isImmediate()) {
            PlanarImage im = null;
            im = op.getRendering();
            if (im == null) {
	      // Op could not be rendered, return null.
                return null;
            }
        }

        // Return the RenderedOp associated with this operation.
        return op;
    }

    /**
     * Creates a <code>Collection</code> which represents the named
     * operation, using the source(s) and/or parameter(s) specified in
     * the <code>ParameterBlock</code>, and applying the specified
     * hints to the destination.  This method should only be used when
     * the final result returned is a <code>Collection</code>.  (This
     * includes <code>com.lightcrafts.mediax.jai.CollectionOp</code>s.)
     *
     * <p> The default<code>JAI</code>instance is used as the source of the
     * registry and tile scheduler; that is, this method is equivalent
     * to <code>getDefaultInstance().createCollectionNS(opName, args,
     * hints)</code>.  The functionality of this method is the same as
     * its corresponding non-static method <code>createCollectionNS()</code>.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     * @param hints  The hints for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the default operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the default operation registry does not
     *         support rendered image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.RenderedImage</code> or a
     *         <code>com.lightcrafts.mediax.jai.CollectionImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>Collection</code> that represents the named
     *          operation, or <code>null</code> if the specified operation
     *          is in the "immediate" mode and the rendering of the
     *          <code>PlanarImage</code> failed.
     */
    public static Collection createCollection(String opName,
                                              ParameterBlock args,
                                              RenderingHints hints) {
        return defaultInstance.createCollectionNS(opName, args, hints);
    }

    /**
     * Creates a <code>Collection</code> which represents the named
     * operation, using the source(s) and/or parameter(s) specified in
     * the <code>ParameterBlock</code>, and applying the specified
     * hints to the destination.  This method should only be used when
     * the final result returned is a <code>Collection</code>.  (This
     * includes <code>com.lightcrafts.mediax.jai.CollectionOp</code>s.)  The
     * source(s) supplied may be a collection of rendered images or a
     * collection of collections that at the very basic level include
     * rendered images.
     * The source(s) supplied are unwrapped to create a single collection
     * that contains RenderedOps and collections as many as the size of
     * the smallest collection supplied in the sources. The nth collection
     * is created using all supplied rendered images and the nth element of
     * each of the collections supplied in the source.
     *
     * <p> The supplied operation name is validated against the
     * operation registry.  The source(s) and/or parameter(s) in the
     * <code>ParameterBlock</code> are val>idated against the named
     * operation's descriptor, both in their numbers and types.
     * Additional restrictions placed on the sources and parameters
     * by an individual operation are also validated by calling its
     * <code>OperationDescriptor.validateArguments()</code> method.
     *
     * <p><code>JAI</code>allows a parameter to have a <code>null</code> input
     * value, if that particular parameter has a default value specified
     * in its operation's descriptor.  In this case, the default value
     * will replace the <code>null</code> input.
     *
     * <p><code>JAI</code>also allows unspecified tailing parameters, if these
     * parameters have default values specified in the operation's
     * descriptor. However, if a parameter, which
     * has a default value, is followed by one or more parameters that
     * have no default values, this parameter must be specified in the
     * <code>ParameterBlock</code>, even if it only has a value of
     * code>null</code>.
     *
     * <p> The rendering hints associated with this instance of
     * <code>JAI</code> are overlaid with the hints passed to this
     * method.  That is, the set of keys will be the union of the
     * keys from the instance's hints and the hints parameter.
     * If the same key exists in both places, the value from the
     * hints parameter will be used.
     *
     * <p> This version of <code>createCollection</code> is
     * non-static; it may be used with a specific instance of the JAI
     * class.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     * @param hints  The hints for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the current operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the current operation registry does not
     *         support rendered image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.RenderedImage</code> or a
     *         <code>com.lightcrafts.mediax.jai.CollectionImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>Collection</code> that represents the named
     *          operation, or <code>null</code> if the specified operation
     *          is in the "immediate" mode and the rendering of the
     *          <code>PlanarImage</code> failed.
     */
    public Collection createCollectionNS(String opName,
                                         ParameterBlock args,
                                         RenderingHints hints) {

        if(opName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI14"));
        } else if (args == null) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI15"));
        }

	String modeName = "collection";

	// Get the OperationDescriptor registered under the specified name.
        OperationDescriptor odesc = (OperationDescriptor)
            operationRegistry.getDescriptor(modeName, opName);

        if (odesc == null) {
            throw new IllegalArgumentException(opName + ": " +
                                               JaiI18N.getString("JAI0"));
        }

	Class destClass = odesc.getDestClass(modeName);

	if (!RenderedImage.class.isAssignableFrom(destClass) &&
	    !CollectionImage.class.isAssignableFrom(destClass)) {
	  throw new IllegalArgumentException(opName + ": " +
					     JaiI18N.getString("JAI5"));
        }

        // Merge rendering hints.  Hints passed in take precedence.
        RenderingHints mergedHints = mergeRenderingHints(renderingHints, hints);

         // Validate input arguments. The ParameterBlock is cloned here
         // because OperationDescriptor.validateArguments() may change
         // its content.

        StringBuffer msg = new StringBuffer();
        args = (ParameterBlock)args.clone();
	if (odesc.validateArguments(modeName, args, msg)) {
            if (RenderedImage.class.isAssignableFrom(destClass)) {
                Vector v = new Vector(1);
                v.add(new RenderedOp(operationRegistry, opName,
                                     args, mergedHints));
                return v;
            } else {
                CollectionOp cOp = new CollectionOp(operationRegistry, opName,
                                                    args, mergedHints);

                // If the operation requests immediate rendering, do so.
                if (odesc.isImmediate()) {
                    Collection coll = null;
                    coll = cOp.getCollection();
                    if(coll == null) {
                        return null;
                    }
                }

                return cOp;
            }

	} else {
            int numSources = odesc.getNumSources();
            Vector sources = args.getSources();


	    // Get the iterator of all the sources that are collection.
            //  Get the iterator for the collection that has the least elements.

            Iterator[] iters = new Iterator[numSources];
            Iterator iter = null;
            int size = Integer.MAX_VALUE;
            for (int i = 0; i < numSources; i++) {
                Object s = sources.elementAt(i);
                if (s instanceof Collection) {
                    iters[i] = ((Collection)s).iterator();
                    if (iter == null || ((Collection)s).size() < size) {
                        iter = iters[i];
                        size = ((Collection)s).size();
                    }
                }
            }

            if (iter == null) {

	      // None of the sources is a collection. The error is
              // with the input arguments.

                throw new IllegalArgumentException(msg.toString());
            }

            // Some sources are of type collection. Need to unwrap them.
            Collection col = null;
            for (int i = 0; i < numSources; i++) {
                Object s = sources.elementAt(i);
                if (s instanceof Collection) {
                    try {
                        col = (Collection)s.getClass().newInstance();
                        break;
                    } catch (Exception e) {
		      // Unable to create this collection type, try next.
                        sendExceptionToListener(
                            JaiI18N.getString("JAI16") + s.getClass().getName(),
                            e);
                    }
                }
            }
            if (col == null) {
                col = new Vector();
            }

            // Get the source types.
            Class[] sourceClasses = odesc.getSourceClasses(modeName);

            while (iter.hasNext()) {
                ParameterBlock pb = new ParameterBlock();
                pb.setParameters(args.getParameters());

                for (int i = 0; i < numSources; i++) {
                    // Get the next source.
                    Object nextSource = null;
                    if (iters[i] == null) {
                        nextSource = sources.elementAt(i);
                    } else {
                        nextSource = iters[i].next();
                    }

                    // If the source is not of a compatible type and
                    // is not a Collection then the 'false' value
                    // returned by validateArguments() above must indicate
                    // a real error.
                    if(!sourceClasses[i].isAssignableFrom(nextSource.getClass()) &&
                       !(nextSource instanceof Collection)) {
                        throw new IllegalArgumentException(msg.toString());
                    }
                    pb.addSource(nextSource);
                }

                Collection c = createCollectionNS(opName, pb, mergedHints);
                if (c instanceof Vector &&
                    c.size() == 1 &&
                    ((Vector)c).elementAt(0) instanceof RenderedOp) {
                    col.add(((Vector)c).elementAt(0));
                } else {
                    col.add(c);
                }
            }

            return col;
        }
    }



  // Convenience create methods for rendered mode.


    /**
     * Creates a <code>RenderedOp</code> with <code>null</code>
     * rendering hints.
     *
     * @param opName  The name of the operation.
     * @param args    The source(s) and/or parameter(s) for the operation.
     */
    public static RenderedOp create(String opName,
				    ParameterBlock args) {
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1 <code>Object</code> parameter.
     *
     * @param opName  The name of the operation.
     * @param param   The <code>Object</code> parameter.
     */
    public static RenderedOp create(String opName,
                                    Object param) {
	ParameterBlock args = new ParameterBlock();
        args.add(param);
	return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 2 <code>Object</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param param1  The first <code>Object</code> parameter.
     * @param param2  The second <code>Object</code> parameter.
     */
    public static RenderedOp create(String opName,
                                    Object param1,
                                    Object param2) {
        ParameterBlock args = new ParameterBlock();
        args.add(param1);
        args.add(param2);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1 <code>Object</code> parameter and
     * 1 <code>int</code> parameter
     *
     * @param opName  The name of the operation.
     * @param param1  The <code>Object</code> parameter.
     * @param param2  The <code>int</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    Object param1,
                                    int param2) {
        ParameterBlock args = new ParameterBlock();
        args.add(param1);
        args.add(param2);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 3 <code>Object</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param param1  The first <code>Object</code> parameter.
     * @param param2  The second <code>Object</code> parameter.
     * @param param3  The third <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    Object param1,
                                    Object param2,
                                    Object param3) {
        ParameterBlock args = new ParameterBlock();
        args.add(param1);
        args.add(param2);
        args.add(param3);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 2 <code>int</code> parameters
     * and one <code>Object</code> parameter
     *
     * @param opName  The name of the operation.
     * @param param1  The first <code>int</code> parameter.
     * @param param2  The second <code>int</code> parameter.
     * @param param3  The <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    int param1,
                                    int param2,
                                    Object param3) {
        ParameterBlock args = new ParameterBlock();
        args.add(param1);
        args.add(param2);
        args.add(param3);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 4 <code>Object</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param param1  The first <code>Object</code> parameter.
     * @param param2  The second <code>Object</code> parameter.
     * @param param3  The third <code>Object</code> parameter.
     * @param param4  The fourth <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    Object param1,
                                    Object param2,
                                    Object param3,
                                    Object param4) {
        ParameterBlock args = new ParameterBlock();
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 2 <code>Object</code> and 2 <code>int</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param param1  The first <code>Object</code> parameter.
     * @param param2  The first <code>int</code> parameter.
     * @param param3  The second <code>Object</code> parameter.
     * @param param4  The second <code>int</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    Object param1,
                                    int param2,
                                    Object param3,
                                    int param4) {
        ParameterBlock args = new ParameterBlock();
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1 <code>RenderedImage</code> source.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src) {
	ParameterBlock args = new ParameterBlock();
	args.addSource(src);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1 Collection source.
     *
     * @param opName  The name of the operation.
     * @param srcCol  The Collection src parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    Collection srcCol) {
	ParameterBlock args = new ParameterBlock();
	args.addSource(srcCol);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source and
     * 1 <code>Object</code> parameter.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param   The <code>Object</code> parameter.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    Object param) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source and
     * 1 <code>int</code> parameter.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param   The <code>int</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    int param) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source
     * and 2 <code>Object</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>object</code> parameter.
     * @param param2  The second <code>Object</code> parameter.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    Object param1,
                                    Object param2) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source,
     * 1 <code>Object</code> and 1 <code>float</code> parameter.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The <code>Object</code> parameter.
     * @param param2  The <code>float</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    Object param1,
                                    float param2) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source
     * and 3 <code>Object</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>Object</code> parameter.
     * @param param2  The second <code>Object</code> parameter.
     * @param param3  The third <code>Object</code> parameter.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    Object param1,
                                    Object param2,
                                    Object param3) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source,
     * 1 <code>Object</code> and 2 <code>int</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The <code>Object</code> parameter.
     * @param param2  The first <code>int</code> parameter.
     * @param param3  The second <code>int</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    Object param1,
                                    int param2,
                                    int param3) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source,
     * 2 <code>float</code> and 1 <code>Object</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>float</code> parameter.
     * @param param2  The second <code>float</code> parameter.
     * @param param3  The <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    float param1,
                                    float param2,
                                    Object param3) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source and
     * 4 <code>Object</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>Object</code> parameter.
     * @param param2  The second <code>Object</code> parameter.
     * @param param3  The third <code>Object</code> parameter.
     * @param param4  The fourth <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    Object param1,
                                    Object param2,
                                    Object param3,
                                    Object param4) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source and
     * 2 <code>Object</code> parameters and 2 <code>int</code> parameters
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>Object</code> parameter.
     * @param param2  The second <code>Object</code> parameter.
     * @param param3  The first <code>int</code> parameter.
     * @param param4  The second <code>int</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    Object param1,
                                    Object param2,
                                    int param3,
                                    int param4) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source and
     * 4 <code>int</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>int</code> parameter.
     * @param param2  The second <code>int</code> parameter.
     * @param param3  The third <code>int</code> parameter.
     * @param param4  The fourth <code>int</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    int param1,
                                    int param2,
                                    int param3,
                                    int param4) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source,
     * 3 <code>float</code> and 1 <code>Object</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>float</code> parameter.
     * @param param2  The second <code>float</code> parameter.
     * @param param3  The third <code>float</code> parameter.
     * @param param4  The <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    float param1,
                                    float param2,
                                    float param3,
                                    Object param4) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source and
     * 5 <code>Object</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>Object</code> parameter.
     * @param param2  The second <code>Object</code> parameter.
     * @param param3  The third <code>Object</code> parameter.
     * @param param4  The fourth <code>Object</code> parameter.
     * @param param5  The fifth <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    Object param1,
                                    Object param2,
                                    Object param3,
                                    Object param4,
                                    Object param5) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        args.add(param5);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1 <code>RenderedImage</code> source,
     * 4 <code>float</code> parameters and one <code>Object</code> parameter.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>float</code> parameter.
     * @param param2  The second <code>float</code> parameter.
     * @param param3  The third <code>float</code> parameter.
     * @param param4  The fourth <code>float</code> parameter.
     * @param param5  The <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    float param1,
                                    float param2,
                                    float param3,
                                    float param4,
                                    Object param5) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        args.add(param5);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source,
     * 3 <code>float</code> parameters, 1 <code>int</code> parameter and 1 <code>Object</code> parameter.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>float</code> parameter.
     * @param param2  The <code>int</code> parameter.
     * @param param3  The second <code>float</code> parameter.
     * @param param4  The third <code>float</code> parameter.
     * @param param5  The <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    float param1,
                                    int param2,
                                    float param3,
                                    float param4,
                                    Object param5) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        args.add(param5);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1
     * <code>RenderedImage</code> source and
     * 6 <code>Object</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>Object</code> parameter.
     * @param param2  The second <code>Object</code> parameter.
     * @param param3  The third <code>Object</code> parameter.
     * @param param4  The fourth <code>Object</code> parameter.
     * @param param5  The fifth <code>Object</code> parameter.
     * @param param6  The sixth <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    Object param1,
                                    Object param2,
                                    Object param3,
                                    Object param4,
                                    Object param5,
                                    Object param6) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        args.add(param5);
        args.add(param6);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 1 <code>RenderedImage</code> source,
     * 5 <code>int</code> parameters and 1 <code>Object</code> parameter.
     *
     * @param opName  The name of the operation.
     * @param src     The <code>RenderedImage</code> src parameter.
     * @param param1  The first <code>int</code> parameter.
     * @param param2  The second <code>int</code> parameter.
     * @param param3  The third <code>int</code> parameter.
     * @param param4  The fourth <code>int</code> parameter.
     * @param param5  The fifth <code>int</code> parameter.
     * @param param6  The <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src,
                                    int param1,
                                    int param2,
                                    int param3,
                                    int param4,
                                    int param5,
                                    Object param6) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        args.add(param5);
        args.add(param6);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>RenderedOp</code> that takes 2
     * <code>RenderedImage</code> sources.
     *
     * @param opName  The name of the operation.
     * @param src1    The first <code>RenderedImage</code> src.
     * @param src2    The second <code>RenderedImage</code> src.
     */
    public static RenderedOp create(String opName,
                             RenderedImage src1,
                             RenderedImage src2) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src1);
        args.addSource(src2);
        return create(opName, args, null);
    }


    /**
     * Creates a <code>RenderedOp</code> that takes 2
     * <code>RenderedImage</code> sources and
     * 4 <code>Object</code> parameters.
     *
     * @param opName  The name of the operation.
     * @param src1    The first <code>RenderedImage</code> src.
     * @param src2    The second <code>RenderedImage</code> src.
     * @param param1  The first <code>Object</code> parameter.
     * @param param2  The second <code>Object</code> parameter.
     * @param param3  The third <code>Object</code> parameter.
     * @param param4  The fourth <code>Object</code> parameter.
     * @deprecated as of JAI 1.1. Instead use
     *             <code>create(String,ParameterBlock)</code>.
     */
    public static RenderedOp create(String opName,
                                    RenderedImage src1,
                                    RenderedImage src2,
                                    Object param1,
                                    Object param2,
                                    Object param3,
                                    Object param4) {
        ParameterBlock args = new ParameterBlock();
        args.addSource(src1);
        args.addSource(src2);
        args.add(param1);
        args.add(param2);
        args.add(param3);
        args.add(param4);
        return create(opName, args, null);
    }

    /**
     * Creates a <code>Collection</code> with <code>null</code>
     * rendering hints.
     *
     * @param opName  The name of the operation.
     * @param args The source(s) and/or parameter(s) for the operation.
     */
    public static Collection createCollection(String opName,
                                              ParameterBlock args) {
        return createCollection(opName, args, null);
    }

     // Create methods for Renderable mode.

    /**
     * Creates a <code>RenderableOp</code> that represents the named
     * operation, using the source(s) and/or parameter(s) specified
     * in the <code>ParameterBlock</code>.  This method should only
     * be used when the final result returned is a single
     * <code>RenderdableImage</code>.
     *
     * <p> The default<code>JAI</code>instance is used as the source of the
     * registry and tile scheduler; that is, this method is equivalent to
     * <code>getDefaultInstance().createRenderableNS(opName, args, hints)</code>.
     * The functionality of this method is the same as its corresponding
     * non-static method <code>createRenderableNS()</code>.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     * @param hints  The hints for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the default operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the default operation registry does not
     *         support renderable image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.renderable.RenderableImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>RenderableOp</code> that represents the named
     *          operation.
     *
     * @since JAI 1.1
     */
    public static RenderableOp createRenderable(String opName,
                                                ParameterBlock args,
                                                RenderingHints hints) {
        return defaultInstance.createRenderableNS(opName, args, hints);
    }

    /**
     * Creates a <code>RenderableOp</code> that represents the named
     * operation, using the source(s) and/or parameter(s) specified
     * in the <code>ParameterBlock</code>.  This method should only
     * be used when the final result returned is a single
     * <code>RenderdableImage</code>.
     *
     * <p> The default<code>JAI</code>instance is used as the source of the
     * registry and tile scheduler; that is, this method is equivalent to
     * <code>getDefaultInstance().createRenderableNS(opName, args, null)</code>.
     * The functionality of this method is the same as its corresponding
     * non-static method <code>createRenderableNS()</code>.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the default operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the default operation registry does not
     *         support renderable image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.renderable.RenderableImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>RenderableOp</code> that represents the named
     *          operation.
     */
    public static RenderableOp createRenderable(String opName,
                                                ParameterBlock args) {
        return defaultInstance.createRenderableNS(opName, args, null);
    }

    /**
     * Creates a <code>RenderableOp</code> that represents the named
     * operation, using the source(s) and/or parameter(s) specified
     * in the <code>ParameterBlock</code>.  This method should only
     * be used when the final result returned is a single
     * <code>RenderableImage</code>.  However, the source(s) supplied
     * may be a collection of renderable images or a collection of
     * collections that at the very basic level include renderable
     * images.
     *
     * <p> The supplied operation name is validated against the
     * operation registry.  The source(s) and/or parameter(s) in the
     * <code>ParameterBlock</code> are validated against the named
     * operation's descriptor, both in their numbers and types.
     * Additional restrictions placed on the sources and parameters
     * by an individual operation are also validated by calling its
     * <code>OperationDescriptor.validateRenderableArguments()</code>
     * method.
     *
     * <p><code>JAI</code>allows a parameter to have a <code>null</code> input
     * value, if that particular parameter has a default value specified
     * in its operation's descriptor.  In this case, the default value
     * will replace the <code>null</code> input.
     *
     * <p><code>JAI</code>also allows unspecified tailing parameters, if these
     * parameters have default values specified in the operation's
     * descriptor. However, if a parameter, which
     * has a default value, is followed by one or more parameters that
     * have no default values, this parameter must be specified in the
     * <code>ParameterBlock</code>, even if it only has a value of
     * code>null</code>.
     *
     * <p> The rendering hints associated with this instance of
     * <code>JAI</code> are overlaid with the hints passed to this
     * method.  That is, the set of keys will be the union of the
     * keys from the instance's hints and the hints parameter.
     * If the same key exists in both places, the value from the
     * hints parameter will be used.
     *
     * <p> This version of the "createRenderable" is non-static; it
     * may be used with a specific instance of the<code>JAI</code>class.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     * @param hints  The hints for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the current operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the current operation registry does not
     *         support renderable image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.renderable.RenderableImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>RenderableOp</code> that represents the named
     *          operation.
     *
     * @since JAI 1.1
     */
    public RenderableOp createRenderableNS(String opName,
                                           ParameterBlock args,
                                           RenderingHints hints) {

        if(opName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI14"));
        } else if (args == null) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI15"));
        }

	String modeName = "renderable";

        // Get the OperationDescriptor registered under the specified name.
        OperationDescriptor odesc = (OperationDescriptor)
            operationRegistry.getDescriptor(modeName, opName);

        if (odesc == null) {
            throw new IllegalArgumentException(opName + ": " +
                                               JaiI18N.getString("JAI0"));
        }

	if (!RenderableImage.class.isAssignableFrom(odesc.getDestClass(modeName))) {
	  throw new IllegalArgumentException(opName + ": " +
					     JaiI18N.getString("JAI4"));
        }


         // Validate input arguments. The ParameterBlock is cloned here
	 // because OperationDescriptor.validateRenderableArguments()
	 // may change its content.

        StringBuffer msg = new StringBuffer();
        args = (ParameterBlock)args.clone();
        if (!odesc.validateArguments(modeName, args, msg)) {
            throw new IllegalArgumentException(msg.toString());
        }

        // Create a RenderableOp.
        RenderableOp op =
            new RenderableOp(operationRegistry, opName, args,
                             mergeRenderingHints(renderingHints, hints));

        // Return the RenderableOp.
        return op;
    }

    /**
     * Creates a <code>RenderableOp</code> that represents the named
     * operation, using the source(s) and/or parameter(s) specified
     * in the <code>ParameterBlock</code>.  This method should only
     * be used when the final result returned is a single
     * <code>RenderableImage</code>.  However, the source(s) supplied
     * may be a collection of renderable images or a collection of
     * collections that at the very basic level include renderable
     * images.
     *
     * <p> The supplied operation name is validated against the
     * operation registry.  The source(s) and/or parameter(s) in the
     * <code>ParameterBlock</code> are validated against the named
     * operation's descriptor, both in their numbers and types.
     * Additional restrictions placed on the sources and parameters
     * by an individual operation are also validated by calling its
     * <code>OperationDescriptor.validateRenderableArguments()</code>
     * method.
     *
     * <p><code>JAI</code>allows a parameter to have a <code>null</code> input
     * value, if that particular parameter has a default value specified
     * in its operation's descriptor.  In this case, the default value
     * will replace the <code>null</code> input.
     *
     * <p><code>JAI</code>also allows unspecified tailing parameters, if these
     * parameters have default values specified in the operation's
     * descriptor. However, if a parameter, which
     * has a default value, is followed by one or more parameters that
     * have no default values, this parameter must be specified in the
     * <code>ParameterBlock</code>, even if it only has a value of
     * code>null</code>.
     *
     * <p> The rendering hints associated with this instance of
     * <code>JAI</code> are overlaid with the hints passed to this
     * method.  That is, the set of keys will be the union of the
     * keys from the instance's hints and the hints parameter.
     * If the same key exists in both places, the value from the
     * hints parameter will be used.
     *
     * <p> This version of the "createRenderable" is non-static; it
     * may be used with a specific instance of the<code>JAI</code>class.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the current operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the current operation registry does not
     *         support renderable image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.renderable.RenderableImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>RenderableOp</code> that represents the named
     *          operation.
     *
     * @deprecated as of JAI 1.1 in favor of
     * <code>createRenderableNS(String,ParameterBlock,RenderingHints)</code>.
     * @see JAI#createRenderableNS(String,ParameterBlock,RenderingHints)
     */
    public RenderableOp createRenderableNS(String opName,
                                           ParameterBlock args) {
        return createRenderableNS(opName, args, null);
    }

    /**
     * Creates a <code>Collection</code> which represents the named
     * operation, using the source(s) and/or parameter(s) specified in
     * the <code>ParameterBlock</code>.  This method should only be used
     * when the final result returned is a <code>Collection</code>.
     * (This includes <code>com.lightcrafts.mediax.jai.CollectionOp</code>s.)
     *
     * <p> The default<code>JAI</code>instance is used as the source of the
     * registry and tile scheduler; that is, this method is equivalent
     * to <code>getDefaultInstance().createRenderableCollectionNS(opName,
     * args,hints)</code>.  The functionality of this method is the same as
     * its corresponding non-static method
     * <code>createRenderableCollectionNS()</code>.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     * @param hints  The hints for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the default operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the default operation registry does not
     *         support renderable image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.renderable.RenderableImage</code> or a
     *         <code>com.lightcrafts.mediax.jai.CollectionImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>Collection</code> that represents the named
     *          operation.
     *
     * @since JAI 1.1
     */
    public static Collection createRenderableCollection(String opName,
                                                        ParameterBlock args,
                                                        RenderingHints hints) {
        return defaultInstance.createRenderableCollectionNS(opName, args, hints);
    }

    /**
     * Creates a <code>Collection</code> which represents the named
     * operation, using the source(s) and/or parameter(s) specified in
     * the <code>ParameterBlock</code>.  This method should only be used
     * when the final result returned is a <code>Collection</code>.
     * (This includes <code>com.lightcrafts.mediax.jai.CollectionOp</code>s.)
     *
     * <p> The default<code>JAI</code>instance is used as the source of the
     * registry and tile scheduler; that is, this method is equivalent
     * to <code>getDefaultInstance().createRenderableCollectionNS(opName,
     * args,null)</code>.  The functionality of this method is the same as
     * its corresponding non-static method
     * <code>createRenderableCollectionNS()</code>.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the default operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the default operation registry does not
     *         support renderable image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.renderable.RenderableImage</code> or a
     *         <code>com.lightcrafts.mediax.jai.CollectionImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>Collection</code> that represents the named
     *          operation.
     */
    public static Collection createRenderableCollection(String opName,
                                                        ParameterBlock args) {
        return defaultInstance.createRenderableCollectionNS(opName, args, null);
    }

    /**
     * Creates a <code>Collection</code> which represents the named
     * operation, using the source(s) and/or parameter(s) specified in
     * the <code>ParameterBlock</code>.  This method should only be used
     * when the final result returned is a <code>Collection</code>.
     * (This includes <code>com.lightcrafts.mediax.jai.CollectionOp</code>s.)  The
     * source(s) supplied may be a collection of renderable images or a
     * collection of collections that at the very basic level include
     * renderable images.
     * The source(s) supplied are unwrapped to create a single collection
     * that contains RenderableOps and collections as many as the size of
     * the smallest collection supplied in the sources. The nth collection
     * is created using all supplied renderable images and the nth element of
     * each of the collections supplied in the source.
     *
     * <p> This method should be used to create a <code>Collection</code>
     * in the renderable image mode.
     *
     * <p> The supplied operation name is validated against the
     * operation registry.  The source(s) and/or parameter(s) in the
     * <code>ParameterBlock</code> are validated against the named
     * operation's descriptor, both in their numbers and types.
     * Additional restrictions placed on the sources and parameters
     * by an individual operation are also validated by calling its
     * <code>OperationDescriptor.validateRenderableArguments()</code>
     * method.
     *
     * <p><code>JAI</code>allows a parameter to have a <code>null</code> input
     * value, if that particular parameter has a default value specified
     * in its operation's descriptor.  In this case, the default value
     * will replace the <code>null</code> input.
     *
     * <p><code>JAI</code>also allows unspecified tailing parameters, if these
     * parameters have default values specified in the operation's
     * descriptor. However, if a parameter, which
     * has a default value, is followed by one or more parameters that
     * have no default values, this parameter must be specified in the
     * <code>ParameterBlock</code>, even if it only has a value of
     * code>null</code>.
     *
     * <p> This version of <code>createRenderableCollection</code> is
     * non-static; it may be used with a specific instance of the JAI
     * class.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the current operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the current operation registry does not
     *         support renderable image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.renderable.RenderableImage</code> or a
     *         <code>com.lightcrafts.mediax.jai.CollectionImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>Collection</code> that represents the named
     *          operation.
     *
     * @deprecated as of JAI 1.1 in favor of
     * <code>createRenderableCollectionNS(String,ParameterBlock,RenderingHints)</code>.
     * @see JAI#createRenderableCollectionNS(String,ParameterBlock,RenderingHints)
     */
    public Collection createRenderableCollectionNS(String opName,
                                                   ParameterBlock args) {
        return createRenderableCollectionNS(opName, args, null);
    }

    /**
     * Creates a <code>Collection</code> which represents the named
     * operation, using the source(s) and/or parameter(s) specified in
     * the <code>ParameterBlock</code>.  This method should only be used
     * when the final result returned is a <code>Collection</code>.
     * (This includes <code>com.lightcrafts.mediax.jai.CollectionOp</code>s.)  The
     * source(s) supplied may be a collection of renderable images or a
     * collection of collections that at the very basic level include
     * renderable images.
     * The source(s) supplied are unwrapped to create a single collection
     * that contains RenderableOps and collections as many as the size of
     * the smallest collection supplied in the sources. The nth collection
     * is created using all supplied renderable images and the nth element of
     * each of the collections supplied in the source.
     *
     * <p> This method should be used to create a <code>Collection</code>
     * in the renderable image mode.
     *
     * <p> The supplied operation name is validated against the
     * operation registry.  The source(s) and/or parameter(s) in the
     * <code>ParameterBlock</code> are validated against the named
     * operation's descriptor, both in their numbers and types.
     * Additional restrictions placed on the sources and parameters
     * by an individual operation are also validated by calling its
     * <code>OperationDescriptor.validateRenderableArguments()</code>
     * method.
     *
     * <p><code>JAI</code>allows a parameter to have a <code>null</code> input
     * value, if that particular parameter has a default value specified
     * in its operation's descriptor.  In this case, the default value
     * will replace the <code>null</code> input.
     *
     * <p><code>JAI</code>also allows unspecified tailing parameters, if these
     * parameters have default values specified in the operation's
     * descriptor. However, if a parameter, which
     * has a default value, is followed by one or more parameters that
     * have no default values, this parameter must be specified in the
     * <code>ParameterBlock</code>, even if it only has a value of
     * code>null</code>.
     *
     * <p> The rendering hints associated with this instance of
     * <code>JAI</code> are overlaid with the hints passed to this
     * method.  That is, the set of keys will be the union of the
     * keys from the instance's hints and the hints parameter.
     * If the same key exists in both places, the value from the
     * hints parameter will be used.
     *
     * <p> This version of <code>createRenderableCollection</code> is
     * non-static; it may be used with a specific instance of the JAI
     * class.
     *
     * @param opName  The name of the operation.
     * @param args  The source(s) and/or parameter(s) for the operation.
     * @param hints  The hints for the operation.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if no
     *         <code>OperationDescriptor</code> is registered under the
     *         specified operation name in the current operation registry.
     * @throws IllegalArgumentException if the
     *         <code>OperationDescriptor</code> registered under the specified
     *         operation name in the current operation registry does not
     *         support renderable image mode.
     * @throws IllegalArgumentException if the specified operation does
     *         not produce a
     *         <code>java.awt.image.renderable.RenderableImage</code> or a
     *         <code>com.lightcrafts.mediax.jai.CollectionImage</code>.
     * @throws IllegalArgumentException if the specified operation is
     *         unable to handle the sources and parameters specified in
     *         <code>args</code>.
     *
     * @return  A <code>Collection</code> that represents the named
     *          operation.
     *
     * @since JAI 1.1
     */
    public Collection createRenderableCollectionNS(String opName,
                                                   ParameterBlock args,
                                                   RenderingHints hints) {
        if(opName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI14"));
        } else if (args == null) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI15"));
        }

	String modeName = "renderableCollection";

        // Get the OperationDescriptor registered under the specified name.
        OperationDescriptor odesc = (OperationDescriptor)
            operationRegistry.getDescriptor(modeName, opName);

        if (odesc == null) {
            throw new IllegalArgumentException(opName + ": " +
                                               JaiI18N.getString("JAI0"));
        }

	Class destClass = odesc.getDestClass(modeName);

	if (!RenderableImage.class.isAssignableFrom(destClass) &&
	    !CollectionImage.class.isAssignableFrom(destClass)) {
	  throw new IllegalArgumentException(opName + ": " +
					     JaiI18N.getString("JAI6"));
        }


         // Validate input arguments. The ParameterBlock is cloned here
         // because OperationDescriptor.validateRenderableArguments()
         // may change its content.

        StringBuffer msg = new StringBuffer();
        args = (ParameterBlock)args.clone();
        RenderingHints mergedHints =
            mergeRenderingHints(renderingHints, hints);
	if (odesc.validateArguments(modeName, args, msg)) {
            if (RenderableImage.class.isAssignableFrom(destClass)) {
                Vector v = new Vector(1);
                RenderableOp op =
                    new RenderableOp(operationRegistry, opName, args,
                                     mergedHints);
                v.add(op);
                return v;
            } else {
                CollectionOp cOp = new
                    CollectionOp(operationRegistry, opName, args,
                                 mergedHints, true);

                // If the operation requests immediate rendering, do so.
                if (odesc.isImmediate()) {
                    Collection coll = null;
                    coll = cOp.getCollection();
                    if(coll == null) {
                        return null;
                    }
                }

                return cOp;
            }

	} else {
            int numSources = odesc.getNumSources();
            Vector sources = args.getSources();


             // Get the iterator of all the sources that are collection.
             // Get the iterator for the collection that has the least elements.

            Iterator[] iters = new Iterator[numSources];
            Iterator iter = null;
            int size = Integer.MAX_VALUE;
            for (int i = 0; i < numSources; i++) {
                Object s = sources.elementAt(i);
                if (s instanceof Collection) {
                    iters[i] = ((Collection)s).iterator();
                    if (iter == null || ((Collection)s).size() < size) {
                        iter = iters[i];
                        size = ((Collection)s).size();
                    }
                }
            }

            if (iter == null) {

	      // None of the sources is a collection. The error is
              // with the input arguments.

                throw new IllegalArgumentException(msg.toString());
            }

            // Some sources are of type collection. Need to unwrap them.
            Collection col = null;
            for (int i = 0; i < numSources; i++) {
                Object s = sources.elementAt(i);
                if (s instanceof Collection) {
                    try {
                        col = (Collection)s.getClass().newInstance();
                        break;
                    } catch (Exception e) {
		      // Unable to create this collection type, try next.
                        sendExceptionToListener(
                            JaiI18N.getString("JAI16") + s.getClass().getName(),
                            e);
                    }
                }
            }
            if (col == null) {
                col = new Vector();
            }

            // Get the source types.
            Class[] sourceClasses = odesc.getSourceClasses(modeName);

            while (iter.hasNext()) {
                ParameterBlock pb = new ParameterBlock();
                pb.setParameters(args.getParameters());

                for (int i = 0; i < numSources; i++) {
                    // Get the next source.
                    Object nextSource = null;
                    if (iters[i] == null) {
                        nextSource = sources.elementAt(i);
                    } else {
                        nextSource = iters[i].next();
                    }

                    // If the source is not of a compatible type and
                    // is not a Collection then the 'false' value
                    // returned by validateArguments() above must indicate
                    // a real error.
                    if(!sourceClasses[i].isAssignableFrom(nextSource.getClass()) &&
                       !(nextSource instanceof Collection)) {
                        throw new IllegalArgumentException(msg.toString());
                    }
                    pb.addSource(nextSource);
                }

                Collection c = createRenderableCollectionNS(opName, pb,
                                                            mergedHints);
                if (c instanceof Vector &&
                    c.size() == 1 &&
                    ((Vector)c).elementAt(0) instanceof RenderableOp) {
                    col.add(((Vector)c).elementAt(0));
                } else {
                    col.add(c);
                }
            }

            return col;
        }
    }

  // Rendering hints.

    /** An inner class defining rendering hint keys. */
    static class RenderingKey extends RenderingHints.Key {
	//cache the class of JAI to keep JAI.class in memory unless
	//the class RenderingKey is GC'ed.  In this case, the 
	// WeakReferences in the map of RenderingHints.Key will release
	// the instances of RenderingKey.  So when JAI is loaded next
	// time, the keys can be recreated without any exception.
	// Fix bug: 4754807
	private static Class JAIclass = JAI.class;

        private Class objectClass;

        RenderingKey(int privateKey, Class objectClass) {
            super(privateKey);
            this.objectClass = objectClass;
        }

        public boolean isCompatibleValue(Object val) {
            return objectClass.isInstance(val);
        }
    }

    /**
     * Returns the <code>RenderingHints</code> associated with this
     * <code>JAI</code> instance.  These rendering hints will be
     * merged with any hints supplied as an argument to the
     * <code>createNS()</code>, <code>createRenderableNS()</code>,
     * or <code>createCollectionNS()</code> methods.
     */
    public RenderingHints getRenderingHints() {
        return renderingHints;
    }

    /**
     * Sets the <code>RenderingHints</code> associated with this
     * <code>JAI</code> instance.  These rendering hints will be
     * merged with any hints supplied as an argument to the
     * <code>createNS()</code>, <code>createRenderableNS()</code>,
     * or <code>createCollectionNS()</code> methods.
     *
     * <p> The <code>hints</code> argument must be non-null, otherwise
     * a <code>IllegalArgumentException</code> will be thrown.
     */
    public void setRenderingHints(RenderingHints hints) {
        if (hints == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        this.renderingHints = hints;
    }

    /**
     * Clears the <code>RenderingHints</code> associated with this
     * <code>JAI</code> instance.
     */
    public void clearRenderingHints() {
        this.renderingHints = new RenderingHints(null);
    }

    /**
     * Returns the hint value associated with a given key
     * in this <code>JAI</code> instance, or <code>null</code>
     * if no value is associated with the given key.
     *
     * @throws IllegalArgumentException if <code>key</code> is
     *         <code>null</code>.
     */
    public Object getRenderingHint(RenderingHints.Key key) {
        if (key == null) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI7"));
        }
        return renderingHints.get(key);
    }

    /**
     * Sets the hint value associated with a given key
     * in this <code>JAI</code> instance.
     *
     * @throws IllegalArgumentException if <code>key</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>value</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>value</code> is
     *         not of the correct type for the given hint.
     */
    public void setRenderingHint(RenderingHints.Key key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI7"));
        }
        if (value == null) {
            throw new IllegalArgumentException(JaiI18N.getString("JAI9"));
        }
        try {
            renderingHints.put(key, value);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    /**
     * Removes the hint value associated with a given key
     * in this <code>JAI</code> instance.
     */
    public void removeRenderingHint(RenderingHints.Key key) {
        renderingHints.remove(key);
    }

    /**
     * Sets an <code>ImagingListener</code> object on this
     * <code>JAI</code>.
     *
     * @param imagingListener The <code>ImagingListener</code> to be used.  If
     *                        the provided <code>ImagingListener</code> is
     *                        <code>null</code>, the default
     *                        <code>ImagingListener</code>, which rethrows the
     *                        <code>RuntimeException</code>s and prints
     *                        the stack trace of the other types to the stream
     *                        <code>System.err</code>, will be set.
     */
    public void setImagingListener(ImagingListener listener) {
        if (listener == null)
            listener = ImagingListenerImpl.getInstance();
        this.renderingHints.put(KEY_IMAGING_LISTENER, listener);
        this.imagingListener = listener;
    }

    /**
     * Gets the <code>ImagingListener</code> object from this
     * <code>JAI</code>.
     *
     * @return The <code>ImagingListener</code> object that currently
     * resides in this <code>JAI</code>.
     */
    public ImagingListener getImagingListener() {
        return imagingListener;
    }

    private void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener = getImagingListener();
        listener.errorOccurred(message, e, this, false);
    }
}
