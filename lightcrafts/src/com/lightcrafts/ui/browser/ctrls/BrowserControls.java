/* Copyright (C) 2005-2011 Fabio Riccardi */

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

        RotateButtons rotators = new RotateButtons(browser);
        ToggleTitleBorder.setBorder(rotators, LOCALE.get("RotateBorderTitle"));

        BoxedButton rater = new BoxedButton(LOCALE.get("RateBorderTitle"), new RatingButton(browser));

        CopyPasteButtons copyPaste = new CopyPasteButtons(browser);
        ToggleTitleBorder.setBorder(copyPaste, LOCALE.get("CopyToolsBorderTitle"));

        SortCtrl sort = new SortCtrl(browser);
        ToggleTitleBorder.setBorder(sort, LOCALE.get("SortBorderTitle"));

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

        BoxedButton collapse = new BoxedButton(LOCALE.get("CollapseBorderTitle"), new CollapseButton(frame));

        BoxedButton latest = new BoxedButton(LOCALE.get("SelectBorderTitle"), new SelectLatestButton(browser));

        BoxedButton trash = new BoxedButton(LOCALE.get("TrashBorderTitle"), new TrashButton(browser));

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
        // add(Box.createHorizontalGlue());
        add(Box.createHorizontalStrut(8));
        add(rotators);
        add(Box.createHorizontalStrut(8));
        add(rater.box);
        add(Box.createHorizontalStrut(8));

        add(trash.box);
        add(Box.createHorizontalStrut(8));

        add(new Separator());
        add(Box.createHorizontalStrut(8));

        add(copyPaste);
        add(Box.createHorizontalStrut(8));

        add(Box.createHorizontalGlue());

        add(browserError);
        add(Box.createHorizontalStrut(8));

        add(collapse.box);
        add(Box.createHorizontalStrut(8));
        add(latest.box);
        add(Box.createHorizontalStrut(8));

        add(new Separator());
        add(Box.createHorizontalStrut(8));

        add(sizeSlider);
        add(Box.createHorizontalStrut(8));
        add(sort);
        add(Box.createHorizontalStrut(8));
//        add(Box.createHorizontalGlue());

        // Add space above and below, to tune the layout:
        Border border = BorderFactory.createEmptyBorder(0, 0, 3, 0);
        setBorder(border);

        ImageDatumComparator comp = sort.getSort();
        boolean inverted = sort.getSortInverted();
        int size = sizeSlider.getValue();

        browser.setSort(comp);
        browser.setSortInverted(inverted);
        browser.setCharacteristicSize(size);
    }

    class Separator extends JSeparator {
        Separator() {
            super(SwingConstants.VERTICAL);
            setMaximumSize(new Dimension(3, 32));
        }
    }

/*
        // add(Box.createHorizontalStrut(8));
        // Box s = Box.createHorizontalBox();
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        // s.add(separator);
        // separator.setLayout(new BoxLayout(separator, BoxLayout.X_AXIS));
        add(separator);
        // add(Box.createHorizontalStrut(8));
        add(Box.createHorizontalGlue());

 */
    public void dispose() {
        ToggleTitleBorder.unsetAllBorders(this);
    }
}
