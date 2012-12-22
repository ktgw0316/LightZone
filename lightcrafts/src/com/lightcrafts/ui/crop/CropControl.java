/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.model.CropBounds;

import javax.swing.*;

/**
 * A control for CropMode that presents four editable text fields for the
 * top, bottom, left, and right pixel coordinates of the crop bounds.
 * <p>
 * These bounds are presented as integers in four spinners, even though
 * the underlying model in CropOverlay uses floating point.
 */

public final class CropControl extends Box {

    private ConstraintModel constraints;
    private ResetButton resetButton;
    private LockButton lockButton;
    private ResetAction reset;

    CropControl(ResetAction reset, boolean isRotateOnly) {
        super(BoxLayout.X_AXIS);

        this.reset = reset;

        final ConstraintMenu aspectMenu;
        final TransposeButton transposeButton;
        final NumberTextField numField;
        final NumberTextField denField;

        // Data model for aspect ratio constraints:
        constraints = new ConstraintModel();

        // Context menu of recent aspect constraints:
        aspectMenu = new ConstraintMenu(constraints);

        // Editable text fields for custom aspect constraints:
        numField = new NumberTextField(constraints.getNumeratorListener());
        denField = new NumberTextField(constraints.getDenominatorListener());

        // Switch numerator and denominator:
        transposeButton = new TransposeButton(constraints);

        // The model needs to update these controls sometimes:
        constraints.setNumeratorTextField(numField);
        constraints.setDenominatorTextField(denField);
        constraints.setTransposeButton(transposeButton);
        constraints.setConstraintMenu(aspectMenu);

        // Activate and deactivate aspect constraint enforcement:
        lockButton = new LockButton(constraints);

        // The reset button, which clears the crop bounds on the overlay:
        resetButton = new ResetButton( reset, isRotateOnly );

        add(denField);
        add(Box.createHorizontalStrut(2));
        add(transposeButton);
        add(Box.createHorizontalStrut(2));
        add(numField);
        add(Box.createHorizontalStrut(2));
        add(lockButton);
        add(Box.createHorizontalStrut(2));
        add(aspectMenu);
        add(Box.createHorizontalStrut(2));
        add(resetButton);
    }

    // Expose the reset button, so it can be shown as the sole control
    // for a rotate-only CropMode.
    public JComponent getResetButton() {
        return resetButton;
    }

    void setResetValue(CropBounds resetValue) {
        reset.setResetValue(resetValue);
    }

    void unlock() {
        if (lockButton.isSelected()) {
            lockButton.doClick();
        }
    }

    CropBounds getResetValue() {
        return reset.getResetValue();
    }

    void setOverlay(CropOverlay overlay) {
        constraints.setOverlay(overlay);
        reset.setOverlay(overlay);
        AspectConstraint constraint = constraints.getAspectConstraint();
        overlay.setAspectConstraint(constraint);
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());

        ResetAction reset = new ResetAction();

        CropControl ctrl = new CropControl(reset, false);

        JFrame frame = new JFrame("Crop Test");
        frame.getContentPane().add(ctrl);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
