/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.file.FileUtil;

import static com.lightcrafts.license.ESDLicenseLibrary.LicenseType.*;
import static com.lightcrafts.license.eSellerateLicenseLibrary.LICENSE_KEY_PATTERN;
import static com.lightcrafts.license.eSellerateLicenseLibrary.LICENSE_KEY_PATTERN_STRING;

/**
 * A {@code MakeItWorkLicenseLibrary} is-an {@link ESDLicenseLibrary} that just
 * makes it work by the presence of a special file.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class MakeItWorkLicenseLibrary implements ESDLicenseLibrary {

    ////////// public /////////////////////////////////////////////////////////

    public ESDError activateKey( String key ) {
        return NO_ESD_ERROR;
    }

    public ESDError buyNow() {
        return null;
    }

    public ESDError deactivateKey( String key ) {
        return NO_ESD_ERROR;
    }

    public void dispose() {
        // nothing
    }

    public String getErrorMessage( ESDError error ) {
        final Throwable t = ((MIWError)error).m_throwable;
        return t.getMessage();
    }

    public String getLicenseKey() {
        return m_key;
    }

    public Pattern getLicenseKeyPattern() {
        return LICENSE_KEY_PATTERN;
    }

    public LicenseType getLicenseType() {
        return LICENSE_NORMAL;
    }

    public Date getTrialLicenseExpirationDate() {
        return null;
    }

    public ESDError initialize() {
        try {
            final File miwFile = getMakeItWorkFile();
            final String s = FileUtil.readEntireFile( miwFile ).trim();
            final Matcher m = MIW_FILE_PATTERN.matcher( s );
            if ( !m.matches() )
                throw new IOException( "Corrupt MIW file" );
            m_key = m.group( 1 );
            return NO_ESD_ERROR;
        }
        catch ( IOException e ) {
            return new MIWError( e );
        }
    }

    public boolean isBasicKey( String key ) {
        return false;
    }

    public boolean isKeyActivated( String key ) {
        return true;
    }

    public boolean isInternetError( ESDError error ) {
        return false;
    }

    public boolean isKeyValid( String key ) {
        return LICENSE_KEY_PATTERN.matcher( key ).matches();
    }

    public ESDError manuallyActivateKey( String key ) {
        return NO_ESD_ERROR;
    }

    public ESDError probeLicenseType() {
        return NO_ESD_ERROR;
    }

    public ESDError requestLicense() {
        return NO_ESD_ERROR;
    }

    public boolean saveLicenseKey( String key ) {
        return true;
    }

    public ESDError tryNow( String key ) {
        return NO_ESD_ERROR;
    }

    ////////// package ////////////////////////////////////////////////////////

    static boolean fileExists() {
        final File miwFile = getMakeItWorkFile();
        System.out.println( "MIW file = " + miwFile.getAbsolutePath() );
        final boolean miwExists = miwFile.exists();
        if ( miwExists )
            System.out.println( "=> DETECTED" );
        else
            System.out.println( "=> NOT DETECTED" );
        return miwExists;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A <code>eSellerateError</code> is-an {@link ESDError} that contains the
     * integer error code returned by the eSellerate library.
     */
    private static final class MIWError extends ESDError {
        MIWError( Throwable t ) {
            m_throwable = t;
        }
        private Throwable m_throwable;
    }

    /**
     * Gets the full path to the license file for the given application.
     *
     * @return Returns the license directory.
     */
    private static File getMakeItWorkFile() {
        final StringBuilder sb = new StringBuilder();
        if ( Platform.getType() == Platform.Windows ) {
            sb.append( System.getenv( "APPDATA" ) );
        } else {
            sb.append( System.getProperty( "user.home" ) );
            sb.append( "/Library/Application Support" );
        }
        sb.append( File.separatorChar );
        sb.append( Version.getApplicationName() );
        sb.append( File.separatorChar );
        sb.append( "License" );
        sb.append( File.separatorChar );
        sb.append( "MIW" );
        return new File( sb.toString() );
    }

    private static final Pattern MIW_FILE_PATTERN =
        Pattern.compile( "# [^ @]+@[^ @]+ (" + LICENSE_KEY_PATTERN_STRING + ')' );

    private String m_key;
}
/* vim:set et sw=4 ts=4: */
