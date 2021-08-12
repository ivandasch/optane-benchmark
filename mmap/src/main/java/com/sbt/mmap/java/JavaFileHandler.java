package com.sbt.mmap.java;

import com.sbt.mmap.FileHandler;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import sun.misc.Unsafe;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

public class JavaFileHandler implements FileHandler {
    /** */
    private static final Method force0 = findNonPublicMethod(
        MappedByteBuffer.class, "force0",
        java.io.FileDescriptor.class, long.class, long.class
    );

    /** */
    private static final Method mappingOffset = findNonPublicMethod(MappedByteBuffer.class, "mappingOffset");

    /** */
    private static final Method mappingAddress = findNonPublicMethod(
        MappedByteBuffer.class, "mappingAddress", long.class
    );

    /**  */
    private static final Field fd = findField(MappedByteBuffer.class, "fd");

    /** */
    private static final Method cleanerMtd = findNonPublicMethod(ByteBuffer.class, "invokeCleaner");


    /** */
    private static int PAGE_SIZE;

    /** */
    private static Unsafe UNSAFE;

    static {
        try {
            UNSAFE = Unsafe.getUnsafe();
        }
        catch (SecurityException ignored) {
            try {
                UNSAFE = AccessController.doPrivileged(
                    (PrivilegedExceptionAction<Unsafe>)() -> {
                        Field f = Unsafe.class.getDeclaredField("theUnsafe");

                        f.setAccessible(true);

                        return (Unsafe)f.get(null);
                    });
            }
            catch (PrivilegedActionException e) {
                throw new RuntimeException("Could not initialize intrinsics.", e.getCause());
            }
        }

        PAGE_SIZE = UNSAFE.pageSize();
    }

    private static final byte[] FILL_BUF = new byte[4 * 1024];

    private final ByteBuffer buf;

    private final FileChannel ch;


    public JavaFileHandler(String path, long size) {
        try {
            ch = FileChannel.open(Paths.get(path), CREATE, READ, WRITE);

            if (ch.size() < size) {
                long remained = size;

                while (remained > 0) {
                    remained -= ch.write(ByteBuffer.wrap(FILL_BUF, 0, Math.min((int)remained, FILL_BUF.length)));
                }
            }

            buf = ch.map(FileChannel.MapMode.READ_WRITE, 0, size);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to mmap file " + path, e);
        }
    }

    public ByteBuffer getBuffer() {
        return buf;
    }

    @Override public void close() throws Exception {
        try {
            if (cleanerMtd != null) {
                cleanerMtd.invoke(buf);
            }
        }
        finally {
            ch.close();
        }
    }

    public void fsync() throws Exception {
        fsync(0, buf.capacity());
    }

    public void fsync(long off, long len) throws Exception {
        long mappedOff = (Long)mappingOffset.invoke(buf);

        assert mappedOff == 0 : mappedOff;

        long addr = (Long)mappingAddress.invoke(buf, mappedOff);

        long delta = (addr + off) % PAGE_SIZE;

        long alignedAddr = (addr + off) - delta;

        force0.invoke(buf, fd.get(buf), alignedAddr, len + delta);
    }


    public static Method findNonPublicMethod(Class<?> cls, String name, Class<?>... paramTypes) {
        while (cls != null) {
            Method mtd = getNonPublicMethod(cls, name, paramTypes);

            if (mtd != null)
                return mtd;

            cls = cls.getSuperclass();
        }

        return null;
    }

    public static Method getNonPublicMethod(Class<?> cls, String name, Class<?>... paramTypes) {
        try {
            Method mtd = cls.getDeclaredMethod(name, paramTypes);

            mtd.setAccessible(true);

            return mtd;
        }
        catch (NoSuchMethodException ignored) {
            // No-op.
        }

        return null;
    }

    public static Field findField(String clsName, String name) {
        try {
            return findField(Class.forName(clsName), name);
        }
        catch (ClassNotFoundException e) {
            // No-op.
        }
        return null;
    }

    public static Field findField(Class<?> cls, String name) {
        while (cls != null) {
            try {
                Field fld = cls.getDeclaredField(name);

                if (!fld.isAccessible())
                    fld.setAccessible(true);

                return fld;
            }
            catch (NoSuchFieldException ignored) {
                // No-op.
            }

            cls = cls.getSuperclass();
        }

        return null;
    }
}
