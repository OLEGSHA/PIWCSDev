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

import java.util.Collection;

/**
 * @author Javapony
 *
 */
@FunctionalInterface
public interface DiscreteField2D<T> {

	T get(double x, double z);
	
	default Collection<T> getAll() {
		throw new UnsupportedOperationException();
	}
	
}
