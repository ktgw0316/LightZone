package com.lightcrafts.utils.cache;

import com.lightcrafts.utils.bytebuffer.ByteBufferAllocator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class LRUCacheObjectMapTest {

    @Test
    void getOnceFreesTheBufferWhetherDecodingSucceedsOrFails() throws IOException {
        final TrackingAllocator allocator = new TrackingAllocator();
        final TestBroker broker = new TestBroker();
        final LRUCacheObjectMap objectMap = new LRUCacheObjectMap( allocator, 16 );
        final Cache cache = new Cache(
            broker, objectMap, new EmptyCacheStore(), new CoalescingFreeBlockManager()
        );

        try {
            cache.put( "entry", new byte[]{ 1, 2, 3 } );
            assertArrayEquals(
                new byte[]{ 1, 2, 3 },
                (byte[])cache.getOnce( "entry", new byte[3] )
            );
            assertEquals( 1, allocator.freedBuffers );

            broker.failDecoding = true;
            cache.put( "failing-entry", new byte[]{ 4, 5, 6 } );
            assertThrows(
                IllegalStateException.class,
                () -> cache.getOnce( "failing-entry", new byte[3] )
            );
            assertEquals( 2, allocator.freedBuffers );
        } finally {
            cache.dispose();
        }
    }

    private static final class TrackingAllocator implements ByteBufferAllocator {
        private final NativeByteBufferAllocator delegate = new NativeByteBufferAllocator( 16 );
        private int freedBuffers;

        @Override
        public ByteBuffer allocByteBuffer( int size ) {
            return delegate.allocByteBuffer( size );
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public void dispose() {
            delegate.dispose();
        }

        @Override
        public boolean freeByteBuffer( ByteBuffer buf ) {
            final boolean freed = delegate.freeByteBuffer( buf );
            if ( freed )
                ++freedBuffers;
            return freed;
        }
    }

    private static final class TestBroker implements CacheObjectBroker {
        private boolean failDecoding;

        @Override
        public Object decodeFromByteBuffer( ByteBuffer buf, Object aux ) {
            if ( failDecoding )
                throw new IllegalStateException( "decode failed" );
            buf.get( (byte[])aux );
            return aux;
        }

        @Override
        public void encodeToByteBuffer( ByteBuffer buf, Object obj ) {
            buf.put( (byte[])obj );
        }

        @Override
        public int getEncodedSizeOf( Object obj ) {
            return ((byte[])obj).length;
        }
    }

    private static final class EmptyCacheStore implements CacheStore {
        @Override
        public void clear() {
        }

        @Override
        public void dispose() {
        }

        @Override
        public ByteBuffer getByteBuffer( int size ) {
            return ByteBuffer.allocate( size );
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public void readFromStore( long pos, ByteBuffer buf ) {
        }

        @Override
        public void writeToStore( long pos, ByteBuffer buf ) {
        }
    }
}
