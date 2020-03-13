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

import ru.windcorp.mineragenesis.rb.config.ConfigurationException;

import java.util.Random;

import ru.windcorp.mineragenesis.noise.NoiseGenerator;
import ru.windcorp.mineragenesis.noise.SimplexNoiseGenerator;
import ru.windcorp.mineragenesis.rb.RockBiomesAddon;
import ru.windcorp.mineragenesis.rb.config.Arguments;

/**
 * @author Javapony
 *
 */
public class Noise2D implements Field2D {

	private final NoiseGenerator source;
	
	private final double frequency;
	private final double amplitude;
	private final double bias;
	
	public Noise2D(NoiseGenerator source, double frequency, double amplitude, double bias) {
		this.source = source;
		this.frequency = frequency;
		this.amplitude = amplitude;
		this.bias = bias;
	}
	
	public static Noise2D build(Arguments args) throws ConfigurationException {
		return new Noise2D(
				new SimplexNoiseGenerator(RockBiomesAddon.computeSeed(args.get("seed", String.class, "0"))),
				args.get("freq", Double.class, 1.0),
				args.get("amp", Double.class, 1.0),
				args.get("bias", Double.class, 0.0)
		);
	}

	@Override
	public double get(double x, double z) {
		return source.noise(x / frequency, z / frequency) * amplitude + bias;
	}
	
	/**
	 * @return the frequency
	 */
	public double getFrequency() {
		return frequency;
	}
	
	/**
	 * @return the amplitude
	 */
	public double getAmplitude() {
		return amplitude;
	}
	
	/**
	 * @return the bias
	 */
	public double getBias() {
		return bias;
	}

	/**
	 * @see ru.windcorp.mineragenesis.rb.fields.Field2D#getMin()
	 */
	@Override
	public double getMin() {
		return bias - amplitude * 0.8;
	}

	/**
	 * @see ru.windcorp.mineragenesis.rb.fields.Field2D#getMax()
	 */
	@Override
	public double getMax() {
		return bias + amplitude * 0.8;
	}
	
	@Override
	public Field2D clone(Random seedGenerator) {
		return new Noise2D(new SimplexNoiseGenerator(seedGenerator.nextLong()), frequency, amplitude, bias);
	}

}
