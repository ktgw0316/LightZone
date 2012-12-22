/*
 * $RCSfile: IIPResolutionOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:28 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Vector;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import java.util.Map;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGDecodeParam;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An OpImage class to generate an image from an IIP connection. A single
 * resolution level of the remote IIP image is retrieved.
 *
 * @see com.lightcrafts.mediax.jai.operator.IIPDescriptor
 * @since 1.0
 */
public class IIPResolutionOpImage extends OpImage {
    // Tile dimension are fixed at 64x64.
    private static final int TILE_SIZE = 64;

    // Default dimensions in tiles of block of tiles to retrieve.
    private static final int TILE_BLOCK_WIDTH = 8;
    private static final int TILE_BLOCK_HEIGHT = 2;

    // Significant delimiters in responses.
    private static final char BLANK = ' ';
    private static final char COLON = ':';
    private static final char SLASH = '/';
    private static final char CR = 0x0d;
    private static final char LF = 0x0a;

    // Colorspace information
    private static final int CS_COLORLESS = 0x0;
    private static final int CS_MONOCHROME = 0x1;
    private static final int CS_PHOTOYCC = 0x2;
    private static final int CS_NIFRGB = 0x3;
    private static final int CS_PLANE_ALPHA = 0x7ffe;

    // Compression types
    private static final int TILE_UNCOMPRESSED = 0x0;
    private static final int TILE_SINGLE_COLOR = 0x1;
    private static final int TILE_JPEG = 0x2;
    private static final int TILE_INVALID = 0xffffffff;

    // cache the ImagingListener
    private static ImagingListener listener =
                JAI.getDefaultInstance().getImagingListener();

    /* The base URL string of the IIP server and image. */
    private String URLString;

    /* The desired resolution in IIP order: 0 is lowest resolution. */
    private int resolution;

    /* The desired sub-image. */
    private int subImage;

    /* The colorspace type. */
    private int colorSpaceType;

    /* Flag indicating whether the image has an opacity channel. */
    private boolean hasAlpha;

    /* Flag indicating whether the opacity channel is premultiplied. */
    private boolean isAlphaPremultilpied;

    /* The minimum tile in the X direction. */
    private int minTileX;

    /* The minimum tile in the Y direction. */
    private int minTileY;

    /* The number of tiles in the X direction. */
    private int numXTiles;

    /* The JPEGDecodeParam cache */
    private JPEGDecodeParam[] decodeParamCache = new JPEGDecodeParam[255];

    /* Property initialization flag. */
    private boolean arePropertiesInitialized = false;

    /* Tile block dimensions eventually used. */
    private int tileBlockWidth = TILE_BLOCK_WIDTH;
    private int tileBlockHeight = TILE_BLOCK_HEIGHT;

    /** cache to extract the ImagingListener. */
    private RenderingHints renderHints;

    /*
     * Convert YCbCr to NIF RGB using the appropriate algorithm.
     */
    private static final void YCbCrToNIFRGB(Raster raster) {
        byte[] data = ((DataBufferByte)raster.getDataBuffer()).getData();

        int offset = 0;
        int length = data.length;
        int MASK1 = 0x000000ff;
        int MASK2 = 0x0000ff00;
        if(raster.getSampleModel().getNumBands() == 3) {
            while(offset < length) {
                float Y  = data[offset] & 0xff;
                float Cb = data[offset + 1] & 0xff;
                float Cr = data[offset + 2] & 0xff;

                int R = (int)(Y + 1.40200F * Cr - 178.255F);
                int G = (int)(Y - 0.34414F * Cb - 0.71414F * Cr + 135.4307F);
                int B = (int)(Y + 1.77200F * Cb - 225.43F);

                int imask = (R >> 5) & 0x18;
                data[offset++] =
                    (byte)(((R & (MASK1 >> imask)) | (MASK2 >> imask)) & 0xff);

                imask = (G >> 5) & 0x18;
                data[offset++] =
                    (byte)(((G & (MASK1 >> imask)) | (MASK2 >> imask)) & 0xff);

                imask = (B >> 5) & 0x18;
                data[offset++] =
                    (byte)(((B & (MASK1 >> imask)) | (MASK2 >> imask)) & 0xff);
            }
        } else { // numBands == 4 (premultiplied NIFRGB with opacity)
            while(offset < length) {
                float Y  = data[offset] & 0xff;
                float Cb = data[offset + 1] & 0xff;
                float Cr = data[offset + 2] & 0xff;

                int R = (int)(-Y - 1.40200F * Cr - 433.255F);
                int G = (int)(-Y + 0.34414F * Cb + 0.71414F * Cr + 119.5693F);
                int B = (int)(-Y - 1.77200F * Cb - 480.43F);

                int imask = (R >> 5) & 0x18;
                data[offset++] =
                    (byte)(((R & (MASK1 >> imask)) | (MASK2 >> imask)) & 0xff);

                imask = (G >> 5) & 0x18;
                data[offset++] =
                    (byte)(((G & (MASK1 >> imask)) | (MASK2 >> imask)) & 0xff);

                imask = (B >> 5) & 0x18;
                data[offset++] =
                    (byte)(((B & (MASK1 >> imask)) | (MASK2 >> imask)) & 0xff);

                offset++; // skip the opacity
            }
        }
    }

    /*
     * Post one or more IIP commands using the HTTP protocol and return a
     * stream from which the response(s) may be read. The "commands" parameter
     * may be null.
     */
    private static InputStream postCommands(String URLSpec,
                                            String[] commands) {
        // Construct the initial command by appending OBJ=IIP,1.0
        StringBuffer spec = new StringBuffer(URLSpec+"&OBJ=iip,1.0");

        if(commands != null) {
            // Append the commands to the string.
            for(int i = 0; i < commands.length; i++) {
                spec.append("&"+commands[i]);
            }
        }

        // Construct the URL and open a stream to read from it.
        InputStream stream = null;
        try {
            URL url = new URL(spec.toString());
            stream = url.openStream();
        } catch(Exception e) {
            String message =
                JaiI18N.getString("IIPResolution4") + spec.toString();
            listener.errorOccurred(message, new ImagingException(message, e),
                                   IIPResolutionOpImage.class, false);
//            throw new RuntimeException(e.getClass()+" "+e.getMessage());
        }

        return stream;
    }

    /*
     * Retrieve the label of the IIP server response. The label is defined
     * to be the bytes in the stream up to the first SLASH or COLON. The
     * returned value will be null if an EOS is reached before any characters
     * which are neither a SLASH or a COLON. If the returned String is non-null
     * it will be lower case.
     */
    private static String getLabel(InputStream stream) {
        boolean charsAppended = false;
        StringBuffer buf = new StringBuffer(16);
        try {
            int i;
            while((i = stream.read()) != -1) {
                char c = (char)(0x000000ff&i);
                if(c == SLASH || c == COLON) {
                    break;
                }
                buf.append(c);
                charsAppended = true;
            }
        } catch(Exception e) {
            String message =
                JaiI18N.getString("IIPResolution5");
            listener.errorOccurred(message, new ImagingException(message, e),
                                   IIPResolutionOpImage.class, false);
//            throw new RuntimeException(e.getClass()+" "+e.getMessage());
        }

        return charsAppended ? buf.toString().toLowerCase() : null;
    }

    /*
     * Retrieve the length of the data stream. This length is assumed to
     * be the an INT derived from the portion of the stream before the
     * first COLON.
     */
    private static int getLength(InputStream stream) {
        return Integer.valueOf(getLabel(stream)).intValue();
    }

    /*
     * Throw or print a RuntimeException in response to an error returned by
     * the IIP server. If no error was returned do nothing. Also grab and
     * discard the response to the "OBJ=iip" command which is always present.
     */
    private static InputStream checkError(String label,
                                          InputStream stream,
                                          boolean throwException) {
        if(label.equals("error")) {
            int length = Integer.valueOf(getLabel(stream)).intValue();
            byte[] b = new byte[length];
            try {
                stream.read(b);
            } catch(Exception e) {
                String message =
                    JaiI18N.getString("IIPResolution6");
                listener.errorOccurred(message, new ImagingException(message,e),
                                       IIPResolutionOpImage.class, false);
//                throw new RuntimeException(e.getClass()+" "+e.getMessage());
            }
            String msg = new String(b);
            if(throwException) {
                throwIIPException(msg);
            } else {
                printIIPException(msg);
            }
        } else if(label.startsWith("iip")) {
            // Ignore this response.
            String iipObjectResponse = getDataAsString(stream, false);
        }

        return stream;
    }

    /*
     * Returns the next segment of the stream until EOS or CRLF as a byte[].
     * The length of the data must be available as an INT in the stream before
     * the COLON.
     */
    private static byte[] getDataAsByteArray(InputStream stream) {
        int length = getLength(stream);
        byte[] b = new byte[length];

        try {
            stream.read(b);
            stream.read(); // CR
            stream.read(); // LF
        } catch(Exception e) {
            String message = JaiI18N.getString("IIPResolution7");
            listener.errorOccurred(message, new ImagingException(message,e),
                                   IIPResolutionOpImage.class, false);
//            throw new RuntimeException(e.getClass()+" "+e.getMessage());
        }

        return b;
    }

    /*
     * Returns the next segment of the stream until EOS or CRLF as a String.
     */
    private static String getDataAsString(InputStream stream,
                                          boolean hasLength) {
        String str = null;
        if(hasLength) {
            try {
                int length = getLength(stream);
                byte[] b = new byte[length];
                stream.read(b);
                stream.read(); // CR
                stream.read(); // LF
                str = new String(b);
            } catch(Exception e) {
                String message = JaiI18N.getString("IIPResolution7");
                listener.errorOccurred(message, new ImagingException(message,e),
                                       IIPResolutionOpImage.class, false);
//                throw new RuntimeException(e.getClass()+" "+e.getMessage());
            }
        } else {
            StringBuffer buf = new StringBuffer(16);
            try {
                int i;
                while((i = stream.read()) != -1) {
                    char c = (char)(0x000000ff&i);
                    if(c == CR) { // if last byte was CR
                        stream.read(); // LF
                        break;
                    }
                    buf.append(c);
                }
                str = buf.toString();
            } catch(Exception e) {
                String message = JaiI18N.getString("IIPResolution7");
                listener.errorOccurred(message, new ImagingException(message,e),
                                       IIPResolutionOpImage.class, false);
//                throw new RuntimeException(e.getClass()+" "+e.getMessage());
            }
        }

        return str;
    }

    /*
     * Flush the next segment of the stream until EOS or CRLF.
     */
    private static void flushData(InputStream stream,
                                  boolean hasLength) {
        if(hasLength) {
            try {
                int length = getLength(stream);
                long numSkipped = stream.skip(length);
                if(numSkipped == length) {
                    stream.read(); // CR
                    stream.read(); // LF
                }
            } catch(Exception e) {
                String message = JaiI18N.getString("IIPResolution8");
                listener.errorOccurred(message, new ImagingException(message,e),
                                       IIPResolutionOpImage.class, false);
//                throw new RuntimeException(e.getClass()+" "+e.getMessage());
            }
        } else {
            try {
                int i;
                while((i = stream.read()) != -1) {
                    if((char)(0x000000ff&i) == CR) { // if last byte was CR
                        stream.read(); // LF
                        break;
                    }
                }
            } catch(Exception e) {
                String message = JaiI18N.getString("IIPResolution8");
                listener.errorOccurred(message, new ImagingException(message,e),
                                       IIPResolutionOpImage.class, false);
//                throw new RuntimeException(e.getClass()+" "+e.getMessage());
            }
        }
    }

    /*
     * Convert a string containing BLANK-seprated INTs into an int[].
     */
    private static int[] stringToIntArray(String s) {
        // Parse string into a Vector.
        Vector v = new Vector();
        int lastBlank = 0;
        int nextBlank = s.indexOf(BLANK, 0);
        do {
            v.add(Integer.valueOf(s.substring(lastBlank, nextBlank)));
            lastBlank = nextBlank + 1;
            nextBlank = s.indexOf(BLANK, lastBlank);
        } while(nextBlank != -1);
        v.add(Integer.valueOf(s.substring(lastBlank)));

        // Convert the Vector to a int[].
        int length = v.size();
        int[] intArray = new int[length];
        for(int i = 0; i < length; i++) {
            intArray[i] = ((Integer)v.get(i)).intValue();
        }

        return intArray;
    }

    /*
     * Convert a string containing BLANK-seprated FLOATs into a float[].
     */
    private static float[] stringToFloatArray(String s) {
        // Parse string into a Vector.
        Vector v = new Vector();
        int lastBlank = 0;
        int nextBlank = s.indexOf(BLANK, 0);
        do {
            v.add(Float.valueOf(s.substring(lastBlank, nextBlank)));
            lastBlank = nextBlank + 1;
            nextBlank = s.indexOf(BLANK, lastBlank);
        } while(nextBlank != -1);
        v.add(Float.valueOf(s.substring(lastBlank)));

        // Convert the Vector to a float[].
        int length = v.size();
        float[] floatArray = new float[length];
        for(int i = 0; i < length; i++) {
            floatArray[i] = ((Float)v.get(i)).floatValue();
        }

        return floatArray;
    }

    /*
     * Format the argument into a generic IIP error message.
     */
    private static String formatIIPErrorMessage(String msg) {
        return new String(JaiI18N.getString("IIPResolutionOpImage0")+" "+msg);
    }

    /*
     * Throw a RuntimeException with the indicated message.
     */
    private static void throwIIPException(String msg) {
        throw new RuntimeException(formatIIPErrorMessage(msg));
    }

    /*
     * Print the supplied message to the standard error stream.
     */
    private static void printIIPException(String msg) {
        System.err.println(formatIIPErrorMessage(msg));
    }

    /*
     * Close the supplied stream ignoring any exceptions.
     */
    private static void closeStream(InputStream stream) {
        try {
            stream.close();
        } catch(Exception e) {
            // Ignore
        }
    }

    /*
     * Derive the image layout (tile grid, image bounds, ColorModel, and
     * SampleModel) by querying the IIP server.
     */
    private static ImageLayout layoutHelper(String URLSpec,
                                            int level,
                                            int subImage) {
        // Create an ImageLayout by construction or cloning.
        ImageLayout il = new ImageLayout();

        // Set the tile offsets to (0,0).
        il.setTileGridXOffset(0);
        il.setTileGridYOffset(0);

        // Set the tile dimensions.
        il.setTileWidth(TILE_SIZE);
        il.setTileHeight(TILE_SIZE);

        // Set the image origin to (0,0).
        il.setMinX(0);
        il.setMinY(0);

        // Retrieve the number of resolutions available and the maximum
        // width and height (the dimensions of resolution numRes - 1).
        int maxWidth = -1;
        int maxHeight = -1;
        int numRes = -1;
        int resolution = -1;
        String[] cmd = new String[] {"OBJ=Max-size", "OBJ=Resolution-number"};
        InputStream stream = postCommands(URLSpec, cmd);
        String label = null;
        while((label = getLabel(stream)) != null) {
            if(label.equals("max-size")) {
                String data = getDataAsString(stream, false);
                int[] wh = stringToIntArray(data);
                maxWidth = wh[0];
                maxHeight = wh[1];
            } else if(label.equals("resolution-number")) {
                String data = getDataAsString(stream, false);
                numRes = Integer.valueOf(data).intValue();
                if(level < 0) {
                    resolution = 0;
                } else if(level >= numRes) {
                    resolution = numRes - 1;
                } else {
                    resolution = level;
                }
            } else {
                checkError(label, stream, true);
            }
        }
        closeStream(stream);

        // Derive the width and height for this resolution level.
        int w = maxWidth;
        int h = maxHeight;
        for(int i = numRes - 1; i > resolution; i--) {
            w = (w + 1)/2;
            h = (h + 1)/2;
        }
        il.setWidth(w);
        il.setHeight(h);

        // Determine image opacity attributes.
        boolean hasAlpha = false;
        boolean isAlphaPremultiplied = false;
        cmd = new String[] {"OBJ=Colorspace,"+resolution+","+subImage};
        stream = postCommands(URLSpec, cmd);
        int colorSpaceIndex = 0;
        int numBands = 0;
        while((label = getLabel(stream)) != null) {
            if(label.startsWith("colorspace")) {
                int[] ia = stringToIntArray(getDataAsString(stream, false));
                numBands = ia[3];
                switch(ia[2]) {
                case CS_MONOCHROME:
                    colorSpaceIndex = ColorSpace.CS_GRAY;
                    break;
                case CS_PHOTOYCC:
                    colorSpaceIndex = ColorSpace.CS_PYCC;
                    break;
                case CS_NIFRGB:
                    colorSpaceIndex = ColorSpace.CS_sRGB;
                    break;
                default:
                    colorSpaceIndex = numBands < 3 ?
                        ColorSpace.CS_GRAY : ColorSpace.CS_sRGB;
                }
                for(int j = 1; j <= numBands; j++) {
                    if(ia[3+j] == CS_PLANE_ALPHA) {
                        hasAlpha = true;
                    }
                }
                isAlphaPremultiplied = ia[1] == 1;
            } else {
                checkError(label, stream, true);
            }
        }
        closeStream(stream);

        // Set the ColorModel.
        ColorSpace cs = ColorSpace.getInstance(colorSpaceIndex);
        int dtSize = DataBuffer.getDataTypeSize(DataBuffer.TYPE_BYTE);
        int[] bits = new int[numBands];
        for(int i = 0; i < numBands; i++) {
            bits[i] = dtSize;
        }
        int transparency = hasAlpha ?
            Transparency.TRANSLUCENT : Transparency.OPAQUE;
        ColorModel cm = new ComponentColorModel(cs, bits,
                                                hasAlpha, isAlphaPremultiplied,
                                                transparency,
                                                DataBuffer.TYPE_BYTE);
        il.setColorModel(cm);

        // Set the SampleModel.
        int[] bandOffsets = new int[numBands];
        for(int i = 0; i < numBands; i++) {
            bandOffsets[i] = i;
        }
        il.setSampleModel(RasterFactory.createPixelInterleavedSampleModel(
                                            DataBuffer.TYPE_BYTE,
                                            TILE_SIZE,
                                            TILE_SIZE,
                                            numBands,
                                            numBands*TILE_SIZE,
                                            bandOffsets));

        return il;
    }

    /**
     * Construct an OpImage given a String representation of a URL,
     * a resolution, and a sub-image index.
     *
     * @param URLSpec The URL of the IIP image including the FIF cimmand
     * if needed and possibly an SDS command.
     * @param level The resolution level with 0 as the lowest resolution.
     * @param subImage The subimage number.

     * @param layout The layout hint; may be null.
     */
    public IIPResolutionOpImage(Map config,
                                String URLSpec,
                                int level,
                                int subImage) {
        super((Vector)null, // the image is sourceless
              layoutHelper(URLSpec, level, subImage),
              config,
              false);

        this.renderHints = (RenderingHints)config;

        // Cache the constructor parameters.
        URLString = URLSpec;
        this.subImage = subImage;

        // Retrieve required parameters from server.
        String[] cmd = new String[] {"OBJ=Resolution-number"};
        InputStream stream = postCommands(cmd);
        String label = null;
        while((label = getLabel(stream)) != null) {
            if(label.equals("resolution-number")) {
                String data = getDataAsString(stream, false);
                int numRes = Integer.valueOf(data).intValue();
                if(level < 0) {
                    resolution = 0;
                } else if(level >= numRes) {
                    resolution = numRes - 1;
                } else {
                    resolution = level;
                }
            } else {
                checkError(label, stream, true);
            }
        }
        endResponse(stream);

        // Cache some values which will be used repetitively.
        ColorSpace cs = colorModel.getColorSpace();
        if(cs.isCS_sRGB()) {
            colorSpaceType = CS_NIFRGB;
        } else if(cs.equals(ColorSpace.getInstance(ColorSpace.CS_GRAY))) {
            colorSpaceType = CS_MONOCHROME;
        } else {
            colorSpaceType = CS_PHOTOYCC;
        }
        hasAlpha = colorModel.hasAlpha();
        isAlphaPremultilpied = colorModel.isAlphaPremultiplied();
        minTileX = getMinTileX();
        minTileY = getMinTileY();
        numXTiles = getNumXTiles();
    }

    /*
     * Post the array of commands to the IIP server.
     */
    private InputStream postCommands(String[] commands) {
        return postCommands(URLString, commands);
    }

    /*
     * End reading the server response.
     */
    private void endResponse(InputStream stream) {
        // Add if-then block and handle socket connection.
        closeStream(stream);
    }

    /*
     * Compute the tile with the specified position in the tile grid.
     * Either a block of tiles or a single tile is retrieved depending
     * on where the requested tile false in the tile grid with respect
     * to the upper left tile in the image. All tiles except that returned
     * are stored in the TileCache.
     */
    public Raster computeTile(int tileX, int tileY) {
        Raster raster = null;

        // If the tile is the upper left tile of a block of tiles
        // where the blocks are counted from the upper left corner
        // of the image then retrieve a block of tiles.
        if((tileX - minTileX) % tileBlockWidth == 0 &&
           (tileY - minTileY) % tileBlockHeight == 0) {
            int endTileX = tileX + tileBlockWidth - 1;
            if(endTileX > getMaxTileX()) {
                endTileX = getMaxTileX();
            }
            int endTileY = tileY + tileBlockHeight - 1;
            if(endTileY > getMaxTileY()) {
                endTileY = getMaxTileY();
            }
            raster = getTileBlock(tileX, tileY, endTileX, endTileY);
        } else if((raster = getTileFromCache(tileX, tileY)) == null) {
            raster = getTileBlock(tileX, tileY, tileX, tileY);
        }

        return raster;
    }

    /*
     * Extract from the IIP response label the coordinates in the tile grid
     * of the tile returned by the IIP server.
     *
     * @param label The IIP response label.
     * @param xy The tile grid coordinates; may be null.
     */
    private Point getTileXY(String label, Point xy) {
        // Get the tile number from the IIP response label.
        int beginIndex = label.indexOf(",", label.indexOf(",")+1)+1;
        int endIndex = label.lastIndexOf(",");
        int tile =
            Integer.valueOf(label.substring(beginIndex, endIndex)).intValue();

        // Calculate the tile coordinates.
        int tileX = (tile + minTileX) % numXTiles;
        int tileY = (tile + minTileX - tileX)/numXTiles + minTileY;

        // Create or set the location.
        if(xy == null) {
            xy = new Point(tileX, tileY);
        } else {
            xy.setLocation(tileX, tileY);
        }

        return xy;
    }

    /*
     * Retrieve a block of tiles from the IIP server. All tiles except
     * the upper left tile are stored in the TileCache wheareas the upper
     * left tile is returned.
     */
    private Raster getTileBlock(int upperLeftTileX, int upperLeftTileY,
                                int lowerRightTileX, int lowerRightTileY) {
        int startTile = (upperLeftTileY - minTileY)*numXTiles +
            upperLeftTileX - minTileX;
        int endTile = (lowerRightTileY - minTileY)*numXTiles +
            lowerRightTileX - minTileX;

        String cmd = null;
        if(startTile == endTile) { // single tile
            cmd = new String("til="+resolution+","+startTile+","+subImage);
        } else { // range of tiles
            cmd = new String("til="+resolution+","+startTile+"-"+
                             endTile+","+subImage);
        }
        InputStream stream = postCommands(new String[] {cmd});
        int compressionType = -1;
        int compressionSubType = -1;
        byte[] data = null;
        String label = null;
        Raster upperLeftTile = null;
        Point tileXY = new Point();
        while((label = getLabel(stream)) != null) {
            if(label.startsWith("tile")) {
                int length = getLength(stream);

                byte[] header = new byte[8];
                try {
                    stream.read(header);
                } catch(Exception e) {
                    throwIIPException(JaiI18N.getString("IIPResolutionOpImage1"));
                }

                length -= 8;

                compressionType =
                    (int)((header[3]<<24)|
                          (header[2]<<16)|
                          (header[1]<<8)|
                          header[0]);
                compressionSubType =
                    (int)((header[7]<<24)|
                          (header[6]<<16)|
                          (header[5]<<8)|
                          header[4]);

                if(length != 0) {
                    data = new byte[length];
                    try {
                        int numBytesRead = 0;
                        int offset = 0;
                        do {
                            numBytesRead = stream.read(data, offset,
                                                       length - offset);
                            offset += numBytesRead;
                        } while(offset < length && numBytesRead != -1);
                        if(numBytesRead != -1) {
                            stream.read(); // CR
                            stream.read(); // LF
                        }
                    } catch(Exception e) {
                        throwIIPException(JaiI18N.getString("IIPResolutionOpImage2"));
                    }
                }

                getTileXY(label, tileXY);
                int tileX = (int)tileXY.getX();
                int tileY = (int)tileXY.getY();
                int tx = tileXToX(tileX);
                int ty = tileYToY(tileY);

                Raster raster = null;

                switch(compressionType) {
                case TILE_UNCOMPRESSED:
                    raster = getUncompressedTile(tx, ty, data);
                    break;
                case TILE_SINGLE_COLOR:
                    raster = getSingleColorTile(tx, ty, compressionSubType);
                    break;
                case TILE_JPEG:
                    raster = getJPEGTile(tx, ty, compressionSubType, data);
                    break;
                case TILE_INVALID:
                default:
                    raster = createWritableRaster(sampleModel,
                                                  new Point(tx, ty));
                    break;
                }

                if(tileX == upperLeftTileX && tileY == upperLeftTileY) {
                    upperLeftTile = raster;
                } else {
                    //System.out.println("Caching "+label);
                    addTileToCache(tileX, tileY, raster);
                }
            } else {
                checkError(label, stream, true);
            }
        }

        endResponse(stream);

        return upperLeftTile;
    }

    /*
     * Create a Raster from the data of an uncompressed tile.
     */
    private Raster getUncompressedTile(int tx, int ty, byte[] data) {
        DataBuffer dataBuffer = new DataBufferByte(data, data.length);

        return Raster.createRaster(sampleModel, dataBuffer, new Point(tx, ty));
    }

    /*
     * Create a Raster of a single color.
     */
    private Raster getSingleColorTile(int tx, int ty, int color) {
        byte R = (byte)(color & 0x000000ff);
        byte G = (byte)((color >> 8) & 0x000000ff);
        byte B = (byte)((color >> 16) & 0x000000ff);
        byte A = (byte)((color >> 24) & 0x000000ff);

        int numBands = sampleModel.getNumBands();
        int length = tileWidth*tileHeight*numBands;
        byte[] data = new byte[length];
        int i = 0;
        switch(numBands) {
        case 1:
            while(i < length) {
                data[i++] = R;
            }
            break;
        case 2:
            while(i < length) {
                data[i++] = R;
                data[i++] = A;
            }
            break;
        case 3:
            while(i < length) {
                data[i++] = R;
                data[i++] = G;
                data[i++] = B;
            }
        case 4:
        default:
            while(i < length) {
                data[i++] = R;
                data[i++] = G;
                data[i++] = B;
                data[i++] = A;
            }
        }

        DataBuffer dataBuffer = new DataBufferByte(data, data.length);

        return Raster.createRaster(sampleModel, dataBuffer, new Point(tx, ty));
    }

    /*
     * Create a Raster from a JPEG-compressed data stream.
     */
    private Raster getJPEGTile(int tx, int ty, int subType, byte[] data) {
        int tableIndex = (subType >> 24) & 0x000000ff;;
        boolean colorConversion = (subType & 0x00ff0000) != 0;
        JPEGDecodeParam decodeParam = null;
        if(tableIndex != 0) {
            decodeParam = getJPEGDecodeParam(tableIndex);
        }

        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        JPEGImageDecoder decoder = decodeParam == null ?
            JPEGCodec.createJPEGDecoder(byteStream) :
            JPEGCodec.createJPEGDecoder(byteStream, decodeParam);

        Raster raster = null;
        try {
            raster = decoder.decodeAsRaster().createTranslatedChild(tx, ty);
        } catch(Exception e) {

            ImagingListener listener =
                ImageUtil.getImagingListener(renderHints);
            listener.errorOccurred(JaiI18N.getString("IIPResolutionOpImage3"),
                                   new ImagingException(e),
                                   this, false);
/*
            String msg = JaiI18N.getString("IIPResolutionOpImage3")+" "+
                e.getMessage();
            throw new RuntimeException(msg);
*/
        }
        closeStream(byteStream);

        if(colorSpaceType == CS_NIFRGB && colorConversion) {
            YCbCrToNIFRGB(raster);
        }

        return raster;
    }

    /*
     * Retrieve the JPEGDecodeParam object for the indicated table. If the
     * object is available in the config, use it; otherwise retrieve it from
     * the server. An ArrayIndexOutOfBoundsException will be thrown if
     * the parameter is not in the range [1,256].
     */
    private synchronized JPEGDecodeParam getJPEGDecodeParam(int tableIndex) {
        JPEGDecodeParam decodeParam = decodeParamCache[tableIndex-1];

        if(decodeParam == null) {
            String cmd = new String("OBJ=Comp-group,"+
                                    TILE_JPEG+","+tableIndex);
            InputStream stream = postCommands(new String[] {cmd});
            String label = null;
            while((label = getLabel(stream)) != null) {
                if(label.startsWith("comp-group")) {
                    byte[] table = getDataAsByteArray(stream);
                    ByteArrayInputStream tableStream =
                        new ByteArrayInputStream(table);
                    JPEGImageDecoder decoder =
                        JPEGCodec.createJPEGDecoder(tableStream);
                    try {
                        // This call is necessary.
                        decoder.decodeAsRaster();
                    } catch(Exception e) {
                        // Ignore.
                    }
                    decodeParam = decoder.getJPEGDecodeParam();
                } else {
                    checkError(label, stream, true);
                }
            }

            endResponse(stream);

            if(decodeParam != null) {
                decodeParamCache[tableIndex-1] = decodeParam;
            }
        }

        return decodeParam;
    }

    /*
     * Obtain valid properties from IIP server and use
     * setProperty() to store the name/value pairs.
     */
    private synchronized void initializeIIPProperties() {
        if(!arePropertiesInitialized) {
            String[] cmd =
                new String[] {"OBJ=IIP", "OBJ=Basic-info", "OBJ=View-info",
                                  "OBJ=Summary-info", "OBJ=Copyright"};
            InputStream stream = postCommands(cmd);
            String label = null;
            while((label = getLabel(stream)) != null) {
                String name = label;
                Object value = null;
                if(label.equals("error")) {
                    flushData(stream, true);
                } else if(label.startsWith("colorspace") ||
                          label.equals("max-size")) {
                    if(label.startsWith("colorspace")) {
                        name = "colorspace";
                    }
                    value = stringToIntArray(getDataAsString(stream, false));
                } else if(label.equals("resolution-number")) {
                    value = Integer.valueOf(getDataAsString(stream, false));
                } else if(label.equals("aspect-ratio") ||
                          label.equals("contrast-adjust") ||
                          label.equals("filtering-value")) {
                    value = Float.valueOf(getDataAsString(stream, false));
                } else if(label.equals("affine-transform")) {
                    float[] a =
                        (float[])stringToFloatArray(getDataAsString(stream,
                                                                    false));
                    value = new AffineTransform(a[0], a[1], a[3],
                                                a[4], a[5], a[7]);
                } else if(label.equals("color-twist")) {
                    value = stringToFloatArray(getDataAsString(stream, false));
                } else if(label.equals("roi")) {
                    name = "roi-iip";
                    float[] rect =
                        stringToFloatArray(getDataAsString(stream, false));
                    value = new Rectangle2D.Float(rect[0], rect[1],
                                                  rect[2], rect[3]);
                } else if(label.equals("copyright") ||
                          label.equals("title") ||
                          label.equals("subject") ||
                          label.equals("author") ||
                          label.equals("keywords") ||
                          label.equals("comment") ||
                          label.equals("last-author") ||
                          label.equals("rev-number") ||
                          label.equals("app-name")) {
                    value = getDataAsString(stream, true);
                } else if(label.equals("iip") ||
                          label.equals("iip-server") ||
                          label.equals("edit-time") ||
                          label.equals("last-printed") ||
                          label.equals("create-dtm") ||
                          label.equals("last-save-dtm")) {
                    value = getDataAsString(stream, false);
                } else { // Ignore unknown objects
                    flushData(stream, false);
                }
                if(name != null && value != null) {
                    setProperty(name, value);
                }
            }
            endResponse(stream);
            arePropertiesInitialized = true;
        }
    }

    /*
     * Forward to superclass after lazy initialization of IIP properties.
     */
    public String[] getPropertyNames() {
        initializeIIPProperties();
        return super.getPropertyNames();
    }

    /*
     * Forward to superclass after lazy initialization of IIP properties.
     */
    public Object getProperty(String name) {
        initializeIIPProperties();
        return super.getProperty(name);
    }

    /**
     * Throws an IllegalArgumentException since the image has no image
     * sources.
     *
     * @param sourceRect ignored.
     * @param sourceIndex ignored.
     * @throws IllegalArgumentException since the image has no image sources.
     */
    public Rectangle mapSourceRect(Rectangle sourceRect,
                                   int sourceIndex) {
        throw new IllegalArgumentException(JaiI18N.getString("AreaOpImage0"));
    }

    /**
     * Throws an IllegalArgumentException since the image has no image
     * sources.
     *
     * @param destRect ignored.
     * @param sourceIndex ignored.
     * @throws IllegalArgumentException since the image has no image sources.
     */
    public Rectangle mapDestRect(Rectangle destRect,
                                 int sourceIndex) {
        throw new IllegalArgumentException(JaiI18N.getString("AreaOpImage0"));
    }

    /**
     * Dispose of any allocated resources.
     */
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
