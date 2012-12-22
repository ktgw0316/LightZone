/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

import javax.swing.filechooser.FileSystemView;
import javax.swing.*;

import sun.awt.shell.ShellFolder;

import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.platform.PrinterLayer;
import com.lightcrafts.utils.ColorProfileInfo;
import com.lightcrafts.utils.directory.DirectoryMonitor;
import com.lightcrafts.utils.directory.WindowsDirectoryMonitor;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.file.ICC_ProfileFileFilter;
import com.lightcrafts.utils.Version;

import static com.lightcrafts.platform.windows.WindowsFileUtil.*;
import com.lightcrafts.ui.LightZoneSkin;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public final class WindowsPlatform extends Platform {

    ////////// public /////////////////////////////////////////////////////////

    public File getDefaultImageDirectory() {
        final String path =
            WindowsFileUtil.getFolderPathOf( FOLDER_MY_PICTURES );
        return path != null ? new File( path ) : null;
    }

    public DirectoryMonitor getDirectoryMonitor() {
        return new WindowsDirectoryMonitor();
    }

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

    public Collection<ColorProfileInfo> getExportProfiles() {
        return getColorProfiles();
    }

    public FileChooser getFileChooser() {
        return super.getFileChooser();
        // return new WindowsFileChooser();
    }

    public String getDisplayNameOf( File file ) {
        String displayName;

        if ( file instanceof ShellFolder ) {
            //
            // This is a stupid hack fix when running on Vista that shows the
            // GUID rather than the display name for the user's home directory
            // (and a few other directories).
            //
            // See: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6488082
            //
            // It's apparently fixed for ShellFolder, but still broken for
            // FileSystemView.
            //
            displayName = ((ShellFolder)file).getDisplayName();
        } else
            displayName = getFileSystemView().getSystemDisplayName( file );

        if ( displayName.endsWith( ".lnk" ) ) {
            //
            // Windows shortcuts' display names sometimes incorrectly show the
            // ".lnk" extension -- remove it.
            //
            displayName = FileUtil.trimExtensionOf( displayName );
        }

        return displayName;
    }

    public File getLightZoneDocumentsDirectory() {
        final File myDocuments =
            FileSystemView.getFileSystemView().getDefaultDirectory();
        return new File( myDocuments, Version.getApplicationName() );
    }

    public LookAndFeel getLookAndFeel() {
        LookAndFeel laf = LightZoneSkin.getLightZoneLookAndFeel();

        boolean addWindows = false;

        if (addWindows) {
            WindowsLookAndFeel quaqua = new WindowsLookAndFeel();

            UIDefaults quaquaDefaults = quaqua.getDefaults();
            Set quaquaKeys = quaquaDefaults.keySet();

            String[] fromQuaqua = new String[] {
                    "FileChooser",
            };

            for (Object key : quaquaKeys) {
                for (String qk : fromQuaqua)
                    if (key instanceof String && ((String) key).startsWith(qk)) {
                        Object value = quaquaDefaults.get(key);
                        UIManager.put(key, value);
                        break;
                    }

            }
        }

        return laf;
    }

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

    public int getPhysicalMemoryInMB() {
        return WindowsMemory.getPhysicalMemoryInMB();
    }

    public Collection<ColorProfileInfo> getPrinterProfiles() {
        return getColorProfiles();
    }

    public boolean hasInternetConnectionTo( String hostName ) {
        return WindowsInternetConnection.hasConnection();
    }

    public void hideFile( File file ) throws IOException {
        WindowsFileUtil.hideFile( file.getAbsolutePath() );
    }

    public boolean isKeyPressed( int keyCode ) {
        return WindowsKeyUtil.isKeyPressed( keyCode );
    }

    public File isSpecialFile( File file ) {
        file = FileUtil.resolveAliasFile( file );
        if ( !(file instanceof WindowsSavedSearch) &&
             WindowsSavedSearch.isSavedSearch( file ) )
            return new WindowsSavedSearch( file );
        if ( isVista() )
            return isSpecialFileOnVista( file );
        return file;
    }

    public static boolean isVista() {
        return System.getProperty( "os.name" ).endsWith( "Vista" );
    }

    public void loadLibraries() throws UnsatisfiedLinkError {
        // We may need clib_jiio and either mlib_jai or mlib_jai_mmx, but we
        // don't load these explicitly so JAI can make up its own mind.
        System.loadLibrary( "Windows" );
    }

    public boolean moveFilesToTrash( String[] pathNames ) {
        return WindowsFileUtil.moveToRecycleBin( pathNames );
    }

    public void readyToOpenFiles() {
        if ( System.getProperty( "IDE" ) == null )
            WindowsLauncher.readyToOpenFiles();
    }

    public String resolveAliasFile( File file ) {
        return WindowsFileUtil.resolveShortcut( file.getAbsolutePath() );
    }

    public boolean showFileInFolder( String path ) {
        return WindowsFileUtil.showInExplorer( path );
    }

    public void showHelpTopic( String topic ) {
        WindowsHelp.showHelpTopic( topic );
    }

    private PrinterLayer printerLayer = new WindowsPrinterLayer();

    public PrinterLayer getPrinterLayer() {
        return printerLayer;
    }

    ////////// private ////////////////////////////////////////////////////////

    private static synchronized Collection<ColorProfileInfo> getColorProfiles() {
        if ( m_profiles != null )
            return m_profiles;
        m_profiles = new ArrayList<ColorProfileInfo>();

        String windir = System.getenv( "WINDIR" );
        if ( windir == null )
            windir = "C:\\WINDOWS";

        final File profileDir = new File(
            windir + "\\system32\\spool\\drivers\\color"
        );
        if ( !profileDir.isDirectory() )
            return m_profiles;

        final File[] files =
            profileDir.listFiles( ICC_ProfileFileFilter.INSTANCE );
        for ( File file : files ) {
            final String path = file.getAbsolutePath();
            try {
                final ICC_Profile profile = ICC_Profile.getInstance( path );
                final String name = ColorProfileInfo.getNameOf( profile );
                m_profiles.add( new ColorProfileInfo( name, path ) );
            }
            catch ( IOException e ) {
                // Trouble reading the file
                System.err.println(
                    "Can't read a color profile from " + path + ": "
                    + e.getMessage()
                );
            }
            catch ( Throwable t ) {
                // Invalid color profile data
                System.err.println(
                    "Bad color profile at " + path + ": " + t.getMessage()
                );
            }
        }
        Collections.sort( m_profiles );
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
    private static List<ColorProfileInfo> m_profiles;
}
/* vim:set et sw=4 ts=4: */
