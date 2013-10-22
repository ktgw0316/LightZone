/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.linux;

import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.utils.ColorProfileInfo;

import javax.help.HelpSet;
import javax.help.HelpSetException;
import javax.help.JHelp;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ICC_Profile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxPlatform extends Platform {

    // My understanding of the state of standard linux color profile
    // locations comes from:
    //
    //      http://bugs.freestandards.org/show_bug.cgi?id=77

    private final static File SystemProfileDir = new File(
        "/usr/share/color/icc"
    );

    private final static File UserProfileDir = new File(
        System.getProperty("user.home"),
        ".color/icc"
    );

    private static Collection<ColorProfileInfo> Profiles;

    public LookAndFeel getLookAndFeel() {
        return LightZoneSkin.getLightZoneLookAndFeel();
    }

    public FileChooser getFileChooser() {
        return new LinuxFileChooser();
    }

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

    public Collection<ColorProfileInfo> getPrinterProfiles() {
        return getColorProfiles();
    }

    public Collection<ColorProfileInfo> getExportProfiles() {
        return getColorProfiles();
    }

    public boolean isKeyPressed(int keyCode) {
        return LinuxKeyUtil.isKeyPressed(keyCode);
    }

    private static Collection<ColorProfileInfo> getColorProfiles() {
        if (Profiles != null) {
            return Profiles;
        }
        Profiles = new HashSet<ColorProfileInfo>();
        Profiles.addAll(getColorProfiles(SystemProfileDir));
        Profiles.addAll(getColorProfiles(UserProfileDir));

        return Profiles;
    }

    private static Collection<ColorProfileInfo> getColorProfiles(
        File profileDir
    ) {
        HashSet<ColorProfileInfo> profiles = new HashSet<ColorProfileInfo>();

        if (! profileDir.isDirectory()) {
            return profiles;
        }
        // Try to interpret every file in there as a color profile:

        File[] files = profileDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                profiles.addAll(getColorProfiles(file));
            }
            else if (file.isFile()) {
                String path = file.getAbsolutePath();
                try {
                    ICC_Profile profile = ICC_Profile.getInstance(path);
                    String name = ColorProfileInfo.getNameOf(profile);
                    ColorProfileInfo info = new ColorProfileInfo(name, path);
                    profiles.add(info);
                }
                catch (IOException e) {
                    // Trouble reading the file
                    String message = e.getMessage();
                    System.err.println(
                        "Can't read a color profile from " + path + ": " +
                        message
                    );
                }
                catch (Throwable e) {
                    // Invalid color profile data
                    String message = e.getMessage();
                    System.err.println(
                        "Not a valid color profile at " + path + ": " +
                        message
                    );
                }
            }
        }
        return profiles;
    }

    public int getPhysicalMemoryInMB() {
        Pattern pattern = Pattern.compile("MemTotal: *([0-9]*) .*");
        try {
            FileReader reader = new FileReader("/proc/meminfo");
            BufferedReader buffer = new BufferedReader(reader);
            String line = buffer.readLine();
            while (line != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    String text = matcher.replaceAll("$1");
                    int i = Integer.parseInt(text);
                    return i / 1024;
                }
                line = buffer.readLine();
            }
            buffer.close();
        }
        catch (IOException  e) {
            System.err.println("Can't read /proc/meminfo: " + e.getMessage());
        }
        catch (NumberFormatException e) {
            System.err.println("Malformed MemTotal text: " + e.getMessage());
        }
        return super.getPhysicalMemoryInMB();
    }

    public void loadLibraries() throws UnsatisfiedLinkError {
        System.loadLibrary("Linux");
    }

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
            "nautilus", "konqueror" // others?
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
