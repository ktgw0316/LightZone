/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2013-     Masahiro Kitagawa */

package com.lightcrafts.platform.linux;

import static com.lightcrafts.platform.linux.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.TextAreaFactory;
import com.lightcrafts.utils.WebBrowser;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TestSSE2 {

    private static String simdRegex;
    private static String modelRegex;
    private static String[] cmd;

    static {
        String osname = System.getProperty("os.name");

        if (osname.contains("Linux")) {
            String osarch = System.getProperty("os.arch");
            if (osarch.contains("arm") || osarch.contains("aarch")) {
                simdRegex = "^Flags:.* (neon|asimd)";
                modelRegex = "^Model name: ";
                cmd = new String[] {"lscpu"};
            } else {
                simdRegex = "^flags\t\t:.*sse2";
                modelRegex = "^model name\t: ";
                cmd = new String[] {"cat", "/proc/cpuinfo"};
            }
        } else if (osname.contains("SunOS")) {
            cmd = new String[] {"sh", "-c", "isainfo -nv ; psrinfo -pv"};
            simdRegex = "^\t.*sse2";
            modelRegex = "^\t";
        } else if (osname.contains("FreeBSD")) {
            cmd = new String[] {"sysctl", "hw"};
            simdRegex = "^hw.instruction_sse: 1";
            modelRegex = "^hw.model: ";
        } else {
            cmd = new String[] {"dmesg"};
            simdRegex = "^  Features=.*SSE2";
            modelRegex = "^CPU: ";
        }
    }

    static boolean hasSSE2() {
        return getCpuInfoLine(simdRegex) != null;
    }

    private static String getCpuInfoLine(String regex) {
        String line = null;
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            try (InputStream in = process.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                while ((line = reader.readLine()) != null) {
                    if (Pattern.compile(regex).matcher(line).find())
                        break;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    private static String getCpuModel() {
        String model = getCpuInfoLine(modelRegex);
        if (model != null)
            return model.replaceFirst(Matcher.quoteReplacement(modelRegex), "");
        return "";
    }

    static void showDialog() {
        String messageA = LOCALE.get("CantRunSSE2Title");
        String messageB = LOCALE.get("CantRunSSE2");
        String messageC = LOCALE.get("FoundProcCpuinfo", getCpuModel());
        String messageD = LOCALE.get("LearnMoreSSE2");
        String messageE = LOCALE.get("LearnMoreSSE2URL");

        JLabel title = new JLabel(messageA);
        title.setFont(title.getFont().deriveFont(22f));
        title.setAlignmentX(.5f);

        StringBuilder buffer = new StringBuilder();
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

