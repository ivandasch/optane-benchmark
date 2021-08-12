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

public class MsyncBenchmark {
    private static final byte[] FILL_BUF = new byte[4 * 1024];

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        @Param({"OPTANE", "JAVA"})
        public FileHandler.TYPE type;

        @Param({"1073741824"})
        public long fileSz;

        @Param({"67108864"})
        public int bufSz;

        @Param({"BEGIN", "MIDDLE", "END"})
        public OffsetType offsetType;

        @Param({"/home/ivandasch/mem"})
        public String path;

        public FileHandler handler;

        public byte[] buf;

        public int offset;

        public Path filePath;

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

            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            rnd.nextBytes(buf);

            switch (offsetType) {
                case BEGIN:
                    offset = 0;
                case MIDDLE:
                    offset = (int)(fileSz >> 1);
                case END:
                    offset = (int)(fileSz - buf.length);
            }

        }

        @TearDown(Level.Invocation)
        public void tearDown() throws Exception {
            if (handler != null) {
                handler.close();
            }
        }
    }

    public enum OffsetType {
        BEGIN,
        MIDDLE,
        END
    }

    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 1)
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Measurement(iterations = 5)
    public void syncPartial(BenchmarkState state) throws Exception {
        FileHandler handler = state.handler;
        ByteBuffer buf = handler.getBuffer();
        handler.getBuffer().position(state.offset);
        buf.put(state.buf);
        handler.fsync(state.offset, state.buf.length);
    }
}
