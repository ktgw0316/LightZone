/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.browser.model.*;
import com.lightcrafts.utils.awt.geom.HiDpi;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.lightcrafts.ui.browser.view.Locale.LOCALE;

/**
 * A base class for components that render ImageDatums from an ImageList.
 */
public abstract class AbstractImageBrowser
        extends JComponent
        implements Scrollable, ImageDatumObserver, ImageListListener {
    public final static Color Background = LightZoneSkin.Colors.BrowserBackground;

    ImageList list;

    // A cached count of the number of images displayed
    int count;

    // Shared with derived classes, for painting logic:

    ImageDatumRenderer renderer;

    ImageBrowserSelectionModel selection;

    ImagePreservationQueue recentImages; // Prevent some image GC

    ImageDatumControl controller;

    // Our preferred height depends on our width; this tells whether the width
    // has ever been set, indicating that our preferred height is valid.
    boolean isWidthInitialized;

    boolean isDisabled;

    private final HashMap<ImageDatum, Integer> datumIndex; // ImageList indices

    private final LinkedList<ImageDatum> previews;    // dispose after getPreview()

    private final ImageBrowserMulticaster listeners;

    private TemplateProvider templates; // Defines templates for popup menus

    private PreviewUpdater.Provider previewer;   // Previews for LZNs

    private final ImageBrowserActions actions;    // For menus

    private final List<ExternalBrowserAction> externalActions;// Menus are extensible

    private int across; // The number of images in a row.

    // Keep track of background task pauses due to characteristic size
    // adjustments, so each pause may be balanced with a resume.
    private boolean sizePausedFlag;

    // Save just so it can be cleaned up in dispose().
    private final DragSourceAdapter dragSrcAdapter;

    // When drag-and-drop gestures end (before Java 1.6), the drag source is
    // the only one who can know whether the drag was a MOVE gesture or a
    // COPY gesture.  So we keep track of this and provide an accessor.
    // See DnDConstants.ACTION_MOVE and DnDConstants.ACTION_COPY.
    private int copyOrMove;

    public boolean justShown = true;

    final Timer paintTimer = new Timer(300, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            justShown = false;
            paintTimer.stop();
            repaint();
        }
    });

    AbstractImageBrowser(ImageList list) {

        this.list = list;

        this.count = getAllImageData().size();

        selection = new ImageBrowserSelectionModel(this);

        previews = new LinkedList<>();

        externalActions = new LinkedList<>();

        datumIndex = new HashMap<>();
        rebuildIndex();

        // Monitor for changes to the ImageDatums.
        list.addImageListListener(this);

        renderer = new ImageDatumRenderer();

        controller = new ImageDatumControl(this);

        // Add mouse listeners for popups, overlays, and selection.
        MouseInputListener mouseHandler =
                new ImageBrowserMouseListener(this, selection, controller);
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        listeners = new ImageBrowserMulticaster();

        actions = new ImageBrowserActions(this);

        recentImages = new ImagePreservationQueue();

        setBackground(Background);

        initKeyMaps();

        ImageBrowserTransferHandler transfer =
                new ImageBrowserTransferHandler(this, selection);
        setTransferHandler(transfer);

        DragSource src = DragSource.getDefaultDragSource();
        DragGestureListener init = transfer.createDragInitiator();
        src.createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_COPY_OR_MOVE, init
        );
        dragSrcAdapter = new DragSourceAdapter() {
            @Override
            public void dragDropEnd(DragSourceDropEvent dsde) {
                copyOrMove = dsde.getDropAction();
            }
        };
        src.addDragSourceListener(dragSrcAdapter);

        updateEnabled();

        across = 1;

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                inferAcross();
                if (!isWidthInitialized) {
                    isWidthInitialized = true;
                }
            }
        });
    }

    // Called from the ImageBrowserTransferHandler for its drag Image.
    Rectangle getBounds(ImageDatum datum) {
        Integer index = datumIndex.get(datum);
        if (index != null) {
            return HiDpi.imageSpaceRectFrom(getBounds(index));
        }
        return null;
    }

    /**
     * The index of the ImageDatum whose bounds overlie the given Point, or
     * -1 if the given point does not overlie any ImageDatum.
     */
    int getIndex(Point p) {
        int size = getCharacteristicSize();
        int row = p.y / size;
        int col = p.x / size;

        // Handle clicks that are in the gaps:
        if (col >= across) {
            return -1;
        }
        int index = row * across + col;
        List<ImageDatum> data = getAllImageData();
        int count = data.size();
        if (index >= count) {
            return -1;
        }
        return index;
    }

    /**
     * All indices of ImageDatums whose bounds intersect the given Rectangle,
     * assuming the given number of ImageDatums.  (We don't access the
     * ImageList here, because that structure is modified on multiple threads.)
     * Used in painting and for cancelling thumbnail tasks.
     */
    private List<Integer> getValidIndices(int count, Rectangle rect) {
        final int size = getCharacteristicSize();
        final int left = rect.x / size;
        final int right = (rect.x + rect.width) / size + 1;
        final int top = rect.y / size;
        final int bottom = (rect.y + rect.height) / size + 1;

        final List<Integer> indices = new ArrayList<>();
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                final int index = y * across + x;
                if (index <= count - 1) {
                    indices.add(index);
                }
            }
        }
        return indices;
    }

    /**
     * Get the index of the ImageDatum which lies below the ImageDatum at
     * the given index.  This is used for arrow key bindings.
     */
    int getIndexBelow(int index) {
        if (getImageDatum(index + across) != null) {
            return index + across;
        }
        return -1;
    }

    /**
     * Get the index of the ImageDatum which lies above the ImageDatum at
     * the given index.  This is used for arrow key bindings.
     */
    int getIndexAbove(int index) {
        if (getImageDatum(index - across) != null) {
            return index - across;
        }
        return -1;
    }

    /**
     * Get the index of the ImageDatum which lies left of the ImageDatum at
     * the given index.  This is used for arrow key bindings.
     */
    private int getIndexLeft(int index) {
        if (getImageDatum(index - 1) != null) {
            return index - 1;
        }
        return -1;
    }


    /**
     * Get the index of the ImageDatum which lies right of the ImageDatum at
     * the given index.  This is used for arrow key bindings.
     */
    private int getIndexRight(int index) {
        if (getImageDatum(index + 1) != null) {
            return index + 1;
        }
        return -1;
    }

    /**
     * Calls setCharacteristicSize(int), but handles transient changes
     * differently.
     */
    public void setCharacteristicSize(int size, boolean isAdjusting) {
        if (isAdjusting && (!sizePausedFlag)) {
            list.pause();
            sizePausedFlag = true;
        } else if ((!isAdjusting) && sizePausedFlag) {
            list.resume();
            sizePausedFlag = false;
        }
        setCharacteristicSize(size);
        inferAcross();
        revalidate();
    }

    // The type of the most recent successful drag gesture, either
    // DnDConstants.ACTION_COPY or DnDConstants.ACTION_MOVE.
    public int wasCopyOrMove() {
        return copyOrMove;
    }

    /**
     * Specify the ImageList "size" parameter, which determines about how big
     * ImageDatum images will be.  Triggers validation.
     */
    public void setCharacteristicSize(int size) {
        if (size != list.getSize()) {
            list.setSize(size);
            revalidate();
        }
    }

    @Override
    public int getScrollableUnitIncrement(
            Rectangle visibleRect, int orientation, int direction
    ) {
        return getCharacteristicSize();
    }

    @Override
    public int getScrollableBlockIncrement(
            Rectangle visibleRect, int orientation, int direction
    ) {
        switch (orientation) {
            case SwingConstants.HORIZONTAL:
                return visibleRect.width;
            case SwingConstants.VERTICAL:
                return visibleRect.height;
        }
        return 1;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return isDisabled;
    }

    abstract protected void renderImageGroup(
            Graphics2D g, List<ImageDatum> data, int index, ImageDatum datum, Rectangle rect);

    /**
     * Declare this method as overridden, just to provide package access to
     * browser painting for the ImageBrowserTransferHandler.
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        if (justShown) {
            if (!paintTimer.isRunning()) {
                paintTimer.start();
            }
            return;
        }
        if (!isWidthInitialized) {
            // Only paint if the component size has been initialized.  Layout
            // jumps are typical the first time this component is displayed,
            // because the preferred height depends on the component width.
            return;
        }
        Graphics2D g = (Graphics2D) graphics;

        // Set up context for the ImageGroup highlights.
        Color oldColor = g.getColor();
        g.setColor(LightZoneSkin.Colors.BrowserGroupColor);

        // Figure out which ImageDatums fall within the clip bounds.
        final Rectangle clip0 = g.getClipBounds();
        final List<ImageDatum> data = getAllImageData();
        final var indices = getValidIndices(data.size(), clip0);

        if (!indices.isEmpty()) {
            HiDpi.resetTransformScaleOf(g);
        }
        final Rectangle clip = g.getClipBounds();

        // Iterate backwards through indices, so repaints get enqueued
        // in a visually pleasing order.
        Collections.reverse(indices);
        for (final int index : indices) {
            final ImageDatum datum = data.get(index);
            if (datum == null) {
                // A race; the image disappeared during painting.
                continue;
            }
            final RenderedImage image = datum.getImage(this);

            // This queue prevents GC of recently painted images:
            recentImages.add(image);

            final Rectangle rect = HiDpi.imageSpaceRectFrom(getBounds(index));
            g.setClip(clip.intersection(rect));
            renderImageGroup(g, data, index, datum, rect);
            boolean selected = selection.isSelected(datum);
            renderer.paint(g, image, datum, rect, selected);
        }
        g.setColor(oldColor);
        g.setClip(clip);

        // The control is drawn as an overlay.
        if (controller.isEnabled()) {
            Rectangle ctrlRect = controller.getRect();
            if (ctrlRect != null && ctrlRect.intersects(clip)) {
                controller.paint(g);
            }
        }
    }

    /**
     * Report the current selection as a list of files.
     */
    public ArrayList<File> getSelectedFiles() {
        ArrayList<File> files = new ArrayList<>();
        for (ImageDatum datum : selection.getSelected()) {
            File file = datum.getFile();
            files.add(file);
        }
        return files;
    }

    /**
     * Report the current lead selection as a file, or null if there is no
     * lead selection.
     */
    public File getLeadSelectedFile() {
        ImageDatum datum = selection.getLeadSelected();
        return (datum != null) ? datum.getFile() : null;
    }

    /**
     * When an ImageDatum image updates, repaint the corresponding Rectangle.
     */
    @Override
    public void imageChanged(ImageDatum datum) {
        Integer i = datumIndex.get(datum);
        if (i != null) {
            Rectangle rect = getBounds(i);
            repaint(rect);
        }
        if (selection.getSelected().contains(datum)) {
            notifySelectionChanged();
        }
    }

    /**
     * The preferred height is the height required to wrap all the images at
     * the characteristic size within the current component width.
     */
    @Override
    public Dimension getPreferredSize() {
        ArrayList<ImageDatum> data = getAllImageData();
        int size = getCharacteristicSize();
        int width = across * size;
        int count = data.size();
        int height = (int) (Math.ceil(count / (double) across) * size);
        return new Dimension(width, height);
    }

    /**
     * The bounds of the ImageDatum at the given index.
     */
    public Rectangle getBounds(int index) {
        int row = index / across;
        int col = index % across;
        int size = getCharacteristicSize();
        return new Rectangle(col * size, row * size, size, size);
    }

    /**
     * Get the last ImageList "size" parameter passed to
     * setCharacteristicSize().
     */
    public int getCharacteristicSize() {
        return list.getSize();
    }

    public void addBrowserListener(ImageBrowserListener listener) {
        listeners.add(listener);
    }

    public void removeBrowserListener(ImageBrowserListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    // In imageAdded(), the browser automatically selects the newly added
    // image.  After batch processing, this can cause an avalanche of
    // selection changes.  So we note the time of the most recent image-added
    // event, and don't update selection if another event comes within 30
    // seconds.
    private long lastAutoSelectionTime;

    @Override
    public void imageAdded(
            ImageList source, final ImageDatum datum, int index
    ) {
        rebuildIndex();
        updateEnabled();
        revalidate();
        repaint();

        count = getAllImageData().size();

        long time = System.currentTimeMillis();
        if (time - lastAutoSelectionTime > 30000) {
            selection.setSelected(Collections.singletonList(datum));
        } else {
            // Just to update the image count:
            notifySelectionChanged();
        }
        lastAutoSelectionTime = time;
    }

    @Override
    public void imageRemoved(ImageList source, ImageDatum datum, int index) {
        updateSelectionDatumRemoved(datum, index);
        rebuildIndex();
        updateEnabled();
        revalidate();
        repaint();

        count = getAllImageData().size();

        // Just because the image count has changed:
        notifySelectionChanged();
    }

    void updateSelectionDatumRemoved(ImageDatum datum, int index) {
        List<ImageDatum> selected = selection.getSelected();
        if (selected.contains(datum)) {
            if (selected.size() == 1) {
                ImageDatum next = getImageDatum(index + 1);
                if (next == null) {
                    next = getImageDatum(index - 1);
                }
                if (next != null) {
                    selection.setLeadSelected(next);
                    Rectangle bounds = getBounds(index);
                    scrollRectToVisible(bounds);
                }
            } else {
                selection.removeSelected(datum);
            }
        }
    }

    @Override
    public void imagesReordered(ImageList source) {
        rebuildIndex();
        revalidate();
        repaint();
    }

    public void setSort(ImageDatumComparator comp) {
        list.setSort(comp);
    }

    public void setSortInverted(boolean inverted) {
        list.setSortInverted(inverted);
    }

    public void setTemplateProvider(TemplateProvider provider) {
        templates = provider;
    }

    public void setImageGroupProvider(ImageGroupProvider provider) {
        list.setImageGroupProvider(provider);
    }

    public void setPreviewProvider(PreviewUpdater.Provider provider) {
        previewer = provider;
    }

    public void addBrowserAction(ExternalBrowserAction action) {
        externalActions.add(action);
    }

    public ImageBrowserActions getActions() {
        return actions;
    }

    // Slew the browser selection to a particular file.
    // This is used to initialize a new browser, maybe by remembering
    // files that were selected last time.
    public void setSelectedFiles(Collection<File> files) {
        selection.clearSelected();
        ArrayList<ImageDatum> data = getAllImageData();
        final ImageDatum finalDatum = data.stream()
                .filter(datum -> files.contains(datum.getFile()))
                .findFirst()
                .orElse(null);
        if (finalDatum == null) return;

        selection.setLeadSelected(finalDatum);
        EventQueue.invokeLater(() -> {
            // Enqueue the scroll update, because selection
            // is routinely initialized during component
            // construction and before layout.
            Rectangle bounds = getBounds(finalDatum);
            if (bounds != null) {
                scrollRectToVisible(bounds);
            }
        });
    }

    // Select the most recently modified ImageDatum in each ImageDatumGroup.
    public void selectLatest() {
        ArrayList<ImageDatum> data = getAllImageData();
        Set<ImageDatum> latest = new LinkedHashSet<>();
        for (ImageDatum datum : data) {
            ImageGroup group = datum.getGroup();
            List<ImageDatum> members = group.getImageDatums();
            long time = 0;
            ImageDatum recent = null;
            for (ImageDatum member : members) {
                File file = member.getFile();
                long t = file.lastModified();
                if (t > time) {
                    recent = member;
                    time = t;
                }
            }
            latest.add(recent);
        }
        selection.clearSelected();
        selection.addSelected(latest);
    }

    public void selectAll() {
        ArrayList<ImageDatum> data = getAllImageData();
        selection.setSelected(data);
    }

    public void clearSelection() {
        selection.clearSelected();
    }

    public int getImageCount() {
        return count;
    }

    abstract ArrayList<ImageDatum> getAllImageData();

    // This method informs the AbstractImageBrowser that only images within
    // the given bounds are visible, and suggests that background work to
    // render thumbnails outside these bounds may be cancelled.
    public void cancelTasks(Rectangle rect) {
        // Figure out which ImageDatums fall within the bounds.
        ArrayList<ImageDatum> data = getAllImageData();
        final var indices = getValidIndices(data.size(), rect);

        // Make a hash set of the excluded ImageDatums, for quick lookup:
        HashSet<ImageDatum> excluded = indices.stream()
                .map(data::get)
                .collect(Collectors.toCollection(HashSet::new));
        data.stream()
                .filter(Predicate.not(excluded::contains))
                .forEach(ImageDatum::cancel);
    }

    // Used by ImageBrowserActions for the apply-Template actions
    TemplateProvider getTemplateProvider() {
        return templates;
    }

    // Needed by mouse listeners like ImageDatumControl.
    ImageDatum getImageDatum(int index) {
        Set<ImageDatum> data = datumIndex.keySet();
        for (Object datum1 : data) {
            ImageDatum datum = (ImageDatum) datum1;
            Integer n = datumIndex.get(datum);
            if (n == index) {
                return datum;
            }
        }
        return null;
    }

    void addContinuousSelected(ImageDatum datum, boolean isAppendix) {
        ImageDatum leadSelected = selection.getLeadSelected();
        if (leadSelected == null) {
            return;
        }
        Integer start = datumIndex.get(leadSelected);
        Integer end = datumIndex.get(datum);
        if (start == null || end == null) {
            return;
        }
        if (end < start) {
            int temp = end;
            end = start;
            start = temp;
        }
        List<ImageDatum> selected = IntStream.rangeClosed(start, end)
                .boxed()
                .map(this::getImageDatum)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (isAppendix) {
            if (selection.isSelected(leadSelected)) {
                selection.addSelected(selected);
            } else {
                selection.removeSelected(selected);
            }
        } else {
            selection.setSelected(selected);
        }
    }

    // The browser popup menu replicates a lot of the logic defined in
    // ImageBrowserActions, except things operate on the ImageDatum defined
    // by the MouseEvent context instead of the ImageDatum defined by the
    // lead selection.
    void showPopup(MouseEvent event) {
        JPopupMenu menu = new JPopupMenu();

        Point p = event.getPoint();
        int index = getIndex(p);
        if (index < 0) {
            return;
        }
        final ImageDatum datum = getImageDatum(index);
        ImageDatumType type = datum.getType();
        final ArrayList<File> files = getSelectedFiles();

        JMenuItem leftItem = new JMenuItem(LOCALE.get("LeftMenuItem"));
        leftItem.addActionListener(actions.getLeftAction());
        leftItem.setEnabled(actions.getLeftAction().isEnabled());
        menu.add(leftItem);

        JMenuItem rightItem = new JMenuItem(LOCALE.get("RightMenuItem"));
        rightItem.addActionListener(actions.getRightAction());
        rightItem.setEnabled(actions.getRightAction().isEnabled());
        menu.add(rightItem);

        // The rating actions submenu, including those that advance the
        // selection and the one that clears the rating
        JMenu ratingMenu = new JMenu(LOCALE.get("RatingItem"));
        List<SelectionAction> ratingActions =
                RatingActions.createRatingActions(this, false);
//        List<SelectionAction> ratingAdvanceActions =
//            RatingActions.createRatingAdvanceActions(this, false);
        Action clearAction =
                RatingActions.createClearRatingAction(this, false);
//        Action clearAdvanceAction =
//            RatingActions.createClearRatingAdvanceAction(this, false);
        for (Action action : ratingActions) {
            JMenuItem ratingItem = new JMenuItem(action);
            String name = (String) action.getValue(Action.NAME);
            // On Windogs only the core fonts seem to see stars
            if (Platform.isWindows()) {
                char star = '\u2605';
                if (name.length() > 0 && name.charAt(0) == star) {
                    ratingItem.setFont(new Font("Serif", Font.PLAIN, 14));
                }
            }

            ratingMenu.add(ratingItem);
        }
//        ratingMenu.addSeparator();
//        for (Action action : ratingAdvanceActions) {
//            JMenuItem ratingItem = new JMenuItem(action);
//            ratingMenu.add(ratingItem);
//        }
        ratingMenu.addSeparator();
        JMenuItem clearRatingItem = new JMenuItem(clearAction);
        ratingMenu.add(clearRatingItem);
//        JMenuItem clearRatingAdvanceItem = new JMenuItem(clearAdvanceAction);
//        ratingMenu.add(clearRatingAdvanceItem);
        menu.add(ratingMenu);

        menu.addSeparator();

        JMenuItem openItem = new JMenuItem(LOCALE.get("EditMenuItem"));
        openItem.addActionListener(e -> {
            selection.setLeadSelected(datum);
            notifyDoubleClicked(datum);
        });
        menu.add(openItem);

        JMenuItem showItem = new JMenuItem(actions.getShowFileInFolderAction());
        menu.add(showItem);

        JMenuItem renameItem = new JMenuItem(LOCALE.get("RenameMenuItem"));
        renameItem.addActionListener(e -> FileActions.renameFile(datum.getFile(), AbstractImageBrowser.this));
        renameItem.setEnabled(true);
        menu.add(renameItem);

        JMenuItem deleteAllItem = new JMenuItem(actions.getTrashAction());
        menu.add(deleteAllItem);

        menu.addSeparator();

        JMenuItem copyTemplate = new JMenuItem(LOCALE.get("CopyMenuItem"));
        copyTemplate.addActionListener(e -> ImageBrowserActions.TemplateClipboard = datum.getFile());
        if (!type.hasLznData()) {
            copyTemplate.setEnabled(false);
        }
        menu.add(copyTemplate);

        JMenuItem applyTemplate = new JMenuItem(actions.getPasteAction());
        menu.add(applyTemplate);

        // Offer template actions, but only if:
        //   the TemplateProvider has been set;
        //   the TemplateProvider is providing at least one action; and
        //   at least one ImageDatum is selected.

        JMenu templateItem = new JMenu(LOCALE.get("ApplyMenuItem"));
        if (
                (templates != null) &&
                        (!files.isEmpty())
        ) {
            List actions = templates.getTemplateActions();
            if (!actions.isEmpty()) {
                for (final Object action : actions) {
                    JMenuItem item = new JMenuItem(action.toString());
                    item.addActionListener(event1 -> templates.applyTemplateAction(action, files.toArray(new File[0])));
                    templateItem.add(item);
                }
            } else {
                templateItem.setEnabled(false);
            }
        } else {
            templateItem.setEnabled(false);
        }
        menu.add(templateItem);

        menu.addSeparator();

        for (final ExternalBrowserAction action : externalActions) {
            String name = action.getName();
            JMenuItem item = new JMenuItem(name);
            item.addActionListener( e -> action.actionPerformed(datum.getFile(), files.toArray(new File[0])));
            menu.add(item);
        }
        menu.addSeparator();

        JMenuItem refreshItem = new JMenuItem(LOCALE.get("RefreshMenuItem"));
        refreshItem.addActionListener(e -> {
            datum.refresh(false);   // don't use caches
        });
        menu.add(refreshItem);

        menu.addSeparator();

        JMenuItem showTypes = new JMenuItem(actions.getShowHideTypesAction());
        menu.add(showTypes);

        menu.show(this, p.x, p.y);
    }

    void notifySelectionChanged() {
        ImageBrowserEvent event;
        ImageDatum leadSelected = selection.getLeadSelected();
        List<ImageDatum> selected = selection.getSelected();

        // If there is no lead selection, but the selection includes only
        // one image, then call it the "lead" for purposes of constructing
        // an ImageBrowserEvent.  (This is good because, e.g., it makes the
        // metadata display look right.)
        ImageDatum lead = leadSelected;
        if (lead == null) {
            if (selected.size() == 1) {
                lead = selected.get(0);
            }
        }
        if (lead != null) {
            PreviewUpdater preview = lead.getPreview(previewer);
            event = new ImageBrowserEvent(
                    lead, selected, preview, getSelectedPreviews(), count
            );
        } else {
            event = new ImageBrowserEvent(
                    null, selected, null, getSelectedPreviews(), count
            );
        }
        disposePreviews(selected);

        listeners.selectionChanged(event);
    }

    void notifyDoubleClicked(ImageDatum datum) {
        PreviewUpdater preview = datum.getPreview(previewer);
        ImageBrowserEvent event =
                new ImageBrowserEvent(
                        datum, selection.getSelected(),
                        preview, getSelectedPreviews(), count
                );
        disposePreviews(Collections.singleton(datum));
        listeners.imageDoubleClicked(event);
    }

    void notifyError(String message) {
        Toolkit.getDefaultToolkit().beep();
        listeners.browserError(message);
    }

    // Clean up PreviewUpdaters created in notifySelectionChanged() and
    // notifyDoubleClicked().
    private void disposePreviews(Collection<ImageDatum> newPreviews) {
        HashSet<ImageDatum> disposable = new HashSet<>(previews);
        if (newPreviews != null) {
            disposable.removeAll(newPreviews);
        }
        for (ImageDatum datum : disposable) {
            datum.disposePreviews();
        }
        previews.clear();
        if (newPreviews != null) {
            previews.addAll(newPreviews);
        }
    }

    // Called from the selection model.
    void repaint(ImageDatum datum) {
        Rectangle bounds = getBounds(datum);
        if (bounds != null) {
            repaint(bounds);
        }
    }

    ArrayList<ImageDatum> getSelectedDatums() {
        return new ArrayList<>(selection.getSelected());
    }

    ImageDatum getLeadSelectedDatum() {
        return selection.getLeadSelected();
    }

    void moveSelectionUp() {
        ImageDatum lead = selection.getLeadSelected();
        if (lead != null) {
            int index = datumIndex.get(lead);
            if (index >= 0) {
                index = getIndexAbove(index);
                if (index >= 0) {
                    ImageDatum datum = getImageDatum(index);
                    selection.setLeadSelected(datum);
                    Rectangle bounds = getBounds(index);
                    scrollRectToVisible(bounds);
                }
            }
        }
    }

    void moveSelectionDown() {
        ImageDatum lead = selection.getLeadSelected();
        if (lead != null) {
            int index = datumIndex.get(lead);
            if (index >= 0) {
                index = getIndexBelow(index);
                if (index >= 0) {
                    ImageDatum datum = getImageDatum(index);
                    selection.setLeadSelected(datum);
                    Rectangle bounds = getBounds(index);
                    scrollRectToVisible(bounds);
                }
            }
        }
    }

    void moveSelectionLeft() {
        ImageDatum lead = selection.getLeadSelected();
        if (lead != null) {
            int index = datumIndex.get(lead);
            if (index >= 0) {
                index = getIndexLeft(index);
                if (index >= 0) {
                    ImageDatum datum = getImageDatum(index);
                    selection.setLeadSelected(datum);
                    Rectangle bounds = getBounds(index);
                    scrollRectToVisible(bounds);
                }
            }
        }
    }

    void moveSelectionRight() {
        ImageDatum lead = selection.getLeadSelected();
        if (lead != null) {
            int index = datumIndex.get(lead);
            if (index >= 0) {
                index = getIndexRight(index);
                if (index >= 0) {
                    ImageDatum datum = getImageDatum(index);
                    selection.setLeadSelected(datum);
                    Rectangle bounds = getBounds(index);
                    scrollRectToVisible(bounds);
                }
            }
        }
    }

    void moveSelectionNext() {
        ImageDatum lead = selection.getLeadSelected();
        if (lead != null) {
            int index = datumIndex.get(lead);
            if (index >= 0) {
                ImageDatum datum = getImageDatum(index + 1);
                if (datum != null) {
                    selection.setLeadSelected(datum);
                    Rectangle bounds = getBounds(index);
                    scrollRectToVisible(bounds);
                }
            }
        }
    }

    private ArrayList<PreviewUpdater> getSelectedPreviews() {
        List<ImageDatum> selected = selection.getSelected();
        ArrayList<PreviewUpdater> previews = new ArrayList<>();
        for (ImageDatum datum : selected) {
            PreviewUpdater preview = datum.getPreview(previewer);
            previews.add(preview);
        }
        return previews;
    }

    private void rebuildIndex() {
        datumIndex.clear();
        List<ImageDatum> data = getAllImageData();
        int n = 0;
        for (ImageDatum datum : data) {
            datumIndex.put(datum, n++);
        }
    }

    private void updateEnabled() {
        if (datumIndex.size() == 0) {
            setDisabled();
        } else {
            setEnabled();
        }
    }

    private void setEnabled() {
        if (isDisabled) {
            removeAll();
            isDisabled = false;
        }
    }

    private void setDisabled() {
        if (!isDisabled) {
            removeAll();
            setLayout(new BorderLayout());
            add(new DisabledLabel(LOCALE.get("NoImagesLabel")));
            isDisabled = true;
        }
    }

    private void inferAcross() {
        Dimension dim = getSize();
        int size = getCharacteristicSize();
        across = dim.width / size;
        across = (across > 0) ? across : 1;
    }

    private void initKeyMaps() {
        setFocusable(true);

        registerKeyboardAction(
                new AbstractAction() {
                    public void actionPerformed(ActionEvent event) {
                        moveSelectionUp();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                WHEN_FOCUSED
        );
        registerKeyboardAction(
                new AbstractAction() {
                    public void actionPerformed(ActionEvent event) {
                        moveSelectionDown();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                WHEN_FOCUSED
        );
        registerKeyboardAction(
                new AbstractAction() {
                    public void actionPerformed(ActionEvent event) {
                        moveSelectionLeft();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                WHEN_FOCUSED
        );
        registerKeyboardAction(
                new AbstractAction() {
                    public void actionPerformed(ActionEvent event) {
                        moveSelectionRight();
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
                WHEN_FOCUSED
        );
        registerKeyboardAction(
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        ImageDatum lead = selection.getLeadSelected();
                        if (lead != null) {
                            notifyDoubleClicked(lead);
                        }
                    }
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                WHEN_FOCUSED
        );

        Action deleteSelected = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                ArrayList<File> files = getSelectedFiles();
                if (!files.isEmpty()) {
                    FileActions.deleteFiles(
                            files.toArray(new File[0]),
                            AbstractImageBrowser.this
                    );
                }
            }
        };
        registerKeyboardAction(
                deleteSelected,
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                WHEN_FOCUSED
        );
        registerKeyboardAction(
                deleteSelected,
                KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
                WHEN_FOCUSED
        );
        // All the rating actions
        List<SelectionAction> ratings =
                RatingActions.createAllActions(this, true);
        for (SelectionAction action : ratings) {
            registerKeyboardAction(
                    action, action.getKeyStroke(), WHEN_FOCUSED
            );
        }
        // The left and right rotate actions
        List<SelectionAction> rotations =
                RotateActions.createAllActions(this, true);
        for (SelectionAction action : rotations) {
            registerKeyboardAction(
                    action,
                    action.getKeyStroke(),
                    WHEN_FOCUSED
            );
        }
    }

    public void dispose() {
        DragSource src = DragSource.getDefaultDragSource();
        src.removeDragSourceListener(dragSrcAdapter);
    }
}
