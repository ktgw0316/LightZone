/*
 *  lcmsjnilib.c.c
 *
 *
 *  Created by Fabio Riccardi on 7/21/06.
 *  Copyright 2006 Light Crafts, Inc.. All rights reserved.
 *
 */
#include <ctype.h>
#include <stdint.h>

#ifndef AUTO_DEP
#include "javah/com_lightcrafts_utils_LCMSNative.h"
#endif

#include "LC_JNIUtils.h"
#include "lcms2.h"

#define DCRaw_METHOD(method) \
name4(Java_,com_lightcrafts_utils_LCMSNative,_,method)

JNIEXPORT jlong JNICALL Java_com_lightcrafts_utils_LCMSNative_cmsOpenProfileFromMem
  (JNIEnv *env, jclass clazz, jbyteArray jdata, jint size)
{
    char *data = (char *) (*env)->GetPrimitiveArrayCritical(env, jdata, 0);

    cmsHPROFILE result = cmsOpenProfileFromMem(data, size);

    (*env)->ReleasePrimitiveArrayCritical(env, jdata, data, 0);

    return (jlong)(intptr_t) result;
}

JNIEXPORT jlong JNICALL Java_com_lightcrafts_utils_LCMSNative_cmsCreateRGBProfile
  (JNIEnv *env, jclass clazz, jdoubleArray jWhitePoint, jdoubleArray jPrimaries, jdouble gamma)
{
      double *WhitePoint = (double *) (*env)->GetPrimitiveArrayCritical(env, jWhitePoint, 0);
      double *Primaries = (double *) (*env)->GetPrimitiveArrayCritical(env, jPrimaries, 0);
      int i;
      cmsHPROFILE result;

      cmsCIExyY w = { WhitePoint[0], WhitePoint[1], WhitePoint[2] };
      
      cmsCIExyYTRIPLE p = {
        {Primaries[0], Primaries[1], Primaries[2]},
        {Primaries[3], Primaries[4], Primaries[5]},
        {Primaries[6], Primaries[7], Primaries[8]}
      };

      cmsToneCurve* gammaTable[3];
      const int context = 1337;

      gammaTable[0] = gammaTable[1] = gammaTable[2] = cmsBuildGamma(gamma == 1 ? (cmsContext) &context : 0, gamma);

      result = cmsCreateRGBProfile(&w, &p, gammaTable);
      
      // _cmsSaveProfile( result, "/Stuff/matrixRGB.icc" );
      
      // cmsFreeToneCurve(gammaTable[0]);
      
      (*env)->ReleasePrimitiveArrayCritical(env, jWhitePoint, WhitePoint, 0);
      (*env)->ReleasePrimitiveArrayCritical(env, jPrimaries, Primaries, 0);

      return (jlong)(intptr_t) result;
}

JNIEXPORT jlong JNICALL Java_com_lightcrafts_utils_LCMSNative_cmsCreateLab2Profile
  (JNIEnv *env, jclass clazz)
{
    return (jlong)(intptr_t) cmsCreateLab2Profile(NULL);
}

JNIEXPORT jboolean JNICALL Java_com_lightcrafts_utils_LCMSNative_cmsCloseProfile
  (JNIEnv *env, jclass clazz, jlong jhProfile)
{
    cmsHPROFILE hProfile = (cmsHPROFILE)(intptr_t) jhProfile;
    return cmsCloseProfile(hProfile);
}

JNIEXPORT jlong JNICALL Java_com_lightcrafts_utils_LCMSNative_cmsCreateTransform
  (JNIEnv *env, jclass clazz, jlong inputProfile, jint inputFormat,
   jlong outputProfile, jint outputFormat, jint intent, jint flags)
{
    return (jlong)(intptr_t) cmsCreateTransform((cmsHPROFILE)(intptr_t) inputProfile, inputFormat,
                                      (cmsHPROFILE)(intptr_t) outputProfile, outputFormat,
                                      intent, flags);
}

JNIEXPORT jlong JNICALL Java_com_lightcrafts_utils_LCMSNative_cmsCreateProofingTransform
  (JNIEnv *env, jclass clazz, jlong inputProfile, jint inputFormat,
   jlong outputProfile, jint outputFormat, jlong proofingProfile,
   jint intent, jint proofingIntent, jint flags)
{
    return (jlong)(intptr_t) cmsCreateProofingTransform((cmsHPROFILE)(intptr_t) inputProfile, inputFormat,
					      (cmsHPROFILE)(intptr_t) outputProfile, outputFormat,
					      (cmsHPROFILE)(intptr_t) proofingProfile,
					      intent, proofingIntent, flags);
}


JNIEXPORT void JNICALL Java_com_lightcrafts_utils_LCMSNative_cmsDeleteTransform
  (JNIEnv *env, jclass clazz, jlong hTransform)
{
    cmsDeleteTransform((cmsHTRANSFORM)(intptr_t) hTransform);
}

void cmsDoTransformGeneric
  (JNIEnv *env, jclass clazz, jlong hTransform, jbyteArray jinputBuffer, jbyteArray joutputBuffer, jint size)
{
    char *inputBuffer = (char *) (*env)->GetPrimitiveArrayCritical(env, jinputBuffer, 0);
    char *outputBuffer = (char *) (*env)->GetPrimitiveArrayCritical(env, joutputBuffer, 0);

    if (hTransform)
        cmsDoTransform((cmsHTRANSFORM)(intptr_t) hTransform, inputBuffer, outputBuffer, size);

    (*env)->ReleasePrimitiveArrayCritical(env, jinputBuffer, inputBuffer, 0);
    (*env)->ReleasePrimitiveArrayCritical(env, joutputBuffer, outputBuffer, 0);
}

JNIEXPORT void JNICALL Java_com_lightcrafts_utils_LCMSNative_cmsDoTransform__J_3B_3BI
  (JNIEnv *env, jclass clazz, jlong hTransform, jbyteArray jinputBuffer, jbyteArray joutputBuffer, jint size)
{
    cmsDoTransformGeneric(env, clazz, hTransform, jinputBuffer, joutputBuffer, size);
}

JNIEXPORT void JNICALL Java_com_lightcrafts_utils_LCMSNative_cmsDoTransform__J_3S_3SI
  (JNIEnv *env, jclass clazz, jlong hTransform, jbyteArray jinputBuffer, jbyteArray joutputBuffer, jint size)
{
    cmsDoTransformGeneric(env, clazz, hTransform, jinputBuffer, joutputBuffer, size);
}

JNIEXPORT void JNICALL Java_com_lightcrafts_utils_LCMSNative_cmsDoTransform__J_3D_3DI
  (JNIEnv *env, jclass clazz, jlong hTransform, jbyteArray jinputBuffer, jbyteArray joutputBuffer, jint size)
{
    cmsDoTransformGeneric(env, clazz, hTransform, jinputBuffer, joutputBuffer, size);
}

