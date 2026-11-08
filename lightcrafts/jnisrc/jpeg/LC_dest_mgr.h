/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef LC_dest_mgr_H
#define LC_dest_mgr_H

#include <vector>

extern "C" {
#   include <jpeglib.h>
}

/**
 * An LC_dest_mgr is-a jpeg_destination_mgr that adds stuff for writing image
 * data to the Java side.
 */
struct LC_dest_mgr : jpeg_destination_mgr {
    using WriteCallback = int (*)(unsigned char*, int);

    WriteCallback m_write;
    std::vector<JOCTET> m_buffer;

    LC_dest_mgr(WriteCallback, int bufSize);

    void reset();
    void write(int byteCount);
};

#endif  /* LC_dest_mgr_H */
/* vim:set et sw=4 ts=4: */
