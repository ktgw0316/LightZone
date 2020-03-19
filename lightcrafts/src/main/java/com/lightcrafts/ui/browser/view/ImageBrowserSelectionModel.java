/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;

import java.util.*;

class ImageBrowserSelectionModel {

    private ImageDatum leadSelected;
    private LinkedList<ImageDatum> selected;
    private AbstractImageBrowser browser;

    ImageBrowserSelectionModel(AbstractImageBrowser browser) {
        this.browser = browser;
        selected = new LinkedList<ImageDatum>();
    }

    void setLeadSelected(ImageDatum datum) {
        setLeadSelected(datum, true);
    }

    void setLeadSelected(ImageDatum datum, boolean clearSelection) {
        if (leadSelected != datum) {
            leadSelected = datum;
            if (clearSelection) {
                selected.clear();
                if (datum != null) {
                    selected.add(datum);
                }
                browser.notifySelectionChanged();
                browser.repaint();
            }
        }
    }

    void addSelected(ImageDatum datum) {
        if ((! selected.contains(datum)) && (datum != null)) {
            selected.add(datum);
            browser.notifySelectionChanged();
            browser.repaint(datum);
        }
    }

    void addSelected(Collection<ImageDatum> datums) {
        boolean added = false;
        for (ImageDatum datum : datums) {
            if ((! selected.contains(datum)) && (datum != null)) {
                selected.add(datum);
                added = true;
                browser.repaint(datum);
            }
        }
        if (added) {
            browser.notifySelectionChanged();
        }
    }

    void setSelected(List<ImageDatum> datums) {

        Set<ImageDatum> removed = new HashSet<ImageDatum>(selected);
        removed.removeAll(datums);

        List<ImageDatum> added = new LinkedList<ImageDatum>(datums);
        added.removeAll(selected);

        if (added.isEmpty() && removed.isEmpty()) {
            return;
        }
        for (ImageDatum datum : removed) {
            selected.remove(datum);
            browser.repaint(datum);
        }
        for (ImageDatum datum : added) {
            if (datum != null) {
                selected.add(datum);
            }
            browser.repaint(datum);
        }
        if (datums.size() == 1) {
            leadSelected = datums.get(0);
        }
        browser.notifySelectionChanged();
    }

    void removeSelected(ImageDatum datum) {
        if (selected.contains(datum)) {
            selected.remove(datum);
            if ((leadSelected != null) && leadSelected.equals(datum)) {
                leadSelected = null;
            }
            browser.notifySelectionChanged();
            browser.repaint(datum);
        }
    }

    void removeSelected(Collection<ImageDatum> datums) {
        boolean removed = false;
        for (ImageDatum datum : datums) {
            if (selected.contains(datum)) {
                selected.remove(datum);
                if ((leadSelected != null) && leadSelected.equals(datum)) {
                    leadSelected = null;
                }
                removed = true;
                browser.repaint(datum);
            }
        }
        if (removed) {
            browser.notifySelectionChanged();
        }
    }

    void clearSelected() {
        if (! selected.isEmpty()) {
            for (ImageDatum datum : selected) {
                browser.repaint(datum);
            }
            leadSelected = null;
            selected.clear();
            browser.notifySelectionChanged();
        }
    }

    ImageDatum getLeadSelected() {
        return leadSelected;
    }

    List<ImageDatum> getSelected() {
        return new ArrayList<ImageDatum>(selected);
    }

    boolean isSelected(ImageDatum datum) {
        return selected.contains(datum);
    }

    boolean isLeadSelected(ImageDatum datum) {
        return leadSelected == datum;
    }
}
