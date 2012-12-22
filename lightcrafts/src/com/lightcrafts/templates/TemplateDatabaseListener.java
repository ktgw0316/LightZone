/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.templates;

/**
 * Users of the TemplateDatabase can subscribe to learn when the set of
 * Templates may have changed.
 */
public interface TemplateDatabaseListener {

    /**
     * For some reason, the TemplateDatabase thinks that if you call
     * getTemplateKeys() now, you may get a different answer than you got
     * before.
     */
    void templatesChanged();
}
