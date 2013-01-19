/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

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
import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.export.SaveOptions;
import com.lightcrafts.ui.print.PrintLayoutModel;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
        private File imageFile;
        private MissingImageFileException(File imageFile) {
            super(LOCALE.get("MissingImageError", imageFile.getPath()));
            this.imageFile = imageFile;
        }
        public File getImageFile() {
            return imageFile;
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
    public final static int SavedDocVersion = 8;

    private ImageMetadata meta;
    private SaveOptions save;
    private Editor editor;
    private Engine engine;
    private CropRotateManager crop;
    private RegionManager regions;
    private ScaleModel scale;
    private XFormModel xform;
    private PrintLayoutModel print;
    private DocUndoManager undo;

    private boolean dirty;
    private Collection<DocumentListener> listeners;

    // Recent values used for image export (to initialize the file chooser):
    private ImageExportOptions export;

    // Documents may be opened in a way that effects how they should be saved:
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
        XmlNode root = doc.getRoot();
        int version = root.getVersion();
        if (version > SavedDocVersion) {
            throw new XMLException(LOCALE.get("FutureLznError"));
        }
        if (version < 0) {
            throw new XMLException(LOCALE.get("MissingLznVersionError"));
        }
        if (meta == null) {

            // Find the original image:
            XmlNode node = root.getChild(ImageTag);
            String path = node.getAttribute(ImagePathTag);
            File file = new File(path);
            if (! file.isFile()) {
                throw new MissingImageFileException(file);
            }
            // Override with a relative path, if one was defined:
            if (node.hasAttribute(ImageRelativePathTag)) {
                path = node.getAttribute(ImageRelativePathTag);
                File relativeFile = new File(path);
                if (relativeFile.isFile()) {
                    file = relativeFile;
                }
            }
            ImageInfo info = ImageInfo.getInstanceFor(file);
            try {
                meta = info.getMetadata();
            }
            catch (FileNotFoundException e) {
                throw new MissingImageFileException(file);
            }
        }
        this.meta = meta;

        // Enforce the saved original image orientation, which defines the
        // coordinate system used for regions and crop bounds:
        XmlNode imageNode = root.getChild(ImageTag);
        if (imageNode.hasAttribute(ImageOrientationTag)) {
            String value = imageNode.getAttribute(ImageOrientationTag);
            try {
                ImageOrientation oldOrientation =
                    ImageOrientation.getOrientationFor(Short.parseShort(value));
                ImageOrientation newOrientation = meta.getOrientation();
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
                ImageOrientation origOrient = meta.getOriginalOrientation();
                meta.setOrientation(origOrient);
            }
        }
        engine = EngineFactory.createEngine(meta, versionInfo, thread);

        xform = new XFormModel(engine);

        regions = new RegionManager();
        crop = new CropRotateManager(engine, xform);

        scale = new ScaleModel(engine);
        XmlNode scaleNode = root.getChild(ScaleTag);
        Scale s = new Scale(scaleNode);
        scale.setScale(s);

        editor = new Editor(engine, scale, xform, regions, crop, this);
        editor.showWait(LOCALE.get("EditorWaitText"));
        crop.setEditor( editor );

        XmlNode controlNode = root.getChild(ControlTag);

        // this does the inverse of save(XmlNode):
        try {
            editor.restore(controlNode);
        } catch (XMLException e) {
            dispose();
            throw e;
        }

        commonInitialization();

        if (root.hasChild(SaveTag)) {
            XmlNode saveNode = root.getChild(SaveTag);
            save = SaveOptions.restore(saveNode);
        }
        if (root.hasChild(PrintTag)) {
            XmlNode printNode = root.getChild(PrintTag);
            Dimension size = engine.getNaturalSize();
            print = new PrintLayoutModel(size.width, size.height);
            print.restore(printNode);
        }
        if (root.hasChild(ExportTag)) {
            XmlNode exportNode = root.getChild(ExportTag);
            export = ImageExportOptions.read(exportNode);
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
    public Document(
        ImageMetadata meta, ProgressThread thread
    )
        throws BadImageFileException,
               ColorProfileException,
               ImagingException,
               IOException,
               UnknownImageTypeException,
               UserCanceledException
    {
        this.meta = meta;

        engine = EngineFactory.createEngine(meta, null, thread);

        xform = new XFormModel(engine);
        regions = new RegionManager();
        scale = new ScaleModel(engine);

        crop = new CropRotateManager(engine, xform);
        editor = new Editor(engine, scale, xform, regions, crop, this);
        crop.setEditor( editor );
        editor.showWait(LOCALE.get("EditorWaitText"));
        commonInitialization();
    }

    private void commonInitialization() {
        undo = new DocUndoManager(this);
        editor.addUndoableEditListener(undo);
        print = null;
        scale.addScaleListener(
            new ScaleListener() {
                public void scaleChanged(Scale scale) {
                    xform.update();
                }
            }
        );
        xform.addXFormListener(
            new XFormListener() {
                public void xFormChanged(AffineTransform xform) {
                    regions.setXForm(xform);
                }
            }
        );
        listeners = new LinkedList<DocumentListener>();
    }

    /**
     * Notify this Document that it has been saved to a File.  Effects the
     * results returned by getSaveOptions() and getName().
     */
    public void setSaveOptions(SaveOptions save) {
        this.save = save;
    }

    /**
     * If this Document has been saved, then this returns the most recent way
     * it was saved.  Otherwise it returns null.
     */
    public SaveOptions getSaveOptions() {
        return save;
    }

    /**
     * If this Document has been saved, return the saved File.  This is just
     * a convenience method for getSaveOptions().getFile().
     */
    public File getFile() {
        return (save != null) ? save.getFile() : null;
    }

    /**
     * Get metadata for this Document's original image file.
     */
    public ImageMetadata getMetadata() {
        return meta;
    }

    /**
     * Get the editor component for this Document.
     */
    public Editor getEditor() {
        return editor;
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
     * Get the backing Engine for this Document.  Useful for printing and for
     * propagating debug actions from the Engine to the menus.
     */
    public Engine getEngine() {
        return engine;
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
        return undo.getUndoAction();
    }

    /**
     * Get the Action that performs redo.
     */
    public Action getRedoAction() {
        return undo.getRedoAction();
    }

    /**
     * Get the DocUndoManager that handles all of undo and redo.
     */
    DocUndoManager getUndoManager() {
        return undo;
    }

    /**
     * Get a List of Actions that cause all the available tool controls to
     * be pushed onto the tool stack in the editor.
     */
    public List<Action> getOperations() {
        return editor.getOperations();
    }

    /**
     * Get the model for scale changes in the editor.  Through the scale model,
     * you can change the editor's zoom factor, and also find out when the zoom
     * changes.
     */
    public ScaleModel getScaleModel() {
        return scale;
    }

    /**
     * Get the RegionManager for this Document, the gateway to everything
     * about tool masks in the editor.  The RegionManager is the place to
     * access the curve type (polygon, basis spline, bezier curve); the curve
     * selection model; and the region copy/paste feature.
     */
    public RegionManager getRegionManager() {
        return regions;
    }

    /**
     * The zoom-to-fit operation depends simultaneously on the ScaleModel,
     * the editor component, and the Engine; therefore this operation is
     * handled inside Document.
     */
    public void zoomToFit() {
        Rectangle rect = editor.getMaxImageBounds();
        // Sometimes during frame initialization, the max image bounds
        // is reported as zero.  Perhaps some layout glitch involving
        // scroll pane interaction?
        if ((rect.width > 0) && (rect.height > 0)) {
            Scale oldScale = scale.getCurrentScale();
            Scale newScale = engine.setScale(rect);
            if (! scale.setScale(newScale)) {
                engine.setScale(oldScale);
            }
        }
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
     * Query the dirty flag on this Document.  True means the user has done
     * some work since this Document was initialized.
     */
    public boolean isDirty() {
        return dirty;
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
        undo.discardAllEdits();
    }

    /**
     * Detect whether the current tool stack includes a RAW Adjustments tool.
     * When a RAW image or an older LZN file is opened (before LZN version 7),
     * this tool will be absent and must be added manually.
     */
    public boolean hasRawAdjustments() {
        return editor.hasRawAdjustments();
    }

    /**
     * Set a cookie, probably representing the way this Document was opened.
     */
    public void setSource(Object source) {
        this.source = source;
    }

    /**
     * Get the cookie from setSource(), probably representing the way this
     * Document was opened.
     */
    public Object getSource() {
        return source;
    }

    public void addDocumentListener(DocumentListener listener) {
        listeners.add(listener);
    }

    public void removeDocumentListener(DocumentListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (DocumentListener listener : listeners) {
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
            Dimension size = engine.getNaturalSize();
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
     * Get the most recent export options applied to this Document, or null
     * if no export options have ever been set.
     */
    public ImageExportOptions getExportOptions() {
        return export;
    }

    /**
     * Set export options on this Document, for getExportOptions().
     */
    public void setExportOptions(ImageExportOptions options) {
        export = options;
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

        XmlNode scaleNode = node.addChild(ScaleTag);
        scale.getCurrentScale().save(scaleNode);

        XmlNode imageNode = node.addChild(ImageTag);
        imageNode.setAttribute(
            ImagePathTag, meta.getFile().getAbsolutePath()
        );
        imageNode.setAttribute(
            ImageOrientationTag,
            Integer.toString(meta.getOrientation().getTIFFConstant())
        );
        if (save != null) {
            String path;
            try {
                path = RelativePathUtility.getRelativePath(
                    save.getFile(), meta.getFile()
                );
            }
            catch (IOException e) {
                path = meta.getFile().getName();
            }
            imageNode.setAttribute(ImageRelativePathTag, path);
        }
        XmlNode controlNode = node.addChild(ControlTag);
        editor.save(controlNode);

        if (save != null) {
            XmlNode saveNode = node.addChild(SaveTag);
            save.save(saveNode);
        }
        if (print != null) {
            XmlNode printNode = node.addChild(PrintTag);
            print.save(printNode);
        }
        if (export != null) {
            XmlNode exportNode = node.addChild(ExportTag);
            export.write(exportNode);
        }
    }

    // Just like save(), but without SaveTag, PrintTag, or ExportTag, an
    // empty image pointer, and no image relative path.  The result may
    // be used either in a Document constructor or in applyTemplate().
    public void saveTemplate(XmlNode node) {
        node.setVersion(SavedDocVersion);

        XmlNode scaleNode = node.addChild(ScaleTag);
        scale.getCurrentScale().save(scaleNode);

        XmlNode imageNode = node.addChild(ImageTag);
        imageNode.setAttribute(ImagePathTag, "");
        XmlNode controlNode = node.addChild(ControlTag);
        editor.save(controlNode);
    }

    public void applyTemplate(XmlNode node)
        throws XMLException
    {
        int version = node.getVersion();
        if (version > SavedDocVersion) {
            throw new XMLException(LOCALE.get("FutureTemplateError"));
        }
        if (version < 0) {
            throw new XMLException(LOCALE.get("MissingTemplateVersionError"));
        }
        // Ignore the scale factor under ScaleTag.

        XmlNode controlNode = node.getChild(ControlTag);
        editor.addControls(controlNode);
    }

    // Since Anton keeps forgetting to dispose documents, I add a finalizer

    public void finalize() throws Throwable {
        super.finalize();
        dispose();
    }

    public static void main(String[] args) throws Exception {
        InputStream in = new FileInputStream(args[0]);
        XmlDocument xml = new XmlDocument(in);
        Document doc = new Document(xml, null);
        RenderedImage image = doc.engine.getRendering(new Dimension(100, 100));
        ImageIO.write(image, "jpeg", new File("out.jpg"));
    }
}
