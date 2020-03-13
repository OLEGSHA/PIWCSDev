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

import java.util.Arrays;
import java.util.Collection;

import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;

/**
 * @author Javapony
 *
 */
public class DiscreteFieldConstant<T> implements DiscreteField2D<T> {
	
	private final T content;

	public DiscreteFieldConstant(T content) {
		this.content = content;
	}

	/**
	 * @see ru.windcorp.mineragenesis.rb.fields.DiscreteField2D#get(double, double)
	 */
	@Override
	public T get(double x, double z) {
		return content;
	}
	
	/**
	 * @see ru.windcorp.mineragenesis.rb.fields.DiscreteField2D#getAll()
	 */
	@Override
	public Collection<T> getAll() {
		return Arrays.asList(content);
	}
	
	public static DiscreteFieldConstant<?> build(Arguments args) throws ConfigurationException {
		return new DiscreteFieldConstant<Object>(args.get(null, Object.class));
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DFOnly[" + content + "]";
	}

}
