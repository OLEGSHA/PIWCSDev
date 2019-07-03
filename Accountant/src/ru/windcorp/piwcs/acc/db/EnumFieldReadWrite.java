/*
 * PIWCS Accountant Plugin
 * Copyright (C) 2019  Javapony/OLEGSHA
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ru.windcorp.piwcs.acc.db;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

import ru.windcorp.piwcs.acc.db.FieldManager.FieldLoader;
import ru.windcorp.piwcs.acc.db.FieldManager.FieldWriter;

/**
 * @author Javapony
 *
 */
public class EnumFieldReadWrite<T extends Enum<T>> implements FieldLoader<T>, FieldWriter<T> {
	
	private static final Map<Class<?>, EnumFieldReadWrite<?>> INSTANCES = new WeakHashMap<>();
	
	@SuppressWarnings("unchecked")
	public static synchronized <T extends Enum<T>> EnumFieldReadWrite<T> getForClass(Class<T> clazz) {
		EnumFieldReadWrite<T> result = (EnumFieldReadWrite<T>) INSTANCES.get(clazz);
		
		if (result == null) {
			result = new EnumFieldReadWrite<>(clazz);
			INSTANCES.put(clazz, result);
		}
		
		return result;
	}
	
	private final Class<T> clazz;

	public EnumFieldReadWrite(Class<T> clazz) {
		this.clazz = clazz;
	}

	/**
	 * @see ru.windcorp.piwcs.acc.db.FieldManager.FieldWriter#write(java.lang.Object)
	 */
	@Override
	public String write(T value) {
		return value.name();
	}

	/**
	 * @see ru.windcorp.piwcs.acc.db.FieldManager.FieldLoader#load(java.lang.String, java.lang.Object)
	 */
	@Override
	public T load(String str, T current) throws IOException {
		try {
			return Enum.valueOf(clazz, str);
		} catch (IllegalArgumentException e) {
			throw new IOException("Unknown value \"" + str + "\" for enum type " + clazz
					+ "; known values: " + EnumSet.allOf(clazz).toString(), e);
		}
	}

}
