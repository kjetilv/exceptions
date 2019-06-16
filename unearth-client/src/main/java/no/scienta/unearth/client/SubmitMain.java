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

import no.scienta.unearth.dto.Submission;

import java.io.FileInputStream;
import java.net.URI;
import java.util.Arrays;

public class SubmitMain {

    public static void main(String[] args) {
        UnearthClient unearthClient = new UnearthClient(URI.create(args[0]));
        Arrays.stream(args).skip(1).forEach(arg -> {
            System.out.println("Uploading " + arg + " ...");
            try (FileInputStream file = new FileInputStream(arg)) {
                Submission submit = unearthClient.submit(file);
                System.out.println("Uploaded: " + unearthClient.print(submit));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load " + arg, e);
            }
        });
    }
}
