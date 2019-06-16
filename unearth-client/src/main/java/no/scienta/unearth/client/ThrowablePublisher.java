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

package no.scienta.unearth.client;

import no.scienta.unearth.munch.util.Throwables;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

final class ThrowablePublisher implements HttpRequest.BodyPublisher {

    private final ByteBuffer bytes;

    ThrowablePublisher(Throwable throwable) {
        this(Throwables.byteBuffer(throwable));
    }

    ThrowablePublisher(InputStream stream) {
        this(read(stream));
    }

    private ThrowablePublisher(ByteBuffer bytes) {
        this.bytes = bytes;
    }

    @Override
    public long contentLength() {
        return bytes.limit();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        subscriber.onNext(bytes);
    }

    private static ByteBuffer read(InputStream stream) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            while (true) {
                int read;
                try {
                    read = stream.read(buffer);
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read", e);
                }
                if (read > 0) {
                    baos.write(buffer, 0, read);
                }
                if (read < 0) {
                    baos.flush();
                    return ByteBuffer.wrap(baos.toByteArray());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read/close", e);
        }
    }
}
