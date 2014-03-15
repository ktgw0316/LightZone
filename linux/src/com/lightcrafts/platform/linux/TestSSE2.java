/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.linux;

import static com.lightcrafts.platform.linux.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.TextAreaFactory;
import com.lightcrafts.utils.WebBrowser;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

class TestSSE2 {
    
    static String osname = System.getProperty("os.name");

    static boolean hasSSE2() {
        if (osname.indexOf("Linux") >= 0) {
            String line = getCpuInfoLine("flags\t\t:");
            return line.contains("sse2");
        else if (osname.indexOf("SunOS") >= 0) {
            return true;
        } else {
            String line = getCpuInfoLine("  Features=");
            return line.contains("SSE2");
        }
    }

    private static String getCpuInfoLine(String key) {
        try {
            BufferedReader reader;
            String line;
            if (osname.indexOf("Linux") >= 0) {
                reader = new BufferedReader(
                    new InputStreamReader(
                        new FileInputStream("/proc/cpuinfo")
                    )
                );
            } else if (osname.indexOf("SunOS") >= 0) {
                // TODO: 
            } else {
                Process process = Runtime.getRuntime().exec("dmesg");
                InputStream in = process.getInputStream();
                reader = new BufferedReader(
                    new InputStreamReader(in)
                );
            }
            do {
                line = reader.readLine();
                if (line != null) {
                    if (line.startsWith(key)) {
                        return line;
                    }
                }
            } while (line != null);
            reader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static void showDialog() {
        String model = "";
        if (osname.indexOf("Linux") >= 0) {
            model = getCpuInfoLine("model name\t: ");
            model = model.replaceFirst("model name\t: ", "");
        else if (osname.indexOf("SunOS") >= 0) {
            // TODO: 
        } else {
            model = getCpuInfoLine("CPU: ");
            model = model.replaceFirst("CPU: ", "");
        }

        String messageA = LOCALE.get("CantRunSSE2Title");
        String messageB = LOCALE.get("CantRunSSE2");
        String messageC = LOCALE.get("FoundProcCpuinfo", model);
        String messageD = LOCALE.get("LearnMoreSSE2");
        String messageE = LOCALE.get("LearnMoreSSE2URL");

        JLabel title = new JLabel(messageA);
        title.setFont(title.getFont().deriveFont(22f));
        title.setAlignmentX(.5f);

        StringBuffer buffer = new StringBuffer();
        buffer.append(messageB);
        buffer.append("\n\n");
        buffer.append(messageC);
        buffer.append("\n\n");
        buffer.append(messageD);

        final String url = messageE;
        JLabel link = new JLabel(url);
        link.setForeground(Color.blue);
        link.setAlignmentX(.5f);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addMouseListener(
            new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    WebBrowser.browse(url);
                }
            }
        );
        JTextArea text = TextAreaFactory.createTextArea(buffer.toString(), 40);
        text.setBackground((new JPanel()).getBackground());

        Box box = Box.createVerticalBox();
        box.add(title);
        box.add(Box.createVerticalStrut(8));
        box.add(text);
        box.add(Box.createVerticalStrut(8));
        box.add(link);

        JOptionPane.showMessageDialog(
            null, box, messageA, JOptionPane.ERROR_MESSAGE
        );
    }
}
