/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import java.awt.*;

class SpacerTraversalPolicy extends FocusTraversalPolicy {

    public Component getDefaultComponent(Container cont) {
        return getFirstSpacer((ZoneWidget) cont);
    }

    public Component getFirstComponent(Container cont) {
        return getFirstSpacer((ZoneWidget) cont);
    }

    public Component getLastComponent(Container cont) {
        return getLastSpacer((ZoneWidget) cont);
    }

    public Component getComponentAfter(Container cont, Component comp) {
        Spacer previous = getPreviousSpacer((ZoneWidget) cont, (Spacer) comp);
        if (previous != null) {
            return previous;
        }
        return getLastSpacer((ZoneWidget) cont);
    }

    public Component getComponentBefore(Container cont, Component comp) {
        Spacer next = getNextSpacer((ZoneWidget) cont, (Spacer) comp);
        if (next != null) {
            return next;
        }
        return getFirstSpacer((ZoneWidget) cont);
    }

    private Spacer getFirstSpacer(ZoneWidget zones) {
        return getSpacerAt(zones, 0);
    }

    private Spacer getLastSpacer(ZoneWidget zones) {
        Spacer first = getFirstSpacer(zones);
        Spacer next = first;
        Spacer last;
        do {
            last = next;
            next = getNextSpacer(zones, next);
        } while (next != null);
        return last;
    }

    private Spacer getNextSpacer(ZoneWidget zones, Spacer spacer) {
        int index = getIndexOf(spacer);
        return getSpacerAt(zones, index + 1);
    }

    private Spacer getPreviousSpacer(ZoneWidget zones, Spacer spacer) {
        int index = getIndexOf(spacer);
        return getSpacerAt(zones, index - 1);
    }

    private int getIndexOf(Spacer spacer) {
        return spacer.getIndex();
    }

    private Spacer getSpacerAt(ZoneWidget zones, int index) {
        Component[] comps = zones.getComponents();
        for (int n=0; n<comps.length; n++) {
            Component comp = comps[n];
            if (comp instanceof Spacer) {
                Spacer spacer = (Spacer) comp;
                int i = spacer.getIndex();
                if (i == index) {
                    return spacer;
                }
            }
        }
        return null;
    }
}
