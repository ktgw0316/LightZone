/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef LC_source_mgr_H
#define LC_source_mgr_H

// standard
#include <jni.h>

extern "C" {
#   include <jpeglib.h>
}

/**
 * An LC_source_mgr is-a jpeg_source_mgr that adds stuff for reading image data
 * from the Java side.
 */
struct LC_source_mgr : jpeg_source_mgr {
    jclass      m_jImageDataProviderClass;
    jobject     m_jImageDataProvider;
    jmethodID   m_getImageData_methodID;
    jobject     m_jByteBuffer;
    JOCTET*     m_buffer;
    jint        m_bufSize;
    bool        m_startOfFile;

    LC_source_mgr( JNIEnv*, jobject jImageDataProvider, int bufSize );
    ~LC_source_mgr();
};

#endif  /* LC_source_mgr_H */
/* vim:set et sw=4 ts=4: */
