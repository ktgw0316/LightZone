/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.LightnessLookupTable;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.ZoneOperation;
import com.lightcrafts.utils.splines;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.LookupTableJAI;
import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.renderable.ParameterBlock;

import static com.lightcrafts.ui.help.HelpConstants.HELP_TOOL_ZONEMAPPER;

class ZoneOperationImpl extends BlendedOperation implements ZoneOperation {
    static final OperationType type = new OperationTypeImpl("Zone Mapper");

    private static final double weight = 10.0;
    private static final int order = 3;
    private double[][] controlPoints = {{0, 0}, {1, 1}};
    private short[] tableData = new short[0x10000];
    private LookupTableJAI table = null;
    private double[][] curve = new double[0x100][2];

    private int scale = LuminosityScale;  // LuminosityScale (default) or RGBScale

    ZoneOperationImpl(Rendering rendering) {
        super(rendering, type);
        setHelpTopic(HELP_TOOL_ZONEMAPPER);
    }

    @Override
    public boolean neutralDefault() {
        return true;
    }

    public OperationImpl createInstance(Rendering rendering) {
        return new ZoneOperationImpl(rendering);
    }

    private double[] lastPoints = null;

    @Override
    public void setScale(int scale) {
        if ((scale != RgbScale) && (scale != LuminosityScale)) {
            throw new IllegalArgumentException(
                "setScale() only accepts RgbScale and LuminosityScale"
            );
        }
        if (this.scale != scale) {
            this.scale = scale;
            table = null;
            settingsChanged();
        }
    }

    @Override
    public void setControlPoints(double[] points) {
        if (lastPoints != null) {
            if (points.length == lastPoints.length) {
                boolean noChange = true;
                for (int i = 0; i < points.length; i++)
                    if (points[i] != lastPoints[i]) {
                        noChange = false;
                        break;
                    }
                if (noChange)
                    return;
            }
        }

        lastPoints = points;

        double[] ctrlPts = new double[points.length];
        System.arraycopy(points, 0, ctrlPts, 0, points.length);
        points = ctrlPts;

        if (points[0] == -1)
            points[0] = 0;

        if (points[points.length-1] == -1)
            points[points.length-1] = 1;

        int npoints = 0;
        for (double p : points)
            if (p >= 0)
                npoints++;

        controlPoints = new double[npoints][2];

        npoints = 0;
        for (int i = 0; i < points.length; i++) {
            if (points[i] >= 0) {
                controlPoints[npoints][0] = (Math.pow(2, i * 8.0 / (points.length - 1)) - 1) / 255.0f;
                controlPoints[npoints][1] = (Math.pow(2, points[i] * 8.0) - 1) / 255.0f;

                npoints++;
            }
        }

        if (rendering.getEngine() != null)
            rendering.getEngine().setFocusedZone(-1, controlPoints);

        table = null;

        settingsChanged();
    }

    private static double fy(double y) {
        return Math.log1p(255.0 * y) / (8 * Math.log(2));
    }

    @Override
    public double getControlPoint(int index) {
        final double x = (Math.pow(2, index * 8.0 / 16.0) - 1) / 255.0f;
        final double y;

        if (controlPoints == null) {
            y = x;
        } else {
            double xmin = 0;
            double ymin = 0;
            for (double[] cp : controlPoints) {
                if (cp[0] == x) {
                    return fy(cp[1]);
                } else if (x > cp[0]) {
                    xmin = cp[0];
                    ymin = cp[1];
                } else {
                    break;
                }
            }

            double xmax = 1;
            double ymax = 1;
            for (int i = controlPoints.length - 1; i >= 0; i--) {
                if (x <= controlPoints[i][0]) {
                    xmax = controlPoints[i][0];
                    ymax = controlPoints[i][1];
                } else {
                    break;
                }
            }

            y = ((x - xmin) / (xmax - xmin)) * (ymax - ymin) + ymin;
        }

        return fy(y);
    }

    @Override
    public void setFocusPoint(int index) {
        // System.out.println("ZoneOperation focus at: " + index);
        int zoneIndex = index >= 0 ? (int) (16 * getControlPoint(index) + 0.5) : -1;

        if (rendering.getEngine() != null)
            rendering.getEngine().setFocusedZone(zoneIndex, controlPoints);
    }

    private void updateCurve() {
        double[] weights = new double[controlPoints.length];
	weights[0] = weights[controlPoints.length - 1] = 1.0;
	for (int i = 1; i < controlPoints.length - 1; i++)
	    weights[i] = weight;
	splines.rbspline(order, controlPoints, weights, curve);
    }

    private LookupTableJAI computeTable(PlanarImage source) {
	if (table != null)
	    return table;

	updateCurve();

        if (controlPoints.length > 2) {
            splines.interpolate(0, 1, 1.0/(tableData.length-1), curve, tableData);
        } else
            for (int i = 0; i < tableData.length; i++) {
                double x = i / (double) (tableData.length - 1);
                double y = controlPoints[0][1] + x * (controlPoints[1][1] - controlPoints[0][1]);
                tableData[i] = (short) (((int) (y * (tableData.length - 1) + 0.5)) & 0xffff);
            }

        return table = source.getColorModel().getNumColorComponents() == 3
                       && scale == LuminosityScale ?
	    new LightnessLookupTable(tableData, true) :
	    new LookupTableJAI(tableData, true);
    }

    private class ZoneMapper extends BlendedTransform {
        ZoneMapper(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            LookupTableJAI table = computeTable(back);
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(back);
            pb.add(table);

            // Add a layout hint to make sure that source and destination match

            RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
                                                      new ImageLayout(back));
            // hints.add(JAIContext.noCacheHint);

            return JAI.create("lookup", pb, hints);
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new ZoneMapper(source);
    }
}
