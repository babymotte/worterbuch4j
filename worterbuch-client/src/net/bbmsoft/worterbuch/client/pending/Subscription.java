/*
 *  Worterbuch Java client library
 *
 *  Copyright (C) 2024 Michael Bachmann
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.bbmsoft.worterbuch.client.pending;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bbmsoft.worterbuch.client.model.ClientMessage;

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public record Subscription<T>(ClientMessage request, Consumer<Optional<T>> callback, Type type) {

}
