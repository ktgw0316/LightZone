/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.ui.operation;

import com.lightcrafts.model.*;
import com.lightcrafts.model.ImageEditor.LensCorrectionsOperation;
import com.lightcrafts.ui.operation.clone.CloneControl;
import com.lightcrafts.ui.operation.clone.SpotControl;
import com.lightcrafts.ui.operation.colorbalance.ColorPickerControl;
import com.lightcrafts.ui.operation.colorbalance.ColorPickerDropperControl;
import com.lightcrafts.ui.operation.drag.DraggableStack;
import com.lightcrafts.ui.operation.drag.DraggableStackListener;
import com.lightcrafts.ui.operation.generic.GenericControl;
import com.lightcrafts.ui.operation.whitebalance.ColorDropperControl;
import com.lightcrafts.ui.operation.whitebalance.RawAdjustmentControl;
import com.lightcrafts.ui.operation.whitepoint.WhitePointControl;
import com.lightcrafts.ui.operation.zone.ZoneControl;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.prefs.Preferences;

import static com.lightcrafts.ui.operation.Locale.LOCALE;

public class OpStack extends DraggableStack
    implements Scrollable, UndoableEditListener
{
    // Intercept certain mouse events to OpControls, and update
    // the OpStack selection and text field focus accordingly:
    static {
        Toolkit.getDefaultToolkit().addAWTEventListener(
            new AWTEventListener() {
                private KeyboardFocusManager focus =
                    KeyboardFocusManager.getCurrentKeyboardFocusManager();
                public void eventDispatched(AWTEvent awtEvent) {
                    MouseEvent mEvent = (MouseEvent) awtEvent;
                    if (mEvent.getID() == MouseEvent.MOUSE_PRESSED) {
                        Component comp = mEvent.getComponent();
                        Container control = SwingUtilities.getAncestorOfClass(
                            SelectableControl.class, comp
                        );
                        if (control == null) {
                            if (comp instanceof SelectableControl) {
                                control = (Container) comp;
                            }
                        }
                        if (control != null) {
                            Container stack = SwingUtilities.getAncestorOfClass(
                                OpStack.class, comp
                            );
                            if (stack != null) {
                                ((OpStack) stack).setSelection(
                                    (SelectableControl) control
                                );
                                Component focused = focus.getFocusOwner();
                                if ((focused instanceof JTextField) &&
                                    (! (comp instanceof JTextField))) {
                                    focus.upFocusCycle();
                                }
                            }
                        }
                    }
                }
            },
            AWTEvent.MOUSE_EVENT_MASK
        );
    }
    /**
     * This width number is public, for the no-Document display mode.
     */
    public final static int PreferredWidth = 280;

    // The auto-expand property is a persistent boolean.
    private final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/ui/operation"
    );
    private final static String AutoExpandKey = "AutoExpand";

    private Engine engine;
    private LinkedList<OpControl> opControls;
    private LinkedList<SelectableControl> extraControls;
    private SelectableControl selection;

    private OpActions actions;

    // Keep an OpStack instance count of Operation names, to provide unique
    // default titles to OpControls:
    private Map<String, Integer> nameCounts = new HashMap<String, Integer>();

    private boolean isStackDragging;    // Indicate the DraggableStack mode

    private boolean autoExpandControls; // Used by SelectableTitle listeners

    private LinkedList<OpStackListener> listeners;

    private OpStackUndoSupport undoSupport;

    public OpStack(Engine engine) {
        this.engine = engine;
        setLayout(null);
        opControls = new LinkedList<OpControl>();
        extraControls = new LinkedList<SelectableControl>();
        listeners = new LinkedList<OpStackListener>();

        addDraggableStackListener(
            // Follow swaps while dragging, then commit swaps when done:
            new DraggableStackListener() {
                private List<Integer> swaps;
                public void dragStarted() {
                    swaps = new LinkedList<Integer>();
                    isStackDragging = true;
                }
                public void swapped(int index) {
                    swaps.add(index);
                }
                public void dragStopped() {
                    for (Integer index : swaps) {
                        swap(index);
                    }
                    isStackDragging = false;
                }
            }
        );
        undoSupport = new OpStackUndoSupport();

        actions = new OpActions(engine, this);

        autoExpandControls = Prefs.getBoolean(AutoExpandKey, false);
    }

    public List<Action> getAddActions() {
        return actions.getActions();
    }

    // Get disabled Actions from resources, for the no-Document display mode:
    public static List<Action> getStaticAddActions() {
        return OpActions.createStaticAddActions();
    }

    public OpControl addZoneControl() {
        int index = getOpControlCount();
        ZoneOperation op = engine.insertZoneOperation(index);
        OpControl control = new ZoneControl(op, this);
        addControl(control, index);
        return control;
    }

    public OpControl addCloneControl() {
        int index = getOpControlCount();
        CloneOperation op = engine.insertCloneOperation(index);
        OpControl control = new CloneControl(op, this);
        addControl(control, index);
        return control;
    }

    public OpControl addSpotControl() {
        int index = getOpControlCount();
        SpotOperation op = engine.insertSpotOperation(index);
        OpControl control = new SpotControl(op, this);
        addControl(control, index);
        return control;
    }

    // The WhitePointOperation is no longer directly accessible in the UI.
    // This method exists for backwards compatibility with LZN files that
    // refer to WhitePointControl.
    public OpControl addWhitePointControl() {
        int index = getOpControlCount();
        WhitePointOperation op = engine.insertWhitePointOperation(index);
        OpControl control = new WhitePointControl(op, this);
        addControl(control, index);
        return control;
    }

    // The LensCorrections should be placed above the RAW Correction tools.
    public OpControl addLensCorrectionsControl() {
        int index = 0;
        for (final OpControl op : opControls) {
            if (!op.isRawCorrection()) {
                break;
            }
            index++;
        }
        LensCorrectionsOperation op = engine.insertLensCorrectionsOperation(index);
        OpControl control = new LensCorrectionsControl(op, this);
        addControl(control, index);
        return control;
    }

    public OpControl addGenericControl(OperationType type) {
        int index = getOpControlCount();
        return addGenericControl(type, index);
    }

    public OpControl addGenericControl(OperationType type, int index) {
        val op = (GenericOperation) engine.insertOperation(type, index);
        val visitor = new VisitorImpl(this);
        op.accept(visitor);
        val control = visitor.getOpControl();
        addControl(control, index);
        return control;
    }

    @RequiredArgsConstructor
    private static class VisitorImpl implements GenericOperationVisitor {
        private final OpStack stack;

        @Getter
        private OpControl opControl;

        @Override
        public void visitColorPickerDropperOperation(ColorPickerDropperOperation op) {
            opControl = new ColorPickerDropperControl(op, stack);
        }

        @Override
        public void visitRawAdjustmentOperation(RawAdjustmentOperation op) {
            opControl = new RawAdjustmentControl(op, stack);
        }

        @Override
        public void visitColorDropperOperation(ColorDropperOperation op) {
            opControl = new ColorDropperControl(op, stack);
        }

        @Override
        public void visitColorPickerOperation(ColorPickerOperation op) {
            opControl = new ColorPickerControl(op, stack);
        }

        @Override
        public void visitGenericOperation(GenericOperation op) {
            opControl = new GenericControl(op, stack);
        }
    }

    public void addControl(SelectableControl control) {
        extraControls.add(control);
        int top = getOpControlCount();
        push(control, top);
        setSelection(control);
        revalidate();
    }

    // OpControls use this to populate the layer choices in LayerControls:
    List<LayerMode> getLayerModes() {
        return engine.getLayerModes();
    }

    // OpControls want to initialize OpTitles with unique names:
    String getNextUniqueName(String baseName) {
        Integer countI = nameCounts.get(baseName);
        if (countI == null) {
            nameCounts.put(baseName, 1);
        }
        int count = nameCounts.get(baseName);
        String uniqueName = baseName + " " + count++;
        nameCounts.put(baseName, count);
        return uniqueName;
    }

    private void addControl(final OpControl control, final int index) {
        if ((control.isRawCorrection() || control.isSingleton()) &&
            getMatchingControl(control, new LinkedList<OpControl>()) != null) {
            return;
        }

        opControls.add(index, control);

        push(control, index);

        notifyControlAdded(control);

        setSelection(control);

        control.addUndoableEditListener(this);

        if (! undoSupport.isRestoring()) {
            UndoableEdit edit = new AbstractUndoableEdit() {
                public String getPresentationName() {
                    String name = control.getTitleText();
                    return LOCALE.get("AddToolEditName", name);
                }
                public void undo() {
                    super.undo();
                    undoSupport.restoreStart();
                    removeControl(control);
                    undoSupport.restoreEnd();
                }
                public void redo() {
                    super.redo();
                    restoreControl(control, index);
                }
            };
            undoSupport.postEdit(edit);
        }
        if (getAutoExpand()) {
            collapseAll();
            control.title.doExpand();
        }
        revalidate();
    }

    /**
     * This does nothing if the current selection is not an OpControl.
     */
    public void removeControl() {
        if (selection instanceof OpControl) {
            removeControl((OpControl) selection);
        }
    }

    public void removeControl(OpControl control) {
        int index = getOpIndexOf(control);
        removeOpControl(index);
    }

    public void removeControl(SelectableControl control) {
        if (selection == control) {
            setSelectionNext(control);
        }
        extraControls.remove(control);
        pop(control);
        resetFocus();
        revalidate();
        repaint();
    }

    private void removeOpControl(final int index) {
        final OpControl opControl = opControls.get(index);

        if (selection == opControl) {
            setSelectionNext(opControl);
        }
        opControls.remove(opControl);
        pop(opControl);

        notifyControlRemoved(opControl);

        engine.removeOperation(index);

        opControl.removeUndoableEditListener(this);

        if (! undoSupport.isRestoring()) {
            UndoableEdit edit = new AbstractUndoableEdit() {
                public String getPresentationName() {
                    String name = opControl.getTitleText();
                    return LOCALE.get("RemoveToolEditName", name);
                }
                public void undo() {
                    super.undo();
                    restoreControl(opControl, index);
                }
                public void redo() {
                    super.redo();
                    undoSupport.restoreStart();
                    removeOpControl(index);
                    undoSupport.restoreEnd();
                }
            };
            undoSupport.postEdit(edit);
        }
        resetFocus();
        revalidate();
        repaint();
    }

    // Take an OpControl that has been removed and reinitialize it:
    private void restoreControl(OpControl control, int index) {
        undoSupport.restoreStart();
        Operation op = control.getOperation();
        OperationType type = op.getType();
        if (control instanceof ZoneControl) {
            op = engine.insertZoneOperation(index);
        }
        else if (control instanceof CloneControl) {
            op = engine.insertCloneOperation(index);
        }
        else if (control instanceof SpotControl) {
            op = engine.insertSpotOperation(index);
        }
        else if (control instanceof WhitePointControl) {
            // WhitePointOperation is deprecated.
            op = engine.insertWhitePointOperation(index);
        }
        else if (control instanceof LensCorrectionsControl) {
            op = engine.insertLensCorrectionsOperation(index);
        }
        else {
            op = engine.insertOperation(type, index);
        }
        control.setOperation(op);
        addControl(control, index);
        undoSupport.restoreEnd();
    }

    // If an OpControl is the focus owner at the time it is removed, then the
    // focus must be reset to a visible ancestor.
    private void resetFocus() {
        KeyboardFocusManager focus =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Component owner = focus.getFocusOwner();
        Container ancestor =
            SwingUtilities.getAncestorOfClass(OpStack.class, owner);
        if (ancestor == null) {
            Container frame =
                SwingUtilities.getAncestorOfClass(JFrame.class, this);
            if (frame != null) {
                frame.requestFocusInWindow();
            }
        }
    }

    // Don't call this method directly; call SelectableTitle.doExpand()
    // instead, so it can keep its triangle collapse/expand widget in sync.
    void expand(final SelectableControl control) {
        if (control.isContentShown()) {
            return;
        }
        control.setShowContent(true);
        if (! undoSupport.isRestoring()) {
            UndoableEdit edit = new AbstractUndoableEdit() {
                public String getPresentationName() {
                    return LOCALE.get("ExpandToolEditName");
                }
                public boolean isSignificant() {
                    return false;
                }
                public void undo() {
                    super.undo();
                    undoSupport.restoreStart();
                    control.title.doCollapse();
                    undoSupport.restoreEnd();
                }
                public void redo() {
                    super.redo();
                    undoSupport.restoreStart();
                    control.title.doExpand();
                    undoSupport.restoreEnd();
                }
            };
            undoSupport.postEdit(edit);
        }
    }

    // Don't call this method directly; call SelectableTitle.doCollapse()
    // instead, so it can keep its triangle collapse/expand widget in sync.
    void collapse(final SelectableControl control) {
        if (! control.isContentShown()) {
            return;
        }
        control.setShowContent(false);
        if (! undoSupport.isRestoring()) {
            UndoableEdit edit = new AbstractUndoableEdit() {
                public String getPresentationName() {
                    return LOCALE.get("CollapseToolEditName");
                }
                public boolean isSignificant() {
                    return false;
                }
                public void undo() {
                    super.undo();
                    undoSupport.restoreStart();
                    control.title.doExpand();
                    undoSupport.restoreEnd();
                }
                public void redo() {
                    super.redo();
                    undoSupport.restoreStart();
                    control.title.doCollapse();
                    undoSupport.restoreEnd();
                }
            };
            undoSupport.postEdit(edit);
        }
    }

    void expandAll() {
        Set<SelectableControl> controls =
            new HashSet<SelectableControl>(opControls);
        controls.addAll(extraControls);
        for (SelectableControl control : controls) {
            control.title.doExpand();
        }
    }

    void collapseAll() {
        Set<SelectableControl> controls =
            new HashSet<SelectableControl>(opControls);
        controls.addAll(extraControls);
        for (SelectableControl control : controls) {
            control.title.doCollapse();
        }
    }

    // This property is set and accessed in SelectableTitle mouse listeners.
    void setAutoExpand(boolean autoExpand) {
        autoExpandControls = autoExpand;
        Prefs.putBoolean(AutoExpandKey, autoExpand);
    }

    boolean getAutoExpand() {
        return autoExpandControls;
    }

    public void setEngineActive(boolean active) {
        engine.setActive(active);
    }

    protected void paintChildren(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        RenderingHints hints = g.getRenderingHints();
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        super.paintChildren(g);
        g.setRenderingHints(hints);
    }

    public boolean canSwapDown(OpControl control) {
        OpControl first = opControls.getFirst();
        return (! control.equals(first));
    }

    public boolean canSwapUp(OpControl control) {
        OpControl last = opControls.getLast();
        return (! control.equals(last));
    }

    public void swapDown(OpControl control) {
        if (! canSwapDown(control)) {
            return;
        }
        int index = getOpIndexOf(control);
        swap(index - 1);
    }

    public void swapUp(OpControl control) {
        if (! canSwapUp(control)) {
            return;
        }
        int index = getOpIndexOf(control);
        swap(index);
    }

    private void swap(final int index) {
        OpControl controlA = opControls.get(index);
        OpControl controlB = opControls.get(index + 1);

        opControls.remove(controlA);
        opControls.remove(controlB);

        opControls.add(index, controlB);
        opControls.add(index + 1, controlA);

        engine.swap(index);

        if (! isStackDragging) {
            // If this swap() call did not originate with a DraggableStack
            // gesture, then sync the DraggableStack layout manually:
            JComponent upper = pop(index);
            push(upper, index + 1);
        }
        if (! undoSupport.isRestoring()) {
            UndoableEdit edit = new AbstractUndoableEdit() {
                public String getPresentationName() {
                    return LOCALE.get("SwapToolEditName");
                }
                public void undo() {
                    super.undo();
                    undoSupport.restoreStart();
                    swap(index);
                    undoSupport.restoreEnd();
                }
                public void redo() {
                    super.redo();
                    undoSupport.restoreStart();
                    swap(index);
                    undoSupport.restoreEnd();
                }
            };
            undoSupport.postEdit(edit);
        }
        revalidate();
    }

    public List<OpControl> getOpControls() {
        return new LinkedList<OpControl>(opControls);
    }

    private int getOpControlCount() {
        return opControls.size();
    }

    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (size.height == 0) {
            // Zero preferred height of a Scrollable on OSX makes screwy
            // JScrollBar behavior:
            size.height = 1;
        }
        return new Dimension(PreferredWidth, size.height);
    }

    public Dimension getMaximumSize() {
        return new Dimension(PreferredWidth, Integer.MAX_VALUE);
    }

    public void addOpStackListener(OpStackListener listener) {
        listeners.add(listener);
    }

    public void removeOpStackListener(OpStackListener listener) {
        listeners.remove(listener);
    }

    private void setSelectionNext(SelectableControl control) {
        SelectableControl nextCtrl = null;
        if (control instanceof OpControl) {
            int index = opControls.indexOf(control);
            if (index > 0) {
                nextCtrl = opControls.get(index - 1);
            }
            else if (index < opControls.size() - 1) {
                nextCtrl = opControls.get(index + 1);
            }
            else if (opControls.size() == 1) {
                if (extraControls.size() > 0) {
                    nextCtrl = extraControls.get(0);
                }
            }
        }
        else {
            int index = extraControls.indexOf(control);
            if (index > 0) {
                nextCtrl = extraControls.get(index - 1);
            }
            else if (index < extraControls.size() - 1) {
                nextCtrl = extraControls.get(index + 1);
            }
            else  if (opControls.size() > 0) {
                nextCtrl = opControls.get(opControls.size() - 1);
            }
        }
        setSelection(nextCtrl);
    }

    private void setSelection(SelectableControl control) {
        if (selection == control) {
            return;
        }
        final SelectableControl oldSelection = selection;

        if (selection != null) {
            selection.setSelected(false);
            int index = opControls.indexOf(selection);
            if (index >= 0)
                engine.setSelectedOperation(index, false);
        }
        selection = control;
        if (selection != null) {
            selection.setSelected(true);
            int index = opControls.indexOf(control);
            if (index >= 0)
                engine.setSelectedOperation(index, true);
        }
        final SelectableControl newSelection = selection;

        // Push an insignificant selection-change edit onto the stack.
        // (Undoing the operations without undoing the regions is confusing.)
        if (! undoSupport.isRestoring()) {
            undoSupport.postEdit(
                new AbstractUndoableEdit() {
                    public String getPresentationName() {
                        return LOCALE.get("ToolSelectionEditName");
                    }
                    public boolean isSignificant() {
                        return false;
                    }
                    public void undo() {
                        super.undo();
                        undoSupport.restoreStart();
                        setSelection(oldSelection);
                        undoSupport.restoreEnd();
                    }
                    public void redo() {
                        super.redo();
                        undoSupport.restoreStart();
                        setSelection(newSelection);
                        undoSupport.restoreEnd();
                    }
                }
            );
        }
        notifySelectionChanged();
    }

    private void notifyControlAdded(OpControl control) {
        for (OpStackListener listener : listeners) {
            listener.opAdded(control);
        }
    }

    private void notifySelectionChanged() {
        for (OpStackListener listener : listeners) {
            if (selection instanceof OpControl) {
                listener.opChanged((OpControl) selection);
            }
            else {
                listener.opChanged(selection);
            }
        }
    }

    // Called from OpControl.setLocked()
    void notifyLockChanged(OpControl control) {
        for (OpStackListener listener : listeners) {
            listener.opLockChanged(control);
        }
    }

    private void notifyControlRemoved(OpControl control) {
        for (OpStackListener listener : listeners) {
            listener.opRemoved(control);
        }
    }

    private int getOpIndexOf(OpControl control) {
        int index = 0;
        for (OpControl c : opControls) {
            if (control.equals(c)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    // String constants used for serialization:
    private final static String ZoneTag = "ZoneOperation";
    private final static String CloneTag = "CloneOperation";
    private final static String SpotTag = "SpotOperation";
    private final static String WhitePointTag = "WhitePointOperation";
    private final static String LensCorrectionsTag = "LensCorrectionsOperation";
    private final static String GenericTag = "GenericOperation";
    private final static String OpTypeTag = "OperationType";

    /**
     * Preserve state under the given XmlNode.
     */
    public void save(XmlNode node) {
        for (OpControl control : opControls) {
            XmlNode child;
            if (control instanceof ZoneControl) {
                child = node.addChild(ZoneTag);
            }
            else if (control instanceof CloneControl) {
                child = node.addChild(CloneTag);
            }
            else if (control instanceof SpotControl) {
                child = node.addChild(SpotTag);
            }
            else if (control instanceof WhitePointControl) {
                // WhitePointOperation is deprecated.
                child = node.addChild(WhitePointTag);
            }
            else if (control instanceof LensCorrectionsControl) {
                child = node.addChild(LensCorrectionsTag);
            }
            else {
                child = node.addChild(GenericTag);
                GenericOperation op = (GenericOperation) control.getOperation();
                OperationType opType = op.getType();
                child.setAttribute(OpTypeTag, opType.getName());
            }
            control.save(child);
        }
    }

    /**
     * Restore the state preserved in the given XmlNode by removing all
     * controls and then calling addControls().  Undo support is suppressed
     * for this operation, because it is typically invoked from undo.
     */
    public void restore(XmlNode node) throws XMLException {
        // suppress undo support during restore:
        undoSupport.restoreStart();

        int count = getOpControlCount();
        for (int n=count-1; n>=0; n--) {
            removeOpControl(n);
        }
        addControls(node);

        undoSupport.restoreEnd();
    }

    /**
     * Add a set of controls whose state is saved in the given XmlNode.
     */
    public List<OpControl> addControls(XmlNode node) throws XMLException {
        List<OpControl> controls = new LinkedList<OpControl>();
        XmlNode[] children = node.getChildren();
        for (XmlNode child : children) {
            OpControl control = null;
            String tag = child.getName();
            if (tag.equals(ZoneTag)) {
                control = addZoneControl();
            }
            else if (tag.equals(CloneTag)) {
                control = addCloneControl();
            }
            else if (tag.equals(SpotTag)) {
                control = addSpotControl();
            }
            else if (tag.equals(WhitePointTag)) {
                control = addWhitePointControl();
            }
            else if (tag.equals(LensCorrectionsTag)) {
                control = addLensCorrectionsControl();
            }
            else if (tag.equals(GenericTag)) {
                String typeTag = child.getAttribute(OpTypeTag);
                Collection<OperationType> types =
                    engine.getGenericOperationTypes();
                for (OperationType type : types) {
                    if (type.getName().equals(typeTag)) {
                        control = addGenericControl(type);
                        break;
                    }
                }
                if (control == null) {
                    throw new XMLException(
                        "Unrecognized GenericOperationType \"" + typeTag + "\""
                    );
                }
            }
            if (control != null) {
                control.restore(child);
                control.readyForUndo();
                controls.add(control);
            }
        }
        // Some controls can only replace a matching control.
        // Find all matches and remove them.
        for (OpControl control : controls) {
            if (control.isRawCorrection() || control.isSingleton()) {
                OpControl matchControl = getMatchingControl(control, controls);
                if (matchControl != null) {
                    int targetIndex = opControls.indexOf(matchControl);
                    removeControl(matchControl);
                    int index = opControls.indexOf(control);
                    while ((index > targetIndex) && (canSwapDown(control))) {
                        swapDown(control);
                        index = opControls.indexOf(control);
                    }
                }
            }
        }
        return controls;
    }

    // Some controls can occur only once in the stack, like default RAW
    // correction tools.  This method examines all tools in the stack to see
    // if there is a control present that could be replaced by the given
    // control, excluding the given List of OpControls which are the default
    // RAW correction tools being added.
    private OpControl getMatchingControl(
        OpControl control, List<OpControl> newControls
    ) {
        OperationType type = control.getOperation().getType();
        if (control.isRawCorrection() || control.isSingleton()) {
            for (OpControl match : opControls) {
                if (! newControls.contains(match)) {
                    if (match.isRawCorrection() || match.isSingleton()) {
                        OperationType matchType =
                            match.getOperation().getType();
                        if (matchType.equals(type)) {
                            return match;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Report whether the stack already includes a RAW Adjustments tool.  Older
     * saved Documents don't include one, so in these cases one must be added
     * manually.
     */
    public boolean hasRawAdjustments() {
        OperationType rawType = engine.getGenericRawAdjustmentsOperationType();
        for (OpControl control : opControls) {
            Operation op = control.getOperation();
            OperationType type = op.getType();
            if (type.getName().startsWith(rawType.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempt to undo the effect of a prior call to addControls().  The
     * List of OpControls given here should match a list returned from
     * addControls().
     * <p>
     * If they don't match, or if the current set of OpControls does not
     * include all the OpControls in the list, then the behavior of this
     * method is indeterminate.  It won't crash though.
     */
    public void removeControls(List<OpControl> controls) {
        for (OpControl control : controls) {
            if (opControls.contains(control)) {
                removeControl(control);
            }
        }
    }

    public void addUndoableEditListener(UndoableEditListener listener) {
        undoSupport.addUndoableEditListener(listener);
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
        undoSupport.removeUndoableEditListener(listener);
    }

    // We listen for UndoableEditEvents on OpControls and pass them along:
    public void undoableEditHappened(UndoableEditEvent event) {
        undoSupport.postEdit(event.getEdit());
    }

    // Begin Scrollable implementation:

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height;
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height / 100;
    }

    // End Scrollable implementation.

    // Extend UndoableEditSupport to be aware when we are doing restore (or
    // undo or redo), so we can know not to create UndoableEdits or broadcast
    // change batches during that time.
    private class OpStackUndoSupport extends UndoableEditSupport {

        private boolean restoring;

        public void postEdit(UndoableEdit edit) {
            if (! restoring) {
                super.postEdit(edit);
            }
        }

        private void restoreStart() {
            restoring = true;
        }

        private void restoreEnd() {
            restoring = false;
        }

        private boolean isRestoring() {
            return restoring;
        }
    }
}
