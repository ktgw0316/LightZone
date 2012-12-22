/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.xml;

import java.io.IOException;

/** This is a checked exception for users of XmlDocument that do reading.
  * Since we don't have schemas (or any other kind of type safety), any
  * document format surprises should be communicated by one of these.
  */
public final class XMLException extends IOException {

    public XMLException( String message ) {
        super( message );
    }

    public XMLException( Throwable cause ) {
        initCause( cause );
    }

    public XMLException( String message, Throwable cause ) {
        super( message );
        initCause( cause );
    }
}
/* vim:set et sw=4 ts=4: */
