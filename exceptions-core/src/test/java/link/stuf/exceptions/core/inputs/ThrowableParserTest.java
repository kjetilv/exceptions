package link.stuf.exceptions.core.inputs;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        return Arrays.stream(new String(out.toByteArray()).split("\n"))
            .filter(line -> StackTraceEntry.MORE.parts(line).length == 0)
            .collect(Collectors.joining("\n"));
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
