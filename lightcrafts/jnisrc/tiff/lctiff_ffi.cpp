/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2026 LightZone Project */

#include <algorithm>
#include <cstdarg>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>
#include <tiffio.h>

#ifdef _WIN32
#include <windows.h>
#endif

#include "LC_TIFFCommon.h"
#include "lctiff_ffi.h"

extern "C" int tiffcp(TIFF*, TIFF*);

namespace {

thread_local std::string lastError;

union FieldValue {
    uint32_t u32;
    uint16_t u16;
    float f;
    char* string;
    void* pointer;
};

void errorHandler(const char*, const char* format, va_list args) {
    char message[1024];
    vsnprintf(message, sizeof(message), format, args);
    lastError = message;
}

void warningHandler(const char*, const char*, va_list) {
}

TIFF* openTiff(const char* filename, const char* mode) {
#ifdef _WIN32
    const int size = MultiByteToWideChar(CP_UTF8, 0, filename, -1, nullptr, 0);
    if (size <= 0) {
        return nullptr;
    }
    std::vector<wchar_t> wideFilename(static_cast<size_t>(size));
    if (MultiByteToWideChar(CP_UTF8, 0, filename, -1, wideFilename.data(), size) == 0) {
        return nullptr;
    }
    return TIFFOpenW(wideFilename.data(), mode);
#else
    return TIFFOpen(filename, mode);
#endif
}

bool validHandle(void* handle) {
    if (handle) {
        return true;
    }
    lastError = "TIFF handle is closed";
    return false;
}

bool mergeFieldInfo(TIFF* tiff) {
    static const TIFFFieldInfo fields[] = {
        {TIFFTAG_LIGHTZONE, -1, -1, TIFF_BYTE, FIELD_CUSTOM, 1, 1,
         const_cast<char*>("LightZone")},
        {TIFFTAG_MS_RATING, -1, -1, TIFF_SHORT, FIELD_CUSTOM, 1, 1,
         const_cast<char*>("Rating")},
    };
    return TIFFMergeFieldInfo(tiff, fields, sizeof(fields) / sizeof(fields[0])) == 0;
}

bool isStringTag(int tag) {
    switch (tag) {
        case TIFFTAG_ARTIST:
        case TIFFTAG_COPYRIGHT:
        case TIFFTAG_DATETIME:
        case TIFFTAG_DOCUMENTNAME:
        case TIFFTAG_HOSTCOMPUTER:
        case TIFFTAG_IMAGEDESCRIPTION:
        case TIFFTAG_INKNAMES:
        case TIFFTAG_MAKE:
        case TIFFTAG_MODEL:
        case TIFFTAG_PAGENAME:
        case TIFFTAG_SOFTWARE:
        case TIFFTAG_TARGETPRINTER:
            return true;
        default:
            return false;
    }
}

} // namespace

extern "C" {

void ltiff_init(void) {
    TIFFSetErrorHandler(&errorHandler);
    TIFFSetWarningHandler(&warningHandler);
}

void* ltiff_open(const char* filename, const char* mode) {
    lastError.clear();
    return filename && mode ? openTiff(filename, mode) : nullptr;
}

void ltiff_close(void* handle) {
    lastError.clear();
    if (handle) {
        TIFFClose(static_cast<TIFF*>(handle));
    }
}

int ltiff_last_error(char* buffer, int capacity) {
    if (!buffer || capacity < 1) {
        return -1;
    }
    const size_t count = std::min(lastError.size(), static_cast<size_t>(capacity - 1));
    std::memcpy(buffer, lastError.data(), count);
    buffer[count] = '\0';
    return static_cast<int>(count);
}

int ltiff_reader_icc_profile(void* handle, unsigned char* buffer, int capacity) {
    lastError.clear();
    if (!validHandle(handle) || capacity < 0 || (capacity > 0 && !buffer)) {
        return -1;
    }
    uint32_t size = 0;
    void* data = nullptr;
    if (!TIFFGetField(static_cast<TIFF*>(handle), TIFFTAG_ICCPROFILE, &size, &data)) {
        return -1;
    }
    if (!buffer) {
        return static_cast<int>(size);
    }
    if (static_cast<uint32_t>(capacity) < size) {
        lastError = "ICC profile buffer is too small";
        return -1;
    }
    std::memcpy(buffer, data, size);
    return static_cast<int>(size);
}

int ltiff_reader_get_int(void* handle, int tag) {
    lastError.clear();
    if (!validHandle(handle)) {
        return -1;
    }
    FieldValue value{}, unused{};
    const int result =
        tag == TIFFTAG_EXTRASAMPLES
            ? TIFFGetField(static_cast<TIFF*>(handle), tag, &value, &unused)
            : TIFFGetField(static_cast<TIFF*>(handle), tag, &value);
    if (!result) {
        return -1;
    }
    switch (tag) {
        case TIFFTAG_BADFAXLINES:
        case TIFFTAG_CONSECUTIVEBADFAXLINES:
        case TIFFTAG_GROUP3OPTIONS:
        case TIFFTAG_GROUP4OPTIONS:
        case TIFFTAG_IMAGEDEPTH:
        case TIFFTAG_IMAGELENGTH:
        case TIFFTAG_IMAGEWIDTH:
        case TIFFTAG_ROWSPERSTRIP:
        case TIFFTAG_SUBFILETYPE:
        case TIFFTAG_TILEDEPTH:
        case TIFFTAG_TILELENGTH:
        case TIFFTAG_TILEWIDTH:
            return static_cast<int>(value.u32);
        case TIFFTAG_BITSPERSAMPLE:
        case TIFFTAG_CLEANFAXDATA:
        case TIFFTAG_COMPRESSION:
        case TIFFTAG_DATATYPE:
        case TIFFTAG_EXTRASAMPLES:
        case TIFFTAG_FILLORDER:
        case TIFFTAG_INKSET:
        case TIFFTAG_MATTEING:
        case TIFFTAG_MAXSAMPLEVALUE:
        case TIFFTAG_MINSAMPLEVALUE:
        case TIFFTAG_ORIENTATION:
        case TIFFTAG_PHOTOMETRIC:
        case TIFFTAG_PLANARCONFIG:
        case TIFFTAG_PREDICTOR:
        case TIFFTAG_RESOLUTIONUNIT:
        case TIFFTAG_SAMPLEFORMAT:
        case TIFFTAG_SAMPLESPERPIXEL:
        case TIFFTAG_THRESHHOLDING:
        case TIFFTAG_YCBCRPOSITIONING:
            return value.u16;
        default:
            lastError = "unsupported tagID";
            return -1;
    }
}

int ltiff_reader_get_int2(void* handle, int tag, int second) {
    lastError.clear();
    if (!validHandle(handle)) {
        return -1;
    }
    if (tag != TIFFTAG_PAGENUMBER) {
        lastError = "unsupported tagID";
        return -1;
    }
    uint16_t values[2]{};
    return TIFFGetField(static_cast<TIFF*>(handle), tag, &values[0], &values[1])
        ? values[second != 0] : -1;
}

int ltiff_reader_get_string(void* handle, int tag, char* buffer, int capacity) {
    lastError.clear();
    if (!validHandle(handle)) {
        return -1;
    }
    if (!isStringTag(tag)) {
        lastError = "unsupported tagID";
        return -1;
    }
    FieldValue value{};
    if (!TIFFGetField(static_cast<TIFF*>(handle), tag, &value)) {
        return -1;
    }
    const size_t length = std::strlen(value.string);
    if (!buffer || capacity < 0 || length >= static_cast<size_t>(capacity)) {
        lastError = "TIFF string buffer is too small";
        return -1;
    }
    std::memcpy(buffer, value.string, length + 1);
    return static_cast<int>(length);
}

int ltiff_reader_number_of_strips(void* handle) {
    lastError.clear();
    return validHandle(handle) ? static_cast<int>(TIFFNumberOfStrips(static_cast<TIFF*>(handle))) : -1;
}

int ltiff_reader_number_of_tiles(void* handle) {
    lastError.clear();
    return validHandle(handle) ? static_cast<int>(TIFFNumberOfTiles(static_cast<TIFF*>(handle))) : -1;
}

int ltiff_reader_strip_size(void* handle) {
    lastError.clear();
    return validHandle(handle) ? static_cast<int>(TIFFStripSize(static_cast<TIFF*>(handle))) : -1;
}

int ltiff_reader_tile_size(void* handle) {
    lastError.clear();
    return validHandle(handle) ? static_cast<int>(TIFFTileSize(static_cast<TIFF*>(handle))) : -1;
}

int ltiff_reader_is_tiled(void* handle) {
    lastError.clear();
    return validHandle(handle) && TIFFIsTiled(static_cast<TIFF*>(handle));
}

int ltiff_reader_next_directory(void* handle) {
    lastError.clear();
    return validHandle(handle) && TIFFReadDirectory(static_cast<TIFF*>(handle));
}

int ltiff_reader_read_strip(void* handle, int index, void* buffer, int size) {
    lastError.clear();
    if (!validHandle(handle) || !buffer || size < 0) {
        return -1;
    }
    return static_cast<int>(
        TIFFReadEncodedStrip(static_cast<TIFF*>(handle), index, buffer, size));
}

int ltiff_reader_read_tile(void* handle, int index, void* buffer, int size) {
    lastError.clear();
    if (!validHandle(handle) || !buffer || size < 0) {
        return -1;
    }
    return static_cast<int>(
        TIFFReadEncodedTile(static_cast<TIFF*>(handle), index, buffer, size));
}

int ltiff_writer_append(void* handle, const char* filename) {
    lastError.clear();
    if (!validHandle(handle) || !filename) {
        return 0;
    }
    TIFF* destination = static_cast<TIFF*>(handle);
    TIFFSetField(destination, TIFFTAG_PAGENUMBER, 0, 2);
    if (!TIFFWriteDirectory(destination)) {
        return 0;
    }
    TIFF* source = openTiff(filename, "r");
    if (!source) {
        return 0;
    }
    FieldValue software{};
    uint16_t pageNumber = 0;
    uint16_t pageCount = 0;
    if (TIFFGetField(source, TIFFTAG_SOFTWARE, &software) &&
        std::strncmp(software.string, "LightZone", std::strlen("LightZone")) == 0 &&
        TIFFGetField(source, TIFFTAG_PAGENUMBER, &pageNumber, &pageCount) &&
        pageCount == 2) {
        TIFFReadDirectory(source);
    }
    const int result = tiffcp(source, destination);
    TIFFSetField(destination, TIFFTAG_PAGENUMBER, 1, 2);
    TIFFClose(source);
    return result;
}

int ltiff_writer_compute_tile(void* handle, int x, int y, int z, int sample) {
    lastError.clear();
    return validHandle(handle)
        ? static_cast<int>(TIFFComputeTile(static_cast<TIFF*>(handle), x, y, z, sample)) : -1;
}

int ltiff_writer_set_bytes(void* handle, int tag, const unsigned char* buffer, int size) {
    lastError.clear();
    if (!validHandle(handle) || !buffer || size < 0) {
        return 0;
    }
    TIFF* tiff = static_cast<TIFF*>(handle);
    mergeFieldInfo(tiff);
    switch (tag) {
        case TIFFTAG_ICCPROFILE:
        case TIFFTAG_JPEGTABLES:
        case TIFFTAG_LIGHTZONE:
        case TIFFTAG_PHOTOSHOP:
        case TIFFTAG_XMLPACKET:
            return TIFFSetField(tiff, tag, size, buffer);
        case TIFFTAG_RICHTIFFIPTC:
            return TIFFSetField(tiff, tag, size / 4, buffer);
        default:
            lastError = "unsupported tagID";
            return 0;
    }
}

int ltiff_writer_set_float(void* handle, int tag, float value) {
    lastError.clear();
    if (!validHandle(handle)) {
        return 0;
    }
    if (tag != TIFFTAG_XRESOLUTION && tag != TIFFTAG_YRESOLUTION) {
        lastError = "unsupported tagID";
        return 0;
    }
    return TIFFSetField(static_cast<TIFF*>(handle), tag, value);
}

int ltiff_writer_set_int(void* handle, int tag, int value) {
    lastError.clear();
    if (!validHandle(handle)) {
        return 0;
    }
    TIFF* tiff = static_cast<TIFF*>(handle);
    mergeFieldInfo(tiff);
    switch (tag) {
        case TIFFTAG_BADFAXLINES:
        case TIFFTAG_CONSECUTIVEBADFAXLINES:
        case TIFFTAG_GROUP3OPTIONS:
        case TIFFTAG_GROUP4OPTIONS:
        case TIFFTAG_IMAGEDEPTH:
        case TIFFTAG_IMAGELENGTH:
        case TIFFTAG_IMAGEWIDTH:
        case TIFFTAG_ROWSPERSTRIP:
        case TIFFTAG_SUBFILETYPE:
        case TIFFTAG_TILEDEPTH:
        case TIFFTAG_TILELENGTH:
        case TIFFTAG_TILEWIDTH:
            return TIFFSetField(tiff, tag, static_cast<uint32_t>(value));
        case TIFFTAG_BITSPERSAMPLE:
        case TIFFTAG_CLEANFAXDATA:
        case TIFFTAG_COMPRESSION:
        case TIFFTAG_DATATYPE:
        case TIFFTAG_FILLORDER:
        case TIFFTAG_INKSET:
        case TIFFTAG_MATTEING:
        case TIFFTAG_MAXSAMPLEVALUE:
        case TIFFTAG_MINSAMPLEVALUE:
        case TIFFTAG_MS_RATING:
        case TIFFTAG_ORIENTATION:
        case TIFFTAG_PHOTOMETRIC:
        case TIFFTAG_PLANARCONFIG:
        case TIFFTAG_PREDICTOR:
        case TIFFTAG_RESOLUTIONUNIT:
        case TIFFTAG_SAMPLEFORMAT:
        case TIFFTAG_SAMPLESPERPIXEL:
        case TIFFTAG_THRESHHOLDING:
        case TIFFTAG_YCBCRPOSITIONING:
            return TIFFSetField(tiff, tag, static_cast<uint16_t>(value));
        default:
            lastError = "unsupported tagID";
            return 0;
    }
}

int ltiff_writer_set_int2(void* handle, int tag, int first, int second) {
    lastError.clear();
    if (!validHandle(handle)) {
        return 0;
    }
    if (tag != TIFFTAG_PAGENUMBER) {
        lastError = "unsupported tagID";
        return 0;
    }
    return TIFFSetField(
        static_cast<TIFF*>(handle),
        tag,
        static_cast<uint16_t>(first),
        static_cast<uint16_t>(second));
}

int ltiff_writer_set_string(void* handle, int tag, const char* value) {
    lastError.clear();
    if (!validHandle(handle) || !value) {
        return 0;
    }
    if (!isStringTag(tag)) {
        lastError = "unsupported tagID";
        return 0;
    }
    return TIFFSetField(static_cast<TIFF*>(handle), tag, value);
}

int ltiff_writer_write_strip(void* handle, int index, const void* buffer, int size) {
    lastError.clear();
    if (!validHandle(handle) || !buffer || size < 0) {
        return -1;
    }
    return static_cast<int>(
        TIFFWriteEncodedStrip(static_cast<TIFF*>(handle), index, const_cast<void*>(buffer), size));
}

int ltiff_writer_write_tile(void* handle, int index, const void* buffer, int size) {
    lastError.clear();
    if (!validHandle(handle) || !buffer || size < 0) {
        return -1;
    }
    return static_cast<int>(
        TIFFWriteEncodedTile(static_cast<TIFF*>(handle), index, const_cast<void*>(buffer), size));
}

} // extern "C"
