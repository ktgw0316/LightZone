package com.lightcrafts.image.libs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

/**
 * Created by Masahiro Kitagawa on 2016/11/07.
 */
public class LCImageReaderFactory {
    public LCImageReader create(File file)
            throws UnsupportedEncodingException, FileNotFoundException, LCImageLibException
    {
        final var path = file.getPath();
        final var i = path.lastIndexOf('.');
        final var ext = i > 0 ? path.substring(i + 1) : "";

        return switch (ext) {
            case "jpg", "jpeg" -> new LCJPEGReader(path);
            case "tif", "tiff" -> new LCTIFFReader(path);
            default -> throw new UnsupportedEncodingException(path);
        };
    }
}
