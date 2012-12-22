/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.advice;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.action.ToggleAction;
import com.lightcrafts.ui.advice.Advice;
import com.lightcrafts.ui.advice.AdvisorDialog;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.editor.RegionManager;
import com.lightcrafts.ui.region.RegionListener;
import com.lightcrafts.ui.region.SharedShape;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AdviceManager implements RegionListener, PropertyChangeListener {

    private ComboFrame frame;

    private Advice startAdvice;
    private Advice endAdvice;
    private Advice emptyFolderAdvice;

    // ToggleAction property change sources, which trigger advice
    private Action regionsAction;

    private AdvisorDialog dialog;

    // A little state machine, to determine when to show the region advice.
    //
    // Region start advice: ShowHideRegionsButton becomes selected.
    //
    // Region end advice: region batch depth goes monotonically from 0 to 2.
    private int depth = 0;
    private boolean depthDecremented;

    public AdviceManager(ComboFrame frame) {
        this.frame = frame;

        startAdvice = new RegionStartAdvice(frame);
        endAdvice = new RegionEndAdvice(frame);
        emptyFolderAdvice = new EmptyFolderAdvice(frame);

        Document doc = frame.getDocument();
        if (doc != null) {
            RegionManager regions = doc.getRegionManager();
            regions.addRegionListener(this);

            regionsAction = regions.getShowHideAction();
            regionsAction.addPropertyChangeListener(this);
        }
    }

    // Called from ComboFrame.initImages().
    public void showEmptyFolderAdvice() {
        nextAdvice(emptyFolderAdvice);
    }

    // Called from ComboFrame.initImages().
    public void hideEmptyFolderAdvice() {
        if (dialog != null) {
            if (dialog.getAdvice() == emptyFolderAdvice) {
                nextAdvice(null);
            }
        }
    }

    // Cleanup is important here, otherwise the complete Document structure
    // will remain in memory for the life of the frame.
    // See ComboFrame.dispose().
    public void dispose() {
        Document doc = frame.getDocument();
        if (doc != null) {
            RegionManager regions = doc.getRegionManager();
            regions.removeRegionListener(this);
            regionsAction.removePropertyChangeListener(this);
        }
    }

    // Listen for the selected state change on the show/hide buttons,
    // and show the relevant advice.
    public void propertyChange(PropertyChangeEvent event) {
        String propName = event.getPropertyName();
        if (propName.equals(ToggleAction.TOGGLE_STATE)) {
            boolean selected = (Boolean) event.getNewValue();
            Object source = event.getSource();
            if (source == regionsAction) {
                if (selected) {
                    nextAdvice(startAdvice);
                }
                else if (dialog != null) {
                    if (dialog.getAdvice() == startAdvice) {
                        nextAdvice(null);
                    }
                    else if (dialog.getAdvice() == endAdvice) {
                        nextAdvice(null);
                    }
                }
            }
        }
    }

    // When a region starts, show the "end" advice.
    public void regionBatchStart(Object cookie) {
        if ((depth == 2) && ! depthDecremented) {
            nextAdvice(endAdvice);
        }
    }

    public void regionChanged(Object cookie, SharedShape shape) {
    }

    // As soon as the follow-mouse region mode ends, hide the "end" advice.
    public void regionBatchEnd(Object cookie) {
        if (! depthDecremented) {
            if (dialog != null) {
                dialog.setVisible(false);
            }
        }
        // Set depthDecremented, but reset it when depth gets back down to zero.
        depthDecremented = (depth > 0);
    }

    private void nextAdvice(Advice advice) {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog = null;
        }
        if (advice != null) {
            dialog = new AdvisorDialog(advice);
            dialog.advise();
        }
    }
}
