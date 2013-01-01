/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.lightcrafts.license.LicenseChecker;
import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.platform.ProgressDialog;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.xml.ElementFilter;
import com.lightcrafts.utils.xml.XMLUtil;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.WebBrowser;

import static com.lightcrafts.app.Locale.LOCALE;

/**
 * <code>CheckForUpdate</code> checks to see if an update of the software is
 * available.  There are two seperate methods: asynchronous and synchronous.
 * <p>
 * The asynchronous method follows the pattern:
 * <pre>
 *  CheckForUpdate.start();
 *  // ... do other application initialization here ...
 *  CheckForUpdate.showAlertIfAvailable();
 * </pre>
 * The synchronous method is simply the call to:
 * <pre>
 *  CheckForUpdate.checkNowAndWait();
 * </pre>
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class CheckForUpdate {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Check for an update now and wait until the check is done.  This method
     * displays all necessary alerts and dialogs to the user.
     */
    public static void checkNowAndWait() {
        //
        // First, check to see if the computer has an internet connection.
        //
        if ( !hasInternetConnection() ) {
            final AlertDialog alert = Platform.getPlatform().getAlertDialog();
            alert.showAlert(
                null,
                LOCALE.get( "NoInternetConnectionErrorMajor" ),
                LOCALE.get( "NoInternetConnectionErrorMinor" ),
                AlertDialog.ERROR_ALERT,
                LOCALE.get( "CheckForUpdateOKButton" )
            );
            return;
        }

        //
        // Second, check to see if we previosuly determined that an update was
        // available.  If so, we don't need to check again.
        //
        if ( isUpdateAvailable( true ) ) {
            showDownloadNowOrCancelAlert();
            return;
        }

        //
        // Finally, put up a ProgressDialog and check to see if there is an
        // update available.
        //
        final ProgressDialog progress =
            Platform.getPlatform().getProgressDialog();
        final SynchronousCheckThread checkThread =
            new SynchronousCheckThread( progress );
        progress.showProgress(
            null, checkThread, LOCALE.get( "CheckingForUpdateMessage" ), false
        );
        final Throwable t = progress.getThrown();
        if ( t != null ) {
            final AlertDialog alert = Platform.getPlatform().getAlertDialog();
            alert.showAlert(
                null,
                LOCALE.get( "CheckingForUpdateProblem" ),
                t.getLocalizedMessage(),
                AlertDialog.ERROR_ALERT,
                LOCALE.get( "CheckForUpdateOKButton" )
            );
            return;
        }
        if ( checkThread.isUpdateAvailable() )
            showDownloadNowOrCancelAlert();
        else
            showNoUpdateIsAvailableAlert();
    }

    /**
     * Shows an alert to the user only if a new version is available.  It is
     * presumed that {@link #start()} was previously called.
     * <p>
     * Note that if automatic updates checks have been disabled via the user
     * preference, this method will do nothing.
     */
    public static void showAlertIfAvailable() {
        if ( shouldCheckForUpdate() && isUpdateAvailable( true ) )
            showDownloadNowOrLaterAlert();
    }

    /**
     * Starts an asynchronous check for an update.  At some later time, call
     * {@link #showAlertIfAvailable()}.
     * <p>
     * Note that if automatic updates checks have been disabled via the user
     * preference, this method will do nothing.
     */
    public static void start() {
        if ( !shouldCheckForUpdate() )
            return;
        //
        // First, check to see if the user asked to be reminded later.  If so,
        // see if it's "later" yet.  If not, don't even bother checking to see
        // if an update is available.
        //
        if ( remindLaterAndIsNotLater() )
            return;

        //
        // Second, check to see if we previosuly determined that an update was
        // available.  If so, we don't need to check again.
        //
        if ( isUpdateAvailable( false ) )
            return;

        //
        // Finally, check to see if an update is available.  Since this
        // involves internet access, it could potentially take a while.  Hence,
        // spawn this part in a seperate thread and return now.
        //
        new BackgroundCheckThread().start();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A <code>BackgroundCheckThread</code> is-a {@link Thread} that checks to
     * see if a new version of the software is available by fetchcing the
     * latest version information from the Light Crafts' version server.
     * <p>
     * Since this runs in the background, all exceptions are caught and
     * ignored.
     */
    private static final class BackgroundCheckThread extends Thread {

        ////////// public /////////////////////////////////////////////////////

        public void run() {
            //
            // First, check to see if this computer has an active internet
            // connection.  If not, skip the check because we don't want to
            // force the computer to connect, especially using dial-up.
            //
            if ( !hasInternetConnection() )
                return;

            try {
                //
                // Now actually check to see if an update is available.
                //
                if ( !checkIfUpdateIsAvailable() )
                    return;
            }
            catch ( Throwable t ) {
                return;
            }

            //
            // An update is available, but check to see if it's the same
            // version that we told the user about last time.  If so, don't
            // tell the user about it again.  (This is for the "Don't remind
            // me" case.)
            //
            try {
                final String lastVersion = m_prefs.get( LAST_VERSION_KEY, "" );
                if ( lastVersion.equals( m_updateVersion ) )
                    return;
                //
                // An update is available and we should tell the user about it.
                //
                m_prefs.putBoolean( IS_UPDATE_AVAILABLE_KEY, true );
                syncPrefs();
            }
            catch ( IllegalStateException e ) {
                // ignore?
            }
        }

        ////////// package ////////////////////////////////////////////////////

        /**
         * Construct a <code>BackgroundCheckThread</code>.
         */
        BackgroundCheckThread() {
            super( "CheckForUpdate.BackgroundCheckThread" );
            setDaemon( true );
        }
    }

    /**
     * A <code>SynchronousCheckThread</code> is-a {@link Thread} that checks to
     * see if a new version of the software is available by fetchcing the
     * latest version information from the Light Crafts' version server.
     * <p>
     * Since this runs synchronously, any exception is reported via
     * {@link ProgressDialog#getThrown()}.
     */
    private static final class SynchronousCheckThread extends ProgressThread {

        ////////// public /////////////////////////////////////////////////////

        public void run() {
            try {
                m_isUpdateAvailable = checkIfUpdateIsAvailable();
            }
            catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }

        ////////// package ////////////////////////////////////////////////////

        /**
         * Construct a <code>SynchronousCheckThread</code>.
         *
         * @param indicator The {@link ProgressIndicator} to use.
         */
        SynchronousCheckThread( ProgressIndicator indicator ) {
            super( indicator );
        }

        /**
         * Returns whether an update is available.
         *
         * @return Returns <code>true</code> only if an update is available.
         */
        boolean isUpdateAvailable() {
            return m_isUpdateAvailable;
        }

        ////////// private ////////////////////////////////////////////////////

        private boolean m_isUpdateAvailable;
    }

    /**
     * A <code>VersionFilter</code> is-an {@link ElementFilter} that matches
     * a <code>version</code> element having matching <code>product</code>,
     * <code>platform</code>, and <code>customer</code> attributes.
     * All comparisons are case-insensitive.
     * <p>
     * The <code>product</code> attribute matches if the value is one of empty,
     * &quot;all&quot;, &quot;any&quot;, &quot;basic&quot;, or &quot;full&quot;.
     * <p>
     * The <code>platform</code> attribute matches if it is contained in the
     * operating system name or the value is one of empty, &quot;all&quot;, or
     * &quot;any&quot;.
     */
    private static final class VersionFilter extends ElementFilter {

        ////////// public /////////////////////////////////////////////////////

        public boolean accept( Node node ) {
            if ( !super.accept( node ) )
                return false;
            final Element element = (Element)node;

            final String product =
                element.getAttribute( "product" ).toLowerCase();
            final boolean productMatches = isAny( product ) ||
                product.equals( LicenseChecker.isBasic() ? "basic" : "full" );
            if ( !productMatches )
                return false;

            final String platform =
                element.getAttribute( "platform" ).toLowerCase();
            final boolean platformMatches = isAny( platform ) ||
                m_osName.contains( platform );
            if ( !platformMatches )
                return false;

            return true;
        }

        ////////// package ////////////////////////////////////////////////////

        VersionFilter() {
            super( "version", "customer", CUSTOMER );
        }

        ////////// private ////////////////////////////////////////////////////

        /**
         * Checks whether the given string matches either &quot;any&quot; or
         * &quot;all&quot;.
         *
         * @param s The string to check.  It is assumed to have been converted
         * to lower case.
         * @return Returns <code>true</code> only if the string is one of
         * empty, &quot;any&quot; or &quot;all&quot;.
         */
        private static boolean isAny( String s ) {
            return s.length() == 0 || s.equals( "all" ) || s.equals( "any" );
        }


        private static final String m_osName =
            System.getProperty( "os.name" ).toLowerCase();
    }

    /**
     * Checks if an update is available by fetching the latest version
     * information from the Light Crafts' version server.
     *
     * @return Returns <code>true</code> only if an update is available.
     */
    private static boolean checkIfUpdateIsAvailable() throws IOException {
        return false;
    }

    /**
     * Gets the message that is to be shown to the user when an update is
     * available.
     *
     * @return Returns said message.
     */
    private static String getUpdateIsAvailableMessage() {
        return LOCALE.get(
            "UpdateIsAvailableMessage", m_updateVersion,
            Version.getApplicationName()
        );
    }

    /**
     * Checks whether this computer has an active internet connection.
     *
     * @return Returns <code>true</code> only if this computer has an active
     * internet connection.
     */
    private static boolean hasInternetConnection() {
        final Platform platform = Platform.getPlatform();
        return platform.hasInternetConnectionTo( CHECK_HOST );
    }

    /**
     * Checks whether an update is available.
     *
     * @param reset If <code>true</code>, resets the "is update available"
     * flag.
     * @return Returns <code>true</code> only if an update is available.
     */
    private static boolean isUpdateAvailable( boolean reset ) {
        try {
            if ( m_prefs.getBoolean( IS_UPDATE_AVAILABLE_KEY, false ) ) {
                if ( reset ) {
                    m_prefs.putBoolean( IS_UPDATE_AVAILABLE_KEY, false );
                    syncPrefs();
                }
                return true;
            }
        }
        catch ( IllegalStateException e ) {
            // ignore?
        }
        return false;
    }

    /**
     * Parses a XML versions document as obtained from the web server.
     *
     * @param doc The {@link Document} to parse.
     * @return Returns <code>true</code> only if the document was parsed
     * successfully and a version we're interested in was found.
     */
    private static boolean parseVersionsDocument( Document doc ) {
        m_updateRevision = 0;
        m_updateURL = "";
        m_updateVersion = "";

        final Element root = doc.getDocumentElement();
        final Element version =
            (Element)XMLUtil.getFirstChildOf( root, new VersionFilter() );
        if ( version == null )
            return false;

        ////////// Parse the version number ///////////////////////////////////

        final Element number = (Element)XMLUtil.getFirstChildOf(
            version, new ElementFilter( "number" )
        );
        if ( number == null )
            return false;
        m_updateVersion = XMLUtil.getTextOfFirstTextChildOf( number );

        ////////// Parse the revision number //////////////////////////////////

        final Element revision = (Element)XMLUtil.getFirstChildOf(
            version, new ElementFilter( "revision" )
        );
        if ( revision == null )
            return false;
        final String updateRevision =
            XMLUtil.getTextOfFirstTextChildOf( revision );
        try {
            m_updateRevision = Integer.parseInt( updateRevision );
        }
        catch ( NumberFormatException e ) {
            return false;
        }

        ////////// Parse the URL //////////////////////////////////////////////

        final Element url = (Element)XMLUtil.getFirstChildOf(
            version, new ElementFilter( "url" )
        );
        if ( url == null )
            return false;
        m_updateURL = XMLUtil.getTextOfFirstTextChildOf( url );

        return true;
    }

    /**
     * Check to see if the user asked to be reminded later and whether it's
     * later.
     *
     * @return Returns <code>true</code> only if the user asked to be reminded
     * later and it's not later yet.
     */
    private static boolean remindLaterAndIsNotLater() {
        try {
            if ( m_prefs.getInt( REMIND_KEY, 0 ) == REMIND_LATER ) {
                final long now = System.currentTimeMillis();
                final long later = m_prefs.getLong( LATER_KEY, 0 );
                return now < later;
            }
        }
        catch ( IllegalStateException e ) {
            // ignore?
        }
        return false;
    }

    /**
     * Checks the user preference as to whether automatic checks for updates
     * should be done.
     *
     * @return Returns <code>true</code> only if we should check for updates.
     */
    private static boolean shouldCheckForUpdate() {
        try {
            return m_prefs.getBoolean( CHECK_FOR_UPDATE_KEY, true );
        }
        catch ( IllegalStateException e ) {
            return true;
        }
    }

    /**
     * Shows an alert to the user indicating that a new version is available.
     * This method is used by the synchronous check mechamism.
     */
    private static void showDownloadNowOrCancelAlert() {
        final AlertDialog alert = Platform.getPlatform().getAlertDialog();
        final int button = alert.showAlert(
            null, getUpdateIsAvailableMessage(),
            null, AlertDialog.WARNING_ALERT,
            LOCALE.get( "DownloadNowButton" ),
            LOCALE.get( "DownloadCancelButton" )
        );
        if ( button == DOWNLOAD_NOW )
            WebBrowser.browse( m_updateURL );
    }

    /**
     * Shows an alert to the user indicating that a new version is available.
     * This method is used by the asynchronous check mechanism.
     */
    private static void showDownloadNowOrLaterAlert() {
        final AlertDialog alert = Platform.getPlatform().getAlertDialog();
        final int button = alert.showAlert(
            null, getUpdateIsAvailableMessage(),
            null, AlertDialog.WARNING_ALERT,
            LOCALE.get( "DownloadNowButton" ),
            LOCALE.get( "RemindMeLaterButton" ),
            LOCALE.get( "DontRemindMeButton" )
        );
        try {
            switch ( button ) {
                case DOWNLOAD_NOW:
                    WebBrowser.browse( m_updateURL );
                    return;
                case DO_NOT_REMIND:
                    m_prefs.putInt( REMIND_KEY, DO_NOT_REMIND );
                    m_prefs.put( LAST_VERSION_KEY, m_updateVersion );
                    m_prefs.remove( LATER_KEY );
                    break;
                case REMIND_LATER:
                    m_prefs.putInt( REMIND_KEY, REMIND_LATER );
                    final long now = System.currentTimeMillis();
                    m_prefs.putLong( LATER_KEY, now + DELTA_FOR_LATER );
                    m_prefs.remove( LAST_VERSION_KEY );
                    break;
            }
            syncPrefs();
        }
        catch ( IllegalStateException e ) {
            // ignore?
        }
    }

    /**
     * Shows an alert to the user indicating that no update is available.
     */
    private static void showNoUpdateIsAvailableAlert() {
        final AlertDialog alert = Platform.getPlatform().getAlertDialog();
        alert.showAlert(
            null,
            LOCALE.get( "NoUpdateIsAvailableMessage" ), null,
            AlertDialog.WARNING_ALERT,
            LOCALE.get( "CheckForUpdateOKButton" )
        );
    }

    /**
     * Write the preferences to disk.
     */
    private static void syncPrefs() {
        try {
            m_prefs.sync();
        }
        catch ( BackingStoreException e ) {
            e.printStackTrace();
        }
    }

    /**
     * User preferences for checking for updates.
     */
    private static final Preferences m_prefs;

    /**
     * The subversion revision number of the update.
     */
    private static int m_updateRevision;

    /**
     * The URL to go to to get the update.
     */
    private static String m_updateURL;

    /**
     * The user-presentable version of the update.
     */
    private static String m_updateVersion;

    /**
     * The value of this preference key stores whether to perform automatic
     * checks for updates.
     */
    private static final String CHECK_FOR_UPDATE_KEY = "CheckForUpdate";

    /**
     * Ths fully qualified host name of that we need to check with to see if
     * there's a new version.
     * @see #CHECK_URL_STRING
     */
    private static final String CHECK_HOST = "versions.lightcrafts.com";

    /**
     * The URL to fetch the <code>versions.xml</code> document from.
     * @see #CHECK_URL_STRING
     */
    private static URL CHECK_URL;

    /**
     * The string of the URL to fetch the <code>versions.xml</code> document
     * from.
     * @see #CHECK_URL
     */
    private static final String CHECK_URL_STRING =
        "http://" + CHECK_HOST + "/products/"
        + Version.getApplicationName().toLowerCase() + "/versions.xml";

    /**
     * The customer type.
     */
    private static final String CUSTOMER;

    /**
     * How many milliseconds in the future "later" is.
     */
    private static final long DELTA_FOR_LATER =
        60*60*24 /* seconds/day */ * 7 /* days */ * 1000 /* to milliseconds */;

    /**
     * The index of the "Download Now" button in the dialog.
     */
    private static final short DOWNLOAD_NOW  = 0;

    /**
     * The index of the "Remind me later" button in the dialog.  It it also
     * used as the value for the {@link #LATER_KEY}.
     */
    private static final short REMIND_LATER  = 1;

    /**
     * The index of the "Remind me later" button in the dialog.  It it also
     * used as the value for the {@link #LATER_KEY}.
     */
    private static final short DO_NOT_REMIND = 2;

    /**
     * The value of this preference key stores whether an update is available.
     */
    private static final String IS_UPDATE_AVAILABLE_KEY = "isUpdateAvailable";

    /**
     * The value of this preference key stores the last updated version we told
     * the user about.  This is used for the "Don't remind me" case.
     */
    private static final String LAST_VERSION_KEY = "lastVersion";

    /**
     * The value of this preference key stores the timestamp (in milliseconds)
     * of when "later" is.  This is used for the "Remind me later" case.
     */
    private static final String LATER_KEY = "later";

    /**
     * The value of this preference key stores the remind method, either
     * {@link #DO_NOT_REMIND} or {@link #REMIND_LATER}.
     */
    private static final String REMIND_KEY = "remind";

    /**
     * Flag used only for testing.  See {@link #main(String[])}.
     */
    private static boolean m_testing;

    static {
        try {
            CHECK_URL = new URL( CHECK_URL_STRING );
        }
        catch ( MalformedURLException e ) {
            e.printStackTrace();
        }

        final String customer = System.getProperty( "customer" );
        CUSTOMER = customer != null && customer.length() > 0 ?
            customer : "generic";

        m_prefs = Preferences.userRoot().node( "com/lightcrafts/app" );
    }

    ////////// main() /////////////////////////////////////////////////////////

    public static void main( String[] args ) throws IOException {
        final String licenseText = LicenseChecker.checkLicense();
        m_testing = true;
        //checkNowAndWait();

        final Document doc =
            XMLUtil.readDocumentFrom( new File( "/tmp/versions.xml" ) );
        parseVersionsDocument( doc );

        System.exit( 0 );
    }
}
/* vim:set et sw=4 ts=4: */
