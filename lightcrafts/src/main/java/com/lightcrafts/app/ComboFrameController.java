/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.app.menu.WindowMenu;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.model.Scale;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.platform.ProgressDialog;
import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.templates.TemplateKey;
import com.lightcrafts.ui.browser.ctrls.FolderCtrl;
import com.lightcrafts.ui.browser.folders.FolderBrowserPane;
import com.lightcrafts.ui.browser.folders.FolderTreeListener;
import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.browser.model.ImageDatumComparator;
import com.lightcrafts.ui.browser.model.ImageList;
import com.lightcrafts.ui.browser.model.PreviewUpdater;
import com.lightcrafts.ui.browser.view.ImageBrowserEvent;
import com.lightcrafts.ui.browser.view.ImageBrowserListener;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.BrowserFactory;
import com.lightcrafts.ui.browser.view.ExternalBrowserAction;
import com.lightcrafts.ui.browser.view.TemplateProvider;
import com.lightcrafts.ui.action.ToggleAction;
import com.lightcrafts.ui.editor.*;
import com.lightcrafts.ui.editor.assoc.DocumentDatabase;
import com.lightcrafts.ui.editor.assoc.DocumentDatabaseListener;
import com.lightcrafts.ui.export.SaveOptions;
import com.lightcrafts.app.other.OtherApplication;
import com.lightcrafts.ui.templates.TemplateControl;
import com.lightcrafts.ui.toolkit.UICompliance;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.filecache.FileCacheFactory;
import com.lightcrafts.utils.thread.ProgressThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import static com.lightcrafts.app.Locale.LOCALE;

/**
 * Owns the workflow and event coordination associated with a {@link ComboFrame}.
 * The frame remains the Swing host: it constructs the widgets and layouts, and
 * implements this small view port for the operations that need to touch them.
 */
final class ComboFrameController implements ImageBrowserListener,
        DocumentDatabaseListener, DocumentListener, ScaleListener,
        FolderTreeListener, com.lightcrafts.ui.templates.TemplateControlListener {

    private static final Logger logger =
            LoggerFactory.getLogger(ComboFrameController.class);

    interface View {
        ComboFrame frame();

        Document document();

        void installDocumentComponents(Document document, String disabledText);

        void finishDocumentUpdate();

        File recentFolder();

        Editor editor();

        DisabledEditor disabledEditor();

        String disabledEditorText();

        void setImage(ImageInfo imageInfo);

        void setImageList(ImageList images);

        ImageList imageList();

        AbstractImageBrowser browser();

        void disposeBrowser();

        void prepareForFolderChange();

        void installBrowser(AbstractImageBrowser browser);

        void setRecentFolder(File folder);

        void setDisabledEditorText(String text);

        FolderCtrl folderCtrl();

        boolean isBrowserVisible();

        boolean isEditorVisible();

        boolean isEditorLayout();

        void showEditorPerspective();

        boolean showBrowserPerspective();

        void ensureToolsVisible();

        AbstractLayout currentLayout();

        void showEmptyFolderAdvice();

        void hideEmptyFolderAdvice();

        void updateMenus();

    }

    private final View view;
    private boolean focusPaused;
    private File lastSelectedFolder;
    private PropertyChangeListener toolButtonListener;
    private final TemplateProvider templateProvider = new TemplateProvider() {
        @Override
        public List getTemplateActions() {
            try {
                return TemplateDatabase.getTemplateKeys();
            } catch (TemplateDatabase.TemplateException e) {
                logger.warn("Failed to load template keys", e);
                return new java.util.ArrayList();
            }
        }

        @Override
        public void applyTemplateAction(Object action, File[] targets) {
            Application.applyTemplate(
                    view.frame(), targets, (TemplateKey) action);
        }

        @Override
        public void applyTemplate(File file, File[] targets) {
            Application.applyTemplate(view.frame(), targets, file);
        }
    };
    private final ExternalBrowserAction printAction =
            new ExternalBrowserAction() {
                @Override
                public String getName() {
                    return LOCALE.get("BrowserPrintMenuItem");
                }

                @Override
                public void actionPerformed(File file, File[] files) {
                    Application.print(view.frame(), file);
                }
            };
    private final ExternalBrowserAction exportAction =
            new ExternalBrowserAction() {
                @Override
                public String getName() {
                    return LOCALE.get("BrowserExportMenuItem");
                }

                @Override
                public void actionPerformed(File file, File[] files) {
                    if (files.length == 1) {
                        Application.export(view.frame(), file);
                    } else {
                        Application.export(view.frame(), files);
                    }
                }
            };

    ComboFrameController(View view) {
        this.view = view;
    }

    TemplateProvider templateProvider() {
        return templateProvider;
    }

    ExternalBrowserAction printAction() {
        return printAction;
    }

    ExternalBrowserAction exportAction() {
        return exportAction;
    }

    void setDocument(Document document) {
        Document previousDocument = view.document();
        if (previousDocument != null) {
            previousDocument.getScaleModel().removeScaleListener(this);
            previousDocument.removeDocumentListener(this);
            previousDocument.getProofAction()
                    .removePropertyChangeListener(toolButtonListener);
            toolButtonListener = null;
        }

        view.installDocumentComponents(
                document, document == null ? disabledEditorText() : null);

        if (document != null) {
            document.getScaleModel().addScaleListener(this);
            document.addDocumentListener(this);
            OtherApplication source = (OtherApplication) document.getSource();
            if (!view.isEditorVisible() || source != null) {
                view.showEditorPerspective();
            }
            toolButtonListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent event) {
                    if (ToggleAction.TOGGLE_STATE.equals(
                            event.getPropertyName())
                            && Boolean.TRUE.equals(event.getNewValue())
                            && view.isEditorLayout()) {
                        view.ensureToolsVisible();
                    }
                }
            };
            document.getProofAction()
                    .addPropertyChangeListener(toolButtonListener);
        } else if (!view.isBrowserVisible()) {
            view.showBrowserPerspective();
        }

        view.finishDocumentUpdate();
    }

    void dispose() {
        Document document = view.document();
        if (document != null && toolButtonListener != null) {
            document.getProofAction()
                    .removePropertyChangeListener(toolButtonListener);
            toolButtonListener = null;
        }
    }

    void showFolder(File folder, boolean useCache) {
        view.disposeBrowser();
        ImageList oldImages = view.imageList();
        if (oldImages != null) {
            oldImages.stop();
        }
        view.prepareForFolderChange();

        initializeImages(folder, useCache);

        AbstractImageBrowser browser = BrowserFactory.createRecent(
                view.imageList());
        view.installBrowser(browser);
        initializeBrowserSelection(folder);

        ImageList images = view.imageList();
        images.start();
        if (oldImages != null) {
            int pauses = oldImages.getPauseDepth();
            for (int n = 0; n < pauses; n++) {
                images.pause();
            }
        }
        view.updateMenus();
        view.setDisabledEditorText(disabledEditorText());
        view.setRecentFolder(folder);
        updateTitle();
    }

    void navigateToFolder(File folder) {
        FolderCtrl folders = view.folderCtrl();
        if (!folders.goToFolder(folder)) {
            return;
        }
        folders.restorePath(getKeyForFolder(folder));
        if (!view.isBrowserVisible()) {
            view.showBrowserPerspective();
        }
    }

    void saveFolder(File folder) {
        FolderCtrl folders = view.folderCtrl();
        if (folder != null && folders.goToFolder(folder)) {
            folders.savePath(getKeyForFolder(folder));
        }
    }

    static void clearFolder(File folder) {
        FolderBrowserPane.clearPath(getKeyForFolder(folder));
    }

    private static String getKeyForFolder(File folder) {
        return Integer.toString(folder.getAbsolutePath().hashCode());
    }

    void pause() {
        ImageList images = view.imageList();
        if (images != null) {
            images.pause();
        }
    }

    void resume() {
        ImageList images = view.imageList();
        if (images != null) {
            images.resume();
        }
        System.gc();
    }

    @Override
    public void selectionChanged(ImageBrowserEvent event) {
        File file = event.getFile();
        if (file != null) {
            view.setImage(ImageInfo.getInstanceFor(file));
            BrowserSelectionMemory.setRememberedFile(file);
        } else {
            view.setImage(null);
        }

        closeForBrowserChange();

        if (view.document() == null) {
            DisabledEditor editor = view.disabledEditor();
            editor.removeImages();
            view.editor().hideWait();

            List<PreviewUpdater> previews = event.getSelectedPreviews();
            int previewCount = previews.size();
            if (previewCount > 8) {
                view.editor().setDisabledText(
                        LOCALE.get("TooManyPreviewsText", previewCount));
                return;
            } else {
                view.editor().setDisabledText(view.disabledEditorText());
            }
            for (PreviewUpdater preview : previews) {
                RenderedImage image = preview.getImage(
                        new PreviewUpdater.Observer() {
                            @Override
                            public void imageChanged(
                                    PreviewUpdater updater, RenderedImage image) {
                                // Preview callbacks may arrive after a
                                // document has replaced the disabled editor.
                                if (view.editor() instanceof DisabledEditor) {
                                    view.disabledEditor().updateImage(
                                            updater, image);
                                }
                            }
                        });
                editor.addImage(preview, image);
            }
        }
        view.updateMenus();
    }

    @Override
    public void imageDoubleClicked(ImageBrowserEvent event) {
        File file = event.getFile();
        if (file == null) {
            return;
        }
        ComboFrame frame = view.frame();
        ComboFrame existingFrame = Application.getFrameForFile(file);
        if (existingFrame != null) {
            existingFrame.requestFocus();
            return;
        }
        closeForBrowserChange();
        if (view.document() == null) {
            Application.open(file, frame, null);
        }
    }

    @Override
    public void browserError(String message) {
        // The legacy frame listener intentionally has no error UI here.
    }

    private void closeForBrowserChange() {
        Document document = view.document();
        if (document == null) {
            return;
        }
        SaveResult saved = document.isDirty() ? autoSave() : SaveResult.Saved;
        if (saved == SaveResult.Saved || saved == SaveResult.DontSave) {
            Application.closeDocument(view.frame());
        }
    }

    boolean closeDocument() {
        if (view.document() == null) {
            return true;
        }
        SaveResult saved = autoSave();
        switch (saved) {
            case Saved:
            case DontSave:
                // Application.closeDocument() disposes the Document.
                Application.closeDocument(view.frame());
                break;
            case CouldntSave:
                switch (Application.saveAs(view.frame())) {
                    case Saved:
                    case DontSave:
                        return true;
                    case CouldntSave:
                    case Cancelled:
                        return false;
                }
            case Cancelled:
                return false;
        }
        return true;
    }

    SaveResult autoSave() {
        SaveResult result = SaveResult.Saved;
        Document document = view.document();
        if (document != null && document.isDirty()) {
            SaveOptions options = document.getSaveOptions();
            if (options == null) {
                if (OtherApplicationShim.shouldSaveDirectly(document)) {
                    options = OtherApplicationShim.createExportOptions(document);
                } else {
                    options = Application.getSaveOptions(document);
                }
            }
            Preferences prefs = Preferences.userRoot().node(
                    "/com/lightcrafts/app");
            boolean autoSave = prefs.getBoolean("AutoSave", true);
            if (autoSave) {
                document.setSaveOptions(options);
                result = Application.save(view.frame()) ?
                        SaveResult.Saved : SaveResult.CouldntSave;
            } else {
                JLabel prompt = new JLabel(LOCALE.get(
                        "AutoSaveQuestion", options.getFile().getName()));
                Box message = Box.createVerticalBox();
                message.add(prompt);
                int dialogOption = UICompliance.showOptionDialog(
                        view.frame(),
                        message,
                        LOCALE.get("AutoSaveDialogTitle"),
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new Object[] {
                                LOCALE.get("AutoSaveSaveOption"),
                                LOCALE.get("AutoSaveSaveAsOption"),
                                LOCALE.get("AutoSaveCancelOption"),
                                LOCALE.get("AutoSaveDontSaveOption")
                        },
                        LOCALE.get("AutoSaveSaveOption"), 3);

                switch (dialogOption) {
                    case 0:
                        document.setSaveOptions(options);
                        result = Application.save(view.frame()) ?
                                SaveResult.Saved : SaveResult.CouldntSave;
                        break;
                    case 1:
                        result = Application.saveAs(view.frame());
                        break;
                    case 2:
                    case -1:
                        result = SaveResult.Cancelled;
                        break;
                    case 3:
                        Application.closeDocumentForce(view.frame());
                        result = SaveResult.DontSave;
                        break;
                }
            }
        }
        return result;
    }

    @Override
    public void folderSelectionChanged(File folder) {
        if (folder != null && folder.equals(lastSelectedFolder)) {
            // Folder monitoring may report a spurious selection event.
            return;
        }
        lastSelectedFolder = folder;
        saveFolder(folder);
        Application.notifyRecentFolder(folder);
        showFolder(folder, true);
    }

    @Override
    public void folderDropAccepted(List<File> files, File folder) {
        // The browser learns the copy/move action only in a later drag-source
        // callback, so retain the existing two-event-queue deferral.
        EventQueue.invokeLater(() -> EventQueue.invokeLater(() -> {
            int copyOrMove = view.browser().wasCopyOrMove();
            switch (copyOrMove) {
                case DnDConstants.ACTION_MOVE:
                    Application.moveFiles(view.frame(), files, folder);
                    break;
                case DnDConstants.ACTION_COPY:
                    Application.copyFiles(view.frame(), files, folder);
                    break;
            }
        }));
    }

    @Override
    public void addTemplate() {
        TemplateControl templates = view.frame().getTemplateControl();
        String namespace = templates.getNamespace();
        TemplateKey key = Application.saveTemplate(view.frame(), namespace);
        if (key != null) {
            templates.setNamespace(key.getNamespace());
        }
    }

    @Override
    public void documentChanged(Document document, boolean isDirty) {
        updateTitle();
    }

    @Override
    public void scaleChanged(Scale scale) {
        updateTitle();
    }

    @Override
    public void docFilesChanged(File imageFile) {
        if (view.browser() != null) {
            File folder = imageFile.getParentFile();
            if (folder != null && folder.equals(view.recentFolder())) {
                view.imageList().regroup();
            }
        }
    }

    void updateTitle() {
        if (view.document() == null) {
            updateTitleNoDoc();
        } else {
            updateTitleDoc();
        }
    }

    void updateTitleNoDoc() {
        if (view.document() != null) {
            return;
        }
        StringBuilder title = new StringBuilder();
        File folder = view.recentFolder();
        if (folder != null) {
            title.append(Platform.getPlatform().getDisplayNameOf(folder));
            title.append(" - ");
        }
        title.append(Version.getApplicationName());
        view.frame().setTitle(title.toString());
        WindowMenu.updateAll();
    }

    void updateTitleDoc() {
        Document document = view.document();
        StringBuilder title = new StringBuilder();
        boolean dirty = document != null && document.isDirty();
        if (dirty) {
            title.append("* ");
        }
        if (document != null) {
            File file = document.getFile();
            ImageMetadata metadata = document.getMetadata();
            if (file == null) {
                file = metadata.getFile();
            }
            String name = file.getName();
            String imageName = metadata.getFile().getName();
            if (!name.equals(imageName)) {
                name = name + " [" + imageName + "]";
            }
            title.append(name);
            Scale scale = document.getScaleModel().getCurrentScale();
            title.append(" (").append(scale).append(") - ");
        }
        title.append(Version.getApplicationName());

        ComboFrame frame = view.frame();
        frame.setTitle(title.toString());
        WindowMenu.updateAll();
        frame.getRootPane().putClientProperty(
                "windowModified", dirty ? Boolean.TRUE : Boolean.FALSE);
    }

    void initializeBrowserSelection(File folder) {
        ImageList images = view.imageList();
        File file = BrowserSelectionMemory.getRememberedFile(folder);
        if (file == null) {
            Collection<ImageDatum> datums = images.getAllImageData();
            if (!datums.isEmpty()) {
                file = datums.iterator().next().getFile();
            }
        }
        if (file != null) {
            Collection<File> files = Collections.singleton(file);
            EventQueue.invokeLater(() -> {
                // The queued selection may run after the frame is disposed.
                if (view.browser() != null) {
                    view.browser().setSelectedFiles(files);
                }
            });
        }
    }

    String disabledEditorText() {
        ImageList images = view.imageList();
        if (images == null) {
            return "";
        }
        switch (AbstractLayout.getRecentLayoutType()) {
            case Editor:
                return LOCALE.get("EditorLayoutDisabledEditorText");
            case Combo:
                return images.getAllImageData().size() > 0
                        ? LOCALE.get("ComboLayoutDisabledEditorText") : null;
            default:
                return images.getAllImageData().size() > 0
                        ? LOCALE.get("BrowserLayoutDisabledEditorText") : null;
        }
    }

    void initializeImages(File directory, boolean useCache) {
        ProgressDialog dialog = Platform.getPlatform().getProgressDialog();
        ProgressThread thread = new ProgressThread(dialog) {
            @Override
            public void run() {
                DocumentDatabase.addDocumentDirectory(directory);
                view.setImageList(new ImageList(
                        directory,
                        100,
                        FileCacheFactory.get(directory),
                        useCache,
                        ImageDatumComparator.CaptureTime,
                        getProgressIndicator()));
            }

            @Override
            public void cancel() {
                ImageList.cancel();
            }
        };
        FileSystemView fileSystemView =
                Platform.getPlatform().getFileSystemView();
        String directoryName = fileSystemView.getSystemDisplayName(directory);
        dialog.showProgress(
                view.frame(), thread,
                LOCALE.get("ScanningMessage", directoryName), 0, 1, true);
        Throwable failure = dialog.getThrown();
        if (failure != null) {
            throw new RuntimeException(LOCALE.get("ScanningError"), failure);
        }

        if (view.imageList().getAllImageData().isEmpty()) {
            EventQueue.invokeLater(() -> {
                if (view.currentLayout() instanceof BrowserLayout) {
                    ((BrowserLayout) view.currentLayout())
                            .ensureFoldersVisible();
                    view.showEmptyFolderAdvice();
                }
            });
        } else {
            view.hideEmptyFolderAdvice();
        }
    }

    void windowGainedFocus(WindowEvent event) {
        ComboFrame frame = view.frame();
        if (frame.isControllerDisposed()) {
            return;
        }
        ComboFrame.LastActiveComboFrame = frame;
        if (focusPaused) {
            resume();
            focusPaused = false;
        }
        frame.getFolderCtrl().resumeFolderMonitor();
        frame.getTemplateControl().refresh();
    }

    void windowLostFocus(WindowEvent event) {
        ComboFrame frame = view.frame();
        if (frame.isControllerDisposed()) {
            return;
        }
        if (!focusPaused) {
            pause();
            focusPaused = true;
        }
        frame.getFolderCtrl().pauseFolderMonitor();
    }

    void mouseWheelMoved(MouseWheelEvent event) {
        if (event.getScrollType() == 2) {
            ComboFrame frame = view.frame();
            Stream.of(frame.getEditor(), frame.getFolderCtrl(),
                            frame.getDocUndoHistory(), frame.getTemplateControl())
                    .filter(java.util.Objects::nonNull)
                    .filter(component -> isMouseWheelEventInComponent(
                            event, frame, component
                                    .getHorizontalMouseWheelSupportComponent()))
                    .findFirst()
                    .ifPresent(component -> component.horizontalMouseWheelMoved(event));
        }
    }

    private boolean isMouseWheelEventInComponent(
            MouseWheelEvent event, ComboFrame frame, JComponent component) {
        if (!component.isShowing()) {
            return false;
        }
        Point componentLocation = component.getLocationOnScreen();
        Point frameLocation = frame.getLocationOnScreen();
        Rectangle bounds = new Rectangle(
                componentLocation.x - frameLocation.x,
                componentLocation.y - frameLocation.y,
                component.getWidth(),
                component.getHeight());
        return bounds.contains(event.getPoint());
    }

}
