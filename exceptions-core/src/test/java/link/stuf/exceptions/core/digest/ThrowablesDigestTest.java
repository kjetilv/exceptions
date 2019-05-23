package link.stuf.exceptions.core.digest;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ThrowablesDigestTest {

    @Test
    public void hash() {
        Digest digest1 = Digest.create(new Throwable(new Throwable(new Throwable())));
        Digest digest2 = Digest.create(new Throwable(new Throwable(new Throwable())));
        assertEquals(digest1.getId(), digest2.getId());
    }

    @Test
    public void hash2() {
        Digest digest1 = Digest.create(new Throwable(new Throwable(new Throwable())));
        Digest digest2 = Digest.create(new Throwable(new Throwable(new Throwable())));
        assertNotEquals(digest1.getId(), digest2.getId());
    }

    @Test
    public void fail() {
        new IOException("Foo is bar", new IOException("Zot")).printStackTrace(System.out);
    }
}
