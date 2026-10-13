/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.app.advice.AdviceManager;
import com.lightcrafts.app.menu.ComboFrameMenuBar;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.model.Scale;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.MemoryMeter;
import com.lightcrafts.ui.browser.ctrls.FolderCtrl;
import com.lightcrafts.ui.browser.folders.FolderTreeListener;
import com.lightcrafts.ui.browser.model.ImageList;
import com.lightcrafts.ui.browser.model.PreviewUpdater;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.BrowserFactory;
import com.lightcrafts.ui.browser.view.ImageBrowserEvent;
import com.lightcrafts.ui.browser.view.ImageBrowserScrollPane;
import com.lightcrafts.ui.editor.*;
import com.lightcrafts.ui.editor.assoc.DocumentDatabase;
import com.lightcrafts.ui.metadata2.MetadataScroll;
import com.lightcrafts.ui.templates.TemplateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;

// An amalgamation of editor and browser components to make a single frame
// for all purposes.  Looks like Aperture and LightRoom.

public class ComboFrame
    extends JFrame
    implements ComponentListener, WindowFocusListener, MouseWheelListener,
               ComboFrameController.View {
    private static final Logger logger = LoggerFactory.getLogger(ComboFrame.class);

    private final ComboFrameController controller;

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

    ComboFrame() {
        controller = new ComboFrameController(this);

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

        templates = new TemplateControl(null, controller);

        menus = new ComboFrameMenuBar(this);
        setJMenuBar(menus);

        File folder = folders.getSelection();
        if ((folder == null) || ! folder.exists()) {
            final File pictureFolder = Platform.getPlatform().getDefaultImageDirectory();
            if (folders.goToFolder(pictureFolder)) {
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
        folderListener = controller;
        folders.addSelectionListener(controller);

        setDocument(null);

        DocumentDatabase.addListener(controller);

        setMinimumSize(new Dimension(800, 400));
        updateTitle();
    }

    public void addTemplate() {
        controller.addTemplate();
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

    @Override
    public Document document() {
        return doc;
    }

    public Editor getEditor() {
        return editor;
    }

    @Override
    public Editor editor() {
        return editor;
    }

    @Override
    public DisabledEditor disabledEditor() {
        return (DisabledEditor) disabledEditor;
    }

    TemplateControl getTemplateControl() {
        return templates;
    }

    FolderCtrl getFolderCtrl() {
        return folders;
    }

    DocUndoHistory getDocUndoHistory() {
        return history;
    }

    ImageList getImageList() {
        return images;
    }

    @Override
    public ImageList imageList() {
        return images;
    }

    @Override
    public AbstractImageBrowser browser() {
        return browser;
    }

    @Override
    public void disposeBrowser() {
        unsetBrowser();
    }

    @Override
    public void prepareForFolderChange() {
        if (doc == null) {
            disabledEditor.removeImages();
        }
        if (info != null) {
            info.endEditing();
        }
        editor.hideWait();
    }

    @Override
    public void installBrowser(AbstractImageBrowser browser) {
        this.browser = browser;
        setBrowser();
    }

    @Override
    public void setRecentFolder(File folder) {
        recentFolder = folder;
    }

    @Override
    public void setDisabledEditorText(String text) {
        editor.setDisabledText(text);
    }

    @Override
    public FolderCtrl folderCtrl() {
        return folders;
    }

    @Override
    public void setImageList(ImageList images) {
        this.images = images;
    }

    @Override
    public AbstractLayout currentLayout() {
        return layout;
    }

    @Override
    public void showEmptyFolderAdvice() {
        advice.showEmptyFolderAdvice();
    }

    @Override
    public void hideEmptyFolderAdvice() {
        advice.hideEmptyFolderAdvice();
    }

    public AbstractImageBrowser getBrowser() {
        return browser;
    }

    @Override
    public File recentFolder() {
        return recentFolder;
    }

    @Override
    public String disabledEditorText() {
        return getDisabledEditorText();
    }

    @Override
    public void updateMenus() {
        menus.update();
    }

    @Override
    public ComboFrame frame() {
        return this;
    }

    boolean isControllerDisposed() {
        return isDisposed();
    }

    public Image getIconImage() {
        return IconImage;
    }

    // Called from the constructor, the folder selection listener, and
    // refresh().  Disposes the current browser, creates a new one, initializes
    // its selection, and replaces the old browser with the new one in the
    // layout.
    void showFolder(File folder, boolean useCache) {
        controller.showFolder(folder, useCache);
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
        controller.selectionChanged(event);
    }

    // Implementing ImageBrowserListener.  Shuts down any open Document,
    // and if the shutdown was successful, shows a preview of the new file,
    // and then initiates the open-Document sequence.
    public void imageDoubleClicked(ImageBrowserEvent event) {
        controller.imageDoubleClicked(event);
    }

    // Propagate the special horizontal-scroll mouse wheel events (Mighty
    // Mouse, two-finger trackpad gesture) to scrollable descendants.
    // This should only be called from the Platform class on Mac.
    public void mouseWheelMoved(MouseWheelEvent e) {
        controller.mouseWheelMoved(e);
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
        controller.setDocument(doc);
    }

    @Override
    public void installDocumentComponents(Document doc, String disabledText) {
        if (this.doc == null && editor != null) {
            disabledEditor.dispose();
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
            templates = new TemplateControl(editor, controller);
            // Don't let the split panes make the editor image too small.
            editor.getImage().setMinimumSize(new Dimension(120, 120));
        }
        else {
            editor = disabledEditor;

            // Don't let the split panes make the editor image too small.
            editor.getImage().setMinimumSize(new Dimension(120, 120));

            editor.setDisabledText(disabledText);

            history = new DocUndoHistory();

            templates.dispose();
            templates = new TemplateControl(null, controller);
        }
        if (memory != null) {
            JComponent toolbar = editor.getToolBar();
            toolbar.add(memory);
        }
        layout.updateEditor(templates, editor, history, info);
    }

    @Override
    public void finishDocumentUpdate() {
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
        return controller.closeDocument();
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

    @Override
    public boolean isEditorVisible() {
        return isEditorVisible;
    }

    @Override
    public boolean isEditorLayout() {
        return layout instanceof EditorLayout;
    }

    @Override
    public void ensureToolsVisible() {
        if (layout instanceof EditorLayout) {
            ((EditorLayout) layout).ensureToolsVisible();
        }
    }

    // *** DocumentListener start ***

    public void documentChanged(Document doc, boolean isDirty) {
        controller.documentChanged(doc, isDirty);
    }

    // *** DocumentListener end ***

    // *** ScaleModelListener start ***

    public void scaleChanged(Scale scale) {
        controller.scaleChanged(scale);
    }

    // *** ScaleModelListener end ***

    // *** DocumentDatabaseListener start ***

    public void docFilesChanged(File imageFile) {
        controller.docFilesChanged(imageFile);
    }

    // *** DocumentDatabaseListener end ***

    public void updateTitleDoc() {
        controller.updateTitleDoc();
    }

    // Called from setDocument(), scaleChanged(), documentChanged(),
    // Application.showPreferences(), the relicensing under the Help menu,
    // and the constructor.
    public void updateTitle() {
        controller.updateTitle();
    }

    // Called from Application before Document initialization, when entering
    // the editor perspective, during file I/O that may wake the browser,
    // and at the start of batch processing.
    public void pause() {
        controller.pause();
    }

    // Called from Application after Document initialization, when exiting
    // the editor perspective, after file I/O that may wake the browser, and
    // after batch processing.
    public void resume() {
        controller.resume();
    }

    public void dispose() {
        super.dispose();
        controller.dispose();
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

    // Do the things we do when there is a new browser.
    private void setBrowser() {
        browser.addBrowserListener(controller);

        browser.setTemplateProvider(controller.templateProvider());
        browser.addBrowserAction(controller.exportAction());
        browser.addBrowserAction(controller.printAction());
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
            browser.removeBrowserListener(controller);
            BrowserFactory.dispose(browser);
        }
    }

    private void updateDisabledEditorText() {
        if (editor instanceof DisabledEditor) {
            String text = getDisabledEditorText();
            editor.setDisabledText(text);
        }
    }

    private String getDisabledEditorText() {
        return controller.disabledEditorText();
    }

    // *** WindowFocusListener start ***
    //
    //     Pause and resume browser background tasks and polling, and keep
    //     the TemplateControl up to date.

    public void windowGainedFocus(WindowEvent event) {
        controller.windowGainedFocus(event);
    }

    public void windowLostFocus(WindowEvent event) {
        controller.windowLostFocus(event);
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
        controller.navigateToFolder(folder);
    }

    // Save the currently selected folder tree path, for later access in
    // the Recent Folders item.
    void saveFolder(File folder) {
        controller.saveFolder(folder);
    }

    // Clear a persistent path previously saved in saveFolder().  Called from
    // Application.addToRecentFolders().
    static void clearFolder(File folder) {
        ComboFrameController.clearFolder(folder);
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

}
