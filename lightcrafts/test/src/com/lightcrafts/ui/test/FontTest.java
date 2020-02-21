/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.test;

import com.lightcrafts.platform.Platform;

import javax.swing.*;
import java.awt.*;

// FontBrowser - look at the available fonts
//
// Copyright (C)1996,1998 by Jef Poskanzer <jef@mail.acme.com>. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.


public class FontTest extends JPanel {

    /// Applet info.
    public String getAppletInfo() {
        return getClass().getName() +
               " - Copyright (C) 1996 by Jef Poskanzer <jef@mail.acme.com>.  All rights reserved.";
    }

    private Choice fontChoice;
    private Choice styleChoice;
    private Choice sizeChoice;
    private TextArea textArea;
    private JLabel label;

    /// Called when the applet is first created.
    public FontTest() {

        setLayout(new BorderLayout());

        Panel choicePanel = new Panel();
        choicePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        String defaultFontName = getFont().getName();
        fontChoice = new Choice();
        fontChoice.addItem("");
        fontChoice.select(0);
        for (int i = 0; i < fontNames.length; ++i) {
            fontChoice.addItem(fontNames[i]);
            if (fontNames[i].equals(defaultFontName))
                fontChoice.select(i + 1);
        }
        choicePanel.add(fontChoice);

        styleChoice = new Choice();
        styleChoice.addItem("PLAIN");
        styleChoice.addItem("BOLD");
        styleChoice.addItem("ITALIC");
        styleChoice.addItem("BOLD ITALIC");
        styleChoice.select(0);
        choicePanel.add(styleChoice);

        sizeChoice = new Choice();
        sizeChoice.addItem("6");
        sizeChoice.addItem("8");
        sizeChoice.addItem("10");
        sizeChoice.addItem("12");
        sizeChoice.addItem("15");
        sizeChoice.addItem("20");
        sizeChoice.addItem("25");
        sizeChoice.select(3);
        choicePanel.add(sizeChoice);

        add("North", choicePanel);

        textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setBackground(Color.darkGray);

        String star = "\u2605";

        label = new JLabel("test Label: ABCDEFGHIJKLMNOPQRSTUVWXYZ " + star);

        add("East", label);
        add("Center", textArea);

        browse();
    }

    /// Event handler.
    public boolean action(Event event, Object what) {
        if (event.target == fontChoice || event.target == styleChoice ||
            event.target == sizeChoice) {
            browse();
            return true;
        }
        return false;
    }

    private void browse() {
        textArea.setText("");
        String fontName = fontChoice.getSelectedItem();
        if (fontName.equals(""))
            return;

        String styleStr = styleChoice.getSelectedItem();
        int style;
        if (styleStr.equals("PLAIN"))
            style = Font.PLAIN;
        else if (styleStr.equals("BOLD"))
            style = Font.BOLD;
        else if (styleStr.equals("ITALIC"))
            style = Font.ITALIC;
        else if (styleStr.equals("BOLD ITALIC"))
            style = Font.BOLD | Font.ITALIC;
        else
            style = Font.PLAIN;

        String sizeStr = sizeChoice.getSelectedItem();
        int size = Integer.parseInt(sizeStr);

        Font font = new Font(fontName, style, size);

        label.setFont(font);
        
        textArea.setFont(font);
        textArea.append("family: " + font.getFamily() + "\n");
        textArea.append("name: " + font.getName() + "\n");
        textArea.append(
                "style:" +
                (font.isPlain() ? " PLAIN" : "") +
                (font.isBold() ? " BOLD" : "") +
                (font.isItalic() ? " ITALIC" : "") +
                "\n");
        textArea.append("size: " + font.getSize() + "\n");
        textArea.append("\n");
        FontMetrics fm = textArea.getFontMetrics(font);
        if (fm == null)
            return;
        textArea.append("leading: " + fm.getLeading() + "\n");
        textArea.append("ascent: " + fm.getAscent() + "\n");
        textArea.append("descent: " + fm.getDescent() + "\n");
        textArea.append("height: " + fm.getHeight() + "\n");
        textArea.append("max ascent: " + fm.getMaxAscent() + "\n");
        textArea.append("max descent: " + fm.getMaxDescent() + "\n");
        textArea.append("max advance: " + fm.getMaxAdvance() + "\n");

        int[] widths = fm.getWidths();
        boolean fixed = true;
        for (int i = 33; i <= 126; ++i)
            if (widths[i] != widths[32]) {
                fixed = false;
                break;
            }
        if (fixed)
            textArea.append("fixed width\n");
        else
            textArea.append("variable width\n");

        textArea.append("\n");
        textArea.append(" !\"#$%&'()*+,-./0123456789:;<=>?\n");
        textArea.append("@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_\n");
        textArea.append("`abcdefghijklmnopqrstuvwxyz{|}~\n");
    }

    /// Main program, so we can run as an application too.
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        JFrame frame = new JFrame();
        frame.setContentPane(new FontTest());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
