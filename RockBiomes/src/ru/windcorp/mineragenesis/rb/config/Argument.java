/*
 * MineraGenesis Rock Biomes Addon
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
package ru.windcorp.mineragenesis.rb.config;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author Javapony
 *
 */
class Argument {
	
	/**
	 * Cannot be <code>null</code> or an array other than Object[]
	 */
	public final Object object;
	public final String label;
	
	public Argument(Object object, String label) {
		this.object = Objects.requireNonNull(object, "object");
		this.label = label;
	}
	
	public boolean is(Class<?> type) {
		if (type == Object.class) return true;
		
		Class<?> myType = object.getClass();
		
		if (myType.isArray() != type.isArray()) return false;
		
		if (object.getClass().isArray()) {
			Class<?> requestedComponentType = type.getComponentType();
			for (Object element : (Object[]) object) {
				if (!requestedComponentType.isInstance(element)) return false;
			}
			return true;
		} else {
			return type.isInstance(object);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T as(Class<T> type) throws ConfigurationException {
		if (type == Object.class) return (T) object;
		
		if (object instanceof Object[]) {
			if (!type.isArray())
				throw new ConfigurationException(this + " cannot be cast to " + type.getSimpleName() + ": array cannot be cast to non-array");
			
			Class<?> requestedComponentType = type.getComponentType();
			int length = Array.getLength(object);
			
			if (length == 0)
				return (T) Array.newInstance(requestedComponentType, 0);
			
			if (requestedComponentType == Object.class) return type.cast(object);
			
			try {
				Object result = Array.newInstance(requestedComponentType, length);
				System.arraycopy(object, 0, result, 0, length);
				return (T) result;
			} catch (ArrayStoreException e) {
				throw new ConfigurationException(this + " cannot be cast to " + type.getSimpleName(), e);
			}
		} else {
			if (!type.isAssignableFrom(object.getClass()))
				throw new ConfigurationException(this + " cannot be cast to " + type.getSimpleName());
			return (T) object;
		}
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return object.getClass().getSimpleName() + " " + valueToString(object) + (label == null ? "" : " :" + label);
	}

	public static String valueToString(Object object) {
		if (object instanceof Object[]) {
			return Arrays.toString((Object[]) object);
		} else {
			return "\"" + object.toString() + "\"";
		}
	}

}
