/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.prefs.Preferences;

class ConstraintModel {

    private static final Preferences Prefs = Preferences.userNodeForPackage(
        ConstraintModel.class
    );
    private static final String CropConstraintKey = "CropConstraint";

    private AspectConstraint constraint;
    private CropOverlay overlay;
    private NumberTextField numeratorField;
    private NumberTextField denominatorField;
    private TransposeButton transposeButton;
    private ConstraintMenu constraintMenu;

    private boolean isLocked;

    ConstraintModel() {
        String text = Prefs.get(
            CropConstraintKey, (new AspectConstraint(4, 6)).toString()
        );
        constraint = AspectConstraint.fromString(text);
        isLocked = true;
    }

    Action createMenuAction(final AspectConstraint constraint) {
        return new AbstractAction(constraint.getNameWithDescription()) {
            public void actionPerformed(ActionEvent event) {
                setAspectConstraint(constraint);
            }
        };
    }

    NumberTextField.Listener getNumeratorListener() {
        return new NumberTextField.Listener() {
            public void numberChanged(int numerator) {
                int denominator = constraint.getDenominator();
                constraint = new AspectConstraint(numerator, denominator);
                if (overlay != null) {
                    overlay.setAspectConstraint(constraint);
                }
                Prefs.put(CropConstraintKey, constraint.toString());
            }
        };
    }

    NumberTextField.Listener getDenominatorListener() {
        return new NumberTextField.Listener() {
            public void numberChanged(int denominator) {
                int numerator = constraint.getNumerator();
                constraint = new AspectConstraint(numerator, denominator);
                if (overlay != null) {
                    overlay.setAspectConstraint(constraint);
                }
                Prefs.put(CropConstraintKey, constraint.toString());
            }
        };
    }

    void setOverlay(CropOverlay overlay) {
        this.overlay = overlay;
        if (isLocked) {
            overlay.setAspectConstraint(constraint);
        }
        else {
            overlay.setAspectConstraint(new AspectConstraint());
        }
    }

    void setNumeratorTextField(NumberTextField field) {
        numeratorField = field;
        numeratorField.setNumber(constraint.getNumerator());
    }

    void setDenominatorTextField(NumberTextField field) {
        denominatorField = field;
        denominatorField.setNumber(constraint.getDenominator());
    }

    void setTransposeButton(TransposeButton button) {
        transposeButton = button;
    }

    void setConstraintMenu(ConstraintMenu menu) {
        constraintMenu = menu;
    }

    AspectConstraint getAspectConstraint() {
        if (isLocked) {
            return constraint;
        }
        else {
            return new AspectConstraint();
        }
    }

    void transpose() {
        setAspectConstraint(constraint.transpose());
    }

    void unlock() {
        isLocked = false;
        if (overlay != null) {
            overlay.setAspectConstraint(new AspectConstraint());
        }
        numeratorField.setEnabled(false);
        denominatorField.setEnabled(false);
        transposeButton.setEnabled(false);
        constraintMenu.setEnabled(false);

        if (numeratorField.isFocusOwner() || denominatorField.isFocusOwner()) {
            KeyboardFocusManager focus =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
            focus.upFocusCycle();
        }
    }

    void lock() {
        isLocked = true;
        if (overlay != null) {
            overlay.setAspectConstraint(constraint);
        }
        numeratorField.setEnabled(true);
        denominatorField.setEnabled(true);
        transposeButton.setEnabled(true);
        constraintMenu.setEnabled(true);
    }

    private void setAspectConstraint(AspectConstraint constraint) {
        this.constraint = constraint;
        if (overlay != null) {
            overlay.setAspectConstraint(constraint);
        }
        syncTextFields();
        Prefs.put(CropConstraintKey, constraint.toString());
    }

    private void syncTextFields() {
        int numerator = constraint.getNumerator();
        numeratorField.setNumber(numerator);
        int denominator = constraint.getDenominator();
        denominatorField.setNumber(denominator);
    }
}
