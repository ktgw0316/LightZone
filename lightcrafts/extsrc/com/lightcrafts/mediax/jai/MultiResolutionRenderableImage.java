/*
 * $RCSfile: MultiResolutionRenderableImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:12 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderContext;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Vector;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;

/**
 * A RenderableImage that produces renderings based on a set of
 * supplied RenderedImages at various resolutions.
 */
public class MultiResolutionRenderableImage
implements WritablePropertySource, RenderableImage, Serializable {

    /** An array of RenderedImage sources. */
    protected transient RenderedImage[] renderedSource;
    private int numSources;

    /** The aspect ratio, derived from the highest-resolution source. */
    protected float aspect;

    /** The min X coordinate in Renderable coordinates. */
    protected float minX;

    /** The min Y coordinate in Renderable coordinates. */
    protected float minY;

    /** The width in Renderable coordinates. */
    protected float width;

    /** The height in Renderable coordinates. */
    protected float height;

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

    private MultiResolutionRenderableImage() {
        eventManager = new PropertyChangeSupportJAI(this);
        properties = new WritablePropertySourceImpl(null, null, eventManager);
    }

    /**
     * Constructs a MultiResolutionRenderableImage with
     * given dimensions from a Vector of progressively
     * lower resolution versions of a RenderedImage.
     *
     * @param renderedSources a Vector of RenderedImages.
     * @param minX the minimum X coordinate of the Renderable,
     *        as a float.
     * @param minY the minimum Y coordinate of the Renderable,
     *        as a float.
     * @param height the height of the Renderable, as a float.
     * @throws IllegalArgumentException if the supplied height is
     * non-positive.
     *
     */
    public MultiResolutionRenderableImage(Vector renderedSources,
                                          float minX,
                                          float minY,
                                          float height) {
        this();

        // Check the height
        if(height <= 0.0F) {
            throw new IllegalArgumentException(JaiI18N.getString("MultiResolutionRenderableImage0"));
        }

        numSources = renderedSources.size();
        this.renderedSource = new RenderedImage[numSources];
        for (int i = 0; i < numSources; i++) {
            this.renderedSource[i] =
                (RenderedImage)renderedSources.elementAt(i);
        }

        int maxResWidth = renderedSource[0].getWidth();
        int maxResHeight = renderedSource[0].getHeight();
        aspect = (float)maxResWidth/maxResHeight;

        this.minX = minX;
        this.width = height*aspect;

        this.minY = minY;
        this.height = height;
    }

    /**
     * Returns an empty Vector, indicating that this RenderableImage
     * has no Renderable sources.
     *
     * @return an empty Vector.
     */
    public Vector getSources() {
        return null;
    }

    /**
     * Returns a list of the properties recognized by this image.
     * If no properties are recognized by this image, null will be returned.
     * The default implementation returns <code>null</code>, i.e.,
     * no property names are recognized.
     *
     * @return an array of Strings representing valid property names.
     *
     * @since JAI 1.1
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
     *                              <code>null</code>.
     */
    public String[] getPropertyNames(String prefix) {
        if ( prefix == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
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
     * Gets a property from the property set of this image.
     * If the property name is not recognized, java.awt.Image.UndefinedProperty
     * will be returned. The default implementation returns
     * <code>java.awt.Image.UndefinedProperty</code>.
     *
     * @param name the name of the property to get, as a String.
     * @return a reference to the property Object, or the value
     *         java.awt.Image.UndefinedProperty.
     *
     * @exception IllegalArgumentException if <code>name</code>
     *                                     is <code>null</code>.
     */
    public Object getProperty(String name) {
        return properties.getProperty(name);
    }

    /**
     * Sets a property on a <code>MultiResolutionRenderableImage</code>.
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
     * Removes the named property from the
     * <code>MultiResolutionRenderableImage</code>.
     *
     * @return The value of the property removed or
     *	       <code>java.awt.Image.UndefinedProperty</code> if it was
     *	       not present in the property set.
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
     * Returns the floating-point width of the RenderableImage.
     */
    public float getWidth() {
        return width;
    }

    /**
     * Returns the floating-point height of the RenderableImage.
     */
    public float getHeight() {
        return height;
    }

    /**
     * Returns the floating-point min X coordinate of the
     * RenderableImage.
     */
    public float getMinX() {
        return minX;
    }

    /**
     * Returns the floating-point max X coordinate of the
     * RenderableImage.
     */
    public float getMaxX() {
        return minX + width;
    }

    /**
     * Returns the floating-point min Y coordinate of the
     * RenderableImage.
     */
    public float getMinY() {
        return minY;
    }

    /**
     * Returns the floating-point max Y coordinate of the
     * RenderableImage.
     */
    public float getMaxY() {
        return minY + height;
    }

    /**
     * Returns false since successive renderings (that is, calls to
     * createRendering() or createScaledRendering()) with the same
     * arguments will never produce different results.
     */
    public boolean isDynamic() {
        return false;
    }

    /**
     * Returns a rendering with a given width, height, and rendering
     * hints.
     *
     * <p> If a JAI rendering hint named
     * <code>JAI.KEY_INTERPOLATION</code> is provided, its
     * corresponding <code>Interpolation</code> object is used as an
     * argument to the JAI operator used to scale the image.  If no
     * such hint is present, an instance of
     * <code>InterpolationNearest</code> is used.
     *
     * @param width the width of the rendering in pixels.
     * @param height the height of the rendering in pixels.
     * @param hints a Hashtable of rendering hints.
     * @throws IllegalArgumentException if width or height are non-positive.
     */
    public RenderedImage createScaledRendering(int width,
                                               int height,
                                               RenderingHints hints) {
        if(width <= 0 && height <= 0) {
            throw new IllegalArgumentException(
			   JaiI18N.getString("MultiResolutionRenderableImage1"));
        }

        int res = numSources - 1;
        while (res > 0) {
            if(height > 0) {
                int imh = renderedSource[res].getHeight();
                if (imh >= height) {
                    break;
                }
            } else {
                int imw = renderedSource[res].getWidth();
                if (imw >= width) {
                    break;
                }
            }
            res--;
        }

        RenderedImage source = renderedSource[res];
        if(width <= 0) {
            width = (int)Math.round(height*source.getWidth()/source.getHeight());
        } else if(height <= 0) {
            height = (int)Math.round(width*source.getHeight()/source.getWidth());
        }
        double sx = (double)width/source.getWidth();
        double sy = (double)height/source.getHeight();
        double tx = (getMinX() - source.getMinX())*sx;
        double ty = (getMinY() - source.getMinY())*sy;

        Interpolation interp =
            Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        if (hints != null) {
            Object obj = hints.get(JAI.KEY_INTERPOLATION);
            if (obj != null) {
                interp = (Interpolation)obj;
            }
        }

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add((float)sx);
        pb.add((float)sy);
        pb.add((float)tx);
        pb.add((float)ty);
        pb.add(interp);

        return JAI.create("scale", pb, null);
    }

    /**
     * Returns the full resolution source RenderedImage
     * with no rendering hints.
     */
    public RenderedImage createDefaultRendering() {
        return renderedSource[0];
    }

    /**
     * Returns a rendering based on a RenderContext.
     *
     * <p> If a JAI rendering hint named
     * <code>JAI.KEY_INTERPOLATION</code> is provided, its
     * corresponding <code>Interpolation</code> object is used as an
     * argument to the JAI operator used to transform the image.  If
     * no such hint is present, an instance of
     * <code>InterpolationNearest</code> is used.
     *
     * <p> The <code>RenderContext</code> may contain a <code>Shape</code>
     * that represents the area-of-interest (aoi).  If the aoi is specifed,
     * it is still legal to return an image that's larger than this aoi.
     * Therefore, by default, the aoi, if specified, is ignored at the
     * rendering.
     *
     * @param renderContext a RenderContext describing the transform
     *        rendering hints.
     * @throws IllegalArgumentException if renderContext is null.
     */
    public RenderedImage createRendering(RenderContext renderContext) {
        if ( renderContext == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Get a clone of the context's transform
        AffineTransform usr2dev = renderContext.getTransform();
        RenderingHints hints = renderContext.getRenderingHints();

        int type = usr2dev.getType();
        if (type == AffineTransform.TYPE_UNIFORM_SCALE ||
            type == AffineTransform.TYPE_GENERAL_SCALE) {
            int width = (int)Math.ceil(usr2dev.getScaleX()*getWidth());
            int height = (int)Math.ceil(usr2dev.getScaleY()*getHeight());

            return createScaledRendering(width, height, hints);
        }

        // Use the square root of the determinant as an estimate of
        // the single-axis scale factor.
        int height =
            (int)Math.ceil(Math.sqrt(usr2dev.getDeterminant())*getHeight());
        int res = numSources - 1;
        while (res > 0) {
            int imh = renderedSource[res].getHeight();
            if (imh >= height) {
                break;
            }
            res--;
        }

        RenderedImage source = renderedSource[res];
        double sx = (double)getWidth()/source.getWidth();
        double sy = (double)getHeight()/source.getHeight();

        AffineTransform transform = new AffineTransform();
        transform.translate(-source.getMinX(), -source.getMinY());
        transform.scale(sx, sy);
        transform.translate(getMinX(), getMinY());
        transform.preConcatenate(usr2dev);

        Interpolation interp =
            Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        if (hints != null) {
            Object obj = hints.get(JAI.KEY_INTERPOLATION);
            if (obj != null) {
                interp = (Interpolation)obj;
            }
        }

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(source);
        pb.add(transform);
        pb.add(interp);

        return JAI.create("affine", pb, null);
    }

    /**
     * Serialize the MultiResolutionRenderableImage.
     *
     * @param out The stream provided by the VM to which to write the object.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Create an array for the serializable form of the sources.
        Object[] sources = new Object[numSources];

        // Copy each source converting it to a serializable form if necessary.
        for(int i = 0; i < numSources; i++) {
            if(renderedSource[i] instanceof Serializable) {
                // Image is already serializable.
                sources[i] = renderedSource[i];
            } else {
                // Derive a serializable form.
                sources[i] = SerializerFactory.getState(renderedSource[i]);
            }
        }

        // Write non-transient fields.
        out.defaultWriteObject();

        // Write array of serializable sources.
        out.writeObject(sources);
    }

    /**
     * Deserialize the MultiResolutionRenderableImage.
     *
     * @param in The stream provided by the VM from which to read the object.
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // Read non-transient fields.
        in.defaultReadObject();

        // Read array of sources.
        Object[] source = (Object[])in.readObject();
        numSources = source.length;
        renderedSource = new RenderedImage[numSources];
        for (int i = 0; i < numSources; i++) {
            if (source[i] instanceof SerializableState) {
                SerializableState ss = (SerializableState)source[i];
                renderedSource[i] = (RenderedImage)ss.getObject();
            } else renderedSource[i] = (RenderedImage)source[i];
        }
    }
}

