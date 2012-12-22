/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view.test;

import com.lightcrafts.ui.browser.ctrls.NavigationPane;
import com.lightcrafts.ui.browser.ctrls.SizeSlider;
import com.lightcrafts.ui.browser.ctrls.SortCtrl;
import com.lightcrafts.ui.browser.folders.FolderBrowserPane;
import com.lightcrafts.ui.browser.folders.FolderTreeListener;
import com.lightcrafts.ui.browser.model.ImageList;
import com.lightcrafts.ui.browser.view.ExpandedImageBrowser;
import com.lightcrafts.ui.scroll.CenteringScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.File;
import java.util.List;

public class BrowserTest {

    private FolderBrowserPane tree;
    private JScrollPane images;
    private ImageList list;
//    private ColumnSlavedImageBrowser display;
//    private RowSlavedImageListDisplay display;
//    private ScrollableSlavedImageBrowser display;
    private ExpandedImageBrowser display;
    private NavigationPane buttons;
    private JPanel sort;

    public BrowserTest() {

        tree = new FolderBrowserPane();
        buttons = new NavigationPane(tree); // new NavigationButtons(tree);
        sort = new JPanel(new FlowLayout());

        images = new CenteringScrollPane();
        images.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        images.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        );
        images.getViewport().setBackground(Color.gray);
        
        images.getHorizontalScrollBar().addAdjustmentListener(
            new AdjustmentListener() {
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    if (! e.getValueIsAdjusting()) {
                        // This is a trick to reorder the thumbnail tasks
                        // so refreshes happen in order in the viewport.
                        EventQueue.invokeLater(
                            new Runnable() {
                                public void run() {
                                    images.repaint();
                                }
                            }
                        );
                    }
                }
            }
        );
        tree.addSelectionListener(
            new FolderTreeListener() {
                public void folderSelectionChanged(File folder) {
                    showFolder(folder);
                }
                public void folderDropAccepted(List<File> files, File folder) {
                }
            }
        );
        File folder = tree.getSelectedFile();
        if (folder != null) {
            showFolder(folder);
        }
    }

    public JComponent getFolderTree() {
        return tree;
    }

    public JComponent getButtons() {
        return buttons;
    }

    public JComponent getImages() {
        return images;
    }

    private void showFolder(File folder) {
        if (list != null) {
            list.stop();
        }
        list = ImageListProgress.createImageList(null, folder);
        display = new ExpandedImageBrowser(list);
//        display = new ScrollableSlavedImageBrowser(list);
//        display = new ColumnSlavedImageBrowser(list);
//        display = new RowSlavedImageListDisplay(list);

        sort.removeAll();

        sort.add(new SortCtrl(display));

        sort.add(new SizeSlider(display));

        JViewport viewport = images.getViewport();
        viewport.setView(display);
        list.start();

        Container parent = sort.getParent();
        if (parent != null) {
            parent.validate();
        }
    }

    public static void main(String[] args) {
        System.loadLibrary("DCRaw");

        BrowserTest browser = new BrowserTest();

        JFrame frame = new JFrame("Folders");
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(browser.getFolderTree());
        panel.add(browser.getButtons(), BorderLayout.NORTH);
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocation(100, 100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        frame = new JFrame("Images");
        panel = new JPanel(new BorderLayout());
        panel.add(browser.getImages());
        panel.add(browser.sort, BorderLayout.NORTH);
        frame.setContentPane(panel);
        frame.setBounds(100, 500, 400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
