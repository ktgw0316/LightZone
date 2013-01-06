/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import java.awt.EventQueue;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.ResourceBundle;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.platform.ProgressDialog;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.*;
import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;

import static com.lightcrafts.license.ESDLicenseCheckerModule.trialLicenseRefDate;
import static com.lightcrafts.license.ESDLicenseLibrary.LicenseType.*;
import static com.lightcrafts.license.ESDLicenseManager.TRIAL_LICENSE_DURATION;
import static com.lightcrafts.license.Locale.LOCALE;

/**
 * This class is a Java wrapper around the eSellerate license library.
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 */
final class eSellerateLicenseLibrary implements ESDLicenseLibrary {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public ESDError activateKey( String key ) {
        //
        // Activation of a licence key takes several seconds, so we show the
        // user an indeterminate progress dialog and check to see if the
        // ActivateThread threw an exception.
        //
        final ProgressDialog pd = Platform.getPlatform().getProgressDialog();
        final ActivateThread at = new ActivateThread( pd, key, true );
        pd.showProgress(
            null, at, LOCALE.get( "ActivatingSerialNumberMessage" ), false
        );
        final Throwable t = pd.getThrown();
        if ( t != null )
            if ( t instanceof RuntimeException )
                throw (RuntimeException)t;
            else
                throw new RuntimeException( t );
        if ( at.m_errorCode != ES_NO_ERROR )
            return new eSellerateError( at.m_errorCode );
        return NO_ESD_ERROR;
    }

    /**
     * {@inheritDoc}
     */
    public ESDError buyNow() {
        WebBrowser.browse( m_properties.getString( "BuyNowURL" ) );
        return NO_ESD_ERROR;
    }

    /**
     * {@inheritDoc}
     */
    public ESDError deactivateKey( String key ) {
        //
        // Deactivation of a licence key takes several seconds, so we show the
        // user an indeterminate progress dialog and check to see if the
        // ActivateThread threw an exception.
        //
        final ProgressDialog pd = Platform.getPlatform().getProgressDialog();
        final ActivateThread at = new ActivateThread( pd, key, false );
        pd.showProgress(
            null, at, LOCALE.get( "DeactivatingSerialNumberMessage" ), false
        );
        final Throwable t = pd.getThrown();
        if ( t != null )
            if ( t instanceof RuntimeException )
                throw (RuntimeException)t;
            else
                throw new RuntimeException( t );
        if ( at.m_errorCode != ES_NO_ERROR )
            return new eSellerateError( at.m_errorCode );

        getLicenseFileFor( "LightZone-RT", null ).delete();
        try {
            writeTrialLicenseFile( 0 ); // expired trial license
            return NO_ESD_ERROR;
        }
        catch ( IOException e ) {
            return new eSellerateError( LC_WRITE_LICENSE_KEY_FAILED );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    public String getErrorMessage( ESDError error ) {
        final int errorCode = ((eSellerateError)error).m_errorCode;
        final String msg = ESgetErrorMessage( errorCode );
        if ( msg == null )
            return LOCALE.get( "UnknownError", errorCode );
        if ( msg.startsWith( "LOCALIZED:" ) )
            return msg.substring( 10 );
        return LOCALE.get( msg );
    }

    /**
     * {@inheritDoc}
     */
    public String getLicenseKey() {
        return m_licenseKey;
    }

    /**
     * {@inheritDoc}
     */
    public Pattern getLicenseKeyPattern() {
        return LICENSE_KEY_PATTERN;
    }

    /**
     * {@inheritDoc}
     */
    public LicenseType getLicenseType() {
        return m_licenseType;
    }

    /**
     * {@inheritDoc}
     */
    public Date getTrialLicenseExpirationDate() {
        if ( m_licenseType != LICENSE_TRIAL )
            return null;
        if ( m_trialLicenseStart * 1000 > trialLicenseRefDate.getTime() )
            return new Date( 0 );
        final long expiration = m_trialLicenseStart + TRIAL_LICENSE_DURATION;
        return new Date( expiration * 1000 );
    }

    /**
     * {@inheritDoc}
     */
    public ESDError initialize() {
        final int errorCode = ESinitialize();
        if ( errorCode != ES_NO_ERROR )
            return new eSellerateError( errorCode );
        return NO_ESD_ERROR;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isBasicKey( String key ) {
        return isKey3Basic( key );
    }

    /**
     * {@inheritDoc}
     */
    public boolean isKeyActivated( String key ) {
        final int errorCode = ESvalidateActivation( key );
        return errorCode == ES_NO_ERROR;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isKeyValid( String key ) {
        if ( !key.startsWith( "LZ3" ) || !isKeyValidImpl( key ) )
            return false;
        if ( !isKeyForUpgrade( key ) )
            return true;

        //
        // Check to see if the user is upgrading from the Roxio edition.
        //
        final DummyLicenseLibrary dll = new DummyLicenseLibrary();
        final String roxioKey = dll.getLicenseKey();
        if ( roxioKey != null && isKeyAnyBasicUpgradeToFull( key ) )
            return true;

        //
        // Check to see if the user has a previously activated license from
        // LightZone 3.0.
        //
        if ( getLicenseType() == LICENSE_NORMAL ) {
            //noinspection RedundantIfStatement
            if ( isKey3Basic( getLicenseKey() ) &&
                 !isKeyAnyBasicUpgradeToBasic( key ) &&
                 !isKeyAnyBasicUpgradeToFull( key ) )
                return false;
            return true;
        }

        //
        // Check to see if the user has a previously activated license from an
        // earlier version of LightZone.
        //
        final byte[] buf;
        try {
            final File licenseFile = getLicenseFile( true, null );
            buf = readLicenseFile( licenseFile );
        }
        catch ( IOException e ) {
            return false;
        }
        if ( buf == null || buf[0] != 'N' )
            return false;
        final String oldKey = decodeNormalLicense( buf );
        if ( isKeyPre3Basic( oldKey ) && isKeyPre3FullUpgradeToFull( key ) )
            return false;
        return isKeyValidImpl( oldKey ) /* && isKeyActivated( oldKey ) */;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInternetError( ESDError error ) {
        if ( error instanceof eSellerateError )
            return ESisInternetError( ((eSellerateError)error).m_errorCode );
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public ESDError manuallyActivateKey( String key ) {
        final int errorCode = ESmanualActivateSerialNumber( key );
        if ( errorCode != ES_NO_ERROR )
            return new eSellerateError( errorCode );
        return NO_ESD_ERROR;
    }

    /**
     * {@inheritDoc}
     */
    public ESDError probeLicenseType() {
        System.err.println( "probeLicenseType()" );
        m_licenseType = LICENSE_INVALID;

        final byte[] buf;
        try {
            final File licenseFile =
                getLicenseFile( true, LZ3_LICENSE_FILE_SUFFIX );
            buf = readLicenseFile( licenseFile );
        }
        catch ( IOException e ) {
            return new eSellerateError( LC_INVALID_LICENSE_FILE );
        }
        if ( buf == null ) {
            //
            // Even though we didn't get a license key, the call to this
            // method succeeded, so return success.
            //
            return NO_ESD_ERROR;
        }

        switch ( buf[0] ) {

            case 'T':   // trial license
                System.err.println( "  trial license" );
                switch ( buf.length ) {

                    case TRIAL_LICENSE_SIZE:
                        final ByteBuffer bb = ByteBuffer.wrap( buf );
                        bb.order( ByteOrder.nativeOrder() );
                        bb.get();   // skip 'T'
                        m_trialLicenseStart = bb.getInt();

                        m_licenseType = LICENSE_TRIAL;
                        break;
/*
                        final short revision = bb.getShort();
                        final long now = System.currentTimeMillis() / 1000;
                        if ( Version.getRevisionNumber() <= revision ||
                             m_trialLicenseStart + TRIAL_LICENSE_DURATION >=
                                 now + TRIAL_EXTENSION_DURATION ) {
                            m_licenseType = ESDLicenseManager.LICENSE_TRIAL;
                            break;
                        }
                        //
                        // The revision of the currently running application is
                        // greater than the one stored in the trial license and
                        // the time left on the existing trial license is less
                        // than the extension (or has expired): fall through to
                        // auto-extend it.
                        //
*/
                    case OLD_TRIAL_LICENSE_SIZE:
                        System.err.println( "  extending trial license" );
                        //
                        // We've read and old trial license file: auto-extend
                        // it.  We do this by setting the start date in the
                        // past.  This way the code that determines whether a
                        // license has expired is the same for both regular
                        // trial and extension trial licenses.
                        //
                        m_trialLicenseStart = System.currentTimeMillis() / 1000
                            - TRIAL_LICENSE_DURATION + TRIAL_EXTENSION_DURATION;
                        try {
                            writeTrialLicenseFile( m_trialLicenseStart );
                        }
                        catch ( IOException e ) {
                            return new eSellerateError( LC_WRITE_LICENSE_KEY_FAILED );
                        }
                        m_licenseType = LICENSE_TRIAL;
                        break;

                    default:
                        System.err.println( "bad byte-count" );
                        return new eSellerateError( LC_INVALID_LICENSE_FILE );
                }
                break;

            case 'N':   // normal license
                m_licenseKey = decodeNormalLicense( buf );
                m_licenseType = LICENSE_NORMAL;
                System.err.println(
                    "  normal license: key = \"" + m_licenseKey + '"'
                );
                break;

            default:
                System.err.println( "  unknown license type: " + buf[0] );
                return new eSellerateError( LC_INVALID_LICENSE_FILE );
        }
        return NO_ESD_ERROR;
    }

    /**
     * {@inheritDoc}
     */
    public ESDError requestLicense() {
        // This is a no-op for eSellerate.
        return NO_ESD_ERROR;
    }

    /**
     * {@inheritDoc}
     */
    public boolean saveLicenseKey( String key ) {
        System.err.println( "saveLicenseKey()" );
        final byte[] normalLicenseBuf = encodeNormalLicense( key );
        try {
            writeLicenseFile( normalLicenseBuf );
            m_licenseKey = key;
            m_licenseType = LICENSE_NORMAL;
            return true;
        }
        catch ( IOException e ) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ESDError tryNow( String key ) {
        System.err.println( "tryNow()" );
        try {
            writeTrialLicenseFile( System.currentTimeMillis() / 1000 );
            return NO_ESD_ERROR;
        }
        catch ( IOException e ) {
            return new eSellerateError( LC_WRITE_LICENSE_KEY_FAILED );
        }
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Checks whether the key is for a basic upgrade to 3.0 basic.
     *
     * @param key The key to check.
     * @return Returns <code>true</code> only if the license is for a basic
     * upgrade to a 3.0 basic version of LightZone.
     */
    private static boolean isKeyAnyBasicUpgradeToBasic( String key ) {
        return key.startsWith( "LZ3UBB" );
    }

    /**
     * Checks whether the key is for a basic upgrade to 3.0 full.
     *
     * @param key The key to check.
     * @return Returns <code>true</code> only if the license is for a basic
     * upgrade to a 3.0 full version of LightZone.
     */
    private static boolean isKeyAnyBasicUpgradeToFull( String key ) {
        return key.startsWith( "LZ3UBF" );
    }

    /**
     * Checks whether the key is for the 3.0 basic version of LightZone.
     *
     * @param key The key to check.
     * @return Returns <code>true</code> only if the license is for the basic
     * version of LightZone.
     * @see #isKeyPre3Basic(String)
     */
    private static boolean isKey3Basic( String key ) {
        return key.startsWith( "LZ3B" ) || key.startsWith( "LZ3UBB" );
    }

    /**
     * Checks whether the key is for the pre-3.0 basic version of LightZone.
     *
     * @param key The key to check.
     * @return Returns <code>true</code> only if the license is for a pre-3.0
     * basic version of LightZone.
     * @see #isKey3Basic(String)
     */
    private static boolean isKeyPre3Basic( String key ) {
        return key.startsWith( "LZRT" );
    }

    /**
     * Checks whether the key is for a pre-3.0 full upgrade to 3.0 full.
     *
     * @param key The key to check.
     * @return Returns <code>true</code> only if the license is for a pre-3.0
     * full upgrade to a 3.0 full version of LightZone.
     */
    private static boolean isKeyPre3FullUpgradeToFull( String key ) {
        return key.startsWith( "LZ3UF" );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * An <code>ActivateThread</code> is-a {@link ProgressThread} that's used
     * either to activate or deactivate an eSellerate license key.  It takes
     * several seconds, so this is used in conjunction with an indeterminate
     * {@link ProgressDialog}.
     */
    private static final class ActivateThread extends ProgressThread {

        /**
         * Construct an <code>ActivateThread</code>.
         *
         * @param indicator The {@link ProgressIndicator} to use.
         * @param key The eSellerate license key.
         * @param activate If <code>true</code>, activate the key; if
         * <code>false</code>, deactivate it.
         */
        ActivateThread( ProgressIndicator indicator, String key,
                        boolean activate ) {
            super( indicator );
            m_activate = activate;
            m_key = key;
        }

        public void run() {
            m_errorCode = m_activate ?
                ESactivateSerialNumber( m_key ) :
                ESdeactivateSerialNumber( m_key );
        }

        int m_errorCode;
        private final boolean m_activate;
        private final String m_key;
    }

    /**
     * A <code>eSellerateError</code> is-an {@link ESDError} that contains the
     * integer error code returned by the eSellerate library.
     */
    private static final class eSellerateError extends ESDError {
        eSellerateError( int errorCode ) {
            m_errorCode = errorCode;
        }
        private int m_errorCode;
    }

    /**
     * The error code used by the eSellerate library to mean "no error."
     */
    private static final int ES_NO_ERROR = 0;

    private static final int LC_INVALID_LICENSE_FILE        = 0x4C5A01;
    private static final int LC_INVALID_LICENSE_KEY         = 0x4C4A02;
    private static final int LC_WRITE_LICENSE_KEY_FAILED    = 0x4C5A03;

    /**
     * Activate the given license key.
     *
     * @param key The license key to activate.
     * @return Returns an error code or {@link #ES_NO_ERROR} if none.
     */
    private static native int ESactivateSerialNumber( String key );

    /**
     * Dectivate the given license key.
     *
     * @param key The license key to deactivate.
     * @return Returns an error code or {@link #ES_NO_ERROR} if none.
     */
    private static native int ESdeactivateSerialNumber( String key );

    /**
     * Gets the error message for the given error code.
     *
     * @param errorCode The error code to get the error message for.
     * @return Returns said error message.
     */
    private static native String ESgetErrorMessage( int errorCode );

    /**
     * Initialize the native code.
     *
     * @return Returns an error code or {@link #ES_NO_ERROR} if none.
     */
    private static native int ESinitialize();

    /**
     * Checks whether the given error is an internet-related error.
     *
     * @param errorCode The error code to get the error message for.
     * @return Returns <code>true</code> only if the error is forgivable.
     */
    private static native boolean ESisInternetError( int errorCode );

    /**
     * Manually activate the given license key.
     *
     * @param key The license key to activate.
     * @return Returns an error code or {@link #ES_NO_ERROR} if none.
     */
    private static native int ESmanualActivateSerialNumber( String key );

    /**
     * Checks that the license key has previously been activated.
     *
     * @param key The license key.
     * @return Returns an error code or {@link #ES_NO_ERROR} if none.
     */
    private static native int ESvalidateActivation( String key );

    /**
     * Checks whether the given license key is valid.
     *
     * @param key The license key to validate.
     * @return Returns an error code or {@link #ES_NO_ERROR} if none.
     */
    private static native int ESvalidateSerialNumber( String key );

    /**
     * This is used to transcode the contents of license files.  It's the MD5
     * hash of "Quid quid latine dictum sit altum viditur".
     *
     * @see #transcode(byte[],int,int)
     */
    private static final String SALT = "0bc4717102ccf6009a1286ba46aaa8c0";

    /**
     * Decodes a license key from the bytes read from a license file.
     *
     * @param buf The bytes containing the contents of the license file.
     * @return Returns the license key.
     * @see #encodeNormalLicense(String)
     */
    private static String decodeNormalLicense( byte[] buf ) {
        transcode( buf, 1, buf.length - 1 );
        try {
            return new String( buf, 1, buf.length - 2, "ASCII" );
        }
        catch ( UnsupportedEncodingException e ) {
            throw new IllegalStateException( e );
        }
    }

    /**
     * Encode a normal license into a byte sequence that is ready to be written
     * to disk.
     *
     * @param key The license key.
     * @return Returns said byte sequence.
     * @see #decodeNormalLicense(byte[])
     */
    private static byte[] encodeNormalLicense( String key ) {
        final byte[] buf = new byte[ 1 /* 'N' */ + key.length() + 1 /* null */ ];
        final ByteBuffer bb = ByteBuffer.wrap( buf );
        bb.put( (byte)'N' );                // normal license
        ByteBufferUtil.put( bb, key, "ASCII" );
        bb.put( (byte)0 );
        transcode( buf, 1, key.length() + 1 /* null */ );
        return buf;
    }

    /**
     * Encode a trial license into a byte sequence that is ready to be written
     * to disk.
     *
     * @param startTime The start time of the trial license (in seconds since
     * epoch).
     * @return Returns said byte sequence.
     */
    private static byte[] encodeTrialLicense( long startTime ) {
        final byte[] buf = new byte[ TRIAL_LICENSE_SIZE ];
        final ByteBuffer bb = ByteBuffer.wrap( buf );
        bb.order( ByteOrder.nativeOrder() );
        bb.put( (byte)'T' );
        bb.putInt( (int)startTime );
        bb.putShort( (short)9999 );
        return buf;
    }

    /**
     * Gets the full path of the license {@link File}, creating the directory
     * it's in if necessary.
     *
     * @param checkForRT If <code>true</code>, check for the presense of an
     * existing LightZone-RT license directory.
     * @param suffix The suffix to append to the name of the license file or
     * <code>null</code> if none.
     * @return Returns the license {@link File}.
     * @throws IOException if the directory the license file is to be in can
     * not be created.
     */
    private static File getLicenseFile( boolean checkForRT, String suffix )
        throws IOException
    {
        File licenseFile =
            getLicenseFileFor( Version.getApplicationName(), suffix );

        if ( !licenseFile.exists() && checkForRT ) {
            final File rtLicenseFile =
                getLicenseFileFor( "LightZone-RT", null );
            FileInputStream fis = null;
            try {
                fis =  new FileInputStream( rtLicenseFile );
                final byte[] licenseBuf = new byte[ 1 ];
                final int bytesRead = fis.read( licenseBuf );
                if ( bytesRead != 0 && licenseBuf[0] == 'N' )
                    licenseFile = rtLicenseFile;
            }
            catch ( IOException e ) {
                // ignore
            }
            finally {
                if ( fis != null ) {
                    try {
                        fis.close();
                    }
                    catch ( IOException e ) {
                        // ignore
                    }
                }
            }
        }

        final File licenseDir = licenseFile.getParentFile();
        if ( !licenseDir.exists() && !licenseDir.mkdirs() ) {
            System.err.println( "  couldn't get license file" );
            throw new IOException( "Could not create license folder." );
        }
        System.err.println( "  license file = \"" + licenseFile + '"' );
        return licenseFile;
    }

    /**
     * Gets the full path to the license file for the given application.
     *
     * @param appName The name of the application.
     * @param suffix The suffix to append to the name of the license file or
     * <code>null</code> if none.
     * @return Returns the license directory.
     */
    private static File getLicenseFileFor( String appName, String suffix ) {
        final StringBuilder sb = new StringBuilder();
        if ( Platform.getType() == Platform.Windows ) {
            sb.append( System.getenv( "APPDATA" ) );
        } else {
            sb.append( System.getProperty( "user.home" ) );
            sb.append( "/Library/Application Support" );
        }
        sb.append( File.separatorChar );
        sb.append( appName );
        sb.append( File.separatorChar );
        sb.append( "License" );
        sb.append( File.separatorChar );
        sb.append( "ESLF" );
        if ( suffix != null )
            sb.append( suffix );
        return new File( sb.toString() );
    }

    /**
     * Checks whether the given key is for an upgrade.
     *
     * @return Returns <code>true</code> only if the key is for an upgrade.
     */
    private static boolean isKeyForUpgrade( String key ) {
        return key.startsWith( "LZ3U" );
    }

    /**
     * Checks whether the given license key is valid according to eSellerate.
     *
     * @param key The license key.
     * @return Returns <code>true</code> only if the key is valid.
     */
    private static boolean isKeyValidImpl( String key ) {
        final int errorCode = ESvalidateSerialNumber( key );
        return errorCode == ES_NO_ERROR;
    }

    /**
     * Read the given license file.
     *
     * @param file The license {@link File} to read.
     * @return Returns the raw bytes of the license file or <code>null</code>
     * if the file does not exist.
     */
    private static byte[] readLicenseFile( File file ) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream( file );
            final byte[] licenseBuf = new byte[ 64 ];
            final int bytesRead = fis.read( licenseBuf );
            System.err.println( "  bytes read = " + bytesRead );
            if ( bytesRead != 0 )
                return (byte[])LCArrays.resize( licenseBuf, bytesRead );
        }
        catch ( FileNotFoundException e ) {

        }
        finally {
            if ( fis != null )
                fis.close();
        }
        return null;
    }

    /**
     * For some measure of obfuscation, we encode the eSellerate license key.
     * The encoding scheme has to be two-way, i.e., it has to be able to be
     * decoded.
     *
     * @param buf The buffer to be either encoded or decoded.
     * @param offset The offset of the first byte to transcode.
     * @param length The number of bytes to transcode.
     */
    private static void transcode( byte[] buf, int offset, int length ) {
        for ( int i = 0; i < length; ++i )
            buf[ i + offset ] ^= SALT.charAt( i % SALT.length() );
    }

    /**
     * Write a license file to disk.
     *
     * @param licenseBuf The raw bytes of the license file.
     */
    private static void writeLicenseFile( byte[] licenseBuf )
        throws IOException
    {
        System.err.println( "writeLicenseFile()" );
        final File licenseFile =
            getLicenseFile( false, LZ3_LICENSE_FILE_SUFFIX );
        final FileOutputStream fos = new FileOutputStream( licenseFile );
        IOException caughtException = null;
        try {
            fos.write( licenseBuf );
        }
        catch ( IOException e ) {
            caughtException = e;
        }
        try {
            fos.close();
        }
        catch ( IOException e ) {
            if ( caughtException == null )
                caughtException = e;
        }
        if ( caughtException != null ) {
            System.err.println( "  write failed; removing now-bad license file" );
            licenseFile.delete();
            throw caughtException;
        }
        System.err.println( "  write succeeded" );
    }

    /**
     * Write the trial license file.
     *
     * @param startTime The start time of the trial license (in seconds since
     * epoch).
     */
    static void writeTrialLicenseFile( long startTime ) throws IOException {
        System.err.println( "writeTrialLicenseFile()" );
        final byte[] trialLicenseBuf = encodeTrialLicense( startTime );
        writeLicenseFile( trialLicenseBuf );
    }

    /**
     * The eSellerate license key.
     */
    private String m_licenseKey;

    /**
     * The license type.
     */
    private LicenseType m_licenseType;

    /**
     * The start time of the trial license (in seconds since epoch).
     */
    private long m_trialLicenseStart;

    /**
     * The {@link Pattern} (as a {@link String}) for an eSellerate serial
     * number.
     */
    static final String LICENSE_KEY_PATTERN_STRING =
        "\\w{10}(?:-[A-HJ-NP-TV-Y0-9]{4}){5}";

    /**
     * The {@link Pattern} for an eSellerate serial number.
     */
    static final Pattern LICENSE_KEY_PATTERN =
        Pattern.compile( LICENSE_KEY_PATTERN_STRING );

    /**
     * The license file suffix used for LightZone 3.0.
     */
    private static final String LZ3_LICENSE_FILE_SUFFIX = "3";

    /**
     * The number of days of a trial license extension (in seconds).
     */
    private static final long TRIAL_EXTENSION_DURATION =
        60*60*24 /* seconds/day */ * 7 /* days */;

    /**
     * The number of bytes in an old trial license file (before the revision
     * number was included to enable auto-extensions).
     */
    private static final int OLD_TRIAL_LICENSE_SIZE =
        1 /* type: 'T' or 'N' */ + 4 /* sizeof(time_t) */;

    /**
     * The number of bytes in a trial license file.
     */
    private static final int TRIAL_LICENSE_SIZE =
        OLD_TRIAL_LICENSE_SIZE + 2 /* revision number */;

    private static final ResourceBundle m_properties =
        ResourceBundle.getBundle(
            "com.lightcrafts.license.resources.eSellerate"
        );

    static {
        System.loadLibrary( "ESD" );
    }

    ////////// main() for testing /////////////////////////////////////////////

    public static void main( String[] args ) {
        try {
            EventQueue.invokeAndWait(
                new Runnable() {
                    public void run() {
                        final ESDLicenseLibrary lib =
                            new eSellerateLicenseLibrary();
                        final ESDLicenseDialogs dialogs =
                            new eSellerateLicenseDialogs();
                        if ( ESDLicenseManager.getLicense( lib, dialogs ) ) {
                            // run LightZone
                        }
                        lib.dispose();
                        System.exit( 0 );
                    }
                }
            );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
/* vim:set et sw=4 ts=4: */
