/* TruncatedFileException.java
   Copyright (C) 2007 Matthew Flaschen
   Copyright (C) 2011 Red Hat, Inc.

   This file is part of IcedTea

   IcedTea is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2, or (at your option)
   any later version.

   IcedTea is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with GNU Classpath; see the file COPYING.  If not, write to the
   Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
   02110-1301 USA.

   Linking this library statically or dynamically with other modules is
   making a combined work based on this library.  Thus, the terms and
   conditions of the GNU General Public License cover the whole
   combination.

   As a special exception, the copyright holders of this library give you
   permission to link this library with independent modules to produce an
   executable, regardless of the license terms of these independent
   modules, and to copy and distribute the resulting executable under
   terms of your choice, provided that you also meet, for each linked
   independent module, the terms and conditions of the license of that
   module.  An independent module is a module which is not derived from
   or based on this library.  If you modify this library, you may extend
   this exception to your version of the library, but you are not
   obligated to do so.  If you do not wish to do so, delete this
   exception statement from your version. */

package com.sun.image.codec.jpeg;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

public class TruncatedFileException extends RuntimeException {

    private final BufferedImage bufferedImage;

    private final Raster raster;

    /**
     * Creates a new exception with the specified {@link BufferedImage}
     * containing the partially read image data.
     *
     * @param bufferedImage the partially decoded image (may be null).
     */
    public TruncatedFileException(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
        this.raster = null;
    }

    /**
     * Creates a new exception with the specified {@link Raster}
     * containing the partially read image data.
     *
     * @param raster the partially decoded image (may be null).
     */
    public TruncatedFileException(Raster raster) {
        this.raster = raster;
        this.bufferedImage = null;
    }

    /**
     * Returns the partially read data as a {@link BufferedImage}
     * if available, or {@code null} if not.
     *
     * @return the partially read data.
     */
    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    /**
     * Returns the partially read data as a {@link Raster}
     * if available, or {@code null} if not.
     *
     * @return the partially read data.
     */
    public Raster getRaster() {
        return raster;
    }

}
