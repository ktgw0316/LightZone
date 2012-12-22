/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

/**
 * A <code>GPSTags</code> defines the constants for GPS metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface GPSTags extends ImageMetaTags {

    /**
     * Indicates the altitude based on the reference in
     * {@link #GPS_ALTITUDE_REF} Altitude is expressed as one [unsigned]
     * rational value.  The reference unit is meters.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_ALTITUDE            = 0x0006;

    /**
     * Indicates the altitude used as the reference altitude. If the reference
     * is sea level and the altitude is above sea level, 0 is given. If the
     * altitude is below sea level, a value of 1 is given and the altitude is
     * indicated as an absolute value in the GPSAltitude tag. The reference
     * unit is meters. Note that this tag is an [unsigned] byte type, unlike
     * other reference tags.
     * <p>
     * Type: Unsigned byte.
     */
    int GPS_ALTITUDE_REF        = 0x0005;

    /**
     * A character string recording the name of the GPS area. The first byte
     * indicates the character code used (Table 6?  Table 7), and this is
     * followed by the name of the GPS area. Since the Type is not ASCII, NULL
     * termination is not necessary.
     * <p>
     * Type: Undefined.
     */
    int GPS_AREA_INFORMATION    = 0x001C;

    /**
     * A character string recording date and time information relative to UTC
     * (Coordinated Universal Time). The format is "YYYY:MM:DD." The length of
     * the string is 11 bytes including NULL.
     * <p>
     * Type: ASCII.
     */
    int GPS_DATE_STAMP          = 0x001D;

    /**
     * Indicates the bearing to the destination point. The range of values is
     * from 0.00 to 359.99.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_DEST_BEARING        = 0x0018;

    /**
     * Indicates the reference used for giving the bearing to the destination
     * point. 'T' denotes true direction and 'M' is magnetic direction.
     * <p>
     * Type: ASCII.
     */
    int GPS_DEST_BEARING_REF    = 0x0017;

    /**
     * Indicates the distance to the destination point.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_DEST_DISTANCE       = 0x001A;

    /**
     * Indicates the unit used to express the distance to the destination
     * point. 'K', 'M' and 'N' represent kilometers, miles and knots.
     * <p>
     * Type: ASCII.
     */
    int GPS_DEST_DISTANCE_REF   = 0x0019;

    /**
     * Indicates the latitude of the destination point. The latitude is
     * expressed as three [unsigned] rational values giving the degrees,
     * minutes, and seconds, respectively. If latitude is expressed as degrees,
     * minutes and seconds, a typical format would be dd/1,mm/1,ss/1. When
     * degrees and minutes are used and, for example, fractions of minutes are
     * given up to two decimal places, the format would be dd/1,mmmm/100,0/1.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_DEST_LATITUDE       = 0x0014;

    /**
     * Indicates whether the latitude of the destination point is north or
     * south latitude. The ASCII value 'N' indicates north latitude, and 'S' is
     * south latitude.
     * <p>
     * Type: ASCII.
     */
    int GPS_DEST_LATITUDE_REF   = 0x0013;

    /**
     * Indicates the longitude of the destination point. The longitude is
     * expressed as three [unsigned] rational values giving the grees, minutes,
     * and seconds, respectively. If longitude is expressed as degrees, minutes
     * and seconds, a typical rmat would be ddd/1,mm/1,ss/1. When degrees and
     * minutes are used and, for example, fractions of minutes are ven up to
     * two decimal places, the format would be ddd/1,mmmm/100,0/1.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_DEST_LONGITUDE      = 0x0016;

    /**
     * Indicates whether the longitude of the destination point is east or west
     * longitude. ASCII 'E' indicates east longitude, and 'W' is west
     * longitude.
     * <p>
     * Type: ASCII.
     */
    int GPS_DEST_LONGITUDE_REF  = 0x0015;

    /**
     * Indicates whether differential correction is applied to the GPS
     * receiver.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>no/a</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>yes</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int GPS_DIFFERENTIAL        = 0x001E;

    /**
     * Indicates the GPS DOP (data degree of precision). An HDOP value is
     * written during two-dimensional measurement, and PDOP during
     * three-dimensional measurement.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_DOP                 = 0x000B;

    /**
     * Indicates the direction of the image when it was captured. The range of
     * values is from 0.00 to 359.99.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_IMG_DIRECTION       = 0x0011;

    /**
     * Indicates the reference for giving the direction of the image when it is
     * captured. 'T' denotes true direction and 'M' is magnetic direction.
     * <p>
     * Type: ASCII.
     */
    int GPS_IMG_DIRECTION_REF   = 0x0010;

    /**
     * Indicates the latitude. The latitude is expressed as three [unsigned]
     * rational values giving the degrees, minutes, and seconds, respectively.
     * If latitude is expressed as degrees, minutes and seconds, a typical
     * format would be dd/1,mm/1,ss/1. When degrees and minutes are used and,
     * for example, fractions of minutes are given up to two decimal places,
     * the format would be dd/1,mmmm/100,0/1.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_LATITUDE            = 0x0002;

    /**
     * Indicates whether the latitude is north or south latitude. The ASCII
     * value 'N' indicates north latitude, and 'S' is south latitude.
     * <p>
     * Type: ASCII.
     */
    int GPS_LATITUDE_REF        = 0x0001;

    /**
     * Indicates the longitude. The longitude is expressed as three [unsigned]
     * rational values giving the degrees, minutes, and seconds, respectively.
     * If longitude is expressed as degrees, minutes and seconds, a typical
     * format would be ddd/1,mm/1,ss/1. When degrees and minutes are used and,
     * for example, fractions of minutes are given up to two decimal places,
     * the format would be ddd/1,mmmm/100,0/1.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_LONGITUDE           = 0x0004;

    /**
     * Indicates whether the longitude is east or west longitude. ASCII 'E'
     * indicates east longitude, and 'W' is west longitude.
     * <p>
     * Type: ASCII
     */
    int GPS_LONGITUDE_REF       = 0x0003;

    /**
     * Indicates the geodetic survey data used by the GPS receiver. If the
     * survey data is restricted to Japan, the value of this tag is 'TOKYO' or
     * 'WGS-84'. If a GPS Info tag is recorded, it is strongly recommended that
     * this tag be recorded.
     * <p>
     * Type: ASCII.
     */
    int GPS_MAP_DATUM           = 0x0012;

    /**
     * Indicates the GPS measurement mode. '2' means two-dimensional
     * measurement and '3' means three-dimensional measurement is in progress.
     * <p>
     * Type: ASCII.
     */
    int GPS_MEASURE_MODE        = 0x000A;

    /**
     * A character string recording the name of the method used for location
     * finding. The first byte indicates the character code used (Table 6?Table
     * 7), and this is followed by the name of the method. Since the Type is
     * not ASCII, NULL termination is not necessary.
     * <p>
     * Type: Undefined.
     */
    int GPS_PROCESSING_METHOD   = 0x001B;

    /**
     * Indicates the GPS satellites used for measurements. This tag can be used
     * to describe the number of satellites, their ID number, angle of
     * elevation, azimuth, SNR and other information in ASCII notation. The
     * format is not specified. If the GPS receiver is incapable of taking
     * measurements, value of the tag shall be set to NULL.
     * <p>
     * Type: ASCII.
     */
    int GPS_SATELLITES          = 0x0008;

    /**
     * Indicates the speed of GPS receiver movement.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_SPEED               = 0x000D;

    /**
     * Indicates the unit used to express the GPS receiver speed of movement.
     * 'K' 'M' and 'N' represents kilometers per hour, miles per hour, and
     * knots.
     * <p>
     * Type: ASCII.
     */
    int GPS_SPEED_REF           = 0x000C;

    /**
     * Indicates the status of the GPS receiver when the image is recorded. 'A'
     * means measurement is in progress, and 'V' means the measurement is
     * Interoperability.
     * <p>
     * Type: ASCII.
     */
    int GPS_STATUS              = 0x0009;

    /**
     * Indicates the time as UTC (Coordinated Universal Time). TimeStamp is
     * expressed as three [unsigned] rational values giving the hour, minute,
     * and second.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_TIME_STAMP          = 0x0007;

    /**
     * Indicates the direction of GPS receiver movement. The range of values is
     * from 0.00 to 359.99.
     * <p>
     * Type: Unsigned rational.
     */
    int GPS_TRACK               = 0x000F;

    /**
     * Indicates the reference for giving the direction of GPS receiver
     * movement. 'T' denotes true direction and 'M' is magnetic direction.
     * <p>
     * Type: ASCII.
     */
    int GPS_TRACK_REF           = 0x000E;

    /**
     * Indicates the version of GPSInfoIFD. The version is given as 2.2.0.0.
     * This tag is mandatory when GPSInfo tag is present. Note that the
     * <code>GPSVersionID</code> tag is written as a different byte than the
     * Exif Version tag.
     * <p>
     * Type: Unsigned byte.
     */
    int GPS_VERSION_ID          = 0x0000;

}
/* vim:set et sw=4 ts=4: */
