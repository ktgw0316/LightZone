package com.lightcrafts.utils.cache;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class NativeByteBufferAllocatorTest {

    @Test
    void allocatesIndependentDirectViewsFromAChunk() {
        final NativeByteBufferAllocator allocator = new NativeByteBufferAllocator( 8 );
        try {
            final ByteBuffer first = allocator.allocByteBuffer( 4 );
            final ByteBuffer second = allocator.allocByteBuffer( 4 );

            assertTrue( first.isDirect() );
            assertTrue( second.isDirect() );
            assertEquals( 4, first.capacity() );
            assertEquals( 4, second.capacity() );
            first.putInt( 0, 0x12345678 );
            second.putInt( 0, 0x10203040 );
            assertEquals( 0x12345678, first.getInt( 0 ) );
            assertEquals( 0x10203040, second.getInt( 0 ) );
        } finally {
            allocator.dispose();
        }
    }

    @Test
    void freesOnlyTheExactAllocatedBufferAndReusesItsBlock() {
        final NativeByteBufferAllocator allocator = new NativeByteBufferAllocator( 4 );
        try {
            final ByteBuffer original = allocator.allocByteBuffer( 4 );
            original.putInt( 0, 1 );

            assertFalse( allocator.freeByteBuffer( original.duplicate() ) );
            assertTrue( allocator.freeByteBuffer( original ) );
            assertFalse( allocator.freeByteBuffer( original ) );

            final ByteBuffer reused = allocator.allocByteBuffer( 4 );
            reused.putInt( 0, 2 );
            assertEquals( 2, original.getInt( 0 ) );
        } finally {
            allocator.dispose();
        }
    }

    @Test
    void rejectsAllocationsLargerThanAChunk() {
        final NativeByteBufferAllocator allocator = new NativeByteBufferAllocator( 4 );
        try {
            assertThrows(
                IllegalArgumentException.class,
                () -> allocator.allocByteBuffer( 5 )
            );
        } finally {
            allocator.dispose();
        }
    }
}
