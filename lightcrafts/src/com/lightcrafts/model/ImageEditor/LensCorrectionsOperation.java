/* Copyright (C) 2015 Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;

import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.DistortionOpImage;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.SliderConfig;

public class LensCorrectionsOperation extends BlendedOperation {
    private static final String AUTO_CORRECTION = "Auto_Correction";
    private static final String DISTORTION_K1 = "Main";
    private static final String DISTORTION_K2 = "Edge";
    private static final String TCA_R = "TCA_Red";
    private static final String TCA_B = "TCA_Blue";

    private boolean auto_correction = false;
    private float distortion_k1 = 0;
    private float distortion_k2 = 0;
    private float tca_r_offset = 0;
    private float tca_b_offset = 0;

    private String cameraMaker = "";
    private String cameraModel = "";
    private String lensName = "";
    private float  focal = 0f;
    private float  aperture = 0f;

    static final OperationType type = new OperationTypeImpl("Lens Corrections");

    public LensCorrectionsOperation(Rendering rendering, OperationType type) {
        super(rendering, type);

        addCheckboxKey(AUTO_CORRECTION);
        addSliderKey(DISTORTION_K1);
        addSliderKey(DISTORTION_K2);
        addSliderKey(TCA_R);
        addSliderKey(TCA_B);

        DecimalFormat format  = new DecimalFormat("0.0");

        setCheckboxValue(AUTO_CORRECTION, false);
        setSliderConfig(DISTORTION_K1, new SliderConfig(-100, 100, distortion_k1, 1, false, format));
        setSliderConfig(DISTORTION_K2, new SliderConfig(-100, 100, distortion_k2, 1, false, format));
        setSliderConfig(TCA_R, new SliderConfig(-2, 2, tca_r_offset, 0.1, false, format));
        setSliderConfig(TCA_B, new SliderConfig(-2, 2, tca_b_offset, 0.1, false, format));

        ImageEditorEngine engine = rendering.getEngine();

        if (engine != null) {
            ImageMetadata meta = engine.getMetadata();
            cameraMaker = meta.getCameraMake(false);
            cameraModel = cameraMaker == null ? ""
                        : meta.getCameraMake(true).substring(cameraMaker.length() + 1);
            lensName = meta.getLens() == null ? "" : meta.getLens();
            focal    = meta.getFocalLength();
            aperture = meta.getAperture();
        }
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
        if (key.equals(AUTO_CORRECTION)) {
            auto_correction = value;
            // TODO: disoble manual settings
        } else {
            return;
        }

        super.setCheckboxValue(key, value);
    }

    @Override
    public void setSliderValue(String key, double value) {
        if (auto_correction) // TODO:
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

        public PlanarImage setFront() {
            PlanarImage front = back;

            ImageEditorEngine engine = rendering.getEngine();

            if (engine != null) {
                PlanarImage sourceImage = engine.getSourceImage();
                int fullWidth = sourceImage.getWidth();
                int fullHeight = sourceImage.getHeight();

                final float scaleFactor = rendering.getScaleFactor();
                if (scaleFactor < 1) {
                    // Append pyramid ratio
                    fullWidth *= scaleFactor;
                    fullHeight *= scaleFactor;
                }

                AffineTransform transform = rendering.getTransform();
                transform.preConcatenate(AffineTransform.getScaleInstance(1 / scaleFactor, 1 / scaleFactor));
                Point2D center = transform.transform(new Point2D.Double(fullWidth / 2, fullHeight / 2), null);

                if (auto_correction) {
                    BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
                    front = new DistortionOpImage(front, JAIContext.fileCacheHint, borderExtender,
                            fullWidth, fullHeight, center,
                                                  cameraMaker, cameraModel, lensName, focal, aperture);
                    front.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
                }
                else if (Math.abs(distortion_k1) > 1e-3 || Math.abs(distortion_k2) > 1e-3 ||
                         Math.abs(tca_r_offset)  > 1e-3 || Math.abs(tca_b_offset)  > 1e-3) {
                    BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
                    float tca_scale = 1e-3f;
                    float distortion_k2_scale = 1e-3f;
                    float distortion_k1_scale = 1e-3f;
                    front = new DistortionOpImage(front, JAIContext.fileCacheHint, borderExtender,
                            fullWidth, fullHeight, center,
                                                  distortion_k1_scale * distortion_k1,
                                                  distortion_k2_scale * distortion_k2,
                                                  1 + tca_scale * tca_r_offset,
                                                  1 + tca_scale * tca_b_offset);
                    front.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
                }
            }

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
