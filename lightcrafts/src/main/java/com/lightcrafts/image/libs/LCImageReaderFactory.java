package com.lightcrafts.image.libs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import lombok.val;

/**
 * Created by Masahiro Kitagawa on 2016/11/07.
 */
public class LCImageReaderFactory {
    public LCImageReader create(File file)
            throws UnsupportedEncodingException, FileNotFoundException, LCImageLibException
    {
        val path = file.getPath();
        val i = path.lastIndexOf('.');
        val ext = i > 0 ? path.substring(i + 1) : "";

        // TODO: Java7 switch-case
        if (ext.equals("jpg") || ext.equals("jpeg") ) {
            return new LCJPEGReader(path);
        } else if (ext.equals("tiff") || ext.equals("tif")) {
            return new LCTIFFReader(path);
        }
        throw new UnsupportedEncodingException(path);
    }
}
