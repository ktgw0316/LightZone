/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ColorProfileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageOrientation;
import javax.media.jai.util.ImagingException;
import com.lightcrafts.model.Engine;
import com.lightcrafts.model.EngineFactory;
import com.lightcrafts.model.Scale;
import com.lightcrafts.ui.export.SaveOptions;
import com.lightcrafts.ui.print.PrintLayoutModel;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;

import javax.imageio.ImageIO;
import javax.swing.Action;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

/**
 * A Document is the public face of an image editor.  It ties together an
 * image, an engine which processes it, and an editor component for user
 * interaction.  It provides access to various pieces of package-private
 * functionality to interact with application context, such as save/restore,
 * undo/redo, frames, menus, printing and export.
 */

public class Document {

    /**
     * Thrown by the constructors when the Image tag in a saved Document
     * doesn't point to a valid source image.
     */
    public class MissingImageFileException extends Exception {
        @Getter
        private File imageFile;

        private MissingImageFileException(File imageFile) {
            super(LOCALE.get("MissingImageError", imageFile.getPath()));
            this.imageFile = imageFile;
        }
    }

    /**
     * A version number for the saved-Document file format:
     *   1: Original version, all the 1.0 releases, until December 29, 2005.
     *   2: For the clone tool, in 1.1 betas, until January 20, 2006.
     *   3: For more precise slider values, until February 8, 2006.
     *   4: Add the region invert switch on tools, until February 18, 2006.
     *   5: The new dropper-style white point tool, until July 21, 2006.
     *   6: Add SaveOptions and ExportOptions, new tools, the black point
     *      compensation export option, and PPI for printing, until April 23,
     *      2007.
     *   7: Add the "RAW Adjustments" controls, implying versioning of
     *      the Engine's RAW conversion, until July 6, 2007.
     *   8: Add selective color controls.
     */
    private final static int SavedDocVersion = 8;

    // metadata for this Document's original image file.
    @Getter
    private ImageMetadata metadata;

    // Notify this Document that it has been saved to a File.  Effects the
    // results returned by getSaveOptions() and getName().
    @Setter
    // If this Document has been saved, then this returns the most recent way
    // it was saved.  Otherwise it returns null.
    @Getter
    private SaveOptions saveOptions;

    @Getter
    private Editor editor;

    // the backing Engine for this Document. Useful for printing and for
    // propagating debug actions from the Engine to the menus.
    @Getter
    private Engine engine;

    private CropRotateManager crop;

    // the gateway to everything about tool masks in the editor.
    // The RegionManager is the place to access the curve type
    // (polygon, basis spline, bezier curve); the curve selection model;
    // and the region copy/paste feature.
    @Getter
    private RegionManager regionManager;

    // Through the scale model, you can change the editor's zoom factor,
    // and also find out when the zoom changes.
    @Getter
    private ScaleModel scaleModel;

    private XFormModel xform;
    private PrintLayoutModel print;

    @Getter (AccessLevel.PACKAGE)
    private DocUndoManager undoManager;

    // True means the user has done some work since this Document was initialized.
    @Getter
    private boolean dirty;

    private Collection<DocumentListener> listeners;

    // Recent values used for image export (to initialize the file chooser):
    @Getter
    private ImageExportOptions exportOptions;

    // Documents may be opened in a way that effects how they should be saved:
    @Getter @Setter
    private Object source;

    /**
     * Calls Document(doc, null).
     */
    public Document(XmlDocument doc)
        throws ColorProfileException,
               UserCanceledException,
               IOException,
               XMLException,
               MissingImageFileException,
               BadImageFileException,
               UnknownImageTypeException
    {
        this(doc, null);
    }

    /**
     * Calls Document(doc, meta, null, null).
     */
    public Document(XmlDocument doc, ImageMetadata meta)
        throws UserCanceledException,
               ColorProfileException,
               IOException,
               XMLException,
               MissingImageFileException,
               BadImageFileException,
               UnknownImageTypeException
    {
        this(doc, meta, null, null);
    }

    /** Initialize from an XmlDocument structure of the form created by
     * <code>save()</code>.
     * <p>
     * Throws XMLException if there's something wrong with the XmlDocument
     * format, and throws IOException if there's trouble reading the image
     * file referenced inside.
     * <p>
     * The ImageMetadata argument can override the image file reference
     * inside the XmlDocument, or it can be null.
     */
    public Document(
        XmlDocument doc,
        ImageMetadata meta,
        ImageInfo versionInfo,
        ProgressThread thread
    )
        throws XMLException,
               IOException,
               BadImageFileException,
               ColorProfileException,
               UnknownImageTypeException,
               UserCanceledException,
               MissingImageFileException,
               ImagingException
    {
        val root = doc.getRoot();
        val version = root.getVersion();
        if (version > SavedDocVersion) {
            throw new XMLException(LOCALE.get("FutureLznError"));
        }
        if (version < 0) {
            throw new XMLException(LOCALE.get("MissingLznVersionError"));
        }
        if (meta == null) {
            // Find the original image:
            val node = root.getChild(ImageTag);
            String path = node.getAttribute(ImagePathTag);
            File file = new File(path);
            if (! file.isFile()) {
                throw new MissingImageFileException(file);
            }
            // Override with a relative path, if one was defined:
            if (node.hasAttribute(ImageRelativePathTag)) {
                path = node.getAttribute(ImageRelativePathTag);
                val relativeFile = new File(path);
                if (relativeFile.isFile()) {
                    file = relativeFile;
                }
            }
            val info = ImageInfo.getInstanceFor(file);
            try {
                meta = info.getMetadata();
            }
            catch (FileNotFoundException ignored) {
                throw new MissingImageFileException(file);
            }
        }
        this.metadata = meta;

        // Enforce the saved original image orientation, which defines the
        // coordinate system used for regions and crop bounds:
        XmlNode imageNode = root.getChild(ImageTag);
        if (imageNode.hasAttribute(ImageOrientationTag)) {
            val value = imageNode.getAttribute(ImageOrientationTag);
            try {
                val oldOrientation =
                    ImageOrientation.getOrientationFor(Short.parseShort(value));
                val newOrientation = meta.getOrientation();
                if (oldOrientation != newOrientation) {
                    meta.setOrientation(oldOrientation);
                }
            }
            catch (NumberFormatException e) {
                throw new XMLException(
                    "Image orientation \"" + value + "\" is not a number", e
                );
            }
        }
        // Backwards compatibility: before XMP support, the convention in LZN
        // files was that the image orientation was its original orientation,
        // before any browser rotations.
        else {
            // Make sure this pre-XMP LZN structure is not a Template.
            // (See Application.saveTemplate().)
            if (! root.getName().equals("Template")) {
                val origOrient = meta.getOriginalOrientation();
                meta.setOrientation(origOrient);
            }
        }
        engine = EngineFactory.createEngine(meta, versionInfo, thread);

        xform = new XFormModel(engine);

        regionManager = new RegionManager();
        crop = new CropRotateManager(engine, xform);

        scaleModel = new ScaleModel(engine);
        val scaleNode = root.getChild(ScaleTag);
        val s = new Scale(scaleNode);
        scaleModel.setScale(s);

        editor = new Editor(engine, scaleModel, xform, regionManager, crop, this);
        editor.showWait(LOCALE.get("EditorWaitText"));
        crop.setEditor( editor );

        val controlNode = root.getChild(ControlTag);

        // this does the inverse of save(XmlNode):
        try {
            editor.restore(controlNode);
        } catch (XMLException e) {
            dispose();
            throw e;
        }

        commonInitialization();

        if (root.hasChild(SaveTag)) {
            val saveNode = root.getChild(SaveTag);
            saveOptions = SaveOptions.restore(saveNode);
        }
        if (root.hasChild(PrintTag)) {
            val printNode = root.getChild(PrintTag);
            Dimension size = engine.getNaturalSize();
            print = new PrintLayoutModel(size.width, size.height);
            print.restore(printNode);
        }
        if (root.hasChild(ExportTag)) {
            val exportNode = root.getChild(ExportTag);
            exportOptions = ImageExportOptions.read(exportNode);
        }
    }

    /**
     * Calls Document(meta, null).
     */
    public Document(ImageMetadata meta)
        throws UserCanceledException,
               ColorProfileException,
               IOException,
               BadImageFileException,
               UnknownImageTypeException
    {
        this(meta, null);
    }

    /**
     * Initialize from an image with everything else set to defaults.
     * (On the resulting instance, <code>getFile()</code> will return null.)
     */
    public Document(ImageMetadata meta, ProgressThread thread)
        throws BadImageFileException,
               ColorProfileException,
               ImagingException,
               IOException,
               UnknownImageTypeException,
               UserCanceledException
    {
        this.metadata = meta;

        engine = EngineFactory.createEngine(meta, null, thread);

        xform = new XFormModel(engine);
        regionManager = new RegionManager();
        scaleModel = new ScaleModel(engine);

        crop = new CropRotateManager(engine, xform);
        editor = new Editor(engine, scaleModel, xform, regionManager, crop, this);
        crop.setEditor( editor );
        editor.showWait(LOCALE.get("EditorWaitText"));
        commonInitialization();
    }

    private void commonInitialization() {
        undoManager = new DocUndoManager(this);
        editor.addUndoableEditListener(undoManager);
        print = null;
        scaleModel.addScaleListener(
            new ScaleListener() {
                @Override
                public void scaleChanged(Scale scale) {
                    xform.update();
                }
            }
        );
        xform.addXFormListener(
            new XFormListener() {
                @Override
                public void xFormChanged(AffineTransform xform) {
                    regionManager.setXForm(xform);
                }
            }
        );
        listeners = new LinkedList<DocumentListener>();
    }

    /**
     * If this Document has been saved, return the saved File.  This is just
     * a convenience method for getSaveOptions().getFile().
     */
    public File getFile() {
        return (saveOptions != null) ? saveOptions.getFile() : null;
    }

    /**
     * Get a placeholder editor that is entirely disabled, for display in
     * cases where there is no Document.
     */
    public static DisabledEditor createDisabledEditor(
        DisabledEditor.Listener listener
    ) {
        return new DisabledEditor(listener);
    }

    /**
     * Get the Action that shows and hides the proof control in the editor.
     */
    public Action getProofAction() {
        return editor.getProofAction();
    }

    /**
     * Get the Action that rotates the image ninety degrees to the right.
     */
    public Action getRotateRightAction() {
        return crop.getRightAction();
    }

    /**
     * Get the Action that rotates the image ninety degrees to the left.
     */
    public Action getRotateLeftAction() {
        return crop.getLeftAction();
    }

    /**
     * Get the Action that performs undo.
     */
    public Action getUndoAction() {
        return undoManager.getUndoAction();
    }

    /**
     * Get the Action that performs redo.
     */
    public Action getRedoAction() {
        return undoManager.getRedoAction();
    }

    /**
     * Get a List of Actions that cause all the available tool controls to
     * be pushed onto the tool stack in the editor.
     */
    public List<Action> getOperations() {
        return editor.getOperations();
    }

    /**
     * The zoom-to-fit operation depends simultaneously on the ScaleModel,
     * the editor component, and the Engine; therefore this operation is
     * handled inside Document.
     */
    public void zoomToFit() {
        editor.setScaleToFit();
    }

    /**
     * Reset the dirty flag, which indicates whether this Document has been
     * edited by the user.  See isDirty().
     */
    public void markClean() {
        if (dirty) {
            dirty = false;
            notifyListeners();
        }
    }

    /**
     * Set the dirty flag, which indicates whether this Document has been
     * edited by the user.  See isDirty().
     */
    public void markDirty() {
        if (! dirty) {
            dirty = true;
            notifyListeners();
        }
    }

    /**
     * If the editor is in zoom-to-fit mode, turn the mode off temporarily.
     */
    public void pushFitMode() {
        editor.pushFitMode();
    }

    /**
     * If the editor was in zoom-to-fit mode before a call to pushFitMode(),
     * then turn the mode back on.
     */
    public boolean popFitMode() {
        return editor.popFitMode();
    }

    /**
     * Forget all undoable edits in the current undo stack.
     */
    public void discardEdits() {
        undoManager.discardAllEdits();
    }

    /**
     * Detect whether the current tool stack includes a RAW Adjustments tool.
     * When a RAW image or an older LZN file is opened (before LZN version 7),
     * this tool will be absent and must be added manually.
     */
    public boolean hasRawAdjustments() {
        return editor.hasRawAdjustments();
    }

    public void addDocumentListener(DocumentListener listener) {
        listeners.add(listener);
    }

    public void removeDocumentListener(DocumentListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (val listener : listeners) {
            listener.documentChanged(this, dirty);
        }
    }

    /**
     * Get the most recent print settings applied to this Document, or null
     * if these parameters have never been set.
     */
    public PrintLayoutModel getPrintLayout() {
        if (print != null) {
            // Image bounds may have changed due to cropping:
            val size = engine.getNaturalSize();
            print.updateImageSize(size.width, size.height);
        }
        return print;
    }

    /**
     * Set print settings for this Document, for getPrintLayout().
     */
    public void setPrintLayout(PrintLayoutModel print) {
        this.print = print;
        markDirty();
    }

    /**
     * Set export options on this Document, for getExportOptions().
     */
    public void setExportOptions(ImageExportOptions options) {
        exportOptions = options;
        markDirty();
    }

    public void dispose() {
        editor.dispose();
        engine.dispose();
    }

    public TemporaryEditorCommitState saveStart() {
        return editor.saveStart();
    }

    public void saveEnd(TemporaryEditorCommitState state) {
        editor.saveEnd(state);
    }

    // String constants used for serialization:
    private final static String ScaleTag = "Scale";
    private final static String ImageTag = "Image";
    private final static String ImagePathTag = "path";
    private final static String ImageOrientationTag = "orientation";
    private final static String ImageRelativePathTag = "relativePath";
    private final static String ControlTag = "Controls";
    private final static String SaveTag = "Save";
    private final static String PrintTag = "Print";
    private final static String ExportTag = "Export";

    public void save(XmlNode node) {
        node.setVersion(SavedDocVersion);

        val scaleNode = node.addChild(ScaleTag);
        scaleModel.getCurrentScale().save(scaleNode);

        val imageNode = node.addChild(ImageTag);
        imageNode.setAttribute(
            ImagePathTag, metadata.getFile().getAbsolutePath()
        );
        imageNode.setAttribute(
            ImageOrientationTag,
            Integer.toString(metadata.getOrientation().getTIFFConstant())
        );
        if (saveOptions != null) {
            String path;
            try {
                path = RelativePathUtility.getRelativePath(
                    saveOptions.getFile(), metadata.getFile()
                );
            }
            catch (IOException e) {
                path = metadata.getFile().getName();
            }
            imageNode.setAttribute(ImageRelativePathTag, path);
        }
        val controlNode = node.addChild(ControlTag);
        editor.save(controlNode);

        if (saveOptions != null) {
            val saveNode = node.addChild(SaveTag);
            saveOptions.save(saveNode);
        }
        if (print != null) {
            val printNode = node.addChild(PrintTag);
            print.save(printNode);
        }
        if (exportOptions != null) {
            val exportNode = node.addChild(ExportTag);
            exportOptions.write(exportNode);
        }
    }

    // Just like save(), but without SaveTag, PrintTag, or ExportTag, an
    // empty image pointer, and no image relative path.  The result may
    // be used either in a Document constructor or in applyTemplate().
    public void saveTemplate(XmlNode node) {
        node.setVersion(SavedDocVersion);

        val scaleNode = node.addChild(ScaleTag);
        scaleModel.getCurrentScale().save(scaleNode);

        val imageNode = node.addChild(ImageTag);
        imageNode.setAttribute(ImagePathTag, "");
        val controlNode = node.addChild(ControlTag);
        editor.save(controlNode);
    }

    public void applyTemplate(XmlNode node)
        throws XMLException
    {
        val version = node.getVersion();
        if (version > SavedDocVersion) {
            throw new XMLException(LOCALE.get("FutureTemplateError"));
        }
        if (version < 0) {
            throw new XMLException(LOCALE.get("MissingTemplateVersionError"));
        }
        // Ignore the scale factor under ScaleTag.

        val controlNode = node.getChild(ControlTag);
        editor.addControls(controlNode);
    }

    // Since Anton keeps forgetting to dispose documents, I add a finalizer

    public void finalize() throws Throwable {
        super.finalize();
        dispose();
    }

    public static void main(String[] args) throws Exception {
        val in = new FileInputStream(args[0]);
        val xml = new XmlDocument(in);
        val doc = new Document(xml, null);
        val image = doc.engine.getRendering(new Dimension(100, 100));
        ImageIO.write(image, "jpeg", new File("out.jpg"));
    }
}
