package link.stuf.exceptions.hashing;

import java.security.MessageDigest;
import java.util.UUID;
import java.util.function.Supplier;

public final class Hasher {

    public static Supplier<UUID> uuid(Hashed hashed) {
        return Memoizer.get(() -> {
            MessageDigest md5 = md5();
            hashed.hashTo(md5::update);
            return UUID.nameUUIDFromBytes(md5.digest());
        });
    }

    private static MessageDigest md5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new IllegalStateException("Expected MD5 implementation", e);
        }
    }

    private Hasher() {
    }
}
