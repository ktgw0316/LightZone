/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2026 LightZone Project */

#include <algorithm>
#include <cstdio>
#include <cstring>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>

extern "C" {
#include <jerror.h>
#include <jpeglib.h>
}

#include "LC_dest_mgr.h"
#include "LC_JPEGWriter.h"

namespace {

thread_local std::string lastError;

struct LCJPEGWriter {
    jpeg_compress_struct cinfo{};
    jpeg_error_mgr errorManager{};
    LC_dest_mgr* destination = nullptr;
    bool started = false;

    LCJPEGWriter() {
        cinfo.err = jpeg_std_error(&errorManager);
        errorManager.error_exit = &errorExit;
        jpeg_create_compress(&cinfo);
    }

    ~LCJPEGWriter() {
        if (started) {
            try {
                jpeg_abort_compress(&cinfo);
            } catch (...) {
            }
        }
        try {
            jpeg_destroy_compress(&cinfo);
        } catch (...) {
        }
        delete destination;
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

extern "C" {

void* lcjpeg_writer_create(
    LCJPEGWriterWriteCallback write,
    int bufferSize,
    int width,
    int height,
    int components,
    int colorSpace,
    int quality) {
    lastError.clear();
    if (!write || bufferSize < 1 || width < 1 || height < 1 || components < 1) {
        setError("Invalid JPEG writer arguments");
        return nullptr;
    }
    try {
        std::unique_ptr<LCJPEGWriter> writer(new LCJPEGWriter());
        writer->destination = new LC_dest_mgr(write, bufferSize);
        writer->cinfo.dest = writer->destination;
        writer->cinfo.image_width = static_cast<JDIMENSION>(width);
        writer->cinfo.image_height = static_cast<JDIMENSION>(height);
        writer->cinfo.input_components = components;
        writer->cinfo.in_color_space = static_cast<J_COLOR_SPACE>(colorSpace);

        jpeg_set_defaults(&writer->cinfo);
        if (components >= 3) {
            for (int i = 0; i < 3; ++i) {
                writer->cinfo.comp_info[i].h_samp_factor = 1;
                writer->cinfo.comp_info[i].v_samp_factor = 1;
            }
        }
        jpeg_set_quality(&writer->cinfo, quality, TRUE);
        jpeg_start_compress(&writer->cinfo, TRUE);
        writer->started = true;
        return writer.release();
    } catch (const std::exception& error) {
        setError(error);
    } catch (...) {
        setError("Unknown JPEG writer error");
    }
    return nullptr;
}

int lcjpeg_writer_destroy(void* handle) {
    lastError.clear();
    auto* writer = static_cast<LCJPEGWriter*>(handle);
    if (!writer) {
        return 0;
    }
    try {
        if (writer->started) {
            writer->started = false;
            jpeg_finish_compress(&writer->cinfo);
        }
    } catch (const std::exception& error) {
        setError(error);
    } catch (...) {
        setError("Unknown JPEG finish error");
    }
    delete writer;
    return lastError.empty() ? 0 : -1;
}

int lcjpeg_writer_write_segment(
    void* handle,
    int marker,
    const unsigned char* buffer,
    int length) {
    lastError.clear();
    auto* writer = static_cast<LCJPEGWriter*>(handle);
    if (!writer || (!buffer && length != 0) || length < 0) {
        setError("Invalid JPEG segment arguments");
        return -1;
    }
    try {
        jpeg_write_marker(
            &writer->cinfo, marker, const_cast<JOCTET*>(buffer), length);
        return 0;
    } catch (const std::exception& error) {
        setError(error);
    } catch (...) {
        setError("Unknown JPEG marker error");
    }
    return -1;
}

int lcjpeg_writer_write_scanlines(
    void* handle,
    const unsigned char* buffer,
    int bufferSize,
    int offset,
    int numLines,
    int lineStride) {
    lastError.clear();
    auto* writer = static_cast<LCJPEGWriter*>(handle);
    if (!writer || !buffer || bufferSize < 0 || offset < 0 ||
        numLines < 0 || lineStride < 0) {
        setError("Invalid JPEG scanline arguments");
        return -1;
    }
    const size_t rowSize =
        static_cast<size_t>(writer->cinfo.image_width) *
        writer->cinfo.input_components;
    if (numLines > 0 &&
        (static_cast<size_t>(offset) +
             static_cast<size_t>(numLines - 1) * static_cast<size_t>(lineStride) +
             rowSize >
         static_cast<size_t>(bufferSize))) {
        setError("JPEG scanline buffer is too small");
        return -1;
    }
    try {
        std::vector<JSAMPROW> rows(static_cast<size_t>(numLines));
        for (int i = 0; i < numLines; ++i) {
            rows[static_cast<size_t>(i)] =
                const_cast<JSAMPROW>(
                    buffer + static_cast<size_t>(offset) +
                    static_cast<size_t>(i) * static_cast<size_t>(lineStride));
        }
        int totalLinesWritten = 0;
        while (totalLinesWritten < numLines) {
            const int linesWritten = jpeg_write_scanlines(
                &writer->cinfo,
                &rows[static_cast<size_t>(totalLinesWritten)],
                numLines - totalLinesWritten);
            if (linesWritten < 1) {
                break;
            }
            totalLinesWritten += linesWritten;
        }
        return totalLinesWritten;
    } catch (const std::exception& error) {
        setError(error);
    } catch (...) {
        setError("Unknown JPEG scanline error");
    }
    return -1;
}

int lcjpeg_writer_last_error(unsigned char* buffer, int capacity) {
    if (!buffer || capacity < 1) {
        return -1;
    }
    const size_t length = std::min(lastError.size(), static_cast<size_t>(capacity - 1));
    std::memcpy(buffer, lastError.data(), length);
    buffer[length] = '\0';
    return static_cast<int>(length);
}

} // extern "C"
