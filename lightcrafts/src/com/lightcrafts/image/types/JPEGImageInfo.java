/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

/**
 * A <code>JPEGImageInfo</code> is-an {@link AuxiliaryImageInfo} for holding
 * additional information for a JPEG image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class JPEGImageInfo extends AuxiliaryImageInfo {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>JPEGImageInfo</code>.
     *
     * @param jpegInfo The JPEG image.
     * @param segIDs Keep the data only for these segment IDs.
     */
    public JPEGImageInfo( ImageInfo jpegInfo, byte... segIDs )
        throws BadImageFileException, IOException
    {
        new SegmentReader().readSegments( jpegInfo, segIDs );
    }

    /**
     * Gets all JPEG data segments having the given ID.
     *
     * @param segID The ID of the segments to get.
     * @return Returns a {@link List} of {@link ByteBuffer}s where each
     * {@link ByteBuffer} is the raw bytes of the segment or returns
     * <code>null</code> if there are no such segments.
     * @see #getAllSegmentsFor(Byte,JPEGSegmentFilter)
     * @see #getFirstSegmentFor(Byte)
     * @see #getFirstSegmentFor(Byte,JPEGSegmentFilter)
     */
    public List<ByteBuffer> getAllSegmentsFor( Byte segID ) {
        return getAllSegmentsFor( segID, null );
    }

    /**
     * Gets all JPEG data segments having the given ID that satisfy the given
     * {@link JPEGSegmentFilter}.
     *
     * @param segID The ID of the segments to get.
     * @param filter The {@link JPEGSegmentFilter} to use.
     * @return Returns a {@link List} of {@link ByteBuffer}s where each
     * {@link ByteBuffer} is the raw bytes of the segment or returns
     * <code>null</code> if there are no such segments.
     * @see #getAllSegmentsFor(Byte)
     * @see #getFirstSegmentFor(Byte)
     * @see #getFirstSegmentFor(Byte,JPEGSegmentFilter)
     */
    public List<ByteBuffer> getAllSegmentsFor( Byte segID,
                                               JPEGSegmentFilter filter ) {
        final List<ByteBuffer> segList = m_segMap.get( segID );
        if ( segList == null )
            return null;
        if ( filter == null )
            return new ArrayList<ByteBuffer>( segList );
        List<ByteBuffer> filteredSegList = null;
        for ( ByteBuffer segBuf : segList )
            if ( filter.accept( segID, segBuf ) ) {
                if ( filteredSegList == null )
                    filteredSegList = new ArrayList<ByteBuffer>();
                filteredSegList.add( segBuf );
            }
        return filteredSegList;
    }

    /**
     * Gets the first JPEG data segment having the given ID.
     *
     * @param segID The ID of the segment to get.
     * @return Returns the bytes of the segment wrapped into a
     * {@link ByteBuffer} or <code>null</code> if there is no such segment.
     * @see #getAllSegmentsFor(Byte)
     * @see #getAllSegmentsFor(Byte,JPEGSegmentFilter)
     * @see #getFirstSegmentFor(Byte,JPEGSegmentFilter)
     */
    public ByteBuffer getFirstSegmentFor( Byte segID ) {
        return getFirstSegmentFor( segID, null );
    }

    /**
     * Gets the first JPEG data segment having the given ID that satisfies the
     * given {@link JPEGSegmentFilter}.
     *
     * @param segID The ID of the segments to get.
     * @param filter The {@link JPEGSegmentFilter} to use.
     * @return Returns the bytes of the segment wrapped into a
     * {@link ByteBuffer} or <code>null</code> if there is no such segment.
     * @see #getAllSegmentsFor(Byte)
     * @see #getAllSegmentsFor(Byte,JPEGSegmentFilter)
     * @see #getFirstSegmentFor(Byte)
     */
    public ByteBuffer getFirstSegmentFor( Byte segID,
                                          JPEGSegmentFilter filter ) {
        final List<ByteBuffer> segList = m_segMap.get( segID );
        if ( segList == null )
            return null;
        if ( filter == null )
            return segList.get( 0 );
        for ( ByteBuffer segBuf : segList )
            if ( filter.accept( segID, segBuf ) )
                return segBuf;
        return null;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A <code>SegmentReader</code> is-a {@link JPEGParserEventHandler} that
     * reads the data for the specified JPEG segments.
     */
    private final class SegmentReader implements JPEGParserEventHandler {

        ////////// public /////////////////////////////////////////////////////

        /**
         * {@inheritDoc}
         */
        public boolean gotSegment( byte segID, int segLength, File jpegFile,
                                   LCByteBuffer buf ) throws IOException {
            if ( m_segsToKeep.contains( segID ) ) {
                ArrayList<ByteBuffer> list = m_segMap.get( segID );
                if ( list == null ) {
                    list = new ArrayList<ByteBuffer>();
                    m_segMap.put( segID, list );
                }
                list.add( ByteBuffer.wrap( buf.getBytes( segLength ) ) );
            }
            return true;
        }

        ////////// package ////////////////////////////////////////////////////

        /**
         * Read the specified segments from a JPEG file.
         *
         * @param jpegInfo The JPEG image to read the segments from.
         * @param segIDs Keep the data only for these segment IDs.
         */
        void readSegments( ImageInfo jpegInfo, byte... segIDs )
            throws BadImageFileException, IOException
        {
            m_segsToKeep = new HashSet<Byte>();
            if ( segIDs != null )
                for ( byte segmentMarker : segIDs )
                    m_segsToKeep.add( segmentMarker );
            JPEGParser.parse(
                this, jpegInfo.getFile(), jpegInfo.getByteBuffer()
            );
        }

        ////////// private ////////////////////////////////////////////////////

        /**
         * The set of segment markers to keep the data for.
         */
        private Set<Byte> m_segsToKeep;
    }

    /**
     * A map from segment IDs (bytes) to {@link ByteBuffer}s containing the
     * segment data.
     */
    private final Map<Byte,ArrayList<ByteBuffer>> m_segMap =
        new HashMap<Byte,ArrayList<ByteBuffer>>();
}
/* vim:set et sw=4 ts=4: */
