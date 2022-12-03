/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.export;

import com.lightcrafts.image.types.ImageType;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;

public class ExportNameUtility {

    /**
     *  A Pattern to decompose a File's name into the form,
     * "(name)-(number).(extension)".
     */
    private final static Pattern FileNamePattern =
        Pattern.compile("(?:_lzn(?:-([1-9]+[0-9]*))?)?(?:\\.([^.]*))?$");

    /**
     * A Pattern to isolate a File's extension string.
     */
    private final static Pattern FileExtensionPattern =
        Pattern.compile("(?:\\.[^.]*)?$");

    /**
     * Iterate through variant Files until we hit one that
     * won't clobber an existing file/
     */
    public static File ensureNotExists(File file) {
        if (! file.exists()) {
            return file;
        }
        final String base = trimFileNumberAndExtension(file);
        final String ext = getFileExtension(file);
        int num = getNumber(file);
        if (num < 0) {
            num = 0;
        }
        do {
            String path = constructPath(base, num++, ext);
            file = new File(path);
        } while (file.exists());

        return file;
    }

    private static String trimFileNumberAndExtension(File file) {
        String path = file.getPath();
        final Matcher matcher = FileNamePattern.matcher(path);
        if (matcher.find()) {
            return matcher.replaceFirst("").concat("_lzn");
        }
        return trimFileExtension(path);
    }

    /**
     * Remove "_lzn", number, and extension strings from the file path.
     */
    public static String getBaseName(File file) {
        String path = file.getPath();
        Matcher matcher = FileNamePattern.matcher(path);
        if (matcher.find()) {
            return matcher.replaceFirst("");
        }
        return null;
    }

    public static String getFileExtension(File file) {
        String path = file.getPath();
        Matcher matcher = FileNamePattern.matcher(path);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }

    public static File setFileExtension(File file, String ext) {
        String path = file.getPath();
        String oldExt = getFileExtension(file);
        if (oldExt != null) {
            if (ext.equals(oldExt)) {
                return file;
            }
            Matcher matcher = FileExtensionPattern.matcher(path);
            path = matcher.replaceFirst("." + ext);
        }
        else {
            path = path + "." + ext;
        }
        return new File(path);
    }

    public static File trimFileExtension(File file) {
        String path = file.getPath();
        String oldExt = getFileExtension(file);
        if (oldExt != null) {
            Matcher matcher = FileExtensionPattern.matcher(path);
            path = matcher.replaceFirst("");
        }
        return new File(path);
    }

    public static String trimFileExtension(String name) {
        File file = new File(name);
        String oldExt = getFileExtension(file);
        if (oldExt != null) {
            Matcher matcher = FileExtensionPattern.matcher(name);
            name = matcher.replaceFirst("");
        }
        return name;
    }

    public static File ensureCompatibleExtension(File file, ImageType type) {
        String ext = getFileExtension(file);
        String[] typeExts = type.getExtensions();
        if (ext != null) {
            for (String typeExt : typeExts) {
                if (ext.equalsIgnoreCase(typeExt)) {
                    return file;
                }
            }
        }
        return setFileExtension(file, typeExts[0]);
    }

    private static String constructPath(String base, int num, String ext) {
        final StringBuilder buffer = new StringBuilder();
        if (base != null) {
            buffer.append(base);
        }
        if (num > 0) {
            buffer.append("-").append(num);
        }
        if (ext != null) {
            buffer.append(".").append(ext);
        }
        return buffer.toString();
    }

    private static int getNumber(File file) {
        String path = file.getPath();
        Matcher matcher = FileNamePattern.matcher(path);
        if (matcher.find()) {
            String num = matcher.group(1);
            if (num != null) {
                try {
                    return Integer.parseInt(num);
                }
                catch (NumberFormatException ignored) {
                }
            }
        }
        return -1;
    }
}
