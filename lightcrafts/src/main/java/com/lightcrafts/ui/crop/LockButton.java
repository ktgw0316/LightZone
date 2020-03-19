/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import static com.lightcrafts.ui.crop.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolToggleButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

final class LockButton extends CoolToggleButton implements ActionListener {

    private static Icon LockedIcon = IconFactory.createInvertedIcon(
        LockButton.class, "lock.png"
    );
    private static Icon UnlockedIcon = IconFactory.createInvertedIcon(
        LockButton.class, "unlocked.png"
    );
    // Comment out the sticky behavior of the LockButton.
    // CropMode now enforces an unlocked state every time crop mode is
    // entered.
//    private static final Preferences Prefs = Preferences.userNodeForPackage(
//        ConstraintModel.class
//    );
//    private static final String CropLockKey = "CropLock";

    private ConstraintModel constraints;

    LockButton(ConstraintModel constraints) {
        this.constraints = constraints;

        addActionListener(this);

//        final boolean isLocked = Prefs.getBoolean(CropLockKey, false);

        setFocusable(false);

//        setSelected(isLocked);
        actionPerformed(null);
    }

    public void actionPerformed(ActionEvent e) {
        if (isSelected()) {
            setIcon(LockedIcon);
            setToolTipText(LOCALE.get("LockedToolTip"));
            constraints.lock();
//            Prefs.putBoolean(CropLockKey, true);
        }
        else {
            setIcon(UnlockedIcon);
            setToolTipText(LOCALE.get("UnlockedToolTip"));
            constraints.unlock();
//            Prefs.putBoolean(CropLockKey, false);
        }
    }
}
