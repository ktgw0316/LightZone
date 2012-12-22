/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import java.io.File;
import java.util.List;

/**
 * AbstractImageBrowser allows a provider to specify a list of Objects that
 * can be applied in the context of one or more image Files.
 */
public interface TemplateProvider {

    /**
     * Return a list of action Objects, to populate for instance a context
     * sensitive popup menu of image thumbnails.
     */
    List getTemplateActions();

    /**
     * Apply one of the Objects from the List to an array of image Files.
     */
    void applyTemplateAction(Object action, File[] targets);

    /**
     * Apply a template defined by the given File to the array of image Files.
     */
    void applyTemplate(File file, File[] targets);
}
