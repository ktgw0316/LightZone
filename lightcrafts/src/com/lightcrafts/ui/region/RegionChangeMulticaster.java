/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import java.util.Collection;
import java.util.Iterator;

/** A chain of multicasters for RegionListeners.
  */

class RegionChangeMulticaster {

    private RegionListener listener;
    private RegionChangeMulticaster next;

    void add(RegionListener listener) {
        RegionChangeMulticaster multicaster = this;
        while (multicaster.next != null) {
            multicaster = multicaster.next;
        }
        multicaster.listener = listener;
        multicaster.next = new RegionChangeMulticaster();
    }

    void remove(RegionListener listener) {
        RegionChangeMulticaster multicaster = this;
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

    void regionBatchStart(Collection cookies) {
        if (listener != null) {
            for (Iterator i=cookies.iterator(); i.hasNext(); ) {
                Object cookie = i.next();
                listener.regionBatchStart(cookie);
            }
            next.regionBatchStart(cookies);
        }
    }

    void regionChanged(Collection cookies, SharedShape shape) {
        if (listener != null) {
            for (Iterator i=cookies.iterator(); i.hasNext(); ) {
                Object cookie = i.next();
                listener.regionChanged(cookie, shape);
            }
            next.regionChanged(cookies, shape);
        }
    }

    void regionBatchEnd(Collection cookies) {
        if (listener != null) {
            for (Iterator i=cookies.iterator(); i.hasNext(); ) {
                Object cookie = i.next();
                listener.regionBatchEnd(cookie);
            }
            next.regionBatchEnd(cookies);
        }
    }
}
