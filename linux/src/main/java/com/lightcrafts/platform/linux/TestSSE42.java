/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2013-     Masahiro Kitagawa */

package com.lightcrafts.platform.linux;

import static com.lightcrafts.platform.linux.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.TextAreaFactory;
import com.lightcrafts.utils.WebBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

class TestSSE42 {

    private static final Logger logger = LoggerFactory.getLogger(TestSSE42.class);

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
                simdRegex = "^flags\t\t:.*sse4_2";
                modelRegex = "^model name\t: ";
                cmd = new String[] {"cat", "/proc/cpuinfo"};
            }
        } else if (osname.contains("SunOS")) {
            cmd = new String[] {"sh", "-c", "isainfo -nv ; psrinfo -pv"};
            simdRegex = "^\t.*sse4_2";
            modelRegex = "^\t";
        } else if (osname.contains("FreeBSD")) {
            cmd = new String[] {"sysctl", "hw"};
            simdRegex = "^hw.instruction_sse42: 1";
            modelRegex = "^hw.model: ";
        } else {
            cmd = new String[] {"dmesg"};
            simdRegex = "^  Features=.*SSE4.2";
            modelRegex = "^CPU: ";
        }
    }

    static boolean hasSSE42() {
        return getCpuInfoLine(simdRegex) != null;
    }

    private static String getCpuInfoLine(String regex) {
        String line = null;
        try {
            final Process process = new ProcessBuilder(cmd).start();
            try (InputStream in = process.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                while ((line = reader.readLine()) != null) {
                    if (Pattern.compile(regex).matcher(line).find())
                        break;
                }
            }
        }
        catch (IOException e) {
            logger.warn("Failed to read CPU info", e);
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
        String messageA = LOCALE.get("CantRunSSE42Title");
        String messageB = LOCALE.get("CantRunSSE42");
        String messageC = LOCALE.get("FoundProcCpuinfo", getCpuModel());

        JLabel title = new JLabel(messageA);
        title.setFont(title.getFont().deriveFont(22f));
        title.setAlignmentX(.5f);

        String message = messageB + "\n\n" + messageC;
        JTextArea text = TextAreaFactory.createTextArea(message, 40);
        text.setBackground((new JPanel()).getBackground());

        Box box = Box.createVerticalBox();
        box.add(title);
        box.add(Box.createVerticalStrut(8));
        box.add(text);

        JOptionPane.showMessageDialog(
                null, box, messageA, JOptionPane.ERROR_MESSAGE
        );
    }
}

