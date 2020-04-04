/*
 * MineraGenesis Rock Biomes Addon
 * Copyright (C) 2020  Javapony/OLEGSHA
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

import java.util.Random;

import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;

/**
 * @author Javapony
 *
 */
public class Field2DConstant implements Field2D {
	
	private final double value;
	
	public Field2DConstant(double value) {
		this.value = value;
	}

	public static Field2DConstant build(Arguments args) throws ConfigurationException {
		return new Field2DConstant(args.get(null, Double.class));
	}

	@Override
	public double get(double x, double z) {
		return value;
	}

	@Override
	public double getMin() {
		return value;
	}

	@Override
	public double getMax() {
		return value;
	}

	@Override
	public Field2D clone(Random seedGenerator) {
		return this;
	}

}
