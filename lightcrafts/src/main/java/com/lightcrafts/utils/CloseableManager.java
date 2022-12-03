/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * A <code>CloseableManager</code> manages a collection of {@link Closeable}s
 * in some way.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface CloseableManager {

    /**
     * Manage a {@link Closeable}.
     *
     * @param closeable The {@link Closeable} to manage.
     */
    void manage( Closeable closeable ) throws IOException;

}
/* vim:set et sw=4 ts=4: */
