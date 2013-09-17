/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.Engine;
import com.lightcrafts.model.Scale;

import java.util.*;

/** This is a model for zoom-related actions on Engine Scales.  It maintains
 * a List of Scales obtained from an Engine, in sorted order.  keeps track of
 * the current Scale and defines the next-larges and next-smallest neighbor
 * Scales.  It also enables and disables two corresponding actions, so long
 * as all Scale changes happen through <code>setScale()</code>.
 */
public class ScaleModel {

    private final static Scale MinScale = new Scale(1, 40);
    private final static Scale MaxScale = new Scale(12, 1);

    private Engine engine;
    private ArrayList<Scale> scales;   // A copy of engine.getPreferredScales();

    private int index;          // The current scale from the scales List
    private Scale offScale;     // The current Scale if it is not from the List

    private LinkedList<ScaleListener> listeners;

    /** Construct a ScaleModel with the Scale set to 1:1.
     */
    ScaleModel(Engine engine) {
        this(engine, new Scale(1, 1));
    }

    /** Construct a Scale model with the Scale set to a given Scale.
     */
    ScaleModel(Engine engine, Scale scale) {
        this.engine = engine;

        scales = new ArrayList(engine.getPreferredScales());
        Collections.sort(scales);
        listeners = new LinkedList<ScaleListener>();

        setScale(scale);
    }

    public List<Scale> getScales() {
        return new ArrayList<Scale>(scales);
    }

    public Scale getCurrentScale() {
        if (offScale != null) {
            return offScale;
        }
        return scales.get(index);
    }

    public boolean setScale(Scale scale) {
        if ((scale.compareTo(MinScale) < 0) || scale.compareTo(MaxScale) > 0) {
            return false;
        }
        int i = getIndexOf(scale);
        if (i >= 0) {
            index = i;
            offScale = null;
        }
        else {
            offScale = scale;
        }
        engine.setScale(scale);

        notifyListeners();

        return true;
    }

    public void addScaleListener(ScaleListener listener) {
        listeners.add(listener);
    }

    public void removeScaleListener(ScaleListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        Scale scale = getCurrentScale();
        for (ScaleListener listener : listeners) {
            listener.scaleChanged(scale);
        }
    }

    public boolean scaleUpDown(int direction) {
        Scale scale = direction < 0 ? getNextScaleDown() : getNextScaleUp();
        return setScale(scale);
    }

    public boolean scaleUp() {
        Scale scale = getNextScaleUp();
        return setScale(scale);
    }

    public boolean scaleDown() {
        Scale scale = getNextScaleDown();
        return setScale(scale);
    }

    boolean scaleBy(float percent) {
        Scale scale = getCurrentScale();
        float factor = scale.getFactor();
        factor *= percent;
        scale = new Scale(factor);
        return setScale(scale);
    }

    private Scale getNextScaleUp() {
        Scale current = getCurrentScale();
        for (Scale next : scales) {
            if (next.compareTo(current) > 0) {
                return next;
            }
        }
        return null;
    }

    private Scale getNextScaleDown() {
        Scale current = getCurrentScale();
        for (int n=scales.size()-1; n>= 0; n--) {
            Scale next = scales.get(n);
            if (next.compareTo(current) < 0) {
                return next;
            }
        }
        return null;
    }

    public boolean canScaleUp() {
        return getNextScaleUp() != null;
    }

    public boolean canScaleDown() {
        return getNextScaleDown() != null;
    }

    private int getIndexOf(Scale scale) {
        for (int n=0; n<scales.size(); n++) {
            Scale s = scales.get(n);
            if (s.equals(scale)) {
                return n;
            }
        }
        return -1;
    }
}
