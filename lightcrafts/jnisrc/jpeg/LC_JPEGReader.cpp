/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2026 LightZone Project */

#include <algorithm>
#include <cstring>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>

#include "LC_JPEGReader.h"

extern "C" {
#include <jerror.h>
#include <jpeglib.h>
}

namespace {

thread_local std::string lastError;

struct PanamaErrorManager {
    jpeg_error_mgr manager;
};

struct PanamaSource : jpeg_source_mgr {
    LCJPEGReaderReadCallback read;
    std::vector<JOCTET> buffer;
    bool startOfFile;

    PanamaSource(LCJPEGReaderReadCallback callback, int bufferSize)
        : read(callback), buffer(static_cast<size_t>(bufferSize)), startOfFile(true) {
        init_source = &initialize;
        fill_input_buffer = &fill;
        skip_input_data = &skip;
        resync_to_restart = &jpeg_resync_to_restart;
        term_source = &terminate;
        bytes_in_buffer = 0;
        next_input_byte = nullptr;
    }

    static void initialize(j_decompress_ptr cinfo) {
        auto* source = static_cast<PanamaSource*>(cinfo->src);
        source->startOfFile = true;
    }

    static boolean fill(j_decompress_ptr cinfo) {
        auto* source = static_cast<PanamaSource*>(cinfo->src);
        const int bytesRead = source->read(
            reinterpret_cast<unsigned char*>(source->buffer.data()),
            static_cast<int>(source->buffer.size()));
        if (bytesRead > static_cast<int>(source->buffer.size())) {
            ERREXIT(cinfo, JERR_BAD_LENGTH);
            return FALSE;
        }
        if (bytesRead <= 0) {
            if (source->startOfFile) {
                ERREXIT(cinfo, JERR_INPUT_EMPTY);
                return FALSE;
            }
            WARNMS(cinfo, JWRN_JPEG_EOF);
            source->buffer[0] = 0xFF;
            source->buffer[1] = JPEG_EOI;
            source->bytes_in_buffer = 2;
        } else {
            source->bytes_in_buffer = static_cast<size_t>(bytesRead);
        }
        source->next_input_byte = source->buffer.data();
        source->startOfFile = false;
        return TRUE;
    }

    static void skip(j_decompress_ptr cinfo, long bytesToSkip) {
        if (bytesToSkip <= 0) {
            return;
        }
        auto* source = static_cast<PanamaSource*>(cinfo->src);
        size_t remaining = static_cast<size_t>(bytesToSkip);
        while (remaining > source->bytes_in_buffer) {
            remaining -= source->bytes_in_buffer;
            if (!fill(cinfo)) {
                return;
            }
        }
        source->next_input_byte += remaining;
        source->bytes_in_buffer -= remaining;
    }

    static void terminate(j_decompress_ptr) {
    }
};

struct LCJPEGReader {
    jpeg_decompress_struct cinfo;
    PanamaErrorManager errorManager;
    PanamaSource source;
    bool started;

    LCJPEGReader(LCJPEGReaderReadCallback callback, int bufferSize)
        : cinfo{}, errorManager{}, source(callback, bufferSize), started(false) {
        cinfo.err = jpeg_std_error(&errorManager.manager);
        errorManager.manager.error_exit = &errorExit;
        jpeg_create_decompress(&cinfo);
        cinfo.src = &source;
    }

    ~LCJPEGReader() {
        if (started) {
            try {
                started = false;
                jpeg_finish_decompress(&cinfo);
            } catch (...) {
            }
        }
        try {
            jpeg_destroy_decompress(&cinfo);
        } catch (const std::exception& error) {
            lastError = error.what();
        } catch (...) {
            lastError = "Unknown JPEG destroy error";
        }
    }

    void finishDecompress() {
        if (started) {
            started = false;
            jpeg_finish_decompress(&cinfo);
        }
    }

    void startDecompress(int maxWidth, int maxHeight) {
        jpeg_read_header(&cinfo, TRUE);
        if (maxWidth > 0 && maxHeight > 0) {
            jpeg_calc_output_dimensions(&cinfo);
            int scale = std::max(
                cinfo.output_width / maxWidth,
                cinfo.output_height / maxHeight);
            if (scale >= 8) {
                scale = 8;
            } else if (scale >= 4) {
                scale = 4;
            } else if (scale >= 2) {
                scale = 2;
            } else {
                scale = 1;
            }
            if (scale != 1) {
                cinfo.scale_num = 1;
                cinfo.scale_denom = scale;
                jpeg_calc_output_dimensions(&cinfo);
            }
        }
        jpeg_start_decompress(&cinfo);
        started = true;
    }

    static void errorExit(j_common_ptr cinfo) {
        char message[JMSG_LENGTH_MAX];
        (*cinfo->err->format_message)(cinfo, message);
        throw std::runtime_error(message);
    }
};

void setError(const char* message) {
    lastError = message;
}

void setError(const std::exception& error) {
    setError(error.what());
}

} // namespace

extern "C" void* lcjpeg_reader_create(
    LCJPEGReaderReadCallback read,
    int bufferSize,
    int maxWidth,
    int maxHeight) {
    lastError.clear();
    if (!read || bufferSize < 2) {
        setError("Invalid JPEG reader arguments");
        return nullptr;
    }
    try {
        std::unique_ptr<LCJPEGReader> reader(new LCJPEGReader(read, bufferSize));
        reader->startDecompress(maxWidth, maxHeight);
        return reader.release();
    } catch (const std::exception& error) {
        setError(error);
    } catch (...) {
        setError("Unknown JPEG reader error");
    }
    return nullptr;
}

extern "C" int lcjpeg_reader_destroy(void* handle) {
    lastError.clear();
    auto* reader = static_cast<LCJPEGReader*>(handle);
    if (!reader) {
        return 0;
    }
    try {
        reader->finishDecompress();
    } catch (const std::exception& error) {
        setError(error);
    } catch (...) {
        setError("Unknown JPEG finish error");
    }
    delete reader;
    return lastError.empty() ? 0 : -1;
}

extern "C" int lcjpeg_reader_width(void* handle) {
    return static_cast<LCJPEGReader*>(handle)->cinfo.output_width;
}

extern "C" int lcjpeg_reader_height(void* handle) {
    return static_cast<LCJPEGReader*>(handle)->cinfo.output_height;
}

extern "C" int lcjpeg_reader_components(void* handle) {
    return static_cast<LCJPEGReader*>(handle)->cinfo.output_components;
}

extern "C" int lcjpeg_reader_color_space(void* handle) {
    return static_cast<LCJPEGReader*>(handle)->cinfo.out_color_space;
}

extern "C" int lcjpeg_reader_read_scanlines(
    void* handle,
    unsigned char* buffer,
    int numLines) {
    lastError.clear();
    auto* reader = static_cast<LCJPEGReader*>(handle);
    if (!reader || !buffer || numLines < 0) {
        setError("Invalid JPEG scanline arguments");
        return -1;
    }
    try {
        const size_t rowSize =
            static_cast<size_t>(reader->cinfo.output_width) *
            reader->cinfo.output_components;
        std::vector<JSAMPROW> rows(static_cast<size_t>(numLines));
        for (int i = 0; i < numLines; ++i) {
            rows[static_cast<size_t>(i)] = buffer + static_cast<size_t>(i) * rowSize;
        }
        int totalLinesRead = 0;
        while (totalLinesRead < numLines) {
            const int linesRead = jpeg_read_scanlines(
                &reader->cinfo,
                &rows[static_cast<size_t>(totalLinesRead)],
                numLines - totalLinesRead);
            if (linesRead < 1) {
                reader->cinfo.err->msg_code = JERR_BAD_LENGTH;
                break;
            }
            totalLinesRead += linesRead;
        }
        return totalLinesRead;
    } catch (const std::exception& error) {
        setError(error);
    } catch (...) {
        setError("Unknown JPEG scanline error");
    }
    return -1;
}

extern "C" int lcjpeg_reader_last_error(unsigned char* buffer, int capacity) {
    if (!buffer || capacity < 1) {
        return -1;
    }
    const size_t length = std::min(lastError.size(), static_cast<size_t>(capacity - 1));
    std::memcpy(buffer, lastError.data(), length);
    buffer[length] = '\0';
    return static_cast<int>(length);
}
