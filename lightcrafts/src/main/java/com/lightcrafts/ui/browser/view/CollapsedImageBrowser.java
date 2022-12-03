/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.browser.model.ImageGroup;
import com.lightcrafts.ui.browser.model.ImageList;

import java.awt.*;
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

    @Override
    protected void renderImageGroup(Graphics2D g, List<ImageDatum> data, int index, ImageDatum datum, Rectangle rect) {
        ImageGroup group = datum.getGroup();
        if (group.isNonTrivial()) {
            ImageGroupCountRenderer.paint(g, rect, datum);
        }
    }

    /**
     * Get the ImageDatums rendered in this AbstractImageBrowser: the ones that
     * are the most recently modified among all ImageDatums in their ImageGroup.
     */
    @Override
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

    @Override
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
