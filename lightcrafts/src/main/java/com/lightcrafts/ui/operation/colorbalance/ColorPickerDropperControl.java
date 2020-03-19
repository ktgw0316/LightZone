/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.colorbalance;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.geom.Point2D;
import java.util.Map;

import com.lightcrafts.ui.operation.OpStack;
import com.lightcrafts.ui.toolkit.DropperButton;
import com.lightcrafts.ui.mode.DropperMode;
import com.lightcrafts.ui.help.HelpConstants;
import com.lightcrafts.model.Operation;
import com.lightcrafts.model.ColorPickerDropperOperation;

import static com.lightcrafts.ui.operation.colorbalance.Locale.LOCALE;
import com.lightcrafts.ui.editor.EditorMode;

/**
 * An OpControl to hold a ColorPickerDropperOperation in an OpStack.  Provides
 * a dropper mode to sample a color from an image and a color wheel to pick a
 * color directly.  Keeps sliders consistent with these alternative
 * specifications.
 */
public class ColorPickerDropperControl extends ColorPickerControl {

    private DropperButton dropperButton;
    private DropperMode dropperMode;

    // Flag dropper button state changes that just synchronize the button
    // when the dropper Mode is externally cancelled, so OpControlModeListener
    // notifications won't fire:
    private boolean isDropperModeCancelling;

    public ColorPickerDropperControl(
        ColorPickerDropperOperation op, OpStack stack
    ) {
        super(op, stack);
    }

    protected void operationChanged(Operation operation) {
        super.operationChanged(operation);

        // Make the button that enters and exits the dropper Mode:
        dropperButton = new DropperButton();
        dropperButton.setAlignmentX( 1f );
        dropperButton.addItemListener(
            new ItemListener() {
                public void itemStateChanged( ItemEvent event ) {
                    getComboFrame().getEditor().setMode( EditorMode.ARROW );
                    if ( event.getStateChange() == ItemEvent.SELECTED ) {
                        notifyListenersEnterMode( dropperMode );
                    } else if ( ! isDropperModeCancelling )
                        notifyListenersExitMode( dropperMode );
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
        // colorContent.add(Box.createHorizontalStrut(8));
        colorContent.add(dropperButton);
    }

    private void setColor(Point2D p) {
        ColorPickerDropperOperation op = (ColorPickerDropperOperation) getOperation();
        Map<String, Double> map = op.setColor(p);
        updateColor(map);
        if (! undoSupport.isRestoring()) {
            undoSupport.postEdit(LOCALE.get("DropperEditName"));
        }
    }

    protected String getHelpTopic() {
        return HelpConstants.HELP_TOOL_COLOR_BALANCE;
    }
}
