/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.app.batch.BatchConfig;
import com.lightcrafts.app.batch.BatchConfigurator;
import com.lightcrafts.app.batch.BatchProcessor;
import com.lightcrafts.app.batch.SendDialog;
import com.lightcrafts.app.menu.ComboFrameMenuBar;
import com.lightcrafts.app.menu.WindowMenu;
import com.lightcrafts.app.other.OtherApplication;
import com.lightcrafts.image.*;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.export.ImageFileExportOptions;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.*;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.model.Engine;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.PrintSettings;
import com.lightcrafts.model.Scale;
import com.lightcrafts.platform.*;
import com.lightcrafts.prefs.PreferencesDialog;
import com.lightcrafts.splash.AboutDialog;
import com.lightcrafts.splash.SplashWindow;
import com.lightcrafts.splash.StartupProgress;
import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.templates.TemplateKey;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.editor.*;
import com.lightcrafts.ui.editor.assoc.DocumentDatabase;
import com.lightcrafts.ui.editor.assoc.DocumentInterpreter;
import com.lightcrafts.ui.export.ExportLogic;
import com.lightcrafts.ui.export.ExportNameUtility;
import com.lightcrafts.ui.export.SaveOptions;
import com.lightcrafts.ui.print.PrintLayoutDialog;
import com.lightcrafts.ui.print.PrintLayoutModel;
import com.lightcrafts.ui.templates.TemplateList;
import com.lightcrafts.ui.help.HelpConstants;
import com.lightcrafts.utils.TerseLoggingHandler;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** This class collects static methods and structures for top-level document
  * management.  These methods handle startup processing; window management;
  * look-and-feel configuration; document-level actions like open, save,
  * close, import, and export; and shutdown.
  */

public class Application {

    public static final String LznNamespace =
        "http://www.lightcrafts.com/lightzone/LightZoneTransform";

    public static final String LznPrefix = "lzn";

    private static Rectangle InitialFrameBounds; // First window bounds

    private static int InitialFrameState;    // First Frame "extended" state

    private static StartupProgress Startup = new StartupProgress();

    private static int RecentCount = 5;     // Max recent file count

    private static LinkedList<ComboFrame> Current =
        new LinkedList<ComboFrame>();

    private static LinkedList<File> RecentFiles = new LinkedList<File>();

    private static LinkedList<File> RecentFolders = new LinkedList<File>();

    private static File LastOpenPath;       // Most recent "Open" selection

    private static SaveOptions LastSaveOptions;     // Most recent "Save"

    private static ImageExportOptions LastExportOptions; // Most recent "Export"

    private static PrintLayoutModel LastPrintLayout;     // Most recent "Print"

    private static boolean IsQuitting;  // Detect shutdown in progress

    private static Platform Env = Platform.getPlatform();

    public static void setStartupProgress(StartupProgress startup) {
        Startup = startup;
    }

    public static void open(ComboFrame parent) {
        FileChooser chooser = Env.getFileChooser();
        File file = chooser.openFile(
            LOCALE.get("OpenFileDialogTitle"),
            LastOpenPath,
            parent,
            ImageFilenameFilter.INSTANCE
        );
        if (file != null) {
            ComboFrame frame = getFrameForFile(file);
            if (frame != parent) {
                open(file, parent, null);
            }
            else {
                parent.requestFocus();
            }
            LastOpenPath = file.getParentFile();
            savePrefs();
        }
    }

    public static void open(ComboFrame frame, File file) {
        ComboFrame priorFrame = getFrameForFile(file);
        if (priorFrame != null) {
            priorFrame.requestFocus();
            return;
        }
        // The IDE event handler calls this with null, because it
        // doesn't know anything about frames.
        if (frame == null) {
            // See if there's an active frame:
            frame = getActiveFrame();
            if (frame == null) {
                // Otherwise, open into a new window.
                frame = openEmpty();
            }
        }
        open(file, frame, null);
    }

    /**
     * Open an image file at the request of some other application.
     */
    public static void openFrom(File file, OtherApplication otherApp) {
        ComboFrame priorFrame = getFrameForFile(file);
        if (priorFrame != null) {
            priorFrame.requestFocus();
            return;
        }
        // The desktop event handlers don't know anything about frames.
        // See if there's an active frame:
        ComboFrame frame = getActiveFrame();
        if (frame == null) {
            // Otherwise, open into a new window.
            frame = openEmpty();
        }
        open(file, frame, otherApp);
    }

    public static void reOpen(ComboFrame frame) {
        Document doc = frame.getDocument();
        File file = doc.getFile();
        ImageMetadata meta = doc.getMetadata();
        OtherApplication otherApp = (OtherApplication) doc.getSource();
        if (file != null) {
            open(file, frame, otherApp);
        }
        else {
            open(meta.getFile(), frame, otherApp);
        }
    }

    static void open(
        final File file, final ComboFrame frame, final OtherApplication otherApp
    ) {
        Document doc = frame.getDocument();
        if ((doc != null) && doc.isDirty()) {
            int result = askToSaveChanges(frame);
            if (result == SAVE_YES) {
                boolean saved = save(frame, true);
                if (! saved) {
                    return;
                }
            }
            else if (result == SAVE_CANCEL) {
                return;
            }
            else if (result < 0) {
                // The dialog was disposed without any selection:
                return;
            }
        }
        // Kick off Document initialization, and display the Document in the
        // ComboFrame if everything works out.
        DocumentInitializer.createDocument(
            file, frame,
            new DocumentInitializerListener() {
                public void documentStarted() {
                }
                public void documentInitialized(Document doc) {
                    if (doc != null) {
                        frame.pause();
                        try {
                            // Ensure the source is set before frame init:
                            doc.setSource(otherApp);
                            ComboFrame docFrame = show(doc, frame);
                            // Ensure layout is complete before zoom-to-fit:
                            docFrame.validate();
                            setInitialSize(doc);
                        }
                        finally {
                            frame.resume();
                        }
                    }
                }
                public void documentCancelled() {
                    System.err.println("Document cancelled");
                }
                public void documentFailed(Throwable t) {
                    handleDocInitError(t, frame);
                }
            }
        );
    }

    private static void handleDocInitError(Throwable t, ComboFrame frame) {
        try {
            throw t;
        }
        catch (BadImageFileException e) {
            showError(LOCALE.get("InvalidImageFileError"), e, frame);
        }
        catch (Document.MissingImageFileException e) {
            showError(LOCALE.get("UnknownOriginalFileError"), e, frame);
        }
        catch (XMLException e) {
            showError(LOCALE.get("MalformedLznFileError"), e, frame);
        }
        catch (IOException e) {
            showError(LOCALE.get("IOFileError"), e, frame);
        }
        catch (OutOfMemoryError e) {
            showError(LOCALE.get("InsufficientMemoryFileError"), e, frame);
        }
        catch (UnknownImageTypeException e) {
            showError(LOCALE.get("ImageFormatFileError"), e, frame);
        }
        catch (UnsupportedColorProfileException e) {
            showError(LOCALE.get("UnsupportedCameraFileError"), e, frame);
        }
        catch (ColorProfileException e) {
            showError(LOCALE.get("UnsupportedColorProfileFileError"), e, frame);
        }
        catch (Throwable e) {
            showError(LOCALE.get("UnknownFileError"), e, frame);
        }
    }

    // This Document initialization method differs from the headless
    // initialization in createDocumentHeadless() by providing user
    // interaction during the initialization process:
    //
    //  1) A ProgressThread can give progress feedback and supports cancel.
    //
    //  2) A ComboFrame provides a parent for dialogs in case an original
    //     image file can't be found.
    //
    //  3) A preview RenderedImage gets passed to the Engine for fast painting
    //     before tiles are ready.

    static Document createDocument(
        File file,
        ComboFrame frame,
        ProgressThread cancel
    ) throws UserCanceledException,
             XMLException,
             UnknownImageTypeException,
             IOException,
             BadImageFileException,
             ColorProfileException,
             Document.MissingImageFileException
    {
        Document doc;

        // First try as a saved-document:
        DocumentReader.Interpretation interp = DocumentReader.read(file);

        // If it's somehow a saved document:
        if (interp != null) {

            XmlDocument xml = interp.xml;
            File imageFile = interp.imageFile;
            ImageInfo info = interp.info;

            if (imageFile == null) {
                // This is a symptom of a template file.
                if (file.getName().endsWith(".lzt")) {
                    int option = Env.getAlertDialog().showAlert(
                        frame,
                        LOCALE.get("TemplateQuestionMajor"),
                        LOCALE.get("TemplateQuestionMinor"),
                        AlertDialog.WARNING_ALERT,
                        LOCALE.get("TemplateImportOption"),
                        LOCALE.get("TemplateOpenOption"),
                        LOCALE.get("TemplateCancelOption")
                    );
                    if (option == 0) {
                        try {
                            InputStream in = new FileInputStream(file);
                            XmlDocument template = new XmlDocument(in);
                            TemplateKey key = TemplateKey.importKey(file);
                            TemplateDatabase.addTemplateDocument(
                                template, key, true
                            );
                            TemplateList.showDialog(frame);
                        }
                        catch (Throwable t) {
                            showError(
                                LOCALE.get(
                                    "TemplateImportError", file.getName()
                                ),
                                t, frame
                            );
                        }
                        return null;
                    }
                    else if (option == 2) {
                        return null;
                    }
                    // option == 1, let the initialization continue...
                }
                // LightweightDocument couldn't figure out the original image
                // path, so let Document throw its MissingImageFileException:
                doc = new Document(xml, null, info, cancel);
            }
            else {
                // Check for a missing image file:
                boolean hunted = false;
                if (! imageFile.exists()) {
                    // Isolate the image file name, admitting both unix and
                    // windows path syntax:
                    String imageFileName =
                        imageFile.getAbsolutePath().replaceAll(
                            ".*[/\\\\]", ""
                        );
                    // Try the DocumentDatabase:
                    File[] files =
                        DocumentDatabase.findImageFiles(imageFileName);

                    // Maybe check the Document file's directory directly,
                    // since DocumentDatabase is sometimes disabled:
                    if (files.length == 0) {
                        File docDir = file.getParentFile();
                        File altImageFile = new File(docDir, imageFileName);
                        if (altImageFile.isFile()) {
                            files = new File[1];
                            files[0] = altImageFile;
                        }
                    }
                    imageFile = DocumentImageSelector.chooseImageFile(
                        file, imageFile, files, LastOpenPath, frame
                    );
                    if (imageFile == null) {
                        // User cancelled.
                        return null;
                    }
                    hunted = true;
                }
                ImageInfo imageFileInfo = ImageInfo.getInstanceFor(imageFile);
                ImageMetadata meta = imageFileInfo.getMetadata();

                // Read the saved document:
                doc = new Document(xml, meta, info, cancel);
                if (hunted) {
                    doc.markDirty();
                }
            }
            DocumentDatabase.addDocumentFile(file);
        }
        else {
            // Maybe it's an image:
            ImageInfo info = ImageInfo.getInstanceFor(file);
            ImageMetadata meta = info.getMetadata();
            ImageType type = info.getImageType();

            // Maybe set up a template with default tools:
            XmlDocument xml = null;

            // First look for default settings in the user-defined templates:
            TemplateKey template = TemplateDatabase.getDefaultTemplate(meta);
            if (template != null) {
                try {
                    xml = TemplateDatabase.getTemplateDocument(template);
                }
                catch (TemplateDatabase.TemplateException e) {
                    // Let xml remain null, try the factory defaults.
                }
            }
            // Then look for factory default settings:
            if (xml == null) {
                xml = DocumentDatabase.getDefaultDocument(meta);
            }
            // Only apply default settings if the image is in a RAW format:
            boolean isRaw = (type instanceof RawImageType);

            if (isRaw && (xml != null)) {
                doc = new Document(xml, meta, null, cancel);
            }
            else {
                doc = new Document(meta, cancel);
            }
        }
        maybeAddRawAdjustments(doc);

        SaveOptions save = doc.getSaveOptions();

        // Make sure the save options point to the place the file was opened
        // from, in case it was moved since it was written:
        if (save != null) {
            save.setFile(file);
        }
        // Check for the legacy LZN saved document format, and mutate into the
        // current default format if the legacy format was used.
        ImageType type = ImageType.determineTypeByExtensionOf(file);
        boolean recentLzn = ((save != null) && save.isLzn());
        boolean oldLzn = (save == null) && type.equals(LZNImageType.INSTANCE);
        // (Basically, if save == null and it's not an original image.)
        if (recentLzn || oldLzn) {
            // A legacy file: clobber the save options with defaults.
            doc.setSaveOptions(null);
            save = getSaveOptions(doc);
            doc.setSaveOptions(save);
        }
        addToRecentFiles(file);

        return doc;
    }

    // This headless Document initialization differs from the behavior in
    // createDocument() in that it does not perform any user interaction
    // during the initialization process: no progress display, no cancel
    // capability, no dialogs for missing originals, and no Engine
    // preview image.

    public static Document createDocumentHeadless(File file)
        throws UserCanceledException,
               XMLException,
               UnknownImageTypeException,
               IOException,
               BadImageFileException,
               ColorProfileException,
               Document.MissingImageFileException
    {
        Document doc;

        // First try as a saved-document:
        DocumentReader.Interpretation interp = DocumentReader.read(file);

        // If it's somehow a saved document:
        if (interp != null) {
            if (interp.imageFile != null) {
                ImageInfo imageFileInfo =
                    ImageInfo.getInstanceFor(interp.imageFile);
                ImageMetadata meta = imageFileInfo.getMetadata();
                doc = new Document(interp.xml, meta, interp.info, null);
            }
            else {
                // Maybe an LZT.  Trigger the MissingImageFileException.
                doc = new Document(interp.xml);
            }
            DocumentDatabase.addDocumentFile(file);
        }
        else {
            // Maybe it's an image:
            ImageInfo info = ImageInfo.getInstanceFor(file);
            ImageMetadata meta = info.getMetadata();
            ImageType type = info.getImageType();

            // Maybe set up a template with default tools:
            XmlDocument xml = null;

            // First look for default settings in the user-defined templates:
            TemplateKey template = TemplateDatabase.getDefaultTemplate(meta);
            if (template != null) {
                try {
                    xml = TemplateDatabase.getTemplateDocument(template);
                }
                catch (TemplateDatabase.TemplateException e) {
                    // Let xml remain null, try the factory defaults.
                }
            }
            // Then look for factory default settings:
            if (xml == null) {
                xml = DocumentDatabase.getDefaultDocument(meta);
            }
            // Only apply default settings if the image is in a RAW format:
            boolean isRaw = (type instanceof RawImageType);

            if (isRaw && (xml != null)) {
                doc = new Document(xml, meta);
            }
            else {
                doc = new Document(meta);
            }
        }
        maybeAddRawAdjustments(doc);

        SaveOptions save = doc.getSaveOptions();

        // Make sure the save options point to the place the file was opened
        // from, in case it was moved since it was written:
        if (save != null) {
            save.setFile(file);
        }
        // Check for the legacy LZN saved document format, and mutate into the
        // current default format if the legacy format was used.
        ImageType type = ImageType.determineTypeByExtensionOf(file);
        boolean recentLzn = ((save != null) && save.isLzn());
        boolean oldLzn = (save == null) && type.equals(LZNImageType.INSTANCE);
        // (Basically, if save == null and it's not an original image.)
        if (recentLzn || oldLzn) {
            // A legacy file: clobber the save options with defaults.
            doc.setSaveOptions(null);
            save = getSaveOptions(doc);
            doc.setSaveOptions(save);
        }
        return doc;
    }

    // New Documents created from RAW files get a "RAW Adjustments" singleton
    // tool, if they don't have one already.  These tools can not be part of
    // the default RAW templates, because the tool's initial state depends on
    // the image itself.
    private static void maybeAddRawAdjustments(Document doc) {
        ImageMetadata meta = doc.getMetadata();
        ImageType type = meta.getImageType();
        if (type instanceof RawImageType) {
            if (! doc.hasRawAdjustments()) {
                Engine engine = doc.getEngine();
                OperationType rawType = engine.getRawAdjustmentsOperationType();
                Editor editor = doc.getEditor();
                editor.addControl(rawType, 0);
                doc.discardEdits();
                doc.markClean();
            }
        }
    }

    public static ComboFrame openEmpty() {
        ComboFrame frame = createNewComboFrame(null);
        addToCurrent(frame);
        frame.setVisible(true);
        return frame;
    }

    public static void openRecentFolder(ComboFrame frame, File folder) {
        // This can be called from the no-frame menu on the Mac.
        if (frame == null) {
            // See if there's an active frame:
            frame = getActiveFrame();
            if (frame == null) {
                // Otherwise, open into a new window.
                frame = openEmpty();
            }
        }
        frame.showRecentFolder(folder);
        addToRecentFolders(folder);
        savePrefs();
    }

    public static void notifyRecentFolder(File folder) {
        addToRecentFolders(folder);
        savePrefs();
    }

    public static boolean save(ComboFrame frame) {
        return save(frame, false);
    }

    private static boolean save( final ComboFrame frame,
                                 final boolean openPending ) {
        final Document doc = frame.getDocument();
        if ( doc == null ) {
            return false;
        }
        boolean saveDirectly = false;
        SaveOptions options = doc.getSaveOptions();
        if (options == null) {
            if (OtherApplicationShim.shouldSaveDirectly(doc)) {
                options = OtherApplicationShim.createExportOptions(doc);
                if (options == null) {
                    // Something went wrong in all the redundant I/O required
                    // by OtherApplication.
                    showError(LOCALE.get("DirectSaveError"), null, frame);
                    return false;
                }
                doc.setSaveOptions(options);
                saveDirectly = true;
            }
            else {
                options = getSaveOptions(doc);

                final FileChooser chooser =
                    Platform.getPlatform().getFileChooser();
                File file = options.getFile();
                file = chooser.saveFile(file, frame);

                if (file == null) {
                    return false;
                }
                options.setFile(file);

                // We've stopped exposing the multilayer TIFF option to users,
                // but the option can slip through via persisted options.
                if (options.isMultilayerTiff()) {
                    // Convert to a single-layer TIFF in this case.
                    final ImageExportOptions export =
                        SaveOptions.getExportOptions(options);
                    options = SaveOptions.createSidecarTiff(export);
                }
                LastSaveOptions = options;
                doc.setSaveOptions(options);
            }
        } else
            saveDirectly = options.shouldSaveDirectly();

        frame.showWait(LOCALE.get("SaveMessage"));

        final boolean isLzn = options.isLzn();
        final File saveFile = options.getFile();

        Throwable error = null;
//        Throwable error = BlockingExecutor.execute(
//            new BlockingExecutor.BlockingRunnable() {
//                public void run() throws IOException {
                    frame.pause();
                    try {
                        TemporaryEditorCommitState state = doc.saveStart();
                        DocumentWriter.save(doc, frame, saveDirectly, null);
                        doc.saveEnd(state);
                        if (!isLzn && OtherApplication.isIntegrationEnabled()) {
                            final OtherApplication app =
                                (OtherApplication)doc.getSource();
                            if (app != null) {
                                app.postSave(
                                    saveFile, saveDirectly, openPending
                                );
                            }
                        }
                    }
                    catch (Exception e) {
                        error = e;
                    }
                    finally {
                        frame.resume();
                    }
//                }
//            }
//        );
        frame.hideWait();

        if (error != null) {
            final File file = options.getFile();
            showError(
                LOCALE.get("SaveError", file.getPath()),
                error, frame
            );
            return false;
        }
        doc.markClean();

        // Don't let synthetic writeback options be persisted:
        if ( saveDirectly ) {
            doc.setSaveOptions(null);
        }
        final File file = options.getFile();
        DocumentDatabase.addDocumentFile(file);

        addToRecentFiles(file);
        savePrefs();

        return true;
    }

    public static SaveResult saveAs(ComboFrame frame) {
        Document doc = frame.getDocument();
        if (doc == null) {
            return SaveResult.DontSave;
        }
        SaveOptions options = getSaveOptions(doc);

        FileChooser chooser = Platform.getPlatform().getFileChooser();
        File file = options.getFile();
        file = chooser.saveFile(file, frame);

        if (file == null) {
            return SaveResult.Cancelled;
        }
        options.setFile(file);

        // We've stopped exposing the multilayer TIFF option to users,
        // but the option can slip through via persisted options.
        if (options.isMultilayerTiff()) {
            // Convert to a single-layer TIFF in this case.
            ImageExportOptions export =
                SaveOptions.getExportOptions(options);
            options = SaveOptions.createSidecarTiff(export);
        }
        LastSaveOptions = options;
        doc.setSaveOptions(options);
        boolean saved = save(frame);
        if (! saved) {
            return SaveResult.CouldntSave;
        }
        savePrefs();

        return SaveResult.Saved;
    }

    public static void saveAll() {
        for (ComboFrame frame : Current) {
            Document doc = frame.getDocument();
            if ((doc != null) && (doc.isDirty())) {
                save(frame);
            }
        }
    }

    public static boolean close(ComboFrame frame) {
        if (closeDocument(frame)) {
            savePrefs();
            removeFromCurrent(frame);
            frame.dispose();
            if (Platform.getType() != Platform.MacOSX) {
                maybeQuit();
            }
            return true;
        }
        return false;
    }

    // This handles ask-to-save and clearing the document on the given frame,
    // but it should only be called from ComboFrame so the auto-save logic
    // can be applied.
    public static boolean closeDocument(ComboFrame frame) {
        Document doc = frame.getDocument();
        if ((doc != null) && doc.isDirty()) {
            final int result = askToSaveChanges(frame);
            if (result == SAVE_YES) {
                boolean saved = save(frame);
                if (! saved) {
                    return false;
                }
            }
            else if (result == SAVE_CANCEL) {
                return false;
            }
            else if (result < 0) {
                // The dialog was disposed without the user selecting anything:
                return false;
            }
        }
        frame.setDocument(null);
        if (doc != null) {
            doc.dispose();
        }
        return true;
    }

    public static void closeDocumentForce(ComboFrame frame) {
        Document doc = frame.getDocument();
        frame.setDocument(null);
        if (doc != null) {
            doc.dispose();
        }
    }

    public static void quit() {
        // In case the 20 second wait before setting the startup flag has
        // not elapsed, set it here.
        StartupCrash.startupEnded();
        // Persist open documents in preferences.
        ArrayList<ComboFrame> frames = new ArrayList<ComboFrame>(Current);
        IsQuitting = true;
        for (ComboFrame frame : frames) {
            boolean closed = close(frame);
            if (!closed) {
                IsQuitting = false;
                return;
            }
            // The last one closing will trigger exit().
        }
        // Unless there are no active ComboFrames.
        savePrefs();
        System.exit(0);
    }

    public static boolean isQuitInProgress() {
        return IsQuitting;
    }

    public static void print(ComboFrame frame) {
        Document doc = frame.getDocument();
        if (doc != null) {
            print(frame, doc, null);
        }
    }

    /*
        On Mac OS X the CocoaPrinter triggers a return of the
        modal print dialog and would prematurely dispose the document,
        so we make a callback for the dialog dismissal.
        This is a hack, if anything breaks you know where to look...
     */

    private static class PrintDoneCallback {
        private final Document doc;

        PrintDoneCallback(Document doc) {
            this.doc = doc;
        }

        public void done() {
            doc.dispose();
            PrinterLayer printer = Env.getPrinterLayer();
            printer.dispose();
        }
    }

    public static void print(ComboFrame frame, File file) {
        try {
            Document doc = createDocument(file, frame, null);
            if (doc == null) {
                // For instance, couldn't locate the original image
                return;
            }
            print(frame, doc, new PrintDoneCallback(doc));
            // document disposal delegated to the done PrintDoneCallback
            // doc.dispose();
        }
        catch (Throwable t) {
            handleDocInitError(t, frame);
        }
    }

    public static void print(final ComboFrame frame, final Document doc, final PrintDoneCallback callback) {
        // Capture a preview image:
        Engine engine = doc.getEngine();
        RenderedImage image = engine.getRendering(new Dimension(400, 300));

        if (image instanceof PlanarImage) {
            image = ((PlanarImage) image).getAsBufferedImage();
        }
        // Restore the previous layout:
        PrintLayoutModel layout = doc.getPrintLayout();

        if (layout == null) {
            Dimension size = engine.getNaturalSize();
            // First try the most recent settings:
            if (LastPrintLayout != null) {
                layout = LastPrintLayout;
                layout.updateImageSize(size.width, size.height);
            }
            else {
                // Last resort is default settings:
                layout = new PrintLayoutModel(size.width, size.height);
            }
        }
        // Show the layout dialog:
        final PrintLayoutDialog dialog = new PrintLayoutDialog(
            (BufferedImage) image, layout, frame, LOCALE.get("PrintDialogTitle")
        );
        // Hook up behaviors for the dialog buttons:
        dialog.addCancelAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    dialog.dispose();
                    if (callback != null)
                        callback.done();
                }
            }
        );
        dialog.addDoneAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    PrintLayoutModel layout = dialog.getPrintLayout();
                    doc.setPrintLayout(layout);
                    dialog.dispose();
                    if (callback != null)
                        callback.done();
                    LastPrintLayout = layout;
                    savePrefs();
                }
            }
        );
        dialog.addPrintAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    PrintLayoutModel layout = dialog.getPrintLayout();
                    doc.setPrintLayout(layout);
                    printHeadless(frame, doc);
                    LastPrintLayout = layout;
                    savePrefs();
                }
            }
        );
        dialog.addPageSetupAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    PrintLayoutModel layout = dialog.getPrintLayout();
                    doc.setPrintLayout(layout);
                    pageSetup(doc);
                    layout = doc.getPrintLayout();
                    PageFormat format = layout.getPageFormat();
                    dialog.setPageFormat(format);
                }
            }
        );
        PrinterLayer printer = Env.getPrinterLayer();
        printer.initialize();

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);

        // dispose happens in PrintDoneCallback
        // printer.dispose();
    }

    public static boolean export(ComboFrame frame) {
        Document doc = frame.getDocument();
        if (doc == null) {
            return false;
        }
        return export(frame, doc);
    }

    public static boolean export(ComboFrame frame, File file) {
        try {
            Document doc = createDocument(file, frame, null);
            boolean result = export(frame, doc);
            doc.dispose();
            return result;
        }
        catch (Throwable t) {
            handleDocInitError(t, frame);
            return false;
        }
    }

    public static boolean export(ComboFrame frame, Document doc) {
        ImageExportOptions oldOptions = doc.getExportOptions();
        ImageMetadata meta = doc.getMetadata();
        final Engine engine = doc.getEngine();
        Dimension size = engine.getNaturalSize();

        ImageExportOptions newOptions;
        if (oldOptions != null) {
            // This Document has been exported before.
            newOptions = ExportLogic.getDefaultExportOptions(
                oldOptions, size
            );
        }
        else if (LastExportOptions != null) {
            // This Document has never been exported, but export has been used.
            File file = doc.getFile();
            if (file != null) {
                // This Document has been saved:
                newOptions = ExportLogic.getDefaultExportOptions(
                    LastExportOptions, meta, size, file.getName()
                );
            }
            else {
                // This Document not has been saved:
                newOptions = ExportLogic.getDefaultExportOptions(
                    LastExportOptions, meta, size
                );
            }
        }
        else {
            // Export has never been used.
            newOptions = ExportLogic.getDefaultExportOptions(
                meta, size
            );
        }
        // Show the export dialog using these defaults:
        FileChooser chooser = Platform.getPlatform().getFileChooser();
        ImageExportOptions options = chooser.exportFile(newOptions, frame);

        // User cancelled:
        if (options == null) {
            return false;
        }
        // Do the write:
        boolean success;
        frame.pause();
        try {
            success = DocumentWriter.exportWithDialog(
                engine, options, LOCALE.get("ExportMessage"), frame
            );
        }
        catch (Throwable e) {   // IOException, unchecked Exceptions
            showError(
                LOCALE.get("ExportError", options.getExportFile().toString()),
                e, frame
            );
            success = false;
        }
        finally {
            frame.resume();
        }
        if (! success) {
            // User canceled.
            File file = options.getExportFile();
            if (file != null) {
                file.delete();
            }
            return false;
        }
        doc.setExportOptions(options);

        LastExportOptions = options;
        addToRecentFiles(options.getExportFile());
        savePrefs();

        return true;
    }

    public static TemplateKey saveTemplate(ComboFrame frame, String namespace) {
        Document doc = frame.getDocument();
        if (doc == null) {
            return null;
        }
        // Loop until a unique name is selected or the dialog is cancelled:
        boolean done = false;
        TemplateKey key;
        do {
            XmlDocument xml = new XmlDocument("Template");  // See Document()
            XmlNode root = xml.getRoot();
            doc.saveTemplate(root);

            SaveTemplateDialog dialog = new SaveTemplateDialog();
            ImageMetadata meta = doc.getMetadata();
            key = dialog.showDialog(meta, xml, namespace, frame);
            if (key == null) {
                // Dialog was disposed, or the user cancelled.
                return null;
            }
            // First check if a template with this name already exists:
            XmlDocument conflict = null;
            try {
                conflict = TemplateDatabase.getTemplateDocument(key);
            }
            catch (TemplateDatabase.TemplateException e) {
                // Interpret as no preexisting template with this name.
            }
            if (conflict != null) {
                int replace = Env.getAlertDialog().showAlert(
                    frame,
                    LOCALE.get("TemplateClobberQuestionMajor", key.toString()),
                    LOCALE.get("TemplateClobberQuestionMinor"),
                    AlertDialog.WARNING_ALERT,
                    LOCALE.get("TemplateClobberReplaceOption"),
                    LOCALE.get("TemplateClobberCancelOption")
                );
                if (replace != 0) {
                    // Skip the procedure below and redo the dialog:
                    continue;
                }
            }
            // Everything is OK, so make the changes:
            try {
                xml = dialog.getModifiedTemplate();
                TemplateDatabase.addTemplateDocument(xml, key, true);
                if (dialog.isDefaultSelected()) {
                    TemplateDatabase.setDefaultTemplate(meta, key);
                }
            }
            catch (TemplateDatabase.TemplateException e) {
                showError(LOCALE.get("TemplateWriteError"), e, frame);
            }
            done = true;
        } while (! done);

        return key;
    }

    /**
     * Append the named template from TemplateDocuments in the current editor.
     */
    public static void applyTemplate(ComboFrame frame, TemplateKey key) {
        Document doc = frame.getDocument();
        if (doc == null) {
            return;
        }
        try {
            XmlDocument template = TemplateDatabase.getTemplateDocument(key);
            if (template != null) {
                XmlNode root = template.getRoot();
                doc.applyTemplate(root);
            }
            else {
                showError(
                    LOCALE.get("TemplateNameError", key.toString()), null, frame
                );
            }
        }
        catch (TemplateDatabase.TemplateException e) {
            showError(
                LOCALE.get("TemplateReadError", key.toString()), e, frame
            );
        }
        catch (XMLException e) {
            showError(
                "Template \"" + key.toString() + "\" is malformed", e, frame
            );
        }
        catch (Throwable t) {
            showError(
                LOCALE.get("TemplateGeneralError", key.toString()), t, frame
            );
        }
    }

    /**
     * Apply the named template from TemplateDocuments to the File[].
     */
    public static void applyTemplate(
        ComboFrame frame, File[] files, TemplateKey key
    ) {
        XmlDocument template;
        try {
            template = TemplateDatabase.getTemplateDocument(key);
        }
        catch (TemplateDatabase.TemplateException e) {
            showError(
                LOCALE.get("TemplateReadError", key.toString()), e, frame
            );
            return;
        }
        if (template == null) {
            showError(
                LOCALE.get("TemplateNameError", key.toString()), null, frame
            );
            return;
        }
        BatchConfig conf = new BatchConfig();
        conf.name = "";
        conf.export = (ImageFileExportOptions)
            SaveOptions.getExportOptions(SaveOptions.getDefaultSaveOptions());

        try {
            BatchProcessor.process(frame, files, template, conf);
        }
        catch (RuntimeException e) {
            showError(
                "An error occurred during batch processing.", e, frame
            );
        }
    }

    /**
     * Interpret the file as a template and apply to the File[].
     */
    public static void applyTemplate(
        ComboFrame frame, File[] files, File file
    ) {
        DocumentReader.Interpretation interp = DocumentReader.read(file);
        if (interp == null) {
            showError(
                LOCALE.get("TemplateInterpError", file.getName()), null, frame
            );
            return;
        }
        BatchConfig conf = new BatchConfig();
        conf.name = "";
        conf.export = (ImageFileExportOptions)
            SaveOptions.getExportOptions(SaveOptions.getDefaultSaveOptions());

        try {
            XmlDocument template = interp.xml;
            BatchProcessor.process(frame, files, template, conf);
        }
        catch (RuntimeException e) {
            showError(
                "An error occurred during batch processing.", e, frame
            );
        }
    }

    /**
     * Apply a null template to the File[], which is our way of performing
     * batch export.
     */
    public static void export(ComboFrame frame, File[] files) {
        BatchConfig conf = BatchConfigurator.showDialog(
            files, frame, true
        );
        if (conf != null) {
            BatchProcessor.process(frame, files, null, conf);
        }
    }

    /**
     * Perform the "send" operation, a form of batch export with narrowly
     * constrained parameters.
     */
    public static void send(ComboFrame frame, File[] files) {
        File folder = frame.getRecentFolder();
        String from = folder.getName();
        BatchConfig conf = SendDialog.showDialog(frame, from, files.length);
        if (conf != null) {
            BatchProcessor.process(frame, files, null, conf);

            if (conf.directory != null && files.length > 0) {
                Platform.getPlatform().showFileInFolder(conf.directory.getAbsolutePath());
            }
        }
    }

    public static List<File> getRecentFiles() {
        return new ArrayList<File>(RecentFiles);
    }

    public static void clearRecentFiles() {
        RecentFiles.clear();
        savePrefs();
    }

    public static List<File> getRecentFolders() {
        return new ArrayList<File>(RecentFolders);
    }

    public static void clearRecentFolders() {
        RecentFolders.clear();
        savePrefs();
    }

    public static List<ComboFrame> getCurrentFrames() {
        List<ComboFrame> frames = new ArrayList<ComboFrame>(Current);
        return frames;
    }

    public static void setLookAndFeel(LookAndFeel laf) {
        // We presume it's always OK to do nothing if this fails:
        try {
            UIManager.setLookAndFeel(laf);
            for (ComboFrame frame : Current) {
                SwingUtilities.updateComponentTreeUI(frame);
                frame.pack();
            }
        }
        catch (UnsupportedLookAndFeelException e) {
            showError("Error setting look and feel", e, null);
        }
    }

    public static void setLookAndFeel(String className) {
        // We presume it's always OK to do nothing if this fails:
        try {
            UIManager.setLookAndFeel(className);
            for (ComboFrame frame : Current) {
                SwingUtilities.updateComponentTreeUI(frame);
                frame.pack();
            }
        }
        catch (ClassNotFoundException e) {
            showError("Error setting look and feel", e, null);
        }
        catch (InstantiationException e) {
            showError("Error setting look and feel", e, null);
        }
        catch (IllegalAccessException e) {
            showError("Error setting look and feel", e, null);
        }
        catch (UnsupportedLookAndFeelException e) {
            showError("Error setting look and feel", e, null);
        }
    }

    public static Preferences getPreferences() {
        return Preferences.userNodeForPackage(Application.class);
    }

    public static void showError(String message, Throwable e, Frame frame) {
        if (e != null) {
            e.printStackTrace();
        }
        if (System.getProperty("dieOnError") != null) {
            System.exit(-1);
        }
        // The splash can conceal other dialogs:
        SplashWindow.disposeSplash();

        String detail = null;
        if (e != null) {
            detail = e.getMessage();
            if (detail == null) {
                detail = e.getClass().toString();
            }
        }
        Env.getAlertDialog().showAlert(
            frame, message, detail, AlertDialog.ERROR_ALERT,
            LOCALE.get("ErrorDialogOk")
        );
    }

    public static void showAbout() {
        ComboFrame frame = getActiveFrame();
        AboutDialog about = new AboutDialog(frame);
        about.centerOnScreen();
        about.setVisible(true);
    }

    public static void showPreferences() {
        PreferencesDialog.showDialog(getActiveFrame());
    }

    static ComboFrame getFrameForFile(File file) {
        for (ComboFrame frame : Current) {
            Document doc = frame.getDocument();
            if (doc != null) {
                File docFile = doc.getFile();
                if (docFile == null) {
                    docFile = doc.getMetadata().getFile();
                }
                if (file.equals(docFile)) {
                    long time = file.lastModified();
                    long docTime = docFile.lastModified();
                    if (time == docTime) {
                        return frame;
                    }
                }
            }
        }
        return null;
    }

    static void copyFiles(ComboFrame frame, List<File> files, File folder) {
        for (File source : files) {
            File target = new File(folder, source.getName());
            try {
                if (target.isFile()) {
                    throw new IOException(
                        LOCALE.get(
                            "MoveExistsMessage",
                            target.getName(),
                            folder.getName()
                        )
                    );
                }
                FileUtil.copyFile(source, target);
            }
            catch (IOException e) {
                showError(
                    LOCALE.get(
                        "MoveCopyFailedMessage",
                        source.getName(),
                        folder.getName()
                    ),
                    e, frame
                );
                return;
            }
        }
    }

    static void moveFiles(ComboFrame frame, List<File> files, File folder) {
        for (File source : files) {
            File target = new File(folder, source.getName());
            try {
                if (target.isFile()) {
                    throw new IOException(
                        LOCALE.get(
                            "MoveExistsMessage",
                            target.getName(),
                            folder.getName()
                        )
                    );
                }
                boolean renamed = source.renameTo(target);
                if (! renamed) {
                    FileUtil.copyFile(source, target);
                }
            }
            catch (IOException e) {
                showError(
                    LOCALE.get(
                        "MoveCopyFailedMessage",
                        source.getName(),
                        folder.getName()
                    ),
                    e, frame
                );
                return;
            }
        }
        for (File source : files) {
            if (source.isFile()) {
                boolean deleted = source.delete();
                if (! deleted) {
                    File oldFolder = source.getParentFile();
                    showError(
                        LOCALE.get(
                            "MoveDeleteFailedMessage",
                            source.getName(),
                            folder.getName(),
                            oldFolder.getName()
                        ),
                        null, frame
                    );
                }
            }
        }
    }

    // Make the ever-present invisible window on the Mac, with a menu.
    private static void openMacPlaceholderFrame() {
        JMenuBar menus = new ComboFrameMenuBar();
        JFrame frame = new JFrame();
        frame.setJMenuBar(menus);
        frame.setBounds(-1000000, -1000000, 0, 0);
        frame.setUndecorated(true);
        frame.setVisible(true);
    }

    private static final int SAVE_YES    = 0;
    private static final int SAVE_CANCEL = 1;

    private static int askToSaveChanges(ComboFrame frame) {
        return Env.getAlertDialog().showAlert(
            frame,
            LOCALE.get("SaveChangesQuestionMajor"),
            LOCALE.get("SaveChangesQuestionMinor"),
            AlertDialog.WARNING_ALERT,
            2,
            LOCALE.get("SaveChangesSaveOption"),
            LOCALE.get("SaveChangesCancelOption"),
            LOCALE.get("SaveChangesDontSaveOption")
        );
    }

    private static boolean askConfirmQuit(ComboFrame frame) {
        String ConfirmQuitKey = "ConfirmQuit";
        Preferences prefs = getPreferences();
        boolean ask = prefs.getBoolean(ConfirmQuitKey, true);
        int option = 0;
        if (ask) {
            String alwaysPrompt = LOCALE.get("ConfirmQuitAlwaysPrompt");
            JCheckBox alwaysCheck = new JCheckBox(alwaysPrompt);

            Box message = Box.createVerticalBox();
            message.add(new JLabel(LOCALE.get("ConfirmQuitQuestion")));
            message.add(alwaysCheck);

            option = JOptionPane.showOptionDialog(
                frame,
                message,
                "Quit LightZone",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[] {
                    LOCALE.get("ConfirmQuitConfirmOption"),
                    LOCALE.get("ConfirmQuitCancelOption")
                },
                LOCALE.get("ConfirmQuitConfirmOption")
            );
            if (option == 0) {
                if (alwaysCheck.isSelected()) {
                    prefs.putBoolean(ConfirmQuitKey, false);
                }
            }
        }
        return (option == 0);
    }

    private static ComboFrame createNewComboFrame(ComboFrame parent) {
        ComboFrame frame = new ComboFrame();
        frame.addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent event) {
                    ComboFrame frame = (ComboFrame) event.getWindow();
                    // On the Mac, we can close the last frame without quitting.
                    if (Platform.getType() != Platform.MacOSX) {
                        if (Current.size() == 1) {
                            boolean confirmed = askConfirmQuit(frame);
                            if (! confirmed) {
                                return;
                            }
                        }
                    }
                    // Trigger the standard cleanup, which results in quit:
                    close(frame);
                }
            }
        );
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.pack();
        setNewWindowBounds(frame, parent);
        return frame;
    }

    private static ComboFrame show(Document doc, ComboFrame frame) {
        if (frame == null) {
            frame = createNewComboFrame(null);
        }
        Document old = frame.getDocument();
        frame.setDocument(doc);
        if (old != null) {
            old.dispose();
        }
        addToCurrent(frame);
        savePrefs();

        frame.setVisible(true);

        return frame;
    }

    private static void setInitialSize(Document doc) {
        doc.zoomToFit();
        ScaleModel scales = doc.getScaleModel();
        Scale s = scales.getCurrentScale();
        if (s.getFactor() > 1f) {
            s = new Scale(1, 1);
            scales.setScale(s);
        }
    }

    static SaveOptions getSaveOptions(Document doc) {
        SaveOptions options = doc.getSaveOptions();
        if (options == null) {
            ImageMetadata meta = doc.getMetadata();

            Preferences prefs = getPreferences();
            boolean byOriginal = prefs.getBoolean("SaveByOriginal", true);
            File dir;
            if (byOriginal || LastSaveOptions == null) {
                dir = meta.getFile().getParentFile();
            }
            else {
                dir = LastSaveOptions.getFile().getParentFile();
            }
            options = SaveOptions.getDefaultSaveOptions();

            ImageFileExportOptions export =
                (ImageFileExportOptions) SaveOptions.getExportOptions(options);
            ImageType type = export.getImageType();

            File file = new File(dir, meta.getFile().getName());
            String name = ExportNameUtility.getBaseName(file);
            name = name + "_lzn." + type.getExtensions()[0];
            file = new File(name);
            file = ExportNameUtility.ensureNotExists(file);

            // Code for the "actual size" save preference:
            if (export.resizeWidth.getValue() == 0 &&
                export.resizeHeight.getValue() == 0
            ) {
                Engine engine = doc.getEngine();
                Dimension size = engine.getNaturalSize();
                options.updateSize(size);
            }
            options.setFile(file);
        }
        return options;
    }

    private static void pageSetup(Document doc) {
        PrintLayoutModel layout = doc.getPrintLayout();
        PageFormat format = layout.getPageFormat();
        format = Platform.getPlatform().getPrinterLayer().pageDialog(format);
        if (format != null)
            layout.setPageFormat(format);
    }

    private static void printHeadless(ComboFrame frame, Document doc) {

        // The Printable:
        final Engine engine = doc.getEngine();

        // The layout info:
        PrintLayoutModel layout = doc.getPrintLayout();

        // The Engine's layout sub-info:
        final PrintSettings settings = layout.getPrintSettings();

        PrinterLayer printer = Platform.getPlatform().getPrinterLayer();

        // The PageFormat:
        printer.setPageFormat(layout.getPageFormat());
        final PageFormat format = printer.getPageFormat();

        // Make up a name for the print job:
        String jobName;
        File file = doc.getFile();
        if (file != null) {
            jobName = file.getName();
        }
        else {
            ImageMetadata meta = doc.getMetadata();
            jobName = meta.getFile().getName();
        }
        printer.setJobName(jobName);

        boolean doPrint = printer.printDialog();

        if (doPrint) {
            ProgressDialog dialog = Platform.getPlatform().getProgressDialog();
            ProgressThread thread = new ProgressThread(dialog) {
                public void run() {
                    try {
                        engine.print(this, format, settings);
                    }
                    catch (PrinterException e) {
                        throw new RuntimeException(e);
                    }
                }
                public void cancel() {
                    engine.cancelPrint();
                }
            };
            dialog.showProgress(
                frame, thread, LOCALE.get("PrintingMessage"), 0, 0, true
            );
            Throwable error = dialog.getThrown();
            if (error != null) {
                if (error instanceof RuntimeException) {
                    throw (RuntimeException) error;
                }
                if (error instanceof PrinterAbortException) {
                    // Means print was deliberately cancelled; do nothing.
                    return;
                }
                showError(
                    LOCALE.get("PrintError"), error, frame
                );
            }
        }
    }

    private static void maybeQuit() {
        if (Current.isEmpty()) {
            System.exit(0);
        }
    }

    private static void addToRecentFiles(File file) {
        RecentFiles.remove(file);
        RecentFiles.addFirst(file);
        while (RecentFiles.size() > RecentCount) {
            RecentFiles.removeLast();
        }
    }

    private static void addToRecentFolders(File file) {
        RecentFolders.remove(file);
        RecentFolders.addFirst(file);
        while (RecentFolders.size() > RecentCount) {
            File old = RecentFolders.removeLast();
            ComboFrame.clearFolder(old);
        }
    }

    private static void addToCurrent(ComboFrame frame) {
        Current.remove(frame);
        Current.addFirst(frame);
        WindowMenu.updateAll();
    }

    private static void removeFromCurrent(ComboFrame frame) {
        Current.remove(frame);
        WindowMenu.updateAll();
    }

    private static ComboFrame getActiveFrame() {
        for (ComboFrame frame : Current) {
            if (frame.isActive()) {
                return frame;
            }
        }
        // There is often no active window, like during event queue
        // tasks that have already disposed a modal dialog.  In these
        // cases, we use the most recently active ComboFrame.
        return ComboFrame.LastActiveComboFrame;
    }

    private static void verifyLibraries() {
        String[] libs = new String[] {
            "DCRaw", "Segment", "JAI", "FASTJAI", "fbf", "LCJPEG", "LCTIFF"
        };
        for (String lib : libs) {
            try {
                System.loadLibrary(lib);
            }
            catch (UnsatisfiedLinkError e) {
                showError(
                    "Couldn't link with native library: " + "\"" + lib, e, null
                );
            }
        }
        try {
            Env.loadLibraries();
        }
        catch (UnsatisfiedLinkError e) {
            showError(
                "Couldn't link with platform-specific native libraries", e, null
            );
        }
        try {
            // Run our expensive static initializers in JAIContext:
            Startup.startupMessage(LOCALE.get("StartupEngineMessage"));
            Class.forName("com.lightcrafts.jai.JAIContext");

            // preload jai_core.jar, jai_codec.jar, jai_imageio.jar:
            Startup.startupMessage(LOCALE.get("StartupClassesMessage"));
            Class.forName("com.lightcrafts.mediax.jai.JAI");
            Class.forName("com.lightcrafts.media.jai.codec.ImageCodec");
        }
        catch (ClassNotFoundException e) {
            showError(
                "Couldn't link with image processing class libraries", e, null
            );
        }
    }

    private static void scanProfiles() {
        // These Platform methods cache their results, which can be expensive
        // to determine the first time through.
        Env.getExportProfiles();
        Env.getPrinterProfiles();
    }

    private static void initLogging() {
        // Abbreviate metadata error messages, which can be scroll blinding.
        Logger logger = Logger.getLogger("com.lightcrafts.image.metadata");
        Handler handler = new TerseLoggingHandler(System.out);
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
    }

    private static void initDocumentDatabase() {
        // This associates images with LZNs, but can be very expensive:
//        DocumentDatabase.init(Startup);

        // Use the DocumentReader to allow the DocumentDatabase to recognize
        // sidecar JPEG, sidecar TIFF, and multilayer TIFF save formats:
        DocumentDatabase.addDocumentInterpreter(
            new DocumentInterpreter() {
                public File getImageFile(File file) {
                    DocumentReader.Interpretation interp =
                        DocumentReader.read(file);
                    return (interp != null) ? interp.imageFile : null;
                }
                public Collection<String> getSuffixes() {
                    Collection<String> tiffs = Arrays.asList(
                        TIFFImageType.INSTANCE.getExtensions()
                    );
                    Collection<String> jpegs = Arrays.asList(
                        JPEGImageType.INSTANCE.getExtensions()
                    );
                    Collection<String> all = new LinkedList<String>();
                    all.addAll(tiffs);
                    all.addAll(jpegs);
                    return all;
                }
            }
        );
    }

    private static void setNewWindowBounds(Frame frame, Frame parent) {
        final int inset = 20;
        // If no parent was supplied, use the "active frame" instead
        if (parent == null) {
            parent = getActiveFrame();
        }
        if ((parent != null) && (parent != frame)) { // First frame is "active"
            // First choice: down and right of the active frame
            Rectangle bounds = parent.getBounds();
            bounds = new Rectangle(
                bounds.x + inset, bounds.y + inset, bounds.width, bounds.height
            );
            // But don't exceed the screen bounds
            GraphicsConfiguration gc = parent.getGraphicsConfiguration();
            Rectangle screen = gc.getBounds();
            if (bounds.getMaxX() > (screen.getMaxX() - inset)) {
                bounds.width = (int) screen.getMaxX() - bounds.x - inset;
            }
            if (bounds.getMaxY() > (screen.getMaxY() - inset)) {
                bounds.height = (int) screen.getMaxY() - bounds.y - inset;
            }
            frame.setBounds(bounds);
        }
        else if ((InitialFrameBounds != null) &&
            (Displays.getVirtualBounds().intersects(InitialFrameBounds))
        ) {
            // Second choice: same as the last bounds saved in preferences
            frame.setBounds(
                InitialFrameBounds.x,
                InitialFrameBounds.y,
                InitialFrameBounds.width,
                InitialFrameBounds.height
            );
            frame.setExtendedState(InitialFrameState);
            // InitialFrameBounds is initialized from preferences, used once,
            // and then discarded
            InitialFrameBounds = null;
        }
        else {
            // Third choice: inset from the screen bounds
            GraphicsConfiguration gc = frame.getGraphicsConfiguration();
            Rectangle bounds = gc.getBounds();
            int x = inset;
            int y = inset;
            int width = bounds.width - 2 * inset;
            int height = bounds.height - 2 * inset;
            frame.setBounds(x, y, width, height);
            // InitialFrameBounds is initialized from preferences, used once,
            // and then discarded
            InitialFrameBounds = null;
        }
        frame.validate();
    }

    private static void setLookAndFeel() {
        LookAndFeel plafName = Env.getLookAndFeel();
        setLookAndFeel(plafName);
    }

    private final static String FirstLaunchTag = "FirstLaunch";

    private static void showFirstTimeHelp() {
        Preferences prefs = getPreferences();
        if (prefs.getBoolean(FirstLaunchTag, true)) {
            Env.showHelpTopic(HelpConstants.HELP_DISCOVER);
            prefs.putBoolean(FirstLaunchTag, false);
        }
        if (VideoLearningCenterDialog.shouldShowDialog()) {
            VideoLearningCenterDialog.showDialog();
        }
    }

    private final static String RecentFileTag = "RecentFile";
    private final static String RecentFolderTag = "RecentFolder";
    private final static String CurrentTag = "Current";
    private final static String OpenTag = "Open";
    private final static String SaveTag = "Save";
    private final static String PrintTag = "Print";
    private final static String ExportTag = "Export";
    private final static String FrameBoundsTag = "Bounds";
    private final static String FrameStateTag = "State";

    private static void savePrefs() {
        Preferences prefs = getPreferences();

        ComboFrame active = getActiveFrame();
        if (active != null) {
            Rectangle bounds = active.getUnmaximizedBounds();
            if (bounds != null) {
                prefs.putInt(FrameBoundsTag + "X", bounds.x);
                prefs.putInt(FrameBoundsTag + "Y", bounds.y);
                prefs.putInt(FrameBoundsTag + "W", bounds.width);
                prefs.putInt(FrameBoundsTag + "H", bounds.height);
            }
            int state = active.getExtendedState();
            prefs.putInt(FrameStateTag, state);
        }
        int n;
        n = 0;
        String key;
        for (File file : RecentFiles) {
            key = RecentFileTag + n;
            String value = file.getAbsolutePath();
            prefs.put(key, value);
            n++;
        }
        // Clear out old recent-docs entries:
        key = RecentFileTag + n++;
        while (prefs.get(key, null) != null) {
            prefs.remove(key);
            key = RecentFileTag + n++;
        }
        n = 0;
        for (File file : RecentFolders) {
            key = RecentFolderTag + n;
            String value = file.getAbsolutePath();
            prefs.put(key, value);
            n++;
        }
        // Clear out old recent-directories entries:
        key = RecentFolderTag + n++;
        while (prefs.get(key, null) != null) {
            prefs.remove(key);
            key = RecentFolderTag + n++;
        }
        // needed: application events
        // (document open, document close, document change)
        // The "windows" menu is relying on static references for updates,
        // and while the set of current documents should be persisted in prefs,
        // a prefs change does not equal a document change, e.g. a new document
        // that has never been saved (image import).
        n = 0;
        for (ComboFrame frame : Current) {
            Document doc = frame.getDocument();
            if (doc == null) {
                continue;
            }
            File file = doc.getFile();
            key = CurrentTag + n;
            if (file != null) {
                String value = file.getAbsolutePath();
                prefs.put(key, value);
                n++;
            }
        }
        // Clear out old current-docs entries:
        key = CurrentTag + n++;
        while (prefs.get(key, null) != null) {
            prefs.remove(key);
            key = CurrentTag + n++;
        }
        if (LastOpenPath != null) {
            String path = LastOpenPath.getAbsolutePath();
            prefs.put(OpenTag, path);
        }
        if (LastSaveOptions != null) {
            // We're remembering the whole SaveOptions object, but
            // SaveOptions.getDefaultSaveOptions() is the authoritative
            // reposity for sticky options.  The LastSaveOptions is only
            // used to remember the recent folder.
            XmlDocument doc = new XmlDocument(SaveTag);
            LastSaveOptions.save(doc.getRoot());
            saveXmlPrefs(SaveTag, doc);
        }
        if (LastPrintLayout != null) {
            XmlDocument doc = new XmlDocument(PrintTag);
            LastPrintLayout.save(doc.getRoot());
            saveXmlPrefs(PrintTag, doc);
        }
        if (LastExportOptions != null) {
            XmlDocument doc = new XmlDocument(ExportTag);
            LastExportOptions.write(doc.getRoot());
            saveXmlPrefs(ExportTag, doc);
        }
        try {
            prefs.sync();
        }
        catch (BackingStoreException e) {
            showError(LOCALE.get("PrefsWriteError"), e, null);
        }
    }

    private static boolean saveXmlPrefs(String tag, XmlDocument doc) {
        Preferences prefs = getPreferences();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            String text = out.toString("UTF-8");
            prefs.put(tag, text);
            return true;
        }
        catch (IOException e) {
            System.err.print("Error saving preferences: ");
            System.err.print(e.getClass().getName() + " ");
            System.err.println(e.getMessage());
            return false;
        }
    }

    private static void restorePrefs() {
        Preferences prefs = getPreferences();
        try {
            // Restore default frame bounds:
            int x = prefs.getInt(FrameBoundsTag + "X", - Integer.MAX_VALUE);
            int y = prefs.getInt(FrameBoundsTag + "Y", - Integer.MAX_VALUE);
            int w = prefs.getInt(FrameBoundsTag + "W", -1);
            int h = prefs.getInt(FrameBoundsTag + "H", -1);
            if ((x > - Integer.MAX_VALUE) &&
                (y > - Integer.MAX_VALUE) &&
                (w > 0) &&
                (h > 0))
            {
                InitialFrameBounds = new Rectangle(x, y, w, h);
            }
            int state = prefs.getInt(FrameStateTag, Frame.NORMAL);
            if (state != Frame.ICONIFIED) {
                InitialFrameState = state;
            }
            else {
                InitialFrameState = Frame.NORMAL;
            }
            // Restore the RecentFile list:
            String[] keys = prefs.keys();
            Map<Integer, File> recentMap = new HashMap<Integer, File>();
            for (String key : keys) {
                if (key.startsWith(RecentFileTag)) {
                    String indexString = key.substring(RecentFileTag.length());
                    try {
                        Integer index = Integer.decode(indexString);
                        String value = prefs.get(key, null);
                        File file = new File(value);
                        recentMap.put(index, file);
                    }
                    catch (NumberFormatException e) {
                        // Bad recent-file pref, just drop it.
                    }
                }
            }
            for (int n=recentMap.size()-1; n>=0; n--) {
                File file = recentMap.get(n);
                if (file != null) {
                    addToRecentFiles(file);
                }
            }
            // Restore the RecentFolder list:
            keys = prefs.keys();
            recentMap = new HashMap<Integer, File>();
            for (String key : keys) {
                if (key.startsWith(RecentFolderTag)) {
                    String indexString =
                        key.substring(RecentFolderTag.length());
                    try {
                        Integer index = Integer.decode(indexString);
                        String value = prefs.get(key, null);
                        File file = new File(value);
                        recentMap.put(index, file);
                    }
                    catch (NumberFormatException e) {
                        // Bad recent-file pref, just drop it.
                    }
                }
            }
            for (int n=recentMap.size()-1; n>=0; n--) {
                File file = recentMap.get(n);
                if (file != null) {
                    addToRecentFolders(file);
                }
            }
            // Restore the last open path:
            String path;
            path = prefs.get(OpenTag, null);
            if (path != null) {
                LastOpenPath = new File(path);
            }
            // Restore default save options:
            XmlDocument doc;
            doc = restoreXmlPrefs(SaveTag);
            if (doc != null) {
                try {
                    LastSaveOptions = SaveOptions.restore(doc.getRoot());
                }
                catch (XMLException e) {
                    System.err.println(
                        "Malformed save preferences: " + e.getMessage()
                    );
                    LastSaveOptions = null;
                }
            }
            // Restore default print settings:
            doc = restoreXmlPrefs(PrintTag);
            if (doc != null) {
                try {
                    LastPrintLayout = new PrintLayoutModel(0, 0);
                    LastPrintLayout.restore(doc.getRoot());
                }
                catch (XMLException e) {
                    System.err.println(
                        "Malformed print preferences: " + e.getMessage()
                    );
                    LastPrintLayout = null;
                }
            }
            // Restore default export options:
            doc = restoreXmlPrefs(ExportTag);
            if (doc != null) {
                try {
                    LastExportOptions = ImageExportOptions.read(doc.getRoot());
                }
                catch (XMLException e) {
                    System.err.println(
                        "Malformed export preferences: " + e.getMessage()
                    );
                    LastExportOptions = null;
                }
            }
        }
        catch (BackingStoreException e) {
            showError(LOCALE.get("PrefsReadError"), e, null);
        }
    }

    private static XmlDocument restoreXmlPrefs(String tag) {
        Preferences prefs = getPreferences();
        String text = prefs.get(tag, null);
        if (text != null) {
            try {
                InputStream in = new ByteArrayInputStream(
                    text.getBytes("UTF-8")
                );
                XmlDocument doc = new XmlDocument(in);
                return doc;
            }
            catch (Exception e) {   // IOException, XMLException
                System.err.print("Error reading preferences: ");
                System.err.print(e.getClass().getName() + " ");
                System.err.println(e.getMessage());
                prefs.remove(tag);
            }
        }
        return null;
    }

    // Set up verbose focus event logging, for development purposes.
    private static void initFocusDebug() {
        KeyboardFocusManager focus =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focus.addPropertyChangeListener(
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    String propName = evt.getPropertyName();
                    Object oldValue = evt.getOldValue();
                    String oldName = (oldValue != null) ?
                        oldValue.getClass().getName() : "null";
                    Object newValue = evt.getNewValue();
                    String newName = (newValue != null) ?
                        newValue.getClass().getName() : "null";
                    System.out.println(
                        propName + ": " + oldName + " -> " + newName
                    );
                }
            }
        );
    }

    public static void main(final String[] args) {
        // Catch startup crashes that prevent launching:
        StartupCrash.checkLastStartupSuccessful();
        StartupCrash.startupStarted();

        ExceptionDialog.installHandler();

        // This debug features streams all focus events to standard output.
        if (System.getProperty("lightcrafts.debug.focus") != null) {
            initFocusDebug();
        }
        // Initialize on the main thread, to allow the splash to display,
        // and to sleep for open events from the native event handlers:
        try {
            Startup.startupMessage(LOCALE.get("StartupLibsMessage"));
            verifyLibraries();
            Startup.startupMessage(LOCALE.get("StartupColorsMessage"));
            scanProfiles();
            Startup.startupMessage(LOCALE.get("StartupPrefsMessage"));
            restorePrefs();
            Startup.startupMessage(LOCALE.get("StartupLogsMessage"));
            initLogging();
            Startup.startupMessage(LOCALE.get("StartupScanMessage"));
            initDocumentDatabase();
            Startup.startupMessage(LOCALE.get("StartupOpeningMessage"));

            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        if (Platform.getType() == Platform.MacOSX) {
                            // Get a Mac menu bar before setting LaF, then restore.
                            Object menuBarUI = UIManager.get("MenuBarUI");
                            setLookAndFeel(new LightZoneSkin().getLightZoneLookAndFeel());
                            UIManager.put("MenuBarUI", menuBarUI);

                            openMacPlaceholderFrame();
                        }
                        else {
                            setLookAndFeel();
                        }
                        openEmpty();
                        Platform.getPlatform().readyToOpenFiles();

                        // Make sure this happens good and late, after a
                        // frame is visible.
                        EventQueue.invokeLater(
                            new Runnable() {
                                public void run() {
                                    showFirstTimeHelp();
                                }
                            }
                        );
                        // Wait twenty seconds after all initialization has
                        // completed before declaring a successful startup,
                        // since crashes that would be fixed by cleared
                        // settings sometimes happen much later, during
                        // queued browser thumbnail tasks.
                        new Thread(
                            new Runnable() {
                                public void run() {
                                    try {
                                        Thread.sleep(20000);
                                    }
                                    catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    StartupCrash.startupEnded();
                                }
                            },
                            "StartupSuccessWait"
                        ).start();
                    }
                }
            );
            AwtWatchdog.spawn();
        }
        catch (Throwable e) {
            (new ExceptionDialog()).handle(e);
        }
    }
}
