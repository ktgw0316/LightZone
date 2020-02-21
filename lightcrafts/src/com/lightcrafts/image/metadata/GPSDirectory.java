/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.metadata.providers.GPSProvider;
import com.lightcrafts.image.metadata.values.UnsignedRationalMetaValue;
import com.lightcrafts.utils.Rational;
import com.lightcrafts.utils.tuple.Pair;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static com.lightcrafts.image.metadata.GPSTags.*;
import static com.lightcrafts.image.metadata.ImageMetaType.*;

/**
 * A <code>GPSDirectory</code> is-an {@link ImageMetadataDirectory} for holding
 * GPS metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class GPSDirectory extends ImageMetadataDirectory
        implements GPSProvider {

    ////////// public /////////////////////////////////////////////////////////

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

    @Override
    public Double getGPSLatitude() {
        return readGPSCoordinate(GPS_LATITUDE, GPS_LATITUDE_REF, "N");
    }

    @Override
    public Double getGPSLongitude() {
        return readGPSCoordinate(GPS_LONGITUDE, GPS_LONGITUDE_REF, "E");
    }

    @NotNull
    @Override
    public String getGPSLatitudeDMS() {
        return readGPSCoordinateDMS(GPS_LATITUDE, GPS_LATITUDE_REF);
    }

    @NotNull
    @Override
    public String getGPSLongitudeDMS() {
        return readGPSCoordinateDMS(GPS_LONGITUDE, GPS_LONGITUDE_REF);
    }

    private Double readGPSCoordinate(int tagID, int refTagID, String orientation) {
        val metadata = readMetadata(tagID, refTagID);
        if (metadata == null) {
            return null;
        }
        val values = metadata.left;
        val refString = metadata.right;

        val sign = (refString.equalsIgnoreCase(orientation)) ? 1 : -1;

        return sign * (values[0].doubleValue()
                + values[1].doubleValue() / 60
                + values[2].doubleValue() / 3600);
    }

    @NotNull
    private String readGPSCoordinateDMS(int tagID, int refTagID) {
        val metadata = readMetadata(tagID, refTagID);
        if (metadata == null) {
            return "";
        }
        val values = metadata.left;
        val refString = metadata.right;

        return values[0].intValue() + "\u00B0"
                + values[1].intValue() + "'"
                + values[2].floatValue() + "\""
                + refString;
    }

    private Pair<Rational[], String> readMetadata(int tagID, int refTagID) {
        val metaValue = getValue(tagID);
        if (metaValue == null) {
            return null;
        }
        val values = ((UnsignedRationalMetaValue) metaValue).getRationalValues();
        if (values.length != 3) {
            return null;
        }
        val refMetaValue = getValue(refTagID);
        if (refMetaValue == null) {
            return null;
        }
        val refString = refMetaValue.getStringValue();
        if (refString == null) {
            return null;
        }
        return Pair.of(values, refString);
    }

    ////////// protected //////////////////////////////////////////////////////

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
        return GPSTags.class;
    }

    ////////// private ////////////////////////////////////////////////////////

    @Getter
    private final String name = "GPS";

    /**
     * Add the tag mappings.
     *
     * @param id The tag's ID.
     * @param name The tag's name.
     * @param type The tag's {@link ImageMetaType}.
     */
    private static void add( int id, String name, ImageMetaType type ) {
        val tagInfo = new ImageMetaTagInfo( id, name, type, false );
        m_tagsByID.put( id, tagInfo );
        m_tagsByName.put( name, tagInfo );
    }

    /**
     * This is where the actual labels for the tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.GPSTags"
    );

    /**
     * A mapping of tags by ID.
     */
    private static final Map<Integer,ImageMetaTagInfo> m_tagsByID =
        new HashMap<Integer,ImageMetaTagInfo>();

    /**
     * A mapping of tags by name.
     */
    private static final Map<String,ImageMetaTagInfo> m_tagsByName =
        new HashMap<String,ImageMetaTagInfo>();

    static {
        add( GPS_ALTITUDE, "GPSAltitude", META_URATIONAL );
        add( GPS_ALTITUDE_REF, "GPSAltitudeRef", META_UBYTE );
        add( GPS_AREA_INFORMATION, "GPSAreaInformation", META_UNDEFINED );
        add( GPS_DATE_STAMP, "GPSDateStamp", META_STRING );
        add( GPS_DEST_BEARING, "GPSDestBearing", META_URATIONAL );
        add( GPS_DEST_BEARING_REF, "GPSDestBearingRef", META_STRING );
        add( GPS_DEST_DISTANCE, "GPSDestDistance", META_URATIONAL );
        add( GPS_DEST_DISTANCE_REF, "GPSDestDistanceRef", META_STRING );
        add( GPS_DEST_LATITUDE, "GPSDestLatitude", META_URATIONAL );
        add( GPS_DEST_LATITUDE_REF, "GPSDestLatitudeRef", META_STRING );
        add( GPS_DEST_LONGITUDE, "GPSDestLongitude", META_URATIONAL );
        add( GPS_DEST_LONGITUDE_REF, "GPSDestLongitudeRef", META_STRING );
        add( GPS_DIFFERENTIAL, "GPSDifferential", META_USHORT );
        add( GPS_DOP, "GPSDop", META_URATIONAL );
        add( GPS_IMG_DIRECTION, "GPSImgDirection", META_URATIONAL );
        add( GPS_IMG_DIRECTION_REF, "GPSImgDirectionRef", META_STRING );
        add( GPS_LATITUDE, "GPSLatitude", META_URATIONAL );
        add( GPS_LATITUDE_REF, "GPSLatitudeRef", META_STRING );
        add( GPS_LONGITUDE, "GPSLongitude", META_URATIONAL );
        add( GPS_LONGITUDE_REF, "GPSLongitudeRef", META_STRING );
        add( GPS_MAP_DATUM, "GPSMapDatum", META_STRING );
        add( GPS_MEASURE_MODE, "GPSMeasureMode", META_STRING );
        add( GPS_PROCESSING_METHOD, "GPSProcessingMethod", META_UNDEFINED );
        add( GPS_SATELLITES, "GPSSatellites", META_STRING );
        add( GPS_SPEED, "GPSSpeed", META_URATIONAL );
        add( GPS_SPEED_REF, "GPSSpeedRef", META_STRING );
        add( GPS_STATUS, "GPSStatus", META_STRING );
        add( GPS_TIME_STAMP, "GPSTimeStamp", META_URATIONAL );
        add( GPS_TRACK, "GPSTrack", META_URATIONAL );
        add( GPS_TRACK_REF, "GPSTrackRef", META_STRING );
        add( GPS_VERSION_ID, "GPSVersionID", META_UBYTE );
    }
}
/* vim:set et sw=4 ts=4: */
