package link.stuf.exceptions.core.throwables;

import link.stuf.exceptions.core.utils.Streams;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Throwables {

    public static ThrowableSpecies species(Throwable throwable) {
        Stream<ShadowThrowable> shadows =
            Streams.causes(throwable).map(ShadowThrowable::create);
        List<ShadowThrowable> causes =
            Streams.reverse(shadows).collect(Collectors.toList());
        return new ThrowableSpecies(causes);
    }

    public static ThrowableSpecimen create(Throwable throwable) {
        return new ThrowableSpecimen(messages(throwable), species(throwable));
    }

    private static List<String> messages(Throwable throwable) {
        return Streams.causes(throwable).map(Throwable::getMessage).collect(Collectors.toList());
    }
}
