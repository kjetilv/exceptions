package link.stuf.exceptions.core.inputs;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

import static org.junit.Assert.*;

@SuppressWarnings("ThrowableNotThrown")
public class ThrowableParserTest {

    @Test
    public void parseSimple() {
        try {
            andFail();
        } catch (Exception e) {
            String output = print(e);
            System.out.println(output);
            ChameleonException chameleon = new ThrowableParser().parse(output);
            assertNotNull(chameleon);
            chameleon.printStackTrace(System.out);
            assertEquals(output, print(chameleon));
        }
    }

    private String print(Throwable e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(out)) {
            e.printStackTrace(pw);
        }
        return new String(out.toByteArray());
    }

    private void andFail() {
        try {
            andFailAgain();
        } catch (Exception e) {
            throw new IllegalStateException("Errr", e);
        }
    }

    private void andFailAgain() {
        throw new IllegalStateException("Argh!");
    }

}
