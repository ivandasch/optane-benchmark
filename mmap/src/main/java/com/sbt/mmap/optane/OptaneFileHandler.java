package com.sbt.mmap.optane;

import java.nio.ByteBuffer;

/**
 *
 */
public class OptaneFileHandler implements com.sbt.mmap.FileHandler {
    static {
        Util.loadLibrary();
    }

    /** */
    private final Context ctx;

    /** */
    private final String path;

    /**
     * @param path File path.
     */
    public OptaneFileHandler(String path, long size) {
        this.path = path;
        this.ctx = mmap(this.path, size);
    }

    /** {@inheritDoc} */
    @Override public void close() throws Exception {
        fsync();
        munmap(ctx.buffer);
    }

    public boolean isPersistentMemory() {
        return ctx.isPmem;
    }

    public ByteBuffer getBuffer() {
        return this.ctx.buffer;
    }

    public void fsync() throws Exception {
        fsync(0, ctx.buffer.capacity());
    }

    public void fsync(long offset, long size) throws Exception {
        force(ctx.buffer, offset, size, ctx.isPmem);
    }

    /** */
    private static native Context mmap(String path, long size);

    /** */
    private static native void munmap(ByteBuffer buffer);

    /** */
    private static native void force(ByteBuffer buffer, long address, long size, boolean isPmem);

    /** */
    static class Context {
        /** */
        private final ByteBuffer buffer;

        /** */
        private final boolean isPmem;

        /** */
        Context(ByteBuffer buffer, boolean isPmem) {
            this.buffer = buffer;
            this.isPmem = isPmem;
        }
    }
}
