/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.model.OperationType;
import com.lightcrafts.model.WhitePointOperation;
import com.lightcrafts.utils.splines;
import com.lightcrafts.utils.ColorScience;

import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.LookupTableJAI;
import com.lightcrafts.mediax.jai.PlanarImage;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.Raster;

class WhitePointOperationImpl extends BlendedOperation implements WhitePointOperation  {
    private static final OperationType type = new OperationTypeImpl("White Dropper");
    private Color color = Color.gray;
    private Point2D p = null;

    Rendering rendering;

    WhitePointOperationImpl(Rendering rendering) {
        super(rendering, type);
        this.rendering = rendering;
        colorInputOnly = true;
    }

    @Override
    public boolean neutralDefault() {
        return true;
    }

    @Override
    public void setWhitePoint(Point2D p) {
        this.p = p;
        settingsChanged();
    }

    @Override
    public void setWhitePoint(Color color) {
        this.color = color;
        this.p = null;
        settingsChanged();
    }

    @Override
    public Color getWhitePoint() {
        return color;
    }

    private class WhiteBalance extends BlendedTransform {
        WhiteBalance(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            if (p != null || color != null) {
                int[] pixel = null;
                if (p != null) {
                    Point2D pp = rendering.getTransform().transform(p, null);

                    int x = (int) pp.getX();
                    int y = (int) pp.getY();

                    if (rendering.getScaleFactor() > 1) {
                        x /= rendering.getScaleFactor();
                        y /= rendering.getScaleFactor();
                    }

                    if (back.getBounds().contains(x, y)) {
                        int tx = back.XToTileX(x);
                        int ty = back.YToTileY(y);

                        Raster tile = back.getTile(tx, ty);

                        int averagePixels = 3;

                        // if (averagePixels <= 1) {
                        //     pixel = tile.getPixel(x, y, pixel);
                        // } else {
                            Rectangle tileBounds = tile.getBounds();
                            Rectangle sampleRect = new Rectangle(x-averagePixels/2,
                                                                 y-averagePixels/2,
                                                                 averagePixels,
                                                                 averagePixels);

                            Rectangle intersection = tileBounds.intersection(sampleRect);

                            pixel = new int[] {0, 0, 0};
                            int currentPixel[] = new int[3];

                            for (int i = intersection.x; i < intersection.x + intersection.width; i++)
                                for (int j = intersection.y; j < intersection.y + intersection.height; j++) {
                                    currentPixel = tile.getPixel(i, j, currentPixel);
                                    for (int k = 0; k < 3; k++)
                                        pixel[k] = (pixel[k] + currentPixel[k]) / 2;
                                }
                        // }

                        color = new Color(pixel[0] / 256, pixel[1] / 256, pixel[2] / 256);
                        p = null; // Set the point to null, from now on we just remember the color...
                    } else {
                        System.out.println("Something funny here...");
                        return back;
                    }
                } else {
                    pixel = new int[] {color.getRed() * 256, color.getGreen() * 256, color.getBlue() * 256};
                }

                double tred = pixel[0] / 2 - pixel[2] / 4 - pixel[1] / 4;
                double tgreen = pixel[1] / 2 - pixel[0] / 4 - pixel[2] / 4;
                double tblue = pixel[2] / 2 - pixel[0] / 4 - pixel[1] / 4;

                double lum = (ColorScience.Wr * pixel[0] + ColorScience.Wg * pixel[1] + ColorScience.Wb * pixel[2]) / (double) 0xffff;

                double polygon[][] = {
                    {0,   0},
                    {lum, 0},
                    {1,   0}
                };

                polygon[1][1] = - tred / 256.;
                double redCurve[][] = new double[256][2];
                splines.bspline(2, polygon, redCurve);

                polygon[1][1] = - tgreen / 256.;
                double greenCurve[][] = new double[256][2];
                splines.bspline(2, polygon, greenCurve);

                polygon[1][1] = - tblue / 256.;
                double blueCurve[][] = new double[256][2];
                splines.bspline(2, polygon, blueCurve);

                short table[][] = new short[3][0x10000];

                splines.Interpolator interpolator = new splines.Interpolator();

                for (int i = 0; i < 0x10000; i++)
                    table[0][i] = (short) (0xffff & (int) Math.min(Math.max(i + 0xff * interpolator.interpolate(i / (double) 0xffff, redCurve), 0), 0xffff));

                interpolator.reset();
                for (int i = 0; i < 0x10000; i++)
                    table[1][i] = (short) (0xffff & (int) Math.min(Math.max(i + 0xff * interpolator.interpolate(i / (double) 0xffff, greenCurve), 0), 0xffff));

                interpolator.reset();
                for (int i = 0; i < 0x10000; i++)
                    table[2][i] = (short) (0xffff & (int) Math.min(Math.max(i + 0xff * interpolator.interpolate(i / (double) 0xffff, blueCurve), 0), 0xffff));

                LookupTableJAI lookupTable = new LookupTableJAI(table, true);

                ParameterBlock pb = new ParameterBlock();
                pb.addSource(back);
                pb.add(lookupTable);
                return JAI.create("lookup", pb, JAIContext.noCacheHint);
            } else {
                return back;
            }
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new WhiteBalance(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }
}
