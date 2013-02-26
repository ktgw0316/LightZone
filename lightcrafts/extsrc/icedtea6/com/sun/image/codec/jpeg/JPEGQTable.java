/* JPEGQTable.java --
   Copyright (C) 2011 Red Hat
   Copyright (C) 2007 Free Software Foundation, Inc.
   Copyright (C) 2007 Matthew Flaschen

   This file is part of GNU Classpath.

   GNU Classpath is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2, or (at your option)
   any later version.

   GNU Classpath is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with GNU Classpath; see the file COPYING.  If not, write to the
   Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
   02110-1301 USA.

   Linking this library statically or dynamically with other modules is
   making a combined work based on this library.  Thus, the terms and
   conditions of the GNU General Public License cover the whole
   combination.

   As a special exception, the copyright holders of this library give you
   permission to link this library with independent modules to produce an
   executable, regardless of the license terms of these independent
   modules, and to copy and distribute the resulting executable under
   terms of your choice, provided that you also meet, for each linked
   independent module, the terms and conditions of the license of that
   module.  An independent module is a module which is not derived from
   or based on this library.  If you modify this library, you may extend
   this exception to your version of the library, but you are not
   obligated to do so.  If you do not wish to do so, delete this
   exception statement from your version. */

package com.sun.image.codec.jpeg;

import java.util.Arrays;

/**
 * Class to encapsulate the JPEG quantization tables.
 *
 * Note: The tables K1Luminance, K1Div2Luminance, K2Chrominance,
 * K2Div2Chrominance is an instance of the superclass.
 *
 * @author Andrew Su (asu@redhat.com)
 *
 */
public class JPEGQTable {

    /**
     * Luminance quantization table (in zig-zag order).
     */
    public static final JPEGQTable StdLuminance;

    /**
     * Chromninance quantization table (in zig-zag order).
     */
    public static final JPEGQTable StdChrominance;

    static {
        /* table for luminance values in zig-zag order */
        int[] table1 = { 16, 11, 12, 14, 12, 10, 16, 14, 13, 14, 18, 17, 16,
                19, 24, 40, 26, 24, 22, 22, 24, 49, 35, 37, 29, 40, 58, 51, 61,
                60, 57, 51, 56, 55, 64, 72, 92, 78, 64, 68, 87, 69, 55, 56, 80,
                109, 81, 87, 95, 98, 103, 104, 103, 62, 77, 113, 121, 112, 100,
                120, 92, 101, 103, 99 };

        StdLuminance = new JPEGQTable(table1);

        /* table for chrominance values in zig-zag order */
        int[] table2 = { 17, 18, 18, 24, 21, 24, 47, 26, 26, 47, 99, 66, 56,
                66, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
                99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
                99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
                99, 99, 99 };
        StdChrominance = new JPEGQTable(table2);
    }

    private int[] table;

    /**
     * Constructs an quantization table from the array that was passed. The
     * coefficients must be in zig-zag order. The array must be of length 64.
     * The table will be copied.
     *
     * @param table
     *            the quantization table, as an int array.
     * @throws IllegalArgumentException
     *             if table is null or table.length is not equal to 64.
     */
    public JPEGQTable(int[] table) {
        /* Table must be 8x8 thus 64 entries */
        if (table == null || table.length != 64) {
            throw new IllegalArgumentException("Not a valid table.");
        }
        this.table = Arrays.copyOf(table, table.length);
    }

    public int[] getTable() {
        return Arrays.copyOf(table, table.length);
    }

    public JPEGQTable getScaledInstance(float scaleFactor, boolean forceBaseline) {
        int limit = (forceBaseline) ? 255 : 32767;
        int[] newTable = new int[table.length];
        for (int i = 0; i < table.length; i++) {
            int newValue = Math.round(table[i] * scaleFactor);
            newTable[i] = (newValue < 1) ? 1 : (newValue > limit) ? limit : newValue;
        }
        return new JPEGQTable(newTable);
    }

}
