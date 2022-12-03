/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor.assoc;

import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.ui.editor.LightweightDocument;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.*;

/**
 * Keep track of all the directories that Documents have ever been saved to or
 * opened from, and explore these places to build an index mapping image files
 * to Document files.
 */

public class DocumentDatabase {

    // An in-memory copy of the Document directory paths:
    private static Set<File> Directories = new LinkedHashSet<File>();

    // Files to Collections of document Files:
    private static Map<File, Collection<File>> ImagesToDocs =
        new HashMap<File, Collection<File>>();

    // DocumentDatabaseListeners (WeakReferences to DocumentDatabaseListeners):
    private static List<WeakReference<DocumentDatabaseListener>> Listeners =
        new LinkedList<WeakReference<DocumentDatabaseListener>>();

    // DocumentInterpreters pick image pointers out of files, by file suffix:
    private static Map<String, DocumentInterpreter> Interpreters =
        new HashMap<String, DocumentInterpreter>();

    static {
        // Initialize the naive DocumentInterpreter, where the LZN structure
        // has just been serialized.
        addDocumentInterpreter(
            new DocumentInterpreter() {
                public File getImageFile(File file) throws IOException {
                    if (! FileUtil.getExtensionOf(file).equals("lzn")) {
                        return null;
                    }
                    file = file.getCanonicalFile();
                    LightweightDocument doc = new LightweightDocument(file);
                    File imageFile = doc.getImageFile().getCanonicalFile();
                    return imageFile;
                }
                public Collection<String> getSuffixes() {
                    return Collections.singleton("lzn");
                }
            }
        );
    }

    /**
     * This class provides a static utility and cannot be constructed.
     */
    private DocumentDatabase() {
    }

    public static void addDocumentInterpreter(DocumentInterpreter interpreter) {
        Collection<String> suffixes = interpreter.getSuffixes();
        for (String suffix : suffixes) {
            Interpreters.put(suffix, interpreter);
        }
    }

    /**
     * Try to find an image file with the given name by trying all the
     * image directories found in all known Documents, as well as all known
     * Document directories.  This can be an expensive operation, but it's
     * useful when a Document can't locate its image.
     */
    public static File[] findImageFiles(String name) {
        Set<File> dirs = new HashSet<File>();
        dirs.addAll(Directories);
        for (File file : ImagesToDocs.keySet()) {
            File imageDir = file.getParentFile();
            if (imageDir != null) {
                dirs.add(imageDir);
            }
        }
        ArrayList<File> files = new ArrayList<File>();
        for (File dir : dirs) {
            File file = new File(dir, name);
            if (file.isFile()) {
                files.add(file);
            }
        }
        return files.toArray(new File[0]);
    }

    /**
     * Add a DocumentDatabaseListener to hear about changes to the set of
     * Document files associated to an image File.
     */
    public static void addListener(DocumentDatabaseListener listener) {
        Listeners.add(new WeakReference<DocumentDatabaseListener>(listener));
    }

    /**
     * Adds the parent of the given File to the database.  This is just a
     * convenience method for {@link #addDocumentDirectory(File)}, which may
     * be faster if the parent of the File has already been added.
     */
    public static void addDocumentFile(File file) {
        if (file.isFile()) {
            File dir = file.getParentFile();
            if (! directoriesContains(dir)) {
                addToDirectories(dir);
            }
            readDirectory(dir);
        }
    }

    private static void addToDirectories(File dir) {
        try {
            dir = dir.getCanonicalFile();
            Directories.add(dir);
        }
        catch (IOException e) {
            // Do nothing.  The directory will just be excluded from the DB.
        }
    }

    private static boolean directoriesContains(File dir) {
        try {
            dir = dir.getCanonicalFile();
            return Directories.contains(dir);
        }
        catch (IOException e) {
            return false;
        }
    }

    /**
     * Add the given directory to the database.  Any Document files in
     * the directory will be added to the index.
     */
    public static void addDocumentDirectory(File dir) {
        if (dir.isDirectory()) {
            addToDirectories(dir);
        }
        readDirectory(dir);
    }

    /**
     * Add the given directory to the database, and recurse through its
     * descendants, adding any discovered Document files to the index.  If
     * the given File is a normal file, do this with its parent instead.
     */
    public static void addDocumentDirectoryRecurse(File dir) {
        if (dir.isDirectory()) {
            readRecurse(dir);
        }
    }

    public static void addDirectories(File[] dirs) {
        for (File dir : dirs) {
            addDocumentDirectory(dir);
        }
    }

    public static List<File> getDocumentsForImage(File imageFile) {
        try {
            imageFile = imageFile.getCanonicalFile();
            Collection<File> docs = ImagesToDocs.get(imageFile);
            if (docs == null) {
                docs = new LinkedList<File>();
                ImagesToDocs.put(imageFile, docs);
            }
            return new LinkedList<File>(docs);
        }
        catch (IOException e) {
            // report no associated Documents
            return Collections.emptyList();
        }
    }

    public static Collection<File> getAssociatedDocuments(File docFile) {
        try {
            LightweightDocument doc = new LightweightDocument(docFile);
            File imageFile = doc.getImageFile().getCanonicalFile();
            return new LinkedList<File>(ImagesToDocs.get(imageFile));
        }
        catch (IOException e) {
            // do nothing
        }
        return Collections.emptySet();
    }

    /**
     * Attempt to interpret the given File as a saved Document.  If this
     * succeeds, then associate the File with the corresponding image File
     * in the index.
     */
    private static void readFile(File file) {
        String suffix = FileUtil.getExtensionOf(file);
        if (suffix == null) {
            return;
        }
        DocumentInterpreter interp = Interpreters.get(suffix);
        if (interp == null) {
            return;
        }
        try {
            file = file.getCanonicalFile();
            File imageFile = interp.getImageFile(file);
            if (imageFile == null) {
                return;
            }
            Collection<File> docs = ImagesToDocs.get(imageFile);
            if (docs == null) {
                docs = new LinkedList<File>();
                ImagesToDocs.put(imageFile, docs);
            }
            if (! docs.contains(file)) {
                docs.add(file);
                notifyListeners(imageFile);
            }
        }
        catch (XMLException e) {
            // Encountered a file that is not an editor file--so what?
        }
        catch (IOException e) {
            System.err.println(
                "DocDB: can't read " + file.getName() + ": " +
                e.getMessage()
            );
        }
    }

    /**
     * Examine all files in the given directory, and try to import them into
     * the index by calling readFile().
     */
    private static void readDirectory(File dir) {
        File[] files = FileUtil.listFiles(dir);
        if (files != null) {    // not a directory, or an I/O error
            for (File file : files) {
                if (file.isFile()) {
                    readFile(file);
                }
            }
        }
    }

    /**
     * Notify listeners that the set of Document files associated with the
     * given image File has changed.
     */
    private static void notifyListeners(File imageFile) {
        for (Iterator i=Listeners.iterator(); i.hasNext(); ) {
            WeakReference ref = (WeakReference) i.next();
            DocumentDatabaseListener listener =
                (DocumentDatabaseListener) ref.get();
            if (listener != null) {
                listener.docFilesChanged(imageFile);
            }
            else {
                i.remove();
            }
        }
    }

    /**
     * If the given File is a normal file, call readFile().  Otherwise, if the
     * given File is a directory, and if this directory is not in the
     * database, then add it to the database and call readRecurse() on all
     * its children.
     */
    private static void readRecurse(File file) {
        if (file.isFile()) {
            readFile(file);
        }
        else if (file.isDirectory()) {
            if (! directoriesContains(file)) {
                addToDirectories(file);
            }
            File[] children = FileUtil.listFiles(file);
            if (children != null) {
                for (File child : children) {
                    readRecurse(child);
                }
            }
        }
    }

    /**
     * We may have a default LZN file in resources, to auto-correct an
     * image.  It depends on the metadata, and if we don't have one for the
     * metadata, or if there is an IO problem, we return null.
     * <p>
     * The XmlDocument that is returned is guaranteed to have a broken
     * reference to its image file.  This must be patched up before
     * proceeding with Document initialization.
     */
    public static XmlDocument getDefaultDocument(ImageMetadata meta) {
        URL url = DefaultDocuments.getDefaultDocumentUrl(meta);
        if (url == null) {
            return null;
        }
        try (InputStream in = url.openStream()) {
            return new XmlDocument(in);
        }
        catch (IOException e) {
            if (DefaultDocuments.Debug) {
                System.err.print(
                    "Error reading default document at " + url + ": "
                );
                System.err.println(e.getMessage());
            }
            return null;
        }
    }
}
