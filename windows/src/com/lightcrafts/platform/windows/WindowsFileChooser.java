/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import com.lightcrafts.platform.DefaultFileChooser;

/**
 * TODO
 */
public class WindowsFileChooser extends DefaultFileChooser {

    public File openFile( String windowTitle, File directory, Frame parent ) {
        if ( directory == null )
            directory = new File( System.getProperty( "user.home" ) );
        String fileName;
        try {
            fileName = WindowsFileUtil.openFile( directory.getAbsolutePath() );
        }
        catch ( IOException e ) {
            fileName = null;
        }
        if ( fileName == null )
            return null;
        return new File( fileName );
    }

}
/* vim:set et sw=4 ts=4: */
