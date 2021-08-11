package com.sbt.mmap.benchmark;

import com.sbt.mmap.FileHandler;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class MmapBenchmark {
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        @Param({"OPTANE", "JAVA"})
        public FileHandler.TYPE type;

        @Param({"134217728", "536870912", "1073741824"})
        public long fileSz;

        @Param({"BEGIN", "MIDDLE", "END"})
        public OffsetType offsetType;

        @Param({"/home/ivandasch/mem"})
        public String path;

        public FileHandler handler;

        public byte[] buf;

        public int offset;

        @Setup(Level.Invocation)
        public void setUp() throws Exception {
            Files.deleteIfExists(Paths.get(path, "file.dat"));
            handler = FileHandler.getHandler(Paths.get(path, "file.dat").toString(), fileSz, type);
            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            buf = new byte[(int) (fileSz >> 4)];
            rnd.nextBytes(buf);

            switch (offsetType) {
                case BEGIN:
                    offset = 0;
                case MIDDLE:
                    offset = (int)(fileSz >> 1);
                case END:
                    offset = (int)(fileSz - buf.length);
            }

            handler.getBuffer().position(offset);
        }

        @TearDown(Level.Invocation)
        public void tearDown() throws Exception {
            if (handler != null) {
                handler.close();
            }
            Files.deleteIfExists(Paths.get(path, "file.dat"));
        }
    }

    public enum OffsetType {
        BEGIN,
        MIDDLE,
        END
    }

    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 1, time = 5)
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Measurement(iterations = 5, time = 5, timeUnit = MILLISECONDS)
    public void syncPartial(BenchmarkState state) throws Exception {
        FileHandler handler = state.handler;
        ByteBuffer buf = handler.getBuffer();
        buf.put(state.buf);
        handler.fsync(state.offset, state.buf.length);
    }

    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 1, time = 10)
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Measurement(iterations = 5, time = 5, timeUnit = MILLISECONDS)
    public void syncFull(BenchmarkState state) throws Exception {
        FileHandler handler = state.handler;
        ByteBuffer buf = handler.getBuffer();
        buf.put(state.buf);
        handler.fsync();
    }
}
