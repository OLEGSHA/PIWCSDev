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
package ru.windcorp.mineragenesis.rb.fields;

import java.util.ArrayList;
import java.util.Collection;

import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;

public class MaxNoiseDiscreteField2D<T> implements DiscreteField2D<T> {
	
	public static class Value<T> {
		final T value;
		final Field2D noise;
		
		public Value(T value, Field2D noise) {
			this.value = value;
			this.noise = noise;
		}
		
		public static Value<Object> build(Arguments args) throws ConfigurationException {
			return new Value<Object>(
					args.get(null, Object.class),
					args.get(null, Field2D.class)
			);
		}
	}
	
	private final Value<T>[] values;
	
	public MaxNoiseDiscreteField2D(Value<T>[] values) {
		this.values = values;
	}
	
	@SuppressWarnings("unchecked")
	public static MaxNoiseDiscreteField2D<?> build(Arguments args) throws ConfigurationException {
		return new MaxNoiseDiscreteField2D<Object>(args.get().toArray(new Value[0]));
	}

	@Override
	public T get(double x, double z) {
		double max = Double.NEGATIVE_INFINITY;
		T result = null;
		
		for (int i = 0; i < values.length; ++i) {
			double n = values[i].noise.get(x, z);
			if (n > max) {
				max = n;
				result = values[i].value;
			}
		}
		
		return result;
	}
	
	/**
	 * @see ru.windcorp.mineragenesis.rb.fields.DiscreteField2D#getAll()
	 */
	@Override
	public Collection<T> getAll() {
		Collection<T> result = new ArrayList<>();
		
		for (Value<T> value : values)
			result.add(value.value);
		
		return result;
	}

}
