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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TestSSE2 {
    
    static String osname = System.getProperty("os.name");

    static boolean hasSSE2() {
        String regex;
        if (osname.indexOf("Linux") >= 0) {
            regex = "^flags\t\t:.*sse2";
        } else if (osname.indexOf("SunOS") >= 0) {
            regex = "^\t.*sse2";
        } else if (osname.indexOf("FreeBSD") >= 0) {
            regex = "^hw.instruction_sse: 1";
        } else {
            regex = "^  Features=.*SSE2";
        }
        return (getCpuInfoLine(regex) != null);
    }

    private static String getCpuInfoLine(String regex) {
        String line = null;
        String[] cmd;
        if (osname.indexOf("Linux") >= 0) {
            cmd = new String[] {"cat", "/proc/cpuinfo"};
        } else if (osname.indexOf("SunOS") >= 0) {
            cmd = new String[] {"sh", "-c", "isainfo -nv ; psrinfo -pv"};
        } else if (osname.indexOf("FreeBSD") >= 0) {
            cmd = new String[] {"sysctl", "hw"};
        } else {
            cmd = new String[] {"dmesg"};
        }

        try {
            Process process = Runtime.getRuntime().exec(cmd);
            InputStream in = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null) {
                if (Pattern.compile(regex).matcher(line).find())
                    break;
            }
            reader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    static void showDialog() {
        String regex;
        if (osname.indexOf("Linux") >= 0) {
            regex = getCpuInfoLine("^model name\t: ");
        } else if (osname.indexOf("SunOS") >= 0) {
            regex = getCpuInfoLine("^\t");
        } else if (osname.indexOf("FreeBSD") >= 0) {
            regex = getCpuInfoLine("^hw.model: ");
        } else {
            regex = getCpuInfoLine("^CPU: ");
        }
        String model = getCpuInfoLine(regex);
        if (model != null)
            model = model.replaceFirst(Matcher.quoteReplacement(regex), "");

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
