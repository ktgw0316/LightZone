/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2023-     Masahiro Kitagawa */

package com.lightcrafts.platform.windows;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Set;
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
        if (topic == null) {
            topic = "index";
        }
        showHelpForLanguage( topic, iso639LangCode );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Launches the Windows HTML Help Viewer to show our application's help.
     *
     * @param topic The topic to show (without the <code>.html</code> extension)
     * @param iso639LangCode The 2-letter ISO 639 language code.
     */
    private static void showHelpForLanguage(@NotNull String topic, String iso639LangCode) {
        final var chm = "LightZone-" + iso639LangCode + ".chm";
        final var html = "LightZone_Help/" + topic + ".html";
        final var param = chm + "::/" + html;
        final String[] cmd = {"hh.exe", param};
        try {
            new ProcessBuilder(cmd).start();
        } catch (IOException e) {
            System.err.println("Unable to launch help viewer for " + param + ": " + e);
        }
    }

    /**
     * The set of languages (specified as ISO 639 language codes) the help is
     * available in.
     */
    private static final Set<String> m_availableISO639LangCodes = Set.of(
            "en", // English
            "da", // Danish
            "nl", // Dutch
            "fr", // French
            "it", // Italian
            "ja", // Japanese
            "es"  // Spanish
    );
}
/* vim:set et sw=4 ts=4: */
