/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import java.io.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * This is a utility class to figure out relative paths between pairs of
 * Files.  It's provides another way to associate saved Documents with their
 * image files.
 */

class RelativePathUtility {

    /**
     * Get a relative path pointing from one File to another.
     * @param source A File to point from.
     * @param target A file to point to.
     * @return A path String pointing from source to target.
     */
    static String getRelativePath(File source, File target) throws IOException {
        source = new File(source.getCanonicalPath());
        target = new File(target.getCanonicalPath());
        File common = getCommonAncestor(source, target);
        if (common == null) {
            return null;
        }
        String upPath = getRelativeAncestor(common, source);
        String downPath = getRelativeDescendant(common, target);
        String path;
        if ((upPath.length() > 0) && (downPath.length() > 0)) {
            path = upPath + File.separatorChar + downPath;
        }
        else {
            path = upPath + downPath;
        }
        return path;
    }

    /**
     * Construct the File that is separated from the given File by the
     * given relative path.
     * @param source A File to point from.
     * @param path A relative path String pointing from the source.
     * @return A File that is the implied target.
     */
    static File getRelativeFile(File source, String path) {
        source = source.getParentFile();
        return new File(source, path);
    }

    /**
     * Get a List of ancestor Files of a given File, in order from highest to
     * lowest ancestor and not including the File itself.
     */
    static List<File> getAncestors(File file) {
        LinkedList<File> list = new LinkedList<File>();
        file = file.getParentFile();
        if (file == null) {
            return list;
        }
        do {
            list.addFirst(file);
            file = file.getParentFile();
        } while (file != null);
        return list;
    }

    /**
     * Find a common ancestor File of the two given Files, if one exists.
     * If no such common ancestor exists, return null.
     */
    static File getCommonAncestor(File f1, File f2) {
        List a1 = getAncestors(f1);
        List a2 = getAncestors(f2);
        Iterator i1 = a1.iterator();
        Iterator i2 = a2.iterator();
        Object common = null;
        while (i1.hasNext() && i2.hasNext()) {
            Object o1 = i1.next();
            Object o2 = i2.next();
            if (! o1.equals(o2)) {
                break;
            }
            common = o1;
        }
        return (File) common;
    }

    /**
     * Get a relative path pointing from an ancestor to a descendant.
     * @return A relative File, pointing from the ancestor to the descendant.
     * @throws IllegalArgumentException If the given descendant is not
     * actually a descendant of the given ancestor.
     */
    private static String getRelativeDescendant(
        File ancestor, File descendant
    ) {
        List ancestors = getAncestors(descendant);
        if (! ancestors.contains(ancestor)) {
            throw new IllegalArgumentException(
                "The descendant \"" + descendant.getPath() + "\" " +
                "does not have ancestor \"" + ancestor.getPath() + "\"."
            );
        }
        // Move an iterator to the location of the ancestor:
        Iterator i = ancestors.iterator();
        File f;
        do {
            f = (File) i.next();
        } while (! ancestor.equals(f));

        // Construct a path String from the remaining iterations:
        StringBuffer path = new StringBuffer();
        while (i.hasNext()) {
            f = (File) i.next();
            path.append(f.getName());
            path.append(File.separatorChar);
        }
        path.append(descendant.getName());

        return path.toString();
    }

    /**
     * Get a relative path pointing from a descendant to an ancestor.
     * @return A relative File, pointing from the descendant to the ancestor.
     * @throws IllegalArgumentException If the given descendant is not
     * actually a descendant of the given ancestor.
     */
    private static String getRelativeAncestor(File ancestor, File descendant) {
        List ancestors = getAncestors(descendant);
        if (! ancestors.contains(ancestor)) {
            throw new IllegalArgumentException(
                "The file \"" + descendant.getPath() + "\" " +
                "does not have ancestor \"" + ancestor.getPath() + "\"."
            );
        }
        // Move an iterator to the location of the ancestor:
        Iterator i = ancestors.iterator();
        File f;
        do {
            f = (File) i.next();
        } while (! ancestor.equals(f));

        // Construct a path String from the remaining iterations:
        StringBuffer path = new StringBuffer();
        while (i.hasNext()) {
            i.next();
            path.append("..");
            if (i.hasNext()) {
                path.append(File.separatorChar);
            }
        }
        return path.toString();
    }

    public static void main(String[] args) throws IOException {

        String s1 = "/a/b/c";
        String s2 = "/a/d/e";

        File f1 = new File(s1);
        File f2 = new File(s2);

        String path = getRelativePath(f1, f2);
        System.out.println(path);    // "../d/e"

        File file = getRelativeFile(f1, path);
        System.out.println(file.getPath());
    }
}
