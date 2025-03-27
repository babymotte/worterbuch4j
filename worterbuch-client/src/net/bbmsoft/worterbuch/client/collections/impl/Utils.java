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

package net.bbmsoft.worterbuch.client.collections.impl;

public class Utils {

//	public static String escape(final String string) {
//		return string.replace("/", "%2F");
//	}
//
//	public static String unescape(final String string) {
//		return string.replace("%2F", "/");
//	}

	public static String fullKey(final Object key, final String rootKey) {
		return rootKey + "/" + key.toString();
	}

	public static String trimKey(final String fullKey, final String rootKey) {
		return fullKey.substring(rootKey.length() + 1);
	}
}
