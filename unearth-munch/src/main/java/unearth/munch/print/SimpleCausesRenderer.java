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

package unearth.munch.print;

import java.util.Collection;

import unearth.munch.model.Cause;
import unearth.util.Streams;

public final class SimpleCausesRenderer implements CausesRenderer {

    private final StackRenderer renderer;

    public SimpleCausesRenderer(StackRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public CausesRendering render(Collection<Cause> causeCollection) {
        return Streams.quickReduce(
            Streams.reverse(causeCollection),
            null,
            (rendering, cause) ->
                new CausesRendering(
                    cause.getCauseStrand().getClassName(),
                    cause.getMessage(),
                    renderer.render(cause),
                    rendering));
    }
}
