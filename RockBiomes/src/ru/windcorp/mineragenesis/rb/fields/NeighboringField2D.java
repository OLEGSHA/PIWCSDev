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

import java.util.Random;

import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;
import ru.windcorp.mineragenesis.rb.gen.Deposit;

/**
 * @author Javapony
 *
 */
public class NeighboringField2D implements Field2D {
	
	private final Field2D source;
	private final double targetValue;
	private final double targetTolerance;
	private final Field2D noise;

	public NeighboringField2D(Field2D source, double targetValue, double targetTolerance, Field2D noise) {
		this.source = source;
		this.targetValue = targetValue;
		this.targetTolerance = targetTolerance;
		this.noise = noise;
	}
	
	public static NeighboringField2D build(Arguments args) throws ConfigurationException {
		return new NeighboringField2D(
				args.get("src", Field2D.class),
				args.get("target", Double.class),
				args.get("tolerance", Double.class),
				args.get("noise", Field2D.class));
	}
	
	public static NeighboringField2D buildForDeposit(Arguments args) throws ConfigurationException {
		return new NeighboringField2D(
				args.get(null, Deposit.class).getColumnDensityField(),
				args.get("target", Double.class),
				args.get("tolerance", Double.class),
				args.get("noise", Field2D.class));
	}

	/**
	 * @see ru.windcorp.mineragenesis.rb.fields.Field2D#get(double, double)
	 */
	@Override
	public double get(double x, double z) {
		double base = transform(source.get(x, z));
		if (base < 0) return base;
		return base * noise.get(x, z);
	}
	
	protected double transform(double x) {
		return -1 * (x - targetValue - targetTolerance) * (x - targetValue + targetTolerance)
				/ (targetTolerance * targetTolerance);
	}

	/**
	 * @see ru.windcorp.mineragenesis.rb.fields.Field2D#getMin()
	 */
	@Override
	public double getMin() {
		double base = transform(source.getMin());
		if (base < 0) return base;
		return base * noise.getMin();
	}

	/**
	 * @see ru.windcorp.mineragenesis.rb.fields.Field2D#getMax()
	 */
	@Override
	public double getMax() {
		double base = transform(source.getMax());
		if (base < 0) return base;
		return base * noise.getMax();
	}

	/**
	 * @see ru.windcorp.mineragenesis.rb.fields.Field2D#clone(java.util.Random)
	 */
	@Override
	public Field2D clone(Random seedGenerator) {
		return new NeighboringField2D(
				source.clone(seedGenerator),
				targetValue, targetTolerance,
				noise.clone(seedGenerator));
	}

}
