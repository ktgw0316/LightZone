/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <jni.h>
#include <tiffio.h>

// local
#include "LC_JNIUtils.h"

/**
 * An LC_TIFFFieldValue is used to contain the value of a TIFF field as gotten
 * via TIFFGetField() or to set via TIFFSetField().
 */
union LC_TIFFFieldValue {
    uint32  u32;
    uint32* u32p;
    uint16  u16;
    uint16* u16p;
    double  d;
    float   f;
    char*   cp;
    void*   vp;
};

/**
 * Get the pointer value previously stored inside the LCTIFFWriter object on
 * the Java side.
 */
inline TIFF* getNativePtr( JNIEnv *env, jobject jObject ) {
    return static_cast<TIFF*>( LC_getNativePtr( env, jObject ) );
}

/* vim:set et sw=4 ts=4: */
