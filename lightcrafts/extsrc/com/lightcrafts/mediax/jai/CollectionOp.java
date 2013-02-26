/*
 * $RCSfile: CollectionOp.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2006/06/16 22:52:05 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import com.lightcrafts.mediax.jai.registry.CIFRegistry;
import com.lightcrafts.mediax.jai.registry.RCIFRegistry;
import com.lightcrafts.mediax.jai.registry.CollectionRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderableCollectionRegistryMode;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.PropertyUtil;

/**
 * A node in a <code>CollectionImage</code> chain. A <code>CollectionOp</code>
 * stores an operation name, a <code>ParameterBlock</code> containing sources
 * and parameters, and a <code>RenderingHints</code> containing hints which
 * may be used in rendering the node.  A set of nodes may be joined together
 * via the source <code>Vector</code>s within their respective
 * <code>ParameterBlock</code>s to form a <u>d</u>irected <u>a</u>cyclic
 * <u>g</u>raph (DAG).  The topology, i.e., connectivity, of the graph may be
 * altered by changing the node's sources.  The operation name, parameters,
 * and rendering hints may also be changed.  A <code>CollectionOp</code> may
 * be used in either the rendered or the renderable mode for
 * <code>Collection</code>s, i.e., "collection" or "renderableCollection"
 * mode, respectively.
 *
 * <p> A <code>CollectionOp</code> may be constructed directly as, for example,
 * <pre>
 * <code>
 * Collection srcCol;
 * double[] constants;
 * ParameterBlock pb =
 *     (new ParameterBlock()).addSource(srcCol).add(constants);
 * CollectionOp node =
 *     new CollectionOp("addConstToCollection", pb, null);
 * </code>
 * </pre>
 * or by the <code>createCollection</code> or <code>createCollectionNS()</code>
 * "collection" mode methods or the <code>createRenderableCollection()</code>
 * or <code>createRenderableCollectionNS()</code> "renderableCollection" mode
 * methods defined in the <code>JAI</code> class.  The difference between
 * direct construction of a node and creation via a convenience method is that
 * in the latter case:
 *
 * <ol>
 * <li> It is verified that the operation supports the appropriate mode,
 *      i.e., "collection" or "renderableCollection".</li>
 * <li> It is verified that the operation generates a
 *      <code>CollectionImage</code>, a <code>RenderedImage</code>
 *      ("collection" mode only), or a <code>RenderableImage</code>
 *      ("renderableCollection" mode only).</li>
 * <li> Global <code>RenderingHints</code> maintained by the <code>JAI</code>
 *      instance are merged with the local <code>RenderingHints</code> with the
 *      local hints taking precedence.</li>
 * <li> Using the <code>validateArguments()</code> method of the associated
 *      <code>OperationDescriptor</code>, the arguments (sources and parameters)
 *      are validated as being compatible with the specified operation in
 *      the appropriate mode.</li>
 * <li> If the arguments are valid, then the <code>CollectionOp</code> is
 *      created; otherwise any source <code>Collection</code>s are
 *      "unwrapped" until a valid argument list is obtained or it is
 *      determined that such is impossible.
 * <li> If the operation is in the rendered mode and is defined to be
 *      "immediate" (the <code>isImmediate()</code> method of the corresponding
 *      <code>OperationDescriptor</code> returns <code>true</code>)
 *      then the node is rendered.</li>
 * </ol>
 *
 * <p> When a chain of nodes is rendered by any means a "parallel" chain of
 * <code>CollectionImage</code>s is created.  Each node in the chain of
 * <code>CollectionOp</code>s corresponds to a node in the chain of
 * <code>CollectionImage</code>s.  <code>Collection</code> methods invoked
 * on the <code>CollectionOp</code> are in general forwarded to the associated
 * <code>CollectionImage</code> which is referred to as the <i>rendering</i>
 * of the node.  The rendering of the node may be a rendered or renderable
 * <code>CollectionImage</code>, i.e., eventually contain
 * <code>RenderedImage</code>s or <code>RenderableImage</code>s, respectively,
 * depending on the mode in which the node is used.
 *
 * <p> The translation between <code>CollectionOp</code> chains and
 * <code>CollectionImage</code> chains makes  use of two levels of
 * indirection provided by the <code>OperationRegistry</code> and either the
 * <code>CollectionImageFactory</code> (CIF) or the
 * <code>RenderableCollectionImageFactory</code> (RCIF) facilities.
 * First, the local <code>OperationRegistry</code> is used to map the
 * operation name into a CIF or RCIF.  This factory then constructs
 * a <code>CollectionImage</code>.  The local
 * <code>OperationRegistry</code> is used in order to take advantage
 * of the best possible implementation of the operation.
 *
 * <p> A node may be rendered explicitly by invoking the method
 * <code>getCollection()</code> which also returns the rendering of the
 * node.  A node may be rendered implicitly by invoking any method
 * defined in the <code>Collection</code> interface.  A rendering of a
 * node may also be obtained by means of the <code>createInstance()</code>
 * method.  This method returns a <code>Collection</code> rendering without
 * marking the node as having been rendered.  If the node is not
 * marked as rendered then it will not fire
 * <code>CollectionChangeEvent</code>s as described below.
 *
 * <p> <code>CollectionOp</code> nodes may participate in Java Bean-style
 * events.  The <code>PropertyChangeEmitter</code> methods may be used
 * to register and unregister <code>PropertyChangeListener</code>s.
 * <code>CollectionOp</code>s are also <code>PropertyChangeListener</code>s
 * so that they may be registered as listeners of other
 * <code>PropertyChangeEmitter</code>s or the equivalent.  Each
 * <code>CollectionOp</code> also automatically receives any
 * <code>CollectionChangeEvent</code>s emitted by any of its sources which
 * are also <code>CollectionOp</code>s and <code>RenderingChangeEvent</code>s
 * from any <code>RenderedOp</code> sources.
 *
 * <p> Certain <code>PropertyChangeEvent</code>s may be emitted by the
 * <code>CollectionOp</code>.  These include the
 * <code>PropertyChangeEventJAI</code>s and
 * <code>PropertySourceChangeEvent</code>s required by virtue of implementing
 * the <code>OperationNode</code> interface.  Additionally a
 * <code>CollectionChangeEvent</code> may be emitted if the node is
 * operating in the "collection" mode, has already been rendered, and one of
 * the following conditions is satisfied:
 * <ul>
 * <li>any of the critical attributes is changed (edited), i.e., the
 * operation name, operation registry, node sources, parameters, or rendering
 * hints; or</li>
 * <li>the node receives a <code>CollectionChangeEvent</code> from one of
 * its <code>CollectionOp</code> sources or a <code>RenderingChangeEvent</code>
 * from one if its <code>RenderedOp</code>.</li>
 * </ul>
 * In either case the following sequence of actions should occur:
 * <ol>
 * <li> A. If the operation name or the registry has changed, a new
 * <code>CollectionImage</code> will be generated by the
 * <code>OperationRegistry</code> for the new operation.
 * <br> B. If the operation name has not changed, an attempt will be made to
 * re-use some elements of the previously generated
 * <code>CollectionImage</code> by invoking <code>update()</code> on the
 * <code>CollectionImageFactory</code> which generated it.  If this attempt
 * fails, a new <code>CollectionImage</code> for this operation will be
 * requested from the <code>OperationRegistry</code>.</li>
 * <li> A <code>CollectionChangeEvent</code> will be fired to all registered
 * listeners of the "Collection" <code>PropertyChangeEvent</code> and to all
 * sinks which are <code>PropertyChangeListener</code>s.  The new and old
 * values set on the event object correspond to the previous and current
 * <code>CollectionImage</code>s, respectively, associated with this node.</li>
 * </ol>
 *
 * <p> <code>CollectionOp</code> nodes are <code>WritablePropertySource</code>s
 * and so manage a name-value database of image meta-data also known as image
 * properties.  Properties may be set on and requested from a node.  The
 * value of a property not explicitly set on the node (via
 * <code>setProperty()</code>) is obtained from the property environment of
 * the node.  When a property is derived from the property environment it is
 * cached locally to ensure synchronization, i.e., that properties do not
 * change spontaneously if for example the same property is modified upstream.
 *
 * <p> The property environment of a <code>CollectionOp</code> is initially
 * derived from that of the corresponding <code>OperationDescriptor</code>
 * as maintained by the <code>OperationRegistry</code>.  It may be modified
 * locally by adding <code>PropertyGenerator</code>s, directives to copy
 * certain properties from specific sources, or requests to suppress certain
 * properties.  These modifications per se cannot be undone directly but
 * may be eliminated as a side effect of other changes to the node as
 * described below.
 * 
 * <p> When a property value is requested an attempt will be made to derive
 * it from the several entities in the following order of precedence:
 * <ol>
 * <li> local properties; </li>
 * <li> the rendering of the node if it is a <code>PropertySource</code>;</li>
 * <li> any registered <code>PropertyGenerator</code>s, or
 * <br> a source specified via a copy-from-source directive;</li>
 * <li> the first source which defines the property. </li>
 * </ol>
 * Local properties are those which have been cached locally either by virtue
 * of direct invocation of <code>setProperty()</code> or due to caching of a
 * property derived from the property environment.
 *
 * <p> All dynamically computed properties of a <code>CollectionOp</code> which
 * have been cached locally, i.e., those cached properties which were not set
 * by an explicit call to <code>setProperty()</code>, will be cleared when any
 * of the critical attributes of the node is edited.  By implication these
 * properties will also be cleared when a <code>CollectionChangeEvent</code>
 * is received from any node source.  The property environment or the cached
 * properties may also be cleared by invoking <code>resetProperties()</code>.
 *
 * @see CollectionImage
 * @see OperationRegistry
 * @see RenderableOp
 * @see RenderedOp
 *
 */
public class CollectionOp extends CollectionImage
    implements OperationNode, PropertyChangeListener {

    /**
     * An object to assist in implementing <code>OperationNode</code>.
     *
     * @since JAI 1.1
     */
    protected OperationNodeSupport nodeSupport;

    /**
     * The <code>PropertySource</code> containing the combined properties
     * of all of the node's sources.
     *
     * @since JAI 1.1
     */
    protected PropertySource thePropertySource;

    /**
     * Flag indicating whether the operation is being instantiated in
     * renderable mode.
     *
     * @since JAI 1.1
     */
    protected boolean isRenderable = false;

    /**
     * The RenderingHints when the node was last rendered, i.e., when
     * "theImage" was set to its current value.
     */
    private transient RenderingHints oldHints;

    /** Node event names. */
    private static Set nodeEventNames = null;

    static {
        nodeEventNames = new HashSet();
        nodeEventNames.add("operationname");
        nodeEventNames.add("operationregistry");
        nodeEventNames.add("parameterblock");
        nodeEventNames.add("sources");
        nodeEventNames.add("parameters");
        nodeEventNames.add("renderinghints");
    }

    /**
     * Constructs a <code>CollectionOp</code> that will be used to
     * instantiate a particular <code>Collection</code> operation from a given
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
     * until their values are actually required, i.e., when a collection
     * rendering is requested.
     *
     * <p> The node is added automatically as a sink of any
     * <code>PlanarImage</code> or <code>CollectionImage</code> sources.
     *
     * @param registry  The <code>OperationRegistry</code> to be used for
     *        instantiation.  if <code>null</code>, the default registry
     *        is used.  Saved by reference.
     * @param opName  The operation name.  Saved by reference.
     * @param pb  The sources and other parameters. If <code>null</code>,
     *        it is assumed that this node has no sources and parameters.
     *        This parameter is cloned.
     * @param hints  The rendering hints.  If <code>null</code>, it is assumed
     *        that no hints are associated with the rendering.
     *        This parameter is cloned.
     * @param isRenderable  Whether the operation is being executed in
     *	      renderable mode.
     *
     * @throws <code>IllegalArgumentException</code> if <code>opName</code>
     *         is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public CollectionOp(OperationRegistry registry,
                        String opName,
                        ParameterBlock pb,
                        RenderingHints hints,
			boolean isRenderable) {

        if (opName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

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

        // Initialize the various helper objects.
        eventManager = new PropertyChangeSupportJAI(this);

        properties = new WritablePropertySourceImpl(null, null, eventManager);

        nodeSupport = new OperationNodeSupport(getRegistryModeName(),
                                               opName,
                                               registry,
                                               pb,
                                               hints,
                                               eventManager);

        this.isRenderable = isRenderable;

        // Add the node as a PropertyChangeListener of itself for
        // the critical attributes of the node.  Case is ignored
        // in the property names but infix caps are used here anyway.
        addPropertyChangeListener("OperationName", this);
        addPropertyChangeListener("OperationRegistry", this);
        addPropertyChangeListener("ParameterBlock", this);
        addPropertyChangeListener("Sources", this);
        addPropertyChangeListener("Parameters", this);
        addPropertyChangeListener("RenderingHints", this);

        // Add self as a sink of any CollectionImage or PlanarImage sources.
        Vector nodeSources = pb.getSources();
        if(nodeSources != null) {
            Iterator it = nodeSources.iterator();
            while(it.hasNext()) {
                Object src = it.next();
                if(src instanceof CollectionImage) {
                    ((CollectionImage)src).addSink(this);
                } else if(src instanceof PlanarImage) {
                    ((PlanarImage)src).addSink(this);
                }
            }
        }
    }

    /**
     * Constructs a <code>CollectionOp</code> that will be used to
     * instantiate a particular <code>Collection</code> operation from a given
     * operation registry, an operation name, a <code>ParameterBlock</code>,
     * and a set of rendering hints.  The operation will use the rendered mode.
     *
     * <p> This method does not validate the contents of the supplied
     * <code>ParameterBlock</code>.  The caller should ensure that
     * the sources and parameters in the <code>ParameterBlock</code>
     * are suitable for the operation this node represents; otherwise
     * some form of error or exception may occur at the time of rendering.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when a collection
     * rendering is requested.
     *
     * @param registry  The <code>OperationRegistry</code> to be used for
     *        instantiation.  if <code>null</code>, the default registry
     *        is used.  Saved by reference.
     * @param opName  The operation name.  Saved by reference.
     * @param pb  The sources and other parameters. If <code>null</code>,
     *        it is assumed that this node has no sources and parameters.
     *        This parameter is cloned.
     * @param hints  The rendering hints.  If <code>null</code>, it is assumed
     *        that no hints are associated with the rendering.
     *        This parameter is cloned.
     *
     * @throws IllegalArgumentException if <code>opName</code> is
     *         <code>null</code>.
     */
    public CollectionOp(OperationRegistry registry,
                        String opName,
                        ParameterBlock pb,
                        RenderingHints hints) {
        this(registry, opName, pb, hints, false);
    }

    /**
     * Constructs a <code>CollectionOp</code> that will be used to
     * instantiate a particular <code>Collection</code> operation from a given
     * operation name, a <code>ParameterBlock</code>, and a set of
     * rendering hints.  The default operation registry is used.
     *
     * <p> This method does not validate the contents of the supplied
     * <code>ParameterBlock</code>.  The caller should ensure that
     * the sources and parameters in the <code>ParameterBlock</code>
     * are suitable for the operation this node represents; otherwise
     * some form of error or exception may occur at the time of rendering.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when a collection
     * rendering is requested.
     *
     * @param opName  The operation name.  Saved by reference.
     * @param pb  The sources and other parameters. If <code>null</code>,
     *        it is assumed that this node has no sources and parameters.
     *        This parameter is cloned.
     * @param hints  The rendering hints.  If <code>null</code>, it is assumed
     *        that no hints are associated with the rendering.
     *        This parameter is cloned.
     *
     * @throws <code>IllegalArgumentException</code> if <code>opName</code> is
     *         <code>null</code>.
     */
    public CollectionOp(String opName,
                        ParameterBlock pb,
                        RenderingHints hints) {
	this(null, opName, pb, hints);
    }

    /**
     * Constructs a <code>CollectionOp</code> that will be used to
     * instantiate a particular <code>Collection</code> operation from a given
     * operation registry, an operation name, and a
     * <code>ParameterBlock</code>  There are no rendering hints
     * associated with this operation.
     * The operation will use the rendered mode.
     *
     * <p> This method does not validate the contents of the supplied
     * <code>ParameterBlock</code>.  The caller should ensure that
     * the sources and parameters in the <code>ParameterBlock</code>
     * are suitable for the operation this node represents; otherwise
     * some form of error or exception may occur at the time of rendering.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when a collection
     * rendering is requested.
     *
     * @param registry  The <code>OperationRegistry</code> to be used for
     *        instantiation.  if <code>null</code>, the default registry
     *        is used.  Saved by reference.
     * @param opName  The operation name.  Saved by reference.
     * @param pb  The sources and other parameters. If <code>null</code>,
     *        it is assumed that this node has no sources and parameters.
     *        This parameter is cloned.
     *
     * @throws <code>IllegalArgumentException</code> if <code>opName</code> is
     *         <code>null</code>.
     *
     * @deprecated as of JAI 1.1.
     * @see #CollectionOp(OperationRegistry,String,ParameterBlock,RenderingHints)
     */
    public CollectionOp(OperationRegistry registry,
                        String opName,
                        ParameterBlock pb) {
        this(registry, opName, pb, null);
    }

    /**
     * Returns whether the operation is being instantiated in renderable mode.
     *
     * @since JAI 1.1
     */
    public boolean isRenderable() {
        return isRenderable;
    }

    /**
     * Returns the name of the <code>RegistryMode</code> corresponding to
     * this <code>CollectionOp</code>.
     *
     * @since JAI 1.1
     */
    public String getRegistryModeName() {
        return isRenderable ?
            RenderableCollectionRegistryMode.MODE_NAME :
            CollectionRegistryMode.MODE_NAME;
    }

    /* ----- Critical attribute accessors and mutators. ----- */

    /**
     * Returns the <code>OperationRegistry</code> that is used
     * by this node.  If the registry had not been set, the default
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
     * will be fired and a <code>CollectionChangeEvent</code> may be
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
    public String getOperationName() {
        return nodeSupport.getOperationName();
    }

    /**
     * Sets the name of the operation this node represents.
     * The parameter is saved by reference.
     *
     * <p> If the supplied name does not equal the current operation name, a
     * <code>PropertyChangeEventJAI</code> named "OperationName"
     * will be fired and a <code>CollectionChangeEvent</code> may be
     * fired if the node has already been rendered.
     *
     * @param opName  The new operation name to be set.
     *
     * @throws <code>IllegalArgumentException</code> if <code>opName</code> is
     *         <code>null</code>.
     */
    public synchronized void setOperationName(String opName) {
        nodeSupport.setOperationName(opName);
    }

    /** Returns a clone of the <code>ParameterBlock</code> of this node. */
    public ParameterBlock getParameterBlock() {
        return (ParameterBlock)nodeSupport.getParameterBlock().clone();
    }

    /**
     * Sets the <code>ParameterBlock</code> of this node.
     * If the specified new <code>ParameterBlock</code> is <code>null</code>,
     * it is assumed that this node has no input sources and parameters.
     * The supplied parameter is cloned.
     *
     * <p> This method does not validate the contents of the supplied
     * <code>ParameterBlock</code>.  The caller should ensure that
     * the sources and parameters in the <code>ParameterBlock</code>
     * are suitable for the operation this node represents; otherwise
     * some form of error or exception may occur at the time of rendering.
     *
     * <p> If the supplied <code>ParameterBlock</code> does not equal the
     * current <code>ParameterBlock</code>, a
     * <code>PropertyChangeEventJAI</code> named "ParameterBlock", "Sources",
     * or "Parameters" will be fired. A <code>CollectionChangeEvent</code>
     * may also be fired if the node has already been rendered.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when a collection
     * rendering is requested.
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
     * will be fired and a <code>CollectionChangeEvent</code> may be
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

    /* ----- Collection generation methods. ----- */

    /**
     * Returns the <code>Collection</code> rendering associated with
     * this operation.
     *
     * <p> This method does not validate the sources and parameters
     * stored in the <code>ParameterBlock</code> against the specification
     * of the operation this node represents.  It is the responsibility
     * of the caller to ensure that the data in the
     * <code>ParameterBlock</code> are suitable for this operation.
     * Otherwise, some kind of exception or error will occur.  Invoking
     * this method will cause any <code>DeferredData</code> parameters
     * in the <code>ParameterBlock</code> to be evaluated.
     *
     * <p> Invoking this method will cause any source <code>RenderedOp</code>
     * nodes to be rendered using <code>getRendering()</code> and any
     * source <code>CollectionOp</code> nodes to be rendered using
     * <code>getCollection()</code>.  Any <code>DeferredData</code> parameters
     * in the <code>ParameterBlock</code> will also be evaluated.
     *
     * @throws RuntimeException if the image factory charged with rendering
     *         the node is unable to create a rendering.
     */
    public Collection getCollection() {
        createCollection();
        return imageCollection;
    }

    /** Creates a <code>Collection</code> rendering if none exists. */
    private synchronized void createCollection() {
        if (imageCollection == null) {
            imageCollection = createInstance(true);
        }
    }

    /**
     * Instantiates a <code>Collection</code> operator that computes
     * the result of this <code>CollectionOp</code>.
     *
     * <p> This method does not validate the sources and parameters
     * stored in the <code>ParameterBlock</code> against the specification
     * of the operation this node represents.  It is the responsibility
     * of the caller to ensure that the data in the
     * <code>ParameterBlock</code> are suitable for this operation.
     * Otherwise, some kind of exception or error will occur.
     *
     * <p> Invoking this method will cause any source <code>RenderedOp</code>
     * or <code>CollectionOp</code> nodes to be rendered using their
     * respective <code>createInstance()</code> methods.  Any
     * <code>DeferredData</code> parameters in the <code>ParameterBlock</code>
     * will also be evaluated.
     *
     * @throws RuntimeException if the image factory charged with rendering
     *         the node is unable to create a rendering.
     */
    public synchronized Collection createInstance() {
        return createInstance(false);
    }

    /**
     * This method performs the actions described by the documentation of
     * <code>createInstance()</code>.  The parameter value selects the method
     * used to render the source(s).
     *
     * @throws RuntimeException if the image factory charged with rendering
     *         the node is unable to create a rendering.
     */
    private synchronized Collection createInstance(boolean isChainFrozen) {
        // Get the PB evaluating any DeferredData objects in the process.
        ParameterBlock args =
            ImageUtil.evaluateParameters(nodeSupport.getParameterBlock());

        ParameterBlock pb = new ParameterBlock();
        pb.setParameters(args.getParameters());

        int numSources = args.getNumSources();
        for (int i = 0; i < numSources; i++) {
            Object source = args.getSource(i);
            Object src = null;

            if (source instanceof RenderedOp) {
                src = isChainFrozen ?
                    ((RenderedOp)source).getRendering() :
                    ((RenderedOp)source).createInstance();
            } else if (source instanceof CollectionOp) {
                CollectionOp co = (CollectionOp)source;
                src = isChainFrozen ?
                    co.getCollection() :
                    co.createInstance();
            } else if (source instanceof RenderedImage ||
                       source instanceof RenderableImage ||
                       source instanceof Collection) {
                src = source;
            } else {
                // Source is some other type. Pass on (for now).
                src = source;
            }
            pb.addSource(src);
        }

        Collection instance = null;
        if(isRenderable) {
            instance = RCIFRegistry.create(nodeSupport.getRegistry(),
                                           nodeSupport.getOperationName(),
                                           pb);
        } else {
            CollectionImageFactory cif =
                CIFRegistry.get(nodeSupport.getRegistry(),
                                nodeSupport.getOperationName());
            instance = cif.create(pb, nodeSupport.getRenderingHints());

            // Set the CollectionImageFactory on the result.
            if(instance != null) {
                ((CollectionImage)instance).setImageFactory(cif);
            }
        }

        // Throw an error if the rendering is null.
        if (instance == null) {
            throw new RuntimeException(JaiI18N.getString("CollectionOp0"));
        }

        // Save the RenderingHints.
        oldHints = nodeSupport.getRenderingHints() == null ?
            null : (RenderingHints)nodeSupport.getRenderingHints().clone();

        return instance;
    }

    /**
     * Returns the <code>Collection</code> rendering associated with this
     * operation with any contained <code>RenderableImage</code>s rendered
     * using the supplied <code>RenderContext</code> parameter.  If the
     * operation is being executed in rendered mode
     * (<code>isRenderable()</code> returns <code>false</code>), invoking
     * this method is equivalent to invoking <code>getCollection()</code>,
     * i.e., the parameter is ignored.  If the operation is being
     * executed in renderable mode, the <code>Collection</code> will differ
     * from that returned by <code>getCollection()</code> due to any contained
     * <code>RenderableImage</code>s having been rendered.  If the
     * <code>Collection</code> contains any nested <code>Collection</code>s,
     * these will be unwrapped recursively such that a rendering is created
     * for all <code>RenderableImage</code>s encountered.  Any
     * <code>RenderingHints</code> in the <code>RenderContext</code> are
     * merged with those set on the node with the argument hints taking
     * precedence.
     *
     * @since JAI 1.1
     */
    public Collection createRendering(RenderContext renderContext) {
        if(!isRenderable) {
            return this;
        }

        // Merge argument hints with node hints.
        RenderingHints mergedHints =
            JAI.mergeRenderingHints(nodeSupport.getRenderingHints(),
                                    renderContext.getRenderingHints());
        if(mergedHints != renderContext.getRenderingHints()) {
            renderContext = (RenderContext)renderContext.clone();
            renderContext.setRenderingHints(mergedHints);
        }

        return renderCollection(imageCollection, renderContext);
    }

    /**
     * Returns a new <code>Collection</code> with any
     * <code>RenderableImage</code>s rendered using the supplied
     * <code>RenderContext</code>.  This method is re-entrant and
     * invokes itself if there is a nested <code>Collection</code>.
     */
    private Collection renderCollection(Collection cIn, RenderContext rc) {
        if(cIn == null || rc == null) {
            throw new IllegalArgumentException(); // no message.
        }

        Collection cOut;
        if(cIn instanceof Set) {
            cOut = Collections.synchronizedSet(new HashSet(cIn.size()));
        } else if(cIn instanceof SortedSet) {
            Comparator comparator = ((SortedSet)cIn).comparator();
            cOut = Collections.synchronizedSortedSet(new TreeSet(comparator));
        } else {
            cOut = new Vector(cIn.size());
        }

        Iterator it = cIn.iterator();
        while(it.hasNext()) {
            Object element = it.next();
            if(element instanceof RenderableImage) {
                cOut.add(((RenderableImage)cIn).createRendering(rc));
            } else if(element instanceof Collection) {
                cOut.add(renderCollection((Collection)element, rc));
            } else {
                cOut.add(element);
            }
        }

        return cOut;
    }

    /* ----- PropertyChangeListener method. ----- */

    /**
     * Implementation of <code>PropertyChangeListener</code>.
     *
     * <p> When invoked with an event which is an instance of either
     * <code>CollectionChangeEvent</code> or
     * <code>RenderingChangeEvent</code> emitted by a
     * <code>CollectionOp</code> or <code>RenderedOp</code> source,
     * respectively, the node will respond by
     * re-rendering itself while retaining any data possible.
     *
     * @since JAI 1.1
     */
    public synchronized void propertyChange(PropertyChangeEvent evt) {
        // If this is a renderable node just return as CollectionChangeEvents
        // should not be emitted for "renderablecollection" mode.
        if(isRenderable()) return;

        //
        // React if and only if the node has been rendered and
        // A: a non-PropertySourceChangeEvent PropertyChangeEventJAI
        //    was received from this node, or
        // B: a CollectionChangeEvent was received from a source node, or
        // C: a RenderingChangeEvent was received from a source node.
        //

        // Cache event and node sources.
        Object evtSrc = evt.getSource();
        Vector nodeSources = nodeSupport.getParameterBlock().getSources();

        // Get the name of the bean property and convert it to lower
        // case now for efficiency later.
        String propName = evt.getPropertyName().toLowerCase(Locale.ENGLISH);

        if(imageCollection != null &&
           ((evt instanceof PropertyChangeEventJAI &&
             evtSrc == this &&
             !(evt instanceof PropertySourceChangeEvent) &&
             nodeEventNames.contains(propName)) ||
            ((evt instanceof CollectionChangeEvent ||
              evt instanceof RenderingChangeEvent) &&
             nodeSources.contains(evtSrc)))) {

            // Save the previous rendering.
            Collection theOldCollection = imageCollection;

            // Initialize the event flag.
            boolean fireEvent = false;

            if(!(imageCollection instanceof CollectionImage)) {

                // Collection is not a CollectionImage so no update possible;
                // invalidate the entire rendering.
                fireEvent = true;
                imageCollection = null;

            } else if(evtSrc == this &&
                      (propName.equals("operationname") ||
                       propName.equals("operationregistry"))) {

                // Operation name or OperationRegistry changed:
                // invalidate the entire rendering.
                fireEvent = true;
                imageCollection = null;

            } else if(evt instanceof CollectionChangeEvent) {

                // Set the event flag.
                fireEvent = true;

                // Save the previous image factory.  We know that the old
                // Collection is a CollectionImage or the first branch of
                // the if-block would have been entered above.
                CollectionImageFactory oldCIF =
                    ((CollectionImage)theOldCollection).getImageFactory();

                if(oldCIF == null) {

                    // The factory is null: no update possible.
                    imageCollection = null;

                } else {

                    // CollectionChangeEvent from a source CollectionOp.
                    CollectionChangeEvent ccEvent = (CollectionChangeEvent)evt;

                    // Construct old and new ParameterBlocks.
                    Vector parameters =
                        nodeSupport.getParameterBlock().getParameters();
                    parameters = ImageUtil.evaluateParameters(parameters);
                    ParameterBlock oldPB =
                        new ParameterBlock((Vector)nodeSources.clone(),
                                           parameters);
                    ParameterBlock newPB =
                        new ParameterBlock((Vector)nodeSources.clone(),
                                           parameters);
                    int sourceIndex = nodeSources.indexOf(ccEvent.getSource());
                    oldPB.setSource(ccEvent.getOldValue(), sourceIndex);
                    newPB.setSource(ccEvent.getNewValue(), sourceIndex);

                    // Update the collection.
                    imageCollection =
                        oldCIF.update(oldPB, oldHints,
                                      newPB, oldHints,
                                      (CollectionImage)theOldCollection,
                                      this);
                }

            } else {
                // not op name, registry change, nor CollectionChangeEvent

                // Save the previous image factory.
                CollectionImageFactory oldCIF =
                    ((CollectionImage)theOldCollection).getImageFactory();

                if(oldCIF == null ||
                   oldCIF != CIFRegistry.get(nodeSupport.getRegistry(),
                                             nodeSupport.getOperationName())) {

                    // Impossible to update unless the old and new CIFs
                    // are equal and non-null.
                    imageCollection = null;

                    // Set event flag.
                    fireEvent = true;

                } else {

                    // Attempt to update the Collection rendering.

                    ParameterBlock oldPB = null;
                    ParameterBlock newPB = null;

                    boolean updateCollection = false;

                    if(propName.equals("parameterblock")) {
                        oldPB = (ParameterBlock)evt.getOldValue();
                        newPB = (ParameterBlock)evt.getNewValue();
                        updateCollection = true;
                    } else if(propName.equals("sources")) {
                        // Replace source(s)
                        Vector params =
                            nodeSupport.getParameterBlock().getParameters();
                        oldPB = new ParameterBlock((Vector)evt.getOldValue(),
                                                   params);
                        newPB = new ParameterBlock((Vector)evt.getNewValue(),
                                                   params);
                        updateCollection = true;
                    } else if(propName.equals("parameters")) {
                        // Replace parameter(s)
                        oldPB = new ParameterBlock(nodeSources,
                                                   (Vector)evt.getOldValue());
                        newPB = new ParameterBlock(nodeSources,
                                                   (Vector)evt.getNewValue());
                        updateCollection = true;
                    } else if(propName.equals("renderinghints")) {
                        oldPB = newPB = nodeSupport.getParameterBlock();
                        updateCollection = true;
                    } else if(evt instanceof RenderingChangeEvent) {
                        // Event from a RenderedOp source.

                        // Replace appropriate source.
                        int renderingIndex =
                            nodeSources.indexOf(evt.getSource());
                        Vector oldSources = (Vector)nodeSources.clone();
                        Vector newSources = (Vector)nodeSources.clone();
                        oldSources.set(renderingIndex, evt.getOldValue());
                        newSources.set(renderingIndex, evt.getNewValue());

                        Vector params =
                            nodeSupport.getParameterBlock().getParameters();

                        oldPB = new ParameterBlock(oldSources, params);
                        newPB = new ParameterBlock(newSources, params);

                        updateCollection = true;
                    }

                    if(updateCollection) {
                        // Set event flag.
                        fireEvent = true;

                        // Evaluate any DeferredData parameters.
                        oldPB = ImageUtil.evaluateParameters(oldPB);
                        newPB = ImageUtil.evaluateParameters(newPB);

                        // Update the collection.
                        RenderingHints newHints =
                            nodeSupport.getRenderingHints();
                        if((imageCollection =
                            oldCIF.update(oldPB, oldHints,
                                          newPB, newHints,
                                          (CollectionImage)theOldCollection,
                                          this)) != null) {
                            oldHints = newHints;
                        }
                    }
                }
            }

            // Re-render the node. This will only occur if imageCollection
            // has been set to null above.
            getCollection();

            // Fire an event if the flag was set.
            if(fireEvent) {
                // Clear the synthetic and cached properties and reset the
                // property source.
                resetProperties(true);

                // Create the event object.
                CollectionChangeEvent ccEvent =
                    new CollectionChangeEvent(this,
                                              theOldCollection,
                                              imageCollection);

                // Fire to all registered listeners.
                eventManager.firePropertyChange(ccEvent);

                // Fire to all PropertyChangeListener sinks.
                Set sinks = getSinks();
                if(sinks != null) {
                    Iterator it = sinks.iterator();
                    while(it.hasNext()) {
                        Object sink = it.next();
                        if(sink instanceof PropertyChangeListener) {
                            ((PropertyChangeListener)sink).propertyChange(ccEvent);
                        }
                    }
                }
            }
        }
    }

    /* ----- Property-related methods. ----- */

    /** Creates a <code>PropertySource</code> if none exists. */
    private synchronized void createPropertySource() {
        if (thePropertySource == null) {
            getCollection();

            PropertySource defaultPS = null;
            if(imageCollection instanceof PropertySource) {
                // Create a <code>PropertySource</code> wrapper of the rendering.
                defaultPS = new PropertySource() {
                        /**
                         * Retrieve the names from an instance of the node.
                         */
                        public String[] getPropertyNames() {
                            return ((PropertySource)imageCollection).getPropertyNames();
                        }

                        public String[] getPropertyNames(String prefix) {
                            return PropertyUtil.getPropertyNames(
                                       getPropertyNames(), prefix);
                        }

                        public Class getPropertyClass(String name) {
                            return null;
                        }

                        /**
                         * Retrieve the actual property values from a
                         * rendering of the node.
                         */
                        public Object getProperty(String name) {
                            return ((PropertySource)imageCollection).getProperty(name);
                        }
                    };
            }

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
            properties.removePropertySource(thePropertySource);
            thePropertySource = null;
        }
    }

    /**
     * Returns the names of properties available from this node.
     * These properties are a combination of those derived
     * from prior nodes in the operation chain and those set locally.
     *
     * @return An array of <code>String</code>s containing valid
     *         property names or <code>null</code> if there are none.
     *
     * @since JAI 1.1
     */
    public synchronized String[] getPropertyNames() {
        createPropertySource();
        return properties.getPropertyNames();
    }

    /**
     * Returns the class expected to be returned by a request for
     * the property with the specified name.  If this information
     * is unavailable, <code>null</code> will be returned.
     *
     * @return The <code>Class</code> expected to be return by a
     *         request for the value of this property or <code>null</code>.
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public Class getPropertyClass(String name) {
        createPropertySource();
        return properties.getPropertyClass(name);
    }

    /**
     * Gets a property from the property set of this <code>Collection</code>.
     * If the property name is not recognized,
     * <code>java.awt.Image.UndefinedProperty</code> will be returned.
     *
     * @param name the name of the property to get, as a String.
     * @return a reference to the property Object, or the value
     *         java.awt.Image.UndefinedProperty.
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public Object getProperty(String name) {
        createPropertySource();
        return properties.getProperty(name);
    }
    
    /**
     * Sets a local property on a node.  Local property settings override
     * properties derived from prior nodes in the operation chain.
     *
     * @param name a String representing the property name.
     * @param value the property's value, as an Object.
     * @exception IllegalArgumentException if <code>name</code>
     *                                     or <code>value</code>
     *                                     is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public void setProperty(String name, Object value) {
        createPropertySource();
        properties.setProperty(name, value);
    }

    /**
     * Removes the named property from the local property
     * set of the <code>CollectionOp</code> as well as from its property
     * environment.
     *
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public void removeProperty(String name) {
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
     * Adds a PropertyGenerator to the node.  The property values
     * emitted by this property generator override any previous
     * definitions.
     *
     * @param pg a PropertyGenerator to be added to this node's
     *        property environment.
     *
     * @since JAI 1.1
     */
    public void addPropertyGenerator(PropertyGenerator pg) {
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
     * @param name a String naming the property to be suppressed.
     * @throws <code>IllegalArgumentException</code> if 
     * <code>name</code> is <code>null</code>.
     *
     * @since JAI 1.1
     */
    public void suppressProperty(String name) {
        nodeSupport.suppressProperty(name);
    }

    /*****************************************************************
     * The following methods override public or protected methods in *
     * CollectionImage thus causing the Collection to be created.    *
     *****************************************************************/

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * returns the number of elements in this <code>Collection</code>.
     */
    public int size() {
        createCollection();
        return imageCollection.size();
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * returns <code>true</code> if this <code>Collection</code> contains
     * no element.
     */
    public boolean isEmpty() {
        createCollection();
        return imageCollection.isEmpty();
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * returns <code>true</code> if this <code>Collection</code> contains
     * the specified object.
     */
    public boolean contains(Object o) {
        createCollection();
        return imageCollection.contains(o);
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * returns an <code>Iterator</code> over the elements in this
     * <code>Collection</code>.
     */
    public Iterator iterator() {
        createCollection();
        return imageCollection.iterator();
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * returns an array containing all of the elements in this
     * <code>Collection</code>.
     */
    public Object[] toArray() {
        createCollection();
        return imageCollection.toArray();
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * returns an array containing all of the elements in this
     * <code>Collection</code> whose runtime type is that of the specified
     * array.
     *
     * @throws <code>ArrayStoreException</code> if the runtime type of the
     *         specified array is not a supertype of the runtime type of
     *         every element in this <code>Collection</code>.
     */
    public Object[] toArray(Object[] a) {
        createCollection();
        return imageCollection.toArray(a);
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * adds the specified object to this <code>Collection</code>.
     */
    public boolean add(Object o) {
        createCollection();
        return imageCollection.add(o);
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * removes the specified object from this <code>Collection</code>.
     */
    public boolean remove(Object o) {
        createCollection();
        return imageCollection.remove(o);
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * returns <code>true</code> if this <code>Collection</code> contains
     * all of the elements in the specified <code>Collection</code>.
     */
    public boolean containsAll(Collection c) {
        createCollection();
        return imageCollection.containsAll(c);
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * adds all of the elements in the specified <code>Collection</code>
     * to this <code>Collection</code>.
     */
    public boolean addAll(Collection c) {
        createCollection();
        return imageCollection.addAll(c);
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * removes all this <code>Collection</code>'s elements that are also
     * contained in the specified <code>Collection</code>.
     */
    public boolean removeAll(Collection c) {
        createCollection();
        return imageCollection.removeAll(c);
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * retains only the elements in this <code>Collection</code> that are
     * contained in the specified <code>Collection</code>.
     */
    public boolean retainAll(Collection c) {
        createCollection();
        return imageCollection.retainAll(c);
    }

    /**
     * Creates the <code>Collection</code> rendering if none yet exists, and
     * removes all of the elements from this <code>Collection</code>.
     */
    public void clear() {
        createCollection();
        imageCollection.clear();
    }
}
