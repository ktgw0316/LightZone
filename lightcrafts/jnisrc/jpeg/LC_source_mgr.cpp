/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <cstdlib>
#include <jni.h>
#include <setjmp.h>
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
#include "LC_source_mgr.h"
#include "util.h"

using namespace std;
using namespace LightCrafts;

/**
 * Initialize the JPEG source.
 */
extern "C" void LC_init_source( j_decompress_ptr cinfo ) {
    LC_source_mgr *const src = static_cast<LC_source_mgr*>( cinfo->src );
    src->m_startOfFile = true;
}

/**
 * Fill the input buffer.
 */
extern "C" boolean LC_fill_input_buffer( j_decompress_ptr cinfo ) {
#ifdef DEBUG
    cerr << "LC_fill_input_buffer()" << endl;
#endif
    LC_source_mgr *const src = static_cast<LC_source_mgr*>( cinfo->src );
    JNIEnv *const env = LC_getJNIEnv( NULL );

    jint bytesRead = env->CallIntMethod(
        src->m_jImageDataProvider, src->m_getImageData_methodID,
        src->m_jByteBuffer
    );
#ifdef DEBUG
    cerr << "\tbytesRead=" << bytesRead << endl;
#endif
    LC_checkForJavaException( env );
    if ( bytesRead <= 0 ) {
        if ( src->m_startOfFile ) {
            //
            // Treat an empty input file as a fatal error.  (This is what the
            // default stdio implementation does.)
            //
            ERREXIT( cinfo, JERR_INPUT_EMPTY );
            LC_throwIllegalStateException( env, "shouldn't have gotten here" );
            return FALSE;
        }
        WARNMS( cinfo, JWRN_JPEG_EOF );
        src->m_buffer[0] = 0xFF;
        src->m_buffer[1] = JPEG_EOI;
        bytesRead = 2;
    }

    src->next_input_byte = src->m_buffer;
    src->bytes_in_buffer = bytesRead;
    src->m_startOfFile = false;
    return TRUE;
}

/**
 * Skip data.
 */
extern "C" void LC_skip_input_data( j_decompress_ptr cinfo, long numBytes ) {
#ifdef DEBUG
    cerr << "LC_skip_input_data(): " << numBytes << endl;
#endif
    if ( numBytes > 0 ) {
        LC_source_mgr *const src = static_cast<LC_source_mgr*>( cinfo->src );
        while ( numBytes > src->bytes_in_buffer ) {
            numBytes -= src->bytes_in_buffer;
            if ( !LC_fill_input_buffer( cinfo ) )
                break;
        }
        src->next_input_byte += numBytes;
        src->bytes_in_buffer -= numBytes;
    }
}

/**
 * Terminate a JPEG source: called by jpeg_finish_decompress() after all data
 * has been read.
 */
extern "C" void LC_term_source( j_decompress_ptr ) {
    // nothing to do
}

/**
 * Construct an LC_source_mgr.
 */
LC_source_mgr::LC_source_mgr( JNIEnv *env, jobject jImageDataProvider,
                              int bufSize ) {
    init_source             = &LC_init_source;
    fill_input_buffer       = &LC_fill_input_buffer;
    skip_input_data         = &LC_skip_input_data;
    resync_to_restart       = &jpeg_resync_to_restart;
    term_source             = &LC_term_source;
    bytes_in_buffer         = 0;
    next_input_byte         = 0;

    m_jImageDataProvider    = (jobject)env->NewGlobalRef( jImageDataProvider );
    m_jImageDataProviderClass = 0;
    m_getImageData_methodID = 0;
    m_jByteBuffer           = 0;
    m_buffer                = 0;
    m_bufSize               = bufSize;
    m_startOfFile           = false;

    //
    // Get the methodID to the getImageData() method that we will use to get
    // image data from the Java side.
    //
    m_jImageDataProviderClass = env->GetObjectClass( m_jImageDataProvider );
    if ( !m_jImageDataProviderClass ) {
        LC_throwIllegalStateException(
            env, "LCImageDataProvider class not found"
        );
        throw LC_JPEGException();
    }
    m_jImageDataProviderClass =
        (jclass)env->NewGlobalRef( m_jImageDataProviderClass );
    m_getImageData_methodID = env->GetMethodID(
        m_jImageDataProviderClass, "getImageData", "(Ljava/nio/ByteBuffer;)I"
    );
    if ( !m_getImageData_methodID ) {
        // NoSuchMethodError already thrown by Java
        throw LC_JPEGException();
    }

    if ( !(m_buffer = new JOCTET[ bufSize ]) ) {
        LC_throwOutOfMemoryError( env, "new JOCTET[] failed" );
        throw LC_JPEGException();
    }

    if ( !(m_jByteBuffer = env->NewDirectByteBuffer( m_buffer, bufSize )) ) {
        delete[] m_buffer;
        LC_throwOutOfMemoryError( env, "NewDirectByteBuffer() failed" );
        throw LC_JPEGException();
    }
    m_jByteBuffer = (jobject)env->NewGlobalRef( m_jByteBuffer );
}

/**
 * Destruct an LC_source_mgr.
 */
LC_source_mgr::~LC_source_mgr() {
#ifdef DEBUG
    cerr << "~LC_source_mgr()" << endl;
#endif
    JNIEnv *const env = LC_getJNIEnv( NULL );
    env->DeleteGlobalRef( m_jImageDataProvider );
    if ( m_jImageDataProviderClass )
        env->DeleteGlobalRef( m_jImageDataProviderClass );
    if ( m_jByteBuffer )
        env->DeleteGlobalRef( m_jByteBuffer );
    delete[] m_buffer;
}

/* vim:set et sw=4 ts=4: */
