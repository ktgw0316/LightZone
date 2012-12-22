/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.action;

import javax.swing.*;
import java.awt.event.ActionEvent;

public abstract class ToggleAction extends AbstractAction {

    // Property keys
    public final static String TOGGLE_STATE = "ToggleState";
    public final static String PRESSED_ICON = "PressedIcon";

    private String onName;
    private Icon onIcon;
    private String onDescription;

    private String offName;
    private Icon offIcon;
    private String offDescription;

    private boolean state;

    public ToggleAction() {
        putValue(TOGGLE_STATE, Boolean.FALSE);
    }

    public ToggleAction(String onName, String offName) {
        super(offName);
        this.onName = onName;
        this.offName = offName;
        putValue(TOGGLE_STATE, Boolean.FALSE);
    }

    public ToggleAction(
        String onName, Icon onIcon, String offName, Icon offIcon
    ) {
        super(offName, offIcon);
        this.onName = onName;
        this.onIcon = onIcon;
        this.offName = offName;
        this.offIcon = offIcon;
        putValue(TOGGLE_STATE, Boolean.FALSE);
    }

    protected abstract void onActionPerformed(ActionEvent event);

    protected abstract void offActionPerformed(ActionEvent event);

    public void setState(boolean state) {
        if (this.state != state) {
            actionPerformed(null);
        }
    }

    public boolean getState() {
        return state;
    }

    public void setName(String name, boolean state) {
        if (state) {
            onName = name;
        }
        else {
            offName = name;
        }
        actionPerformed(null);
    }

    public String getName(boolean state) {
        return state ? onName : offName;
    }

    public void setIcon(Icon icon, boolean state) {
        if (state) {
            onIcon = icon;
        }
        else {
            offIcon = icon;
        }
        actionPerformed(null);
    }

    public void setPressedIcon(Icon icon) {
        putValue(PRESSED_ICON, icon);
    }

    public void setDescription(String description, boolean state) {
        if (state) {
            onDescription = description;
        }
        else {
            offDescription = description;
        }
        actionPerformed(null);
    }
    
    public void actionPerformed(ActionEvent event) {
        state = ! state;

        String name = state ? onName : offName;
        putValue(NAME, name);

        Icon icon = state ? onIcon : offIcon;
        putValue(SMALL_ICON, icon);

        String description = state ? onDescription : offDescription;
        putValue(SHORT_DESCRIPTION, description);

        // If it's a real event (not a programmatic call to setState()),
        // then invoke the ActionListeners:

        if (event != null) {
            if (state) {
                onActionPerformed(event);
            }
            else {
                offActionPerformed(event);
            }
        }
        putValue(TOGGLE_STATE, Boolean.valueOf(state));
    }
}
