/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * An <code>ImageOrientation</code> specifies the orientation of an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public enum ImageOrientation {

    /**
     * The 0th row represents the visual top of the image, and the 0th column
     * represents the visual left-hand side.
     * (This means the image is landscape.)
     */
    ORIENTATION_LANDSCAPE( TIFF_ORIENTATION_LANDSCAPE ) {       // TIFF 1
        @Override
        public ImageOrientation get90CW() {
            return ORIENTATION_90CCW;                           // TIFF 6
        }

        @Override
        public ImageOrientation getVFlip() {
            return ORIENTATION_VFLIP;                           // TIFF 4
        }

        @Override
        public RenderedImage correct(RenderedImage src) {
            return src;
        }

        @Override
        public int getRotationTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_LANDSCAPE:                     // TIFF 1
                    return 0;
                case ORIENTATION_180:                           // TIFF 3
                    return 180;
                case ORIENTATION_90CCW:                         // TIFF 6
                    return -90;
                case ORIENTATION_90CW:                          // TIFF 8
                    return 90;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public boolean isRotatableTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_180:                           // TIFF 3
                case ORIENTATION_90CCW:                         // TIFF 6
                case ORIENTATION_90CW:                          // TIFF 8
                case ORIENTATION_LANDSCAPE:                     // TIFF 1
                    return true;
                default:
                    return false;
            }
        }
    },

    /**
     * The 0th row represents the visual bottom of the image, and the 0th
     * column represents the visual right-hand side.
     * (This means the image is rotated 180 degrees.)
     */
    ORIENTATION_180( TIFF_ORIENTATION_180 ) {                   // TIFF 3
        @Override
        public ImageOrientation get90CW() {
            return ORIENTATION_90CW;                            // TIFF 8
        }

        @Override
        public ImageOrientation getVFlip() {
            return ORIENTATION_SEASCAPE;                        // TIFF 2
        }

        @Override
        public RenderedImage correct(RenderedImage src) {
            final int w = src.getWidth();
            final int h = src.getHeight();

            final var xform = AffineTransform.getTranslateInstance(w, h);
            xform.quadrantRotate(2);

            return transformedImage(src, w, h, xform);
        }

        @Override
        public int getRotationTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_LANDSCAPE:                     // TIFF 1
                    return 180;
                case ORIENTATION_180:                           // TIFF 3
                    return 0;
                case ORIENTATION_90CCW:                         // TIFF 6
                    return 90;
                case ORIENTATION_90CW:                          // TIFF 8
                    return -90;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public boolean isRotatableTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_180:                           // TIFF 3
                case ORIENTATION_90CCW:                         // TIFF 6
                case ORIENTATION_90CW:                          // TIFF 8
                case ORIENTATION_LANDSCAPE:                     // TIFF 1
                    return true;
                default:
                    return false;
            }
        }
    },

    /**
     * The 0th row represents the visual right-hand side of the image, and the
     * 0th column represents the visual top.
     * (This means the image is rotated 90 CCW.)
     */
    ORIENTATION_90CCW( TIFF_ORIENTATION_90CCW ) {               // TIFF 6
        @Override
        public ImageOrientation get90CW() {
            return ORIENTATION_180;                             // TIFF 3
        }

        @Override
        public ImageOrientation getVFlip() {
            return ORIENTATION_90CW_VFLIP;                      // TIFF 7
        }

        @Override
        public RenderedImage correct(RenderedImage src) {
            final int w = src.getWidth();
            final int h = src.getHeight();

            final var xform = AffineTransform.getTranslateInstance(h, 0);
            xform.quadrantRotate(1);

            return transformedImage(src, h, w, xform);
        }

        @Override
        public int getRotationTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_LANDSCAPE:                     // TIFF 1
                    return 90;
                case ORIENTATION_180:                           // TIFF 3
                    return -90;
                case ORIENTATION_90CCW:                         // TIFF 6
                    return 0;
                case ORIENTATION_90CW:                          // TIFF 8
                    return 180;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public boolean isRotatableTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_180:                           // TIFF 3
                case ORIENTATION_90CCW:                         // TIFF 6
                case ORIENTATION_90CW:                          // TIFF 8
                case ORIENTATION_LANDSCAPE:                     // TIFF 1
                    return true;
                default:
                    return false;
            }
        }
    },

    /**
     * The 0th row represents the visual left-hand side of the image, and the
     * 0th column represents the visual top.
     * (This means the image is rotated 90 CCW and vertically flipped.)
     */
    ORIENTATION_90CCW_VFLIP( TIFF_ORIENTATION_90CCW_VFLIP ) {   // TIFF 5
        @Override
        public ImageOrientation get90CW() {
            return ORIENTATION_SEASCAPE;                        // TIFF 2
        }

        @Override
        public ImageOrientation getVFlip() {
            return ORIENTATION_90CW;                            // TIFF 8
        }

        @Override
        public RenderedImage correct(RenderedImage src) {
            final int w = src.getWidth();
            final int h = src.getHeight();

            final var xform = AffineTransform.getQuadrantRotateInstance(1);
            xform.scale(1, -1);

            return transformedImage(src, h, w, xform);
        }

        @Override
        public int getRotationTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_90CCW_VFLIP:                   // TIFF 5
                    return 0;
                case ORIENTATION_90CW_VFLIP:                    // TIFF 7
                    return 180;
                case ORIENTATION_SEASCAPE:                      // TIFF 2
                    return 90;
                case ORIENTATION_VFLIP:                         // TIFF 4
                    return -90;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public boolean isRotatableTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_90CCW_VFLIP:                   // TIFF 5
                case ORIENTATION_90CW_VFLIP:                    // TIFF 7
                case ORIENTATION_SEASCAPE:                      // TIFF 2
                case ORIENTATION_VFLIP:                         // TIFF 4
                    return true;
                default:
                    return false;
            }
        }
    },

    /**
     * The 0th row represents the visual left-hand side of the image, and the
     * 0th column represents the visual bottom.
     * (This means the image is rotated 90 CW.)
     */
    ORIENTATION_90CW( TIFF_ORIENTATION_90CW ) {                 // TIFF 8
        @Override
        public ImageOrientation get90CW() {
            return ORIENTATION_LANDSCAPE;                       // TIFF 1
        }

        @Override
        public ImageOrientation getVFlip() {
            return ORIENTATION_90CCW_VFLIP;                     // TIFF 5
        }

        @Override
        public RenderedImage correct(RenderedImage src) {
            final int w = src.getWidth();
            final int h = src.getHeight();

            final var xform = AffineTransform.getTranslateInstance(0, w);
            xform.quadrantRotate(3);

            return transformedImage(src, h, w, xform);
        }

        @Override
        public int getRotationTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_LANDSCAPE:                     // TIFF 1
                    return -90;
                case ORIENTATION_180:                           // TIFF 3
                    return 90;
                case ORIENTATION_90CCW:                         // TIFF 6
                    return 180;
                case ORIENTATION_90CW:                          // TIFF 8
                    return 0;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public boolean isRotatableTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_180:                           // TIFF 3
                case ORIENTATION_90CCW:                         // TIFF 6
                case ORIENTATION_90CW:                          // TIFF 8
                case ORIENTATION_LANDSCAPE:                     // TIFF 1
                    return true;
                default:
                    return false;
            }
        }
    },

    /**
     * The 0th row represents the visual right-hand side of the image, and the
     * 0th column represents the visual bottom.
     * (This means the image is rotated 90 CW and horizontally flipped.)
     */
    ORIENTATION_90CW_VFLIP( TIFF_ORIENTATION_90CW_VFLIP ) {     // TIFF 7
        @Override
        public ImageOrientation get90CW() {
            return ORIENTATION_VFLIP;                           // TIFF 4
        }

        @Override
        public ImageOrientation getVFlip() {
            return ORIENTATION_90CCW;                           // TIFF 6
        }

        @Override
        public RenderedImage correct(RenderedImage src) {
            final int w = src.getWidth();
            final int h = src.getHeight();

            final var xform = AffineTransform.getTranslateInstance(h, w);
            xform.quadrantRotate(3);
            xform.scale(1, -1);

            return transformedImage(src, h, w, xform);
        }

        @Override
        public int getRotationTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_90CCW_VFLIP:                   // TIFF 5
                    return 180;
                case ORIENTATION_90CW_VFLIP:                    // TIFF 7
                    return 0;
                case ORIENTATION_SEASCAPE:                      // TIFF 2
                    return -90;
                case ORIENTATION_VFLIP:                         // TIFF 4
                    return 90;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public boolean isRotatableTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_90CCW_VFLIP:                   // TIFF 5
                case ORIENTATION_90CW_VFLIP:                    // TIFF 7
                case ORIENTATION_SEASCAPE:                      // TIFF 2
                case ORIENTATION_VFLIP:                         // TIFF 4
                    return true;
                default:
                    return false;
            }
        }
    },

    /**
     * The 0th row represents the visual top of the image, and the 0th column
     * represents the visual right-hand side.
     * (This means the image is seascape.)
     */
    ORIENTATION_SEASCAPE( TIFF_ORIENTATION_SEASCAPE ) {         // TIFF 2
        @Override
        public ImageOrientation get90CW() {
            return ORIENTATION_90CW_VFLIP;                      // TIFF 7
        }

        @Override
        public ImageOrientation getVFlip() {
            return ORIENTATION_180;                             // TIFF 3
        }

        @Override
        public RenderedImage correct(RenderedImage src) {
            final int w = src.getWidth();
            final int h = src.getHeight();

            final var xform = AffineTransform.getTranslateInstance(w, 0);
            xform.scale(-1, 1);

            return transformedImage(src, w, h, xform);
        }

        @Override
        public int getRotationTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_90CCW_VFLIP:                   // TIFF 5
                    return -90;
                case ORIENTATION_90CW_VFLIP:                    // TIFF 7
                    return 90;
                case ORIENTATION_SEASCAPE:                      // TIFF 2
                    return 0;
                case ORIENTATION_VFLIP:                         // TIFF 4
                    return 180;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public boolean isRotatableTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_90CCW_VFLIP:                   // TIFF 5
                case ORIENTATION_90CW_VFLIP:                    // TIFF 7
                case ORIENTATION_SEASCAPE:                      // TIFF 2
                case ORIENTATION_VFLIP:                         // TIFF 4
                    return true;
                default:
                    return false;
            }
        }
    },

    /**
     * The 0th row represents the visual bottom of the image, and the 0th
     * column represents the visual left-hand side.
     * (This means the image is vertically flipped.)
     */
    ORIENTATION_VFLIP( TIFF_ORIENTATION_VFLIP ) {               // TIFF 4
        @Override
        public ImageOrientation get90CW() {
            return ORIENTATION_90CCW_VFLIP;                     // TIFF 5
        }

        @Override
        public ImageOrientation getVFlip() {
            return ORIENTATION_LANDSCAPE;                       // TIFF 1
        }

        @Override
        public RenderedImage correct(RenderedImage src) {
            final int w = src.getWidth();
            final int h = src.getHeight();

            final var xform = AffineTransform.getTranslateInstance(0, h);
            xform.scale(1, -1);

            return transformedImage(src, w, h, xform);
        }

        @Override
        public int getRotationTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_90CCW_VFLIP:                   // TIFF 5
                    return 90;
                case ORIENTATION_90CW_VFLIP:                    // TIFF 7
                    return -90;
                case ORIENTATION_SEASCAPE:                      // TIFF 2
                    return 180;
                case ORIENTATION_VFLIP:                         // TIFF 4
                    return 0;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public boolean isRotatableTo( ImageOrientation o ) {
            switch ( o ) {
                case ORIENTATION_90CCW_VFLIP:                   // TIFF 5
                case ORIENTATION_90CW_VFLIP:                    // TIFF 7
                case ORIENTATION_SEASCAPE:                      // TIFF 2
                case ORIENTATION_VFLIP:                         // TIFF 4
                    return true;
                default:
                    return false;
            }
        }
    },

    /**
     * It is unknown what the 0th row and 0th column represent.
     */
    ORIENTATION_UNKNOWN( TIFF_ORIENTATION_UNKNOWN ) {           // TIFF 9
        @Override
        public ImageOrientation get90CW() {
            return ORIENTATION_UNKNOWN;                         // TIFF 9
        }

        @Override
        public ImageOrientation getVFlip() {
            return ORIENTATION_UNKNOWN;                         // TIFF 9
        }

        @Override
        public RenderedImage correct(RenderedImage src) {
            return src;
        }

        @Override
        public int getRotationTo( ImageOrientation o ) {
            throw new IllegalArgumentException();
        }

        @Override
        public boolean isRotatableTo( ImageOrientation o ) {
            return false;
        }
    };

    private static @NotNull BufferedImage transformedImage(
            @NotNull RenderedImage src, int dstWidth, int dstHeight, AffineTransform xform) {
        final var cm = src.getColorModel();
        final var raster = cm.createCompatibleWritableRaster(dstWidth, dstHeight);
        final var dst = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
        final Graphics2D g = dst.createGraphics();
        g.drawRenderedImage(src, xform);
        g.dispose();
        return dst;
    }

    /**
     * Gets the orientation that is rotated 180 degrees from this orientation.
     *
     * @return Returns said orientation.
     */
    public final ImageOrientation get180() {
        return get90CW().get90CW();
    }

    /**
     * Gets the orientation that is rotated 90 degrees counter-clockwise from
     * this orientation.
     *
     * @return Returns said orientation.
     */
    public final ImageOrientation get90CCW() {
        return get180().get90CW();
    }

    /**
     * Gets the orientation that is rotated 90 degrees clockwise from this
     * orientation.
     *
     * @return Returns said orientation.
     */
    public abstract ImageOrientation get90CW();

    /**
     * Gets the orientation that is flipped horizontally from this orientation.
     *
     * @return Returns said orientation.
     */
    public final ImageOrientation getHFlip() {
        return get180().getVFlip();
    }

    /**
     * Gets the orientation that is flipped vertically from this orientation.
     *
     * @return Returns said orientation.
     */
    public abstract ImageOrientation getVFlip();

    public abstract RenderedImage correct(RenderedImage src);

    /**
     * Gets the rotation angle from this <code>ImageOrientation</code> to
     * another.
     *
     * @param o Another <code>ImageOrientation</code>.
     * @return Returns said rotation angle in degrees where positive angles are
     * clockwise.
     * @see #isRotatableTo(ImageOrientation)
     * @throws IllegalArgumentException if
     * {@link #isRotatableTo(ImageOrientation)} would return <code>false</code>
     * for the same argument.
     */
    public abstract int getRotationTo( ImageOrientation o );

    /**
     * Get the <code>ImageOrientation</code> for the given TIFF constant.
     *
     * @param tiffConstant The constant used in the TIFF specification to
     * encode orientation.
     * @return Returns said <code>ImageOrientation</code>.
     * @throws IllegalArgumentException if the constant is not in the range
     * [0,9].
     * @see #getTIFFConstant()
     */
    public static ImageOrientation getOrientationFor( int tiffConstant ) {
        switch ( tiffConstant ) {
            case 0: // As a special case, equate to landscape.
            case TIFF_ORIENTATION_LANDSCAPE:
                return ORIENTATION_LANDSCAPE;
            case TIFF_ORIENTATION_180:
                return ORIENTATION_180;
            case TIFF_ORIENTATION_90CCW:
                return ORIENTATION_90CCW;
            case TIFF_ORIENTATION_90CCW_VFLIP:
                return ORIENTATION_90CCW_VFLIP;
            case TIFF_ORIENTATION_90CW:
                return ORIENTATION_90CW;
            case TIFF_ORIENTATION_90CW_VFLIP:
                return ORIENTATION_90CW_VFLIP;
            case TIFF_ORIENTATION_SEASCAPE:
                return ORIENTATION_SEASCAPE;
            case TIFF_ORIENTATION_VFLIP:
                return ORIENTATION_VFLIP;
            case TIFF_ORIENTATION_UNKNOWN:
                return ORIENTATION_UNKNOWN;
            default:
                throw new IllegalArgumentException(
                    "orientation must be [0,9]"
                );
        }
    }

    /**
     * Gets the constant used in the TIFF specification to encode this
     * orientation.
     *
     * @return Returns an integer between 1 and 9, inclusive.
     * @see #getOrientationFor(int)
     */
    public short getTIFFConstant() {
        return m_tiffConstant;
    }

    /**
     * Tests whether this orientation is rotatable to another.
     *
     * @param o Another <code>ImageOrientation</code>.
     * @return Returns <code>true</code> only if this orientation can be
     * rotated to the other.
     * @see #getRotationTo(ImageOrientation)
     */
    public abstract boolean isRotatableTo( ImageOrientation o );

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Constructs an <code>ImageOrientation</code>.
     *
     * @param tiffConstant The constant used in the TIFF specification to
     * encode orientation.
     */
    ImageOrientation( short tiffConstant ) {
        assert tiffConstant >= 1 && tiffConstant <= 9;
        m_tiffConstant = tiffConstant;
    }

    /**
     * The constant used in the TIFF specification to encode this orientation.
     */
    private final short m_tiffConstant;
}
/* vim:set et sw=4 ts=4: */
