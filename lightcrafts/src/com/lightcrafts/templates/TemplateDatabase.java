/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.templates;

import static com.lightcrafts.templates.Locale.LOCALE;

import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.RawImageType;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;

import lombok.val;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Read and write XmlDocuments from and to Preferences using user-provided
 * String keys.  Also, inspect the current set of key Strings.
 */
public class TemplateDatabase {

    /**
     * Any failure in the backing store mechanism for templates causes this
     * exception to be thrown.
     */
    public static class TemplateException extends Exception {
        TemplateException(String message) {
            super(message);
        }
        TemplateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // The namespace that indicates a camera default Template.
    private final static String CameraDefaultNamespace = "CameraDefault";

    // A namespace assigned to legacy Templates without a namespace.
    private final static String DefaultNamespace =
        LOCALE.get("DefaultTemplateNamespace");

    private final static String TemplateSuffix = "Template_";

    static File TemplateDir;

    private final static LinkedList<TemplateDatabaseListener> listeners =
        new LinkedList<TemplateDatabaseListener>();

    private final static Preferences Prefs =
        Preferences.userNodeForPackage(TemplateDatabase.class);

    /**
     * This class provides a static utility and cannot be constructed.
     */
    private TemplateDatabase() {
    }

    public static void addListener(TemplateDatabaseListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(TemplateDatabaseListener listener) {
        listeners.remove(listener);
    }

    public static void deployFactoryTemplates() {
        TemplateDeployer.deploy();
        notifyListeners();
    }

    public static List<TemplateKey> getTemplateKeys() throws TemplateException {
        checkTemplateDir();
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    return name.endsWith(".lzt");
                }
                return false;
            }
        };
        val files = FileUtil.listFiles(TemplateDir, filter, false);
        if (files == null) {
            throw new TemplateException(
                "Couldn't read Template names from " +
                TemplateDir.getAbsolutePath()
            );
        }
        List<TemplateKey> keys = new ArrayList<TemplateKey>();
        for (val file : files) {
            TemplateKey key = new TemplateKey(file);
            keys.add(key);
        }
        Collections.sort(keys);
        return keys;
    }

    public static XmlDocument getTemplateDocument(TemplateKey key)
        throws TemplateException
    {
        checkTemplateDir();
        File file = key.getFile();
        if (file == null) {
            throw new TemplateException(
                    "No Template named \"" + key + "\" exists in " +
                            TemplateDir.getAbsolutePath()
            );
        }
        try (InputStream in = new FileInputStream(file)) {
            return new XmlDocument(in);
        }
        catch (XMLException e) {
            throw new TemplateException(
                "Template \"" + key + "\" in " +
                TemplateDir.getAbsolutePath() +
                " is malformed: " + e.getMessage(),
                e
            );
        }
        catch (IOException e) {
            throw new TemplateException(
                "Couldn't access Template \"" + key + "\" in " +
                TemplateDir.getAbsolutePath() + ": " + e.getMessage(),
                e
            );
        }
    }

    public static void addTemplateDocument(
        XmlDocument doc, TemplateKey key, boolean force
    ) throws TemplateException {
        checkTemplateDir();
        File file = key.getFile();
        if (file.exists() && (! force)) {
            throw new TemplateException(
                    "A Template named \"" + key + "\" already exists in " +
                            TemplateDir.getAbsolutePath()
            );
        }
        try (OutputStream out = new FileOutputStream(file)) {
            doc.write(out);
        }
        catch (IOException e) {
            throw new TemplateException(
                "Couldn't add Template \"" + key + "\" in " +
                TemplateDir.getAbsolutePath(),
                e
            );
        }
        notifyListeners();
    }

    public static void removeTemplateDocument(TemplateKey key)
        throws TemplateException
    {
        checkTemplateDir();
        File file = key.getFile();
        boolean deleted = file.delete();
        if (! deleted) {
            throw new TemplateException(
                "Couldn't delete Template \"" + key + "\" in " +
                TemplateDir.getAbsolutePath()
            );
        }
        notifyListeners();
    }

    public static void setDefaultTemplate(ImageMetadata meta, TemplateKey key)
        throws TemplateException
    {
        val camera = meta.getCameraMake(true);
        if (camera == null) {
            return;
        }
        if (key != null) {
            XmlDocument xml = getTemplateDocument(key);
            TemplateKey defaultKey =
                new TemplateKey(CameraDefaultNamespace, camera);
            addTemplateDocument(xml, defaultKey, true);
        }
        else {
            key = new TemplateKey(CameraDefaultNamespace, camera);
            removeTemplateDocument(key);
        }
    }

    public static TemplateKey getDefaultTemplate(ImageMetadata meta) throws TemplateException {
        boolean isRaw = (meta.getImageType() instanceof RawImageType);
        if (!isRaw) {
            return null;
        }
        val camera = meta.getCameraMake(true);
        if (camera == null) {
            return null;
        }

        val key = new TemplateKey(CameraDefaultNamespace, camera);
        XmlDocument xml;
        try {
            xml = getTemplateDocument(key);
        }
        catch (TemplateException e) {
            // Counts as a missing template.
            throw new TemplateException("Template error: " + e.getMessage(), e);
        }
        if (xml == null) {
            // Compatibility for users of the LightZone 2 beta:
            // search for default templates in preferences.
            return getDefaultFromPrefs(meta);
        }
        return key;
    }

    // Legacy preferences-based default mechanism.
    private static TemplateKey getDefaultFromPrefs(ImageMetadata meta) {
        val camera = meta.getCameraMake(true);
        if (camera == null) {
            return null;
        }
        val name = Prefs.get(camera, null);
        if (name == null) {
            return null;
        }

        XmlDocument xml = null;
        val key = new TemplateKey(CameraDefaultNamespace, name);
        try {
            xml = getTemplateDocument(key);
        }
        catch (TemplateException e) {
            // Counts as a missing template.
            System.err.println(
                "Default template not found: " + e.getMessage()
            );
        }
        if (xml == null) {
            // The template's gone, so remove the camera key also:
            Prefs.remove(camera);
            return null;
        }
        return key;
    }

    private static void notifyListeners() {
        for (TemplateDatabaseListener listener : listeners) {
            listener.templatesChanged();
        }
    }

    private static void checkTemplateDir() throws TemplateException {
        if (TemplateDir != null) {
            return;
        }
        TemplateDir = new File(
            Platform.getPlatform().getLightZoneDocumentsDirectory(),
            "Templates"
        );
        if (! TemplateDir.isDirectory()) {
            boolean success = TemplateDir.mkdirs();
            if (! success) {
                String path = TemplateDir.getAbsolutePath();
                TemplateDir = null;
                throw new TemplateException(
                    "Couldn't initialize Template directory at " + path
                );
            }
        }
        if (System.getProperty("lightcrafts.debug") == null) {
            if (! TemplateDeployer.hasDeployed()) {
                TemplateDeployer.deploy();
            }
        }
        // Backwardsompatibility measures:
        // migrate templates from preferences into files.
        migratePrefsTemplates();
        // migrate templates from an old folder to the correct folder:
        migrateTemplateFolders();
        // migrate old templates with no namespace to a default namespace:
        migrateTemplateNamespace();
    }

    // In early pre-release versions of LightZone 2, Templates were stored in
    // Preferences.  This method is called at initialization and attempts to
    // copy any Templates from preferences into files.  If the copy is
    // successful, the preferences are erased.
    private static void migratePrefsTemplates() {
        try {
            val names = getPrefsTemplateNames();
            for (val name : names) {
                try {
                    XmlDocument doc = getPrefsTemplateDocument(name);
                    TemplateKey newKey = new TemplateKey("", name);
                    addTemplateDocument(doc, newKey, false);
                    removePrefsTemplateDocument(name);
                    System.out.println(
                        "Migrated old-style template " + name
                    );
                }
                catch (TemplateException e) {
                    System.err.println(
                        "Failed to migrate old-style template " + name
                    );
                }
            }
        }
        catch (TemplateException e) {
            System.err.println(
                "Couldn't access old-style templates"
            );
        }
    }

    // In later pre-release versions of LightZone 2, Templates on Windows were
    // stored as files under "Application Data\LightZone\Templates", instead
    // of "My Documents\LightZone\Templates".
    // In versions of LightZone earlier than 4.1.0~beta14, Templates on Linux were
    // stored as files under "~/LightZone/Templates", instead
    // of "~/.local/share/LightZone/Templates".
    // This method copies templates from the old folder to the new one.
    private static void migrateTemplateFolders() {
        final String oldPath;

        if (Platform.isWindows()) {
            oldPath = "Application Data\\LightZone\\Templates";
        }
        else if (Platform.isLinux()) {
            oldPath = "LightZone/Templates";
        }
        else {
            // Only on Windows and Linux did we move the template folder.
            return;
        }

        val home = System.getProperty("user.home");
        val oldTemplateDir = new File(home, oldPath);
        if (!oldTemplateDir.isDirectory()) {
            return;
        }
        val oldFiles = FileUtil.listFiles(oldTemplateDir);
        if (oldFiles == null) {
            return;
        }
        for (val oldFile : oldFiles) {
            val newFile = new File(TemplateDir, oldFile.getName());
            try {
                FileUtil.copyFile(oldFile, newFile);
                System.out.println(
                    "Copied a template from " +
                    oldFile + " to " + newFile
                );
                val msg = oldTemplateDir.delete()
                        ? "Deleted an old template at "
                        : "Failed to delete an old template at ";
                System.out.println(msg + oldFile);
            }
            catch (IOException e) {
                System.out.println(
                    "Failed to migrate a template from " +
                    oldFile + " to " + newFile + ": " +
                    e.getClass().getName() + " " + e.getMessage()
                );
                return;
            }
        }
        // All files in the old folder were successfully copied.
        val msg = oldTemplateDir.delete()
                ? "Deleted old template folder "
                : "Failed to delete old template folder ";
        System.out.println(msg + oldTemplateDir.getAbsolutePath());
    }

    // Older templates have no namespace.  Find templates whose namespace is
    // the empty string, and put them in a default namespace.
    private static void migrateTemplateNamespace() {
        try {
            List<TemplateKey> keys = TemplateDatabase.getTemplateKeys();
            for (TemplateKey key : keys) {
                String namespace = key.getNamespace();
                if (namespace.equals("")) {
                    try {
                        XmlDocument xml = TemplateDatabase.getTemplateDocument(key);
                        String name = key.getName();
                        TemplateKey newKey = new TemplateKey(DefaultNamespace, name);
                        TemplateDatabase.addTemplateDocument(xml, newKey, false);
                        TemplateDatabase.removeTemplateDocument(key);
                    }
                    catch (TemplateException e) {
                        // Skip this one and continue.
                        System.out.println(
                            "Template migration failed for " + key
                        );
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (TemplateException e) {
            // OK, just allow some templates with the empty namespace
            System.out.println("Failed to migrate template namespaces");
        }
    }

    // To help migrate LightZone 2 beta templates into files.
    private static List<String> getPrefsTemplateNames()
        throws TemplateException
    {
        try {
            val keys = Prefs.keys();
            ArrayList<String> names = new ArrayList<String>();
            for (val key : keys) {
                val name = getNameFromPrefsKey(key);
                if (name != null) {
                    names.add(name);
                }
            }
            Collections.sort(names);
            return names;
        }
        catch (BackingStoreException e) {
            throw new TemplateException("Couldn't read template names", e);
        }
    }

    // To help migrate LightZone 2 beta templates into files.
    private static XmlDocument getPrefsTemplateDocument(String name)
        throws TemplateException
    {
        try {
            val key = getPrefsKeyFromName(name);
            val text = Prefs.get(key, null);
            if (text == null) {
                throw new TemplateException(
                    "No template named \"" + name + "\""
                );
            }
            val bytes = text.getBytes();
            val in = new ByteArrayInputStream(bytes);
            return new XmlDocument(in);
        }
        catch (IOException e) {
            throw new TemplateException(
                "Couldn't access template \"" + name + "\"", e
            );
        }
    }

    private static void removePrefsTemplateDocument(String name) {
        val key = getPrefsKeyFromName(name);
        Prefs.remove(key);
    }

    private static String getPrefsKeyFromName(String name) {
        return TemplateSuffix + name;
    }

    private static String getNameFromPrefsKey(String key) {
        if (key.startsWith(TemplateSuffix)) {
            return key.replaceFirst(TemplateSuffix, "");
        }
        return null;
    }
}
