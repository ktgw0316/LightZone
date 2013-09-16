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
#include "javah/com_lightcrafts_image_libs_LCTIFFWriter.h"
#endif

using namespace std;
using namespace LightCrafts;

extern "C" int tiffcp( TIFF*, TIFF* );

char const AppName[] = "LightZone";

////////// Local functions ////////////////////////////////////////////////////

/**
 * Checks whether a given TIFF file is a 2-page (layered) TIFF file created by
 * LightZone.
 */
bool isLightZoneLayeredTIFF( TIFF *tiff ) {
    LC_TIFFFieldValue value[2];
    if ( !TIFFGetField( tiff, TIFFTAG_SOFTWARE, &value[0] ) )
        return false;
    if ( ::strncmp( value[0].cp, AppName, ::strlen( AppName ) ) )
        return false;
    if ( !TIFFGetField( tiff, TIFFTAG_PAGENUMBER, &value[0], &value[1] ) )
        return false;
    return value[1].u16 == 2;
}

////////// JNI ////////////////////////////////////////////////////////////////

#define LCTIFFWriter_METHOD(method) \
        name4(Java_,com_lightcrafts_image_libs_LCTIFFWriter,_,method)

#define LCTIFFWriter_CONSTANT(constant) \
        name3(com_lightcrafts_image_libs_LCTIFFWriter,_,constant)

/**
 * Append the TIFF image in the given file.
 */
JNIEXPORT jboolean JNICALL LCTIFFWriter_METHOD(append)
    ( JNIEnv *env, jobject jLCTIFFWriter, jbyteArray jFileNameUtf8 )
{
    TIFF *const destTIFF = getNativePtr( env, jLCTIFFWriter );
    TIFFSetField( destTIFF, TIFFTAG_PAGENUMBER, 0, 2 );
    int result = TIFFWriteDirectory( destTIFF );
    if ( !result )
        return JNI_FALSE;
    jbyteArray_to_c const cFileName( env, jFileNameUtf8 );
    TIFF *const srcTIFF = LC_TIFFOpen( cFileName, "r" );
    if ( !srcTIFF )
        return JNI_FALSE;
    if ( isLightZoneLayeredTIFF( srcTIFF ) )
        TIFFReadDirectory( srcTIFF );
    result = tiffcp( srcTIFF, destTIFF );
    TIFFSetField( destTIFF, TIFFTAG_PAGENUMBER, 1, 2 );
    TIFFClose( srcTIFF );
    return result ? JNI_TRUE : JNI_FALSE;
}

/**
 * Compute which tile an (x,y,z,s) value is in.
 */
JNIEXPORT jint JNICALL LCTIFFWriter_METHOD(computeTile)
    ( JNIEnv *env, jobject jLCTIFFWriter, jint x, jint y, jint z, jint sample )
{
    TIFF *const tiff = getNativePtr( env, jLCTIFFWriter );
    return TIFFComputeTile( tiff, x, y, z, sample );
}

/**
 * Open a TIFF image file for writing.
 */
JNIEXPORT void JNICALL LCTIFFWriter_METHOD(openForWriting)
    ( JNIEnv *env, jobject jLCTIFFWriter, jbyteArray jFileNameUtf8 )
{
    jbyteArray_to_c const cFileName( env, jFileNameUtf8 );
    LC_setNativePtr( env, jLCTIFFWriter, LC_TIFFOpen( cFileName, "w" ) );
}

/**
 * Set the given byte metadata field.
 */
JNIEXPORT jboolean JNICALL LCTIFFWriter_METHOD(setByteField)
    ( JNIEnv *env, jobject jLCTIFFWriter, jint tagID, jbyteArray jArray )
{
    TIFF *const tiff = getNativePtr( env, jLCTIFFWriter );
    LC_TIFFFieldValue value;

    jarray_to_c<jbyte> const cArray( env, jArray );
    switch ( tagID ) {

        case TIFFTAG_ICCPROFILE:
        case TIFFTAG_JPEGTABLES:
        case TIFFTAG_LIGHTCRAFTS_LIGHTZONE:
        case TIFFTAG_PHOTOSHOP:
        case TIFFTAG_RICHTIFFIPTC:
        case TIFFTAG_XMLPACKET:
            value.vp = cArray;
            return TIFFSetField( tiff, tagID, cArray.length(), value.vp );

        default:
            LC_throwIllegalArgumentException( env, "unsupported tagID" );
            return JNI_FALSE;
    }
}

/**
 * Set the given float metadata field.
 */
JNIEXPORT jboolean JNICALL LCTIFFWriter_METHOD(setFloatField)
    ( JNIEnv *env, jobject jLCTIFFWriter, jint tagID, jfloat jValue )
{
    TIFF *const tiff = getNativePtr( env, jLCTIFFWriter );
    LC_TIFFFieldValue value;
    switch ( tagID ) {

        case TIFFTAG_XRESOLUTION:
        case TIFFTAG_YRESOLUTION:
            value.f = jValue;
            return TIFFSetField( tiff, tagID, value.f );

        default:
            LC_throwIllegalArgumentException( env, "unsupported tagID" );
            return JNI_FALSE;
    }
}

/**
 * Set the given integer metadata field.
 */
JNIEXPORT jboolean JNICALL LCTIFFWriter_METHOD(setIntField)
    ( JNIEnv *env, jobject jLCTIFFWriter, jint tagID, jint jValue )
{
    TIFF *const tiff = getNativePtr( env, jLCTIFFWriter );
    LC_TIFFFieldValue value;
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
            value.u32 = jValue;
            return TIFFSetField( tiff, tagID, value.u32 );

        case TIFFTAG_BITSPERSAMPLE:
        case TIFFTAG_CLEANFAXDATA:
        case TIFFTAG_COMPRESSION:
        case TIFFTAG_DATATYPE:
        case TIFFTAG_FILLORDER:
        case TIFFTAG_INKSET:
        case TIFFTAG_MATTEING:
        case TIFFTAG_MAXSAMPLEVALUE:
        case TIFFTAG_MINSAMPLEVALUE:
        case TIFFTAG_MS_RATING:
        case TIFFTAG_ORIENTATION:
        case TIFFTAG_PHOTOMETRIC:
        case TIFFTAG_PLANARCONFIG:
        case TIFFTAG_PREDICTOR:
        case TIFFTAG_RESOLUTIONUNIT:
        case TIFFTAG_SAMPLEFORMAT:
        case TIFFTAG_SAMPLESPERPIXEL:
        case TIFFTAG_THRESHHOLDING:
        case TIFFTAG_YCBCRPOSITIONING:
            value.u16 = jValue;
            return TIFFSetField( tiff, tagID, value.u16 );

        default:
            LC_throwIllegalArgumentException( env, "unsupported tagID" );
            return JNI_FALSE;
    }
}

/**
 * Set the given two-value integer metadata field.
 */
JNIEXPORT jboolean JNICALL LCTIFFWriter_METHOD(setIntField2)
    ( JNIEnv *env, jobject jLCTIFFWriter, jint tagID, jint jValue1,
      jint jValue2 )
{
    TIFF *const tiff = getNativePtr( env, jLCTIFFWriter );
    LC_TIFFFieldValue value[2];
    switch ( tagID ) {

        case TIFFTAG_PAGENUMBER:
            value[0].u16 = jValue1;
            value[1].u16 = jValue2;
            return TIFFSetField( tiff, tagID, value[0].u16, value[1].u16 );

        default:
            LC_throwIllegalArgumentException( env, "unsupported tagID" );
            return JNI_FALSE;
    }
}

/**
 * Set the given string metadata field.
 */
JNIEXPORT jboolean JNICALL LCTIFFWriter_METHOD(setStringField)
    ( JNIEnv *env, jobject jLCTIFFWriter, jint tagID, jbyteArray jValueUtf8 )
{
    TIFF *const tiff = getNativePtr( env, jLCTIFFWriter );
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
        case TIFFTAG_TARGETPRINTER: {
            jbyteArray_to_c const cValue( env, jValueUtf8 );
            return TIFFSetField(
                tiff, tagID, static_cast<char const*>( cValue )
            );
        }

        default:
            LC_throwIllegalArgumentException( env, "unsupported tagID" );
            return JNI_FALSE;
    }
}

/**
 * Encodes and writes a strip from a jbyteArray to a TIFF image.
 */
JNIEXPORT jint JNICALL LCTIFFWriter_METHOD(writeStripByte)
    ( JNIEnv *env, jobject jLCTIFFWriter, jint stripIndex, jbyteArray jBuf,
      jlong offset, jint stripSize )
{
    jarray_to_c<jbyte> const cBuf( env, jBuf );
    if ( !cBuf ) {
        LC_throwOutOfMemoryError( env, "GetPrimitiveArrayCritical() failed" );
        return 0;
    }
    return TIFFWriteEncodedStrip(
        getNativePtr( env, jLCTIFFWriter ), stripIndex, (jbyte*)cBuf + offset, stripSize
    );
}

/**
 * Encodes and writes a strip from a jshortArray to a TIFF image.
 */
JNIEXPORT jint JNICALL LCTIFFWriter_METHOD(writeStripShort)
    ( JNIEnv *env, jobject jLCTIFFWriter, jint stripIndex, jshortArray jBuf,
      jlong offset, jint stripSize )
{
    jarray_to_c<jshort> const cBuf( env, jBuf );
    if ( !cBuf ) {
        LC_throwOutOfMemoryError( env, "GetPrimitiveArrayCritical() failed" );
        return 0;
    }
    return TIFFWriteEncodedStrip(
        getNativePtr( env, jLCTIFFWriter ), stripIndex, (jshort*)cBuf + offset, stripSize
    );
}

/**
 * Encodes and writes a tile from a jbyteArray to a TIFF image.
 */
JNIEXPORT jint JNICALL LCTIFFWriter_METHOD(writeTileByte)
    ( JNIEnv *env, jobject jLCTIFFWriter, jint tileIndex, jbyteArray jBuf,
      jlong offset, jint tileSize )
{
    jarray_to_c<jbyte> const cBuf( env, jBuf );
    if ( !cBuf ) {
        LC_throwOutOfMemoryError( env, "GetPrimitiveArrayCritical() failed" );
        return 0;
    }
    return TIFFWriteEncodedTile(
        getNativePtr( env, jLCTIFFWriter ), tileIndex, (jbyte*)cBuf + offset, tileSize
    );
}

/**
 * Encodes and writes a tile from a jshortArray to a TIFF image.
 */
JNIEXPORT jint JNICALL LCTIFFWriter_METHOD(writeTileShort)
    ( JNIEnv *env, jobject jLCTIFFWriter, jint tileIndex, jshortArray jBuf,
      jlong offset, jint tileSize )
{
    jarray_to_c<jshort> const cBuf( env, jBuf );
    if ( !cBuf ) {
        LC_throwOutOfMemoryError( env, "GetPrimitiveArrayCritical() failed" );
        return 0;
    }
    return TIFFWriteEncodedTile(
        getNativePtr( env, jLCTIFFWriter ), tileIndex, (jshort*)cBuf + offset, tileSize
    );
}

/* vim:set et sw=4 ts=4: */
