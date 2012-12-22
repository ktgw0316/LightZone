/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef LC_JPEGReader_H
#define LC_JPEGReader_H

// standard
#include <cstdio>                       /* for FILE */
#include <jni.h>

extern "C" {
#   include <jpeglib.h>
}

// local
#include "LC_source_mgr.h"

/**
 * An LC_JPEGReader contains data that needs to be maintained between native
 * function invocations for reading JPEG images.  Its destructor also ensures
 * proper clean-up.
 */
struct LC_JPEGReader {
    //
    // We read image data either from a FILE *or* from an LC_source_mgr.
    //
    FILE*                   m_file;
    LC_source_mgr*          m_src;

    jpeg_decompress_struct  cinfo;
    jpeg_error_mgr          m_errMgr;

    LC_JPEGReader();
    ~LC_JPEGReader();

    void setFields( JNIEnv*, jobject jLCJPEGReader );
    void start_decompress( int maxWidth, int maxHeight );

private:
    /**
     * This flag is used to know if the destructor should call
     * jpeg_finish_decompress() based on whether jpeg_start_decompress() has
     * been successfully called.
     */
    bool m_startedDecompress;
};

#endif  /* LC_JPEGReader_H */
/* vim:set et sw=4 ts=4: */
