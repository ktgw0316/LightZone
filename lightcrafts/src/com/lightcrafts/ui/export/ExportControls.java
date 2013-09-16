/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.export;

import com.lightcrafts.image.export.*;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.ColorProfileInfo;

import static com.lightcrafts.ui.export.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.WidePopupComboBox;
import com.lightcrafts.model.RenderingIntent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import static java.awt.color.ICC_Profile.icAbsoluteColorimetric;
import static java.awt.color.ICC_Profile.icPerceptual;
import static java.awt.color.ICC_Profile.icRelativeColorimetric;
import static java.awt.color.ICC_Profile.icSaturation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

/**
 * A collection of controls hooked up to an ImageExportOptions, including
 * special-casing to present either a quality control for JPEG or a bit-depth
 * control for TIFF.
 */

public class ExportControls extends JPanel {

    private static Collection<ColorProfileInfo> Profiles =
        Platform.getPlatform().getExportProfiles();

    private final static Map<String, Integer> Sizes =
        new LinkedHashMap<String, Integer>();

    public static final int defaultSaveSize = 1024;

    static {
        Sizes.put(LOCALE.get("FitWithin1024"), 1024);
        Sizes.put(LOCALE.get("FitWithin1536"), 1536);
        Sizes.put(LOCALE.get("FitWithin2048"), 2048);
        Sizes.put(LOCALE.get("FitWithin2560"), 2560);
        Sizes.put(LOCALE.get("DontLimit"), 0);
    }

    private ImageFileExportOptions options;

    private int ctrlCount;

    /**
     * Initialize the ExportControls according to the given ImageExportOptions.
     * If textResize is true, let the user specify arbitrary export resize
     * bounds.  If textResize is false, show a multiple-choice control instead.
     */
    public ExportControls(ImageExportOptions options, boolean textResize) {
        if (! (options instanceof ImageFileExportOptions)) {
            // Someone handed us an LZNImageType.ExportOptions.
            options = null;
        }
        this.options = (ImageFileExportOptions) options;
        if (options == null) {
            // This means: show no controls.
            return;
        }
        setLayout(new GridBagLayout());

        addColorControl();
        if (textResize) {
            addTextSizeControl();
        }
        else {
            addMultiSizeControl();
        }
        ImageType type = options.getImageType();
        if (type instanceof JPEGImageType) {
            addRenderingIntentControl();
            addQualityControl();
            addPpiControl();
        }
        if (type instanceof TIFFImageType) {
            addRenderingIntentControl();
            addCompressionControl();
            addDepthControl();
            addPpiControl();
        }
    }

    private void addColorControl() {
        final WidePopupComboBox combo = new WidePopupComboBox();
        List<ColorProfileInfo> profiles =
            ColorProfileInfo.arrangeForMenu(Profiles);
        Set<String> names = new HashSet<String>();
        for (ColorProfileInfo info : profiles) {
            if (info != null) {
                String name = info.getName();
                combo.addItem(name);
                names.add(name);
            }
            else {
                combo.addItem(null);
            }
        }
        // Set the default color profile from the current options:
        ColorProfileOption defaultOption = options.colorProfile;
        String name = defaultOption.getValue();
        if (! names.contains(name)) {
            combo.addItem(name);
        }
        combo.setSelectedItem(name);

        combo.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    if (event.getStateChange() == ItemEvent.SELECTED) {
                        String selected = (String) combo.getSelectedItem();
                        options.colorProfile.setValue(selected);
                    }
                }
            }
        );
        addLabelledControl(LOCALE.get("ColorProfileLabel"), combo);
    }

    private void addTextSizeControl() {
        ExportSizeFields fields = new ExportSizeFields(options);
        final JTextField xSize = fields.getXText();
        final JTextField ySize = fields.getYText();

        JButton reset = new JButton(LOCALE.get("ResetSizeButton"));
        reset.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    int width = options.originalWidth.getValue();
                    int height = options.originalHeight.getValue();
                    xSize.setText(Integer.toString(width));
                    ySize.setText(Integer.toString(height));
                }
            }
        );
        Box controls = Box.createHorizontalBox();
        controls.add(xSize);
        controls.add(Box.createHorizontalStrut(6));
        controls.add(new JLabel("x"));
        controls.add(Box.createHorizontalStrut(6));
        controls.add(ySize);
        controls.add(Box.createHorizontalStrut(6));
        controls.add(new JLabel(LOCALE.get("PixelsLabel")));
        controls.add(Box.createHorizontalStrut(12));
        controls.add(Box.createHorizontalGlue());
        controls.add(reset);

        addLabelledControl(LOCALE.get("ResizeToLabel"), controls);
    }

    private void addMultiSizeControl() {
        JComboBox sizes = new JComboBox();
        for (String name : Sizes.keySet()) {
            sizes.addItem(name);
        }
        sizes.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        int size = Sizes.get(e.getItem());
                        options.resizeWidth.setValue(size);
                        options.resizeHeight.setValue(size);
                    }
                }
            }
        );
        for (Map.Entry<String, Integer> entry : Sizes.entrySet()) {
            int entrySize = entry.getValue();
            if (options.resizeWidth.getValue() == entrySize) {
                String name = entry.getKey();
                sizes.setSelectedItem(name);
                break;
            }
        }
        sizes.setMaximumSize(sizes.getPreferredSize());

        Box controls = Box.createHorizontalBox();
        controls.add(sizes);
        controls.add(Box.createHorizontalGlue());

        addLabelledControl(LOCALE.get("ResizeToLabel"), controls);
    }

    private void addQualityControl() {
        int quality =
            ((JPEGImageType.ExportOptions) options).quality.getValue();

        final JLabel text = new JLabel("100");
        Dimension textSize = text.getPreferredSize();
        text.setHorizontalAlignment(SwingConstants.RIGHT);
        text.setPreferredSize(textSize);

        final JSlider slider = new JSlider(0, 100);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setValue(quality);
        text.setText(Integer.toString(quality));

        slider.addChangeListener(
            new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    int value = slider.getValue();
                    QualityOption option =
                        ((JPEGImageType.ExportOptions) options).quality;
                    option.setValue(value);
                    text.setText(Integer.toString(value));
                }
            }
        );
        Box textSlider = Box.createHorizontalBox();
        textSlider.add(slider);
        textSlider.add(Box.createHorizontalStrut(6));
        textSlider.add(text);

        addLabelledControl(LOCALE.get("QualityLabel"), textSlider);
    }

    private void addDepthControl() {
        final String bits8 = LOCALE.get("BitsPixel8");
        final String bits16 = LOCALE.get("BitsPixel16");

        final JComboBox combo = new JComboBox();
        combo.addItem(bits8);
        combo.addItem(bits16);

        int value = 0;
        if (options instanceof TIFFImageType.ExportOptions) {
            value = ((TIFFImageType.ExportOptions) options).
                bitsPerChannel.getValue();
        }
        if (value == 8) {
            combo.setSelectedItem(bits8);
        }
        if (value == 16) {
            combo.setSelectedItem(bits16);
        }
        combo.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    if (event.getStateChange() == ItemEvent.SELECTED) {

                        Object selected = combo.getSelectedItem();
                        int bits = (selected.equals(bits8)) ? 8 : 16;

                        if (options instanceof TIFFImageType.ExportOptions) {
                            ((TIFFImageType.ExportOptions) options).
                                bitsPerChannel.setValue(bits);
                        }
                    }
                }
            }
        );
        addLabelledControl(LOCALE.get("BitDepthLabel"), combo);
    }

    private void addPpiControl() {
        PpiField ppi = new PpiField(options);
        Box controls = Box.createHorizontalBox();
        controls.add(ppi);
        controls.add(Box.createHorizontalGlue());
        addLabelledControl("Pixels per inch", controls);
    }

    private void addRenderingIntentControl() {
        WidePopupComboBox renderingIntent = new WidePopupComboBox();
        renderingIntent.setFocusable(false);

        RenderingIntent[] intents = RenderingIntent.getAll();
        for (RenderingIntent intent : intents) {
            renderingIntent.addItem(intent);
        }
        int code = options.renderingIntent.getValue();
        boolean bpc = options.blackPointCompensation.getValue();
        RenderingIntent intent = convertIntToRenderingIntent(code, bpc);
        renderingIntent.setSelectedItem(intent);

        renderingIntent.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        WidePopupComboBox combo =
                            (WidePopupComboBox) e.getSource();
                        RenderingIntent intent =
                            (RenderingIntent) combo.getSelectedItem();
                        int code = convertRenderingIntentToInt(intent);
                        boolean bpc = convertRenderingIntentToBPC(intent);
                        options.renderingIntent.setValue(code);
                        options.blackPointCompensation.setValue(bpc);
                    }
                }
            }
        );
        addLabelledControl(LOCALE.get("RenderingIntentLabel"), renderingIntent);
    }

    private void addCompressionControl() {
        JCheckBox check = new JCheckBox("LZW");
        check.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    boolean selected =
                        event.getStateChange() == ItemEvent.SELECTED;
                    if (options instanceof TIFFImageType.ExportOptions) {
                        ((TIFFImageType.ExportOptions) options).
                            lzwCompression.setValue(selected);
                    }
                }
            }
        );
        boolean selected =
            ((TIFFImageType.ExportOptions) options).lzwCompression.getValue();
        check.setSelected(selected);

        addLabelledControl(LOCALE.get("CompressionLabel"), check);
    }

    void addLabelledControl(String name, JComponent control) {
        addLabelledControl(name, control, true);
    }

    private void addLabelledControl(
        String name, JComponent control, boolean enabled
    ) {
        JLabel label = new JLabel(name + ":");
        label.setHorizontalAlignment(SwingConstants.RIGHT);

        Box ctrlBox = Box.createHorizontalBox();
        ctrlBox.add(control);
        ctrlBox.add(Box.createHorizontalGlue());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = ctrlCount++;
        constraints.ipadx = 2;
        constraints.ipady = 2;
        constraints.insets = new Insets(2, 2, 2, 2);

        constraints.anchor = GridBagConstraints.EAST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        add(label, constraints);

        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        add(ctrlBox, constraints);

        if (! enabled) {
            label.setEnabled(false);
            control.setEnabled(false);
        }
    }

    // Convert a RenderingIntent into one of the ICC_Profile members,
    // "icPerceptual" etc., used in RenderingIntentOption.
    private static int convertRenderingIntentToInt(RenderingIntent intent) {
        if (intent == RenderingIntent.ABSOLUTE_COLORIMETRIC) {
            return icAbsoluteColorimetric;
        }
        if (intent == RenderingIntent.PERCEPTUAL) {
            return icPerceptual;
        }
        if (intent == RenderingIntent.RELATIVE_COLORIMETRIC) {
            return icRelativeColorimetric;
        }
        if (intent == RenderingIntent.RELATIVE_COLORIMETRIC_BP) {
            return icRelativeColorimetric;
        }
        if (intent == RenderingIntent.SATURATION) {
            return icSaturation;
        }
        return icPerceptual;
    }

    // Tell if the given RenderingIntent implies the blackPointCompensation
    // option in ImageFileExportOptions.
    private static boolean convertRenderingIntentToBPC(RenderingIntent intent) {
        return intent == RenderingIntent.RELATIVE_COLORIMETRIC_BP;
    }

    // Combine a ICC_Profile rendering intent code ("icPerceptual" etc.) with
    // a black-point-compensation flag to make a RenderingIntent.
    private static RenderingIntent convertIntToRenderingIntent(
        int code, boolean bpc
    ) {
        RenderingIntent intent;
        switch (code) {
            case icAbsoluteColorimetric:
                intent = RenderingIntent.ABSOLUTE_COLORIMETRIC;
                break;
            case icPerceptual:
                intent = RenderingIntent.PERCEPTUAL;
                break;
            case icRelativeColorimetric:
                if (bpc) {
                    intent = RenderingIntent.RELATIVE_COLORIMETRIC_BP;
                }
                else {
                    intent = RenderingIntent.RELATIVE_COLORIMETRIC;
                }
                break;
            case icSaturation:
                intent = RenderingIntent.SATURATION;
                break;
            default:
                intent = RenderingIntent.PERCEPTUAL;
        }
        return intent;
    }
}
