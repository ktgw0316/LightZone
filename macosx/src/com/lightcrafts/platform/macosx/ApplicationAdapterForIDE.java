/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.io.File;

import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

import com.lightcrafts.app.Application;

/**
 * A <code>ApplicationAdapterForIDE</code> is-an {@link ApplicationAdapter} for
 * the LightZone application.
 * <p>
 * Note that this class is used only when running in an IDE.
 */
class ApplicationAdapterForIDE extends ApplicationAdapter {

    ////////// public /////////////////////////////////////////////////////////

    public void handleAbout( ApplicationEvent event ) {
        event.setHandled( true );
        Application.showAbout();
    }

    public void handleOpenApplication( ApplicationEvent event ) {
        // do nothing
    }

    public void handleOpenFile( ApplicationEvent event ) {
        final File file = new File( event.getFilename() );
        if ( file.exists() && file.isFile() ) {
            event.setHandled( true );
            Application.open( null, file );
        }
    }

    public void handlePreferences( ApplicationEvent event ) {
        event.setHandled( true );
        Application.showPreferences();
    }

    public void handleQuit( ApplicationEvent event ) {
        event.setHandled( false );
        Application.quit();
    }

    ////////// package ////////////////////////////////////////////////////////

    static synchronized void initialize() {
        if ( INSTANCE == null ) {
            INSTANCE = new ApplicationAdapterForIDE();
            final com.apple.eawt.Application app =
                com.apple.eawt.Application.getApplication();
            app.addApplicationListener( INSTANCE );
            app.addPreferencesMenuItem();
            app.setEnabledPreferencesMenu( true );
        }
    }

    ////////// private ////////////////////////////////////////////////////////

    private ApplicationAdapterForIDE() {
        // do nothing
    }

    private static ApplicationAdapterForIDE INSTANCE;
}
/* vim:set et sw=4 ts=4: */
