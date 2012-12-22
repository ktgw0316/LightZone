/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageList;

import java.util.prefs.Preferences;

/**
 * A factory that makes both expanded and collapsed browsers, and also makes
 * default browsers to match the most recently requested type.
 */
public class BrowserFactory {

    // The default default
    private final static boolean IsDefaultBrowserCollapsed = false;

    private final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/ui/browser/view"
    );
    private final static String CollapsedKey = "BrowserCollapsed";

    public static AbstractImageBrowser createExpanded(ImageList images) {
        Prefs.putBoolean(CollapsedKey, false);
        return new ExpandedImageBrowser(images);
    }

    public static AbstractImageBrowser createCollapsed(ImageList images) {
        Prefs.putBoolean(CollapsedKey, true);
        return new CollapsedImageBrowser(images);
    }

    public static AbstractImageBrowser createRecent(ImageList images) {
        return isDefaultCollapsed() ?
            createCollapsed(images) : createExpanded(images);
    }

    public static boolean isDefaultCollapsed() {
        return Prefs.getBoolean(CollapsedKey, IsDefaultBrowserCollapsed);
    }

    public static boolean isCollapsed(AbstractImageBrowser browser) {
        return (browser instanceof CollapsedImageBrowser);
    }

    public static void dispose(AbstractImageBrowser browser) {
        browser.dispose();
    }
}
