/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.media.jai.opimage.RIFUtil;

import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.BorderExtender;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Jul 30, 2006
 * Time: 10:57:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class BilateralFilterRIF implements RenderedImageFactory {
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderHints);

        // Get BorderExtender from renderHints if any.
        BorderExtender extender = RIFUtil.getBorderExtenderHint(renderHints);

        float sigma_d = paramBlock.getFloatParameter(0);
        float sigma_r = paramBlock.getFloatParameter(1);

        return new BilateralFilterOpImage(paramBlock.getRenderedSource(0),
                                                   extender,
                                                   renderHints,
                                                   layout,
                                                   sigma_d, sigma_r);
    }
}
