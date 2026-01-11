/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2026-     Masahiro Kitagawa */

package com.lightcrafts.jai.operator;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.OperationDescriptorImpl;
import org.eclipse.imagen.ParameterBlockImageN;
import org.eclipse.imagen.RenderedOp;
import org.eclipse.imagen.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "LCUnsharpMask" operation.
 *
 * <p>Unsharp masking is derived from a photographic technique for improving the sharpness of images. In its digital
 * form it is implemented using convolution to create a low-pass filtered version of a source image. The low-pass image
 * is then subtracted from the original image, creating a high-pass image. The high pass image is then added back to the
 * original image, creating enhanced edge contrast. By adjusting a scaling factor, the degree of high pass add-back can
 * be controlled.
 *
 * <p>
 *
 * <table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>LCUnsharpMask</td></tr>
 * <tr><td>LocalName</td>   <td>LCUnsharpMask</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.jai</td></tr>
 * <tr><td>Description</td> <td>Performs unsharp masking to sharpen or smooth an image.</td></tr>
 * <tr><td>Version</td>     <td>1.1</td></tr>
 * <tr><td>arg0Desc</td>    <td>The gain factor.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The threshold.</td></tr>
 * </table>
 *
 * <p>
 *
 * <table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>   <th>Class Type</th>
 *                     <th>Default Value</th></tr>
 * <tr><td>gain</td> <td>java.lang.Float</td>
 *                     <td>1.0F</td>
 * <tr><td>threshold</td> <td>java.lang.Int</td>
 *                     <td>0</td>
 * </table>
 *
 * @see org.eclipse.imagen.OperationDescriptor
 */
public class LCUnsharpMaskDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "LCUnsharpMask"},
        {"LocalName",   "LCUnsharpMask"},
        {"Vendor",      "com.lightcrafts.jai"},
        {"Description", "Unsharp Mask Operation"},
        {"DocURL",      "none"},
        {"Version",     "1.0"}
    };

    /**
     * The parameter class list for this operation.
     */
    private static final Class[] paramClasses = {
        Double.class, Integer.class
    };

    /**
     * The parameter name list for this operation.
     */
    private static final String[] paramNames = {
        "gain", "threshold"
    };

    /**
     * The parameter default value list for this operation.
     */
    private static final Object[] paramDefaults = {
         1.0, 0
    };

    /** Constructor. */
    public LCUnsharpMaskDescriptor() {
        super(resources, 2, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Performs UnsharpMask operation on the image.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link ImageN#create(String,ParameterBlock,RenderingHints)}.
     *
     * @see ImageN
     * @see ParameterBlockImageN
     * @see RenderedOp
     * @param source0 <code>RenderedImage</code> source 0.
     * @param source1 <code>RenderedImage</code> source 1.
     * @param gain The sharpening value.
     * @param threshold The sharpening threshold.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    RenderedImage source1,
                                    double gain, int threshold,
                                    RenderingHints hints)  {
        ParameterBlockImageN pb =
            new ParameterBlockImageN("LCUnsharpMask",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);
        pb.setParameter("gain", gain);
        pb.setParameter("threshold", threshold);

        return ImageN.create("LCUnsharpMask", pb, hints);
    }
}
