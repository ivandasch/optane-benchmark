package com.sbt.mmap.benchmark;

import com.sbt.mmap.FileHandler;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class WalSegmentSwitchBenchmark {
    private static final byte[] FILL_BUF = new byte[4 * 1024];

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        @Param({"OPTANE", "JAVA"})
        public FileHandler.TYPE type;

        @Param({"1073741824"})
        public long fileSz;

        @Param({"16777216", "33554432"})
        public int bufSz;

        @Param({"/home/ivandasch/mem"})
        public String path;

        public Path filePath;

        public FileHandler handler;

        public byte[] buf;

        @Setup(Level.Trial)
        public void setUpTrial() throws Exception {
            filePath = Paths.get(path, "file.dat");
            Files.deleteIfExists(filePath);

            try(FileChannel ch = FileChannel.open(filePath, CREATE, READ, WRITE)) {
                if (ch.size() < fileSz) {
                    long remained = fileSz;

                    while (remained > 0) {
                        remained -= ch.write(ByteBuffer.wrap(FILL_BUF, 0, Math.min((int)remained, FILL_BUF.length)));
                    }
                }
            }

            buf = new byte[bufSz];
        }

        @TearDown(Level.Trial)
        public void tearDownTrial() throws Exception {
            Files.deleteIfExists(filePath);
        }


        @Setup(Level.Invocation)
        public void setUp() throws Exception {
            handler = FileHandler.getHandler(filePath.toString(), fileSz, type);
            ThreadLocalRandom.current().nextBytes(buf);
        }

        @TearDown(Level.Invocation)
        public void tearDown() throws Exception {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 1, time = 5)
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Measurement(iterations = 5, time = 5, timeUnit = MILLISECONDS)
    public void switchSegment(BenchmarkState state) throws Exception {
        FileHandler handler = state.handler;
        ByteBuffer buf = handler.getBuffer();
        long offset = buf.capacity() - state.buf.length;
        buf.position((int)offset);
        buf.put(state.buf);
        handler.fsync(offset, state.buf.length);
        handler.close();
        state.handler = FileHandler.getHandler(state.filePath.toString(), state.fileSz, state.type);
    }
}
