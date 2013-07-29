/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <cstdlib>
#include <jni.h>
#include <memory>                       /* for auto_ptr */
#ifdef  DEBUG
#include <iostream>
#endif

extern "C" {
#   include <jpeglib.h>
#   include <jerror.h>
}

// local
#include "LC_JNIUtils.h"
#include "LC_JPEGException.h"
#include "LC_JPEGReader.h"
#include "LC_source_mgr.h"
#include "util.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_image_libs_LCJPEGReader.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// Local functions ////////////////////////////////////////////////////

/**
 * Simply cast the result of LC_getNativePtr() to a LCJPEGReader*.
 */
inline LC_JPEGReader* getNativePtr( JNIEnv *env, jobject jLCJPEGReader ) {
    return static_cast<LC_JPEGReader*>( LC_getNativePtr( env, jLCJPEGReader ) );
}

////////// JNI ////////////////////////////////////////////////////////////////

#define LCJPEGReader_METHOD(method) \
        name4(Java_,com_lightcrafts_image_libs_LCJPEGReader,_,method)

#define LCJPEGReader_CONSTANT(constant) \
        name3(com_lightcrafts_image_libs_LCJPEGReader,_,constant)

/**
 * Begin using the given LCImageDataProvider to get image data.
 */
JNIEXPORT void JNICALL LCJPEGReader_METHOD(beginRead)
    ( JNIEnv *env, jobject jLCJPEGReader, jobject jImageDataProvider,
      jint bufSize, jint maxWidth, jint maxHeight )
{
#ifdef DEBUG
    cerr << "beginRead()" << endl;
#endif
    //
    // Here we use an auto_ptr to ensure that the LC_JPEGReader object will get
    // deallocated if anything bad happens.
    //
    auto_ptr<LC_JPEGReader> reader( new LC_JPEGReader );
    if ( !reader.get() ) {
        LC_throwOutOfMemoryError( env, "new LC_JPEGReader failed" );
        return;
    }

    try {
        reader->m_src = new LC_source_mgr( env, jImageDataProvider, bufSize );
        reader->cinfo.src = reader->m_src;
        reader->start_decompress( maxWidth, maxHeight );
        reader->setFields( env, jLCJPEGReader );
    }
    catch ( LC_JPEGException const& ) {
        //
        // We will have thrown a Java exception by this point, so just return.
        //
        return;
    }

    //
    // Since everything has worked so far, release the auto_ptr and store it
    // on the Java side.
    //
    LC_setNativePtr( env, jLCJPEGReader, reader.release() );
}

/**
 * Dispose of all memory and resources.
 */
JNIEXPORT void JNICALL LCJPEGReader_METHOD(dispose)
    ( JNIEnv *env, jobject jLCJPEGReader )
{
#ifdef DEBUG
    cerr << "dispose()" << endl;
#endif
    if ( LC_JPEGReader *const reader = getNativePtr( env, jLCJPEGReader ) ) {
        LC_setNativePtr( env, jLCJPEGReader, 0 );
        delete reader;
    }
}

/**
 * Open a JPEG image file.
 */
JNIEXPORT void JNICALL LCJPEGReader_METHOD(openForReading)
    ( JNIEnv *env, jobject jLCJPEGReader, jbyteArray jFileNameUtf8, jint maxWidth,
      jint maxHeight )
{
#ifdef DEBUG
    cerr << "openForReading()" << endl;
#endif
    //
    // Here we use an auto_ptr to ensure that the LC_JPEGReader object will get
    // deallocated if anything bad happens.
    //
    auto_ptr<LC_JPEGReader> reader( new LC_JPEGReader );
    if ( !reader.get() ) {
        LC_throwOutOfMemoryError( env, "new LC_JPEGReader failed" );
        return;
    }

    //
    // Open the JPEG file.
    //
    jbyteArray_to_c const cFileName( env, jFileNameUtf8 );
    if ( !(reader->m_file = LC_fopen( cFileName, "rb" )) ) {
        LC_throwFileNotFoundException( env, cFileName );
        return;
    }

    try {
        jpeg_stdio_src( &reader->cinfo, reader->m_file );
        reader->start_decompress( maxWidth, maxHeight );
        reader->setFields( env, jLCJPEGReader );
    }
    catch ( LC_JPEGException const& ) {
        //
        // We will have thrown a Java exception by this point, so just return.
        //
        return;
    }

    //
    // Since everything has worked so far, release the auto_ptr and store it
    // on the Java side.
    //
    LC_setNativePtr( env, jLCJPEGReader, reader.release() );
}

/**
 * Read a number of scanlines from a JPEG image.
 */
JNIEXPORT jint JNICALL LCJPEGReader_METHOD(readScanLines)
    ( JNIEnv *env, jobject jLCJPEGReader, jbyteArray jBuf, jlong offset,
      jint numLines )
{
#ifdef DEBUG
    cerr << "readScanLines()" << endl;
#endif
    jarray_to_c<JSAMPLE> cBuf( env, jBuf );
    if ( !cBuf ) {
        LC_throwOutOfMemoryError( env, "jarray_to_c failed" );
        return -1;
    }

    LC_JPEGReader *const reader = getNativePtr( env, jLCJPEGReader );
    jpeg_decompress_struct &cinfo = reader->cinfo;
    //
    // The jpeg_read_scanlines() function wants its buffer as a pointer to a
    // JSAMPROW (a pointer to an array of pointers to JSAMPLE), so we have to
    // create such an array and initialize each pointer to point to each "row"
    // inside the buffer passed from Java.
    //
    int const rowSize = cinfo.output_width * cinfo.output_components;

#ifdef __GNUC__
    JSAMPROW row[ numLines ];
#else
    auto_vec<JSAMPROW> row( new JSAMPROW[ numLines ] );
#endif
    for ( int i = 0; i < numLines; ++i )
        row[i] = (JSAMPLE*)cBuf + offset + i * rowSize;

    try {
        int totalLinesRead = 0;
        while ( totalLinesRead < numLines ) {
            int const linesRead = jpeg_read_scanlines(
                &cinfo, &row[ totalLinesRead ], numLines - totalLinesRead
            );
            if ( linesRead < 1 ) {
                cinfo.err->msg_code = JERR_BAD_LENGTH;
                break;
            }
            totalLinesRead += linesRead;
        }
        return totalLinesRead;
    }
    catch ( LC_JPEGException const& ) {
        //
        // We will have thrown a Java exception by this point, so just return.
        //
        return -1;
    }
}

/* vim:set et sw=4 ts=4: */
