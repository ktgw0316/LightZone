/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.batch;

import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;

import java.io.*;
import java.util.prefs.Preferences;

/**
 * A configuration bundle for TemplateApplicator, specifying:
 *
 *      a list of files that will be processed;
 *      a flag saying whether new tools replace or supplement existing tools;
 *      a flag saying whether always to create new versions or to overwrite
 *          existing versions; and
 *      if files will be created, a name pattern for the new files.
 */
public class BatchConfig {

    // Batch processor configurations are sticky:
    private final static Preferences Prefs =
        Preferences.userRoot().node("/com/lightcrafts/app/batch");

    // Name pattern for new files:
    public String name;

    // The format for output files:
    public ImageFileExportOptions export =
        JPEGImageType.INSTANCE.newExportOptions();

    // The place where output files should go:
    public File directory;

    void writeDebug(OutputStream out) {
        PrintWriter printer = new PrintWriter(out);
        printer.println("batch name: " + name);
        printer.println("output folder: " + directory.getAbsolutePath());
        printer.println("export options:");
        printer.flush();
        XmlDocument doc = new XmlDocument("Export");
        XmlNode root = doc.getRoot();
        export.write(root);
        try {
            doc.write(out);
        }
        catch (IOException e) {
            e.printStackTrace(printer);
        }
    }

    private final static String NewFileNameKey = "TemplateBatchName";
    private final static String ExportKey = "TemplateExportOptions";
    private final static String DirectoryKey = "TemplateOutputDirectory";

    public void saveToPrefs(String context) {
        Prefs.put(NewFileNameKey + context, name);

        String path = directory.getAbsolutePath();
        Prefs.put(DirectoryKey + context, path);

        XmlDocument doc = new XmlDocument(ExportKey);
        export.write(doc.getRoot());
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.write(out);
            String text = out.toString("UTF-8");
            Prefs.put(ExportKey + context, text);
        }
        catch (IOException e) {
            System.err.print(
                "Error saving BatchConfig preferences: "
            );
            System.err.print(e.getClass().getName() + " ");
            System.err.println(e.getMessage());
        }
    }

    public void restoreFromPrefs(String context) {
        name = Prefs.get(NewFileNameKey + context, "");

        String path = Prefs.get(DirectoryKey + context, null);
        if (path != null) {
            directory = new File(path);
        }
        else {
            directory = null;
        }
        String text = Prefs.get(ExportKey + context, null);
        if (text != null) {
            try {
                InputStream in = new ByteArrayInputStream(
                    text.getBytes("UTF-8")
                );
                XmlDocument doc = new XmlDocument(in);
                XmlNode root = doc.getRoot();
                export = (ImageFileExportOptions) ImageExportOptions.read(root);
            }
            catch (IOException e) {
                System.err.print(
                    "Error reading BatchConfig preferences: "
                );
                System.err.print(e.getClass().getName() + " ");
                System.err.println(e.getMessage());
                Prefs.remove(ExportKey + context);
            }
        }
    }
}
