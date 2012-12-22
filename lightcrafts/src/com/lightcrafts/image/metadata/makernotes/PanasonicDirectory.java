/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.providers.OrientationProvider;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.ImageOrientation.*;
import static com.lightcrafts.image.metadata.makernotes.PanasonicTags.*;

/**
 * A <code>PanasonicDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding Panasonic-specific maker-note metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class PanasonicDirectory extends MakerNotesDirectory implements
    OrientationProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the maker-notes adjustments for Panasonic.
     *
     * @param buf The {@link LCByteBuffer} the metadata is in.
     * @param offset The offset to the start of the maker-notes.
     * @return Returns said adjustments.
     */
    public int[] getMakerNotesAdjustments( LCByteBuffer buf, int offset ) {
        //
        // The 12 bytes are:
        //
        //      0- 8: "Panasonic"
        //      9-11: 0 0 0
        //
        return new int[]{ 12, offset };
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;Panasonic&quot;.
     */
    public String getName() {
        return "Panasonic";
    }

    /**
     * {@inheritDoc}
     */
    public ImageOrientation getOrientation() {
        final ImageMetaValue value = getValue( PANASONIC_ROTATION );
        if ( value != null )
            switch ( value.getIntValue() ) {
                case 1:
                    return ORIENTATION_LANDSCAPE;
                case 6:
                    return ORIENTATION_90CW;
                case 8:
                    return ORIENTATION_90CCW;
            }
        return ORIENTATION_UNKNOWN;
    }

    /**
     * {@inheritDoc}
     */
    public ImageMetaTagInfo getTagInfoFor( Integer id ) {
        return m_tagsByID.get( id );
    }

    /**
     * {@inheritDoc}
     */
    public ImageMetaTagInfo getTagInfoFor( String name ) {
        return m_tagsByName.get( name );
    }

    /**
     * Puts a key/value pair into this directory.  For a Panasonic tag that has
     * subfields, expands the values into multiple key/value pairs.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The value to put.
     * @see #valueToString(ImageMetaValue)
     */
    public void putValue( Integer tagID, ImageMetaValue value ) {
        switch ( tagID ) {
            case PANASONIC_CONTRAST: {
                if ( !value.isNumeric() )
                    return;
                final int n = value.getIntValue();
                if ( n >= 0x100 )       // Leica values
                    value = new UnsignedShortMetaValue( (n >>> 4) & 0x3 );
                break;
            }
            case PANASONIC_SPOT_MODE: {
                if ( !(value instanceof LongMetaValue) ||
                     value.getValueCount() != 2 )
                    return;
                //
                // The value of interest is the second number only, so discard
                // the first.
                //
                final LongMetaValue n = (LongMetaValue)value;
                value = new UnsignedByteMetaValue( n.getLongValueAt( 1 ) );
                break;
            }
        }
        super.putValue( tagID, value );
    }

    /**
     * {@inheritDoc}
     */
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case PANASONIC_FIRMWARE_VERSION: {
                if ( !(value instanceof UndefinedMetaValue) )
                    return "unknown";                   // TODO: localize
                final UndefinedMetaValue u = (UndefinedMetaValue)value;
                final byte[] buf = u.getUndefinedValue();
                final StringBuilder sb = new StringBuilder();
                boolean dot = false;
                for ( byte b : buf ) {
                    if ( !dot )
                        dot = true;
                    else
                        sb.append( '.' );
                    sb.append( (char)b );
                }
                return sb.toString();
            }
            case PANASONIC_INTERNAL_SERIAL_NUMBER: {
                final Matcher m =
                    m_serialNumberPattern.matcher( value.getStringValue() );
                if ( !m.matches() )
                    break;
                try {
                    int year = Integer.parseInt( m.group( 2 ) );
                    year += year < 70 ? 2000 : 1900;
                    return  '(' + m.group(1) + ") " + year + ':'
                            + m.group(3) + ':' + m.group(4) + " #" + m.group(5);
                }
                catch ( NumberFormatException e ) {
                    break;
                }
            }
            case PANASONIC_WHITE_BALANCE_BIAS: {
                if ( !value.isNumeric() )
                    break;
                final double bias = value.getFloatValue() / 3;
                return MetadataUtil.convertBiasFromAPEX( bias );
            }
        }
        return super.valueToString( value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Get the {@link ResourceBundle} to use for tags.
     *
     * @return Returns said {@link ResourceBundle}.
     */
    protected ResourceBundle getTagLabelBundle() {
        return m_tagBundle;
    }

    /**
     * {@inheritDoc}
     */
    protected Class<? extends ImageMetaTags> getTagsInterface() {
        return PanasonicTags.class;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Add the tag mappings.
     *
     * @param id The tag's ID.
     * @param name The tag's name.
     * @param type The tag's {@link ImageMetaType}.
     */
    private static void add( int id, String name, ImageMetaType type ) {
        final ImageMetaTagInfo tagInfo =
            new ImageMetaTagInfo( id, name, type, false );
        m_tagsByID.put( id, tagInfo );
        m_tagsByName.put( name, tagInfo );
    }

    /**
     * This pattern is used to determine how to calculate the focal length.
     */
    private static final Pattern m_serialNumberPattern =
        Pattern.compile( "^([A-Z]\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{4})" );

    /**
     * A mapping of tags by ID.
     */
    private static final Map<Integer,ImageMetaTagInfo> m_tagsByID =
        new HashMap<Integer, ImageMetaTagInfo>();

    /**
     * A mapping of tags by name.
     */
    private static final Map<String,ImageMetaTagInfo> m_tagsByName =
        new HashMap<String, ImageMetaTagInfo>();

    /**
     * This is where the actual labels for the Panasonic tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.makernotes.PanasonicTags"
    );

    static {
        add( PANASONIC_AUDIO, "Audio", META_USHORT );
        add( PANASONIC_BURST_MODE, "BurstMode", META_USHORT );
        add( PANASONIC_COLOR_EFFECT, "ColorEffect", META_USHORT );
        add( PANASONIC_COLOR_MODE, "ColorMode", META_USHORT );
        add( PANASONIC_CONTRAST, "Contrast", META_USHORT );
        add( PANASONIC_FIRMWARE_VERSION, "FirmwareVersion", META_UNDEFINED );
        add( PANASONIC_FLASH_BIAS, "FlashBias", META_SSHORT );
        add( PANASONIC_FOCUS_MODE, "FocusMode", META_USHORT );
        add( PANASONIC_IMAGE_QUALITY, "ImageQuality", META_USHORT );
        add( PANASONIC_IMAGE_STABILIZER, "ImageStabilizer", META_USHORT );
        add( PANASONIC_INTERNAL_SERIAL_NUMBER, "InternalSerialNumber", META_UNDEFINED );
        add( PANASONIC_MACRO_MODE, "MacroMode", META_USHORT );
        add( PANASONIC_NOISE_REDUCTION, "NoiseReduction", META_USHORT );
        add( PANASONIC_ROTATION, "Rotation", META_USHORT );
        add( PANASONIC_SELF_TIMER, "SelfTimer", META_USHORT );
        add( PANASONIC_SEQUENCE_NUMBER, "SequenceNumber", META_ULONG );
        add( PANASONIC_SHOOTING_MODE, "ShootingMode", META_USHORT );
        add( PANASONIC_SPOT_MODE, "SpotMode", META_UBYTE );
        add( PANASONIC_WHITE_BALANCE, "WhiteBalance", META_USHORT );
        add( PANASONIC_WHITE_BALANCE_BIAS, "WhiteBalanceBias", META_SSHORT );
    }
}
/* vim:set et sw=4 ts=4: */
