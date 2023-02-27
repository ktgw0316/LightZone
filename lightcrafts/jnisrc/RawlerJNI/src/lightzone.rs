use std::fs::File;
use std::io::BufReader;
use std::iter;
use std::path::Path;
use rawler::{get_decoder, Buffer, RawImageData};
use rawler::decoders::RawDecodeParams;
use rawler::imgop::{Dim2, Point, Rect, Result, rescale_f32_to_u16};
use rawler::imgop::matrix::{multiply, normalize, pseudo_inverse};
use rawler::imgop::raw::{clip_euclidean_norm_avg, DevelopParams};
use rawler::imgop::sensor::bayer::ppg::demosaic_ppg;
use rawler::imgop::xyz::Illuminant;
use rawler::pixarray::RgbF32;

// use multiversion::multiversion;
use rayon::prelude::*;

// Constant matrix for converting ProPhoto RGB to XYZ(D50):
// http://www.brucelindbloom.com/Eqn_RGB_XYZ_Matrix.html
#[allow(clippy::excessive_precision)]
const PROPHOTORGB_TO_XYZ_D50: [[f32; 3]; 3] = [
  [0.7976749, 0.1351917, 0.0313534],
  [0.2880402, 0.7118741, 0.0000857],
  [0.0000000, 0.0000000, 0.8252100],
];

// #[multiversion]
// #[clone(target = "[x86|x86_64]+avx+avx2")]
// #[clone(target = "x86+sse")]
fn rgb_to_prophoto_rgb_with_wb(rgb: &mut RgbF32, wb_coeff: &[f32; 4], xyz2cam: [[f32; 3]; 4]) {
  let rgb2cam = normalize(multiply(&xyz2cam, &PROPHOTORGB_TO_XYZ_D50));
  let cam2rgb = pseudo_inverse(rgb2cam);

  rgb.for_each(|pix| {
    // We apply wb coeffs on the fly
    let r = pix[0] * wb_coeff[0];
    let g = pix[1] * wb_coeff[1];
    let b = pix[2] * wb_coeff[2];
    let rgb = [
      cam2rgb[0][0] * r + cam2rgb[0][1] * g + cam2rgb[0][2] * b,
      cam2rgb[1][0] * r + cam2rgb[1][1] * g + cam2rgb[1][2] * b,
      cam2rgb[2][0] * r + cam2rgb[2][1] * g + cam2rgb[2][2] * b,
    ];
    // We do not apply gamma correction
    clip_euclidean_norm_avg(&rgb)
  })
}

/// Collect iterator into array
fn collect_array<T, I, const N: usize>(itr: I) -> [T; N]
  where
      T: Default + Copy,
      I: IntoIterator<Item = T>,
{
  let mut res = [T::default(); N];
  for (it, elem) in res.iter_mut().zip(itr) {
    *it = elem
  }

  res
}

/// Correct data by blacklevel and whitelevel on CFA (bayer) data.
/// This version is optimized vor vectorization, so please check
/// modifications on godbolt before committing.
// #[multiversion]
// #[clone(target = "[x86|x86_64]+avx+avx2")]
// #[clone(target = "x86+sse")]
fn correct_blacklevel(raw: &mut [f32], width: usize, _height: usize, blacklevel: &[f32; 4], whitelevel: &[f32; 4]) {
  //assert_eq!(width % 2, 0, "width is {}", width);
  //assert_eq!(height % 2, 0, "height is {}", height);
  // max value can be pre-computed for all channels.
  let max = [
    whitelevel[0] - blacklevel[0],
    whitelevel[1] - blacklevel[1],
    whitelevel[2] - blacklevel[2],
    whitelevel[3] - blacklevel[3],
  ];
  let clip = |v: f32| {
    if v.is_sign_negative() {
      0.0
    } else {
      v
    }
  };
  // Process two bayer lines at once.
  raw.par_chunks_exact_mut(width * 2).for_each(|lines| {
    // It's bayer data, so we have two lines for sure.
    let (line0, line1) = lines.split_at_mut(width);
    //line0.array_chunks_mut::<2>().zip(line1.array_chunks_mut::<2>()).for_each(|(a, b)| {
    line0.chunks_exact_mut(2).zip(line1.chunks_exact_mut(2)).for_each(|(a, b)| {
      a[0] = clip(a[0] - blacklevel[0]) / max[0];
      a[1] = clip(a[1] - blacklevel[1]) / max[1];
      b[0] = clip(b[0] - blacklevel[2]) / max[2];
      b[1] = clip(b[1] - blacklevel[3]) / max[3];
    });
  });
}

// #[multiversion]
// #[clone(target = "[x86|x86_64]+avx+avx2")]
// #[clone(target = "x86+sse")]
fn raw_u16_to_float(pix: &[u16]) -> Vec<f32> {
  pix.iter().copied().map(f32::from).collect()
}

fn correct_raw(pixels: &[u16], params: &DevelopParams) -> Result<(Vec<f32>, Dim2)> {
  let black_level: [f32; 4] = match params.blacklevel.levels.len() {
    1 => Ok(collect_array(iter::repeat(params.blacklevel.levels[0].as_f32()))),
    4 => Ok(collect_array(params.blacklevel.levels.iter().map(|p| p.as_f32()))),
    c => Err(format!("Black level sample count of {} is invalid", c)),
  }?;
  let white_level: [f32; 4] = match params.whitelevel.len() {
    1 => Ok(collect_array(iter::repeat(params.whitelevel[0] as f32))),
    4 => Ok(collect_array(params.whitelevel.iter().map(|p| *p as f32))),
    c => Err(format!("White level sample count of {} is invalid", c)),
  }?;

  let mut pixels = raw_u16_to_float(pixels);

  correct_blacklevel(&mut pixels, params.width, params.height, &black_level, &white_level);
  Ok((pixels, Dim2::new(params.width, params.height)))
}

pub fn get_raw<P: AsRef<Path>>(path: P, params: RawDecodeParams) -> Result<(Vec<u16>, Dim2)> {
  let mut raw_file = BufReader::new(File::open(&path).unwrap());
  // .map_err(|e| RawlerError::with_io_error("load buffer", &path, e))?);

  // Read whole raw file
  // TODO: Large input file bug, we need to test the raw file before open it
  let in_buffer = Buffer::new(&mut raw_file).unwrap(); // ?;

  let mut rawfile = in_buffer.into();

  // Get decoder or return
  let decoder = get_decoder(&mut rawfile).unwrap(); // ?;
  //decoder.decode_metadata(&mut rawfile)?;
  let rawimage = decoder.raw_image(&mut rawfile, params, false).unwrap(); // ?;
  let params = rawimage.develop_params().unwrap(); // ?;
  let buf = match rawimage.data {
    RawImageData::Integer(buf) => buf,
    RawImageData::Float(_) => todo!(),
  };
  assert_eq!(rawimage.cpp, 1);
  let (pixels, dim) = correct_raw(&buf, &params).unwrap();
  let output = rescale_f32_to_u16(&pixels, 0, u16::MAX);
  Ok((output, dim))
}

/// Develop a RAW image to ProPhoto RGB
fn develop_raw_prophoto_rgb(pixels: &[u16], params: &DevelopParams) -> Result<(Vec<f32>, Dim2)> {
  let (pixels, _) = correct_raw(pixels, params).unwrap();

  let raw_size = Rect::new_with_points(Point::zero(), Point::new(params.width, params.height));
  let active_area = params.active_area.unwrap_or(raw_size);
  let crop_area = params.crop_area.unwrap_or(active_area).adapt(&active_area);
  let rgb = demosaic_ppg(&pixels, Dim2::new(params.width, params.height), params.cfa.clone(), active_area);
  let mut cropped_pixels = if raw_size.d != crop_area.d { rgb.crop(crop_area) } else { rgb };

  // Convert to ProPhoto RGB from XYZ
  let wb_coeff: [f32; 4] = params.wb_coeff;
  // log::debug!("Develop raw, wb: {:?}, black: {:?}, white: {:?}", wb_coeff, black_level, white_level);

  //Color Space Conversion
  let xyz2cam = params
      .color_matrices
      .iter()
      .find(|m| m.illuminant == Illuminant::D65)
      .ok_or("Illuminant matrix D65 not found")?
      .matrix;
  rgb_to_prophoto_rgb_with_wb(&mut cropped_pixels, &wb_coeff, xyz2cam);

  // Flatten into Vec<f32>
  let prophoto_rgb: Vec<f32> = cropped_pixels.into_inner().into_iter().flatten().collect();

  assert_eq!(prophoto_rgb.len(), crop_area.d.w * crop_area.d.h * 3);

  Ok((prophoto_rgb, crop_area.d))
}

pub fn raw_to_prophoto_rgb<P: AsRef<Path>>(path: P, params: RawDecodeParams) -> Result<(Vec<u16>, Dim2)> {
  let mut raw_file = BufReader::new(File::open(&path).unwrap());
      // .map_err(|e| RawlerError::with_io_error("load buffer", &path, e))?);

  // Read whole raw file
  // TODO: Large input file bug, we need to test the raw file before open it
  let in_buffer = Buffer::new(&mut raw_file).unwrap(); // ?;

  let mut rawfile = in_buffer.into();

  // Get decoder or return
  let decoder = get_decoder(&mut rawfile).unwrap(); // ?;
  //decoder.decode_metadata(&mut rawfile)?;
  let rawimage = decoder.raw_image(&mut rawfile, params, false).unwrap(); // ?;
  let params = rawimage.develop_params().unwrap(); // ?;
  let buf = match rawimage.data {
    RawImageData::Integer(buf) => buf,
    RawImageData::Float(_) => todo!(),
  };
  assert_eq!(rawimage.cpp, 1);
  let (prophoto_rgbf, dim) = develop_raw_prophoto_rgb(&buf, &params)?;
  let output = rescale_f32_to_u16(&prophoto_rgbf, 0, u16::MAX);
  Ok((output, dim))
}

