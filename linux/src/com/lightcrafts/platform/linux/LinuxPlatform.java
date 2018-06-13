/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.linux;

import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.utils.Version;

import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.help.JHelp;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ICC_Profile;
import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxPlatform extends Platform {

    private final static String home = System.getProperty( "user.home" );

    // My understanding of the state of standard linux color profile
    // locations comes from:
    //
    //      http://bugs.freestandards.org/show_bug.cgi?id=77

    private final static File SystemProfileDir = new File(
        "/usr/share/color/icc"
    );

    private final static File UserProfileDir = new File(
        home, ".color/icc"
    );

    private static Collection<ColorProfileInfo> Profiles;

    @Override
    public File getDefaultImageDirectory() {
        ProcessBuilder pb = new ProcessBuilder("xdg-user-dir", "PICTURES");
        try {
            Process p = pb.start();
            BufferedReader br =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            br.close();
            p.waitFor();
            p.destroy();
            if (p.exitValue() == 0 && line != null && ! line.equals(home)) {
                return new File(line);
            }
        }
        catch (IOException e) {
        }
        catch (InterruptedException e) {
        }

        return new File( home, Version.getApplicationName() );
    }

    @Override
    public File getLightZoneDocumentsDirectory() {
        final String appName = Version.getApplicationName();
        final String path = ".local/share/" + appName;
        return new File( home, path );
    }

    @Override
    public LookAndFeel getLookAndFeel() {
        return LightZoneSkin.getLightZoneLookAndFeel();
    }

    @Override
    public ICC_Profile getDisplayProfile() {
        Preferences prefs = Preferences.userRoot().node(
            "/com/lightcrafts/platform/linux"
        );
        String path = prefs.get("DisplayProfile", null);
        if (path != null) {
            try {
                return ICC_Profile.getInstance(path);
            }
            catch (Throwable e) {
                System.err.println("Malformed display profile at " + path);
                // return null;
            }
        }
        return null;
    }

    @Override
    public Collection<ColorProfileInfo> getPrinterProfiles() {
        return getColorProfiles();
    }

    @Override
    public Collection<ColorProfileInfo> getExportProfiles() {
        return getColorProfiles();
    }

    @Override
    public boolean isKeyPressed(int keyCode) {
        return LinuxKeyUtil.isKeyPressed(keyCode);
    }

    private static synchronized Collection<ColorProfileInfo> getColorProfiles() {
        if (Profiles == null) {
            Profiles = new HashSet<ColorProfileInfo>();
            Profiles.addAll(getColorProfiles(SystemProfileDir));
            Profiles.addAll(getColorProfiles(UserProfileDir));
        }
        return Profiles;
    }

    @Override
    public void loadLibraries() throws UnsatisfiedLinkError {
        System.loadLibrary("Linux");
    }

    @Override
    public void makeModal(Dialog dialog) {
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
    }

    public boolean showFileInFolder( String path ) {
        // If the path points to a file, pop up to its enclosing folder.
        File file = new File(path);
        if (file.isFile()) {
            path = file.getParent();
        }
        String[] fileManagers = new String[] {
            "nautilus",  // Gnome
            "dolphin",   // KDE
            "konqueror", // KDE
            "nemo",      // Cinnamon
            "caja",      // MATE
            "thunar",    // Xfce
            "pcmanfm",   // LXDE
            "rox-filer"  // RQX
            // others?
        };
        try {
            Runtime rt = Runtime.getRuntime();
            for (String fileManager : fileManagers ) {
                String[] args = new String[]{ "which", fileManager };
                if (rt.exec(args).waitFor() == 0) {
                    args = new String[] { fileManager, path };
                    rt.exec(args);
                    return true;
                }
            }
        }
        catch ( Exception e ) {
            // do nothing
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void showHelpTopic(String topic) {
        // TODO: use the "topic" argument to pick an initial page
        try {
            URL url = HelpSet.findHelpSet(null, "LightZone.hs");
            HelpSet help = new HelpSet(null, url);
            String title = help.getTitle();
            JHelp jhelp = new JHelp(help);
            help.setHomeID("index");
            try {
                jhelp.setCurrentID(topic);
            }
            catch (Throwable t) {
                jhelp.setCurrentID("index");
            }
            JFrame frame = new JFrame();
            frame.setTitle(title);
            frame.setContentPane(jhelp);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
        catch (HelpSetException e) {
            getPlatform().getAlertDialog().showAlert(
                null,
                "Couldn't initialize the LightZone help system.",
                e.getClass().getName() + ": " + e.getMessage(),
                AlertDialog.ERROR_ALERT,
                "OK");
        }
    }

    public static void main(String[] args)
        throws UnsupportedLookAndFeelException
    {
        Platform platform = Platform.getPlatform();
        platform.loadLibraries();
        System.out.println(platform.getPhysicalMemoryInMB());

        UIManager.setLookAndFeel(platform.getLookAndFeel());
        platform.showHelpTopic("New_Features");
    }
}
/* vim:set et sw=4 ts=4: */
