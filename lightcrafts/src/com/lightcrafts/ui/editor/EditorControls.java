/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.Engine;
import com.lightcrafts.model.Preview;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.ui.operation.OpStack;
import com.lightcrafts.ui.operation.OpStackListener;
import com.lightcrafts.ui.operation.SelectableControl;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;
import java.awt.*;
import java.util.Iterator;
import java.util.List;

/**
 * The container for an Editor's tool stack and Previews.
 */
public final class EditorControls extends JPanel {

    private OpStack stack;
    private PreviewSplit container;

    public EditorControls(Engine engine) {
        List previewList = engine.getPreviews();
        Preview[] previews;
        // Look out: a template-preview Engine has no previews
        int previewCount = previewList.size();
        if (previewCount > 0) {
            previews = new Preview[previewList.size() - 1];            
            Iterator it = previewList.iterator();
            // Skip the first preview; it's the styles preview
            it.next();
            int i = 0;
            while (it.hasNext()) {
                previews[i++] = (Preview) it.next();
            }
        }
        else {
            // Must be a template-preview Engine
            previews = new Preview[0];
        }
        stack = new OpStack(engine);
        container = new PreviewSplit(stack, previews);
        setLayout(new BorderLayout());
        add(container);
        setBorder(LightZoneSkin.getPaneBorder());
    }

    // Create disabled DocControls, for the no-Document display mode:
    public EditorControls() {
        container = new PreviewSplit();
        setLayout(new BorderLayout());
        add(container);
    }

    public List<Action> getOperations() {
        return stack.getAddActions();
    }

    public List<OpControl> getOpControls() {
        return stack.getOpControls();
    }

    public void addOpStackListener(OpStackListener listener) {
        stack.addOpStackListener(listener);
    }

    public void removeOpStackListener(OpStackListener listener) {
        stack.removeOpStackListener(listener);
    }

    public void addUndoableEditListener(UndoableEditListener listener) {
        stack.addUndoableEditListener(listener);
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
        stack.removeUndoableEditListener(listener);
    }

    public void setShowPreview(boolean show) {
        container.setShowPreview(show);
    }

    public void setPreview(Preview preview) {
        container.setPreview(preview);
    }

    public void setDropper(Point p) {
        container.setDropper(p);
    }

    public void addControl(SelectableControl control) {
        stack.addControl(control);
    }

    public void removeControl(SelectableControl control) {
        stack.removeControl(control);
    }

    public void save(XmlNode root) {
        stack.save(root);
    }

    public void restore(XmlNode root) throws XMLException {
        stack.restore(root);
    }

    public List<OpControl> addControls(XmlNode root) throws XMLException {
        return stack.addControls(root);
    }

    public OpControl addControl(OperationType type, int index) {
        return stack.addGenericControl(type, index);
    }

    public void removeControls(List<OpControl> controls) {
        stack.removeControls(controls);
    }

    boolean hasRawAdjustments() {
        return stack.hasRawAdjustments();
    }
}