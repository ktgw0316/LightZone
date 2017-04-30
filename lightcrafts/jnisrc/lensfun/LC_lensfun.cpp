/* Copyright (C) 2015- Masahiro Kitagawa */

#include <iostream>
#include <jni.h>
#include <lensfun.h>
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
  jstring lensNameStr, jfloat focal, jfloat aperture )
{
    const char *cameraMaker = env->GetStringUTFChars(cameraMakerStr, NULL);
    const char *cameraModel = env->GetStringUTFChars(cameraModelStr, NULL);
    const char *lensName    = env->GetStringUTFChars(lensNameStr, NULL);

    // Load lensfun database
    lfDatabase *ldb = lf_db_new();
    ldb->Load();

    // Find camera in the database
    const lfCamera *camera = NULL;
    const lfCamera **cameras = ldb->FindCamerasExt(cameraMaker, cameraModel);

    if (!cameras) {
        std::cerr << "Cannot find the camera " << cameraModel << " in database" << std::endl;
    }
    else {
        camera = cameras[0];
        std::cerr << "camera: " << camera->Model << std::endl; // DEBUG
    }
    lf_free(cameras);
    env->ReleaseStringUTFChars(cameraMakerStr, 0);
    env->ReleaseStringUTFChars(cameraModelStr, 0);

    // Get lens informations from the database
    const lfLens **lenses = ldb->FindLenses(camera, NULL, lensName);

    if (!lenses) {
        std::cerr << "Cannot find the lens " << lensName << " in database" << std::endl;
        env->ReleaseStringUTFChars(lensNameStr, 0);
        return false;
    }

    for (int i = 0, imax = sizeof(lenses) / sizeof(lenses[0]); i < imax; ++i) {
        std::cerr << "lense" << i << ": " << lenses[i]->Model << std::endl; // DEBUG
    }
    const lfLens *lens = lenses[0];
    lf_free(lenses);
    env->ReleaseStringUTFChars(lensNameStr, 0);

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

