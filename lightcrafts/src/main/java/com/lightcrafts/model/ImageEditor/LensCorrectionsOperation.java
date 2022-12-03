/* Copyright (C) 2015- Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.DistortionOpImage;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.utils.Lensfun;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.List;

public class LensCorrectionsOperation extends BlendedOperation {
    private static final String MANUAL_MODE = "Manual_Correction";
    private static final String SEPARATOR = ": ";
    public static final String CAMERA_NAME = "Camera";
    public static final String LENS_NAME = "Lens";
    public static final String DISTORTION_K1 = "Main";
    public static final String DISTORTION_K2 = "Edge";
    public static final String TCA_R = "TCA_Red";
    public static final String TCA_B = "TCA_Blue";

    private static final float distortion_k1_scale = 1e-3f;
    private static final float distortion_k2_scale = 1e-3f;
    private static final float tca_scale = 1e-3f;

    private boolean manual_mode = false;
    private float distortion_k1 = 0;
    private float distortion_k2 = 0;
    private float tca_r_offset = 0;
    private float tca_b_offset = 0;

    private SliderConfig distortion_k1_config;
    private SliderConfig distortion_k2_config;
    private SliderConfig tca_r_offset_config;
    private SliderConfig tca_b_offset_config;

    private final ImageMetadata meta;

    @NotNull
    private String cameraMaker = "";

    @NotNull
    private String cameraModel = "";

    @NotNull
    private String lensMaker = "";

    @NotNull
    private String lensModel = "";

    private float focal = 0f;
    private float aperture = 0f;

    private Lensfun lf;

    static final OperationType type = new OperationTypeImpl("Lens Corrections");

    public LensCorrectionsOperation(Rendering rendering, OperationType type, ImageMetadata meta) {
        super(rendering, type);

        this.meta = meta;
        setCameraFromMetadata();
        setLensFromMetadata();

        lf = Lensfun.updateInstance(cameraMaker, cameraModel, lensMaker, lensModel, focal, aperture);

        addChoiceKey(CAMERA_NAME);
        addChoiceKey(LENS_NAME);
        addChoiceValue(CAMERA_NAME, "(Automatic)" + SEPARATOR);
        addChoiceValues(CAMERA_NAME, lf.getAllCameraNames());
        addChoiceValue(LENS_NAME, "(Automatic)" + SEPARATOR);
        addChoiceValues(LENS_NAME, lf.getLensNamesFor(cameraMaker, cameraModel));

        addCheckboxKey(MANUAL_MODE);
        setCheckboxValue(MANUAL_MODE, false);

        val format = new DecimalFormat("0.0");
        distortion_k1_config = new SliderConfig(-50, 50, distortion_k1, 1, false, format);
        distortion_k2_config = new SliderConfig(-50, 50, distortion_k2, 1, false, format);
        tca_r_offset_config  = new SliderConfig(-5, 5, tca_r_offset, 0.1, false, format);
        tca_b_offset_config  = new SliderConfig(-5, 5, tca_b_offset, 0.1, false, format);
        addSliderKeys();
    }

    @Override
    public void dispose() {
        super.dispose();
        lf.dispose();
        lf = null;
    }

    private void addSliderKeys() {
        addSliderKey(DISTORTION_K1);
        addSliderKey(DISTORTION_K2);
        addSliderKey(TCA_R);
        addSliderKey(TCA_B);
        setSliderConfig(DISTORTION_K1, distortion_k1_config);
        setSliderConfig(DISTORTION_K2, distortion_k2_config);
        setSliderConfig(TCA_R,         tca_r_offset_config );
        setSliderConfig(TCA_B,         tca_b_offset_config );
        settingsChanged();
    }

    private void setCameraFromMetadata() {
        val make = (meta == null) ? null : meta.getCameraMake(false);
        if (make == null || make.isEmpty()) {
            cameraMaker = "";
            cameraModel = "";
            return;
        }

        cameraMaker = make.equalsIgnoreCase("RICOH") ? "PENTAX" : make;
        val makeModel = meta.getCameraMake(true);
        if (makeModel == null || makeModel.isEmpty()) {
            cameraModel = "";
            return;
        }

        // Remove long maker name, such as "RICOH IMAGING COMPANY, LTD." or
        // "OLYMPUS IMAGING CORP." from the makeModel.
        val ss = new String[] {"LTD.", "CORP."};
        for (val s : ss) {
            int idx = makeModel.toUpperCase().lastIndexOf(s);
            if (idx > 0) {
                cameraModel = makeModel.substring(idx + s.length()).trim();
                return;
            }
        }
        cameraModel = makeModel.substring(make.length());
    }

    private void setLensFromMetadata() {
        val lens = (meta == null) ? null : meta.getLens();
        if (lens == null) {
            lensModel = "";
            focal     = 0f;
            aperture  = 0f;
        }
        else {
            lensModel = lens;
            focal     = meta.getFocalLength();
            aperture  = meta.getAperture();
        }
        lensMaker = "";
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public boolean neutralDefault() {
        return false;
    }

    @Override
    public void setCheckboxValue(String key, boolean value) {
        if (! key.equals(MANUAL_MODE)) {
            return;
        }
        manual_mode = value;
        super.setCheckboxValue(key, value);
    }

    @Override
    public void setChoiceValue(String key, String value) {
        if (value == null) {
            return;
        }

        if (key.equals(CAMERA_NAME)) {
            val names = value.split(SEPARATOR, 2);
            cameraModel = names[1];
            if (cameraModel.isEmpty()) {
                setCameraFromMetadata();
            } else {
                cameraMaker = names[0];
            }
            updateLenses();
        } else if (key.equals(LENS_NAME)) {
            val names = value.split(SEPARATOR, 2);
            lensModel = names[1];
            if (lensModel.isEmpty()) {
                setLensFromMetadata();
            } else {
                lensMaker = names[0];
            }
        } else {
            return;
        }
        super.setChoiceValue(key, value);
    }

    private void updateLenses() {
        clearChoiceValues(LENS_NAME);
        final List<String> values = (cameraModel.isEmpty())
                ? lf.getAllLensNames()
                : lf.getLensNamesFor(cameraMaker, cameraModel);
        addChoiceValue(LENS_NAME, "(Automatic)" + SEPARATOR);
        addChoiceValues(LENS_NAME, values);
    }

    @Override
    public void setSliderValue(String key, double value) {
        value = roundValue(key, value);

        if (key.equals(DISTORTION_K1) && distortion_k1 != value)
            distortion_k1 = (float) value;
        else if (key.equals(DISTORTION_K2) && distortion_k2 != value)
            distortion_k2 = (float) value;
        else if (key.equals(TCA_R) && tca_r_offset != value)
            tca_r_offset = (float) value;
        else if (key.equals(TCA_B) && tca_b_offset != value)
            tca_b_offset = (float) value;
        else
            return;

        super.setSliderValue(key, value);
    }

    private class LensCorrections extends BlendedTransform {
        LensCorrections(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            val sourceBounds = rendering.getSourceBounds();
            int fullWidth  = sourceBounds.width;
            int fullHeight = sourceBounds.height;

            val transform = rendering.getTransform();
            val scaleFactor = rendering.getScaleFactor();
            if (scaleFactor < 1) {
                // Append pyramid ratio
                fullWidth  *= scaleFactor;
                fullHeight *= scaleFactor;

                transform.concatenate(AffineTransform.getScaleInstance(1 / scaleFactor, 1 / scaleFactor));
            }

            final PlanarImage front;
            if (manual_mode && Math.abs(distortion_k1) < 1e-3 && Math.abs(distortion_k2) < 1e-3
                            && Math.abs(tca_r_offset)  < 1e-3 && Math.abs(tca_b_offset)  < 1e-3) {
                front = back;
            }
            else {
                if (manual_mode) {
                    lf = Lensfun.updateInstance(cameraMaker, cameraModel, "", "", focal, aperture)
                            .updateModifier(
                                    fullWidth, fullHeight,
                                    distortion_k1_scale * distortion_k1,
                                    distortion_k2_scale * distortion_k2,
                                    1 + tca_scale * tca_r_offset,
                                    1 + tca_scale * tca_b_offset);
                }
                else {
                    lf = Lensfun.updateInstance(cameraMaker, cameraModel, lensMaker, lensModel, focal, aperture)
                            .updateModifier(fullWidth, fullHeight);
                }
                val shiftX = back.getMinX();
                val shiftY = back.getMinY();
                val shifted = JAI.create("affine", back,
                        AffineTransform.getTranslateInstance(-shiftX, -shiftY));
                val borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
                val corrected = new DistortionOpImage(shifted, JAIContext.fileCacheHint, borderExtender, lf);
                front = JAI.create("affine", corrected,
                        AffineTransform.getTranslateInstance(shiftX, shiftY));
            }
            front.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
            return front;
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new LensCorrections(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }

    @Override
    public boolean hasFooter() {
        return false;
    }
}
