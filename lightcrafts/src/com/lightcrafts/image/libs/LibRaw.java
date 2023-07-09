package com.lightcrafts.image.libs;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.raw.RawDecoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.util.Arrays;
import java.util.Date;

public class LibRaw extends RawDecoder {
    final String filePath;

    int         progress_flags;
    int         process_warnings;

    // libraw_iparams_t

    String make;
    String model;
    int raw_count;
    int dng_version;
    int         colors;
    int filters;
    String      cdesc;
    
    // libraw_image_sizes_t
    
    int raw_height,
            raw_width,
            height,
            width,
            top_margin,
            left_margin;
    int iheight,
            iwidth;
    float pixel_aspect;
    int         flip;

    // libraw_colordata_t

    int[][] white = new int[8][8];
    float[] cam_mul = new float[4];
    float[] pre_mul = new float[4];
    float[][] cmatrix = new float[3][4];
    float[][] rgb_cam = new float[3][4];
    float[][] cam_xyz = new float[4][3];
    int[] curve;
    int black;
    int[] cblack = new int[8];
    int maximum;
    int[] channel_maximum = new int[4];
    
    // Phase One
    int format, key_off, t_black, black_off, split_col, tag_21a;
    float tag_210;
    
    float       flash_used; 
    float       canon_ev; 
    String      model2;
    byte[] profile;
    int profile_length;
    
    // libraw_imgother_t
    
    float       iso_speed; 
    float       shutter;
    float       aperture;
    float       focal_len;
    long timestamp;
    int shot_order;
    int[] gpsdata = new int[32];
    String      desc,
                artist;

    int tformat;
    int twidth,
                theight;
    int tlength;
    int         tcolors;
    
    String filter_pattern;
        

    static final public int 
    LIBRAW_PROGRESS_START = 0,
    LIBRAW_PROGRESS_OPEN                = 1,
    LIBRAW_PROGRESS_IDENTIFY            = 1<<1,
    LIBRAW_PROGRESS_SIZE_ADJUST         = 1<<2,
    LIBRAW_PROGRESS_LOAD_RAW            = 1<<3,
    LIBRAW_PROGRESS_REMOVE_ZEROES       = 1<<4,
    LIBRAW_PROGRESS_BAD_PIXELS          = 1<<5,
    LIBRAW_PROGRESS_DARK_FRAME          = 1<<6,
    LIBRAW_PROGRESS_SCALE_COLORS        = 1<<8,
    LIBRAW_PROGRESS_PRE_INTERPOLATE     = 1<<9,
    LIBRAW_PROGRESS_INTERPOLATE         = 1<<10,
    LIBRAW_PROGRESS_MIX_GREEN           = 1<<11,
    LIBRAW_PROGRESS_MEDIAN_FILTER       = 1<<12,
    LIBRAW_PROGRESS_HIGHLIGHTS          = 1<<13,
    LIBRAW_PROGRESS_FUJI_ROTATE         = 1<<14,
    LIBRAW_PROGRESS_FLIP                = 1<<15,
    LIBRAW_PROGRESS_APPLY_PROFILE       = 1<<16,
    LIBRAW_PROGRESS_CONVERT_RGB         = 1<<17,
    LIBRAW_PROGRESS_STRETCH             = 1<<18,
/* reserved */
    LIBRAW_PROGRESS_STAGE19             = 1<<19,
    LIBRAW_PROGRESS_STAGE20             = 1<<20,
    LIBRAW_PROGRESS_STAGE21             = 1<<21,
    LIBRAW_PROGRESS_STAGE22             = 1<<22,
    LIBRAW_PROGRESS_STAGE23             = 1<<23,
    LIBRAW_PROGRESS_STAGE24             = 1<<24,
    LIBRAW_PROGRESS_STAGE25             = 1<<25,
    LIBRAW_PROGRESS_STAGE26             = 1<<26,
    LIBRAW_PROGRESS_STAGE27             = 1<<27,

    LIBRAW_PROGRESS_THUMB_LOAD          = 1<<28,
    LIBRAW_PROGRESS_TRESERVED1          = 1<<29,
    LIBRAW_PROGRESS_TRESERVED2          = 1<<30,
    LIBRAW_PROGRESS_TRESERVED3          = 1<<31;
    
    native int openFile(String fname);
    native short[] unpackImage(boolean interpolate, boolean halfSize);
    native byte[] unpackThumb();

//    native int adjustSizesInfoOnly();
//    native int adjustMaximum();
    
    /* void set_memerror_handler( memory_callback cb,void *data)
    void set_dataerror_handler(data_callback func, void *data)
    void set_progress_handler(progress_callback pcb, void *data) */
    
    private native synchronized void recycle();

    public LibRaw(String filePath) {
        this.filePath = filePath;
        createLibRawObject();
        openFile(filePath);
        disposeLibRawObject();
    }

    private native synchronized void createLibRawObject();
    
    private native synchronized void disposeLibRawObject();

    @SuppressWarnings("unused")
    private final long libRawObject = 0;

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getCameraMake(boolean includeModel) {
        String make = getMake();
        final String model = getModel();
        if ( make == null || model == null )
            return null;
        make = make.toUpperCase();
        if ( !includeModel )
            return make;
        return undupMakeModel( make, model.toUpperCase() );
    }

    /**
     * Some camera manufacturers duplicate the "make" at the beginning of the
     * "model" metadata field so that, when concatenated, you get something
     * like "Canon Canon EOS 10D".  This is dumb, so this method takes the
     * make and model strings and returns a single make/model string without a
     * duplicate make.
     *
     * @param make The make the camera.
     * @param model The model of the camera.
     * @return Returns the make and model with a duplicate make removed (if it
     * was duplicated) or the make and model concatenated with a space (if not
     * duplicated).
     */
    private static String undupMakeModel(String make, String model) {
        make = make.trim();
        model = model.trim();
        final String MAKE  = make.toUpperCase();
        final String MODEL = model.toUpperCase();
        if (MODEL.contains(MAKE)) {
            // Case 1: The model contains the make, e.g., "Canon EOS 10D"
            // contains "Canon", so just return the model.
            return model;
        }

        final int spacePos = MODEL.indexOf(' ');
        if (spacePos > 1) {
            if (MAKE.contains(MODEL.substring(0, spacePos))) {
                // Case 2: The make contains the first word of the model.  This
                // case is needed for at least Nikon because their make has the
                // word "Corporation" in it, e.g., "Nikon Corporation", so this
                // kind of make won't be in the model.
                // However, if the make contains the first word of the model,
                // e.g., "Nikon Coproration" contains "Nikon" (the first word
                // of "Nikon D2X"), then just return the model.
                return model;
            }
        }

        // If we get here, assume the make isn't duplicated.
        return make + ' ' + model;
    }

    public float getAperture() {
        return aperture;
    }

    public Date getCaptureDateTime() {
        return new Date(1000 * timestamp);
    }

    public float getFocalLength() {
        return focal_len;
    }

    public int getISO() {
        return (int) iso_speed;
    }

    public float getShutterSpeed() {
        return shutter;
    }

    public int getImageHeight() {
        return iheight;
    }

    public int getImageWidth() {
        return iwidth;
    }

    public int getRawHeight() {
        return raw_height;
    }
    public int getRawWidth() {
        return raw_width;
    }
    public int getThumbHeight() {
        return theight;
    }
    public int getThumbWidth() {
        return twidth;
    }
    public float[] getCameraMultipliers() {
        return cam_mul;
    }
    public float[][] getCameraRGB() {
        return rgb_cam;
    }
    public float[][] getCameraXYZ() {
        return cam_xyz;
    }
    public float[] getDaylightMultipliers() {
        return pre_mul;
    }
    public int getFilters() {
        if (filter_pattern.startsWith("BGGR"))
            return 0x16161616;
        else if (filter_pattern.startsWith("GRBG"))
            return 0x61616161;
        else if (filter_pattern.startsWith("GBRG"))
            return 0x49494949;
        else if (filter_pattern.startsWith("RGGB"))
            return 0x94949494;
        else
            return -1;
    }
    public boolean decodable() {
        // TODO: make this real
        return true; // m_decodable;
    }
    public int rawCount() {
        return raw_count;
    }
    public int dngVersion() {
        return dng_version;
    }
    public int rawColors() {
        return colors;
    }
    public int getMaximum() {
        return maximum;
    }
    public int getBlack() {
        return black;
    }
    
    public int progress(int stage, int iteration, int expected) {
        System.out.println("progress - stage: " + strProgress(stage) + ", iteration: " + iteration + ", expected: " + expected);
        return 0;
    }

    public synchronized RenderedImage getThumbnail() throws UnknownImageTypeException, BadImageFileException {
        return getThumbnail(0, 0);
    }

    public synchronized RenderedImage getThumbnail(int maxWidth, int maxHeight) throws UnknownImageTypeException, BadImageFileException {
        long time = System.currentTimeMillis();
        createLibRawObject();
        openFile(filePath);
        byte[] thumbnail = unpackThumb();
        time = System.currentTimeMillis() - time;
        System.out.println("LibRaw Exception (p:" + progress_flags + ", w:" + process_warnings + ")");
        disposeLibRawObject();

        if (thumbnail == null)
            throw new BadImageFileException(new File(filePath), "LibRaw Exception (p:" + progress_flags + ", w:" + process_warnings + ")");
        else
            System.out.println("Read " + (2 * thumbnail.length) + " bytes in " + time + "ms");

        if (tformat == 1) {
            // TODO: what to do with the color space? How do we detect AdobeRGB?
        	return JPEGImageType.getImageFromBuffer(thumbnail, 0, thumbnail.length, JAIContext.sRGBColorSpace, maxWidth, maxHeight);
        } else {
            final ColorModel cm;
            if (tcolors == 3)
                cm = new ComponentColorModel(JAIContext.sRGBColorSpace,
                                             false, false,
                                             Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            else if (tcolors == 4)
                cm = new ComponentColorModel(JAIContext.CMYKColorSpace,
                                             false, false,
                                             Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            else
                throw new UnknownImageTypeException("Weird number of bands: " + tcolors);

            final DataBuffer buf = new DataBufferByte(thumbnail, tcolors * twidth * theight);

            final WritableRaster raster = Raster.createInterleavedRaster(
                    buf, twidth, theight,
                    tcolors * twidth, tcolors,
                    tcolors == 3 ? new int[]{ 0, 1, 2 } : new int[]{0}, null
            );

            return new BufferedImage(cm, raster, false, null);
        }
    }
    
    public synchronized RenderedImage getPreview() throws BadImageFileException {
        return getImage(true);
    }
    
    private static final String[] four_color_cameras = {
        "OLYMPUS E-1",
        "OLYMPUS E-300",
        "OLYMPUS E-330",
        "OLYMPUS E-500",
        "OLYMPUS E-510",
        "OLYMPUS E-400",
        "OLYMPUS E-410",
        "OLYMPUS E-20,E-20N,E-20P",
        "OLYMPUS E-10",
        "Leica Camera AG M8 Digital Camera"
        // TODO: what about the Panasonic 4/3 and the Leica Digilux 3?
    };
    
    public synchronized RenderedImage getImage() throws BadImageFileException {
        return getImage(false);
    }
    
    private synchronized RenderedImage getImage(boolean halfSize) throws BadImageFileException {
        long time = System.currentTimeMillis();
        createLibRawObject();
        openFile(filePath);

        final String makeModel = make + ' ' + model;
        final boolean four_colors = Arrays.stream(four_color_cameras)
                .anyMatch(s -> s.equalsIgnoreCase(makeModel));

        short[] image_data = unpackImage(four_colors, halfSize);
        disposeLibRawObject();
        time = System.currentTimeMillis() - time;

        if (image_data == null)
            throw new BadImageFileException(new File(filePath), "LibRaw Exception (p:" + progress_flags + ", w:" + process_warnings + ")");
        else
            System.out.println("Read " + (2 * image_data.length) + " bytes in " + time + "ms");

        final int width, height;
        if (halfSize) {
            width = iwidth / 2;
            height = iheight / 2;
        } else {
            width = iwidth;
            height = iheight;
        }

        final int bands = 3;
        final ColorModel cm = JAIContext.colorModel_linear16;
        final DataBuffer buf = new DataBufferUShort(image_data, bands * width * height);
        final WritableRaster raster = Raster.createInterleavedRaster(
                buf, width, height, bands * width, bands, new int[]{ 0, 1, 2 }, null);
        return new BufferedImage(cm, raster, false, null);
    }
    
    public static String strProgress(int p) {
        switch(p) {
            case LIBRAW_PROGRESS_START:
                return "Starting";
            case LIBRAW_PROGRESS_OPEN :
                return "Opening file";
            case LIBRAW_PROGRESS_IDENTIFY :
                return "Reading metadata";
            case LIBRAW_PROGRESS_SIZE_ADJUST:
                return "Adjusting size";
            case LIBRAW_PROGRESS_LOAD_RAW:
                return "Reading RAW data";
            case LIBRAW_PROGRESS_REMOVE_ZEROES:
                return "Clearing zero values";
            case LIBRAW_PROGRESS_BAD_PIXELS :
                return "Removing dead pixels";
            case LIBRAW_PROGRESS_DARK_FRAME:
                return "Subtracting dark frame data";
            case LIBRAW_PROGRESS_SCALE_COLORS:
                return "Scaling colors";
            case LIBRAW_PROGRESS_PRE_INTERPOLATE:
                return "Pre-interpolating";
            case LIBRAW_PROGRESS_INTERPOLATE:
                return "Interpolating";
            case LIBRAW_PROGRESS_MIX_GREEN :
                return "Mixing green channels";
            case LIBRAW_PROGRESS_MEDIAN_FILTER   :
                return "Median filter";
            case LIBRAW_PROGRESS_HIGHLIGHTS:
                return "Highlight recovery";
            case LIBRAW_PROGRESS_FUJI_ROTATE :
                return "Rotating Fuji diagonal data";
            case LIBRAW_PROGRESS_FLIP :
                return "Flipping image";
            case LIBRAW_PROGRESS_APPLY_PROFILE:
                return "ICC conversion";
            case LIBRAW_PROGRESS_CONVERT_RGB:
                return "Converting to RGB";
            case LIBRAW_PROGRESS_STRETCH:
                return "Stretching image";
            case LIBRAW_PROGRESS_THUMB_LOAD:
                return "Loading thumbnail";
            default:
                return "Some strange things";
        }
    }

    public static void main(String[] args) {
        System.out.println("Testing LibRaw");

        LibRaw libRaw = new LibRaw("/Stuff/Pictures/New Raw Support/Canon 450D/IMG_1598.CR2");

        System.out.println("LibRaw (p:" + libRaw.progress_flags + ", w:" + libRaw.process_warnings + ") - make: " + libRaw.make + ", model: " + libRaw.model + ", timestamp: " + new Date(1000 * libRaw.timestamp));
        System.out.println("Filter pattern: " + libRaw.filter_pattern);

        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 3; j++)
                System.out.print(libRaw.cam_xyz[i][j] + " ");
        System.out.println();

        try {
            RenderedImage image = libRaw.getImage();
            System.out.println("Image (p:" + libRaw.progress_flags + ", w:" + libRaw.process_warnings + "): " + image);
            ImageIO.write(image, "PNG", new File("/tmp/out.png"));

            RenderedImage preview = libRaw.getPreview();
            System.out.println("Image (p:" + libRaw.progress_flags + ", w:" + libRaw.process_warnings + "): " + preview);
            ImageIO.write(preview, "PNG", new File("/tmp/out-preview.png"));

            RenderedImage thumbnail = libRaw.getThumbnail(0, 0);
            System.out.println("Thumbnail (p:" + libRaw.progress_flags + ", w:" + libRaw.process_warnings + "): " + thumbnail);
            ImageIO.write(thumbnail, "PNG", new File("/tmp/out-thumb.png"));
        } catch (Exception e) {
            System.out.println("LibRaw Exception (p:" + libRaw.progress_flags + ", w:" + libRaw.process_warnings + ")");
            e.printStackTrace();
        }

        System.out.println("Done testing LibRaw!");
    }

    static {
        System.loadLibrary("LIBRAW");
    }
}
