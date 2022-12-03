/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.platform.PrinterLayer;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.file.FileUtil;

import javax.swing.filechooser.FileSystemView;
import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static com.lightcrafts.platform.windows.WindowsFileUtil.FOLDER_MY_PICTURES;

public final class WindowsPlatform extends Platform {

    ////////// public /////////////////////////////////////////////////////////

    @Override
    public File getDefaultImageDirectory() {
        final String path =
            WindowsFileUtil.getFolderPathOf( FOLDER_MY_PICTURES );
        return path != null ? new File( path ) : null;
    }

    @Override
    public ICC_Profile getDisplayProfile() {
        final String path =
            WindowsColorProfileManager.getSystemDisplayProfilePath();
        if ( path == null )
            return null;
        try {
            final InputStream in = new File( path ).toURL().openStream();
            final ICC_Profile profile = ICC_Profile.getInstance( in );
            System.out.println(
                "Display profile " + ColorProfileInfo.getNameOf( profile )
            );
            return profile;
        }
        catch ( IOException e ) {
            return null;
        }
    }

    @Override
    public Collection<ColorProfileInfo> getExportProfiles() {
        return getColorProfiles();
    }

    @Override
    public String getDisplayNameOf( File file ) {
        String displayName = getFileSystemView().getSystemDisplayName( file );

        if ( displayName.endsWith( ".lnk" ) ) {
            //
            // Windows shortcuts' display names sometimes incorrectly show the
            // ".lnk" extension -- remove it.
            //
            displayName = FileUtil.trimExtensionOf( displayName );
        }

        return displayName;
    }

    @Override
    public File getLightZoneDocumentsDirectory() {
        final File myDocuments =
            FileSystemView.getFileSystemView().getDefaultDirectory();
        return new File( myDocuments, Version.getApplicationName() );
    }

    @Override
    public String[] getPathComponentsToPicturesFolder() {
        final File picturesDir =
            Platform.getPlatform().getDefaultImageDirectory();
        if ( picturesDir == null )
            return null;
        final String[] picturesComponents =
            picturesDir.toString().split( "\\\\" );
        if ( picturesComponents.length != 5 ) {
            //
            // We expect the components to be:
            //
            //      C:
            //      Documents and Settings
            //      {user}
            //      My Documents
            //      My Pictures
            //
            // If it isn't, forget it.
            //
            return null;
        }
        final String[] wantedComponents = new String[2];
        System.arraycopy( picturesComponents, 3, wantedComponents, 0, 2 );
        return wantedComponents;
    }

    @Override
    public Collection<ColorProfileInfo> getPrinterProfiles() {
        return getColorProfiles();
    }

    @Override
    public void hideFile( File file ) throws IOException {
        WindowsFileUtil.hideFile( file.getAbsolutePath() );
    }

    @Override
    public File isSpecialFile( File file ) {
        file = FileUtil.resolveAliasFile( file );
        if ( !(file instanceof WindowsSavedSearch) &&
             WindowsSavedSearch.isSavedSearch( file ) )
            return new WindowsSavedSearch( file );
        if ( isVista() )
            return isSpecialFileOnVista( file );
        return file;
    }

    /**
     * True if it's newer than Vista.
     */
    public static boolean isVista() {
        String version = System.getProperty( "os.version" );
        int majorVersion = Character.getNumericValue(version.charAt(0));
        return majorVersion >= 6;
    }

    @Override
    public void loadLibraries() throws UnsatisfiedLinkError {
        // We may need clib_jiio and either mlib_jai or mlib_jai_mmx, but we
        // don't load these explicitly so JAI can make up its own mind.
        System.loadLibrary( "Windows" );
    }

    @Override
    public String resolveAliasFile( File file ) {
        return WindowsFileUtil.resolveShortcut( file.getAbsolutePath() );
    }

    @Override
    public void showHelpTopic( String topic ) {
        WindowsHelp.showHelpTopic( topic );
    }

    private PrinterLayer printerLayer = new WindowsPrinterLayer();

    @Override
    public PrinterLayer getPrinterLayer() {
        return printerLayer;
    }

    ////////// private ////////////////////////////////////////////////////////

    private static synchronized Collection<ColorProfileInfo> getColorProfiles() {
        if (m_profiles == null) {
            String windir = System.getenv("WINDIR");
            if (windir == null)
                windir = "C:\\WINDOWS";

            final File profileDir = new File(
                    windir + "\\system32\\spool\\drivers\\color");
            m_profiles = getColorProfiles(profileDir);
        }
        return m_profiles;
    }

    /**
     * Checks whether the given {@link File} is special in some way on Windows
     * Vista to deal with Vista-specific quirks.
     *
     * @param file The {@link File} to check.
     * @return If the file is special, returns a new {@link File} to deal with
     * a Vista-specific quirk; otherwise returns the passed-in file.
     */
    private File isSpecialFileOnVista( File file ) {
        //
        // Under Vista, the user's Desktop has a special folder that's some
        // kind of "link" to the user's home folder, i.e., Desktop\{user}.
        // Java correctly recognizes it as a folder, but listFiles() on it
        // always returns an empty array -- it must be a bug.
        //
        // To work around this, we check to see if the given file has the path
        // Desktop\{user}.  If so, we return a new File of the user's real home
        // folder of C:\Users\{user} for which listFiles() works correctly.
        //
        final String displayName = getDisplayNameOf( file );
        if ( !displayName.equals( System.getProperty( "user.name" ) ) )
            return file;
        final File parentDir = file.getParentFile();
        if ( parentDir == null )
            return file;
        if ( parentDir.getName().equals( "Desktop" ) )
            return new File( System.getProperty( "user.home" ) );
        return file;
    }

    /**
     * The ICC profiles from
     * <code>$WINDIR\system32\spool\drivers\color</code>.
     */
    private static Collection<ColorProfileInfo> m_profiles;
}
/* vim:set et sw=4 ts=4: */
