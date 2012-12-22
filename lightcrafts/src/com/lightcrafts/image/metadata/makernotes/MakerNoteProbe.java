/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import java.util.ArrayList;

import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;

/**
 * <code>MakerNoteProbe</code> is the abstract base class for classes that
 * &quot;probe&quot; the contents of metadata looking for
 * &quotsignature&quot; metadata to determine what type of maker notes they
 * are.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class MakerNoteProbe {

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
    determineMakerNotesFrom( ImageMetadata metadata ) {
        for ( MakerNoteProbe probe : m_makerNoteProbes ) {
            final Class<? extends MakerNotesDirectory> dirClass =
                probe.match( metadata );
            if ( dirClass != null )
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
        m_makerNoteProbes.add( this );
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
    protected abstract Class<? extends MakerNotesDirectory>
    match( ImageMetadata metadata );

    /**
     * Checks if the maker notes inside the given metadata matches a particular
     * maker notes class by using the camera make.
     *
     * @param metadata The {@link ImageMetadata} to probe.
     * @param makeWanted The make of the camera used to photograph the image.
     * @param dir The {@link MakerNotesDirectory} subclass to return if the
     * make wanted matches the make found in the metadata.
     * @return Returns the class of the {@link MakerNotesDirectory} for the
     * maker notes or <code>null</code> if the manufacturer of maker notes
     * could not be determined.
     */
    protected static Class<? extends MakerNotesDirectory>
    matchUsingMake( ImageMetadata metadata, String makeWanted,
                    Class<? extends MakerNotesDirectory> dir ) {
        final String make = metadata.getCameraMake( false );
        if ( make != null )
            return make.contains( makeWanted.toUpperCase() ) ? dir : null;
        return null;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The global static list of all <code>MakerNoteProbe</code>s.
     */
    private static final ArrayList<MakerNoteProbe> m_makerNoteProbes =
        new ArrayList<MakerNoteProbe>();

    static {
        // TODO: is there a better way to do this?
        final MakerNoteProbe probesToLoad[] = {
            CanonMakerNoteProbe.INSTANCE,
            FujiMakerNoteProbe.INSTANCE,
            KodakMakerNoteProbe.INSTANCE,
            MinoltaMakerNoteProbe.INSTANCE,
            NikonMakerNoteProbe.INSTANCE,
            OlympusMakerNoteProbe.INSTANCE,
            PanasonicMakerNoteProbe.INSTANCE,
            PentaxMakerNoteProbe.INSTANCE,
        };
    }
}
/* vim:set et sw=4 ts=4: */
