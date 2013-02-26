/*
 * $RCSfile: IntegerSequence.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:10 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.util.NoSuchElementException;

/**
 * A growable sorted integer set.  Adding an integer to the sequence
 * results in it being placed into the sequence in sorted order.  Adding
 * an integer that is already part of the sequence has no effect. 
 *
 * <p> This structure is used by various subclasses of
 * <code>OpImage</code> to keep track of horizontal and vertical
 * source splits.  Each instance of IntegerSequence provides an
 * internal enumeration by means of which the elements of the sequence
 * may be accessed in order.  The enumerator is initialized by the
 * <code>startEnumeration</code> method, and the
 * <code>hasMoreElements</code> and <code>nextElement</code> methods
 * allow looping through the elements.  Only one enumeration at a time
 * is supported.  Calling <code>insert()</code> from multiple threads
 * is not supported.
 *
 */
public class IntegerSequence extends Object {

    /** Lower bound of the valid integer range. */
    private int min;

    /** Upper bound of the valid integer range. */
    private int max;

    /** The default initial capacity of iArray. */
    private static final int DEFAULT_CAPACITY = 16;

    /** The array storing the unsorted integer values. */
    private int[] iArray = null;

    /** The capacity of iArray. */
    private int capacity = 0;

    /** The number of (non-unique) elements actually stored in iArray. */
    private int numElts = 0;

    /** True if iArray has been sorted and purged of duplicates. */
    private boolean isSorted = false;

    /** The current element of the iteration. */
    private int currentIndex = -1;

    /** Constructs a sequence bounded by an inclusive range of values.
     *  @param min Lower bound of the valid integer range.
     *  @param max Upper bound of the valid integer range.
     *  @throws  IllegalArgumentException if min > max.
     */
    public IntegerSequence(int min, int max) {
        if (min > max)
	  throw new IllegalArgumentException(JaiI18N.getString("IntegerSequence1"));
        this.min = min;
        this.max = max;

        this.capacity = DEFAULT_CAPACITY;
        this.iArray = new int[capacity];
        this.numElts = 0;
        this.isSorted = true;
    }

    /** Constructs a sequence that may contain any integer value. */
    public IntegerSequence() {
        this(java.lang.Integer.MIN_VALUE, java.lang.Integer.MAX_VALUE);
    }

    /**
     * Inserts an integer into the sequence.  If the value falls out
     * of the desired range, it will be silently rejected.  Inserting
     * an element that is already a member of the sequence has no
     * effect.
     *
     * @param element The <code>int</code> to be inserted.
     */
    public void insert(int element) {
      // Ignore elements that fall outside the desired range. 
        if (element < min || element > max) {
            return;
        }

        if (numElts >= capacity) {
            int newCapacity = 2*capacity;
            int[] newArray = new int[newCapacity];
            System.arraycopy(iArray, 0, newArray, 0, capacity);

            this.capacity = newCapacity;
            this.iArray = newArray;
        }
        isSorted = false;
        iArray[numElts++] = element;
    }

    /** Resets the iterator to the beginning of the sequence. */
    public void startEnumeration() {
        if (!isSorted) {
            // Sort the contents of iArray
            java.util.Arrays.sort(iArray, 0, numElts);
            
            // Compact the array, removing duplicate entries.
            int readPos = 1;
            int writePos = 1;
            int prevElt = iArray[0];

            //
            // Loop invariants: writePos <= readPos
            //                  iArray[0..readPos - 1] contains no duplicates
            //
            for (readPos = 1; readPos < numElts; ++readPos) {
                int currElt = iArray[readPos];
                if (currElt != prevElt) {
                    iArray[writePos++] = currElt;
                    prevElt = currElt;
                }
            }
            
            numElts = writePos;
            isSorted = true;
        }

        currentIndex = 0;
    }

    /** Returns true if more elements are available to be iterated over. */
    public boolean hasMoreElements() {
        return currentIndex < numElts;
    }

    /**
     * Returns the next element of the iteration in ascending order.
     * If the end of the array has been reached, a
     * <code>java.util.NoSuchElementException will be thrown.
     *
     * @throws NoSuchElementException if the end of the array has
     * been reached.
     */
     public int nextElement() {
         if (currentIndex < numElts) {
             return iArray[currentIndex++];
         } else {
             throw new NoSuchElementException(JaiI18N.getString("IntegerSequence0"));
         }
     }

    /**
     * Returns the number of elements contained within this <code>IntegerSequence</code>.
     */
    public int getNumElements() {
	return numElts;
    }

    /** Returns a <code>String</code> representation of the sequence. */
    public String toString() {
        String s;
        int i;

        if (numElts == 0) {
            s = "[<empty>]";
        } else {
            s = "[";

            startEnumeration();
            for (i = 0; i < numElts - 1; i++) { 
                s += iArray[i];
                s += ", ";
            }
            
            s += iArray[numElts - 1];
            s += "]";
        }
        
        return s;
    }
}
