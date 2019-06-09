package link.stuf.exceptions.munch;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Throwables {

    public static ByteBuffer byteBuffer(Throwable throwable) {
        return ByteBuffer.wrap(bytes(throwable));
    }

    public static String string(Throwable throwable) {
        return new String(bytes(throwable), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(Throwable throwable) {
        Objects.requireNonNull(throwable);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (PrintWriter pw = new PrintWriter(baos)) {
                throwable.printStackTrace(pw);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize: " + throwable, e);
        }
    }
}
