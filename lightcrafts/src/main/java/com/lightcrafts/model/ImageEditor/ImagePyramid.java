/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import lombok.val;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.ArrayList;

class ImagePyramid {
    private RenderedImage currentImage;
    private int currentLevel = 0;

    final int mipScaleRatio;
    final ArrayList<RenderedImage> renderings = new ArrayList<RenderedImage>();

    ImagePyramid(RenderedImage image, int mipScaleRatio) {
        this.mipScaleRatio = mipScaleRatio;
        currentImage = image;
        renderings.add(currentImage);
    }

    synchronized public RenderedImage getUpImage() {
        if (currentLevel > 0) {
            currentLevel--;
            currentImage = renderings.get(currentLevel);
        }
        return currentImage;
    }

    synchronized public RenderedImage getDownImage() {
        currentLevel++;
        if (renderings.size() <= currentLevel) {
            RenderedOp smaller = createDownScaleOp(currentImage, mipScaleRatio);
            smaller.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
            renderings.add(smaller);
            currentImage = smaller;
        } else {
            currentImage = renderings.get(currentLevel);
        }
        return currentImage;
    }

    public RenderedImage getImage(int level) {
        if (level < 0)
            return null;

        while (currentLevel < level)
            getDownImage();
        while (currentLevel > level)
            getUpImage();

        return currentImage;
    }

    RenderedOp createDownScaleOp(RenderedImage src, int ratio) {
        val kernel = Functions.getLanczos2Kernel(ratio);
        val ko = kernel.getXOrigin();
        val kdata = kernel.getHorizontalKernelData();
        val qsFilterArray = new float[kdata.length - ko];
        System.arraycopy(kdata, ko, qsFilterArray, 0, qsFilterArray.length);

        val params = new ParameterBlock();
        params.addSource(src);
        params.add(ratio);
        params.add(ratio);
        params.add(qsFilterArray);
        params.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        return JAI.create("FilteredSubsample", params,
                new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                        BorderExtender.createInstance(BorderExtender.BORDER_COPY)));
    }

}
