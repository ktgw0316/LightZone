/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2015-     Masahiro Kitagawa */

package com.lightcrafts.utils.raw;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.libs.LCImageLibException;
import com.lightcrafts.image.libs.LCImageReaderFactory;
import com.lightcrafts.image.metadata.MetadataUtil;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.ForkDaemon;
import com.lightcrafts.utils.LRUHashMap;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

/**
 * Get raw image data by inrerfacing with dcraw as a coprocess.
 *
 * @author Fabio Riccardi [fabio@lightcrafts.com]
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
public final class DCRaw extends RawDecoder {

    //////////  public ////////////////////////////////////////////////////////

    static private final Map<String,DCRaw> dcrawCache =
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
    public LocalDateTime getCaptureDateTime() {
        if (m_captureDateTime <= 0)
            return null;
        final var instant = Instant.ofEpochMilli(m_captureDateTime);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public float[] getDaylightMultipliers() {
        return m_pre_mul.clone();
    }

    private static String readln(InputStream s) {
        try {
            int c = '\n';
            while (c == '\n' || c == '\r') {
                // Skip carriage returns and line feeds
                c = s.read();
            }
            final var sb = new StringBuilder();
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
        final var appDir = Platform.getApplicationDirectory();
        try (final var stream = Files.walk(appDir)) {
            DCRAW_PATH = stream
                    .filter(Files::isExecutable)
                    .filter(path -> path.getFileName().toString().equals(DCRAW_NAME))
                    .findFirst()
                    .orElseThrow()
                    .toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            System.err.println("dcraw_lz not found in appDir: " + appDir);
            throw new RuntimeException(e);
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
        final var info = new String[]{DCRAW_PATH, "-v", "-i", "-t", "0", m_fileName};
        final var secondaryInfo = new String[]{DCRAW_PATH, "-v", "-i", "-s", "1", "-t", "0", m_fileName};

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
            final var multipliers = line.substring(search.length()).split("\\s");
            m_secondary_cam_mul[0] = Float.parseFloat(multipliers[0]);
            m_secondary_cam_mul[1] = Float.parseFloat(multipliers[1]);
            m_secondary_cam_mul[2] = Float.parseFloat(multipliers[2]);
            m_secondary_cam_mul[3] = Float.parseFloat(multipliers[3]);
            return;
        }

        String args;
        // if (line.startsWith(search = FILENAME)) {
        //     final var filename = line.substring(search.length());
        // } else
        if (line.startsWith(search = TIMESTAMP)) {
            final var timestamp = line.substring(search.length());
            try {
                m_captureDateTime = LocalDateTime.parse(timestamp, ISO_LOCAL_DATE_TIME)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli();
            } catch (DateTimeParseException e) {
                m_captureDateTime = 0;
            }
        } else if (line.startsWith(search = CAMERA)) {
            final var camera = line.substring(search.length());
            m_make = camera.substring(0, camera.indexOf(' '));
            m_model = camera.substring(m_make.length() + 1);
            m_decodable = isSupported();
        } else if (line.startsWith(search = ISO)) {
            final var iso = line.substring(search.length());
            m_ISO = Integer.decode(iso);
        } else if (line.startsWith(search = SHUTTER)) {
            final var shutterSpeed = line.substring(search.length() + 2);
            try {
                float exposureTime = Float.parseFloat
                        (shutterSpeed.substring(0, shutterSpeed.indexOf(" sec")));
                if (exposureTime != 0)
                    m_shutterSpeed = 1 / exposureTime;
            } catch (NumberFormatException ignored) { }
        } else if (line.startsWith(search = APERTURE)) {
            final var aperture = line.substring(search.length() + 2);
            try {
                m_aperture = Float.parseFloat(aperture);
            } catch (NumberFormatException ignored) { }
        } else if (line.startsWith(search = FOCAL_LENGTH)) {
            final var focalLenght = line.substring(search.length());
            try {
                m_focalLength = Float.parseFloat(
                        focalLenght.substring(0, focalLenght.indexOf(" mm")));
            } catch (NumberFormatException ignored) { }
            // } else if (line.startsWith(search = NUM_RAW_IMAGES)) {
            //     final var numRawImages = line.substring(search.length());
            // } else if (line.startsWith(search = EMBEDDED_ICC_PROFILE)) {
            //     final var embeddedICCProfile = line.substring(search.length());
        } else if (line.startsWith(CANNOT_DECODE)) {
            m_decodable = false;
        } else if ((args = match(line, THUMB_SIZE)) != null) {
            final var sizes = args.split(" x ");
            m_thumbWidth = Integer.decode(sizes[0]);
            m_thumbHeight = Integer.decode(sizes[1]);
        } else if ((args = match(line, FULL_SIZE)) != null) {
            final var sizes = args.split(" x ");
            m_fullWidth = Integer.decode(sizes[0]);
            m_fullHeight = Integer.decode(sizes[1]);
        } else if ((args = match(line, IMAGE_SIZE)) != null) {
            final var sizes = args.split(" x ");
            m_rawWidth = Integer.decode(sizes[0]);
            m_rawHeight = Integer.decode(sizes[1]);
        } else if ((args = match(line, OUTPUT_SIZE)) != null) {
            final var sizes = args.split(" x ");
            m_imageWidth = Integer.decode(sizes[0]);
            m_imageHeight = Integer.decode(sizes[1]);
        } else if (line.startsWith(search = RAW_COLORS)) {
            final var rawColors = line.substring(search.length());
            m_rawColors = Integer.decode(rawColors);
        } else if (line.startsWith(search = FILTER_PATTERN)) {
            final var pattern = line.substring(search.length());
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
            final var multipliers = line.substring(search.length()).split("\\s");
            m_pre_mul[0] = Float.parseFloat(multipliers[0]);
            m_pre_mul[1] = Float.parseFloat(multipliers[1]);
            m_pre_mul[2] = Float.parseFloat(multipliers[2]);
            m_pre_mul[3] = m_pre_mul[1];
        } else if (line.startsWith(search = CAMERA_MULTIPLIERS)) {
            final var multipliers = line.substring(search.length()).split("\\s");
            m_cam_mul[0] = Float.parseFloat(multipliers[0]);
            m_cam_mul[1] = Float.parseFloat(multipliers[1]);
            m_cam_mul[2] = Float.parseFloat(multipliers[2]);
            m_cam_mul[3] = Float.parseFloat(multipliers[3]);
        } else if (line.startsWith(CAMERA_RGB_PROFILE)) {
            final var rgb_cam = line.substring(CAMERA_RGB_PROFILE.length()).split("\\s");
            m_rgb_cam = new float[9];
            for (int i = 0; i < 9; i++) {
                m_rgb_cam[i] = Float.parseFloat(rgb_cam[i]);
            }
        } else if (line.startsWith(CAMERA_XYZ_PROFILE)) {
            final var xyz_cam = line.substring(CAMERA_XYZ_PROFILE.length()).split("\\s");
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
            final var S1 = readln(s);
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
                    final var S2 = readln(s);
                    final var S3 = readln(s);
                    if (S2 == null || S3 == null) {
                        throw new BadImageFileException(file);
                    }
                    final var dimensions = S2.split("\\s");
                    width = Integer.parseInt(dimensions[0]);
                    height = Integer.parseInt(dimensions[1]);
                    dataType = S3.equals("255") ? DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT;
                    break;
                case "P7":
                    final var SWIDTH = readln(s);
                    final var SHEIGHT = readln(s);
                    final var SDEPTH = readln(s);
                    final var SMAXVAL = readln(s);
                    if (SWIDTH == null || SHEIGHT == null || SDEPTH == null || SMAXVAL == null) {
                        throw new BadImageFileException(file);
                    }
                    final var WIDTH = "WIDTH ";
                    width = Integer.parseInt(SWIDTH.substring(WIDTH.length()));
                    final var HEIGHT = "HEIGHT ";
                    height = Integer.parseInt(SHEIGHT.substring(HEIGHT.length()));
                    final var DEPTH = "DEPTH ";
                    bands = Integer.parseInt(SDEPTH.substring(DEPTH.length()));
                    final var MAXVAL = "MAXVAL ";
                    dataType = SMAXVAL.substring(MAXVAL.length()).equals("65535")
                            ? DataBuffer.TYPE_USHORT
                            : DataBuffer.TYPE_BYTE;
                    break;
                default:
                    throw new BadImageFileException(file);
            }
            final var imageData = new ImageData(width, height, bands, dataType);
            final var totalData = width * height * bands * (dataType == DataBuffer.TYPE_BYTE ? 1 : 2);

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
            throw new UnknownImageTypeException("Unsupported Camera");
        }

        RenderedImage result = null;
        File of = null;

        try {
            if (mode == dcrawMode.preview && m_thumbWidth >= 1024 && m_thumbHeight >= 768) {
                mode = dcrawMode.thumb;
            }

            final var t1 = System.currentTimeMillis();

            if (secondaryPixels)
                runDCRawInfo(true);

            of = getDcrawOutputFile(mode, secondaryPixels);

            final long t2;
            final int totalData;
            if (of.getName().endsWith(".jpg") || of.getName().endsWith(".tiff")) {
                try {
                    final var readerFactory = new LCImageReaderFactory();
                    final var reader = readerFactory.create(of);
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
                final var imageData = readPPM(of);
                t2 = System.currentTimeMillis();
                totalData = imageData.width *
                        imageData.height *
                        imageData.bands * (imageData.dataType == DataBuffer.TYPE_BYTE ? 1 : 2);
                final var cm = getColorModel(mode, imageData.bands, imageData.dataType);
                final var bufSize = imageData.bands * imageData.width * imageData.height;
                final DataBuffer buf = imageData.dataType == DataBuffer.TYPE_BYTE
                        ? new DataBufferByte(   (byte[]) imageData.data, bufSize)
                        : new DataBufferUShort((short[]) imageData.data, bufSize);
                final var bandOffsets = imageData.bands == 3 ? new int[]{0, 1, 2} : new int[]{0};
                final var raster = Raster.createInterleavedRaster(
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

    public synchronized RenderedImage getPreview()
            throws BadImageFileException, UnknownImageTypeException, IOException
    {
        return runDCRaw(DCRaw.dcrawMode.preview);
    }

    public synchronized RenderedImage getThumbnail()
            throws UnknownImageTypeException, BadImageFileException, IOException
    {
        return runDCRaw(DCRaw.dcrawMode.thumb);
    }

    public synchronized RenderedImage getImage()
            throws BadImageFileException, UnknownImageTypeException, IOException
    {
        return runDCRaw(DCRaw.dcrawMode.full, false);
    }

    private File getDcrawOutputFile(dcrawMode mode, boolean secondaryPixels)
            throws IOException, BadImageFileException
    {
        File of = File.createTempFile("LZRAWTMP", ".ppm");
        String ofName = null;

        final var cmd = dcrawCommandLine(mode, secondaryPixels, of);

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

                final var args = match(line, DCRAW_OUTPUT);
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
        final var makeModel = (m_make + ' ' + m_model).toUpperCase();
        final var four_colors = four_color_cameras.contains(makeModel);
        final var path = of.getAbsolutePath();

        final var cmd = new ArrayList<>(Arrays.asList(DCRAW_PATH, "-F", path, "-v"));

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

    private final float[] m_cam_mul = new float[4];
    private final float[] m_pre_mul = new float[4];
    private float[] m_rgb_cam;
    private float[] m_xyz_cam;
    private final float[] m_secondary_cam_mul = new float[4];

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

    private static final Set<String> supported_cameras = new HashSet<>(Arrays.asList(
            "AGFAPHOTO DC-833M",
            "ALCATEL 5035D",
            "APPLE QUICKTAKE 100",
            "APPLE QUICKTAKE 150",
            "APPLE QUICKTAKE 200",
            "ARRIRAW FORMAT",
            "AVT F-080C",
            "AVT F-145C",
            "AVT F-201C",
            "AVT F-510C",
            "AVT F-810C",
            "BAUMER TXG14",
            "BLACKMAGIC URSA",
            "CANON EOS 1000D", "CANON EOS REBEL XS", "CANON EOS KISS F",
            "CANON EOS 100D", "CANON EOS REBEL SL1", "CANON EOS KISS X7",
            "CANON EOS 10D",
            "CANON EOS 1100D", "CANON EOS REBEL T3", "CANON EOS KISS X50",
            "CANON EOS 1200D", "CANON EOS REBEL T5", "CANON EOS KISS X70",
            "CANON EOS 1300D", "CANON EOS REBEL T6", "CANON EOS KISS X80",
            "CANON EOS 1500D", "CANON EOS REBEL T7", "CANON EOS KISS X90", "CANON EOS 2000D",
            "CANON EOS 20D",
            "CANON EOS 3000D", "CANON EOS REBEL T100", "CANON EOS 4000D",
            "CANON EOS 300D", "CANON EOS REBEL", "CANON EOS KISS",
            "CANON EOS 30D",
            "CANON EOS 350D", "CANON EOS REBEL XT", "CANON EOS KISS N",
            "CANON EOS 400D", "CANON EOS REBEL XTI", "CANON EOS KISS X",
            "CANON EOS 40D",
            "CANON EOS 450D", "CANON EOS REBEL XSI", "CANON EOS KISS X2",
            "CANON EOS 500D", "CANON EOS REBEL T1I", "CANON EOS KISS X3",
            "CANON EOS 50D",
            "CANON EOS 550D", "CANON EOS REBEL T2I", "CANON EOS KISS X4",
            "CANON EOS 5D MARK II",
            "CANON EOS 5D MARK III",
            "CANON EOS 5D MARK IV",
            "CANON EOS 5D",
            "CANON EOS 5DS R",
            "CANON EOS 5DS",
            "CANON EOS 600D", "CANON EOS REBEL T3I", "CANON EOS KISS X5",
            "CANON EOS 60D",
            "CANON EOS 650D", "CANON EOS REBEL T4I", "CANON EOS KISS X6I",
            "CANON EOS 6D MARK II",
            "CANON EOS 6D",
            "CANON EOS 700D", "CANON EOS REBEL T5I", "CANON EOS KISS X7I",
            "CANON EOS 70D",
            "CANON EOS 750D", "CANON EOS REBEL T6I", "CANON EOS KISS X8I",
            "CANON EOS 760D", "CANON EOS REBEL T6S", "CANON EOS KISS X9", "CANON EOS 8000D",
            "CANON EOS 77D",
            "CANON EOS 7D MARK II",
            "CANON EOS 7D",
            "CANON EOS 800D", "CANON EOS REBEL T7I", "CANON EOS KISS X9I",
            "CANON EOS 80D",
            "CANON EOS 9000D",
            "CANON EOS C500",
            "CANON EOS D2000C",
            "CANON EOS D30",
            "CANON EOS D60",
            "CANON EOS M",
            "CANON EOS M10",
            "CANON EOS M100",
            "CANON EOS M2", // no raw sample
            "CANON EOS M3",
            "CANON EOS M5",
            "CANON EOS M6",
            "CANON EOS REBEL SL2", "CANON EOS 200D",
            "CANON EOS R",
            "CANON EOS R3",
            "CANON EOS R5",
            "CANON EOS R6",
            "CANON EOS R6m2",
            "CANON EOS R7",
//            "CANON EOS R8",
            "CANON EOS R10",
            "CANON EOS RP",
            "CANON EOS-1D MARK II N",
            "CANON EOS-1D MARK II",
            "CANON EOS-1D MARK III",
            "CANON EOS-1D MARK IV",
            "CANON EOS-1D X MARK II",
            "CANON EOS-1D X",
            "CANON EOS-1D",
            "CANON EOS-1DS MARK II",
            "CANON EOS-1DS MARK III",
            "CANON EOS-1DS",
            "CANON IXUS 160", // (CHDK HACK)
            "CANON POWERSHOT 600",
            "CANON POWERSHOT A3300 IS", // (CHDK HACK)
            "CANON POWERSHOT A460", // (CHDK HACK)
            "CANON POWERSHOT A470", // (CHDK HACK)
            "CANON POWERSHOT A5 ZOOM",
            "CANON POWERSHOT A5",
            "CANON POWERSHOT A50",
            "CANON POWERSHOT A530", // (CHDK HACK)
            "CANON POWERSHOT A570", // (CHDK HACK)
            "CANON POWERSHOT A590", // (CHDK HACK)
            "CANON POWERSHOT A610", // (CHDK HACK)
            "CANON POWERSHOT A620", // (CHDK HACK)
            "CANON POWERSHOT A630", // (CHDK HACK)
            "CANON POWERSHOT A640", // (CHDK HACK)
            "CANON POWERSHOT A650", // (CHDK HACK)
            "CANON POWERSHOT A710 IS", // (CHDK HACK)
            "CANON POWERSHOT A720 IS", // (CHDK HACK)
            "CANON POWERSHOT G1 X MARK II",
            "CANON POWERSHOT G1 X MARK III",
            "CANON POWERSHOT G1 X",
            "CANON POWERSHOT G1",
            "CANON POWERSHOT G10",
            "CANON POWERSHOT G11",
            "CANON POWERSHOT G12",
            "CANON POWERSHOT G15",
            "CANON POWERSHOT G16",
            "CANON POWERSHOT G2",
            "CANON POWERSHOT G3 X",
            "CANON POWERSHOT G3",
            "CANON POWERSHOT G5 X",
            "CANON POWERSHOT G5",
            "CANON POWERSHOT G6",
            "CANON POWERSHOT G7 X MARK II",
            "CANON POWERSHOT G7 X",
            "CANON POWERSHOT G7", // (CHDK HACK)
            "CANON POWERSHOT G9 X MARK II",
            "CANON POWERSHOT G9 X",
            "CANON POWERSHOT G9",
            "CANON POWERSHOT PRO1",
            "CANON POWERSHOT PRO70",
            "CANON POWERSHOT PRO90 IS",
            "CANON POWERSHOT S100",
            "CANON POWERSHOT S110",
            "CANON POWERSHOT S120",
            "CANON POWERSHOT S2 IS", // (CHDK HACK)
            "CANON POWERSHOT S3 IS", // (CHDK HACK)
            "CANON POWERSHOT S30",
            "CANON POWERSHOT S40",
            "CANON POWERSHOT S45",
            "CANON POWERSHOT S5 IS", // (CHDK HACK)
            "CANON POWERSHOT S50",
            "CANON POWERSHOT S60",
            "CANON POWERSHOT S70",
            "CANON POWERSHOT S90",
            "CANON POWERSHOT S95",
            "CANON POWERSHOT SD300", // (CHDK HACK)
            "CANON POWERSHOT SX1 IS",
            "CANON POWERSHOT SX110 IS", // (CHDK HACK)
            "CANON POWERSHOT SX120 IS", // (CHDK HACK)
            "CANON POWERSHOT SX20 IS", // (CHDK HACK)
            "CANON POWERSHOT SX220 HS", // (CHDK HACK)
            "CANON POWERSHOT SX30 IS", // (CHDK HACK)
            "CANON POWERSHOT SX50 HS",
            "CANON POWERSHOT SX60 HS",
            "CASIO EX-FH100",
            "CASIO EX-S100",
            "CASIO EX-S20",
            "CASIO EX-Z1050",
            "CASIO EX-Z1080",
            "CASIO EX-Z4",
            "CASIO EX-Z50",
            "CASIO EX-Z500",
            "CASIO EX-Z55",
            "CASIO EX-Z60",
            "CASIO EX-Z75",
            "CASIO EX-Z750",
            "CASIO EX-Z8",
            "CASIO EX-Z850",
            "CASIO EX-ZR100",
            "CASIO EXLIM PRO 505",
            "CASIO EXLIM PRO 600",
            "CASIO EXLIM PRO 700",
            "CASIO QV-2000UX",
            "CASIO QV-3000EX",
            "CASIO QV-3500EX",
            "CASIO QV-4000",
            "CASIO QV-5700",
            "CASIO QV-R41",
            "CASIO QV-R51",
            "CASIO QV-R61",
            "CONTAX N DIGITAL",
            "CREATIVE PC-CAM 600",
            "DJI 4384X3288",
            "DXO ONE",
            "FOCULUS 531C",
            "FUJIFILM FINEPIX E550",
            "FUJIFILM FINEPIX E900",
            "FUJIFILM FINEPIX F500EXR",
            "FUJIFILM FINEPIX F505EXR", // no raw sample
            "FUJIFILM FINEPIX F550EXR",
            "FUJIFILM FINEPIX F600EXR",
            "FUJIFILM FINEPIX F605EXR", // no raw sample
            "FUJIFILM FINEPIX F700",
            "FUJIFILM FINEPIX F710",
            "FUJIFILM FINEPIX F770EXR",
            "FUJIFILM FINEPIX F775EXR", // no raw sample
            "FUJIFILM FINEPIX F800EXR",
            "FUJIFILM FINEPIX F810", // no raw sample
            "FUJIFILM FINEPIX F900EXR",
            "FUJIFILM FINEPIX S1",
            "FUJIFILM FINEPIX S100FS",
            "FUJIFILM FINEPIX S200EXR",
            "FUJIFILM FINEPIX S205EXR", // no raw sample
            "FUJIFILM FINEPIX S20PRO",
            "FUJIFILM FINEPIX S2PRO",
            "FUJIFILM FINEPIX S3PRO",
            "FUJIFILM FINEPIX S5000",
            "FUJIFILM FINEPIX S5100", "FUJIFILM FINEPIX S5500",
            "FUJIFILM FINEPIX S5200", "FUJIFILM FINEPIX S5600",
            "FUJIFILM FINEPIX S5PRO",
            "FUJIFILM FINEPIX S6000FD",
            "FUJIFILM FINEPIX S6500FD", // no raw sample
            "FUJIFILM FINEPIX S7000",
            "FUJIFILM FINEPIX S9000", "FUJIFILM FINEPIX S9500",
            "FUJIFILM FINEPIX S9100", "FUJIFILM FINEPIX S9600",
            "FUJIFILM FINEPIX X100",
            "FUJIFILM FINEPIXS2PRO",
            "FUJIFILM GFX 100", // OK
            "FUJIFILM GFX100S", // OK
            "FUJIFILM GFX 50S",
            "FUJIFILM GFX50S II", // OK
            "FUJIFILM GFX 50R", // OK
            "FUJIFILM HS11", "FUJIFILM HS10",
            "FUJIFILM HS20EXR",
            "FUJIFILM HS22EXR", // no raw sample
            "FUJIFILM HS30EXR",
            "FUJIFILM HS33EXR", // no raw sample
            "FUJIFILM HS35EXR", // no raw sample
            "FUJIFILM HS50EXR",
            "FUJIFILM IS-1",
            "FUJIFILM SL1000",
            "FUJIFILM X-A1",
            "FUJIFILM X-A2",
            "FUJIFILM X-A3",
            "FUJIFILM X-A5",
            "FUJIFILM X-A7", // no raw sample
            "FUJIFILM X-E1",
            "FUJIFILM X-E2",
            "FUJIFILM X-E2S",
            "FUJIFILM X-E3", // OK
            "FUJIFILM X-E4", // OK
            "FUJIFILM X-H1",
            "FUJIFILM X-H2S", // NG: wrong image size
            "FUJIFILM X-M1",
            "FUJIFILM X-PRO1",
            "FUJIFILM X-PRO2",
            "FUJIFILM X-PRO3", // OK
            "FUJIFILM X-S1",
            "FUJIFILM X-S10", // no raw sample
            "FUJIFILM X-T1",
            "FUJIFILM X-T10",
            "FUJIFILM X-T100", // OK
            "FUJIFILM X-T2",
            "FUJIFILM X-T20",
            "FUJIFILM X-T200", // OK
            "FUJIFILM X-T3",
            "FUJIFILM X-T30", // OK
            "FUJIFILM X-T30 II",
            "FUJIFILM X-T4", // OK
            "FUJIFILM X-T5", // OK
            "FUJIFILM X10", // no raw sample
            "FUJIFILM X100F",
            "FUJIFILM X100S",
            "FUJIFILM X100T",
            "FUJIFILM X100V", // OK
            "FUJIFILM X20",
            "FUJIFILM X30",
            "FUJIFILM X70",
            "FUJIFILM XF1",
            "FUJIFILM XF10",
            "FUJIFILM XQ1",
            "FUJIFILM XQ2",
            "HASSELBLAD CFV",
            "HASSELBLAD CFV-2",
            "HASSELBLAD H3D",
            "HASSELBLAD H4D",
            "HASSELBLAD V96C",
            "HASSELBLAD X1D",
            "IMACON IXPRESS", // 16-, 22-, 39-MEGAPIXEL",
            "ISG", // 2020X1520
            "KODAK C330",
            "KODAK C603",
            "KODAK DC120", // (ALSO TRY KDC2TIFF)
            "KODAK DC20",
            "KODAK DC25",
            "KODAK DC40",
            "KODAK DC50",
            "KODAK DCS PRO 14", "KODAK DCS PRO 14N",
            "KODAK DCS PRO 14NX",
            "KODAK DCS PRO SLR_C",
            "KODAK DCS PRO SLR_N",
            "KODAK DCS200",
            "KODAK DCS315C",
            "KODAK DCS330C",
            "KODAK DCS420",
            "KODAK DCS460",
            "KODAK DCS460A",
            "KODAK DCS460D",
            "KODAK DCS520C",
            "KODAK DCS560C",
            "KODAK DCS620C",
            "KODAK DCS620X",
            "KODAK DCS660C",
            "KODAK DCS660M",
            "KODAK DCS720X",
            "KODAK DCS760C",
            "KODAK DCS760M",
            "KODAK EOSDCS1",
            "KODAK EOSDCS3B",
            "KODAK KAI-0340",
            "KODAK NC2000F",
            "KODAK P712 ZOOM DIGITAL CAMERA",
            "KODAK P850 ZOOM DIGITAL CAMERA",
            "KODAK P880 ZOOM DIGITAL CAMERA",
            "KODAK PB645C",
            "KODAK PB645H",
            "KODAK PB645M",
            "KODAK PROBACK",
            "KODAK Z1015",
            "KODAK Z980",
            "KODAK Z981",
            "KODAK Z990",
            "LEAF AFI 7",
            "LEAF AFI-II 12",
            "LEAF APTUS 17",
            "LEAF APTUS 22",
            "LEAF APTUS 54S",
            "LEAF APTUS 65",
            "LEAF APTUS 75",
            "LEAF APTUS 75S",
            "LEAF CANTARE",
            "LEAF CATCHLIGHT",
            "LEAF CMOST",
            "LEAF DCB2",
            "LEAF VALEO 11",
            "LEAF VALEO 17",
            "LEAF VALEO 22",
            "LEAF VALEO 6",
            "LEAF VOLARE",
            "LEICA C (TYP 112)",
            "LEICA CL",
            "LEICA D-LUX (TYP 109)",
            "LEICA D-LUX2",
            "LEICA D-LUX3",
            "LEICA D-LUX4",
            "LEICA D-LUX5",
            "LEICA D-LUX6",
            "LEICA DIGILUX 2",
            "LEICA DIGILUX 3",
            "LEICA M (TYP 240)",
            "LEICA M (TYP 262)",
            "LEICA M MONOCHROM (TYP 246)",
            "LEICA M10",
            "LEICA M8",
            "LEICA M9",
            "LEICA Q (TYP 116)",
            "LEICA R8",
            "LEICA R9 - DIGITAL BACK DMR",
            "LEICA S (TYP 007)",
            "LEICA SL (TYP 601)",
            "LEICA T (TYP 701)",
            "LEICA TL",
            "LEICA TL2",
            "LEICA V-LUX (TYP 114)",
            "LEICA V-LUX1",
            "LEICA V-LUX2",
            "LEICA V-LUX3",
            "LEICA V-LUX4",
            "LEICA V-LUX5", // TODO: Check
            "LEICA X (TYP 113)",
            "LEICA X VARIO (TYP 107)",
            "LEICA X-E (TYP 102)",
            "LEICA X-U (TYP 113)",
            "LEICA X1",
            "LEICA X2",
            "LENOVO A820",
            "LOGITECH FOTOMAN PIXTURA",
            "MAMIYA ZD",
            "MATRIX 4608X3288",
            "MICRON 2010",
            "MINOLTA ALPHA-7 DIGITAL",
            "MINOLTA DIMAGE 5",
            "MINOLTA DIMAGE 7",
            "MINOLTA DIMAGE 7HI",
            "MINOLTA DIMAGE 7I",
            "MINOLTA DIMAGE A1",
            "MINOLTA DIMAGE A2",
            "MINOLTA DIMAGE A200",
            "MINOLTA DIMAGE G400",
            "MINOLTA DIMAGE G500",
            "MINOLTA DIMAGE G530",
            "MINOLTA DIMAGE G600",
            "MINOLTA DIMAGE Z2",
            "MINOLTA DYNAX 5", "MINOLTA DYNAX 5D", "MINOLTA MAXXUM 5D",
            "MINOLTA DYNAX 7", "MINOLTA DYNAX 7D", "MINOLTA MAXXUM 7D",
            "MINOLTA KD-400Z",
            "MINOLTA KD-510Z",
            "MINOLTA RD175",
            "MOTOROLA PIXL",
            "NIKON 1 AW1",
            "NIKON 1 J1",
            "NIKON 1 J2",
            "NIKON 1 J3",
            "NIKON 1 J4",
            "NIKON 1 J5",
            "NIKON 1 S1",
//            "NIKON 1 S2", // TODO
            "NIKON 1 V1",
            "NIKON 1 V2",
            "NIKON 1 V3",
            "NIKON COOLPIX A",
            "NIKON COOLPIX P330",
            "NIKON COOLPIX P340",
            "NIKON COOLPIX P6000",
            "NIKON COOLPIX P7000",
            "NIKON COOLPIX P7100",
            "NIKON COOLPIX P7700",
            "NIKON COOLPIX P7800",
            "NIKON COOLPIX S6", // ("DIAG RAW" HACK)
            "NIKON D1",
            "NIKON D100",
            "NIKON D1H",
            "NIKON D1X",
            "NIKON D200",
            "NIKON D2H",
            "NIKON D2HS",
            "NIKON D2X",
            "NIKON D2XS",
            "NIKON D3",
            "NIKON D300",
            "NIKON D3000",
            "NIKON D300S",
            "NIKON D3100",
            "NIKON D3200",
            "NIKON D3300",
            "NIKON D3400",
            "NIKON D3500", // OK
            "NIKON D3S",
            "NIKON D3X",
            "NIKON D4",
            "NIKON D40",
            "NIKON D40X",
            "NIKON D4S",
            "NIKON D5",
            "NIKON D50",
            "NIKON D500",
            "NIKON D5000",
            "NIKON D5100",
            "NIKON D5200",
            "NIKON D5300",
            "NIKON D5500",
            "NIKON D5600",
            "NIKON D6", // OK
            "NIKON D60",
            "NIKON D600",
            "NIKON D610",
            "NIKON D70",
            "NIKON D700",
            "NIKON D7000",
            "NIKON D70S",
            "NIKON D7100",
            "NIKON D7200",
            "NIKON D750",
            "NIKON D7500",
            "NIKON D780", // OK
            "NIKON D80",
            "NIKON D800",
            "NIKON D800E",
            "NIKON D810",
            "NIKON D850",
            "NIKON D90",
            "NIKON DF",
            "NIKON E2100", // ("DIAG RAW" HACK)
            "NIKON E2500", // ("DIAG RAW" HACK)
            "NIKON E3200", // ("DIAG RAW" HACK)
            "NIKON E3700", // ("DIAG RAW" HACK)
            "NIKON E4300", // ("DIAG RAW" HACK)
            "NIKON E4500", // ("DIAG RAW" HACK)
            "NIKON E5000",
            "NIKON E5400",
            "NIKON E5700",
            "NIKON E700", // ("DIAG RAW" HACK)
            "NIKON E800", // ("DIAG RAW" HACK)
            "NIKON E8400",
            "NIKON E8700",
            "NIKON E880", // ("DIAG RAW" HACK)
            "NIKON E8800",
            "NIKON E900", // ("DIAG RAW" HACK)
            "NIKON E950", // ("DIAG RAW" HACK)
            "NIKON E990", // ("DIAG RAW" HACK)
            "NIKON E995", // ("DIAG RAW" HACK)
            "NIKON Z 5", // OK
            "NIKON Z 50", // OK
            "NIKON Z 6", // OK
            "NIKON Z 7", // OK
            "NIKON Z 9", // OK
            "NIKON Z FC", // OK
            "NOKIA 1200X1600",
            "NOKIA LUMIA 1020",
            "NOKIA N9",
            "NOKIA N95",
            "NOKIA X2",
            "OLYMPUS AIR-A01",
            "OLYMPUS C3030Z",
            "OLYMPUS C5050Z",
            "OLYMPUS C5060WZ",
            "OLYMPUS C7070WZ",
            "OLYMPUS C70Z,C7000Z",
            "OLYMPUS C740UZ",
            "OLYMPUS C770UZ",
            "OLYMPUS C8080WZ",
            "OLYMPUS E-1",
            "OLYMPUS E-10",
            "OLYMPUS E-20", "OLYMPUS E-20,E-20N,E-20P",
            "OLYMPUS E-3",
            "OLYMPUS E-30",
            "OLYMPUS E-300",
            "OLYMPUS E-330",
            "OLYMPUS E-400",
            "OLYMPUS E-410",
            "OLYMPUS E-420",
//            "OLYMPUS E-450", // TODO
            "OLYMPUS E-5",
            "OLYMPUS E-500",
            "OLYMPUS E-510",
            "OLYMPUS E-520",
//            "OLYMPUS E-600", // TODO
            "OLYMPUS E-620",
            "OLYMPUS E-M1",
            "OLYMPUS E-M10",
            "OLYMPUS E-M10MARKII",
            "OLYMPUS E-M10 MARK III", // OK
            "OLYMPUS E-M10 MARK IIIS", // OK
            "OLYMPUS E-M10MARKIV", // OK
            "OLYMPUS E-M1MARKII",
            "OLYMPUS E-M1MARKIII", // OK
            "OLYMPUS E-M1X", // OK
            "OLYMPUS E-M5",
            "OLYMPUS E-M5MARKII",
            "OLYMPUS E-P1",
            "OLYMPUS E-P2",
            "OLYMPUS E-P3",
            "OLYMPUS E-P5",
            "OLYMPUS E-PL1",
            "OLYMPUS E-PL10", // OK
            "OLYMPUS E-PL1S",
            "OLYMPUS E-PL2",
            "OLYMPUS E-PL3",
            "OLYMPUS E-PL5",
            "OLYMPUS E-PL6",
            "OLYMPUS E-PL7",
            "OLYMPUS E-PL8",
            "OLYMPUS E-PL9",
            "OLYMPUS E-PM1",
            "OLYMPUS E-PM2",
//            "OLYMPUS OM-1", // TODO
            "OLYMPUS PEN-F",
            "OLYMPUS SH-2",
            "OLYMPUS SP310",
            "OLYMPUS SP320",
            "OLYMPUS SP350",
            "OLYMPUS SP500UZ",
            "OLYMPUS SP510UZ",
            "OLYMPUS SP550UZ",
            "OLYMPUS SP560UZ",
//            "OLYMPUS SP565UZ", // TODO
            "OLYMPUS SP570UZ",
            "OLYMPUS STYLUS1", "OLYMPUS STYLUS1,1S",
            "OLYMPUS TG-4",
            "OLYMPUS TG-5",
            "OLYMPUS TG-6", // OK
            "OLYMPUS XZ-1",
            "OLYMPUS XZ-10",
            "OLYMPUS XZ-2",
//            "OLYMPUS X200,D560Z,C350Z",
            "OMNIVISION RP_OV5647", // (RASPBERRY PI)
//            "PANASONIC AG-GH4", // TODO
            "PANASONIC DC-FZ1000M2", "PANASONIC DC-FZ10002", // OK
//            "PANASONIC DC-FZ45", // TODO
            "PANASONIC DC-FZ80", "PANASONIC DC-FZ82", "PANASONIC DC-FZ85",
            "PANASONIC DC-FZ81", "PANASONIC DC-FZ83", // no raw sample
            "PANASONIC DC-G100", "PANASONIC DC-G110", // OK
            "PANASONIC DC-G9",
            "PANASONIC DC-G90", "PANASONIC DC-G91", "PANASONIC DC-G95", "PANASONIC DC-G95D", "PANASONIC DC-G99", "PANASONIC DC-G99D", // OK
            "PANASONIC DC-GF10", "PANASONIC DC-GF90", // no raw sample
            "PANASONIC DC-GF9",
            "PANASONIC DC-GH5",
            "PANASONIC DC-GH5M2", // TODO: Check
            "PANASONIC DC-GH5S",
            "PANASONIC DC-GX7MK3",
            "PANASONIC DC-GX800",
            "PANASONIC DC-GX850",
//            "PANASONIC DC-GX880", // TODO
            "PANASONIC DC-GX9",
            "PANASONIC DC-LX100M2", // OK
//            "PANASONIC DC-S1", "PANASONIC DC-S5", // NG
//            "PANASONIC DC-S1H", // no raw sample
//            "PANASONIC DC-S1R", // NG
            "PANASONIC DC-TZ90",
//            "PANASONIC DC-TZ96", // TODO
//            "PANASONIC DC-TZ97", // TODO
            "PANASONIC DC-ZS70", "PANASONIC DC-TZ90", "PANASONIC DC-TZ91", "PANASONIC DC-TZ92", "PANASONIC DC-TZ93", "PANASONIC DC-TZ95",
            "PANASONIC DC-ZS200", "PANASONIC DC-TX2", "PANASONIC DC-TZ200", "PANASONIC DC-TZ202", "PANASONIC DC-TZ220", "PANASONIC DC-ZS220",
            "PANASONIC DMC-CM1",
            "PANASONIC DMC-FX150",
//            "PANASONIC DMC-FX180", // TODO
            "PANASONIC DMC-FZ100",
            "PANASONIC DMC-FZ1000",
            "PANASONIC DMC-FZ150",
            "PANASONIC DMC-FZ18",
            "PANASONIC DMC-FZ200",
            "PANASONIC DMC-FZ2000",
            "PANASONIC DMC-FZ2500",
            "PANASONIC DMC-FZ28",
            "PANASONIC DMC-FZ30",
            "PANASONIC DMC-FZ300",
            "PANASONIC DMC-FZ330",
            "PANASONIC DMC-FZ35",
            "PANASONIC DMC-FZ38",
            "PANASONIC DMC-FZ40", "PANASONIC DMC-FZ45",
//            "PANASONIC DMC-FZ42", // TODO
            "PANASONIC DMC-FZ50",
            "PANASONIC DMC-FZ70", "PANASONIC DMC-FZ72",
            "PANASONIC DMC-FZ8",
            "PANASONIC DMC-G1",
            "PANASONIC DMC-G10",
            "PANASONIC DMC-G2",
            "PANASONIC DMC-G3",
            "PANASONIC DMC-G5",
            "PANASONIC DMC-G6",
            "PANASONIC DMC-G7", "PANASONIC DMC-G70",
            "PANASONIC DMC-G80",
            "PANASONIC DMC-G81",
            "PANASONIC DMC-G85",
            "PANASONIC DMC-GF1",
            "PANASONIC DMC-GF2",
            "PANASONIC DMC-GF3",
            "PANASONIC DMC-GF5",
            "PANASONIC DMC-GF6",
            "PANASONIC DMC-GF7",
            "PANASONIC DMC-GH1",
            "PANASONIC DMC-GH2",
            "PANASONIC DMC-GH3",
            "PANASONIC DMC-GH4",
            "PANASONIC DMC-GM1",
            "PANASONIC DMC-GM1S", // TODO: Check
            "PANASONIC DMC-GM5",
            "PANASONIC DMC-GX1",
            "PANASONIC DMC-GX7",
            "PANASONIC DMC-GX8",
            "PANASONIC DMC-GX80", "PANASONIC DMC-GX85", "PANASONIC DMC-GX7MK2",
            "PANASONIC DMC-L1",
            "PANASONIC DMC-L10",
            "PANASONIC DMC-LC1",
            "PANASONIC DMC-LF1",
            "PANASONIC DMC-LX1",
            "PANASONIC DMC-LX100",
            "PANASONIC DMC-LX2",
            "PANASONIC DMC-LX3",
            "PANASONIC DMC-LX5",
            "PANASONIC DMC-LX7",
            "PANASONIC DMC-LX9", "PANASONIC DMC-LX15", "PANASONIC DMC-LX10",
            "PANASONIC DMC-ZS100", "PANASONIC DMC-ZS110", "PANASONIC DMC-TZ100", "PANASONIC DMC-TZ101", "PANASONIC DMC-TZ110", "PANASONIC DMC-TX1",
            "PANASONIC DMC-ZS40", "PANASONIC DMC-TZ60", "PANASONIC DMC-TZ61",
            "PANASONIC DMC-ZS50", "PANASONIC DMC-TZ70", "PANASONIC DMC-TZ71",
            "PANASONIC DMC-ZS60", "PANASONIC DMC-TZ80", "PANASONIC DMC-TZ81", "PANASONIC DMC-TZ85", // OK
            "PENTAX _IST D",
            "PENTAX _IST DL",
            "PENTAX _IST DL2",
            "PENTAX _IST DS",
            "PENTAX _IST DS2",
            "PENTAX 645D",
            "PENTAX 645Z",
            "PENTAX K-1 II", // no raw sample
            "PENTAX K-1",
            "PENTAX K-3 II",
            "PENTAX K-3",
            "PENTAX K-5 II S",
            "PENTAX K-5 II",
            "PENTAX K-5",
            "PENTAX K-50",
            "PENTAX K-500",
            "PENTAX K-7",
            "PENTAX K-70",
            "PENTAX K-R",
            "PENTAX K-S1",
            "PENTAX K-S2",
            "PENTAX K-X",
            "PENTAX K100D SUPER",
            "PENTAX K100D",
            "PENTAX K10D",
            "PENTAX K110D", // no raw sample
            "PENTAX K2000", "PENTAX K-M",
            "PENTAX K200D",
            "PENTAX K20D",
            "PENTAX KP",
            "PENTAX OPTIO 33WR",
            "PENTAX OPTIO 750Z",
            "PENTAX OPTIO S",
            "PENTAX OPTIO S4",
            "PENTAX Q-S1",
            "PENTAX Q7",
            "PHASE ONE H 10",
            "PHASE ONE H 20",
            "PHASE ONE H 25",
            "PHASE ONE LIGHTPHASE",
            "PHASE ONE P 20",
            "PHASE ONE P 25",
            "PHASE ONE P 30",
            "PHASE ONE P 45",
            "PHASE ONE P 45+",
            "PHOTRON BC2-HD",
            "PIXELINK A782",
            "POLAROID X530",
            "REDCODE R3D FORMAT",
            "RICOH CAPLIO GX100",
            "RICOH GR II",
            "RICOH GR",
            "RICOH GX200",
            "RICOH GXR",
            "RICOH GXR A12",
            "RICOH GXR A16",
            "RICOH GXR MOUNT A12",
            "ROLLEI D530FLEX",
            "ROVERSHOT 3320AF",
            "SAMSUNG EK-GN120",
            "SAMSUNG EX1",
            "SAMSUNG EX2F",
            "SAMSUNG GX-1S",
            "SAMSUNG GX10",
            "SAMSUNG GX20",
            "SAMSUNG NX MINI",
            "SAMSUNG NX1",
            "SAMSUNG NX10",
            "SAMSUNG NX100",
            "SAMSUNG NX1000",
            "SAMSUNG NX11",
            "SAMSUNG NX1100",
            "SAMSUNG NX20",
            "SAMSUNG NX200",
            "SAMSUNG NX2000",
            "SAMSUNG NX210",
            "SAMSUNG NX30",
            "SAMSUNG NX300",
            "SAMSUNG NX3000",
            "SAMSUNG NX300M",
            "SAMSUNG NX500",
            "SAMSUNG S85", // (HACKED)
            "SAMSUNG S850", // (HACKED)
            "SAMSUNG WB2000",
            "SAMSUNG WB550",
            "SARNOFF 4096X5440",
            "SEIKO EPSON CORP. R-D1",
            "SIGMA DP1 MERILL",
            "SIGMA DP1",
            "SIGMA DP1S",
            "SIGMA DP1X",
            "SIGMA DP2 MERILL",
            "SIGMA DP2",
            "SIGMA DP2S",
            "SIGMA DP2X",
            "SIGMA SD1 MERILL",
            "SIGMA SD1",
            "SIGMA SD10",
            "SIGMA SD14",
            "SIGMA SD15",
            "SIGMA SD9",
            "SINAR 3072X2048",
            "SINAR 4080X4080",
            "SINAR 4080X5440",
            "SINAR STI FORMAT",
            "SMAL ULTRA-POCKET 3",
            "SMAL ULTRA-POCKET 4",
            "SMAL ULTRA-POCKET 5",
            "SONY DSC-F828",
            "SONY DSC-R1",
            "SONY DSC-RX0",
            "SONY DSC-RX0M2", // OK
            "SONY DSC-RX1",
            "SONY DSC-RX10",
            "SONY DSC-RX100",
            "SONY DSC-RX100M2",
            "SONY DSC-RX100M3",
            "SONY DSC-RX100M4",
            "SONY DSC-RX100M5",
            "SONY DSC-RX100M5A", // OK
            "SONY DSC-RX100M6", // OK
            "SONY DSC-RX100M7", // OK
            "SONY DSC-RX10M2",
            "SONY DSC-RX10M3",
            "SONY DSC-RX10M4",
            "SONY DSC-RX1R",
            "SONY DSC-RX1RM2",
            "SONY DSC-V3",
            "SONY DSLR-A100",
            "SONY DSLR-A200",
            "SONY DSLR-A230",
            "SONY DSLR-A290",
            "SONY DSLR-A300",
            "SONY DSLR-A330",
            "SONY DSLR-A350",
            "SONY DSLR-A380",
            "SONY DSLR-A390",
            "SONY DSLR-A450",
            "SONY DSLR-A500",
            "SONY DSLR-A550",
            "SONY DSLR-A560",
            "SONY DSLR-A580",
            "SONY DSLR-A700",
            "SONY DSLR-A850",
            "SONY DSLR-A900",
            "SONY ILCA-68",
            "SONY ILCA-77M2",
            "SONY ILCA-99M2",
            "SONY ILCE-1", // OK
            "SONY ILCE-3000",
            "SONY ILCE-3500", // OK
            "SONY ILCE-5000",
            "SONY ILCE-5100",
            "SONY ILCE-6000",
            "SONY ILCE-6001", // TODO: Check
            "SONY ILCE-6100", // TODO: Check
            "SONY ILCE-6300",
            "SONY ILCE-6400", // TODO: Check
            "SONY ILCE-6500",
            "SONY ILCE-6600", // TODO: Check
            "SONY ILCE-7",
            "SONY ILCE-7C", // OK
            "SONY ILCE-7M2",
            "SONY ILCE-7M3",
            "SONY ILCE-7M4", // OK
            "SONY ILCE-7R",
            "SONY ILCE-7RM2",
            "SONY ILCE-7RM3",
            "SONY ILCE-7RM3A", // TODO: Check
            "SONY ILCE-7RM4", // OK
            "SONY ILCE-7RM4A", // TODO: Check
            "SONY ILCE-7RM5", // TODO: no dcraw matrix
            "SONY ILCE-7S",
            "SONY ILCE-7SM2",
            "SONY ILCE-7SM3", // OK
            "SONY ILCE-9",
            "SONY ILCE-9M2", // TODO: Check
            "SONY ILCE-QX1",
            "SONY NEX-3",
            "SONY NEX-3N",
            "SONY NEX-5",
            "SONY NEX-5N",
            "SONY NEX-5R",
            "SONY NEX-5T",
            "SONY NEX-6",
            "SONY NEX-7",
            "SONY NEX-C3",
            "SONY NEX-F3",
            "SONY SLT-A33",
            "SONY SLT-A35",
            "SONY SLT-A37",
            "SONY SLT-A55", "SONY SLT-A55V",
            "SONY SLT-A57",
            "SONY SLT-A58",
            "SONY SLT-A65", "SONY SLT-A65V",
            "SONY SLT-A77", "SONY SLT-A77V",
            "SONY SLT-A99", "SONY SLT-A99V",
            "SONY XCD-SX910CR",
            "STV680 VGA",
            "XIRO XPLORER V",
            "YI M1"
    ));

    private String shortCameraName() {
        var makeModel = (m_make + ' ' + m_model).toUpperCase();
        makeModel = Camera.getCompatibleCameraName(makeModel);
        switch (m_make.toUpperCase()) {
            case "CANON":
                if (m_model.toUpperCase().startsWith("EOS ")) {
                    makeModel = makeModel.replace(" DIGITAL", "");
                }
                break;
            case "LEICA":
                makeModel = makeModel
                        .replace(" CAMERA AG", "")
                        .replace(" DIGITAL CAMERA", "");
                break;
            case "KONICA":
            case "MINOLTA":
                makeModel = makeModel
                        .replace("KONICA MINOLTA ", "MINOLTA ")
                        .replace(" CAMERA, INC.", "")
                        .replace(" CO., LTD.", "");
                break;
            case "NIKON":
                makeModel = makeModel.replace(" CORPORATION", "");
                break;
            case "OLYMPUS":
                makeModel = makeModel
                        .replace(" CORPORATION", "")
                        .replace(" IMAGING", "")
                        .replace(" CORP.", "")
                        .replace(" OPTICAL CO., LTD", "");
                break;
            case "RICOH":
                // e.g. "RICOH IMAGING COMPANY, LTD. PENTAX 645Z" or "RICOH IMAGING COMPANY, LTD. GR II"
                makeModel = makeModel
                        .replace("RICOH IMAGING COMPANY, LTD.", "PENTAX")
                        .replace("PENTAX PENTAX", "PENTAX");
                break;
            case "SAMSUNG":
                makeModel = makeModel.replace(" TECHWIN", "");
                break;
        }
        return makeModel;
    }

    public boolean isSupported() {
        return m_fileName.toLowerCase().endsWith(".dng") || supported_cameras.contains(shortCameraName());
    }

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
