/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.app.advice.AdviceManager;
import com.lightcrafts.app.menu.ComboFrameMenuBar;
import com.lightcrafts.app.menu.WindowMenu;
import com.lightcrafts.app.other.OtherApplication;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.model.Scale;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.platform.ProgressDialog;
import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.templates.TemplateKey;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.MemoryMeter;
import com.lightcrafts.ui.action.ToggleAction;
import com.lightcrafts.ui.browser.ctrls.FolderCtrl;
import com.lightcrafts.ui.browser.folders.FolderBrowserPane;
import com.lightcrafts.ui.browser.folders.FolderTreeListener;
import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.browser.model.ImageDatumComparator;
import com.lightcrafts.ui.browser.model.ImageList;
import com.lightcrafts.ui.browser.model.PreviewUpdater;
import com.lightcrafts.ui.browser.view.*;
import com.lightcrafts.ui.editor.*;
import com.lightcrafts.ui.editor.assoc.DocumentDatabase;
import com.lightcrafts.ui.editor.assoc.DocumentDatabaseListener;
import com.lightcrafts.ui.export.SaveOptions;
import com.lightcrafts.ui.metadata2.MetadataScroll;
import com.lightcrafts.ui.templates.TemplateControl;
import com.lightcrafts.ui.templates.TemplateControlListener;
import com.lightcrafts.ui.toolkit.UICompliance;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.filecache.FileCacheFactory;
import com.lightcrafts.utils.thread.ProgressThread;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.*;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import static com.lightcrafts.app.Locale.LOCALE;

// An amalgamation of editor and browser components to make a single frame
// for all purposes.  Looks like Aperture and LightRoom.

public class ComboFrame
    extends JFrame
    implements ComponentListener,
               DocumentDatabaseListener,
               DocumentListener,
               ImageBrowserListener,
               ScaleListener,
               WindowFocusListener,
               TemplateControlListener,
               MouseWheelListener {
    // This is the frame icon, for frame decorations and the windows task bar:
    public final static Image IconImage;
    static {
        URL url = ComboFrame.class.getResource("resources/LightZone.png");
        IconImage = Toolkit.getDefaultToolkit().createImage(url);
    }

    // Application often wants to know which was the most recently active:
    static ComboFrame LastActiveComboFrame;

    // The data structure for the image under edit in this frame, or null if
    // there is no active editor:
    private Document doc;

    // The content pane of this frame, mainly polymorphic layout logic
    private AbstractLayout layout;

    // The browser component:
    private AbstractImageBrowser browser;

    // The data structure for the browser:
    private ImageList images;

    // The tree widget with buttons that directs the browser:
    private FolderCtrl folders;

    // The adapter that updates the browser on folder events:
    private FolderTreeListener folderListener;

    // The scroll pane for the browser:
    private ImageBrowserScrollPane browserScroll;

    // The metadata component:
    private MetadataScroll info;

    // The editor structure, enabled or disabled:
    private DisabledEditor disabledEditor;
    private Editor editor;

    // The editor undo stack, enabled or disabled:
    private DocUndoHistory history;

    // The template control, enabled or disabled:
    private TemplateControl templates;

    // The toolbar structure, including some shared buttons:
    private LayoutHeader header;

    // Some buttons (crop, rotate) require the "Tools" fading tab to show.
    private PropertyChangeListener toolButtonListener;

    // The listeners and logic to trigger AdvisorDialogs.
    private AdviceManager advice;

    // Depending on the perspective, we may or may not want to initialize
    // the editor at browser selection events.
    private boolean isEditorVisible;

    // Depending on the perspective, we may or may not want to run the browser
    // background threads.
    private boolean isBrowserVisible;

    // Menus that go with this frame:
    private ComboFrameMenuBar menus;

    // Remember the current browser folder, for updating the title
    private File recentFolder;

    // For debug mode only:
    private MemoryMeter memory;

    // In case someone maximizes, quits, relaunches, and unmaximizes, we must
    // know what unmaximized size to restore.
    private Rectangle unMaximizedBounds;

    // In windowLostFocus() and windowGainedFocus(), the ImageList is paused
    // and resumed.  This flag is set and reset in each case, so we can detect
    // the initial, unbalanced gained focus.
    private boolean focusPausedFlag;

    ComboFrame() {
        // Make sure LastActiveComboFrame is never null, for startup document
        // open events that arrive before the first frame gains focus.
        if (LastActiveComboFrame == null) {
            LastActiveComboFrame = this;
        }

        // Java 1.6 will just use a cofee cup otherwise...
        setIconImage(IconImage);

        // Update LastActiveAppFrame, pause background tasks:
        addWindowFocusListener(this);

        // Keep track of the unmaximized bounds, so they can be saved in prefs:
        addComponentListener(this);

        if (System.getProperty("lightcrafts.debug") != null) {
            initMemoryMeter();
        }
        folders = new FolderCtrl();

        browserScroll = new ImageBrowserScrollPane();
        // Don't let the split panes make the browser too small.
        browserScroll.setMinimumSize(new Dimension(120, 120));

        disabledEditor = Document.createDisabledEditor(
            new DisabledEditor.Listener() {
                public void imageClicked(Object key) {
                    PreviewUpdater updater = (PreviewUpdater) key;
                    File file = updater.getFile();
                    Application.open(ComboFrame.this, file);
                }
            }
        );
        editor = disabledEditor;

        // Don't let the split panes make the editor image too small.
        editor.getImage().setMinimumSize(new Dimension(120, 120));

        history = new DocUndoHistory();

        templates = new TemplateControl(null, this);

        menus = new ComboFrameMenuBar(this);
        setJMenuBar(menus);

        File folder = folders.getSelection();
        if ((folder == null) || ! folder.exists()) {
            if (folders.goToPicturesFolder()) {
                folder = folders.getSelection();
            }
        }
        if ((folder == null) || ! folder.exists()) {
            folder = new File(System.getProperty("user.home"));
        }
        advice = new AdviceManager(this);

        showFolder(folder, true);

        info = new MetadataScroll();
        // The metadata table needs a preferred size for the initial layout:
        info.setPreferredSize(new Dimension(250, 250));
        info.setBackground(LightZoneSkin.Colors.ToolPanesBackground);

        setBackground(LightZoneSkin.Colors.FrameBackground);

        header = new LayoutHeader(this);

        layout = new BrowserLayout(
            templates,
            editor,
            history,
            folders,
            browserScroll,
            info,
            header,
            this
        );
        switch (layout.getLayoutType()) {
            case Browser:
                isEditorVisible = false;
                isBrowserVisible = true;
                header.setBrowseSelected();
                break;
            case Editor:
                isEditorVisible = true;
                isBrowserVisible = false;
                header.setEditSelected();
                break;
            case Combo:
                isEditorVisible = true;
                isBrowserVisible = true;
                // Let modeButtons get initialized when layout changes.
        }
        if (! isBrowserVisible) {
            images.pause();
        }
        folderListener = new FolderTreeListener() {
            File recentFolder;
            public void folderSelectionChanged(File folder) {
                if ((folder != null) && folder.equals(recentFolder)) {
                    // The folder monitor mechanism causes spurious
                    // folder selection events.
                    return;
                }
                recentFolder = folder;
                // Persist the new path:
                saveFolder(folder);
                // Add a reference to this path to the recent menus:
                Application.notifyRecentFolder(folder);
                // Update this frame:
                showFolder(folder, true);
            }
            public void folderDropAccepted(
                final List<File> files, final File folder
            ) {
                // We must handle the drop on a much later event queue task,
                // because we get our flag saying whether to move or copy from
                // the browser, which itself only finds out in a DragSource
                // callback that happens after the drop is completed.
                EventQueue.invokeLater(
                    new Runnable() {
                        public void run() {
                            EventQueue.invokeLater(
                                new Runnable() {
                                    public void run() {
                                        int copyOrMove = browser.wasCopyOrMove();
                                        switch (copyOrMove) {
                                            case DnDConstants.ACTION_MOVE:
                                                Application.moveFiles(
                                                    ComboFrame.this, files, folder
                                                );
                                                break;
                                            case DnDConstants.ACTION_COPY:
                                                Application.copyFiles(
                                                    ComboFrame.this, files, folder
                                                );
                                                break;
                                        }
                                    }
                                }
                            );
                        }
                    }
                );
            }
        };
        folders.addSelectionListener(folderListener);

        setDocument(null);

        DocumentDatabase.addListener(this);

        // Disabled editor text depends on the browser initialization and
        // the layout.
        String disabledText = getDisabledEditorText();
        editor.setDisabledText(disabledText);

        updateTitle();
    }

    public void addTemplate() {
        String namespace = templates.getNamespace();
        TemplateKey key = Application.saveTemplate(ComboFrame.this, namespace);
        if (key != null) {
            namespace = key.getNamespace();
            templates.setNamespace(namespace);
        }
    }

    public void refresh() {
        File folder = folders.getSelection();
        if (folder != null) {
            showFolder(folder, false);
        }
    }

    public Document getDocument() {
        return doc;
    }

    public Editor getEditor() {
        return editor;
    }

    public AbstractImageBrowser getBrowser() {
        return browser;
    }

    public Image getIconImage() {
        return IconImage;
    }

    // Called from the constructor, the folder selection listener, and
    // refresh().  Disposes the current browser, creates a new one, initializes
    // its selection, and replaces the old browser with the new one in the
    // layout.
    void showFolder(File folder, boolean useCache) {
        unsetBrowser();
        if (images != null) {
            images.stop();
        }
        if (doc == null) {
            ((DisabledEditor) editor).removeImages();
        }
        if (info != null) {
            info.endEditing();
        }

        editor.hideWait();

        ImageList oldImages = images;

        initImages(folder, useCache);

        browser = BrowserFactory.createRecent(images);
        setBrowser();   // Puts browser as the viewport in browserScroll

        initBrowserSelection(folder);

        images.start();

        if (oldImages != null) {
            // migrate the pause depth to the new browser
            int pauses = oldImages.getPauseDepth();
            for (int n=0; n<pauses; n++) {
                images.pause();
            }
        }
        menus.update();

        String disabledText = getDisabledEditorText();
        editor.setDisabledText(disabledText);

        recentFolder = folder;

        updateTitle();
    }

    void setBrowserCollapsed() {
        if (! BrowserFactory.isCollapsed(browser)) {
            ArrayList<File> files = browser.getSelectedFiles();
            unsetBrowser();
            browser = BrowserFactory.createCollapsed(images);
            setBrowser();
            browser.setSelectedFiles(files);
            menus.update();
        }
    }

    void setBrowserExpanded() {
        if (BrowserFactory.isCollapsed(browser)) {
            ArrayList<File> files = browser.getSelectedFiles();
            unsetBrowser();
            browser = BrowserFactory.createExpanded(images);
            setBrowser();
            browser.setSelectedFiles(files);
            menus.update();
        }
    }

    public void setImage(ImageInfo imageInfo) {
        info.setImage(imageInfo);
    }

    // Implementing ImageBrowserListener.  Updates the metadata display,
    // shuts down any open Document, shows a preview if the shutdown was
    // successful, remembers the selection in preferences, and updates the
    // frame title.
    public void selectionChanged(ImageBrowserEvent event) {
        File file = event.getFile();
        if (file != null) {
            ImageInfo imageInfo = ImageInfo.getInstanceFor(file);
            setImage(imageInfo);
            BrowserSelectionMemory.setRememberedFile(file);
        }
        else {
            setImage(null);
        }
        // Attempt to put away any current editor:
        if (doc != null) {
            SaveResult saved = SaveResult.Saved;
            if (doc.isDirty()) {
                saved = autoSave();
            }
            if (
                saved == SaveResult.Saved ||
                saved == SaveResult.DontSave
            ) {
                Application.closeDocument(this);
            }
        }
        // If the editor is disabled, then show previews:
        if (doc == null) {
            // Remove any old previews:
            ((DisabledEditor) editor).removeImages();
            // Take away the "Click to Edit" message:
            editor.hideWait();

            // Get the current previews:
            List<PreviewUpdater> previews = event.getSelectedPreviews();

            // See if the user has selected a large number of new images:
            int previewCount = previews.size();
            if (previewCount > 8) {
                // We don't show previews at bulk selection events, assuming
                // the user made the selection for some purpose other than
                // seeing previews.
                String text = LOCALE.get("TooManyPreviewsText", previewCount);
                editor.setDisabledText(text);
                return;
            }
            else {
                String text = getDisabledEditorText();
                editor.setDisabledText(text);
            }
            for (PreviewUpdater preview : previews) {
                // Install each preview in the DisabledEditor display:
                RenderedImage image = preview.getImage(
                    new PreviewUpdater.Observer() {
                        public void imageChanged(
                            PreviewUpdater updater, RenderedImage image
                        ) {
                            // This callback could come at any time,
                            // maybe even after a Document has opened.
                            if (editor instanceof DisabledEditor) {
                                ((DisabledEditor) editor).updateImage(
                                    updater, image
                                );
                            }
                        }
                    }
                );
                ((DisabledEditor) editor).addImage(preview, image);
            }
        }
        menus.update();
    }

    // Implementing ImageBrowserListener.  Shuts down any open Document,
    // and if the shutdown was successful, shows a preview of the new file,
    // and then initiates the open-Document sequence.
    public void imageDoubleClicked(ImageBrowserEvent event) {
        File file = event.getFile();
        if (file == null) {
            return;
        }
        ComboFrame frame = Application.getFrameForFile(file);
        if (frame != null) {
            frame.requestFocus();
            return;
        }
        // Attempt to put away any current editor:
        if (doc != null) {
            SaveResult saved = SaveResult.Saved;
            if (doc.isDirty()) {
                saved = autoSave();
            }
            if (
                saved == SaveResult.Saved ||
                saved == SaveResult.DontSave
            ) {
                Application.closeDocument(this);
            }
        }
        // If the close was successful, then update:
        if (doc == null) {
            Application.open(file, this, null);
        }
    }

    // Part of the convoluted event handling for horizontal-scroll events.
    // See mouseWheelMoved().
    private boolean isMouseWheelEventInComponent(
        MouseWheelEvent e, JComponent comp
    ) {
        if (comp.isShowing()) {
            Point compLoc = comp.getLocationOnScreen();
            Point frameLoc = getLocationOnScreen();
            Rectangle bounds = new Rectangle(
                compLoc.x - frameLoc.x,
                compLoc.y - frameLoc.y,
                comp.getWidth(),
                comp.getHeight()
            );
            return bounds.contains(e.getPoint());
        }
        return false;
    }

    // Propagate the special horizontal-scroll mouse wheel events (Mighty
    // Mouse, two-finger trackpad gesture) to scrollable descendants.
    // This should only be called from the Platform class on Mac.
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() == 2) {
            Stream.of(editor, folders, history, templates)
                    .filter(Objects::nonNull)
                    .filter(elem -> isMouseWheelEventInComponent(e, elem.getHorizontalMouseWheelSupportComponent()))
                    .findFirst()
                    .ifPresent(elem -> elem.horizontalMouseWheelMoved(e));
        }
    }

    public void browserError(String message) {
    }

    public File getRecentFolder() {
        return recentFolder;
    }

    void showWait(String text) {
        editor.showWait(text);
    }

    void hideWait() {
        editor.hideWait();
    }

    // Updates the editor to edit the given Document, or disables the editor
    // if the argument is null.  Called from Application show() and close()
    // methods, and also from the ComboFrame constructor.
    void setDocument(Document doc) {
        if (this.doc != null) {
            ScaleModel scale = this.doc.getScaleModel();
            scale.removeScaleListener(this);
            this.doc.removeDocumentListener(this);
            this.doc.getProofAction().
                removePropertyChangeListener(toolButtonListener);
            toolButtonListener = null;
        }
        else {
            if (editor != null) {
                ((DisabledEditor) editor).dispose();
            }
        }
        if (advice != null) {
            advice.dispose();
        }
        this.doc = doc;

        advice = new AdviceManager(this);

        if (doc != null) {
            editor = doc.getEditor();
            history = new DocUndoHistory(doc);
            templates.dispose();
            templates = new TemplateControl(editor, this);
            ScaleModel scale = doc.getScaleModel();
            scale.addScaleListener(this);
            doc.addDocumentListener(this);
            OtherApplication source = (OtherApplication) doc.getSource();
            if ((! isEditorVisible) || (source != null)) {
                showEditorPerspective();
            }
            toolButtonListener =
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent event) {
                        String propName = event.getPropertyName();
                        if (propName.equals(ToggleAction.TOGGLE_STATE)) {
                            boolean selected = (Boolean) event.getNewValue();
                            if (selected && (layout instanceof EditorLayout)) {
                               ((EditorLayout) layout).ensureToolsVisible();
                            }
                        }
                    }
                };
            // Don't let the split panes make the editor image too small.
            editor.getImage().setMinimumSize(new Dimension(120, 120));

            doc.getProofAction().addPropertyChangeListener(toolButtonListener);
        }
        else {
            editor = disabledEditor;

            // Don't let the split panes make the editor image too small.
            editor.getImage().setMinimumSize(new Dimension(120, 120));

            String disabledText = getDisabledEditorText();
            editor.setDisabledText(disabledText);

            history = new DocUndoHistory();

            templates.dispose();
            templates = new TemplateControl(null, this);

            // TODO: we come here from showBrowserPerspective...
            if (! isBrowserVisible) {
                showBrowserPerspective();
            }
        }
        if (memory != null) {
            JComponent toolbar = editor.getToolBar();
            toolbar.add(memory);
        }
        layout.updateEditor(templates, editor, history, info);

        setContentPane(layout);
        header.update();
        menus.update();
        updateTitle();

        if (layout.getLayoutType() == AbstractLayout.LayoutType.Combo) {
            browser.requestFocusInWindow();
        }
    }

    public boolean openSelected() {
        // If there is a preview showing, then open the preview for editing.
        if (editor instanceof DisabledEditor) {
            PreviewUpdater updater =
                (PreviewUpdater) ((DisabledEditor) editor).getLastKey();
            if (updater != null) {
                File file = updater.getFile();
                Application.open(this, file);
                return true;
            }
        }
        return false;
    }

    public void showEditorPerspective() {
        layout.dispose();
        layout = new EditorLayout(
            templates,
            editor,
            history,
            folders,
            browserScroll,
            info,
            header
        );
        layout.updateEditor(templates, editor, history, info);

        repaint();
        setContentPane(layout);
        validate();

        requestFocusInWindow();

        if (isBrowserVisible) {
            pause();
            isBrowserVisible = false;
        }
        isEditorVisible = true;

        header.setEditSelected();

        updateDisabledEditorText();

        menus.update();
    }

    public boolean closeDocument() {
        if (doc != null) {
//            // If an unapplied template is selected, reset it before committing
//            // any changes.
//            templates.clearSelection();

            SaveResult saved = autoSave();

            switch (saved) {
                case Saved:
                case DontSave:
                    // Could just call setDocument(null), but
                    // Application.closeDocument() disposes the Document.
                    Application.closeDocument(this);
                    break;
                case CouldntSave:
                    switch (Application.saveAs(this)) {
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
        }
        return true;
    }

    // A false return value means that the perspective change was cancelled by
    // the user.
    public boolean showBrowserPerspective() {
        if (! closeDocument()) {
            return false;
        }
        if (! isBrowserVisible) {
            layout.dispose();
            layout = new BrowserLayout(
                templates,
                editor,
                history,
                folders,
                browserScroll,
                info,
                header,
                this
            );

            browser.justShown = true;

            repaint();
            setContentPane(layout);
            validate();

            browser.requestFocusInWindow();

            if (! isBrowserVisible) {
                resume();
                isBrowserVisible = true;
            }
            isEditorVisible = false;

            File file = browser.getLeadSelectedFile();
            ImageInfo imageInfo = ImageInfo.getInstanceFor(file);
            setImage(imageInfo);

            header.setBrowseSelected();

            updateDisabledEditorText();

            menus.update();
        }
        return true;
    }

    public void showComboPerspective() {
        layout.dispose();
        layout = new ComboLayout(
            templates,
            editor,
            history,
            folders,
            browserScroll,
            info,
            header,
            this
        );
        setContentPane(layout);
        validate();
        repaint();

        browser.requestFocusInWindow();

        if (! isBrowserVisible) {
            resume();
            isBrowserVisible = true;
        }
        isEditorVisible = true;

        updateDisabledEditorText();

        menus.update();
    }

    // The browser menu may want to suppress things in editor layout
    public boolean isBrowserVisible() {
        return isBrowserVisible;
    }

    // *** DocumentListener start ***

    public void documentChanged(Document doc, boolean isDirty) {
        updateTitle();
    }

    // *** DocumentListener end ***

    // *** ScaleModelListener start ***

    public void scaleChanged(Scale scale) {
        updateTitle();
    }

    // *** ScaleModelListener end ***

    // *** DocumentDatabaseListener start ***

    public void docFilesChanged(File imageFile) {
        if (browser != null) {
            // Regroup images in the browser, but only if the changed file is
            // in the directory being browsed.
            File folder = imageFile.getParentFile();
            if ((folder != null) && folder.equals(recentFolder)) {
                images.regroup();
            }
        }
    }

    // *** DocumentDatabaseListener end ***

    // Called from showFolder().
    private void updateTitleNoDoc() {
        if (doc != null) {
            return;
        }
        StringBuffer buffer = new StringBuffer();
        if (recentFolder != null) {
            String name =
                Platform.getPlatform().getDisplayNameOf( recentFolder );
            buffer.append(name);
            buffer.append(" - ");
        }
        buffer.append(Version.getApplicationName());

        setTitle(buffer.toString());

        WindowMenu.updateAll();
    }

    public void updateTitleDoc() {
        StringBuffer sb = new StringBuffer();
        boolean dirty = (doc != null) && doc.isDirty();
        if (dirty) {
            sb.append("* ");
        }
        if (doc != null) {
            File file = doc.getFile();
            ImageMetadata meta = doc.getMetadata();
            if (file == null) {
                file = meta.getFile();
            }
            String name = file.getName();
            String imageName = meta.getFile().getName();
            if (! name.equals(imageName)) {
                name = name + " [" + imageName + "]";
            }
            sb.append(name);
            ScaleModel scaleModel = doc.getScaleModel();
            Scale scale = scaleModel.getCurrentScale();
            sb.append(" (").append(scale).append(")");
            sb.append(" - ");
        }
        sb.append(Version.getApplicationName());

        setTitle(sb.toString());

        WindowMenu.updateAll();

        // Handle the little-known close button dot for dirty windows on Mac:
        JRootPane root = getRootPane();
        root.putClientProperty(
            "windowModified", dirty ? Boolean.TRUE : Boolean.FALSE
        );
    }

    // Called from setDocument(), scaleChanged(), documentChanged(),
    // Application.showPreferences(), the relicensing under the Help menu,
    // and the constructor.
    public void updateTitle() {
        if (doc != null) {
            updateTitleDoc();
        }
        else {
            updateTitleNoDoc();
        }
    }

    // Called from Application before Document initialization, when entering
    // the editor perspective, during file I/O that may wake the browser,
    // and at the start of batch processing.
    public void pause() {
        if (images != null) {
            images.pause();
        }
    }

    // Called from Application after Document initialization, when exiting
    // the editor perspective, after file I/O that may wake the browser, and
    // after batch processing.
    public void resume() {
        if (images != null) {
            images.resume();
        }
        // Encourage JVM to release free heap space
        System.gc();
    }

    public void dispose() {
        super.dispose();
        if (memory != null) {
            memory.dispose();
        }
        images.stop();

        if (browser != null) {
            unsetBrowser();
        }

        remove(menus);
        setJMenuBar(null);
        menus.dispose();

        templates.dispose();

        folders.removeSelectionListener(folderListener);
        folders.dispose();

        if (advice != null) {
            advice.dispose();
            advice = null;
        }
        // Document.dispose() calls DocPanel.dispose(), but if we instantiated
        // the DocPanel ourselves, then we must clean it up:
        if (doc == null) {
            ((DisabledEditor) editor).dispose();
        }
        else {
            // Sever connection between Document actions and this frame.
            doc.getProofAction().
                removePropertyChangeListener(toolButtonListener);
        }
        header.dispose();
        layout.dispose();

        // Sever references, to fix leaks associated with lingering
        // DocFrame instances:
        setContentPane(new JPanel());
        doc = null;
        images = null;
        layout = null;
        browser = null;
        editor = null;
        folders = null;
        browserScroll = null;
        info = null;
        header = null;

        if (LastActiveComboFrame == this) {
            LastActiveComboFrame = null;
        }
    }

    // Sometimes the focus listener gets called after dispose().
    private boolean isDisposed() {
        return browser == null;
    }

    // Initialize the ImageList data for the browser from the given directory
    // under a ProgressDialog.
    private void initImages(final File directory, final boolean useCache) {
        ProgressDialog dialog = Platform.getPlatform().getProgressDialog();
        ProgressThread thread = new ProgressThread(dialog) {
            public void run() {
                DocumentDatabase.addDocumentDirectory(directory);
                images = new ImageList(
                    directory,
                    100,
                    FileCacheFactory.get(directory),
                    useCache,
                    ImageDatumComparator.CaptureTime,
                    getProgressIndicator()
                );
            }
            public void cancel() {
                ImageList.cancel();
            }
        };
        FileSystemView view = Platform.getPlatform().getFileSystemView();
        String dirName = view.getSystemDisplayName(directory);
        dialog.showProgress(
            this, thread, LOCALE.get("ScanningMessage", dirName), 0, 1, true
        );
        Throwable t = dialog.getThrown();
        if (t != null) {
            throw new RuntimeException(LOCALE.get("ScanningError"), t);
        }
        if (images.getAllImageData().isEmpty()) {
            // Enqueue this, because the layout may be getting initialized
            // on this same task.
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        if (layout instanceof BrowserLayout) {
                            ((BrowserLayout) layout).ensureFoldersVisible();
                            advice.showEmptyFolderAdvice();
                        }
                    }
                }
            );
        }
        else {
            advice.hideEmptyFolderAdvice();
        }
    }

    // Do the things we do when there is a new browser.
    private void setBrowser() {
        browser.addBrowserListener(this);

        browser.setTemplateProvider(templateProvider);
        browser.addBrowserAction(exportAction);
        browser.addBrowserAction(printAction);
        browser.setImageGroupProvider(new LznImageGroupProvider());
        browser.setPreviewProvider(new LznPreviewProvider(this));

        browserScroll.setBrowser(browser);

        if (layout != null) {
            // This method may be called at browser initialization, before the
            // layout member is initialized.
            header.update();
            layout.updateBrowser();
        }
        // Now here is a mystery: a repaint() at this time does not result in a
        // call to paint() on the browser.  The call works on a subsequent task.
        //
        // Also initialize the browser selection on a subsequent task, rather
        // than propagating a selection change from here.
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    browser.repaint();
                }
            }
        );
    }

    // Undo the things we do when there is a new browser.
    private void unsetBrowser() {
        if (browser != null) {
            browser.removeBrowserListener(this);
            BrowserFactory.dispose(browser);
        }
    }

    private void updateDisabledEditorText() {
        if (editor instanceof DisabledEditor) {
            String text = getDisabledEditorText();
            editor.setDisabledText(text);
        }
    }

    private void initBrowserSelection(File folder) {
        // Set an initial selection in the browser, from prefs if possible
        File file = BrowserSelectionMemory.getRememberedFile(folder);
        if (file == null) {
            Collection<ImageDatum> datums = images.getAllImageData();
            if (! datums.isEmpty()) {
                ImageDatum datum = images.getAllImageData().get(0);
                file = datum.getFile();
            }
        }
        if (file != null) {
            // Enqueue, because this method is called during the constructor.
            final Collection<File> files = Collections.singleton(file);
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        // This task may run after dispose().
                        if (browser != null) {
                            browser.setSelectedFiles(files);
                        }
                    }
                }
            );
        }
    }

    private String getDisabledEditorText() {
        if (images == null) {
            // This gets called during shutdown.
            return "";
        }
        switch (AbstractLayout.getRecentLayoutType()) {
            case Editor:
                // "Open an Image"
                return LOCALE.get("EditorLayoutDisabledEditorText");
            case Combo:
                if (images.getAllImageData().size() > 0) {
                    // "Select an Image"
                    return LOCALE.get("ComboLayoutDisabledEditorText");
                }
                // (no message; the browser will have one instead)
                return null;
            default:
                if (images.getAllImageData().size() > 0) {
                    // "Select an Image"
                    return LOCALE.get("BrowserLayoutDisabledEditorText");
                }
                // (no message; the browser will have one instead)
                return null;
        }
    }

    private SaveResult autoSave() {
        SaveResult result = SaveResult.Saved;
        if (doc != null) {
            if (doc.isDirty()) {
                SaveOptions options = doc.getSaveOptions();
                if (options == null) {
                    if (OtherApplicationShim.shouldSaveDirectly(doc)) {
                        options = OtherApplicationShim.createExportOptions(doc);
                    }
                    else {
                        options = Application.getSaveOptions(doc);
                    }
                }
                Preferences prefs = Preferences.userRoot().node(
                    "/com/lightcrafts/app"
                );
                boolean autoSave = prefs.getBoolean("AutoSave", true);
                if (autoSave) {
                    doc.setSaveOptions(options);
                    result = Application.save(this) ?
                        SaveResult.Saved : SaveResult.CouldntSave;
                }
                else {
                    JLabel prompt = new JLabel(
                        LOCALE.get(
                            "AutoSaveQuestion", options.getFile().getName()
                        )
                    );

                    Box message = Box.createVerticalBox();
                    message.add(prompt);
                    int dialogOption = UICompliance.showOptionDialog(
                        this,
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
                        LOCALE.get("AutoSaveSaveOption"), 3
                    );

                    switch (dialogOption) {
                        case 0:
                            // "save"
                            doc.setSaveOptions(options);
                            result = Application.save(this) ?
                                SaveResult.Saved : SaveResult.CouldntSave;
                            break;
                        case 1:
                            // "save elsewhere"
                            result = Application.saveAs(this);
                            break;
                        case 2:
                        case -1:
                            // "cancel"
                            result = SaveResult.Cancelled;
                            break;
                        case 3:
                            // "don't save"
                            Application.closeDocumentForce(this);
                            result = SaveResult.DontSave;
                            break;
                    }
                }
            }
        }
        return result;
    }

    // *** WindowFocusListener start ***
    //
    //     Pause and resume browser background tasks and polling, and keep
    //     the TemplateControl up to date.

    public void windowGainedFocus(WindowEvent event) {
        if (! isDisposed()) {
            LastActiveComboFrame = this;
            if (focusPausedFlag) {
                resume();
                focusPausedFlag = false;
            }
            folders.resumeFolderMonitor();
            templates.refresh();
        }
    }

    public void windowLostFocus(WindowEvent event) {
        if (! isDisposed()) {
            if (! focusPausedFlag) {
                pause();
                focusPausedFlag = true;
            }
            folders.pauseFolderMonitor();
        }
    }

    // *** WindowFocusListener end **

    // *** ComponentListener start ***
    //
    //     Keep track of our unmaximizd bounds so they can be persisted.

    public void componentResized(ComponentEvent e) {
        int state = getExtendedState();
        if (state == JFrame.NORMAL) {
            unMaximizedBounds = getBounds();
        }
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
        int state = getExtendedState();
        if (state == JFrame.NORMAL) {
            unMaximizedBounds = getBounds();
        }
    }

    Rectangle getUnmaximizedBounds() {
        // The unMaximizedBounds is only initialized after the frame validates.
        if (unMaximizedBounds != null) {
            return new Rectangle(unMaximizedBounds);
        }
        return null;
    }

    // *** ComponentListener end ***

    // *** Folder tree path persistence start ***
    //
    //     This supports the "Recent Folders" mechanism in the File menu.

    // Trigger a folder navigation, starting with the folder tree.
    // Called via the Recent Folders item and Application.openFolder().
    void showFolder(File folder) {
        if (!folders.goToFolder(folder)) {
            return;
        }
        String key = getKeyForFolder(folder);
        folders.restorePath(key);
        // This triggers the selection listener, which updates things.
        if (! isBrowserVisible) {
            // Make sure the browser is showing:
            showBrowserPerspective();
        }
    }

    // Save the currently selected folder tree path, for later access in
    // the Recent Folders item.
    void saveFolder(File folder) {
        // Don't rely on folders.getSelection(), which isn't current during the
        // selection listener callback.
        if (folder != null && folders.goToFolder(folder)) {
            String key = getKeyForFolder(folder);
            folders.savePath(key);
        }
    }

    // Clear a persistent path previously saved in saveFolder().  Called from
    // Application.addToRecentFolders().
    static void clearFolder(File folder) {
        String key = getKeyForFolder(folder);
        FolderBrowserPane.clearPath(key);
    }

    private static String getKeyForFolder(File folder) {
        String path = folder.getAbsolutePath();
        int hash = path.hashCode();
        return Integer.toString(hash);
    }

    // *** Folder tree path persistence end ***

    private void initMemoryMeter() {
        if (memory == null) {
            memory = new MemoryMeter();
            Border empty = BorderFactory.createEmptyBorder(6, 0, 6, 0);
            Border line = BorderFactory.createLineBorder(Color.gray);
            Border compound = BorderFactory.createCompoundBorder(empty, line);
            memory.setBorder(compound);
        }
    }

    // *** Helper interface implementations for use in the browser: start. ***

    private TemplateProvider templateProvider = new TemplateProvider() {
        public List getTemplateActions() {
            try {
                return TemplateDatabase.getTemplateKeys();
            }
            catch (TemplateDatabase.TemplateException e) {
                // Templates are broken, abort.
                e.printStackTrace();
                return new ArrayList();
            }
        }
        public void applyTemplateAction(Object action, File[] targets) {
            TemplateKey key = (TemplateKey) action;
            Application.applyTemplate(ComboFrame.this, targets, key);
        }
        public void applyTemplate(File file, File[] targets) {
            Application.applyTemplate(ComboFrame.this, targets, file);
        }
    };

    private ExternalBrowserAction printAction = new ExternalBrowserAction() {
        public String getName() {
            return LOCALE.get("BrowserPrintMenuItem");
        }
        public void actionPerformed(File file, File[] files) {
            Application.print(ComboFrame.this, file);
        }
    };

    private ExternalBrowserAction exportAction = new ExternalBrowserAction() {
        public String getName() {
            return LOCALE.get("BrowserExportMenuItem");
        }
        public void actionPerformed(File file, File[] files) {
            if (files.length == 1) {
                Application.export(ComboFrame.this, file);
            }
            else {
                Application.export(ComboFrame.this, files);
            }
        }
    };

    // *** Helper interface implementations for use in the browser: end. ***
}
