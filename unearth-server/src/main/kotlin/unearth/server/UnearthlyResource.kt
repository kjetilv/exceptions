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

package unearth.server

data class UnearthlyResource<I, R>(
    val path: String,
    val methods: List<Methed> = listOf(Methed.GET),
    val getter: (I) -> R? = { id -> null },
    val poster: (I, R) -> Unit = { id, resource -> },
    val putter: (I, R) -> Unit = { id, resource -> }
) {

    fun get(id: I): R? = getter(id)

    fun post(id: I, resource: R) = poster(id, resource)

    fun put(id: I, resource: R) = putter(id, resource)
}

enum class Methed {

    GET,

    POST,

    PUT,

    HEAD
}
