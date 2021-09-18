/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.browser.model.ImageDatumType;
import com.lightcrafts.ui.browser.model.ImageGroup;
import com.lightcrafts.ui.browser.model.ImageList;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.utils.awt.geom.HiDpi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Like ExpandedImageBrowser, except only the one ImageDatum in each ImageGroup
 * with the most recent file modification time is shown.
 */
public class CollapsedImageBrowser extends AbstractImageBrowser {

    public CollapsedImageBrowser(ImageList list) {
        super(list);
    }

    protected void paintComponent(Graphics graphics) {
        if (justShown) {
            if (!paintTimer.isRunning())
                paintTimer.start();
            return;
        }
        
        if (! isWidthInitialized) {
            // Only paint if the component size has been initialized.  Layout
            // jumps are typical the first time this component is displayed,
            // because the preferred height depends on the component width.
            return;
        }
        Graphics2D g = (Graphics2D) graphics;

        // Figure out which ImageDatums fall within the clip bounds.
        final Rectangle clip0 = g.getClipBounds();
        List<ImageDatum> datums = getAllImageData();
        int[] indices = getIndices(datums.size(), clip0);

        HiDpi.resetTransformScaleOf(g);
        final Rectangle clip = g.getClipBounds();

        // Iterate backwards through indices, so repaints get enqueued
        // in a visually pleasing order.
        for (int i=indices.length-1; i>=0; i--) {
            int index = indices[i];
            if (index < 0) {
                continue;
            }
            ImageDatum datum = datums.get(index);
            if (datum == null) {
                // A race; the image disappeared during painting.
                continue;
            }
            RenderedImage image = datum.getImage(this);

            // This queue prevents GC of recently painted images:
            recentImages.add(image);

            final Rectangle rect = HiDpi.imageSpaceRectFrom(getBounds(index));
            g.setClip(clip.intersection(rect));

            File file = datum.getFile();
            String label = file.getName();
            ImageDatumType type = datum.getType();
            String tag = type.toString();
            ImageMetadata meta = datum.getMetadata(true);
            boolean selected = selection.isSelected(datum);
            renderer.paint(g, image, label, tag, meta, rect, selected);

            ImageGroup group = datum.getGroup();
            if (group.isNonTrivial()) {
                ImageGroupCountRenderer.paint(g, rect, datum);
            }
        }
        g.setClip(clip);

        // The control is drawn as an overlay.
        if (controller.isEnabled()) {
            Rectangle ctrlRect = controller.getRect();
            if (ctrlRect != null) {
                if (ctrlRect.intersects(clip)) {
                    controller.paint(g);
                }
            }
        }
    }

    /**
     * Get the ImageDatums rendered in this AbstractImageBrowser: the ones that
     * are the most recently modified among all ImageDatums in their ImageGroup.
     */
    ArrayList<ImageDatum> getAllImageData() {
        List<ImageDatum> allDatums = list.getAllImageData();
        Set<ImageDatum> recentDatums = new LinkedHashSet<ImageDatum>();
        for (ImageDatum datum : allDatums) {
            ImageGroup group = datum.getGroup();
            List<ImageDatum> members = group.getImageDatums();
            long lastTime = 0;
            ImageDatum lastMember = null;
            for (ImageDatum member : members) {
                File file = member.getFile();
                long modTime = file.lastModified();
                if (modTime > lastTime) {
                    lastMember = member;
                    lastTime = modTime;
                }
            }
            recentDatums.add(lastMember);
        }
        return new ArrayList<ImageDatum>(recentDatums);
    }

    void updateSelectionDatumRemoved(ImageDatum datum, int index) {
        List<ImageDatum> selected = selection.getSelected();
        if (selected.contains(datum)) {
            if (selected.size() == 1) {
                ArrayList<ImageDatum> visible = getAllImageData();
                ArrayList<ImageDatum> all = list.getAllImageData();
                ImageDatum next = null;
                if (index == 0) {
                    // If the first image was deleted, select the new first
                    if (visible.size() > 0) {
                        next = visible.get(0);
                    }
                }
                else {
                    // If the deleted one was NOT a version, walk FORWARDS
                    // starting at the SAME position until we find a visible
                    // image.
                    if (! datum.getType().hasLznData()) {
                        int max = all.size() - 1;
                        while ((index < max) && (! visible.contains(next))) {
                            next = all.get(index++);
                        }
                    }
                    // Otherwise, walk BACKWARDS starting with the PREVIOUS
                    // position until we find a visible image
                    else {
                        while ((index > 0) && (! visible.contains(next))) {
                            next = all.get(--index);
                        }
                    }
                }
                if (next != null) {
                    int i = visible.indexOf(next);
                    selection.setLeadSelected(next);
                    if (i >= 0) {
                        Rectangle bounds = getBounds(i);
                        scrollRectToVisible(bounds);
                    }
                }
            }
            else {
                selection.removeSelected(datum);
            }
        }
    }
}
