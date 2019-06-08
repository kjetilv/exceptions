package link.stuf.exceptions.munch;

import java.security.MessageDigest;
import java.util.UUID;
import java.util.function.Supplier;

final class Hasher {

    private static final String HASH = "MD5";

    static Supplier<UUID> uuid(Hashed hashed) {
        return Memoizer.get(() -> {
            MessageDigest md5 = md5();
            hashed.hashTo(md5::update);
            return UUID.nameUUIDFromBytes(md5.digest());
        });
    }

    private static MessageDigest md5() {
        try {
            return MessageDigest.getInstance(HASH);
        } catch (Exception e) {
            throw new IllegalStateException("Expected " + HASH + " implementation", e);
        }
    }

    private Hasher() {
    }
}
