/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.splash;

/**
 * To get startup progress feedback from Application, override this class and
 * call Application.setStartupProgress().
 */

public class StartupProgress {

    public void startupMessage(String text) {
        System.out.println(text);
    }
}
