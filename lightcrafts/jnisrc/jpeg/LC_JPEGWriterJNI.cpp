/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <cstdlib>
#include <jni.h>
#include <memory>                       /* for unique_ptr */
#ifdef  DEBUG
#include <iostream>
#endif

extern "C" {
#   include <jpeglib.h>
}

// local
#include "LC_JNIUtils.h"
#include "LC_JPEGException.h"
#include "LC_JPEGWriter.h"
#include "LC_source_mgr.h"
#include "util.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_image_libs_LCJPEGWriter.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// Local functions ////////////////////////////////////////////////////

/**
 * Simply cast the result of LC_getNativePtr() to a LCJPEGWriter*.
 */
inline LC_JPEGWriter* getNativePtr( JNIEnv *env, jobject jLCJPEGWriter ) {
    return static_cast<LC_JPEGWriter*>( LC_getNativePtr( env, jLCJPEGWriter ) );
}

////////// JNI ////////////////////////////////////////////////////////////////

#define LCJPEGWriter_METHOD(method) \
        name4(Java_,com_lightcrafts_image_libs_LCJPEGWriter,_,method)

#define LCJPEGWriter_CONSTANT(constant) \
        name3(com_lightcrafts_image_libs_LCJPEGWriter,_,constant)

/**
 * Begin using the given LCImageDataReceiver to get image data.
 */
JNIEXPORT void JNICALL LCJPEGWriter_METHOD(beginWrite)
    ( JNIEnv *env, jobject jLCJPEGWriter, jobject jImageDataReceiver,
      jint bufSize, jint width, jint height, jint colorsPerPixel,
      jint colorSpace, jint quality )
{
#ifdef DEBUG
    cerr << "beginWrite(): bufSize=" << bufSize
         <<               ", width=" << width
         <<              ", height=" << height
         <<      ", colorsPerPixel=" << colorsPerPixel
         <<          ", colorSpace=" << colorSpace
         <<             ", quality=" << quality
         << endl;
#endif
    //
    // Here we use an unique_ptr to ensure that the LC_JPEGWriter object will get
    // deallocated if anything bad happens.
    //
    unique_ptr<LC_JPEGWriter> writer( new LC_JPEGWriter );
    if ( !writer.get() ) {
        LC_throwOutOfMemoryError( env, "new LC_JPEGWriter failed" );
        return;
    }

    try {
        writer->m_dest = new LC_dest_mgr( env, jImageDataReceiver, bufSize );
        writer->cinfo.dest = writer->m_dest;
        writer->start_compress(
            width, height, colorsPerPixel, colorSpace, quality
        );
    }
    catch ( LC_JPEGException const& ) {
        //
        // We will have thrown a Java exception by this point, so just return.
        //
        return;
    }

    //
    // Since everything has worked so far, release the unique_ptr and store it
    // on the Java side.
    //
    LC_setNativePtr( env, jLCJPEGWriter, writer.release() );
}

/**
 * Dispose of all memory and resources.
 */
JNIEXPORT void JNICALL LCJPEGWriter_METHOD(dispose)
    ( JNIEnv *env, jobject jLCJPEGWriter )
{
    if ( LC_JPEGWriter *const writer = getNativePtr( env, jLCJPEGWriter ) ) {
        LC_setNativePtr( env, jLCJPEGWriter, 0 );
        delete writer;
    }
}

/**
 * Open a JPEG image file.
 */
JNIEXPORT void JNICALL LCJPEGWriter_METHOD(openForWriting)
    ( JNIEnv *env, jobject jLCJPEGWriter, jbyteArray jFileNameUtf8, jint width,
      jint height, jint colorsPerPixel, jint colorSpace, jint quality )
{
#ifdef DEBUG
    cerr << "openForWriting():  width=" << width
         <<                 ", height=" << height
         <<         ", colorsPerPixel=" << colorsPerPixel
         <<             ", colorSpace=" << colorSpace
         <<                ", quality=" << quality
         << endl;
#endif
    //
    // Here we use an unique_ptr to ensure that the LC_JPEGWriter object will get
    // deallocated if anything bad happens.
    //
    unique_ptr<LC_JPEGWriter> writer( new LC_JPEGWriter );
    if ( !writer.get() ) {
        LC_throwOutOfMemoryError( env, "new LC_JPEGWriter failed" );
        return;
    }

    //
    // Open the JPEG file.
    //
    jbyteArray_to_c const cFileName( env, jFileNameUtf8 );
    if ( !(writer->m_file = LC_fopen( cFileName, "wb" )) ) {
        LC_throwIOException( env, cFileName );
        return;
    }

    try {
        jpeg_stdio_dest( &writer->cinfo, writer->m_file );
        writer->start_compress(
            width, height, colorsPerPixel, colorSpace, quality
        );
    }
    catch ( LC_JPEGException const& ) {
        //
        // We will have thrown a Java exception by this point, so just return.
        //
        return;
    }

    //
    // Since everything has worked so far, release the unique_ptr and store it
    // on the Java side.
    //
    LC_setNativePtr( env, jLCJPEGWriter, writer.release() );
}

/**
 * Write an APP segment to the JPEG file.
 */
JNIEXPORT void JNICALL LCJPEGWriter_METHOD(writeSegment)
    ( JNIEnv *env, jobject jLCJPEGWriter, jint marker, jbyteArray jBuf )
{
#ifdef DEBUG
    cerr << "writeSegment()" << endl;
#endif
    LC_JPEGWriter *const writer = getNativePtr( env, jLCJPEGWriter );

    jarray_to_c<JOCTET> cBuf( env, jBuf );
    if ( !cBuf ) {
        LC_throwOutOfMemoryError( env, "jarray_to_c failed" );
        return;
    }
    try {
        jpeg_write_marker( &writer->cinfo, marker, cBuf, cBuf.length() );
    }
    catch ( LC_JPEGException const& ) {
        //
        // We will have thrown a Java exception by this point, so just return.
        //
    }
}

/**
 * Write a number of scanlines to the JPEG image.
 */
JNIEXPORT jint JNICALL LCJPEGWriter_METHOD(writeScanLines)
    ( JNIEnv *env, jobject jLCJPEGWriter, jbyteArray jBuf, jint offset,
      jint numLines, jint lineStride )
{
#ifdef DEBUG
    cerr << "writeScanLines()" << endl;
#endif
    jarray_to_c<JSAMPLE> cBuf( env, jBuf );
    if ( !cBuf ) {
        LC_throwOutOfMemoryError( env, "jarray_to_c failed" );
        return -1;
    }

    LC_JPEGWriter *const writer = getNativePtr( env, jLCJPEGWriter );
    jpeg_compress_struct &cinfo = writer->cinfo;
    //
    // The jpeg_write_scanlines() function wants its buffer as a pointer to a
    // JSAMPROW (a pointer to an array of pointers to JSAMPLE), so we have to
    // create such an array and initialize each pointer to point to each "row"
    // inside the buffer passed from Java.
    //
    unique_ptr<JSAMPROW[]> row( new JSAMPROW[ numLines ] );

    for ( int i = 0; i < numLines; ++i )
        row[i] = cBuf + offset + i * lineStride;

    try {
        int totalLinesWritten = 0;
        while ( totalLinesWritten < numLines ) {
            int const linesWritten = jpeg_write_scanlines(
                &cinfo, &row[ totalLinesWritten ], numLines - totalLinesWritten
            );
            if ( linesWritten < 1 )
                break;
            totalLinesWritten += linesWritten;
        }
        return totalLinesWritten;
    }
    catch ( LC_JPEGException const& ) {
        //
        // We will have thrown a Java exception by this point, so just return.
        //
        return -1;
    }
}

/* vim:set et sw=4 ts=4: */
