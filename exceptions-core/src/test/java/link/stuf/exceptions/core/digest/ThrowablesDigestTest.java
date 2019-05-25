package link.stuf.exceptions.core.digest;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ThrowablesDigestTest {

    @Test
    public void hash() {
        ThrowableSpecies digest1 = ThrowableSpecies.create(new Throwable(new Throwable(new Throwable())));
        ThrowableSpecies digest2 = ThrowableSpecies.create(new Throwable(new Throwable(new Throwable())));
        assertEquals(digest1.getHash(), digest2.getHash());
    }

    @Test
    public void hash2() {
        ThrowableSpecies digest1 = ThrowableSpecies.create(new Throwable(new Throwable(new Throwable())));
        ThrowableSpecies digest2 = ThrowableSpecies.create(new Throwable(new Throwable(new Throwable())));
        assertNotEquals(digest1.getHash(), digest2.getHash());
    }

    @Test
    public void fail() {
        new IOException("Foo is bar", new IOException("Zot")).printStackTrace(System.out);
    }
}
