/* Copyright (C) 2026 LightZone Project */

#ifndef LCMS_FFI_H
#define LCMS_FFI_H

#ifdef _WIN32
#define LCMS_FFI_EXPORT __declspec(dllexport)
#else
#define LCMS_FFI_EXPORT
#endif

#ifdef __cplusplus
extern "C" {
#endif

LCMS_FFI_EXPORT void* lcms_open_profile_from_mem(const unsigned char* data, int size);
LCMS_FFI_EXPORT void* lcms_create_rgb_profile(
    const double* whitePoint, const double* primaries, double gamma);
LCMS_FFI_EXPORT void* lcms_create_lab2_profile(void);
LCMS_FFI_EXPORT int lcms_close_profile(void* profile);
LCMS_FFI_EXPORT void* lcms_create_transform(
    void* inputProfile,
    int inputFormat,
    void* outputProfile,
    int outputFormat,
    int intent,
    int flags);
LCMS_FFI_EXPORT void* lcms_create_proofing_transform(
    void* inputProfile,
    int inputFormat,
    void* outputProfile,
    int outputFormat,
    void* proofingProfile,
    int intent,
    int proofingIntent,
    int flags);
LCMS_FFI_EXPORT void lcms_delete_transform(void* transform);
LCMS_FFI_EXPORT int lcms_do_transform_bytes(
    void* transform,
    const unsigned char* input,
    int inputElements,
    unsigned char* output,
    int outputElements,
    int pixels);
LCMS_FFI_EXPORT int lcms_do_transform_shorts(
    void* transform,
    const unsigned short* input,
    int inputElements,
    unsigned short* output,
    int outputElements,
    int pixels);
LCMS_FFI_EXPORT int lcms_do_transform_doubles(
    void* transform,
    const double* input,
    int inputElements,
    double* output,
    int outputElements,
    int pixels);

#ifdef __cplusplus
}
#endif

#undef LCMS_FFI_EXPORT

#endif
