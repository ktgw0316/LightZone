/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.jai.utils.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.prefs.Preferences;

/**
 * A renderer for ImageDatums that has insets but no visible border, unless
 * the "selected" flag is set.
 */
class ImageDatumRenderer {

    // Parameters for the selection highlight border:
    private final static Color SelectColor = LightZoneSkin.Colors.BrowserSelectHighlight;
    private final static Stroke SelectStroke = new BasicStroke(2f);

    // An empty border around every rendering: (Room for ImageGroup highlight)
    private final static Insets ImageInset = new Insets(8, 8, 8, 8);

    private final static int TextHeight = Platform.isMac() ? 19 : 14;

    // Vertical space between the image area and the label text:
    private final static int TextGap = 4;

    // Label background color cloned from iView Pro:
    private final static Color LabelBackground = LightZoneSkin.Colors.BrowserLabelBackground;

    private final static Color LabelForeground = LightZoneSkin.Colors.BrowserLabelForeground;

    // A preference determines whether to show the ImageDatumType overlays.
    private final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/ui/browser/view"
    );
    private final static String ImageTypesKey = "ImageTypes";

    private static boolean ShowImageTypes = Prefs.getBoolean(ImageTypesKey, true);

    static void setShowImageTypes(boolean show) {
        if (show != ShowImageTypes) {
            ShowImageTypes = show;
            Prefs.putBoolean(ImageTypesKey, show);
        }
    }

    static boolean doesShowImageTypes() {
        return ShowImageTypes;
    }

    // This component renders the text:
    private JLabel label = new JLabel();
    {
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setForeground(LabelForeground);
        label.setBackground(LabelBackground);
        label.setFont(new Font("SansSerif", Font.PLAIN, 10));
        label.setOpaque(true);
    }

    private static final AffineTransform identityTransform = new AffineTransform();

    /**
     * Paint the given ImageDatum's image into the given graphics context at
     * the given Rectangle.
     */
    void paint(
        Graphics2D g,
        RenderedImage image,
        String text,
        String tag,
        int rating,
        Rectangle rect,
        boolean selected
    ) {
        // Find the AffineTransform to center the image inside the border
        Dimension imageSize =
            new Dimension(image.getWidth(), image.getHeight());

        // Leave room for the text:
        label.setText(text);
        Insets insets = new Insets(
            ImageInset.top,
            ImageInset.left,
            ImageInset.bottom + TextHeight + TextGap,
            ImageInset.right
        );

        // Maybe add a selection highlight
        if (selected) {
            Color oldColor = g.getColor();
            Stroke oldStroke = g.getStroke();
            g.setColor(SelectColor);
            g.setStroke(SelectStroke);
            g.drawRect(rect.x + 2, rect.y + 2, rect.width - 4, rect.height - 4);
            g.setStroke(oldStroke);
            g.setColor(oldColor);
        }


        // Paint the image centered inside the insets
        AffineTransform imageXform = getImageXform(rect, insets, imageSize);

        // Avoid pushing scaling transforms in drawRenderedImage, it is really slow...
        RenderedImage xformedImage = image;
        if (!imageXform.isIdentity()) {
            RenderingHints extenderHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                              BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            ParameterBlock params = new ParameterBlock();
            params.addSource(image);
            params.add(imageXform);
            params.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
            xformedImage = JAI.create("Affine", params, extenderHints);
        }
        g.drawRenderedImage(xformedImage, identityTransform);

        // Find the text area bounds:
        Rectangle textRect = getTextRectangle(rect);

        // Render the text
        AffineTransform oldXform = g.getTransform();
        Color oldColor = g.getColor();
        g.translate(textRect.x, textRect.y);
        label.setBounds(textRect);
        label.paint(g);
        g.setColor(oldColor);
        g.setTransform(oldXform);

        // Show any rating number and the star
        if (rating > 0) {
            ImageDatumRatingRenderer.paint(g, rect, rating);
        }
        // Show the type overlay (RAW, LZN, etc.)
        if (ShowImageTypes) {
            ImageDatumTypeRenderer.paint(g, rect, tag);
        }
    }

    // Get the bounds of the rectangle where the label text goes, including
    // its filled background, for the image displayed inside the given display
    // rectangle.  Used in paint(), for drawing the text, and also in
    // StarSlider
    static Rectangle getTextRectangle(Rectangle rect) {
        Insets insets = new Insets(
            ImageInset.top,
            ImageInset.left,
            ImageInset.bottom + TextHeight + TextGap,
            ImageInset.right
        );
        Rectangle textRect = new Rectangle(
            rect.x + insets.left + TextGap,
            rect.y + rect.height - insets.bottom + TextGap,
            rect.width - insets.right - insets.left - 2 * TextGap,
            TextHeight
        );
        return textRect;
    }

    // Get the AffineTransform which maps a rectangle of the given image size
    // at the origin into the given Rectangle with Insets.
    private static AffineTransform getImageXform(
        Rectangle rect, Insets insets, Dimension image
    ) {
        // First, compute the transform that maps into this size at the origin

        Dimension bound = rect.getSize();

        // Two possible scale factors, depending on the aspect ratio
        double hSpace = bound.width - insets.left - insets.right;
        double vSpace = bound.height - insets.top - insets.bottom;

        double sWide = hSpace / (double) image.width;
        double sTall = vSpace / (double) image.height;

        // The actual scale factor is the lesser
        double s = Math.min(sWide, sTall);

        AffineTransform xform = AffineTransform.getScaleInstance(s, s);

        // Depending on the aspect ratio, center vertically or horizontally
        if (sWide < sTall) {
            // Center the image vertically
            double gap = insets.top + (vSpace - image.height * s) / 2;
            AffineTransform trans =
                AffineTransform.getTranslateInstance(insets.left, gap);
            xform.preConcatenate(trans);
        }
        else {
            // Center the image horizontally
            double gap = insets.left + (hSpace - image.width * s) / 2;
            AffineTransform trans =
                AffineTransform.getTranslateInstance(gap, insets.top);
            xform.preConcatenate(trans);
        }
        // Finally, append a translation to the location of the target
        AffineTransform trans = AffineTransform.getTranslateInstance(
            rect.x, rect.y
        );
        xform.preConcatenate(trans);

        return xform;
    }
}
