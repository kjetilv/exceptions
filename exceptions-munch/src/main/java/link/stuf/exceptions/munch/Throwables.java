package link.stuf.exceptions.munch;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Throwables {

    public static ByteBuffer byteBuffer(Throwable throwable) {
        return ByteBuffer.wrap(bytes(throwable));
    }

    public static String string(Throwable throwable) {
        return new String(bytes(throwable), StandardCharsets.UTF_8);
    }

    public static ThrowableSpecies species(Throwable throwable) {
        Stream<ThrowableStack> shadows =
            Streams.causes(throwable).map(ThrowableStack::create);
        List<ThrowableStack> causes =
            Streams.reverse(shadows).collect(Collectors.toList());
        return new ThrowableSpecies(causes);
    }

    public static ThrowableSpecimen create(Throwable throwable) {
        return new ThrowableSpecimen(messages(throwable), species(throwable));
    }

    private static byte[] bytes(Throwable throwable) {
        Objects.requireNonNull(throwable);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (PrintWriter pw = new PrintWriter(baos)) {
                throwable.printStackTrace(pw);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to print: " + throwable, e);
        }
    }

    private static List<String> messages(Throwable throwable) {
        return Streams.causes(throwable).map(Throwable::getMessage).collect(Collectors.toList());
    }
}
