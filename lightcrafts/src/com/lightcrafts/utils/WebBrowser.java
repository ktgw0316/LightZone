/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.lang.reflect.Method;

/**
 * Bare-bones browser launcher.  Supports: Mac OS X, GNU/Linux, Unix, and
 * Windows XP.  This code is in the public domain.
 *
 * @version 1.1
 * @author Dem T. Pilafian [dem@pilafian.com]
 * @see <a href="http://www.centerkey.com/java/browser/">Bare Bones Browser Launch for Java</a>.
 */
public final class WebBrowser {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Direct the user's default web browser to browse the given URL.
     *
     * @param url The URL to browse.
     * @return Returns <code>true</code> if the browser was successfully
     * launched.
     */
    public static boolean browse( String url ) {
        final String osName = System.getProperty( "os.name" );
        try {
            if ( osName.startsWith( "Mac OS" ) ) {
                final Class macUtils =
                    Class.forName( "com.apple.mrj.MRJFileUtils" );
                final Method openURL =
                    macUtils.getDeclaredMethod( "openURL", String.class );
                openURL.invoke( null, url );
                return true;
            }
            if ( osName.startsWith( "Windows" ) ) {
                Runtime.getRuntime().exec(
                    "rundll32 url.dll,FileProtocolHandler " + url
                );
                return true;
            }
            final Runtime rt = Runtime.getRuntime();
            for ( String browser : m_browsers ) {
                final String[] args = new String[]{ "which", browser };
                if ( rt.exec( args ).waitFor() == 0 ) {
                    rt.exec( new String[]{ browser, url } );
                    return true;
                }
            }
        }
        catch ( Exception e ) {
            // do nothing
        }
        return false;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The non-MacOSX/Windows implementation tries to launch various browsers.
     * This is the list of the browsers to try.
     */
    private static final String[] m_browsers = {
        "chromium-browser",
        "google-chrome",
        "firefox",
        "iceweasel",
        "safari",
        "opera",
        "konqueror",
        "epiphany-browser"
    };
}
/* vim:set et sw=4 ts=4: */
