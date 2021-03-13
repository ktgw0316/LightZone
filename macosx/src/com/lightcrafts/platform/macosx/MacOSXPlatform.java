/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.platform.*;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.file.FileUtil;

import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

/**
 * <code>MacOSXPlatform</code> is-a {@link Platform} for Mac&nbsp;OS;&nbsp;X.
 */
public final class MacOSXPlatform extends Platform {

    private final static String home = System.getProperty("user.home");

    private final static File[] SystemProfileDirs = new File[] { new File("/Library/ColorSync/Profiles"),
            new File("/System/Library/ColorSync/Profiles") };

    private final static File UserProfileDir = new File(home, "Library/ColorSync/Profiles");

    @Override
    public void bringAppToFront(String appName) {
        AppleScript.bringAppToFront(appName);
    }

    @Override
    public AlertDialog getAlertDialog() {
        return DefaultAlertDialog.INSTANCE; // new MacOSXAlertDialog();
    }

    @Override
    public ICC_Profile getDisplayProfile() {
        try {
            final String path = MacOSXColorProfileManager.getSystemDisplayProfilePath();
            final InputStream in = new File(path).toURL().openStream();
            return ICC_Profile.getInstance(in);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public File getDefaultImageDirectory() {
        return new File(home, "Pictures");
    }

    @Override
    public FileChooser getFileChooser() {
        return new MacOSXFileChooser();
    }

    @Override
    public synchronized Collection<ColorProfileInfo> getExportProfiles() {
        if (m_exportProfiles == null) {
            m_exportProfiles = new HashSet<ColorProfileInfo>();
            for (File SystemProfileDir : SystemProfileDirs) {
                m_exportProfiles.addAll(getColorProfiles(SystemProfileDir));
            }
            m_exportProfiles.addAll(getColorProfiles(UserProfileDir));
        }
        return m_exportProfiles;
    }

    @Override
    public File getLightZoneDocumentsDirectory() {
        final String appName = Version.getApplicationName();
        final String path = "Library/Application Support/" + appName;
        return new File(home, path);
    }

    @Override
    public String[] getPathComponentsToPicturesFolder() {
        return new String[] { System.getProperty("user.name"), "Pictures" };
    }

    @Override
    public Collection<ColorProfileInfo> getPrinterProfiles() {
        return getExportProfiles();
        /*
         * return MacOSXColorProfileManager.getProfilesFor(
         * MacOSXColorProfileManager.CM_OUTPUT_CLASS );
         */
    }

    @Override
    public ProgressDialog getProgressDialog() {
        return new DefaultProgressDialog(); // new MacOSXProgressDialog();
    }

    @Override
    public File isSpecialFile(File file) {
        file = FileUtil.resolveAliasFile(file);
        if (!(file instanceof MacOSXSmartFolder) && MacOSXSmartFolder.isSmartFolder(file))
            return new MacOSXSmartFolder(file);
        return file;
    }

    @Override
    public void loadLibraries() throws UnsatisfiedLinkError {
        System.loadLibrary("MacOSX");
    }

    @Override
    public boolean moveFilesToTrash(String[] pathNames) {
        return MacOSXFileUtil.moveToTrash(pathNames);
    }

    @Override
    public String resolveAliasFile(File file) {
        return MacOSXFileUtil.resolveAlias(file.getAbsolutePath());
    }

    @Override
    public void showHelpTopic(String topic) {
        MacOSXHelp.showHelpTopic(topic);
    }

    ////////// private ////////////////////////////////////////////////////////

    private Collection<ColorProfileInfo> m_exportProfiles;
}
/* vim:set et sw=4 ts=4: */
