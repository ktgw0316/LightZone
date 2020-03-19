/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.regex.Matcher;

import com.lightcrafts.platform.Platform;

/**
 * A <code>LocalUtil</code> is a utility class for getting localized strings.
 *
 * @author Anton Kast [anton@lightcrafts.com]
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class LocaleUtil {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Constructs a <code>LocaleUtil</code> object.
     *
     * @param localeClass The {@link Class} to get the localized strings for.
     */
    public LocaleUtil( Class localeClass ) {
        m_resources = ResourceBundle.getBundle( localeClass.getName() );
    }

    /**
     * Gets the localized string for the given key.  If not found, the name of
     * the current platform's type is appended to the key and attempts to get
     * the localized string for that composite key.
     *
     * @param key The key to get the localized string for.
     */
    public String get( String key ) {
        try {
            return m_resources.getString( key );
        }
        catch ( MissingResourceException e ) {
            return m_resources.getString( key + Platform.getType() );
        }
    }

    /**
     * Gets the localized string for the given key and replaces the first
     * occurrence of "$1" in the value with the given substitution.
     *
     * @param key The key to get the localized string for.
     * @param subs The substitution integers.
     */
    public String get( String key, int... subs ) {
        final String[] subsAsStrings = new String[ subs.length ];
        for ( int i = 0; i < subs.length; ++i )
            subsAsStrings[i] = Integer.toString( subs[i] );
        return get( key, subsAsStrings );
    }

    /**
     * Gets the localized string for the given key and replaces the first
     * occurrence of <code>$</code><i>i</i> in the string with the <i>ith</i>
     * substitution.
     *
     * @param key The key to get the localized string for.
     * @param subs The substitution strings.
     */
    public String get( String key, String... subs ) {
        String s = get( key );
        for ( int i = 0; i < subs.length; ++i  )
            s = s.replaceFirst( "\\$" + (i+1), Matcher.quoteReplacement( subs[i] ) );
        return s;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final ResourceBundle m_resources;
}
/* vim:set et sw=4 ts=4: */
