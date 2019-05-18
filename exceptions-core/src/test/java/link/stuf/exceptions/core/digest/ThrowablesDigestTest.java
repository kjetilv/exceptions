package link.stuf.exceptions.core.digest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ThrowablesDigestTest {

    @Test
    public void hash() {
        ThrowablesDigest digest1 = ThrowablesDigest.of(new Throwable(new Throwable(new Throwable()))); ThrowablesDigest digest2 = ThrowablesDigest.of(new Throwable(new Throwable(new Throwable())));
        assertEquals(digest1.getId(), digest2.getId());
    }
    @Test
    public void hash2() {
        ThrowablesDigest digest1 = ThrowablesDigest.of(new Throwable(new Throwable(new Throwable())));
        ThrowablesDigest digest2 = ThrowablesDigest.of(new Throwable(new Throwable(new Throwable())));
        assertNotEquals(digest1.getId(), digest2.getId());
    }

}
