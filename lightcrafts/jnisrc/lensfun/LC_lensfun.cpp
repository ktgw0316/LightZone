/* Copyright (C) 2015- Masahiro Kitagawa */

#include "LC_lensfun.h"

#include <cstring>
#include <vector>
#include <jni.h>
#ifndef AUTO_DEP
#include "javah/com_lightcrafts_utils_Lensfun.h"
#endif

#include "LC_JNIUtils.h"

extern "C"
JNIEXPORT jboolean JNICALL Java_com_lightcrafts_utils_Lensfun_lensfunTerms
( JNIEnv *env, jclass cls,
  jintArray jDistModel, jfloatArray jDistTerms,
  jfloatArray jTcaTerms,
  jstring cameraMakerStr, jstring cameraModelStr,
  jstring lensMakerStr, jstring lensModelStr,
  jfloat focal, jfloat aperture )
{
    // Load lensfun database
    lfDatabase *ldb = lf_db_new();
    if (ldb->Load() != LF_NO_ERROR) {
        ldb->Destroy();
        return false;
    }

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
JNIEXPORT jobjectArray JNICALL
Java_com_lightcrafts_utils_Lensfun_getCameraNames
(JNIEnv *env, jclass cls)
{
    lfDatabase *ldb = lf_db_new();
    ldb->Load();
    const jobjectArray newArr = createJArray(env, ldb->GetCameras());
    ldb->Destroy();
    return newArr;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_lightcrafts_utils_Lensfun_getLensNames
(JNIEnv *env, jclass cls)
{
    lfDatabase *ldb = lf_db_new();
    ldb->Load();
    const jobjectArray newArr = createJArray(env, ldb->GetLenses());
    ldb->Destroy();
    return newArr;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_lightcrafts_utils_Lensfun_getLensNamesForCamera
(JNIEnv *env, jclass cls,
 jstring cameraMakerStr, jstring cameraModelStr)
{
    lfDatabase *ldb = lf_db_new();
    ldb->Load();

    const lfCamera *camera = findCamera(env, ldb, cameraMakerStr, cameraModelStr);
    const lfLens* const* allLenses = ldb->GetLenses();

    std::vector<const lfLens*> list;
    for (int i = 0; allLenses[i]; ++i) {
        const lfLens** lenses =
            ldb->FindLenses(camera, allLenses[i]->Maker, allLenses[i]->Model);
        if (!lenses) {
            continue;
        }
        list.push_back(lenses[0]);

        // DEBUG
        std::cout << "*** lens(" << i << ") = "
            << lenses[0]->Maker << ": " << lenses[0]->Model << std::endl;  
        lf_free(lenses);
    }
    const jobjectArray newArr = createJArray(env, list, list.size());
    ldb->Destroy();
    return newArr;
}

