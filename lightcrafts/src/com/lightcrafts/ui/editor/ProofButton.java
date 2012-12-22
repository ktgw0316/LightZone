/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolToggleButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * A toggle button to show and hide a ProofSelectableControl.
 */
class ProofButton extends CoolToggleButton implements ItemListener {

    private static Icon Icon =
        IconFactory.createIcon(ProofButton.class, "proof.png");

    private final static String ShowTip = LOCALE.get("ShowProofToolTip");
    private final static String HideTip = LOCALE.get("HideProofToolTip");

    private final static String ShowName = LOCALE.get("ShowProofActionName");
    private final static String HideName = LOCALE.get("HideProofActionName");

    private EditorControls editor;
    private ProofSelectableControl control;

    private Action action;  // programmatically toggle this button

    ProofButton(EditorControls editor, ProofSelectableControl control) {
        this.editor = editor;
        this.control = control;
        setIcon(Icon);
        setToolTipText(ShowTip);
        initAction();
        addItemListener(this);
    }

    // Disabled, for the no-Document display mode.
    ProofButton() {
        setIcon(Icon);
        setToolTipText(ShowTip);
        setEnabled(false);
    }

    public void itemStateChanged(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            editor.addControl(control);
            action.putValue(Action.NAME, HideName);
            setToolTipText(HideTip);
        }
        else {
            editor.removeControl(control);
            action.putValue(Action.NAME, ShowName);
            setToolTipText(ShowTip);
        }
    }

    public Action getShowHideAction() {
        return action;
    }

    private void initAction() {
        action = new AbstractAction(ShowName) {
            public void actionPerformed(ActionEvent event) {
                setSelected(! isSelected());
            }
        };
    }
}
