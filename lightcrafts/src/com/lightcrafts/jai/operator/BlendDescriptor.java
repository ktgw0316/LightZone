/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.operator;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;
import javax.media.jai.registry.RenderableRegistryMode;

import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 1, 2005
 * Time: 10:09:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class BlendDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Blend"},
        {"LocalName",   "Blend"},
        {"Vendor",      "lightcrafts.com"},
        {"Description", "Blend Two Images and a Mask"},
        {"DocURL",      "none"},
        {"Version",     "1.0"}
    };

    private static Class[] paramClasses = { String.class, Double.class, ROIShape.class, RenderedImage.class };
    private static String[] paramNames = { "blendingMode", "opacity", "mask", "colorSelection" };
    private static Object[] paramDefaults = { "Overlay", 1.0, null, null };

    /** Constructor. */
    public BlendDescriptor() {
        super(resources, 2, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }


    /**
     * Adds two images.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link javax.media.jai.JAI#create(String,java.awt.image.renderable.ParameterBlock,java.awt.RenderingHints)}.
     *
     * @see javax.media.jai.JAI
     * @see javax.media.jai.ParameterBlockJAI
     * @see javax.media.jai.RenderedOp
     *
     * @param source0 <code>RenderedImage</code> source 0.
     * @param source1 <code>RenderedImage</code> source 1.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    RenderedImage source1,
                                    RenderedImage colorSelection,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Blend",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);
        pb.setParameter("blendingMode", paramDefaults[0]);
        pb.setParameter("opacity", paramDefaults[1]);
        pb.setParameter("mask", paramDefaults[2]);
        pb.setParameter("colorSelection", colorSelection);
        return JAI.create("Blend", pb, hints);
    }

    public static RenderedOp create(RenderedImage source0,
                                    RenderedImage source1,
                                    RenderedImage colorSelection,
                                    String blendingMode,
                                    Double opacity,
                                    ROI mask,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Blend",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);
        pb.setSource("colorSelection", colorSelection);
        pb.setParameter("blendingMode", blendingMode);
        pb.setParameter("opacity", opacity);
        pb.setParameter("mask", mask);
        return JAI.create("Blend", pb, hints);
    }

    /**
     * Adds two images.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,java.awt.image.renderable.ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see javax.media.jai.RenderableOp
     *
     * @param source0 <code>RenderableImage</code> source 0.
     * @param source1 <code>RenderableImage</code> source 1.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                RenderableImage source1,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Blend",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);
        pb.setParameter("blendingMode", paramDefaults[0]);
        pb.setParameter("opacity", paramDefaults[1]);
        pb.setParameter("mask", paramDefaults[2]);
        return JAI.createRenderable("Blend", pb, hints);
    }
}
