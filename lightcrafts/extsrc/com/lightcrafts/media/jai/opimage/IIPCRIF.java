/*
 * $RCSfile: IIPCRIF.java,v $
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
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderableImageOp;
import java.awt.image.renderable.RenderContext;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.EnumeratedParameter;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.LookupTableJAI;
import com.lightcrafts.mediax.jai.MultiResolutionRenderableImage;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.ROIShape;
import com.lightcrafts.mediax.jai.TiledImage;
import com.lightcrafts.mediax.jai.operator.TransposeDescriptor;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.codec.ImageCodec;
import com.lightcrafts.media.jai.codec.ImageDecoder;
import com.lightcrafts.media.jai.codec.MemoryCacheSeekableStream;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * This CRIF implements the "iip" operation in the rendered and renderable
 * image layers.
 *
 * <p> In renderable mode this operation is designed to execute on the
 * server as many composed operations (those specified via parameters) as
 * the server's capability permits.  In the 1.0 implementation all operations
 * are actually carried out on the client: server-side processing will be
 * added in a subsequent release.
 *
 * <p> Rendered operation returns the default rendering of renderable mode
 * operation.
 *
 * <p> The actual set of composed operations is described in section 2.2.1.1
 * "Composed Image Commands" ot the "Internet Imaging Protocol Specification"
 * version 1.0.5.  The sequence in which these commands are to be applied is
 * also described in this section.
 *
 * <p> More detailed information actually required to understand and implement
 * the composed operations is available in the "FlashPix Format Specification"
 * version 1.0.1.  Of particular interest are Section 2 "Image Data
 * Representation", Section 5.3.3 "Relationship of NIF RGB to sRGB",
 * Section 5.4 "Relating PhotoYCC to NIG RGB", Section 7.2 "Viewing Transform
 * Parameters", and Section 7.3 "Sequence of Viewing Parameter
 * Transformations".  Also of note are Tables 3.6 and 3.7.
 *
 * @since 1.0
 *
 * @see IIPDescriptor
 * @see IIPResolutionRIF
 * @see IIPResolutionOpImage
 * @see <a href="http://www.digitalimaging.org">Digital Imaging Group</a>
 *
 */
public class IIPCRIF extends CRIFImpl {
    // Bitmask constants indicating supplied parameters.
    private static final int MASK_FILTER = 0x1;
    private static final int MASK_COLOR_TWIST = 0x2;
    private static final int MASK_CONTRAST = 0x4;
    private static final int MASK_ROI_SOURCE = 0x8;
    private static final int MASK_TRANSFORM = 0x10;
    private static final int MASK_ASPECT_RATIO = 0x20;
    private static final int MASK_ROI_DESTINATION = 0x40;
    private static final int MASK_ROTATION = 0x80;
    private static final int MASK_MIRROR_AXIS = 0x100;
    private static final int MASK_ICC_PROFILE = 0x200;
    private static final int MASK_JPEG_QUALITY = 0x400;
    private static final int MASK_JPEG_TABLE = 0x800;

    // Constants indicating server vendors.
    private static final int VENDOR_HP = 0;
    private static final int VENDOR_LIVE_PICTURE = 1;
    private static final int VENDOR_KODAK = 2;
    private static final int VENDOR_UNREGISTERED = 255;
    private static final int VENDOR_EXPERIMENTAL = 999;

    //  Bitmask constants indicating server capabilities
    private static final int SERVER_CVT_JPEG = 0x1;
    private static final int SERVER_CVT_FPX = 0x2;
    private static final int SERVER_CVT_MJPEG = 0x4;
    private static final int SERVER_CVT_MFPX = 0x8;
    private static final int SERVER_CVT_M2JPEG = 0x10;
    private static final int SERVER_CVT_M2FPX = 0x20;
    private static final int SERVER_CVT_JTL = 0x40;

    // Special bitmask combinations
    private static final int SERVER_JPEG_PARTIAL =
        SERVER_CVT_JPEG | SERVER_CVT_MJPEG;
    private static final int SERVER_JPEG_FULL =
        SERVER_JPEG_PARTIAL | SERVER_CVT_M2JPEG;
    private static final int SERVER_FPX_PARTIAL =
        SERVER_CVT_FPX | SERVER_CVT_MFPX;
    private static final int SERVER_FPX_FULL =
        SERVER_FPX_PARTIAL | SERVER_CVT_M2FPX;

    // --- RGB[A] <-> PhotoYCC[A] metric conversion matrices ---
    // As stated in the FlashPix specification, these matrices are
    // sufficient for tone and color correction but should not be
    // used for actual color space conversion calculations.

    // PhotoYCCA -> RGBA metric color conversion matrix
    private static final double[][] YCCA_TO_RGBA =
    new double[][]
        {{1.358400, 0.000000, 1.821500, 0.000000},
            {1.358400, -0.430300, -0.927100, 0.000000},
                {1.358400, 2.217900, 0.000000, 0.000000},
                    {0.000000, 0.000000, 0.000000, 1.000000}};

    // PhotoYCCA -> RGBA metric color conversion constant
    private static final double[][] YCCA_TO_RGBA_CONST =
        new double[][] {{-249.55}, {194.14}, {-345.99}, {0.0}};

    // RGBA -> PhotoYCCA metric color conversion matrix
    private static final double[][] RGBA_TO_YCCA =
        new double[][]
        {{0.220018, 0.432276, 0.083867, 0.000000},
            {-0.134755, -0.264756, 0.399511, 0.000000},
                {0.384918, -0.322373, -0.062544, 0.000000},
                    {0.000000, 0.000000, 0.000000, 1.000000}};

    // RGBA -> PhotoYCCA metric color conversion constant
    private static final double[][] RGBA_TO_YCCA_CONST =
        new double[][] {{0.0005726}, {155.9984}, {137.0022}, {0.0}};

    // PhotoYCC -> RGB metric color conversion matrix
    private static final double[][] YCC_TO_RGB =
        new double[][]
        {{1.358400, 0.000000, 1.821500},
            {1.358400, -0.430300, -0.927100},
                {1.358400, 2.217900, 0.000000}};

    // PhotoYCC -> RGB metric color conversion constant
    private static final double[][] YCC_TO_RGB_CONST =
        new double[][] {{-249.55}, {194.14}, {-345.99}};

    // RGB -> PhotoYCC metric color conversion matrix
    private static final double[][] RGB_TO_YCC =
        new double[][]
        {{0.220018, 0.432276, 0.083867},
            {-0.134755, -0.264756, 0.399511},
                {0.384918, -0.322373, -0.062544}};

    // RGB -> PhotoYCC metric color conversion constant
    private static final double[][] RGB_TO_YCC_CONST =
        new double[][] {{0.0005726}, {155.9984}, {137.0022}};

    /**
     * Returns the operation mask based on the supplied parameters.
     */
    private static final int getOperationMask(ParameterBlock pb) {
        int opMask = 0;

        // Initialize the operation mask according to which
        // parameters are actually supplied.
        if(pb.getFloatParameter(2) != 0.0F) {
            opMask |= MASK_FILTER;
        }
        if(pb.getObjectParameter(3) != null) {
            opMask |= MASK_COLOR_TWIST;
        }
        if(Math.abs(pb.getFloatParameter(4) - 1.0F) > 0.01F) {
            opMask |= MASK_CONTRAST;
        }
        if(pb.getObjectParameter(5) != null) {
            opMask |= MASK_ROI_SOURCE;
        }
        AffineTransform tf = (AffineTransform)pb.getObjectParameter(6);
        if(!tf.isIdentity()) {
            opMask |= MASK_TRANSFORM;
        }
        if(pb.getObjectParameter(7) != null) {
            opMask |= MASK_ASPECT_RATIO;
        }
        if(pb.getObjectParameter(8) != null) {
            opMask |= MASK_ROI_DESTINATION;
        }
        if(pb.getIntParameter(9) != 0) {
            opMask |= MASK_ROTATION;
        }
        if(pb.getObjectParameter(10) != null) {
            opMask |= MASK_MIRROR_AXIS;
        }
        if(pb.getObjectParameter(11) != null) {
            opMask |= MASK_ICC_PROFILE;
        }
        if(pb.getObjectParameter(12) != null) {
            opMask |= MASK_JPEG_QUALITY;
        }
        if(pb.getObjectParameter(13) != null) {
            opMask |= MASK_JPEG_TABLE;
        }

        return opMask;
    }

    /**
     * Returns the server capability mask.
     */
    private static final int getServerCapabilityMask(String URLSpec,
                                                     RenderedImage lowRes) {
        int vendorID = 255; // Unregistered vendor.
        int serverMask = 0;

        // Get the server bitmask from the properties of the thumbnail image.
        if(lowRes.getProperty("iip-server") != null &&
           lowRes.getProperty("iip-server") != Image.UndefinedProperty) {
            String serverString = (String)lowRes.getProperty("iip-server");
            int dot = serverString.indexOf(".");
            vendorID =
                Integer.valueOf(serverString.substring(0, dot)).intValue();
            serverMask =
                Integer.valueOf(serverString.substring(dot + 1)).intValue();
        }

        // If the vendor is not one the three that defined the IIP
        // specification then assume that the response to the OBJ=IIP-server
        // command is inaccurate. This may not be true in general but it
        // is true of the only other IIP server tested with this code.
        if(serverMask != 127 &&
           vendorID != VENDOR_HP &&
           vendorID != VENDOR_LIVE_PICTURE &&
           vendorID != VENDOR_KODAK) {
            int[] maxSize = (int[])lowRes.getProperty("max-size");
            String rgn =
                "&RGN=0.0,0.0,"+(64.0F/maxSize[0])+","+(64.0F/maxSize[1]);

            // Actually test these capabilities
            if(canDecode(URLSpec, "&CNT=0.9&WID=64&CVT=JPEG", "JPEG")) {
                // CVT-JPEG && CVT-MJPEG && CVT-M2JPEG
                serverMask = SERVER_JPEG_FULL;
            } else if(canDecode(URLSpec, "&CNT=0.9&WID=64&CVT=FPX", "FPX")) {
                // CVT-FPX && CVT-MFPX && CVT-M2FPX
                serverMask = SERVER_FPX_FULL;
            } else if(canDecode(URLSpec, rgn+"&CVT=JPEG", "JPEG")) {
                // CVT-JPEG && CVT-MJPEG
                serverMask = SERVER_JPEG_PARTIAL;
            } else if(canDecode(URLSpec, rgn+"&CVT=FPX", "FPX")) {
                // CVT-FPX && CVT-MFPX
                serverMask = SERVER_FPX_PARTIAL;
            }
        }

        return serverMask;
    }

    /**
     * Test whether an image can be decoded from an IIP CVT URL.
     *
     * @param base The base IIP URL including the image specification.
     * @param suffix The IIP URL suffix including the CVT string.
     * @param fmt The desired format: "JPEG" or "FPX".
     * @return Whether the returned stream can be dedoced successfully.
     */
    private static boolean canDecode(String base, String suffix, String fmt) {
        StringBuffer buf = new StringBuffer(base);

        URL url = null;
        InputStream stream = null;
        RenderedImage rendering = null;

        boolean itWorks = false;

        try {
            buf.append(suffix);
            url = new URL(buf.toString());
            stream = url.openStream();
            ImageDecoder decoder =
                ImageCodec.createImageDecoder(fmt, stream, null);
            rendering = decoder.decodeAsRenderedImage();
            itWorks = true;
        } catch(Exception e) {
            itWorks = false; // redundant
        }

        return itWorks;
    }

    /**
     * Multiply two matrix parameters and return the result. The number of
     * columns of the first parameter must equal the number of rows of
     * the second parameter. The result will have the same number of rows
     * as the first parameter and the same number of columns as the
     * second parameter.
     */
    private static final double[][] matrixMultiply(double[][] A,
                                                   double[][] B) {
        if(A[0].length != B.length) {
            throw new RuntimeException(JaiI18N.getString("IIPCRIF0"));
        }

        int nRows = A.length;
        int nCols = B[0].length;
        double[][] C = new double[nRows][nCols];

        int nSum = A[0].length;
        for(int r = 0; r < nRows; r++) {
            for(int c = 0; c < nCols; c++) {
                C[r][c] = 0.0;
                for(int k = 0; k < nSum; k++) {
                    C[r][c] += A[r][k]*B[k][c];
                }
            }
        }

        return C;
    }

    /**
     * Compose a matrix A and a vector b into an array suitable for the
     * "Bandcombine" operation. The number of rows in the matrix must
     * equal the number of elements in the vector.
     */
    private static final double[][] composeMatrices(double[][] A,
                                                    double[][] b) {
        int nRows = A.length;
        if(nRows != b.length) {
            throw new RuntimeException(JaiI18N.getString("IIPCRIF1"));
        } else if(b[0].length != 1) {
            throw new RuntimeException(JaiI18N.getString("IIPCRIF2"));
        }
        int nCols = A[0].length;

        double[][] bcMatrix = new double[nRows][nCols+1];

        for(int r = 0; r < nRows; r++) {
            for(int c = 0; c < nCols; c++) {
                bcMatrix[r][c] = A[r][c];
            }
            bcMatrix[r][nCols] = b[r][0];
        }

        return bcMatrix;
    }

    /**
     * Generate a matrix which can perform the composite mapping from the
     * original color space to normalized Photo YCC, apply the color-twist
     * transformation, and return normalized Photo YCC to the original
     * color space including casting down opacity and chroma channels where
     * appropriate.
     */
    private static final double[][] getColorTwistMatrix(ColorModel colorModel,
							ParameterBlock pb) {
        // Convert color-twist matrix to 2D form.
        float[] ctwParam = (float[])pb.getObjectParameter(3);
        double[][] ctw = new double[4][4];
        int k = 0;
        for(int r = 0; r < 4; r++) {
            for(int c = 0; c < 4; c++) {
                ctw[r][c] = ctwParam[k++];
            }
        }

        // Calculate composed metric color conversion/color-twist matrix H
        // and constant d.
        double[][] H = null;
        double[][] d = null;
        int csType = colorModel.getColorSpace().getType();
        if(csType == ColorSpace.TYPE_GRAY ||
           csType == ColorSpace.TYPE_RGB) {
            // Calculate RGBA->YCCA->CTW->RGBA composed matix.
            H = matrixMultiply(matrixMultiply(YCCA_TO_RGBA, ctw),
                               RGBA_TO_YCCA);
            d = YCCA_TO_RGBA_CONST;
        } else { // PYCC
            H = ctw;
            d = new double[][] {{0.0}, {0.0}, {0.0}, {0.0}};
        }

        // Calculate matrix A and vector b to cast data upwards to 4 bands.
        double[][] A = null;
        double[][] b = null;
        if(csType == ColorSpace.TYPE_GRAY) {
            if(colorModel.hasAlpha()) {
                A = new double[][]
                    {{1.0, 0.0}, {1.0, 0.0}, {1.0, 0.0}, {0.0, 1.0}};
                b = new double[][] {{0.0}, {0.0}, {0.0}, {0.0}};
            } else {
                A = new double[][] {{1.0}, {1.0}, {1.0}, {0.0}};
                b = new double[][] {{0.0}, {0.0}, {0.0}, {255.0}};
            }
        } else if(!colorModel.hasAlpha()) { // RGB or YCC (no alpha)
            A = new double[][]
                {{1.0, 0.0, 0.0},
                    {0.0, 1.0, 0.0},
                        {0.0, 0.0, 1.0},
                            {0.0, 0.0, 0.0}};
            b = new double[][] {{0.0}, {0.0}, {0.0}, {255.0}};
        } else { // RGBA or YCCA
            A = new double[][]
                {{1.0, 0.0, 0.0, 0.0},
                    {0.0, 1.0, 0.0, 0.0},
                        {0.0, 0.0, 1.0, 0.0},
                            {0.0, 0.0, 0.0, 1.0}};
            b = new double[][] {{0.0}, {0.0}, {0.0}, {0.0}};
        }

        // Determine whether chroma or opacity may be deleted.
        boolean truncateChroma = false;
        if(csType == ColorSpace.TYPE_GRAY &&
           ctwParam[4] == 0.0F && ctwParam[7] == 0.0F &&
           ctwParam[8] == 0.0F && ctwParam[11] == 0.0F) {
            truncateChroma = true;
        }
        boolean truncateAlpha = false;
        if(!colorModel.hasAlpha() && ctwParam[15] == 1.0F) {
            truncateAlpha = true;
        }

        // Calculate matrix T to truncate data down to alpha-less or
        // chroma-less data as appropriate.
        double[][] T = null;
        if(truncateAlpha && truncateChroma) {
            T = new double[][] {{1.0, 0.0, 0.0, 0.0}};
        } else if(truncateChroma) {
            T = new double[][]
                {{1.0, 0.0, 0.0, 0.0},
                    {0.0, 0.0, 0.0, 1.0}};
        } else if(truncateAlpha) {
            T = new double[][]
                {{1.0, 0.0, 0.0, 0.0},
                    {0.0, 1.0, 0.0, 0.0},
                        {0.0, 0.0, 1.0, 0.0}};
        } else { // Retain all bands
            T = new double[][]
                {{1.0, 0.0, 0.0, 0.0},
                    {0.0, 1.0, 0.0, 0.0},
                        {0.0, 0.0, 1.0, 0.0},
                            {0.0, 0.0, 0.0, 1.0}};
        }

        // Combine the matrices and vectors to get the overall transform.
        double[][] TH = matrixMultiply(T, H);
        double[][] THA = matrixMultiply(TH, A);
        double[][] THb = matrixMultiply(TH, b);
        double[][] THd = matrixMultiply(TH, d);
        double[][] Td = matrixMultiply(T, d);

        for(int r = 0; r < THb.length; r++) {
            for(int c = 0; c < THb[r].length; c++) {
                THb[r][c] += Td[r][c] - THd[r][c];
            }
        }

        // Compose the results into a form appropriate for "BandCombine".
        return composeMatrices(THA, THb);
    }

    /**
     * Creates a lookup table for the contrast operation. This is to be
     * applied to grayscale or RGB data possibly with an alpha channel.
     */
    private static final LookupTableJAI createContrastLUT(float K,
                                                          int numBands) {
        byte[] contrastTable = new byte[256];

        double p = 0.43F;

        // Generate the LUT to be applied to the color band(s).
        for(int i = 0; i < 256; i++) {
            float j = (float)(i - 127.5F)/255.0F;
            float f = 0.0F;
            if(j < 0.0F) {
                f = (float)(-p*Math.pow(-j/p, K));
            } else if(j > 0.0F) {
                f = (float)(p*Math.pow(j/p, K));
            }
            int val = (int)(f*255.0F + 127.5F);
            if(val < 0) {
                contrastTable[i] = 0;
            } else if(val > 255) {
                contrastTable[i] = (byte)255;
            } else {
                contrastTable[i] = (byte)(val & 0x000000ff);
            }
        }

        // Allocate LUT memory.
        byte[][] data = new byte[numBands][];

        // Set all LUT color bands to the same previously calculated table.
        // If alpha is present, set the LUT for it to a ramp.
        if(numBands % 2 == 1) { // no alpha channel present
            for(int i = 0; i < numBands; i++) {
                data[i] = contrastTable;
            }
        } else { // alpha channel present
            for(int i = 0; i < numBands - 1; i++) {
                data[i] = contrastTable;
            }
            data[numBands-1] = new byte[256];
            byte[] b = data[numBands-1];
            for(int i = 0; i < 256; i++) {
                b[i] = (byte)i;
            }
        }

        return new LookupTableJAI(data);
    }

    /** Constructor. */
    public IIPCRIF() {
        super("IIP");
    }

    /**
     * Performs all operations on the server.
     */
    private RenderedImage serverProc(int serverMask,
                                     RenderContext renderContext,
                                     ParameterBlock paramBlock,
                                     int opMask,
                                     RenderedImage lowRes) {
        // Ensure that one of the four expected combinations obtains.
        if((serverMask & SERVER_JPEG_FULL) != SERVER_JPEG_FULL &&
           (serverMask & SERVER_FPX_FULL) != SERVER_FPX_FULL &&
           (serverMask & SERVER_JPEG_PARTIAL) != SERVER_JPEG_PARTIAL &&
           (serverMask & SERVER_FPX_PARTIAL) != SERVER_FPX_PARTIAL) {
            return null;
        }

        ImagingListener listener = ImageUtil.getImagingListener(renderContext);

        // Set JPEG and full server flags.
        boolean isJPEG = false;
        boolean isFull = false;
        if((serverMask & SERVER_JPEG_FULL) == SERVER_JPEG_FULL) {
            isJPEG = isFull = true;
        } else if((serverMask & SERVER_FPX_FULL) == SERVER_FPX_FULL) {
            isJPEG = false;
            isFull = true;
        } else if((serverMask & SERVER_JPEG_PARTIAL) == SERVER_JPEG_PARTIAL) {
            isJPEG = true;
            isFull = false;
        }

        // Create a StringBuffer for the composed image command URL.
        StringBuffer buf =
            new StringBuffer((String)paramBlock.getObjectParameter(0));
        //TODO: subImages (how?)

        // Filtering.
        if((opMask & MASK_FILTER) != 0) {
            buf.append("&FTR="+paramBlock.getFloatParameter(2));
        }

        // Color-twist.
        if((opMask & MASK_COLOR_TWIST) != 0) {
            buf.append("&CTW=");
            float[] ctw = (float[])paramBlock.getObjectParameter(3);
            for(int i = 0; i < ctw.length; i++) {
                buf.append(ctw[i]);
                if(i != ctw.length-1) {
                    buf.append(",");
                }
            }
        }

        // Contrast.
        if((opMask & MASK_CONTRAST) != 0) {
            buf.append("&CNT="+paramBlock.getFloatParameter(4));
        }

        // Source rectangle of interest.
        if((opMask & MASK_ROI_SOURCE) != 0) {
            Rectangle2D roi =
                (Rectangle2D)paramBlock.getObjectParameter(5);
            buf.append("&ROI="+roi.getX()+","+ roi.getY()+","+
                       roi.getWidth()+","+roi.getHeight());
        }

        // If full support for the CVT command is available, decompose the
        // AffineTransform specifying the transformation from renderable to
        // rendered coordinates into a translation, a pure scale, and the
        // residual transformation. The residual transformation may then be
        // concatenated with the server-side affine transform (after
        // inversion), the pure scale may be effected by specifying the WID
        // and HEI composed image command modifiers, and the translation as
        // a subsequent operation. If the WID and HEI modifiers are not
        // available, i.e., the server support is partial, this becomes
        // more problematic. Fortunately no such servers are known to exist.

        // Initialize the post-processing transform to the identity.
        AffineTransform postTransform = new AffineTransform();

        // Retrieve (a clone of) the renderable-to-rendered mapping.
        AffineTransform at =
            (AffineTransform)renderContext.getTransform().clone();

        // If the translation is non-zero set the post-transform.
        if(at.getTranslateX() != 0.0 || at.getTranslateY() != 0.0) {
            postTransform.setToTranslation(at.getTranslateX(),
                                           at.getTranslateY());
            double[] m = new double[6];
            at.getMatrix(m);
            at.setTransform(m[0], m[1], m[2], m[3], 0.0, 0.0);
        }

        // Determine the renderable destination region of interest.
        Rectangle2D rgn = null;
        if((opMask & MASK_ROI_DESTINATION) != 0) {
            rgn = (Rectangle2D)paramBlock.getObjectParameter(8);
        } else {
            float aspectRatio = 1.0F;
            if((opMask & MASK_ASPECT_RATIO) != 0) {
                aspectRatio = paramBlock.getFloatParameter(7);
            } else {
                aspectRatio =
                    ((Float)(lowRes.getProperty("aspect-ratio"))).floatValue();
            }
            rgn = new Rectangle2D.Float(0.0F, 0.0F, aspectRatio, 1.0F);
        }

        // Apply the renderable-to-rendered mapping to the renderable
        // destination region of interest.
        Rectangle dstROI = at.createTransformedShape(rgn).getBounds();

        // Calculate the pure scale portion of the
        // renderable-to-rendered mapping.
        AffineTransform scale =
            AffineTransform.getScaleInstance(dstROI.getWidth()/
                                             rgn.getWidth(),
                                             dstROI.getHeight()/
                                             rgn.getHeight());

        // Determine the residual mapping.
        try {
            at.preConcatenate(scale.createInverse());
        } catch(Exception e) {
            String message = JaiI18N.getString("IIPCRIF6");
            listener.errorOccurred(message,
                                   new ImagingException(message, e),
                                   this, false);
//            throw new RuntimeException(JaiI18N.getString("IIPCRIF6"));
        }

        // Compose the inverse residual mapping with the renderable
        // transform.
        AffineTransform afn =
            (AffineTransform)paramBlock.getObjectParameter(6);
        try {
            afn.preConcatenate(at.createInverse());
        } catch(Exception e) {
            String message = JaiI18N.getString("IIPCRIF6");
            listener.errorOccurred(message,
                                   new ImagingException(message, e),
                                   this, false);
//            throw new RuntimeException(JaiI18N.getString("IIPCRIF6"));
        }

        if(isFull) {
            // Append the WID and HEI composed image command modifiers using
            // the dimensions of the rendered destination region of interest.
            buf.append("&WID="+dstROI.width+"&HEI="+dstROI.height);
            /* XXX Begin suppressed section.
        } else if((opMask & MASK_TRANSFORM) != 0) {
            Point2D[] dstPts =
                new Point2D[] {new Point2D.Double(rgn.getMinX(),
                                                  rgn.getMinY()),
                                   new Point2D.Double(rgn.getMaxX(),
                                                      rgn.getMinY()),
                                   new Point2D.Double(rgn.getMinX(),
                                                      rgn.getMaxY())};
            Point2D[] srcPts = new Point2D[3];
            afn.transform(dstPts, 0, srcPts, 0, 3);

            double LLeft = srcPts[0].distance(srcPts[2]);
            double LTop = srcPts[0].distance(srcPts[1]);

            int[] maxSize = (int[])lowRes.getProperty("max-size");

            double H = maxSize[1]*LLeft;
            double W = maxSize[1]*LTop;

            double m = Math.max(H, W*(double)maxSize[1]/(double)maxSize[0]);

            int Hp = (int)(m + 0.5);
            int Wp = (int)(m*(double)maxSize[0]/(double)maxSize[1] + 0.5);
            System.out.println("Estimated dimensions = "+Wp+" x "+Hp);

            AffineTransform scl =
                AffineTransform.getScaleInstance(dstROI.getWidth()/Wp,
                                                 dstROI.getHeight()/Hp);
            System.out.println("scl = "+scl);
            afn.preConcatenate(scl);
            End suppressed section. XXX */
        }


        // Append the affine tranform composed image command.
        double[] matrix = new double[6];
        afn.getMatrix(matrix);
        buf.append("&AFN="+
                   matrix[0]+","+matrix[2]+",0,"+matrix[4]+","+
                   matrix[1]+","+matrix[3]+",0,"+matrix[5]+
                   ",0,0,1,0,0,0,0,1");

        // Destination aspect ratio.
        if((opMask & MASK_ASPECT_RATIO) != 0) {
            buf.append("&RAR="+paramBlock.getFloatParameter(7));
        }

        // Destination rectangle of interest.
        if((opMask & MASK_ROI_DESTINATION) != 0) {
            Rectangle2D dstRGN =
                (Rectangle2D)paramBlock.getObjectParameter(8);
            buf.append("&RGN="+dstRGN.getX()+","+ dstRGN.getY()+","+
                       dstRGN.getWidth()+","+dstRGN.getHeight());
        }

        // Rotation and mirroring.
        if(isFull) {
            if((opMask & MASK_ROTATION) != 0 ||
               (opMask & MASK_MIRROR_AXIS) != 0) {
                buf.append("&RFM="+paramBlock.getIntParameter(9));
                if((opMask & MASK_MIRROR_AXIS) != 0) {
                    String axis = (String)paramBlock.getObjectParameter(10);
                    if(axis.equalsIgnoreCase("x")) {
                        buf.append(",0");
                    } else {
                        buf.append(",90");
                    }
                }
            }
        }

        // ICC profile.
        if((opMask & MASK_ICC_PROFILE) != 0) {
            // According to the IIP specification this is not supported
            // over HTTP connections and that is all that is available from
            // the vendors right now, i.e., no socket connections are
            // available (includes LivePicture and TrueSpectra).
        }

        // JPEG quality and compression group index.
        if(isJPEG) {
            if((opMask & MASK_JPEG_QUALITY) != 0) {
                buf.append("&QLT="+paramBlock.getIntParameter(12));
            }

            if((opMask & MASK_JPEG_TABLE) != 0) {
                buf.append("&CIN="+paramBlock.getIntParameter(13));
            }
        }

        // Set the format string.
        String format = isJPEG ? "JPEG" : "FPX";

        // Append the CVT command.
        buf.append("&CVT="+format);

        // Create a URL with the CVT string, open a stream from it, and
        // decode the image using the appropriate decoder.
        InputStream stream = null;
        RenderedImage rendering = null;
        try {
            URL url = new URL(buf.toString());
            stream = url.openStream();
            MemoryCacheSeekableStream sStream =
                new MemoryCacheSeekableStream(stream);
            rendering = JAI.create(format, sStream);
        } catch(Exception e) {
            String message =
                JaiI18N.getString("IIPCRIF7") + " " + buf.toString();
            listener.errorOccurred(message,
                                   new ImagingException(message, e),
                                   this, false);
//            throw new RuntimeException(e.getClass()+" "+e.getMessage());
        }

        // If WID and HEI modifiers are unavailable add scale.
        if(!isFull) {
            postTransform.scale(dstROI.getWidth()/rendering.getWidth(),
                                dstROI.getHeight()/rendering.getHeight());
        }

        // Translate (and scale) the result if necessary.
        if(!postTransform.isIdentity()) {
            Interpolation interp =
                Interpolation.getInstance(Interpolation.INTERP_NEAREST);
            RenderingHints hints = renderContext.getRenderingHints();
            if(hints != null && hints.containsKey(JAI.KEY_INTERPOLATION)) {
                interp = (Interpolation)hints.get(JAI.KEY_INTERPOLATION);
            }
            rendering = JAI.create("affine", rendering,
                                   postTransform, interp);
        }

        return rendering;
    }

    /**
     * Performs all operations on the client.
     */
    private RenderedImage clientProc(RenderContext renderContext,
                                     ParameterBlock paramBlock,
                                     int opMask,
                                     RenderedImage lowRes) {
        // Cache RenderContext components.
        AffineTransform at = renderContext.getTransform();
        RenderingHints hints = renderContext.getRenderingHints();

        ImagingListener listener = ImageUtil.getImagingListener(renderContext);

        // Obtain the number of levels and the size of the largest one.
        int[] maxSize = (int[])lowRes.getProperty("max-size");
        int maxWidth = maxSize[0];
        int maxHeight = maxSize[1];
        int numLevels =
            ((Integer)lowRes.getProperty("resolution-number")).intValue();

        // Calculate the aspect ratios.
        float aspectRatioSource = (float)maxWidth/(float)maxHeight;
        float aspectRatio = (opMask & MASK_ASPECT_RATIO) != 0 ?
            paramBlock.getFloatParameter(7) : aspectRatioSource;

        // Determine the bounds of the destination image.
        Rectangle2D bounds2D = new Rectangle2D.Float(0.0F, 0.0F,
                                                     aspectRatio, 1.0F);

        // Determine the dimensions of the rendered destination image.
        int width;
        int height;
        if(at.isIdentity()) { // Default rendering.
            AffineTransform afn =
                (AffineTransform)paramBlock.getObjectParameter(6);
            Rectangle2D bounds =
                afn.createTransformedShape(bounds2D).getBounds2D();
            double H = maxHeight*bounds.getHeight();
            double W = maxHeight*bounds.getWidth();
            double m = Math.max(H, W/aspectRatioSource);
            height = (int)(m + 0.5);
            width = (int)(aspectRatioSource*m + 0.5);
            at = AffineTransform.getScaleInstance(width, height);
            renderContext = (RenderContext)renderContext.clone();
            renderContext.setTransform(at);
        } else {
            Rectangle bounds = at.createTransformedShape(bounds2D).getBounds();
            width = bounds.width;
            height = bounds.height;
        }

        // Determine which resolution level of the IIP image to request.
        int res = numLevels - 1;
        int hRes = maxHeight;
        while(res > 0) {
            hRes = (int)((hRes + 1.0F)/2.0F); // get the next height
            if(hRes < height) { // stop if the next height is too small
                break;
            }
            res--;
        }

        // Create a RenderableImage from the selected resolution level.
        int[] subImageArray = (int[])paramBlock.getObjectParameter(1);
        int subImage = subImageArray.length < res + 1 ? 0 : subImageArray[res];
        if(subImage < 0) {
            subImage = 0;
        }
        ParameterBlock pb = new ParameterBlock();
        pb.add(paramBlock.getObjectParameter(0)).add(res).add(subImage);
        RenderedImage iipRes = JAI.create("iipresolution", pb);
        Vector sources = new Vector(1);
        sources.add(iipRes);
        RenderableImage ri =
            new MultiResolutionRenderableImage(sources, 0.0F, 0.0F,
                                               1.0F);

        // Filtering.
        if((opMask & MASK_FILTER) != 0) {
            float filter = paramBlock.getFloatParameter(2);
            pb = (new ParameterBlock()).addSource(ri).add(filter);
            ri = new RenderableImageOp(new FilterCRIF(), pb);
        }

        // Color-twist.
        // Cache the original number of bands in case the number of bands
        // changes due to addition of chroma and/or alpha channels in the
        // color-twist procedure.
	int nBands = iipRes.getSampleModel().getNumBands();
        if((opMask & MASK_COLOR_TWIST) != 0) {
	    double[][] ctw = getColorTwistMatrix(iipRes.getColorModel(),
						 paramBlock);
            pb = (new ParameterBlock()).addSource(ri).add(ctw);
            ri = JAI.createRenderable("bandcombine", pb);
	    nBands = ctw.length;
        }

        // Contrast.
        if((opMask & MASK_CONTRAST) != 0) {
            int csType = iipRes.getColorModel().getColorSpace().getType();
            boolean isPYCC =
                csType != ColorSpace.TYPE_GRAY &&
                csType != ColorSpace.TYPE_RGB;

            if(isPYCC) {
                double[][] matrix;
                if(nBands == 3) { // PYCC
                    matrix = composeMatrices(YCC_TO_RGB, YCC_TO_RGB_CONST);
                } else { // PYCC-A
                    matrix = composeMatrices(YCCA_TO_RGBA, YCCA_TO_RGBA_CONST);
                }
                pb = (new ParameterBlock()).addSource(ri).add(matrix);
                ri = JAI.createRenderable("bandcombine", pb);
            }

            float contrast = paramBlock.getFloatParameter(4);
            LookupTableJAI lut = createContrastLUT(contrast, nBands);

            pb = (new ParameterBlock()).addSource(ri).add(lut);
            ri = JAI.createRenderable("lookup", pb);

            if(isPYCC) {
                double[][] matrix;
                if(nBands == 3) { // PYCC
                    matrix = composeMatrices(RGB_TO_YCC, RGB_TO_YCC_CONST);
                } else { // PYCC-A
                    matrix = composeMatrices(RGBA_TO_YCCA, RGBA_TO_YCCA_CONST);
                }
                pb = (new ParameterBlock()).addSource(ri).add(matrix);
                ri = JAI.createRenderable("bandcombine", pb);
            }
        }

        // Source rectangle of interest.
        if((opMask & MASK_ROI_SOURCE) != 0) {
            // Get the source rectangle of interest.
            Rectangle2D rect = (Rectangle2D)paramBlock.getObjectParameter(5);

            // Check for intersection with source bounds.
            if(!rect.intersects(0.0, 0.0, aspectRatioSource, 1.0)) {
                throw new RuntimeException(JaiI18N.getString("IIPCRIF5"));
            }

            // Create the source rectangle.
            Rectangle2D rectS = new Rectangle2D.Float(0.0F, 0.0F,
                                                      aspectRatioSource, 1.0F);

            // Crop out the desired region.
            if(!rect.equals(rectS)) {
                // Clip to the source bounds.
                rect = rect.createIntersection(rectS);

                // Crop to the clipped rectangle of interest.
                pb = (new ParameterBlock()).addSource(ri);
                pb.add((float)rect.getMinX()).add((float)rect.getMinY());
                pb.add((float)rect.getWidth()).add((float)rect.getHeight());
                ri = JAI.createRenderable("crop", pb);

                /* XXX
                // Embed the cropped image in an image the size of the source.
                pb = (new ParameterBlock()).addSource(ri);
                pb.add((float)rectS.getMinX()).add((float)rectS.getMinY());
                pb.add((float)rectS.getWidth()).add((float)rectS.getHeight());
                ri = JAI.createRenderable("crop", pb);
                */
            }
        }

        // Spatial orientation.
        if((opMask & MASK_TRANSFORM) != 0) {
            AffineTransform afn =
                (AffineTransform)paramBlock.getObjectParameter(6);
            try {
                // The transform parameter is a backward mapping so invert it.
                afn = afn.createInverse();
            } catch(java.awt.geom.NoninvertibleTransformException e) {
                // This should never happen due to descriptor check.
                listener.errorOccurred(JaiI18N.getString("AffineNotInvertible"),
                                       e, this, false);

            }
            pb = (new ParameterBlock()).addSource(ri).add(afn);
            if(hints != null && hints.containsKey(JAI.KEY_INTERPOLATION)) {
                pb.add(hints.get(JAI.KEY_INTERPOLATION));
            }
            ri = JAI.createRenderable("affine", pb);
        }

        // Destination rectangle of interest.
        // Set the destination rectangle of interest.
        Rectangle2D rgn = (opMask & MASK_ROI_DESTINATION) != 0 ?
            (Rectangle2D)paramBlock.getObjectParameter(8) : bounds2D;

        // Verify that the region is non-empty.
        if(rgn.isEmpty()) {
            throw new RuntimeException(JaiI18N.getString("IIPCRIF3"));
        }

        // Create a Rectangle2D for the current image.
        Rectangle2D riRect = new Rectangle2D.Float((float)ri.getMinX(),
                                                   (float)ri.getMinY(),
                                                   (float)ri.getWidth(),
                                                   (float)ri.getHeight());

        // If the current image bounds are not those of the requested
        // region then crop the image.
        if(!rgn.equals(riRect)) {
            // Intersect rgn with source image bounds.
            rgn = rgn.createIntersection(riRect);

            // Crop to the rectangle of interest.
            pb = (new ParameterBlock()).addSource(ri);
            pb.add((float)rgn.getMinX()).add((float)rgn.getMinY());
            pb.add((float)rgn.getWidth()).add((float)rgn.getHeight());
            ri = JAI.createRenderable("crop", pb);
        }

        // Return the rendering.
        return ri.createRendering(renderContext);
    }

    /**
     * Returns the default rendering of the RenderableImage produced by
     * the "iip" operation.
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        RenderableImage iipImage = JAI.createRenderable("iip", paramBlock);

        return iipImage.createDefaultRendering();
    }

    /**
     * Applies the specified set of operations to the IIP image
     * and returns a RenderedImage that satisfies the rendering context
     * provided.
     */
    public RenderedImage create(RenderContext renderContext,
                                ParameterBlock paramBlock) {
        // Get the operation mask.
        int opMask = getOperationMask(paramBlock);

        ImagingListener listener = ImageUtil.getImagingListener(renderContext);

        // Get the lowest resolution level of the IIP image for property use.
        ParameterBlock pb = new ParameterBlock();
        int[] subImageArray = (int[])paramBlock.getObjectParameter(1);
        pb.add(paramBlock.getObjectParameter(0)).add(0).add(subImageArray[0]);
        RenderedImage lowRes = JAI.create("iipresolution", pb);

        // Get the server capability mask.
        int serverMask =
            getServerCapabilityMask((String)paramBlock.getObjectParameter(0),
                                    lowRes);

        RenderedImage rendering = null;

        // Select the processing path based on the server's capabilities.
        if((serverMask & SERVER_JPEG_FULL) == SERVER_JPEG_FULL ||
           (serverMask & SERVER_FPX_FULL) == SERVER_FPX_FULL ||
           (serverMask & SERVER_JPEG_PARTIAL) == SERVER_JPEG_PARTIAL ||
           (serverMask & SERVER_FPX_PARTIAL) == SERVER_FPX_PARTIAL) {
            // All (FULL) or most (PARTIAL) ops on server
            rendering = serverProc(serverMask,
                                   renderContext, paramBlock, opMask, lowRes);
        } else {
            // All ops on client
            rendering = clientProc(renderContext, paramBlock, opMask, lowRes);

            // Do special processing if source rectangle of interest given.
            // The following approach works but is rather slow.
            if((opMask & MASK_ROI_SOURCE) != 0) {
                // Retrieve the source rectangle of interest.
                Rectangle2D rgn =
                    (Rectangle2D)paramBlock.getObjectParameter(5);

                // Retrieve a clone of the renderable transform.
                AffineTransform at = (AffineTransform)
                    ((AffineTransform)(paramBlock.getObjectParameter(6))).clone();

                // If the transform is not the identity, invert it.
                if(!at.isIdentity()) {
                    try {
                    at = at.createInverse();
                    } catch(Exception e) {
                        String message = JaiI18N.getString("IIPCRIF6");
                        listener.errorOccurred(message,
                                               new ImagingException(message, e),
                                               this, false);

//                        throw new RuntimeException(JaiI18N.getString("IIPCRIF6"));
                    }
                }

                // Compose the inverted renderable transform with the
                // renderable-to-rendered transform to get the transform
                // from source renderable coordinates to destination
                // rendered coordinates.
                at.preConcatenate(renderContext.getTransform());

                // Create an ROI in destination rendered space.
                ROIShape roi = new ROIShape(at.createTransformedShape(rgn));

                // Create a TiledImage to contain the masked result.
                TiledImage ti = new TiledImage(rendering.getMinX(),
                                               rendering.getMinY(),
                                               rendering.getWidth(),
                                               rendering.getHeight(),
                                               rendering.getTileGridXOffset(),
                                               rendering.getTileGridYOffset(),
                                               rendering.getSampleModel(),
                                               rendering.getColorModel());

                // Set the TiledImage data source to the rendering.
                ti.set(rendering, roi);

                // Create a constant-valued image for the background.
                pb = new ParameterBlock();
                pb.add((float)ti.getWidth());
                pb.add((float)ti.getHeight());
                Byte[] bandValues =
                    new Byte[ti.getSampleModel().getNumBands()];
                for(int b = 0; b < bandValues.length; b++) {
                    bandValues[b] = new Byte((byte)255);
                }
                pb.add(bandValues);

                ImageLayout il = new ImageLayout();
                il.setSampleModel(ti.getSampleModel());
                RenderingHints rh = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
                                                       il);

                PlanarImage constImage = JAI.create("constant", pb, rh);

                // Compute a complement ROI.
                ROI complementROI =
                    (new ROIShape(ti.getBounds())).subtract(roi);;

                // Fill the background.
                int maxTileY = ti.getMaxTileY();
                int maxTileX = ti.getMaxTileX();
                for(int j = ti.getMinTileY(); j <= maxTileY; j++) {
                    for(int i = ti.getMinTileX(); i <= maxTileX; i++) {
                        if(!roi.intersects(ti.getTileRect(i, j))) {
                            ti.setData(constImage.getTile(i, j),
                                       complementROI);
                        }
                    }
                }

                // Set the rendering to the TiledImage.
                rendering = ti;
            }
        }

        // If the server supports only the first tier of composed image
        // command modifiers or none at all then the "RFM" modifier
        // effect must be replicated on the client if this would be
        // required by the supplied parameters.
        if((serverMask & SERVER_JPEG_FULL) != SERVER_JPEG_FULL &&
           (serverMask & SERVER_FPX_FULL) != SERVER_FPX_FULL) {
            if((opMask & MASK_ROTATION) != 0) {
	        // NOTE: The transpose operation uses clockwise rotation
	        // whereas this operation expects counterclockwise.
                EnumeratedParameter transposeType = null;
                switch(paramBlock.getIntParameter(9)) {
                case 90:
                    transposeType = TransposeDescriptor.ROTATE_270;
                    break;
                case 180:
                    transposeType = TransposeDescriptor.ROTATE_180;
                    break;
                case 270:
                    transposeType = TransposeDescriptor.ROTATE_90;
                    break;
                }
                if(transposeType != null) { // deliberately redundant test
                    rendering =
                        JAI.create("transpose", rendering, transposeType);
                }
            }

            if((opMask & MASK_MIRROR_AXIS) != 0) {
                String axis = (String)paramBlock.getObjectParameter(10);
                EnumeratedParameter transposeType =
                    axis.equalsIgnoreCase("x") ?
                    TransposeDescriptor.FLIP_VERTICAL :
                    TransposeDescriptor.FLIP_HORIZONTAL;
                rendering = JAI.create("transpose", rendering, transposeType);
            }
        }

        return rendering;
    }

    /**
     * Returns the bounds of the RenderableImage.  This will be the
     * rendering-independent destination rectangle of interest if supplied
     * or the rendering-independent destination image bounds if not.
     */
    public Rectangle2D getBounds2D(ParameterBlock paramBlock) {
        int opMask = getOperationMask(paramBlock);

        if((opMask & MASK_ROI_DESTINATION) != 0) {
            return (Rectangle2D)paramBlock.getObjectParameter(8);
        }

        float aspectRatioDestination;
        if((opMask & MASK_ASPECT_RATIO) != 0) {
            aspectRatioDestination = paramBlock.getFloatParameter(7);
        } else {
            // Get the lowest resolution level of the IIP image.
            ParameterBlock pb = new ParameterBlock();
            int[] subImageArray = (int[])paramBlock.getObjectParameter(1);
            pb.add(paramBlock.getObjectParameter(0));
            pb.add(0).add(subImageArray[0]);
            RenderedImage lowRes = JAI.create("iipresolution", pb);

            int[] maxSize = (int[])lowRes.getProperty("max-size");

            aspectRatioDestination = (float)maxSize[0]/(float)maxSize[1];
        }

        return new Rectangle2D.Float(0.0F, 0.0F, aspectRatioDestination, 1.0F);
    }

  public static void main(String[] args) {
      int nr = 0;
      int nc = 0;

      double[][] x = matrixMultiply(RGBA_TO_YCCA, YCCA_TO_RGBA);
      nr = x.length;
      nc = x[0].length;
      for(int r = 0; r < nr; r++) {
          for(int c = 0; c < nc; c++) {
              System.out.print(x[r][c]+" ");
          }
          System.out.println("");
      }
      System.out.println("");

      x = matrixMultiply(RGB_TO_YCC, YCC_TO_RGB);
      nr = x.length;
      nc = x[0].length;
      for(int r = 0; r < nr; r++) {
          for(int c = 0; c < nc; c++) {
              System.out.print(x[r][c]+" ");
          }
          System.out.println("");
      }
      System.out.println("");

      double[][] b = new double[][] {{1.0}, {2.0}, {3.0}, {4.0}};
      double[][] A = composeMatrices(YCCA_TO_RGBA, b);
      nr = A.length;
      nc = A[0].length;
      for(int r = 0; r < nr; r++) {
          for(int c = 0; c < nc; c++) {
              System.out.print(A[r][c]+" ");
          }
          System.out.println("");
      }
      System.out.println("");

      double[][] d4 = matrixMultiply(RGBA_TO_YCCA, YCCA_TO_RGBA_CONST);
      nr = d4.length;
      nc = d4[0].length;
      for(int r = 0; r < nr; r++) {
          for(int c = 0; c < nc; c++) {
              System.out.print(-d4[r][c]+" ");
          }
          System.out.println("");
      }
      System.out.println("");

      double[][] d3 = matrixMultiply(RGB_TO_YCC, YCC_TO_RGB_CONST);
      nr = d3.length;
      nc = d3[0].length;
      for(int r = 0; r < nr; r++) {
          for(int c = 0; c < nc; c++) {
              System.out.print(-d3[r][c]+" ");
          }
          System.out.println("");
      }
      System.out.println("");
  }
}



