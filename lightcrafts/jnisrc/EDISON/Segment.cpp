/*
 *  minimal.cpp
 *  EDISON
 *
 *  Created by Fabio Riccardi on 4/27/05.
 *  Copyright 2005 __MyCompanyName__. All rights reserved.
 *
 */

#include <jni.h>
#include <stdarg.h>
#include <stdlib.h>

#include "segm/msImageProcessor.h"

bool stop_flag = false;
int percentDone = 0;

void bgLogVar(const char *, va_list) { }

extern "C" JNIEXPORT jbyteArray JNICALL Java_com_lightcrafts_utils_Segment_segmentImage
  (JNIEnv *env, jclass clazz, jbyteArray image, jint channels, jint height, jint width)
{
    byte *data = (byte *) env->GetPrimitiveArrayCritical(image, 0);

    msImageProcessor processor;

    if (channels == 1)
      processor.DefineImage(data, GRAYSCALE, height, width);
    else
      processor.DefineImage(data, COLOR, height, width);

    env->ReleasePrimitiveArrayCritical(image, data, 0);

    processor.SetSpeedThreshold(0.5);
    processor.Segment(4, 4.0, 20, HIGH_SPEEDUP);

    int imageSize = height * width * channels;

    jbyteArray result = env->NewByteArray(imageSize);

    byte *out_data = (byte *) env->GetPrimitiveArrayCritical(result, 0);

    processor.GetResults(out_data);

    env->ReleasePrimitiveArrayCritical(result, out_data, 0);

    return result;
}
