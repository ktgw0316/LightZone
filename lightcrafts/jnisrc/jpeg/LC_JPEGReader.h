/* Copyright (C) 2026 LightZone Project */

#ifndef LC_JPEGREADER_H
#define LC_JPEGREADER_H

#ifdef _WIN32
#define LCJPEG_EXPORT __declspec(dllexport)
#else
#define LCJPEG_EXPORT
#endif

typedef int (*LCJPEGReaderReadCallback)(unsigned char*, int);

extern "C" {
LCJPEG_EXPORT void* lcjpeg_reader_create(
    LCJPEGReaderReadCallback read,
    int bufferSize,
    int maxWidth,
    int maxHeight);
LCJPEG_EXPORT int lcjpeg_reader_destroy(void* handle);
LCJPEG_EXPORT int lcjpeg_reader_width(void* handle);
LCJPEG_EXPORT int lcjpeg_reader_height(void* handle);
LCJPEG_EXPORT int lcjpeg_reader_components(void* handle);
LCJPEG_EXPORT int lcjpeg_reader_color_space(void* handle);
LCJPEG_EXPORT int lcjpeg_reader_read_scanlines(void* handle, unsigned char* buffer, int numLines);
LCJPEG_EXPORT int lcjpeg_reader_last_error(unsigned char* buffer, int capacity);
}

#undef LCJPEG_EXPORT

#endif
