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
 *     along with Unearth.  If not, see <https://www.gnu.org/licenses/>.
 */
package unearth.munch.parser;

import java.io.ByteArrayOutputStream;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ThrowableParserTest {
    
    @Test
    public void parseSuppressed() {
        try {
            andFailSuppressed();
        } catch (MyRuntimeException e) {
            String output = print(e);
            System.out.println(output);
            Throwable chameleon = ThrowableParser.parse(output);
            assertNotNull(chameleon);
            String print = print(chameleon);
            System.out.println("\n\n" + print);
        }
    }
    
    @Test
    public void parseConflated() {
        
        try {
            andFail();
        } catch (Exception e) {
            String output = conflate(print(e));
            System.out.println(output);
            Throwable chameleon = ThrowableParser.parse(output);
            assertNotNull(chameleon);
            String print = print(chameleon);
            System.out.println(output);
            System.out.println(print);
        }
    }
    
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
        }
    }
    
    private static final Pattern WS = Pattern.compile("\\s+");
    
    private static final String S = " ";
    
    private static String conflate(String ePrint) {
        
        return WS.matcher(String.join(S, ePrint.split("\n")))
            .replaceAll(S);
    }
    
    private static String print(Throwable e) {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(out)) {
            e.printStackTrace(pw);
        }
        return new String(out.toByteArray());
    }
    
    private static void andFail() {
        try {
            andFailAgain();
        } catch (Exception e) {
            throw new IllegalStateException("Errr", e);
        }
    }
    
    private static void andFailSuppressed() {
        try {
            andFailAgainSuppressed();
        } catch (MyRuntimeException e) {
            MyRuntimeException errr = new MyRuntimeException("Errr", e);
            errr.addSuppressed(new IllegalStateException("Aff", new InterruptedIOException("Int")));
            errr.addSuppressed(new IllegalArgumentException("Uff", new NumberFormatException("Ans42")));
            throw errr;
        }
    }
    
    private static void andFailAgain() {
        throw new IllegalStateException("Argh!");
    }
    
    private static void andFailAgainSuppressed() {
        MyRuntimeException exception = new MyRuntimeException();
        exception.addSuppressed(new ConnectException("1.0.0.1"));
        throw exception;
    }
    
    private static class MyRuntimeException extends RuntimeException {
        
        private MyRuntimeException() {
            super("Argh!", null, true, true);
        }
        
        private MyRuntimeException(String errr, MyRuntimeException e) {
            super(errr, e, true, true);
        }
    }
}
