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
package unearth.storage;

import java.time.Instant;

import unearth.hashable.Hashable;
import unearth.hashable.Hashed;
import unearth.jdbc.D;
import unearth.jdbc.S;
import unearth.jdbc.Stmt;
import unearth.jdbc.TypeSafeJdbc;
import unearth.munch.id.FaultId;
import unearth.munch.id.FaultStrandId;
import unearth.munch.model.Cause;
import unearth.munch.model.CauseStrand;
import unearth.munch.model.Fault;
import unearth.munch.model.FaultStrand;
import unearth.munch.model.FeedEntry;
import unearth.munch.print.CauseFrame;

final class Setter {

    static <T extends Hashed> Stmt byId(Stmt stmt, T hashable) {

        return new IdSetter<>(stmt, hashable).noop();
    }

    static <T extends Hashable> S<Integer, D<Hashable>> list(Stmt stmt, T t) {

        return new ListSetter<>(stmt, t);
    }

    static S<CauseStrand, D<String>> cause(Stmt stmt) {

        return cause(stmt, null);
    }

    static S<CauseStrand, D<String>> cause(Stmt stmt, Cause cause) {

        return new CauseSetter(stmt, cause);
    }

    static D<FaultStrand> fault(Stmt stmt) {

        return fault(stmt, null);
    }

    static D<FaultStrand> fault(Stmt stmt, Fault fault) {

        return new FaultSetter(stmt, fault);
    }

    static Stmt faultStrand(Stmt stmt, FaultStrand faultStrand) {

        return new FaultStrandSetter(stmt, faultStrand).noop();
    }

    static S<CauseFrame.ClassLoader,
        S<CauseFrame.Module,
            S<CauseFrame.ModuleVer,
                S<CauseFrame.ClassName,
                    S<CauseFrame.Method,
                        S<CauseFrame.File,
                            S<Integer, D<Boolean>>>>>>>> causeFrame(
        Stmt stmt, CauseFrame causeFrame
    ) {

        return new CauseFrameSetter(stmt, causeFrame);
    }

    static D<String> causeStrand(Stmt stmt) {

        return causeStrand(stmt, null);
    }

    static D<String> causeStrand(Stmt stmt, CauseStrand causeStrand) {

        return new CauseStrandSetter(stmt, causeStrand);
    }

    static S<FaultId, S<FaultStrandId, S<Instant, S<Long, S<Long, D<Long>>>>>> feedEntry(
        Stmt stmt, FeedEntry feedEntry
    ) {

        return new FeedEntrySetter(stmt, feedEntry);
    }

    private Setter() {
    }

    private static class CauseStrandSetter extends TypeSafeJdbc<CauseStrand>
        implements D<String> {

        CauseStrandSetter(Stmt stmt, CauseStrand causeStrand) {

            super(stmt, causeStrand);
        }

        @Override
        public Stmt set(String className) {

            return s(className);
        }
    }

    private static class CauseFrameSetter extends TypeSafeJdbc<CauseFrame> implements
        S<CauseFrame.ClassLoader,
            S<CauseFrame.Module,
                S<CauseFrame.ModuleVer,
                    S<CauseFrame.ClassName,
                        S<CauseFrame.Method,
                            S<CauseFrame.File,
                                S<Integer, D<Boolean>>>>>>>> {

        CauseFrameSetter(Stmt stmt, CauseFrame causeFrame) {

            super(stmt, causeFrame);
        }

        @Override
        public S<CauseFrame.Module,
            S<CauseFrame.ModuleVer,
                S<CauseFrame.ClassName,
                    S<CauseFrame.Method,
                        S<CauseFrame.File,
                            S<Integer, D<Boolean>>>>>>> set(
            CauseFrame.ClassLoader classLoader
        ) {

            return set(
                () -> s(classLoader),
                () -> modjul -> set(
                    () -> s(modjul),
                    () -> modjulVer -> set(
                        () -> s(modjulVer),
                        () -> className -> set(
                            () -> s(className),
                            () -> method -> set(
                                () -> s(method),
                                () -> file -> set(
                                    () -> s(file),
                                    () -> line -> set(
                                        () -> s(line),
                                        () -> this::s)))))));
        }
    }

    private static class CauseSetter extends TypeSafeJdbc<Cause>
        implements S<CauseStrand, D<String>> {

        CauseSetter(Stmt stmt, Cause cause) {

            super(stmt, cause);
        }

        @Override
        public D<String> set(CauseStrand causeStrand) {

            s(causeStrand);
            return this::s;
        }
    }

    private static class FaultSetter extends TypeSafeJdbc<Fault>
        implements D<FaultStrand> {

        FaultSetter(Stmt stmt, Fault fault) {

            super(stmt, fault);
        }

        @Override
        public Stmt set(FaultStrand faultStrand) {

            return s(faultStrand);
        }
    }

    private static class FaultStrandSetter extends TypeSafeJdbc<FaultStrand> {

        FaultStrandSetter(Stmt stmt, FaultStrand fault) {

            super(stmt, fault);
        }
    }

    private static class ListSetter<T extends Hashable> extends TypeSafeJdbc<T> implements S<Integer, D<Hashable>> {

        ListSetter(Stmt stmt, T hashable) {

            super(stmt, hashable);
        }

        @Override
        public D<Hashable> set(Integer from) {

            s(from);
            return this::s;
        }
    }

    private static final class FeedEntrySetter extends TypeSafeJdbc<FeedEntry>
        implements S<FaultId, S<FaultStrandId, S<Instant, S<Long, S<Long, D<Long>>>>>> {

        private FeedEntrySetter(Stmt stmt, FeedEntry feedEntry) {

            super(stmt, feedEntry);
        }

        @Override
        public S<FaultStrandId, S<Instant, S<Long, S<Long, D<Long>>>>> set(FaultId faultId) {

            s(faultId.getHash());
            return faultStrandId -> {
                s(faultStrandId.getHash());
                return time -> {
                    s(time);
                    return globalSequenceNo -> {
                        s(globalSequenceNo);
                        return faultSequenceNo -> {
                            s(faultSequenceNo);
                            return this::s;
                        };
                    };
                };
            };
        }
    }

    private static class IdSetter<T extends Hashed> extends TypeSafeJdbc<T> {

        IdSetter(Stmt stmt, T hashed) {

            super(stmt, hashed);
        }
    }
}
