/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2014-     Masahiro Kitagawa */

package com.lightcrafts.utils;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Browser launcher, just a wrapper to java.awt.Desktop.browse().
 */
public final class WebBrowser {

    /**
     * Direct the user's default web browser to browse the given URL.
     *
     * @param url The URL to browse.
     * @return Returns <code>true</code> if the browser was successfully
     * launched.
     */
    public static boolean browse(String url) {
        try {
            return browse(new URI(url));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Direct the user's default web browser to browse the given URL.
     *
     * @param uri The URI to browse.
     * @return Returns <code>true</code> if the browser was successfully
     * launched.
     */
    public static boolean browse(URI uri) {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        final Desktop desktop = Desktop.getDesktop();
        if(!desktop.isSupported(Desktop.Action.BROWSE)) {
            return false;
        }

        try {
            desktop.browse(uri);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
/* vim:set et sw=4 ts=4: */
