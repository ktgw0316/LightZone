/* Copyright (C) 2026 LightZone Project */

#ifndef LC_JPEGWRITER_H
#define LC_JPEGWRITER_H

#ifdef _WIN32
#define LCJPEG_WRITER_EXPORT __declspec(dllexport)
#else
#define LCJPEG_WRITER_EXPORT
#endif

typedef int (*LCJPEGWriterWriteCallback)(unsigned char*, int);

extern "C" {
LCJPEG_WRITER_EXPORT void* lcjpeg_writer_create(
    LCJPEGWriterWriteCallback write,
    int bufferSize,
    int width,
    int height,
    int components,
    int colorSpace,
    int quality);
LCJPEG_WRITER_EXPORT int lcjpeg_writer_destroy(void* handle);
LCJPEG_WRITER_EXPORT int lcjpeg_writer_write_segment(
    void* handle,
    int marker,
    const unsigned char* buffer,
    int length);
LCJPEG_WRITER_EXPORT int lcjpeg_writer_write_scanlines(
    void* handle,
    const unsigned char* buffer,
    int bufferSize,
    int offset,
    int numLines,
    int lineStride);
LCJPEG_WRITER_EXPORT int lcjpeg_writer_last_error(unsigned char* buffer, int capacity);
}

#undef LCJPEG_WRITER_EXPORT

#endif
