/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.browser.folders.FolderBrowserPane;
import com.lightcrafts.ui.browser.folders.FolderTreeListener;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.ui.toolkit.MenuButton;
import org.jvnet.substance.SubstanceLookAndFeel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;

public class NavigationButtons extends JPanel {

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

    private FolderBrowserPane browser;

    private JButton btnBack;
    private JButton btnForward;
    private MenuButton btnPath;

    public NavigationButtons( FolderBrowserPane browser ) {
        this.browser = browser;
        setOpaque(true);
        setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        createComponents();
        initComponents();
        plumbComponents();
        layoutComponents();
        setMaximumSize(new Dimension(1000, btnPath.getHeight() + 4));
    }

    private void createComponents() {
        btnBack = new CoolButton(/*CoolButton.ButtonStyle.LEFT*/);
        btnForward = new CoolButton(/*CoolButton.ButtonStyle.RIGHT*/);
        btnPath = browser.getPathButton();
    }

    private void initComponents() {
        btnBack.setIcon(ImgBack);
        btnBack.setToolTipText(LOCALE.get("BackToolTip"));
        btnBack.setEnabled(false);

        btnForward.setIcon(ImgFwd);
        btnForward.setToolTipText(LOCALE.get("ForwardToolTip"));
        btnForward.setEnabled(false);

        btnPath.setIcon(imgPath);
        btnPath.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);
        btnPath.setToolTipText(LOCALE.get("PathToolTip"));
    }

    private void plumbComponents() {
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
    }

    private void layoutComponents() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // Add space above and below, to tune the Mac layout:
        Border border = BorderFactory.createEmptyBorder(3, 0, 3, 0);
        setBorder(border);

        add(Box.createHorizontalStrut(8));

        add(btnBack);
        add(btnForward);

        add(Box.createHorizontalStrut(8));

        add(btnPath);

        add(Box.createHorizontalStrut(8));

        add(Box.createHorizontalGlue());
    }
}
