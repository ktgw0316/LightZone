/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui;

import org.jvnet.substance.theme.SubstanceTheme;
import org.jvnet.substance.theme.SubstanceComplexTheme;
import org.jvnet.substance.theme.SubstanceEbonyTheme;
import org.jvnet.substance.color.BaseColorScheme;
import org.jvnet.substance.color.ColorScheme;
import org.jvnet.substance.skin.SubstanceAbstractSkin;
import org.jvnet.substance.painter.SimplisticSoftBorderReverseGradientPainter;
import org.jvnet.substance.painter.GlassGradientPainter;
import org.jvnet.substance.painter.AlphaControlBackgroundComposite;
import org.jvnet.substance.button.ClassicButtonShaper;
import org.jvnet.substance.watermark.SubstanceNoneWatermark;
import org.jvnet.substance.title.ArcHeaderPainter;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.SubstanceToggleButtonUI;
import org.jvnet.substance.SubstanceButtonUI;
import org.jvnet.substance.SubstanceLabelUI;

import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;

import contrib.com.jgoodies.looks.common.FontSet;
import contrib.com.jgoodies.looks.common.FontPolicy;
import com.lightcrafts.mediax.jai.IHSColorSpace;

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

        float components[] = new float[3];
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

    private static final RenderingHints aliasingRenderHints;

    static {
        aliasingRenderHints = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        aliasingRenderHints.put(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        aliasingRenderHints.put(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );
    }

    public static class LightZoneButtonUI extends SubstanceButtonUI {
        public static ComponentUI createUI(JComponent b) {
            AbstractButton button = (AbstractButton) b;
            button.setRolloverEnabled(true);
            button.setOpaque(false);
            button.setFocusable(false);
            button.setFocusPainted(false);
            return new LightZoneButtonUI();
        }

        // On Windows text aliasing is off for some reason...
        public void paint(java.awt.Graphics graphics, javax.swing.JComponent jComponent) {
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHints(aliasingRenderHints);
            super.paint(graphics, jComponent);
        }

        public void installDefaults(final AbstractButton b) {
            super.installDefaults(b);
            b.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        }
    }

    public static class LightZoneToggleButtonUI extends SubstanceToggleButtonUI {
        public static ComponentUI createUI(JComponent b) {
            AbstractButton button = (AbstractButton) b;
            button.setRolloverEnabled(true);
            button.setFocusable(false);
            button.setFocusPainted(false);
            return new LightZoneToggleButtonUI();
        }

        // On Windows text aliasing is off for some reason...
        public void paint(java.awt.Graphics graphics, javax.swing.JComponent jComponent) {
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHints(aliasingRenderHints);
            super.paint(graphics, jComponent);
        }

        public void installDefaults(final AbstractButton b) {
            super.installDefaults(b);
            b.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        }
    }

    public static class LightZoneLookAndFeel extends SubstanceLookAndFeel {
        protected void initClassDefaults(UIDefaults table) {
            super.initClassDefaults(table);
            Object[] uiDefaults = {
                "ButtonUI", LightZoneButtonUI.class.getName(),
                "ToggleButtonUI", LightZoneToggleButtonUI.class.getName()
            };
            table.putDefaults(uiDefaults);
        }
    }

    public static LookAndFeel getLightZoneLookAndFeel() {
        LookAndFeel substance = new LightZoneLookAndFeel();

        LightZoneLookAndFeel.setSkin(new LightZoneSkin());

        FontPolicy newFontPolicy = new FontPolicy() {
            public FontSet getFontSet(String lafName,
                                      UIDefaults table) {
                return new LightZoneSkin.LightZoneFontSet();
            }
        };

        LightZoneLookAndFeel.setFontPolicy(newFontPolicy);

        UIManager.put(SubstanceLookAndFeel.NO_EXTRA_ELEMENTS, Boolean.TRUE);

        UIManager.put("ToolTip.backgroundInactive", substance.getDefaults().get("ToolTip.background"));
        UIManager.put("ToolTip.foregroundInactive", substance.getDefaults().get("ToolTip.foreground"));

        return substance;
    }
}
