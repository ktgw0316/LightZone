/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.browser.ctrls.FolderCtrl;
import com.lightcrafts.ui.browser.view.ImageBrowserScrollPane;
import com.lightcrafts.ui.editor.DocUndoHistory;
import com.lightcrafts.ui.editor.Editor;
import com.lightcrafts.ui.layout.FadingTabConfiguration;
import com.lightcrafts.ui.metadata2.MetadataScroll;
import com.lightcrafts.ui.templates.TemplateControl;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;

/**
 * This AbstractLayout holds the top-level layout of the "editor" perspective
 * in a ComboFrame: an Editor, its tool components, a TemplateControl and
 * an InfoPane components.
 */
class EditorLayout extends AbstractLayout {

    EditorLayout(
        TemplateControl templates,
        Editor editor,
        DocUndoHistory history,
        FolderCtrl folders,
        ImageBrowserScrollPane browser,
        MetadataScroll info,
        LayoutHeader header
    ) {
        super(
            LayoutType.Editor,
            templates, editor, history, folders, browser, info, header
        );
        updateEditor(templates, editor, history, info);
    }

    void updateEditor(
        TemplateControl templates,
        Editor editor,
        DocUndoHistory history,
        MetadataScroll info
    ) {
        super.updateEditor(templates, editor, history, info);

        initFaders();

        JComponent leftButtons = leftFader.getButtonContainer();
        JComponent rightButtons = rightFader.getButtonContainer();

        leftSplit.setLeftComponent(leftFader);
        leftSplit.setRightComponent(editor.getImage());
        leftSplit.setResizeWeight(0d);

        rightSplit.setLeftComponent(leftSplit);
        rightSplit.setRightComponent(rightFader);
        rightSplit.setResizeWeight(1d);

        restoreLayout();

        header.removeButtons();
        header.addButtons(editor.getToolBar());

        setLayout(new BorderLayout());
        removeAll();
        add(rightSplit);
        add(header, BorderLayout.NORTH);
        add(leftButtons, BorderLayout.WEST);
        add(rightButtons, BorderLayout.EAST);

        leftSplit.updateOppositeMinSize();
    }

    void updateBrowser() {
        // No browser is visible in this layout.
    }

    void ensureToolsVisible() {
        String toolsLabel = LOCALE.get("ToolsTabLabel");
        rightFader.setSelected(toolsLabel);
    }

    List<FadingTabConfiguration> getLeftFaderConfs() {
        String templatesLabel = LOCALE.get("TemplatesTabLabel");
        String undoLabel = LOCALE.get("UndoTabLabel");
        String templatesTip = LOCALE.get("TemplatesTabToolTip");
        String undoTip = LOCALE.get("UndoTabToolTip");
        List<FadingTabConfiguration> tabs =
            new LinkedList<FadingTabConfiguration>();
        tabs.add(
            new FadingTabConfiguration(templates, templatesLabel, templatesTip)
        );
        tabs.add(
            new FadingTabConfiguration(history, undoLabel, undoTip)
        );
        return tabs;
    }

    List<FadingTabConfiguration> getRightFaderConfs() {
        String toolsLabel = LOCALE.get("ToolsTabLabel");
        String infoLabel = LOCALE.get("InfoTabLabel");
        String toolsTip = LOCALE.get("ToolsTabToolTip");
        String infoTip = LOCALE.get("InfoTabToolTip");
        List<FadingTabConfiguration> tabs =
            new LinkedList<FadingTabConfiguration>();
        tabs.add(
            new FadingTabConfiguration(
                editor.getToolStack(), toolsLabel, toolsTip
            )
        );
        tabs.add(
            new FadingTabConfiguration(info, infoLabel, infoTip)
        );
        return tabs;
    }

    String getDefaultLeftFaderTab() {
        return LOCALE.get("TemplatesTabLabel");
    }

    String getDefaultRightFaderTab() {
        return LOCALE.get("ToolsTabLabel");
    }

    // On entering this layout, check that the right split pane divider
    // hasn't compressed the tool stack below its minimum size.
    public void doLayout() {
        super.doLayout();
        rightSplit.checkConstraints();
    }
}
