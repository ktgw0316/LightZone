/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

/**
 * A linked list of multicasters for ImageBrowserListeners, so that listeners
 * can be added and removed during listener callbacks.
 */

class ImageBrowserMulticaster {

    private ImageBrowserListener listener;
    private ImageBrowserMulticaster next;

    void add(ImageBrowserListener listener) {
        ImageBrowserMulticaster multicaster = this;
        while (multicaster.next != null) {
            multicaster = multicaster.next;
        }
        multicaster.listener = listener;
        multicaster.next = new ImageBrowserMulticaster();
    }

    void remove(ImageBrowserListener listener) {
        ImageBrowserMulticaster multicaster = this;
        do {
            if (multicaster.listener == listener) {
                multicaster.listener = null;
                if (multicaster.next != null) {
                    multicaster.listener = multicaster.next.listener;
                    multicaster.next = multicaster.next.next;
                }
                return;
            }
            multicaster = multicaster.next;
        } while (multicaster != null);
    }

    void selectionChanged(ImageBrowserEvent event) {
        if (listener != null) {
            listener.selectionChanged(event);
            next.selectionChanged(event);
        }
    }

    void imageDoubleClicked(ImageBrowserEvent event) {
        if (listener != null) {
            listener.imageDoubleClicked(event);
            next.imageDoubleClicked(event);
        }
    }

    void browserError(String message) {
        if (listener != null) {
            listener.browserError(message);
            next.browserError(message);
        }
    }
}
