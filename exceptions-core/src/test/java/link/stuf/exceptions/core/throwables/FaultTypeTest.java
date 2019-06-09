package link.stuf.exceptions.core.throwables;

import link.stuf.exceptions.munch.data.FaultType;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class FaultTypeTest {

    @Test
    public void hash() {
        FaultType d1 = newFaultType(); FaultType d2 = newFaultType();
        assertEquals(d1.getHash(), d2.getHash());
    }

    private FaultType newFaultType() {
        Throwable cause = new Throwable();
        Throwable cause1 = new Throwable(cause);
        return FaultType.create(new Throwable(cause1));
    }

    @Test
    public void hash2() {
        FaultType digest1 = newFaultType();
        FaultType digest2 = newFaultType();
        assertNotEquals(digest1.getHash(), digest2.getHash());
    }

    @Test
    public void fail() {
        new IOException("Foo is bar", new IOException("Zot")).printStackTrace(System.out);
    }
}
