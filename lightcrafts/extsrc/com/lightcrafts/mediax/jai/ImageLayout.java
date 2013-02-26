/*
 * $RCSfile: ImageLayout.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:09 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;

/**
 * A class describing the desired layout of an <code>OpImage</code>.
 *
 * <p> The <code>ImageLayout</code> class encapsulates three types of information about
 * an image:
 *
 * <ul>
 * <li> The image bounds, comprising the min X and Y coordinates,
 *      image width, and image height;
 * <li> The tile grid layout, comprising the tile grid X and Y offsets,
 *      the tile width, and the tile height; and
 * <li> The <code>SampleModel</code> and <code>ColorModel</code> of the image.
 * </ul>
 *
 * <p> Each of these parameters may be set individually, or left unset.
 * An unset parameter will cause the corresponding value of a given
 * <code>RenderedImage</code> to be used.  For example, the code:
 *
 * <pre>
 * ImageLayout layout;
 * RenderedImage im;
 *
 * int width = layout.getTileWidth(im);
 * </pre>
 *
 * will return the tile width of the <code>ImageLayout</code> if it is set,
 * or the tile width of the image <code>im</code> if it is not.
 *
 * <p> <code>ImageLayout</code> objects are primarily intended to be passed as part
 * of the <code>renderingHints</code> argument of the <code>create()</code> method of
 * <code>RenderedImageFactory</code>.  The <code>create()</code> method may remove parameter
 * settings that it cannot deal with, prior to passing the
 * <code>ImageLayout</code> to any <code>OpImage</code> constructors.  New <code>OpImage</code> subclasses
 * are not required to accept an <code>ImageLayout</code> parameter, but most will
 * at least need to synthesize one to be passed up the constructor
 * chain.
 *
 * <p> Methods that modify the state of an <code>ImageLayout</code> return a reference
 * to 'this' following the change.  This allows multiple modifications to
 * be made in a single expression.  This provides a way of modifying an
 * <code>ImageLayout</code> within a superclass constructor call.
 *
 */
public class ImageLayout extends Object implements Cloneable, Serializable {
    /** A bitmask to specify the validity of <code>minX</code>. */
    public static final int MIN_X_MASK = 0x1;
    /** A bitmask to specify the validity of <code>minY</code>. */
    public static final int MIN_Y_MASK = 0x2;
    /** A bitmask to specify the validity of <code>width</code>. */
    public static final int WIDTH_MASK = 0x4;
    /** A bitmask to specify the validity of <code>height</code>. */
    public static final int HEIGHT_MASK = 0x8;

    /** A bitmask to specify the validity of <code>tileGridXOffset</code>. */
    public static final int TILE_GRID_X_OFFSET_MASK = 0x10;
    /** A bitmask to specify the validity of <code>tileGridYOffset</code>. */
    public static final int TILE_GRID_Y_OFFSET_MASK = 0x20;
    /** A bitmask to specify the validity of <code>tileWidth</code>. */
    public static final int TILE_WIDTH_MASK = 0x40;
    /** A bitmask to specify the validity of <code>tileHeight</code>. */
    public static final int TILE_HEIGHT_MASK = 0x80;

    /** A bitmask to specify the validity of <code>sampleModel</code>. */
    public static final int SAMPLE_MODEL_MASK = 0x100;

    /** A bitmask to specify the validity of <code>colorModel</code>. */
    public static final int COLOR_MODEL_MASK = 0x200;

    /** The image's minimum X coordinate. */
    int minX = 0;

    /** The image's minimum Y coordinate. */
    int minY = 0;

    /** The image's <code>width</code>. */
    int width = 0;

    /** The image's height. */
    int height = 0;

    /** The X coordinate of tile (0, 0). */
    int tileGridXOffset = 0;

    /** The Y coordinate of tile (0, 0). */
    int tileGridYOffset = 0;

    /** The width of a tile. */
    int tileWidth = 0;

    /** The height of a tile. */
    int tileHeight = 0;

    /** The image's <code>SampleModel</code>. */
    transient SampleModel sampleModel = null;

    /** The image's <code>ColorModel</code>. */
    transient ColorModel colorModel = null;

    /** The 'or'-ed together valid bitmasks. */
    protected int validMask = 0;

    /** Constructs an <code>ImageLayout</code> with no parameters set. */
    public ImageLayout() {}

    /**
     * Constructs an <code>ImageLayout</code> with all its parameters set.
     * The <code>sampleModel</code> and <code>colorModel</code> parameters are ignored if null.
     *
     * @param minX the image's minimum X coordinate.
     * @param minY the image's minimum Y coordinate.
     * @param width the image's width.
     * @param height the image's height.
     * @param tileGridXOffset the X coordinate of tile (0, 0).
     * @param tileGridYOffset the Y coordinate of tile (0, 0).
     * @param tileWidth the width of a tile.
     * @param tileHeight the height of a tile.
     * @param sampleModel the image's <code>SampleModel</code>.
     * @param colorModel the image's <code>ColorModel</code>.
     */
    public ImageLayout(int minX,
                       int minY,
                       int width,
                       int height,
                       int tileGridXOffset,
                       int tileGridYOffset,
                       int tileWidth,
                       int tileHeight,
                       SampleModel sampleModel,
                       ColorModel colorModel) {
        setMinX(minX);
        setMinY(minY);
        setWidth(width);
        setHeight(height);
        setTileGridXOffset(tileGridXOffset);
        setTileGridYOffset(tileGridYOffset);
        setTileWidth(tileWidth);
        setTileHeight(tileHeight);
        if (sampleModel != null) {
            setSampleModel(sampleModel);
        }
        if (colorModel != null) {
            setColorModel(colorModel);
        }
    }


    /**
     * Constructs an <code>ImageLayout</code> with only the image dimension
     * parameters set.
     *
     * @param minX the image's minimum X coordinate.
     * @param minY the image's minimum Y coordinate.
     * @param width the image's width.
     * @param height the image's height.
     */
    public ImageLayout(int minX,
                       int minY,
                       int width,
                       int height) {
        setMinX(minX);
        setMinY(minY);
        setWidth(width);
        setHeight(height);
    }

    /**
     * Constructs an <code>ImageLayout</code> with its tile grid layout,
     * <code>SampleModel</code>, and <code>ColorModel</code> parameters set.
     * The <code>sampleModel</code> and <code>colorModel</code> parameters are ignored if null.
     *
     * @param tileGridXOffset the X coordinate of tile (0, 0).
     * @param tileGridYOffset the Y coordinate of tile (0, 0).
     * @param tileWidth the width of a tile.
     * @param tileHeight the height of a tile.
     * @param sampleModel the image's <code>SampleModel</code>.
     * @param colorModel the image's <code>ColorModel</code>.
     */
    public ImageLayout(int tileGridXOffset,
                       int tileGridYOffset,
                       int tileWidth,
                       int tileHeight,
                       SampleModel sampleModel,
                       ColorModel colorModel) {
        setTileGridXOffset(tileGridXOffset);
        setTileGridYOffset(tileGridYOffset);
        setTileWidth(tileWidth);
        setTileHeight(tileHeight);
        if (sampleModel != null) {
            setSampleModel(sampleModel);
        }
        if (colorModel != null) {
            setColorModel(colorModel);
        }
    }

    /**
     * Constructs an <code>ImageLayout</code> with all its parameters set
     * to equal those of a given <code>RenderedImage</code>.
     *
     * @param im a <code>RenderedImage</code> whose layout will be copied.
     */
    public ImageLayout(RenderedImage im) {
        this(im.getMinX(),
             im.getMinY(),
             im.getWidth(),
             im.getHeight(),
             im.getTileGridXOffset(),
             im.getTileGridYOffset(),
             im.getTileWidth(),
             im.getTileHeight(),
             im.getSampleModel(),
             im.getColorModel());
    }

    /**
     * Returns the 'or'-ed together bitmask indicating parameter validity.
     * To determine the validity of a particular parameter, say tile width,
     * test <code>getValidMask() & ImageLayout.TILE_WIDTH_MASK</code>
     * against <code>0</code>.
     *
     * <p> To test a single mask value or set of mask values, the
     * convenience method isValid() may be used.
     *
     * @return an int that is the logical 'or' of the valid mask values,
     *         with a '1' bit representing the setting of a value.
     */
    public int getValidMask() {
        return validMask;
    }

    /**
     * Returns <code>true</code> if all the parameters specified by the argument are set.
     *
     * @param mask a bitmask.
     * @return a boolean truth value.
     */
    public final boolean isValid(int mask) {
        return (validMask & mask) == mask;
    }

    /**
     * Sets selected bits of the valid bitmask.  The valid bitmask is
     * set to the logical 'or' of its prior value and a new value.
     *
     * @param mask the new mask value to be 'or'-ed with the prior value.
     * @return a reference to this <code>ImageLayout</code> following the change.
     */
    public ImageLayout setValid(int mask) {
        validMask |= mask;
        return this;
    }

    /**
     * Clears selected bits of the valid bitmask.  The valid bitmask
     * is set to the logical 'and' of its prior value and the negation
     * of the new mask value.  This effectively subtracts from the set of
     * valid parameters.
     *
     * @param mask the new mask value to be negated and 'and'-ed with
     *        the prior value.
     * @return a reference to this <code>ImageLayout</code> following the change.
     */
    public ImageLayout unsetValid(int mask) {
        validMask &= ~mask;
        return this;
    }

    /**
     * Marks the parameters dealing with the image bounds
     * (minX, minY, width, and height) as being invalid.
     *
     * @return a reference to this <code>ImageLayout</code> following the change.
     */
    public ImageLayout unsetImageBounds() {
        unsetValid(MIN_X_MASK |
                   MIN_Y_MASK |
                   WIDTH_MASK |
                   HEIGHT_MASK);
        return this;
    }

    /**
     * Marks the parameters dealing with the tile layout (tileGridXOffset,
     * tileGridYOffset, tileWidth, and tileHeight) as being invalid.
     *
     * @return a reference to this <code>ImageLayout</code> following the change.
     */
    public ImageLayout unsetTileLayout() {
        unsetValid(TILE_GRID_X_OFFSET_MASK |
                   TILE_GRID_Y_OFFSET_MASK |
                   TILE_WIDTH_MASK |
                   TILE_HEIGHT_MASK);
        return this;
    }

    /**
     * Returns the value of <code>minX</code> if it is valid, and
     * otherwise returns the value from the supplied <code>RenderedImage</code>.
     * If <code>minX</code> is not valid and fallback is null, 0 is returned.
     *
     * @param fallback the <code>RenderedImage</code> fallback.
     * @return the appropriate value of minX.
     */
    public int getMinX(RenderedImage fallback) {
        if (isValid(MIN_X_MASK)) {
            return minX;
        } else {
            if (fallback == null) {
                return 0;
            } else {
                return fallback.getMinX();
            }
        }
    }

    /**
     * Sets <code>minX</code> to the supplied value and marks it as valid.
     *
     * @param minX the minimum X coordinate of the image, as an int.
     * @return a reference to this <code>ImageLayout</code> following the change.
     */
    public ImageLayout setMinX(int minX) {
        this.minX = minX;
        setValid(MIN_X_MASK);
        return this;
    }

    /**
     * Returns the value of <code>minY</code> if it is valid, and
     * otherwise returns the value from the supplied <code>RenderedImage</code>.
     * If <code>minY</code> is not valid and fallback is null, 0 is returned.
     *
     * @param fallback the <code>RenderedImage</code> fallback.
     * @return the appropriate value of minY.
     */
    public int getMinY(RenderedImage fallback) {
        if (isValid(MIN_Y_MASK)) {
            return minY;
        } else {
            if (fallback == null) {
                return 0;
            } else {
                return fallback.getMinY();
            }
        }
    }

    /**
     * Sets <code>minY</code> to the supplied value and marks it as valid.
     *
     * @param minY the minimum Y coordinate of the image, as an int.
     * @return a reference to this <code>ImageLayout</code> following the change.
     */
    public ImageLayout setMinY(int minY) {
        this.minY = minY;
        setValid(MIN_Y_MASK);
        return this;
    }

    /**
     * Returns the value of <code>width</code> if it is valid, and
     * otherwise returns the value from the supplied <code>RenderedImage</code>.
     * If <code>width</code> is not valid and fallback is null, 0 is returned.
     *
     * @param fallback the <code>RenderedImage</code> fallback.
     * @return the appropriate value of width.
     */
    public int getWidth(RenderedImage fallback) {
        if (isValid(WIDTH_MASK)) {
            return width;
        } else {
            if (fallback == null) {
                return 0;
            } else {
                return fallback.getWidth();
            }
        }
    }

    /**
     * Sets <code>width</code> to the supplied value and marks it as valid.
     *
     * @param width the width of the image, as an int.
     * @return a reference to this <code>ImageLayout</code> following the change.
     * @throws IllegalArgumentException if <code>width</code> is non-positive.
     */
   public ImageLayout setWidth(int width) {
       if(width <= 0) {
           throw new IllegalArgumentException(JaiI18N.getString("ImageLayout0"));
       }
       this.width = width;
       setValid(WIDTH_MASK);
       return this;
    }

    /**
     * Returns the value of height if it is valid, and
     * otherwise returns the value from the supplied <code>RenderedImage</code>.
     * If height is not valid and fallback is null, 0 is returned.
     *
     * @param fallback the <code>RenderedImage</code> fallback.
     * @return the appropriate value of height.
     */
    public int getHeight(RenderedImage fallback) {
        if (isValid(HEIGHT_MASK)) {
            return height;
        } else {
            if (fallback == null) {
                return 0;
            } else {
                return fallback.getHeight();
            }
        }
    }

    /**
     * Sets height to the supplied value and marks it as valid.
     *
     * @param height the height of the image, as an int.
     * @return a reference to this <code>ImageLayout</code> following the change.
     * @throws IllegalArgumentException if <code>height</code> is non-positive.
     */
    public ImageLayout setHeight(int height) {
       if(height <= 0) {
           throw new IllegalArgumentException(JaiI18N.getString("ImageLayout0"));
       }
       this.height = height;
       setValid(HEIGHT_MASK);
       return this;
    }

    /**
     * Returns the value of <code>tileGridXOffset</code> if it is valid, and
     * otherwise returns the value from the supplied <code>RenderedImage</code>.
     * If <code>tileGridXOffset</code> is not valid and fallback is null, 0 is returned.
     *
     * @param fallback the <code>RenderedImage</code> fallback.
     * @return the appropriate value of tileGridXOffset.
     */
    public int getTileGridXOffset(RenderedImage fallback) {
        if (isValid(TILE_GRID_X_OFFSET_MASK)) {
            return tileGridXOffset;
        } else {
            if (fallback == null) {
                return 0;
            } else {
                return fallback.getTileGridXOffset();
            }
        }
    }

    /**
     * Sets <code>tileGridXOffset</code> to the supplied value and marks it as valid.
     *
     * @param tileGridXOffset the X coordinate of tile (0, 0), as an int.
     * @return a reference to this <code>ImageLayout</code> following the change.
     */
    public ImageLayout setTileGridXOffset(int tileGridXOffset) {
        this.tileGridXOffset = tileGridXOffset;
        setValid(TILE_GRID_X_OFFSET_MASK);
        return this;
    }

    /**
     * Returns the value of <code>tileGridYOffset</code> if it is valid, and
     * otherwise returns the value from the supplied <code>RenderedImage</code>.
     * If <code>tileGridYOffset</code> is not valid and fallback is null, 0 is returned.
     *
     * @param fallback the <code>RenderedImage</code> fallback.
     * @return the appropriate value of tileGridYOffset.
     */
    public int getTileGridYOffset(RenderedImage fallback) {
        if (isValid(TILE_GRID_Y_OFFSET_MASK)) {
            return tileGridYOffset;
        } else {
            if (fallback == null) {
                return 0;
            } else {
                return fallback.getTileGridYOffset();
            }
        }
    }

    /**
     * Sets <code>tileGridYOffset</code> to the supplied value and marks it as valid.
     *
     * @param tileGridYOffset the Y coordinate of tile (0, 0), as an int.
     * @return a reference to this <code>ImageLayout</code> following the change.
     */
    public ImageLayout setTileGridYOffset(int tileGridYOffset) {
        this.tileGridYOffset = tileGridYOffset;
        setValid(TILE_GRID_Y_OFFSET_MASK);
        return this;
    }

    /**
     * Returns the value of <code>tileWidth</code> if it is valid, and
     * otherwise returns the value from the supplied <code>RenderedImage</code>.
     * If <code>tileWidth</code> is not valid and fallback is null, 0 is returned.
     *
     * @param fallback the <code>RenderedImage</code> fallback.
     * @return the appropriate value of tileWidth.
     */
    public int getTileWidth(RenderedImage fallback) {
        if (isValid(TILE_WIDTH_MASK)) {
            return tileWidth;
        } else {
            if (fallback == null) {
                return 0;
            } else {
                return fallback.getTileWidth();
            }
        }
    }

    /**
     * Sets <code>tileWidth</code> to the supplied value and marks it as valid.
     *
     * @param tileWidth the width of a tile, as an int.
     * @return a reference to this <code>ImageLayout</code> following the change.
     * @throws IllegalArgumentException if <code>tileWidth</code> is
     *                                  non-positive.
     */
    public ImageLayout setTileWidth(int tileWidth) {
       if(tileWidth <= 0) {
           throw new IllegalArgumentException(JaiI18N.getString("ImageLayout0"));
       }
       this.tileWidth = tileWidth;
       setValid(TILE_WIDTH_MASK);
       return this;
    }

    /**
     * Returns the value of tileHeight if it is valid, and
     * otherwise returns the value from the supplied <code>RenderedImage</code>.
     * If tileHeight is not valid and fallback is null, 0 is returned.
     *
     * @param fallback the <code>RenderedImage</code> fallback.
     * @return the appropriate value of tileHeight.
     */
    public int getTileHeight(RenderedImage fallback) {
        if (isValid(TILE_HEIGHT_MASK)) {
            return tileHeight;
        } else {
            if (fallback == null) {
                return 0;
            } else {
                return fallback.getTileHeight();
            }
        }
    }

    /**
     * Sets tileHeight to the supplied value and marks it as valid.
     *
     * @param tileHeight the height of a tile, as an int.
     * @return a reference to this <code>ImageLayout</code> following the change.
     * @throws IllegalArgumentException if <code>tileHeight</code> is
     *                                  non-positive.
     */
    public ImageLayout setTileHeight(int tileHeight) {
       if(tileHeight <= 0) {
           throw new IllegalArgumentException(JaiI18N.getString("ImageLayout0"));
       }
       this.tileHeight = tileHeight;
       setValid(TILE_HEIGHT_MASK);
       return this;
    }

    /**
     * Returns the value of <code>sampleModel</code> if it is valid, and
     * otherwise returns the value from the supplied <code>RenderedImage</code>.
     * If <code>sampleModel</code> is not valid and fallback is null, null is returned.
     *
     * @param fallback the <code>RenderedImage</code> fallback.
     * @return the appropriate value of <code>sampleModel</code>.
     */
    public SampleModel getSampleModel(RenderedImage fallback) {
        if (isValid(SAMPLE_MODEL_MASK)) {
            return sampleModel;
        } else {
            if (fallback == null) {
                return null;
            } else {
                return fallback.getSampleModel();
            }
        }
    }

    /**
     * Sets <code>sampleModel</code> to the supplied value and marks it as valid.
     *
     * @param sampleModel the new <code>SampleModel</code>.
     * @return a reference to this <code>ImageLayout</code> following the change.
     */
    public ImageLayout setSampleModel(SampleModel sampleModel) {
        this.sampleModel = sampleModel;
        setValid(SAMPLE_MODEL_MASK);
        return this;
    }

    /**
     * Returns the value of <code>colorModel</code> if it is valid, and
     * otherwise returns the value from the supplied <code>RenderedImage</code>.
     * If <code>colorModel</code> is not valid and fallback is null, null is returned.
     *
     * @param fallback the <code>RenderedImage</code> fallback.
     * @return the appropriate value of <code>colorModel</code>.
     */
    public ColorModel getColorModel(RenderedImage fallback) {
        if (isValid(COLOR_MODEL_MASK)) {
            return colorModel;
        } else {
            if (fallback == null) {
                return null;
            } else {
                return fallback.getColorModel();
            }
        }
    }

    /**
     * Sets <code>colorModel</code> to the supplied value and marks it as valid.
     *
     * @param colorModel the new <code>ColorModel</code>.
     * @return a reference to this <code>ImageLayout</code> following the change.
     */
    public ImageLayout setColorModel(ColorModel colorModel) {
        this.colorModel = colorModel;
        setValid(COLOR_MODEL_MASK);
        return this;
    }

    /** Returns a String containing the values of all valid fields. */
    public String toString() {
        String s = "ImageLayout[";
        boolean first = true;

        if (isValid(MIN_X_MASK)) {
            s += "MIN_X=" + minX;
            first = false;
        }

        if (isValid(MIN_Y_MASK)) {
            if (!first) {
                s += ", ";
            }
            s += "MIN_Y=" + minY;
            first = false;
        }

        if (isValid(WIDTH_MASK)) {
            if (!first) {
                s += ", ";
            }
            s += "WIDTH=" + width;
            first = false;
        }

        if (isValid(HEIGHT_MASK)) {
            if (!first) {
                s += ", ";
            }
            s += "HEIGHT=" + height;
            first = false;
        }

        if (isValid(TILE_GRID_X_OFFSET_MASK)) {
            if (!first) {
                s += ", ";
            }
            s += "TILE_GRID_X_OFFSET=" + tileGridXOffset;
            first = false;
        }

        if (isValid(TILE_GRID_Y_OFFSET_MASK)) {
            if (!first) {
                s += ", ";
            }
            s += "TILE_GRID_Y_OFFSET=" + tileGridYOffset;
            first = false;
        }

        if (isValid(TILE_WIDTH_MASK)) {
            if (!first) {
                s += ", ";
            }
            s += "TILE_WIDTH=" + tileWidth;
            first = false;
        }

        if (isValid(TILE_HEIGHT_MASK)) {
            if (!first) {
                s += ", ";
            }
            s += "TILE_HEIGHT=" + tileHeight;
            first = false;
        }

        if (isValid(SAMPLE_MODEL_MASK)) {
            if (!first) {
                s += ", ";
            }
            s += "SAMPLE_MODEL=" + sampleModel;
            first = false;
        }

        if (isValid(COLOR_MODEL_MASK)) {
            if (!first) {
                s += ", ";
            }
            s += "COLOR_MODEL=" + colorModel;
        }

        s += "]";
        return s;
    }

    /**
     * Returns a clone of the <code>ImageLayout</code> as an Object.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
     * Serialize the <code>ImageLayout</code>.
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Write the non-static and non-transient fields.
        out.defaultWriteObject();

        // Create and write a serializable SampleModel.
        if(isValid(SAMPLE_MODEL_MASK)) {
            out.writeObject(SerializerFactory.getState(sampleModel, null));
        }

        // Create and write a serializable ColorModel.
        if(isValid(COLOR_MODEL_MASK)) {
            out.writeObject(SerializerFactory.getState(colorModel, null));
        }
    }

    /**
     * Deserialize the <code>ImageLayout</code>.
     * @throws IOException
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // Read the non-static and non-transient fields.
        in.defaultReadObject();

        // Read the serializable form of the SampleModel.
        if(isValid(SAMPLE_MODEL_MASK)) {
            Object object = in.readObject();
            if (!(object instanceof SerializableState))
                sampleModel = null;

            SerializableState ss = (SerializableState)object;
            Class c = ss.getObjectClass();
            if (SampleModel.class.isAssignableFrom(c))
                sampleModel = (SampleModel)ss.getObject();
            else
                sampleModel = null;
        }

        // Read the serializable form of the ColorModel.
        if(isValid(COLOR_MODEL_MASK)) {
            Object object = in.readObject();
            if (!(object instanceof SerializableState))
                colorModel = null;

            SerializableState ss = (SerializableState)object;
            Class c = ss.getObjectClass();
            if (ColorModel.class.isAssignableFrom(c))
                colorModel = (ColorModel)ss.getObject();
            else
                colorModel = null;
        }
    }

    /**
     * Tests if the specified <code>Object</code> equals this
     * <code>ImageLayout</code>.
     *
     * @param obj the <code>Object</code> to test for equality
     *
     * @return <code>true</code> if the specified <code>Object</code>
     * is an instance of <code>ImageLayout</code> and equals this
     * <code>ImageLayout</code>; <code>false</code> otherwise.
     *
     * @since JAI 1.1
     */
    public boolean equals(Object obj) {

	if (this == obj)
	    return true;

	if (!(obj instanceof ImageLayout))
	    return false;

	ImageLayout il = (ImageLayout)obj;

	return (validMask       == il.validMask      ) &&
	       (width           == il.width          ) &&
	       (height          == il.height         ) &&
	       (minX            == il.minX           ) &&
	       (minY            == il.minY           ) &&
	       (tileHeight      == il.tileHeight     ) &&
	       (tileWidth       == il.tileWidth      ) &&
	       (tileGridXOffset == il.tileGridXOffset) &&
	       (tileGridYOffset == il.tileGridYOffset) &&
	       (sampleModel.equals(il.sampleModel   )) &&
	       (colorModel.equals(il.colorModel));
    }

    /**
     * Returns the hash code for this <code>ImageLayout</code>.
     *
     * @return a hash code for this <code>ImageLayout</code>.
     *
     * @since JAI 1.1
     */
    public int hashCode() {

	int code = 0, i = 1;

	// This implementation is quite arbitrary.
	// hashCode's NEED not be uniqe for two "different" objects
	code += (width           * i++);
	code += (height          * i++);
	code += (minX            * i++);
	code += (minY            * i++);
	code += (tileHeight      * i++);
	code += (tileWidth       * i++);
	code += (tileGridXOffset * i++);
	code += (tileGridYOffset * i++);

	code ^= sampleModel.hashCode();
	code ^= validMask;
	code ^= colorModel.hashCode();

	return code;
    }
}
