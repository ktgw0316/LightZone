/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import com.lightcrafts.image.ImageInfo;

/**
 * An <code>AuxiliaryImageInfo</code> is an abstract base class for classes
 * that need to hold auxiliary information for an image.
 * <p>
 * The reason for having a seperate class for this (as opposed to putting
 * auxiliary information directly into {@link ImageInfo}) is because:
 *  <ul>
 *    <li>
 *      The information can be specific for a given type of image and an
 *      image's type isn't known at the time an {@link ImageInfo} is
 *      constructed.
 *    </li>
 *    <li>
 *      Not every {@link ImageInfo} needs or has auxiliary information.
 *    </li>
 *  </ul>
 * Currently, this class is empty so {@link Object} could have been used
 * instead of having a whole new class.  However, this class serves as a
 * placeholder in case any data or methods need to be added in the future.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class AuxiliaryImageInfo {

}
/* vim:set et sw=4 ts=4: */
