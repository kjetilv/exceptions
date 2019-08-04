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

package no.scienta.unearth.client;

import no.scienta.unearth.dto.Submission;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("SameParameterValue")
public class SubmitMain {

    public static void main(String[] args) {
        URI uri = arg(args, 0, URI::create);
        UnearthlyClient client = new UnearthlyClient(uri);
        args(args, 1).forEach(arg -> {
            System.out.println("Uploading " + arg + " ...");
            Submission submit = client.submit(Paths.get(arg));
            System.out.println("Uploaded: " + client.print(submit));
            client.retrieve(submit.getFaultId()).printStackTrace(System.out);
        });
    }

    private static Stream<String> args(String[] args, int skip) {
        return Arrays.stream(args).skip(skip);
    }

    private static <T> T arg(String[] args, int i, Function<String, T> fun) {
        return Optional.of(args)
            .filter(a -> a.length > i)
            .map(a -> a[i])
            .map(fun)
            .orElseThrow(() ->
                new IllegalStateException("Expected " + (i + 1) + " args: [URI] [Files...]")
            );
    }
}
