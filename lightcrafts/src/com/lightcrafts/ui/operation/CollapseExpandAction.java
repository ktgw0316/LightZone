/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.ui.action.ToggleAction;
import static com.lightcrafts.ui.operation.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;

class CollapseExpandAction extends ToggleAction {

    private static Icon CollapsedIcon = IconFactory.createInvertedIcon(
        CollapseExpandAction.class, "collapsed.png"
    );
    private static Icon ExpandedIcon = IconFactory.createInvertedIcon(
        CollapseExpandAction.class, "expanded.png"
    );
    private static String ExpandToolTip = LOCALE.get("ExpandToolTip");
    private static String CollapseToolTip = LOCALE.get("CollapseToolTip");

    private SelectableTitle title;

    CollapseExpandAction(SelectableTitle title) {
        this.title = title;
        setIcon(ExpandedIcon, false);
        setIcon(CollapsedIcon, true);
        setDescription(CollapseToolTip, false);
        setDescription(ExpandToolTip, true);
        setState(false);
    }

    boolean isCollapsed() {
        return getState();
    }

    protected void onActionPerformed(ActionEvent event) {
        title.doCollapse();
    }

    protected void offActionPerformed(ActionEvent event) {
        title.doExpand();
    }
}
