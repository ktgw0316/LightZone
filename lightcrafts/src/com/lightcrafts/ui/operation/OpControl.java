/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.model.Operation;
import com.lightcrafts.ui.mode.Mode;
import static com.lightcrafts.ui.operation.Locale.LOCALE;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEditSupport;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;

/** This is the base class for all the Operation controls in an OpStack.
  * <p>
  * Important features required in derived classes include:
  * <li>
  * <ul>Support for <code>save(XmlNode)</code> and
  * <code>restore(XmlNode)</code>, used for serialization and also undo.</ul>
  * <ul>Support for operation changes as notified through operationChanged(),
  * necessary for undo.</ul>
  * </li>
  */

public abstract class OpControl extends SelectableControl {

    protected OpControlUndoSupport undoSupport;

    private Operation operation;
    private boolean isActive;
    private boolean isLocked;

    private LockedOverlay lockOver;
    private OpFooter footer;
    private OpControlEdit currentEdit;

    private List<OpControlModeListener> modeListeners;

    protected OpControl(Operation operation, OpStack stack) {
        this.operation = operation;

        undoSupport = new OpControlUndoSupport();
        modeListeners = new LinkedList<OpControlModeListener>();

        remove(title);
        title = new OpTitle(this, stack);
        title.setBackground(Background);
        title.setFont(ControlFont);
        add(title);

        maybeSetEngineDeactivatable();

        isActive = true;

        isLocked = false;
        lockOver = new LockedOverlay();

        final List layerModes = stack.getLayerModes();
        footer = new OpFooter(this, layerModes);
        if (hasFooter())
            add(footer);

        String name = OpActions.getName(operation);
        if (! isSingleton()) {
            name = stack.getNextUniqueName(name);
        }
        title.setTitleText(name);
    }

    // Derived classes must call readyForUndo() when they're ready for their
    // first undo snapshot, probably at the end of their constructors:
    protected void readyForUndo() {
        undoSupport.initialize();
    }

    void setActivated(boolean active) {
        operation.setActivated(active);
        ((OpTitle) title).setActive(active);
        isActive = active;
        if (isActive) {
            undoSupport.postEdit(
                LOCALE.get("EnableEditName") + ' ' + title.getTitleText(), true
            );
        }
        else {
            undoSupport.postEdit(
                LOCALE.get("DisableEditName") + ' ' + title.getTitleText(), true
            );
        }
    }

    boolean isActivated() {
        return isActive;
    }

    String getTitleText() {
        return title.getTitleText();
    }

    public void setLocked(boolean locked) {
        final boolean wasLocked = isLocked;
        if (locked) {
            add(lockOver, PALETTE_LAYER);
            title.addLock();
        }
        else {
            remove(lockOver);
            title.removeLock();
            if (! isContentVisible) {
                title.doExpand();
            }
        }
        isLocked = locked;
        if (isLocked != wasLocked) {
            if (isLocked) {
                undoSupport.postEdit(
                    LOCALE.get("LockEditName") + ' ' + title.getTitleText(),
                    false
                );
            }
            else {
                undoSupport.postEdit(
                    LOCALE.get("UnlockEditName") + ' ' + title.getTitleText(),
                    false
                );
            }
            title.findOpStack().notifyLockChanged(this);
            revalidate();
            repaint();
        }
    }

    public boolean isLocked() {
        return isLocked;
    }

    public boolean isSingleton() {
        return operation.isSingleton();
    }

    public boolean hasFooter() {
        return operation.hasFooter();
    }

    public boolean isRawCorrection() {
         return title.getTitleText().startsWith("RAW") ||
                 operation.getType().getName().startsWith("RAW");
    }

    public Operation getOperation() {
        return operation;
    }

    public void addModeListener(OpControlModeListener listener) {
        modeListeners.add(listener);
    }

    public void removeModeListener(OpControlModeListener listener) {
        modeListeners.remove(listener);
    }

    public void notifyListenersEnterMode(Mode mode) {
        for (OpControlModeListener listener : modeListeners) {
            listener.enterMode(mode);
        }
    }

    public void notifyListenersExitMode(Mode mode) {
        for (OpControlModeListener listener : modeListeners) {
            listener.exitMode(mode);
        }
    }

    void setRegionInverted(boolean inverted) {
        operation.setRegionInverted(inverted);
    }

    /** Derived classes must be able to handle Operation changes at any time.
      * When the Operation changes, current Operation settings
      * must be copied into the new Operation.
      * <p>
      * The framework promises that the class of the given Operation will equal
      * the class of the Operation passed in the constructor.
      */
    protected void operationChanged(Operation operation) {
        maybeSetEngineDeactivatable();
        footer.operationChanged(operation);
    }

    // To support undo, the OpStack must be able to replace this OpControl's
    // Operation. Engines don't allow Operations to be recycled, but we must
    // recycle OpControls because references to them are held by the undo
    // edits.
    void setOperation(Operation operation) {
        final Class oldClass = this.operation.getClass();
        final Class newClass = operation.getClass();
        if (! oldClass.equals(newClass)) {
            throw new RuntimeException(
                "Incompatible Operation change: " +
                "was " + oldClass + ", is " + newClass
            );
        }
        this.operation = operation;
        operation.setActivated(isActive);
        operationChanged(operation);
    }

    // Restore to presets, and capture the change as an undoable edit.
    void restorePresets(XmlNode presets) throws XMLException {
        restore(presets);
        undoSupport.postEdit(LOCALE.get("ApplyPresetEditName"));
    }

    public void doLayout() {
        super.doLayout();

        // Cram the layer controls under the content:
        if (isContentVisible) {
            final Point contentLoc = content.getLocation();
            final Dimension contentSize = content.getSize();

            final Dimension layerSize =
                (! isSingleton() && hasFooter()) ? footer.getPreferredSize() : new Dimension();

            contentSize.height -= layerSize.height;
            content.setSize(contentSize);

            final Dimension size = getSize();
            final Insets insets = getInsets();

            final int minX = insets.left;
            final int maxX = size.width - insets.right;

            if (! isSingleton() && hasFooter()) {
                footer.setLocation(
                    minX, contentLoc.y + contentSize.height
                );
                footer.setSize(maxX - minX, layerSize.height);
            }
            if (isLocked) {
                final Rectangle lockBounds = new Rectangle(
                    insets.left,
                    contentLoc.y,
                    maxX - minX,
                    contentSize.height + layerSize.height
                );
                lockOver.setBounds(lockBounds);
            }
        }
    }

    public Dimension getPreferredSize() {
        final Dimension size = super.getPreferredSize();
        if (isContentVisible && ! isSingleton() && hasFooter()) {
            size.height += footer.getPreferredSize().height;
        }
        return size;
    }

    public boolean isSwappable() {
        return ! operation.isSingleton();
    }

    private final static String ActiveTag = "Active";
    private final static String LockedTag = "Locked";

    /**
     * Store state under the given XmlDocument.XmlNode.
     */
    public void save(XmlNode node) {
        footer.save(node);
        title.save(node);
        node.setAttribute(ActiveTag, Boolean.toString(isActive));
        node.setAttribute(LockedTag, Boolean.toString(isLocked));
    }

    /**
     * Reinitialize from data under the given XmlDocument.XmlNode.
     */
    public void restore(XmlNode node) throws XMLException {
        undoSupport.restoreStart();
        footer.restore(node);
        title.restore(node);
        maybeSetEngineDeactivatable();
        if (node.hasAttribute(ActiveTag)) {
            final boolean active = Boolean.valueOf(node.getAttribute(ActiveTag));
            setActivated(active);
            // Default to isActive==true is OK.
        }
        if (node.hasAttribute(LockedTag)) {
            final boolean locked = Boolean.valueOf(node.getAttribute(LockedTag));
            if (locked) {
                // Don't call setLocked(false), because that expands the tool.
                setLocked(locked);
            }
            // Default to isLocked==false is OK.
        }
        undoSupport.restoreEnd();
    }

    public void addUndoableEditListener(UndoableEditListener listener) {
        undoSupport.addUndoableEditListener(listener);
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
        undoSupport.removeUndoableEditListener(listener);
    }

    // Whenever an Operation is created, check whether the tool title is one
    // of the kind that should not be deactivated by the "show original"
    // button.
    private void maybeSetEngineDeactivatable() {
        if (isRawCorrection() || isSingleton()) {
            // Sign of a special tool, decouple it from the "original" button:
            operation.setEngineDeActivatable(false);
        }
    }

    /**
     * Extend UndoableEditSupport to be aware when we are doing restore,
     * so it won't broadcast the change batches that happen then.
     */
    public class OpControlUndoSupport extends UndoableEditSupport {

        private boolean restoring;

        // This method is called from readyForUndo() to indicate that this
        // OpControl is ready for its first state snapshot by OpControlEdit:
        public void initialize() {
            currentEdit = new OpControlEdit();
        }

        public void postEdit(String name, boolean significant) {
            if ((! restoring) && (currentEdit != null)) {
                currentEdit.end(name);
                currentEdit.setSignificance(significant);
                postEdit(currentEdit);
                currentEdit = new OpControlEdit();
            }
        }

        public void postEdit(String name) {
            if ((! restoring) && (currentEdit != null)) {
                currentEdit.end(name);
                postEdit(currentEdit);
                currentEdit = new OpControlEdit();
            }
        }

        public void restoreStart() {
            restoring = true;
        }

        public void restoreEnd() {
            restoring = false;
        }

        public boolean isRestoring() {
            return restoring;
        }
    }

    /** A state preserve/restore edit, like StateEdit but with XmlDocuments
      * instead of Hashtables.
      */
    private final class OpControlEdit extends AbstractUndoableEdit {

        private XmlDocument beforeDoc = new XmlDocument("before");
        private XmlDocument afterDoc = new XmlDocument("after");

        private String name;

        private boolean isSignificant = true;

        private OpControlEdit() {
            save(beforeDoc.getRoot());
        }

        private void end(String name) {
            this.name = name;
            save(afterDoc.getRoot());
        }

        private void setSignificance(boolean significant) {
            isSignificant = significant;
        }

        public String getPresentationName() {
            return name;
        }

        public boolean isSignificant() {
            return isSignificant;
        }

        public void undo() {
            super.undo();
            try {
                restore(beforeDoc.getRoot());
            }
            catch (XMLException e) {
                final CannotUndoException cue = new CannotUndoException();
                cue.initCause(e);
                throw cue;
            }
            currentEdit = new OpControlEdit();
        }

        public void redo() {
            super.redo();
            try {
                restore(afterDoc.getRoot());
            }
            catch (XMLException e) {
                final CannotRedoException cre = new CannotRedoException();
                cre.initCause(e);
                throw cre;
            }
            currentEdit = new OpControlEdit();
        }
    }
}
