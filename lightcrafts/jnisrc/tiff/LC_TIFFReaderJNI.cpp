/* Copyright (C) 2005-2011 Fabio Riccardi */

// standard
#include <cstdlib>
#include <cstring>
#include <jni.h>
#include <stdarg.h>
#include <tiffio.h>

// local
#include "LC_JNIUtils.h"
#include "LC_TIFFCommon.h"
#include "util.h"
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_image_libs_LCTIFFReader.h"
#endif

using namespace std;
using namespace LightCrafts;

////////// JNI ////////////////////////////////////////////////////////////////

#define LCTIFFReader_METHOD(method) \
        name4(Java_,com_lightcrafts_image_libs_LCTIFFReader,_,method)

#define LCTIFFReader_CONSTANT(constant) \
        name3(com_lightcrafts_image_libs_LCTIFFReader,_,constant)

/**
 * Get the ICC Profile from the TIFF image file.
 */
JNIEXPORT jbyteArray JNICALL LCTIFFReader_METHOD(getICCProfileData)
    ( JNIEnv *env, jobject jLCTIFFReader )
{
    uint32 profileSize;
    void *profileData;
    int const result = TIFFGetField(
        getNativePtr( env, jLCTIFFReader ), TIFFTAG_ICCPROFILE,
        &profileSize, &profileData
    );
    if ( !result )
        return NULL;
    jbyteArray jProfileData = (jbyteArray)env->NewByteArray( profileSize );
    jarray_to_c<jbyte> cProfileData( env, jProfileData );
    ::memcpy( cProfileData, profileData, profileSize );
    return jProfileData;
}

/**
 * Get the given integer metadata field.
 */
JNIEXPORT jint JNICALL LCTIFFReader_METHOD(getIntField)
    ( JNIEnv *env, jobject jLCTIFFReader, jint tagID )
{
    TIFF *const tiff = getNativePtr( env, jLCTIFFReader );
    LC_TIFFFieldValue value, unused;
    int result;
    switch ( tagID ) {
        case TIFFTAG_EXTRASAMPLES:
            //
            // Special case for those tags that have extra values we're not
            // interested in.
            //
            result = TIFFGetField( tiff, tagID, &value, &unused );
            break;
        default:
            result = TIFFGetField( tiff, tagID, &value );
    }
    if ( !result )
        return -1;
    switch ( tagID ) {

        case TIFFTAG_BADFAXLINES:
        case TIFFTAG_CONSECUTIVEBADFAXLINES:
        case TIFFTAG_GROUP3OPTIONS:
        case TIFFTAG_GROUP4OPTIONS:
        case TIFFTAG_IMAGEDEPTH:
        case TIFFTAG_IMAGELENGTH:
        case TIFFTAG_IMAGEWIDTH:
        case TIFFTAG_ROWSPERSTRIP:
        case TIFFTAG_SUBFILETYPE:
        case TIFFTAG_TILEDEPTH:
        case TIFFTAG_TILELENGTH:
        case TIFFTAG_TILEWIDTH:
            return value.u32;

        case TIFFTAG_BITSPERSAMPLE:
        case TIFFTAG_CLEANFAXDATA:
        case TIFFTAG_COMPRESSION:
        case TIFFTAG_DATATYPE:
        case TIFFTAG_FILLORDER:
        case TIFFTAG_INKSET:
        case TIFFTAG_MATTEING:
        case TIFFTAG_MAXSAMPLEVALUE:
        case TIFFTAG_MINSAMPLEVALUE:
        case TIFFTAG_ORIENTATION:
        case TIFFTAG_PHOTOMETRIC:
        case TIFFTAG_PLANARCONFIG:
        case TIFFTAG_PREDICTOR:
        case TIFFTAG_RESOLUTIONUNIT:
        case TIFFTAG_SAMPLEFORMAT:
        case TIFFTAG_SAMPLESPERPIXEL:
        case TIFFTAG_THRESHHOLDING:
        case TIFFTAG_YCBCRPOSITIONING:
        case TIFFTAG_EXTRASAMPLES:
            return value.u16;

        default:
            LC_throwIllegalArgumentException( env, "unsupported tagID" );
            return -1;
    }
}

/**
 * Get one of the values for a two-value integer metadata field.
 */
JNIEXPORT jint JNICALL LCTIFFReader_METHOD(getIntField2)
    ( JNIEnv *env, jobject jLCTIFFReader, jint tagID, jboolean getSecond )
{
    TIFF *const tiff = getNativePtr( env, jLCTIFFReader );
    LC_TIFFFieldValue value[2];

    switch ( tagID ) {

        case TIFFTAG_PAGENUMBER:
            return  TIFFGetField( tiff, tagID, &value[0], &value[1] ) ?
                    value[ getSecond ].u16 : -1;

        default:
            LC_throwIllegalArgumentException( env, "unsupported tagID" );
            return -1;
    }
}

/**
 * Get the given integer metadata field.
 */
JNIEXPORT jstring JNICALL LCTIFFReader_METHOD(getStringField)
    ( JNIEnv *env, jobject jLCTIFFReader, jint tagID )
{
    TIFF *const tiff = getNativePtr( env, jLCTIFFReader );
    LC_TIFFFieldValue value;
    switch ( tagID ) {

        case TIFFTAG_ARTIST:
        case TIFFTAG_COPYRIGHT:
        case TIFFTAG_DATETIME:
        case TIFFTAG_DOCUMENTNAME:
        case TIFFTAG_HOSTCOMPUTER:
        case TIFFTAG_IMAGEDESCRIPTION:
        case TIFFTAG_INKNAMES:
        case TIFFTAG_MAKE:
        case TIFFTAG_MODEL:
        case TIFFTAG_PAGENAME:
        case TIFFTAG_SOFTWARE:
        case TIFFTAG_TARGETPRINTER:
            return  TIFFGetField( tiff, tagID, &value ) ?
                    env->NewStringUTF( value.cp ) : NULL;

        default:
            LC_throwIllegalArgumentException( env, "unsupported tagID" );
            return NULL;
    }
}

/**
 * Get the number of strips in the TIFF image.
 */
JNIEXPORT jint JNICALL LCTIFFReader_METHOD(getNumberOfStrips)
    ( JNIEnv *env, jobject jLCTIFFReader )
{
    return TIFFNumberOfStrips( getNativePtr( env, jLCTIFFReader ) );
}

/**
 * Get the number of tiles in the TIFF image.
 */
JNIEXPORT jint JNICALL LCTIFFReader_METHOD(getNumberOfTiles)
    ( JNIEnv *env, jobject jLCTIFFReader )
{
    return TIFFNumberOfTiles( getNativePtr( env, jLCTIFFReader ) );
}

/**
 * Get the size of a strip in bytes.
 */
JNIEXPORT jint JNICALL LCTIFFReader_METHOD(getStripSize)
    ( JNIEnv *env, jobject jLCTIFFReader )
{
    return TIFFStripSize( getNativePtr( env, jLCTIFFReader ) );
}

/**
 * Get the size of a tile in bytes.
 */
JNIEXPORT jint JNICALL LCTIFFReader_METHOD(getTileSize)
    ( JNIEnv *env, jobject jLCTIFFReader )
{
    return TIFFTileSize( getNativePtr( env, jLCTIFFReader ) );
}

/**
 * Returns true only if the TIFF image is tiled.
 */
JNIEXPORT jboolean JNICALL LCTIFFReader_METHOD(isTiled)
    ( JNIEnv *env, jobject jLCTIFFReader )
{
    return TIFFIsTiled( getNativePtr( env, jLCTIFFReader ) );
}

/**
 * Read the next TIFF directory.
 */
JNIEXPORT jboolean JNICALL LCTIFFReader_METHOD(nextDirectory)
    ( JNIEnv *env, jobject jLCTIFFReader )
{
    return TIFFReadDirectory( getNativePtr( env, jLCTIFFReader ) );
}

/**
 * Open a TIFF image file for reading.
 */
JNIEXPORT void JNICALL LCTIFFReader_METHOD(openForReading)
    ( JNIEnv *env, jobject jLCTIFFReader, jbyteArray jFileNameUtf8 )
{
    jbyteArray_to_c const cFileName( env, jFileNameUtf8 );
    LC_setNativePtr( env, jLCTIFFReader, LC_TIFFOpen( cFileName, "r" ) );
}

/**
 * Reads and decodes a strip from a TIFF image into a jbyteArray.
 */
JNIEXPORT jint JNICALL LCTIFFReader_METHOD(readStripByte)
    ( JNIEnv *env, jobject jLCTIFFReader, jint stripIndex, jbyteArray jBuf,
      jlong offset, jint stripSize )
{
    jarray_to_c<jbyte> cBuf( env, jBuf );
    if ( !cBuf ) {
        LC_throwOutOfMemoryError( env, "GetPrimitiveArrayCritical() failed" );
        return 0;
    }
    return TIFFReadEncodedStrip(
        getNativePtr( env, jLCTIFFReader ), stripIndex, (jbyte*)cBuf + offset, stripSize
    );
}

/**
 * Reads and decodes a strip from a TIFF image into a jshortArray.
 */
JNIEXPORT jint JNICALL LCTIFFReader_METHOD(readStripShort)
    ( JNIEnv *env, jobject jLCTIFFReader, jint stripIndex, jshortArray jBuf,
      jlong offset, jint stripSize )
{
    jarray_to_c<jshort> cBuf( env, jBuf );
    if ( !cBuf ) {
        LC_throwOutOfMemoryError( env, "GetPrimitiveArrayCritical() failed" );
        return 0;
    }
    return TIFFReadEncodedStrip(
        getNativePtr( env, jLCTIFFReader ), stripIndex, (jshort*)cBuf + offset, stripSize
    );
}

/**
 * Reads and decodes a tile from a TIFF image into a jbyteArray.
 */
JNIEXPORT jint JNICALL LCTIFFReader_METHOD(readTileByte)
    ( JNIEnv *env, jobject jLCTIFFReader, jint tileIndex, jbyteArray jBuf,
      jlong offset, jint tileSize )
{
    jarray_to_c<jbyte> cBuf( env, jBuf );
    if ( !cBuf ) {
        LC_throwOutOfMemoryError( env, "GetPrimitiveArrayCritical() failed" );
        return 0;
    }
    return TIFFReadEncodedTile(
        getNativePtr( env, jLCTIFFReader ), tileIndex, (jbyte*)cBuf + offset, tileSize
    );
}

/**
 * Reads and decodes a tile from a TIFF image into a jshortArray.
 */
JNIEXPORT jint JNICALL LCTIFFReader_METHOD(readTileShort)
    ( JNIEnv *env, jobject jLCTIFFReader, jint tileIndex, jshortArray jBuf,
      jlong offset, jint tileSize )
{
    jarray_to_c<jshort> cBuf( env, jBuf );
    if ( !cBuf ) {
        LC_throwOutOfMemoryError( env, "GetPrimitiveArrayCritical() failed" );
        return 0;
    }
    return TIFFReadEncodedTile(
        getNativePtr( env, jLCTIFFReader ), tileIndex, (jshort*)cBuf + offset, tileSize
    );
}

/* vim:set et sw=4 ts=4: */
