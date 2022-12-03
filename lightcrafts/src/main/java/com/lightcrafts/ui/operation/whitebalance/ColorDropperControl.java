/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.whitebalance;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.util.Map;

import com.lightcrafts.model.ColorDropperOperation;
import com.lightcrafts.model.Operation;
import com.lightcrafts.ui.mode.DropperMode;
import com.lightcrafts.ui.operation.OpStack;
import com.lightcrafts.ui.operation.generic.GenericControl;
import com.lightcrafts.ui.toolkit.DropperButton;

import static com.lightcrafts.ui.operation.whitebalance.Locale.LOCALE;
import com.lightcrafts.ui.editor.EditorMode;

/**
 * An OpControl to hold a ColorDropperOperation in an OpStack.  Provides a
 * dropper mode to sample a point from an image and update sliders
 * accordingly.
 */
public class ColorDropperControl extends GenericControl {

    private DropperButton dropperButton;
    private DropperMode dropperMode;

    private Box colorContent;

    // Flag dropper button state changes that just synchronize the button
    // when the dropper Mode is externally cancelled, so OpControlModeListener
    // notifications won't fire:
    private boolean isDropperModeCancelling;

    public ColorDropperControl(ColorDropperOperation op, OpStack stack) {
        super(op, stack);
    }

    protected void operationChanged(Operation operation) {
        super.operationChanged(operation);

        // Make the button that enters and exits the dropper Mode:
        dropperButton = new DropperButton();
        dropperButton.setAlignmentX(1f);
        dropperButton.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    boolean selected =
                        (event.getStateChange() == ItemEvent.SELECTED);
                    if (selected) {
                        getComboFrame().getEditor().setMode( EditorMode.ARROW );
                        notifyListenersEnterMode(dropperMode);
                    }
                    else {
                        if (! isDropperModeCancelling) {
                            notifyListenersExitMode(dropperMode);
                        }
                    }
                }
            }
        );
        // Initialize the dropper Mode, and let it exit itself:
        dropperMode = new DropperMode( this );
        dropperMode.addListener(
            new DropperMode.Listener() {
                public void pointSelected(Point2D p) {
                    setColor(p);
//                    // Trigger Mode-exit via the toggle button:
//                    dropperButton.setSelected(false);
                }
                public void modeCancelled() {
                    // Reset the toggle button, without firing notifications:
                    isDropperModeCancelling = true;
                    dropperButton.setSelected(false);
                    isDropperModeCancelling = false;
                }
            }
        );
        colorContent = Box.createHorizontalBox();
        colorContent.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        colorContent.add(Box.createHorizontalStrut(8));
        colorContent.add(dropperButton);

        JComponent content = getContent();
        content.add(colorContent);
    }

    // The RawAdjustmentControl derives from this, and wants to add a little
    // to the layout.
    Box getColorContent() {
        return colorContent;
    }

    private void setColor(Point2D p) {
        ColorDropperOperation op = (ColorDropperOperation) getOperation();
        Map<String, Float> sliders = op.setColor(p);
        for (String key : sliders.keySet()) {
            double value = sliders.get(key);
            slewSlider(key, value);
        }
        if (! undoSupport.isRestoring()) {
            undoSupport.postEdit(LOCALE.get("PickWhiteBalanceEditName"));
        }
    }
}
