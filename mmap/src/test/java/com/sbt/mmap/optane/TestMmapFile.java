package com.sbt.mmap.optane;

import com.sbt.mmap.FileHandler;
import com.sbt.mmap.java.JavaFileHandler;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestMmapFile {
    public static final String path = "/home/ivandasch/mem/test.dat";

    private static final long size = 1024 * 1024 + 189;

    @Test
    public void testIntel() throws Exception {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        byte[] data = new byte[512 * 1024];
        rnd.nextBytes(data);

        int offset = 128 * 1024;
        try(FileHandler hnd = FileHandler.getHandler(path, size, FileHandler.TYPE.OPTANE)) {
            ByteBuffer buf = hnd.getBuffer();

            buf.position(offset);
            buf.put(data, 0, data.length);

            hnd.fsync(offset, data.length);

            checkData(path, data, offset);
        }
        checkData(path, data, offset);
    }

    @Test
    public void testErrors() throws Exception {
        try {
            FileHandler hnd = FileHandler.getHandler("/test.dat", 1024, FileHandler.TYPE.OPTANE);
            fail("Expected failure");
        }
        catch (Exception e) {
            assertTrue(e.getMessage().contains("errno=13 Permission denied"));
        }
    }

    @Before
    public void setUp() throws Exception {
        Files.deleteIfExists(Paths.get(path));
    }

    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(Paths.get(path));
    }

    @Test
    public void testJava() throws Exception {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        byte[] data = new byte[512 * 1024];
        rnd.nextBytes(data);

        int offset = 128 * 1024;
        try(FileHandler hnd = FileHandler.getHandler(path, size, FileHandler.TYPE.JAVA)) {
            ByteBuffer buf = hnd.getBuffer();

            buf.position(offset);
            buf.put(data, 0, data.length);

            hnd.fsync(offset, data.length);

            checkData(path, data, offset);
        }
        checkData(path, data, offset);
    }

    private void checkData(String path, byte[] data, long offset) throws Exception {
        try(FileInputStream stream = new FileInputStream(path)) {
            assert stream.skip(offset) == offset;

            byte[] buf = new byte[data.length];

            assertTrue(stream.read(buf) > 0);
            assertArrayEquals(buf, data);
        }
    }

}
