package link.stuf.exceptions.core.clearing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import link.stuf.exceptions.api.ThrowablesHandler;
import link.stuf.exceptions.core.digest.Packages;
import link.stuf.exceptions.core.digest.ThrowableMessages;
import link.stuf.exceptions.core.digest.ThrowableReducer;
import link.stuf.exceptions.core.digest.ThrowablesDigest;
import link.stuf.exceptions.core.hashing.Hashed;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MeteringHandler implements ThrowablesHandler {

    private static final String UUID = "uuid";

    private static final String MESSAGES = "messages";

    private final Map<UUID, ThrowablesDigest> exceptions = new ConcurrentHashMap<>();

    private final MeterRegistry metrics;

    private final ThrowableReducer throwableReducer;

    public MeteringHandler(MeterRegistry metrics) {
        this.metrics = metrics;
        throwableReducer = new ThrowableReducer(
            Packages.all(),
            Packages.none(),
            Packages.none());
    }

    @Override
    public SimpleHandlingPolicy onException(Throwable throwable) {
        ThrowablesDigest digest = ThrowablesDigest.of(throwable);
        ThrowableMessages messages = ThrowableMessages.of(throwable);

        count(digest);
        count(digest, messages);
        boolean isNew = record(digest);

        return new SimpleHandlingPolicy(digest, digest.map(throwableReducer), throwable, isNew);
    }

    private boolean record(ThrowablesDigest digest) {
        ThrowablesDigest existing = exceptions.putIfAbsent(digest.getId(), digest);
        return existing == null;
    }

    private void count(ThrowablesDigest digest) {
        counter(digest).count();
    }

    private void count(ThrowablesDigest digest, ThrowableMessages messages) {
        messageCounter(digest, messages).count();
    }

    private Counter counter(Hashed exc) {
        return metrics.counter("exceptions", Collections.singleton(uuidTag(exc)));
    }

    private Counter messageCounter(Hashed exc, Hashed messages) {
        return metrics.counter("exceptions", Arrays.asList(uuidTag(exc), messageTag(messages)));
    }

    private Tag uuidTag(Hashed exc) {
        return Tag.of(UUID, exc.getId().toString());
    }

    private Tag messageTag(Hashed messages) {
        return Tag.of(MESSAGES, messages.getId().toString());
    }
}
