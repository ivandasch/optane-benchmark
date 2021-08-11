package com.sbt.mmap;

import com.sbt.mmap.java.JavaFileHandler;
import com.sbt.mmap.optane.OptaneFileHandler;
import java.nio.ByteBuffer;

public interface FileHandler extends AutoCloseable {
    ByteBuffer getBuffer();
    void fsync() throws Exception;
    void fsync(long off, long len) throws Exception;

    static FileHandler getHandler(String path, long size, TYPE type) {
        switch (type) {
            case JAVA:
                return new JavaFileHandler(path, size);
            case OPTANE:
                return new OptaneFileHandler(path, size);
            default:
                throw new IllegalArgumentException("Not supported type of file handler: " + type);
        }
    }

    enum TYPE {
        OPTANE,
        JAVA
    }
}
