/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;

import javax.swing.*;
import java.util.Map;
import java.util.HashMap;
import java.lang.ref.WeakReference;

/**
 * A JMenu that holds ComboFrameMenuItems and can pass on update signals
 * to the handlers for each menu item when the owning ComboFrame's properties
 * change.
 * <p>
 * Each menu also serve to hold references needed by its menu items, in such
 * a way that these references may be nulled when the menu's frame is disposed.
 * This is needed for the Mac, where the framework holds references to menu
 * items indefinitely.
 */
class UpdatableDisposableMenu extends JMenu {

    private Map<String, Object> props = new HashMap<String, Object>();

    private WeakReference<ComboFrame> frameRef;

    UpdatableDisposableMenu(ComboFrame frame, String key) {
        frameRef = new WeakReference<ComboFrame>(frame);
        MenuFactory.configureMenu(key, this);

        // The frame may be null on the Mac, when there is no active window.
        if (frame != null) {
            // Hold a hard reference to the current Document, so all the weak
            // references in UpdatableMenuItems will be valid until this reference
            // is nulled.
            Document doc = frame.getDocument();
            put("document", doc);
        }
    }

    ComboFrame getComboFrame() {
        return frameRef.get();
    }

    void put(String key, Object value) {
        props.put(key, value);
    }

    Object get(String key) {
        return props.get(key);
    }

    void remove(String key) {
        props.remove(key);
    }

    boolean containsKey(String key) {
        return props.containsKey(key);
    }

    void update() {
        for (int n=0; n<getItemCount(); n++) {
            JMenuItem item = getItem(n);
            if (item instanceof UpdatableMenuItem) {
                ((UpdatableMenuItem) item).update();
            }
            else if (item instanceof UpdatableDisposableMenu) {
                ((UpdatableDisposableMenu) item).update();
            }
        }
        // Now that the UpdatableMenuItems have updated, it's OK for the
        // last Document to get GC'd.
        ComboFrame frame = getComboFrame();
        if (frame != null) {
            Document doc = frame.getDocument();
            put("document", doc);
        }
        else {
            put("document", null);
        }
    }

    /**
     * Disposal is very important because of static framework references to
     * menubar structures on the Mac.
     */
    public void dispose() {
        props.clear();
        for (int n=0; n<getItemCount(); n++) {
            JMenuItem item = getItem(n);
            if (item != null) {
                item.setAction(null);
            }
            // Null means the item is not a JMenuItem.
        }
        removeAll();
    }
}
