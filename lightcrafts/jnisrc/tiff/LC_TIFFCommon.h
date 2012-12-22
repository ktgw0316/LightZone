/* Copyright (C) 2005-2011 Fabio Riccardi */

#ifndef LC_TIFFCommon_H
#define LC_TIFFCommon_H

#include <tiffio.h>

/**
 *  Open a TIFF file.  This function correctly handles path names containing
 *  Unicode UTF-8) on all platforms.
 */
TIFF* LC_TIFFOpen( char const *filename, char const *mode );

#endif /* LC_TIFFCommon_H */
/* vim:set et sw=4 ts=4: */
