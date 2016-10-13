/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

import java.util.HashSet;
import java.util.Locale;

/**
 * Launches the Windows HTML Help Viewer to show our application's help.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class WindowsHelp {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Launches the Windows HTML Help Viewer to show our application's help.
     *
     * @param topic The topic to show or <code>null</code> to show the cover
     * page.
     */
    public static void showHelpTopic( String topic ) {
        String iso639LangCode = Locale.getDefault().getLanguage();
        if ( !m_availableISO639LangCodes.contains( iso639LangCode ) )
            iso639LangCode = "en";
        showHelpForLanguage( topic, iso639LangCode );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Launches the Windows HTML Help Viewer to show our application's help.
     *
     * @param htmlFile The HTML file to show (without the <code>.html</code>
     * extension) or <code>null</code> to show the cover page.
     * @param iso639LangCode The 2-letter ISO 639 language code.  Must be in
     */
    private static native void showHelpForLanguage( String htmlFile,
                                                    String iso639LangCode );

    /**
     * The set of languages (specified as ISO 639 language codes) the help is
     * available in.
     */
    private static final HashSet<String> m_availableISO639LangCodes =
        new HashSet<String>();

    static {
        System.loadLibrary( "Windows" );

        m_availableISO639LangCodes.add( "en" ); // English
        m_availableISO639LangCodes.add( "da" ); // Danish
        m_availableISO639LangCodes.add( "nl" ); // Dutch
        m_availableISO639LangCodes.add( "fr" ); // French
        m_availableISO639LangCodes.add( "it" ); // Italian
        m_availableISO639LangCodes.add( "ja" ); // Japanese
        m_availableISO639LangCodes.add( "es" ); // Spanish
    }
}
/* vim:set et sw=4 ts=4: */
