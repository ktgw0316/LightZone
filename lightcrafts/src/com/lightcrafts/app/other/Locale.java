/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.other;

import com.lightcrafts.utils.LocaleUtil;

/**
 * A <code>Locale</code> holds the singleton static instance of
 * {@link LocaleUtil} for the <code>com.lightcrafts.app.other</code> package.
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 */
final class Locale {
    
    static final LocaleUtil LOCALE = new LocaleUtil( Locale.class );

}
/* vim:set et sw=4 ts=4: */
