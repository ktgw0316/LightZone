/*
 * Copyright (c) 1988-1997 Sam Leffler
 * Copyright (c) 1991-1997 Silicon Graphics, Inc.
 *
 *  Revised:  2/18/01 BAR -- added syntax for extracting single images from
 *                          multi-image TIFF files.
 *
 *    New syntax is:  sourceFileName,image#
 *
 * image# ranges from 0..<n-1> where n is the # of images in the file.
 * There may be no white space between the comma and the filename or
 * image number.
 *
 *    Example:   tiffcp source.tif,1 destination.tif
 *
 * Copies the 2nd image in source.tif to the destination.
 *
 *****
 * Permission to use, copy, modify, distribute, and sell this software and
 * its documentation for any purpose is hereby granted without fee, provided
 * that (i) the above copyright notices and this permission notice appear in
 * all copies of the software and related documentation, and (ii) the names of
 * Sam Leffler and Silicon Graphics may not be used in any advertising or
 * publicity relating to the software without the specific, prior written
 * permission of Sam Leffler and Silicon Graphics.
 *
 * THE SOFTWARE IS PROVIDED "AS-IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 *
 * IN NO EVENT SHALL SAM LEFFLER OR SILICON GRAPHICS BE LIABLE FOR
 * ANY SPECIAL, INCIDENTAL, INDIRECT OR CONSEQUENTIAL DAMAGES OF ANY KIND,
 * OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
 * WHETHER OR NOT ADVISED OF THE POSSIBILITY OF DAMAGE, AND ON ANY THEORY OF
 * LIABILITY, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE
 * OF THIS SOFTWARE.
 */

#include "tif_config.h"

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <assert.h>
#include <ctype.h>

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#include "tiffio.h"

#if defined(VMS)
#define unlink delete
#endif

#define streq(a, b) (strcmp(a, b) == 0)
#define strneq(a, b, n) (strncmp(a, b, n) == 0)

#define TRUE 1
#define FALSE 0

static int outtiled = -1;
static uint32_t tilewidth;
static uint32_t tilelength;

static uint16_t config = (uint16_t)-1;
static uint16_t compression = (uint16_t)-1;
static uint16_t predictor = (uint16_t)-1;
static uint16_t fillorder;
static uint16_t orientation;
static uint32_t rowsperstrip;
static uint32_t g3opts;
static int ignore = FALSE; /* if true, ignore read errors */
static uint32_t defg3opts = (uint32_t)-1;
static int quality = 75; /* JPEG quality */
static int jpegcolormode = JPEGCOLORMODE_RGB;
static uint16_t defcompression = (uint16_t)-1;
static uint16_t defpredictor = (uint16_t)-1;

static int processCompressOptions(char *);
static void usage(void);

static char comma = ','; /* (default) comma separator character */
static TIFF *bias = NULL;
static int pageNum = 0;

static int nextSrcImage(TIFF *tif, char **imageSpec)
/*
  seek to the next image specified in *imageSpec
  returns 1 if success, 0 if no more images to process
  *imageSpec=NULL if subsequent images should be processed in sequence
*/
{
  if (**imageSpec == comma) { /* if not @comma, we've done all images */
    char *start = *imageSpec + 1;
    tdir_t nextImage = (tdir_t)strtol(start, imageSpec, 0);
    if (start == *imageSpec)
      nextImage = TIFFCurrentDirectory(tif);
    if (**imageSpec) {
      if (**imageSpec == comma) {
        /* a trailing comma denotes remaining images in sequence */
        if ((*imageSpec)[1] == '\0')
          *imageSpec = NULL;
      } else {
        fprintf(stderr, "Expected a %c separated image # list after %s\n",
                comma, TIFFFileName(tif));
        exit(-4); /* syntax error */
      }
    }
    if (TIFFSetDirectory(tif, nextImage))
      return 1;
    fprintf(stderr, "%s%c%d not found!\n", TIFFFileName(tif), comma,
            (int)nextImage);
  }
  return 0;
}

static TIFF *openSrcImage(char **imageSpec)
/*
  imageSpec points to a pointer to a filename followed by optional ,image#'s
  Open the TIFF file and assign *imageSpec to either NULL if there are
  no images specified, or a pointer to the next image number text
*/
{
  TIFF *tif;
  char *fn = *imageSpec;
  *imageSpec = strchr(fn, comma);
  if (*imageSpec) { /* there is at least one image number specifier */
    **imageSpec = '\0';
    tif = TIFFOpen(fn, "r");
    /* but, ignore any single trailing comma */
    if (!(*imageSpec)[1]) {
      *imageSpec = NULL;
      return tif;
    }
    if (tif) {
      **imageSpec = comma; /* replace the comma */
      if (!nextSrcImage(tif, imageSpec)) {
        TIFFClose(tif);
        tif = NULL;
      }
    }
  } else
    tif = TIFFOpen(fn, "r");
  return tif;
}

#define CopyField(tag, v)                                                      \
  if (TIFFGetField(in, tag, &v))                                               \
  TIFFSetField(out, tag, v)
#define CopyField2(tag, v1, v2)                                                \
  if (TIFFGetField(in, tag, &v1, &v2))                                         \
  TIFFSetField(out, tag, v1, v2)
#define CopyField3(tag, v1, v2, v3)                                            \
  if (TIFFGetField(in, tag, &v1, &v2, &v3))                                    \
  TIFFSetField(out, tag, v1, v2, v3)
#define CopyField4(tag, v1, v2, v3, v4)                                        \
  if (TIFFGetField(in, tag, &v1, &v2, &v3, &v4))                               \
  TIFFSetField(out, tag, v1, v2, v3, v4)

static void cpTag(TIFF *in, TIFF *out, uint16_t tag, uint16_t count,
                  TIFFDataType type) {
  switch (type) {
  case TIFF_SHORT:
    if (count == 1) {
      uint16_t shortv;
      CopyField(tag, shortv);
    } else if (count == 2) {
      uint16_t shortv1, shortv2;
      CopyField2(tag, shortv1, shortv2);
    } else if (count == 4) {
      uint16_t *tr, *tg, *tb, *ta;
      CopyField4(tag, tr, tg, tb, ta);
    } else if (count == (uint16_t)-1) {
      uint16_t shortv1;
      uint16_t *shortav;
      CopyField2(tag, shortv1, shortav);
    }
    break;
  case TIFF_LONG: {
    uint32_t longv;
    CopyField(tag, longv);
  } break;
  case TIFF_RATIONAL:
    if (count == 1) {
      float floatv;
      CopyField(tag, floatv);
    } else if (count == (uint16_t)-1) {
      float *floatav;
      CopyField(tag, floatav);
    }
    break;
  case TIFF_ASCII: {
    char *stringv;
    CopyField(tag, stringv);
  } break;
  case TIFF_DOUBLE:
    if (count == 1) {
      double doublev;
      CopyField(tag, doublev);
    } else if (count == (uint16_t)-1) {
      double *doubleav;
      CopyField(tag, doubleav);
    }
    break;
  default:
    TIFFError(TIFFFileName(in),
              "Data type %d is not supported, tag %d skipped.", tag, type);
  }
}

static struct cpTag {
  uint16_t tag;
  uint16_t count;
  TIFFDataType type;
} tags[] = {
    {TIFFTAG_SUBFILETYPE, 1, TIFF_LONG},
    {TIFFTAG_THRESHHOLDING, 1, TIFF_SHORT},
    {TIFFTAG_DOCUMENTNAME, 1, TIFF_ASCII},
    {TIFFTAG_IMAGEDESCRIPTION, 1, TIFF_ASCII},
    {TIFFTAG_MAKE, 1, TIFF_ASCII},
    {TIFFTAG_MODEL, 1, TIFF_ASCII},
    {TIFFTAG_MINSAMPLEVALUE, 1, TIFF_SHORT},
    {TIFFTAG_MAXSAMPLEVALUE, 1, TIFF_SHORT},
    {TIFFTAG_XRESOLUTION, 1, TIFF_RATIONAL},
    {TIFFTAG_YRESOLUTION, 1, TIFF_RATIONAL},
    {TIFFTAG_PAGENAME, 1, TIFF_ASCII},
    {TIFFTAG_XPOSITION, 1, TIFF_RATIONAL},
    {TIFFTAG_YPOSITION, 1, TIFF_RATIONAL},
    {TIFFTAG_RESOLUTIONUNIT, 1, TIFF_SHORT},
    {TIFFTAG_SOFTWARE, 1, TIFF_ASCII},
    {TIFFTAG_DATETIME, 1, TIFF_ASCII},
    {TIFFTAG_ARTIST, 1, TIFF_ASCII},
    {TIFFTAG_HOSTCOMPUTER, 1, TIFF_ASCII},
    {TIFFTAG_WHITEPOINT, (uint16_t)-1, TIFF_RATIONAL},
    {TIFFTAG_PRIMARYCHROMATICITIES, (uint16_t)-1, TIFF_RATIONAL},
    {TIFFTAG_HALFTONEHINTS, 2, TIFF_SHORT},
    {TIFFTAG_INKSET, 1, TIFF_SHORT},
    {TIFFTAG_DOTRANGE, 2, TIFF_SHORT},
    {TIFFTAG_TARGETPRINTER, 1, TIFF_ASCII},
    {TIFFTAG_SAMPLEFORMAT, 1, TIFF_SHORT},
    {TIFFTAG_YCBCRCOEFFICIENTS, (uint16_t)-1, TIFF_RATIONAL},
    {TIFFTAG_YCBCRSUBSAMPLING, 2, TIFF_SHORT},
    {TIFFTAG_YCBCRPOSITIONING, 1, TIFF_SHORT},
    {TIFFTAG_REFERENCEBLACKWHITE, (uint16_t)-1, TIFF_RATIONAL},
    {TIFFTAG_EXTRASAMPLES, (uint16_t)-1, TIFF_SHORT},
    {TIFFTAG_SMINSAMPLEVALUE, 1, TIFF_DOUBLE},
    {TIFFTAG_SMAXSAMPLEVALUE, 1, TIFF_DOUBLE},
    {TIFFTAG_STONITS, 1, TIFF_DOUBLE},
};
#define NTAGS (sizeof(tags) / sizeof(tags[0]))

#define CopyTag(tag, count, type) cpTag(in, out, tag, count, type)

typedef int (*copyFunc)(TIFF *in, TIFF *out, uint32_t l, uint32_t w,
                        uint16_t samplesperpixel);
static copyFunc pickCopyFunc(TIFF *, TIFF *, uint16_t, uint16_t);

int tiffcp(TIFF *in, TIFF *out) {
  uint16_t bitspersample, samplesperpixel;
  copyFunc cf;
  uint32_t width, length;
  struct cpTag *p;

  CopyField(TIFFTAG_IMAGEWIDTH, width);
  CopyField(TIFFTAG_IMAGELENGTH, length);
  CopyField(TIFFTAG_BITSPERSAMPLE, bitspersample);
  CopyField(TIFFTAG_SAMPLESPERPIXEL, samplesperpixel);
  if (compression != (uint16_t)-1)
    TIFFSetField(out, TIFFTAG_COMPRESSION, compression);
  else
    CopyField(TIFFTAG_COMPRESSION, compression);
  if (compression == COMPRESSION_JPEG) {
    uint16_t input_compression, input_photometric;

    if (TIFFGetField(in, TIFFTAG_COMPRESSION, &input_compression) &&
        input_compression == COMPRESSION_JPEG) {
      TIFFSetField(in, TIFFTAG_JPEGCOLORMODE, JPEGCOLORMODE_RGB);
    }
    if (TIFFGetField(in, TIFFTAG_PHOTOMETRIC, &input_photometric)) {
      if (input_photometric == PHOTOMETRIC_RGB) {
        if (jpegcolormode == JPEGCOLORMODE_RGB)
          TIFFSetField(out, TIFFTAG_PHOTOMETRIC, PHOTOMETRIC_YCBCR);
        else
          TIFFSetField(out, TIFFTAG_PHOTOMETRIC, PHOTOMETRIC_RGB);
      } else
        TIFFSetField(out, TIFFTAG_PHOTOMETRIC, input_photometric);
    }
  } else if (compression == COMPRESSION_SGILOG ||
             compression == COMPRESSION_SGILOG24)
    TIFFSetField(out, TIFFTAG_PHOTOMETRIC,
                 samplesperpixel == 1 ? PHOTOMETRIC_LOGL : PHOTOMETRIC_LOGLUV);
  else
    CopyTag(TIFFTAG_PHOTOMETRIC, 1, TIFF_SHORT);
  if (fillorder != 0)
    TIFFSetField(out, TIFFTAG_FILLORDER, fillorder);
  else
    CopyTag(TIFFTAG_FILLORDER, 1, TIFF_SHORT);
  /*
   * Will copy `Orientation' tag from input image
   */
  TIFFGetFieldDefaulted(in, TIFFTAG_ORIENTATION, &orientation);
  switch (orientation) {
  case ORIENTATION_BOTRIGHT:
  case ORIENTATION_RIGHTBOT: /* XXX */
    TIFFWarning(TIFFFileName(in), "using bottom-left orientation");
    orientation = ORIENTATION_BOTLEFT;
  /* fall thru... */
  case ORIENTATION_LEFTBOT: /* XXX */
  case ORIENTATION_BOTLEFT:
    break;
  case ORIENTATION_TOPRIGHT:
  case ORIENTATION_RIGHTTOP: /* XXX */
  default:
    TIFFWarning(TIFFFileName(in), "using top-left orientation");
    orientation = ORIENTATION_TOPLEFT;
  /* fall thru... */
  case ORIENTATION_LEFTTOP: /* XXX */
  case ORIENTATION_TOPLEFT:
    break;
  }
  TIFFSetField(out, TIFFTAG_ORIENTATION, orientation);
  /*
   * Choose tiles/strip for the output image according to
   * the command line arguments (-tiles, -strips) and the
   * structure of the input image.
   */
  if (outtiled == -1)
    outtiled = TIFFIsTiled(in);
  if (outtiled) {
    /*
     * Setup output file's tile width&height.  If either
     * is not specified, use either the value from the
     * input image or, if nothing is defined, use the
     * library default.
     */
    if (tilewidth == (uint32_t)-1)
      TIFFGetField(in, TIFFTAG_TILEWIDTH, &tilewidth);
    if (tilelength == (uint32_t)-1)
      TIFFGetField(in, TIFFTAG_TILELENGTH, &tilelength);
    TIFFDefaultTileSize(out, &tilewidth, &tilelength);
    TIFFSetField(out, TIFFTAG_TILEWIDTH, tilewidth);
    TIFFSetField(out, TIFFTAG_TILELENGTH, tilelength);
  } else {
    /*
     * RowsPerStrip is left unspecified: use either the
     * value from the input image or, if nothing is defined,
     * use the library default.
     */
    if (rowsperstrip == (uint32_t)0) {
      if (!TIFFGetField(in, TIFFTAG_ROWSPERSTRIP, &rowsperstrip))
        rowsperstrip = TIFFDefaultStripSize(out, rowsperstrip);
    } else if (rowsperstrip == (uint32_t)-1)
      rowsperstrip = length;
    TIFFSetField(out, TIFFTAG_ROWSPERSTRIP, rowsperstrip);
  }
  if (config != (uint16_t)-1)
    TIFFSetField(out, TIFFTAG_PLANARCONFIG, config);
  else
    CopyField(TIFFTAG_PLANARCONFIG, config);
  if (samplesperpixel <= 4)
    CopyTag(TIFFTAG_TRANSFERFUNCTION, 4, TIFF_SHORT);
  CopyTag(TIFFTAG_COLORMAP, 4, TIFF_SHORT);
  /* SMinSampleValue & SMaxSampleValue */
  switch (compression) {
  case COMPRESSION_JPEG:
    TIFFSetField(out, TIFFTAG_JPEGQUALITY, quality);
    TIFFSetField(out, TIFFTAG_JPEGCOLORMODE, jpegcolormode);
    break;
  case COMPRESSION_LZW:
  case COMPRESSION_ADOBE_DEFLATE:
  case COMPRESSION_DEFLATE:
    if (predictor != (uint16_t)-1)
      TIFFSetField(out, TIFFTAG_PREDICTOR, predictor);
    else
      CopyField(TIFFTAG_PREDICTOR, predictor);
    break;
  case COMPRESSION_CCITTFAX3:
  case COMPRESSION_CCITTFAX4:
    if (compression == COMPRESSION_CCITTFAX3) {
      if (g3opts != (uint32_t)-1)
        TIFFSetField(out, TIFFTAG_GROUP3OPTIONS, g3opts);
      else
        CopyField(TIFFTAG_GROUP3OPTIONS, g3opts);
    } else
      CopyTag(TIFFTAG_GROUP4OPTIONS, 1, TIFF_LONG);
    CopyTag(TIFFTAG_BADFAXLINES, 1, TIFF_LONG);
    CopyTag(TIFFTAG_CLEANFAXDATA, 1, TIFF_LONG);
    CopyTag(TIFFTAG_CONSECUTIVEBADFAXLINES, 1, TIFF_LONG);
    CopyTag(TIFFTAG_FAXRECVPARAMS, 1, TIFF_LONG);
    CopyTag(TIFFTAG_FAXRECVTIME, 1, TIFF_LONG);
    CopyTag(TIFFTAG_FAXSUBADDRESS, 1, TIFF_ASCII);
    break;
  }
  {
    uint32_t len32;
    void **data;
    if (TIFFGetField(in, TIFFTAG_ICCPROFILE, &len32, &data))
      TIFFSetField(out, TIFFTAG_ICCPROFILE, len32, data);
  }
  {
    uint16_t ninks;
    const char *inknames;
    if (TIFFGetField(in, TIFFTAG_NUMBEROFINKS, &ninks)) {
      TIFFSetField(out, TIFFTAG_NUMBEROFINKS, ninks);
      if (TIFFGetField(in, TIFFTAG_INKNAMES, &inknames)) {
        int inknameslen = strlen(inknames) + 1;
        const char *cp = inknames;
        while (ninks > 1) {
          cp = strchr(cp, '\0');
          if (cp) {
            cp++;
            inknameslen += (strlen(cp) + 1);
          }
          ninks--;
        }
        TIFFSetField(out, TIFFTAG_INKNAMES, inknameslen, inknames);
      }
    }
  }
  {
    unsigned short pg0, pg1;
    if (TIFFGetField(in, TIFFTAG_PAGENUMBER, &pg0, &pg1)) {
      if (pageNum < 0) /* only one input file */
        TIFFSetField(out, TIFFTAG_PAGENUMBER, pg0, pg1);
      else
        TIFFSetField(out, TIFFTAG_PAGENUMBER, pageNum++, 0);
    }
  }

  for (p = tags; p < &tags[NTAGS]; p++)
    CopyTag(p->tag, p->count, p->type);

  cf = pickCopyFunc(in, out, bitspersample, samplesperpixel);
  return (cf ? (*cf)(in, out, length, width, samplesperpixel) : FALSE);
}

/*
 * Copy Functions.
 */
#define DECLAREcpFunc(x)                                                       \
  static int x(TIFF *in, TIFF *out, uint32_t imagelength, uint32_t imagewidth, \
               tsample_t spp)

#define DECLAREreadFunc(x)                                                     \
  static void x(TIFF *in, uint8_t *buf, uint32_t imagelength,                  \
                uint32_t imagewidth, tsample_t spp)
typedef void (*readFunc)(TIFF *, uint8_t *, uint32_t, uint32_t, tsample_t);

#define DECLAREwriteFunc(x)                                                    \
  static int x(TIFF *out, uint8_t *buf, uint32_t imagelength,                  \
               uint32_t imagewidth, tsample_t spp)
typedef int (*writeFunc)(TIFF *, uint8_t *, uint32_t, uint32_t, tsample_t);

/*
 * Contig -> contig by scanline for rows/strip change.
 */
DECLAREcpFunc(cpContig2ContigByRow) {
  tdata_t buf = _TIFFmalloc(TIFFScanlineSize(in));
  uint32_t row;

  (void)imagewidth;
  (void)spp;
  for (row = 0; row < imagelength; row++) {
    if (TIFFReadScanline(in, buf, row, 0) < 0 && !ignore)
      goto done;
    if (TIFFWriteScanline(out, buf, row, 0) < 0)
      goto bad;
  }
done:
  _TIFFfree(buf);
  return (TRUE);
bad:
  _TIFFfree(buf);
  return (FALSE);
}

typedef void biasFn(void *image, void *bias, uint32_t pixels);

#define subtract(bits)                                                         \
  static void subtract##bits(void *i, void *b, uint32_t pixels) {              \
    uint##bits *image = i;                                                     \
    uint##bits *bias = b;                                                      \
    while (pixels--) {                                                         \
      *image = *image > *bias ? *image - *bias : 0;                            \
      image++, bias++;                                                         \
    }                                                                          \
  }

subtract(8) subtract(16) subtract(32)

    static biasFn *lineSubtractFn(unsigned bits) {
  switch (bits) {
  case 8:
    return subtract8;
  case 16:
    return subtract16;
  case 32:
    return subtract32;
  }
  return NULL;
}

/*
 * Contig -> contig by scanline while subtracting a bias image.
 */
DECLAREcpFunc(cpBiasedContig2Contig) {
  if (spp == 1) {
    tsize_t biasSize = TIFFScanlineSize(bias);
    tsize_t bufSize = TIFFScanlineSize(in);
    tdata_t buf, biasBuf;
    uint32_t biasWidth = 0, biasLength = 0;
    TIFFGetField(bias, TIFFTAG_IMAGEWIDTH, &biasWidth);
    TIFFGetField(bias, TIFFTAG_IMAGELENGTH, &biasLength);
    if (biasSize == bufSize && imagelength == biasLength &&
        imagewidth == biasWidth) {
      uint16_t sampleBits = 0;
      biasFn *subtractLine;
      TIFFGetField(in, TIFFTAG_BITSPERSAMPLE, &sampleBits);
      subtractLine = lineSubtractFn(sampleBits);
      if (subtractLine) {
        uint32_t row;
        buf = _TIFFmalloc(bufSize);
        biasBuf = _TIFFmalloc(bufSize);
        for (row = 0; row < imagelength; row++) {
          if (TIFFReadScanline(in, buf, row, 0) < 0 && !ignore)
            break;
          if (TIFFReadScanline(bias, biasBuf, row, 0) < 0 && !ignore)
            break;
          subtractLine(buf, biasBuf, imagewidth);
          if (TIFFWriteScanline(out, buf, row, 0) < 0) {
            _TIFFfree(buf);
            _TIFFfree(biasBuf);
            return FALSE;
          }
        }
        _TIFFfree(buf);
        _TIFFfree(biasBuf);
        TIFFSetDirectory(bias, TIFFCurrentDirectory(bias)); /* rewind */
        return TRUE;

      } else {
        fprintf(stderr, "No support for biasing %d bit pixels\n", sampleBits);
        return FALSE;
      }
    }
    fprintf(stderr, "Bias image %s,%d\nis not the same size as %s,%d\n",
            TIFFFileName(bias), TIFFCurrentDirectory(bias), TIFFFileName(in),
            TIFFCurrentDirectory(in));
    return FALSE;
  } else {
    fprintf(stderr, "Can't bias %s,%d as it has >1 Sample/Pixel\n",
            TIFFFileName(in), TIFFCurrentDirectory(in));
    return FALSE;
  }
}

/*
 * Strip -> strip for change in encoding.
 */
DECLAREcpFunc(cpDecodedStrips) {
  tsize_t stripsize = TIFFStripSize(in);
  tdata_t buf = _TIFFmalloc(stripsize);

  (void)imagewidth;
  (void)spp;
  if (buf) {
    tstrip_t s, ns = TIFFNumberOfStrips(in);
    uint32_t row = 0;
    for (s = 0; s < ns; s++) {
      tsize_t cc = (row + rowsperstrip > imagelength)
                       ? TIFFVStripSize(in, imagelength - row)
                       : stripsize;
      if (TIFFReadEncodedStrip(in, s, buf, cc) < 0 && !ignore)
        break;
      if (TIFFWriteEncodedStrip(out, s, buf, cc) < 0) {
        _TIFFfree(buf);
        return (FALSE);
      }
      row += rowsperstrip;
    }
    _TIFFfree(buf);
    return (TRUE);
  }
  return (FALSE);
}

/*
 * Separate -> separate by row for rows/strip change.
 */
DECLAREcpFunc(cpSeparate2SeparateByRow) {
  tdata_t buf = _TIFFmalloc(TIFFScanlineSize(in));
  uint32_t row;
  tsample_t s;

  (void)imagewidth;
  for (s = 0; s < spp; s++) {
    for (row = 0; row < imagelength; row++) {
      if (TIFFReadScanline(in, buf, row, s) < 0 && !ignore)
        goto done;
      if (TIFFWriteScanline(out, buf, row, s) < 0)
        goto bad;
    }
  }
done:
  _TIFFfree(buf);
  return (TRUE);
bad:
  _TIFFfree(buf);
  return (FALSE);
}

/*
 * Contig -> separate by row.
 */
DECLAREcpFunc(cpContig2SeparateByRow) {
  tdata_t inbuf = _TIFFmalloc(TIFFScanlineSize(in));
  tdata_t outbuf = _TIFFmalloc(TIFFScanlineSize(out));
  register uint8_t *inp, *outp;
  register uint32_t n;
  uint32_t row;
  tsample_t s;

  /* unpack channels */
  for (s = 0; s < spp; s++) {
    for (row = 0; row < imagelength; row++) {
      if (TIFFReadScanline(in, inbuf, row, 0) < 0 && !ignore)
        goto done;
      inp = ((uint8_t *)inbuf) + s;
      outp = (uint8_t *)outbuf;
      for (n = imagewidth; n-- > 0;) {
        *outp++ = *inp;
        inp += spp;
      }
      if (TIFFWriteScanline(out, outbuf, row, s) < 0)
        goto bad;
    }
  }
done:
  if (inbuf)
    _TIFFfree(inbuf);
  if (outbuf)
    _TIFFfree(outbuf);
  return (TRUE);
bad:
  if (inbuf)
    _TIFFfree(inbuf);
  if (outbuf)
    _TIFFfree(outbuf);
  return (FALSE);
}

/*
 * Separate -> contig by row.
 */
DECLAREcpFunc(cpSeparate2ContigByRow) {
  tdata_t inbuf = _TIFFmalloc(TIFFScanlineSize(in));
  tdata_t outbuf = _TIFFmalloc(TIFFScanlineSize(out));
  register uint8_t *inp, *outp;
  register uint32_t n;
  uint32_t row;
  tsample_t s;

  for (row = 0; row < imagelength; row++) {
    /* merge channels */
    for (s = 0; s < spp; s++) {
      if (TIFFReadScanline(in, inbuf, row, s) < 0 && !ignore)
        goto done;
      inp = (uint8_t *)inbuf;
      outp = ((uint8_t *)outbuf) + s;
      for (n = imagewidth; n-- > 0;) {
        *outp = *inp++;
        outp += spp;
      }
    }
    if (TIFFWriteScanline(out, outbuf, row, 0) < 0)
      goto bad;
  }
done:
  if (inbuf)
    _TIFFfree(inbuf);
  if (outbuf)
    _TIFFfree(outbuf);
  return (TRUE);
bad:
  if (inbuf)
    _TIFFfree(inbuf);
  if (outbuf)
    _TIFFfree(outbuf);
  return (FALSE);
}

static void cpStripToTile(uint8_t *out, uint8_t *in, uint32_t rows,
                          uint32_t cols, int outskew, int inskew) {
  while (rows-- > 0) {
    uint32_t j = cols;
    while (j-- > 0)
      *out++ = *in++;
    out += outskew;
    in += inskew;
  }
}

static void cpContigBufToSeparateBuf(uint8_t *out, uint8_t *in, uint32_t rows,
                                     uint32_t cols, int outskew, int inskew,
                                     tsample_t spp, int bytes_per_sample) {
  while (rows-- > 0) {
    uint32_t j = cols;
    while (j-- > 0) {
      int n = bytes_per_sample;

      while (n--) {
        *out++ = *in++;
      }
      in += (spp - 1) * bytes_per_sample;
    }
    out += outskew;
    in += inskew;
  }
}

static void cpSeparateBufToContigBuf(uint8_t *out, uint8_t *in, uint32_t rows,
                                     uint32_t cols, int outskew, int inskew,
                                     tsample_t spp, int bytes_per_sample) {
  while (rows-- > 0) {
    uint32_t j = cols;
    while (j-- > 0) {
      int n = bytes_per_sample;

      while (n--) {
        *out++ = *in++;
      }
      out += (spp - 1) * bytes_per_sample;
    }
    out += outskew;
    in += inskew;
  }
}

static int cpImage(TIFF *in, TIFF *out, readFunc fin, writeFunc fout,
                   uint32_t imagelength, uint32_t imagewidth, tsample_t spp) {
  int status = FALSE;
  tdata_t buf = _TIFFmalloc(TIFFRasterScanlineSize(in) * imagelength);

  if (buf) {
    (*fin)(in, (uint8_t *)buf, imagelength, imagewidth, spp);
    status = (fout)(out, (uint8_t *)buf, imagelength, imagewidth, spp);
    _TIFFfree(buf);
  }
  return (status);
}

DECLAREreadFunc(readContigStripsIntoBuffer) {
  tsize_t scanlinesize = TIFFScanlineSize(in);
  uint8_t *bufp = buf;
  uint32_t row;

  (void)imagewidth;
  (void)spp;
  for (row = 0; row < imagelength; row++) {
    if (TIFFReadScanline(in, (tdata_t)bufp, row, 0) < 0 && !ignore)
      break;
    bufp += scanlinesize;
  }
}

DECLAREreadFunc(readSeparateStripsIntoBuffer) {
  tsize_t scanlinesize = TIFFScanlineSize(in);
  tdata_t scanline = _TIFFmalloc(scanlinesize);

  (void)imagewidth;
  if (scanline) {
    uint8_t *bufp = (uint8_t *)buf;
    uint32_t row;
    tsample_t s;
    for (row = 0; row < imagelength; row++) {
      /* merge channels */
      for (s = 0; s < spp; s++) {
        uint8_t *bp = bufp + s;
        tsize_t n = scanlinesize;
        uint8_t *sbuf = scanline;

        if (TIFFReadScanline(in, scanline, row, s) < 0 && !ignore)
          goto done;
        while (n-- > 0)
          *bp = *sbuf++, bp += spp;
      }
      bufp += scanlinesize * spp;
    }

  done:
    _TIFFfree(scanline);
  }
}

DECLAREreadFunc(readContigTilesIntoBuffer) {
  tdata_t tilebuf = _TIFFmalloc(TIFFTileSize(in));
  uint32_t imagew = TIFFScanlineSize(in);
  uint32_t tilew = TIFFTileRowSize(in);
  int iskew = imagew - tilew;
  uint8_t *bufp = (uint8_t *)buf;
  uint32_t tw, tl;
  uint32_t row;

  (void)spp;
  if (tilebuf == 0)
    return;
  (void)TIFFGetField(in, TIFFTAG_TILEWIDTH, &tw);
  (void)TIFFGetField(in, TIFFTAG_TILELENGTH, &tl);

  for (row = 0; row < imagelength; row += tl) {
    uint32_t nrow = (row + tl > imagelength) ? imagelength - row : tl;
    uint32_t colb = 0;
    uint32_t col;

    for (col = 0; col < imagewidth; col += tw) {
      if (TIFFReadTile(in, tilebuf, col, row, 0, 0) < 0 && !ignore)
        goto done;
      if (colb + tilew > imagew) {
        uint32_t width = imagew - colb;
        uint32_t oskew = tilew - width;
        cpStripToTile(bufp + colb, tilebuf, nrow, width, oskew + iskew, oskew);
      } else
        cpStripToTile(bufp + colb, tilebuf, nrow, tilew, iskew, 0);
      colb += tilew;
    }
    bufp += imagew * nrow;
  }
done:
  _TIFFfree(tilebuf);
}

DECLAREreadFunc(readSeparateTilesIntoBuffer) {
  uint32_t imagew = TIFFRasterScanlineSize(in);
  uint32_t tilew = TIFFTileRowSize(in);
  int iskew = imagew - tilew * spp;
  tdata_t tilebuf = _TIFFmalloc(TIFFTileSize(in));
  uint8_t *bufp = (uint8_t *)buf;
  uint32_t tw, tl;
  uint32_t row;
  uint16_t bps, bytes_per_sample;

  if (tilebuf == 0)
    return;
  (void)TIFFGetField(in, TIFFTAG_TILEWIDTH, &tw);
  (void)TIFFGetField(in, TIFFTAG_TILELENGTH, &tl);
  (void)TIFFGetField(in, TIFFTAG_BITSPERSAMPLE, &bps);
  assert(bps % 8 == 0);
  bytes_per_sample = bps / 8;

  for (row = 0; row < imagelength; row += tl) {
    uint32_t nrow = (row + tl > imagelength) ? imagelength - row : tl;
    uint32_t colb = 0;
    uint32_t col;

    for (col = 0; col < imagewidth; col += tw) {
      tsample_t s;

      for (s = 0; s < spp; s++) {
        if (TIFFReadTile(in, tilebuf, col, row, 0, s) < 0 && !ignore)
          goto done;
        /*
         * Tile is clipped horizontally.  Calculate
         * visible portion and skewing factors.
         */
        if (colb + tilew * spp > imagew) {
          uint32_t width = imagew - colb;
          int oskew = tilew * spp - width;
          cpSeparateBufToContigBuf(bufp + colb + s * bytes_per_sample, tilebuf,
                                   nrow, width / (spp * bytes_per_sample),
                                   oskew + iskew, oskew / spp, spp,
                                   bytes_per_sample);
        } else
          cpSeparateBufToContigBuf(bufp + colb + s * bytes_per_sample, tilebuf,
                                   nrow, tw, iskew, 0, spp, bytes_per_sample);
      }
      colb += tilew * spp;
    }
    bufp += imagew * nrow;
  }
done:
  _TIFFfree(tilebuf);
}

DECLAREwriteFunc(writeBufferToContigStrips) {
  uint32_t row, rowsperstrip;
  tstrip_t strip = 0;

  (void)imagewidth;
  (void)spp;
  (void)TIFFGetFieldDefaulted(out, TIFFTAG_ROWSPERSTRIP, &rowsperstrip);
  for (row = 0; row < imagelength; row += rowsperstrip) {
    uint32_t nrows =
        (row + rowsperstrip > imagelength) ? imagelength - row : rowsperstrip;
    tsize_t stripsize = TIFFVStripSize(out, nrows);
    if (TIFFWriteEncodedStrip(out, strip++, buf, stripsize) < 0)
      return (FALSE);
    buf += stripsize;
  }
  return (TRUE);
}

DECLAREwriteFunc(writeBufferToSeparateStrips) {
  uint32_t rowsize = imagewidth * spp;
  uint32_t rowsperstrip;
  tdata_t obuf = _TIFFmalloc(TIFFStripSize(out));
  tstrip_t strip = 0;
  tsample_t s;

  if (obuf == NULL)
    return (0);
  (void)TIFFGetFieldDefaulted(out, TIFFTAG_ROWSPERSTRIP, &rowsperstrip);
  for (s = 0; s < spp; s++) {
    uint32_t row;
    for (row = 0; row < imagelength; row += rowsperstrip) {
      uint32_t nrows =
          (row + rowsperstrip > imagelength) ? imagelength - row : rowsperstrip;
      tsize_t stripsize = TIFFVStripSize(out, nrows);

      cpContigBufToSeparateBuf(obuf, (uint8_t *)buf + row * rowsize + s, nrows,
                               imagewidth, 0, 0, spp, 1);
      if (TIFFWriteEncodedStrip(out, strip++, obuf, stripsize) < 0) {
        _TIFFfree(obuf);
        return (FALSE);
      }
    }
  }
  _TIFFfree(obuf);
  return (TRUE);
}

DECLAREwriteFunc(writeBufferToContigTiles) {
  uint32_t imagew = TIFFScanlineSize(out);
  uint32_t tilew = TIFFTileRowSize(out);
  int iskew = imagew - tilew;
  tdata_t obuf = _TIFFmalloc(TIFFTileSize(out));
  uint8_t *bufp = (uint8_t *)buf;
  uint32_t tl, tw;
  uint32_t row;

  (void)spp;
  if (obuf == NULL)
    return (FALSE);
  (void)TIFFGetField(out, TIFFTAG_TILELENGTH, &tl);
  (void)TIFFGetField(out, TIFFTAG_TILEWIDTH, &tw);
  for (row = 0; row < imagelength; row += tilelength) {
    uint32_t nrow = (row + tl > imagelength) ? imagelength - row : tl;
    uint32_t colb = 0;
    uint32_t col;

    for (col = 0; col < imagewidth; col += tw) {
      /*
       * Tile is clipped horizontally.  Calculate
       * visible portion and skewing factors.
       */
      if (colb + tilew > imagew) {
        uint32_t width = imagew - colb;
        int oskew = tilew - width;
        cpStripToTile(obuf, bufp + colb, nrow, width, oskew, oskew + iskew);
      } else
        cpStripToTile(obuf, bufp + colb, nrow, tilew, 0, iskew);
      if (TIFFWriteTile(out, obuf, col, row, 0, 0) < 0) {
        _TIFFfree(obuf);
        return (FALSE);
      }
      colb += tilew;
    }
    bufp += nrow * imagew;
  }
  _TIFFfree(obuf);
  return (TRUE);
}

DECLAREwriteFunc(writeBufferToSeparateTiles) {
  uint32_t imagew = TIFFScanlineSize(out);
  tsize_t tilew = TIFFTileRowSize(out);
  uint32_t iimagew = TIFFRasterScanlineSize(out);
  int iskew = iimagew - tilew * spp;
  tdata_t obuf = _TIFFmalloc(TIFFTileSize(out));
  uint8_t *bufp = (uint8_t *)buf;
  uint32_t tl, tw;
  uint32_t row;
  uint16_t bps, bytes_per_sample;

  if (obuf == NULL)
    return (FALSE);
  (void)TIFFGetField(out, TIFFTAG_TILELENGTH, &tl);
  (void)TIFFGetField(out, TIFFTAG_TILEWIDTH, &tw);
  (void)TIFFGetField(out, TIFFTAG_BITSPERSAMPLE, &bps);
  assert(bps % 8 == 0);
  bytes_per_sample = bps / 8;

  for (row = 0; row < imagelength; row += tl) {
    uint32_t nrow = (row + tl > imagelength) ? imagelength - row : tl;
    uint32_t colb = 0;
    uint32_t col;

    for (col = 0; col < imagewidth; col += tw) {
      tsample_t s;
      for (s = 0; s < spp; s++) {
        /*
         * Tile is clipped horizontally.  Calculate
         * visible portion and skewing factors.
         */
        if (colb + tilew > imagew) {
          uint32_t width = (imagew - colb);
          int oskew = tilew - width;

          cpContigBufToSeparateBuf(
              obuf, bufp + (colb * spp) + s, nrow, width / bytes_per_sample,
              oskew, (oskew * spp) + iskew, spp, bytes_per_sample);
        } else
          cpContigBufToSeparateBuf(obuf, bufp + (colb * spp) + s, nrow,
                                   tilewidth, 0, iskew, spp, bytes_per_sample);
        if (TIFFWriteTile(out, obuf, col, row, 0, s) < 0) {
          _TIFFfree(obuf);
          return (FALSE);
        }
      }
      colb += tilew;
    }
    bufp += nrow * iimagew;
  }
  _TIFFfree(obuf);
  return (TRUE);
}

/*
 * Contig strips -> contig tiles.
 */
DECLAREcpFunc(cpContigStrips2ContigTiles) {
  return cpImage(in, out, readContigStripsIntoBuffer, writeBufferToContigTiles,
                 imagelength, imagewidth, spp);
}

/*
 * Contig strips -> separate tiles.
 */
DECLAREcpFunc(cpContigStrips2SeparateTiles) {
  return cpImage(in, out, readContigStripsIntoBuffer,
                 writeBufferToSeparateTiles, imagelength, imagewidth, spp);
}

/*
 * Separate strips -> contig tiles.
 */
DECLAREcpFunc(cpSeparateStrips2ContigTiles) {
  return cpImage(in, out, readSeparateStripsIntoBuffer,
                 writeBufferToContigTiles, imagelength, imagewidth, spp);
}

/*
 * Separate strips -> separate tiles.
 */
DECLAREcpFunc(cpSeparateStrips2SeparateTiles) {
  return cpImage(in, out, readSeparateStripsIntoBuffer,
                 writeBufferToSeparateTiles, imagelength, imagewidth, spp);
}

/*
 * Contig strips -> contig tiles.
 */
DECLAREcpFunc(cpContigTiles2ContigTiles) {
  return cpImage(in, out, readContigTilesIntoBuffer, writeBufferToContigTiles,
                 imagelength, imagewidth, spp);
}

/*
 * Contig tiles -> separate tiles.
 */
DECLAREcpFunc(cpContigTiles2SeparateTiles) {
  return cpImage(in, out, readContigTilesIntoBuffer, writeBufferToSeparateTiles,
                 imagelength, imagewidth, spp);
}

/*
 * Separate tiles -> contig tiles.
 */
DECLAREcpFunc(cpSeparateTiles2ContigTiles) {
  return cpImage(in, out, readSeparateTilesIntoBuffer, writeBufferToContigTiles,
                 imagelength, imagewidth, spp);
}

/*
 * Separate tiles -> separate tiles (tile dimension change).
 */
DECLAREcpFunc(cpSeparateTiles2SeparateTiles) {
  return cpImage(in, out, readSeparateTilesIntoBuffer,
                 writeBufferToSeparateTiles, imagelength, imagewidth, spp);
}

/*
 * Contig tiles -> contig tiles (tile dimension change).
 */
DECLAREcpFunc(cpContigTiles2ContigStrips) {
  return cpImage(in, out, readContigTilesIntoBuffer, writeBufferToContigStrips,
                 imagelength, imagewidth, spp);
}

/*
 * Contig tiles -> separate strips.
 */
DECLAREcpFunc(cpContigTiles2SeparateStrips) {
  return cpImage(in, out, readContigTilesIntoBuffer,
                 writeBufferToSeparateStrips, imagelength, imagewidth, spp);
}

/*
 * Separate tiles -> contig strips.
 */
DECLAREcpFunc(cpSeparateTiles2ContigStrips) {
  return cpImage(in, out, readSeparateTilesIntoBuffer,
                 writeBufferToContigStrips, imagelength, imagewidth, spp);
}

/*
 * Separate tiles -> separate strips.
 */
DECLAREcpFunc(cpSeparateTiles2SeparateStrips) {
  return cpImage(in, out, readSeparateTilesIntoBuffer,
                 writeBufferToSeparateStrips, imagelength, imagewidth, spp);
}

/*
 * Select the appropriate copy function to use.
 */
static copyFunc pickCopyFunc(TIFF *in, TIFF *out, uint16_t bitspersample,
                             uint16_t samplesperpixel) {
  uint16_t shortv;
  uint32_t w, l, tw, tl;
  int bychunk;

  (void)TIFFGetField(in, TIFFTAG_PLANARCONFIG, &shortv);
  if (shortv != config && bitspersample != 8 && samplesperpixel > 1) {
    fprintf(stderr,
            "%s: Cannot handle different planar configuration w/ bits/sample "
            "!= 8\n",
            TIFFFileName(in));
    return (NULL);
  }
  TIFFGetField(in, TIFFTAG_IMAGEWIDTH, &w);
  TIFFGetField(in, TIFFTAG_IMAGELENGTH, &l);
  if (!(TIFFIsTiled(out) || TIFFIsTiled(in))) {
    uint32_t irps = (uint32_t)-1L;
    TIFFGetField(in, TIFFTAG_ROWSPERSTRIP, &irps);
    /* if biased, force decoded copying to allow image subtraction */
    bychunk = !bias && (rowsperstrip == irps);
  } else { /* either in or out is tiled */
    if (bias) {
      fprintf(stderr, "%s: Cannot handle tiled configuration w/bias image\n",
              TIFFFileName(in));
      return (NULL);
    }
    if (TIFFIsTiled(out)) {
      if (!TIFFGetField(in, TIFFTAG_TILEWIDTH, &tw))
        tw = w;
      if (!TIFFGetField(in, TIFFTAG_TILELENGTH, &tl))
        tl = l;
      bychunk = (tw == tilewidth && tl == tilelength);
    } else { /* out's not, so in must be tiled */
      TIFFGetField(in, TIFFTAG_TILEWIDTH, &tw);
      TIFFGetField(in, TIFFTAG_TILELENGTH, &tl);
      bychunk = (tw == w && tl == rowsperstrip);
    }
  }
#define T 1
#define F 0
#define pack(a, b, c, d, e)                                                    \
  ((long)(((a) << 11) | ((b) << 3) | ((c) << 2) | ((d) << 1) | (e)))
  switch (pack(shortv, config, TIFFIsTiled(in), TIFFIsTiled(out), bychunk)) {
    /* Strips -> Tiles */
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_CONTIG, F, T, F):
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_CONTIG, F, T, T):
    return cpContigStrips2ContigTiles;
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_SEPARATE, F, T, F):
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_SEPARATE, F, T, T):
    return cpContigStrips2SeparateTiles;
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_CONTIG, F, T, F):
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_CONTIG, F, T, T):
    return cpSeparateStrips2ContigTiles;
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_SEPARATE, F, T, F):
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_SEPARATE, F, T, T):
    return cpSeparateStrips2SeparateTiles;
    /* Tiles -> Tiles */
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_CONTIG, T, T, F):
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_CONTIG, T, T, T):
    return cpContigTiles2ContigTiles;
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_SEPARATE, T, T, F):
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_SEPARATE, T, T, T):
    return cpContigTiles2SeparateTiles;
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_CONTIG, T, T, F):
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_CONTIG, T, T, T):
    return cpSeparateTiles2ContigTiles;
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_SEPARATE, T, T, F):
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_SEPARATE, T, T, T):
    return cpSeparateTiles2SeparateTiles;
    /* Tiles -> Strips */
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_CONTIG, T, F, F):
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_CONTIG, T, F, T):
    return cpContigTiles2ContigStrips;
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_SEPARATE, T, F, F):
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_SEPARATE, T, F, T):
    return cpContigTiles2SeparateStrips;
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_CONTIG, T, F, F):
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_CONTIG, T, F, T):
    return cpSeparateTiles2ContigStrips;
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_SEPARATE, T, F, F):
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_SEPARATE, T, F, T):
    return cpSeparateTiles2SeparateStrips;
    /* Strips -> Strips */
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_CONTIG, F, F, F):
    return bias ? cpBiasedContig2Contig : cpContig2ContigByRow;
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_CONTIG, F, F, T):
    return cpDecodedStrips;
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_SEPARATE, F, F, F):
  case pack(PLANARCONFIG_CONTIG, PLANARCONFIG_SEPARATE, F, F, T):
    return cpContig2SeparateByRow;
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_CONTIG, F, F, F):
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_CONTIG, F, F, T):
    return cpSeparate2ContigByRow;
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_SEPARATE, F, F, F):
  case pack(PLANARCONFIG_SEPARATE, PLANARCONFIG_SEPARATE, F, F, T):
    return cpSeparate2SeparateByRow;
  }
#undef pack
#undef F
#undef T
  fprintf(stderr, "tiffcp: %s: Don't know how to copy/convert image.\n",
          TIFFFileName(in));
  return (NULL);
}

/* vim: set ts=8 sts=8 sw=8 noet: */
