/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import com.lightcrafts.ui.operation.OpActions;
import com.lightcrafts.platform.Platform;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.media.jai.*;
import javax.media.jai.operator.CompositeDescriptor;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;

public class ButtonIcon implements Icon {
    public static final Color UNSELECTED_COLOR = new Color(210, 210, 210); // new Color(238, 238, 238);
    public static final Color PRESSED_COLOR = new Color(170, 170, 170);
    public static final Color SELECTED_COLOR = new Color(170, 170, 170);
    public static final Color DISABLED_COLOR = new Color(118, 118, 118);

    public static final int UNSELECTED = 0;
    public static final int SELECTED = 1;
    public static final int PRESSED = 2;
    public static final int DISABLED = 3;

    public static final int PLAIN_BUTTON = 0;
    public static final int LEFT_BUTTON = 1;
    public static final int RIGHT_BUTTON = 2;
    public static final int CENTER_BUTTON = 3;

    private static final boolean useResource = true; // Platform.getType() != Platform.MacOSX;

    private static boolean isJava14 = System.getProperty("java.version").startsWith("1.4");
    private static boolean isOSXTiger = Platform.getType() == Platform.MacOSX && System.getProperty("os.version").startsWith("10.4");
    private static boolean isOSXPanther = Platform.getType() == Platform.MacOSX && System.getProperty("os.version").startsWith("10.3");

    private static BufferedImage leftPieceNormal;
    private static BufferedImage centerPieceNormal;
    private static BufferedImage rightPieceNormal;
    private static BufferedImage rightEdgeNormal;

    private static BufferedImage leftPiecePressed;
    private static BufferedImage centerPiecePressed;
    private static BufferedImage rightPiecePressed;
    private static BufferedImage rightEdgePressed;

    private static BufferedImage leftPieceSelected;
    private static BufferedImage centerPieceSelected;
    private static BufferedImage rightPieceSelected;
    private static BufferedImage rightEdgeSelected;

    private static BufferedImage leftPieceDisabled;
    private static BufferedImage centerPieceDisabled;
    private static BufferedImage rightPieceDisabled;
    private static BufferedImage rightEdgeDisabled;

    static {
        try {
            leftPieceNormal = ImageIO.read(ButtonIcon.class.getResource("resources/LeftPiece.png"));
            centerPieceNormal = ImageIO.read(ButtonIcon.class.getResource("resources/CenterPiece.png"));
            rightPieceNormal = ImageIO.read(ButtonIcon.class.getResource("resources/RightPiece.png"));
            rightEdgeNormal = ImageIO.read(ButtonIcon.class.getResource("resources/RightEdge.png"));

            leftPiecePressed = ImageIO.read(ButtonIcon.class.getResource("resources/LeftPiecePressed.png"));
            centerPiecePressed = ImageIO.read(ButtonIcon.class.getResource("resources/CenterPiecePressed.png"));
            rightPiecePressed = ImageIO.read(ButtonIcon.class.getResource("resources/RightPiecePressed.png"));
            rightEdgePressed = ImageIO.read(ButtonIcon.class.getResource("resources/RightEdgePressed.png"));

            leftPieceSelected = ImageIO.read(ButtonIcon.class.getResource("resources/LeftPieceSelected.png"));
            centerPieceSelected = ImageIO.read(ButtonIcon.class.getResource("resources/CenterPieceSelected.png"));
            rightPieceSelected = ImageIO.read(ButtonIcon.class.getResource("resources/RightPieceSelected.png"));
            rightEdgeSelected = ImageIO.read(ButtonIcon.class.getResource("resources/RightEdgeSelected.png"));

            leftPieceDisabled = ImageIO.read(ButtonIcon.class.getResource("resources/LeftPieceDisabled.png"));
            centerPieceDisabled = ImageIO.read(ButtonIcon.class.getResource("resources/CenterPieceDisabled.png"));
            rightPieceDisabled = ImageIO.read(ButtonIcon.class.getResource("resources/RightPieceDisabled.png"));
            rightEdgeDisabled = ImageIO.read(ButtonIcon.class.getResource("resources/RightEdgeDisabled.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // private final Color baseColor;
    private int state;

    private BufferedImage image;

    private static Color colorFromState(int state) {
        switch (state) {
            case DISABLED:
                return UNSELECTED_COLOR; // DISABLED_COLOR;
            case SELECTED:
                return SELECTED_COLOR;
            case PRESSED:
                return PRESSED_COLOR;
            case UNSELECTED:
                return UNSELECTED_COLOR;
            default:
                throw new IllegalArgumentException("Bad Button State");
        }
    }

    private static BufferedImage makeDisabled(BufferedImage image) {
        RescaleOp rescale1 = new RescaleOp(new float[]{.75f, .75f, .75f, 1},
                                          new float[]{0, 0, 0, 0},
                                          null);
        float lightness = 0.3f;
        RescaleOp rescale2 = new RescaleOp(new float[]{1 - lightness, 1 - lightness, 1 - lightness, 1},
                                          new float[]{lightness * 255, lightness * 255, lightness * 255, 0},
                                          null);
        return rescale2.filter(rescale1.filter(image, null), null);
    }

    ButtonIcon(int width, int state, int buttonType) {
        this.state = state;
        image = basicButton(width, 20, buttonType);
    }

    public ButtonIcon(BufferedImage icon, int state, int buttonType) {
        this(icon, 0, state, buttonType);
    }

    public ButtonIcon(BufferedImage icon, int width, int state, int buttonType) {
        this.state = state;
        image = iconButton(icon, width, buttonType);
    }

    public ButtonIcon(String text, int state, int buttonType) {
        this(text, 0, state, buttonType);
    }

    public ButtonIcon(String text, int width, int state, int buttonType) {
        this.state = state;
        image = textButton(text, width, buttonType);
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        ((Graphics2D) g).drawRenderedImage(image, AffineTransform.getTranslateInstance(x, y));
    }

    public int getIconWidth() {
        return image.getWidth();
    }

    public int getIconHeight() {
        return image.getHeight();
    }

    static final float darkening = 0.85f;
    static final float litening = 1.03f;

    static final Color multiply(Color color, float factor) {
        return new Color((int) (color.getRed() * factor),
                         (int) (color.getGreen() * factor),
                         (int) (color.getBlue() * factor));
    }

    BufferedImage basicButtonFromResouce( int sizeX, int sizeY, int buttonType ) {
        sizeX += 6;
        sizeY += 7;

        if (buttonType != PLAIN_BUTTON) {
            switch (buttonType) {
                case LEFT_BUTTON:
                    sizeX += 2 + 1;
                    break;
                case CENTER_BUTTON:
                    sizeX += 2 + 2 + 1;
                    break;
                case RIGHT_BUTTON:
                    sizeX += 2;
                    break;
                default:
                    throw new IllegalArgumentException("unknown button type: " + buttonType);
            }
        }

        int width = sizeX;
        if (buttonType != PLAIN_BUTTON) {
            switch (buttonType) {
                case LEFT_BUTTON:
                    width -= 5;
                    break;
                case CENTER_BUTTON:
                    width -= 9;
                    break;
                case RIGHT_BUTTON:
                    width -= 5;
                    break;
                default:
                    throw new IllegalArgumentException("unknown button type: " + buttonType);
            }
        }

        BufferedImage leftPiece;
        BufferedImage centerPiece;
        BufferedImage rightPiece;
        BufferedImage rightEdge;

        switch (state) {
            case UNSELECTED:
                leftPiece = leftPieceNormal;
                centerPiece = centerPieceNormal;
                rightPiece = rightPieceNormal;
                rightEdge = rightEdgeNormal;
                break;
            case SELECTED:
                leftPiece = leftPieceSelected;
                centerPiece = centerPieceSelected;
                rightPiece = rightPieceSelected;
                rightEdge = rightEdgeSelected;
                break;
            case PRESSED:
                leftPiece = leftPiecePressed;
                centerPiece = centerPiecePressed;
                rightPiece = rightPiecePressed;
                rightEdge = rightEdgePressed;
                break;
            case DISABLED:
                leftPiece = leftPieceDisabled;
                centerPiece = centerPieceDisabled;
                rightPiece = rightPieceDisabled;
                rightEdge = rightEdgeDisabled;
                break;
            default:
                throw new IllegalArgumentException("Bad Button State");
        }

        BufferedImage buttonTile = new BufferedImage(width, sizeY, BufferedImage.TYPE_INT_ARGB);

        if (buttonType == PLAIN_BUTTON || buttonType == LEFT_BUTTON)
            buttonTile.setData(leftPiece.getData());

        int leftPadding = (buttonType == PLAIN_BUTTON || buttonType == LEFT_BUTTON) ? 5 : 0;

        for (int i = 0; i <= sizeX - leftPadding - 5; i++)
            buttonTile.setData(centerPiece.getData().createTranslatedChild(i+leftPadding, 0));

        if (buttonType == LEFT_BUTTON || buttonType == CENTER_BUTTON)
            buttonTile.setData(rightEdge.getData().createTranslatedChild(width-1, 0));

        if (buttonType == PLAIN_BUTTON || buttonType == RIGHT_BUTTON)
            buttonTile.setData(rightPiece.getData().createTranslatedChild(width-5+1, 0));
        return buttonTile;
    }

    public BufferedImage basicButton( int sizeX, int sizeY, int buttonType ) {
        if (buttonType != PLAIN_BUTTON) {
            switch (buttonType) {
                case LEFT_BUTTON:
                    sizeX += 2 + 1;
                    break;
                case CENTER_BUTTON:
                    sizeX += 2 + 2 + 1;
                    break;
                case RIGHT_BUTTON:
                    sizeX += 2;
                    break;
                default:
                    throw new IllegalArgumentException("unknown button type: " + buttonType);
            }
        }

        BufferedImage buttonTile = new BufferedImage(sizeX+1, sizeY+1, BufferedImage.TYPE_INT_ARGB);
        final Color baseColor = colorFromState(state);

        Color rimColor = multiply(baseColor, litening);
        Color rimColorDark = multiply(rimColor, darkening);
        Color baseColorDark = multiply(baseColor, darkening);

        Graphics2D g = (Graphics2D) buttonTile.getGraphics();
        g.setPaint(new GradientPaint(0, 0, rimColor, 0, sizeY, rimColorDark));
        g.fillRoundRect(0, 0, sizeX, sizeY, 6, 6);
        g.setPaint(new GradientPaint(0, 1, baseColor, 0, sizeY-2, baseColorDark));
        g.fillRoundRect(1, 1, sizeX-2, sizeY-2, 6, 6);
        if (buttonType != PLAIN_BUTTON) {
            Color baseColorDarker = multiply(baseColorDark, darkening);
            g.setPaint(null);
            g.setColor(baseColorDarker);
            switch (buttonType) {
                case LEFT_BUTTON:
                    g.drawLine(sizeX-2, 0, sizeX-2, sizeY);
                    break;
                case CENTER_BUTTON:
                    g.drawLine(sizeX-2, 0, sizeX-2, sizeY);
                    break;
                case RIGHT_BUTTON:
                    break;
                default:
                    throw new IllegalArgumentException("unknown button type: " + buttonType);
            }
        }
        g.dispose();

        if (state == DISABLED)
            buttonTile = makeDisabled(buttonTile);

        ShadowFactory sf = new ShadowFactory(3, 0.8f, new Color(128, 128, 128));
        sf.setRenderingHint(ShadowFactory.KEY_BLUR_QUALITY, ShadowFactory.VALUE_BLUR_QUALITY_HIGH);
        BufferedImage shadowedTile = sf.createShadow(buttonTile);

        g = (Graphics2D) shadowedTile.getGraphics();
        g.drawRenderedImage(buttonTile, AffineTransform.getTranslateInstance(3, 2));
        g.dispose();

        if (buttonType != PLAIN_BUTTON) {
            Raster clipped;
            switch (buttonType) {
                case LEFT_BUTTON:
                    clipped = shadowedTile.getData(new Rectangle(0, 0,
                                                                 shadowedTile.getWidth() - 5,
                                                                 shadowedTile.getHeight()));
                    break;
                case CENTER_BUTTON:
                    clipped = shadowedTile.getData(new Rectangle(5, 0,
                                                                 shadowedTile.getWidth() - 10,
                                                                 shadowedTile.getHeight())).createTranslatedChild(0, 0);
                    break;
                case RIGHT_BUTTON:
                    clipped = shadowedTile.getData(new Rectangle(5, 0,
                                                                 shadowedTile.getWidth() - 5,
                                                                 shadowedTile.getHeight())).createTranslatedChild(0, 0);
                    break;
                default:
                    throw new IllegalArgumentException("unknown button type: " + buttonType);
            }
            shadowedTile = new BufferedImage(shadowedTile.getColorModel(), (WritableRaster) clipped, false, null);
        }
        return shadowedTile;
    }

    public BufferedImage textButton(String text, int width, int buttonType) {
        Font font = new Font("Lucida Grande", Font.PLAIN, 12);
        TextLayout layout = new TextLayout(text, font, new FontRenderContext(null, true, true));

        Rectangle2D bounds = layout.getBounds();
        if (width == 0)
            width = (int) bounds.getWidth() + 16;
        int height = 20; // (int) bounds.getHeight() + 8;

        BufferedImage buttonTile = useResource
                                   ? basicButtonFromResouce(width, height, buttonType)
                                   : basicButton(width, height, buttonType);

        Graphics2D g = (Graphics2D) buttonTile.getGraphics();
        if (state == DISABLED)
            g.setColor(Color.gray);
        else
            g.setColor(Color.BLACK);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        float x = (float) (buttonTile.getWidth() / 2.0 - bounds.getWidth() / 2.0) + 1;
        float y = (float) (buttonTile.getHeight() / 2.0 - bounds.getHeight() / 2.0) - 1;

        if (!isOSXPanther) {
            // Workaround for broken TextLayout on Panther
            y += layout.getAscent() - layout.getDescent();
            layout.draw(g, x, y);
        } else {
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            bounds = fm.getStringBounds(text, g);
            y += fm.getAscent() - layout.getDescent();
            g.drawString(text, x, y);
        }

        boolean debug = false;
        if (debug)
            g.drawRect((int) (x + bounds.getX()-1), (int) (y + bounds.getY()-1),
                       (int) (bounds.getWidth()+2), (int) (bounds.getHeight()+2));

        g.dispose();
        return buttonTile;
    }

    public BufferedImage iconButton(BufferedImage icon, int width, int buttonType) {
        if (state == DISABLED)
            icon = makeDisabled(icon);

        int maxSize = 18;

        float scale;
        if (icon.getHeight() > maxSize)
            scale = maxSize / (float) icon.getHeight();
        else
            scale = 1;

        if (width == -1)
            width = Math.max(26, (int) (scale * icon.getWidth()) + 14);
        else if (width == 0)
            width = Math.max(26, (int) (scale * icon.getWidth()));

        BufferedImage buttonTile = useResource
                                   ? basicButtonFromResouce(width, 20, buttonType)
                                   : basicButton(width, 20, buttonType);

        RenderingHints extenderHint = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                         BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);

        float x = (int) (buttonTile.getWidth() / 2f - scale * icon.getWidth() / 2f + 0.5f);
        float y = (int) (buttonTile.getHeight() / 2f - scale * icon.getHeight() / 2f - 0.5f);

        if (buttonType != PLAIN_BUTTON) {
            switch (buttonType) {
                case LEFT_BUTTON:
                    x += 1;
                    break;
                case CENTER_BUTTON:
                    x -= 1;
                    break;
                case RIGHT_BUTTON:
                    x -= 2;
                    break;
                default:
                    throw new IllegalArgumentException("unknown button type: " + buttonType);
            }
        }

        AffineTransform transform = AffineTransform.getTranslateInstance(x, y);
        if (scale != 1) {
            float scaleX = (float) Math.floor(scale * icon.getWidth()) / (float) icon.getWidth();
            float scaleY = (float) Math.floor(scale * icon.getHeight()) / (float) icon.getHeight();
            transform.concatenate(AffineTransform.getScaleInstance(scaleX, scaleY));
        }

        // Render the scaled button image.
        //
        // We use Java2D for the scaling, unless we happen to be on Mac OSX
        // with Java 1.4, in which case we use JAI.

        if (! (isOSXTiger && isJava14)) {
            Graphics2D g = (Graphics2D) buttonTile.getGraphics();
            BufferedImageOp op = new AffineTransformOp(
                transform, AffineTransformOp.TYPE_BILINEAR
            );
            g.drawImage(icon, op, 0, 0);
            g.dispose();
            return buttonTile;
        } else {

            // This block is just a JAI way to make a scaled rendering of the
            // given button image using JAI.

            RenderedImage adjustedIcon;
            if (!transform.isIdentity()) {
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(icon);
                pb.add(transform);
                pb.add(interp);
                adjustedIcon = JAI.create("Affine", pb, extenderHint);
            } else
                adjustedIcon = icon;

            ParameterBlock pb = new ParameterBlock();
            pb.add((float) buttonTile.getWidth());
            pb.add((float) buttonTile.getHeight());
            pb.add(new Byte[]{new Byte((byte) 0),
                              new Byte((byte) 0),
                              new Byte((byte) 0),
                              new Byte((byte) 0)});
            final RenderedOp zero = JAI.create("Constant", pb, null);

            final RenderedOp extendedIcon = JAI.create("Overlay", zero, adjustedIcon);

            final BufferedImage allRGB = new BufferedImage(buttonTile.getWidth(), buttonTile.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            allRGB.setData(buttonTile.getData().createChild(0, 0,
                buttonTile.getWidth(), buttonTile.getHeight(),
                0, 0, new int[]{0, 1, 2}));

            final BufferedImage allAlpha = new BufferedImage(buttonTile.getWidth(), buttonTile.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            allAlpha.setData(buttonTile.getData().createChild(0, 0,
                buttonTile.getWidth(), buttonTile.getHeight(),
                0, 0, new int[]{3}));

            final BufferedImage iconRGB = new BufferedImage(extendedIcon.getWidth(), extendedIcon.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            iconRGB.setData(extendedIcon.getData().createChild(0, 0,
                extendedIcon.getWidth(), extendedIcon.getHeight(),
                0, 0, new int[]{0, 1, 2}));

            final BufferedImage iconAlpha = new BufferedImage(extendedIcon.getWidth(), extendedIcon.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            iconAlpha.setData(extendedIcon.getData().createChild(0, 0,
                extendedIcon.getWidth(), extendedIcon.getHeight(),
                0, 0, new int[]{3}));

            pb = new ParameterBlock();
            pb.addSource(iconRGB);
            pb.addSource(allRGB);
            pb.add(iconAlpha);
            pb.add(allAlpha);
            pb.add(new Boolean(false));
            pb.add(CompositeDescriptor.DESTINATION_ALPHA_LAST);
            return JAI.create("Composite", pb, null).getAsBufferedImage();
        }
    }

    public static void main(String args[]) throws Exception {
        if (true) {
            new File("../../Buttons").mkdir();

            ButtonIcon buttonTile = new ButtonIcon(25, UNSELECTED, LEFT_BUTTON);
            BufferedImage buttonImage = buttonTile.image;

            ImageIO.write(buttonImage, "PNG", new File("../../Buttons/LeftButtonImage.png"));

            buttonTile = new ButtonIcon(25, UNSELECTED, RIGHT_BUTTON);
            buttonImage = buttonTile.image;

            ImageIO.write(buttonImage, "PNG", new File("../../Buttons/RightButtonImage.png"));

            buttonTile = new ButtonIcon(25, UNSELECTED, CENTER_BUTTON);
            buttonImage = buttonTile.image;

            ImageIO.write(buttonImage, "PNG", new File("../../Buttons/CenterButtonImage.png"));

            buttonTile = new ButtonIcon(25, UNSELECTED, PLAIN_BUTTON);
            buttonImage = buttonTile.image;

            ImageIO.write(buttonImage, "PNG", new File("../../Buttons/ButtonImage.png"));

            BufferedImage leftPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(0, 0, 5, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            BufferedImage rightPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(buttonImage.getWidth() - 5, 0, 5, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            BufferedImage centerPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(5, 0, 1, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);

            ImageIO.write(leftPiece, "PNG", new File("../../Buttons/LeftPiece.png"));
            ImageIO.write(rightPiece, "PNG", new File("../../Buttons/RightPiece.png"));
            ImageIO.write(centerPiece, "PNG", new File("../../Buttons/CenterPiece.png"));

            buttonTile = new ButtonIcon(25, UNSELECTED, LEFT_BUTTON);
            buttonImage = buttonTile.image;
            BufferedImage rightEdge = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(buttonImage.getWidth()-1, 0, 1, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            ImageIO.write(rightEdge, "PNG", new File("../../Buttons/RightEdge.png"));

            buttonTile = new ButtonIcon(25, PRESSED, PLAIN_BUTTON);
            buttonImage = buttonTile.image;
            leftPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(0, 0, 5, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            rightPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(buttonImage.getWidth() - 5, 0, 5, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            centerPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(5, 0, 1, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);

            ImageIO.write(leftPiece, "PNG", new File("../../Buttons/LeftPiecePressed.png"));
            ImageIO.write(rightPiece, "PNG", new File("../../Buttons/RightPiecePressed.png"));
            ImageIO.write(centerPiece, "PNG", new File("../../Buttons/CenterPiecePressed.png"));

            buttonTile = new ButtonIcon(25, PRESSED, LEFT_BUTTON);
            buttonImage = buttonTile.image;
            rightEdge = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(buttonImage.getWidth()-1, 0, 1, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            ImageIO.write(rightEdge, "PNG", new File("../../Buttons/RightEdgePressed.png"));


            buttonTile = new ButtonIcon(25, SELECTED, PLAIN_BUTTON);
            buttonImage = buttonTile.image;
            leftPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(0, 0, 5, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            rightPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(buttonImage.getWidth() - 5, 0, 5, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            centerPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(5, 0, 1, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);

            ImageIO.write(leftPiece, "PNG", new File("../../Buttons/LeftPieceSelected.png"));
            ImageIO.write(rightPiece, "PNG", new File("../../Buttons/RightPieceSelected.png"));
            ImageIO.write(centerPiece, "PNG", new File("../../Buttons/CenterPieceSelected.png"));

            buttonTile = new ButtonIcon(25, SELECTED, LEFT_BUTTON);
            buttonImage = buttonTile.image;
            rightEdge = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(buttonImage.getWidth()-1, 0, 1, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            ImageIO.write(rightEdge, "PNG", new File("../../Buttons/RightEdgeSelected.png"));


            buttonTile = new ButtonIcon(25, DISABLED, PLAIN_BUTTON);
            buttonImage = buttonTile.image;
            leftPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(0, 0, 5, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            rightPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(buttonImage.getWidth() - 5, 0, 5, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            centerPiece = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(5, 0, 1, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);

            ImageIO.write(leftPiece, "PNG", new File("../../Buttons/LeftPieceDisabled.png"));
            ImageIO.write(rightPiece, "PNG", new File("../../Buttons/RightPieceDisabled.png"));
            ImageIO.write(centerPiece, "PNG", new File("../../Buttons/CenterPieceDisabled.png"));

            buttonTile = new ButtonIcon(25, DISABLED, LEFT_BUTTON);
            buttonImage = buttonTile.image;
            rightEdge = new BufferedImage(
                    buttonImage.getColorModel(),
                    (WritableRaster) buttonImage.getData().createChild(buttonImage.getWidth()-1, 0, 1, buttonImage.getHeight(),
                                                                       0, 0, new int[] {0, 1, 2, 3}),
                    false, null);
            ImageIO.write(rightEdge, "PNG", new File("../../Buttons/RightEdgeDisabled.png"));
        }

        BufferedImage icon = null;

        try {
            icon = ImageIO.read(OpActions.class.getResource("resources/hue_saturation.png"));
        } catch (Exception e) { }

        JButton button = new ImageOnlyButton(new ButtonIcon(icon, UNSELECTED, PLAIN_BUTTON),
                                             new ButtonIcon(icon, SELECTED, PLAIN_BUTTON));

        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        panel.setBackground(new Color(211, 211, 211));
        panel.add(button);
        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(200, 200);
        frame.show();
    }
}
