/* @(#)RingBuffer.java	1.2 02/10/24 21:03:23 */
package tilecachetool;

/**
 * <p>Title: Tile Cache Monitoring Tool</p>
 * <p>Description: Monitors and displays JAI Tile Cache activity.</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>    All Rights Reserved</p>
 * <p>Company: Virtual Visions Software, Inc.</p>
 *
 * @author Dennis Sigel
 * @version 1.01
 *
 * Circular array (fixed length)
 */

public final class RingBuffer {

    private int size  = 100; // minimum default size
    private int start = 0;
    private int count = 0;
    private int write_pointer = 0;
    private boolean filled = false;
    private float[] array;


    /**
     * Constructor
     * @param size ring buffer array dimension
     */
    public RingBuffer(int size) {
        if ( size > 0 ) {
            this.size = size;
        } else {
            this.size = 100;
        }

        array = new float[this.size];
    }

    /**
     * Changes the ring buffer array dimension
     * @param new_size The new ring buffer dimension
     * @throws IllegalArgumentException if <code>new_size</code> .lt. 2
     * @since TCT 1.0
     */
    public synchronized void setSize(int new_size) {
        int old_size = size;

        if ( size < 2 ) {
            throw new IllegalArgumentException("Invalid size.");
        }

        int min = 0;

        if ( old_size < new_size ) {
            filled = false;
            min = old_size;
        } else {
            filled = true;
            min = new_size;
        }

        float[] temp = array;
        array = new float[new_size];

        for ( int i = 0; i < min; i++ ) {
            array[i] = temp[i];
        }

        count = min;
        size  = new_size;
    }

    /**
     * Returns the current number of entries in the ring buffer,
     * up to the ring buffer size.
     */
    public int getCount() {
        return count;
    }

    /**
     * Returns the ring buffer size.
     */
    public int getSize() {
        return size;
    }

    /**
     * Write a data point into the ring buffer
     * @param data Floating point value of JVM memory usage.
     */
    public synchronized void write(float data) {
        if ( filled == false ) {
            if ( count == size ) {
                filled = true;
            } else {
                count += 1;
            }
        }

        if ( filled ) {
            start = (start + 1) % size;
        }

        array[write_pointer] = data;
        write_pointer = (write_pointer + 1) % size;
    }

    /**
     * Read a data point value from the ring buffer.  Returns the
     * value as a floating point number.
     * @param i Data index offset from the starting index.
     */
    public synchronized float read(int i) {
        if ( i < 0 ) i = 0;
        return array[(start + i) % size];
    }
}

