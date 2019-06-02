package link.stuf.exceptions.munch;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class ThrowableStackTest {

    @Test
    public void can_hash() {
        ThrowableStack digest = ThrowableStack.create(new Exception());

        UUID hash = digest.getHash();

        Assert.assertEquals(hash, digest.getHash());
    }
}
