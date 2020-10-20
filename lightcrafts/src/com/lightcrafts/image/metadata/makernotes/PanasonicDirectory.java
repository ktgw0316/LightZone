/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.providers.FlashProvider;
import com.lightcrafts.image.metadata.providers.ISOProvider;
import com.lightcrafts.image.metadata.providers.LensProvider;
import com.lightcrafts.image.metadata.providers.OrientationProvider;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.ImageOrientation.*;
import static com.lightcrafts.image.metadata.makernotes.PanasonicTags.*;

/**
 * A <code>PanasonicDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding Panasonic-specific maker-note metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class PanasonicDirectory extends MakerNotesDirectory implements
    FlashProvider, ISOProvider, LensProvider, OrientationProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFlash() {
        final ImageMetaValue value = getValue( PANASONIC_FLASH_FIRED );
        return value != null ? value.getIntValue() : -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getISO() {
        final ImageMetaValue value = getValue( PANASONIC_ISO );
        if (value == null || hasTagValueLabelFor(value) != null) {
            return 0;
        } else {
            final int iso = value.getIntValue();
            switch (iso) {
                case 65534:
                case 65535:
                    return 0;
                default:
                    return iso;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLens() {
        final ImageMetaValue value = getValue( PANASONIC_LENS_TYPE );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * Gets the maker-notes adjustments for Panasonic.
     *
     * @param buf The {@link LCByteBuffer} the metadata is in.
     * @param offset The offset to the start of the maker-notes.
     * @return Returns said adjustments.
     */
    @Override
    public int[] getMakerNotesAdjustments( LCByteBuffer buf, int offset ) {
        //
        // The 12 bytes are:
        //
        //      0- 8: "Panasonic"
        //      9-11: 0 0 0
        //
        return new int[]{ 12, 0 };
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;Panasonic&quot;.
     */
    @Override
    public String getName() {
        return "Panasonic";
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public ImageMetaTagInfo getTagInfoFor( Integer id ) {
        return m_tagsByID.get( id );
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public void putValue( Integer tagID, ImageMetaValue value ) {
        switch ( tagID ) {
            case PANASONIC_CONTRAST_MODE: {
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
    public void setOrientation( ImageOrientation orientation ) {
        final int value;
        switch ( orientation ) {
            case ORIENTATION_LANDSCAPE:
                value = 1;
                break;
            case ORIENTATION_90CW:
                value = 6;
                break;
            case ORIENTATION_90CCW:
                value = 8;
                break;
            default:
                value = 0;
        }
        setValue( PANASONIC_ROTATION, value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
            case PANASONIC_ISO: {
                final String label = hasTagValueLabelFor(value);
                if (label != null) {
                    return label;
                } else {
                    final int iso = value.getIntValue();
                    switch (iso) {
                        case 65534:
                            return "Intelligent";
                        case 65535:
                            return "n/a";
                        default:
                            return String.valueOf(iso);
                    }
                }
            }
            case PANASONIC_TIME_SINCE_POWER_ON: {
                if ( !value.isNumeric() )
                    break;
                final StringBuilder sb = new StringBuilder();
                long time = value.getLongValue();
                if ( time >= 24 * 3600 ) {
                    final int days = (int)(time / (24 * 3600));
                    sb.append( days );
                    sb.append( " days " );  // TODO: localize
                    time -= (long) days * 24 * 3600;
                }
                final int h = (int)(time / 3600);
                time -= h * 3600L;
                final int m = (int)(time / 60);
                time -= m * 60L;
                final int s = (int)time;
                sb.append( String.format( "%02d:%02d:%02d", h, m, s ) );
                return sb.toString();
            }
            case PANASONIC_TRAVEL_DAY:
                if ( !value.isNumeric() )
                    break;
                return value.getIntValue() == 65535 ? "n/a" : value.getStringValue();
            case PANASONIC_WHITE_BALANCE_BIAS:
                if ( !value.isNumeric() )
                    break;
                final double bias = value.getIntValue() / 3.0;
                return MetadataUtil.convertBiasFromAPEX( bias );
        }
        return super.valueToString( value );
    }

    ////////// protected //////////////////////////////////////////////////////

    @Override
    protected ImageMetaValue getLongFocalValue() {
        return null;
    }

    @Override
    protected ImageMetaValue getShortFocalValue() {
        return null;
    }

    @Override
    protected ImageMetaValue getMaxApertureValue() {
        return null;
    }

    /**
     * Get the {@link ResourceBundle} to use for tags.
     *
     * @return Returns said {@link ResourceBundle}.
     */
    @Override
    protected ResourceBundle getTagLabelBundle() {
        return m_tagBundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    private static void add( int id, String name, ImageMetaType type,
                             boolean isChangeable ) {
        final ImageMetaTagInfo tagInfo =
            new ImageMetaTagInfo( id, name, type, isChangeable );
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
            new HashMap<>();

    /**
     * A mapping of tags by name.
     */
    private static final Map<String,ImageMetaTagInfo> m_tagsByName =
            new HashMap<>();

    /**
     * This is where the actual labels for the Panasonic tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.makernotes.PanasonicTags"
    );

    static {
        add( PANASONIC_ACCESSORY_TYPE, "AccessoryType", META_STRING, false );
        add( PANASONIC_AUDIO, "Audio", META_USHORT, false );
        add( PANASONIC_AUTO_FOCUS_ASSIST_LAMP, "AutoFocusAssistLamp", META_USHORT, false );
        add( PANASONIC_BABY_AGE, "BabyAge", META_DATE, false );
        add( PANASONIC_BABY_AGE_2, "BabyAge2", META_DATE, false );
        add( PANASONIC_BURST_MODE, "BurstMode", META_USHORT, false );
        add( PANASONIC_CITY, "City", META_STRING, false );
        add( PANASONIC_COLOR_EFFECT, "ColorEffect", META_USHORT, false );
        add( PANASONIC_COLOR_MODE, "ColorMode", META_USHORT, false );
        add( PANASONIC_CONTRAST, "Contrast", META_SSHORT, false );
        add( PANASONIC_CONTRAST_MODE, "ContrastMode", META_USHORT, false );
        add( PANASONIC_CONVERSION_LENS, "ConversionLens", META_USHORT, false );
        add( PANASONIC_COUNTRY, "Country", META_STRING, false );
        add( PANASONIC_EXIF_VERSION, "EXIFVersion", META_UNDEFINED, false );
        add( PANASONIC_FACES_DETECTED, "FacesDetected", META_USHORT, false );
        add( PANASONIC_FILM_MODE, "FilmMode", META_USHORT, false );
        add( PANASONIC_FIRMWARE_VERSION, "FirmwareVersion", META_UNDEFINED, false );
        add( PANASONIC_FLASH_BIAS, "FlashBias", META_SSHORT, false );
        add( PANASONIC_FLASH_FIRED, "FlashFired", META_USHORT, false );
        add( PANASONIC_FOCUS_MODE, "FocusMode", META_USHORT, false );
        add( PANASONIC_FLASH_WARNING, "FlashWarning", META_USHORT, false );
        add( PANASONIC_IMAGE_QUALITY, "ImageQuality", META_USHORT, false );
        add( PANASONIC_IMAGE_STABILIZER, "ImageStabilizer", META_USHORT, false );
        add( PANASONIC_INTELLIGENT_RESOLUTION, "IntelligentResolution", META_UBYTE, false );
        add( PANASONIC_INTERNAL_SERIAL_NUMBER, "InternalSerialNumber", META_UNDEFINED, false );
        add( PANASONIC_ISO, "ISO", META_USHORT, false );
        add( PANASONIC_LANDMARK, "Landmark", META_STRING, false );
        add( PANASONIC_LENS_SERIAL_NUMBER, "LensSerialNumber", META_STRING, false );
        add( PANASONIC_LENS_TYPE, "LensType", META_STRING, false );
        add( PANASONIC_MACRO_MODE, "MacroMode", META_USHORT, false );
        add( PANASONIC_NOISE_REDUCTION, "NoiseReduction", META_USHORT, false );
        add( PANASONIC_OPTICAL_ZOOM_MODE, "OpticalZoomMode", META_USHORT, false );
        add( PANASONIC_ROTATION, "Rotation", META_USHORT, true );
        add( PANASONIC_SATURATION, "Saturation", META_USHORT, false );
        add( PANASONIC_SCENE_MODE, "SceneMode", META_USHORT, false );
        add( PANASONIC_SELF_TIMER, "SelfTimer", META_USHORT, false );
        add( PANASONIC_SEQUENCE_NUMBER, "SequenceNumber", META_ULONG, false );
        add( PANASONIC_SHARPNESS, "Sharpness", META_USHORT, false );
        add( PANASONIC_SHOOTING_MODE, "ShootingMode", META_USHORT, false );
        add( PANASONIC_SPOT_MODE, "SpotMode", META_UBYTE, false );
        add( PANASONIC_STATE, "State", META_STRING, false );
        add( PANASONIC_TEXT_STAMP, "TextStamp", META_USHORT, false );
        add( PANASONIC_TEXT_STAMP_2, "TextStamp2", META_USHORT, false );
        add( PANASONIC_TEXT_STAMP_3, "TextStamp3", META_USHORT, false );
        add( PANASONIC_TEXT_STAMP_4, "TextStamp4", META_USHORT, false );
        add( PANASONIC_TIME_SINCE_POWER_ON, "TimeSincePowerOn", META_ULONG, false );
        add( PANASONIC_TRAVEL_DAY, "TravelDay", META_USHORT, false );
        add( PANASONIC_WHITE_BALANCE, "WhiteBalance", META_USHORT, false );
        add( PANASONIC_WHITE_BALANCE_BIAS, "WhiteBalanceBias", META_SSHORT, false );
        add( PANASONIC_WORLD_TIME_LOCATION, "WorldTimeDestination", META_USHORT, false );
    }
}
/* vim:set et sw=4 ts=4: */
