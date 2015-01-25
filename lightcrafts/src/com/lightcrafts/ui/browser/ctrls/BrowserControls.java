/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2015 Masahiro Kitagawa */

package com.lightcrafts.ui.browser.ctrls;

import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;
import com.lightcrafts.ui.browser.model.ImageDatumComparator;
import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.ImageBrowserEvent;
import com.lightcrafts.ui.browser.view.ImageBrowserListener;
import com.lightcrafts.ui.browser.view.ImageBrowserScrollPane;
import com.lightcrafts.ui.layout.ToggleTitleBorder;
import com.lightcrafts.ui.toolkit.BoxedButton;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.CollapseButton;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;

/**
 * A container for the various browser control widgets, and also a place where
 * their interactions are defined.
 * <p>
 * Such as:
 * <ul>
 * <li>The browser scroll pane layout centering behavior is disabled except
 * during interactive thumbnail size changes.</li>
 * <li>A browser error label is updated on browser error events and selection
 * events.</li>
 * <li>Initial values for the sort control and the thumbnail size are
 * enforced.</li>
 * </ul>
 */
public class BrowserControls extends Box {

    private JLabel browserError;

    public BrowserControls(final ImageBrowserScrollPane browserScroll, ComboFrame frame) {
        super(BoxLayout.X_AXIS);

        AbstractImageBrowser browser = browserScroll.getBrowser();

        BoxedButton rotator   = new BoxedButton(LOCALE.get("RotateBorderTitle"),    new RotateButtons(browser));
        BoxedButton rater     = new BoxedButton(LOCALE.get("RateBorderTitle"),      new RatingButton(browser));
        BoxedButton copyPaste = new BoxedButton(LOCALE.get("CopyToolsBorderTitle"), new CopyPasteButtons(browser));

        SortCtrl sortCtrl = new SortCtrl(browser);
        BoxedButton sort      = new BoxedButton(LOCALE.get("SortBorderTitle"),      sortCtrl);

        SizeSlider sizeSlider = new SizeSlider(browser);
        ToggleTitleBorder.setBorder(sizeSlider, LOCALE.get("SizeBorderTitle"));
        // Use the centering layout only when the SizeSlider is working,
        // so other size changes (like when files come and go) don't result
        // in scroll jumps:
        sizeSlider.addMouseListener(
            new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    browserScroll.setCenteringLayout(true);
                }
                public void mouseReleased(MouseEvent e) {
                    browserScroll.setCenteringLayout(false);
                }
            }
        );
        //BoxedButton size      = new BoxedButton(LOCALE.get("SizeBorderTitle"),      sizeSlider);

        BoxedButton collapse  = new BoxedButton(LOCALE.get("CollapseBorderTitle"),  new CollapseButton(frame));
        BoxedButton latest    = new BoxedButton(LOCALE.get("SelectBorderTitle"),    new SelectLatestButton(browser));
        BoxedButton trash     = new BoxedButton(LOCALE.get("TrashBorderTitle"),     new TrashButton(browser));

        browserScroll.setCenteringLayout(false);

        browserError = new JLabel();

        browser.addBrowserListener(
            new ImageBrowserListener() {
                public void selectionChanged(ImageBrowserEvent event) {
                    // Clear any lingering error messages from a prior events
                    browserError.setText("");
                }
                public void imageDoubleClicked(ImageBrowserEvent event) {
                }
                public void browserError(String message) {
                    browserError.setText(message);
                }
            }
        );

        final int space = 8;
        // add(Box.createHorizontalGlue());
        add(Box.createHorizontalStrut(space));
        add(rotator.box);
        add(Box.createHorizontalStrut(space));
        add(rater.box);
        add(Box.createHorizontalStrut(space));

        add(trash.box);
        add(Box.createHorizontalStrut(space));

        add(new Separator());
        add(Box.createHorizontalStrut(space));

        add(copyPaste.box);
        add(Box.createHorizontalStrut(space));

        add(Box.createHorizontalGlue());

        add(browserError);
        add(Box.createHorizontalStrut(space));

        add(collapse.box);
        add(Box.createHorizontalStrut(space));
        add(latest.box);
        add(Box.createHorizontalStrut(space));

        add(new Separator());
        add(Box.createHorizontalStrut(space));

        add(sizeSlider);
        add(Box.createHorizontalStrut(space));
        add(sort.box);
        add(Box.createHorizontalStrut(space));
        // add(Box.createHorizontalGlue());

        // Add space above and below, to tune the layout:
        Border border = BorderFactory.createEmptyBorder(0, 0, 3, 0);
        setBorder(border);

        ImageDatumComparator comp = sortCtrl.getSort();
        boolean inverted = sortCtrl.getSortInverted();
        int sizeValue = sizeSlider.getValue();

        browser.setSort(comp);
        browser.setSortInverted(inverted);
        browser.setCharacteristicSize(sizeValue);
    }

    class Separator extends JSeparator {
        Separator() {
            super(SwingConstants.VERTICAL);
            setMaximumSize(new Dimension(3, 32));
        }
    }

/*
        // add(Box.createHorizontalStrut(space));
        // Box s = Box.createHorizontalBox();
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        // s.add(separator);
        // separator.setLayout(new BoxLayout(separator, BoxLayout.X_AXIS));
        add(separator);
        // add(Box.createHorizontalStrut(space));
        add(Box.createHorizontalGlue());

 */
    public void dispose() {
        ToggleTitleBorder.unsetAllBorders(this);
    }
}
