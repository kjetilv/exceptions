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

package unearth.util.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import unearth.util.once.Once;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class OnceTest {
    
    @Test
    public void testSimpleGetOnce() {
        int c = next();
        Supplier<Data> supplier = Once.get(slowGet());
        Collection<Future<Data>> futures = futures(supplier, 20);
        Collection<Data> allData = futures.stream().map(OnceTest::await).collect(Collectors.toSet());
        assertEquals(1, allData.size());
        Data singleData = allData.iterator().next();
        assertEquals(c + 1, singleData.getC());
        assertEquals(c + 2, next());
        
        assertSame(singleData, supplier.get());
        assertSame(singleData, supplier.get());
    }
    
    @Test
    public void testSimpleGetOptional() {
        int c = next();
        Supplier<Data> supplier = Once.get(slowGet());
        assertTrue(Once.maybe(supplier).get().isEmpty());
        Data v = supplier.get();
        assertTrue(Once.maybe(supplier).get().isPresent());
        assertSame(v, Once.maybe(supplier).get().get());
        assertSame(v, supplier.get());
    }
    
    @Test
    public void testSimpleFailure() {
        int c = next();
        String msg = UUID.randomUUID().toString();
        AtomicInteger counter = new AtomicInteger();
        Supplier<Data> supplier = Once.get(failGet(msg, counter));
        Collection<Future<Data>> futures = futures(supplier, 20);
        Collection<Throwable> failures = futures.stream()
            .map(OnceTest::awaitFailure)
            .map(OnceTest::getRoot)
            .collect(Collectors.toList());
        assertEquals(1, counter.get());
        assertEquals(1, new HashSet<>(failures).size());
        assertEquals(msg, failures.iterator().next().getMessage());
        assertEquals(c + 1, next());
        
        try {
            supplier.get();
        } catch (Exception e) {
            assertEquals(msg, getRoot(e).getMessage());
        }
    }
    
    private static Throwable getRoot(Exception e) {
        Throwable walker = e;
        while (true) {
            if (walker.getCause() == null || walker.getCause() == e) {
                return walker;
            }
            walker = walker.getCause();
        }
    }
    
    private static Exception awaitFailure(Future<Data> future) {
        try {
            await(future);
        } catch (Exception e) {
            return e;
        }
        throw new IllegalStateException("Foo");
    }
    
    private static final AtomicInteger COUNTER = new AtomicInteger();
    
    private static Data await(Future<Data> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    private static int next() {
        return new Data().getC();
    }
    
    private static Collection<Future<Data>> futures(Supplier<Data> supplier, int size) {
        ExecutorService executor = pool(size);
        Collection<Future<Data>> futures = IntStream.range(0, size * 2)
            .mapToObj(i -> executor.submit(supplier::get))
            .collect(Collectors.toSet());
        executor.shutdown();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }));
        return futures;
    }
    
    private static Supplier<Data> slowGet() {
        return () -> {
            sleep100();
            return new Data();
        };
    }
    
    private static Supplier<Data> failGet(String msg, AtomicInteger counter) {
        return () -> {
            sleep100();
            counter.incrementAndGet();
            throw new IllegalStateException(msg);
        };
    }
    
    private static ExecutorService pool(int size) {
        return new ThreadPoolExecutor(
            size,
            size,
            1, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(size));
    }
    
    private static void sleep100() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
    
    public static final class Data {
        
        private final int c;
        
        Data() {
            c = COUNTER.getAndIncrement();
        }
        
        public int getC() {
            return c;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(c);
        }
        
        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Data && c == ((Data) o).c;
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + c + "]";
        }
    }
}
