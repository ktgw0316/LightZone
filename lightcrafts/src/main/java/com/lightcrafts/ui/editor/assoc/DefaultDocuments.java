/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.ui.editor.assoc;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.RawImageType;
import com.lightcrafts.utils.raw.Camera;
import com.lightcrafts.utils.xml.XmlDocument;

import java.io.*;
import java.net.URL;

/**
 * A resolver from ImageMetadata objects to resource URL's for
 * default LZN files.
 */

class DefaultDocuments {

    static final boolean Debug =
        System.getProperty("lightcrafts.debug") != null;

    /**
     * Return the URL of a default LZN document suitable for an image with
     * the given metadata.
     */
    static URL getDefaultDocumentUrl(ImageMetadata meta) {
        if (isRaw(meta)) {
            // ImageMetadata.getCameraMake() is not completely implemented,
            // and still returns null in may cases where a valid make String
            // is available.
            String make = meta.getCameraMake(true);
            if (make != null) {
                return Camera.getLztUrl(make);
            } else if (Debug) {
                System.err.println(
                    "No camera make found for RAW file \"" +
                    meta.getFile().getName() +
                    "\""
                );
            }
        }
        return null;
    }

    /**
     * Tell if the given ImageMetadata points to a File containing RAW
     * image data.  Only RAW files can have default Documents.
     */
    private static boolean isRaw(ImageMetadata meta) {
        ImageType type = meta.getImageType();
        return (type instanceof RawImageType);
    }

    public static void main(String[] args)
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        File file = new File(args[0]);
        ImageInfo info = ImageInfo.getInstanceFor(file);
        ImageMetadata meta = info.getMetadata();
        URL url = getDefaultDocumentUrl(meta);
        if (url != null) {
            XmlDocument doc = DocumentDatabase.getDefaultDocument(meta);
            if (doc != null) {
                doc.write(System.out);
            }
        }
        System.out.println(url);
    }
}
