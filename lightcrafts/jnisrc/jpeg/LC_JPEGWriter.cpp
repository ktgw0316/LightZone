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
#include "LC_JPEGException.h"
#include "LC_JPEGWriter.h"
#include "util.h"

using namespace std;

/**
 * Construct a LC_JPEGWriter.
 */
LC_JPEGWriter::LC_JPEGWriter() {
    m_file = 0;
    m_dest = 0;
    m_startedCompress = false;

    //
    // Change things so we're handling errors in our own way.
    //
    cinfo.err = jpeg_std_error( &m_errMgr );
    m_errMgr.error_exit = &LC_error_exit;

    jpeg_create_compress( &cinfo );
}

/**
 * Destruct a LC_JPEGWriter.
 */
LC_JPEGWriter::~LC_JPEGWriter() {
#ifdef DEBUG
    cerr << "~LC_JPEGWriter()" << endl;
#endif
    try {
        if ( m_startedCompress )
            jpeg_finish_compress( &cinfo );
        jpeg_destroy_compress( &cinfo );
    }
    catch ( LC_JPEGException const& ) {
        //
        // We will have thrown a Java exception by this point, but we want to
        // finish our clean-up, so keep going.
        //
    }
    if ( m_file )
        ::fclose( m_file );
    delete m_dest;
}

/**
 * Set compression parameters and start compression.
 */
void LC_JPEGWriter::start_compress( int width, int height, int colorsPerPixel,
                                    int colorSpace, int quality ) {
    cinfo.image_width      = width;
    cinfo.image_height     = height;
    cinfo.input_components = colorsPerPixel;
    cinfo.in_color_space   = static_cast<J_COLOR_SPACE>( colorSpace );

    jpeg_set_defaults( &cinfo );

    //
    // Enable high quality (non subsampled) JPEG output, compatible with all apps.
    //

    cinfo.comp_info[0].h_samp_factor = 1;
    cinfo.comp_info[0].v_samp_factor = 1;
    cinfo.comp_info[1].h_samp_factor = 1;
    cinfo.comp_info[1].v_samp_factor = 1;
    cinfo.comp_info[2].h_samp_factor = 1;
    cinfo.comp_info[2].v_samp_factor = 1;

    jpeg_set_quality( &cinfo, quality, TRUE );
    jpeg_start_compress( &cinfo, TRUE );
    m_startedCompress = true;
}

/* vim:set et sw=4 ts=4: */
