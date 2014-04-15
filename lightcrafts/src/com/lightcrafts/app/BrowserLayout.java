/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.browser.ctrls.BrowserControls;
import com.lightcrafts.ui.browser.ctrls.FolderCtrl;
import com.lightcrafts.ui.browser.view.ImageBrowserFooter;
import com.lightcrafts.ui.browser.view.ImageBrowserScrollPane;
import com.lightcrafts.ui.editor.DocUndoHistory;
import com.lightcrafts.ui.editor.Editor;
import com.lightcrafts.ui.layout.FadingTabConfiguration;
import com.lightcrafts.ui.layout.SmartSplitPane;
import com.lightcrafts.ui.metadata2.MetadataScroll;
import com.lightcrafts.ui.templates.TemplateControl;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * This AbstractLayout holds the top-level layout of the "browser" perspective
 * in a ComboFrame: a TemplateControl, a FolderCtrl, a disabled Editor, an
 * AbstractImageBrowser, and an InfoPane, with no Editor toolbar or tool stack.
 */
class BrowserLayout extends AbstractLayout {

    // An extra split pane, to divide the browser and the editor image.
    private final static int MiddleDividerLoc = -1;
    private final static String MiddleDividerKey = "middleDividerLocation";
    private SmartSplitPane middleSplit;

    // Store some layout elements, so they can be replaced at folder selection.
    private JPanel browserPanel;
    private BrowserControls browserCtrls;
    private JComponent toolBar;
    private ImageBrowserFooter footer;
    private final ComboFrame frame;

    BrowserLayout(
        TemplateControl templates,
        Editor editor,
        DocUndoHistory history,
        FolderCtrl folders,
        ImageBrowserScrollPane browser,
        MetadataScroll info,
        LayoutHeader header,
        ComboFrame frame // TODO: make this a little thinner...
    ) {
        super(
            LayoutType.Browser,
            templates, editor, history, folders, browser, info, header
        );

        this.frame = frame;

        initToolBar();

        initFaders();

        // Combine the browser with the browser controls and a footer:
        browserPanel = new JPanel(new BorderLayout());
        browserPanel.add(browser);
        browserPanel.add(toolBar, BorderLayout.NORTH);

        footer = new ImageBrowserFooter();
        initFooter();
        browser.getBrowser().addBrowserListener(footer);
        browserPanel.add(footer, BorderLayout.SOUTH);

        // Combine the preview with the browser panel:
        middleSplit = new SmartSplitPane(MiddleDividerKey, null);
        middleSplit.setLeftComponent(editor.getImage());
        middleSplit.setRightComponent(browserPanel);
        middleSplit.setResizeWeight(.5d);
        middleSplit.restoreFromPrefs(
            MiddleDividerLoc, SmartSplitPane.VERTICAL_SPLIT
        );
        // Fader on the left of the left split:
        leftSplit.setLeftComponent(leftFader);
        leftSplit.setRightComponent(middleSplit);
        leftSplit.setResizeWeight(0d);

        // Fader on the right of the right split:
        rightSplit.setLeftComponent(leftSplit);
        rightSplit.setRightComponent(rightFader);
        rightSplit.setResizeWeight(1d);

        restoreLayout();

        // The first time this layout is used, initialize the middle split
        // pane divider (between the thumbnails and the preview area).
        String firstLayoutTag = "FirstLayout";
        Preferences prefs = Preferences.userNodeForPackage(BrowserLayout.class);
        boolean isFirstLayout = prefs.getBoolean(firstLayoutTag, true);
        if (isFirstLayout) {
            // Must enqueue, because the JSplitPane API requires the location
            // to be specified relative to the top of the component, we
            // want to specify relative to the bottom, and so we can only
            // do this after the component size has been determined.
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        Dimension size = middleSplit.getSize();
                        // The number, 220, is coupled with the thumbnail
                        // size initialization in SizeSlider.
                        middleSplit.setDividerLocation(size.height - 220);
                    }
                }
            );
            prefs.putBoolean(firstLayoutTag, false);
        }
        HelpButton help = new HelpButton();
        help.setAlignmentX(1f);

        setLayout(new BorderLayout());
        add(rightSplit);

        add(this.header, BorderLayout.NORTH);
        add(leftFader.getButtonContainer(), BorderLayout.WEST);
        add(rightFader.getButtonContainer(), BorderLayout.EAST);
    }

    void updateEditor(
        TemplateControl templates,
        Editor editor,
        DocUndoHistory history,
        MetadataScroll info
    ) {
        super.updateEditor(templates, editor, history, info);

        // In this layout, we assume the TemplateControl never needs updating,
        // and the ImageMetadata is not used.
        middleSplit.setLeftFrozenDivider(editor.getImage());
    }

    void updateBrowser() {
        browserPanel.remove(toolBar);
        browser.getBrowser().removeBrowserListener(footer);
        browserPanel.remove(footer);

        initToolBar();
        browserPanel.add(toolBar, BorderLayout.NORTH);

        footer = new ImageBrowserFooter();
        initFooter();

        browser.getBrowser().addBrowserListener(footer);
        browserPanel.add(footer, BorderLayout.SOUTH);

        validate();
    }

    private void initFooter() {
        ArrayList<File> files = browser.getBrowser().getSelectedFiles();
        File leadFile = browser.getBrowser().getLeadSelectedFile();
        int count = browser.getBrowser().getImageCount();
        footer.setSelectedFiles(leadFile, files, count);
    }

    void ensureFoldersVisible() {
        String foldersLabel = LOCALE.get("FoldersTabLabel");
        leftFader.setSelected(foldersLabel);
    }

    private void initToolBar() {
        if (browserCtrls != null) {
            browserCtrls.dispose();
        }
        browserCtrls = new BrowserControls(browser, frame);
        toolBar = Box.createHorizontalBox();
        // toolBar.add(Box.createHorizontalGlue());
        toolBar.add(browserCtrls);
        // toolBar.add(Box.createHorizontalGlue());
    }

    List<FadingTabConfiguration> getLeftFaderConfs() {
        String foldersLabel = LOCALE.get("FoldersTabLabel");
        String foldersTip = LOCALE.get("FoldersTabToolTip");
        List<FadingTabConfiguration> tabs =
            new LinkedList<FadingTabConfiguration>();
        tabs.add(
            new FadingTabConfiguration(folders, foldersLabel, foldersTip)
        );
        return tabs;
    }

    List<FadingTabConfiguration> getRightFaderConfs() {
        String infoLabel = LOCALE.get("InfoTabLabel");
        String infoTip = LOCALE.get("InfoTabToolTip");
        List<FadingTabConfiguration> tabs =
            new LinkedList<FadingTabConfiguration>();
        tabs.add(new FadingTabConfiguration(info, infoLabel, infoTip));
        return tabs;
    }

    String getDefaultLeftFaderTab() {
        return LOCALE.get("FoldersTabLabel");
    }

    String getDefaultRightFaderTab() {
        return LOCALE.get("InfoTabLabel");
    }

    void dispose() {
        super.dispose();
        browserCtrls.dispose();
    }
}
