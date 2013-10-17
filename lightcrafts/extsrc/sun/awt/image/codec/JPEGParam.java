/* JPEGParam.java -- keeps track of encode and decode parameters for JPEG.
 * Copyright (C) 2011 Red Hat
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package sun.awt.image.codec;

import java.util.Arrays;

import com.sun.image.codec.jpeg.JPEGDecodeParam;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGHuffmanTable;
import com.sun.image.codec.jpeg.JPEGQTable;
import com.sun.imageio.plugins.jpeg.JPEG;

/**
 * This class encapsulates the information about encoding and decoding the JPEG
 * image.
 *
 * @author Andrew Su (asu@redhat.com)
 *
 */
public class JPEGParam implements JPEGEncodeParam {
    /*
     * NOTE: bands mean the same thing as components, trying to keep it
     * Consistent with the documentation
     *
     * NOTE: subsampling is not done implementing.
     */

    private float quality = JPEG.DEFAULT_QUALITY;
    private int colorID = -1;
    private int width;
    private int height;
    private int numBands;
    private boolean imageInfoValid = false;
    private boolean tableInfoValid = false;
    private JPEGQTable[] qTable = new JPEGQTable[NUM_TABLES];
    private JPEGHuffmanTable[] acHuffmanTable = new JPEGHuffmanTable[NUM_TABLES];
    private JPEGHuffmanTable[] dcHuffmanTable = new JPEGHuffmanTable[NUM_TABLES];

    private int restartInterval = 0;
    private int[] horizontalSubsampleComponents;
    private int[] verticalSubsampleComponents;

    /* [marker between 0xE0 to 0xEF minus 0xE0 to get index][data] */
    private byte[][][] markers = new byte[16][][];
    private byte[][] commentMarker = null;

    /* number of components each color id has (color id from JPEGDecodeParam) */
    private static int[] components = { 0, 1, 3, 3, 4, 3, 4, 4, 4, 4, 4, 4, };
    private int[] qTableComponentMapping;
    private int[] acHuffmanComponentMapping;
    private int[] dcHuffmanComponentMapping;

    /*
     * Breakdown for marker bytes
     * 5 for name.
     * 2 for version.
     * 1 for density type.
     * 2 for x density.
     * 2 for y density.
     * 2 for thumbnail.
     */
    private byte APP0_MARKER_NUM_BYTES = 14;

    public JPEGParam(JPEGEncodeParam param) {
        this((JPEGDecodeParam) param);
    }

    public JPEGParam(JPEGDecodeParam param) {
        this(param.getEncodedColorID(), param.getNumComponents());

        setTableInfoValid(param.isTableInfoValid());
        setImageInfoValid(param.isImageInfoValid());
        setRestartInterval(param.getRestartInterval());

        // Copy the Q tables and Huffman tables.
        for (int i = 0; i < NUM_TABLES; i++) {
            qTable[i] = param.getQTable(i);
            acHuffmanTable[i] = param.getACHuffmanTable(i);
            dcHuffmanTable[i] = param.getDCHuffmanTable(i);
        }

        // Next we want to copy the component mappings.
        for (int i = 0; i < getNumComponents(); i++) {
            setQTableComponentMapping(i, param.getQTableComponentMapping(i));
            setACHuffmanComponentMapping(i,
                    param.getACHuffmanComponentMapping(i));
            setDCHuffmanComponentMapping(i,
                    param.getDCHuffmanComponentMapping(i));
        }

        // Copy all the marker data.
        for (int i = APP0_MARKER; i < APPF_MARKER; i++) {
            byte[][] markerData = param.getMarkerData(i);
            byte[][] copyMarkerData = null;
            if (markerData != null) {
                copyMarkerData = new byte[markerData.length][];
                for (int j = 0; j < markerData.length; j++) {
                    copyMarkerData[j] = Arrays.copyOf(markerData[j],
                            markerData[j].length);
                }
            }
            setMarkerData(i, copyMarkerData);
        }

        byte[][] commentData = param.getMarkerData(COMMENT_MARKER);
        byte[][] copyCommentData = null;
        if (commentData != null) {
            copyCommentData = new byte[commentData.length][];
            for (int i = 0; i < commentData.length; i++) {
                copyCommentData[i] = Arrays.copyOf(commentData[i],
                        commentData[i].length);
            }
            setMarkerData(COMMENT_MARKER, copyCommentData);
        }
    }

    public JPEGParam(int colorID) {
        this(colorID, components[colorID]);
    }

    public JPEGParam(int colorID, int numBands) {
        // We were given an invalid color id, or the number of bands given to us
        // did not match requirements.
        if (colorID < 0
                || colorID >= JPEGDecodeParam.NUM_COLOR_ID
                || (colorID != COLOR_ID_UNKNOWN && numBands != components[colorID])) {
            throw new IllegalArgumentException();
        }
        this.colorID = colorID;
        this.numBands = numBands;

        initialize();
    }

    private void initialize() {

        qTable[0] = JPEGQTable.StdLuminance;
        qTable[1] = JPEGQTable.StdChrominance;

        acHuffmanTable[0] = JPEGHuffmanTable.StdACLuminance;
        acHuffmanTable[1] = JPEGHuffmanTable.StdACChrominance;

        dcHuffmanTable[0] = JPEGHuffmanTable.StdDCLuminance;
        dcHuffmanTable[1] = JPEGHuffmanTable.StdDCChrominance;

        qTableComponentMapping = new int[getNumComponents()];
        acHuffmanComponentMapping = new int[getNumComponents()];
        dcHuffmanComponentMapping = new int[getNumComponents()];

        horizontalSubsampleComponents = new int[getNumComponents()];
        verticalSubsampleComponents = new int[getNumComponents()];

        /*
         * we can just set these to true since they are using default values
         * right now
         */
        setTableInfoValid(true);
        setImageInfoValid(true);

        setMarkerData(APP0_MARKER,
                arrayAdd(getMarkerData(APP0_MARKER), createAPP0MarkerData()));

    }

    private byte[] createAPP0MarkerData() {
        byte[] data = null;
        // Create JFIF APP0 Marker if compatible.
        // By compatible, it must be one of the following cases.
        // Reference:
        // http://www.jpeg.org/public/jfif.pdf
        // http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/JFIF.html
        switch (colorID) {
            case COLOR_ID_UNKNOWN:
            case COLOR_ID_GRAY:
            case COLOR_ID_RGB:
            case COLOR_ID_YCbCr:
            case COLOR_ID_CMYK:
                data = new byte[APP0_MARKER_NUM_BYTES];

                // Null terminated JFIF string. [5 bytes]
                data[0] = 'J';
                data[1] = 'F';
                data[2] = 'I';
                data[3] = 'F';
                data[4] = 0x0;

                // Version number [2 bytes]
                data[5] = 1;
                data[6] = 2;

                // Density unit [1 byte]
                data[7] = DENSITY_UNIT_ASPECT_RATIO;

                // X density [2 bytes]
                data[8] = 0;
                data[9] = 1;

                // Y density [2 bytes]
                data[10] = 0;
                data[11] = 1;

                // Thumbnail [2 bytes]
                data[12] = 0;
                data[13] = 0;
                break;
        }

        return data;
    }

    public void setQuality(float quality, boolean forceBaseline) {
        if (quality < 0.0) {
            quality = 0.00f;
        } else if (quality > 1.0) {
            quality = 1.0f;
        }

        this.quality = quality; // preserve original.

        /*
         * Since quality value of 1 is the lowest compression, we want our
         * QTable to contain as much 1s as possible. Since scaling is by a
         * factor, we want to invert the selection such that highest quality is
         * 0 and lowest is 1.
         */
        quality = 1 - quality;

        // We will scale our QTables to match the quality value given to us.
        for (int i = 0; i < NUM_TABLES; i++) {
            if (qTable[i] != null) {
                qTable[i] = qTable[i].getScaledInstance(quality, forceBaseline);
            }
        }
    }

    public Object clone() {
        JPEGParam c = new JPEGParam(this);
        return c;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getHorizontalSubsampling(int component) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component");
        }

        return horizontalSubsampleComponents[component];
    }

    @Override
    public int getVerticalSubsampling(int component) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component");
        }

        return verticalSubsampleComponents[component];
    }

    @Override
    public JPEGQTable getQTable(int tableNum) {
        if (tableNum < 0 || tableNum > NUM_TABLES)
            throw new IllegalArgumentException("tableNum must be [0-"
                    + (NUM_TABLES - 1) + "]");
        return qTable[tableNum];
    }

    @Override
    public JPEGQTable getQTableForComponent(int component) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component");
        }

        return qTable[qTableComponentMapping[component]];
    }

    @Override
    public JPEGHuffmanTable getDCHuffmanTable(int tableNum) {
        if (tableNum < 0 || tableNum > NUM_TABLES)
            throw new IllegalArgumentException("tableNum must be [0-"
                    + (NUM_TABLES - 1) + "]");
        return dcHuffmanTable[tableNum];
    }

    @Override
    public JPEGHuffmanTable getDCHuffmanTableForComponent(int component) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component");
        }

        return dcHuffmanTable[dcHuffmanComponentMapping[component]];
    }

    @Override
    public JPEGHuffmanTable getACHuffmanTable(int tableNum) {
        if (tableNum < 0 || tableNum > NUM_TABLES)
            throw new IllegalArgumentException("tableNum must be [0-"
                    + (NUM_TABLES - 1) + "]");
        return acHuffmanTable[tableNum];
    }

    @Override
    public JPEGHuffmanTable getACHuffmanTableForComponent(int component) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component");
        }

        return acHuffmanTable[acHuffmanComponentMapping[component]];
    }

    @Override
    public int getDCHuffmanComponentMapping(int component) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component");
        }
        return dcHuffmanComponentMapping[component];
    }

    @Override
    public int getACHuffmanComponentMapping(int component) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component");
        }
        return acHuffmanComponentMapping[component];
    }

    @Override
    public int getQTableComponentMapping(int component) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component");
        }
        return qTableComponentMapping[component];
    }

    @Override
    public boolean isImageInfoValid() {
        return imageInfoValid;
    }

    @Override
    public boolean isTableInfoValid() {
        return tableInfoValid;
    }

    @Override
    public boolean getMarker(int marker) {
        byte[][] data = null;
        switch (marker) {
            case APP0_MARKER:
            case APP1_MARKER:
            case APP2_MARKER:
            case APP3_MARKER:
            case APP4_MARKER:
            case APP5_MARKER:
            case APP6_MARKER:
            case APP7_MARKER:
            case APP8_MARKER:
            case APP9_MARKER:
            case APPA_MARKER:
            case APPB_MARKER:
            case APPC_MARKER:
            case APPD_MARKER:
            case APPE_MARKER:
            case APPF_MARKER:
                data = markers[marker - APP0_MARKER];
                break;
            case COMMENT_MARKER:
                data = commentMarker;
                break;
            default:
                throw new IllegalArgumentException("Marker provided is invalid");
        }

        return data != null && data.length > 0;
    }

    @Override
    public byte[][] getMarkerData(int marker) {
        byte[][] data = null;

        switch (marker) {
            case APP0_MARKER:
            case APP1_MARKER:
            case APP2_MARKER:
            case APP3_MARKER:
            case APP4_MARKER:
            case APP5_MARKER:
            case APP6_MARKER:
            case APP7_MARKER:
            case APP8_MARKER:
            case APP9_MARKER:
            case APPA_MARKER:
            case APPB_MARKER:
            case APPC_MARKER:
            case APPD_MARKER:
            case APPE_MARKER:
            case APPF_MARKER:
                data = markers[marker - APP0_MARKER];
                break;
            case COMMENT_MARKER:
                // TODO: Add stuff for comment marker
                break;
            default:
                throw new IllegalArgumentException("Marker provided is invalid");
        }
        return data;
    }

    @Override
    public int getEncodedColorID() {
        return colorID;
    }

    @Override
    public int getNumComponents() {
        return numBands;
    }

    @Override
    public int getRestartInterval() {
        return restartInterval;
    }

    @Override
    public int getDensityUnit() {
        if (!getMarker(APP0_MARKER))
            throw new IllegalArgumentException("APP0 Marker not found.");
        byte[] data = getValidAPP0Marker();

        if (data == null)
            throw new IllegalArgumentException("No valid APP0 Marker found");

        return data[7];
    }

    @Override
    public int getXDensity() {
        if (!getMarker(APP0_MARKER))
            throw new IllegalArgumentException("APP0 Marker not found.");
        byte[] data = getValidAPP0Marker();

        if (data == null)
            throw new IllegalArgumentException("No valid APP0 Marker found");

        // data[8] is the upper portion of the density value
        // data[9] is the lower portion of the density value
        int upper = data[8] << 8; // Shift it so we can merge with lower value.
        int lower = data[9] & 0xFF; // Keep it in bounds 0 - 256
        return upper | lower; // Merge

    }

    @Override
    public int getYDensity() {
        if (!getMarker(APP0_MARKER))
            throw new IllegalArgumentException("APP0 Marker not found.");
        byte[] data = getValidAPP0Marker();

        if (data == null)
            throw new IllegalArgumentException("No valid APP0 Marker found");

        // data[10] is the upper portion of the density value
        // data[11] is the lower portion of the density value
        int upper = data[10] << 8; // Shift it so we can merge with lower value.
        int lower = data[11] & 0xFF;// Keep it in bounds 0 - 256
        return upper | lower; // merge
    }

    @Override
    public void setHorizontalSubsampling(int component, int subsample) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component");
        }

        horizontalSubsampleComponents[component] = subsample;
    }

    @Override
    public void setVerticalSubsampling(int component, int subsample) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component");
        }

        verticalSubsampleComponents[component] = subsample;
    }

    @Override
    public void setQTable(int tableNum, JPEGQTable qTable) {
        if (tableNum < 0 || tableNum > NUM_TABLES)
            throw new IllegalArgumentException("tableNum must be [0-"
                    + (NUM_TABLES - 1) + "]");

        this.qTable[tableNum] = qTable;
    }

    @Override
    public void setDCHuffmanTable(int tableNum, JPEGHuffmanTable huffTable) {
        if (tableNum < 0 || tableNum > NUM_TABLES)
            throw new IllegalArgumentException("tableNum must be [0-"
                    + (NUM_TABLES - 1) + "]");

        dcHuffmanTable[tableNum] = huffTable;
    }

    @Override
    public void setACHuffmanTable(int tableNum, JPEGHuffmanTable huffTable) {
        if (tableNum < 0 || tableNum > NUM_TABLES)
            throw new IllegalArgumentException("tableNum must be [0-"
                    + (NUM_TABLES - 1) + "]");
        acHuffmanTable[tableNum] = huffTable;
    }

    @Override
    public void setACHuffmanComponentMapping(int component, int table) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component specified.");
        } else if (table < 0 || table > NUM_TABLES) {
            throw new IllegalArgumentException("Invalid table specified");
        }

        acHuffmanComponentMapping[component] = table;
    }

    @Override
    public void setDCHuffmanComponentMapping(int component, int table) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component specified.");
        } else if (table < 0 || table > NUM_TABLES) {
            throw new IllegalArgumentException("Invalid table specified");
        }

        dcHuffmanComponentMapping[component] = table;
    }

    @Override
    public void setQTableComponentMapping(int component, int table) {
        if (component < 0 || component > getNumComponents()) {
            throw new IllegalArgumentException("Invalid component specified.");
        } else if (table < 0 || table > NUM_TABLES) {
            throw new IllegalArgumentException("Invalid table specified");
        }

        qTableComponentMapping[component] = table;
    }

    @Override
    public void setImageInfoValid(boolean flag) {
        imageInfoValid = flag;
    }

    @Override
    public void setTableInfoValid(boolean flag) {
        tableInfoValid = flag;
    }

    @Override
    public void setMarkerData(int marker, byte[][] data) {
        if (data == null) {
            return;
        }

        switch (marker) {
            case APP0_MARKER:
            case APP1_MARKER:
            case APP2_MARKER:
            case APP3_MARKER:
            case APP4_MARKER:
            case APP5_MARKER:
            case APP6_MARKER:
            case APP7_MARKER:
            case APP8_MARKER:
            case APP9_MARKER:
            case APPA_MARKER:
            case APPB_MARKER:
            case APPC_MARKER:
            case APPD_MARKER:
            case APPE_MARKER:
            case APPF_MARKER:
                markers[marker - APP0_MARKER] = data;
                break;
            case COMMENT_MARKER:
                commentMarker = data;
                break;
            default:
                throw new IllegalArgumentException("Marker provided is invalid");
        }
    }

    @Override
    public void addMarkerData(int marker, byte[] data) {
        if (data == null) {
            return;
        }
        switch (marker) {
            case APP0_MARKER:
            case APP1_MARKER:
            case APP2_MARKER:
            case APP3_MARKER:
            case APP4_MARKER:
            case APP5_MARKER:
            case APP6_MARKER:
            case APP7_MARKER:
            case APP8_MARKER:
            case APP9_MARKER:
            case APPA_MARKER:
            case APPB_MARKER:
            case APPC_MARKER:
            case APPD_MARKER:
            case APPE_MARKER:
            case APPF_MARKER:
                markers[marker - APP0_MARKER] = arrayAdd(markers[marker
                        - APP0_MARKER], data);
                break;
            case COMMENT_MARKER:
                commentMarker = arrayAdd(commentMarker, data);
                break;
            default:
                throw new IllegalArgumentException("Marker provided is invalid");
        }
    }

    @Override
    public void setRestartInterval(int restartInterval) {
        this.restartInterval = restartInterval;
    }

    @Override
    public void setDensityUnit(int unit) {
        if (unit < 0 || unit > NUM_DENSITY_UNIT) {
            throw new IllegalArgumentException("Invalid density unit.");
        }

        byte[] data = getValidAPP0Marker();
        if (data == null) { // We will create one now.
            data = createAPP0MarkerData();
            // markers[0] = array of APP0_MARKER
            markers[0] = arrayAdd(markers[0], data);
        }

        data[7] = (byte) unit;
    }

    @Override
    public void setXDensity(int density) {
        byte[] data = getValidAPP0Marker();
        if (data == null) { // We will create one now.
            data = createAPP0MarkerData();
            // markers[0] = array of APP0_MARKER
            markers[0] = arrayAdd(markers[0], data);
        }

        byte upper = (byte) (density >>> 8 & 0xFF); // unsigned shift to keep it
                                                    // positive
        byte lower = (byte) (density & 0xFF);
        data[8] = upper;
        data[9] = lower;
    }

    @Override
    public void setYDensity(int density) {
        byte[] data = getValidAPP0Marker();
        if (data == null) { // We will create one now.
            data = createAPP0MarkerData();
            // markers[0] = array of APP0_MARKER
            markers[0] = arrayAdd(markers[0], data);
        }

        byte upper = (byte) (density >>> 8 & 0xFF); // unsigned shift to keep it
                                                    // positive
        byte lower = (byte) (density & 0xFF);
        data[10] = upper;
        data[11] = lower;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * get the quality value.
     *
     * @return currently set quality value.
     */
    public float getQuality() {
        return quality;
    }

    /**
     * Appends new data to original array
     *
     * @param origArr
     * @param newArr
     * @return
     */
    private byte[][] arrayAdd(byte[][] origArr, byte[] newArr) {
        byte[][] newData;
        if (origArr != null) {
            newData = Arrays.copyOf(origArr, origArr.length + 1);
            newData[origArr.length] = Arrays.copyOf(newArr, newArr.length);
        } else {
            newData = new byte[1][];
            newData[0] = Arrays.copyOf(newArr, newArr.length);
        }

        return newData;
    }

    private byte[] getValidAPP0Marker() {
        byte[][] app0Markers = getMarkerData(APP0_MARKER);
        for (int i = 0; i < app0Markers.length; i++) {
            byte[] data = app0Markers[i];
            if (data[0] == 'J' && data[1] == 'F' && data[2] == 'I'
                    && data[3] == 'F' && data[4] == 0x0) {
                if (data[5] <= 1) { // version is 1 or below.
                    // We have a valid JFIF header.
                    return data;
                }
            }
        }
        return null;
    }
}
