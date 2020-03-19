/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import static com.lightcrafts.ui.crop.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

final class ResetButton extends CoolButton {

    ResetButton( ResetAction action, boolean isRotateOnly ) {
        setText(LOCALE.get("ResetButton"));

        addActionListener(action);

        setToolTipText(
            LOCALE.get(
                isRotateOnly ? "ResetRotateToolTip" : "ResetCropToolTip"
            )
        );

        // Shave off some of the width padded around the button text by the L&F.
        Dimension size = getPreferredSize();
        size.width -= 16;
        setMinimumSize(size);
        setPreferredSize(size);
        setMaximumSize(size);

        setEnabled(action.isEnabled());

        action.addPropertyChangeListener(
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("enabled")) {
                        setEnabled(evt.getNewValue().equals(Boolean.TRUE));
                    }
                }
            }
        );
    }
}
