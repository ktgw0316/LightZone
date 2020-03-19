/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.ui.editor.Document;

interface DocumentInitializerListener {

    void documentStarted();

    void documentInitialized(Document doc);

    void documentCancelled();

    void documentFailed(Throwable t);
}
