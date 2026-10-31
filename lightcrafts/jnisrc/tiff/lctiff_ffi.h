/* Copyright (C) 2026 LightZone Project */

#ifndef LCTIFF_FFI_H
#define LCTIFF_FFI_H

#ifdef _WIN32
#define LCTIFF_FFI_EXPORT __declspec(dllexport)
#else
#define LCTIFF_FFI_EXPORT
#endif

#ifdef __cplusplus
extern "C" {
#endif

LCTIFF_FFI_EXPORT void ltiff_init(void);
LCTIFF_FFI_EXPORT void* ltiff_open(const char* filename, const char* mode);
LCTIFF_FFI_EXPORT void ltiff_close(void* handle);
LCTIFF_FFI_EXPORT int ltiff_last_error(char* buffer, int capacity);
LCTIFF_FFI_EXPORT int ltiff_reader_icc_profile(
    void* handle, unsigned char* buffer, int capacity);
LCTIFF_FFI_EXPORT int ltiff_reader_get_int(void* handle, int tag);
LCTIFF_FFI_EXPORT int ltiff_reader_get_int2(void* handle, int tag, int second);
LCTIFF_FFI_EXPORT int ltiff_reader_get_string(
    void* handle, int tag, char* buffer, int capacity);
LCTIFF_FFI_EXPORT int ltiff_reader_number_of_strips(void* handle);
LCTIFF_FFI_EXPORT int ltiff_reader_number_of_tiles(void* handle);
LCTIFF_FFI_EXPORT int ltiff_reader_strip_size(void* handle);
LCTIFF_FFI_EXPORT int ltiff_reader_tile_size(void* handle);
LCTIFF_FFI_EXPORT int ltiff_reader_is_tiled(void* handle);
LCTIFF_FFI_EXPORT int ltiff_reader_next_directory(void* handle);
LCTIFF_FFI_EXPORT int ltiff_reader_read_strip(
    void* handle, int index, void* buffer, int size);
LCTIFF_FFI_EXPORT int ltiff_reader_read_tile(
    void* handle, int index, void* buffer, int size);
LCTIFF_FFI_EXPORT int ltiff_writer_append(void* handle, const char* filename);
LCTIFF_FFI_EXPORT int ltiff_writer_compute_tile(
    void* handle, int x, int y, int z, int sample);
LCTIFF_FFI_EXPORT int ltiff_writer_set_bytes(
    void* handle, int tag, const unsigned char* buffer, int size);
LCTIFF_FFI_EXPORT int ltiff_writer_set_float(void* handle, int tag, float value);
LCTIFF_FFI_EXPORT int ltiff_writer_set_int(void* handle, int tag, int value);
LCTIFF_FFI_EXPORT int ltiff_writer_set_int2(
    void* handle, int tag, int first, int second);
LCTIFF_FFI_EXPORT int ltiff_writer_set_string(
    void* handle, int tag, const char* value);
LCTIFF_FFI_EXPORT int ltiff_writer_write_strip(
    void* handle, int index, const void* buffer, int size);
LCTIFF_FFI_EXPORT int ltiff_writer_write_tile(
    void* handle, int index, const void* buffer, int size);

#ifdef __cplusplus
}
#endif

#undef LCTIFF_FFI_EXPORT

#endif
