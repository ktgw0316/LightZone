/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A TransferHandler that uses an AbstractImageBrowser's selection model to
 * construct Transferables for drag and drop export.
 */
class ImageBrowserTransferHandler extends TransferHandler {

    /**
     * This DragInitiator knows how to start drag datatransfers.  It must
     * be attached to an AbstractImageBrowser like this:
     * <code>
     *    DragSource src = DragSource.getDefaultDragSource();
     *    DragGestureListener init = transfer.createDragInitiator();
     *    src.createDefaultDragGestureRecognizer(
     *        browser, DnDConstants.ACTION_COPY, init
     *    );
     * </code>
     */
    class DragInitiator
        extends DragSourceAdapter implements DragGestureListener
    {
        public void dragGestureRecognized(DragGestureEvent event) {
            if (selection.getSelected().isEmpty()) {
                return;
            }
            Transferable trans = createTransferable(null);
            if (DragSource.isDragImageSupported()) {
                Image image = createDragImage();
                Point origin = event.getDragOrigin();
                Point offset = new Point(- origin.x, - origin.y);
                event.startDrag(null, image, offset, trans, this);
            }
            else {
                event.startDrag(null, trans, this);
            }
        }
    }

    private AbstractImageBrowser browser;
    private ImageBrowserSelectionModel selection;

    ImageBrowserTransferHandler(
        AbstractImageBrowser browser, ImageBrowserSelectionModel selection
    ) {
        this.browser = browser;
        this.selection = selection;
    }

    DragGestureListener createDragInitiator() {
        return new DragInitiator();
    }

    protected Transferable createTransferable(JComponent c) {
        List<ImageDatum> datums = selection.getSelected();
        return new ImageDatumTransferable(datums);
    }

    public int getSourceActions(JComponent c) {
        return MOVE | COPY;
    }

    public boolean canImport(JComponent c, DataFlavor[] flavors) {
        return false;
    }

    private Image createDragImage() {
        List<ImageDatum> datums = selection.getSelected();
        Rectangle bounds = new Rectangle();
        for (ImageDatum datum : datums) {
            Rectangle rect = browser.getBounds(datum);
            bounds.add(rect);
        }
        BufferedImage image = new BufferedImage(
            bounds.width, bounds.height, BufferedImage.TYPE_4BYTE_ABGR
        );
        Graphics2D g = (Graphics2D) image.getGraphics();
        Composite composite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, .5f
        );
        g.setComposite(composite);
        for (ImageDatum datum : datums) {
            Rectangle rect = browser.getBounds(datum);
            g.setClip(rect);
            browser.paintComponent(g);
        }
        return image;
    }
}
