/*
 * jdcolor.c
 *
 * Copyright (C) 1991-1997, Thomas G. Lane.
 * This file is part of the Independent JPEG Group's software.
 * For conditions of distribution and use, see the accompanying README file.
 *
 * This file contains output colorspace conversion routines.
 */

#define JPEG_INTERNALS
#include "jinclude.h"
#include "jpeglib.h"


/* Private subobject */

typedef struct {
  struct jpeg_color_deconverter pub; /* public fields */

  /* Private state for YCC->RGB conversion */
  int * Cr_r_tab;   /* => table for Cr to R conversion */
  int * Cb_b_tab;   /* => table for Cb to B conversion */
  INT32 * Cr_g_tab;   /* => table for Cr to G conversion */
  INT32 * Cb_g_tab;   /* => table for Cb to G conversion */
} my_color_deconverter;

typedef my_color_deconverter * my_cconvert_ptr;


/**************** YCbCr -> RGB conversion: most common case **************/

/*
 * YCbCr is defined per CCIR 601-1, except that Cb and Cr are
 * normalized to the range 0..MAXJSAMPLE rather than -0.5 .. 0.5.
 * The conversion equations to be implemented are therefore
 *  R = Y                + 1.40200 * Cr
 *  G = Y - 0.34414 * Cb - 0.71414 * Cr
 *  B = Y + 1.77200 * Cb
 * where Cb and Cr represent the incoming values less CENTERJSAMPLE.
 * (These numbers are derived from TIFF 6.0 section 21, dated 3-June-92.)
 *
 * To avoid floating-point arithmetic, we represent the fractional constants
 * as integers scaled up by 2^16 (about 4 digits precision); we have to divide
 * the products by 2^16, with appropriate rounding, to get the correct answer.
 * Notice that Y, being an integral input, does not contribute any fraction
 * so it need not participate in the rounding.
 *
 * For even more speed, we avoid doing any multiplications in the inner loop
 * by precalculating the constants times Cb and Cr for all possible values.
 * For 8-bit JSAMPLEs this is very reasonable (only 256 entries per table);
 * for 12-bit samples it is still acceptable.  It's not very reasonable for
 * 16-bit samples, but if you want lossless storage you shouldn't be changing
 * colorspace anyway.
 * The Cr=>R and Cb=>B values can be rounded to integers in advance; the
 * values for the G calculation are left scaled up, since we must add them
 * together before rounding.
 */

#define SCALEBITS 15  /* speediest right-shift on some machines */
#define ONE_HALF  ((INT32) 1 << (SCALEBITS-1))
#define FIX(x)    ((INT32) ((x) * (1L<<SCALEBITS) + 0.5))


/*
 * Initialize tables for YCC->RGB colorspace conversion.
 */

LOCAL(void)
build_ycc_rgb_table (j_decompress_ptr cinfo)
{
  my_cconvert_ptr cconvert = (my_cconvert_ptr) cinfo->cconvert;
  int i;
  INT32 x;
  SHIFT_TEMPS

  cconvert->Cr_r_tab = (int *)
    (*cinfo->mem->alloc_small) ((j_common_ptr) cinfo, JPOOL_IMAGE,
        (MAXJSAMPLE+1) * SIZEOF(int));
  cconvert->Cb_b_tab = (int *)
    (*cinfo->mem->alloc_small) ((j_common_ptr) cinfo, JPOOL_IMAGE,
        (MAXJSAMPLE+1) * SIZEOF(int));
  cconvert->Cr_g_tab = (INT32 *)
    (*cinfo->mem->alloc_small) ((j_common_ptr) cinfo, JPOOL_IMAGE,
        (MAXJSAMPLE+1) * SIZEOF(INT32));
  cconvert->Cb_g_tab = (INT32 *)
    (*cinfo->mem->alloc_small) ((j_common_ptr) cinfo, JPOOL_IMAGE,
        (MAXJSAMPLE+1) * SIZEOF(INT32));

  for (i = 0, x = -CENTERJSAMPLE; i <= MAXJSAMPLE; i++, x++) {
    /* i is the actual input pixel value, in the range 0..MAXJSAMPLE */
    /* The Cb or Cr value we are thinking of is x = i - CENTERJSAMPLE */
    /* Cr=>R value is nearest int to 1.40200 * x */
    cconvert->Cr_r_tab[i] = (int)
        RIGHT_SHIFT(FIX(1.40200) * x + ONE_HALF, SCALEBITS);
    /* Cb=>B value is nearest int to 1.77200 * x */
    cconvert->Cb_b_tab[i] = (int)
        RIGHT_SHIFT(FIX(1.77200) * x + ONE_HALF, SCALEBITS);
    /* Cr=>G value is scaled-up -0.71414 * x */
    cconvert->Cr_g_tab[i] = (- FIX(0.71414)) * x;
    /* Cb=>G value is scaled-up -0.34414 * x */
    /* We also add in ONE_HALF so that need not do it in inner loop */
    cconvert->Cb_g_tab[i] = (- FIX(0.34414)) * x + ONE_HALF;
  }
}

/*
 * Convert some rows of samples to the output colorspace.
 *
 * Note that we change from noninterleaved, one-plane-per-component format
 * to interleaved-pixel format.  The output buffer is therefore three times
 * as wide as the input buffer.
 * A starting row offset is provided only for the input buffer.  The caller
 * can easily adjust the passed output_buf value to accommodate any row
 * offset required on that side.
 */

#if defined( __POWERPC__ ) && defined( LC_USE_ALTIVEC )
#include <altivec.h>
#endif

METHODDEF(void)
ycc_rgb_convert (j_decompress_ptr cinfo,
     JSAMPIMAGE input_buf, JDIMENSION input_row,
     JSAMPARRAY output_buf, int num_rows)
{
  int y, cb, cr;
  JSAMPROW outptr;
  JSAMPROW inptr0, inptr1, inptr2;
  JDIMENSION col;
  JDIMENSION num_cols = cinfo->output_width;
  JSAMPLE *range_limit = cinfo->sample_range_limit;
  my_cconvert_ptr cconvert = (my_cconvert_ptr) cinfo->cconvert;
#if defined( __POWERPC__ ) && defined( LC_USE_ALTIVEC )
  const vector char b128 = (vector char) (128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128, 128);
  const vector short zero = vec_splat_s16(0);
  const vector short shift = vec_splat_s16(1);

  const vector short constants = (vector short) (- FIX(0.34414)/2, FIX(1.77200)/2, FIX(1.40200)/2, - FIX(0.71414)/2);
  const vector short crr = vec_splat(constants, 2);
  const vector short cbg = vec_splat(constants, 0);
  const vector short crg = vec_splat(constants, 3);
  const vector short cbb = vec_splat(constants, 1);

  while (--num_rows >= 0) {
    inptr0 = input_buf[0][input_row];
    inptr1 = input_buf[1][input_row];
    inptr2 = input_buf[2][input_row];
    input_row++;
    outptr = *output_buf++;

    vector unsigned char yAlign = vec_lvsl( 0, inptr0 );
    vector unsigned char cbAlign = vec_lvsl( 0, inptr1 );
    vector unsigned char crAlign = vec_lvsl( 0, inptr2 );

    vector char vy  = vec_ld(0, &inptr0[0]);
    vector char vcb = vec_ld(0, &inptr1[0]);
    vector char vcr = vec_ld(0, &inptr2[0]);

    for (col = 0; col < num_cols; col+=16) {
      if (num_cols - col >= 16) {
	vector char y_extra  = vec_ld(0, &inptr0[col+16]);
	vy = vec_perm(vy, y_extra, yAlign);

	vector char cb_extra = vec_ld(0, &inptr1[col+16]);
	vcb = vec_perm(vcb, cb_extra, cbAlign) - b128;

	vector char cr_extra = vec_ld(0, &inptr2[col+16]);
	vcr = vec_perm(vcr, cr_extra, crAlign) - b128;

	vector short yh = vec_mergeh((vector char) vec_splat_u8(0), vy);
	vector short cbh = vec_sl(vec_unpackh(vcb), (vector unsigned short) shift);
	vector short crh = vec_sl(vec_unpackh(vcr), (vector unsigned short) shift);

	vector short yl = vec_mergel((vector char) vec_splat_u8(0), vy);
	vector short cbl = vec_sl(vec_unpackl(vcb), (vector unsigned short) shift);
	vector short crl = vec_sl(vec_unpackl(vcr), (vector unsigned short) shift);

	vector char r = vec_packsu(vec_max(zero, yh + vec_mradds(crh, crr, zero)),
				   vec_max(zero, yl + vec_mradds(crl, crr, zero)));
	vector char g = vec_packsu(vec_max(zero, yh + vec_mradds(cbh, cbg, zero) + vec_mradds(crh, crg, zero)),
				   vec_max(zero, yl + vec_mradds(cbl, cbg, zero) + vec_mradds(crl, crg, zero)));
	vector char b = vec_packsu(vec_max(zero, yh + vec_mradds(cbh, cbb, zero)),
				   vec_max(zero, yl + vec_mradds(cbl, cbb, zero)));

	// mix red and green first
	vector char rgh = vec_mergeh(r, g);
	vector char rgl = vec_mergel(r, g);

	// then add the blue
	vector int rgb1 = vec_perm(rgh,    b, (vector unsigned char) ( 0,  1, 16,  2,  3, 17,  4,  5, 18,  6,  7, 19,  8,  9, 20, 10));
	vector int rgb2 = vec_perm(rgh,    b, (vector unsigned char) (11, 21, 12, 13, 21, 14, 15, 22,  0,  0, 23,  0,  0, 25,  0,  0));
	           rgb2 = vec_perm(rgb2, (vector int) rgl, (vector unsigned char) ( 0,  1,  2,  3,  4,  5,  6,  7, 16, 17, 10, 18, 19, 13, 20, 21));
	vector int rgb3 = vec_perm(rgl,    b, (vector unsigned char) (26,  6,  7, 27,  8,  9, 28, 10, 11, 29, 12, 13, 30, 14, 15, 31));

	int offset = (int) outptr & 15L;

	if( offset ) {
	  /*
	   * Deal with unaligned data intelligently, see:
	   * http://developer.apple.com/hardware/ve/downloads/add.c
	   */

	  vector unsigned char align_out = vec_lvsr( 0, outptr );
	  vector unsigned int store = vec_perm( rgb1, rgb1, align_out );

          if (offset & 1) {
            vec_ste( (vector unsigned char) store, 0, outptr );
            outptr++;
          }

          if ((16 - offset) & 2) {
            vec_ste( (vector unsigned short) store, 0, (unsigned short *) outptr );
            outptr += 2;
          }

	  // avoid looping, the compiler will try to be smart and unroll them
	  if (offset <= 4) {
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	  } else if (offset <= 8) {
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	  } else if (offset <= 12) {
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	  }

	  store = vec_perm( rgb1, rgb2, align_out );
	  vec_st( store, 0, (unsigned int *) outptr );
	  outptr+=16;

	  store = vec_perm( rgb2, rgb3, align_out );
	  vec_st( store, 0, (unsigned int *) outptr );
	  outptr+=16;

	  store = vec_perm( rgb3, rgb3, align_out );

	  // avoid looping
	  if (offset >= 12) {
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	  } else if (offset >= 8) {
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	  } else if (offset >= 4) {
	    vec_ste( store, 0, (unsigned int *) outptr );
	    outptr+=4;
	  }

          if (offset & 2) {
            vec_ste( (vector unsigned short) store, 0, (unsigned short *) outptr );
            outptr += 2;
          }

          if (offset & 1) {
            vec_ste( (vector unsigned char) store, 0, (unsigned char *) outptr );
            outptr++;
          }
	} else {
	  // we're lucky
	  vec_st( rgb1, 0, (int *) outptr );
	  outptr += 16;
	  vec_st( rgb2, 0, (int *) outptr );
	  outptr += 16;
	  vec_st( rgb3, 0, (int *) outptr );
	  outptr += 16;
	}

	vy = y_extra;
	vcb = cb_extra;
	vcr = cr_extra;
      } else {
        // last pixels in the row that won't fit in a vector
        for ( ; col < num_cols; col++) {
          y  = GETJSAMPLE(inptr0[col]);
          cb = GETJSAMPLE(inptr1[col]) - 128;
          cr = GETJSAMPLE(inptr2[col]) - 128;
          // Range-limiting is essential due to noise introduced by DCT losses.
          outptr[RGB_RED] =   range_limit[y + RIGHT_SHIFT(                      FIX(1.40200) * cr + ONE_HALF, SCALEBITS)];
          outptr[RGB_GREEN] = range_limit[y + RIGHT_SHIFT(- FIX(0.34414) * cb - FIX(0.71414) * cr + ONE_HALF, SCALEBITS)];
          outptr[RGB_BLUE] =  range_limit[y + RIGHT_SHIFT(  FIX(1.77200) * cb                     + ONE_HALF, SCALEBITS)];
          outptr += RGB_PIXELSIZE;
        }
      }
    }
  }
#else
  /* copy these pointers into registers if possible */
  // JSAMPLE * range_limit = cinfo->sample_range_limit;
  int *Crrtab = cconvert->Cr_r_tab;
  int *Cbbtab = cconvert->Cb_b_tab;
  INT32 *Crgtab = cconvert->Cr_g_tab;
  INT32 *Cbgtab = cconvert->Cb_g_tab;
  SHIFT_TEMPS

  while (--num_rows >= 0) {
    inptr0 = input_buf[0][input_row];
    inptr1 = input_buf[1][input_row];
    inptr2 = input_buf[2][input_row];
    input_row++;
    outptr = *output_buf++;
    for (col = 0; col < num_cols; col++) {
      y  = GETJSAMPLE(inptr0[col]);
      cb = GETJSAMPLE(inptr1[col]);
      cr = GETJSAMPLE(inptr2[col]);
      /* Range-limiting is essential due to noise introduced by DCT losses. */
      outptr[RGB_RED] =   range_limit[y + Crrtab[cr]];
      outptr[RGB_GREEN] = range_limit[y +
				      ((int) RIGHT_SHIFT(Cbgtab[cb] + Crgtab[cr],
							 SCALEBITS))];
      outptr[RGB_BLUE] =  range_limit[y + Cbbtab[cb]];
      outptr += RGB_PIXELSIZE;
    }
  }
#endif
}

/**************** Cases other than YCbCr -> RGB **************/


/*
 * Color conversion for no colorspace change: just copy the data,
 * converting from separate-planes to interleaved representation.
 */

METHODDEF(void)
null_convert (j_decompress_ptr cinfo,
        JSAMPIMAGE input_buf, JDIMENSION input_row,
        JSAMPARRAY output_buf, int num_rows)
{
  register JSAMPROW inptr, outptr;
  register JDIMENSION count;
  register int num_components = cinfo->num_components;
  JDIMENSION num_cols = cinfo->output_width;
  int ci;

  while (--num_rows >= 0) {
    for (ci = 0; ci < num_components; ci++) {
      inptr = input_buf[ci][input_row];
      outptr = output_buf[0] + ci;
      for (count = num_cols; count > 0; count--) {
  *outptr = *inptr++; /* needn't bother with GETJSAMPLE() here */
  outptr += num_components;
      }
    }
    input_row++;
    output_buf++;
  }
}


/*
 * Color conversion for grayscale: just copy the data.
 * This also works for YCbCr -> grayscale conversion, in which
 * we just copy the Y (luminance) component and ignore chrominance.
 */

METHODDEF(void)
grayscale_convert (j_decompress_ptr cinfo,
       JSAMPIMAGE input_buf, JDIMENSION input_row,
       JSAMPARRAY output_buf, int num_rows)
{
  jcopy_sample_rows(input_buf[0], (int) input_row, output_buf, 0,
        num_rows, cinfo->output_width);
}


/*
 * Convert grayscale to RGB: just duplicate the graylevel three times.
 * This is provided to support applications that don't want to cope
 * with grayscale as a separate case.
 */

METHODDEF(void)
gray_rgb_convert (j_decompress_ptr cinfo,
      JSAMPIMAGE input_buf, JDIMENSION input_row,
      JSAMPARRAY output_buf, int num_rows)
{
  register JSAMPROW inptr, outptr;
  register JDIMENSION col;
  JDIMENSION num_cols = cinfo->output_width;

  while (--num_rows >= 0) {
    inptr = input_buf[0][input_row++];
    outptr = *output_buf++;
    for (col = 0; col < num_cols; col++) {
      /* We can dispense with GETJSAMPLE() here */
      outptr[RGB_RED] = outptr[RGB_GREEN] = outptr[RGB_BLUE] = inptr[col];
      outptr += RGB_PIXELSIZE;
    }
  }
}


/*
 * Adobe-style YCCK->CMYK conversion.
 * We convert YCbCr to R=1-C, G=1-M, and B=1-Y using the same
 * conversion as above, while passing K (black) unchanged.
 * We assume build_ycc_rgb_table has been called.
 */

METHODDEF(void)
ycck_cmyk_convert (j_decompress_ptr cinfo,
       JSAMPIMAGE input_buf, JDIMENSION input_row,
       JSAMPARRAY output_buf, int num_rows)
{
  my_cconvert_ptr cconvert = (my_cconvert_ptr) cinfo->cconvert;
  register int y, cb, cr;
  register JSAMPROW outptr;
  register JSAMPROW inptr0, inptr1, inptr2, inptr3;
  register JDIMENSION col;
  JDIMENSION num_cols = cinfo->output_width;
  /* copy these pointers into registers if possible */
  register JSAMPLE * range_limit = cinfo->sample_range_limit;
  register int * Crrtab = cconvert->Cr_r_tab;
  register int * Cbbtab = cconvert->Cb_b_tab;
  register INT32 * Crgtab = cconvert->Cr_g_tab;
  register INT32 * Cbgtab = cconvert->Cb_g_tab;
  SHIFT_TEMPS

  while (--num_rows >= 0) {
    inptr0 = input_buf[0][input_row];
    inptr1 = input_buf[1][input_row];
    inptr2 = input_buf[2][input_row];
    inptr3 = input_buf[3][input_row];
    input_row++;
    outptr = *output_buf++;
    for (col = 0; col < num_cols; col++) {
      y  = GETJSAMPLE(inptr0[col]);
      cb = GETJSAMPLE(inptr1[col]);
      cr = GETJSAMPLE(inptr2[col]);
      /* Range-limiting is essential due to noise introduced by DCT losses. */
      outptr[0] = range_limit[MAXJSAMPLE - (y + Crrtab[cr])]; /* red */
      outptr[1] = range_limit[MAXJSAMPLE - (y +     /* green */
            ((int) RIGHT_SHIFT(Cbgtab[cb] + Crgtab[cr],
             SCALEBITS)))];
      outptr[2] = range_limit[MAXJSAMPLE - (y + Cbbtab[cb])]; /* blue */
      /* K passes through unchanged */
      outptr[3] = inptr3[col];  /* don't need GETJSAMPLE here */
      outptr += 4;
    }
  }
}


/*
 * Empty method for start_pass.
 */

METHODDEF(void)
start_pass_dcolor (j_decompress_ptr cinfo)
{
  /* no work needed */
}


/*
 * Module initialization routine for output colorspace conversion.
 */

GLOBAL(void)
jinit_color_deconverter (j_decompress_ptr cinfo)
{
  my_cconvert_ptr cconvert;
  int ci;

  cconvert = (my_cconvert_ptr)
    (*cinfo->mem->alloc_small) ((j_common_ptr) cinfo, JPOOL_IMAGE,
        SIZEOF(my_color_deconverter));
  cinfo->cconvert = (struct jpeg_color_deconverter *) cconvert;
  cconvert->pub.start_pass = start_pass_dcolor;

  /* Make sure num_components agrees with jpeg_color_space */
  switch (cinfo->jpeg_color_space) {
  case JCS_GRAYSCALE:
    if (cinfo->num_components != 1)
      ERREXIT(cinfo, JERR_BAD_J_COLORSPACE);
    break;

  case JCS_RGB:
  case JCS_YCbCr:
    if (cinfo->num_components != 3)
      ERREXIT(cinfo, JERR_BAD_J_COLORSPACE);
    break;

  case JCS_CMYK:
  case JCS_YCCK:
    if (cinfo->num_components != 4)
      ERREXIT(cinfo, JERR_BAD_J_COLORSPACE);
    break;

  default:      /* JCS_UNKNOWN can be anything */
    if (cinfo->num_components < 1)
      ERREXIT(cinfo, JERR_BAD_J_COLORSPACE);
    break;
  }

  /* Set out_color_components and conversion method based on requested space.
   * Also clear the component_needed flags for any unused components,
   * so that earlier pipeline stages can avoid useless computation.
   */

  switch (cinfo->out_color_space) {
  case JCS_GRAYSCALE:
    cinfo->out_color_components = 1;
    if (cinfo->jpeg_color_space == JCS_GRAYSCALE ||
  cinfo->jpeg_color_space == JCS_YCbCr) {
      cconvert->pub.color_convert = grayscale_convert;
      /* For color->grayscale conversion, only the Y (0) component is needed */
      for (ci = 1; ci < cinfo->num_components; ci++)
  cinfo->comp_info[ci].component_needed = FALSE;
    } else
      ERREXIT(cinfo, JERR_CONVERSION_NOTIMPL);
    break;

  case JCS_RGB:
    cinfo->out_color_components = RGB_PIXELSIZE;
    if (cinfo->jpeg_color_space == JCS_YCbCr) {
      cconvert->pub.color_convert = ycc_rgb_convert;
      build_ycc_rgb_table(cinfo);
    } else if (cinfo->jpeg_color_space == JCS_GRAYSCALE) {
      cconvert->pub.color_convert = gray_rgb_convert;
    } else if (cinfo->jpeg_color_space == JCS_RGB && RGB_PIXELSIZE == 3) {
      cconvert->pub.color_convert = null_convert;
    } else
      ERREXIT(cinfo, JERR_CONVERSION_NOTIMPL);
    break;

  case JCS_CMYK:
    cinfo->out_color_components = 4;
    if (cinfo->jpeg_color_space == JCS_YCCK) {
      cconvert->pub.color_convert = ycck_cmyk_convert;
      build_ycc_rgb_table(cinfo);
    } else if (cinfo->jpeg_color_space == JCS_CMYK) {
      cconvert->pub.color_convert = null_convert;
    } else
      ERREXIT(cinfo, JERR_CONVERSION_NOTIMPL);
    break;

  default:
    /* Permit null conversion to same output space */
    if (cinfo->out_color_space == cinfo->jpeg_color_space) {
      cinfo->out_color_components = cinfo->num_components;
      cconvert->pub.color_convert = null_convert;
    } else      /* unsupported non-null conversion */
      ERREXIT(cinfo, JERR_CONVERSION_NOTIMPL);
    break;
  }

  if (cinfo->quantize_colors)
    cinfo->output_components = 1; /* single colormapped output component */
  else
    cinfo->output_components = cinfo->out_color_components;
}
/* vim:set et sw=2 ts=2: */
