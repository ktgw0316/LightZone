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
import java.util.LinkedList;
import java.util.List;

/**
 * This AbstractLayout holds the top-level layout for the standard perspective,
 * including of an Editor and an AbstractImageBrowser.
 */
class ComboLayout extends AbstractLayout {

    // An extra split pane, to divide the browser and the editor image.
    private final static int MiddleDividerLoc = -1;
    private final static String MiddleDividerKey = "middleDividerLocation";
    private SmartSplitPane middleSplit;

    // Store the toolbars, so they can be replaced at folder selection and
    // document changes.
    private JPanel browserPanel;
    private JComponent editorToolBar;
    private BrowserControls browserToolBar;
    private ImageBrowserFooter footer;
    private final ComboFrame frame;

    ComboLayout(
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
            LayoutType.Combo,
            templates, editor, history, folders, browser, info, header
        );
        this.frame = frame;
        browserToolBar = new BrowserControls(browser, frame);
        editorToolBar = editor.getToolBar();

        initFaders();

        // Combine the browser with the browser controls:
        browserPanel = new JPanel(new BorderLayout());
        browserPanel.add(browser);
        browserPanel.add(browserToolBar, BorderLayout.NORTH);

        footer = new ImageBrowserFooter();
        browser.getBrowser().addBrowserListener(footer);
        browserPanel.add(footer, BorderLayout.SOUTH);

        // Combine the preview with the browser:
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

        setLayout(new BorderLayout());
        add(rightSplit);
        add(editorToolBar, BorderLayout.NORTH);
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

        remove(editorToolBar);
        editorToolBar = editor.getToolBar();
        add(editorToolBar, BorderLayout.NORTH);

        JComponent image = editor.getImage();
        middleSplit.setLeftFrozenDivider(image);

        // Redo the faders, because they hold the TemplateControl and the
        // Editor's tool stack.
        remove(leftFader.getButtonContainer());
        remove(rightFader.getButtonContainer());
        initFaders();
        leftSplit.setLeftFrozenDivider(leftFader);
        rightSplit.setRightFrozenDivider(rightFader);
        add(leftFader.getButtonContainer(), BorderLayout.WEST);
        add(rightFader.getButtonContainer(), BorderLayout.EAST);

        validate();

        // The ImageMetadata argument is not used in this layout.
    }

    void updateBrowser() {
        browserPanel.remove(browserToolBar);
        browserToolBar.dispose();
        browserToolBar = new BrowserControls(browser, frame);
        browserPanel.add(browserToolBar, BorderLayout.NORTH);

        footer = new ImageBrowserFooter();
        browser.getBrowser().addBrowserListener(footer);
        browserPanel.add(footer, BorderLayout.SOUTH);

        validate();
    }

//    private void initToolBars() {
//        browserToolBar = new BrowserControls(browser);
//        editorToolBar = editor.getToolBar();
//    }

    List<FadingTabConfiguration> getLeftFaderConfs() {
        String templatesLabel = LOCALE.get("TemplatesTabLabel");
        String templatesTip = LOCALE.get("TemplatesTabToolTip");
        String foldersLabel = LOCALE.get("FoldersTabLabel");
        String foldersTip = LOCALE.get("FoldersTabToolTip");
        List<FadingTabConfiguration> tabs =
            new LinkedList<FadingTabConfiguration>();
        tabs.add(
            new FadingTabConfiguration(folders, foldersLabel, foldersTip)
        );
        tabs.add(
            new FadingTabConfiguration(templates, templatesLabel, templatesTip)
        );
        return tabs;
    }

    List<FadingTabConfiguration> getRightFaderConfs() {
        String infoLabel = LOCALE.get("InfoTabLabel");
        String infoTip = LOCALE.get("InfoTabToolTip");
        String toolsLabel = LOCALE.get("ToolsTabLabel");
        String toolsTip = LOCALE.get("ToolsTabToolTip");
        List<FadingTabConfiguration> tabs =
            new LinkedList<FadingTabConfiguration>();
        tabs.add(
            new FadingTabConfiguration(
                editor.getToolStack(), toolsLabel, toolsTip
            )
        );
        tabs.add(new FadingTabConfiguration(info, infoLabel, infoTip));
        return tabs;
    }

    String getDefaultLeftFaderTab() {
        return LOCALE.get("FoldersTabLabel");
    }

    String getDefaultRightFaderTab() {
        return LOCALE.get("ToolsTabLabel");
    }

    void dispose() {
        super.dispose();
        browserToolBar.dispose();
    }
}
