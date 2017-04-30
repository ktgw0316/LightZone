/* Copyright (C) 2015- Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.List;

import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.DistortionOpImage;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;
import com.lightcrafts.utils.Lensfun;

import lombok.val;

public class LensCorrectionsOperation extends BlendedOperation {
    private static final String MANUAL_MODE = "Manual_Correction";
    private static final String SEPARATOR = ": ";
    private static final String CAMERA_NAME = "Camera";
    private static final String LENS_NAME = "Lens";
    private static final String DISTORTION_K1 = "Main";
    private static final String DISTORTION_K2 = "Edge";
    private static final String TCA_R = "TCA_Red";
    private static final String TCA_B = "TCA_Blue";

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
    private String cameraMaker = "";
    private String cameraModel = "";
    private String lensMaker = "";
    private String lensModel = "";
    private float  focal = 0f;
    private float  aperture = 0f;

    static final OperationType type = new OperationTypeImpl("Lens Corrections");

    public LensCorrectionsOperation(Rendering rendering, OperationType type, ImageMetadata meta) {
        super(rendering, type);

        this.meta = meta;
        setCameraFromMetadata();
        setLensFromMetadata();

        addChoiceKey(CAMERA_NAME);
        addChoiceKey(LENS_NAME);
        addChoiceValue(CAMERA_NAME, "(Automatic)" + SEPARATOR);
        addChoiceValues(CAMERA_NAME, Lensfun.getAllCameraNames());
        addChoiceValue(LENS_NAME, "(Automatic)" + SEPARATOR);
        addChoiceValues(LENS_NAME, Lensfun.getAllLensNames());

        addCheckboxKey(MANUAL_MODE);
        setCheckboxValue(MANUAL_MODE, false);

        val format = new DecimalFormat("0.0");
        distortion_k1_config = new SliderConfig(-200, 200, distortion_k1, 1, false, format);
        distortion_k2_config = new SliderConfig(-200, 200, distortion_k2, 1, false, format);
        tca_r_offset_config  = new SliderConfig(-2, 2, tca_r_offset, 0.1, false, format);
        tca_b_offset_config  = new SliderConfig(-2, 2, tca_b_offset, 0.1, false, format);
        addSliderKeys();
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
        if (meta == null) {
            return;
        }
        cameraMaker = meta.getCameraMake(false);
        cameraModel = cameraMaker == null ? "" : meta.getCameraMake(true);
    }

    private void setLensFromMetadata() {
        if (meta == null) {
            return;
        }
        lensMaker = "";
        lensModel = meta.getLens() == null ? "" : meta.getLens();
        focal     = meta.getFocalLength();
        aperture  = meta.getAperture();
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
            if ("".equals(cameraModel)) {
                setCameraFromMetadata();
            } else {
                cameraMaker = names[0];
            }
            updateLenses();
        } else if (key.equals(LENS_NAME)) {
            val names = value.split(SEPARATOR, 2);
            lensModel = names[1];
            if ("".equals(lensModel)) {
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
        final List<String> values = ("".equals(cameraModel))
                ? Lensfun.getAllLensNames()
                : Lensfun.getLensNamesFor(cameraMaker, cameraModel);
        addChoiceValue(LENS_NAME, "(Automatic)" + SEPARATOR);
        addChoiceValues(LENS_NAME, values);
    }

    @Override
    public void setSliderValue(String key, double value) {
        if (! manual_mode) // TODO:
            return;

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
            else {
                transform.preConcatenate(AffineTransform.getScaleInstance(1 / scaleFactor, 1 / scaleFactor));
            }
            val center = transform.transform(new Point2D.Double(fullWidth / 2, fullHeight / 2), null);

            PlanarImage front = back;
            if (! manual_mode) {
                val borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
                front = new DistortionOpImage(front, JAIContext.fileCacheHint, borderExtender,
                        fullWidth, fullHeight, center,
                        cameraMaker, cameraModel, lensMaker, lensModel, focal, aperture);
            }
            else if (Math.abs(distortion_k1) > 1e-3 || Math.abs(distortion_k2) > 1e-3 ||
                    Math.abs(tca_r_offset)  > 1e-3 || Math.abs(tca_b_offset)  > 1e-3) {
                val borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
                front = new DistortionOpImage(front, JAIContext.fileCacheHint, borderExtender,
                        fullWidth, fullHeight, center,
                        distortion_k1_scale * distortion_k1,
                        distortion_k2_scale * distortion_k2,
                        1 + tca_scale * tca_r_offset,
                        1 + tca_scale * tca_b_offset);
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
