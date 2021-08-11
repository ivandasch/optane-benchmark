package com.sbt.mmap.optane;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Util {
    private static boolean loaded = false;
    private static final String extension = ".so";

    private Util() {
        // No-op.
    }

    static boolean isLoaded() {
        if (loaded)
            return true;

        try {
            System.loadLibrary("mmap");
            loaded = true;
        }
        catch (UnsatisfiedLinkError e) {
            loaded = false;
        }
        return loaded;
    }

    static String getLibName() {
        return "/com/sbt/mmap/optane/" + getOSName() + "/" + getOSArch() + "/" + "libmmap" + extension;
    }

    static String getOSArch() {
        return System.getProperty("os.arch", "");
    }

    static String getOSName() {
        if (System.getProperty("os.name", "").contains("Linux"))
            return "linux";
        else
            throw new UnsupportedOperationException("Operating System is not supported");
    }

    static void loadLibrary() {
        if (isLoaded())
            return;

        String libName = getLibName();
        File nativeLib = null;

        try (InputStream in = Util.class.getResourceAsStream(libName)) {
            if (in == null) {
                throw new ExceptionInInitializerError("Failed to load native mmap library, "
                    + libName + " not found");
            }

            nativeLib = File.createTempFile("libmmap", extension);

            try (FileOutputStream out = new FileOutputStream(nativeLib)) {
                byte[] buf = new byte[4096];
                int bytesRead;
                while(true) {
                    bytesRead = in.read(buf);
                    if (bytesRead == -1) break;
                    out.write(buf, 0, bytesRead);
                }
            }

            System.load(nativeLib.getAbsolutePath());
            loaded = true;
        }
        catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load native mmap library");
        }
        finally {
            if (nativeLib != null)
                nativeLib.deleteOnExit();
        }
    }
}
