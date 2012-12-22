/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.PreviewUpdater;
import com.lightcrafts.ui.browser.model.ImageDatum;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The event structure used in ImageBrowserListener.
 */
public class ImageBrowserEvent {

    private ImageDatum lead;
    private List<ImageDatum> datums;
    private PreviewUpdater preview;
    private ArrayList<PreviewUpdater> previews;
    private int imageCount;

    /*
     * The one preview image is a preview for the file that was clicked.  The
     * List of preview images are previews for all currently selected images.
     */
    ImageBrowserEvent(
        ImageDatum datum,
        List<ImageDatum> datums,
        PreviewUpdater preview,
        List<PreviewUpdater> previews,
        int imageCount
    ) {
        this.lead = datum;
        this.datums = new ArrayList<ImageDatum>(datums);
        this.preview = preview;
        this.previews = new ArrayList<PreviewUpdater>(previews);
        this.imageCount = imageCount;
    }

    public File getFile() {
        return (lead != null) ? lead.getFile() : null;
    }

    public List<File> getFiles() {
        ArrayList<File> files = new ArrayList<File>();
        for (ImageDatum datum : datums) {
            files.add(datum.getFile());
        }
        return files;
    }

    public PreviewUpdater getPreview() {
        return preview;
    }

    public List<PreviewUpdater> getSelectedPreviews() {
        return new ArrayList<PreviewUpdater>(previews);
    }

    public int getImageCount() {
        return imageCount;
    }

    ImageDatum getLead() {
        return lead;
    }

    List<ImageDatum> getDatums() {
        return datums;
    }
}
