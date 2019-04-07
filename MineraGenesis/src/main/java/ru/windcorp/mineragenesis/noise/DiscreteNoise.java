/* 
 * MineraGenesis Minecraft mod
 * Copyright (C) 2019  Javapony and contributors
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
 */
package ru.windcorp.mineragenesis.noise;

public class DiscreteNoise<T> {
	
	private final T[] values;
	private final FractalNoise[] noises;
	
	public DiscreteNoise(T[] values, FractalNoise[] noises) {
		this.values = values;
		this.noises = noises;
	}

	public T getValue(double x, double y) {
		double max = Double.NEGATIVE_INFINITY;
		T result = null;
		
		for (int i = 0; i < noises.length; ++i) {
			double n = noises[i].getValue(x, y);
			if (n > max) {
				max = n;
				result = values[i];
			}
		}
		
		return result;
	}

	public T[] getValues() {
		return values;
	}

	public FractalNoise[] getNoises() {
		return noises;
	}

}
