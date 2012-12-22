/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.file;

import java.io.File;

/**
 * A <code>SmartFolder</code> is-a {@link File} that represents some kind of
 * &quot;smart folder.&quot;
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class SmartFolder extends File {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Tests whether the {@link File} denoted by this <code>SmartFolder</code>
     * actually is a "Smart Folder".
     *
     * @return Returns <code>true</code> only if it is.
     */
    public abstract boolean isSmartFolder();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct a <code>SmartFolder</code>.
     *
     * @param pathname The abstract pathname.
     */
    protected SmartFolder( String pathname ) {
        super( pathname );
    }

    /**
     * Construct a <code>SmartFolder</code>.
     *
     * @param file The original {@link File}.
     */
    protected SmartFolder( File file ) {
        super( file.getAbsolutePath() );
    }

    /**
     * Construct a <code>SmartFolder</code>.
     *
     * @param parent The parent {@link File}.
     * @param child The child filename.
     */
    protected SmartFolder( File parent, String child ) {
        super( parent, child );
    }

    /**
     * Construct a <code>SmartFolder</code>.
     *
     * @param parent The parent abstract pathname.
     * @param child The child filename.
     */
    protected SmartFolder( String parent, String child ) {
        super( parent, child );
    }

}
/* vim:set et sw=4 ts=4: */
