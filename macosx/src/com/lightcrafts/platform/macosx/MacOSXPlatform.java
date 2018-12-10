/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.*;

import ch.randelshofer.quaqua.filechooser.QuaquaFileSystemView;
import ch.randelshofer.quaqua.QuaquaLookAndFeel;

import com.lightcrafts.platform.*;
import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.Version;

import static com.lightcrafts.platform.macosx.MacOSXColorProfileManager.*;
import com.lightcrafts.ui.LightZoneSkin;

/**
 * <code>MacOSXPlatform</code> is-a {@link Platform} for Mac&nbsp;OS;&nbsp;X.
 */
public final class MacOSXPlatform extends Platform {

    private final static String home = System.getProperty("user.home");

    private final static File[] SystemProfileDirs = new File[] {
        new File("/Library/ColorSync/Profiles"),
        new File("/System/Library/ColorSync/Profiles")
    };

    private final static File UserProfileDir = new File(
        home, "Library/ColorSync/Profiles"
    );

    @Override
    public void bringAppToFront( String appName ) {
        AppleScript.bringAppToFront( appName );
    }

    @Override
    public AlertDialog getAlertDialog() {
        return DefaultAlertDialog.INSTANCE; // new MacOSXAlertDialog();
    }

    @Override
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

    @Override
    public File getDefaultImageDirectory() {
        return new File( home, "Pictures" );
    }

    @Override
    public synchronized Collection<ColorProfileInfo> getExportProfiles() {
        if ( m_exportProfiles == null ) {
            m_exportProfiles = new HashSet<ColorProfileInfo>();
            for (File SystemProfileDir : SystemProfileDirs) {
                m_exportProfiles.addAll(getColorProfiles(SystemProfileDir));
            }
            m_exportProfiles.addAll(getColorProfiles(UserProfileDir));
        }
        return m_exportProfiles;
    }

    @Override
    public FileSystemView getFileSystemView() {
        return QuaquaFileSystemView.getQuaquaFileSystemView();
    }

    @Override
    public File getLightZoneDocumentsDirectory() {
        final String appName = Version.getApplicationName();
        final String path = "Library/Application Support/" + appName;
        return new File( home, path );
    }

    @Override
    public LookAndFeel getLookAndFeel() {
        LookAndFeel lookAndFeel = LightZoneSkin.getLightZoneLookAndFeel();

        boolean addQuaqua = true;

        if (addQuaqua) {
            QuaquaLookAndFeel quaqua = new QuaquaLookAndFeel();

            UIDefaults quaquaDefaults = quaqua.getDefaults();
            Object[] quaquaKeys = quaquaDefaults.keySet().toArray();

            String[] fromQuaqua = new String[] {
                    "FileView",
                    "Tree",
                    "MenuBar",
                    "RadioButtonMenuItem",
                    "CheckBoxMenuItem",
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
        return lookAndFeel;
    }

    @Override
    public String[] getPathComponentsToPicturesFolder() {
        return new String[]{ System.getProperty( "user.name" ), "Pictures" };
    }

    @Override
    public Collection<ColorProfileInfo> getPrinterProfiles() {
        return getExportProfiles();
        /* return MacOSXColorProfileManager.getProfilesFor(
            MacOSXColorProfileManager.CM_OUTPUT_CLASS
        ); */
    }

    @Override
    public ProgressDialog getProgressDialog() {
        return new DefaultProgressDialog(); // new MacOSXProgressDialog();
    }

    @Override
    public File isSpecialFile( File file ) {
        file = FileUtil.resolveAliasFile( file );
        if ( !(file instanceof MacOSXSmartFolder) &&
             MacOSXSmartFolder.isSmartFolder( file ) )
            return new MacOSXSmartFolder( file );
        return file;
    }

    @Override
    public void loadLibraries() throws UnsatisfiedLinkError {
        System.loadLibrary( "MacOSX" );
    }

    @Override
    public boolean moveFilesToTrash( String[] pathNames ) {
        return MacOSXFileUtil.moveToTrash( pathNames );
    }

    @Override
    public void readyToOpenFiles() {
        MacOSXLauncher.readyToOpenFiles();
    }

    @Override
    public String resolveAliasFile( File file ) {
        return MacOSXFileUtil.resolveAlias( file.getAbsolutePath() );
    }

    @Override
    public void showHelpTopic( String topic ) {
        MacOSXHelp.showHelpTopic( topic );
    }

/*
    public static final int WHEEL_HORIZONTAL_SCROLLg = 2;

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
