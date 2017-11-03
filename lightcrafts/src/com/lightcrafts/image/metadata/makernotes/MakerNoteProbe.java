/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata.makernotes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import lombok.NonNull;
import lombok.val;

import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;

/**
 * <code>MakerNoteProbe</code> is the abstract base class for classes that
 * &quot;probe&quot; the contents of metadata looking for
 * &quotsignature&quot; metadata to determine what type of maker notes they
 * are.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
public abstract class MakerNoteProbe<T extends MakerNotesDirectory> {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Tries to determine the type of makes notes in the metadata.
     *
     * @param metadata The {@link ImageMetadata} to probe.
     * @return Returns the class of the {@link ImageMetadataDirectory} for the
     * maker notes or <code>null</code> if the manufacturer of maker notes
     * could not be determined.
     */
    public static Class<? extends MakerNotesDirectory>
    determineMakerNotesFrom(ImageMetadata metadata) {
        for (val probe : m_makerNoteProbes) {
            val dirClass = probe.match(metadata);
            if (dirClass != null)
                return dirClass;
        }
        return null;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct a <code>MakerNoteProbe</code> and add the instance to the
     * global static list of all <code>MakerNoteProbe</code>s.
     */
    protected MakerNoteProbe() {
        m_dir = getDirClass();
        m_make = m_dir.getName().toUpperCase();
        m_makerNoteProbes.add(this);
    }

    /**
     * Checks if the maker notes inside the given metadata matches a particular
     * maker notes class.
     *
     * @param metadata The {@link ImageMetadata} to probe.
     * @return Returns the class of the {@link MakerNotesDirectory} for the
     * maker notes or <code>null</code> if the manufacturer of maker notes
     * could not be determined.
     */
    protected Class<T>
    match(ImageMetadata metadata) {
        return matchUsingMake(metadata);
    }

    /**
     * Checks if the maker notes inside the given metadata matches a particular
     * maker notes class by using the camera make.
     *
     * @param metadata The {@link ImageMetadata} to probe.
     * @return Returns the class of the {@link MakerNotesDirectory} for the
     * maker notes or <code>null</code> if the manufacturer of maker notes
     * could not be determined.
     */
    final Class<T> matchUsingMake(@NonNull ImageMetadata metadata) {
        val make = metadata.getCameraMake(false);
        if (make == null)
            return null;
        return make.contains(m_make) ? m_dir : null;
    }

    protected abstract Class<T> getDirClass();

    ////////// private ////////////////////////////////////////////////////////

    private Class<T> m_dir;
    private final String m_make;

    /**
     * The global static list of all <code>MakerNoteProbe</code>s.
     */
    private static final Set<MakerNoteProbe> m_makerNoteProbes =
        new HashSet<MakerNoteProbe>();

    static {
        // TODO: is there a better way to do this?
        Arrays.asList(
                CanonMakerNoteProbe.create(),
                FujiMakerNoteProbe.create(),
                KodakMakerNoteProbe.create(),
                MinoltaMakerNoteProbe.create(),
                NikonMakerNoteProbe.create(),
                OlympusMakerNoteProbe.create(),
                PanasonicMakerNoteProbe.create(),
                PentaxMakerNoteProbe.create(),
                SonyMakerNoteProbe.create()
        );
    }
}
/* vim:set et sw=4 ts=4: */
