/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Mar 1, 2005
 * Time: 6:31:09 PM
 * To change this template use File | Settings | File Templates.
 */

public class splines {

    private splines() { }

/*  Subroutine to generate B-spline basis functions for open knot vectors

    order    = order of the B-spline basis function
    vertices = number of defining polygon vertices
    nbasis[] = array containing the basis functions
               nbasis[0] contains the basis function associated with B1 etc.
    t        = parameter value
    knots[]  = knot vector
*/

    static void basis(int order, double t, int vertices, int[] knots, double[] nbasis) {
        double[] temp = new double[knots.length - 1];

        /* calculate the first order basis functions n[i][1]	*/

        for (int i = 0; i < knots.length - 1; i++)
            if ((t >= knots[i]) && (t < knots[i + 1]))
                temp[i] = 1;
            else
                temp[i] = 0;

        /* calculate the higher order basis functions */

        for (int k = 2; k <= order; k++) {
            for (int i = 0; i < knots.length - k; i++) {
                // first term of the basis function recursion relation
                double d;
                if (temp[i] != 0)     /* if the lower order basis function is zero skip the calculation */
                    d = ((t - knots[i]) * temp[i]) / (knots[i + k - 1] - knots[i]);
                else
                    d = 0;

                // second term of the basis function recursion relation
                double e;
                if (temp[i + 1] != 0) /* if the lower order basis function is zero skip the calculation */
                    e = ((knots[i + k] - t) * temp[i + 1]) / (knots[i + k] - knots[i + 1]);
                else
                    e = 0;

                temp[i] = d + e;
            }
        }

        if (t == (double) knots[knots.length-1])		/*    pick up last point	*/
            temp[vertices-1] = 1;

        /* put in n array	*/

        System.arraycopy(temp, 0, nbasis, 0, vertices);
    }

/*
    Subroutine to generate a B-spline open knot vector with multiplicity
    equal to the order at the ends.

    order        = order of the basis function
    vertices     = the number of defining polygon vertices
    knots[]      = array containing the knot vector
*/

    static void knot(int vertices, int order, int[] knots) {
        knots[0] = 0;
        for (int i = 1; i < vertices + order; i++) {
            if ((i > order - 1) && (i < vertices + 1))
                knots[i] = knots[i - 1] + 1;
            else
                knots[i] = knots[i - 1];
        }
    }

/*  Subroutine to generate a B-spline curve using an uniform open knot vector

    polygon[][] = array containing the defining polygon vertices
                  polygon[0] contains the x-component of the vertex
                  polygon[1] contains the y-component of the vertex
                  polygon[2] contains the z-component of the vertex
    order       = order of the basis function
    curve[][]   = array containing the curve points

    curve and polygon can have an arbitrary number of dimensions
*/

    public static void bspline(int order, double[][] polygon, double[][] curve) {
        assert(polygon[0].length == curve[0].length);
        int[] knots = new int[polygon.length + order];
        double[] nbasis = new double[polygon.length];
        int dimensions = polygon[0].length;

        /* generate the uniform open knot vector */

        knot(polygon.length, order, knots);

        /* calculate the points on the bspline curve */

        double step = ((double) knots[knots.length-1]) / ((double) (curve.length - 1));

        double t = 0; // curve parameter value 0 <= t <= 1
        int icount = 0;
        for (int n = 0; n < curve.length; n++) {
            if ((double) knots[knots.length-1] - t < 5e-6)
                t = (double) knots[knots.length-1];

            // generate the basis function for this value of t
            basis(order, t, polygon.length, knots, nbasis);

            // generate a point on the curve
            for (int j = 0; j < dimensions; j++) {
                double x = 0;
                // Do local matrix multiplication
                for (int i = 0; i < polygon.length; i++) {
                    x += nbasis[i] * polygon[i][j];
                }
                curve[icount][j] = x;
            }
            icount++;
            t += step;
        }
    }

/*  Subroutine to generate rational B-spline basis functions--open knot vector

    c        = order of the B-spline basis function
    d        = first term of the basis function recursion relation
    e        = second term of the basis function recursion relation
    h[]	     = array containing the homogeneous weights
    npts     = number of defining polygon vertices
    nplusc   = constant -- npts + c -- maximum number of knot values
    r[]      = array containing the rationalbasis functions
               r[1] contains the basis function associated with B1 etc.
    t        = parameter value
    temp[]   = temporary array
    x[]      = knot vector
*/

    static void rbasis(int order, double t, int vertices, int[] knots, double[] weights, double[] nbasis) {
        double[] temp = new double[knots.length - 1];

        /* calculate the first order basis functions n[i][1]	*/

        for (int i = 0; i < knots.length - 1; i++)
            if ((t >= knots[i]) && (t < knots[i + 1]))
                temp[i] = 1;
            else
                temp[i] = 0;

        /* calculate the higher order basis functions */

        for (int k = 2; k <= order; k++) {
            for (int i = 0; i < knots.length - k; i++) {
                // first term of the basis function recursion relation
                double d;
                if (temp[i] != 0)     /* if the lower order basis function is zero skip the calculation */
                    d = ((t - knots[i]) * temp[i]) / (knots[i + k - 1] - knots[i]);
                else
                    d = 0;

                // second term of the basis function recursion relation
                double e;
                if (temp[i + 1] != 0) /* if the lower order basis function is zero skip the calculation */
                    e = ((knots[i + k] - t) * temp[i + 1]) / (knots[i + k] - knots[i + 1]);
                else
                    e = 0;

                temp[i] = d + e;
            }
        }

        if (t == (double) knots[knots.length-1])		/*    pick up last point	*/
            temp[vertices-1] = 1;

        /* calculate sum for denominator of rational basis functions */

        double sum = 0;
        for (int i = 0; i < vertices; i++) {
            sum = sum + temp[i] * weights[i];
        }

        /* form rational basis functions and put in r vector */

        for (int i = 0; i < vertices; i++) {
            if (sum != 0)
                nbasis[i] = (temp[i] * weights[i]) / sum;
            else
                nbasis[i] = 0;
        }
    }

/*  Subroutine to generate a rational B-spline curve using an uniform open knot vector

    b[]         = array containing the defining polygon vertices
                  b[1] contains the x-component of the vertex
                  b[2] contains the y-component of the vertex
                  b[3] contains the z-component of the vertex
	h[]			= array containing the homogeneous weighting factors
    k           = order of the B-spline basis function
    nbasis      = array containing the basis functions for a single value of t
    nplusc      = number of knot values
    npts        = number of defining polygon vertices
    p[,]        = array containing the curve points
                  p[1] contains the x-component of the point
                  p[2] contains the y-component of the point
                  p[3] contains the z-component of the point
    p1          = number of points to be calculated on the curve
    t           = parameter value 0 <= t <= npts - k + 1
    x[]         = array containing the knot vector
*/

    public static void rbspline(int order, double[][] polygon, double[] weights, double[][] curve) {
        assert(polygon[0].length == curve[0].length);
        assert(polygon.length == weights.length);
        int[] knots = new int[polygon.length + order];
        double[] nbasis = new double[polygon.length];
        int dimensions = polygon[0].length;

        /* generate the uniform open knot vector */

        knot(polygon.length, order, knots);

        /* calculate the points on the bspline curve */

        double step = ((double) knots[knots.length-1]) / ((double) (curve.length - 1));

        double t = 0; // curve parameter value 0 <= t <= 1
        int icount = 0;
        for (int n = 0; n < curve.length; n++) {
            if ((double) knots[knots.length-1] - t < 5e-6)
                t = (double) knots[knots.length-1];

            // generate the basis function for this value of t
            rbasis(order, t, polygon.length, knots, weights, nbasis);

            // generate a point on the curve
            for (int j = 0; j < dimensions; j++) {
                double x = 0;
                // Do local matrix multiplication
                for (int i = 0; i < polygon.length; i++) {
                    x += nbasis[i] * polygon[i][j];
                }
                curve[icount][j] = x;
            }
            icount++;
            t += step;
        }
    }

    public static void main(String[] args) {
        int vertices = 4;
        int order = 2;     /* second order, change to 4 to get fourth order */
        int points = 11;   /* eleven points on curve */

        double[][] polygon = {
                {1, 1, 1},
                {2, 3, 1},
                {4, 3, 1},
                {3, 1, 1}
        };
        double[][] curve = new double[points][3];

        bspline(order, polygon, curve);

        System.out.print("\nPolygon points\n\n");

        for (int i = 0; i < vertices; i++)
            System.out.print(" " + polygon[i][0] + " " + polygon[i][1] + " " + polygon[i][2] + " \n");

        System.out.print("\nCurve points\n\n");

        for (int i = 0; i < points; i++)
            System.out.print(" " + curve[i][0] + " " + curve[i][1] + " " + curve[i][2] + " \n");

        vertices = 4;
        order = 2;     /* second order, change to 4 to get fourth order */
        points = 11;   /* eleven points on curve */

        double[] weights = new double[vertices];

        for (int i=0; i < vertices; i++)
            weights[i] = 1.0;

        weights[2] = 0.25; /*  vary the homogeneous weighting factor 0, 0.25, 1.0, 5.0 */

        double[][] polygon2 = {
                {0, 0, 1},
                {1, 2, 1},
                {2.5, 0, 1},
                {4, 2, 1},
                {5, 0, 1}
        };

        curve = new double[points][3];

        rbspline(order, polygon, weights, curve);

        System.out.print("\nPolygon points\n\n");

        for (int i = 0; i < vertices; i++)
            System.out.print(" " + polygon[i][0] + " " + polygon[i][1] + " " + polygon[i][2] + " \n");

        System.out.print("\nCurve points\n\n");

        for (int i = 0; i < points; i++)
            System.out.print(" " + curve[i][0] + " " + curve[i][1] + " " + curve[i][2] + " \n");
    }

    public static double[] interpolate(double min, double max, double step, double[][] curve, double[] result) {
        Interpolator interpolator = new Interpolator();

        if (result == null)
            result = new double[(int)((max-min)/step)];

        for (int i = 0; i < result.length; i++)
            result[i] = interpolator.interpolate(min + step * i, curve);

        return result;
    }

    public static short[] interpolate(double min, double max, double step, double[][] curve, short[] result) {
        Interpolator interpolator = new Interpolator();

        if (result == null)
            result = new short[(int)((max-min)/step)];

        for (int i = 0; i < result.length; i++)
            result[i] = (short)((int) (interpolator.interpolate(min + step * i, curve) * 0xffff + 0.5) & 0xffff);

        return result;
    }

    public static class Interpolator {
        private int lastIdx = 0;

        public void reset() {
            lastIdx = 0;
        }

        public double interpolate(double x, double[][] curve) {
            if (x < curve[0][0])
                return x * curve[0][1] / curve[0][0];
            int last = curve.length - 1;
            if (x > curve[last][0])
                return curve[last][1] + (x - curve[last][0]) * (1 - curve[last][1]) / (1 - curve[last][0]);

            int i = lastIdx;
            while (i < curve.length-1 && curve[i+1][0] < x)
                i++;
            lastIdx = i;

            if (curve[i][0] == x)
                return curve[i][1];
            else if (curve[i][0] < x) {
                assert(curve[i+1][0] >= x);
                return curve[i][1] + (x - curve[i][0]) * (curve[i+1][1] - curve[i][1]) / (curve[i+1][0] - curve[i][0]);
            } else {
                assert(curve[i-1][0] <= x);
                return curve[i-1][1] + (x - curve[i-1][0]) * (curve[i][1] - curve[i-1][1]) / (curve[i][0] - curve[i-1][0]);
            }
        }
    }

    /*
        The b-spline curve comes out in parametric coordinates,
        we have to convert back to cartesian coordinates using interpolation
        This code assumes two dimensional curves
    */

    /* public static double interpolate(double x, double curve[][]) {
        if (x < curve[0][0])
            return x * curve[0][1] / curve[0][0];
        int last = curve.length - 1;
        if (x > curve[last][0])
            return curve[last][1] + (x - curve[last][0]) * (1 - curve[last][1]) / (1 - curve[last][0]);

        // assert(x >= curve[0][0] && x<= curve[curve.length-1][0]);

        int i = search(x, curve);

        if (curve[i][0] == x)
            return curve[i][1];
        else if (curve[i][0] < x) {
            assert(curve[i+1][0] >= x);
            return curve[i][1] + (x - curve[i][0]) * (curve[i+1][1] - curve[i][1]) / (curve[i+1][0] - curve[i][0]);
        } else {
            assert(curve[i-1][0] <= x);
            return curve[i-1][1] + (x - curve[i-1][0]) * (curve[i][1] - curve[i-1][1]) / (curve[i][0] - curve[i-1][0]);
        }
    } */

    // finds one of the extremas of the curve segment where the point x belongs to
    // we perform a binary search, this is probably overkill since we normally scan
    // the curve sequentially...
    /* public static int search(double x, double curve[][]) {
        int low = 0;
        int high = curve.length - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (curve[mid][0] < x && curve[mid + 1][0] < x)
                low = mid + 1;
            else if (curve[mid][0] > x && curve[mid - 1][0] > x)
                high = mid - 1;
            else
                return mid;
        }
        return -1;
    } */
}
