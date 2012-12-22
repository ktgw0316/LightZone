/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.templates;

import com.lightcrafts.utils.file.FileUtil;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A model for Templates as a name and a namespace, with mappings to get from
 * these to a File and from a File to the names.
 */
public class TemplateKey implements Comparable<TemplateKey> {

    // The first capture group is the namespace, the second is the name,
    // and semicolon is the optional separator.
    private static Pattern NamePattern =
        Pattern.compile("(?:([^;]*);)?(.*)");

    // Here are example results of matching with this pattern:
    //
    // File Name    $1 (Name)      $2 (Namespace)
    //
    // a;b          a               b
    // ;a           (empty)         b
    // a;           a               (empty)
    // a            (empty)         a
    // a;b;c        a               b;c

    private String namespace;
    private String name;
    private File file;

    public static TemplateKey importKey(File file) {
        return new TemplateKey(
            new File(TemplateDatabase.TemplateDir, file.getName())
        );
    }

    public TemplateKey(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
        String encodedNamespace = FileUtil.encodeFilename(namespace);
        String encodedName = FileUtil.encodeFilename(name);
        this.file = new File(
            TemplateDatabase.TemplateDir,
            encodedNamespace + ';' + encodedName + ".lzt"
        );
    }

    TemplateKey(File file) {
        this.file = file;
        String fileName = file.getName();
        String baseName = FileUtil.trimExtensionOf(fileName);
        Matcher matcher = NamePattern.matcher(baseName);
        String encodedNamespace = matcher.replaceAll("$1");
        String encodedName = matcher.replaceAll("$2");
        namespace = FileUtil.decodeFilename(encodedNamespace);
        name = FileUtil.decodeFilename(encodedName);
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public File getFile() {
        return file;
    }

    public String toString() {
        if (! namespace.equals("")) {
            return namespace + ';' + name;
        }
        else {
            return name;
        }
    }

    public int compareTo(TemplateKey o) {
        return toString().compareTo(o.toString());
    }
}
