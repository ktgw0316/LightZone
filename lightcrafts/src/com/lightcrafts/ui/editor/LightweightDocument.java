/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;

import static com.lightcrafts.ui.editor.Locale.LOCALE;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to parse saved Documents and extract the contents of their
 * Image tags.
 */

public class LightweightDocument {

    private final static String ImageTag = "Image";
    private final static String PathTag = "path";
    private final static String RelativePathTag = "relativePath";

    private final static Pattern PathsPattern = Pattern.compile(
        ".*" +                                        // anything
        "<\\s*" + ImageTag +                          // "Image" element
        "\\s+(?:.*\\s+)?" +                           // white-any-white
        PathTag + "\\s*=\\s*\"([^\"]*)\"" +           // capture "path" attr
        "(\\s+(?:.*\\s+)?" +                          // white-any-white
        RelativePathTag + "\\s*=\\s*\"([^\"]*)\")?" + // capture "relativePath"
        ".*"                                          // anything
    );

    private File imageFile;
    private File docFile;

    public LightweightDocument(String path) throws IOException, XMLException {
        this(new File(path));
    }

    /**
     * Construct a LightweightDocument from the XML content already extracted.
     */
    public LightweightDocument(File file, XmlDocument doc) throws XMLException {
        XmlNode root = doc.getRoot();
        XmlNode imageNode = root.getChild("Image");
        String path = imageNode.getAttribute("path");
        imageFile = new File(path);
        if (! imageFile.isFile()) {
            if (imageNode.hasAttribute("relativePath")) {
                String relativepath = imageNode.getAttribute("relativePath");
                File relativeFile =
                    RelativePathUtility.getRelativeFile(file, relativepath);
                if (relativeFile.isFile()) {
                    imageFile = relativeFile;
                }
            }
        }
    }

    /**
     * Construct a LightweightDocument by grepping the given file for LZN
     * content.
     */
    public LightweightDocument(File file) throws IOException, XMLException {
        docFile = file;
        InputStream in = new FileInputStream(file);

        // XML parsing takes too long for scanning hundreds of files:
//        XmlDocument doc = new XmlDocument(in);
//        XmlNode root = doc.getRoot();
//        XmlNode imageNode = root.getChild(ImageTag);
//        String imagePath = imageNode.getAttribute(PathTag);

        // Instead, we just do some grepping:
        String line;
        Matcher matcher = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            do {
                line = reader.readLine();
                if (line != null) {
                    matcher = PathsPattern.matcher(line);
                }
            } while ((line != null) && (!matcher.matches()));
        }

        if (matcher == null) {
            // readLine() returned null the first time it was called.
            throw new IOException(LOCALE.get("EmptyFileError", file.getName()));
        }

        if (! matcher.matches()) {
            throw new XMLException(LOCALE.get("MissingImageTagError"));
        }
        String path = matcher.replaceFirst("$1");
        if (path.length() == 0) {
            throw new XMLException(LOCALE.get("MissingImagePathError"));
        }
        imageFile = new File(matcher.replaceFirst("$1"));

        String relativePath = matcher.replaceFirst("$3");
        // The relative path attribute was introduced in LZN version 3,
        // and is therefore optional.  Starting in version 6, it overrides
        // the absolute path when present.
        if (relativePath.length() > 0) {
            File relativeFile = RelativePathUtility.getRelativeFile(
                docFile, relativePath
            );
            if (relativeFile.isFile()) {
                imageFile = relativeFile;
            }
        }
    }

    public File getDocFile() {
        return docFile;
    }

    public File getImageFile() {
        return imageFile;
    }

    public boolean imageFileExists() {
        return ((imageFile != null) && imageFile.isFile());
    }
}
