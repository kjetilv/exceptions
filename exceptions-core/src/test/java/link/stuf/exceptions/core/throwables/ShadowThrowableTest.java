package link.stuf.exceptions.core.throwables;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ShadowThrowableTest {

    @Test
    public void can_hash() {
        ShadowThrowable digest = ShadowThrowable.create(new Exception());

        UUID hash = digest.getHash();

        assertEquals(hash, digest.getHash());
    }
}
