/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <cstdio>
#include <sys/types.h>
#ifdef  DEBUG
#include <iostream>
#endif

extern "C" {
#   include <jpeglib.h>
}

// local
#include "LC_JNIUtils.h"
#include "LC_JPEGException.h"
#include "LC_JPEGReader.h"
#include "util.h"

#define MAX(a,b)    ( (a) > (b) ? (a) : (b) )

using namespace std;

/**
 * Construct an LC_JPEGReader.
 */
LC_JPEGReader::LC_JPEGReader() {
    m_file = 0;
    m_src = 0;
    m_startedDecompress = false;

    jpeg_create_decompress( &cinfo );
    //
    // Change things so we're handling errors in our own way.
    //
    cinfo.err = jpeg_std_error( &m_errMgr );
    m_errMgr.error_exit = &LC_error_exit;
}

/**
 * Destruct an LC_JPEGReader.
 */
LC_JPEGReader::~LC_JPEGReader() {
#ifdef DEBUG
    cerr << "~LC_JPEGReader()" << endl;
#endif
    if ( m_startedDecompress ) {
        try {
            jpeg_finish_decompress( &cinfo );
        }
        catch ( LC_JPEGException const& ) {
            //
            // We will have thrown a Java exception by this point, but we want to
            // finish our clean-up, so keep going.
            //
        }
    }
    jpeg_destroy_decompress( &cinfo );
    if ( m_file )
        ::fclose( m_file );
    delete m_src;
}

/**
 * Set the width, height, and colorsPerPixel fields in the LCJPEGReader Java
 * object.
 */
void LC_JPEGReader::setFields( JNIEnv *env, jobject jLCJPEGReader ) {
#ifdef DEBUG
    cerr << "LC_JPEGReader::setFields(): width=" << cinfo.output_width
         <<                          ", height=" << cinfo.output_height
         <<                  ", colorsPerPixel=" << cinfo.output_components
         <<                      ", colorSpace=" << cinfo.out_color_space
         << endl;
#endif
    LC_setIntField( env, jLCJPEGReader, "m_width" , cinfo.output_width );
    LC_setIntField( env, jLCJPEGReader, "m_height", cinfo.output_height );
    LC_setIntField(
        env, jLCJPEGReader, "m_colorsPerPixel", cinfo.output_components
    );
    LC_setIntField( env, jLCJPEGReader, "m_colorSpace", cinfo.out_color_space );
}

/**
 * Read the JPEG header and start decompression.
 */
void LC_JPEGReader::start_decompress( int maxWidth, int maxHeight ) {
    jpeg_read_header( &cinfo, TRUE );

    if ( maxWidth > 0 && maxHeight > 0 ) {
        jpeg_calc_output_dimensions( &cinfo );

        int scale =
            MAX( cinfo.output_width/maxWidth, cinfo.output_height/maxHeight );

        if ( scale >= 8 )
            scale = 8;
        else if ( scale >= 4 )
            scale = 4;
        else if ( scale >= 2 )
            scale = 2;
        else
            scale = 1;

        if ( scale != 1 ) {
            cinfo.scale_num = 1;
            cinfo.scale_denom = scale;
            jpeg_calc_output_dimensions( &cinfo );
        }
    }
    jpeg_start_decompress( &cinfo );
    m_startedDecompress = true;
}

/* vim:set et sw=4 ts=4: */
