/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.WebBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

/**
 * Display a popup advertisement for the LightZone Video Learning Center.
 */

public class VideoLearningCenterDialog extends JFrame {

    private final static Color LinkColor = new Color(195, 195, 195);
    private final static Color LinkHighlightColor = new Color(249, 155, 28);

    class LinkListener implements MouseListener {
        private JComponent comp;
        private String url;
        LinkListener(JComponent comp, String url) {
            this.comp = comp;
            this.url = url;
        }
        public void mousePressed(MouseEvent e) {
        }
        public void mouseEntered(MouseEvent e) {
            comp.setForeground(LinkHighlightColor);
        }
        public void mouseExited(MouseEvent e) {
            comp.setForeground(LinkColor);
        }
        public void mouseReleased(MouseEvent e) {
        }
        public void mouseClicked(MouseEvent e) {
            WebBrowser.browse(url);
        }
    }

    class LinkLabel extends JLabel {
        LinkLabel(String text) {
            this(text, null);
            setForeground(LinkColor);
            setAlignmentX(.5f);
            setHorizontalAlignment(SwingConstants.CENTER);
        }
        LinkLabel(String text, String url) {
            super(text);
            if (url != null) {
                setText("<html><u>" + text + "</u></html>");
                addMouseListener(new LinkListener(this, url));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
            super.paintComponent(g);
        }
    }

    VideoLearningCenterDialog() {
        String mainUrl = Version.getVideoLearningCenterURL().toString();

        LinkLabel topText = new LinkLabel("Want to learn LightZone?");
        LinkLabel leftText = new LinkLabel("Check out these videos at the ");
        LinkLabel mainLink = new LinkLabel("Video Learning Center", mainUrl);

        ArrayList<LinkLabel> videoLinks = new ArrayList<LinkLabel>();
        Map<String, URL> videoUrls = Version.getVideoURLs();
        for (Map.Entry<String, URL> entry : videoUrls.entrySet()) {
            LinkLabel videoLink =
                new LinkLabel(entry.getKey(), entry.getValue().toString());
            videoLinks.add(videoLink);
        }
        Font font = topText.getFont();
        font = font.deriveFont(24f);
        topText.setFont(font);

        Box content = Box.createVerticalBox();
        content.setBorder(
            BorderFactory.createEmptyBorder(24, 32, 32, 24)
        );
        getContentPane().add(content);
        
        JPanel firstLine = new JPanel(new FlowLayout());
        firstLine.add(leftText);
        firstLine.add(mainLink);

        content.add(topText);
        content.add(Box.createVerticalStrut(16));
        content.add(firstLine);
        content.add(Box.createVerticalStrut(16));

        Box linkBox = Box.createVerticalBox();        
        for (LinkLabel link : videoLinks) {
            Box panel = Box.createHorizontalBox();
            panel.add(link);
            panel.add(Box.createHorizontalGlue());
            linkBox.add(panel);
            linkBox.add(Box.createVerticalStrut(8));
        }
        linkBox.setBorder(
            BorderFactory.createEmptyBorder(0, 32, 0, 32)
        );
        content.add(linkBox);
        
        pack();

        setTitle("LightZone - Video Learning Center");

        setResizable(false);
    }

    static boolean shouldShowDialog() {
//        Preferences prefs = Preferences.userRoot().node("/com/lightcrafts/app");
//        return prefs.getBoolean("VideoLearningCenterPopup", true);
        return false;
    }

    static void showDialog() {
        JFrame dialog = new VideoLearningCenterDialog();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    public static void main(String[] args)
        throws UnsupportedLookAndFeelException
    {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
        showDialog();
    }
}
