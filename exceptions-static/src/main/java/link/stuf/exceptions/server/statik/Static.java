package link.stuf.exceptions.server.statik;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Static {

    private final ClassLoader classLoader;

    private final String preamble;

    private final Map<String, String> cache = new HashMap<>();

    public Static(ClassLoader classLoader, String preamble) {
        this.classLoader = classLoader;
        this.preamble = preamble;
    }

    public String read(String path) {
        return cache.computeIfAbsent(path, p -> readPath(preamble + path));
    }

    private String readPath(String path) {
        byte[] buffer = new byte[8192];
        try (
            InputStream resourceAsStream = classLoader.getResourceAsStream(path);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            if (resourceAsStream == null) {
                throw new IllegalArgumentException("No such path: " + path);
            }
            while (true) {
                int read = resourceAsStream.read(buffer);
                if (read > 0) {
                    outputStream.write(buffer, 0, read);
                }
                if (read < 0) {
                    return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
