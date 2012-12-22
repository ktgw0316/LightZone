/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.LZNDocumentProvider;
import com.lightcrafts.image.types.LZNImageType;
import com.lightcrafts.utils.LightCraftsException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Encapsulates the cumbersome procedures to extract LZN data and an image
 * file pointer, if any are defined, from a file.
 */
public class DocumentReader {

    /**
     * The output from reading a file is a triplet: and XML document holding
     * the LZN data; an ImageInfo, in case the file is also an image; and a
     * file where the original image is supposed to be located.
     */
    public static class Interpretation {
        public XmlDocument xml;
        public File imageFile;
        public ImageInfo info;
    }

    /**
     * Tell quickly if the given file has any LZN data in it.
     */
    public static boolean isReadable(File file) {
        ImageInfo info = ImageInfo.getInstanceFor(file);
        try {
            return info.getImageType() instanceof LZNDocumentProvider;
        }
        catch ( Throwable t ) {
            return false;
        }
    }

    /**
     * Identify the LZN content and image file pointer contained in the given
     * file, or return null if no LZN data can be identified.
     * <p>
     * It is possible that LZN data exists but no image file can be found.
     * This is typical in "LZT" (template) files, for instance.
     */
    public static Interpretation read(File file) {
        XmlDocument xmlDoc = null;
        File imageFile = null;

        ImageInfo info = ImageInfo.getInstanceFor(file);
        ImageType type;
        try {
            type = info.getImageType();
        }
        catch (IOException e) {
            return null;
        }
        catch (LightCraftsException e) {
            if (file.getName().endsWith(".lzt")) {
                // This is a symptom of a template file, which does have an
                // Interpretation.  So we continue.
                type = LZNImageType.INSTANCE;
            }
            else {
                return null;
            }
        }
        if (type == LZNImageType.INSTANCE) {
            try {
                InputStream in = new FileInputStream(file);
                xmlDoc = new XmlDocument(in);
                LightweightDocument lwDoc = new LightweightDocument(file);
                imageFile = lwDoc.getImageFile();
            }
            catch (IOException e) {
                // Fall back to the embedded document test.
            }
        }
        // Second try as an image with embedded document metadata:
        if (xmlDoc == null) {
            try {
                if (type instanceof LZNDocumentProvider) {
                    final LZNDocumentProvider p = (LZNDocumentProvider)type;
                    Document lznDoc = p.getLZNDocument(info);
                    if (lznDoc != null) {
                        xmlDoc = new XmlDocument(lznDoc.getDocumentElement());
                        if (xmlDoc != null) {
                            // The original image may be in the same file,
                            // or referenced through a path pointer:
                            XmlNode root = xmlDoc.getRoot();
                            // (tag copied from ui.editor.Document)
                            XmlNode imageNode = root.getChild("Image");
                            // (tag written in export())
                            if ( imageNode.hasAttribute("self")) {
                                imageFile = file;
                            }
                            else {
                                LightweightDocument lwDoc =
                                    new LightweightDocument(file, xmlDoc);
                                imageFile = lwDoc.getImageFile();
                            }
                        }
                    }
                }
            }
            catch (BadImageFileException e) {
                return null;
            }
            catch (IOException e) {
                return null;
            }
            catch (UnknownImageTypeException e) {
                return null;
            }
            catch (Throwable t) {
                System.out.println("Unexpected error in DocumentReader.read()");
                System.out.println(
                    t.getClass().getName() + ": " + t.getMessage()
                );
                return null;
            }
        }
        if (xmlDoc != null) {
            Interpretation interp = new Interpretation();
            interp.xml = xmlDoc;
            interp.imageFile = imageFile;
            interp.info = info;
            return interp;
        }
        else {
            return null;
        }
    }
}
