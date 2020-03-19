/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor.assoc;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Saved document files include a pointer to an original image file. The
 * DocumentDatabase needs to know how to unpack such pointers.  Service
 * providers can implement this interface and extend the way the pointers are
 * extracted.
 * <p>
 * @see DocumentDatabase#addDocumentInterpreter
 */
public interface DocumentInterpreter {

    /**
     * Identify the image file pointer contained in the given file, or return
     * null if no pointer can be identified.
     * <p>
     * It is possible that LZN data exists but no image file can be found.
     * This is typical when files have been moved around in the file system,
     * and with "LZT" (template) files, for instance.
     */
    File getImageFile(File file) throws IOException;

    /**
     * Files are filtered by name suffix in the DocumentDatabase, to minimize
     * the number of calls to interpret().
     */
    Collection<String> getSuffixes();
}
