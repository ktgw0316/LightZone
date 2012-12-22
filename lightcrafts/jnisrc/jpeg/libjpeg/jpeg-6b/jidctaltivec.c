/*
 * jidctaltivec.c
 */

#include <altivec.h>

#define JPEG_INTERNALS
#include "jinclude.h"
#include "jpeglib.h"
#include "jdct.h"		/* Private declarations for DCT subsystem */


inline static void Matrix_Transpose ( vector signed short *input, vector signed short *output )
{
  vector signed short a0, a1, a2, a3, a4, a5, a6, a7;
  vector signed short b0, b1, b2, b3, b4, b5, b6, b7;

  b0 = vec_mergeh( input[0], input[4] );     /* [ 00 40 01 41 02 42 03 43 ]*/
  b1 = vec_mergel( input[0], input[4] );     /* [ 04 44 05 45 06 46 07 47 ]*/
  b2 = vec_mergeh( input[1], input[5] );     /* [ 10 50 11 51 12 52 13 53 ]*/
  b3 = vec_mergel( input[1], input[5] );     /* [ 14 54 15 55 16 56 17 57 ]*/
  b4 = vec_mergeh( input[2], input[6] );     /* [ 20 60 21 61 22 62 23 63 ]*/
  b5 = vec_mergel( input[2], input[6] );     /* [ 24 64 25 65 26 66 27 67 ]*/
  b6 = vec_mergeh( input[3], input[7] );     /* [ 30 70 31 71 32 72 33 73 ]*/
  b7 = vec_mergel( input[3], input[7] );     /* [ 34 74 35 75 36 76 37 77 ]*/

  a0 = vec_mergeh( b0, b4 );                 /* [ 00 20 40 60 01 21 41 61 ]*/
  a1 = vec_mergel( b0, b4 );                 /* [ 02 22 42 62 03 23 43 63 ]*/
  a2 = vec_mergeh( b1, b5 );                 /* [ 04 24 44 64 05 25 45 65 ]*/
  a3 = vec_mergel( b1, b5 );                 /* [ 06 26 46 66 07 27 47 67 ]*/
  a4 = vec_mergeh( b2, b6 );                 /* [ 10 30 50 70 11 31 51 71 ]*/
  a5 = vec_mergel( b2, b6 );                 /* [ 12 32 52 72 13 33 53 73 ]*/
  a6 = vec_mergeh( b3, b7 );                 /* [ 14 34 54 74 15 35 55 75 ]*/
  a7 = vec_mergel( b3, b7 );                 /* [ 16 36 56 76 17 37 57 77 ]*/

  output[0] = vec_mergeh( a0, a4 );          /* [ 00 10 20 30 40 50 60 70 ]*/
  output[1] = vec_mergel( a0, a4 );          /* [ 01 11 21 31 41 51 61 71 ]*/
  output[2] = vec_mergeh( a1, a5 );          /* [ 02 12 22 32 42 52 62 72 ]*/
  output[3] = vec_mergel( a1, a5 );          /* [ 03 13 23 33 43 53 63 73 ]*/
  output[4] = vec_mergeh( a2, a6 );          /* [ 04 14 24 34 44 54 64 74 ]*/
  output[5] = vec_mergel( a2, a6 );          /* [ 05 15 25 35 45 55 65 75 ]*/
  output[6] = vec_mergeh( a3, a7 );          /* [ 06 16 26 36 46 56 66 76 ]*/
  output[7] = vec_mergel( a3, a7 );          /* [ 07 17 27 37 47 57 67 77 ]*/

}

#define IDCT_Transform(vx,vy) \
                                                                  \
  /* 1st stage. */                                                \
  t9 = vec_mradds( a1, vx[1], zero );  /* t8 = (a1) * x1 - x7  */ \
  t8 = vec_subs( t9, vx[7]);                                      \
  t1 = vec_mradds( a1, vx[7], vx[1] ); /* t1 = (a1) * x7 + x1  */ \
  t7 = vec_mradds( a2, vx[5], vx[3] ); /* t7 = (a2) * x5 + x3  */ \
  t3 = vec_mradds( ma2, vx[3], vx[5] );/* t3 = (-a2) * x5 + x3 */ \
                                                                  \
  /* 2nd stage */                                                 \
  t5 = vec_adds( vx[0], vx[4] );        /* t5 = x0 + x4 */        \
  t0 = vec_subs( vx[0], vx[4] );        /* t0 = x0 - x4 */        \
  t9 = vec_mradds( a0, vx[2], zero );   /* t4 = (a0) * x2 - x6 */ \
  t4 = vec_subs( t9, vx[6] );                                     \
  t2 = vec_mradds( a0, vx[6], vx[2] );  /* t2 = (a0) * x6 + x2 */ \
                                                                  \
  t6 = vec_adds( t8, t3 );              /* t6 = t8 + t3 */        \
  t3 = vec_subs( t8, t3 );              /* t3 = t8 - t3 */        \
  t8 = vec_subs( t1, t7 );              /* t8 = t1 - t7 */        \
  t1 = vec_adds( t1, t7 );              /* t1 = t1 + t7 */        \
                                                                  \
  /* 3rd stage. */                                                \
  t7 = vec_adds( t5, t2 );              /* t7 = t5 + t2 */        \
  t2 = vec_subs( t5, t2 );              /* t2 = t5 - t2 */        \
  t5 = vec_adds( t0, t4 );              /* t5 = t0 + t4 */        \
  t0 = vec_subs( t0, t4 );              /* t0 = t0 - t4 */        \
                                                                  \
  t4 = vec_subs( t8, t3 );              /* t4 = t8 - t3 */        \
  t3 = vec_adds( t8, t3 );              /* t3 = t8 + t3 */        \
                                                                  \
  /* 4th stage. */                                                \
  vy[0] = vec_adds( t7, t1 );        /* y0 = t7 + t1 */           \
  vy[7] = vec_subs( t7, t1 );        /* y7 = t7 - t1 */           \
  vy[1] = vec_mradds( c4, t3, t5 );  /* y1 = (c4) * t3 + t5  */   \
  vy[6] = vec_mradds( mc4, t3, t5 ); /* y6 = (-c4) * t3 + t5 */   \
  vy[2] = vec_mradds( c4, t4, t0 );  /* y2 = (c4) * t4 + t0  */   \
  vy[5] = vec_mradds( mc4, t4, t0 ); /* y5 = (-c4) * t4 + t0 */   \
  vy[3] = vec_adds( t2, t6 );        /* y3 = t2 + t6 */           \
  vy[4] = vec_subs( t2, t6 );        /* y4 = t2 - t6 */


/* Pre-Scaling matrix -- scaled by 1 */
static vector signed short PreScale[8] = {
  (vector signed short)( 4095, 5681, 5351, 4816, 4095, 4816, 5351, 5681 ),
  (vector signed short)( 5681, 7880, 7422, 6680, 5681, 6680, 7422, 7880 ),
  (vector signed short)( 5351, 7422, 6992, 6292, 5351, 6292, 6992, 7422 ),
  (vector signed short)( 4816, 6680, 6292, 5663, 4816, 5663, 6292, 6680 ),
  (vector signed short)( 4095, 5681, 5351, 4816, 4095, 4816, 5351, 5681 ),
  (vector signed short)( 4816, 6680, 6292, 5663, 4816, 5663, 6292, 6680 ),
  (vector signed short)( 5351, 7422, 6992, 6292, 5351, 6292, 6992, 7422 ),
  (vector signed short)( 5681, 7880, 7422, 6680, 5681, 6680, 7422, 7880 )
};

GLOBAL(void)
     jpeg_idct_altivec (j_decompress_ptr cinfo, jpeg_component_info * compptr,
			JCOEFPTR coef_block,
			JSAMPARRAY output_buf, JDIMENSION output_col)
{
  short *input = (short *) coef_block;
  int *quant = (int *) compptr->dct_table;

  vector signed short t0, t1, t2, t3, t4, t5, t6, t7, t8, t9;
  vector signed short a0, a1, a2, ma2, c4, mc4, zero;
  vector signed short vx[8], vy[8];
  vector signed short *vec_input = ( vector signed short * ) input;
  vector int *vec_quant = ( vector int * ) quant;
  vector unsigned char *vec_char;
  int i;

  /* Load the multiplication constants.  Note: these constants
   * could all be loaded directly ( like zero case ), but using the
   * SpecialConstants approach causes vsplth instructions to be
   * generated instead of lvx which is more efficient given the remainder
   * of the instruction mix.
   */
  vector signed short SpecialConstants =
    (vector signed short)( 23170, 13573, 6518, 21895, -23170, -21895, 0 , 0);

  c4   = vec_splat( SpecialConstants, 0 );  /* c4 = cos(4*pi/16)  */
  a0   = vec_splat( SpecialConstants, 1 );  /* a0 = c6/c2         */
  a1   = vec_splat( SpecialConstants, 2 );  /* a1 = c7/c1         */
  a2   = vec_splat( SpecialConstants, 3 );  /* a2 = c5/c3         */
  mc4  = vec_splat( SpecialConstants, 4 );  /* -c4                */
  ma2  = vec_splat( SpecialConstants, 5 );  /* -a2                */
  zero = (vector signed short)(0);

  vector unsigned char inputAlign = vec_lvsl( 0, input );
  vector unsigned char quantAlign = vec_lvsl( 0, quant );

  /* Load the rows of input data, dequantize and Pre-Scale them. */
  vector short in = vec_ld(0, &vec_input[0]);
  vector int q0 = vec_ld(0, &vec_quant[0]);
  for (i = 0; i < 8; i++) {
    // deal with the possible lack of alignment of input and quantization tables
    vector short in_extra = vec_ld(0, &vec_input[i+1]);
    in = vec_perm(in, in_extra, inputAlign);

    vector int q1 = vec_ld(0, &vec_quant[2*i+1]);
    vector int q_extra = vec_ld(0, &vec_quant[2*i+2]);
    q0 = vec_perm(q0, q1, quantAlign);
    q1 = vec_perm(q1, q_extra, quantAlign);

    vx[i] = vec_mradds( vec_mladd( in, vec_packs(q0, q1), zero), PreScale[i], zero );

    in = in_extra;
    q0 = q_extra;
  }

  /* Perform IDCT first on the 8 columns */
  IDCT_Transform( vx, vy );

  /* Transpose matrix to work on rows */
  Matrix_Transpose( vy, vx );

  /* Perform IDCT next on the 8 rows */
  IDCT_Transform( vx, vy );

  /* Transpose matrix for the result */
  Matrix_Transpose( vy, vx );

  vector short offset = (vector short) (128, 128, 128, 128, 128, 128, 128, 128);
  vector short min    = vec_splat_s16(0);

  /* Post-scale and store result. */

  // vec_packsu takes care of saturating to 255
  vector unsigned int result0 = vec_packsu(vec_max(min, vec_add(vx[0], offset)),
					   vec_max(min, vec_add(vx[1], offset)));

  vector unsigned int result1 = vec_packsu(vec_max(min, vec_add(vx[2], offset)),
					   vec_max(min, vec_add(vx[3], offset)));

  vector unsigned int result2 = vec_packsu(vec_max(min, vec_add(vx[4], offset)),
					   vec_max(min, vec_add(vx[5], offset)));

  vector unsigned int result3 = vec_packsu(vec_max(min, vec_add(vx[6], offset)),
					   vec_max(min, vec_add(vx[7], offset)));


  unsigned char *out_buffer = output_buf[0] + output_col;
  vec_ste(vec_splat(result0, 0), 0, (unsigned int *) out_buffer);
  vec_ste(vec_splat(result0, 1), 0, (unsigned int *) out_buffer + 1);

  out_buffer = output_buf[1] + output_col;
  vec_ste(vec_splat(result0, 2), 0, (unsigned int *) out_buffer);
  vec_ste(vec_splat(result0, 3), 0, (unsigned int *) out_buffer + 1);

  out_buffer = output_buf[2] + output_col;
  vec_ste(vec_splat(result1, 0), 0, (unsigned int *) out_buffer);
  vec_ste(vec_splat(result1, 1), 0, (unsigned int *) out_buffer + 1);

  out_buffer = output_buf[3] + output_col;
  vec_ste(vec_splat(result1, 2), 0, (unsigned int *) out_buffer);
  vec_ste(vec_splat(result1, 3), 0, (unsigned int *) out_buffer + 1);

  out_buffer = output_buf[4] + output_col;
  vec_ste(vec_splat(result2, 0), 0, (unsigned int *) out_buffer);
  vec_ste(vec_splat(result2, 1), 0, (unsigned int *) out_buffer + 1);

  out_buffer = output_buf[5] + output_col;
  vec_ste(vec_splat(result2, 2), 0, (unsigned int *) out_buffer);
  vec_ste(vec_splat(result2, 3), 0, (unsigned int *) out_buffer + 1);

  out_buffer = output_buf[6] + output_col;
  vec_ste(vec_splat(result3, 0), 0, (unsigned int *) out_buffer);
  vec_ste(vec_splat(result3, 1), 0, (unsigned int *) out_buffer + 1);

  out_buffer = output_buf[7] + output_col;
  vec_ste(vec_splat(result3, 2), 0, (unsigned int *) out_buffer);
  vec_ste(vec_splat(result3, 3), 0, (unsigned int *) out_buffer + 1);
}
