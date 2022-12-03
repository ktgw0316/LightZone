/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import java.util.*;

/**
 * A selection model for Curves.
 * <p>
 * The model is that there may be any nonnegative number of selected Curves.
 * The selection can change either explicitly by a call to
 * setSelectedCurve(), addSelectedCurve(), or removeSelectedCurve(), or by a
 * call to setCookie().
 * <p>
 * When setCookie() is called, the selection is restored to whatever it was
 * the last time the given cookie was set.
 */

public class CurveSelection {

    public static interface Listener {
        void selectionChanged(
            CurveIterator oldSelection, CurveIterator newSelection
        );
    }

    // The current cookie:
    private Object cookie;

    // Maps cookie Objects to Collections of Curves:
    private Map<Object,Collection<Curve>> selections =
        new HashMap<Object,Collection<Curve>>();

    // To get the current selection: (Collection) selections.get(cookie)

    private List<Listener> listeners = new LinkedList<Listener>();

    void setSelectedCurve(Curve curve) {
        if (cookie != null) {
            Collection<Curve> oldSelection = selections.get(cookie);
            if (isSelected(curve) && (oldSelection.size() == 1)) {
                return;
            }
            oldSelection = new LinkedList<Curve>(oldSelection);
            final Collection<Curve> newSelection = selections.get(cookie);
            newSelection.clear();
            newSelection.add(curve);
            notifyListeners(oldSelection);
        }
    }

    void addCurve(Curve newCurve) {
        if (isSelected(newCurve)) {
            return;
        }
        final Collection<Curve> curves = selections.get(cookie);
        final Collection<Curve> oldSelection = new LinkedList<Curve>(curves);
        curves.add(newCurve);
        notifyListeners(oldSelection);
    }

    void removeCurve(Curve oldCurve) {
        if (! isSelected(oldCurve)) {
            return;
        }
        final Collection<Curve> curves = selections.get(cookie);
        final Collection<Curve> oldSelection = new LinkedList<Curve>(curves);
        curves.remove(oldCurve);
        notifyListeners(oldSelection);
    }

    void clear() {
        final Collection<Curve> curves = selections.get(cookie);
        final Collection<Curve> oldSelection = new LinkedList<Curve>(curves);
        curves.clear();
        notifyListeners(oldSelection);
    }

    CurveIterator iterator() {
        if (cookie != null) {
            final Collection<Curve> curves = selections.get(cookie);
            return new CurveIterator(curves);
        }
        return new CurveIterator(Collections.EMPTY_SET);
    }

    boolean isSelected(Curve curve) {
        final Collection<Curve> curves = selections.get(cookie);
        return curves.contains(curve);
    }

    void setCookie(Object cookie) {
        if (this.cookie != cookie) {
            Collection<Curve> oldSelection = selections.get(this.cookie);
            if (oldSelection == null) {
                oldSelection = Collections.emptySet();
            }
            this.cookie = cookie;
            selections.put(cookie, new LinkedList<Curve>());
            notifyListeners(oldSelection);
        }
    }

    void addSelectionListener(Listener listener) {
        listeners.add(listener);
    }

    void removeSelectionListener(Listener listener) {
        listeners.remove(listener);
    }

    void notifyListeners(Collection<Curve> oldSelection) {
        for ( Listener listener : listeners ) {
            final CurveIterator oldIter = new CurveIterator( oldSelection );
            final CurveIterator newIter = iterator();
            listener.selectionChanged( oldIter, newIter );
        }
    }
}
