/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef LC_dest_mgr_H
#define LC_dest_mgr_H

// standard
#include <jni.h>

extern "C" {
#   include <jpeglib.h>
}

/**
 * An LC_dest_mgr is-a jpeg_destination_mgr that adds stuff for writing image
 * data to the Java side.
 */
struct LC_dest_mgr : jpeg_destination_mgr {
    jclass      m_jImageDataReceiverClass;
    jobject     m_jImageDataReceiver;
    jmethodID   m_putImageData_methodID;
    jobject     m_jByteBuffer;
    JOCTET*     m_buffer;
    jint        m_bufSize;

    LC_dest_mgr( JNIEnv*, jobject jImageDataReceiver, jint bufSize );
    ~LC_dest_mgr();

    void reset() {
        next_output_byte = m_buffer;
        free_in_buffer = m_bufSize;
    }

    jint write( JNIEnv*, j_compress_ptr, jint );
};

#endif  /* LC_dest_mgr_H */
/* vim:set et sw=4 ts=4: */
