/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.splash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To get startup progress feedback from Application, override this class and
 * call Application.setStartupProgress().
 */

public class StartupProgress {

    private static final Logger logger = LoggerFactory.getLogger(StartupProgress.class);

    public void startupMessage(String text) {
        logger.info(text);
    }
}
