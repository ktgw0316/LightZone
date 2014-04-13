/*
 * $RCSfile: MediaLibAccessor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/11/23 21:08:28 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.FilePermission;
import java.lang.NoClassDefFoundError;
import java.security.AccessController;
import java.security.PrivilegedAction;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.DataBufferUtils;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.sun.medialib.mlib.*;

/**
 *  An adapter class for presenting image data in a mediaLibImage
 *  format, even if the data isn't stored that way.  MediaLibAccessor
 *  is meant to make the common case (ComponentRasters) and allow
 *  them to be accelerated via medialib.  Note that unlike RasterAccessor,
 *  MediaLibAccessor does not work with all cases.  In the event that
 *  MediaLibAccessor can not deal with a give collection of Rasters,
 *  findCompatibleTag will return the value MediaLibAccessor.TAG_INCOMPATIBLE.
 *  OpImages that use MediaLibAccessor should be paired with RIF's
 *  which check that findCompatibleTag returns a valid tag before
 *  actually constructing the Mlib OpImage.
 *
 */

public class MediaLibAccessor {
    /**
     *  Value indicating how far COPY_MASK info is shifted to avoid
     *  interfering with the data type info
     */
    private static final int COPY_MASK_SHIFT = 7;

    /* Value indicating how many bits the COPY_MASK is */
    private static final int COPY_MASK_SIZE = 1;

    /** The bits of a FormatTag associated with how dataArrays are obtained. */
    public static final int COPY_MASK = 0x1 << COPY_MASK_SHIFT;

    /** Flag indicating data is raster's data. */
    public static final int UNCOPIED = 0x0 << COPY_MASK_SHIFT;

    /** Flag indicating data is a copy of the raster's data. */
    public static final int COPIED = 0x01 << COPY_MASK_SHIFT;

    /** The bits of a FormatTag associated with pixel datatype. */
    public static final int DATATYPE_MASK = (0x1 << COPY_MASK_SHIFT) - 1;

    /**
     * Value indicating how far BINARY_MASK info is shifted to avoid
     * interfering with the data type and copying info.
     */
    private static final int BINARY_MASK_SHIFT =
        COPY_MASK_SHIFT+COPY_MASK_SIZE;

    /** Value indicating how many bits the BINARY_MASK is */
    private static final int BINARY_MASK_SIZE = 1;

    /** The bits of a FormatTag associated with binary data. */
    public static final int BINARY_MASK =
        ((1 << BINARY_MASK_SIZE) - 1) << BINARY_MASK_SHIFT;

    /** Flag indicating data are not binary. */
    public static final int NONBINARY = 0x0 << BINARY_MASK_SHIFT;

    /** Flag indicating data are binary. */
    public static final int BINARY = 0x1 << BINARY_MASK_SHIFT;

    /** FormatTag indicating data in byte arrays and uncopied. */
    public static final int
        TAG_BYTE_UNCOPIED = DataBuffer.TYPE_BYTE | UNCOPIED;

    /** FormatTag indicating data in unsigned short arrays and uncopied. */
    public static final int
        TAG_USHORT_UNCOPIED = DataBuffer.TYPE_USHORT | UNCOPIED;

    /** FormatTag indicating data in short arrays and uncopied. */
    public static final int
        TAG_SHORT_UNCOPIED = DataBuffer.TYPE_SHORT | UNCOPIED;

    /** FormatTag indicating data in integer arrays and uncopied. */
    public static final int
        TAG_INT_UNCOPIED = DataBuffer.TYPE_INT | UNCOPIED;

    /** FormatTag indicating data in float arrays and uncopied. */
    public static final int
        TAG_FLOAT_UNCOPIED = DataBuffer.TYPE_FLOAT | UNCOPIED;

    /** FormatTag indicating data in double arrays and uncopied. */
    public static final int
        TAG_DOUBLE_UNCOPIED = DataBuffer.TYPE_DOUBLE | UNCOPIED;

    /** FormatTag indicating data in byte arrays and uncopied. */
    public static final int
        TAG_BYTE_COPIED = DataBuffer.TYPE_BYTE | COPIED;

    /** FormatTag indicating data in unsigned short arrays and copied. */
    public static final int
        TAG_USHORT_COPIED = DataBuffer.TYPE_USHORT | COPIED;

    /** FormatTag indicating data in short arrays and copied. */
    public static final int
        TAG_SHORT_COPIED = DataBuffer.TYPE_SHORT | COPIED;

    /** FormatTag indicating data in short arrays and copied. */
    public static final int
        TAG_INT_COPIED = DataBuffer.TYPE_INT | COPIED;

    /** FormatTag indicating data in float arrays and copied. */
    public static final int
        TAG_FLOAT_COPIED = DataBuffer.TYPE_FLOAT | COPIED;

    /** FormatTag indicating data in double arrays and copied. */
    public static final int
        TAG_DOUBLE_COPIED = DataBuffer.TYPE_DOUBLE | COPIED;

    /** The raster that is the source of pixel data. */
    protected Raster raster;

    /** The rectangle of the raster that MediaLibAccessor addresses. */
    protected Rectangle rect;

    /** The number of bands per pixel in the data array. */
    protected int numBands;

    /** The offsets of each band in the src image */
    protected int bandOffsets[];

    /** Tag indicating the data type of the data and whether its copied */
    protected int formatTag;

    /** Area of mediaLib images that represent image data */
    protected mediaLibImage mlimages[] = null;

    /**
     * Whether packed data are preferred when processing binary images.
     * This tag is ignored if the data are not binary.
     */
    private boolean areBinaryDataPacked = false;

    private static boolean useMlibVar = false;
    private static boolean useMlibVarSet = false;

    private static synchronized boolean useMlib() {
       if (!useMlibVarSet) {
           setUseMlib();
           useMlibVarSet = true;

           System.out.println("Light Crafts JAI Library - 02/05/07");
       }

       return useMlibVar;
    }

    private static void setUseMlib() {

	// Fix of 4726600: Disable medialib before the searching of 
	// medialib library if the property is set.
        boolean disableMediaLib = false;
        try {
            disableMediaLib =
                Boolean.getBoolean("com.sun.media.jai.disableMediaLib");
        } catch (java.security.AccessControlException e) {
            // Because the property com.sun.media.jai.disableMediaLib isn't
            // defined as public, the users shouldn't know it.  In most of
            // the cases, it isn't defined, and thus no access permission
            // is granted to it in the policy file.  When JAI is utilized in
            // a security environment, AccessControlException will be thrown.
            // In this case, we suppose that the users would like to use
            // medialib accelaration.  So, the medialib won't be disabled.

            // The fix of 4531501
        }

        // If mediaLib usage has been explicity disabled.
        if (disableMediaLib) {
            useMlibVar = false;
            return;
        }

        try {

            SecurityManager securityManager =
                System.getSecurityManager();

            if (securityManager != null &&
                MediaLibAccessor.class.getClassLoader() != null) {

                // a non-null security manager means we're in an applet
                // if this.classLoader == null, we're an installed extension
                // and the doPrivleged block should be ok.

                // native code doesn't currently load on Wintel regardless
                // of where the dll's are even if this code is removed.
                // At some point we'll need to come up with a better
                // solution.  For now this is a good work around because
                // on Sparc it will cause
                // a SecurityException to be thrown instead of an
                // ExceptionInInitializerError in the doPriviliged block
                // which can't be caught.
                // If MediaLib is rewritten so that the Exception is thrown
                // after the class is loaded this chunk of code can be
                // removed.
		String osName = System.getProperty("os.name");
		String osArch = System.getProperty("os.arch");

		// The fix of 4531469
		if ((osName.equals("Solaris") || osName.equals("SunOS")) &&
		    osArch.equals("sparc")) {
		    FilePermission fp =
			new FilePermission("/usr/bin/uname","execute");
		    securityManager.checkPermission(fp);
		}
            }

            Boolean result = (Boolean)
                AccessController.doPrivileged(new PrivilegedAction() {
                     public Object run() {
                         return new Boolean(Image.isAvailable());
                     }
                });
            useMlibVar = result.booleanValue();
            if (!useMlibVar) {
                forwardToListener(JaiI18N.getString("MediaLibAccessor2"),
                                  new MediaLibLoadException());
            }
        } catch (NoClassDefFoundError ncdfe) {
            // If mediaLib jar file is not found, fall back to Java code.
            useMlibVar = false;
            forwardToListener(JaiI18N.getString("MediaLibAccessor3"), ncdfe);
        } catch (ClassFormatError cfe) {
            // If mediaLib jar file is not found, fall back to Java code.
            useMlibVar = false;
            forwardToListener(JaiI18N.getString("MediaLibAccessor3"), cfe);
        } catch (SecurityException se) {
            // If mediaLib jar file is not found, fall back to Java code.
            useMlibVar = false;
            forwardToListener(JaiI18N.getString("MediaLibAccessor4"), se);
        }

	if (useMlibVar == false)
	    return;
    }

    /**
     * Forwards the supplied message and exception to the
     * <code>ImagingListener</code> set on the default JAI instance.
     * If none is set (which should not happen) the message is simply
     * printed to <code>System.err</code>.
     */
    private static void forwardToListener(String message,
                                          Throwable thrown) {
        ImagingListener listener =
            JAI.getDefaultInstance().getImagingListener();

        if(listener != null) {
            listener.errorOccurred(message, thrown, MediaLibAccessor.class,
                                   false);
        } else {
            System.err.println(message);
        }
    }

    /**
     * Returns <code>true</code> if mediaLib is able to handle the
     * source(s) and destination image format.  Currently, all of the
     * following conditions must be met in order for this method to
     * return <code>true</code>.
     * <ul>
     * <li>MediaLib is available.</li>
     * <li>All sources must be <code>RenderedImage</code>.</li>
     * <li>All sources and destination must have
     *     <code>ComponentSampleModel</code> and
     *     <code>ComponentColorModel</code>.</li>
     * <li>All sources and destination must have less than or equal
     *     to 4 bands of pixel data.</li>
     * </ul>
     * Additional checks for each individual <code>OpImage</code>
     * should be done in its corresponding <code>RIF</code>.
     *
     * @param args  Input arguments that include sources.
     * @param layout  Destination image layout; may be <code>null</code>.
     */
    public static boolean isMediaLibCompatible(ParameterBlock args,
                                               ImageLayout layout) {
        if (!isMediaLibCompatible(args)) {
            // sources not supported
            return false;
        }

        if (layout != null) {	// validate destination
            SampleModel sm = layout.getSampleModel(null);
            if (sm != null) {
                if (!(sm instanceof ComponentSampleModel) ||
                    sm.getNumBands() > 4) {
                    return false;
                }
            }

            ColorModel cm = layout.getColorModel(null);
            if (cm != null && (!(cm instanceof ComponentColorModel))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns <code>true</code> if mediaLib is able to handle the
     * source(s) image format.  Currently, all of the following
     * conditions must be met in order for this method to return
     * <code>true</code>.
     * <ul>
     * <li>MediaLib is available.</li>
     * <li>All sources must be <code>RenderedImage</code>.</li>
     * <li>All sources must have <code>ComponentSampleModel</code> and
     *     <code>ComponentColorModel</code>.</li>
     * <li>All sources must have less than or equal to 4 bands of pixel
     *     data.</li>
     * </ul>
     * Additional checks for each individual <code>OpImage</code>
     * should be done in its corresponding <code>RIF</code>.
     *
     * @param args  Input arguments that include sources.
     */
    public static boolean isMediaLibCompatible(ParameterBlock args) {
        if (!useMlib()) {		// mediaLib is not available
            return false;
        }

        int numSrcs = args.getNumSources();
        for (int i = 0; i < numSrcs; i++) {
            Object src = args.getSource(i);
            if (!(src instanceof RenderedImage) ||
                !isMediaLibCompatible((RenderedImage)src)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns <code>true</code> if mediaLib is able to handle the
     * image.  Currently, all of the following conditions must be
     * met in order for this method to return <code>true</code>.
     * <ul>
     * <li>MediaLib is available.</li>
     * <li>The image must have <code>ComponentSampleModel</code> and
     *     <code>ComponentColorModel</code>.</li>
     * <li>The image must have less than or equal to 4 bands of pixel
     *     data.</li>
     * </ul>
     * Additional checks for each individual <code>OpImage</code>
     * should be done in its corresponding <code>RIF</code>.
     *
     * @param image The image the compatibility of which is to be checked.
     */
    public static boolean isMediaLibCompatible(RenderedImage image) {
        if (!useMlib()) {		// mediaLib is not available
            return false;
        }

        SampleModel sm = image.getSampleModel();
        ColorModel cm = image.getColorModel();

        return (sm instanceof ComponentSampleModel &&
                sm.getNumBands() <= 4 &&
                (cm == null || cm instanceof ComponentColorModel));
    }

    /**
     * Returns <code>true</code> if mediaLib is able to handle
     * an image having the supplied <code>SampleModel</code> and
     * <code>ColorModel</code>.  Currently, all of the following conditions
     * must be met in order for this method to return <code>true</code>:
     * <ul>
     * <li>mediaLib is available.</li>
     * <li>The <code>SampleModel</code> is an instance of
     * <code>ComponentSampleModel</code> or one of its subclasses.</li>
     * <li>The <code>ColorModel</code> is <code>null</code> or an
     * instance of <code>ComponentColorModel</code> or a subclass thereof.</li>
     * <li>The image must have no more than 4 bands of pixel data.</li>
     * </ul>
     *
     * @param sm The image <code>SampleModel</code>.
     * @param cm The image <code>ColorModel</code>.
     *
     * @throws NullPointerException if <code>sm</code> is <code>null</code>.
     */
    public static boolean isMediaLibCompatible(SampleModel sm,
                                               ColorModel cm) {
        if (!useMlib()) {		// mediaLib is not available
            return false;
        }

        return (sm instanceof ComponentSampleModel &&
                sm.getNumBands() <= 4 &&
                (cm == null || cm instanceof ComponentColorModel));
    }

    /**
     * Returns <code>true</code> if mediaLib is able to handle the
     * source(s) and destination image format as binary (also known
     * as bit or bilevel) image data.  Currently, all of the
     * following conditions must be met in order for this method to
     * return <code>true</code>.
     * <ul>
     * <li>MediaLib is available.</li>
     * <li>All sources must be <code>RenderedImage</code>s.</li>
     * <li>All sources and destination must have a
     *     <code>MultiPixelPackedSampleModel</code>.</li>
     * <li>All sources and destination must have represent
     *     single-bit data.</li>
     * <li>All sources and destination must have
     *     a single band of pixel data.</li>
     * </ul>
     * Additional checks for each individual <code>OpImage</code>
     * should be done in its corresponding <code>RIF</code>.
     *
     * @param args  Input arguments that include sources.
     * @param layout  Destination image layout; may be <code>null</code>.
     */
    public static boolean isMediaLibBinaryCompatible(ParameterBlock args,
                                                     ImageLayout layout) {
        if (!useMlib()) {		// mediaLib is not available
            return false;
        }

        SampleModel sm = null;

        int numSrcs = args.getNumSources();
        for (int i = 0; i < numSrcs; i++) { 	// sources not supported
            Object src = args.getSource(i);
            if (!(src instanceof RenderedImage) ||
                (sm = ((RenderedImage)src).getSampleModel()) == null ||
                !ImageUtil.isBinary(sm)) {
                return false;
            }
        }

        if (layout != null) {	// validate destination
            if ((sm = layout.getSampleModel(null)) != null &&
                !ImageUtil.isBinary(sm)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns <code>true</code> if the number of bands of all the
     * <code>RenderedImage</code> sources and destination are the same.
     *
     * @throws ClassCastException  if any source is not
     *         <code>RenderedImage</code>.
     */
    public static boolean hasSameNumBands(ParameterBlock args,
                                          ImageLayout layout) {
        int numSrcs = args.getNumSources();

        if (numSrcs > 0) {
            RenderedImage src = args.getRenderedSource(0);
            int numBands = src.getSampleModel().getNumBands();

            for (int i = 1; i < numSrcs; i++) {
                src = args.getRenderedSource(i);
                if (src.getSampleModel().getNumBands() != numBands) {
                    return false;
                }
            }

            if (layout != null) {
                SampleModel sm = layout.getSampleModel(null);
                if (sm != null && sm.getNumBands() != numBands) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     *  Returns the most efficient FormatTag that is compatible with
     *  the destination raster and all source rasters.
     *
     *  @param srcs the source <code>Raster</code>; may be <code>null</code>.
     *  @param dst  the destination <code>Raster</code>.
     */
    public static int findCompatibleTag(Raster srcs[], Raster dst) {
        SampleModel dstSM = dst.getSampleModel();
        int dstDT = dstSM.getDataType();

        int defaultDataType = dstSM.getDataType();

        boolean allComponentSampleModel =
             dstSM instanceof ComponentSampleModel;
        boolean allBinary = ImageUtil.isBinary(dstSM);

        // use highest precision datatype of all srcs & dst
        if (srcs != null) {
            int numSources = srcs.length;
            int i;
            for (i = 0; i < numSources; i++) {
                SampleModel srcSampleModel = srcs[i].getSampleModel();
                if (!(srcSampleModel instanceof ComponentSampleModel)) {
                    allComponentSampleModel = false;
                }
                if (!ImageUtil.isBinary(srcSampleModel)) {
                    allBinary = false;
                }
                int srcDataType = srcSampleModel.getTransferType();
                if (srcDataType > defaultDataType) {
                    defaultDataType = srcDataType;
                }
            }
        }

        if(allBinary) {
            // The copy flag is not set until the mediaLibImage is
            // created as knowing this information requires too much
            // processing to determine here.
            return DataBuffer.TYPE_BYTE | BINARY;
        }

        if (!allComponentSampleModel) {
            if ((defaultDataType == DataBuffer.TYPE_BYTE) ||
                (defaultDataType == DataBuffer.TYPE_USHORT) ||
                (defaultDataType == DataBuffer.TYPE_SHORT)) {
                defaultDataType = DataBuffer.TYPE_INT;
            }
        }

        int tag = defaultDataType | COPIED;

        if (!allComponentSampleModel) {
            return tag;
        }

        //  see if they all have same DT and are pixelSequential

        SampleModel srcSM[];
        if (srcs == null) {
            srcSM = new SampleModel[0];
        } else {
            srcSM = new SampleModel[srcs.length];
        }
        for (int i = 0; i < srcSM.length; i++) {
            srcSM[i] = srcs[i].getSampleModel();
            if (dstDT != srcSM[i].getDataType()) {
                return tag;
            }
        }
        if (isPixelSequential(dstSM)) {
            for (int i = 0; i < srcSM.length; i++) {
                if (!isPixelSequential(srcSM[i])) {
                    return tag;
                }
            }
            for (int i = 0; i < srcSM.length; i++) {
                if (!hasMatchingBandOffsets((ComponentSampleModel)dstSM,
                                            (ComponentSampleModel)srcSM[i])) {
                    return tag;
                }
            }
            return dstDT | UNCOPIED;
        }
        return tag;
    }

    /**
     *  Determines if the SampleModel stores data in a way that can
     *  be represented by a mediaLibImage without copying
     */
    public static boolean isPixelSequential(SampleModel sm) {
        ComponentSampleModel csm = null;
        if (sm instanceof ComponentSampleModel) {
            csm = (ComponentSampleModel)sm;
        } else {
            return false;
        }
        int pixelStride = csm.getPixelStride();
        int bandOffsets[] = csm.getBandOffsets();
        int bankIndices[] = csm.getBankIndices();
        if (pixelStride != bandOffsets.length) {
            return false;
        }
        for (int i = 0; i < bandOffsets.length; i++) {
            if (bandOffsets[i] >= pixelStride ||
                bankIndices[i] != bankIndices[0]) {
                return false;
            }
            for (int j = i+1; j < bandOffsets.length; j++) {
               if (bandOffsets[i] == bandOffsets[j]) {
                   return false;
               }
            }
        }
        return true;
    }

    /**
     *  Determines if the src ComponentSampleModel and dst
     *  ComponentSampleModel have matching band offsets.  If they
     *  don't mediaLib can't deal with the image without a copy.
     */
    public static boolean hasMatchingBandOffsets(ComponentSampleModel dst,
                                                 ComponentSampleModel src) {
       int srcBandOffsets[] = dst.getBandOffsets();
       int dstBandOffsets[] = src.getBandOffsets();
       if (srcBandOffsets.length != dstBandOffsets.length) {
           return false;
       }
       for (int i = 0; i < srcBandOffsets.length; i++) {
           if (srcBandOffsets[i] != dstBandOffsets[i]) {
               return false;
           }
       }
       return true;
    }

    public static int getMediaLibDataType(int formatTag) {
        int dataType = formatTag & DATATYPE_MASK;
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                return Constants.MLIB_BYTE;
            case DataBuffer.TYPE_USHORT:
                return Constants.MLIB_USHORT;
            case DataBuffer.TYPE_SHORT:
                return Constants.MLIB_SHORT;
            case DataBuffer.TYPE_INT:
                return Constants.MLIB_INT;
            case DataBuffer.TYPE_DOUBLE:
                return Constants.MLIB_DOUBLE;
            case DataBuffer.TYPE_FLOAT:
                return Constants.MLIB_FLOAT;
        }
        return -1;
    }

    /**
     *  Constructs a MediaLibAccessor object out of a Raster, Rectangle
     *  and formatTag returned from MediaLibAccessor.findCompatibleTag().
     *
     *  In the case of binary data the copy mask bits of the formatTag
     *  will be reset within the constructor according to whether the
     *  data are in fact copied. This cannot be easily determined before
     *  the data are actually copied.
     */
    public MediaLibAccessor(Raster raster, Rectangle rect, int formatTag,
                            boolean preferPacked) {
        areBinaryDataPacked = preferPacked;

        this.raster = raster;
        this.rect = new Rectangle(rect);
        this.formatTag = formatTag;

        if(isBinary()) {
            // Set binary-specific fields and return.
            numBands = 1;
            bandOffsets = new int[] {0};

            int mlibType;
            int scanlineStride;
            byte[] bdata;
            mlimages = new mediaLibImage[1];

            if(areBinaryDataPacked) {
                mlibType = Constants.MLIB_BIT;
                scanlineStride = (rect.width+7)/8;
                bdata = ImageUtil.getPackedBinaryData(raster, rect);

                // Update format tag depending on whether the data were copied.
                if(bdata ==
                   ((DataBufferByte)raster.getDataBuffer()).getData()) {
                    this.formatTag |= UNCOPIED;
                } else {
                    this.formatTag |= COPIED;
                }
            } else { // unpacked
                mlibType = Constants.MLIB_BYTE;
                scanlineStride = rect.width;
                bdata = ImageUtil.getUnpackedBinaryData(raster, rect);
                this.formatTag |= COPIED;
            }

            mlimages[0] = new mediaLibImage(mlibType,
                                            1,
                                            rect.width,
                                            rect.height,
                                            scanlineStride,
                                            0,
                                            bdata);

            return;
        }

        if ((formatTag & COPY_MASK) == UNCOPIED) {
            ComponentSampleModel csm =
                (ComponentSampleModel)raster.getSampleModel();

            numBands = csm.getNumBands();
            bandOffsets = csm.getBandOffsets();
            int dataOffset = raster.getDataBuffer().getOffset();
            dataOffset +=
             (rect.y-raster.getSampleModelTranslateY())*csm.getScanlineStride()+
             (rect.x-raster.getSampleModelTranslateX())*csm.getPixelStride();

            // dataoffset should and is in terms of dataElements

            // scanline stride should be in terms of dataElements
            int scanlineStride = csm.getScanlineStride();

            switch (formatTag & DATATYPE_MASK) {
            case DataBuffer.TYPE_BYTE:
                DataBufferByte dbb = (DataBufferByte)raster.getDataBuffer();
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_BYTE,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      dataOffset,
                                      dbb.getData());
                break;

            case DataBuffer.TYPE_USHORT:
                DataBufferUShort dbus =
                    (DataBufferUShort)raster.getDataBuffer();
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_USHORT,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      dataOffset,
                                      dbus.getData());
                break;
            case DataBuffer.TYPE_SHORT:
                DataBufferShort dbs = (DataBufferShort)raster.getDataBuffer();
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_SHORT,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      dataOffset,
                                      dbs.getData());
                break;
            case DataBuffer.TYPE_INT:
                DataBufferInt dbi = (DataBufferInt)raster.getDataBuffer();
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_INT,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      dataOffset,
                                      dbi.getData());
                break;
            case DataBuffer.TYPE_FLOAT:
                DataBuffer dbf = raster.getDataBuffer();
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_FLOAT,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      dataOffset,
                                      DataBufferUtils.getDataFloat(dbf));
                break;
            case DataBuffer.TYPE_DOUBLE:
                DataBuffer dbd = raster.getDataBuffer();
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_DOUBLE,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      dataOffset,
                                      DataBufferUtils.getDataDouble(dbd));
                break;
            default:
                throw new IllegalArgumentException((formatTag & DATATYPE_MASK) +JaiI18N.getString("MediaLibAccessor1"));
            }
        } else {
            // Copying the data because we can't deal with it
            numBands = raster.getNumBands();
            bandOffsets = new int[numBands];
            for (int i = 0; i < numBands; i++) {
                bandOffsets[i] = i;
            }
            int scanlineStride = rect.width*numBands;

            switch (formatTag & DATATYPE_MASK) {
            case DataBuffer.TYPE_BYTE:
                byte bdata[] = new byte[rect.width*rect.height*numBands];
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_BYTE,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      0,
                                      bdata);
                break;
            case DataBuffer.TYPE_USHORT:
                short usdata[] = new short[rect.width*rect.height*numBands];
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_USHORT,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      0,
                                      usdata);
                break;
            case DataBuffer.TYPE_SHORT:
                short sdata[] = new short[rect.width*rect.height*numBands];
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_SHORT,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      0,
                                      sdata);
                break;
            case DataBuffer.TYPE_INT:
                int idata[] = new int[rect.width*rect.height*numBands];
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_INT,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      0,
                                      idata);
                break;
            case DataBuffer.TYPE_FLOAT:
                float fdata[] = new float[rect.width*rect.height*numBands];
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_FLOAT,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      0,
                                      fdata);
                break;
            case DataBuffer.TYPE_DOUBLE:
                double ddata[] = new double[rect.width*rect.height*numBands];
                mlimages = new mediaLibImage[1];
                mlimages[0] =
                    new mediaLibImage(Constants.MLIB_DOUBLE,
                                      numBands,
                                      rect.width,
                                      rect.height,
                                      scanlineStride,
                                      0,
                                      ddata);
                break;
            default:
                throw new IllegalArgumentException((formatTag & DATATYPE_MASK) + JaiI18N.getString("MediaLibAccessor1"));
            }
            copyDataFromRaster();
        }
    }

    /**
     *  Constructs a MediaLibAccessor object out of a Raster, Rectangle
     *  and formatTag returned from MediaLibAccessor.findCompatibleTag().
     */
    public MediaLibAccessor(Raster raster, Rectangle rect, int formatTag) {
        this(raster, rect, formatTag, false);
    }

    /**
     * Returns <code>true</code> if the <code>MediaLibAccessor</code>
     * represents binary data.
     */
    public boolean isBinary() {
        return ((formatTag & BINARY_MASK) == BINARY);
    }

    /**
     *  Returns an array of mediaLibImages which represents the input raster.
     *  An array is returned instead of a single mediaLibImage because
     *  in some cases, an input Raster can't be represented by one
     *  mediaLibImage (unless copying is done) but can be represented
     *  by several mediaLibImages without copying.
     */
    public mediaLibImage[] getMediaLibImages() {
        return mlimages;
    }

    /**
     *  Returns the data type of the RasterAccessor object. Note that
     *  this datatype is not necessarily the same data type as the
     *  underlying raster.
     */
    public int getDataType() {
        return formatTag & DATATYPE_MASK;
    }

    /**
     *  Returns true if the MediaLibAccessors's data is copied from it's
     *  raster.
     */
    public boolean isDataCopy() {
        return ((formatTag & COPY_MASK) == COPIED);
    }

    /** Returns the bandOffsets. */
    public int[] getBandOffsets() {
        return bandOffsets;
    }

    /**
     *  Returns parameters in the appropriate order if MediaLibAccessor
     *  has reordered the bands or is attempting to make a
     *  BandSequential image look like multiple PixelSequentialImages
     */
    public int[] getIntParameters(int band, int params[]) {
        int returnParams[] = new int[numBands];
        for (int i = 0; i < numBands; i++) {
            returnParams[i] = params[bandOffsets[i+band]];
        }
        return returnParams;
    }

    /**
     *  Returns parameters in the appropriate order if MediaLibAccessor
     *  has reordered the bands or is attempting to make a
     *  BandSequential image look like multiple PixelSequentialImages
     */
    public int[][] getIntArrayParameters(int band, int[][] params) {
        int returnParams[][] = new int[numBands][];
        for (int i = 0; i < numBands; i++) {
            returnParams[i] = params[bandOffsets[i+band]];
        }
        return returnParams;
    }

    /**
     *  Returns parameters in the appropriate order if MediaLibAccessor
     *  has reordered the bands or is attempting to make a
     *  BandSequential image look like multiple PixelSequentialImages
     */
    public double[] getDoubleParameters(int band, double params[]) {
        double returnParams[] = new double[numBands];
        for (int i = 0; i < numBands; i++) {
            returnParams[i] = params[bandOffsets[i+band]];
        }
        return returnParams;
    }


    /**
     *  Copy data from Raster to MediaLib image
     */
    private void copyDataFromRaster() {
        // Writeback should only be necessary on destRasters which
        // should be writable so this cast should succeed.

        if (raster.getSampleModel() instanceof ComponentSampleModel) {
            ComponentSampleModel csm =
               (ComponentSampleModel)raster.getSampleModel();
            int rasScanlineStride = csm.getScanlineStride();
            int rasPixelStride = csm.getPixelStride();

            int subRasterOffset =
             (rect.y-raster.getSampleModelTranslateY())*rasScanlineStride+
             (rect.x-raster.getSampleModelTranslateX())*rasPixelStride;

            int rasBankIndices[] = csm.getBankIndices();
            int rasBandOffsets[] = csm.getBandOffsets();
            int rasDataOffsets[] = raster.getDataBuffer().getOffsets();

            if (rasDataOffsets.length == 1) {
                for (int i = 0; i < numBands; i++) {
                    rasBandOffsets[i] += rasDataOffsets[0] +
                       subRasterOffset;
                }
            } else if (rasDataOffsets.length == rasBandOffsets.length) {
                for (int i = 0; i < numBands; i++) {
                    rasBandOffsets[i] += rasDataOffsets[i] +
                        subRasterOffset;
                }
            }

            Object mlibDataArray = null;
            switch (getDataType()) {
            case DataBuffer.TYPE_BYTE:
                byte bArray[][] = new byte[numBands][];
                for (int i = 0; i < numBands; i++) {
                    bArray[i] = mlimages[0].getByteData();
                }
                mlibDataArray = bArray;
                break;
            case DataBuffer.TYPE_USHORT:
                short usArray[][] = new short[numBands][];
                for (int i = 0; i < numBands; i++) {
                    usArray[i] = mlimages[0].getUShortData();
                }
                mlibDataArray = usArray;
                break;
            case DataBuffer.TYPE_SHORT:
                short sArray[][] = new short[numBands][];
                for (int i = 0; i < numBands; i++) {
                    sArray[i] = mlimages[0].getShortData();
                }
                mlibDataArray = sArray;
                break;
            case DataBuffer.TYPE_INT:
                int iArray[][] = new int[numBands][];
                for (int i = 0; i < numBands; i++) {
                    iArray[i] = mlimages[0].getIntData();
                }
                mlibDataArray = iArray;
                break;
            case DataBuffer.TYPE_FLOAT:
                float fArray[][] = new float[numBands][];
                for (int i = 0; i < numBands; i++) {
                    fArray[i] = mlimages[0].getFloatData();
                }
                mlibDataArray = fArray;
                break;
            case DataBuffer.TYPE_DOUBLE:
                double dArray[][] = new double[numBands][];
                for (int i = 0; i < numBands; i++) {
                    dArray[i] = mlimages[0].getDoubleData();
                }
                mlibDataArray = dArray;
                break;
            }



            Object rasDataArray = null;
            switch (csm.getDataType()) {
                case DataBuffer.TYPE_BYTE: {
                    DataBufferByte dbb =
                        (DataBufferByte)raster.getDataBuffer();
                    byte rasByteDataArray[][] = new byte[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        rasByteDataArray[i] =
                            dbb.getData(rasBankIndices[i]);
                    }
                    rasDataArray = rasByteDataArray;
                    }
                    break;
                case DataBuffer.TYPE_USHORT: {
                    DataBufferUShort dbus =
                        (DataBufferUShort)raster.getDataBuffer();
                    short rasUShortDataArray[][] = new short[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        rasUShortDataArray[i] =
                            dbus.getData(rasBankIndices[i]);
                    }
                    rasDataArray = rasUShortDataArray;
                    }
                    break;
                case DataBuffer.TYPE_SHORT: {
                    DataBufferShort dbs =
                        (DataBufferShort)raster.getDataBuffer();
                    short rasShortDataArray[][] = new short[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        rasShortDataArray[i] =
                            dbs.getData(rasBankIndices[i]);
                    }
                    rasDataArray = rasShortDataArray;
                    }
                    break;
                case DataBuffer.TYPE_INT: {
                    DataBufferInt dbi =
                        (DataBufferInt)raster.getDataBuffer();
                    int rasIntDataArray[][] = new int[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        rasIntDataArray[i] =
                            dbi.getData(rasBankIndices[i]);
                    }
                    rasDataArray = rasIntDataArray;
                    }
                    break;
                case DataBuffer.TYPE_FLOAT: {
                    DataBuffer dbf =
                        raster.getDataBuffer();
                    float rasFloatDataArray[][] = new float[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        rasFloatDataArray[i] =
                            DataBufferUtils.getDataFloat(dbf, rasBankIndices[i]);
                    }
                    rasDataArray = rasFloatDataArray;
                    }
                    break;
                case DataBuffer.TYPE_DOUBLE: {
                    DataBuffer dbd =
                        raster.getDataBuffer();
                    double rasDoubleDataArray[][] = new double[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        rasDoubleDataArray[i] =
                            DataBufferUtils.getDataDouble(dbd, rasBankIndices[i]);
                    }
                    rasDataArray = rasDoubleDataArray;
                    }
                    break;
            }


            // dst = mlib && src = ras
            Image.Reformat(
                    mlibDataArray,
                    rasDataArray,
                    numBands,
                    rect.width,rect.height,
                    getMediaLibDataType(this.getDataType()),
                    bandOffsets,
                    rect.width*numBands,
                    numBands,
                    getMediaLibDataType(csm.getDataType()),
                    rasBandOffsets,
                    rasScanlineStride,
                    rasPixelStride);
        } else {
            // If COPIED and the raster doesn't have ComponentSampleModel
            // data is moved with getPixel/setPixel (even byte/short)
            switch (getDataType()) {
            case DataBuffer.TYPE_INT:
                raster.getPixels(rect.x,rect.y,
                                 rect.width,rect.height,
                                 mlimages[0].getIntData());
                break;
            case DataBuffer.TYPE_FLOAT:
                raster.getPixels(rect.x,rect.y,
                                 rect.width,rect.height,
                                 mlimages[0].getFloatData());
                break;
            case DataBuffer.TYPE_DOUBLE:
                raster.getPixels(rect.x,rect.y,
                                 rect.width,rect.height,
                                 mlimages[0].getDoubleData());
                break;
            }
        }
    }


    /**
     *  Copies data back into the MediaLibAccessor's raster.  Note that
     *  the data is casted from the intermediate data format to
     *  the raster's format.  If clamping is needed, the call
     *  clampDataArrays() method needs to be called before
     *  calling the copyDataToRaster() method.
     */
    public void copyDataToRaster() {
        if (isDataCopy()) {

            if(isBinary()) {
                if(areBinaryDataPacked) {
                    ImageUtil.setPackedBinaryData(mlimages[0].getBitData(),
                                                  (WritableRaster)raster,
                                                  rect);
                } else { // unpacked
                    ImageUtil.setUnpackedBinaryData(mlimages[0].getByteData(),
                                                    (WritableRaster)raster,
                                                    rect);
                }
                return;
            }

            // Writeback should only be necessary on destRasters which
            // should be writable so this cast should succeed.
            WritableRaster wr = (WritableRaster)raster;

            if (wr.getSampleModel() instanceof ComponentSampleModel) {
                ComponentSampleModel csm =
                   (ComponentSampleModel)wr.getSampleModel();
                int rasScanlineStride = csm.getScanlineStride();
                int rasPixelStride = csm.getPixelStride();

                int subRasterOffset =
                 (rect.y-raster.getSampleModelTranslateY())*rasScanlineStride+
                 (rect.x-raster.getSampleModelTranslateX())*rasPixelStride;

                int rasBankIndices[] = csm.getBankIndices();
                int rasBandOffsets[] = csm.getBandOffsets();
                int rasDataOffsets[] = raster.getDataBuffer().getOffsets();

                if (rasDataOffsets.length == 1) {
                    for (int i = 0; i < numBands; i++) {
                        rasBandOffsets[i] += rasDataOffsets[0] +
                           subRasterOffset;
                    }
                } else if (rasDataOffsets.length == rasBandOffsets.length) {
                    for (int i = 0; i < numBands; i++) {
                        rasBandOffsets[i] += rasDataOffsets[i] +
                            subRasterOffset;
                    }
                }

                Object mlibDataArray = null;
                switch (getDataType()) {
                case DataBuffer.TYPE_BYTE:
                    byte bArray[][] = new byte[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        bArray[i] = mlimages[0].getByteData();
                    }
                    mlibDataArray = bArray;
                    break;
                case DataBuffer.TYPE_USHORT:
                    short usArray[][] = new short[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        usArray[i] = mlimages[0].getUShortData();
                    }
                    mlibDataArray = usArray;
                    break;
                case DataBuffer.TYPE_SHORT:
                    short sArray[][] = new short[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        sArray[i] = mlimages[0].getShortData();
                    }
                    mlibDataArray = sArray;
                    break;
                case DataBuffer.TYPE_INT:
                    int iArray[][] = new int[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        iArray[i] = mlimages[0].getIntData();
                    }
                    mlibDataArray = iArray;
                    break;
                case DataBuffer.TYPE_FLOAT:
                    float fArray[][] = new float[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        fArray[i] = mlimages[0].getFloatData();
                    }
                    mlibDataArray = fArray;
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    double dArray[][] = new double[numBands][];
                    for (int i = 0; i < numBands; i++) {
                        dArray[i] = mlimages[0].getDoubleData();
                    }
                    mlibDataArray = dArray;
                    break;
                }


		byte tmpDataArray[] = null;
                Object rasDataArray = null;
                switch (csm.getDataType()) {
                    case DataBuffer.TYPE_BYTE: {
                        DataBufferByte dbb =
                            (DataBufferByte)raster.getDataBuffer();
                        byte rasByteDataArray[][] = new byte[numBands][];
                        for (int i = 0; i < numBands; i++) {
                            rasByteDataArray[i] =
                                dbb.getData(rasBankIndices[i]);
                        }
			tmpDataArray =  rasByteDataArray[0];
                        rasDataArray = rasByteDataArray;
                        }
                        break;
                    case DataBuffer.TYPE_USHORT: {
                        DataBufferUShort dbus =
                            (DataBufferUShort)raster.getDataBuffer();
                        short rasUShortDataArray[][] = new short[numBands][];
                        for (int i = 0; i < numBands; i++) {
                            rasUShortDataArray[i] =
                                dbus.getData(rasBankIndices[i]);
                        }
                        rasDataArray = rasUShortDataArray;
                        }
                        break;
                    case DataBuffer.TYPE_SHORT: {
                        DataBufferShort dbs =
                            (DataBufferShort)raster.getDataBuffer();
                        short rasShortDataArray[][] = new short[numBands][];
                        for (int i = 0; i < numBands; i++) {
                            rasShortDataArray[i] =
                                dbs.getData(rasBankIndices[i]);
                        }
                        rasDataArray = rasShortDataArray;
                        }
                        break;
                    case DataBuffer.TYPE_INT: {
                        DataBufferInt dbi =
                            (DataBufferInt)raster.getDataBuffer();
                        int rasIntDataArray[][] = new int[numBands][];
                        for (int i = 0; i < numBands; i++) {
                            rasIntDataArray[i] =
                                dbi.getData(rasBankIndices[i]);
                        }
                        rasDataArray = rasIntDataArray;
                        }
                        break;
                    case DataBuffer.TYPE_FLOAT: {
                        DataBuffer dbf =
                            raster.getDataBuffer();
                        float rasFloatDataArray[][] = new float[numBands][];
                        for (int i = 0; i < numBands; i++) {
                            rasFloatDataArray[i] =
                                DataBufferUtils.getDataFloat(dbf, rasBankIndices[i]);
                        }
                        rasDataArray = rasFloatDataArray;
                        }
                        break;
                    case DataBuffer.TYPE_DOUBLE: {
                        DataBuffer dbd =
                            raster.getDataBuffer();
                        double rasDoubleDataArray[][] = new double[numBands][];
                        for (int i = 0; i < numBands; i++) {
                            rasDoubleDataArray[i] =
                                DataBufferUtils.getDataDouble(dbd, rasBankIndices[i]);
                        }
                        rasDataArray = rasDoubleDataArray;
                        }
                        break;
                }


                // src = mlib && dst = ras
                Image.Reformat(
                        rasDataArray,
                        mlibDataArray,
                        numBands,
                        rect.width,rect.height,
                        getMediaLibDataType(csm.getDataType()),
                        rasBandOffsets,
                        rasScanlineStride,
                        rasPixelStride,
                        getMediaLibDataType(this.getDataType()),
                        bandOffsets,
                        rect.width*numBands,
                        numBands);
            } else {
                // If COPIED and the raster doesn't have ComponentSampleModel
                // data is moved with getPixel/setPixel (even byte/short)
                switch (getDataType()) {
                case DataBuffer.TYPE_INT:
                    wr.setPixels(rect.x,rect.y,
                                 rect.width,rect.height,
                                 mlimages[0].getIntData());
                    break;
                case DataBuffer.TYPE_FLOAT:
                    wr.setPixels(rect.x,rect.y,
                                 rect.width,rect.height,
                                 mlimages[0].getFloatData());
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    wr.setPixels(rect.x,rect.y,
                                 rect.width,rect.height,
                                 mlimages[0].getDoubleData());
                    break;
                }
            }
        }
    }

    /**
     * Clamps data array values to a range that the underlying raster
     * can deal with.  For example, if the underlying raster stores
     * data as bytes, but the samples ares unpacked into integer arrays by
     * the RasterAccessor object for an operation, the operation will
     * need to call clampDataArrays() so that the data in the int
     * arrays is restricted to the range 0..255 before a setPixels()
     * call is made on the underlying raster.  Note that some
     * operations (for example, lookup) can guarantee that their
     * results don't need clamping so they can call
     * RasterAccessor.copyDataToRaster() without first calling this
     * function.
     */
    public void clampDataArrays () {
        if (!isDataCopy()) {
            return;
        }

        // additonal medialib check:  If it's a componentSampleModel
        // we get a free cast when we call medialibWrapper.Reformat
        // to copy the data to the source.  So we don't need to cast
        // here.
        if (raster.getSampleModel() instanceof ComponentSampleModel) {
            return;
        }

        int bits[] = raster.getSampleModel().getSampleSize();

        // Do we even need a clamp?  We do if there's any band
        // of the source image stored in that's less than 32 bits
        // and is stored in a byte, short or int format.  (The automatic
        // cast's between floats/doubles and 32-bit ints in setPixel()
        // generall do what we want.)

        boolean needClamp = false;
        boolean uniformBitSize = true;
        for (int i = 0; i < bits.length; i++) {
            int bitSize = bits[0];
            if (bits[i] < 32) {
                needClamp = true;
            }
            if (bits[i] != bitSize) {
               uniformBitSize = false;
            }
        }

        if (!needClamp) {
            return;
        }

        int dataType = raster.getDataBuffer().getDataType();
        double hiVals[] = new double[bits.length];
        double loVals[] = new double[bits.length];

        if (dataType == DataBuffer.TYPE_USHORT &&
            uniformBitSize && bits[0] == 16) {
            for (int i = 0; i < bits.length; i++) {
                hiVals[i] = (double)0xFFFF;
                loVals[i] = (double)0;
            }
        } else if (dataType == DataBuffer.TYPE_SHORT &&
            uniformBitSize && bits[0] == 16) {
            for (int i = 0; i < bits.length; i++) {
                hiVals[i] = (double)Short.MAX_VALUE;
                loVals[i] = (double)Short.MIN_VALUE;
            }
        } else if (dataType == DataBuffer.TYPE_INT &&
            uniformBitSize && bits[0] == 32) {
            for (int i = 0; i < bits.length; i++) {
                hiVals[i] = (double)Integer.MAX_VALUE;
                loVals[i] = (double)Integer.MIN_VALUE;
            }
        } else {
            for (int i = 0; i < bits.length; i++) {
                hiVals[i] = (double)((1 << bits[i]) - 1);
                loVals[i] = (double)0;
            }
        }
        clampDataArray(hiVals,loVals);
    }

    private void clampDataArray(double hiVals[], double loVals[]) {
        switch (getDataType()) {
        case DataBuffer.TYPE_INT:
            clampIntArrays(toIntArray(hiVals),toIntArray(loVals));
            break;
        case DataBuffer.TYPE_FLOAT:
            clampFloatArrays(toFloatArray(hiVals),toFloatArray(loVals));
            break;
        case DataBuffer.TYPE_DOUBLE:
            clampDoubleArrays(hiVals,loVals);
            break;
        }
    }

    private int[] toIntArray(double vals[]) {
        int returnVals[] = new int[vals.length];
        for (int i = 0; i < vals.length; i++) {
            returnVals[i] = (int)vals[i];
        }
        return returnVals;
    }

    private float[] toFloatArray(double vals[]) {
        float returnVals[] = new float[vals.length];
        for (int i = 0; i < vals.length; i++) {
            returnVals[i] = (float)vals[i];
        }
        return returnVals;
    }

    private void clampIntArrays(int hiVals[], int loVals[]) {
        int width = rect.width;
        int height = rect.height;
        int scanlineStride = numBands*width;
        for (int k = 0; k < numBands; k++)  {
            int data[] = mlimages[0].getIntData();
            int scanlineOffset = k;
            int hiVal = hiVals[k];
            int loVal = loVals[k];
            for (int j = 0; j < height; j++)  {
                int pixelOffset = scanlineOffset;
                for (int i = 0; i < width; i++)  {
                    int tmp = data[pixelOffset];
                    if (tmp < loVal) {
                        data[pixelOffset] = loVal;
                    } else if (tmp > hiVal) {
                        data[pixelOffset] = hiVal;
                    }
                    pixelOffset += numBands;
                }
                scanlineOffset += scanlineStride;
            }
        }
    }

    private void clampFloatArrays(float hiVals[], float loVals[]) {
        int width = rect.width;
        int height = rect.height;
        int scanlineStride = numBands*width;
        for (int k = 0; k < numBands; k++)  {
            float data[] =  mlimages[0].getFloatData();
            int scanlineOffset = k;
            float hiVal = hiVals[k];
            float loVal = loVals[k];
            for (int j = 0; j < height; j++)  {
                int pixelOffset = scanlineOffset;
                for (int i = 0; i < width; i++)  {
                    float tmp = data[pixelOffset];
                    if (tmp < loVal) {
                        data[pixelOffset] = loVal;
                    } else if (tmp > hiVal) {
                        data[pixelOffset] = hiVal;
                    }
                    pixelOffset += numBands;
                }
                scanlineOffset += scanlineStride;
            }
        }
    }

    private void clampDoubleArrays(double hiVals[], double loVals[]) {
        int width = rect.width;
        int height = rect.height;
        int scanlineStride = numBands*width;
        for (int k = 0; k < numBands; k++)  {
            double data[] = mlimages[0].getDoubleData();
            int scanlineOffset = k;
            double hiVal = hiVals[k];
            double loVal = loVals[k];
            for (int j = 0; j < height; j++)  {
                int pixelOffset = scanlineOffset;
                for (int i = 0; i < width; i++)  {
                    double tmp = data[pixelOffset];
                    if (tmp < loVal) {
                        data[pixelOffset] = loVal;
                    } else if (tmp > hiVal) {
                        data[pixelOffset] = hiVal;
                    }
                    pixelOffset += numBands;
                }
                scanlineOffset += scanlineStride;
            }
        }
    }
}

class MediaLibLoadException extends Exception {
    MediaLibLoadException() {
        super();
    }

    public synchronized Throwable
        fillInStackTrace() {
        return this;
    }
}
