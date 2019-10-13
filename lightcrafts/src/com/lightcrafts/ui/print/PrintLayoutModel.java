/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import com.lightcrafts.model.PrintSettings;
import com.lightcrafts.model.RenderingIntent;
import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;
import com.lightcrafts.platform.PrinterLayer;
import com.lightcrafts.platform.Platform;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.util.LinkedList;

/**
 * A model for a "Print Layout" dialog that handles the various logical
 * linkages among margins, scale settings, units of measurement, and paper
 * orientation.
 * <p>
 * PrintLayoutModels provide the model for Print Preview dialogs, but they
 * are also public so they can be saved between dialogs.
 */

public class PrintLayoutModel {

    static class LengthUnit {

        static final LengthUnit POINT = new LengthUnit("points", 1.);
        static final LengthUnit INCH = new LengthUnit("inches", 1. / 72.);
        static final LengthUnit CM = new LengthUnit("cm", 2.54 / 72.);

        private String name;
        private double unitsPerPoint;

        private LengthUnit(String name, double unitsPerPoint) {
            this.name = name;
            this.unitsPerPoint = unitsPerPoint;
        }

        public String toString() {
            return name;
        }

        double toPoints(double length) {
            return length / unitsPerPoint;
        }

        double fromPoints(double length) {
            return length * unitsPerPoint;
        }

        static LengthUnit[] getAll() {
            return new LengthUnit[] { INCH, CM, POINT };
        }
    }

    private int imageWidth;         // natural image dimensions, in pixels
    private int imageHeight;
    private int pixelsPerInch;      // determines the "scale"
    private double aspectRatio;
    private ColorProfileInfo profile;
    private RenderingIntent intent;
    private PageFormat pageFormat;
    private Rectangle2D imageRect;  // printed dimensions, in points
    private boolean keepCentered;
    private LinkedList<PrintLayoutModelListener> listeners;

    public PrintLayoutModel(int width, int height) {
        PrinterLayer printer = Platform.getPlatform().getPrinterLayer();
        printer.initialize();
        pageFormat = printer.getPageFormat();
        printer.dispose();

        listeners = new LinkedList<PrintLayoutModelListener>();

        pixelsPerInch = 300;

        updateImageSize(width, height);

        intent = RenderingIntent.PERCEPTUAL;
    }

    // Update the natural image size, in pixels
    public void updateImageSize(int width, int height) {
        if ((width == imageWidth) && (height == imageHeight)) {
            return;
        }
        imageWidth = width;
        imageHeight = height;

        aspectRatio = width / (double) height;

        scaleToFit();
    }

    public RenderingIntent getRenderingIntent() {
        return intent;
    }

    public void setRenderingIntent(RenderingIntent intent) {
        this.intent = intent;
        notifyListeners();
    }

    public ColorProfileInfo getColorProfileInfo() {
        return profile;
    }

    public void setPageFormat(PageFormat pageFormat) {
        this.pageFormat = pageFormat;
        if (keepCentered) {
            centerByTranslation();
        }
        notifyListeners();
    }

    public PageFormat getPageFormat() {
        return pageFormat;
    }

    public void setColorProfile(ColorProfileInfo profile) {
        this.profile = profile;
        notifyListeners();
    }

    public Rectangle2D getImageRect() {
        return (Rectangle2D) imageRect.clone();
    }

    /**
     * Get a PrintSettings instance whose imageable area reflects the
     * internal logic used by the page layout dialog to render its preview
     * images.
     * <p>
     * If this synthetic PrintSettings is passed to an Engine for printing,
     * and if the Engine simply stretches its image to fit the imageable
     * area, then the printed image will match the dialog's preview.
     */
    public PrintSettings getPrintSettings() {
        PrintSettings settings = new PrintSettings();
        settings.setRenderingIntent(intent);
        settings.setColorProfile(
            (profile != null) ? profile.getICCProfile() : null
        );
        settings.setPrintBounds(imageRect);
        settings.setPixelsPerInch(pixelsPerInch);
        return settings;
    }

    void setImageX(double leftInset, LengthUnit unit) {
        leftInset = unit.toPoints(leftInset);
        setImageXSilent(leftInset);
        keepCentered = false;
        notifyListeners();
    }

    void setImageY(double topInset, LengthUnit unit) {
        topInset = unit.toPoints(topInset);
        setImageYSilent(topInset);
        keepCentered = false;
        notifyListeners();
    }

    void setImageWidth(double width, LengthUnit unit) {
        width = unit.toPoints(width);
        setImageWidthSilent(width);
        setImageHeightSilent(width / aspectRatio);
        if (keepCentered) {
            centerByTranslation();
        }
        notifyListeners();
    }

    void setImageHeight(double height, LengthUnit unit) {
        height = unit.toPoints(height);
        setImageWidthSilent(height * aspectRatio);
        setImageHeightSilent(height);
        if (keepCentered) {
            centerByTranslation();
        }
        notifyListeners();
    }

    void setKeepCentered(boolean keepCentered) {
        this.keepCentered = keepCentered;
        if (keepCentered) {
            centerByTranslation();
        }
        notifyListeners();
    }

    void setPpi(int ppi) {
        pixelsPerInch = ppi;
    }

    int getPpi() {
        return pixelsPerInch;
    }

    double getScale() {
        // ppi = pixels / inch
        // LengthUnit.INCH.fromPoints(imageRect.getWidth()) = inch
        // imageWidth = pixels
        double widthInInches = LengthUnit.INCH.fromPoints(imageRect.getWidth());
        return pixelsPerInch * widthInInches / imageWidth;
    }

    void setScale(double scale) {
        // complement of getScale()
        double widthInInches = scale * imageWidth / pixelsPerInch;
        setImageWidth(widthInInches, LengthUnit.INCH);
    }

    boolean isKeepCentered() {
        return keepCentered;
    }

    void scaleToFit() {
        // Find the largest rectangle that is centered on the Paper's center
        // and that is also contained within the PageFormat's imageable area:
        Rectangle2D imageableRect = new Rectangle2D.Double(
            pageFormat.getImageableX(),
            pageFormat.getImageableY(),
            pageFormat.getImageableWidth(),
            pageFormat.getImageableHeight()
        );
        Point2D center = getPaperCenter();
        double minHalfWidth = Math.min(
            center.getX() - imageableRect.getX(),
            imageableRect.getX() + imageableRect.getWidth() - center.getX()
        );
        double minHalfHeight = Math.min(
            center.getY() - imageableRect.getY(),
            imageableRect.getY() + imageableRect.getHeight() - center.getY()
        );
        Rectangle2D fitRect = new Rectangle2D.Double(
            center.getX() - minHalfWidth,
            center.getY() - minHalfHeight,
            2 * minHalfWidth,
            2 * minHalfHeight
        );
        // Find the correct image bounds within this rectangle:
        imageRect = PreviewComponent.getImageBounds(fitRect, aspectRatio);
        notifyListeners();
    }

    void addListener(PrintLayoutModelListener listener) {
        listeners.add(listener);
    }

    void removeListener(PrintLayoutModelListener listener) {
        listeners.remove(listener);
    }

    private void setImageXSilent(double x) {
        imageRect.setRect(
            x,
            imageRect.getY(),
            imageRect.getWidth(),
            imageRect.getHeight()
        );
    }

    private void setImageYSilent(double y) {
        imageRect.setRect(
            imageRect.getX(),
            y,
            imageRect.getWidth(),
            imageRect.getHeight()
        );
    }

    private void setImageWidthSilent(double width) {
        imageRect.setRect(
            imageRect.getX(),
            imageRect.getY(),
            width,
            imageRect.getHeight()
        );
    }

    private void setImageHeightSilent(double height) {
        imageRect.setRect(
            imageRect.getX(),
            imageRect.getY(),
            imageRect.getWidth(),
            height
        );
    }

    private Point2D getPaperCenter() {
        // Don't rely on PageFormat getWidth() and getHeight(), which
        // corrrepsond to imageable area.  Instead, get the real paper center.
        Paper paper = pageFormat.getPaper();
        double width = paper.getWidth();
        double height = paper.getHeight();
        if (pageFormat.getOrientation() != 1) {
            // One of the landscape orientations:
            double temp = width;
            width = height;
            height = temp;
        }
        double centerX = width / 2;
        double centerY = height / 2;
        return new Point2D.Double(centerX, centerY);
    }

    private void centerByTranslation() {
        Point2D center = getPaperCenter();

        double x = center.getX() - imageRect.getWidth() / 2;
        double y = center.getY() - imageRect.getHeight() / 2;

        setImageXSilent(x);
        setImageYSilent(y);
    }

    private void notifyListeners() {
        for (PrintLayoutModelListener listener : listeners) {
            listener.layoutChanged(this);
        }
    }

    private final static String ProfileTag = "Profile";
    private final static String IntentTag = "Intent";
    private final static String PageTag = "Page";
    private final static String ImageRectTag = "ImageBounds";
    private final static String CenteredTag = "Centered";
    private final static String PpiTag = "ppi";

    private final static String ProfileNameTag = "Name";
    private final static String ProfilePathTag = "Path";

    public void save(XmlNode root) {
        root.setAttribute(CenteredTag, keepCentered? "True" : "False");

        root.setAttribute(IntentTag, intent.toString());

        root.setAttribute(PpiTag, Integer.toString(pixelsPerInch));

        if (profile != null) {
            XmlNode profileNode = root.addChild(ProfileTag);
            profileNode.setAttribute(ProfileNameTag, profile.getName());
            profileNode.setAttribute(ProfilePathTag, profile.getPath());
        }
        XmlNode pageNode = root.addChild(PageTag);
        savePageFormat(pageNode);

        XmlNode imageRectNode = root.addChild(ImageRectTag);
        imageRectNode.setAttribute("x", Double.toString(imageRect.getX()));
        imageRectNode.setAttribute("y", Double.toString(imageRect.getY()));
        imageRectNode.setAttribute("w", Double.toString(imageRect.getWidth()));
        imageRectNode.setAttribute("h", Double.toString(imageRect.getHeight()));

    }

    public void restore(XmlNode root) throws XMLException {
        keepCentered = Boolean.valueOf(root.getAttribute(CenteredTag));

        String intentName = root.getAttribute(IntentTag);
        RenderingIntent[] intents = RenderingIntent.getAll();
        intent = null;
        for (RenderingIntent i : intents) {
            if (intentName.equals(i.toString())) {
                intent = i;
            }
        }
        if (intent == null) {
            throw new XMLException("Invalid rendering intent: " + intentName);
        }
        if (root.hasAttribute(PpiTag)) {
            // ppi attribute added in LZN version 6
            pixelsPerInch = Integer.parseInt(root.getAttribute(PpiTag));
        }
        else {
            // arbitrary but sensible default, mostly backwards compatible
            pixelsPerInch = 300;
        }
        profile = null;
        XmlNode profileNode = null;
        try {
            profileNode = root.getChild(ProfileTag);
        }
        catch (XMLException e) {
            // OK, color profile is optional.
        }
        if (profileNode != null) {
            String profileName = profileNode.getAttribute(ProfileNameTag);
            String profilePath = profileNode.getAttribute(ProfilePathTag);
            profile = new ColorProfileInfo(profileName, profilePath);
        }
        XmlNode pageNode = root.getChild(PageTag);
        restorePageFormat(pageNode);

        XmlNode imageRectNode = root.getChild(ImageRectTag);
        try {
            double x = Double.valueOf(imageRectNode.getAttribute("x"));
            double y = Double.valueOf(imageRectNode.getAttribute("y"));
            double w = Double.valueOf(imageRectNode.getAttribute("w"));
            double h =
                Double.valueOf(imageRectNode.getAttribute("h"));
            imageRect = new Rectangle2D.Double(x, y, w, h);

            // The restored imageRect may not make sense with the current
            // imageWidth and imageHeight.
            // If the saved aspect ratio is far off, then we scaleToFit().
            // Otherwise we allow a little mismatch, to preserve the printed
            // image bounds.
            double restoredAspect =
                imageRect.getWidth() / imageRect.getHeight();
            double currentAspect = (imageWidth > 0 && imageHeight > 0) ?
                imageWidth / (double) imageHeight : restoredAspect;
            if (Math.abs(restoredAspect / currentAspect - 1) > 0.01) {
                scaleToFit();
            }
        }
        catch (NumberFormatException e) {
            throw new XMLException(
                "Can't interpret image bounds: " + e.getMessage()
            );
        }
    }

    private final static String OrientationTag = "Orientation";
    private final static String PaperTag = "Paper";

    private void savePageFormat(XmlNode pageRoot) {
        int orientation = pageFormat.getOrientation();
        pageRoot.setAttribute(OrientationTag, Integer.toString(orientation));

        Paper paper = pageFormat.getPaper();
        XmlNode paperNode = pageRoot.addChild(PaperTag);

        paperNode.setAttribute(
            "width", Double.toString(paper.getWidth())
        );
        paperNode.setAttribute(
            "height", Double.toString(paper.getHeight())
        );
        paperNode.setAttribute(
            "x", Double.toString(paper.getImageableX())
        );
        paperNode.setAttribute(
            "y", Double.toString(paper.getImageableY())
        );
        paperNode.setAttribute(
            "w", Double.toString(paper.getImageableWidth())
        );
        paperNode.setAttribute(
            "h", Double.toString(paper.getImageableHeight())
        );
    }

    private void restorePageFormat(XmlNode pageRoot) throws XMLException {
        int orientation =
            Integer.parseInt(pageRoot.getAttribute(OrientationTag));
        pageFormat.setOrientation(orientation);

        Paper paper = pageFormat.getPaper();
        XmlNode paperNode = pageRoot.getChild(PaperTag);

        // Backwards compatibility: releases 1.0.5 and earlier omitted
        // Paper width and height.
        if (paperNode.hasAttribute("width") && paperNode.hasAttribute("height")) {
            double width = Double.parseDouble(paperNode.getAttribute("width"));
            double height = Double.parseDouble(paperNode.getAttribute("height"));
            paper.setSize(width, height);
        }
        double x = Double.parseDouble(paperNode.getAttribute("x"));
        double y = Double.parseDouble(paperNode.getAttribute("y"));
        double w = Double.parseDouble(paperNode.getAttribute("w"));
        double h = Double.parseDouble(paperNode.getAttribute("h"));
        paper.setImageableArea(x, y, w, h);

        pageFormat.setPaper(paper);
    }
}
