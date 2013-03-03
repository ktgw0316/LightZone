/*
 * $RCSfile: RenderableOp.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:20 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.PropertyUtil;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ContextualRenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Vector;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;
import com.lightcrafts.mediax.jai.registry.CRIFRegistry;

/**
 * A node in a renderable imaging chain.  This is the Java Advanced
 * Imaging version of the Java2D class <code>RenderableImageOp</code>.
 * Instead of an explicit <code>ContextualRenderedImageFactory</code>,
 * the indirection of the <code>OperationRegistry</code> is used.
 *
 * <p> A <code>RenderableOp</code> stores an operation name and a
 * <code>ParameterBlock</code> containing sources and parameters.  A set of
 * nodes may be joined together via the source <code>Vector</code>s within
 * their respective <code>ParameterBlock</code>s to form a <u>d</u>irected
 * <u>a</u>cyclic <u>g</u>raph (DAG).  The topology, i.e., connectivity, of
 * the graph may be altered by changing the node's sources.  The operation
 * name and parameters may also be changed.
 *
 * <p> Such chains provide a framework for resolution- and rendering-
 * independent imaging.  They are useful in that a chain may be manipulated
 * dynamically and rendered multiple times.  Thus for example the same
 * chain of operations may be applied to different images or the parameters
 * of certain operations in a chain may be modified interactively.
 *
 * <p> A <code>RenderableOp</code> may be constructed directly as, for example,
 * <pre>
 * <code>
 * RenderableImage addend1;
 * RenderableImage addend2;
 * ParameterBlock pb =
 *     (new ParameterBlock()).addSource(addend1).addSource(addend2);
 * RenderableOp node = new RenderableOp("add", pb);
 * </code>
 * </pre>
 * or via the <code>createRenderable()</code> or
 * <code>createRenderableNS()</code> methods defined in the <code>JAI</code>
 * class.  The difference between direct construction of a node and creation
 * via a convenience method is that in the latter case:
 *
 * <ol>
 * <li> It is verified that the operation supports the renderable mode.</li>
 * <li> Using the <code>validateArguments()</code> method of the associated
 *      <code>OperationDescriptor</code>, the arguments (sources and parameters)
 *      are validated as being compatible with the specified operation.</li>
 * <li> Global <code>RenderingHints</code> maintained by the <code>JAI</code>
 *      instance are set on the <code>RenderableOp</code> using
 *      <code>setRenderingHints()</code>.</li>
 * </ol>
 *
 * <p> When a chain of nodes is rendered by any any of the
 * <code>createRendering()</code> methods, a "parallel" chain of
 * <code>RenderedImage</code>s is created.  Each node in the chain of
 * <code>RenderableOp</code>s corresponds to a node in the chain of
 * <code>RenderedImage</code>s.  A <code>RenderedImage</code> associated
 * with a given node is referred to as a <i>rendering</i> of the node.
 *
 * <p> The translation between <code>RenderableOp</code> chains and
 * <code>RenderedImage</code> (usually <code>OpImage</code>) chains makes
 * use of three levels of indirection provided by the
 * <code>OperationRegistry</code>, <code>ContextualRenderedImageFactory</code>,
 * (CRIF), and <code>RenderedImageFactory</code> (RIF) facilities.  First, the
 * <code>OperationRegistry</code> is used to map the operation name into a
 * CRIF.  This CRIF then constructs a <code>RenderedImage</code> via its
 * <code>create(RenderContext,&nbsp;ParameterBlock)</code> method.  The third
 * level of indirection results from the operation name being mapped within
 * <code>create()</code> into the optimum RIF which actually creates the
 * <code>RenderedImage</code>.  (Note that this third level of indirection is a
 * function of the CRIF implementation of the renderable <code>create()</code>
 * method: that provided by the convenience class <code>CRIFImpl</code>
 * provides this indirection.)  If the <code>RenderedImage</code> returned by
 * the CRIF <code>create()</code> invocation is a <code>RenderedOp</code>, it
 * is replaced with the rendering of the <code>RenderedOp</code> (usually an
 * <code>OpImage</code>).
 *
 * <p> <code>RenderingHints</code> may be set on a <code>RenderableOp</code>
 * to provide a set of common hints to be used in all invocations of the
 * various <code>createRendering()</code> methods on the node.  These hints
 * are merged with any hints supplied to <code>createRendering()</code>
 * either explicitly or via a <code>RenderContext</code>.  Directly
 * supplied hints take precedence over the common hints.
 *
 * <p> <code>RenderableOp</code> nodes may participate in Java Bean-style
 * events.  The <code>PropertyChangeEmitter</code> methods may be used
 * to register and unregister <code>PropertyChangeListener</code>s.
 * Certain <code>PropertyChangeEvent</code>s may be emitted by the
 * <code>RenderableOp</code>.  These include the
 * <code>PropertyChangeEventJAI</code>s and
 * <code>PropertySourceChangeEvent</code>s required by virtue of implementing
 * the <code>OperationNode</code> interface.
 *
 * <p> <code>RenderableOp</code> nodes are <code>WritablePropertySource</code>s
 * and so manage a name-value database of image meta-data also known as image
 * properties.  Properties may be set on and requested from a node.  The
 * value of a property not explicitly set on the node (via
 * <code>setProperty()</code>) is obtained from the property environment of
 * the node.  When a property is derived from the property environment it is
 * cached locally to ensure synchronization, i.e., that properties do not
 * change spontaneously if for example the same property is modified upstream.
 *
 * <p> The property environment of the <code>RenderableOp</code> is initially
 * derived from that of the corresponding <code>OperationDescriptor</code>
 * as maintained by the <code>OperationRegistry</code>.  It may be modified
 * locally by adding a <code>PropertyGenerator</code> or by suppressing a
 * specific property.  These modifications cannot be undone.
 * 
 * <p> When a property value is requested an attempt will be made to derive
 * it from the several entities in the following order of precedence:
 * <ol>
 * <li> local properties; </li>
 * <li> any registered <code>PropertyGenerator</code>s, or
 * <br> a source specified via a copy-from-source directive;</li>
 * <li> the first source which defines the property. </li>
 * </ol>
 * Local properties are those which have been cached locally either by virtue
 * of direct invocation of <code>setProperty()</code> or due to caching of a
 * property derived from the property environment.
 *
 * <p> The properties of a <code>RenderableOp</code> node are copied to each
 * rendering generated by any of the <code>createRendering()</code> methods.
 * Properties already set on the rendering are not copied, i.e., those of the
 * rendering take precedence.
 *
 * <p> A <code>RenderableOp</code> chain created on a client may be passed
 * to a server via a <code>RemoteImage</code>.  Any <code>RenderedImage</code>
 * sources which are not <code>Serializable</code> will be wrapped in
 * <code>SerializableRenderedImage</code>s for serialization.  The tile
 * transmission parameters will be determined from the common
 * <code>RenderingHints</code> of the node.  All other non-serializable
 * objects will attempt to be serialized using
 * <code>SerializerFactory</code>.  If no <code>Serializer</code> is
 * available for a particular object, a
 * <code>java.io.NotSerializableException</code> may result.  Image
 * properties (meta-data) are serialized insofar as they are serializable:
 * non-serializable components are simply eliminated from the local cache
 * of properties and from the property environment.
 *
 * @see CRIFImpl
 * @see CollectionOp
 * @see OperationRegistry
 * @see RenderedOp
 * @see java.awt.RenderingHints
 * @see java.awt.image.renderable.ContextualRenderedImageFactory
 * @see java.awt.image.renderable.RenderableImageOp
 * @see java.awt.image.renderable.RenderContext
 */
public class RenderableOp implements
    RenderableImage, OperationNode, WritablePropertySource, Serializable {

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

    /**
     * The <code>ContextualRenderedImageFactory</code> used to
     * generate renderings.
     */
    protected transient ContextualRenderedImageFactory crif = null;

    /**
     * Constructs a RenderableOp given the name of the operation to be 
     * performed and a ParameterBlock containing RenderableImage sources
     * and other parameters.  Any RenderedImage sources referenced by the
     * ParameterBlock will be ignored.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when a rendering of
     * the node is requested or the renderable dimensions are queried.
     *
     * @param registry  The <code>OperationRegistry</code> to be used for
     *        instantiation.  if <code>null</code>, the default registry
     *        is used.  Saved by reference.
     * @param opName  The operation name.  Saved by reference.
     * @param pb  The sources and other parameters. If <code>null</code>,
     *        it is assumed that this node has no sources and parameters.
     *        This parameter is cloned.
     * @param hints The common node <code>RenderingHints</code> to be set;
     *        it may be <code>null</code>.
     *        This parameter is cloned.
     *
     * @throws IllegalArgumentException if <code>opName</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1
     */
    public RenderableOp(OperationRegistry registry,
                        String opName,
                        ParameterBlock pb,
			RenderingHints hints) {

        if(pb == null) {
            // Ensure that the PB is non-null.
            pb = new ParameterBlock();
        } else {
            // Clone the PB.
            pb = (ParameterBlock)pb.clone();
        }

        // Clone the hints if non-null.
        if(hints != null) {
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
    }

    /**
     * Constructs a RenderableOp given the name of the operation to be 
     * performed and a ParameterBlock containing RenderableImage sources
     * and other parameters.  Any RenderedImage sources referenced by the
     * ParameterBlock will be ignored.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when a rendering of
     * the node is requested or the renderable dimensions are queried.
     *
     * @param registry  The <code>OperationRegistry</code> to be used for
     *        instantiation.  if <code>null</code>, the default registry
     *        is used.  Saved by reference.
     * @param opName  The operation name.  Saved by reference.
     * @param pb  The sources and other parameters. If <code>null</code>,
     *        it is assumed that this node has no sources and parameters.
     *        This parameter is cloned.
     *
     * @throws IllegalArgumentException if <code>opName</code> is
     *         <code>null</code>.
     */
    public RenderableOp(OperationRegistry registry,
                        String opName,
                        ParameterBlock pb) {
        this(registry, opName, pb, null);
    }
    
    /**
     * Constructs a RenderableOp given the name of the operation to be 
     * performed and a ParameterBlock containing RenderableImage sources
     * and other parameters.    The default operation registry
     * is used.  Any RenderedImage sources referenced by the
     * ParameterBlock will be ignored.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when a rendering of
     * the node is requested or the renderable dimensions are queried.
     *
     * @param opName  The operation name.  Saved by reference.
     * @param pb  The sources and other parameters. If <code>null</code>,
     *        it is assumed that this node has no sources and parameters.
     *        This parameter is cloned.
     *
     * @throws IllegalArgumentException if <code>opName</code> is <code>null</code>.
     */
    public RenderableOp(String opName, ParameterBlock pb) {
        this(null, opName, pb);
    }

    /**
     * Returns the name of the <code>RegistryMode</code> corresponding to
     * this <code>RenderableOp</code>.  This method always returns the
     * <code>String</code> "renderable".
     *
     * @since JAI 1.1
     */
    public String getRegistryModeName() {
        return RegistryMode.getMode("renderable").getName();
    }

    /* ----- Critical attribute main accessors and mutators. ----- */

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
     * default registry is used.
     *
     * <p> If the supplied registry does not equal the current registry, a
     * <code>PropertyChangeEventJAI</code> named "OperationRegistry"
     * will be fired
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
     * will be fired.
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
    public ParameterBlock getParameterBlock() {
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
     * or "Parameters" will be fired.
     *
     * <p> The <code>ParameterBlock</code> may include
     * <code>DeferredData</code> parameters.  These will not be evaluated
     * until their values are actually required, i.e., when a rendering of
     * the node is requested or the renderable dimensions are queried.
     *
     * @param pb  The new <code>ParameterBlock</code> to be set;
     *        it may be <code>null</code>.
     */
    public synchronized void setParameterBlock(ParameterBlock pb) {
        nodeSupport.setParameterBlock(pb == null ?
                                      new ParameterBlock() :
                                      (ParameterBlock)pb.clone());
    }

    /**
     * Returns a clone of the common <code>RenderingHints</code> of this node
     * or <code>null</code>.
     *
     * @since JAI 1.1
     */
    public RenderingHints getRenderingHints() {
        RenderingHints hints = nodeSupport.getRenderingHints();
        return hints == null ? null : (RenderingHints)hints.clone();
    }
    
    /**
     * Sets the common <code>RenderingHints</code> of this node.
     * The supplied parameter is cloned if non-<code>null</code>.
     *
     * <p> If the supplied <code>RenderingHints</code> does not equal the
     * current <code>RenderingHints</code>, a
     * <code>PropertyChangeEventJAI</code> named "RenderingHints"
     * will be fired.
     *
     * @param hints The new <code>RenderingHints</code> to be set;
     *        it may be <code>null</code>.
     *
     * @since JAI 1.1
     */
    public synchronized void setRenderingHints(RenderingHints hints) {
        if(hints != null) {
            hints = (RenderingHints)hints.clone();
        }
        nodeSupport.setRenderingHints(hints);
    }
    
    /* ----- RenderableImage methods (except properties). ----- */

    private Vector getRenderableSources() {
        Vector sources = null;

        int numSrcs = nodeSupport.getParameterBlock().getNumSources();
        if (numSrcs > 0) {
            sources = new Vector();
            for (int i = 0; i < numSrcs; i++) {
                Object o = nodeSupport.getParameterBlock().getSource(i);
                if (o instanceof RenderableImage) {
                    sources.add(o);
                }
            }
        }
        return sources;
    }

    /** 
     * Returns a vector of RenderableImages that are the sources of
     * image data for this RenderableImage. Note that this method may
     * return an empty vector, to indicate that the image has sources
     * but none of them is a RenderableImage, or null to indicate the
     * image has no source of any type.
     *
     * @return a (possibly empty) Vector of RenderableImages, or null.
     */
    public Vector getSources() {
        return getRenderableSources();
    }

    /** Use registry to find an appropriate CRIF */
    private synchronized ContextualRenderedImageFactory findCRIF() {
	if (crif == null) {
	    // find the CRIF(JAI) from the registry.
	    crif = CRIFRegistry.get(getRegistry(), getOperationName());
	}
	if (crif == null) {
	    throw new RuntimeException(JaiI18N.getString("RenderableOp2"));
	}

	return crif;
    }
    
    /**
     * Return the rendering-independent width of the image.
     *
     * @return the image width as a float.
     */
    public float getWidth() {
	findCRIF();
        ParameterBlock paramBlock =
            ImageUtil.evaluateParameters(nodeSupport.getParameterBlock());
        Rectangle2D boundingBox = crif.getBounds2D(paramBlock);
	return (float)boundingBox.getWidth();
    }

    /**
     * Return the rendering-independent height of the image.
     *
     * @return the image height as a float.
     */
    public float getHeight() {
	findCRIF();
        ParameterBlock paramBlock =
            ImageUtil.evaluateParameters(nodeSupport.getParameterBlock());
        Rectangle2D boundingBox = crif.getBounds2D(paramBlock);
	return (float)boundingBox.getHeight();
    }

    /** 
     * Gets the minimum X coordinate of the rendering-independent image data.
     */
    public float getMinX() {
	findCRIF();
        ParameterBlock paramBlock =
            ImageUtil.evaluateParameters(nodeSupport.getParameterBlock());
        Rectangle2D boundingBox = crif.getBounds2D(paramBlock);
	return (float)boundingBox.getX();
    }
    
    /** 
     * Gets the minimum Y coordinate of the rendering-independent image data.
     */
    public float getMinY() {
	findCRIF();
        ParameterBlock paramBlock =
            ImageUtil.evaluateParameters(nodeSupport.getParameterBlock());
        Rectangle2D boundingBox = crif.getBounds2D(paramBlock);
	return (float)boundingBox.getY();
    }
    
    /**
     * Returns a default rendering of this <code>RenderableImage</code>.
     * In all cases the area of interest will equal the image bounds.
     * Any hints set on the node via <code>setRenderingHints()</code> will
     * be used.
     *
     * <p> The dimensions of the created <code>RenderedImage</code> are
     * determined in the following order of precedence:
     * <ol>
     * <li>If a <code>JAI.KEY_DEFAULT_RENDERING_SIZE</code> hint is set on
     * the node it is used unless both its dimensions are non-positive.</li>
     * <li>The value returned by <code>JAI.getDefaultRenderingSize()</code>
     * is used unless it is <code>null</code>.
     * <li>An identity transform from renderable to rendered coordinates
     * is applied.
     * </ol>
     * Either dimension of the default rendering size set in the hints or on
     * the default <code>JAI</code> instance may be non-positive in which case
     * the other dimension and the renderable aspect ratio will be used to
     * compute the rendered image size.
     *
     * <p> This method does not validate sources and parameters supplied
     * in the <code>ParameterBlock</code> supplied at construction against
     * the specification of the operation this node represents.  It is the
     * caller's responsibility to ensure that the data in the
     * <code>ParameterBlock</code> are suitable for this operation.
     * Otherwise, some kind of exception or error will occur.  Invoking this
     * method will cause any <code>DeferredData</code> parameters to be
     * evaluated.
     *
     * @return The default RenderedImage.
     */
    public RenderedImage createDefaultRendering() {
        // Get the default dimensions.
        Dimension defaultDimension = null;
        RenderingHints hints = nodeSupport.getRenderingHints();
        if(hints != null &&
           hints.containsKey(JAI.KEY_DEFAULT_RENDERING_SIZE)) {
            defaultDimension =
                (Dimension)hints.get(JAI.KEY_DEFAULT_RENDERING_SIZE);
        }
        if(defaultDimension == null ||
           (defaultDimension.width <= 0 && defaultDimension.height <= 0)) {
            defaultDimension = JAI.getDefaultRenderingSize();
        }

        // Initialize scale factors to represent the identify transform.
        double sx = 1.0;
        double sy = 1.0;

        // Reset the scale factors if a default dimension is set.
        if(defaultDimension != null &&
           (defaultDimension.width > 0 || defaultDimension.height > 0)) {
            if(defaultDimension.width > 0 && defaultDimension.height > 0) {
                sx = defaultDimension.width/getWidth();
                sy = defaultDimension.height/getHeight();
            } else if(defaultDimension.width > 0) {
                sx = sy = defaultDimension.width/getWidth();
            } else { // defaultDimension.height > 0
                sx = sy = defaultDimension.height/getHeight();
            }
        }

        // Create the renderable-to-rendered scaling.
        AffineTransform transform = AffineTransform.getScaleInstance(sx, sy);

        // Return the rendering applying the computed transform.
        return createRendering(new RenderContext(transform));
    }
    
    /** 
     * Gets a RenderedImage instance of this image with width w, and
     * height h in pixels.  The RenderContext is built automatically
     * with an appropriate usr2dev transform and an area of interest
     * of the full image.  The rendering hints come from hints
     * passed in.  These hints will be merged with any set on the node
     * via <code>setRenderingHints()</code> with the hints passed in taking
     * precedence.
     *
     * <p> If w == 0, it will be taken to equal
     * Math.round(h*(getWidth()/getHeight())).
     * Similarly, if h == 0, it will be taken to equal
     * Math.round(w*(getHeight()/getWidth())).  One of
     * w or h must be non-zero or else an IllegalArgumentException 
     * will be thrown.
     *
     * <p> This method does not validate sources and parameters supplied
     * in the <code>ParameterBlock</code> supplied at construction against
     * the specification of the operation this node represents.  It is the
     * caller's responsibility to ensure that the data in the
     * <code>ParameterBlock</code> are suitable for this operation.
     * Otherwise, some kind of exception or error will occur.  Invoking this
     * method will cause any <code>DeferredData</code> parameters to be
     * evaluated.
     *
     * @param w the width of rendered image in pixels, or 0.
     * @param h the height of rendered image in pixels, or 0.
     * @param hints a RenderingHints object containg hints.
     * @return a RenderedImage containing the rendered data.
     *
     * @throws IllegalArgumentException if both w and h are zero.
     */
    public RenderedImage createScaledRendering(int w, int h,
                                               RenderingHints hints) {
        if ((w == 0) && (h == 0)) {
            throw new IllegalArgumentException(JaiI18N.getString("RenderableOp3"));
        }
        
        if (w == 0) {
            w = Math.round(h*(getWidth()/getHeight()));
        } else if (h == 0) {
            h = Math.round(w*(getHeight()/getWidth()));
        }
        double sx = (double)w/getWidth();
        double sy = (double)h/getHeight();
        
	AffineTransform usr2dev = AffineTransform.getScaleInstance(sx, sy);
        RenderContext renderContext = new RenderContext(usr2dev, hints);
        return createRendering(renderContext);
    }

    /** 
     * Gets a RenderedImage that represents a rendering of this image
     * using a given RenderContext.  This is the most general way to obtain a
     * rendering of a RenderableImage.
     *
     * <p> This method does not validate sources and parameters supplied
     * in the <code>ParameterBlock</code> supplied at construction against
     * the specification of the operation this node represents.  It is the
     * caller's responsibility to ensure that the data in the
     * <code>ParameterBlock</code> are suitable for this operation.
     * Otherwise, some kind of exception or error will occur.  Invoking this
     * method will cause any <code>DeferredData</code> parameters to be
     * evaluated.
     *
     * <p> The <code>RenderContext</code> may contain a <code>Shape</code>
     * that represents the area-of-interest (aoi).  If the aoi is specifed,
     * it is still legal to return an image that's larger than this aoi.
     * Therefore, by default, the aoi, if specified, is ignored at the
     * rendering.
     *
     * <p> Any hints in the <code>RenderContext</code> will be merged with any
     * set on the node via <code>setRenderingHints()</code> with the hints
     * in the <code>RenderContext</code> taking precedence.
     *
     * @param renderContext the RenderContext to use to produce the rendering.
     * @return a RenderedImage containing the rendered data.
     */
    public RenderedImage createRendering(RenderContext renderContext) {
	findCRIF();

        // Clone the original ParameterBlock; if the ParameterBlock
        // contains RenderableImage sources, they will be replaced by
        // RenderedImages.
        ParameterBlock nodePB = nodeSupport.getParameterBlock();
        Vector nodeParams =
            ImageUtil.evaluateParameters(nodePB.getParameters());
        ParameterBlock renderedPB =
            new ParameterBlock((Vector)nodePB.getSources().clone(),
                               nodeParams);
        Vector sources = getRenderableSources();

        try {
            // This assumes that if there is no renderable source, that there
            // is a rendered source in the ParameterBlock.

            // If there are any hints set on the node, create a new
            // RenderContext which merges them with those in the RenderContext
            // passed in with the passed in hints taking precedence.
            RenderContext rcIn = renderContext;
            RenderingHints nodeHints = nodeSupport.getRenderingHints();
            if(nodeHints != null) {
                RenderingHints hints = renderContext.getRenderingHints();
                RenderingHints mergedHints =
                    JAI.mergeRenderingHints(nodeHints, hints);
                if(mergedHints != hints) {
                    rcIn = new RenderContext(renderContext.getTransform(),
                                             renderContext.getAreaOfInterest(),
                                             mergedHints);
                }
            }

            if (sources != null) {
                Vector renderedSources = new Vector();
                for (int i = 0; i < sources.size(); i++) {
                    RenderContext rcOut =
                        crif.mapRenderContext(i, rcIn,
                                              renderedPB,
                                              this);
                    RenderableImage src =
                        (RenderableImage)sources.elementAt(i);
                    RenderedImage renderedImage = src.createRendering(rcOut);
                    if (renderedImage == null) {
                        return null;
                    }
                    
                    // Add this rendered image to the ParameterBlock's
                    // list of RenderedImages.
                    renderedSources.addElement(renderedImage);
                }
                
                if (renderedSources.size() > 0) {
                    renderedPB.setSources(renderedSources);
                }
            }

            RenderedImage rendering = crif.create(rcIn, renderedPB);

            // Replace with the actual rendering if a RenderedOp.
            if(rendering instanceof RenderedOp) {
                rendering = ((RenderedOp)rendering).getRendering();
            }

	    // Copy properties to the rendered node.
	    if(rendering != null &&
               rendering instanceof WritablePropertySource) {
	        String[] propertyNames = getPropertyNames();
                if(propertyNames != null) {
                    WritablePropertySource wps =
                        (WritablePropertySource)rendering;

                    // Save the names of rendered properties.
                    HashSet wpsNameSet = null;
                    String[] wpsNames = wps.getPropertyNames();
                    if(wpsNames != null) {
                        wpsNameSet = new HashSet();
                        for(int j = 0; j < wpsNames.length; j++) {
                            wpsNameSet.add(new CaselessStringKey(wpsNames[j]));
                        }
                    }

                    // Copy any properties not already defined by the rendering.
                    for(int j = 0; j < propertyNames.length; j++) {
                        String name = propertyNames[j];
                        if(wpsNameSet == null ||
                           !wpsNameSet.contains(new CaselessStringKey(name))) {
                            Object value = getProperty(name);
                            if(value != null &&
                               value != java.awt.Image.UndefinedProperty) {
                                wps.setProperty(name, value);
                            }
                        }
                    }
		}
	    }

	    return rendering;
        } catch (ArrayIndexOutOfBoundsException e) {
            // This should never happen
            return null;
        }
    }

    /**
     * Returns false, i.e., successive renderings with the same arguments
     * will produce identical results.
     */
    public boolean isDynamic() {
        return false;
    }

    /* ----- Property-related methods. ----- */

    /** Creates a <code>PropertySource</code> if none exists. */
    private synchronized void createPropertySource() {
        if (thePropertySource == null) {
            // Create a <code>PropertySource</code> encapsulating the
            // property environment of the node.
            thePropertySource = nodeSupport.getPropertySource(this, null);

            // Add the <code>PropertySource</code> to the helper object.
            properties.addProperties(thePropertySource);
        }
    }

    /**
     * Returns the names of properties available from this node.
     * These properties are a combination of those derived
     * from prior nodes in the imaging chain and those set locally.
     *
     * @return An array of <code>String</code>s containing valid
     *         property names or <code>null</code> if there are none.
     */
    public String[] getPropertyNames () {
        createPropertySource();
        return properties.getPropertyNames();
    }
    
    /**
     * Returns an array of <code>String</code>s recognized as names by
     * this property source that begin with the supplied prefix.  If
     * no property names match, <code>null</code> will be returned.
     * The comparison is done in a case-independent manner.
     *
     * @throws IllegalArgumentException if prefix is null.
     *
     * @return an array of <code>String</code>s giving the valid
     * property names.
     */
    public String[] getPropertyNames(String prefix) {
	// This gives us a list of all non-suppressed properties
        return PropertyUtil.getPropertyNames(getPropertyNames(), prefix);
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
     * Gets a property from the property set of this image.
     * If the property name is not recognized,
     * <code>java.awt.Image.UndefinedProperty</code> will be returned.
     *
     * @param name the name of the property to get, as a String.
     * @return a reference to the property Object, or the value
     *         java.awt.Image.UndefinedProperty.
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     */
    public Object getProperty(String name) {
        createPropertySource();
        return properties.getProperty(name);
    }
    
    /**
     * Sets a local property on a node.  Local property settings override
     * properties derived from prior nodes in the imaging chain.
     *
     * <p> If the node is serialized then serializable properties will
     * also be serialized but non-serializable properties will be lost.
     *
     * @param name a String representing the property name.
     * @param value the property's value, as an Object.
     * @exception IllegalArgumentException if <code>name</code>
     *                                     or <code>value</code>
     *                                     is <code>null</code>.
     */
    public void setProperty(String name, Object value) {
        createPropertySource();
        properties.setProperty(name, value);
    }

    /**
     * Removes the named property from the local property
     * set of the <code>RenderableOp</code> as well as from its property
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
     */
    public void suppressProperty(String name) {
        nodeSupport.suppressProperty(name);
    }

    /* ----- PropertyChangeEmitter methods. ----- */

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

    /* ----- Source Vector convenience methods. ----- */

    /**
     * Returns one of the node's sources as an Object.
     *
     * @param index the index of the source.
     */
    public Object getSource(int index) {
        Vector sources = nodeSupport.getParameterBlock().getSources();
        return sources.elementAt(index);
    }

    /**
     * Sets one of the node's sources to an Object.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * @param source the source, as an Object.
     * @param index the index of the source.
     * @throws IllegalArgumentException if <code>source</code> is
     *         <code>null</code>.
     */
    public void setSource(Object source, int index) {
        if (source == null)
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        ParameterBlock pb =
            (ParameterBlock)nodeSupport.getParameterBlock().clone();
        pb.setSource(source, index);
        nodeSupport.setParameterBlock(pb);
    }

    /**
     * Removes all the node's sources.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * @since JAI 1.1
     */
    public void removeSources() {
        ParameterBlock pb =
            (ParameterBlock)nodeSupport.getParameterBlock().clone();
        pb.removeSources();
        nodeSupport.setParameterBlock(pb);
    }

    /* ----- Parameter Vector convenience methods. ----- */

    /**
     * Returns one of the node's parameters, as a byte.
     *
     * @param index the index of the parameter.
     */
    public byte getByteParameter(int index) {
        return nodeSupport.getParameterBlock().getByteParameter(index);
    }

    /**
     * Returns one of the node's parameters, as a char.
     *
     * @param index the index of the parameter.
     */
    public char getCharParameter(int index) {
        return nodeSupport.getParameterBlock().getCharParameter(index);
    }

    /**
     * Returns one of the node's parameters, as a short.
     *
     * @param index the index of the parameter.
     */
    public short getShortParameter(int index) {
        return nodeSupport.getParameterBlock().getShortParameter(index);
    }

    /**
     * Returns one of the node's parameters, as an int.
     *
     * @param index the index of the parameter.
     */
    public int getIntParameter(int index) {
        return nodeSupport.getParameterBlock().getIntParameter(index);
    }

    /**
     * Returns one of the node's parameters, as a long.
     *
     * @param index the index of the parameter.
     */
    public long getLongParameter(int index) {
        return nodeSupport.getParameterBlock().getLongParameter(index);
    }

    /**
     * Returns one of the node's parameters, as a float.
     *
     * @param index the index of the parameter.
     */
    public float getFloatParameter(int index) {
        return nodeSupport.getParameterBlock().getFloatParameter(index);
    }

    /**
     * Returns one of the node's parameters, as a double.
     *
     * @param index the index of the parameter.
     */
    public double getDoubleParameter(int index) {
        return nodeSupport.getParameterBlock().getDoubleParameter(index);
    }

    /**
     * Returns one of the node's parameters, as an Object.
     *
     * @param index the index of the parameter.
     */
    public Object getObjectParameter(int index) {
        return nodeSupport.getParameterBlock().getObjectParameter(index);
    }

    /**
     * Sets one of the node's parameters to a byte.
     * This is a convenience method that invokes
     * <code>setParameter(Object,int)</code> and so adheres to the same event
     * firing behavior.
     *
     * @param param the parameter, as a byte.
     * @param index the index of the parameter.
     */
    public void setParameter(byte param, int index) {
        setParameter(new Byte(param), index);
    }

    /**
     * Sets one of the node's parameters to a char.
     * This is a convenience method that invokes
     * <code>setParameter(Object,int)</code> and so adheres to the same event
     * firing behavior.
     *
     * @param param the parameter, as a char.
     * @param index the index of the parameter.
     */
    public void setParameter(char param, int index) {
        setParameter(new Character(param), index);
    }

    /**
     * Sets one of the node's parameters to a short.
     * This is a convenience method that invokes
     * <code>setParameter(Object,int)</code> and so adheres to the same event
     * firing behavior.
     *
     * @param param the parameter, as a short.
     * @param index the index of the parameter.
     */
    public void setParameter(short param, int index) {
        setParameter(new Short(param), index);
    }

    /**
     * Sets one of the node's parameters to an int.
     * This is a convenience method that invokes
     * <code>setParameter(Object,int)</code> and so adheres to the same event
     * firing behavior.
     *
     * @param param the parameter, as an int.
     * @param index the index of the parameter.
     */
    public void setParameter(int param, int index) {
        setParameter(new Integer(param), index);
    }

    /**
     * Sets one of the node's parameters to a long.
     * This is a convenience method that invokes
     * <code>setParameter(Object,int)</code> and so adheres to the same event
     * firing behavior.
     *
     * @param param the parameter, as a long.
     * @param index the index of the parameter.
     */
    public void setParameter(long param, int index) {
        setParameter(new Long(param), index);
    }

    /**
     * Sets one of the node's parameters to a float.
     * This is a convenience method that invokes
     * <code>setParameter(Object,int)</code> and so adheres to the same event
     * firing behavior.
     *
     * @param param the parameter, as a float.
     * @param index the index of the parameter.
     */
    public void setParameter(float param, int index) {
        setParameter(new Float(param), index);
    }
 
    /**
     * Sets one of the node's parameters to a double.
     * This is a convenience method that invokes
     * <code>setParameter(Object,int)</code> and so adheres to the same event
     * firing behavior.
     *
     * @param param the parameter, as a double.
     * @param index the index of the parameter.
     */
    public void setParameter(double param, int index) {
        setParameter(new Double(param), index);
    }

    /**
     * Sets one of the node's parameters to an Object.
     * This is a convenience method that invokes
     * <code>setParameterBlock()</code> and so adheres to the same event
     * firing behavior.
     *
     * <p> The <code>Object</code> may be a
     * <code>DeferredData</code> instance.  It will not be evaluated
     * until its value is actually required, i.e., when a rendering of
     * the node is requested or the renderable dimensions are queried.
     *
     * @param param the parameter, as an Object.
     * @param index the index of the parameter.
     */
    public void setParameter(Object param, int index) {
        ParameterBlock pb =
            (ParameterBlock)nodeSupport.getParameterBlock().clone();
        pb.set(param, index);
        nodeSupport.setParameterBlock(pb);
    }
}
