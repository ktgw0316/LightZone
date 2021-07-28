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
        std::string maker(lf_mlstr_get(list[i]->Maker));
        std::string model(lf_mlstr_get(list[i]->Model));

        if (model.compare(maker + " ") == 0) {
            // Remove maker name and a space from model
            model.erase(0, maker.length() + 1);
        }
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
(JNIEnv *env, jobject obj, jstring pathStr)
{
    const auto path = env->GetStringUTFChars(pathStr, NULL);
    auto lf = new LC_lensfun(path);
    return reinterpret_cast<uintptr_t>(lf);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lightcrafts_utils_Lensfun_destroy
(JNIEnv *env, jobject obj, jlong handle)
{
    // Set the Java field to zero before delete the corresponding object
    const jclass cls = env->GetObjectClass(obj);
    const jfieldID field = env->GetFieldID(cls, "_handle", "J");
    if (!field) {
        return;
    }
    env->SetLongField(obj, field, 0);

    auto lf = reinterpret_cast<LC_lensfun*>(handle);
    delete lf;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_lightcrafts_utils_Lensfun_getCameraNames
(JNIEnv *env, jobject obj, jlong handle)
{
    auto lf = reinterpret_cast<LC_lensfun*>(handle);
    const auto jarr = createJArray(env, lf->getCameras());
    return jarr;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_lightcrafts_utils_Lensfun_getLensNames
(JNIEnv *env, jobject obj, jlong handle)
{
    auto lf = reinterpret_cast<LC_lensfun*>(handle);
    const auto jarr = createJArray(env, lf->getLenses());
    return jarr;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_lightcrafts_utils_Lensfun_getLensNamesForCamera
(JNIEnv *env, jobject obj,
 jlong handle, jstring cameraMakerStr, jstring cameraModelStr)
{
    auto lf = reinterpret_cast<LC_lensfun*>(handle);

    const auto cameraMaker = env->GetStringUTFChars(cameraMakerStr, NULL);
    const auto cameraModel = env->GetStringUTFChars(cameraModelStr, NULL);

    const auto camera = lf->findCamera(cameraMaker, cameraModel);

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
 jfloat focal, jfloat aperture)
{
    auto lf = reinterpret_cast<LC_lensfun*>(handle);

    const auto cameraMaker = env->GetStringUTFChars(cameraMakerStr, NULL);
    const auto cameraModel = env->GetStringUTFChars(cameraModelStr, NULL);
    const auto lensMaker   = env->GetStringUTFChars(lensMakerStr, NULL);
    const auto lensModel   = env->GetStringUTFChars(lensModelStr, NULL);

    lf->initModifier(
            fullWidth, fullHeight,
            cameraMaker, cameraModel,
            lensMaker, lensModel, focal, aperture);

    env->ReleaseStringUTFChars(cameraMakerStr, 0);
    env->ReleaseStringUTFChars(cameraModelStr, 0);
    env->ReleaseStringUTFChars(lensMakerStr, 0);
    env->ReleaseStringUTFChars(lensModelStr, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lightcrafts_utils_Lensfun_initModifierWithPoly5Lens
  (JNIEnv *env, jobject obj,
  jlong handle,
  jint fullWidth, jint fullHeight,
  jfloat k1, jfloat k2, jfloat kr, jfloat kb,
  jfloat focal, jfloat aperture)
{
    auto lf = reinterpret_cast<LC_lensfun*>(handle);

    auto lens = lf->getDefaultLens();
    if (!lens) {
        return;
    }

#if (LF_VERSION >= 0x00035f00) // 0.3.95
    lfLensCalibAttributes attr = {0, 0, 1, fullWidth / float(fullHeight)};
    lfLensCalibDistortion dc = {LF_DIST_MODEL_POLY5, focal, focal, 0, {k1, k2}, attr};
    lfLensCalibTCA tcac = {LF_TCA_MODEL_LINEAR, focal, {kr, kb}, attr};
    lens->RemoveCalibrations();
#else
    lfLensCalibDistortion dc = {LF_DIST_MODEL_POLY5, focal, {k1, k2}};
    lfLensCalibTCA tcac = {LF_TCA_MODEL_LINEAR, focal, {kr, kb}};
#endif
    lens->AddCalibDistortion(&dc);
    lens->AddCalibTCA(&tcac);
    // FIXME: Wrong autoscale, cf. https://github.com/lensfun/lensfun/issues/945

    lf->initModifier(fullWidth, fullHeight, 1, lens, focal, aperture);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lightcrafts_utils_Lensfun_distortionColor
  (JNIEnv *env, jobject obj,
  jlong handle,
  jshortArray jsrcData, jshortArray jdstData,
  jint srcRectX, jint srcRectY, jint srcRectWidth, jint srcRectHeight,
  jint dstRectX, jint dstRectY, jint dstRectWidth, jint dstRectHeight,
  jint srcPixelStride, jint dstPixelStride,
  jint srcROffset, jint srcGOffset, jint srcBOffset,
  jint dstROffset, jint dstGOffset, jint dstBOffset,
  jint srcLineStride, jint dstLineStride)
{
    auto lf = reinterpret_cast<LC_lensfun*>(handle);
    auto srcData = reinterpret_cast<unsigned short*>(
            env->GetPrimitiveArrayCritical(jsrcData, 0));
    auto dstData = reinterpret_cast<unsigned short*>(
            env->GetPrimitiveArrayCritical(jdstData, 0));

    lf->applyModifier(
            srcData, dstData,
            srcRectX, srcRectY,
            srcRectWidth, srcRectHeight,
            dstRectX, dstRectY,
            dstRectWidth, dstRectHeight,
            srcPixelStride, dstPixelStride,
            srcROffset, srcGOffset, srcBOffset,
            dstROffset, dstGOffset, dstBOffset,
            srcLineStride, dstLineStride);

    env->ReleasePrimitiveArrayCritical(jsrcData, srcData, 0);
    env->ReleasePrimitiveArrayCritical(jdstData, dstData, 0);
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_lightcrafts_utils_Lensfun_backwardMapRect
  (JNIEnv *env, jobject obj,
  jlong handle,
  jint dstRectX, jint dstRectY, jint dstRectWidth, jint dstRectHeight)
{
    auto lf = reinterpret_cast<LC_lensfun*>(handle);

    jintArray jsrcRect = env->NewIntArray(4);
    if (!jsrcRect) {
        return nullptr;
    }
    jint* srcRect = env->GetIntArrayElements(jsrcRect, nullptr);
    if (!srcRect) {
        return nullptr;
    }
    lf->backwardMapRect(
            reinterpret_cast<int*>(srcRect),
            dstRectX, dstRectY, dstRectWidth, dstRectHeight);
    env->ReleaseIntArrayElements(jsrcRect, srcRect, 0);
    return jsrcRect;
}

//
// LC_lensfun
//

LC_lensfun::LC_lensfun(const char* path)
{
    ldb = new lfDatabase();
    lfError err;

    std::cout << "Lensfun: loading database";
    if (strlen(path) > 0) {
        std::cout << " from " << path;
#if (LF_VERSION >= 0x00035f00) // 0.3.95
        err = ldb->Load(path);
#else
        err = ldb->LoadDirectory(path) ? LF_NO_ERROR : LF_NO_DATABASE;
#endif
    } else {
        err = ldb->Load();
    }
    std::cout << std::endl;

    if (err != LF_NO_ERROR) {
        std::cerr << "Lensfun database could not be loaded" << std::endl;
    }
}

LC_lensfun::~LC_lensfun()
{
    if (mod) {
        delete mod;
        mod = nullptr;
    }
    if (ldb) {
        delete ldb;
        ldb = nullptr;
    }
    if (default_lens) {
        delete default_lens;
        default_lens = nullptr;
    }
}

const lfCamera* LC_lensfun::findCamera(
        const char *cameraMaker, const char *cameraModel) const
{
    const auto cameras = ldb->FindCamerasExt(cameraMaker, cameraModel);

    if (!cameras) {
#ifdef DEBUG
        std::cerr << "Cannot find the camera \""
            << cameraMaker << ": " << cameraModel << "\""
            << " in database" << std::endl;
#endif
        return nullptr;
    }
    const auto camera = cameras[0];
    lf_free(cameras);
    return camera;
}

const lfLens* LC_lensfun::findLens(
        const lfCamera* camera, const char *lensMaker, const char *lensModel) const
{
    const auto lenses = ldb->FindLenses(camera, lensMaker, lensModel);

    if (!lenses) {
        return nullptr;
    }

#ifdef DEBUG
    for (int i = 0; lenses[i]; ++i) {
        std::cerr << "** lens" << i << " = "
            << lenses[i]->Maker << ": " << lenses[i]->Model << std::endl;
    }
#endif

    const auto lens = lenses[0];
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

lfLens* LC_lensfun::getDefaultLens() {
    if (!default_lens) {
        default_lens = const_cast<lfLens*>(
                findLens(nullptr, "Generic", "Rectilinear 10-1000mm f/1.0"));
        if (!default_lens->Check()) {
            std::cout << "Lensfun: Failed to get default lens" << std::endl;
            default_lens = nullptr;
        }
    }
    return default_lens;
}

void LC_lensfun::initModifier
( int fullWidth, int fullHeight,
  const char* cameraMaker, const char* cameraModel,
  const char* lensMaker, const char* lensModel,
  float focal, float aperture )
{
    const auto camera = findCamera(cameraMaker, cameraModel);
    const auto found_lens = findLens(camera, lensMaker, lensModel);
    const auto lens = found_lens ? found_lens : getDefaultLens();

    if (camera) {
        std::cout << "Lensfun: camera maker: " << camera->Maker << std::endl;
        std::cout << "Lensfun: camera model: " << camera->Model << std::endl;
    }
    else {
        std::cout << "Lensfun: Camera not found" << std::endl;
    }
    if (found_lens) {
        std::cout << "Lensfun: lens model  : " << lens->Model << std::endl;
    }
    else {
        std::cout << "Lensfun: fallback to the default lens" << std::endl;
    }

#if (LF_VERSION >= 0x00035f00) // 0.3.95
    const float crop = camera ? camera->CropFactor : 1.0f;
#else
    const float crop = camera ? camera->CropFactor : lens->CropFactor;
#endif

    initModifier(fullWidth, fullHeight, crop, lens, focal, aperture);
}

void LC_lensfun::initModifier
( int fullWidth, int fullHeight, float crop,
  const lfLens* lens, float focal, float aperture )
{
    if (focal < 0.1f) {
        focal = lens->MaxFocal;
    }
    if (aperture < 0.1f) {
        aperture = lens->MinAperture;
    }
    constexpr float distance = 10; // TODO:
    constexpr float scale = 0; // automatic scaling
    const lfLensType targeom = lens->Type;

    if (mod) {
        delete mod;
        mod = nullptr;
    }
#if (LF_VERSION >= 0x00035f00) // 0.3.95
    mod = new lfModifier(crop, fullWidth, fullHeight, LF_PF_U16);
#else
    mod = new lfModifier(lens, crop, fullWidth, fullHeight);
#endif
    if (!mod) {
        // FIXME: This causes crash
        std::cout << "*** mod unavailable" << std::endl;
        return;
    }
#if (LF_VERSION >= 0x00035f00) // 0.3.95
    mod->EnableDistortionCorrection(lens, focal);
    mod->EnableTCACorrection(lens, focal);
    mod->EnableVignettingCorrection(lens, focal, aperture, distance);
    mod->EnableProjectionTransform(lens, focal, targeom);
    mod->EnableScaling(scale);
#else
    mod->Initialize(lens, LF_PF_U16, focal, aperture, distance, scale, targeom,
            LF_MODIFY_ALL, false);
#endif
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
#pragma omp parallel
    {
        auto pos = new float[dstRectWidth * 2 * 3];

#pragma omp for schedule (guided)
        for (int y = dstRectY; y < dstRectY + dstRectHeight; ++y) {
            mod->ApplySubpixelGeometryDistortion(dstRectX, y, dstRectWidth, 1, pos);

            for (int x = dstRectX, i = 0; x < dstRectX + dstRectWidth; ++x, i += 6) {
                const auto srcRX = pos[i];
                const auto srcRY = pos[i + 1];
                const auto srcGX = pos[i + 2];
                const auto srcGY = pos[i + 3];
                const auto srcBX = pos[i + 4];
                const auto srcBY = pos[i + 5];

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
                    dstData[dstIdx + dstROffset] = MitchellInterp(srcData, srcPixelStride,
                            srcROffset, srcLineStride, srcRX - srcRectX, srcRY - srcRectY);
                    dstData[dstIdx + dstGOffset] = MitchellInterp(srcData, srcPixelStride,
                            srcGOffset, srcLineStride, srcGX - srcRectX, srcGY - srcRectY);
                    dstData[dstIdx + dstBOffset] = MitchellInterp(srcData, srcPixelStride,
                            srcBOffset, srcLineStride, srcBX - srcRectX, srcBY - srcRectY);
                }
            }
        }

        delete[] pos;
    } // omp parallel
}

void LC_lensfun::backwardMapRect
( int* srcRectParams,
  int dstRectX, int dstRectY,
  int dstRectWidth, int dstRectHeight ) const
{
    auto top    = new float[dstRectWidth  * 2 * 3];
    auto bottom = new float[dstRectWidth  * 2 * 3];
    auto left   = new float[dstRectHeight * 2 * 3];
    auto right  = new float[dstRectHeight * 2 * 3];

    mod->ApplySubpixelGeometryDistortion(dstRectX, dstRectY,
            dstRectWidth, 1, top);
    mod->ApplySubpixelGeometryDistortion(dstRectX, dstRectY + dstRectHeight,
            dstRectWidth, 1, bottom);
    mod->ApplySubpixelGeometryDistortion(dstRectX,
            dstRectY, 1, dstRectHeight, left);
    mod->ApplySubpixelGeometryDistortion(dstRectX + dstRectWidth,
            dstRectY, 1, dstRectHeight, right);

    // initial values
    auto srcRectY    = top[1];
    auto srcRectMaxY = bottom[1];
    auto srcRectX    = left[0];
    auto srcRectMaxX = right[0];

#pragma omp parallel
    {
#pragma omp for simd reduction(min:srcRectY, max:srcRectMaxY) nowait
        for (int x = dstRectX, i = 0; x < dstRectX + dstRectWidth; ++x, i += 6) {
            // Find topmost pixel
            const auto srcRY0 = top[i + 1];
            const auto srcGY0 = top[i + 3];
            const auto srcBY0 = top[i + 5];
            const auto minY = std::min(srcRY0, std::min(srcGY0, srcBY0));
            if (minY < srcRectY) {
                srcRectY = minY;
            }

            // Find bottommost pixel
            const auto srcRY1 = bottom[i + 1];
            const auto srcGY1 = bottom[i + 3];
            const auto srcBY1 = bottom[i + 5];
            const auto maxY = std::max(srcRY1, std::max(srcGY1, srcBY1));
            if (maxY > srcRectMaxY) {
                srcRectMaxY = maxY;
            }
        }

#pragma omp for simd reduction(min:srcRectX, max:srcRectMaxX)
        for (int y = dstRectY, i = 0; y < dstRectY + dstRectHeight; ++y, i += 6) {
            // Find leftmost pixel
            const auto srcRX0 = left[i];
            const auto srcGX0 = left[i + 2];
            const auto srcBX0 = left[i + 4];
            const auto minX = std::min(srcRX0, std::min(srcGX0, srcBX0));
            if (minX < srcRectX) {
                srcRectX = minX;
            }

            // Find rightmost pixel
            const auto srcRX1 = right[i];
            const auto srcGX1 = right[i + 2];
            const auto srcBX1 = right[i + 4];
            const auto maxX = std::max(srcRX1, std::max(srcGX1, srcBX1));
            if (maxX > srcRectMaxX) {
                srcRectMaxX = maxX;
            }
        }
    }

    delete[] top;
    delete[] bottom;
    delete[] left;
    delete[] right;

    // margin is required for interpolation
    const int topMost    = static_cast<int>(srcRectX) - 1;
    const int leftMost   = static_cast<int>(srcRectY) - 1;
    const int bottomMost = static_cast<int>(srcRectMaxX) + 2;
    const int rightMost  = static_cast<int>(srcRectMaxY) + 2;
    srcRectParams[0] = topMost;
    srcRectParams[1] = leftMost;
    srcRectParams[2] = bottomMost - topMost;
    srcRectParams[3] = rightMost - leftMost;
}
