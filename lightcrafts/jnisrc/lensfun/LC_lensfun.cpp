/* Copyright (C) 2015- Masahiro Kitagawa */

#include "LC_lensfun.h"

#include <cmath>
#include <cstring>
#include <vector>
#include <omp.h>
#include <jni.h>
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_utils_Lensfun.h"
#endif

#include "LC_JNIUtils.h"
#include "interpolation.h"

extern "C"
JNIEXPORT jboolean JNICALL Java_com_lightcrafts_utils_Lensfun_lensfunTerms
( JNIEnv *env, jclass cls,
  jintArray jDistModel, jfloatArray jDistTerms,
  jfloatArray jTcaTerms,
  jstring cameraMakerStr, jstring cameraModelStr,
  jstring lensMakerStr, jstring lensModelStr,
  jfloat focal, jfloat aperture )
{
/*
    const char *cameraMaker = env->GetStringUTFChars(cameraMakerStr, NULL);
    const char *cameraModel = env->GetStringUTFChars(cameraModelStr, NULL);
    const char *lensMaker = env->GetStringUTFChars(lensMakerStr, NULL);
    const char *lensModel = env->GetStringUTFChars(lensModelStr, NULL);
    
    const boolean b = lensfunTerms(
    // TODO:
    );

    env->ReleaseStringUTFChars(cameraMakerStr, 0);
    env->ReleaseStringUTFChars(cameraModelStr, 0);
    env->ReleaseStringUTFChars(lensMakerStr, 0);
    env->ReleaseStringUTFChars(lensModelStr, 0);

    return b;
    */
    return true;
}

/*
boolean LC_lensfun::lensfunTerms() {
    const lfCamera *camera = findCamera(env, ldb, cameraMakerStr, cameraModelStr);
    const lfLens *lens = findLenses(env, ldb, camera, lensMakerStr, lensModelStr);
    if (!lens) {
        ldb->Destroy();
        return false;
    }

    float crop = lens->CropFactor;
    if (focal < 0.1f) {
        focal = lens->MaxFocal;
    }
    if (aperture < 0.1f) {
        aperture = lens->MinAperture;
    }

    // Get distortion model and terms
    lfLensCalibDistortion dist;
    if (lens->InterpolateDistortion((float)focal, dist)) {
        const lfDistortionModel dist_model = dist.Model;
        float* dist_terms = dist.Terms;

        jint* distModel = env->GetIntArrayElements(jDistModel, 0);
        jfloat* distTerms = env->GetFloatArrayElements(jDistTerms, 0);

        switch (dist_model) {
        case LF_DIST_MODEL_POLY3:
            distModel[0] = 1;
            distTerms[0] = dist_terms[0];

            std::cerr << "DistortionModel: poly3" << std::endl;
            std::cerr << "Terms: " << dist_terms[0] << std::endl;
            break;
        case LF_DIST_MODEL_POLY5:
            distModel[0] = 2;
            distTerms[0] = dist_terms[0];
            distTerms[1] = dist_terms[1];

            std::cerr << "DistortionModel: poly5" << std::endl;
            std::cerr << "Terms: " << dist_terms[0] << ", "
                                   << dist_terms[1] << std::endl;
            break;
        case LF_DIST_MODEL_PTLENS:
            distModel[0] = 3;
            distTerms[0] = dist_terms[0];
            distTerms[1] = dist_terms[1];
            distTerms[2] = dist_terms[2];

            std::cerr << "DistortionModel: PTLens" << std::endl;
            std::cerr << "Terms: " << dist_terms[0] << ", "
                                   << dist_terms[1] << ", "
                                   << dist_terms[2] << std::endl;
            break;
        default:
            std::cerr << "DistortionModel: Unknown" << std::endl;
        }

        env->ReleaseIntArrayElements(jDistModel, distModel, 0);
        env->ReleaseFloatArrayElements(jDistTerms, distTerms, 0);
    }

    // Get TCA model and terms
    lfLensCalibTCA tca;
    if (lens->InterpolateTCA((float)focal, tca)) {
        const lfTCAModel tca_model = tca.Model;
        float* tca_terms = tca.Terms;

        // jint* tcaModel = env->GetIntArrayElements(jTcaModel, 0);
        jfloat* tcaTerms = env->GetFloatArrayElements(jTcaTerms, 0);

        switch (tca_model) {
        case LF_TCA_MODEL_LINEAR:
            // tcaModel[0] = 1;
            tcaTerms[0] = tca_terms[0];
            tcaTerms[1] = tca_terms[1];

            std::cerr << "TCAModel: Linear" << std::endl;
            std::cerr << "Terms: " << "kr = "<< tca_terms[0] << ", "
                                   << "kb = "<< tca_terms[1] << std::endl;
            break;
        case LF_TCA_MODEL_POLY3:
            // tcaModel[0] = 2;
            // for (int i = 0; i < 6; ++i) {
            //     tcaTerms[0] = tca_terms[0];
            // }

            // NOTE: LightZone currently supports linear TCA model only.
            tcaTerms[0] = 1;
            tcaTerms[1] = 1;

            std::cerr << "TCAModel: Poly3" << std::endl;
            std::cerr << "Terms: " << "br = " << tca_terms[0] << ", "
                                   << "cr = " << tca_terms[1] << ", "
                                   << "vr = " << tca_terms[2] << "; "
                                   << "bb = " << tca_terms[3] << ", "
                                   << "cb = " << tca_terms[4] << ", "
                                   << "vb = " << tca_terms[5] << std::endl;
            break;
        default:
            std::cerr << "TCAModel: Unknown" << std::endl;
        }

        env->ReleaseFloatArrayElements(jTcaTerms, tcaTerms, 0);
    }

    // TODO: Get vignetting model and terms

    ldb->Destroy();
    return true;
}
*/

template <typename T>
inline jobjectArray createJArray(JNIEnv *env, const T list, int size = -1)
{
    if (size < 0) {
        // list must be null-terminated in this case
        size = 0;
        while (list[size]) {
            ++size;
        }
    }

    const jclass clazz = env->FindClass("java/lang/String");
    const jobjectArray newArr = env->NewObjectArray(size, clazz, nullptr);

    jstring utf_str;
    for (int i = 0; i < size; ++i) {
        const char* c_maker = lf_mlstr_get(list[i]->Maker);
        const char* c_model = lf_mlstr_get(list[i]->Model);

        const int len = strlen(c_maker);
        const int cmp = strncmp(c_model, c_maker, len);
        if (!cmp) {
            // Remove maker name and a space from model
            c_model += len + 1;
        }

        const std::string maker(c_maker);
        const std::string model(c_model);
        const std::string name = maker + ": " + model;
        const char* c_name = name.c_str();
        utf_str = env->NewStringUTF(c_name);
        env->SetObjectArrayElement(newArr, i, utf_str);
        env->DeleteLocalRef(utf_str);
    }

    return newArr;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_lightcrafts_utils_Lensfun_init
(JNIEnv *env, jobject obj)
{
    LC_lensfun* lf = new LC_lensfun();
    return reinterpret_cast<long>(lf);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lightcrafts_utils_Lensfun_destroy
(JNIEnv *env, jobject obj, jlong handle)
{
    // Set the Java field to zero before delete the corresponding object
    const jclass cls = env->GetObjectClass(obj);
    const jfieldID field = env->GetFieldID(cls, "handle", "L");
    if (!field) {
        return;
    }
    env->SetLongField(obj, field, 0);

    LC_lensfun* lf = reinterpret_cast<LC_lensfun*>(handle);
    delete lf;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_lightcrafts_utils_Lensfun_getCameraNames
(JNIEnv *env, jclass cls, jlong handle)
{
    LC_lensfun* lf = reinterpret_cast<LC_lensfun*>(handle);
    const jobjectArray newArr = createJArray(env, lf->getCameras());
    return newArr;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_lightcrafts_utils_Lensfun_getLensNames
(JNIEnv *env, jclass cls, jlong handle)
{
    LC_lensfun* lf = reinterpret_cast<LC_lensfun*>(handle);
    const jobjectArray newArr = createJArray(env, lf->getLenses());
    return newArr;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_lightcrafts_utils_Lensfun_getLensNamesForCamera
(JNIEnv *env, jclass cls,
 jlong handle, jstring cameraMakerStr, jstring cameraModelStr)
{
    LC_lensfun* lf = reinterpret_cast<LC_lensfun*>(handle);

    const char *cameraMaker = env->GetStringUTFChars(cameraMakerStr, NULL);
    const char *cameraModel = env->GetStringUTFChars(cameraModelStr, NULL);

    const lfCamera *camera = lf->findCamera(cameraMaker, cameraModel);

    env->ReleaseStringUTFChars(cameraMakerStr, 0);
    env->ReleaseStringUTFChars(cameraModelStr, 0);
    
    const lfLens* const* allLenses = lf->getLenses();

    std::vector<const lfLens*> list;
    for (int i = 0; allLenses[i]; ++i) {
        const lfLens* lens =
            lf->findLens(camera, allLenses[i]->Maker, allLenses[i]->Model);
        if (!lens) {
            continue;
        }
        list.push_back(lens);

#ifdef DEBUG
        std::cout << "*** lens(" << i << ") = "
            << lens->Maker << ": " << lens->Model << std::endl;  
#endif
    }
    return createJArray(env, list, list.size());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lightcrafts_utils_Lensfun_initModifier
(JNIEnv *env, jobject obj, jlong handle,
 jint fullWidth, jint fullHeight,
 jstring cameraMakerStr, jstring cameraModelStr,
 jstring lensMakerStr, jstring lensModelStr,
 float focal, float aperture)
{
    LC_lensfun* lf = reinterpret_cast<LC_lensfun*>(handle);

    const char *cameraMaker = env->GetStringUTFChars(cameraMakerStr, NULL);
    const char *cameraModel = env->GetStringUTFChars(cameraModelStr, NULL);
    const char *lensMaker = env->GetStringUTFChars(lensMakerStr, NULL);
    const char *lensModel = env->GetStringUTFChars(lensModelStr, NULL);

    lf->initModifier(
            fullWidth, fullHeight,
            cameraMaker, cameraModel,
            lensMaker, lensModel, focal, aperture);

    env->ReleaseStringUTFChars(cameraMakerStr, 0);
    env->ReleaseStringUTFChars(cameraModelStr, 0);
    env->ReleaseStringUTFChars(lensMakerStr, 0);
    env->ReleaseStringUTFChars(lensModelStr, 0);
}

//
// LC_lensfun
//

LC_lensfun::LC_lensfun() : ldb(lf_db_new())
{
    ldb->Load();
}

LC_lensfun::~LC_lensfun()
{
    delete mod;
    ldb->Destroy();
}

const lfCamera* LC_lensfun::findCamera(
        const char *cameraMaker, const char *cameraModel) const
{
    const lfCamera **cameras = ldb->FindCamerasExt(cameraMaker, cameraModel);

    if (!cameras) {
#ifdef DEBUG
        std::cerr << "Cannot find the camera \""
            << cameraMaker << ": " << cameraModel << "\"" 
            << " in database" << std::endl;
#endif
        return nullptr;
    }
    const lfCamera *camera = cameras[0];
    lf_free(cameras);
    return camera;
}

const lfLens* LC_lensfun::findLens(
        const lfCamera* camera, const char *lensMaker, const char *lensModel) const
{
    const lfLens **lenses = ldb->FindLenses(camera, lensMaker, lensModel);

    if (!lenses) {
        return nullptr;
    }

#ifdef DEBUG
    for (int i = 0; lenses[i]; ++i) {
        std::cerr << "** lens" << i << " = "
            << lenses[i]->Maker << ": " << lenses[i]->Model << std::endl;
    }
#endif

    const lfLens *lens = lenses[0];
    lf_free(lenses);
    return lens;
}

const lfCamera* const* LC_lensfun::getCameras() const
{
    return ldb->GetCameras();
}

const lfLens* const* LC_lensfun::getLenses() const
{
    return ldb->GetLenses();
}

void LC_lensfun::initModifier
( int fullWidth, int fullHeight,
  const char* cameraMaker, const char* cameraModel,
  const char* lensMaker, const char* lensModel,
  float focal, float aperture )
{
    const lfCamera *camera = findCamera(cameraMaker, cameraModel);
    const lfLens *lens = findLens(camera, lensMaker, lensModel);
    if (!lens) {
        return;
    }

    const float crop = camera ? camera->CropFactor : lens->CropFactor;
    if (focal < 0.1f) {
        focal = lens->MaxFocal;
    }
    if (aperture < 0.1f) {
        aperture = lens->MinAperture;
    }
    constexpr float distance = 10; // TODO:
    constexpr float scale = 0; // automatic scaling
    const lfLensType targeom = lens->Type;

    mod = new lfModifier(lens, crop, fullWidth, fullHeight);
    if (!mod) {
        return;
    }
    mod->Initialize(lens, LF_PF_U16, focal, aperture, distance, scale, targeom,
            LF_MODIFY_ALL, false);
}

void LC_lensfun::applyModifier
( const unsigned short *srcData, unsigned short *dstData,
  int srcRectX, int srcRectY,
  int srcRectWidth, int srcRectHeight,
  int dstRectX, int dstRectY,
  int dstRectWidth, int dstRectHeight,
  int srcPixelStride, int dstPixelStride,
  int srcROffset, int srcGOffset, int srcBOffset,
  int dstROffset, int dstGOffset, int dstBOffset,
  int srcLineStride, int dstLineStride ) const
{
    float* pos = new float[dstRectWidth * 2 * 3];

#pragma omp parallel for schedule (guided)
    for (int y = dstRectY; y < dstRectY + dstRectHeight; ++y) {
        mod->ApplySubpixelGeometryDistortion(dstRectX, y, dstRectWidth, 1, pos);

        for (int x = dstRectX, i = 0; x < dstRectX + dstRectWidth; ++x, i += 6) {
            const float srcRX = pos[i];
            const float srcRY = pos[i + 1];
            const float srcGX = pos[i + 2];
            const float srcGY = pos[i + 3];
            const float srcBX = pos[i + 4];
            const float srcBY = pos[i + 5];

            const int dstIdx =
                dstPixelStride * (x - dstRectX) + (y - dstRectY) * dstLineStride;

            if (srcRX < srcRectX || srcRX >= srcRectX + srcRectWidth
             || srcRY < srcRectY || srcRY >= srcRectY + srcRectHeight
             || srcGX < srcRectX || srcGX >= srcRectX + srcRectWidth
             || srcGY < srcRectY || srcGY >= srcRectY + srcRectHeight
             || srcBX < srcRectX || srcBX >= srcRectX + srcRectWidth
             || srcBY < srcRectY || srcBY >= srcRectY + srcRectHeight) {
                dstData[dstIdx + dstROffset] = 0;
                dstData[dstIdx + dstGOffset] = 0;
                dstData[dstIdx + dstBOffset] = 0;
            }
            else {
                const unsigned short valueR = BilinearInterp(
                        srcData, srcPixelStride, srcROffset, srcLineStride,
                        srcRX - srcRectX, srcRY - srcRectY);
                const unsigned short valueG = BilinearInterp(
                        srcData, srcPixelStride, srcGOffset, srcLineStride,
                        srcGX - srcRectX, srcGY - srcRectY);
                const unsigned short valueB = BilinearInterp(
                        srcData, srcPixelStride, srcBOffset, srcLineStride,
                        srcBX - srcRectX, srcBY - srcRectY);
                dstData[dstIdx + dstROffset] = valueR < 0xffff ? valueR : 0xffff;
                dstData[dstIdx + dstGOffset] = valueG < 0xffff ? valueG : 0xffff;
                dstData[dstIdx + dstBOffset] = valueB < 0xffff ? valueB : 0xffff;
            }
        }
    }

    delete[] pos;
}

void LC_lensfun::backwardMapRect
( int* srcRectParams,
  int fullWidth, int fullHeight,
  int centerX, int centerY,
  int dstRectX, int dstRectY,
  int dstRectWidth, int dstRectHeight,
  const char* cameraMakerStr, const char* cameraModelStr,
  const char* lensMakerStr, const char* lensModelStr,
  float focal, float aperture ) const
{
    float* top    = new float[dstRectWidth  * 2 * 3];
    float* bottom = new float[dstRectWidth  * 2 * 3];
    float* left   = new float[dstRectHeight * 2 * 3];
    float* right  = new float[dstRectHeight * 2 * 3];

    mod->ApplySubpixelGeometryDistortion(dstRectX, dstRectY,
            dstRectWidth, 1, top);
    mod->ApplySubpixelGeometryDistortion(dstRectX, dstRectY + dstRectHeight,
            dstRectWidth, 1, bottom);
    mod->ApplySubpixelGeometryDistortion(dstRectX,
            dstRectY, 1, dstRectHeight, left);
    mod->ApplySubpixelGeometryDistortion(dstRectX + dstRectWidth,
            dstRectY, 1, dstRectHeight, right);

    // initial values
    int srcRectY    = top[1];
    int srcRectMaxY = bottom[1];
    int srcRectX    = left[0];
    int srcRectMaxX = right[0];

#pragma omp parallel
    {
#pragma omp for simd reduction(min:srcRectY, max:srcRectMaxY) nowait
        for (int x = dstRectX, i = 0; x < dstRectX + dstRectWidth; ++x, i += 6) {
            // Find topmost pixel
            const float srcRY0 = top[i + 1];
            const float srcGY0 = top[i + 3];
            const float srcBY0 = top[i + 5];
            const float minY = std::min(srcRY0, std::min(srcGY0, srcBY0));
            if (minY < srcRectY) {
                srcRectY = minY;
            }

            // Find bottommost pixel
            const float srcRY1 = bottom[i + 1];
            const float srcGY1 = bottom[i + 3];
            const float srcBY1 = bottom[i + 5];
            const float maxY = std::max(srcRY1, std::max(srcGY1, srcBY1));
            if (maxY > srcRectMaxY) {
                srcRectMaxY = maxY;
            }
        }

#pragma omp for simd reduction(min:srcRectX, max:srcRectMaxX)
        for (int y = dstRectY, i = 0; y < dstRectY + dstRectHeight; ++y, i += 6) {
            // Find leftmost pixel
            const float srcRX0 = left[i];
            const float srcGX0 = left[i + 2];
            const float srcBX0 = left[i + 4];
            const float minX = std::min(srcRX0, std::min(srcGX0, srcBX0));
            if (minX < srcRectX) {
                srcRectX = minX;
            }

            // Find rightmost pixel
            const float srcRX1 = right[i];
            const float srcGX1 = right[i + 2];
            const float srcBX1 = right[i + 4];
            const float maxX = std::max(srcRX1, std::max(srcGX1, srcBX1));
            if (maxX > srcRectMaxX) {
                srcRectMaxX = maxX;
            }
        }
    }

    delete[] top;
    delete[] bottom;
    delete[] left;
    delete[] right;

    // return values
    srcRectParams[0] = srcRectX;
    srcRectParams[1] = srcRectY;
    srcRectParams[2] = srcRectMaxX - srcRectX + 1; // width
    srcRectParams[3] = srcRectMaxY - srcRectY + 1; // height
}

