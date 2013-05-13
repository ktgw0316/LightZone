/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.*;

import ch.randelshofer.quaqua.panther.filechooser.OSXPantherFileSystemView;
import ch.randelshofer.quaqua.QuaquaLookAndFeel;

import com.lightcrafts.platform.*;
import com.lightcrafts.utils.ColorProfileInfo;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.Version;

import static com.lightcrafts.platform.macosx.MacOSXColorProfileManager.*;
import com.lightcrafts.ui.LightZoneSkin;

/**
 * <code>MacOSXPlatform</code> is-a {@link Platform} for Mac&nbsp;OS;&nbsp;X.
 */
public final class MacOSXPlatform extends Platform {

    public void bringAppToFront( String appName ) {
        AppleScript.bringAppToFront( appName );
    }

    public AlertDialog getAlertDialog() {
        return DefaultAlertDialog.INSTANCE; // new MacOSXAlertDialog();
    }

    public ICC_Profile getDisplayProfile() {
        try {
            final String path =
                MacOSXColorProfileManager.getSystemDisplayProfilePath();
            final InputStream in = new File( path ).toURL().openStream();
            return ICC_Profile.getInstance( in );
        }
        catch ( IOException e ) {
            return null;
        }
    }

    public File getDefaultImageDirectory() {
        final String home = System.getProperty( "user.home" );
        return new File( home, "Pictures" );
    }

    public synchronized Collection<ColorProfileInfo> getExportProfiles() {
        if ( m_exportProfiles == null ) {
            final Collection<ColorProfileInfo> colorspaceProfiles =
                MacOSXColorProfileManager.getProfilesFor( CM_COLORSPACE_CLASS );
            final Collection<ColorProfileInfo> displayProfiles =
                MacOSXColorProfileManager.getProfilesFor( CM_DISPLAY_CLASS );
            final Collection<ColorProfileInfo> outputProfiles =
                MacOSXColorProfileManager.getProfilesFor( CM_OUTPUT_CLASS );

            m_exportProfiles = new ArrayList<ColorProfileInfo>();

            if ( colorspaceProfiles != null )
                m_exportProfiles.addAll( colorspaceProfiles );
            if ( displayProfiles != null )
                m_exportProfiles.addAll( displayProfiles );
            if ( outputProfiles != null )
                m_exportProfiles.addAll( outputProfiles );

            if ( m_exportProfiles.isEmpty() )
                m_exportProfiles = null;
            else
                m_exportProfiles =
                    Collections.unmodifiableCollection( m_exportProfiles );
        }
        return m_exportProfiles;
    }

    public FileChooser getFileChooser() {
        return MacOSXFileChooser.getFileChooser();
    }

    public FileSystemView getFileSystemView() {
        return OSXPantherFileSystemView.getQuaquaFileSystemView();
    }

    public File getLightZoneDocumentsDirectory() {
        final String home = System.getProperty( "user.home" );
        final String appName = Version.getApplicationName();
        final String path = "Library/Application Support/" + appName;
        return new File( home, path );
    }

    public LookAndFeel getLookAndFeel() {
        LookAndFeel lookAndFeel = LightZoneSkin.getLightZoneLookAndFeel();

        boolean addQuaqua = true;

        if (addQuaqua) {
            QuaquaLookAndFeel quaqua = new QuaquaLookAndFeel();

            UIDefaults quaquaDefaults = quaqua.getDefaults();
            Set quaquaKeys = new TreeSet(quaquaDefaults.keySet());

            String[] fromQuaqua = new String[] {
                    "FileChooser",
                    "FileView",
                    "Tree",
                    "MenuBar",
                    "RadioButtonMenuItem",
                    "CheckBoxMenuItem",
            };

            for (Object key : quaquaKeys) {
                for (String qk : fromQuaqua)
                    if (((String) key).startsWith(qk)) {
                        Object value = quaquaDefaults.get(key);
                        UIManager.put(key, value);
                        break;
                    }

            }
        }
        return lookAndFeel;
    }

    public String[] getPathComponentsToPicturesFolder() {
        return new String[]{ System.getProperty( "user.name" ), "Pictures" };
    }

    public int getPhysicalMemoryInMB() {
        return MacOSXMemory.getPhysicalMemoryInMB();
    }

    public Collection<ColorProfileInfo> getPrinterProfiles() {
        return getExportProfiles();
        /* return MacOSXColorProfileManager.getProfilesFor(
            MacOSXColorProfileManager.CM_OUTPUT_CLASS
        ); */
    }

    public ProgressDialog getProgressDialog() {
        return new DefaultProgressDialog(); // new MacOSXProgressDialog();
    }

    public boolean hasInternetConnectionTo( String hostName ) {
        return MacOSXInternetConnection.hasConnectionTo( hostName );
    }

    public File isSpecialFile( File file ) {
        file = FileUtil.resolveAliasFile( file );
        if ( !(file instanceof MacOSXSmartFolder) &&
             MacOSXSmartFolder.isSmartFolder( file ) )
            return new MacOSXSmartFolder( file );
        return file;
    }

    public void loadLibraries() throws UnsatisfiedLinkError {
        System.loadLibrary( "MacOSX" );
    }

    public boolean moveFilesToTrash( String[] pathNames ) {
        return MacOSXFileUtil.moveToTrash( pathNames );
    }

    public void readyToOpenFiles() {
        if ( System.getProperty( "IDE" ) != null )
            ApplicationAdapterForIDE.initialize();
        else
            MacOSXLauncher.readyToOpenFiles();
    }

    public String resolveAliasFile( File file ) {
        return MacOSXFileUtil.resolveAlias( file.getAbsolutePath() );
    }

    public boolean showFileInFolder( String path ) {
        return MacOSXFileUtil.showInFinder( path );
    }

    public void showHelpTopic( String topic ) {
        MacOSXHelp.showHelpTopic( topic );
    }

    public PrinterLayer getPrinterLayer() {
        return MacOSXPrinterLayer.INSTANCE;
    }

    public static final int WHEEL_HORIZONTAL_SCROLL = 2;

/*
    static class MouseWheelDispatcher implements MacOSXMightyMouse.Listener {
        final JFrame frame;
        final MouseWheelListener listener;

        MouseWheelDispatcher(JFrame frame, MouseWheelListener listener) {
            this.frame = frame;
            this.listener = listener;
        }

        private int eventId = 0;

        public void mightyMouseEvent( MacOSXMightyMouse.Event e ) {
            int x = e.getX();
            int y = frame.getHeight() - e.getY();
            final MouseWheelEvent event = new MouseWheelEvent(frame, eventId++, System.currentTimeMillis(),
                                                              0, x, y, 0, false, WHEEL_HORIZONTAL_SCROLL,
                                                              1, e.getHScrollDelta());
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        listener.mouseWheelMoved(event);
                    }
                }
            );
        }
    }

    public void registerMouseWheelListener(MouseWheelListener listener, JFrame frame) {
        MacOSXMightyMouse.setListener(new MouseWheelDispatcher(frame, listener), frame);
    }
*/

    ////////// private ////////////////////////////////////////////////////////

    private Collection<ColorProfileInfo> m_exportProfiles;
}
/* vim:set et sw=4 ts=4: */
