package com.lightcrafts.image.libs;

import javax.media.jai.PlanarImage;
import com.lightcrafts.utils.UserCanceledException;

import java.awt.image.RenderedImage;

/**
 * Created by Masahiro Kitagawa on 2016/11/07.
 */
public interface LCImageReader {
    /**
     * Gets the image.
     *
     * @return Returns said image.
     */
    RenderedImage getImage() throws LCImageLibException, UserCanceledException;
}
