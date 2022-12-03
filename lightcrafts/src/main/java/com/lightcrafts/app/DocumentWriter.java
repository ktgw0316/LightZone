/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.app.other.OtherApplication;
import com.lightcrafts.app.other.LightroomApplication;
import com.lightcrafts.app.other.UnknownApplication;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.types.LZNImageType;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.CachedImage;
import javax.media.jai.PlanarImage;
import com.lightcrafts.model.Engine;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.browser.model.PreviewUpdater;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.export.SaveOptions;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import lombok.val;

/**
 * Lengthy procedures that get run during save and export.
 */
public class DocumentWriter {

    /**
     * Save the given Document to a File, reporting progress during lengthy
     * jobs using the given ProgressThread.  The XMLException can only arise
     * when surprises occur in the LZN structure as it is navigated for
     * features that mangle XML like "copy original" and "multilayer TIFF".
     */
    public static boolean save(
        Document doc, ComboFrame frame, boolean saveDirectly,
        ProgressThread progress
    ) throws IOException {
        // Construct the LZN data that encode the Document's state:
        val xml = new XmlDocument(
            Application.LznNamespace, "LightZoneTransform"
        );
        doc.save(xml.getRoot());

        try {
            val options = doc.getSaveOptions();

            if (options.isLzn()) {
                // Just write the XML to a file, with a thumbnail and a preview:
                frame.pause();
                saveLzn(doc, xml);
                frame.resume();
                return true;
            }

            // We're performing some kind of Engine export:
            val engine = doc.getEngine();
            val export = SaveOptions.getExportOptions(options);
            if (export == null) {
                return false;
            }
            val app = (OtherApplication)doc.getSource();

            // Mangle LZN and add it to the export options as appropriate:
            val shouldAddLzn = mangleLzn(xml, options, app);
            if (shouldAddLzn) {
                addLznMetadata(export, xml);
            }

            if (progress != null) {
                export(engine, export, progress);
                return true;
            }

            val message = (app != null && app != UnknownApplication.INSTANCE
                    && saveDirectly)
                    ? LOCALE.get("SavingToMessage", app.getName())
                    : LOCALE.get("SavingMessage");
            return exportWithDialog(engine, export, message, frame);
        }
        catch (XMLException e) {
            throw new IOException(
                "Internal error in XML mangling: " + e.getMessage()
            );
        }
    }

    private static boolean mangleLzn(
            XmlDocument xml,
            SaveOptions options,
            OtherApplication app
    ) throws XMLException {
        if (options.isSidecarJpeg() || options.isSidecarTiff()) {
            if (app instanceof LightroomApplication) {
                val file = LightroomApplication.getOriginalFile(
                        options.getFile()
                );
                mangleLznSidecarFile(xml,file);
            }
            return true;
        }
        else if (options.isMultilayerTiff()) {
            mangleLznMultilayerTiff(xml);
            return true;
        }
        return false;
    }

    /**
     * Call DocumentWriter.export() under a progress dialog.
     */
    static boolean exportWithDialog(
        final Engine engine,
        final ImageExportOptions options,
        final String title,
        final Frame parent
    ) throws IOException {
        val dialog = Platform.getPlatform().getProgressDialog();
        val thread = new ProgressThread(dialog) {
            @Override
            public void run() {
                try {
                    // Write the file:
                    export(engine, options, this);
                }
                catch (IOException e) {
                    // This exception should be unpacked in the error handling
                    // below, following dialog.showProgress().
                    throw new RuntimeException(e);
                }
            }
        };
        dialog.showProgress(parent, thread, title, 0, 10, true);

        // Unpack any Throwable, in case it hides a checked exception:
        val error = dialog.getThrown();
        if (error != null) {
            if ( error instanceof IOException )
                throw (IOException) error;
            if ( error instanceof RuntimeException )
                throw (RuntimeException) error;
            throw new RuntimeException( error );
        }
        return !thread.isCanceled();
    }

    /**
     * Write the XML to a file as-is, and add a thumbnail for the browser.
     */
    private static void saveLzn(Document doc, XmlDocument xmlDoc)
            throws IOException {
        val engine = doc.getEngine();
        val options = doc.getSaveOptions();
        val file = options.getFile();

        // Fill up the LZN file:
        try (OutputStream out = new FileOutputStream(file)) {
            xmlDoc.write(out);
        }

        // Add thumbnail data for the browser:
        val thumbRendering = (PlanarImage) engine.getRendering(new Dimension(320, 320));
        // divorce the preview from the document
        val thumb = new CachedImage(thumbRendering, JAIContext.fileCache);
        val info = ImageInfo.getInstanceFor(file);
        LZNImageType.INSTANCE.putImage(info, thumb);

        // Cache a high resolution preview:
        val size = PreviewUpdater.PreviewSize;
        val previewRendering = (PlanarImage) engine.getRendering(new Dimension(size, size));
        // divorce the preview from the document
        val preview = new CachedImage(previewRendering, JAIContext.fileCache);
        PreviewUpdater.cachePreviewForImage(file, preview);
    }

    /**
     * Set the "auxiliary data" on the ImageExportOptions
     * to an XMP structure derived from the contents of the XmlDocument while
     * preserving any preexisting XMP in the given metadata.
     */
    private static void addLznMetadata(ImageExportOptions export,
                                       XmlDocument xml) throws IOException {
        val out = new ByteArrayOutputStream();
        xml.write(out);
        val bytes = out.toByteArray();
        export.setAuxData(bytes);
    }

    private static void mangleLznSidecarFile(XmlDocument xml, File file)
        throws XMLException
    {
        // Tags cloned from com.lightcrafts.ui.editor.Document:
        val root = xml.getRoot();
        val imageNode = root.getChild("Image");
        imageNode.setAttribute("path", file.getAbsolutePath());
        imageNode.setAttribute("relativePath", file.getName());
    }

    private static void mangleLznMultilayerTiff(XmlDocument xml)
        throws XMLException
    {
        // Tags cloned from com.lightcrafts.ui.editor.Document:
        val root = xml.getRoot();
        val imageNode = root.getChild("Image");
        imageNode.setAttribute("path", "");
        imageNode.setAttribute("relativePath", "");
        imageNode.setAttribute("self", "true");
    }

    /**
     * Call Engine.export() with some options and a progress object for
     * feedback.  This works by creating a temp file, streaming into that,
     * removing the final destination file if it exists, and then renaming
     * the temp file.  If anything goes wrong, the temp file gets cleaned up.
     */
    public static void export(
        Engine engine, ImageExportOptions options, ProgressThread progress
    ) throws IOException {
        // TODO: Java7 nio2
        File tempFile = null;
        try {
            // Set up the temp file where the export goes first:
            val exportFile = options.getExportFile();
            val exportDir = exportFile.getParentFile();
            tempFile = File.createTempFile(
                "LZExport", ".tmp", exportDir
            );
            options.setExportFile(tempFile);

            // Write to the temp file:
            engine.write(progress, options);

            // Restore the final destination file:

            options.setExportFile(exportFile);

            // First unlink any file that is in the way:
            if (exportFile.exists()) {
                boolean deleteOK = exportFile.delete();
                if (! deleteOK) {
                    // What side effects does closeAll() have on other threads?
                    ImageInfo.closeAll();
                    deleteOK = exportFile.delete();
                }
                if (! deleteOK) {
                    throw new IOException(
                        LOCALE.get("ExportDeleteError", exportFile.getPath())
                    );
                }
            }
            // Then move the temp file to the final destination:
            boolean renameOK = tempFile.renameTo(exportFile);
            if (! renameOK) {
                // What side effects does closeAll() have on other threads?
                ImageInfo.closeAll();
                renameOK = tempFile.renameTo(exportFile);
            }
            if (! renameOK) {
                throw new IOException(
                    LOCALE.get(
                        "ExportRenameError",
                        tempFile.getPath(),
                        exportFile.getPath()
                    )
                );
            }
        }
        finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }
}
