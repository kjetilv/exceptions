package link.stuf.exceptions.digest;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ThrowableDigestTest {

    @Test
    public void can_hash() {
        ThrowableDigest digest = new ThrowableDigest(new Exception(), null);

        UUID hash = digest.getId();

        assertEquals(hash, digest.getId());
    }

}
