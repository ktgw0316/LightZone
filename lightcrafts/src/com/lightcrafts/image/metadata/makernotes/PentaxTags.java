/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetaTags;

/**
 * A <code>PentaxTags</code> defines the constants used for Pentax maker note
 * metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface PentaxTags extends ImageMetaTags {

    /**
     * Auto AF point
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>65535 =&nbsp;</td><td>none</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>top-left</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>top-center</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>top-right</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>left</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>center</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>right</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>bottom-left</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td><td>bottom-center</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>9 =&nbsp;</td><td>bottom-right</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_AUTO_AF_POINT            = 0x000F;

    /**
     * Black point.
     * <p>
     * Type: Unsigned short (4).
     */
    int PENTAX_BLACK_POINT              = 0x0200;

    /**
     * Blue balance.
     * The value is n / 256.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_BLUE_BALANCE             = 0x001B;

    /**
     * Contrast.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>high</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>medium low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>medium high</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_CONTRAST                 = 0x0020;

    /**
     * Data dump.
     * <p>
     * Type: Undefined.
     */
    int PENTAX_DATA_DUMP                = 0x03FE;

    /**
     * Capture date.
     * The value is 4 bytes: YYYYMMDD.
     * The year is always big-endian.
     * <p>
     * Type: Undefined.
     */
    int PENTAX_DATE                     = 0x0006;

    /**
     * Destination city.
     * The values are the same as those for {@link #PENTAX_HOME_TOWN_CITY}.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_DESTINATION_CITY         = 0x0024;

    /**
     * Destination city code.
     * <p>
     * Type: Undefined.
     */
    int PENTAX_DESTINATION_CITY_CODE    = 0x1001;

    /**
     * Destination DST
     * The values are the same as those for {@link #PENTAX_HOME_TOWN_DST}.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_DESTINATION_DST          = 0x0026;

    /**
     * Digital zoom.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_DIGITAL_ZOOM             = 0x001E;

    /**
     * Exposure compensation.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_EXPOSURE_COMPENSATION    = 0x0016;

    /**
     * Exposure time.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_EXPOSURE_TIME            = 0x0012;

    /**
     * Flash mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>auto, did not fire</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>off</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>auto, did not fire, red-eye reduction</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>256 =&nbsp;</td><td>auto, fired</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>258 =&nbsp;</td><td>on</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>259 =&nbsp;</td><td>auto, fired, red-eye reduction</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>260 =&nbsp;</td><td>on, red-eye reduction</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>264 =&nbsp;</td><td>on, soft</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_FLASH_MODE               = 0x000C;

    /**
     * F-stop number.
     * The value is n / 10.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_FNUMBER                  = 0x0013;

    /**
     * Focal length.
     * If the camera model is one of Optio 30, 33WR, 43WR, 450, 550, 555,
     * 750Z or X, then the value is n / 10; otherwise, n / 100.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_FOCAL_LENGTH             = 0x001D;

    /**
     * Focus position.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_FOCUS_POSITION           = 0x0010;

    /**
     * Frame number.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_FRAME_NUMBER             = 0x0029;

    /**
     * Home-town city
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>Pago Pago</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>Honolulu</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>Anchorage</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>Vancouver</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>San Fransisco</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>Los Angeles</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>Calgary</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>Denver</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td><td>Mexico City</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>9 =&nbsp;</td><td>Chicago</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>10 =&nbsp;</td><td>Miami</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>11 =&nbsp;</td><td>Toronto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>12 =&nbsp;</td><td>New York</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>13 =&nbsp;</td><td>Santiago</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>14 =&nbsp;</td><td>Caracus</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>15 =&nbsp;</td><td>Halifax</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>16 =&nbsp;</td><td>Buenos Aires</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>17 =&nbsp;</td><td>Sao Paulo</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>18 =&nbsp;</td><td>Rio de Janeiro</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>19 =&nbsp;</td><td>Madrid</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>20 =&nbsp;</td><td>London</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>21 =&nbsp;</td><td>Paris</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>22 =&nbsp;</td><td>Milan</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>23 =&nbsp;</td><td>Rome</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>24 =&nbsp;</td><td>Berlin</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>25 =&nbsp;</td><td>Johannesburg</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>26 =&nbsp;</td><td>Istanbul</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>27 =&nbsp;</td><td>Cairo</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>28 =&nbsp;</td><td>Jerusalem</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>29 =&nbsp;</td><td>Moscow</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>30 =&nbsp;</td><td>Jeddah</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>31 =&nbsp;</td><td>Tehran</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>32 =&nbsp;</td><td>Dubai</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>33 =&nbsp;</td><td>Karachi</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>34 =&nbsp;</td><td>Kabul</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>35 =&nbsp;</td><td>Male</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>36 =&nbsp;</td><td>Delhi</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>37 =&nbsp;</td><td>Colombo</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>38 =&nbsp;</td><td>Kathmandu</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>39 =&nbsp;</td><td>Dacca</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>40 =&nbsp;</td><td>Yangon</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>41 =&nbsp;</td><td>Bangkok</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>42 =&nbsp;</td><td>Kuala Lumpur</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>43 =&nbsp;</td><td>Vientiane</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>44 =&nbsp;</td><td>Singapore</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>45 =&nbsp;</td><td>Phnom Penh</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>46 =&nbsp;</td><td>Ho Chi Minh</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>47 =&nbsp;</td><td>Jakarta</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>48 =&nbsp;</td><td>Hong Kong</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>49 =&nbsp;</td><td>Perth</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>50 =&nbsp;</td><td>Beijing</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>51 =&nbsp;</td><td>Shanghai</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>52 =&nbsp;</td><td>Manila</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>53 =&nbsp;</td><td>Taipei</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>54 =&nbsp;</td><td>Seoul</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>55 =&nbsp;</td><td>Adelaide</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>56 =&nbsp;</td><td>Tokyo</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>57 =&nbsp;</td><td>Guam</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>58 =&nbsp;</td><td>Sydney</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>59 =&nbsp;</td><td>Noumea</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>60 =&nbsp;</td><td>Wellington</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>61 =&nbsp;</td><td>Auckland</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>62 =&nbsp;</td><td>Lima</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>63 =&nbsp;</td><td>Dakar</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>64 =&nbsp;</td><td>Algiers</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>65 =&nbsp;</td><td>Helsinki</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>66 =&nbsp;</td><td>Athens</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>67 =&nbsp;</td><td>Nairobi</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>68 =&nbsp;</td><td>Amsterdam</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>69 =&nbsp;</td><td>Stockholm</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_HOME_TOWN_CITY           = 0x0023;

    /**
     * Home-town city code.
     * <p>
     * Type: Undefined.
     */
    int PENTAX_HOME_TOWN_CITY_CODE      = 0x1000;

    /**
     * Home-town DST.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>no</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>yes</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_HOME_TOWN_DST            = 0x0025;

    /**
     * Image size.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>640 x 480</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>full</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>1024 x 768</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>1280 x 960</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>1600 x 1200</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>2048 x 1536</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td><td>2560 x 1920</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>19 =&nbsp;</td><td>320 x 240</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>21 =&nbsp;</td><td>2592 x 1944</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>22 =&nbsp;</td><td>2304 x 1728</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_IMAGE_SIZE               = 0x0009;

    /**
     * ISO
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>50</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>64</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>80</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>100</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>125</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td><td>160</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>9 =&nbsp;</td><td>200</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>10 =&nbsp;</td><td>250</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>11 =&nbsp;</td><td>320</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>12 =&nbsp;</td><td>400</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>13 =&nbsp;</td><td>500</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>14 =&nbsp;</td><td>640</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>15 =&nbsp;</td><td>800</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>16 =&nbsp;</td><td>1000</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>17 =&nbsp;</td><td>1250</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>18 =&nbsp;</td><td>1600</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>21 =&nbsp;</td><td>3200</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>50 =&nbsp;</td><td>50</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>100 =&nbsp;</td><td>100</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>200 =&nbsp;</td><td>200</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>400 =&nbsp;</td><td>400</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>800 =&nbsp;</td><td>800</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1600 =&nbsp;</td><td>1600</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3200 =&nbsp;</td><td>3200</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_ISO                      = 0x0014;

    /**
     * Lens type.
     * The first byte is a lens group ID and the second byte is a lens ID
     * within that group.
     * <p>
     * Type: Unsigned byte (2).
     */
    int PENTAX_LENS_TYPE                = 0x003F;

    /**
     * Metering mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>multi-segment</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>center-weighted</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>spot</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_METERING_MODE            = 0x0017;

    /**
     * Mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>night</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>manual</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_MODE                     = 0x0001;

    /**
     * Picture mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>program</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>landscape</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td><td>sport</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>9 =&nbsp;</td><td>night scene</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>11 =&nbsp;</td><td>soft</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>12 =&nbsp;</td><td>surf & snow</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>13 =&nbsp;</td><td>sunset</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>14 =&nbsp;</td><td>autumn</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>15 =&nbsp;</td><td>flower</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>17 =&nbsp;</td><td>fireworks</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>18 =&nbsp;</td><td>text</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>19 =&nbsp;</td><td>panorama</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>30 =&nbsp;</td><td>self portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>37 =&nbsp;</td><td>museum</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>38 =&nbsp;</td><td>food</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>40 =&nbsp;</td><td>green mode</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>49 =&nbsp;</td><td>light pet</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>50 =&nbsp;</td><td>dark pet</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>51 =&nbsp;</td><td>medium pet</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>53 =&nbsp;</td><td>underwater</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>54 =&nbsp;</td><td>candlelight</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>55 =&nbsp;</td><td>natural skin tone</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>56 =&nbsp;</td><td>synchronous sound record</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_PICTURE_MODE             = 0x000B;

    /**
     * Preview image data for Optio 330RS.
     * <p>
     * Type: Undefined.
     */
    int PENTAX_PREVIEW_IMAGE_DATA       = 0x2000;

    /**
     * Preview image length.
     * <p>
     * Type: Unsigned long.
     */
    int PENTAX_PREVIEW_IMAGE_LENGTH     = 0x0003;

    /**
     * Preview image size.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_PREVIEW_IMAGE_SIZE       = 0x0002;

    /**
     * Preview image start.
     * <p>
     * Type: Unsigned long.
     */
    int PENTAX_PREVIEW_IMAGE_START      = 0x0004;

    /**
     * Print image matching.
     * <p>
     * Type: Undefined.
     */
    int PENTAX_PRINT_IMAGE_MATCHING     = 0x0E00;

    /**
     * Quality.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>good</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>better</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>best</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>TIFF</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>RAW</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_QUALITY                  = 0x0008;

    /**
     * Raw image size (width, height).
     * <p>
     * Type: Unsigned short (2).
     */
    int PENTAX_RAW_IMAGE_SIZE           = 0x0039;

    /**
     * Red balance.
     * The value is n / 256.
     * <p>
     * Type: Unsigned short.
     */
    int PENTAX_RED_BALANCE              = 0x001C;

    /**
     * Saturation
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>high</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>medium low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>medium high</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_SATURATION               = 0x001F;

    /**
     * Sharpness
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>soft</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>hard</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>medium soft</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>medium hard</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_SHARPNESS                = 0x0021;

    /**
     * Capture time.
     * The value is 3 bytes: HHMMSS.
     * <p>
     * Type: Undefined.
     */
    int PENTAX_TIME                     = 0x0007;

    /**
     * Tone curve.
     * <p>
     * Type: Undefined.
     */
    int PENTAX_TONE_CURVE               = 0x0402;

    /**
     * Tone curves.
     * <p>
     * Type: Undefined.
     */
    int PENTAX_TONE_CURVES              = 0x0403;

    /**
     * White balance.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>daylight</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>shade</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>fluorescent</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>tungsten</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>manual</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_WHITE_BALANCE            = 0x0019;

    /**
     * White balance mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>auto daylight</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>auto shade</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>auto flash</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>auto tungsten</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>65535 =&nbsp;</td><td>user-selected</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>65534 =&nbsp;</td><td>preset</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_WHITE_BALANCE_MODE       = 0x001A;

    /**
     * White point.
     * <p>
     * Type: Unsigned short (4).
     */
    int PENTAX_WHITE_POINT              = 0x0201;

    /**
     * World time location.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>home town</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>destination</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PENTAX_WORLD_TIME_LOCATION      = 0x0022;

}
/* vim:set et sw=4 ts=4: */
