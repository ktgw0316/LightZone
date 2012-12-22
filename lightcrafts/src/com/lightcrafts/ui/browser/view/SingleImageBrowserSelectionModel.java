/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;

import java.util.*;

/**
 * A browser selection model for the LightZone Basic mode, which disallows
 * selection of more than a single image at a time.
 */
class SingleImageBrowserSelectionModel extends ImageBrowserSelectionModel {

    private ImageDatum selected;
    private AbstractImageBrowser browser;

    SingleImageBrowserSelectionModel(AbstractImageBrowser browser) {
        super(browser);
        this.browser = browser;
    }

    void setLeadSelected(ImageDatum datum) {
        if (selected != datum) {
            selected = datum;
            browser.notifySelectionChanged();
            browser.repaint();
        }
    }

    void addSelected(ImageDatum datum) {
        if (selected == null) {
            setLeadSelected(datum);
        }
    }

    void addSelected(Collection<ImageDatum> datums) {
        if (datums.size() == 1) {
            setLeadSelected(datums.iterator().next());
        }
    }

    void setSelected(List<ImageDatum> datums) {
        if (datums.size() == 1) {
            setLeadSelected(datums.get(0));
        }
    }

    void removeSelected(ImageDatum datum) {
        if (selected == datum) {
            setLeadSelected(null);
        }
    }

    void clearSelected() {
        if (selected != null) {
            browser.repaint(selected);
            selected = null;
            browser.notifySelectionChanged();
        }
    }

    ImageDatum getLeadSelected() {
        return selected;
    }

    List<ImageDatum> getSelected() {
        if (selected != null) {
            return Collections.singletonList(selected);
        }
        return Collections.emptyList();
    }

    boolean isSelected(ImageDatum datum) {
        return selected == datum;
    }

    boolean isLeadSelected(ImageDatum datum) {
        return selected == datum;
    }
}
