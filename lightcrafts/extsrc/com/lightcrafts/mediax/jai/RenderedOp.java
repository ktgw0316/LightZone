/*
 * $RCSfile: RenderedOp.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2006/06/16 22:52:04 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.PropertyUtil;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import com.lightcrafts.mediax.jai.registry.RIFRegistry;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;
import com.lightcrafts.mediax.jai.remote.PlanarImageServerProxy;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;
import com.lightcrafts.mediax.jai.util.ImagingListener;

/**
 * A node in a rendered imaging chain.  A <code>RenderedOp</code> stores
 * an operation name, a <code>ParameterBlock</code> containing sources and
 * parameters, and a <code>RenderingHints</code> containing hints which
 * may be used in rendering the node.  A set of nodes may be joined together
 * via the source <code>Vector</code>s within their respective
 * <code>ParameterBlock</code>s to form a <u>d</u>irected <u>a</u>cyclic
 * <u>g</u>raph (DAG).  The topology, i.e., connectivity, of the graph may be
 * altered by changing the node's sources.  The operation name, parameters,
 * and rendering hints may also be changed.
 *
 * <p> Such chains are useful for example as arguments to a
 * <code>RemoteImage</code>; they convey the structure of an imaging
 * chain in a compact representation and at a suitably high level of
 * abstraction to allow the server some leeway in materializing the
 * results.  They are also useful in that a chain may be manipulated
 * dynamically and rendered multiple times.  Thus for example the same
 * chain of operations may be applied to different images or the parameters
 * of certain operations in a chain may be modified interactively.
 *
 * <p> A <code>RenderedOp</code> may be constructed directly as, for example,
 * <pre>
 * <code>
 * ParameterBlock pb =
 *     (new ParameterBlock()).add("SomeFile.tif");
 * RenderedOp node = new RenderedOp("fileload", pb, null);
 * </code>
 * </pre>
 * or via the <code>create</code> or <code>createNS()</code> methods defined
 * in the <code>JAI</code> class.  The difference between direct construction
 * of a node and creation via a convenience method is that in the latter case:
 *
 * <ol>
 * <li> It is verified that the operation supports the rendered mode.</li>
 * <li> Using the <code>validateArguments()</code> method of the associated
 *      <code>OperationDescriptor</code>, the arguments (sources and parameters)
 *      are validated as being compatible with the specified operation.</li>
 * <li> Global <code>RenderingHints</code> maintained by the <code>JAI</code>
 *      instance are merged with the local <code>RenderingHints</code> with the
 *      local hints taking precedence.</li>
 * <li> If the operation is defined to be "immediate" (the
 *      <code>isImmediate()</code> method of the corresponding
 *      <code>OperationDescriptor</code> returns <code>true</code>)
 *      then the node is rendered.</li>
 * </ol>
 *
 * <p> When a chain of nodes is rendered by any means a "parallel" chain of
 * <code>RenderedImage</code>s is created.  Each node in the chain of
 * <code>RenderedOp</code>s corresponds to a node in the chain of
 * <code>RenderedImage</code>s.  <code>RenderedImage</code> methods invoked
 * on the <code>RenderedOp</code> are in general forwarded to the associated
 * <code>RenderedImage</code> which is referred to as the <i>rendering</i>
 * of the node.
 *
 * <p> The translation between <code>RenderedOp</code> chains and
 * <code>RenderedImage</code> (usually <code>OpImage</code>) chains makes
 * use of two levels of indirection provided by the
 * <code>OperationRegistry</code> and <code>RenderedImageFactory</code>
 * (RIF) facilities.  First, the local <code>OperationRegistry</code> is
 * used to map the operation name into a RIF.  This RIF then constructs
 * a <code>RenderedImage</code> (usually an <code>OpImage</code>) which
 * does the actual image data processing.  The local
 * <code>OperationRegistry</code> is used in order to take advantage
 * of the best possible implementation of the operation, e.g., RIFs that
 * provide acceleration for certain cases or RIFs that are known to a server
 * without having to burden the client.
 *
 * <p> A node may be rendered explicitly by invoking the method
 * <code>getRendering()</code> which also returns the rendering of the
 * node.  A node may be rendered implicitly by invoking any method
 * defined in the <code>RenderedImage</code> interface.  A node may also be
 * rendered implicitly by invoking any method the execution of which
 * <ul>
 * <li> requires some dimensional quantity of the image such as its
 * bounds or tile layout;</li>
 * <li> retrieves image data by any means;</li>
 * </ul>
 * The current rendering may be obtained without forcing the rendering of
 * an unrendered node via the method <code>getCurrentRendering()</code>.
 * A node may also be re-rendered via <code>getNewRendering()</code> which
 * regenerates the rendering from the existing set of sources, parameters,
 * and hints.
 *
 * <p> A rendering of a node may also be obtained by means of the
 * <code>createInstance()</code> method.  This method returns a
 * <code>PlanarImage</code> rendering without marking the node as
 * having been rendered.  If the node is not marked as rendered then it
 * will not fire <code>RenderingChangeEvent</code>s as described below.
 *
 * <p> <code>RenderedOp</code> nodes may participate in Java Bean-style
 * events.  The <code>PropertyChangeEmitter</code> methods may be used
 * to register and unregister <code>PropertyChangeListener</code>s.
 * <code>RenderedOp</code>s are also <code>PropertyChangeListener</code>s
 * so that they may be registered as listeners of other
 * <code>PropertyChangeEmitter</code>s or the equivalent.  Each
 * <code>RenderedOp</code> also automatically receives any
 * <code>RenderingChangeEvent</code>s emitted by any of its sources which
 * are also <code>RenderedOp</code>s or any <code>CollectionChangeEvent</code>s
 * from any <code>CollectionOp</code> sources.
 *
 * <p> Certain <code>PropertyChangeEvent</code>s may be emitted by the
 * <code>RenderedOp</code>.  These include the
 * <code>PropertyChangeEventJAI</code>s and
 * <code>PropertySourceChangeEvent</code>s required by virtue of implementing
 * the <code>OperationNode</code> interface.  Additionally a
 * <code>RenderingChangeEvent</code> may be emitted if the node has already
 * been rendered and both of the following conditions are satisfied:
 * <ol>
 * <li>A. any of the critical attributes is changed (edited), i.e., the
 * operation name, operation registry, node sources, parameters, or rendering
 * hints; or
 * <br>B. the node receives a <code>RenderingChangeEvent</code> from one of
 * its <code>RenderedOp</code> sources or a <code>CollectionChangeEvent</code>
 * from one of its <code>CollectionOp</code> sources.</li>
 * <li>the old and new renderings differ over some non-empty region.</li>
 * </ol>
 *
 * <p> When a rendered <code>RenderedOp</code> node receives a
 * <code>RenderingChangeEvent</code> from a <code>RenderedOp</code> source,
 * then if the rendering is an <code>OpImage</code>, the region of
 * the current rendering which may be retained will be determined by using
 * <code>mapSourceRect()</code> to forward map the bounds of the invalid
 * region.  A similar procedure is used for "InvalidRegion" events emitted
 * by source <code>RenderedImage</code>s such as <code>TiledImage</code>s.
 * If a critical attribute of the node is edited, then the
 * <code>getInvalidRegion()</code> method of the corresponding
 * <code>OperationDescriptor</code> will be used to determine the
 * invalid region.  If the complement of the invalid region contains any tiles
 * of the current rendering and the rendering is an <code>OpImage</code>, a
 * new rendering of the node will be generated and the
 * identified tiles will be retained from the old rendering insofar as
 * possible.  This might involve for example adding tiles to a
 * <code>TileCache</code> under the ownership of the new rendering.  A
 * <code>RenderingChangeEvent</code> will then be fired to all
 * <code>PropertyChangeListener</code>s of the node, and to any sinks that
 * are <code>PropertyChangeListener</code>s.  The <code>newRendering</code>
 * parameter of the event constructor (which may be retrieved via the
 * <code>getNewValue()</code> method of the event) will be set to either
 * the new rendering of the node or to <code>null</code> if it was not
 * possible to retain any tiles of the previous rendering.
 *
 * <p> <code>RenderedOp</code> nodes are <code>WritablePropertySource</code>s
 * and so manage a name-value database of image meta-data also known as image
 * properties.  Properties may be set on and requested from a node.  The
 * value of a property not explicitly set on the node (via
 * <code>setProperty()</code>) is obtained from the property environment of
 * the node.  When a property is derived from the property environment it is
 * cached locally to ensure synchronization, i.e., that properties do not
 * change spontaneously if for example the same property is modified upstream.
 *
 * <p> The property environment of a <code>RenderedOp</code> is initially
 * derived from that of the corresponding <code>OperationDescriptor</code>
 * as maintained by the <code>OperationRegistry</code>.  It may be modified
 * locally by adding <code>PropertyGenerator</code>s, directives to copy
 * certain properties from specific sources, or requests to suppress certain
 * properties.  These modifications per se cannot be undone directly but
 * may be eliminated as a side effect of other changes to the node as
 * described below.
 *
 * <p> The <code>RenderedOp</code> itself synthesizes several property values,
 * which may neither be set nor removed.  These are: <code>image_width</code>,
 * <code>image_height</code>, <code>image_min_x_coord</code>,
 * <code>image_min_y_coord</code>, <code>tile_cache</code> and
 * <code>tile_cache_key</code>.  These properties are referred to as
 * <i>synthetic properties</i>.  The property <code>tile_cache_key</code>
 * has a value of type {@link TileCache} which indicates where the tiles
 * of the rendering are cached, if anywhere.  The value of the property
 * <code>tile_cache_key</code> is a {@link RenderedImage} by which the
 * cached tiles are referenced in the indicated cache.  If the rendering
 * is of type {@link OpImage} or
 * {@link com.lightcrafts.mediax.jai.remote.PlanarImageServerProxy} then the value of
 * <code>tile_cache_key</code> will be set to the rendering itself and the
 * value of <code>tile_cache</code> to the value returned by invoking
 * <code>getTileCache()</code> on the rendering.  Otherwise these properties
 * will be set to the same values as the properties of the same names set
 * on the rendering.  It is legal for these properties to have the value
 * <code>java.awt.Image.UndefinedProperty</code>.
 *
 * <p> When a property value is requested an attempt will be made to derive
 * it from the several entities in the following order of precedence:
 * <ol>
 * <li> synthetic properties; </li>
 * <li> local properties; </li>
 * <li> the rendering of the node; </li>
 * <li> any registered <code>PropertyGenerator</code>s, or
 * <br> a source specified via a copy-from-source directive;</li>
 * <li> the first node source which defines the property. </li>
 * </ol>
 * Local properties are those which have been cached locally either by virtue
 * of direct invocation of <code>setProperty()</code> or due to caching of a
 * property derived from the property environment.  Note that the properties
 * of a node are not copied to its rendering.
 *
 * <p> All dynamically computed properties of a <code>RenderedOp</code> which
 * have been cached locally, i.e., those cached properties which were not set
 * by an explicit call to <code>setProperty()</code>, will be cleared when any
 * of the critical attributes of the node is edited.  By implication these
 * properties will also be cleared when a <code>RenderingChangeEvent</code>
 * is received from any node source.  The property environment or the cached
 * properties may also be cleared by invoking <code>resetProperties()</code>.
 *
 * <p> As mentioned, a <code>RenderedOp</code> chain created on a client
 * may be passed to a server via a <code>RemoteImage</code>.  Whether the
 * node has been previously rendered is irrelevant to its ability to be
 * serialized.  Any <code>RenderedImage</code> sources which are not
 * <code>Serializable</code> will be wrapped in
 * <code>SerializableRenderedImage</code>s for serialization.  The tile
 * transmission parameters will be determined from the
 * <code>RenderingHints</code> of the node.  All other non-serializable
 * objects will attempt to be serialized using
 * <code>SerializerFactory</code>.  If no <code>Serializer</code> is
 * available for a particular object, a
 * <code>java.io.NotSerializableException</code> may result.  Image
 * properties (meta-data) are serialized insofar as they are serializable:
 * non-serializable components are simply eliminated from the local cache
 * of properties and from the property environment.
 *
 * <p> Note that <code>RenderedOp</code> nodes used to instantiate
 * operations which have a corresponding <code>OperationDescriptor</code>
 * the <code>isImmediate()</code> method of which returns
 * <code>true</code> are rendered upon deserialization.
 *
 * <p> <code>RenderedOp</code> represents a single <code>PlanarImage</code>
 * as a node in a <code>RenderedImage</code> operation chain.  Its companion
 * classes, <code>RenderableOp</code> and <code>CollectionOp</code>, represent
 * nodes in operation chains of <code>RenderableImage</code>s and
 * <code>CollectionImage</code>s, respectively.
 *
 *
 * @see CollectionOp
 * @see JAI
 * @see OperationDescriptor
 * @see OperationRegistry
 * @see OpImage
 * @see RenderableOp
 * @see RenderingChangeEvent
 * @see com.lightcrafts.mediax.jai.remote.SerializableRenderedImage
 * @see com.lightcrafts.mediax.jai.remote.Serializer
 * @see com.lightcrafts.mediax.jai.remote.SerializerFactory
 * @see java.awt.RenderingHints
 * @see java.awt.image.renderable.ParameterBlock
 * @see java.awt.image.renderable.RenderedImageFactory
 *
 */
public class RenderedOp extends PlanarImage
    implements OperationNode, PropertyChangeListener, Serializable {

    /**
     * An object to assist in implementing <code>OperationNode</code>.
     *
     * @since JAI 1.1
     */
    protected OperationNodeSupport nodeSupport;

    /**
     * The <code>PropertySource</code> containing the combined properties
     * of all of the node's sources.
     */
    protected transient PropertySource thePropertySource;

    /** The rendering of the current image, not preserved over RMI. */
    protected transient PlanarImage theImage;

    /**
     * The RenderingHints when the node was last rendered, i.e., when
     * "theImage" was set to its current value.
     */
    private transient RenderingHints oldHints;

    /** Names of synthesized properties. */
    // XXX Synthetic properties should never be inherited. This might imply
    // a need for setting non-inheritable in addition to suppressed properties.
    private static List synthProps;

    /** Synthesized properties. */
    private Hashtable synthProperties = null;

    /** Node event names. */
    private static Set nodeEventNames = null;

    /**
     * Whether dispose() has been invoked.
     */
    private boolean isDisposed = false;

    static {
        CaselessStringKey[] propKeys =
            new CaselessStringKey[] {
                new CaselessStringKey("image_width"),
                new CaselessStringKey("image_height"),
                new CaselessStringKey("image_min_x_coord"),
                new CaselessStringKey("image_min_y_coord"),
                new CaselessStringKey("tile_cache"),
                new CaselessStringKey("tile_cache_key")
                    };
        synthProps = Arrays.asList(propKeys);

        nodeEventNames = new HashSet();
        nodeEventNames.add("operationname");
        nodeEventNames.add("operationregistry");
        nodeEventNames.add("parameterblock");
        nodeEventNames.add("sources");
        nodeEventNames.add("parameters");
        nodeEventNames.add("renderinghints");
    }

    /**
     * Constructs a <code>RenderedOp</code> that will be used to
     * instantiate a particular rendered operation from the specified
     * operation registry, an operation name, a <code>ParameterBlock</code>,
     * and a set of rendering hints.
     *
     * <p> This method does not validate the contents of the supplied
     * <code>ParameterBlock</code>.  The caller should ensure that
     * the sources and parameters in the <code>ParameterBlock</code>
     * are suitable for the operation this node represents; otherwise
     * some form of error or exception may occur at the time of rendering.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when the node is
     * rendered.
     *
     * <p> The node is added automatically as a sink of any
     * <code>PlanarImage</code> or <code>CollectionImage</code> sources.
     *
     * @param registry  The <code>OperationRegistry</code> to be used for
     *        instantiation.  if <code>null</code>, the default registry
     *        is used.  Saved by reference.
     * @param opName  The operation name.  Saved by reference.
     * @param pb  The sources and parameters. If <code>null</code>,
     *        it is assumed that this node has no sources and parameters.
     *        This parameter is cloned.
     * @param hints  The rendering hints.  If <code>null</code>, it is assumed
     *        that no hints are associated with the rendering.
     *        This parameter is cloned.
     *
     * @throws IllegalArgumentException if <code>opName</code> is
     *         <code>null</code>.
     */
    public RenderedOp(OperationRegistry registry,
                      String opName,
                      ParameterBlock pb,
                      RenderingHints hints) {
        super(new ImageLayout(), null, null);

        if(pb == null) {
            // Ensure that the PB is non-null.
            pb = new ParameterBlock();
        } else {
            // Clone the PB per the doc.
            pb = (ParameterBlock)pb.clone();
        }

        if(hints != null) {
            // Clone the hints per the doc.
            hints = (RenderingHints)hints.clone();
        }

        nodeSupport =
            new OperationNodeSupport(getRegistryModeName(),
                                     opName,
                                     registry,
                                     pb,
                                     hints,
                                     eventManager);

        // Add the node as a PropertyChangeListener of itself for
        // the critical attributes of the node.  Case is ignored
        // in the property names but infix caps are used here anyway.
        addPropertyChangeListener("OperationName", this);
        addPropertyChangeListener("OperationRegistry", this);
        addPropertyChangeListener("ParameterBlock", this);
        addPropertyChangeListener("Sources", this);
        addPropertyChangeListener("Parameters", this);
        addPropertyChangeListener("RenderingHints", this);

        // Add self as a sink of any PlanarImage or CollectionImage sources.
        Vector nodeSources = pb.getSources();
        if(nodeSources != null) {
            Iterator it = nodeSources.iterator();
            while(it.hasNext()) {
                Object src = it.next();
                if(src instanceof PlanarImage) {
                    ((PlanarImage)src).addSink(this);
                } else if(src instanceof CollectionImage) {
                    ((CollectionImage)src).addSink(this);
                }
            }
        }
    }

    /**
     * Constructs a <code>RenderedOp</code> that will be used to
     * instantiate a particular rendered operation from the default
     * operation registry, an operation name, a <code>ParameterBlock</code>,
     * and a set of rendering hints.  The default operation registry
     * is used.
     *
     * <p> This method does not validate the contents of the supplied
     * <code>ParameterBlock</code>.  The caller should ensure that
     * the sources and parameters in the <code>ParameterBlock</code>
     * are suitable for the operation this node represents; otherwise
     * some form of error or exception may occur at the time of rendering.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when the node is
     * rendered.
     *
     * <p> The node is added automatically as a sink of any
     * <code>PlanarImage</code> or <code>CollectionImage</code> sources.
     *
     * @param opName  The operation name.  Saved by reference.
     * @param pb  The sources and parameters. If <code>null</code>,
     *        it is assumed that this node has no sources and parameters.
     *        This parameter is cloned.
     * @param hints  The rendering hints.  If <code>null</code>, it is assumed
     *        that no hints are associated with the rendering.
     *        This parameter is cloned.
     *
     * @throws IllegalArgumentException if <code>opName</code> is
     *         <code>null</code>.
     */
    public RenderedOp(String opName,
                      ParameterBlock pb,
                      RenderingHints hints) {
	this(null, opName, pb, hints);
    }

    /**
     * A <code>TileComputationListener</code> to pass to the
     * <code>scheduleTiles()</code> method of the rendering to intercept
     * method calls such that the image reference is this
     * <code>RenderedOp</code>.
     */
    private class TCL implements TileComputationListener {
        RenderedOp node;

        private TCL(RenderedOp node) {
            this.node = node;
        }

        public void tileComputed(Object eventSource,
                                 TileRequest[] requests,
                                 PlanarImage image, int tileX, int tileY,
                                 Raster tile) {
            if(image == theImage) {
                // Forward call to all listeners.
                TileComputationListener[] listeners =
                    getTileComputationListeners();

                if(listeners != null) {
                    int numListeners = listeners.length;

                    for(int i = 0; i < numListeners; i++) {
                        listeners[i].tileComputed(node, requests, image,
                                                  tileX, tileY, tile);
                    }
                }
            }
        }

        public void tileCancelled(Object eventSource,
                                  TileRequest[] requests,
                                  PlanarImage image, int tileX, int tileY) {
            if(image == theImage) {
                // Forward call to all listeners.
                TileComputationListener[] listeners =
                    getTileComputationListeners();

                if(listeners != null) {
                    int numListeners = listeners.length;

                    for(int i = 0; i < numListeners; i++) {
                        listeners[i].tileCancelled(node, requests, image,
                                                   tileX, tileY);
                    }
                }
            }
        }

        public void tileComputationFailure(Object eventSource,
                                           TileRequest[] requests,
                                           PlanarImage image,
                                           int tileX, int tileY,
                                           Throwable situation) {
            if(image == theImage) {
                // Forward call to all listeners.
                TileComputationListener[] listeners =
                    getTileComputationListeners();

                if(listeners != null) {
                    int numListeners = listeners.length;

                    for(int i = 0; i < numListeners; i++) {
                        listeners[i].tileComputationFailure(node, requests,
                                                            image, tileX, tileY,
                                                            situation);
                    }
                }
            }
        }
    }

    /**
     * Returns the name of the <code>RegistryMode</code> corresponding to
     * this <code>RenderedOp</code>.  This method always returns the
     * <code>String</code> "rendered".
     *
     * @since JAI 1.1
     */
    public String getRegistryModeName() {
        return RegistryMode.getMode("rendered").getName();
    }

    /* ----- Critical attribute main accessors and mutators. ----- */

    /**
     * Returns the <code>OperationRegistry</code> that is used
     * by this node.  If the registry is not set, the default
     * registry is returned.
     */
    public synchronized OperationRegistry getRegistry() {
        return nodeSupport.getRegistry();
    }

    /**
     * Sets the <code>OperationRegistry</code> that is used by
     * this node.  If the specified registry is <code>null</code>, the
     * default registry is used.  The parameter is saved by reference.
     *
     * <p> If the supplied registry does not equal the current registry, a
     * <code>PropertyChangeEventJAI</code> named "OperationRegistry"
     * will be fired and a <code>RenderingChangeEvent</code> may be
     * fired if the node has already been rendered.
     *
     * @param registry  The new <code>OperationRegistry</code> to be set;
     *        it may be <code>null</code>.
     */
    public synchronized void setRegistry(OperationRegistry registry) {
        nodeSupport.setRegistry(registry);
    }

    /**
     * Returns the name of the operation this node represents as
     * a <code>String</code>.
     */
    public synchronized String getOperationName() {
        return nodeSupport.getOperationName();
    }

    /**
     * Sets the name of the operation this node represents.
     * The parameter is saved by reference.
     *
     * <p> If the supplied name does not equal the current operation name, a
     * <code>PropertyChangeEventJAI</code> named "OperationName"
     * will be fired and a <code>RenderingChangeEvent</code> may be
     * fired if the node has already been rendered.
     *
     * @param opName  The new operation name to be set.
     *
     * @throws IllegalArgumentException if <code>opName</code> is
     *         <code>null</code>.
     */
    public synchronized void setOperationName(String opName) {
        nodeSupport.setOperationName(opName);
    }

    /** Returns a clone of the <code>ParameterBlock</code> of this node. */
    public synchronized ParameterBlock getParameterBlock() {
        return (ParameterBlock)nodeSupport.getParameterBlock().clone();
    }

    /**
     * Sets the <code>ParameterBlock</code> of this node.
     * If the specified new <code>ParameterBlock</code> is <code>null</code>,
     * it is assumed that this node has no input sources and parameters.
     * The supplied parameter is cloned.
     *
     * <p> This method does not validate the content of the supplied
     * <code>ParameterBlock</code>.  The caller should ensure that
     * the sources and parameters in the <code>ParameterBlock</code>
     * are suitable for the operation this node represents; otherwise
     * some form of error or exception may occur at the time of rendering.
     *
     * <p> If the supplied <code>ParameterBlock</code> does not equal the
     * current <code>ParameterBlock</code>, a
     * <code>PropertyChangeEventJAI</code> named "ParameterBlock", "Sources",
     * or "Parameters" will be fired. A <code>RenderingChangeEvent</code>
     * may also be fired if the node has already been rendered.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when the node is
     * rendered.
     *
     * <p> The node is registered as a sink of any <code>PlanarImage</code>
     * or <code>CollectionImage</code> sources contained in the supplied
     * <code>ParameterBlock</code>.  The node is also removed as a sink of
     * any previous <code>PlanarImage</code> or <code>CollectionImage</code>
     * sources if these are not in the new <code>ParameterBlock</code>.
     *
     * @param pb  The new <code>ParameterBlock</code> to be set;
     *        it may be <code>null</code>.
     */
    public synchronized void setParameterBlock(ParameterBlock pb) {
        Vector nodeSources = nodeSupport.getParameterBlock().getSources();
        if(nodeSources != null && nodeSources.size() > 0) {
            Iterator it = nodeSources.iterator();
            while(it.hasNext()) {
                Object src = it.next();
                if(src instanceof PlanarImage) {
                    ((PlanarImage)src).removeSink(this);
                } else if(src instanceof CollectionImage) {
                    ((CollectionImage)src).removeSink(this);
                }
            }
        }

        if(pb != null) {
            Vector newSources = pb.getSources();;
            if(newSources != null && newSources.size() > 0) {
                Iterator it = newSources.iterator();
                while(it.hasNext()) {
                    Object src = it.next();
                    if(src instanceof PlanarImage) {
                        ((PlanarImage)src).addSink(this);
                    } else if(src instanceof CollectionImage) {
                        ((CollectionImage)src).addSink(this);
                    }
                }
            }
        }

        nodeSupport.setParameterBlock(pb == null ?
                                      new ParameterBlock() :
                                      (ParameterBlock)pb.clone());
    }

    /**
     * Returns a clone of the <code>RenderingHints</code> of this node or
     * <code>null</code>.
     */
    public RenderingHints getRenderingHints() {
        RenderingHints hints = nodeSupport.getRenderingHints();
        return hints == null ? null : (RenderingHints)hints.clone();
    }

    /**
     * Sets the <code>RenderingHints</code> of this node.
     * The supplied parameter is cloned if non-<code>null</code>.
     *
     * <p> If the supplied <code>RenderingHints</code> does not equal the
     * current <code>RenderingHints</code>, a
     * <code>PropertyChangeEventJAI</code> named "RenderingHints"
     * will be fired and a <code>RenderingChangeEvent</code> may be
     * fired if the node has already been rendered.
     *
     * @param hints The new <code>RenderingHints</code> to be set;
     *        it may be <code>null</code>.
     */
    public synchronized void setRenderingHints(RenderingHints hints) {
        if(hints != null) {
            hints = (RenderingHints)hints.clone();
        }
        nodeSupport.setRenderingHints(hints);
    }

    /* ----- Rendering generation methods. ----- */

    /**
     * Instantiate a <code>PlanarImage</code> that computes the result
     * of this <code>RenderedOp</code>.  The local
     * <code>OperationRegistry</code> of this node is used to translate
     * the operation name into a <code>RenderedImageFactory</code> and
     * eventually an actual <code>RenderedImage</code> (usually an
     * <code>OpImage</code>).
     *
     * <p> During this method, all the sources supplied in the
     * <code>ParameterBlock</code> are checked. If any of the sources
     * is a <code>RenderedOp</code>, a rendering of that source is
     * created. This propagates all the way up to the top of the op
     * chain.  If any of the sources is a <code>Collection</code>,
     * then the collection is passed to the operation as-is. If there
     * is a <code>RenderedOp</code> anywhere in the collection, it is
     * up to the individual operation to create the rendering for that
     * <code>RenderedOp</code>.
     *
     * <p> This method does not validate the sources and parameters
     * stored in the <code>ParameterBlock</code> against the specification
     * of the operation this node represents.  It is the responsibility
     * of the caller to ensure that the data in the
     * <code>ParameterBlock</code> are suitable for this operation.
     * Otherwise, some kind of exception or error will occur.
     *
     * <p> Invoking this method will cause any source <code>RenderedOp</code>
     * nodes to be rendered using <code>getRendering()</code> and any
     * source <code>CollectionOp</code> nodes to be rendered using
     * <code>getCollection()</code>.  Any <code>DeferredData</code> parameters
     * in the <code>ParameterBlock</code> will also be evaluated.
     *
     * <p> The <code>RenderedImage</code> generated by the selected
     * <code>RenderedImageFactory</code> will be converted to a
     * <code>PlanarImage</code> by invoking
     * <code>PlanarImage.wrapRenderedImage()</code>.
     *
     * @return The resulting image as a <code>PlanarImage</code>.
     *
     * @throws RuntimeException if the image factory charged with rendering
     *         the node is unable to create a rendering.
     */
    public synchronized PlanarImage createInstance() {
        return createInstance(false);
    }

    /**
     * This method performs the actions described by the documentation of
     * <code>createInstance()</code> optionally marking the node as rendered
     * according to the parameter.
     *
     * @throws RuntimeException if the image factory charged with rendering
     *         the node is unable to create a rendering.
     *
     * @see #createInstance()
     *
     * @since JAI 1.1
     */
    protected synchronized PlanarImage createInstance(boolean isNodeRendered) {
        ParameterBlock pb = new ParameterBlock();
        Vector parameters = nodeSupport.getParameterBlock().getParameters();

        // Evaluate and DeferredData parameters.
        pb.setParameters(ImageUtil.evaluateParameters(parameters));

        int numSources = getNumSources();
        for (int i = 0; i < numSources; i++) {
            Object source = getNodeSource(i);
            Object ai = null;

            if (source instanceof RenderedOp) {
                RenderedOp src = (RenderedOp)source;
		ai = isNodeRendered ?
                    src.getRendering() :
                    src.createInstance();
            } else if (source instanceof CollectionOp) {
                ai = ((CollectionOp)source).getCollection();
            } else if ((source instanceof RenderedImage) ||
                       (source instanceof Collection)) {
                // XXX: RenderedImageList - bpb 8 dec 2000
                // If source is a RenderedImageAdapter which is wrapping a
                // RenderedImageList whose primary image is a RenderedOp,
                // set ai to the rendering of that RenderedOp.
                ai = source;
            } else {
                // Source is some other type. Pass on (for now).
                ai = source;
            }
            pb.addSource(ai);
        }

        // Create the rendering.
        RenderedImage rendering =
            RIFRegistry.create(getRegistry(),
                               nodeSupport.getOperationName(),
                               pb,
                               nodeSupport.getRenderingHints());

        // Throw an exception if the rendering is null.
        if (rendering == null) {
            throw new RuntimeException(JaiI18N.getString("RenderedOp0"));
        }

        // XXX: RenderedImageList - bpb 8 dec 2000
        // If rendering is a wrapped RenderedImageList whose primary image
        // is a RenderedOp, reset the sources of the primary image
        // to the source List of this node. That is to say, replace
        // the OpImage sources with RenderedOp sources. Also, register
        // this node as a PropertyChangeListener of the primary image.
        // Somehow this node also needs to be able to identify
        // RenderingChangeEvents emitted by the primary image.
        // The invalid region would be extracted from such RCEs and used
        // in creating a new RCE with this node as its source which would
        // be fired as usual to all listeners and sinks.

        // Ensure that the rendering is a PlanarImage.
        PlanarImage instance = PlanarImage.wrapRenderedImage(rendering);

        // Save the RenderingHints.
        oldHints = nodeSupport.getRenderingHints() == null ?
            null : (RenderingHints)nodeSupport.getRenderingHints().clone();

        return instance;
    }

    /**
     * Creates a <code>PlanarImage</code> rendering if none exists
     * and sets <code>theImage</code> to the resulting value.  This method
     * performs the same actions as <code>createInstance()</code> but sets
     * <code>theImage</code> to the result.
     *
     * @throws RuntimeException if the image factory charged with rendering
     *         the node is unable to create a rendering.
     *
     * @see #createInstance()
     *
     * @since JAI 1.1
     */
    protected synchronized void createRendering() {
        if (theImage == null) {
            setImageLayout(new ImageLayout(theImage = createInstance(true)));

            if(theImage != null) {
                // Get listeners, wrap, and add to OpImage listener list.
                theImage.addTileComputationListener(new TCL(this));
            }
        }
    }

    /**
     * Returns the <code>PlanarImage</code> rendering associated with this
     * <code>RenderedOp</code> node.  This method performs the same action
     * as <code>createRendering()</code> but returns <code>theImage</code>.
     *
     * @throws RuntimeException if the image factory charged with rendering
     *         the node is unable to create a rendering.
     *
     * @see #createRendering()
     * @see #createInstance()
     */
    public PlanarImage getRendering() {
        createRendering();
        return theImage;
    }

    /**
     * Returns the value of the protected variable <code>theImage</code>
     * which may be <code>null</code> if no rendering has yet been created.
     * This method does not force the node to be rendered.
     *
     * @since JAI 1.1
     */
    public PlanarImage getCurrentRendering() {
        return theImage;
    }

    /**
     * Forces the node to be re-rendered and returns the new rendering.
     *
     * <p> If the node has not yet been rendered this method is identical to
     * <code>getRendering()</code>.
     *
     * <p> If the node has already been rendered, then a new rendering will be
     * generated. The synthetic and locally cached properties and the property
     * environment of the node will all be reset.  All registered
     * <code>PropertyChangeListener</code>s and any
     * <code>PropertyChangeListener</code> sinks will be notifed of the
     * change in the rendering via a <code>RenderingChangeEvent</code>
     * the invalid region of which will be <code>null</code>.
     *
     * <p> This method could be used for example to trigger a
     * re-rendering of the node in cases where this would not happen
     * automatically but is desirable to the application.  One
     * example occurs if a parameter of the operation is a referent of
     * some other entity which changes but the parameter itself does not
     * change according to <code>equals()</code>.  This could occur for
     * example for an image file input operation wherein the path to the
     * file remains the same but the content of the file changes.
     *
     * @return The (possibly regenerated) rendering of the node. This value
     * may be ignored if the intent of invoking the method was merely to
     * re-render the node and generate events for
     * <code>RenderingChangeEvent</code> listeners.
     *
     * @since JAI 1.1
     */
    public PlanarImage getNewRendering() {
        if(theImage == null) {
            return getRendering();
        }

        // Save the previous rendering.
        PlanarImage theOldImage = theImage;

        // Clear the current rendering.
        theImage = null;

        // XXX The rest of this method is effectively duplicated from the
        // end of propertyChange(). Should another method be created to be
        // called in these two places in order to avoid code duplication?

        // Re-render the node.
        createRendering();

        // Clear the synthetic and cached properties and reset the
        // property source.
        resetProperties(true);

        // Create the event object.
        RenderingChangeEvent rcEvent =
            new RenderingChangeEvent(this, theOldImage, theImage, null);

        // Fire to all registered listeners.
        eventManager.firePropertyChange(rcEvent);

        // Fire an event to all PropertyChangeListener sinks.
        Vector sinks = getSinks();
        if(sinks != null) {
            int numSinks = sinks.size();
            for(int i = 0; i < numSinks; i++) {
                Object sink = sinks.get(i);
                if(sink instanceof PropertyChangeListener) {
                    ((PropertyChangeListener)sink).propertyChange(rcEvent);
                }
            }
        }

        return theImage;
    }

    /* ----- PropertyChangeListener method. ----- */

    /**
     * Implementation of <code>PropertyChangeListener</code>.
     *
     * <p> When invoked with an event which is an instance of
     * <code>RenderingChangeEvent</code> or <code>CollectionChangeEvent</code>
     * emitted by a <code>RenderedOp</code> or <code>CollectionOp</code>,
     * respectively, the node will respond by re-rendering itself while
     * retaining any tiles possible.  It will respond to an "InvalidRegion"
     * event emitted by a source <code>RenderedImage</code> in a manner
     * similar to that applied for <code>RenderingChangeEvent</code>s.
     *
     * @see TiledImage#propertyChange
     *
     * @since JAI 1.1
     */
    public synchronized void propertyChange(PropertyChangeEvent evt) {
        //
        // React if and only if the node has been rendered and
        // A: a non-PropertySourceChangeEvent PropertyChangeEventJAI
        //    was received from this node, or
        // B: a RenderingChangeEvent was received from a source RenderedOp, or
        // C: a CollectionChangeEvent was received from a source CollectionOp, or
        // D: an "InvalidRegion" event was received from a source RenderedImage.
        //

        // Cache event and node sources.
        Object evtSrc = evt.getSource();
        Vector nodeSources = nodeSupport.getParameterBlock().getSources();

        // Get the name of the bean property and convert it to lower
        // case now for efficiency later.
        String propName = evt.getPropertyName().toLowerCase(Locale.ENGLISH);

        if(theImage != null &&
           ((evt instanceof PropertyChangeEventJAI &&
             evtSrc == this &&
             !(evt instanceof PropertySourceChangeEvent) &&
             nodeEventNames.contains(propName)) ||
            ((evt instanceof RenderingChangeEvent ||
              evt instanceof CollectionChangeEvent ||
              (evt instanceof PropertyChangeEventJAI &&
               evtSrc instanceof RenderedImage &&
               propName.equals("invalidregion"))) &&
             nodeSources.contains(evtSrc)))) {

            // Save the previous rendering.
            PlanarImage theOldImage = theImage;

            // Initialize the event flag.
            boolean fireEvent = false;

            // Set default invalid region to null (the entire image).
            Shape invalidRegion = null;

            if(evtSrc == this &&
               (propName.equals("operationname") ||
                propName.equals("operationregistry"))) {

                // Operation name or OperationRegistry changed:
                // invalidate the entire rendering.
                fireEvent = true;
                theImage = null;

            } else if(evt instanceof RenderingChangeEvent ||
                      (evtSrc instanceof RenderedImage &&
                       propName.equals("invalidregion"))) {

                // Set the event flag.
                fireEvent = true;

                Shape srcInvalidRegion = null;

                if(evt instanceof RenderingChangeEvent) {
                    // RenderingChangeEvent presumably from a source RenderedOp.
                    RenderingChangeEvent rcEvent = (RenderingChangeEvent)evt;

                    // Get the invalidated region of the source.
                    srcInvalidRegion = rcEvent.getInvalidRegion();

                    // If entire source is invalid replace with source bounds.
                    if(srcInvalidRegion == null) {
                        srcInvalidRegion =
                            ((PlanarImage)rcEvent.getOldValue()).getBounds();
                    }
                } else {
                    // Get the invalidated region of the source.
                    srcInvalidRegion = (Shape)evt.getNewValue();

                    // If entire source is invalid replace with source bounds.
                    if(srcInvalidRegion == null) {
                        RenderedImage rSrc = (RenderedImage)evtSrc;
                        srcInvalidRegion =
                            new Rectangle(rSrc.getMinX(), rSrc.getMinY(),
                                          rSrc.getWidth(), rSrc.getHeight());
                    }
                }

                // Only process further if the rendering is an OpImage.
                if(!(theImage instanceof OpImage)) {

                    // Clear the current rendering.
                    theImage = null;

                } else {
                    // Save the previous rendering as an OpImage.
                    OpImage oldOpImage = (OpImage)theImage;

                    // Cache source invalid bounds.
                    Rectangle srcInvalidBounds =
                        srcInvalidRegion.getBounds();

                    // If bounds are empty, replace srcInvalidRegion with
                    // the complement of the image bounds within the
                    // bounds of all tiles.
                    if(srcInvalidBounds.isEmpty()) {
                        int x = oldOpImage.tileXToX(oldOpImage.getMinTileX());
                        int y = oldOpImage.tileYToY(oldOpImage.getMinTileY());
                        int w = oldOpImage.getNumXTiles()*
                            oldOpImage.getTileWidth();
                        int h = oldOpImage.getNumYTiles()*
                            oldOpImage.getTileHeight();
                        Rectangle tileBounds = new Rectangle(x, y, w, h);
                        Rectangle imageBounds = oldOpImage.getBounds();
                        if(!tileBounds.equals(imageBounds)) {
                            Area tmpArea = new Area(tileBounds);
                            tmpArea.subtract(new Area(imageBounds));
                            srcInvalidRegion = tmpArea;
                            srcInvalidBounds = srcInvalidRegion.getBounds();
                        }
                    }

                    // ----- Determine invalid destination region. -----

                    boolean saveAllTiles = false;
                    ArrayList validTiles = null;
                    if(srcInvalidBounds.isEmpty()) {
                        invalidRegion = srcInvalidRegion;
                        saveAllTiles = true;
                    } else {
                        // Get index of source which changed.
                        int idx = nodeSources.indexOf(evtSrc);

                        // Determine bounds of invalid destination region.
                        Rectangle dstRegionBounds =
                            oldOpImage.mapSourceRect(srcInvalidBounds,
                                                     idx);

                        if(dstRegionBounds == null) {
                            dstRegionBounds = oldOpImage.getBounds();
                        }

                        // Determine invalid destination region.
                        Point[] indices = getTileIndices(dstRegionBounds);
                        int numIndices = indices != null ? indices.length : 0;
                        GeneralPath gp = null;

                        for(int i = 0; i < numIndices; i++) {
                            if (i % 1000 == 0 && gp != null)
				gp = new GeneralPath(new Area(gp));

                            Rectangle dstRect =
                                getTileRect(indices[i].x, indices[i].y);
                            Rectangle srcRect =
                                oldOpImage.mapDestRect(dstRect, idx);
                            if(srcRect == null) {
                                gp = null;
                                break;
                            }
                            if(srcInvalidRegion.intersects(srcRect)) {
                                if(gp == null) {
                                    gp = new GeneralPath(dstRect);
                                } else {
                                    gp.append(dstRect, false);
                                }
                            } else {
                                if(validTiles == null) {
                                    validTiles = new ArrayList();
                                }
                                validTiles.add(indices[i]);
                            }
                        }

                        invalidRegion = (gp == null) ? null : new Area(gp);
                    }

                    // Clear the current rendering.
                    theImage = null;

                    // Retrieve the old TileCache.
                    TileCache oldCache = oldOpImage.getTileCache();

                    // Only perform further processing if there is a cache
                    // and there are tiles to save.
                    if(oldCache != null &&
                       (saveAllTiles || validTiles != null)) {
                        // Re-render the node.
                        createRendering();

                        // Only perform further processing if the new
                        // rendering is an OpImage with a non-null TileCache.
                        if(theImage instanceof OpImage &&
                           ((OpImage)theImage).getTileCache() != null) {
                            OpImage newOpImage = (OpImage)theImage;
                            TileCache newCache =
                                newOpImage.getTileCache();
                            Object tileCacheMetric =
                                newOpImage.getTileCacheMetric();

                            if(saveAllTiles) {
                                Raster[] tiles =
                                    oldCache.getTiles(oldOpImage);
                                int numTiles = tiles == null ?
                                    0 : tiles.length;
                                for(int i = 0; i < numTiles; i++) {
                                    Raster tile = tiles[i];
                                    int tx =
                                        newOpImage.XToTileX(tile.getMinX());
                                    int ty =
                                        newOpImage.YToTileY(tile.getMinY());
                                    newCache.add(newOpImage,
                                                 tx, ty, tile,
                                                 tileCacheMetric);
                                }
                            } else { // save some, but not all, tiles
                                int numValidTiles = validTiles.size();
                                for(int i = 0; i < numValidTiles; i++) {
                                    Point tileIndex = (Point)validTiles.get(i);
                                    Raster tile =
                                        oldCache.getTile(oldOpImage,
                                                         tileIndex.x,
                                                         tileIndex.y);
                                    if(tile != null) {
                                        newCache.add(newOpImage,
                                                     tileIndex.x,
                                                     tileIndex.y,
                                                     tile,
                                                     tileCacheMetric);
                                    }
                                }
                            }
                        }
                    }
                }
            } else { // not op name or registry change nor RenderingChangeEvent
                ParameterBlock oldPB = null;
                ParameterBlock newPB = null;

                boolean checkInvalidRegion = false;
                if(propName.equals("parameterblock")) {
                    oldPB = (ParameterBlock)evt.getOldValue();
                    newPB = (ParameterBlock)evt.getNewValue();
                    checkInvalidRegion = true;
                } else if(propName.equals("sources")) {
                    // Replace source(s)
                    Vector params =
                        nodeSupport.getParameterBlock().getParameters();
                    oldPB = new ParameterBlock((Vector)evt.getOldValue(),
                                               params);
                    newPB = new ParameterBlock((Vector)evt.getNewValue(),
                                               params);
                    checkInvalidRegion = true;
                } else if(propName.equals("parameters")) {
                    // Replace parameter(s)
                    oldPB = new ParameterBlock(nodeSources,
                                               (Vector)evt.getOldValue());
                    newPB = new ParameterBlock(nodeSources,
                                               (Vector)evt.getNewValue());
                    checkInvalidRegion = true;
                } else if(propName.equals("renderinghints")) {
                    oldPB = newPB = nodeSupport.getParameterBlock();
                    checkInvalidRegion = true;
                } else if(evt instanceof CollectionChangeEvent) {
                    // Event from a CollectionOp source.

                    // Replace appropriate source.
                    int collectionIndex = nodeSources.indexOf(evtSrc);
                    Vector oldSources = (Vector)nodeSources.clone();
                    Vector newSources = (Vector)nodeSources.clone();
                    oldSources.set(collectionIndex, evt.getOldValue());
                    newSources.set(collectionIndex, evt.getNewValue());

                    Vector params =
                        nodeSupport.getParameterBlock().getParameters();

                    oldPB = new ParameterBlock(oldSources, params);
                    newPB = new ParameterBlock(newSources, params);

                    checkInvalidRegion = true;
                }

                if(checkInvalidRegion) {
                    // Set event flag.
                    fireEvent = true;

                    // Get the associated OperationDescriptor.
                    OperationRegistry registry = nodeSupport.getRegistry();
                    OperationDescriptor odesc = (OperationDescriptor)
                        registry.getDescriptor(OperationDescriptor.class,
                                               nodeSupport.getOperationName());

                    // Evaluate any DeferredData parameters.
                    oldPB = ImageUtil.evaluateParameters(oldPB);
                    newPB = ImageUtil.evaluateParameters(newPB);

                    // Determine the invalid region.
                    invalidRegion = (Shape)
                        odesc.getInvalidRegion(RenderedRegistryMode.MODE_NAME,
                                               oldPB,
                                               oldHints,
                                               newPB,
                                               nodeSupport.getRenderingHints(),
                                               this);

                    if(invalidRegion == null ||
                       !(theImage instanceof OpImage)) {

                        // Can't save any tiles; clear the rendering.
                        theImage = null;

                    } else {
                        // Create a new rendering.
                        OpImage oldRendering = (OpImage)theImage;
                        theImage = null;
                        createRendering();

                        // If the new rendering is also an OpImage,
                        // save some tiles.
                        if(theImage instanceof OpImage &&
                           oldRendering.getTileCache() != null &&
                           ((OpImage)theImage).getTileCache() != null) {
                            OpImage newRendering = (OpImage)theImage;

                            // Save some values.
                            TileCache oldCache = oldRendering.getTileCache();
                            TileCache newCache = newRendering.getTileCache();
                            Object tileCacheMetric =
                                newRendering.getTileCacheMetric();

                            // If bounds are empty, replace invalidRegion with
                            // the complement of the image bounds within the
                            // bounds of all tiles.
                            if(invalidRegion.getBounds().isEmpty()) {
                                int x = oldRendering.tileXToX(
                                            oldRendering.getMinTileX());
                                int y = oldRendering.tileYToY(
                                            oldRendering.getMinTileY());
                                int w = oldRendering.getNumXTiles()*
                                    oldRendering.getTileWidth();
                                int h = oldRendering.getNumYTiles()*
                                    oldRendering.getTileHeight();
                                Rectangle tileBounds =
                                    new Rectangle(x, y, w, h);
                                Rectangle imageBounds =
                                    oldRendering.getBounds();
                                if(!tileBounds.equals(imageBounds)) {
                                    Area tmpArea = new Area(tileBounds);
                                    tmpArea.subtract(new Area(imageBounds));
                                    invalidRegion = tmpArea;
                                }
                            }

                            if(invalidRegion.getBounds().isEmpty()) {
                                // Save all tiles.
                                Raster[] tiles =
                                    oldCache.getTiles(oldRendering);
                                int numTiles = tiles == null ?
                                    0 : tiles.length;
                                for(int i = 0; i < numTiles; i++) {
                                    Raster tile = tiles[i];
                                    int tx =
                                        newRendering.XToTileX(tile.getMinX());
                                    int ty =
                                        newRendering.YToTileY(tile.getMinY());
                                    newCache.add(newRendering,
                                                 tx, ty, tile,
                                                 tileCacheMetric);
                                }
                            } else {
                                // Copy tiles not in invalid region from old
                                // TileCache to new TileCache.
                                Raster[] tiles =
                                    oldCache.getTiles(oldRendering);
                                int numTiles = tiles == null ?
                                    0 : tiles.length;
                                for(int i = 0; i < numTiles; i++) {
                                    Raster tile = tiles[i];
                                    Rectangle bounds = tile.getBounds();
                                    if(!invalidRegion.intersects(bounds)) {
                                        newCache.add(newRendering,
                                                     newRendering.XToTileX(bounds.x),
                                                     newRendering.YToTileY(bounds.y),
                                                     tile,
                                                     tileCacheMetric);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Re-render the node. This will only occur if theImage
            // has been set to null above.
            createRendering();

            // Fire an event if the flag was set.
            if(fireEvent) {
                // Clear the synthetic and cached properties and reset the
                // property source.
                resetProperties(true);

                // Create the event object.
                RenderingChangeEvent rcEvent =
                    new RenderingChangeEvent(this, theOldImage, theImage,
                                             invalidRegion);

                // Fire to all registered listeners.
                eventManager.firePropertyChange(rcEvent);

                // Fire an event to all PropertyChangeListener sinks.
                Vector sinks = getSinks();
                if(sinks != null) {
                    int numSinks = sinks.size();
                    for(int i = 0; i < numSinks; i++) {
                        Object sink = sinks.get(i);
                        if(sink instanceof PropertyChangeListener) {
                            ((PropertyChangeListener)sink).propertyChange(rcEvent);
                        }
                    }
                }
            }
        }
    }

    /* ----- Node source methods: interact with ParameterBlock sources ----- */

    /**
     * Adds a source to the <code>ParameterBlock</code> of this node.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * @param source The source to be added to the
     *        <code>ParameterBlock</code>
     * @deprecated as of JAI 1.1 Use <code>addSource(Object)</code>.
     */
    public synchronized void addNodeSource(Object source) {
        addSource(source);
    }

    /**
     * Sets the specified source stored in the <code>ParameterBlock</code>
     * of this node to a new source object.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * @param source The Source to be set.
     * @param index  The Index at which it is to be set.
     *
     * @throws IllegalArgumentException if
     *         <code>source</code> is <code>null</code>.
     * @throws ArrayIndexOutOfBoundsException if
     *         <code>index</code> is invalid.
     * @deprecated as of JAI 1.1 Use <code>setSource(Object,int)</code>.
     */
    public synchronized void setNodeSource(Object source, int index) {
        setSource(source, index);
    }

    /**
     * Returns the specified source stored in the
     * <code>ParameterBlock</code> of this node.
     * If there is no source corresponding to the specified index, an
     * <code>ArrayIndexOutOfBoundsException</code> will be thrown.
     *
     * @param index  The index of the source.
     * @deprecated as of JAI 1.1 Use <code>getSourceObject(int)</code>.
     */
    public synchronized Object getNodeSource(int index) {
        return nodeSupport.getParameterBlock().getSource(index);
    }

    /* ----- Parameter methods: interact with ParameterBlock params ----- */

    /**
     * Returns the number of parameters stored in the
     * <code>ParameterBlock</code> of this node.
     */
    public synchronized int getNumParameters() {
        return nodeSupport.getParameterBlock().getNumParameters();
    }

    /**
     * Returns a clone of the <code>Vector</code> of parameters stored in the
     * <code>ParameterBlock</code> of this node.
     */
    public synchronized Vector getParameters() {
        // In the Sun JDK ParameterBlock the parameter Vector is never null.
        Vector params = nodeSupport.getParameterBlock().getParameters();
        return params == null ? null : (Vector)params.clone();
    }

    /**
     * Returns the specified parameter stored in the
     * <code>ParameterBlock</code> of this node as a <code>byte</code>.
     * An <code>ArrayIndexOutOfBoundsException</code> may occur if an
     * invalid index is supplied
     *
     * @param index  The index of the parameter.
     *
     * @throws ArrayIndexOutOfBoundsException if
     * <code>index</code> is invalid.
     */
    public synchronized byte getByteParameter(int index) {
        return nodeSupport.getParameterBlock().getByteParameter(index);
    }

    /**
     * Returns the specified parameter stored in the
     * <code>ParameterBlock</code> of this node as a <code>char</code>.
     * An <code>ArrayIndexOutOfBoundsException</code> may occur if an
     * invalid index is supplied
     *
     * @param index  The index of the parameter.
     *
     * @throws ArrayIndexOutOfBoundsException if
     * <code>index</code> is invalid.
     */
    public synchronized char getCharParameter(int index) {
        return nodeSupport.getParameterBlock().getCharParameter(index);
    }

    /**
     * Returns the specified parameter stored in the
     * <code>ParameterBlock</code> of this node as a <code>short</code>.
     * An <code>ArrayIndexOutOfBoundsException</code> may occur if an
     * invalid index is supplied
     *
     * @param index  The index of the parameter.
     *
     * @throws ArrayIndexOutOfBoundsException if
     * <code>index</code> is invalid.
     */
    public synchronized short getShortParameter(int index) {
        return nodeSupport.getParameterBlock().getShortParameter(index);
    }

    /**
     * Returns the specified parameter stored in the
     * <code>ParameterBlock</code> of this node as an <code>int</code>.
     * An <code>ArrayIndexOutOfBoundsException</code> may occur if an
     * invalid index is supplied
     *
     * @param index  The index of the parameter.
     *
     * @throws ArrayIndexOutOfBoundsException if
     * <code>index</code> is invalid.
     */
    public synchronized int getIntParameter(int index) {
        return nodeSupport.getParameterBlock().getIntParameter(index);

    }

    /**
     * Returns the specified parameter stored in the
     * <code>ParameterBlock</code> of this node as a <code>long</code>.
     * An <code>ArrayIndexOutOfBoundsException</code> may occur if an
     * invalid index is supplied
     *
     * @param index  The index of the parameter.
     *
     * @throws ArrayIndexOutOfBoundsException if
     * <code>index</code> is invalid.
     */
    public synchronized long getLongParameter(int index) {
        return nodeSupport.getParameterBlock().getLongParameter(index);
    }

    /**
     * Returns the specified parameter stored in the
     * <code>ParameterBlock</code> of this node as a <code>float</code>.
     * An <code>ArrayIndexOutOfBoundsException</code> may occur if an
     * invalid index is supplied
     *
     * @param index  The index of the parameter.
     *
     * @throws ArrayIndexOutOfBoundsException if
     * <code>index</code> is invalid.
     */
    public synchronized float getFloatParameter(int index) {
        return nodeSupport.getParameterBlock().getFloatParameter(index);
    }

    /**
     * Returns the specified parameter stored in the
     * <code>ParameterBlock</code> of this node as a <code>double</code>.
     * An <code>ArrayIndexOutOfBoundsException</code> may occur if an
     * invalid index is supplied
     *
     * @param index  The index of the parameter.
     *
     * @throws ArrayIndexOutOfBoundsException if
     * <code>index</code> is invalid.
     */
    public synchronized double getDoubleParameter(int index) {
        return nodeSupport.getParameterBlock().getDoubleParameter(index);
    }

    /**
     * Returns the specified parameter stored in the
     * <code>ParameterBlock</code> of this node as an <code>Object</code>.
     * An <code>ArrayIndexOutOfBoundsException</code> may occur if an
     * invalid index is supplied
     *
     * @param index  The index of the parameter.
     *
     * @throws ArrayIndexOutOfBoundsException if
     * <code>index</code> is invalid.
     */
    public synchronized Object getObjectParameter(int index) {
        return nodeSupport.getParameterBlock().getObjectParameter(index);
    }

    /**
     * Sets all the parameters of this node.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * <p> The <code>Vector</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when the node is
     * rendered.
     *
     * @since JAI 1.1
     */
    public synchronized void setParameters(Vector parameters) {
        ParameterBlock pb =
            (ParameterBlock)nodeSupport.getParameterBlock().clone();
        pb.setParameters(parameters);
        nodeSupport.setParameterBlock(pb);
    }

    /**
     * Sets one of the node's parameters to a <code>byte</code>.
     * If the <code>index</code> lies beyond the current source list,
     * the list is extended with nulls as needed.
     * This is a convenience method that invokes
     * <code>setParameter(Object,int)</code> and so adheres to the same event
     * firing behavior.
     *
     * @param param The parameter, as a <code>byte</code>.
     * @param index The index of the parameter.
     */
   public synchronized void setParameter(byte param, int index) {
       setParameter(new Byte(param), index);
    }

    /**
     * Sets one of the node's parameters to a <code>char</code>.
     * If the <code>index</code> lies beyond the current source list,
     * the list is extended with nulls as needed.
     * This is a convenience method that invokes
     * <code>setParameter(Object,int)</code> and so adheres to the same event
     * firing behavior.
     *
     * @param param The parameter, as a <code>char</code>.
     * @param index The index of the parameter.
     */
    public  synchronized void setParameter(char param, int index) {
        setParameter(new Character(param), index);
    }

    /**
     * Sets one of the node's parameters to a <code>short</code>.
     * If the <code>index</code> lies beyond the current source list,
     * the list is extended with nulls as needed.
     *
     * @param param The parameter, as a <code>short</code>.
     * @param index The index of the parameter.
     */
    public synchronized void setParameter(short param, int index) {
        setParameter(new Short(param), index);
    }

    /**
     * Sets one of the node's parameters to an <code>in</code>t.
     * If the <code>index</code> lies beyond the current source list,
     * the list is extended with nulls as needed.
     *
     * @param param The parameter, as an <code>int</code>.
     * @param index The index of the parameter.
     */
    public synchronized void setParameter(int param, int index) {
        setParameter(new Integer(param), index);
    }

    /**
     * Sets one of the node's parameters to a <code>long</code>.
     * If the <code>index</code> lies beyond the current source list,
     * the list is extended with nulls as needed.
     *
     * @param param The parameter, as a <code>long</code>.
     * @param index The index of the parameter.
     */
    public synchronized void setParameter(long param, int index) {
        setParameter(new Long(param), index);
    }

    /**
     * Sets one of the node's parameters to a <code>float</code>.
     * If the <code>index</code> lies beyond the current source list,
     * the list is extended with nulls as needed.
     *
     * @param param The parameter, as a <code>float</code>.
     * @param index The index of the parameter.
     */
    public synchronized void setParameter(float param, int index) {
        setParameter(new Float(param), index);
    }

    /**
     * Sets one of the node's parameters to a <code>double</code>.
     * If the <code>index</code> lies beyond the current source list,
     * the list is extended with nulls as needed.
     *
     * @param param The parameter, as a <code>double</code>.
     * @param index The index of the parameter.
     */
    public synchronized void setParameter(double param, int index) {
        setParameter(new Double(param), index);
    }

    /**
     * Sets one of the node's parameters to an <code>Object</code>.
     * If the <code>index</code> lies beyond the current source list,
     * the list is extended with nulls as needed.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * <p> The <code>Object</code> may be a
     * <code>DeferredData</code> instance.  It will not be evaluated
     * until its value is actually required, i.e., when the node is
     * rendered.
     *
     * @param param The parameter, as an <code>Object</code>.
     * @param index The index of the parameter.
     */
    public synchronized void setParameter(Object param, int index) {
        ParameterBlock pb =
            (ParameterBlock)nodeSupport.getParameterBlock().clone();
        pb.set(param, index);
        nodeSupport.setParameterBlock(pb);
    }

    /* ----- RenderingHints methods. ----- */

    /**
     * Sets a hint in the <code>RenderingHints</code> of this node.  This
     * is a convenience method which calls <code>setRenderingHints()</code>
     * and so adheres to the same event firing behavior.
     *
     * @throws IllegalArgumentException
     *         if the key or value is <code>null</code>.
     * @throws IllegalArgumentException
     *         value is not appropriate for the specified key.
     *
     * @since JAI 1.1
     */
    public synchronized void setRenderingHint(RenderingHints.Key key,
                                              Object value) {

        if ( key == null || value == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        RenderingHints rh = nodeSupport.getRenderingHints();
        if(rh == null) {
            nodeSupport.setRenderingHints(new RenderingHints(key, value));
        } else {
            rh.put(key, value);
            nodeSupport.setRenderingHints(rh);
        }
    }

    /**
     * Gets a hint from the <code>RenderingHints</code> of this node.
     *
     * @return the value associated with the specified key or
     *         <code>null</code> if the key is not mapped to any value.
     *
     * @since JAI 1.1
     */
    public synchronized Object getRenderingHint(RenderingHints.Key key) {
        RenderingHints rh = nodeSupport.getRenderingHints();
        return rh == null ? null : rh.get(key);
    }

    /* ----- Property-related methods. ----- */

    /** Creates a <code>PropertySource</code> if none exists. */
    private synchronized void createPropertySource() {
        if (thePropertySource == null) {
            // Create a <code>PropertySource</code> wrapper of the rendering.
            PropertySource defaultPS =
                new PropertySource() {
                    /**
                     * Retrieve the names from an instance of the node.
                     */
                    public String[] getPropertyNames() {
                        return getRendering().getPropertyNames();
                    }

                    public String[] getPropertyNames(String prefix) {
                        return PropertyUtil.getPropertyNames(
                                   getPropertyNames(), prefix);
                    }

                    public Class getPropertyClass(String name) {
                        return null;
                    }

                    /**
                     * Retrieve the actual property values from a rendering
                     * of the node.
                     */
                    public Object getProperty(String name) {
                        return getRendering().getProperty(name);
                    }
                };

            // Create a <code>PropertySource</code> encapsulating the
            // property environment of the node.
            thePropertySource = nodeSupport.getPropertySource(this, defaultPS);

            // Add the <code>PropertySource</code> to the helper object.
            properties.addProperties(thePropertySource);
        }
    }

    /**
     * Resets the <code>PropertySource</code>.  If the parameter is
     * <code>true</code> then the property environment is completely
     * reset; if <code>false</code> then only cached properties are
     * cleared, i.e., those which were derived from the property
     * environment and are now stored in the local cache.
     *
     * @since JAI 1.1
     */
    protected synchronized void resetProperties(boolean resetPropertySource) {
        properties.clearCachedProperties();
        if (resetPropertySource && thePropertySource != null) {
            synthProperties = null;
            properties.removePropertySource(thePropertySource);
            thePropertySource = null;
        }
    }

    /**
     * Returns the names of properties available from this node.
     * These properties are a combination of those derived
     * from prior nodes in the imaging chain, those set locally,
     * and a number of locally derived, immutable properties
     * based on the rendering associated with this node --
     * height, width, and so forth.
     *
     * @return An array of <code>String</code>s containing valid
     *         property names.
     */
    public synchronized String[] getPropertyNames() {
        createPropertySource();

	// Initialize names to synthetic property names.
	Vector names = new Vector(synthProps);

        // Create a dummy key for later use.
        CaselessStringKey key = new CaselessStringKey("");

        // Get property names managed by WritablePropertySourceImpl.
        // This includes those of thePropertySource.
        String[] localNames = properties.getPropertyNames();
        if(localNames != null) {
            int length = localNames.length;
            for(int i = 0; i < length; i++) {
                key.setName(localNames[i]);

                // Check for duplicates being inserted
                if (!names.contains(key)) {
                    names.add(key.clone());
                }
            }
        }

        // Return an array.
        String[] propertyNames = null;
        int numNames = names.size();
        if(numNames > 0) {
            propertyNames = new String[numNames];
            for(int i = 0; i < numNames; i++) {
                propertyNames[i] = ((CaselessStringKey)names.get(i)).getName();
            }
        }

        return propertyNames;
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
        createPropertySource();
        return properties.getPropertyClass(name);
    }

    /** Initialize the synthProperties Hashtable if needed. */
    private synchronized void createSynthProperties() {
        if (synthProperties == null) {
            synthProperties = new Hashtable();
            synthProperties.put(new CaselessStringKey("image_width"),
                                new Integer(theImage.getWidth()));
            synthProperties.put(new CaselessStringKey("image_height"),
                                new Integer(theImage.getHeight()));
            synthProperties.put(new CaselessStringKey("image_min_x_coord"),
                                new Integer(theImage.getMinX()));
            synthProperties.put(new CaselessStringKey("image_min_y_coord"),
                                new Integer(theImage.getMinY()));

            if(theImage instanceof OpImage) {
                synthProperties.put(new CaselessStringKey("tile_cache_key"),
                                    theImage);
                Object tileCache = ((OpImage)theImage).getTileCache();
                synthProperties.put(new CaselessStringKey("tile_cache"),
                                    tileCache == null ?
                                    java.awt.Image.UndefinedProperty :
                                    tileCache);
            } else if(theImage instanceof PlanarImageServerProxy) {
                synthProperties.put(new CaselessStringKey("tile_cache_key"),
                                    theImage);
                Object tileCache =
                    ((PlanarImageServerProxy)theImage).getTileCache();
                synthProperties.put(new CaselessStringKey("tile_cache"),
                                    tileCache == null ?
                                    java.awt.Image.UndefinedProperty :
                                    tileCache);
            } else {
                Object tileCacheKey = theImage.getProperty("tile_cache_key");
                synthProperties.put(new CaselessStringKey("tile_cache_key"),
                                    tileCacheKey == null ?
                                    java.awt.Image.UndefinedProperty :
                                    tileCacheKey);
                Object tileCache = theImage.getProperty("tile_cache");
                synthProperties.put(new CaselessStringKey("tile_cache"),
                                    tileCache == null ?
                                    java.awt.Image.UndefinedProperty :
                                    tileCache);
            }
        }
    }

    /**
     * Returns the property associated with the specified property name,
     * or <code>java.awt.Image.UndefinedProperty</code> if the specified
     * property is not set on the image.  If <code>name</code> equals the
     * name of any synthetic property, i.e., <code>image_width</code>,
     * <code>image_height</code>, <code>image_min_x_coord</code>, or
     * <code>image_min_y_coord</code>, then the node will be rendered.
     *
     * @param name A <code>String</code> naming the property.
     *
     * @throws IllegalArgumentException if
     *         <code>name</code> is <code>null</code>.
     */
    public synchronized Object getProperty(String name) {

        if (name == null)
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        createPropertySource();
        CaselessStringKey key = new CaselessStringKey(name);

        // Attempt to retrieve from synthetic properties.
        // If present, return the value directly.
	if (synthProps.contains(key)) {
            createRendering();

            // Create synthProperties Hashtable "just in time."
            createSynthProperties();
            return synthProperties.get(key);
        }

        // Attempt to retrieve from local properties.
        Object value = properties.getProperty(name);

        // If still undefined, query the property environment.
        if(value == java.awt.Image.UndefinedProperty) {
            value = thePropertySource.getProperty(name);
        }

        // Special case handling of ROI property: clip to destination bounds.
        // XXX Do we really want to do this (clip ROI to dest bounds)?
        if(value != java.awt.Image.UndefinedProperty &&
           name.equalsIgnoreCase("roi") &&
           value instanceof ROI) {
            ROI roi = (ROI)value;
            Rectangle imageBounds = getBounds();
            if(!imageBounds.contains(roi.getBounds())) {
                value = roi.intersect(new ROIShape(imageBounds));
            }
        }

        return value;
    }

    /**
     * Sets a local property on a node.  The synthetic properties
     * (containing image width, height, and location) may not be set.
     * Local property settings override properties derived from prior
     * nodes in the imaging chain.
     *
     * <p> If the node is serialized then serializable properties will
     * also be serialized but non-serializable properties will be lost.
     *
     * @param name A <code>String</code> representing the property name.
     * @param value The property's value, as an <code>Object</code>.
     *
     * @throws IllegalArgumentException if
     * <code>name</code> is <code>null</code>.
     * @throws IllegalArgumentException if
     * <code>value</code> is <code>null</code>.
     * @throws RuntimeException if <code>name</code>
     * conflicts with Synthetic property.
     */
    public synchronized void setProperty(String name, Object value) {
	if (name == null)
	  throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
	if (value == null)
	  throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        // Check whether property conflicts with synthetic properties.
	if (synthProps.contains(new CaselessStringKey(name))) {
	    throw new RuntimeException(JaiI18N.getString("RenderedOp4"));
        }

        createPropertySource();
        super.setProperty(name, value);
    }

    /**
     * Removes the named property from the local property
     * set of the <code>RenderedOp</code> as well as from its property
     * environment.  The synthetic properties
     * (containing image width, height, and position) may not be removed.
     *
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     * @throws RuntimeException if <code>name</code>
     * conflicts with Synthetic property.
     *
     * @since JAI 1.1
     */
    public void removeProperty(String name) {
	if (name == null)
	  throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        // Check whether property conflicts with synthetic properties.
	if (synthProps.contains(new CaselessStringKey(name))) {
	    throw new RuntimeException(JaiI18N.getString("RenderedOp4"));
        }

        createPropertySource();
        properties.removeProperty(name);
    }

    /**
     * Returns the property associated with the specified property name,
     * or <code>java.awt.Image.UndefinedProperty</code> if the specified
     * property is not set on the image.  This method is dynamic in the
     * sense that subsequent invocations of this method on the same object
     * may return different values as a function of changes in the property
     * environment of the node, e.g., a change in which
     * <code>PropertyGenerator</code>s are registered or in the values
     * associated with properties of node sources.  The case of the property
     * name passed to this method is ignored.
     *
     * @param name A <code>String</code> naming the property.
     *
     * @throws IllegalArgumentException if
     *         <code>name</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public synchronized Object getDynamicProperty(String name) {
	createPropertySource();
        return thePropertySource.getProperty(name);
    }

    /**
     * Adds a <code>PropertyGenerator</code> to the node.  The property values
     * emitted by this property generator override any previous
     * definitions.
     *
     * @param pg A <code>PropertyGenerator</code> to be added to this node's
     *        property environment.
     *
     * @throws IllegalArgumentException if
     * <code>pg</code> is <code>null</code>.
     */
    public synchronized void addPropertyGenerator(PropertyGenerator pg) {
        nodeSupport.addPropertyGenerator(pg);
    }

    /**
     * Forces a property to be copied from the specified source node.
     * By default, a property is copied from the first source node
     * that emits it.  The result of specifying an invalid source is
     * undefined.
     *
     * @param propertyName the name of the property to be copied.
     * @param sourceIndex the index of the from which to copy the property.
     * @throws IllegalArgumentException if <code>propertyName</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public synchronized void copyPropertyFromSource(String propertyName,
                                                    int sourceIndex) {
        nodeSupport.copyPropertyFromSource(propertyName, sourceIndex);
    }

    /**
     * Removes a named property from the property environment of this
     * node.  Unless the property is stored locally either due
     * to having been set explicitly via <code>setProperty()</code>
     * or to having been cached for property
     * synchronization purposes, subsequent calls to
     * <code>getProperty(name)</code> will return
     * <code>java.awt.Image.UndefinedProperty</code>, and <code>name</code>
     * will not appear on the list of properties emitted by
     * <code>getPropertyNames()</code>.  To delete the property from the
     * local property set of the node, <code>removeProperty()</code> should
     * be used.
     *
     * @param name A <code>String</code> naming the property to be suppressed.
     *
     * @throws IllegalArgumentException if
     * <code>name</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>name</code>
     * conflicts with Synthetic property.
     */
    public synchronized void suppressProperty(String name) {
        if (name == null)
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	if (synthProps.contains(new CaselessStringKey(name))) {
	    throw new IllegalArgumentException(JaiI18N.getString("RenderedOp5"));
	}

        nodeSupport.suppressProperty(name);
    }

    /*****************************************************************
     * The following methods override public or protected methods in *
     * PlanarImage  thus causing this node to be rendered.           *
     ****************************************************************/

    /**
     * Renders the node if it has not already been rendered, and returns
     * the X coordinate of the leftmost column of the rendered image.
     */
    public int getMinX() {
        createRendering();
        return theImage.getMinX();
    }

    /**
     * Renders the node if it has not already been rendered, and returns
     * the X coordinate of the uppermost row of the rendered image.
     */
    public int getMinY() {
        createRendering();
        return theImage.getMinY();
    }

    /**
     * Renders the node if it has not already been rendered,
     * and returns the width of the rendered image.
     */
    public int getWidth() {
        createRendering();
        return theImage.getWidth();
    }

    /**
     * Renders the node if it has not already been rendered,
     * and returns the height of the rendered image.
     */
    public int getHeight() {
        createRendering();
        return theImage.getHeight();
    }

    /**
     * Renders the node if it has not already been rendered,
     * and returns the tile width of the rendered image.
     */
    public int getTileWidth() {
        createRendering();
        return theImage.getTileWidth();
    }

    /**
     * Renders the node if it has not already been rendered,
     * and returns the tile height of the rendered image.
     */
    public int getTileHeight() {
        createRendering();
        return theImage.getTileHeight();
    }

    /**
     * Renders the node if it has not already been rendered,
     * and returns the tile grid X offset of the rendered image.
     */
    public int getTileGridXOffset() {
        createRendering();
        return theImage.getTileGridXOffset();
    }

    /**
     * Renders the node if it has not already been rendered,
     * and returns the tile grid Y offset of the rendered image.
     */
    public int getTileGridYOffset() {
        createRendering();
        return theImage.getTileGridYOffset();
    }

    /**
     * Renders the node if it has not already been rendered, and
     * returns the <code>SampleModel</code> of the rendered image.
     */
    public SampleModel getSampleModel() {
        createRendering();
        return theImage.getSampleModel();
    }

    /**
     * Renders the node if it has not already been rendered, and
     * returns the <code>ColorModel</code> of the rendered image.
     */
    public ColorModel getColorModel() {
        createRendering();
        return theImage.getColorModel();
    }

    /**
     * Renders the node if it has not already been rendered,
     * and returns the specified tile of the rendered image.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     *
     * @return  The requested tile as a <code>Raster</code>.
     */
    public Raster getTile(int tileX, int tileY) {
        createRendering();
        return theImage.getTile(tileX, tileY);
    }

    /**
     * Renders the node if it has not already been rendered, and
     * returns the entire rendered image as a <code>Raster</code>.
     */
    public Raster getData() {
        createRendering();
        return theImage.getData();
    }

    /**
     * Renders the node if it has not already been rendered, and
     * returns a specified rectangular region of the rendered
     * image as a <code>Raster</code>.
     */
    public Raster getData(Rectangle rect) {
        createRendering();
        return theImage.getData(rect);
    }

    /**
     * Renders the node if it has not already been rendered, and
     * copies and returns the entire rendered image into a single raster.
     */
    public WritableRaster copyData() {
        createRendering();
        return theImage.copyData();
    }

    /**
     * Renders the node if it has not already been rendered, and
     * copies a specified rectangle of the rendered image into
     * the given <code>WritableRaster</code>.
     *
     * @param raster A <code>WritableRaster</code> to be filled with image data.
     * @return A reference to the supplied <code>WritableRaster</code>.
     *
     */
    public WritableRaster copyData(WritableRaster raster) {
        createRendering();
        return theImage.copyData(raster);
    }

    /**
     * Renders the node if it has not already been rendered, and
     * returns the tiles indicated by the <code>tileIndices</code>
     * of the rendered image as an array of <code>Raster</code>s.
     *
     * @param tileIndices An array of Points representing TileIndices.
     * @return An array of Raster containing the tiles corresponding
     *         to the given TileIndices.
     *
     * @throws IllegalArgumentException  If <code>tileIndices</code> is
     *         <code>null</code>.
     */
    public Raster[] getTiles(Point tileIndices[]) {
        createRendering();
        return theImage.getTiles(tileIndices);
    }

    /**
     * Queues a list of tiles for computation.  Registered listeners
     * will be notified after each tile has been computed.  The event
     * source parameter passed to such listeners will be the node itself;
     * the image parameter will be the rendering of the node.  The
     * <code>RenderedOp</code> itself in fact should monitor any
     * <code>TileComputationListener</code> events of its rendering and
     * forward any such events to any of its registered listeners.
     *
     * @param tileIndices A list of tile indices indicating which tiles
     *        to schedule for computation.
     * @throws IllegalArgumentException  If <code>tileIndices</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public TileRequest queueTiles(Point[] tileIndices) {
        createRendering();
        return theImage.queueTiles(tileIndices);
    }

    /**
     * Issue an advisory cancellation request to nullify processing of
     * the indicated tiles.
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
        createRendering();
        theImage.cancelTiles(request, tileIndices);
    }

    /**
     * Renders the node if it has not already been rendered.
     * Hints that the given tiles of the rendered image might be
     * needed in the near future.
     *
     * @param tileIndices A list of tileIndices indicating which tiles
     *        to prefetch.
     *
     * @throws IllegalArgumentException  If <code>tileIndices</code> is
     *         <code>null</code>.
     */
    public void prefetchTiles(Point tileIndices[]) {
        createRendering();
        theImage.prefetchTiles(tileIndices);
    }

    // ----- Methods dealing with source Vector: forward call to ParamBlock.

    /**
     * Adds a <code>PlanarImage</code> source to the
     * <code>ParameterBlock</code> of this node.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * <p><i> Note that the behavior of this method has changed as of
     * Java Advanced Imaging 1.1.  To obtain the previous behavior use
     * <code>getRendering().addSource()</code>.  The description of the
     * previous behavior is as follows:
     * <blockquote>
     *
     * Renders the node if it has not already been rendered, and
     * adds a <code>PlanarImage</code> source to the list of sources
     * of the rendered image.
     *
     * </blockquote>
     * </i>
     *
     * @param source The source to be added to the
     *        <code>ParameterBlock</code>
     *
     * @throws IllegalArgumentException if <code>source</code> is
     *         <code>null</code>.
     *
     * @deprecated as of JAI 1.1. Use <code>addSource(Object)</code>.
     */
    public synchronized void addSource(PlanarImage source) {
        Object sourceObject = source;
        addSource(sourceObject);
    }

    /**
     * Sets the specified source stored in the <code>ParameterBlock</code>
     * of this node to a new <code>PlanarImage</code> source.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * <p><i> Note that the behavior of this method has changed as of
     * Java Advanced Imaging 1.1.  To obtain the previous behavior use
     * <code>getRendering().setSource()</code>.  The description of the
     * previous behavior is as follows:
     * <blockquote>
     *
     * Renders the node if it has not already been rendered, and
     * sets the specified source of the rendered image to the
     * supplied <code>PlanarImage</code>.
     * An <code>ArrayIndexOutOfBoundsException</code> may be thrown if
     * an invalid <code>index</code> is supplied.
     *
     * </blockquote>
     * </i>
     *
     * @param source The source, as a <code>PlanarImage</code>.
     * @param index The index of the source.
     * @throws IllegalArgumentException if <code>source</code> is
     *         <code>null</code>.
     * @throws ArrayIndexOutOfBoundsException if
     *         <code>index</code> is invalid.
     *
     * @deprecated as of JAI 1.1. Use <code>setSource(Object, int)</code>.
     */
    public synchronized void setSource(PlanarImage source, int index) {
        Object sourceObject = source;
        setSource(sourceObject, index);
    }

    /**
     * Returns the specified <code>PlanarImage</code> source stored in the
     * <code>ParameterBlock</code> of this node.
     * If there is no source corresponding to the specified index, an
     * <code>ArrayIndexOutOfBoundsException</code> will be thrown.
     *
     * <p><i> Note that the behavior of this method has changed as of
     * Java Advanced Imaging 1.1.  To obtain the previous behavior use
     * <code>getRendering().getSource()</code>.  The description of the
     * previous behavior is as follows:
     * <blockquote>
     *
     * Renders the node if it has not already been rendered, and
     * returns the specified <code>PlanarImage</code> source of
     * the rendered image. If there is no source corresponding to
     * the specified index, this method will throw an
     * <code>ArrayIndexOutOfBoundsException</code>.
     * The source returned may differ from the source stored in
     * the <code>ParameterBlock</code> of this node.
     *
     * </blockquote>
     * </i>
     *
     * @param index  The index of the desired source.
     * @return       A <code>PlanarImage</code> source.
     * @throws ArrayIndexOutOfBoundsException if
     *         <code>index</code> is invalid.
     * @throws ClassCastException if the source at the indicated index is
     *         not a <code>PlanarImage</code>.
     *
     * @deprecated as of JAI 1.1. Use <code>getSourceObject()</code>.
     */
    public PlanarImage getSource(int index) {
        return (PlanarImage)nodeSupport.getParameterBlock().getSource(index);
    }

    /**
     * Removes the specified <code>PlanarImage</code> source from the
     * <code>ParameterBlock</code> of this node.
     *
     * <p><i> Note that the behavior of this method has changed as of
     * Java Advanced Imaging 1.1.  To obtain the previous behavior use
     * <code>getRendering().removeSource()</code>.  The description of the
     * previous behavior is as follows:
     * <blockquote>
     *
     * Renders the node if it has not already been rendered, and
     * removes a <code>PlanarImage</code> source from the list
     * of sources of the rendered image.
     *
     * </blockquote>
     * </i>
     *
     * @param source A <code>PlanarImage</code> to be removed.
     *
     * @throws IllegalArgumentException if
     * <code>source</code> is <code>null</code>.
     * @return <code>true</code> if the element was present, <code>false</code>
     * otherwise.
     *
     * @deprecated as of JAI 1.1. Use <code>removeSource(Object)</code>.
     */
    public synchronized boolean removeSource(PlanarImage source) {
        Object sourceObject = source;
        return removeSource(sourceObject);
    }

    /**
     * Adds a source to the <code>ParameterBlock</code> of this node.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * <p> The node is added automatically as a sink if the source is a
     * <code>PlanarImage</code> or a <code>CollectionImage</code>.
     *
     * @param source The source to be added to the
     *        <code>ParameterBlock</code>
     * @throws IllegalArgumentException if
     *         <code>source</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public synchronized void addSource(Object source) {
        if (source == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        ParameterBlock pb =
            (ParameterBlock)nodeSupport.getParameterBlock().clone();
        pb.addSource(source);
        nodeSupport.setParameterBlock(pb);

        if(source instanceof PlanarImage) {
            ((PlanarImage)source).addSink(this);
        } else if(source instanceof CollectionImage) {
            ((CollectionImage)source).addSink(this);
        }
    }

    /**
     * Sets the specified source stored in the <code>ParameterBlock</code>
     * of this node to a new source object.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * <p> The node is added automatically as a sink if the source is a
     * <code>PlanarImage</code> or a <code>CollectionImage</code>.  If
     * appropriate the node is removed as a sink of any previous source
     * at the same index.
     *
     * @param source The Source to be set.
     * @param index  The Index at which it is to be set.
     *
     * @throws IllegalArgumentException if
     *         <code>source</code> is <code>null</code>.
     * @throws ArrayIndexOutOfBoundsException if
     *         <code>index</code> is invalid.
     *
     * @since JAI 1.1
     */
    public synchronized void setSource(Object source, int index) {
        if (source == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        ParameterBlock pb =
            (ParameterBlock)nodeSupport.getParameterBlock().clone();

        if(index < pb.getNumSources()) {
            Object priorSource = pb.getSource(index);
            if(priorSource instanceof PlanarImage) {
                ((PlanarImage)priorSource).removeSink(this);
            } else if(priorSource instanceof CollectionImage) {
                ((CollectionImage)priorSource).removeSink(this);
            }
        }

        pb.setSource(source, index);
        nodeSupport.setParameterBlock(pb);

        if(source instanceof PlanarImage) {
            ((PlanarImage)source).addSink(this);
        } else if(source instanceof CollectionImage) {
            ((CollectionImage)source).addSink(this);
        }
    }

    /**
     * Removes the specified <code>Object</code> source from the
     * <code>ParameterBlock</code> of this node.
     *
     * <p> The node is removed automatically as a sink if the source is a
     * <code>PlanarImage</code> or a <code>CollectionImage</code>.
     *
     * @param source A <code>Object</code> to be removed.
     *
     * @throws IllegalArgumentException if
     * <code>source</code> is <code>null</code>.
     * @return <code>true</code> if the element was present, <code>false</code>
     * otherwise.
     *
     * @since JAI 1.1
     */
    public synchronized boolean removeSource(Object source) {
        if (source == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        ParameterBlock pb =
            (ParameterBlock)nodeSupport.getParameterBlock().clone();

        Vector nodeSources = pb.getSources();
        if(nodeSources.contains(source)) {
            if(source instanceof PlanarImage) {
                ((PlanarImage)source).removeSink(this);
            } else if(source instanceof CollectionImage) {
                ((CollectionImage)source).removeSink(this);
            }
        }

        boolean result = nodeSources.remove(source);
        nodeSupport.setParameterBlock(pb);

        return result;
    }

    /**
     * Returns the specified <code>PlanarImage</code> source stored in the
     * <code>ParameterBlock</code> of this node.
     * If there is no source corresponding to the specified index, an
     * <code>ArrayIndexOutOfBoundsException</code> will be thrown.
     *
     * @param index  The index of the desired source.
     * @return       A <code>PlanarImage</code> source.
     * @throws ArrayIndexOutOfBoundsException if
     *         <code>index</code> is invalid.
     * @throws ClassCastException if the source at the indicated index is
     *         not a <code>PlanarImage</code>.
     *
     * @since JAI 1.1
     */
    public PlanarImage getSourceImage(int index) {
        return (PlanarImage)nodeSupport.getParameterBlock().getSource(index);
    }

    /**
     * Returns the specified source stored in the
     * <code>ParameterBlock</code> of this node.
     * If there is no source corresponding to the specified index, an
     * <code>ArrayIndexOutOfBoundsException</code> will be thrown.
     *
     * @param index  The index of the source.
     * @throws ArrayIndexOutOfBoundsException if
     *         <code>index</code> is invalid.
     *
     * @since JAI 1.1
     */
    public synchronized Object getSourceObject(int index) {
        return nodeSupport.getParameterBlock().getSource(index);
    }

    /**
     * Returns the number of sources stored in the
     * <code>ParameterBlock</code> of this node.
     * This may differ from the number of sources of the rendered image.
     */
    public int getNumSources() {
        // This method must return the number of sources of this node,
        // not the number of sources of the rendered image. Otherwise,
        // it'll cause exception in some cases.
        return nodeSupport.getParameterBlock().getNumSources();
    }

    /**
     * Returns a clone of the <code>Vector</code> of sources stored in the
     * <code>ParameterBlock</code> of this node.
     * This may differ from the source vector of the rendering of the node.
     */
    public synchronized Vector getSources() {
        Vector srcs = nodeSupport.getParameterBlock().getSources();
        return srcs == null ? null : (Vector)srcs.clone();
    }

    /**
     * Replaces the sources in the <code>ParameterBlock</code> of this node
     * with a new list of sources.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * <p> The node is added automatically as a sink of any source which is a
     * <code>PlanarImage</code> or a <code>CollectionImage</code>.  It is
     * also automatically removed as a sink of any such prior sources which
     * are no longer sources.
     *
     * @param sourceList  A <code>List</code> of sources.
     *
     * @throws IllegalArgumentException if
     *         <code>sourceList</code> is <code>null</code>.
     */
    public synchronized void setSources(List sourceList) {
        if (sourceList == null)
	    throw new IllegalArgumentException(
                JaiI18N.getString("Generic0"));

        ParameterBlock pb =
            (ParameterBlock)nodeSupport.getParameterBlock().clone();

        Iterator it = pb.getSources().iterator();
        while(it.hasNext()) {
            Object priorSource = it.next();
            if(!sourceList.contains(priorSource)) {
                if(priorSource instanceof PlanarImage) {
                    ((PlanarImage)priorSource).removeSink(this);
                } else if(priorSource instanceof CollectionImage) {
                    ((CollectionImage)priorSource).removeSink(this);
                }
            }
        }

        pb.removeSources();

        int size = sourceList.size();
        for (int i = 0; i < size; i++) {
            Object src = sourceList.get(i);
            pb.addSource(src);
            if(src instanceof PlanarImage) {
                ((PlanarImage)src).addSink(this);
            } else if(src instanceof CollectionImage) {
                ((CollectionImage)src).addSink(this);
            }
        }

        nodeSupport.setParameterBlock(pb);
    }

    /**
     * Removes all the sources stored in the
     * <code>ParameterBlock</code> of this node.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * <p> The node is removed automatically as a sink of any source which
     * is a <code>PlanarImage</code> or a <code>CollectionImage</code>.
     */
    public synchronized void removeSources() {
        ParameterBlock pb =
            (ParameterBlock)nodeSupport.getParameterBlock().clone();
        Iterator it = pb.getSources().iterator();
        while(it.hasNext()) {
            Object priorSource = it.next();
            if(priorSource instanceof PlanarImage) {
                ((PlanarImage)priorSource).removeSink(this);
            } else if(priorSource instanceof CollectionImage) {
                ((CollectionImage)priorSource).removeSink(this);
            }
            it.remove();
        }
        nodeSupport.setParameterBlock(pb);
    }

    // ----- Methods dealing with sinks Vector.

    /**
     * Adds a <code>PlanarImage</code> sink to the list of sinks of the node.
     *
     * <p><i> Note that the behavior of this method has changed as of
     * Java Advanced Imaging 1.1.  To obtain the previous behavior use
     * <code>getRendering().addSink()</code>.  The description of the
     * previous behavior is as follows:
     * <blockquote>
     *
     * Renders the node if it has not already been rendered, and
     * adds a <code>PlanarImage</code> sink to the list of sinks
     * of the rendered image.
     *
     * </blockquote>
     *
     * <p> Note also that this class no longer overrides
     * <code>getSinks()</code>.  To obtain the previous behavior of
     * <code>getSinks()</code> use <code>getRendering().getSinks()</code>.
     * </i>
     *
     * @throws IllegalArgumentException if
     * <code>sink</code> is <code>null</code>.
     */
    public synchronized void addSink(PlanarImage sink) {
        if (sink == null) {
	  throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        super.addSink(sink);
    }

    /**
     * Removes a <code>PlanarImage</code> sink from the list of sinks of
     * the node.
     *
     * <p><i> Note that the behavior of this method has changed as of
     * Java Advanced Imaging 1.1.  To obtain the previous behavior use
     * <code>getRendering().removeSink()</code>.  The description of the
     * previous behavior is as follows:
     * <blockquote>
     *
     * Renders the node if it has not already been rendered, and
     * removes a <code>PlanarImage</code> sink from the list of sinks
     * of the rendered image.
     *
     * </blockquote>
     *
     * <p> Note also that this class no longer overrides
     * <code>getSinks()</code>.  To obtain the previous behavior of
     * <code>getSinks()</code> use <code>getRendering().getSinks()</code>.
     * </i>
     *
     * @throws IllegalArgumentException if
     * <code>sink</code> is <code>null</code>.
     */
    public synchronized boolean removeSink(PlanarImage sink) {
        if (sink == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        return super.removeSink(sink);
    }

    /**
     * Removes all sinks from the list of sinks of the node.
     *
     * @since JAI 1.1
     */
    public void removeSinks() {
        super.removeSinks();
    }

    /**
     * Adds a sink to the list of node sinks.  If the sink is an
     * instance of <code>PropertyChangeListener</code> it will be
     * notified in the same manner as registered listeners for the
     * changes to the "Rendering" property of this node as long as its
     * <code>WeakReference</code> has not yet been cleared.
     *
     * @throws IllegalArgumentException if
     * <code>sink</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public boolean addSink(Object sink) {
        if (sink == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        return super.addSink(sink);
    }

    /**
     * Removes a sink from the list of node sinks.  If the sink
     * is a <code>PropertyChangeListener</code> for the "Rendering"
     * property of this node it will no longer be eligible for
     * notification events indicating a change in this property.
     *
     * @throws IllegalArgumentException if
     * <code>sink</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public boolean removeSink(Object sink) {
        if (sink == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        return super.removeSink(sink);
    }

    /* ----- Image coordinate mapping methods ----- */

    /**
     * Computes the position in the specified source that best
     * matches the supplied destination image position. If it
     * is not possible to compute the requested position,
     * <code>null</code> will be returned. If the point is mapped
     * outside the source bounds, the coordinate value or <code>null</code>
     * may be returned at the discretion of the implementation.
     *
     * <p>If the rendering of the node is an <code>OpImage</code>, the
     * call is forwarded to the equivalent method of the rendering.
     * Otherwise <code>destPt</code> is returned to indicate the identity
     * mapping. In either case if the node had not been rendered it will
     * be.</p>
     *
     * @param destPt the position in destination image coordinates
     * to map to source image coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>destPt</code> or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>destPt</code> is
     * <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>sourceIndex</code> is
     * negative or greater than or equal to the number of sources.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapDestPoint(Point2D destPt, int sourceIndex) {
        if (destPt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("Generic1"));
        }

        createRendering();

        if(theImage != null && theImage instanceof OpImage) {
            return ((OpImage)theImage).mapDestPoint(destPt, sourceIndex);
        }

        return destPt;
    }

    /**
     * Computes the position in the destination that best
     * matches the supplied source image position. If it
     * is not possible to compute the requested position,
     * <code>null</code> will be returned. If the point is mapped
     * outside the destination bounds, the coordinate value or
     * <code>null</code> may be returned at the discretion of the
     * implementation.
     *
     * <p>If the rendering of the node is an <code>OpImage</code>, the
     * call is forwarded to the equivalent method of the rendering.
     * Otherwise <code>sourcePt</code> is returned to indicate the identity
     * mapping. In either case if the node had not been rendered it will
     * be.</p>
     *
     * @param sourcePt the position in source image coordinates
     * to map to destination image coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>sourcePt</code> or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>sourcePt</code> is
     * <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>sourceIndex</code> is
     * negative or greater than or equal to the number of sources.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt, int sourceIndex) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("Generic1"));
        }

        createRendering();

        if(theImage != null && theImage instanceof OpImage) {
            return ((OpImage)theImage).mapSourcePoint(sourcePt, sourceIndex);
        }

        return sourcePt;
    }


    /* ----- Object cleanup ----- */

    /**
     * Hints that this node and its rendering will no longer be used.
     *
     * <p>If <code>theImage</code> is non-<code>null</code>, then this
     * call is first forwarded to <code>theImage.dispose()</code>.
     * Subsequent to this <code>super.dispose()</code> is invoked.</p>
     *
     * <p> The results of referencing an image after a call to
     * <code>dispose()</code> are undefined.</p>
     *
     * @since JAI 1.1.2
     */
    public synchronized void dispose() {
        if(isDisposed) {
            return;
        }

        isDisposed = true;

        if(theImage != null) {
            theImage.dispose();
        }

        super.dispose();
    }

    /* ----- [De]serialization methods ----- */

    /** Serializes the <code>RenderedOp</code>. */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Write non-static and non-transient fields.
        out.defaultWriteObject();

        // Explicitly serialize the required superclass fields.
        out.writeObject(eventManager);
        out.writeObject(properties);
    }

    /**
     * Deserialize the <code>RenderedOp</code>.
     */
    private synchronized void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        // Read non-static and non-transient fields.
        in.defaultReadObject();

        // Explicitly deserialize the required superclass fields.
        eventManager = (PropertyChangeSupportJAI)in.readObject();
        properties = (WritablePropertySourceImpl)in.readObject();

        // If this operation requires immediate rendering then render it.
	OperationDescriptor odesc = (OperationDescriptor)
		    getRegistry().getDescriptor(
			"rendered", nodeSupport.getOperationName());

        if (odesc.isImmediate()) {
            createRendering();
        }
    }

    void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener =
            (ImagingListener)getRenderingHints().get(JAI.KEY_IMAGING_LISTENER);

        listener.errorOccurred(message, e, this, false);
    }
}
