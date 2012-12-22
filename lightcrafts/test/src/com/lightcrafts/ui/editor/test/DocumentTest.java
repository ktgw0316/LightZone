/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor.test;

import com.lightcrafts.app.Application;
import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnsupportedColorProfileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.ui.editor.Document;

import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;

/** Attempt to restore a saved document.  This will not work in a headless
  * environment.
  */
public class DocumentTest {

    // Regex patterns to compare against File names to skip during recursion
    // of the file system tree:
    private static String[] Excludes = new String[] {
        "CVS", "[Ii]mages"
    };

    private static boolean Error;

    private static String toString(File file) {
        return "\"" + file.getAbsolutePath() + "\"";
    }

    private static boolean isExcluded(File file) {
        String name = file.getName();
        for (int n=0; n<Excludes.length; n++) {
            String pattern = Excludes[n];
            if (name.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    public static boolean test(final File file) {
        if (! file.exists()) {
            System.err.println("Does not exist: " + toString(file));
            return false;
        }
        if (isExcluded(file)) {
            System.out.println("Skipping " + toString(file));
            return true;
        }
        System.out.println("Testing " + toString(file));
        Error = false;

        // This procedure should parallel Application.open():
        try {
            InputStream in = new FileInputStream(file);
            XmlDocument xmlDoc;
            try {
                xmlDoc = new XmlDocument(in);
            }
            catch (IOException e) {
                // Maybe it's an image:
                try {
                    ImageInfo info = ImageInfo.getInstanceFor(file);
                    ImageMetadata meta = info.getMetadata();
                    new Document(meta, null);
                }
                catch (BadImageFileException f) {
                    System.err.println(e.getMessage());
                }
                catch (IOException f) {
                    System.err.println(e.getMessage());
                }
                catch (UnsupportedColorProfileException f) {
                    System.err.println(e.getMessage());
                }
                return false;
            }
            new Document(xmlDoc, null);
        }
        catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            Error = true;
        }
        catch (XMLException e) {
            System.err.println(e.getMessage());
            Error = true;
        }
        catch (BadImageFileException e) {
            System.err.println(e.getMessage());
            Error = true;
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            Error = true;
        }
        catch (Throwable t) {
            t.printStackTrace(System.err);
            Error = true;
        }
        return ! Error;
    }

    public static boolean test(File[] files) {
        boolean success = true;
        for (int n=0; n<files.length; n++) {
            File file = files[n];
            if (file.isDirectory()) {
                success = success && testDirectory(file);
            }
            else {
                success = success && test(file);
            }
        }
        return success;
    }

    public static boolean testDirectory(File dir) {
        if (! dir.isDirectory()) {
            System.err.println("Not a directory: " + toString(dir));
            return false;
        }
        if (isExcluded(dir)) {
            System.out.println("Skipping " + toString(dir));
            return true;
        }
        File[] files = dir.listFiles();
        return test(files);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: DocumentTest (file) [file] [file] ...");
        }
        Application.main(new String[] {});

        // Initialize and make sure it worked:
        try {
            EventQueue.invokeAndWait(new Runnable() { public void run() {} });
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
            System.err.println("Error in initialization:");
            e.getCause().printStackTrace(System.err);
            System.out.println("DocumentTest failed");
            return;
        }
        boolean success = true;

        for (int n=0; n<args.length; n++) {
            File file = new File(args[n]);
            if (file.isDirectory()) {
                success = success && testDirectory(file);
            }
            else {
                success = success && test(file);
            }
        }
        if (success) {
            System.out.println("DocumentTest succeeded");
        }
        else {
            System.out.println("DocumentTest failed");
        }
        // Quit (the import dialog will linger):
        try {
            EventQueue.invokeAndWait(
                new Runnable() {
                    public void run() {
                        Application.quit();
                    }
                }
            );
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
            System.err.println("Error in quit:");
            e.getCause().printStackTrace(System.err);
        }
    }
}
