/* Copyright (C) 2006 Light Crafts, Inc. */
/* Copyright (C) 2026 LightZone Project */

#include <stdint.h>

#include "lcms2.h"
#include "lcms_ffi.h"

void* lcms_open_profile_from_mem(const unsigned char* data, int size) {
    if (!data || size < 0) {
        return NULL;
    }
    return cmsOpenProfileFromMem(data, (cmsUInt32Number) size);
}

void* lcms_create_rgb_profile(
    const double* whitePoint, const double* primaries, double gamma) {
    if (!whitePoint || !primaries) {
        return NULL;
    }

    const cmsCIExyY white = {whitePoint[0], whitePoint[1], whitePoint[2]};
    const cmsCIExyYTRIPLE primary = {
        {primaries[0], primaries[1], primaries[2]},
        {primaries[3], primaries[4], primaries[5]},
        {primaries[6], primaries[7], primaries[8]}
    };
    cmsToneCurve* curves[3];
    const int context = 1337;
    curves[0] = curves[1] = curves[2] =
        cmsBuildGamma(gamma == 1 ? (cmsContext) &context : NULL, gamma);
    return cmsCreateRGBProfile(&white, &primary, curves);
}

void* lcms_create_lab2_profile(void) {
    return cmsCreateLab2Profile(NULL);
}

int lcms_close_profile(void* profile) {
    return cmsCloseProfile((cmsHPROFILE) profile);
}

void* lcms_create_transform(
    void* inputProfile,
    int inputFormat,
    void* outputProfile,
    int outputFormat,
    int intent,
    int flags) {
    return cmsCreateTransform(
        (cmsHPROFILE) inputProfile,
        (cmsUInt32Number) inputFormat,
        (cmsHPROFILE) outputProfile,
        (cmsUInt32Number) outputFormat,
        (cmsUInt32Number) intent,
        (cmsUInt32Number) flags);
}

void* lcms_create_proofing_transform(
    void* inputProfile,
    int inputFormat,
    void* outputProfile,
    int outputFormat,
    void* proofingProfile,
    int intent,
    int proofingIntent,
    int flags) {
    return cmsCreateProofingTransform(
        (cmsHPROFILE) inputProfile,
        (cmsUInt32Number) inputFormat,
        (cmsHPROFILE) outputProfile,
        (cmsUInt32Number) outputFormat,
        (cmsHPROFILE) proofingProfile,
        (cmsUInt32Number) intent,
        (cmsUInt32Number) proofingIntent,
        (cmsUInt32Number) flags);
}

void lcms_delete_transform(void* transform) {
    if (transform) {
        cmsDeleteTransform((cmsHTRANSFORM) transform);
    }
}

static int check_transform_buffers(
    cmsHTRANSFORM transform,
    cmsUInt32Number pixels,
    int inputElements,
    int outputElements,
    int expectedBytes) {
    if (!transform) {
        return 0;
    }
    if (inputElements < 0 || outputElements < 0) {
        return -1;
    }

    const cmsUInt32Number inputFormat = cmsGetTransformInputFormat(transform);
    const cmsUInt32Number outputFormat = cmsGetTransformOutputFormat(transform);
    if (T_BYTES(inputFormat) != expectedBytes ||
        T_BYTES(outputFormat) != expectedBytes ||
        (expectedBytes == 0 && (!T_FLOAT(inputFormat) || !T_FLOAT(outputFormat)))) {
        return -1;
    }

    const uint64_t inputRequired =
        (uint64_t) pixels * (T_CHANNELS(inputFormat) + T_EXTRA(inputFormat));
    const uint64_t outputRequired =
        (uint64_t) pixels * (T_CHANNELS(outputFormat) + T_EXTRA(outputFormat));
    return (uint64_t) inputElements >= inputRequired &&
                   (uint64_t) outputElements >= outputRequired
        ? 0
        : -1;
}

int lcms_do_transform_bytes(
    void* transform,
    const unsigned char* input,
    int inputElements,
    unsigned char* output,
    int outputElements,
    int pixels) {
    if (pixels < 0 || !input || !output ||
        check_transform_buffers(
            (cmsHTRANSFORM) transform,
            (cmsUInt32Number) pixels,
            inputElements,
            outputElements,
            1) < 0) {
        return -1;
    }
    if (transform) {
        cmsDoTransform((cmsHTRANSFORM) transform, input, output, (cmsUInt32Number) pixels);
    }
    return 0;
}

int lcms_do_transform_shorts(
    void* transform,
    const unsigned short* input,
    int inputElements,
    unsigned short* output,
    int outputElements,
    int pixels) {
    if (pixels < 0 || !input || !output ||
        check_transform_buffers(
            (cmsHTRANSFORM) transform,
            (cmsUInt32Number) pixels,
            inputElements,
            outputElements,
            2) < 0) {
        return -1;
    }
    if (transform) {
        cmsDoTransform((cmsHTRANSFORM) transform, input, output, (cmsUInt32Number) pixels);
    }
    return 0;
}

int lcms_do_transform_doubles(
    void* transform,
    const double* input,
    int inputElements,
    double* output,
    int outputElements,
    int pixels) {
    if (pixels < 0 || !input || !output ||
        check_transform_buffers(
            (cmsHTRANSFORM) transform,
            (cmsUInt32Number) pixels,
            inputElements,
            outputElements,
            0) < 0) {
        return -1;
    }
    if (transform) {
        cmsDoTransform((cmsHTRANSFORM) transform, input, output, (cmsUInt32Number) pixels);
    }
    return 0;
}
