/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2026 LightZone Project */

#include <cstdio>
#include <stdexcept>

extern "C" {
#include <jpeglib.h>
}

#include "LC_dest_mgr.h"

namespace {

void initialize(j_compress_ptr cinfo) {
    auto* destination = static_cast<LC_dest_mgr*>(cinfo->dest);
    destination->reset();
}

boolean flushBuffer(j_compress_ptr cinfo) {
    auto* destination = static_cast<LC_dest_mgr*>(cinfo->dest);
    destination->write(static_cast<int>(destination->m_buffer.size()));
    destination->reset();
    return TRUE;
}

void finish(j_compress_ptr cinfo) {
    auto* destination = static_cast<LC_dest_mgr*>(cinfo->dest);
    const int written =
        static_cast<int>(destination->m_buffer.size()) -
        static_cast<int>(destination->free_in_buffer);
    if (written > 0) {
        destination->write(written);
    }
}

} // namespace

LC_dest_mgr::LC_dest_mgr(WriteCallback callback, int bufSize)
    : m_write(callback), m_buffer(static_cast<size_t>(bufSize)) {
    init_destination = &initialize;
    empty_output_buffer = &flushBuffer;
    term_destination = &finish;
    reset();
}

void LC_dest_mgr::reset() {
    next_output_byte = m_buffer.data();
    free_in_buffer = m_buffer.size();
}

void LC_dest_mgr::write(int byteCount) {
    const int bytesWritten =
        m_write(reinterpret_cast<unsigned char*>(m_buffer.data()), byteCount);
    if (bytesWritten != byteCount) {
        throw std::runtime_error("JPEG output callback failed");
    }
}
