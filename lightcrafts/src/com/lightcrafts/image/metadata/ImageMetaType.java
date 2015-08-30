/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * <code>ImageMetaType</code> defines the constants for the types of image
 * metadata.
 * <p>
 * The constants have the values they do because they are the values as defined
 * as part of the TIFF specification.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public enum ImageMetaType {

    ////////// public /////////////////////////////////////////////////////////

    /** 64-bit double precision IEEE format. */
    META_DOUBLE( TIFF_FIELD_TYPE_DOUBLE ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isFloatingPointType();
        }
    },

    /** 32-bit single-precision IEEE format. */
    META_FLOAT( TIFF_FIELD_TYPE_FLOAT ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isFloatingPointType();
        }
    },

    /** 32-bit unsigned integer. */
    META_IFD( TIFF_FIELD_TYPE_IFD ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            switch ( t ) {
                case META_IFD:
                case META_ULONG:
                    return true;
                default:
                    return false;
            }
        }
    },

    /** 32-bit unsigned integer. */
    META_IFD64( TIFF_FIELD_TYPE_IFD64 ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            switch ( t ) {
                case META_IFD64:
                case META_UINT64:
                    return true;
                default:
                    return false;
            }
        }
    },

    /** 8-bit signed integer. */
    META_SBYTE( TIFF_FIELD_TYPE_SBYTE ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isIntegerType();
        }
    },

    /** 32-bit signed integer. */
    META_SLONG( TIFF_FIELD_TYPE_SLONG ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isIntegerType();
        }
    },

    /** 64-bit signed integer. */
    META_SINT64( TIFF_FIELD_TYPE_SINT64 ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isIntegerType();
        }
    },

    /** Two SIGNED_LONGs: numerator/denominator. */
    META_SRATIONAL( TIFF_FIELD_TYPE_SRATIONAL ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isRationalType();
        }
    },

    /** 16-bit signed integer */
    META_SSHORT( TIFF_FIELD_TYPE_SSHORT ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isIntegerType();
        }
    },

    /** 8-bit byte containing one 7-bit ASCII code. */
    META_STRING( TIFF_FIELD_TYPE_ASCII ),

    /** 8-bit unsigned integer. */
    META_UBYTE( TIFF_FIELD_TYPE_UBYTE ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isIntegerType() || t == META_UNDEFINED;
        }
    },

    /** Undefined. */
    META_UNDEFINED( TIFF_FIELD_TYPE_UNDEFINED ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t == META_UNDEFINED || t == META_UBYTE;
        }
    },

    /** UNICODE. **/
    // META_UNICODE( TIFF_FIELD_TYPE_UNICODE ),

    /** An unknown type. */
    META_UNKNOWN( (byte)-2 ),

    /** 32-bit unsigned integer. */
    META_ULONG( TIFF_FIELD_TYPE_ULONG ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isIntegerType() || t == META_IFD;
        }
    },

    /** 64-bit unsigned integer. */
    META_UINT64( TIFF_FIELD_TYPE_UINT64 ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isIntegerType() || t == META_IFD64;
        }
    },

    /** Two ULONGs: numerator/denominator. */
    META_URATIONAL( TIFF_FIELD_TYPE_URATIONAL ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isRationalType();
        }
    },

    /** 16-bit unsigned integer. */
    META_USHORT( TIFF_FIELD_TYPE_USHORT ) {
        public boolean isCompatibleWith( ImageMetaType t ) {
            return t.isIntegerType();
        }
    },

    /**
     * A date.  This type isn't defined in the TIFF specification.  We define
     * it to distinguish dates from ordinary strings.
     */
    META_DATE( (byte)-1 );

    /**
     * Get the <code>ImageMetaType</code> for the given TIFF constant.
     *
     * @param tiffConstant The constant used in the TIFF specification to
     * encode field type.
     * @return Returns said <code>ImageMetaType</code>.
     * @throws IllegalArgumentException if the constant is not in the range
     * [1,13] or -1.
     * @see #getTIFFConstant()
     */
     public static ImageMetaType getTypeFor( int tiffConstant ) {
        switch ( tiffConstant ) {
            case TIFF_FIELD_TYPE_ASCII:
                return META_STRING;
            case TIFF_FIELD_TYPE_DOUBLE:
                return META_DOUBLE;
            case TIFF_FIELD_TYPE_FLOAT:
                return META_FLOAT;
            case TIFF_FIELD_TYPE_IFD:
                return META_IFD;
            case TIFF_FIELD_TYPE_IFD64:
                return META_IFD64;
            case TIFF_FIELD_TYPE_SBYTE:
                return META_SBYTE;
            case TIFF_FIELD_TYPE_SLONG:
                return META_SLONG;
            case TIFF_FIELD_TYPE_SINT64:
                return META_SINT64;
            case TIFF_FIELD_TYPE_SRATIONAL:
                return META_SRATIONAL;
            case TIFF_FIELD_TYPE_SSHORT:
                return META_SSHORT;
            case TIFF_FIELD_TYPE_UBYTE:
                return META_UBYTE;
            case TIFF_FIELD_TYPE_ULONG:
                return META_ULONG;
            case TIFF_FIELD_TYPE_UINT64:
                return META_UINT64;
            case TIFF_FIELD_TYPE_UNDEFINED:
                return META_UNDEFINED;
            case TIFF_FIELD_TYPE_URATIONAL:
                return META_URATIONAL;
            case TIFF_FIELD_TYPE_USHORT:
                return META_USHORT;
            // case TIFF_FIELD_TYPE_UNICODE:
                // return META_UNICODE;
            // case TIFF_FIELD_TYPE_COMPLEX:
                // return META_COMPLEX;
            case -1:
                return META_DATE;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Gets the constant used in the TIFF specification to encode this type.
     *
     * @return Returns said type.
     * @see #getTypeFor(int)
     */
    public final byte getTIFFConstant() {
        return m_tiffConstant;
    }

    /**
     * Checks whether the given <code>ImageMetaType</code> is compatible with
     * this <code>ImageMetaType</code>.
     *
     * @param t Another <code>ImageMetaType</code>.
     * @return Returns <code>true</code> only if the two types are compatible.
     */
    public boolean isCompatibleWith( ImageMetaType t ) {
        return t == this;
    }

    /**
     * Checks whether this <code>ImageMetaType</code> is a date type.
     *
     * @return Returns <code>true</code> only if this
     * <code>ImageMetaType</code> is a date type.
     */
    public final boolean isDateType() {
        return this == META_DATE;
    }

    /**
     * Checks whether this <code>ImageMetaType</code> is a floating-point type,
     * i.e., {@link #META_DOUBLE} or {@link #META_FLOAT}.
     *
     * @return Returns <code>true</code> only if this
     * <code>ImageMetaType</code> is a floating-point type.
     * @see #isIntegerType()
     * @see #isNumericType()
     * @see #isRationalType()
     */
    public final boolean isFloatingPointType() {
        switch ( this ) {
            case META_DOUBLE:
            case META_FLOAT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks whether this <code>ImageMetaType</code> is an integer type.
     *
     * @return Returns <code>true</code> only if this
     * <code>ImageMetaType</code> is an integer type.
     * @see #isFloatingPointType()
     * @see #isNumericType()
     * @see #isRationalType()
     */
    public final boolean isIntegerType() {
        switch ( this ) {
            case META_SBYTE:
            case META_SINT64:
            case META_SLONG:
            case META_SSHORT:
            case META_UBYTE:
            case META_UINT64:
            case META_ULONG:
            case META_USHORT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks whether this <code>ImageMetaType</code> is any numeric type.
     *
     * @return Returns <code>true</code> only if this
     * <code>ImageMetaType</code> is any numeric type.
     * @see #isFloatingPointType()
     * @see #isIntegerType()
     * @see #isRationalType()
     */
    public final boolean isNumericType() {
        return isFloatingPointType() || isIntegerType() || isRationalType();
    }

    /**
     * Checks whether this <code>ImageMetaType</code> is a rational type.
     *
     * @return Returns <code>true</code> only if this
     * <code>ImageMetaType</code> is a rational type.
     * @see #isFloatingPointType()
     * @see #isIntegerType()
     * @see #isNumericType()
     */
    public final boolean isRationalType() {
        switch ( this ) {
            case META_SRATIONAL:
            case META_URATIONAL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks whether this <code>ImageMetaType</code> is a string type.
     *
     * @return Returns <code>true</code> only if this
     * <code>ImageMetaType</code> is a string type.
     */
    public final boolean isStringType() {
        switch ( this ) {
            case META_STRING:
            // case META_UNICODE:
                return true;
            default:
                return false;
        }
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Constructs an <code>ImageOrientation</code>.
     *
     * @param tiffConstant The constant used in the TIFF specification to
     * encode field type.
     */
    private ImageMetaType( byte tiffConstant ) {
        m_tiffConstant = tiffConstant;
    }

    /**
     * The constant used in the TIFF specification to encode this metadata type.
     */
    private final byte m_tiffConstant;
}
/* vim:set et sw=4 ts=4: */
