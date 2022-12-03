/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.templates;

import com.lightcrafts.utils.file.FileUtil;

import lombok.Getter;
import lombok.val;

import java.io.File;
import java.util.regex.Pattern;

/**
 * A model for Templates as a name and a namespace, with mappings to get from
 * these to a File and from a File to the names.
 */
@Getter
public class TemplateKey implements Comparable<TemplateKey> {

    // The first capture group is the namespace, the second is the name,
    // and semicolon is the optional separator.
    private static final Pattern NamePattern =
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

    private final String namespace;
    private final String name;
    private final File file;

    public static TemplateKey importKey(File file) {
        return new TemplateKey(
            new File(TemplateDatabase.TemplateDir, file.getName())
        );
    }

    public TemplateKey(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
        val encodedNamespace = FileUtil.encodeFilename(namespace);
        val encodedName = FileUtil.encodeFilename(name);
        file = new File(
            TemplateDatabase.TemplateDir,
            encodedNamespace + ';' + encodedName + ".lzt"
        );
    }

    TemplateKey(File file) {
        this.file = file;
        val fileName = file.getName();
        val baseName = FileUtil.trimExtensionOf(fileName);
        val matcher = NamePattern.matcher(baseName);
        val encodedNamespace = matcher.replaceAll("$1");
        val encodedName = matcher.replaceAll("$2");
        namespace = FileUtil.decodeFilename(encodedNamespace);
        name = FileUtil.decodeFilename(encodedName);
    }

    @Override
    public String toString() {
        return namespace.equals("") ? name : namespace + ';' + name;
    }

    @Override
    public int compareTo(TemplateKey o) {
        return toString().compareTo(o.toString());
    }
}
