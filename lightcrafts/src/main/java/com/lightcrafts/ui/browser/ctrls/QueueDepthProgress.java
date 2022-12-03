/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.browser.model.ImageList;
import com.lightcrafts.ui.browser.model.ImageTaskQueueListener;

import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;

import javax.swing.*;
import java.awt.*;

public class QueueDepthProgress
    extends JPanel implements ImageTaskQueueListener
{
    private JProgressBar progress;

    public QueueDepthProgress(ImageList list) {
        setLayout(new BorderLayout());
        list.addQueueListener(this);
        setFixedSize();
    }

    public void queueDepthChanged(int depth) {
        if (depth > 0) {
            if (progress == null) {
                addProgressBar();
            }
            int max = progress.getMaximum();
            if (depth > max) {
                progress.setMaximum(depth);
            }
            progress.setValue(max - depth);
        }
        else {
            if (progress != null) {
                removeProgressBar();
            }
        }
    }

    private void addProgressBar() {
        progress = new JProgressBar(0, 10);
        progress.setToolTipText(LOCALE.get("ProgressToolTip"));
        add(progress);
        revalidate();
        repaint();
    }

    private void removeProgressBar() {
        remove(progress);
        progress = null;
        revalidate();
        repaint();
    }

    private void setFixedSize() {
        Dimension dim = (new JProgressBar()).getPreferredSize();
        setPreferredSize(dim);
        setMinimumSize(dim);
        setMaximumSize(dim);
    }
}
