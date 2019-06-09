package link.stuf.exceptions.munch;

import link.stuf.exceptions.munch.data.CauseType;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class ThrowableStackTest {

    @Test
    public void can_hash() {
        CauseType digest = CauseType.create(new Exception());

        UUID hash = digest.getHash();

        Assert.assertEquals(hash, digest.getHash());
    }
}
