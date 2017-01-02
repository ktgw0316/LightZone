package com.lightcrafts.ui.toolkit;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

import jiconfont.IconCode;
import jiconfont.icons.FontAwesome;
import jiconfont.icons.GoogleMaterialDesignIcons;
import jiconfont.swing.IconFontSwing;
import lombok.val;

/**
 * Created by Masahiro Kitagawa on 2017/01/02.
 */
public final class IconFontFactory {
    private static final Color COLOR = Color.LIGHT_GRAY;
    private static final float SIZE = 20f;

    public static Icon buildIcon(String name) {
        val code = iconCodeMap.get(name);
        return code != null ? buildIcon(code) : new ImageIcon();
    }

    public static Icon buildIcon(IconCode code) {
        return IconFontSwing.buildIcon(code, SIZE, COLOR);
    }

    public static Icon buildHighlightedIcon(IconCode code) {
        return IconFontSwing.buildIcon(code, SIZE, COLOR.brighter());
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
            }};
}
