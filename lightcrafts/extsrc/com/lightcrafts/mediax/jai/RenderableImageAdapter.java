/*
 * $RCSfile: RenderableImageAdapter.java,v $
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
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderContext;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import com.lightcrafts.media.jai.util.PropertyUtil;

/**
 * An adapter class for externally-generated RenderableImages.  All
 * methods are simply forwarded to the image being adapted.  The
 * purpose of this class is simply to ensure that the PropertySource
 * interface is available for all JAI RenderableImages.
 *
 * <p> The set of properties available on the image will be a combination of
 * those defined locally via <code>setProperty()</code> and those defined
 * on the source image with the local properties taking precedence.  No
 * <code>PropertySourceChangeEvent</code> will be generated as a result of
 * changes to the property set of the source image.
 */
public final class RenderableImageAdapter
    implements RenderableImage, WritablePropertySource {

    /** A reference to the external RenderableImage. */
    private RenderableImage im;

    /** A helper object to manage firing events. */
    private PropertyChangeSupportJAI eventManager = null;

    /** A helper object to manage the image properties. */
    private WritablePropertySourceImpl properties = null;

    /**
     * Adapts a RenderableImage into a RenderableImageAdapter.
     * If the image is already an instance of RenderableImageAdapter,
     * it is returned unchanged.
     *
     * @param im a RenderableImage.
     *
     * @return a RenderableImageAdapter.
     *
     * @throws IllegalArgumentException if <code>im</code> is <code>null</code>.
     */
    public static RenderableImageAdapter
        wrapRenderableImage(RenderableImage im) {
        if (im == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if (im instanceof RenderableImageAdapter) {
            return (RenderableImageAdapter)im;
        } else {
            return new RenderableImageAdapter(im);
        }
    }

    /** 
     * Constructs a RenderableImageAdapter from a RenderableImage. 
     *
     * @throws IllegalArgumentException if <code>im</code> is <code>null</code>.
     */
    public RenderableImageAdapter(RenderableImage im) {
        if ( im == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        this.im = im;
        eventManager = new PropertyChangeSupportJAI(this);
        properties = new WritablePropertySourceImpl(null, null, eventManager);
    }

    /**
     * Returns the reference to the external <code>RenderableImage</code>
     * originally supplied to the constructor.
     *
     * @since JAI 1.1.2
     */
    public final RenderableImage getWrappedImage() {
        return im;
    }

    /*
     * Returns a vector of RenderableImages that are the sources of
     * image data for this RenderableImage. Note that this method may
     * return an empty vector, to indicate that the image has no sources,
     * or null, to indicate that no information is available.
     *
     * @return a (possibly empty) Vector of RenderableImages, or null.
     */
    public final Vector getSources() {
        return im.getSources();
    }
    
    /**
     * Gets a property from the property set of this image.
     * If the property name is not recognized, java.awt.Image.UndefinedProperty
     * will be returned.
     *
     * @param name the name of the property to get, as a String.
     * @throws IllegalArgumentException if <code>name</code> is
     * <code>null</code>.
     * @return a reference to the property Object, or the value
     *         java.awt.Image.UndefinedProperty.
     */
    public final Object getProperty(String name) {
        // Retrieve the property from the local cache.
        Object property =  properties.getProperty(name);

        // If it is still undefined, forward the call.
        if(property == java.awt.Image.UndefinedProperty) {
            property = im.getProperty(name);
        }

        return property;
    }

    /**
     * Returns the class expected to be returned by a request for
     * the property with the specified name.  If this information
     * is unavailable, <code>null</code> will be returned.
     *
     * @return The <code>Class</code> expected to be return by a
     *         request for the value of this property or <code>null</code>.
     * @throws IllegalArgumentException if <code>name</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1
     */
    public Class getPropertyClass(String name) {
        // Get the class if the property is local.
        Class propClass = properties.getPropertyClass(name);

        // If not local ...
        if(propClass == null) {
            // Get the property value.
            Object propValue = getProperty(name);

            if(propValue != java.awt.Image.UndefinedProperty) {
                // If the property is defined, get the class.
                propClass = propValue.getClass();
            }
        }

        return propClass;
    }

    /** 
     * Returns a list of the properties recognized by this image.  If
     * no properties are available, <code>null</code> will be
     * returned.
     *
     * @return an array of <code>String</code>s representing valid
     *         property names.
     */
    public final String[] getPropertyNames() {
        return RenderedImageAdapter.mergePropertyNames(
                   properties.getPropertyNames(),
                   im.getPropertyNames());
    }
    
    /**
     * Returns an array of <code>String</code>s recognized as names by
     * this property source that begin with the supplied prefix.  If
     * no property names match, <code>null</code> will be returned.
     * The comparison is done in a case-independent manner.
     *
     * @throws IllegalArgumentException if <code>prefix</code> is
     * <code>null</code>.
     * @return an array of <code>String</code>s giving the valid
     * property names.
     */
    public String[] getPropertyNames(String prefix) {
        return PropertyUtil.getPropertyNames(getPropertyNames(), prefix);
    }

    /**
     * Sets a property on a <code>RenderableImageAdapter</code>.
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
     * Removes the named property from the <code>RenderableImageAdapter</code>.
     *
     * @throws IllegalArgumentException if <code>name</code> is
     * <code>null</code>.
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
     * Add a PropertyChangeListener for a specific property.  The case of
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
     * Gets the width in user coordinate space.  By convention, the
     * usual width of a RenderableImage is equal to the image's aspect
     * ratio (width divided by height).
     *
     * @return the width of the image in user coordinates.
     */
    public final float getWidth() {
        return im.getWidth();
    }
  
    /** 
     * Gets the height in user coordinate space.  By convention, the
     * usual height of a RenderedImage is equal to 1.0F.
     *
     * @return the height of the image in user coordinates.
     */
    public final float getHeight() {
        return im.getHeight();
    }
    
    /** 
     * Gets the minimum X coordinate of the rendering-independent image.
     */
    public final float getMinX() {
        return im.getMinX();
    }
  
    /**
     * Gets the minimum Y coordinate of the rendering-independent image.
     */
    public final float getMinY() {
        return im.getMinY();
    }

    /**
     * Returns true if successive renderings (that is, calls to
     * createRendering() or createScaledRendering()) with the same arguments
     * may produce different results.  This method may be used to
     * determine whether an existing rendering may be cached and
     * reused.
     */
    public final boolean isDynamic() {
        return im.isDynamic();
    }

    /** 
     * Gets a RenderedImage instance of this image with width w, and
     * height h in pixels.  The RenderContext is built automatically
     * with an appropriate usr2dev transform and an area of interest
     * of the full image.  All the rendering hints come from hints
     * passed in.
     *
     * @param w the width of rendered image in pixels.
     * @param h the height of rendered image in pixels.
     * @param hints a RenderingHints object containing rendering hints.
     * @return a RenderedImage containing the rendered data.
     */
    public final RenderedImage createScaledRendering(int w, int h,
                                                     RenderingHints hints) {
        return im.createScaledRendering(w, h, hints);
    }
  
    /** 
     * Gets a RenderedImage instance of this image with a default
     * width and height in pixels.  The RenderContext is built
     * automatically with an appropriate usr2dev transform and an area
     * of interest of the full image.  All the rendering hints come
     * from hints passed in.  Implementors of this interface must be
     * sure that there is a defined default width and height.
     *
     * @return a RenderedImage containing the rendered data.
     */
    public final RenderedImage createDefaultRendering() {
        return im.createDefaultRendering();
    }
  
    /** 
     * Gets a RenderedImage instance of this image from a
     * RenderContext.  This is the most general way to obtain a
     * rendering of a RenderableImage.
     *
     * @param renderContext the RenderContext to use to produce the rendering.
     * @return a RenderedImage containing the rendered data.
     */
    public final RenderedImage createRendering(RenderContext renderContext) {
        return im.createRendering(renderContext);
    }
}
