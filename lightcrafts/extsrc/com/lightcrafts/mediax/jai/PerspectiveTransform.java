/*
 * $RCSfile: PerspectiveTransform.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:15 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.NoninvertibleTransformException;
import java.io.Serializable;


/**
 * A 2D perspective (or projective) transform, used by various OpImages.
 *
 * <p> A perspective transformation is capable of mapping an arbitrary
 * quadrilateral into another arbitrary quadrilateral, while
 * preserving the straightness of lines.  Unlike an affine
 * transformation, the parallelism of lines in the source is not
 * necessarily preserved in the output.
 *
 * <p> Such a coordinate transformation can be represented by a 3x3
 * matrix which transforms homogenous source coordinates
 * <code>(x,&nbsp;y,&nbsp;1)</code> into destination coordinates
 * <code>(x',&nbsp;y',&nbsp;w)</code>.  To convert back into non-homogenous
 * coordinates (X, Y), <code>x'</code> and <code>y'</code> are divided by
 * <code>w</code>.
 *
 * <pre>
 *	[ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
 *	[ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
 *	[ w ]   [  m20  m21  m22  ] [ 1 ]   [ m20x + m21y + m22 ]
 *
 *	  x' = (m00x + m01y + m02)
 *	  y' = (m10x + m11y + m12)
 *
 *        w  = (m20x + m21y + m22)
 *
 *        X = x' / w
 *        Y = y' / w
 * </pre>
 */
public final class PerspectiveTransform implements Cloneable, Serializable {

    private static final double PERSPECTIVE_DIVIDE_EPSILON = 1.0e-10;

    /** An element of the transform matrix. */
    double m00, m01, m02, m10, m11, m12, m20, m21, m22;

    /** Constructs an identity PerspectiveTransform. */
    public PerspectiveTransform() {
        m00 = m11 = m22 = 1.0;
        m01 = m02 = m10 = m12 = m20 = m21 = 0.0;
    }

    /**
     * Constructs a new PerspectiveTransform from 9 floats.
     * @deprecated as of JAI 1.1 Use PerspectiveTransform(double[][]) instead.
     */
    public PerspectiveTransform(float m00, float m01, float m02,
                                float m10, float m11, float m12,
                                float m20, float m21, float m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    /**
     * Constructs a new PerspectiveTransform from 9 doubles.
     * @deprecated as of JAI 1.1 Use PerspectiveTransform(double[][]) instead.
     */
    public PerspectiveTransform(double m00, double m01, double m02,
                                double m10, double m11, double m12,
                                double m20, double m21, double m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    /**
     * Constructs a new PerspectiveTransform from a one-dimensional
     * array of 9 floats, in row-major order.
     * The values in the array are assumed to be
     * { m00 m01 m02 m10 m11 m12 m20 m21 m22 }.
     * @throws IllegalArgumentException if flatmatrix is null
     * @throws ArrayIndexOutOfBoundsException if flatmatrix is too small
     * @deprecated as of JAI 1.1 Use PerspectiveTransform(double[][]) instead.
     */
    public PerspectiveTransform(float[] flatmatrix) {
        if ( flatmatrix == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        m00 = flatmatrix[0];
        m01 = flatmatrix[1];
        m02 = flatmatrix[2];
        m10 = flatmatrix[3];
        m11 = flatmatrix[4];
        m12 = flatmatrix[5];
        m20 = flatmatrix[6];
        m21 = flatmatrix[7];
        m22 = flatmatrix[8];
    }

    /**
     * Constructs a new PerspectiveTransform from a two-dimensional
     * array of floats.
     * @throws IllegalArgumentException if matrix is null
     * @throws ArrayIndexOutOfBoundsException if matrix is too small
     *
     * @deprecated as of JAI 1.1 Use PerspectiveTransform(double[][]) instead.
     */
    public PerspectiveTransform(float[][] matrix) {
        if ( matrix == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        m00 = matrix[0][0];
        m01 = matrix[0][1];
        m02 = matrix[0][2];
        m10 = matrix[1][0];
        m11 = matrix[1][1];
        m12 = matrix[1][2];
        m20 = matrix[2][0];
        m21 = matrix[2][1];
        m22 = matrix[2][2];
    }

    /**
     * Constructs a new PerspectiveTransform from a one-dimensional
     * array of 9 doubles, in row-major order.
     * The values in the array are assumed to be
     * { m00 m01 m02 m10 m11 m12 m20 m21 m22 }.
     * @throws IllegalArgumentException if flatmatrix is null
     * @throws ArrayIndexOutOfBoundsException if flatmatrix is too small
     *
     * @deprecated as of JAI 1.1 Use PerspectiveTransform(double[][]) instead.
     */
    public PerspectiveTransform(double[] flatmatrix) {
        if ( flatmatrix == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        m00 = flatmatrix[0];
        m01 = flatmatrix[1];
        m02 = flatmatrix[2];
        m10 = flatmatrix[3];
        m11 = flatmatrix[4];
        m12 = flatmatrix[5];
        m20 = flatmatrix[6];
        m21 = flatmatrix[7];
        m22 = flatmatrix[8];
    }

    /**
     * Constructs a new PerspectiveTransform from a two-dimensional
     * array of doubles.
     * @throws IllegalArgumentException if matrix is null
     * @throws ArrayIndexOutOfBoundsException if matrix is too small
     */
    public PerspectiveTransform(double[][] matrix) {
        if ( matrix == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        m00 = matrix[0][0];
        m01 = matrix[0][1];
        m02 = matrix[0][2];
        m10 = matrix[1][0];
        m11 = matrix[1][1];
        m12 = matrix[1][2];
        m20 = matrix[2][0];
        m21 = matrix[2][1];
        m22 = matrix[2][2];
    }

    /**
     * Constructs a new PerspectiveTransform with the same effect
     * as an existing AffineTransform.
     * @throws IllegalArgumentException if transform is null
     */
    public PerspectiveTransform(AffineTransform transform) {
        if ( transform == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        m00 = transform.getScaleX();
        m01 = transform.getShearX();
        m02 = transform.getTranslateX();
        m10 = transform.getShearY();
        m11 = transform.getScaleY();
        m12 = transform.getTranslateY();
        m20 = 0.0;
        m21 = 0.0;
        m22 = 1.0;
    }

    /**
     * Replaces the matrix with its adjoint.
     */
    private final void makeAdjoint() {
        double m00p = m11*m22 - m12*m21;
        double m01p = m12*m20 - m10*m22; // flipped sign
        double m02p = m10*m21 - m11*m20;
        double m10p = m02*m21 - m01*m22; // flipped sign
        double m11p = m00*m22 - m02*m20;
        double m12p = m01*m20 - m00*m21; // flipped sign
        double m20p = m01*m12 - m02*m11;
        double m21p = m02*m10 - m00*m12; // flipped sign
        double m22p = m00*m11 - m01*m10;

        // Transpose and copy sub-determinants
        m00 = m00p;
        m01 = m10p;
        m02 = m20p;
        m10 = m01p;
        m11 = m11p;
        m12 = m21p;
        m20 = m02p;
        m21 = m12p;
        m22 = m22p;
    }

    /**
     * Scales the matrix elements so m22 is equal to 1.0.
     * m22 must not be equal to 0.
     */
    private final void normalize() {
        double invscale = 1.0/m22;
        m00 *= invscale;
        m01 *= invscale;
        m02 *= invscale;
        m10 *= invscale;
        m11 *= invscale;
        m12 *= invscale;
        m20 *= invscale;
        m21 *= invscale;
        m22 = 1.0;
    }

    private static final void getSquareToQuad(double x0, double y0,
                                              double x1, double y1,
                                              double x2, double y2,
                                              double x3, double y3,
                                              PerspectiveTransform tx) {
        double dx3 = x0 - x1 + x2 - x3;
        double dy3 = y0 - y1 + y2 - y3;

        tx.m22 = 1.0F;

        if ((dx3 == 0.0F) && (dy3 == 0.0F)) { // to do: use tolerance
            tx.m00 = x1 - x0;
            tx.m01 = x2 - x1;
            tx.m02 = x0;
            tx.m10 = y1 - y0;
            tx.m11 = y2 - y1;
            tx.m12 = y0;
            tx.m20 = 0.0F;
            tx.m21 = 0.0F;
        } else {
            double dx1 = x1 - x2;
            double dy1 = y1 - y2;
            double dx2 = x3 - x2;
            double dy2 = y3 - y2;

            double invdet = 1.0F/(dx1*dy2 - dx2*dy1);
            tx.m20 = (dx3*dy2 - dx2*dy3)*invdet;
            tx.m21 = (dx1*dy3 - dx3*dy1)*invdet;
            tx.m00 = x1 - x0 + tx.m20*x1;
            tx.m01 = x3 - x0 + tx.m21*x3;
            tx.m02 = x0;
            tx.m10 = y1 - y0 + tx.m20*y1;
            tx.m11 = y3 - y0 + tx.m21*y3;
            tx.m12 = y0;
        }
    }

    /**
     * Creates a PerspectiveTransform that maps the unit square
     * onto an arbitrary quadrilateral.
     *
     * <pre>
     * (0, 0) -> (x0, y0)
     * (1, 0) -> (x1, y1)
     * (1, 1) -> (x2, y2)
     * (0, 1) -> (x3, y3)
     * </pre>
     */
    public static PerspectiveTransform getSquareToQuad(double x0, double y0,
                                                       double x1, double y1,
                                                       double x2, double y2,
                                                       double x3, double y3) {
        PerspectiveTransform tx = new PerspectiveTransform();
        getSquareToQuad(x0, y0, x1, y1, x2, y2, x3, y3, tx);
        return tx;
    }


    /**
     * Creates a PerspectiveTransform that maps the unit square
     * onto an arbitrary quadrilateral.
     *
     * <pre>
     * (0, 0) -> (x0, y0)
     * (1, 0) -> (x1, y1)
     * (1, 1) -> (x2, y2)
     * (0, 1) -> (x3, y3)
     * </pre>
     */
    public static PerspectiveTransform getSquareToQuad(float x0, float y0,
                                                       float x1, float y1,
                                                       float x2, float y2,
                                                       float x3, float y3) {
        return getSquareToQuad((double)x0, (double)y0,
                               (double)x1, (double)y1,
                               (double)x2, (double)y2,
                               (double)x3, (double)y3);
    }


    /**
     * Creates a PerspectiveTransform that maps an arbitrary
     * quadrilateral onto the unit square.
     *
     * <pre>
     * (x0, y0) -> (0, 0)
     * (x1, y1) -> (1, 0)
     * (x2, y2) -> (1, 1)
     * (x3, y3) -> (0, 1)
     * </pre>
     */
    public static PerspectiveTransform getQuadToSquare(double x0, double y0,
                                                       double x1, double y1,
                                                       double x2, double y2,
                                                       double x3, double y3) {
        PerspectiveTransform tx = new PerspectiveTransform();
        getSquareToQuad(x0, y0, x1, y1, x2, y2, x3, y3, tx);
        tx.makeAdjoint();
        return tx;
    }

    /**
     * Creates a PerspectiveTransform that maps an arbitrary
     * quadrilateral onto the unit square.
     *
     * <pre>
     * (x0, y0) -> (0, 0)
     * (x1, y1) -> (1, 0)
     * (x2, y2) -> (1, 1)
     * (x3, y3) -> (0, 1)
     * </pre>
     */
    public static PerspectiveTransform getQuadToSquare(float x0, float y0,
                                                       float x1, float y1,
                                                       float x2, float y2,
                                                       float x3, float y3) {
        return getQuadToSquare((double)x0, (double)y0,
                               (double)x1, (double)y1,
                               (double)x2, (double)y2,
                               (double)x3, (double)y3);
    }

    /**
     * Creates a PerspectiveTransform that maps an arbitrary
     * quadrilateral onto another arbitrary quadrilateral.
     *
     * <pre>
     * (x0, y0) -> (x0p, y0p)
     * (x1, y1) -> (x1p, y1p)
     * (x2, y2) -> (x2p, y2p)
     * (x3, y3) -> (x3p, y3p)
     * </pre>
     */
    public static PerspectiveTransform getQuadToQuad(double x0, double y0,
                                                     double x1, double y1,
                                                     double x2, double y2,
                                                     double x3, double y3,
                                                     double x0p, double y0p,
                                                     double x1p, double y1p,
                                                     double x2p, double y2p,
                                                     double x3p, double y3p) {
        PerspectiveTransform tx1 =
                          getQuadToSquare(x0, y0, x1, y1, x2, y2, x3, y3);

        PerspectiveTransform tx2 =
                  getSquareToQuad(x0p, y0p, x1p, y1p, x2p, y2p, x3p, y3p);

        tx1.concatenate(tx2);
        return tx1;
    }


    /**
     * Creates a PerspectiveTransform that maps an arbitrary
     * quadrilateral onto another arbitrary quadrilateral.
     *
     * <pre>
     * (x0, y0) -> (x0p, y0p)
     * (x1, y1) -> (x1p, y1p)
     * (x2, y2) -> (x2p, y2p)
     * (x3, y3) -> (x3p, y3p)
     * </pre>
     */
    public static PerspectiveTransform getQuadToQuad(float x0, float y0,
                                                     float x1, float y1,
                                                     float x2, float y2,
                                                     float x3, float y3,
                                                     float x0p, float y0p,
                                                     float x1p, float y1p,
                                                     float x2p, float y2p,
                                                     float x3p, float y3p) {
        return getQuadToQuad((double)x0, (double)y0,
                              (double)x1, (double)y1,
                              (double)x2, (double)y2,
                              (double)x3, (double)y3,
                              (double)x0p, (double)y0p,
                              (double)x1p, (double)y1p,
                              (double)x2p, (double)y2p,
                              (double)x3p, (double)y3p);
    }

    /**
     * Returns the determinant of the matrix representation of the
     * transform.
     */
    public double getDeterminant() {
	return ( (m00 * ((m11 * m22) - (m12 * m21))) -
                 (m01 * ((m10 * m22) - (m12 * m20))) +
                 (m02 * ((m10 * m21) - (m11 * m20))) );

    }

    /**
     * Retrieves the 9 specifiable values in the 3x3 affine
     * transformation matrix into an array of double precision values.
     * The values are stored into the array as
     * { m00 m01 m02 m10 m11 m12 m20 m21 m22 }.
     *
     * @param flatmatrix The double array used to store the returned
     *        values.  The length of the array is assumed to be at
     *        least 9.
     * @throws ArrayIndexOutOfBoundsException if flatmatrix is too small
     * @deprecated as of JAI 1.1 Use double[][] getMatrix(double[][] matrix) instead.
     */
    public double[] getMatrix(double[] flatmatrix) {
        if (flatmatrix == null) {
            flatmatrix = new double[9];
        }

        flatmatrix[0] = m00;
        flatmatrix[1] = m01;
        flatmatrix[2] = m02;
        flatmatrix[3] = m10;
        flatmatrix[4] = m11;
        flatmatrix[5] = m12;
        flatmatrix[6] = m20;
        flatmatrix[7] = m21;
        flatmatrix[8] = m22;

        return flatmatrix;
    }

    /**
     * Retrieves the 9 specifiable values in the 3x3 affine
     * transformation matrix into a 2-dimensional array of double
     * precision values.  The values are stored into the 2-dimensional
     * array using the row index as the first subscript and the column
     * index as the second.
     *
     * @param matrix The 2-dimensional double array to store the
     *        returned values.  The array is assumed to be at least 3x3.
     * @throws ArrayIndexOutOfBoundsException if matrix is too small
     */
    public double[][] getMatrix(double[][] matrix) {
        if (matrix == null) {
            matrix = new double[3][3];
        }

        matrix[0][0] = m00;
        matrix[0][1] = m01;
        matrix[0][2] = m02;
        matrix[1][0] = m10;
        matrix[1][1] = m11;
        matrix[1][2] = m12;
        matrix[2][0] = m20;
        matrix[2][1] = m21;
        matrix[2][2] = m22;

        return matrix;
    }

    /**
     * Concatenates this transform with a translation transformation.
     * This is equivalent to calling concatenate(T), where T is an
     * PerspectiveTransform represented by the following matrix:
     * <pre>
     *		[   1    0    tx  ]
     *		[   0    1    ty  ]
     *		[   0    0    1   ]
     * </pre>
     */
    public void translate(double tx, double ty) {
        PerspectiveTransform Tx = new PerspectiveTransform();
        Tx.setToTranslation(tx, ty);
        concatenate(Tx);
    }

    /**
     * Concatenates this transform with a rotation transformation.
     * This is equivalent to calling concatenate(R), where R is an
     * PerspectiveTransform represented by the following matrix:
     * <pre>
     *		[   cos(theta)    -sin(theta)    0   ]
     *		[   sin(theta)     cos(theta)    0   ]
     *		[       0              0         1   ]
     * </pre>
     * Rotating with a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     *
     * @param theta The angle of rotation in radians.
     */
    public void rotate(double theta) {
        PerspectiveTransform Tx = new PerspectiveTransform();
        Tx.setToRotation(theta);
        concatenate(Tx);
    }

    /**
     * Concatenates this transform with a translated rotation transformation.
     * This is equivalent to the following sequence of calls:
     * <pre>
     *		translate(x, y);
     *		rotate(theta);
     *		translate(-x, -y);
     * </pre>
     * Rotating with a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     *
     * @param theta The angle of rotation in radians.
     * @param x The X coordinate of the origin of the rotation
     * @param y The Y coordinate of the origin of the rotation
     */
    public void rotate(double theta, double x, double y) {
        PerspectiveTransform Tx = new PerspectiveTransform();
        Tx.setToRotation(theta, x, y);
        concatenate(Tx);
    }

    /**
     * Concatenates this transform with a scaling transformation.
     * This is equivalent to calling concatenate(S), where S is an
     * PerspectiveTransform represented by the following matrix:
     * <pre>
     *		[   sx   0    0   ]
     *		[   0    sy   0   ]
     *		[   0    0    1   ]
     * </pre>
     *
     * @param sx The X axis scale factor.
     * @param sy The Y axis scale factor.
     */
    public void scale(double sx, double sy) {
        PerspectiveTransform Tx = new PerspectiveTransform();
        Tx.setToScale(sx, sy);
        concatenate(Tx);
    }

    /**
     * Concatenates this transform with a shearing transformation.
     * This is equivalent to calling concatenate(SH), where SH is an
     * PerspectiveTransform represented by the following matrix:
     * <pre>
     *		[   1   shx   0   ]
     *		[  shy   1    0   ]
     *		[   0    0    1   ]
     * </pre>
     *
     * @param shx The factor by which coordinates are shifted towards
     *        the positive X axis direction according to their Y
     *        coordinate.
     * @param shy The factor by which coordinates are shifted towards
     *        the positive Y axis direction according to their X
     *        coordinate.
     */
    public void shear(double shx, double shy) {
        PerspectiveTransform Tx = new PerspectiveTransform();
        Tx.setToShear(shx, shy);
        concatenate(Tx);
    }

    /**
     * Resets this transform to the Identity transform.
     */
    public void setToIdentity() {
        m00 = m11 = m22 = 1.0;
        m01 = m10 = m02 = m20 = m12 = m21 = 0.0;
    }

    /**
     * Sets this transform to a translation transformation.
     * The matrix representing this transform becomes:
     * <pre>
     *		[   1    0    tx  ]
     *		[   0    1    ty  ]
     *		[   0    0    1   ]
     * </pre>
     * @param tx The distance by which coordinates are translated in the
     * X axis direction
     * @param ty The distance by which coordinates are translated in the
     * Y axis direction
     */
    public void setToTranslation(double tx, double ty) {
        m00 = 1.0;
        m01 = 0.0;
        m02 = tx;
        m10 = 0.0;
        m11 = 1.0;
        m12 = ty;
        m20 = 0.0;
        m21 = 0.0;
        m22 = 1.0;
    }

    /**
     * Sets this transform to a rotation transformation.
     * The matrix representing this transform becomes:
     * <pre>
     *		[   cos(theta)    -sin(theta)    0   ]
     *		[   sin(theta)     cos(theta)    0   ]
     *		[       0              0         1   ]
     * </pre>
     * Rotating with a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     * @param theta The angle of rotation in radians.
     */
    public void setToRotation(double theta) {
        m00 = Math.cos(theta);
        m01 = -Math.sin(theta);
        m02 = 0.0;
        m10 = - m01;    // Math.sin(theta);
        m11 = m00;      // Math.cos(theta);
        m12 = 0.0;
        m20 = 0.0;
        m21 = 0.0;
        m22 = 1.0;
    }

    /**
     * Sets this transform to a rotation transformation
     * about a specified point (x, y).  This is equivalent
     * to the following sequence of calls:
     *
     * <pre>
     *		setToTranslate(x, y);
     *		rotate(theta);
     *		translate(-x, -y);
     * </pre>
     *
     * Rotating with a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     *
     * @param theta The angle of rotation in radians.
     * @param x The X coordinate of the origin of the rotation
     * @param y The Y coordinate of the origin of the rotation
     */
    public void setToRotation(double theta, double x, double y) {
        setToRotation(theta);
	double sin = m10;
	double oneMinusCos = 1.0 - m00;
	m02 = x * oneMinusCos + y * sin;
	m12 = y * oneMinusCos - x * sin;
    }

    /**
     * Sets this transform to a scale transformation
     * with scale factors sx and sy.
     * The matrix representing this transform becomes:
     * <pre>
     *		[   sx   0    0   ]
     *		[   0    sy   0   ]
     *		[   0    0    1   ]
     * </pre>
     *
     * @param sx The X axis scale factor.
     * @param sy The Y axis scale factor.
     */
    public void setToScale(double sx, double sy) {
        m00 = sx;
        m01 = 0.0;
        m02 = 0.0;
        m10 = 0.0;
        m11 = sy;
        m12 = 0.0;
        m20 = 0.0;
        m21 = 0.0;
        m22 = 1.0;
    }

    /**
     * Sets this transform to a shearing transformation
     * with shear factors sx and sy.
     * The matrix representing this transform becomes:
     * <pre>
     *		[   1  shx    0   ]
     *		[ shy    1    0   ]
     *		[   0    0    1   ]
     * </pre>
     *
     * @param shx The factor by which coordinates are shifted towards
     *        the positive X axis direction according to their Y
     *        coordinate.
     * @param shy The factor by which coordinates are shifted towards
     *        the positive Y axis direction according to their X
     *        coordinate.
     */
    public void setToShear(double shx, double shy) {
        m00 = 1.0;
        m01 = shx;
        m02 = 0.0;
        m10 = shy;
        m11 = 1.0;
        m12 = 0.0;
        m20 = 0.0;
        m21 = 0.0;
        m22 = 1.0;
    }

    /**
     * Sets this transform to a given AffineTransform.
     * @throws IllegalArgumentException if Tx is null
     */
    public void setTransform(AffineTransform Tx) {
        if ( Tx == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        m00 = Tx.getScaleX();
        m01 = Tx.getShearX();
        m02 = Tx.getTranslateX();
        m10 = Tx.getShearY();
        m11 = Tx.getScaleY();
        m12 = Tx.getTranslateY();
        m20 = 0.0;
        m21 = 0.0;
        m22 = 1.0;
    }

    /**
     * Sets this transform to a given PerspectiveTransform.
     * @throws IllegalArgumentException if Tx is null
     */
    public void setTransform(PerspectiveTransform Tx) {
        if ( Tx == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        m00 = Tx.m00;
        m01 = Tx.m01;
        m02 = Tx.m02;
        m10 = Tx.m10;
        m11 = Tx.m11;
        m12 = Tx.m12;
        m20 = Tx.m20;
        m21 = Tx.m21;
        m22 = Tx.m22;
    }

    /**
     * Sets this transform to a given PerspectiveTransform,
     * expressed by the elements of its matrix.  <i>Important Note: The
     * matrix elements in the argument list are in column-major order
     * unlike those of the constructor, which are in row-major order.</i>
     * @deprecated as of JAI 1.1 Use double[][] getMatrix(double[][] matrix) instead.
     */
    public void setTransform(float m00, float m10, float m20,
                             float m01, float m11, float m21,
                             float m02, float m12, float m22) {
        this.m00 = (double)m00;
        this.m01 = (double)m01;
        this.m02 = (double)m02;
        this.m10 = (double)m10;
        this.m11 = (double)m11;
        this.m12 = (double)m12;
        this.m20 = (double)m20;
        this.m21 = (double)m21;
        this.m22 = (double)m22;
    }

    /**
     * Sets this transform using a two-dimensional array of double precision
     * values.  The row index is first, and the column index is second.
     *
     * @param matrix The 2D double array to be used for setting this transform.
     *        The array is assumed to be at least 3x3.
     * @throws IllegalArgumentException if matrix is null
     * @throws ArrayIndexOutOfBoundsException if matrix is too small
     * @since JAI 1.1
     */
    public void setTransform(double[][] matrix) {
        if ( matrix == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        m00 = matrix[0][0];
        m01 = matrix[0][1];
        m02 = matrix[0][2];
        m10 = matrix[1][0];
        m11 = matrix[1][1];
        m12 = matrix[1][2];
        m20 = matrix[2][0];
        m21 = matrix[2][1];
        m22 = matrix[2][2];
    }

    /**
     * Post-concatenates a given AffineTransform to this transform.
     * @throws IllegalArgumentException if Tx is null
     */
    public void concatenate(AffineTransform Tx) {
        if ( Tx == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Extend Tx: Tx.m20 = 0, Tx.m21 = 0, Tx.m22 = 1

        double tx_m00 = Tx.getScaleX();
        double tx_m01 = Tx.getShearX();
        double tx_m02 = Tx.getTranslateX();
        double tx_m10 = Tx.getShearY();
        double tx_m11 = Tx.getScaleY();
        double tx_m12 = Tx.getTranslateY();

        double m00p = m00*tx_m00 + m10*tx_m01 + m20*tx_m02;
        double m01p = m01*tx_m00 + m11*tx_m01 + m21*tx_m02;
        double m02p = m02*tx_m00 + m12*tx_m01 + m22*tx_m02;
        double m10p = m00*tx_m10 + m10*tx_m11 + m20*tx_m12;
        double m11p = m01*tx_m10 + m11*tx_m11 + m21*tx_m12;
        double m12p = m02*tx_m10 + m12*tx_m11 + m22*tx_m12;
        double m20p = m20;
        double m21p = m21;
        double m22p = m22;

        m00 = m00p;
        m10 = m10p;
        m20 = m20p;
        m01 = m01p;
        m11 = m11p;
        m21 = m21p;
        m02 = m02p;
        m12 = m12p;
        m22 = m22p;
    }

    /**
     * Post-concatenates a given PerspectiveTransform to this transform.
     * @throws IllegalArgumentException if Tx is null
     */
    public void concatenate(PerspectiveTransform Tx) {
        if ( Tx == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        double m00p = m00*Tx.m00 + m10*Tx.m01 + m20*Tx.m02;
        double m10p = m00*Tx.m10 + m10*Tx.m11 + m20*Tx.m12;
        double m20p = m00*Tx.m20 + m10*Tx.m21 + m20*Tx.m22;
        double m01p = m01*Tx.m00 + m11*Tx.m01 + m21*Tx.m02;
        double m11p = m01*Tx.m10 + m11*Tx.m11 + m21*Tx.m12;
        double m21p = m01*Tx.m20 + m11*Tx.m21 + m21*Tx.m22;
        double m02p = m02*Tx.m00 + m12*Tx.m01 + m22*Tx.m02;
        double m12p = m02*Tx.m10 + m12*Tx.m11 + m22*Tx.m12;
        double m22p = m02*Tx.m20 + m12*Tx.m21 + m22*Tx.m22;

        m00 = m00p;
        m10 = m10p;
        m20 = m20p;
        m01 = m01p;
        m11 = m11p;
        m21 = m21p;
        m02 = m02p;
        m12 = m12p;
        m22 = m22p;
    }

    /**
     * Pre-concatenates a given AffineTransform to this transform.
     * @throws IllegalArgumentException if Tx is null
     */
    public void preConcatenate(AffineTransform Tx) {
        if ( Tx == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Extend Tx: Tx.m20 = 0, Tx.m21 = 0, Tx.m22 = 1

        double tx_m00 = Tx.getScaleX();
        double tx_m01 = Tx.getShearX();
        double tx_m02 = Tx.getTranslateX();
        double tx_m10 = Tx.getShearY();
        double tx_m11 = Tx.getScaleY();
        double tx_m12 = Tx.getTranslateY();

        double m00p = tx_m00*m00 + tx_m10*m01;
        double m01p = tx_m01*m00 + tx_m11*m01;
        double m02p = tx_m02*m00 + tx_m12*m01 + m02;
        double m10p = tx_m00*m10 + tx_m10*m11;
        double m11p = tx_m01*m10 + tx_m11*m11;
        double m12p = tx_m02*m10 + tx_m12*m11 + m12;
        double m20p = tx_m00*m20 + tx_m10*m21;
        double m21p = tx_m01*m20 + tx_m11*m21;
        double m22p = tx_m02*m20 + tx_m12*m21 + m22;

        m00 = m00p;
        m10 = m10p;
        m20 = m20p;
        m01 = m01p;
        m11 = m11p;
        m21 = m21p;
        m02 = m02p;
        m12 = m12p;
        m22 = m22p;
    }

    /**
     * Pre-concatenates a given PerspectiveTransform to this transform.
     * @throws IllegalArgumentException if Tx is null
     */
    public void preConcatenate(PerspectiveTransform Tx) {
        if ( Tx == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        double m00p = Tx.m00*m00 + Tx.m10*m01 + Tx.m20*m02;
        double m10p = Tx.m00*m10 + Tx.m10*m11 + Tx.m20*m12;
        double m20p = Tx.m00*m20 + Tx.m10*m21 + Tx.m20*m22;
        double m01p = Tx.m01*m00 + Tx.m11*m01 + Tx.m21*m02;
        double m11p = Tx.m01*m10 + Tx.m11*m11 + Tx.m21*m12;
        double m21p = Tx.m01*m20 + Tx.m11*m21 + Tx.m21*m22;
        double m02p = Tx.m02*m00 + Tx.m12*m01 + Tx.m22*m02;
        double m12p = Tx.m02*m10 + Tx.m12*m11 + Tx.m22*m12;
        double m22p = Tx.m02*m20 + Tx.m12*m21 + Tx.m22*m22;

        m00 = m00p;
        m10 = m10p;
        m20 = m20p;
        m01 = m01p;
        m11 = m11p;
        m21 = m21p;
        m02 = m02p;
        m12 = m12p;
        m22 = m22p;
    }

    /**
     * Returns a new PerpectiveTransform that is the inverse
     * of the current transform.
     * @throws NoninvertibleTransformException if transform cannot be inverted
     */
     public PerspectiveTransform createInverse()
         throws NoninvertibleTransformException, CloneNotSupportedException {

	     PerspectiveTransform tx = (PerspectiveTransform)clone();
	     tx.makeAdjoint();
	     if (Math.abs(tx.m22) <  PERSPECTIVE_DIVIDE_EPSILON) {
  	       throw new NoninvertibleTransformException(JaiI18N.getString("PerspectiveTransform0"));
	     }
	     tx.normalize();
	     return tx;
    }

    /**
     * Returns a new PerpectiveTransform that is the adjoint,
     * of the current transform.  The adjoint is defined as
     * the matrix of cofactors, which in turn are the determinants
     * of the submatrices defined by removing the row and column
     * of each element from the original matrix in turn.
     *
     * <p> The adjoint is a scalar multiple of the inverse matrix.
     * Because points to be transformed are converted into homogeneous
     * coordinates, where scalar factors are irrelevant, the adjoint
     * may be used in place of the true inverse. Since it is unnecessary
     * to normalize the adjoint, it is both faster to compute and more
     * numerically stable than the true inverse.
     */
    public PerspectiveTransform createAdjoint()
    throws CloneNotSupportedException{

	    PerspectiveTransform tx = (PerspectiveTransform)clone();
	    tx.makeAdjoint();
	    return tx;
    }

    /**
     * Transforms the specified ptSrc and stores the result in ptDst.
     * If ptDst is null, a new Point2D object will be allocated before
     * storing. In either case, ptDst containing the transformed point
     * is returned for convenience.
     * Note that ptSrc and ptDst can the same. In this case, the input
     * point will be overwritten with the transformed point.
     *
     * @param ptSrc The array containing the source point objects.
     * @param ptDst The array where the transform point objects are returned.
     * @throws IllegalArgumentException if ptSrc is null
     */
    public Point2D transform(Point2D ptSrc, Point2D ptDst) {
        if ( ptSrc == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (ptDst == null) {
            if (ptSrc instanceof Point2D.Double) {
                ptDst = new Point2D.Double();
            } else {
                ptDst = new Point2D.Float();
            }
        }

        double x = ptSrc.getX();
        double y = ptSrc.getY();
        double w = m20 * x + m21 * y + m22;
        ptDst.setLocation((m00 * x + m01 * y + m02) / w,
                          (m10 * x + m11 * y + m12) / w);

        return ptDst;
    }

    /**
     * Transforms an array of point objects by this transform.
     * @param ptSrc The array containing the source point objects.
     * @param ptDst The array where the transform point objects are returned.
     * @param srcOff The offset to the first point object to be transformed
     * in the source array.
     * @param dstOff The offset to the location where the first transformed
     * point object is stored in the destination array.
     * @param numPts The number of point objects to be transformed.
     * @throws IllegalArgumentException if ptSrc is null
     * @throws IllegalArgumentException if ptDst is null
     * @throws ArrayIndexOutOfBoundsException if ptSrc is too small
     */
    public void transform(Point2D[] ptSrc, int srcOff,
			  Point2D[] ptDst, int dstOff,
			  int numPts) {

        if ( ptSrc == null || ptDst == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        while (numPts-- > 0) {
            /* Copy source coords into local variables in case src == dst. */
            Point2D src = ptSrc[srcOff++];
            Point2D dst = ptDst[dstOff++];
            if (dst == null) {
                if (src instanceof Point2D.Double) {
                    dst = new Point2D.Double();
                } else {
                    dst = new Point2D.Float();
                }
                ptDst[dstOff - 1] = dst;
            }

            double x = src.getX();
            double y = src.getY();
            double w = m20 * x + m21 * y + m22;

            if (w == 0) {
                dst.setLocation(x, y);
            } else {
                dst.setLocation((m00 * x + m01 * y + m02) / w,
                                (m10 * x + m11 * y + m12) / w);
            }
        }
    }

    /**
     * Transforms an array of floating point coordinates by this transform.
     * @param srcPts The array containing the source point coordinates.
     * Each point is stored as a pair of x,y coordinates.
     * @param srcOff The offset to the first point to be transformed
     * in the source array.
     * @param dstPts The array where the transformed point coordinates are
     * returned.  Each point is stored as a pair of x,y coordinates.
     * @param dstOff The offset to the location where the first transformed
     * point is stored in the destination array.
     * @param numPts The number of points to be transformed.
     * @throws IllegalArgumentException if srcPts is null
     * @throws ArrayIndexOutOfBoundsException if srcPts is too small
     */
    public void transform(float[] srcPts, int srcOff,
			  float[] dstPts, int dstOff,
			  int numPts) {

        if ( srcPts == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (dstPts == null) {
            dstPts = new float[numPts * 2 + dstOff];
        }

        while (numPts-- > 0) {
            float x = srcPts[srcOff++];
            float y = srcPts[srcOff++];
            double w = m20 * x + m21 * y + m22;

            if (w == 0) {
                dstPts[dstOff++] = x;
                dstPts[dstOff++] = y;
            } else {
                dstPts[dstOff++] = (float)((m00 * x + m01 * y + m02) / w);
                dstPts[dstOff++] = (float)((m10 * x + m11 * y + m12) / w);
            }
        }
    }

    /**
     * Transforms an array of double precision coordinates by this transform.
     * @param srcPts The array containing the source point coordinates.
     * Each point is stored as a pair of x,y coordinates.
     * @param dstPts The array where the transformed point coordinates are
     * returned.  Each point is stored as a pair of x,y coordinates.
     * @param srcOff The offset to the first point to be transformed
     * in the source array.
     * @param dstOff The offset to the location where the first transformed
     * point is stored in the destination array.
     * @param numPts The number of point objects to be transformed.
     * @throws IllegalArgumentException if srcPts is null
     * @throws ArrayIndexOutOfBoundsException if srcPts is too small
     */
    public void transform(double[] srcPts, int srcOff,
			  double[] dstPts, int dstOff,
			  int numPts) {

        if ( srcPts == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (dstPts == null) {
            dstPts = new double[numPts * 2 + dstOff];
        }

        while (numPts-- > 0) {
            double x = srcPts[srcOff++];
            double y = srcPts[srcOff++];
            double w = m20 * x + m21 * y + m22;

            if (w == 0) {
                dstPts[dstOff++] = x;
                dstPts[dstOff++] = y;
            } else {
                dstPts[dstOff++] = (m00 * x + m01 * y + m02) / w;
                dstPts[dstOff++] = (m10 * x + m11 * y + m12) / w;
            }
        }
    }

    /**
     * Transforms an array of floating point coordinates by this transform,
     * storing the results into an array of doubles.
     * @param srcPts The array containing the source point coordinates.
     * Each point is stored as a pair of x,y coordinates.
     * @param srcOff The offset to the first point to be transformed
     * in the source array.
     * @param dstPts The array where the transformed point coordinates are
     * returned.  Each point is stored as a pair of x,y coordinates.
     * @param dstOff The offset to the location where the first transformed
     * point is stored in the destination array.
     * @param numPts The number of points to be transformed.
     * @throws IllegalArgumentException if srcPts is null
     * @throws ArrayIndexOutOfBoundsException if srcPts is too small
     */
    public void transform(float[] srcPts, int srcOff,
			  double[] dstPts, int dstOff,
			  int numPts) {

        if ( srcPts == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (dstPts == null) {
            dstPts = new double[numPts * 2 + dstOff];
        }

        while (numPts-- > 0) {
            float x = srcPts[srcOff++];
            float y = srcPts[srcOff++];
            double w = m20 * x + m21 * y + m22;

            if (w == 0) {
                dstPts[dstOff++] = x;
                dstPts[dstOff++] = y;
            } else {
                dstPts[dstOff++] = (m00 * x + m01 * y + m02) / w;
                dstPts[dstOff++] = (m10 * x + m11 * y + m12) / w;
            }
        }
    }

    /**
     * Transforms an array of double precision coordinates by this transform,
     * storing the results into an array of floats.
     * @param srcPts The array containing the source point coordinates.
     * Each point is stored as a pair of x,y coordinates.
     * @param dstPts The array where the transformed point coordinates are
     * returned.  Each point is stored as a pair of x,y coordinates.
     * @param srcOff The offset to the first point to be transformed
     * in the source array.
     * @param dstOff The offset to the location where the first transformed
     * point is stored in the destination array.
     * @param numPts The number of point objects to be transformed.
     * @throws IllegalArgumentException if srcPts is null
     * @throws ArrayIndexOutOfBoundsException if srcPts is too small
     */
    public void transform(double[] srcPts, int srcOff,
			  float[] dstPts, int dstOff,
			  int numPts) {

        if ( srcPts == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (dstPts == null) {
            dstPts = new float[numPts * 2 + dstOff];
        }

        while (numPts-- > 0) {
            double x = srcPts[srcOff++];
            double y = srcPts[srcOff++];
            double w = m20 * x + m21 * y + m22;

            if (w == 0) {
                dstPts[dstOff++] = (float)x;
                dstPts[dstOff++] = (float)y;
            } else {
                dstPts[dstOff++] = (float)((m00 * x + m01 * y + m02) / w);
                dstPts[dstOff++] = (float)((m10 * x + m11 * y + m12) / w);
            }
        }
    }

    /**
     * Inverse transforms the specified ptSrc and stores the result in ptDst.
     * If ptDst is null, a new Point2D object will be allocated before
     * storing. In either case, ptDst containing the transformed point
     * is returned for convenience.
     * Note that ptSrc and ptDst can the same. In this case, the input
     * point will be overwritten with the transformed point.
     * @param ptSrc The point to be inverse transformed.
     * @param ptDst The resulting transformed point.
     * @throws NoninvertibleTransformException  if the matrix cannot be
     *                                         inverted.
     * @throws IllegalArgumentException if ptSrc is null
     */
    public Point2D inverseTransform(Point2D ptSrc, Point2D ptDst)
	throws NoninvertibleTransformException
    {
        if ( ptSrc == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (ptDst == null) {
	    if (ptSrc instanceof Point2D.Double) {
		ptDst = new Point2D.Double();
	    } else {
		ptDst = new Point2D.Float();
	    }
	}
	// Copy source coords into local variables in case src == dst
	double x = ptSrc.getX();
	double y = ptSrc.getY();

        double tmp_x = (m11*m22 - m12*m21) * x +
            (m02*m21 - m01*m22) * y +
            (m01*m12 - m02*m11);
        double tmp_y = (m12*m20 - m10*m22) * x +
            (m00*m22 - m02*m20) * y +
            (m02*m10 - m00*m12);
        double w = (m10*m21 - m11*m20) * x +
            (m01*m20 - m00*m21) * y +
            (m00*m11 - m01*m10);

        double wabs = w;
        if (w < 0) {
            wabs = - w;
        }
        if (wabs < PERSPECTIVE_DIVIDE_EPSILON) {
            throw new
		NoninvertibleTransformException(
				     JaiI18N.getString("PerspectiveTransform1"));
        }

        ptDst.setLocation(tmp_x/w, tmp_y/w);

        return ptDst;
    }

    /**
     * Inverse transforms an array of double precision coordinates by
     * this transform.
     * @param srcPts The array containing the source point coordinates.
     * Each point is stored as a pair of x,y coordinates.
     * @param dstPts The array where the transformed point coordinates are
     * returned.  Each point is stored as a pair of x,y coordinates.
     * @param srcOff The offset to the first point to be transformed
     * in the source array.
     * @param dstOff The offset to the location where the first transformed
     * point is stored in the destination array.
     * @param numPts The number of point objects to be transformed.
     * @throws NoninvertibleTransformException  if the matrix cannot be
     *                                         inverted.
     * @throws IllegalArgumentException if srcPts is null
     * @throws ArrayIndexOutOfBoundsException if srcPts is too small
     * @throws NoninvertibleTransformException transform cannot be inverted
     */
    public void inverseTransform(double[] srcPts, int srcOff,
                                 double[] dstPts, int dstOff,
                                 int numPts)
        throws NoninvertibleTransformException
    {
        if ( srcPts == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (dstPts == null) {
            dstPts = new double[numPts * 2 + dstOff];
        }

        while (numPts-- > 0) {
            double x = srcPts[srcOff++];
            double y = srcPts[srcOff++];

            double tmp_x = (m11*m22 - m12*m21) * x +
                (m02*m21 - m01*m22) * y +
                (m01*m12 - m02*m11);
            double tmp_y = (m12*m20 - m10*m22) * x +
                (m00*m22 - m02*m20) * y +
                (m02*m10 - m00*m12);
            double w = (m10*m21 - m11*m20) * x +
                (m01*m20 - m00*m21) * y +
                (m00*m11 - m01*m10);

            double wabs = w;
            if (w < 0) {
                wabs = - w;
            }
            if (wabs < PERSPECTIVE_DIVIDE_EPSILON) {
                throw new NoninvertibleTransformException(
				    JaiI18N.getString("PerspectiveTransform1"));
            }

            dstPts[dstOff++] = tmp_x / w;
            dstPts[dstOff++] = tmp_y / w;
        }
    }

    /**
     * Returns a String that represents the value of this Object.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Perspective transform matrix\n");
        sb.append(this.m00);
        sb.append("\t");
        sb.append(this.m01);
        sb.append("\t");
        sb.append(this.m02);
        sb.append("\n");
        sb.append(this.m10);
        sb.append("\t");
        sb.append(this.m11);
        sb.append("\t");
        sb.append(this.m12);
        sb.append("\n");
        sb.append(this.m20);
        sb.append("\t");
        sb.append(this.m21);
        sb.append("\t");
        sb.append(this.m22);
        sb.append("\n");
        return new String(sb);
    }

    /**
     * Returns the boolean true value if this PerspectiveTransform is an
     * identity transform. Returns false otherwise.
     */
    public boolean isIdentity() {
        return m01 == 0.0 && m02 == 0.0 &&
            m10 == 0.0 && m12 == 0.0 &&
            m20 == 0.0 && m21 == 0.0 &&
            m22 != 0.0 && m00/m22 == 1.0 && m11/m22 == 1.0;
    }

    /**
     * Returns a copy of this PerspectiveTransform object.
     */
    public Object clone() {
	try {
	    return super.clone();
	} catch (CloneNotSupportedException e) {
	    // this shouldn't happen, since we are Cloneable
	    throw new InternalError();
	}
    }


    /**
     * Tests if this PerspectiveTransform equals a supplied one.
     *
     * @param obj The PerspectiveTransform to be compared to this one.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof PerspectiveTransform)) {
            return false;
        }

        PerspectiveTransform a = (PerspectiveTransform)obj;

	return ((m00 == a.m00) && (m10 == a.m10) && (m20 == a.m20) &&
		(m01 == a.m01) && (m11 == a.m11) && (m21 == a.m21) &&
		(m02 == a.m02) && (m12 == a.m12) && (m22 == a.m22));
    }
}
