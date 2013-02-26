/*
 * $RCSfile: Rational.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/04/29 23:19:18 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.util;

/**
 * A class to perform Rational arithmetic.
 *
 * @since 1.0
 */
public class Rational {
    
    public long num;
    public long denom;
    
    public Rational(long num, long denom) {
        this.num = num;
        this.denom = denom;
    }

    public Rational(Rational r) {
	this.num = r.num;
	this.denom = r.denom;
    }

    /**
     * Returns a Rational defined by a given number of terms
     * of a continued fraction:
     *
     * terms[0] +          1
     *            -------------------------
     *            terms[1] +          1
     *                       --------------
     *                       terms[2] + ...
     */     
    public static Rational createFromFrac(long[] terms, int len) {
        Rational r = new Rational(0, 1);
        for (int i = len - 1; i >= 0; i--) {
            r.add(terms[i]);
            if (i != 0) {
                r.invert();
            }
        }
	
        return r;
    }
    
    private static final int MAX_TERMS = 20;
    
    /**
     * Returns a Rational that is within the given tolerance
     * of a given float value.
     */
    public static Rational approximate(float f, float tol) {
        // Expand f as a continued fraction by repeatedly removing the integer
        // part and inverting.
        float rem = f;
        long[] d = new long[MAX_TERMS];
        int index = 0;
        for (int i = 0; i < MAX_TERMS; i++) {
            int k = (int)Math.floor(rem);
            d[index++] = k;
	    
            rem -= k;
            if (rem == 0) {
                break;
            }
            rem = 1.0F/rem;
        }
	
        // Evaluate with increasing number of terms until the tolerance
        // has been reached
        Rational r = null;
        for (int i = 1; i <= index; i++) {
            r = Rational.createFromFrac(d, i);
            if (Math.abs(r.floatValue() - f) < tol) {
                return r;
            }
        }

        return r;
    }

    /**
     * Returns a Rational that is within the given tolerance
     * of a given double value.
     */
    public static Rational approximate(double f, double tol) {
        // Expand f as a continued fraction by repeatedly removing the integer
        // part and inverting.
        double rem = f;
        long[] d = new long[MAX_TERMS];
        int index = 0;
        for (int i = 0; i < MAX_TERMS; i++) {
            long k = (long)Math.floor(rem);
            d[index++] = k;
	    
            rem -= k;
            if (rem == 0) {
                break;
            }
            rem = 1.0F/rem;
        }
	
        // Evaluate with increasing number of terms until the tolerance
        // has been reached
        Rational r = null;
        for (int i = 1; i <= index; i++) {
            r = Rational.createFromFrac(d, i);
            if (Math.abs(r.doubleValue() - f) < tol) {
                return r;
            }
        }

        return r;
    }

    private static long gcd(long m, long n) {
        if (m < 0) {
            m = -m;
        }
        if (n < 0) {
            n = -n;
        }

        while (n > 0) {
            long tmp = m % n;
            m = n;
            n = tmp;
        }
        return m;
    }

    /** Reduces the internal representation to lowest terms. */
    private void normalize() {
        if (denom < 0) {
            num = -num;
            denom = -denom;
        }

        long gcd = gcd(num, denom);
        if (gcd > 1) {
            num /= gcd;
            denom /= gcd;
        }
    }

    /**
     * Adds an integer to this Rational value.
     */
    public void add(long i) {
        num += i*denom;
        normalize();
    }

    /**
     * Adds an integer to this Rational value.
     */
    public void add(Rational r) {
	num = num * r.denom + r.num * denom;
	denom *= r.denom;
        normalize();
    }

    /**
     * Subtracts an int from this Rational value.
     */
    public void subtract(long i) {
        num -= i*denom;
        normalize();
    }

    /**
     * Subtracts an integer to this Rational value.
     */
    public void subtract(Rational r) {
	num = num * r.denom - r.num * denom;
	denom *= r.denom;
        normalize();
    }

    /**
     * Multiplies an integer to this Rational value.
     */
    public void multiply(long i) {
	num *= i;
	normalize();
    }

    /**
     * Multiplies a Rational to this Rational value.
     */
    public void multiply(Rational r) {
	num *= r.num;
	denom *= r.denom;
	normalize();
    }

    /**
     * Inverts this Rational value.
     */
    public void invert() {
        long tmp = num;
        num = denom;
        denom = tmp;
    }

    /**
     * Returns the approximate float value of this Rational.
     */
    public float floatValue() {
        return (float)num/denom;
    }

    /**
     * Returns the approximate double value of this Rational.
     */
    public double doubleValue() {
        return (double)num/denom;
    }

    /**
     * Returns this Rational as a String in the form '###/###'.
     */
    public String toString() {
        return num + "/" + denom;
    }

    /**
     * Returns the ceil (equivalent of Math.ceil())
     */
    public static int ceil(long num, long denom) {
	
	int ret = (int)(num/denom);

	if (num > 0) {
	    if ((num % denom) != 0) {
		ret += 1;
	    }
	}

	return ret;
    }

    /**
     * Returns the floor (equivalent of Math.floor())
     */
    public static int floor(long num, long denom) {

	int ret = (int)(num/denom);

	if (num < 0) {
	    if ((num % denom) != 0) {
		ret -= 1;
	    }
	}

	return ret;
    }

    /**
     * Prints out rational approximations of a floating point argument
     * with 1 to 8 digits of accuracy.
     */
    public static void main(String[] args) {
        float f = Float.parseFloat(args[0]);
        for (int i = 1; i < 15; i++) {
            Rational r = Rational.approximate(f, (float)Math.pow(10, -i));
            System.out.println(r + " = " + r.floatValue());
        }
    }
}
