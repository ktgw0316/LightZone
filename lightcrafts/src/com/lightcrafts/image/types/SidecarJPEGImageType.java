/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.w3c.dom.Document;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.utils.xml.XMLUtil;

import static com.lightcrafts.image.types.JPEGConstants.JPEG_APP4_MARKER;

/**
 * A <code>SidecarJPEGImageType</code> is-a {@link JPEGImageType} for sidecar
 * JPEG images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class SidecarJPEGImageType extends JPEGImageType
    implements LZNDocumentProvider {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>SidecarJPEGImageType</code>. */
    @SuppressWarnings({"FieldNameHidesFieldInSuperclass"})
    public static final SidecarJPEGImageType INSTANCE =
        new SidecarJPEGImageType();

    /**
     * <code>ExportOptions</code> are {@link JPEGImageType.ExportOptions} for
     * sidecar JPEG images.
     */
    public static final class ExportOptions
        extends JPEGImageType.ExportOptions {

        ////////// package ////////////////////////////////////////////////////

        /**
         * Construct an <code>ExportOptions</code>.
         */
        ExportOptions() {
            super( INSTANCE );
        }
    }

    /**
     * Gets the LightZone document (if any) from the given JPEG image.
     *
     * @param imageInfo The image to get the LightZone document from.
     * @return Returns said {@link Document} or <code>null</code> if none.
     */
    public Document getLZNDocument( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ByteBuffer buf = getFirstSegment( imageInfo, JPEG_APP4_MARKER );
        if ( buf == null )
            return null;
        byte[] b;
        if ( buf.hasArray() ) {
        	b = buf.array();
        }
        else {
        	b = new byte[buf.remaining()];
        	buf.get(b);
        }
        final InputStream is = new ByteArrayInputStream( b );
        return XMLUtil.readDocumentFrom( is );
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return super.getName() + "-LZN";
    }

    /**
     * {@inheritDoc}
     */
    public ExportOptions newExportOptions() {
        return new ExportOptions();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>SidecarJPEGImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private SidecarJPEGImageType() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
