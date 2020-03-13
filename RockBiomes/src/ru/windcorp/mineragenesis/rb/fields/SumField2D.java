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

public class SumField2D implements Field2D {
	
	private final Field2D[] sources;
	
	private final double bias;
	private final boolean normalize;
	private final double normalizingDivisor;
	
	private final double max;
	private final double min;
	
	public SumField2D(Field2D[] sources, double bias, boolean normalize) {
		this.sources = sources;
		this.bias = bias;
		this.normalize = normalize;
		
		double max = bias;
		double min = bias;
		
		for (Field2D field : sources) {
			max += field.getMax();
			min += field.getMin();
		}
		
		if (normalize) {
			this.normalizingDivisor = max;
			this.max = bias + 1;
			this.min = bias - 1;
		} else {
			this.normalizingDivisor = Double.NaN;
			this.min = min;
			this.max = max;
		}
	}
	
	public static SumField2D build(Arguments args) throws ConfigurationException {
		double bias = args.get("bias", Double.class, 0.0);
		boolean normalized = args.get("norm", Double.class, 0.0) == 1;
		return new SumField2D(args.get().toArray(new Field2D[0]), bias, normalized);
	}

	@Override
	public double get(double x, double z) {
		double result = bias;
		
		for (int i = 0; i < sources.length; ++i) {
			result += sources[i].get(x, z);
		}
		
		if (normalize) {
			result /= normalizingDivisor;
		}
		
		return result;
	}

	public Field2D[] getSources() {
		return sources;
	}

	public double getBias() {
		return bias;
	}
	
	public boolean isNormalized() {
		return normalize;
	}
	
	/**
	 * @return the max
	 */
	@Override
	public double getMax() {
		return max;
	}
	
	/**
	 * @return the min
	 */
	@Override
	public double getMin() {
		return min;
	}
	
	/**
	 * @see ru.windcorp.mineragenesis.rb.fields.Field2D#clone(long)
	 */
	@Override
	public Field2D clone(Random seedGenerator) {
		Field2D[] sources = new Field2D[this.sources.length];
		
		for (int i = 0; i < sources.length; i++) {
			sources[i] = this.sources[i].clone(seedGenerator);
		}
		
		return new SumField2D(sources, this.bias, this.normalize);
	}

}
