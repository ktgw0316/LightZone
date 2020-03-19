/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.*;

import javax.media.jai.PlanarImage;
import com.lightcrafts.ui.editor.EditorMode;

import java.util.prefs.Preferences;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.PreferenceChangeEvent;

/** A base class implementing Operation that does nothing.
  */

abstract class OperationImpl implements Operation {

    // Fabio: here's an example of preference change handling.

    // The key to the preference node to listen on:
    static final String InteractiveUpdateKey = "InteractiveUpdate";

    // The default value for the preference node:
    // (Keep synced with the preferences user interface!)
    static final int DefaultInteractiveUpdateValue = 1;

    // Add the global listener to the preference node:
    static {
        Preferences prefs = Preferences.userNodeForPackage(OperationImpl.class);
        prefs.addPreferenceChangeListener(
            new PreferenceChangeListener() {
                public void preferenceChange(PreferenceChangeEvent evt) {
                    if (evt.getKey().equals(InteractiveUpdateKey)) {
                        Preferences node = evt.getNode();

                        // Warning: this is happening on a random thread.

                        interactiveRegionUpdates = node.getInt(
                            InteractiveUpdateKey, DefaultInteractiveUpdateValue
                        );
                        // maybe trigger an image rendering
                    }
                }
            }
        );
    }

    // End preference change handling example.

    protected Rendering rendering;
    private String name;
    private Region region;
    private RGBColorSelection colorSelection = new RGBColorSelection();
    protected boolean invertedRegion;
    private boolean active;
    protected boolean changed;
    private boolean selected = false;

    // The depth of the current batch:
    int batch;

    static int interactiveRegionUpdates = Preferences.userNodeForPackage(OperationImpl.class).getInt(InteractiveUpdateKey, 1);

    protected Transform operation = null;

    protected float scale = 1;

    OperationImpl(Rendering engine, String name) {
        this.rendering = engine;
        this.name = name;
        active = true;
        changed = false;
        region = null;
//        colorSelection = new ColorSelection();
    }

    abstract public boolean neutralDefault();

    @Override
    public EditorMode getPreferredMode() {
        return EditorMode.ARROW;
    }

    public String getName() {
        return name;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean getSelected() {
        return selected;
    }

    @Override
    public void setActivated(boolean active) {
        this.active = active;
        settingsChanged();
    }

    private boolean deactivatable = true;

    public boolean isDeactivatable() {
        return deactivatable;
    }

    @Override
    public void setEngineDeActivatable(boolean deactivatable) {
        this.deactivatable = deactivatable;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public RGBColorSelection getColorSelection() {
        return colorSelection;
    }

    @Override
    public void changeBatchStarted() {
        assert batch >= 0;
        batch++;
    }

    private boolean regionChanged = false;

    @Override
    public void changeBatchEnded() {
        assert batch > 0;
        --batch;

        // System.out.println("batch: " + batch + ", interactiveRegionUpdates: " + interactiveRegionUpdates);

        if (/* batch == 0 || */ (regionChanged && interactiveRegionUpdates == 1)) {
            settingsChanged();
            regionChanged = false;
        }
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    // Whenever Operation parameters change, the Engine must update:
    void settingsChanged() {
        changed = true;
        rendering.update(this, batch != 0);
    }

    @Override
    public void setRegion(Region newRegion) {
        region = newRegion;
        regionChanged = true;

        // System.out.println("batch: " + batch + ", interactiveRegionUpdates: " + interactiveRegionUpdates);

        if (batch == 0 || (interactiveRegionUpdates == 0)) // prevent repaints while the user is modifying the region
            settingsChanged();
    }

    @Override
    public void setColorSelection(RGBColorSelection newColors) {
        if (newColors != null) {
            colorSelection = newColors;
        }
        else {
            colorSelection = new RGBColorSelection();
        }
        // System.out.println("setColorSelection(): " + colorSelection);

        // TODO: Paul - Anton, batching doesn't seem to work right
        // if (batch == 0) // prevent repaints while the user is modifying the region
            settingsChanged();
    }

    @Override
    public void setRegionInverted(boolean inverted) {
        invertedRegion = inverted;
        settingsChanged();
    }

    protected abstract void updateOp(Transform op);
    protected abstract Transform createOp(PlanarImage source);

    @Override
    public void dispose() {
        if (operation != null)
            operation.dispose();
    }

    protected PlanarImage render(PlanarImage source, float scale) {
        if (scale != this.scale) {
            this.scale = scale;
            changed = true;
        }

        if (operation != null)
            operation.setSource(source);

        if (changed || operation == null) {
            if (operation == null)
                operation = createOp(source);
            else
                updateOp(operation);

            changed = false;
        }

        return operation.render();
    }

    public Preview getPreview() {
        return null;
    }

    @Override
    public boolean hasFooter() {
        return true;
    }
}
