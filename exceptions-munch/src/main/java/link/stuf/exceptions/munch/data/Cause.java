package link.stuf.exceptions.munch.data;

import link.stuf.exceptions.munch.AbstractHashedIdentified;
import link.stuf.exceptions.munch.ChameleonException;
import link.stuf.exceptions.munch.dto.ThrowableDto;
import link.stuf.exceptions.munch.ids.CauseId;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class Cause extends AbstractHashedIdentified<CauseId> {

    private final CauseType causeType;

    private final String message;

    static Cause create(Throwable cause) {
        return new Cause(CauseType.create(cause), cause.getMessage());
    }

    private Cause(CauseType causeType, String message) {
        this.causeType = Objects.requireNonNull(causeType);
        this.message = message;
    }

    CauseType getCauseType() {
        return causeType;
    }

    Throwable toThrowable(Throwable t) {
        Throwable exception = new ChameleonException(causeType.getClassName(), message, t);
        exception.setStackTrace(causeType.getStackTrace().toArray(StackTraceElement[]::new));
        return exception;
    }

    ThrowableDto toThrowableDto(ThrowableDto cause) {
        return new ThrowableDto(this.causeType.getClassName(), this.message, this.causeType, cause);
    }

    @Override
    protected CauseId id(UUID hash) {
        return new CauseId(hash);
    }

    @Override
    protected String toStringBody() {
        return "causeType:" + causeType + " message:" + message;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, causeType);
        hashString(h, message);
    }
}
