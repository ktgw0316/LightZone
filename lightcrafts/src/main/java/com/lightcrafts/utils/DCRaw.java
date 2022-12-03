/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2015-     Masahiro Kitagawa */

package com.lightcrafts.utils;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.libs.LCImageLibException;
import com.lightcrafts.image.libs.LCImageReaderFactory;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.MetadataUtil;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.val;

/**
 * Get raw image data by inrerfacing with dcraw as a coprocess.
 *
 * @author Fabio Riccardi [fabio@lightcrafts.com]
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
public final class DCRaw implements
    ApertureProvider, CaptureDateTimeProvider, FocalLengthProvider,
    ISOProvider, MakeModelProvider, ShutterSpeedProvider, WidthHeightProvider {

    //////////  public ////////////////////////////////////////////////////////

    static private Map<String,DCRaw> dcrawCache =
        new LRUHashMap<>(100);

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
    @Override
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
    @Override
    public Date getCaptureDateTime() {
        return  m_captureDateTime > 0 ?
                new Date( m_captureDateTime * 1000 ) : null;
    }

    public float[] getDaylightMultipliers() {
        return m_pre_mul.clone();
    }

    private static String readln(InputStream s) {
        try {
            int c = '\n';
            while (c > 0 && c != 255 && (c == '\n' || c == '\r')) {
                // Skip carriage returns and line feeds
                c = s.read();
            }
            val sb = new StringBuffer();
            while (c > 0 && c != 255 && c != '\n' && c != '\r') {
                sb.append((char) c);
                c = s.read();
            }
            return ((c == -1 || c == 255) && sb.length() == 0) ? null : new String(sb);
        } catch (IOException e) {
            return null;
        }
    }

    // TODO: we need a more robust parsing mechanism here...

    // private final static String FILENAME = "Filename: ";
    private final static String TIMESTAMP = "Timestamp: ";
    private final static String CAMERA = "Camera: ";
    private final static String ISO = "ISO speed: ";
    private final static String SHUTTER = "Shutter: ";
    private final static String APERTURE = "Aperture: ";
    private final static String FOCAL_LENGTH = "Focal Length: ";
    // private final static String NUM_RAW_IMAGES = "Number of raw images: ";
    // private final static String EMBEDDED_ICC_PROFILE = "Embedded ICC profile: ";
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

    private static final String DCRAW_NAME = "dcraw_lz";
    private static String DCRAW_PATH;
    static {
        DCRAW_PATH = System.getProperty("java.library.path") + File.separatorChar + DCRAW_NAME;
        if (! new File(DCRAW_PATH).canExecute()) {
            String dir = System.getProperty("install4j.appDir");
            if (dir == null)
                dir = ".";
            DCRAW_PATH = dir + File.separatorChar + DCRAW_NAME;
        }
    }

    private static String match(@NonNull String s, @NonNull String tag) {
        if (!s.startsWith(tag))
            return null;

        s = s.substring(tag.length());
        int index = 0;
        while(s.charAt(index) == ' ')
            index++;
        if (index > 0)
            s = s.substring(index);
        return s;
    }

    private int runDCRawInfo(boolean secondary) throws IOException {
        val info = new String[]{DCRAW_PATH, "-v", "-i", "-t", "0", m_fileName};
        val secondaryInfo = new String[]{DCRAW_PATH, "-v", "-i", "-s", "1", "-t", "0", m_fileName};

        final int error;

        synchronized (DCRaw.class) {
            final Process p = execProcess(secondary ? secondaryInfo : info);
            final InputStream dcrawStdErr = getDcrawStdErr(p);
            final InputStream dcrawStdOut = getDcrawStdOut(p);
            try {
                String line;
                while ((line = readln(dcrawStdOut)) != null) {
                    // System.out.println(line);
                    parseDCRawInfo(line, secondary);
                }

                // Flush stderr just in case...
                while ((line = readln(dcrawStdErr)) != null)
                    ; // System.out.println(line);
            } finally {
                if (p != null) {
                    dcrawStdErr.close();
                    dcrawStdOut.close();
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    error = p.exitValue();
                    p.destroy();
                } else {
                    error = 0;
                }
            }
        }
        return error;
    }

    private void parseDCRawInfo(String line, boolean secondary) {
        String search;

        if (secondary && line.startsWith(search = CAMERA_MULTIPLIERS)) {
            val multipliers = line.substring(search.length()).split("\\s");
            m_secondary_cam_mul[0] = Float.parseFloat(multipliers[0]);
            m_secondary_cam_mul[1] = Float.parseFloat(multipliers[1]);
            m_secondary_cam_mul[2] = Float.parseFloat(multipliers[2]);
            m_secondary_cam_mul[3] = Float.parseFloat(multipliers[3]);
            return;
        }

        String args;
        // if (line.startsWith(search = FILENAME)) {
        //     val filename = line.substring(search.length());
        // } else
        if (line.startsWith(search = TIMESTAMP)) {
            val timestamp = line.substring(search.length());
            try {
                m_captureDateTime = new SimpleDateFormat().parse(timestamp).getTime();
            } catch (ParseException e) {
                m_captureDateTime = 0;
            }
        } else if (line.startsWith(search = CAMERA)) {
            val camera = line.substring(search.length());
            m_make = camera.substring(0, camera.indexOf(' '));
            m_model = camera.substring(m_make.length() + 1);
        } else if (line.startsWith(search = ISO)) {
            val iso = line.substring(search.length());
            m_ISO = Integer.decode(iso);
        } else if (line.startsWith(search = SHUTTER)) {
            val shutterSpeed = line.substring(search.length() + 2);
            try {
                float exposureTime = Float.valueOf
                        (shutterSpeed.substring(0, shutterSpeed.indexOf(" sec")));
                if (exposureTime != 0)
                    m_shutterSpeed = 1 / exposureTime;
            } catch (NumberFormatException ignored) { }
        } else if (line.startsWith(search = APERTURE)) {
            val aperture = line.substring(search.length() + 2);
            try {
                m_aperture = Float.valueOf(aperture);
            } catch (NumberFormatException ignored) { }
        } else if (line.startsWith(search = FOCAL_LENGTH)) {
            val focalLenght = line.substring(search.length());
            try {
                m_focalLength = Float.valueOf(
                        focalLenght.substring(0, focalLenght.indexOf(" mm")));
            } catch (NumberFormatException ignored) { }
            // } else if (line.startsWith(search = NUM_RAW_IMAGES)) {
            //     val numRawImages = line.substring(search.length());
            // } else if (line.startsWith(search = EMBEDDED_ICC_PROFILE)) {
            //     val embeddedICCProfile = line.substring(search.length());
        } else if (line.startsWith(CANNOT_DECODE)) {
            m_decodable = false;
        } else if ((args = match(line, THUMB_SIZE)) != null) {
            val sizes = args.split(" x ");
            m_thumbWidth = Integer.decode(sizes[0]);
            m_thumbHeight = Integer.decode(sizes[1]);
        } else if ((args = match(line, FULL_SIZE)) != null) {
            val sizes = args.split(" x ");
            m_fullWidth = Integer.decode(sizes[0]);
            m_fullHeight = Integer.decode(sizes[1]);
        } else if ((args = match(line, IMAGE_SIZE)) != null) {
            val sizes = args.split(" x ");
            m_rawWidth = Integer.decode(sizes[0]);
            m_rawHeight = Integer.decode(sizes[1]);
        } else if ((args = match(line, OUTPUT_SIZE)) != null) {
            val sizes = args.split(" x ");
            m_imageWidth = Integer.decode(sizes[0]);
            m_imageHeight = Integer.decode(sizes[1]);
        } else if (line.startsWith(search = RAW_COLORS)) {
            val rawColors = line.substring(search.length());
            m_rawColors = Integer.decode(rawColors);
        } else if (line.startsWith(search = FILTER_PATTERN)) {
            val pattern = line.substring(search.length());
            if (pattern.length() >= 8
                    && !pattern.substring(0,4).equals(pattern.substring(4,8)))
                m_filters = -1;
            else if (pattern.startsWith("BG/GR"))
                m_filters = 0x16161616;
            else if (pattern.startsWith("GR/BG"))
                m_filters = 0x61616161;
            else if (pattern.startsWith("GB/RG"))
                m_filters = 0x49494949;
            else if (pattern.startsWith("RG/GB"))
                m_filters = 0x94949494;
            else
                m_filters = -1;
        } else if (line.startsWith(search = DAYLIGHT_MULTIPLIERS)) {
            val multipliers = line.substring(search.length()).split("\\s");
            m_pre_mul[0] = Float.parseFloat(multipliers[0]);
            m_pre_mul[1] = Float.parseFloat(multipliers[1]);
            m_pre_mul[2] = Float.parseFloat(multipliers[2]);
            m_pre_mul[3] = m_pre_mul[1];
        } else if (line.startsWith(search = CAMERA_MULTIPLIERS)) {
            val multipliers = line.substring(search.length()).split("\\s");
            m_cam_mul[0] = Float.parseFloat(multipliers[0]);
            m_cam_mul[1] = Float.parseFloat(multipliers[1]);
            m_cam_mul[2] = Float.parseFloat(multipliers[2]);
            m_cam_mul[3] = Float.parseFloat(multipliers[3]);
        } else if (line.startsWith(CAMERA_RGB_PROFILE)) {
            val rgb_cam = line.substring(CAMERA_RGB_PROFILE.length()).split("\\s");
            m_rgb_cam = new float[9];
            for (int i = 0; i < 9; i++) {
                m_rgb_cam[i] = Float.parseFloat(rgb_cam[i]);
            }
        } else if (line.startsWith(CAMERA_XYZ_PROFILE)) {
            val xyz_cam = line.substring(CAMERA_XYZ_PROFILE.length()).split("\\s");
            m_xyz_cam = new float[9];
            for (int i = 0; i < 9; i++) {
                m_xyz_cam[i] = Float.parseFloat(xyz_cam[i]);
            }
        }
    }

    private InputStream getDcrawStdOut(Process p) {
        return p == null
                ? ForkDaemon.INSTANCE.getStdOut()
                : p.getInputStream();
    }

    private InputStream getDcrawStdErr(Process p) {
        return p == null
                ? ForkDaemon.INSTANCE.getStdErr()
                : new BufferedInputStream(p.getErrorStream());
    }

    private Process execProcess(String[] cmd) throws IOException {
        if (ForkDaemon.INSTANCE != null) {
            ForkDaemon.INSTANCE.invoke(cmd);
            return null;
        } else {
            return Runtime.getRuntime().exec(cmd);
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

    private static ImageData readPPM(File file) throws BadImageFileException {
        try (FileInputStream s = new FileInputStream(file)) {
            val S1 = readln(s);
            if (S1 == null) {
                throw new BadImageFileException(file);
            }

            final int width;
            final int height;
            final int bands;
            final int dataType;
            switch (S1) {
                case "P5":
                case "P6":
                    bands = S1.equals("P5") ? 1 : 3;
                    val S2 = readln(s);
                    val S3 = readln(s);
                    if (S2 == null || S3 == null) {
                        throw new BadImageFileException(file);
                    }
                    val dimensions = S2.split("\\s");
                    width = Integer.parseInt(dimensions[0]);
                    height = Integer.parseInt(dimensions[1]);
                    dataType = S3.equals("255") ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT;
                    break;
                case "P7":
                    val SWIDTH = readln(s);
                    val SHEIGHT = readln(s);
                    val SDEPTH = readln(s);
                    val SMAXVAL = readln(s);
                    if (SWIDTH == null || SHEIGHT == null || SDEPTH == null || SMAXVAL == null) {
                        throw new BadImageFileException(file);
                    }
                    val WIDTH = "WIDTH ";
                    width = Integer.parseInt(SWIDTH.substring(WIDTH.length()));
                    val HEIGHT = "HEIGHT ";
                    height = Integer.parseInt(SHEIGHT.substring(HEIGHT.length()));
                    val DEPTH = "DEPTH ";
                    bands = Integer.parseInt(SDEPTH.substring(DEPTH.length()));
                    val MAXVAL = "MAXVAL ";
                    dataType = SMAXVAL.substring(MAXVAL.length()).equals("65535")
                            ? DataBuffer.TYPE_USHORT
                            : DataBuffer.TYPE_BYTE;
                    break;
                default:
                    throw new BadImageFileException(file);
            }
            val imageData = new ImageData(width, height, bands, dataType);
            val totalData = width * height * bands * (dataType == DataBuffer.TYPE_BYTE ? 1 : 2);

            try (FileChannel c = s.getChannel()) {
                if (file.length() != totalData + c.position()) {
                    throw new BadImageFileException(file);
                }

                ByteBuffer bb = c.map(FileChannel.MapMode.READ_ONLY, c.position(), totalData);

                if (dataType == DataBuffer.TYPE_USHORT) {
                    bb.order(ByteOrder.nativeOrder());
                    bb.asShortBuffer().get((short[]) imageData.data);

                    // Dirty hack to prevent crash on Arch Linux (issue #125)
                    if (ByteOrder.nativeOrder() != ByteOrder.BIG_ENDIAN) {
                        for (int i = 0; i < ((short[]) imageData.data).length; ++i) {
                            ((short[]) imageData.data)[i] =
                                    Short.reverseBytes(((short[]) imageData.data)[i]);
                        }
                    }
                } else {
                    bb.get((byte[]) imageData.data);
                }

                ByteBufferUtil.clean(bb);
            }
            return imageData;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadImageFileException(file, e);
        }
    }

    private static final String DCRAW_OUTPUT = "Writing data to ";

    public enum dcrawMode {full, preview, thumb}

    public RenderedImage runDCRaw(dcrawMode mode)
            throws IOException, UnknownImageTypeException, BadImageFileException
    {
        return runDCRaw(mode, false);
    }

    public synchronized RenderedImage runDCRaw(dcrawMode mode, boolean secondaryPixels)
            throws IOException, UnknownImageTypeException, BadImageFileException
    {
        if (!m_decodable || (mode == dcrawMode.full && m_rawColors != 3)) {
            throw new UnknownImageTypeException("Unsuported Camera");
        }

        RenderedImage result = null;
        File of = null;

        try {
            if (mode == dcrawMode.preview && m_thumbWidth >= 1024 && m_thumbHeight >= 768) {
                mode = dcrawMode.thumb;
            }

            val t1 = System.currentTimeMillis();

            if (secondaryPixels)
                runDCRawInfo(true);

            of = getDcrawOutputFile(mode, secondaryPixels);

            final long t2;
            final int totalData;
            if (of.getName().endsWith(".jpg") || of.getName().endsWith(".tiff")) {
                try {
                    val readerFactory = new LCImageReaderFactory();
                    val reader = readerFactory.create(of);
                    result = reader.getImage();
                } catch (LCImageLibException | UserCanceledException e) {
                    e.printStackTrace();
                }
                if (result == null) {
                    throw new BadImageFileException(of);
                }
                t2 = System.currentTimeMillis();
                totalData = result.getWidth() *
                        result.getHeight() *
                        result.getColorModel().getNumColorComponents() *
                        (result.getColorModel().getTransferType() == DataBuffer.TYPE_BYTE ? 1 : 2);
            } else {
                val imageData = readPPM(of);
                t2 = System.currentTimeMillis();
                totalData = imageData.width *
                        imageData.height *
                        imageData.bands * (imageData.dataType == DataBuffer.TYPE_BYTE ? 1 : 2);
                val cm = getColorModel(mode, imageData.bands, imageData.dataType);
                val bufSize = imageData.bands * imageData.width * imageData.height;
                final DataBuffer buf = imageData.dataType == DataBuffer.TYPE_BYTE
                        ? new DataBufferByte(   (byte[]) imageData.data, bufSize)
                        : new DataBufferUShort((short[]) imageData.data, bufSize);
                val bandOffsets = imageData.bands == 3 ? new int[]{0, 1, 2} : new int[]{0};
                val raster = Raster.createInterleavedRaster(
                        buf, imageData.width, imageData.height,
                        imageData.bands * imageData.width, imageData.bands, bandOffsets, null);

                result = new BufferedImage(cm, raster, false, null);
            }
            System.out.println("Read " + totalData + " bytes in " + (t2 - t1) + "ms");
        } finally {
            if (of != null && !of.delete()) {
                System.out.println("Could not delete temporary file: " + of);
            }
        }
        return result;
    }

    private File getDcrawOutputFile(dcrawMode mode, boolean secondaryPixels)
            throws IOException, BadImageFileException
    {
        File of = File.createTempFile("LZRAWTMP", ".ppm");
        String ofName = null;

        val cmd = dcrawCommandLine(mode, secondaryPixels, of);

        final int error;

        synchronized (DCRaw.class) {
            final Process p;
            final InputStream dcrawStdErr;
            final InputStream dcrawStdOut;
            if (ForkDaemon.INSTANCE != null) {
                p = null;
                ForkDaemon.INSTANCE.invoke(cmd);
                dcrawStdErr = ForkDaemon.INSTANCE.getStdErr();
                dcrawStdOut = ForkDaemon.INSTANCE.getStdOut();
            } else {
                p = Runtime.getRuntime().exec(cmd);
                dcrawStdErr = new BufferedInputStream(p.getErrorStream());
                dcrawStdOut = p.getInputStream();
            }

            String line;
            // output expected on stderr
            while ((line = readln(dcrawStdErr)) != null) {
                System.out.println(line);

                val args = match(line, DCRAW_OUTPUT);
                if (args != null)
                    ofName = args.substring(0, args.indexOf(" ..."));
            }

            // Flush stdout just in case...
            while ((line = readln(dcrawStdOut)) != null)
                System.out.println(line);

            if (p != null) {
                dcrawStdErr.close();

                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                error = p.exitValue();
                p.destroy();
            } else {
                error = 0;
            }
        }

        System.out.println("dcraw value: " + error);

        if (error > 0) {
            throw new BadImageFileException(of);
        }

        if (ofName == null) {
            System.out.println("Cannot get output filename. Falling back to: " + of.getPath());
        } else if (!ofName.equals(of.getPath())) {
            if (!of.delete()) {
                System.out.println("Could not delete temporary file: " + of);
            }
            of = new File(ofName);
        }
        return of;
    }

    private String[] dcrawCommandLine(dcrawMode mode, boolean secondaryPixels, File of) {
        val makeModel = (m_make + ' ' + m_model).toUpperCase();
        val four_colors = four_color_cameras.contains(makeModel);
        val path = of.getAbsolutePath();

        val cmd = new ArrayList<String>(Arrays.asList(DCRAW_PATH, "-F", path, "-v"));

        switch (mode) {
        case full:
            if (four_colors)
                cmd.addAll(Arrays.asList("-f", "-H", "1", "-t", "0", "-o", "0", "-4"));
            else if (m_filters == -1 || (m_make != null && m_make.equalsIgnoreCase("SIGMA")))
                cmd.addAll(Arrays.asList("-H", "1", "-t", "0", "-o", "0", "-4"));
            else if (secondaryPixels)
                cmd.addAll(Arrays.asList("-j", "-H", "1", "-t", "0", "-s", "1", "-d", "-4"));
            else
                cmd.addAll(Arrays.asList("-j", "-H", "1", "-t", "0", "-d", "-4"));
            break;
        case preview:
            cmd.addAll(Arrays.asList("-t", "0", "-o", "1", "-w", "-h"));
            break;
        case thumb:
            cmd.add("-e");
            break;
        default:
            throw new IllegalArgumentException("Unknown mode " + mode);
        }
        cmd.add(m_fileName);
        return cmd.toArray(new String[0]);
    }

    private ColorModel getColorModel(dcrawMode mode, int bands, int dataType)
            throws UnknownImageTypeException
    {
        final ColorModel cm;
        if (mode == dcrawMode.full) {
            if (bands == 1) {
                cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                        false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
            } else {
                cm = JAIContext.colorModel_linear16;
            }
        } else {
            if (bands == 3) {
                cm = new ComponentColorModel(JAIContext.sRGBColorSpace,
                        false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            } else if (bands == 4) {
                cm = new ComponentColorModel(JAIContext.CMYKColorSpace,
                        false, false, Transparency.OPAQUE, dataType);
            } else {
                throw new UnknownImageTypeException("Weird number of bands: " + bands);
            }
        }
        return cm;
    }

    ////////// private ////////////////////////////////////////////////////////

    @Getter @Accessors(prefix = "m_", fluent = true)
    private boolean m_decodable = true;

    private final String m_fileName;

    @Getter @Accessors(prefix = "m_")
    private String m_make;

    @Getter @Accessors(prefix = "m_")
    private String m_model;

    private long m_captureDateTime;

    @Getter @Accessors(prefix = "m_")
    private int m_imageWidth;

    @Getter @Accessors(prefix = "m_")
    private int m_imageHeight;

    @Getter @Accessors(prefix = "m_")
    private int m_fullWidth;

    @Getter @Accessors(prefix = "m_")
    private int m_fullHeight;

    @Getter @Accessors(prefix = "m_")
    private int m_thumbWidth;

    @Getter @Accessors(prefix = "m_")
    private int m_thumbHeight;

    @Getter @Accessors(prefix = "m_")
    private int m_rawWidth;

    @Getter @Accessors(prefix = "m_")
    private int m_rawHeight;

    @Getter @Accessors(prefix = "m_")
    private float m_shutterSpeed;

    @Getter @Accessors(prefix = "m_")
    private float m_aperture;

    @Getter @Accessors(prefix = "m_")
    private float m_focalLength;

    @Getter @Accessors(prefix = "m_")
    private int m_ISO;

    @Getter @Accessors(prefix = "m_")
    private int m_filters;

    @Getter @Accessors(prefix = "m_", fluent = true)
    private int m_rawColors;

    private float[] m_cam_mul = new float[4];
    private float[] m_pre_mul = new float[4];
    private float[] m_rgb_cam;
    private float[] m_xyz_cam;
    private float[] m_secondary_cam_mul = new float[4];

    private static final Set<String> four_color_cameras = new HashSet<>(Arrays.asList(
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
            "LEICA CAMERA AG M8 DIGITAL CAMERA"
            // TODO: what about the Panasonic 4/3 and the Leica Digilux 3?
    ));

    public native static void interpolateGreen(
            short[] srcData, short[] destData, int width, int height,
            int srcLineStride, int destLineStride,
            int srcOffset, int rOffset, int gOffset, int bOffset,
            int gx, int gy, int ry );

    public native static void interpolateRedBlue(
            short[] jdata, int width, int height, int lineStride,
            int rOffset, int gOffset, int bOffset,
            int rx0, int ry0, int bx0, int by0);
}
/* vim:set et sw=4 ts=4: */
