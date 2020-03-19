/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.browser.folders.FolderBrowserPane;
import com.lightcrafts.ui.browser.folders.FolderTreeListener;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.ui.toolkit.MenuButton;
import com.lightcrafts.ui.toolkit.PaneTitle;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: May 19, 2007
 * Time: 2:34:33 PM
 */
public class NavigationPane extends PaneTitle {

    private static Icon ImgFwd =
        IconFactory.createInvertedIcon(NavigationButtons.class, "forward.png");

    private static Icon ImgBack =
            IconFactory.createInvertedIcon(NavigationButtons.class, "back.png");

    private static Icon imgPath;

    static {
        try {
            imgPath = new ImageIcon(ImageIO.read(NavigationButtons.class.getResource("resources/path.png")));
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't initialize NavigationButtons", e);
        }
    }

    public NavigationPane(final FolderBrowserPane browser) {
        final JButton btnBack = new CoolButton(/*CoolButton.ButtonStyle.LEFT*/);
        final JButton btnForward = new CoolButton(/*CoolButton.ButtonStyle.RIGHT*/);
        final MenuButton btnPath = browser.getPathButton();

        btnBack.setIcon(ImgBack);
        btnBack.setToolTipText(LOCALE.get("BackToolTip"));
        btnBack.setEnabled(false);

        btnForward.setIcon(ImgFwd);
        btnForward.setToolTipText(LOCALE.get("ForwardToolTip"));
        btnForward.setEnabled(false);

        btnPath.setIcon(imgPath);
        // btnPath.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);
        btnPath.setToolTipText(LOCALE.get("PathToolTip"));

        btnBack.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    browser.goBack();
                }
            }
        );
        btnForward.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    browser.goForward();
                }
            }
        );
        browser.addSelectionListener(
            new FolderTreeListener() {
                public void folderSelectionChanged(File folder) {
                    btnBack.setEnabled(browser.isBackAvailable());
                    btnForward.setEnabled(browser.isForwardAvailable());
                }
                public void folderDropAccepted(List<File> files, File folder) {
                }
            }
        );

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalStrut(4));
        buttonBox.add(btnBack);
        buttonBox.add(btnForward);
        buttonBox.add(Box.createHorizontalStrut(8));
        buttonBox.add(btnPath);
        buttonBox.add(Box.createHorizontalGlue());

        assembleTitle(buttonBox);
    }
}
