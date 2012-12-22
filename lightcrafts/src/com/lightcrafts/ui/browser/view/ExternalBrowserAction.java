/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import java.io.File;

/**
 * Implementations of BrowserAction are provided by a ActionProvider to an
 * AbstractImageBrowser and used to configure popup menu items.
 */
public interface ExternalBrowserAction {

    String getName();

    /**
     * The action's menu item was selected with the given selected files
     * and the given lead selected File.
     */
    void actionPerformed(File file, File[] files);
}
