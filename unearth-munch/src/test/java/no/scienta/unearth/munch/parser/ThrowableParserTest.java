/*
 *     This file is part of Unearth.
 *
 *     Unearth is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Unearth is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 *     This file is part of Unearth.
 *
 *     Unearth is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Unearth is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.munch.parser;

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
            Throwable chameleon = ThrowableParser.parse(output);
            assertNotNull(chameleon);
            String print = print(chameleon);
            System.out.println(output);
            System.out.println(print);
//            assertEquals(output, print);
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
