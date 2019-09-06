/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * A {@code MacOSXFolderFilter} is-a {@link FolderFilter} for
 * Mac&nbsp;OS&nbsp;X that filters out additional directories that the Finder
 * hides.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class MacOSXFolderFilter extends FolderFilter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public boolean accept(File file) {
        if (!super.accept(file))
            return false;
        final String parent = file.getParent();
        if (!("/".equals(parent) ||
             m_volumePattern.matcher(parent).matches()))
            return true;

        if (m_volumePattern.matcher(parent).matches() &&
             file.getName().equals("tmp")
       )
            return true;

        return !m_hiddenDirectories.contains(file.getName());
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The set of directories that are hidden by the Finder.
     */
    private static final HashSet<String> m_hiddenDirectories = new HashSet<>();

    /**
     * The {@link Pattern} used to match the parent directory of a candidate
     * directory to determine whether it should be filtered out or not.
     * <p>
     * The set of hidden file names should be filtered out only if the parent's
     * path matches {@code /Volumes/*}.
     */
    private static final Pattern m_volumePattern =
        Pattern.compile("^/Volumes/[^/]*$");

    static {
        final String[] hiddenDirectories = {
            "automount",
            "bin",
            "cores",
            "dev",
            "etc",
            "Library",
            "lost+found",
            "opt",
            "private",
            "sbin",
            "System",
            "tmp",
            "usr",
            "var",
            "Volumes",
        };
        m_hiddenDirectories.addAll(Arrays.asList(hiddenDirectories));
    }

}
/* vim:set et sw=4 ts=4: */
