#include <jni.h>

#include "LZLibRaw.h"
#include "LC_JNIUtils.h"

#include <iostream>

#ifndef LC_AUTO_DEP
#include "javah/com_lightcrafts_image_libs_LibRaw.h"
#endif

using namespace std;
using namespace LightCrafts;

JNIEXPORT void JNICALL
Java_com_lightcrafts_image_libs_LibRaw_createLibRawObject(JNIEnv *env,
                                                          jobject obj) {
  env->SetLongField(
      obj, env->GetFieldID(env->GetObjectClass(obj), "libRawObject", "J"),
      (long)new LZLibRaw());
}

JNIEXPORT void JNICALL
Java_com_lightcrafts_image_libs_LibRaw_disposeLibRawObject(JNIEnv *env,
                                                           jobject obj) {
  jclass libRawClass = env->GetObjectClass(obj);
  jfieldID libRawObjectID = env->GetFieldID(libRawClass, "libRawObject", "J");
  auto *libRaw = (LZLibRaw *)env->GetLongField(obj, libRawObjectID);
  delete libRaw;
  env->SetLongField(obj, libRawObjectID, 0);
}

JNIEXPORT void JNICALL
Java_com_lightcrafts_image_libs_LibRaw_recycle(JNIEnv *env, jobject obj) {
  jclass libRawClass = env->GetObjectClass(obj);
  jfieldID libRawObjectID = env->GetFieldID(libRawClass, "libRawObject", "J");
  auto *libRaw = (LZLibRaw *)env->GetLongField(obj, libRawObjectID);
  if (libRaw != nullptr) {
    libRaw->recycle();
  }
}

#define SET_INT(name, value)                                                   \
  env->SetIntField(obj, env->GetFieldID(env->GetObjectClass(obj), #name, "I"), \
                   libRaw->imgdata.value);

#define SET_LONG(name, value)                                                  \
  env->SetLongField(obj,                                                       \
                    env->GetFieldID(env->GetObjectClass(obj), #name, "J"),     \
                    libRaw->imgdata.value);

#define SET_FLOAT(name, value)                                                 \
  env->SetFloatField(obj,                                                      \
                     env->GetFieldID(env->GetObjectClass(obj), #name, "F"),    \
                     libRaw->imgdata.value);

#define SET_STRING(name, value)                                                \
  env->SetObjectField(                                                         \
      obj, env->GetFieldID(libRawClass, #name, "Ljava/lang/String;"),          \
      env->NewStringUTF(libRaw->imgdata.value));

#define SET_ARRAY(name, sig, type, value, size)                                \
  {                                                                            \
    jfieldID id = env->GetFieldID(libRawClass, #name, sig);                    \
    j##type##Array jarr = (j##type##Array)env->GetObjectField(obj, id);        \
    type *arr = (type *)env->GetPrimitiveArrayCritical(jarr, 0);               \
    for (int i = 0; i < size; i++) {                                           \
      arr[i] = libRaw->imgdata.value[i];                                       \
    }                                                                          \
    env->ReleasePrimitiveArrayCritical(jarr, arr, 0);                          \
  }

#define SET_ARRAY2D(name, sig, type, value, rows, cols)                        \
  {                                                                            \
    jfieldID id = env->GetFieldID(libRawClass, #name, sig);                    \
    jobjectArray jrow = (jobjectArray)env->GetObjectField(obj, id);            \
    for (int i = 0; i < rows; i++) {                                           \
      jintArray jarr = (jintArray)env->GetObjectArrayElement(jrow, i);         \
      type *arr = (type *)env->GetPrimitiveArrayCritical(jarr, 0);             \
      for (int j = 0; j < cols; j++) {                                         \
        arr[j] = libRaw->imgdata.value[i][j];                                  \
      }                                                                        \
      env->ReleasePrimitiveArrayCritical(jarr, arr, 0);                        \
    }                                                                          \
  }

JNIEXPORT jint JNICALL Java_com_lightcrafts_image_libs_LibRaw_openFile(
    JNIEnv *env, jobject obj, jstring jFileName) {
  jclass libRawClass = env->GetObjectClass(obj);
  auto *libRaw = (LibRaw *)env->GetLongField(
      obj, env->GetFieldID(libRawClass, "libRawObject", "J"));

  if (libRaw == nullptr)
    return -1;

  // NOTE: if these are not specified here they're not going to be picked up by
  // unpack
  libRaw->imgdata.params.user_flip = 0;
  libRaw->imgdata.params.use_fuji_rotate = 0;

  jstring_to_c const cFileName(env, jFileName);
  int status = libRaw->open_file(cFileName);

  SET_INT(progress_flags, progress_flags);
  SET_INT(process_warnings, process_warnings);

  // libraw_iparams_t

  SET_STRING(make, idata.make);
  SET_STRING(model, idata.model);

  SET_INT(raw_count, idata.raw_count);
  SET_INT(dng_version, idata.dng_version);
  SET_INT(colors, idata.colors);
  SET_INT(filters, idata.filters);
  SET_STRING(cdesc, idata.cdesc);

  // libraw_image_sizes_t

  SET_INT(raw_height, sizes.raw_height);
  SET_INT(raw_width, sizes.raw_width);
  SET_INT(height, sizes.height);
  SET_INT(width, sizes.width);
  SET_INT(top_margin, sizes.top_margin);
  SET_INT(left_margin, sizes.left_margin);
  SET_INT(iheight, sizes.iheight);
  SET_INT(iwidth, sizes.iwidth);
  SET_FLOAT(pixel_aspect, sizes.pixel_aspect);
  SET_INT(flip, sizes.flip);

  // libraw_colordata_t

  SET_ARRAY2D(white, "[[I", int, color.white, 8, 8);

  SET_ARRAY(cam_mul, "[F", float, color.cam_mul, 4);
  SET_ARRAY(pre_mul, "[F", float, color.pre_mul, 4);

  SET_ARRAY2D(cmatrix, "[[F", float, color.cmatrix, 3, 4);
  SET_ARRAY2D(rgb_cam, "[[F", float, color.rgb_cam, 3, 4);
  SET_ARRAY2D(cam_xyz, "[[F", float, color.cam_xyz, 4, 3);

  // TODO: ushort      curve[0x10000];

  SET_INT(black, color.black);
  SET_ARRAY(cblack, "[I", int, color.cblack, 8);
  SET_INT(maximum, color.maximum);
  SET_ARRAY(channel_maximum, "[I", int, color.linear_max, 4);

  // TODO: ph1_t       phase_one_data

  SET_FLOAT(flash_used, color.flash_used);
  SET_FLOAT(canon_ev, color.canon_ev);
  SET_STRING(model2, color.model2);

  // TODO: void        *profile

  SET_INT(profile_length, color.profile_length);

  SET_FLOAT(iso_speed, other.iso_speed);
  SET_FLOAT(shutter, other.shutter);
  SET_FLOAT(aperture, other.aperture);
  SET_FLOAT(focal_len, other.focal_len);
  SET_LONG(timestamp, other.timestamp);
  SET_INT(shot_order, other.shot_order);
  SET_ARRAY(gpsdata, "[I", int, other.gpsdata, 32);
  SET_STRING(desc, other.desc);
  SET_STRING(artist, other.artist);

  // libraw_thumbnail_t

  SET_INT(tformat, thumbnail.tformat);
  SET_INT(twidth, thumbnail.twidth);
  SET_INT(theight, thumbnail.theight);
  SET_INT(tlength, thumbnail.tlength);
  SET_INT(tcolors, thumbnail.tcolors);

  // TODO: char       *thumb

  if (libRaw->imgdata.idata.filters) {
    char filter_pattern[17];
    if (!libRaw->imgdata.idata.cdesc[3])
      libRaw->imgdata.idata.cdesc[3] = 'G';
    for (int i = 0; i < 16; i++)
      filter_pattern[i] =
          libRaw->imgdata.idata.cdesc[libRaw->FC(i >> 1, i & 1)];
    filter_pattern[10] = '\0';
    env->SetObjectField(
        obj,
        env->GetFieldID(libRawClass, "filter_pattern", "Ljava/lang/String;"),
        env->NewStringUTF(filter_pattern));
  }

  return status;
}

struct callback {
  JNIEnv *env;
  jobject obj;
  jmethodID mid;
};

int my_progress_callback(void *data, enum LibRaw_progress p, int iteration,
                         int expected) {
  char *passed_string = (char *)data;
  clock_t time = clock();
  fprintf(stderr, "Callback: %s  pass %d of %d, data passed: %p, time: %f\n",
          libraw_strprogress(p), iteration, expected, passed_string,
          time / (float)CLOCKS_PER_SEC);

  //    if (data != nullptr) {
  //        callback *c = (callback *) data;
  //        return c->env->CallIntMethod(c->obj, c->mid, p, iteration,
  //        expected);
  //    }
  return 0; // continue processing
}

extern "C" void interpolate_bayer(void *ptr) {
  auto *processor = (LZLibRaw *)ptr;
  processor->lz_interpolate();
}

JNIEXPORT jshortArray JNICALL
Java_com_lightcrafts_image_libs_LibRaw_unpackImage(JNIEnv *env, jobject obj,
                                                   jboolean interpolate,
                                                   jboolean half_size) {
  jclass libRawClass = env->GetObjectClass(obj);
  auto *libRaw = (LZLibRaw *)env->GetLongField(
      obj, env->GetFieldID(libRawClass, "libRawObject", "J"));
  if (libRaw == nullptr)
    return nullptr;

  int status = libRaw->unpack();
  if (status != LIBRAW_SUCCESS)
    return nullptr;

  // params
  if (half_size)
    libRaw->imgdata.params.half_size = 1;  // -h
  else if (interpolate)
    libRaw->imgdata.params.four_color_rgb = 1;  // -f
  else
    libRaw->set_interpolate_bayer_handler(interpolate_bayer);

//  libRaw->imgdata.params.use_camera_wb = 1;  // -w
  libRaw->imgdata.params.use_camera_matrix = 1;  // -M

  libRaw->imgdata.params.use_fuji_rotate = 0;  // -j
  libRaw->imgdata.params.highlight = 1;  // -H 1
  libRaw->imgdata.params.user_flip = 0;  // -t 0
  libRaw->imgdata.params.gamm[0] = libRaw->imgdata.params.gamm[1] = 1;  // -g 1 1
  libRaw->imgdata.params.no_auto_bright = 1;  // -W
  libRaw->imgdata.params.output_bps = 16;  // -6
  libRaw->imgdata.params.output_color = 4; // -o 4 (Kodak ProPhoto RGB D65)
  // libRaw->imgdata.params.output_tiff = 1;  // -T

  jclass cls = env->GetObjectClass(obj);
  jmethodID mid = env->GetMethodID(cls, "progress", "(III)I");
  callback data = {env, obj, mid};
  if (mid != nullptr)
    libRaw->set_progress_handler(my_progress_callback, (void *)&data);

  status = libRaw->dcraw_process();
  if (status != LIBRAW_SUCCESS)
    return nullptr;

  libraw_processed_image_t *image = libRaw->dcraw_make_mem_image(&status);
  if (image == nullptr)
    return nullptr;

  constexpr int bands = 3;
  const int buffer_size = bands * image->width * image->height;
  jshortArray jimage_data = env->NewShortArray((jsize)buffer_size);
  if (jimage_data != nullptr) {
    env->SetShortArrayRegion(jimage_data, 0, buffer_size,
                             (jshort *)image->data);
  }
  free(image);
  return jimage_data;
}

JNIEXPORT jbyteArray JNICALL
Java_com_lightcrafts_image_libs_LibRaw_unpackThumb(JNIEnv *env, jobject obj) {
  jclass libRawClass = env->GetObjectClass(obj);
  auto *libRaw = (LZLibRaw *)env->GetLongField(
      obj, env->GetFieldID(libRawClass, "libRawObject", "J"));

  if (libRaw == nullptr)
    return nullptr;

  int status = libRaw->unpack_thumb();
  if (status != LIBRAW_SUCCESS)
    return nullptr;

  libraw_processed_image_t *thumb = libRaw->dcraw_make_mem_thumb(&status);
  if (thumb == nullptr)
    return nullptr;

  const int buffer_size = thumb->data_size;
  jbyteArray jthumbnail_data = env->NewByteArray((jsize)buffer_size);
  if (jthumbnail_data != nullptr) {
    env->SetByteArrayRegion(jthumbnail_data, 0, buffer_size,
                            (jbyte *)thumb->data);
    SET_INT(tformat, thumbnail.tformat);
  }
  free(thumb);
  return jthumbnail_data;
}
