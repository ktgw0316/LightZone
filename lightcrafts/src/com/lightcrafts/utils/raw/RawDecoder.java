package com.lightcrafts.utils.raw;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.providers.*;

import java.awt.image.RenderedImage;
import java.io.IOException;

public abstract class RawDecoder implements
        ApertureProvider, CaptureDateTimeProvider, FocalLengthProvider,
        ISOProvider, MakeModelProvider, ShutterSpeedProvider, WidthHeightProvider {
    abstract public boolean decodable();

    public abstract float[] getCameraMultipliers();

    public abstract float[][] getCameraRGB();

    public abstract float[] getDaylightMultipliers();

    public abstract int getFilters();

    public abstract RenderedImage getImage() throws BadImageFileException, UnknownImageTypeException, IOException;

    abstract public String getMake();

    abstract public String getModel();

    public abstract RenderedImage getPreview() throws BadImageFileException, UnknownImageTypeException, IOException;

    public abstract int getRawWidth();

    public abstract int getRawHeight();

    public abstract RenderedImage getThumbnail() throws UnknownImageTypeException, BadImageFileException, IOException;

    public abstract int getThumbHeight();

    public abstract int getThumbWidth();

    abstract public int rawColors();

}
