/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2013-     Masahiro Kitagawa */

package com.lightcrafts.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLaf;
import contrib.com.jgoodies.looks.common.FontSet;
import org.jvnet.substance.button.ClassicButtonShaper;
import org.jvnet.substance.color.BaseColorScheme;
import org.jvnet.substance.color.ColorScheme;
import org.jvnet.substance.painter.AlphaControlBackgroundComposite;
import org.jvnet.substance.painter.GlassGradientPainter;
import org.jvnet.substance.painter.SimplisticSoftBorderReverseGradientPainter;
import org.jvnet.substance.skin.SubstanceAbstractSkin;
import org.jvnet.substance.theme.SubstanceComplexTheme;
import org.jvnet.substance.theme.SubstanceEbonyTheme;
import org.jvnet.substance.theme.SubstanceTheme;
import org.jvnet.substance.title.ArcHeaderPainter;
import org.jvnet.substance.watermark.SubstanceNoneWatermark;

import javax.media.jai.IHSColorSpace;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.util.Map;

public class LightZoneSkin extends SubstanceAbstractSkin {
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
    }

    static Color relight(Color color, float amount) {
        IHSColorSpace ihs = IHSColorSpace.getInstance();

        float[] components = new float[3];
        components = ihs.fromRGB(color.getColorComponents(components));
        components[0] *= amount;
        components = ihs.toRGB(components);
        return new Color(components[0], components[1], components[2]);
    }

    public static class CustomColorScheme extends BaseColorScheme {
        private final Color mainUltraLightColor;
        private final Color mainExtraLightColor;
        private final Color mainLightColor;
        private final Color mainMidColor;
        private final Color mainDarkColor;
        private final Color mainUltraDarkColor;
        private final Color foregroundColor;

        public CustomColorScheme(Color baseColor) {
            mainUltraLightColor = relight(baseColor, 0.95f);
            mainExtraLightColor = relight(baseColor, 0.85f);
            mainLightColor = relight(baseColor, 0.7f);
            mainMidColor = relight(baseColor, 0.6f);
            mainDarkColor = relight(baseColor, 0.5f);
            mainUltraDarkColor = relight(baseColor, 0.4f);
            foregroundColor = Color.white;
        }

        public Color getForegroundColor() { return foregroundColor; }
        public Color getUltraLightColor() { return mainUltraLightColor; }
        public Color getExtraLightColor() { return mainExtraLightColor; }
        public Color getLightColor() { return mainLightColor; }
        public Color getMidColor() { return mainMidColor; }
        public Color getDarkColor() { return mainDarkColor; }
        public Color getUltraDarkColor() { return mainUltraDarkColor; }
    }

    public static SubstanceTheme makeTheme(ColorScheme colorScheme, String name) {
        SubstanceTheme activeTheme = new SubstanceTheme(colorScheme, name, SubstanceTheme.ThemeKind.DARK);

        SubstanceTheme basicTheme = new SubstanceEbonyTheme().tint(0.05);
        SubstanceTheme defaultTheme = basicTheme.shade(0.2);
        SubstanceTheme disabledTheme = basicTheme.shade(0.3);
        SubstanceTheme activeTitleTheme = defaultTheme;

        SubstanceComplexTheme theme = new SubstanceComplexTheme(name + " Theme",
                                                                SubstanceTheme.ThemeKind.DARK, activeTheme, defaultTheme, disabledTheme,
                                                                activeTitleTheme);

        theme.setNonActivePainter(new SimplisticSoftBorderReverseGradientPainter());
        theme.setSelectedTabFadeStart(0.4);
        theme.setSelectedTabFadeEnd(0.7);
        theme.setCellRendererBackgroundTheme(new SubstanceEbonyTheme());

        return theme;
    }

    public static final SubstanceTheme orangeTheme = makeTheme(new CustomColorScheme(Colors.LZOrange), "Orange");

    public LightZoneSkin() {
        SubstanceTheme activeTheme = new SubstanceEbonyTheme();
        SubstanceTheme defaultTheme = activeTheme.shade(0.2);
        SubstanceTheme disabledTheme = activeTheme.shade(0.3);
        SubstanceTheme activeTitleTheme = defaultTheme;

        SubstanceComplexTheme theme = new SubstanceComplexTheme(NAME,
                                                                SubstanceTheme.ThemeKind.DARK, activeTheme, defaultTheme, disabledTheme,
                                                                activeTitleTheme);
        theme.setNonActivePainter(new SimplisticSoftBorderReverseGradientPainter());
        theme.setSelectedTabFadeStart(0.3);
        theme.setSelectedTabFadeEnd(0.6);
        theme.setCellRendererBackgroundTheme(new SubstanceEbonyTheme());

        this.theme = theme;
        this.shaper = new ClassicButtonShaper();
        this.watermark = new SubstanceNoneWatermark();
        this.gradientPainter = new GlassGradientPainter();
        this.titlePainter = new ArcHeaderPainter();
        this.tabBackgroundComposite = new AlphaControlBackgroundComposite(0.5f);
    }

    public String getDisplayName() {
        return NAME;
    }

    public static class LightZoneFontSet implements FontSet {
        FontUIResource controlFont;
        FontUIResource menuFont;
        FontUIResource titleFont;
        FontUIResource windowTitleFont;
        FontUIResource smallFont;
        FontUIResource messageFont;

        String fontFamily = Font.SANS_SERIF;

        public LightZoneFontSet() {
            controlFont = new FontUIResource(fontFamily, Font.PLAIN, 13);
            menuFont = new FontUIResource(fontFamily, Font.PLAIN, 13);
            titleFont = new FontUIResource(fontFamily, Font.BOLD, 11);
            windowTitleFont = new FontUIResource(fontFamily, Font.BOLD, 16);
            smallFont = new FontUIResource(fontFamily, Font.PLAIN, 13);
            messageFont = new FontUIResource(fontFamily, Font.BOLD, 13);
        }

        public FontUIResource getControlFont() {
            return controlFont;
        }

        public FontUIResource getMenuFont() {
            return menuFont;
        }

        public FontUIResource getTitleFont() {
            return titleFont;
        }

        public FontUIResource getWindowTitleFont() {
            return windowTitleFont;
        }

        public FontUIResource getSmallFont() {
            return smallFont;
        }

        public FontUIResource getMessageFont() {
            return messageFont;
        }
    }

    public static final FontSet fontSet = new LightZoneFontSet();

    public static Border getImageBorder() {
        return getPaneBorder(); // new CompoundBorder(getPaneBorder(), new MatteBorder(6, 6, 6, 6, Colors.EditorBackground));
    }

    public static Border getPaneBorder() {
        return new EtchedBorder(EtchedBorder.LOWERED, new Color(48, 48, 48), new Color(23, 23, 23));
    }

    public static LookAndFeel getLightZoneLookAndFeel() {
        final Color accentColor = Colors.LZOrange.darker();
        final int r = accentColor.getRed();
        final int g = accentColor.getGreen();
        final int b = accentColor.getBlue();
        final String colorString = String.format("#%02x%02x%02x", r, g, b);

        final FlatLaf laf = new FlatDarculaLaf();
        laf.setExtraDefaults(Map.of("@accentColor", colorString));
        return laf;
    }
}
