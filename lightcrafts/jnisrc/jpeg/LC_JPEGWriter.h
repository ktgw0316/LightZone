/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef LC_JPEGWriter_H
#define LC_JPEGWriter_H

// standard
#include <cstdio>                       /* for FILE */
#include <jni.h>

extern "C" {
#   include <jpeglib.h>
}

// local
#include "LC_dest_mgr.h"

/**
 * An LC_JPEGWriter contains data that needs to be maintained between native
 * function invocations for writing JPEG images.  Its destructor also ensures
 * proper clean-up.
 */
struct LC_JPEGWriter {
    //
    // We write image data either to a FILE *or* to an LC_dest_mgr.
    //
    FILE*                   m_file;
    LC_dest_mgr*            m_dest;

    jpeg_compress_struct    cinfo;
    jpeg_error_mgr          m_errMgr;

    LC_JPEGWriter();
    ~LC_JPEGWriter();

    void start_compress( int, int, int, int, int );

private:
    /**
     * This flag is used to know if the destructor should call
     * jpeg_finish_compress() based on whether jpeg_start_compress() has been
     * successfully called.
     */
    bool m_startedCompress;
};

#endif  /* LC_JPEGWriter_H */
/* vim:set et sw=4 ts=4: */
