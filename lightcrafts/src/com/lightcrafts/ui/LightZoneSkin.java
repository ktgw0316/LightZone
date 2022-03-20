/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2013-     Masahiro Kitagawa */

package com.lightcrafts.ui;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;

import javax.media.jai.IHSColorSpace;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.color.ColorSpace;

public class LightZoneSkin {
    public static String NAME = "LightZone";

    public static class Colors {
        public final static Color NeutralGray;

        static {
            float[] comps = ColorSpace.getInstance(ColorSpace.CS_sRGB).fromCIEXYZ(
                    ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB).toCIEXYZ(new float[]{0.18f, 0.18f, 0.18f}));
            NeutralGray = new Color(comps[0], comps[0], comps[0]);
        }

        static {
            Color temp = NeutralGray.darker();
            BrowserImageTypeLabelBackground = new Color(
                    temp.getRed(), temp.getGreen(), temp.getBlue(), 128
            );
        }

        public final static Color EditorBackground = NeutralGray.darker();

        public final static Color FrameBackground = new Color(28, 28, 28);

        public final static Color ToolPanesBackground = new Color(62, 62, 62);

        public final static Color LabelForeground = new Color(229, 229, 229);

        public final static Color ToolsBackground = ToolPanesBackground;
        public final static Color ToolTitleTextColor = LabelForeground;
        public final static Color ToolPanesForeground = LabelForeground;

        public final static Color BrowserBackground = NeutralGray.darker();

        public final static Color BrowserSelectHighlight = new Color(188, 188, 154);
        public final static Color BrowserLabelBackground = new Color(38, 38, 38);

        public final static Color BrowserLabelForeground = LabelForeground;

        public final static Color BrowserGroupColor = Color.gray;
        public final static Color BrowserImageTypeLabelBackground;

        public final static Color LZOrange = new Color(254, 155, 14);
        public final static Color SelectedToolBorder = relight(LZOrange, 0.7f);

        private static Color relight(Color color, float amount) {
            IHSColorSpace ihs = IHSColorSpace.getInstance();

            float[] components = new float[3];
            components = ihs.fromRGB(color.getColorComponents(components));
            components[0] *= amount;
            components = ihs.toRGB(components);
            return new Color(components[0], components[1], components[2]);
        }
    }

    public static class LightZoneFontSet {
        // cf. https://www.formdev.com/flatlaf/typography/#available
        public static final FontUIResource TitleFont = new FontUIResource(UIManager.getFont("small.font"));
        public static final FontUIResource SmallFont = new FontUIResource(UIManager.getFont("small.font"));
    }

    public static Border getImageBorder() {
        return getPaneBorder(); // new CompoundBorder(getPaneBorder(), new MatteBorder(6, 6, 6, 6, Colors.EditorBackground));
    }

    public static Border getPaneBorder() {
        return new EtchedBorder(EtchedBorder.LOWERED, new Color(48, 48, 48), new Color(23, 23, 23));
    }

    public static LookAndFeel getLightZoneLookAndFeel() {
        return new FlatMaterialDarkerIJTheme();
    }
}
