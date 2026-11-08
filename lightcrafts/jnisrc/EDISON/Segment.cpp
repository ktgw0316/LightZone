/*
 *  minimal.cpp
 *  EDISON
 *
 *  Created by Fabio Riccardi on 4/27/05.
 *  Copyright 2005 __MyCompanyName__. All rights reserved.
 *
 */

#include <cstdint>
#include <climits>

#include "segm/msImageProcessor.h"

extern "C" int segment_image(
        const unsigned char* image, int channels, int height, int width,
        unsigned char* result, int resultCapacity) {
    if (!image || !result || (channels != 1 && channels != 3)
            || height <= 0 || width <= 0) {
        return -1;
    }
    if (height > INT_MAX / width || height * width > INT_MAX / channels) {
        return -1;
    }
    const auto imageSize = channels * height * width;
    if (resultCapacity < imageSize) {
        return -1;
    }
    msImageProcessor processor;

    if (channels == 1)
      processor.DefineImage(const_cast<byte*>(image), GRAYSCALE, height, width);
    else
      processor.DefineImage(const_cast<byte*>(image), COLOR, height, width);

    processor.SetSpeedThreshold(0.5);
    processor.Segment(4, 4.0, 20);

    processor.GetResults(reinterpret_cast<byte*>(result));
    return static_cast<int>(imageSize);
}
