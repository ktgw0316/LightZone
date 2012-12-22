/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.file;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * A <code>OrderableRandomAccessFile</code> is like a {@link RandomAccessFile}
 * except that its read and write methods can alternatively be done in
 * little-endian order.
 * <p>
 * The reason this class isn't derived from {@link RandomAccessFile} is because
 * {@link RandomAccessFile} stupidly has its read and write methods declared
 * <code>final</code>.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class OrderableRandomAccessFile
    implements Closeable, DataInput, DataOutput {

    ////////// public /////////////////////////////////////////////////////////

    public OrderableRandomAccessFile( File file, String mode )
        throws FileNotFoundException
    {
        this( new RandomAccessFile( file, mode ) );
    }

    public OrderableRandomAccessFile( String fileName, String mode )
        throws FileNotFoundException
    {
        this( new RandomAccessFile( fileName, mode ) );
    }

    public OrderableRandomAccessFile( RandomAccessFile file ) {
        this( file, ByteOrder.BIG_ENDIAN );
    }

    public OrderableRandomAccessFile( RandomAccessFile file, ByteOrder order ) {
        m_file = file;
        m_order = order;
    }

    public synchronized void close() throws IOException {
        if ( m_file != null ) {
            m_file.close();
            m_file = null;
        }
    }

    public FileChannel getChannel() {
        return m_file.getChannel();
    }

    public FileDescriptor getFD() throws IOException {
        return m_file.getFD();
    }

    public long getFilePointer() throws IOException {
        return m_file.getFilePointer();
    }

    public RandomAccessFile getRandomAccessFile() {
        return m_file;
    }

    public long length() throws IOException {
        return m_file.length();
    }

    public ByteOrder order() {
        return m_order;
    }

    public void order( ByteOrder order ) {
        m_order = order;
    }

    public int read() throws IOException {
        return m_file.read();
    }

    public int read( byte[] b ) throws IOException {
        return m_file.read( b );
    }

    public int read( byte[] b, int off, int len ) throws IOException {
        return m_file.read( b, off, len );
    }

    public boolean readBoolean() throws IOException {
        return m_file.readBoolean();
    }

    public byte readByte() throws IOException {
        return m_file.readByte();
    }

    public char readChar() throws IOException {
        char c = m_file.readChar();
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            c = Character.reverseBytes( c );
        return c;
    }

    public double readDouble() throws IOException {
        long n = m_file.readLong();
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            n = Long.reverseBytes( n );
        return Double.longBitsToDouble( n );
    }

    public float readFloat() throws IOException {
        int n = m_file.readInt();
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            n = Integer.reverseBytes( n );
        return Float.intBitsToFloat( n );
    }

    public void readFully( byte[] b ) throws IOException {
        m_file.readFully( b );
    }

    public void readFully( byte[] b, int off, int len ) throws IOException {
        m_file.readFully( b, off, len );
    }

    public int readInt() throws IOException {
        int n = m_file.readInt();
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            n = Integer.reverseBytes( n );
        return n;
    }

    public String readLine() throws IOException {
        return m_file.readLine();
    }

    public long readLong() throws IOException {
        long n = m_file.readLong();
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            n = Long.reverseBytes( n );
        return n;
    }

    public short readShort() throws IOException {
        short n = m_file.readShort();
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            n = Short.reverseBytes( n );
        return n;
    }

    public int readUnsignedByte() throws IOException {
        return m_file.readUnsignedByte();
    }

    public int readUnsignedShort() throws IOException {
        int n = m_file.readUnsignedShort();
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            n = Short.reverseBytes( (short)n );
        return n & 0xFFFF;
    }

    public String readUTF() throws IOException {
        return m_file.readUTF();
    }

    public void seek( long pos ) throws IOException {
        m_file.seek( pos );
    }

    public void setLength( long newLength ) throws IOException {
        m_file.setLength( newLength );
    }

    public int skipBytes( int n ) throws IOException {
        return m_file.skipBytes( n );
    }

    public void write( byte[] b ) throws IOException {
        m_file.write( b );
    }

    public void write( byte[] b, int off, int len ) throws IOException {
        m_file.write( b, off, len );
    }

    public void write( int b ) throws IOException {
        m_file.write( b );
    }

    public void writeBoolean( boolean v ) throws IOException {
        m_file.writeBoolean( v );
    }

    public void writeByte( int v ) throws IOException {
        m_file.writeByte( v );
    }

    public void writeBytes( String s ) throws IOException {
        m_file.writeBytes( s );
    }

    public void writeChar( int v ) throws IOException {
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            v = Short.reverseBytes( (short)v );
        m_file.writeChar( v );
    }

    public void writeChars( String s ) throws IOException {
        for ( int i = 0; i < s.length(); ++i )
            writeChar( s.charAt( i ) );
    }

    public void writeDouble( double v ) throws IOException {
        long n = Double.doubleToLongBits( v );
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            n = Long.reverseBytes( n );
        m_file.writeLong( n );
    }

    public void writeFloat( float v ) throws IOException {
        int n = Float.floatToIntBits( v );
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            n = Integer.reverseBytes( n );
        m_file.writeInt( n );
    }

    public void writeInt( int v ) throws IOException {
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            v = Integer.reverseBytes( v );
        m_file.writeInt( v );
    }

    public void writeLong( long v ) throws IOException {
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            v = Long.reverseBytes( v );
        m_file.writeLong( v );
    }

    public void writeShort( int v ) throws IOException {
        if ( m_order == ByteOrder.LITTLE_ENDIAN )
            v = Short.reverseBytes( (short)v );
        m_file.writeShort( v );
    }

    public void writeUTF( String s ) throws IOException {
        m_file.writeUTF( s );
    }

    ////////// protected //////////////////////////////////////////////////////

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    ////////// private ////////////////////////////////////////////////////////

    private RandomAccessFile m_file;

    private ByteOrder m_order;
}
/* vim:set et sw=4 ts=4: */
