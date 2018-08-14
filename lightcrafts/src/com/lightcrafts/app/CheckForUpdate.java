/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.app;

import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.platform.ProgressDialog;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.WebBrowser;
import com.lightcrafts.utils.thread.ProgressThread;
import de.dimaki.refuel.appcast.entity.Appcast;
import de.dimaki.refuel.updater.boundary.Updater;
import de.dimaki.refuel.updater.entity.ApplicationStatus;
import lombok.val;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.lightcrafts.app.Locale.LOCALE;

/**
 * <code>CheckForUpdate</code> checks to see if an update of the software is
 * available.  There are two separate methods: asynchronous and synchronous.
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
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
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
    private static void showAlertIfAvailable() {
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

    public static boolean isEnabled() {
        // Refuel only supports Java 8 or later
        return Double.parseDouble(System.getProperty("java.specification.version")) >= 1.8;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A <code>BackgroundCheckThread</code> is-a {@link Thread} that checks to
     * see if a new version of the software is available by fetchcing the
     * latest version information from a version server.
     * <p>
     * Since this runs in the background, all exceptions are caught and
     * ignored.
     */
    private static final class BackgroundCheckThread extends Thread {

        ////////// public /////////////////////////////////////////////////////

        @Override
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
     * latest version information from a version server.
     * <p>
     * Since this runs synchronously, any exception is reported via
     * {@link ProgressDialog#getThrown()}.
     */
    private static final class SynchronousCheckThread extends ProgressThread {

        ////////// public /////////////////////////////////////////////////////

        @Override
        public void run() {
            m_isUpdateAvailable = checkIfUpdateIsAvailable();
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
     * Checks if an update is available by fetching the latest version
     * information from a version server.
     *
     * @return Returns <code>true</code> only if an update is available.
     */
    private static boolean checkIfUpdateIsAvailable() {
        val currentVersion = Version.getVersionName();
        return checkIfUpdateIsAvailable(currentVersion, CHECK_URL);
    }

    static boolean checkIfUpdateIsAvailable(String currentVersion, URL url) {
        val updater = new Updater();
        if (currentVersion.contains("alpha") || currentVersion.contains("beta") || currentVersion.contains("rc")) {
            // TODO:
        }
        val applicationStatus = updater.getApplicationStatus(currentVersion, url);
        if (!ApplicationStatus.UPDATE_AVAILABLE.equals(applicationStatus)) {
            // This also handles the case for development versions when the version
            // resource hasn't been populated.
            return false;
        }
        val appcast = applicationStatus.getAppcast();
        return parseAppcast(appcast);
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
     * Parses an Appcast document as obtained from the web server.
     *
     * @param appcast The {@link Appcast} to parse.
     * @return Returns <code>true</code> only if the document was parsed
     * successfully and a version we're interested in was found.
     */
    private static boolean parseAppcast(Appcast appcast) {
        m_updateURL = appcast.getChannel().getItems().get(0).getReleaseNotesLink();
        m_updateVersion = appcast.getLatestVersion();
        return m_updateURL != null && m_updateVersion != null;
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
    private static final String CHECK_HOST = "raw.githubusercontent.com";

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
            "https://" + CHECK_HOST
            + "/ktgw0316/homebrew-" + Version.getApplicationName().toLowerCase()
            + "/master/appcast.xml";

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

    static {
        try {
            CHECK_URL = new URL( CHECK_URL_STRING );
        }
        catch ( MalformedURLException e ) {
            e.printStackTrace();
        }
        m_prefs = Preferences.userRoot().node( "com/lightcrafts/app" );
    }

    ////////// main() /////////////////////////////////////////////////////////

    public static void main( String[] args ) throws MalformedURLException {
        val isAvailable = checkIfUpdateIsAvailable(
                "4.1.9", new URL("file:///tmp/lightzone/appcast.xml"));
        System.exit(isAvailable ? 0 : 1);
    }
}
/* vim:set et sw=4 ts=4: */
