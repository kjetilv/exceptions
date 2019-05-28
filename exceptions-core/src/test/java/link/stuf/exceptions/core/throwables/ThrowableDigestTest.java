package link.stuf.exceptions.core.throwables;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ThrowableDigestTest {

    @Test
    public void can_hash() {
        ShadowThrowable digest = new ShadowThrowable(new Exception(), null);

        UUID hash = digest.getHash();

        assertEquals(hash, digest.getHash());
    }

}
