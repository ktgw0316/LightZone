/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static com.lightcrafts.ui.editor.Locale.LOCALE;

/**
 * The DocUndoManager is an UndoManager that also keeps "undo" and "redo"
 * Actions in sync and maintains the Document's "dirty" flag.
 */
class DocUndoManager extends UndoManager {

    private Document doc;
    private UndoAction undoAction;
    private RedoAction redoAction;

    // A list of all edits currently in the stack, for user presentation
    private List<UndoableEdit> edits;

    // A cursor whose previous element is the most recently performed edit
    private ListIterator<UndoableEdit> currentEdit;

    // Forward events to other listeners, for DocUndoHistory
    private List<UndoableEditListener> listeners;

    DocUndoManager(Document doc) {
        this.doc = doc;
        undoAction = new UndoAction();
        redoAction = new RedoAction();
        edits = new LinkedList<UndoableEdit>();
        currentEdit = edits.listIterator();
        listeners = new LinkedList<UndoableEditListener>();
    }

    Action getUndoAction() {
        return undoAction;
    }

    Action getRedoAction() {
        return redoAction;
    }

    @Override
    public boolean addEdit(UndoableEdit edit) {
        boolean inProgress = super.addEdit(edit);
        if (edit.isSignificant()) {
            doc.markDirty();
            currentEdit.add(edit);
            while (currentEdit.hasNext()) {
                currentEdit.next();
                currentEdit.remove();
            }
        }
        undoAction.updateUndoState();
        redoAction.updateRedoState();
        // debugDump();
        return inProgress;
    }

    @Override
    public void undo() {
        final Editor editor = doc.getEditor();
        final EditorMode mode = editor.getMode();
        int numUndos = 1;
        if ( mode == EditorMode.CROP || mode == EditorMode.ROTATE ) {
            editor.setMode( EditorMode.ARROW );
            numUndos = 2;
        }
        for ( int i = 0; i < numUndos; ++i ) {
            super.undo();
            currentEdit.previous();
        }
        notifyListeners( null );
        // debugDump();
    }

    @Override
    public void redo() {
        super.redo();
        currentEdit.next();
        notifyListeners(null);
        // debugDump();
    }

    @Override
    public void discardAllEdits() {
        super.discardAllEdits();
        edits.clear();
        currentEdit = edits.listIterator();
        undoAction.updateUndoState();
        redoAction.updateRedoState();
    }

    List<UndoableEdit> getEdits() {
        return new LinkedList<UndoableEdit>(edits);
    }

    int getEditIndex() {
        return currentEdit.previousIndex();
    }

    void setEditIndex(int newIndex) {
        int index = getEditIndex();
        while (newIndex > index) {
            //redo();
            redoAction.actionPerformed( null );
            index = getEditIndex();
        }
        while (newIndex < index) {
            //undo();
            undoAction.actionPerformed( null );
            index = getEditIndex();
        }
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent event) {
        super.undoableEditHappened(event);
        notifyListeners(event);
    }

    void addUndoableEditListener(UndoableEditListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(UndoableEditEvent event) {
        for (UndoableEditListener listener : listeners) {
            listener.undoableEditHappened(event);
        }
    }

    private final class UndoAction extends AbstractAction {
        UndoAction() {
            super(LOCALE.get("UndoActionName"));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            try {
                undo();
            }
            catch (CannotUndoException e) {
                throw new RuntimeException(LOCALE.get("CannotUndoError"), e);
            }
            updateUndoState();
            redoAction.updateRedoState();
            if (! isEnabled()) {
                doc.markClean();
            }
        }

        void updateUndoState() {
            if (canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, getUndoPresentationName());
            }
            else {
                setEnabled(false);
                putValue(Action.NAME, LOCALE.get("UndoActionName"));
            }
        }

        @Override
        protected UndoAction clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }

    private final class RedoAction extends AbstractAction {
        RedoAction() {
            super(LOCALE.get("RedoActionName"));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            try {
                redo();
            }
            catch (CannotRedoException e) {
                throw new RuntimeException(LOCALE.get("CannotRedoError"), e);
            }
            updateRedoState();
            undoAction.updateUndoState();
            if (undoAction.isEnabled()) {
                doc.markDirty();
            }
        }

        void updateRedoState() {
            if (canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, getRedoPresentationName());
            }
            else {
                setEnabled(false);
                putValue(Action.NAME, LOCALE.get("RedoActionName"));
            }
        }

        @Override
        protected RedoAction clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }
}
