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
#include "LC_dest_mgr.h"
#include "util.h"

using namespace std;
using namespace LightCrafts;

/**
 * Empty the output buffer.
 */
extern "C" boolean LC_empty_output_buffer( j_compress_ptr cinfo ) {
#ifdef DEBUG
    cerr << "LC_empty_output_buffer()" << endl;
#endif
    LC_dest_mgr *const dest = static_cast<LC_dest_mgr*>( cinfo->dest );
    JNIEnv *const env = LC_getJNIEnv( NULL );
    dest->write( env, cinfo, dest->m_bufSize );
    dest->reset();
    return TRUE;
}

/**
 * Initialize the JPEG destination.
 */
extern "C" void LC_init_destination( j_compress_ptr cinfo ) {
#ifdef DEBUG
    cerr << "LC_init_destination()" << endl;
#endif
    LC_dest_mgr *const dest = static_cast<LC_dest_mgr*>( cinfo->dest );
    dest->reset();
}

/**
 * Terminate a JPEG destination: called by jpeg_finish_decompress() after all
 * data has been written.
 */
extern "C" void LC_term_destination( j_compress_ptr cinfo ) {
#ifdef DEBUG
    cerr << "LC_term_destination()" << endl;
#endif
    LC_dest_mgr *const dest = static_cast<LC_dest_mgr*>( cinfo->dest );
    jint const limit = dest->m_bufSize - dest->free_in_buffer;
    if ( limit > 0 ) {
#ifdef DEBUG
        cerr << "\tlimit=" << limit << endl;
#endif
        JNIEnv *const env = LC_getJNIEnv( NULL );

        static jclass jBufferClass;
        static jmethodID limit_methodID;
        if ( !jBufferClass ) {
            //
            // Get the methodID for Buffer.limit() just once.
            //
            jBufferClass = LC_findClassOrDie( env, "java/nio/Buffer" );
            jBufferClass = (jclass)env->NewGlobalRef( jBufferClass );
            limit_methodID = env->GetMethodID(
                jBufferClass, "limit", "(I)Ljava/nio/Buffer;"
            );
            if ( !limit_methodID ) {
                // NoSuchMethodError already thrown by Java
                throw LC_JPEGException();
            }
        }

        //
        // We have to call Buffer.limit() to limit the number of bytes to be
        // written to the remaining bytes.
        //
        env->CallObjectMethod( dest->m_jByteBuffer, limit_methodID, limit );
        LC_checkForJavaException( env );
        dest->write( env, cinfo, limit );
    }
}

/**
 * Construct an LC_dest_mgr.
 */
LC_dest_mgr::LC_dest_mgr( JNIEnv *env, jobject jImageDataReceiver,
                          jint bufSize ) {
    init_destination        = &LC_init_destination;
    empty_output_buffer     = &LC_empty_output_buffer;
    term_destination        = &LC_term_destination;

    m_jImageDataReceiver    = (jobject)env->NewGlobalRef( jImageDataReceiver );
    m_jImageDataReceiverClass = 0;
    m_putImageData_methodID = 0;
    m_jByteBuffer           = 0;
    m_buffer                = 0;
    m_bufSize               = bufSize;

    //
    // Get the methodID to the putImageData() method that we will use to get
    // image data from the Java side.
    //
    m_jImageDataReceiverClass = env->GetObjectClass( m_jImageDataReceiver );
    if ( !m_jImageDataReceiverClass ) {
        LC_throwIllegalStateException(
            env, "LCImageDataReceiver class not found"
        );
        throw LC_JPEGException();
    }
    m_jImageDataReceiverClass =
        (jclass)env->NewGlobalRef( m_jImageDataReceiverClass );
    m_putImageData_methodID = env->GetMethodID(
        m_jImageDataReceiverClass, "putImageData", "(Ljava/nio/ByteBuffer;)I"
    );
    if ( !m_putImageData_methodID ) {
        // NoSuchMethodError already thrown by Java
        return;
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
 * Destruct an LC_dest_mgr.
 */
LC_dest_mgr::~LC_dest_mgr() {
#ifdef DEBUG
    cerr << "~LC_dest_mgr()" << endl;
#endif
    JNIEnv *const env = LC_getJNIEnv( NULL );
    env->DeleteGlobalRef( m_jImageDataReceiver );
    if ( m_jImageDataReceiverClass )
        env->DeleteGlobalRef( m_jImageDataReceiverClass );
    if ( m_jByteBuffer )
        env->DeleteGlobalRef( m_jByteBuffer );
    delete[] m_buffer;
}

/**
 * Call the LCImageDataReceiver to receive the image data.
 */
jint LC_dest_mgr::write( JNIEnv *env, j_compress_ptr cinfo, jint byteCount ) {
#ifdef DEBUG
    cerr << "LC_dest_mgr::write(): byteCount=" << byteCount << endl;
#endif
    jint const bytesWritten = env->CallIntMethod(
        m_jImageDataReceiver, m_putImageData_methodID, m_jByteBuffer
    );
    LC_checkForJavaException( env );
    if ( bytesWritten != byteCount ) {
        ERREXIT( cinfo, JERR_FILE_WRITE );
        LC_throwIllegalStateException( env, "shouldn't have gotten here" );
        return -1;
    }
    return bytesWritten;
}

/* vim:set et sw=4 ts=4: */
