/*
 * Copyright (c) 2019. Masahiro Kitagawa
 */

package com.lightcrafts.ui.toolkit;

import jiconfont.IconCode;
import jiconfont.icons.FontAwesome;
import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons;
import jiconfont.swing.IconFontSwing;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Masahiro Kitagawa on 2017/01/02.
 */
public final class IconFontFactory {
    private static final Color DEFAULT_COLOR = Color.LIGHT_GRAY;
    private static final float DEFAULT_SIZE = 20f;

    public static Icon buildIcon(String name) {
        return buildIcon(name, DEFAULT_SIZE);
    }

    public static Icon buildIcon(String name, float size) {
        return buildIcon(name, size, DEFAULT_COLOR);
    }

    public static Icon buildIcon(String name, float size, @Nullable Color color) {
        val code = iconCodeMap.get(name);
        return buildIcon(code, size, color);
    }

    public static BufferedImage buildIconImage(String name, float size) {
        val icon = buildIcon(name, size);
        final int width = icon.getIconWidth();
        final int height = icon.getIconHeight();
        if (width < 0 || height < 0) {
            return null;
        }
        val bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        val g = bi.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return bi;
    }

    public static Icon buildIcon(@Nullable IconCode code, float size, @Nullable Color color) {
        return (code == null || color == null)
                ? new ImageIcon()
                : IconFontSwing.buildIcon(code, size, color);
    }

    static {
        IconFontSwing.register(GoogleMaterialDesignIcons.getIconFont());
        IconFontSwing.register(FontAwesome.getIconFont());
    }

    private static final Map<String, IconCode> iconCodeMap =
            new HashMap<String, IconCode>() {{
                // ui.browser.ctrls
                put("back", FontAwesome.ARROW_LEFT);
                put("forward", FontAwesome.ARROW_RIGHT);

                // put("path", );
                put("recent", FontAwesome.MAGIC);

                put("rotateLeft", GoogleMaterialDesignIcons.ROTATE_LEFT);
                put("rotateRight", GoogleMaterialDesignIcons.ROTATE_RIGHT);
                put("flipHoriz", GoogleMaterialDesignIcons.SWAP_HORIZ); // GoogleMaterialDesignIcons.FLIP
                put("flipVert", GoogleMaterialDesignIcons.SWAP_VERT);

                put("star", FontAwesome.STAR);

                put("trash", FontAwesome.TRASH);

                put("copy", FontAwesome.FILES_O);
                put("paste", FontAwesome.CLIPBOARD);

                put("thumbgrow", GoogleMaterialDesignIcons.VIEW_COMFY);
                put("thumbshrink", GoogleMaterialDesignIcons.VIEW_MODULE);

                put("sort_down", FontAwesome.SORT_AMOUNT_ASC);
                put("sort_up", FontAwesome.SORT_AMOUNT_DESC);

                // app.resources
                put("open", FontAwesome.FOLDER_OPEN);
                put("edit", FontAwesome.PENCIL_SQUARE_O); // GoogleMaterialDesignIcons.PALETTE
                put("print", FontAwesome.PRINT);
                put("undo", GoogleMaterialDesignIcons.UNDO);
                put("redo", GoogleMaterialDesignIcons.REDO);
                put("revert", FontAwesome.RECYCLE); // FontAwesome.REFRESH
                put("save", FontAwesome.FLOPPY_O); // GoogleMaterialDesignIcons.SAVE
                put("styles", GoogleMaterialDesignIcons.STYLE); // FontAwesome.COG
                put("send", FontAwesome.PAPER_PLANE_O); // FontAwesome.ENVELOPE
                // put("convert", );
                // put("stacked", );
                // put("unstacked", );
                put("info", FontAwesome.QUESTION_CIRCLE);

                // ui.operation.resources
                put("FilmGrain", FontAwesome.FILM);

                // ui.metadata2.UrgencyMetadata.UrgencyMetadataEntry
                put("square", FontAwesome.SQUARE);
            }};
}
