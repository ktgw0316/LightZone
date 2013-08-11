/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.libs.LCJPEGReader;
import com.lightcrafts.image.libs.LCTIFFReader;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.MetadataUtil;
import com.lightcrafts.jai.JAIContext;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Map;

import sun.nio.ch.DirectBuffer;

/**
 * Get raw image data by inrerfacing with dcraw as a coprocess.
 *
 * @author Fabio Riccardi [fabio@lightcrafts.com]
 */
public final class DCRaw implements
    ApertureProvider, CaptureDateTimeProvider, FocalLengthProvider,
    ISOProvider, MakeModelProvider, ShutterSpeedProvider, WidthHeightProvider {

    //////////  public ////////////////////////////////////////////////////////

    public static final class DCRawException extends LightCraftsException {
        public final int code;

        DCRawException(int code) {
            super("DCRaw Error: " + code);
            this.code = code;
        }
    }

    static private Map<String,DCRaw> dcrawCache =
        new LRUHashMap<String,DCRaw>(100);

    public static synchronized DCRaw getInstanceFor( String fileName ) {
        DCRaw instance = dcrawCache.get(fileName);
        if (instance == null) {
            instance = new DCRaw(fileName);
            dcrawCache.put(fileName, instance);
        }
        return instance;
    }

    /**
     * Construct a <code>DCRaw</code> object.
     *
     * @param fileName The full path of the raw image file.
     */
    private DCRaw( String fileName ) {
        m_fileName = fileName;
        try {
            runDCRawInfo(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    public float getAperture() {
        return m_aperture;
    }

    /**
     * {@inheritDoc}
     */
    public String getCameraMake( boolean includeModel ) {
        String make = getMake();
        final String model = getModel();
        if ( make == null || model == null )
            return null;
        make = make.toUpperCase();
        if ( !includeModel )
            return make;
        return MetadataUtil.undupMakeModel( make, model.toUpperCase() );
    }

    public float[] getCameraMultipliers() {
        return m_cam_mul.clone();
    }

    public float[] getSecondaryCameraMultipliers() {
        return m_secondary_cam_mul.clone();
    }

    public float[][] getCameraRGB() {
        if ( m_rgb_cam == null )
            return null;
        return new float[][] {
            { m_rgb_cam[0], m_rgb_cam[1], m_rgb_cam[2] },
            { m_rgb_cam[3], m_rgb_cam[4], m_rgb_cam[5] },
            { m_rgb_cam[6], m_rgb_cam[7], m_rgb_cam[8] }
        };
    }

    public float[][] getCameraXYZ() {
        if ( m_xyz_cam == null )
            return null;
        return new float[][] {
            { m_xyz_cam[0], m_xyz_cam[1], m_xyz_cam[2] },
            { m_xyz_cam[3], m_xyz_cam[4], m_xyz_cam[5] },
            { m_xyz_cam[6], m_xyz_cam[7], m_xyz_cam[8] }
        };
    }

    /**
     * {@inheritDoc}
     */
    public Date getCaptureDateTime() {
        return  m_captureDateTime > 0 ?
                new Date( m_captureDateTime * 1000 ) : null;
    }

    public float[] getDaylightMultipliers() {
        return m_pre_mul.clone();
    }

    /**
     * {@inheritDoc}
     */
    public float getFocalLength() {
        return m_focalLength;
    }

    /**
     * {@inheritDoc}
     */
    public int getImageHeight() {
        return m_height;
    }

    /**
     * {@inheritDoc}
     */
    public int getImageWidth() {
        return m_width;
    }

    /**
     * {@inheritDoc}
     */
    public int getFullHeight() {
        return m_fullHeight;
    }

    /**
     * {@inheritDoc}
     */
    public int getFullWidth() {
        return m_fullWidth;
    }

    /**
     * {@inheritDoc}
     */
    public int getRawHeight() {
        return m_rawHeight;
    }

    /**
     * {@inheritDoc}
     */
    public int getRawWidth() {
        return m_rawWidth;
    }

    /**
     * {@inheritDoc}
     */
    public int getThumbHeight() {
        return m_thumbHeight;
    }

    /**
     * {@inheritDoc}
     */
    public int getThumbWidth() {
        return m_thumbWidth;
    }

    /**
     * {@inheritDoc}
     */
    public int getISO() {
        return m_iso;
    }

    /**
     * {@inheritDoc}
     */
    public String getMake() {
        return m_make;
    }

    /**
     * {@inheritDoc}
     */
    public String getModel() {
        return m_model;
    }

    private static String readln(InputStream s) {
        try {
            int c;
            StringBuffer sb = new StringBuffer();
            while ((c = s.read()) > 0 && c != 255 && (c == '\n' || c == '\r'))
                ; // Skip carriage returns and line feeds
            while (c > 0 && c != 255 && c != '\n' && c != '\r') {
                sb.append((char) c);
                c = s.read();
            }
            if ((c == -1 || c == 255) && sb.length() == 0)
                return null;
            return new String(sb);
        } catch (IOException e) {
            return null;
        }
    }

    // TODO: we need a more robust parsing mechanism here...

    private final static String FILENAME = "Filename: ";
    private final static String TIMESTAMP = "Timestamp: ";
    private final static String CAMERA = "Camera: ";
    private final static String ISO = "ISO speed: ";
    private final static String SHUTTER = "Shutter: ";
    private final static String APERTURE = "Aperture: ";
    private final static String FOCAL_LENGTH = "Focal Length: ";
    private final static String NUM_RAW_IMAGES = "Number of raw images: ";
    private final static String EMBEDDED_ICC_PROFILE = "Embedded ICC profile: ";
    private final static String CANNOT_DECODE = "Cannot decode file";
    private final static String THUMB_SIZE  = "Thumb size: ";
    private final static String FULL_SIZE   = "Full size: ";
    private final static String IMAGE_SIZE  = "Image size: ";
    private final static String OUTPUT_SIZE = "Output size: ";
    private final static String RAW_COLORS = "Raw colors: ";
    private final static String FILTER_PATTERN = "Filter pattern: ";
    private final static String DAYLIGHT_MULTIPLIERS = "Daylight multipliers: ";
    private final static String CAMERA_MULTIPLIERS = "Camera multipliers: ";
    private final static String CAMERA_RGB_PROFILE = "Camera RGB Profile: ";
    private final static String CAMERA_XYZ_PROFILE = "Camera XYZ Profile: ";

    private static String DCRAW_PATH = "./dcraw";
    static {
        String appDir = System.getProperty("install4j.appDir");
        if (appDir != null) {
            DCRAW_PATH = appDir + "/dcraw";
        }
    }

    private static String match(String s, String tag) {
        if (s.startsWith(tag)) {
            s = s.substring(tag.length());
            int index = 0;
            while(s.charAt(index) == ' ')
                index++;
            if (index > 0)
                s = s.substring(index);
            return s;
        }
        return null;
    }

    private void runDCRawInfo(boolean secondary) throws IOException {
        String info[] = { DCRAW_PATH, "-v", "-i", "-t", "0", m_fileName };
        String secondaryInfo[] = { DCRAW_PATH, "-v", "-i", "-s", "1", "-t", "0", m_fileName };

        synchronized (DCRaw.class) {
            Process p = null;
            InputStream dcrawStdOut;
            InputStream dcrawStdErr;
            if (ForkDaemon.INSTANCE != null) {
                ForkDaemon.INSTANCE.invoke(secondary ? secondaryInfo : info);
                dcrawStdOut = ForkDaemon.INSTANCE.getStdOut();
                dcrawStdErr = ForkDaemon.INSTANCE.getStdErr();
            } else {
                p = Runtime.getRuntime().exec(secondary ? secondaryInfo : info);
                dcrawStdOut = p.getInputStream();
                dcrawStdErr = new BufferedInputStream(p.getErrorStream());
            }

            // output expected on stdout
            String line, args;
            while ((line = readln(dcrawStdOut)) != null) {
                // System.out.println(line);

                String search;

                if (secondary) {
                    if (line.startsWith(search = CAMERA_MULTIPLIERS)) {
                        String multipliers[] = line.substring(search.length()).split("\\s");
                        m_secondary_cam_mul[0] = Float.parseFloat(multipliers[0]);
                        m_secondary_cam_mul[1] = Float.parseFloat(multipliers[1]);
                        m_secondary_cam_mul[2] = Float.parseFloat(multipliers[2]);
                        m_secondary_cam_mul[3] = Float.parseFloat(multipliers[3]);
                    }
                } else {
                    if (line.startsWith(search = FILENAME)) {
                        String filename = line.substring(search.length());
                    } else if (line.startsWith(search = TIMESTAMP)) {
                        String timestamp = line.substring(search.length());
                        m_captureDateTime = new Date(timestamp).getTime();
                    } else if (line.startsWith(search = CAMERA)) {
                        String camera = line.substring(search.length());
                        m_make = camera.substring(0, camera.indexOf(' '));
                        m_model = camera.substring(m_make.length() + 1);
                    } else if (line.startsWith(search = ISO)) {
                        String iso = line.substring(search.length());
                        m_iso = Integer.decode(iso);
                    } else if (line.startsWith(search = SHUTTER)) {
                        String shutterSpeed = line.substring(search.length() + 2);
                        float exposureTime = 0;
                        try {
                            exposureTime = Float.valueOf(shutterSpeed.substring(0, shutterSpeed.indexOf(" sec")));
                            if (exposureTime != 0)
                                m_shutterSpeed = 1 / exposureTime;
                        } catch (NumberFormatException e) { }
                    } else if (line.startsWith(search = APERTURE)) {
                        String aperture = line.substring(search.length() + 2);
                        try {
                            m_aperture = Float.valueOf(aperture);
                        } catch (NumberFormatException e) { }
                    } else if (line.startsWith(search = FOCAL_LENGTH)) {
                        String focalLenght = line.substring(search.length());
                        try {
                            m_focalLength = Float.valueOf(focalLenght.substring(0, focalLenght.indexOf(" mm")));
                        } catch (NumberFormatException e) { }
                    } else if (line.startsWith(search = NUM_RAW_IMAGES)) {
                        String numRawImages = line.substring(search.length());
                        m_secondaryPixels = numRawImages.startsWith("2");
                    } else if (line.startsWith(search = EMBEDDED_ICC_PROFILE)) {
                        String embeddedICCProfile = line.substring(search.length());
                    } else if (line.startsWith(CANNOT_DECODE)) {
                        m_decodable = false;
                    } else if ((args = match(line, THUMB_SIZE)) != null) {
                        String sizes[] = args.split(" x ");
                        m_thumbWidth = Integer.decode(sizes[0]);
                        m_thumbHeight = Integer.decode(sizes[1]);
                    } else if ((args = match(line, FULL_SIZE)) != null) {
                        String sizes[] = args.split(" x ");
                        m_fullWidth = Integer.decode(sizes[0]);
                        m_fullHeight = Integer.decode(sizes[1]);
                    } else if ((args = match(line, IMAGE_SIZE)) != null) {
                        String sizes[] = args.split(" x ");
                        m_rawWidth = Integer.decode(sizes[0]);
                        m_rawHeight = Integer.decode(sizes[1]);
                    } else if ((args = match(line, OUTPUT_SIZE)) != null) {
                        String sizes[] = args.split(" x ");
                        m_width = Integer.decode(sizes[0]);
                        m_height = Integer.decode(sizes[1]);
                    } else if (line.startsWith(search = RAW_COLORS)) {
                        String rawColors = line.substring(search.length());
                        m_rawColors = Integer.decode(rawColors);
                    } else if (line.startsWith(search = FILTER_PATTERN)) {
                        String pattern = line.substring(search.length());
                        if (pattern.length() >= 8 && !pattern.substring(0,4).equals(pattern.substring(4,4)))
                            m_filters = -1;
                        else if (pattern.startsWith("BGGR"))
                            m_filters = 0x16161616;
                        else if (pattern.startsWith("GRBG"))
                            m_filters = 0x61616161;
                        else if (pattern.startsWith("GBRG"))
                            m_filters = 0x49494949;
                        else if (pattern.startsWith("RGGB"))
                            m_filters = 0x94949494;
                        else
                            m_filters = -1;
                    } else if (line.startsWith(search = DAYLIGHT_MULTIPLIERS)) {
                        String multipliers[] = line.substring(search.length()).split("\\s");
                        m_pre_mul[0] = Float.parseFloat(multipliers[0]);
                        m_pre_mul[1] = Float.parseFloat(multipliers[1]);
                        m_pre_mul[2] = Float.parseFloat(multipliers[2]);
                        m_pre_mul[3] = m_pre_mul[1];
                    } else if (line.startsWith(search = CAMERA_MULTIPLIERS)) {
                        String multipliers[] = line.substring(search.length()).split("\\s");
                        m_cam_mul[0] = Float.parseFloat(multipliers[0]);
                        m_cam_mul[1] = Float.parseFloat(multipliers[1]);
                        m_cam_mul[2] = Float.parseFloat(multipliers[2]);
                        m_cam_mul[3] = Float.parseFloat(multipliers[3]);
                    } else if (line.startsWith(CAMERA_RGB_PROFILE)) {
                        String rgb_cam[] = line.substring(CAMERA_RGB_PROFILE.length()).split("\\s");
                        m_rgb_cam = new float[9];
                        for (int i = 0; i < 9; i++) {
                            m_rgb_cam[i] = Float.parseFloat(rgb_cam[i]);
                        }
                    } else if (line.startsWith(CAMERA_XYZ_PROFILE)) {
                        String xyz_cam[] = line.substring(CAMERA_XYZ_PROFILE.length()).split("\\s");
                        m_xyz_cam = new float[9];
                        for (int i = 0; i < 9; i++) {
                            m_xyz_cam[i] = Float.parseFloat(xyz_cam[i]);
                        }
                    }
                }
            }

            // Flush stderr just in case...
            while ((line = readln(dcrawStdErr)) != null)
                ; // System.out.println(line);

            if (p != null) {
                dcrawStdOut.close();
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                m_error = p.exitValue();
                p.destroy();
            } else
                m_error = 0;
        }
    }

    private static class ImageData {
        final int width, height, bands, dataType;
        final Object data;

        ImageData(int width, int height, int bands, int dataType) {
            this.width = width;
            this.height = height;
            this.bands = bands;
            this.dataType = dataType;
            this.data = dataType == DataBuffer.TYPE_BYTE
                        ? new byte[bands * width * height]
                        : new short[bands * width * height];
        }
    }

    private static ImageData readPPM(File file) throws IOException, BadImageFileException {
        FileInputStream s = new FileInputStream(file);

        ImageData imageData;
        try {
            String S1 = readln(s);

            int width;
            int height;
            int bands;
            int dataType;
            if (S1.equals("P5") || S1.equals("P6")) {
                bands = S1.equals("P5") ? 1 : 3;
                String S2 = readln(s);
                String S3 = readln(s);

                String dimensions[] = S2.split("\\s");
                width = Integer.parseInt(dimensions[0]);
                height = Integer.parseInt(dimensions[1]);

                dataType = S3.equals("255") ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT;

                imageData = new ImageData(width, height, bands, dataType);
            } else if (S1.equals("P7")) {
                String WIDTH = "WIDTH ";
                String HEIGHT = "HEIGHT ";
                String DEPTH = "DEPTH ";
                String MAXVAL = "MAXVAL ";
                String TUPLTYPE = "TUPLTYPE ";
                String ENDHDR = "ENDHDR";
                String SWIDTH = readln(s);
                width = Integer.parseInt(SWIDTH.substring(WIDTH.length()));
                String SHEIGHT = readln(s);
                height = Integer.parseInt(SHEIGHT.substring(HEIGHT.length()));
                String SDEPTH = readln(s);
                bands = Integer.parseInt(SDEPTH.substring(DEPTH.length()));
                String SMAXVAL = readln(s);
                dataType = SMAXVAL.substring(MAXVAL.length()).equals("65535")
                           ? DataBuffer.TYPE_USHORT
                           : DataBuffer.TYPE_BYTE;
                String STUPLTYPE = readln(s);
                String SENDHDR = readln(s);
                imageData = new ImageData(width, height, bands, dataType);
            } else
                return null;

            int totalData = width * height * bands * (dataType == DataBuffer.TYPE_BYTE ? 1 : 2);

            FileChannel c = s.getChannel();

            if (file.length() != totalData + c.position()) {
                c.close();
                throw new BadImageFileException(file);
            }

            ByteBuffer bb = c.map(FileChannel.MapMode.READ_ONLY, c.position(), totalData);

            if (dataType == DataBuffer.TYPE_USHORT) {
                bb.order(ByteOrder.BIG_ENDIAN);
                bb.asShortBuffer().get((short[]) imageData.data);
            } else
                bb.get((byte[]) imageData.data);

            if (bb instanceof DirectBuffer)
                ((DirectBuffer) bb).cleaner().clean();

            c.close();
        } catch (Exception e) {
            e.printStackTrace();
            s.close();
            throw new BadImageFileException(file, e);
        } finally {
            s.close();
        }

        return imageData;
    }

    private static final String DCRAW_OUTPUT = "Writing data to ";

    public enum dcrawMode {full, preview, thumb}

    public RenderedImage runDCRaw(dcrawMode mode) throws IOException, UnknownImageTypeException, BadImageFileException {
        return runDCRaw(mode, false);
    }

    public synchronized RenderedImage runDCRaw(dcrawMode mode, boolean secondaryPixels)
            throws IOException, UnknownImageTypeException, BadImageFileException
    {
        if (!m_decodable || (mode == dcrawMode.full && m_rawColors != 3))
            throw new UnknownImageTypeException("Unsuported Camera");

        RenderedImage result = null;

        File of = null;

        try {
            if (mode == dcrawMode.preview) {
                if (m_thumbWidth >= 1024 && m_thumbHeight >= 768) {
                    mode = dcrawMode.thumb;
                }
            }

            long t1 = System.currentTimeMillis();

            of = File.createTempFile("LZRAWTMP", ".ppm");

            boolean four_colors = false;
            final String makeModel = m_make + ' ' + m_model;
            for (String s : four_color_cameras)
                if (s.equalsIgnoreCase(makeModel)) {
                    four_colors = true;
                    break;
                }

            if (secondaryPixels)
                runDCRawInfo(true);

            String cmd[];
            switch (mode) {
                case full:
                    if (four_colors)
                        cmd = new String[] { DCRAW_PATH, "-F", of.getAbsolutePath(), "-v", "-f", "-H", "1", "-t", "0", "-o", "0", "-4", m_fileName };
                    else if (m_filters == -1 || (m_make != null && m_make.equalsIgnoreCase("SIGMA")))
                        cmd = new String[] { DCRAW_PATH, "-F", of.getAbsolutePath(), "-v", "-H", "1", "-t", "0", "-o", "0", "-4", m_fileName };
                    else if (secondaryPixels)
                        cmd = new String[] { DCRAW_PATH, "-F", of.getAbsolutePath(), "-v", "-j", "-H", "1", "-t", "0", "-s", "1", "-d", "-4", m_fileName };
                    else
                        cmd = new String[] { DCRAW_PATH, "-F", of.getAbsolutePath(), "-v", "-j", "-H", "1", "-t", "0", "-d", "-4", m_fileName };
                    break;
                case preview:
                    cmd = new String[] { DCRAW_PATH, "-F", of.getAbsolutePath(), "-v", "-t", "0", "-o", "1", "-w", "-h", m_fileName };
                    break;
                case thumb:
                    cmd = new String[] { DCRAW_PATH, "-F", of.getAbsolutePath(), "-v", "-e", m_fileName };
                    break;
                default:
                    throw new IllegalArgumentException("Unknown mode " + mode);
            }

            String ofName = null;

            synchronized (DCRaw.class) {
                Process p = null;
                InputStream dcrawStdErr;
                InputStream dcrawStdOut;
                if (ForkDaemon.INSTANCE != null) {
                    ForkDaemon.INSTANCE.invoke(cmd);
                    dcrawStdErr = ForkDaemon.INSTANCE.getStdErr();
                    dcrawStdOut = ForkDaemon.INSTANCE.getStdOut();
                } else {
                    p = Runtime.getRuntime().exec(cmd);
                    dcrawStdErr = new BufferedInputStream(p.getErrorStream());
                    dcrawStdOut = p.getInputStream();
                }

                String line, args;
                // output expected on stderr
                while ((line = readln(dcrawStdErr)) != null) {
                    // System.out.println(line);

                    if ((args = match(line, DCRAW_OUTPUT)) != null)
                        ofName = args.substring(0, args.indexOf(" ..."));
                }

                // Flush stdout just in case...
                while ((line = readln(dcrawStdOut)) != null)
                    ; // System.out.println(line);

                if (p != null) {
                    dcrawStdErr.close();

                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    m_error = p.exitValue();
                    p.destroy();
                } else
                    m_error = 0;
            }

            System.out.println("dcraw value: " + m_error);

            if (m_error > 0) {
                of.delete();
                throw new BadImageFileException(of);
            }

            if (!ofName.equals(of.getPath())) {
                of.delete();
                of = new File(ofName);
            }

            if (of.getName().endsWith(".jpg") || of.getName().endsWith(".tiff")) {
                if (of.getName().endsWith(".jpg")) {
                    try {
                        LCJPEGReader jpegReader = new LCJPEGReader(of.getPath());
                        result = jpegReader.getImage();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        LCTIFFReader tiffReader = new LCTIFFReader(of.getPath());
                        result = tiffReader.getImage(null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                long t2 = System.currentTimeMillis();

                int totalData = result.getWidth() *
                                result.getHeight() *
                                result.getColorModel().getNumColorComponents() *
                                (result.getColorModel().getTransferType() == DataBuffer.TYPE_BYTE ? 1 : 2);

                System.out.println("Read " + totalData + " bytes in " + (t2 - t1) + "ms");
            } else {
                ImageData imageData;
                try {
                    imageData = readPPM(of);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new BadImageFileException(of, e);
                }

                // do not change the initial image geometry
                // m_width = Math.min(m_width, imageData.width);
                // m_height = Math.min(m_height, imageData.height);

                long t2 = System.currentTimeMillis();

                int totalData = imageData.width *
                                imageData.height *
                                imageData.bands * (imageData.dataType == DataBuffer.TYPE_BYTE ? 1 : 2);

                System.out.println("Read " + totalData + " bytes in " + (t2 - t1) + "ms");

                final ColorModel cm;
                if (mode == dcrawMode.full) {
                    if (imageData.bands == 1) {
                        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                     false, false,
                                                     Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
                    } else {
                        cm = JAIContext.colorModel_linear16;
                    }
                } else {
                    if (imageData.bands == 3)
                        cm = new ComponentColorModel(JAIContext.sRGBColorSpace,
                                                     false, false,
                                                     Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                    else if (imageData.bands == 4)
                        cm = new ComponentColorModel(JAIContext.CMYKColorSpace,
                                                     false, false,
                                                     Transparency.OPAQUE, imageData.dataType);
                    else
                        throw new UnknownImageTypeException("Weird number of bands: " + imageData.bands);
                }

                final DataBuffer buf = imageData.dataType == DataBuffer.TYPE_BYTE
                                       ? new DataBufferByte((byte[]) imageData.data,
                                                            imageData.bands * imageData.width * imageData.height)
                                       : new DataBufferUShort((short[]) imageData.data,
                                                              imageData.bands * imageData.width * imageData.height);

                final WritableRaster raster = Raster.createInterleavedRaster(
                    buf, imageData.width, imageData.height,
                    imageData.bands * imageData.width, imageData.bands,
                    imageData.bands == 3 ? new int[]{ 0, 1, 2 } : new int[]{0}, null
                );

                result = new BufferedImage(cm, raster, false, null);
            }
        } catch (IOException e) {
            if (of != null)
                of.delete();
            throw e;
        } finally {
            if (of != null)
                of.delete();
        }
        return result;
    }

    public int getFilters() {
        return m_filters;
    }

    public boolean getSecondaryPixels() {
        return m_secondaryPixels;
    }

    /**
     * {@inheritDoc}
     */
    public float getShutterSpeed() {
        return m_shutterSpeed;
    }

    public boolean decodable() {
        return m_decodable;
    }

    public int rawColors() {
        return m_rawColors;
    }

    ////////// private ////////////////////////////////////////////////////////

    private int m_error;

    private boolean m_decodable = true;

    private final String m_fileName;

    private String m_make;
    private String m_model;

    private long m_captureDateTime;

    private int m_width;
    private int m_height;

    private int m_fullWidth;
    private int m_fullHeight;

    private int m_thumbWidth;
    private int m_thumbHeight;

    private int m_rawWidth;
    private int m_rawHeight;

    private float m_shutterSpeed;
    private float m_aperture;
    private float m_focalLength;
    private int m_iso;

    private int m_filters;

    private int m_rawColors;

    private boolean m_secondaryPixels;

    private float m_cam_mul[] = new float[4];
    private float m_pre_mul[] = new float[4];
    private float m_rgb_cam[];
    private float m_xyz_cam[];
    private float m_secondary_cam_mul[] = new float[4];

    private static final String four_color_cameras[] = {
        // "OLYMPUS E-3",
        "OLYMPUS E-1",
        "OLYMPUS E-300",
        "OLYMPUS E-330",
        "OLYMPUS E-500",
        "OLYMPUS E-510",
        "OLYMPUS E-520",
        "OLYMPUS E-400",
        "OLYMPUS E-410",
        "OLYMPUS E-420",
        "OLYMPUS E-20,E-20N,E-20P",
        "OLYMPUS E-10",
        "Leica Camera AG M8 Digital Camera"
        // TODO: what about the Panasonic 4/3 and the Leica Digilux 3?
    };

    public native static void interpolateGreen(short[] srcData, short[] destData, int width, int height,
                                               int srcLineStride, int destLineStride,
                                               int srcOffset, int rOffset, int gOffset, int bOffset,
                                               int gx, int gy, int ry );

    public native static void interpolateRedBlue(short[] jdata, int width, int height, int lineStride,
                                                 int rOffset, int gOffset, int bOffset,
                                                 int rx0, int ry0, int bx0, int by0);
}
/* vim:set et sw=4 ts=4: */
