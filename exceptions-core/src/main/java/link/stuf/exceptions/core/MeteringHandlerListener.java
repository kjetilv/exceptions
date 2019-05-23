package link.stuf.exceptions.core;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import link.stuf.exceptions.core.clearing.HandlerListener;
import link.stuf.exceptions.core.digest.Digest;
import link.stuf.exceptions.core.digest.Occurrence;
import link.stuf.exceptions.core.hashing.Hashed;

import java.util.Arrays;
import java.util.Collections;

public class MeteringHandlerListener implements HandlerListener {

    private final MeterRegistry metrics;

    public MeteringHandlerListener(MeterRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public void handled(Digest digest, Occurrence occurrence, Throwable source) {
        count(digest);
        count(digest, occurrence);
    }

    private void count(Digest digest) {
        counter(digest).count();
    }

    private void count(Digest digest, Occurrence messages) {
        messageCounter(digest, messages).count();
    }

    private Counter counter(Hashed exc) {
        return metrics.counter("exceptions", Collections.singleton(hashTag(exc)));
    }

    private Counter messageCounter(Hashed exc, Hashed messages) {
        return metrics.counter("exceptions-" + exc.getId(), Arrays.asList(hashTag(exc), messageTag(messages)));
    }

    private Tag hashTag(Hashed exc) {
        return Tag.of(UUID, exc.getId().toString());
    }

    private Tag messageTag(Hashed messages) {
        return Tag.of(MESSAGES, messages.getId().toString());
    }

    private static final String UUID = "uuid";

    private static final String MESSAGES = "messages";
}
