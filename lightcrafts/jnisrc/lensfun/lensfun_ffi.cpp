/* Copyright (C) 2026 LightZone Project */

#include "LC_lensfun.h"

#include <climits>
#include <cstring>
#include <string>
#include <vector>

namespace {

std::string displayName(const lfLens* lens) {
    std::string maker(lf_mlstr_get(lens->Maker));
    std::string model(lf_mlstr_get(lens->Model));
    if (model.compare(maker + " ") == 0) {
        model.erase(0, maker.length() + 1);
    }
    return maker + ": " + model;
}

std::vector<std::string> getNames(
        LC_lensfun* lensfun, int kind, const char* cameraMaker, const char* cameraModel) {
    std::vector<std::string> names;
    if (kind == 0) {
        const auto cameras = lensfun->getCameras();
        for (int i = 0; cameras[i]; ++i) {
            std::string maker(lf_mlstr_get(cameras[i]->Maker));
            std::string model(lf_mlstr_get(cameras[i]->Model));
            if (model.compare(maker + " ") == 0) {
                model.erase(0, maker.length() + 1);
            }
            names.push_back(maker + ": " + model);
        }
    } else if (kind == 1) {
        const auto lenses = lensfun->getLenses();
        for (int i = 0; lenses[i]; ++i) {
            names.push_back(displayName(lenses[i]));
        }
    } else if (kind == 2) {
        const auto camera = lensfun->findCamera(cameraMaker, cameraModel);
        const auto allLenses = lensfun->getLenses();
        for (int i = 0; allLenses[i]; ++i) {
            const auto lens =
                    lensfun->findLens(camera, allLenses[i]->Maker, allLenses[i]->Model);
            if (lens) {
                names.push_back(displayName(lens));
            }
        }
    }
    return names;
}

} // namespace

extern "C" {

void* lensfun_create(const char* path) {
    return path ? new LC_lensfun(path) : nullptr;
}

void lensfun_destroy(void* handle) {
    delete static_cast<LC_lensfun*>(handle);
}

int lensfun_get_names(
        void* handle, int kind, const char* cameraMaker, const char* cameraModel,
        char* buffer, int capacity) {
    if (!handle || capacity < 0) {
        return -1;
    }
    const auto names = getNames(
            static_cast<LC_lensfun*>(handle), kind,
            cameraMaker ? cameraMaker : "", cameraModel ? cameraModel : "");
    size_t required = 1;
    for (const auto& name : names) {
        required += name.size() + 1;
    }
    if (required > INT_MAX) {
        return -1;
    }
    if (buffer && capacity >= static_cast<int>(required)) {
        char* cursor = buffer;
        for (const auto& name : names) {
            std::memcpy(cursor, name.data(), name.size());
            cursor += name.size();
            *cursor++ = '\0';
        }
        *cursor = '\0';
    }
    return static_cast<int>(required);
}

void lensfun_init_modifier(
        void* handle, int fullWidth, int fullHeight,
        const char* cameraMaker, const char* cameraModel,
        const char* lensMaker, const char* lensModel,
        float focal, float aperture) {
    if (handle) {
        static_cast<LC_lensfun*>(handle)->initModifier(
                fullWidth, fullHeight, cameraMaker, cameraModel,
                lensMaker, lensModel, focal, aperture);
    }
}

void lensfun_init_modifier_poly5(
        void* handle, int fullWidth, int fullHeight,
        float k1, float k2, float kr, float kb, float focal, float aperture) {
    if (!handle) {
        return;
    }
    auto lensfun = static_cast<LC_lensfun*>(handle);
    auto lens = lensfun->getDefaultLens();
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

    lensfun->initModifier(fullWidth, fullHeight, 1, lens, focal, aperture);
}

void lensfun_distortion_color(
        void* handle, const unsigned short* srcData, unsigned short* dstData,
        int srcRectX, int srcRectY, int srcRectWidth, int srcRectHeight,
        int dstRectX, int dstRectY, int dstRectWidth, int dstRectHeight,
        int srcPixelStride, int dstPixelStride,
        int srcROffset, int srcGOffset, int srcBOffset,
        int dstROffset, int dstGOffset, int dstBOffset,
        int srcLineStride, int dstLineStride) {
    if (handle && srcData && dstData) {
        static_cast<LC_lensfun*>(handle)->applyModifier(
                srcData, dstData,
                srcRectX, srcRectY, srcRectWidth, srcRectHeight,
                dstRectX, dstRectY, dstRectWidth, dstRectHeight,
                srcPixelStride, dstPixelStride,
                srcROffset, srcGOffset, srcBOffset,
                dstROffset, dstGOffset, dstBOffset,
                srcLineStride, dstLineStride);
    }
}

void lensfun_backward_map_rect(
        void* handle, int dstRectX, int dstRectY, int dstRectWidth, int dstRectHeight,
        int* srcRectParams) {
    if (handle && srcRectParams) {
        static_cast<LC_lensfun*>(handle)->backwardMapRect(
                srcRectParams, dstRectX, dstRectY, dstRectWidth, dstRectHeight);
    }
}

} // extern "C"
