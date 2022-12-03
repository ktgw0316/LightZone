/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import java.io.IOException;

/**
 * A <code>ImageFileExportOptions</code> is-a {@link ImageExportOptions} that
 * contains a collection of options that can be set for exporting an image to a
 * particular image file format such as JPEG or TIFF.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class ImageFileExportOptions extends ImageExportOptions {

    ////////// public /////////////////////////////////////////////////////////

    public final BlackPointCompensationOption   blackPointCompensation;
    public final ColorProfileOption             colorProfile;
    public final RenderingIntentOption          renderingIntent;
    public final ResizeWidthOption              resizeWidth;
    public final ResizeHeightOption             resizeHeight;
    public final ResolutionOption               resolution;
    public final ResolutionUnitOption           resolutionUnit;

    /**
     * {@inheritDoc}
     */
    public void readFrom( ImageExportOptionReader r ) throws IOException {
        super.readFrom( r );
        try {
            blackPointCompensation.readFrom( r );
        }
        catch ( IOException e ) {
            // This setting must be optional, for backwards compatibility.
        }
        colorProfile.readFrom( r );
        renderingIntent.readFrom( r );
        resizeWidth.readFrom( r );
        resizeHeight.readFrom( r );
        try {
            resolution.readFrom( r );
            resolutionUnit.readFrom( r );
        }
        catch ( IOException e ) {
            // This setting must be optional, for backwards compatibility.
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeTo( ImageExportOptionWriter w ) throws IOException {
        super.writeTo( w );
        blackPointCompensation.writeTo( w );
        colorProfile.writeTo( w );
        renderingIntent.writeTo( w );
        resizeWidth.writeTo( w );
        resizeHeight.writeTo( w );
        resolution.writeTo( w );
        resolutionUnit.writeTo( w );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct an <code>ImageFileExportOptions</code>.
     *
     * @param imageType The {@link ImageType} these options are for.
     */
    protected ImageFileExportOptions( ImageType imageType ) {
        super( imageType );
        blackPointCompensation  = new BlackPointCompensationOption( this );
        colorProfile            = new ColorProfileOption( this );
        renderingIntent         = new RenderingIntentOption( this );
        resizeWidth             = new ResizeWidthOption( 0, this );
        resizeHeight            = new ResizeHeightOption( 0, this );
        resolution              = new ResolutionOption( this );
        resolutionUnit          = new ResolutionUnitOption( this );
    }

    @Deprecated
    protected void save( XmlNode node ) {
        super.save( node );
        blackPointCompensation.save( node );
        colorProfile.save( node );
        renderingIntent.save( node );
        resizeWidth.save( node );
        resizeHeight.save( node );
        resolution.save( node );
        resolutionUnit.save( node );
    }

    @Deprecated
    protected void restore( XmlNode node ) throws XMLException {
        super.restore( node );
        if ( node.hasChild( blackPointCompensation.getName() ) ) {
            // This setting must be optional, for backwards compatibility.
            blackPointCompensation.restore( node );
        }
        colorProfile.restore( node );
        if ( node.hasChild( renderingIntent.getName() ) )
            renderingIntent.restore( node );
        resizeWidth.restore( node );
        resizeHeight.restore( node );
        if ( node.hasChild( resolution.getName() ) )
            resolution.restore( node );
        if ( node.hasChild( resolutionUnit.getName() ) )
            resolutionUnit.restore( node );
    }

}
/* vim:set et sw=4 ts=4: */
