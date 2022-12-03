/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 28, 2005
 * Time: 10:49:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class Segment {
    synchronized public static native byte[] segmentImage(byte[] image, int channels, int height, int width);
}
