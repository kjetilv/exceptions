package link.stuf.exceptions.server.statik;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Static {

    private final ClassLoader classLoader;

    private final String preamble;

    private final Map<String, String> cacheData = new ConcurrentHashMap<>();

    public Static(ClassLoader classLoader, String preamble) {
        this.classLoader = classLoader;
        this.preamble = preamble;
    }

    public String read(String path) {
        return cacheData.computeIfAbsent(path, __ -> readPath(path));
    }

    private String readPath(String path) {
        byte[] buffer = new byte[8192];
        try (InputStream in = classLoader.getResourceAsStream(preamble + path)) {
            if (in == null) {
                throw new IllegalArgumentException("No such path: " + path);
            }
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                while (true) {
                    int read = in.read(buffer);
                    if (read > 0) {
                        out.write(buffer, 0, read);
                    } else if (read < 0) {
                        return new String(out.toByteArray(), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}
