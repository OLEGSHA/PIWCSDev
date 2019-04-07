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

public class FractalNoise {
	
	private final NoiseGenerator[] sources;
	private final double[] frequencies;
	private final double[] amplitudes;
	
	private final double bias;
	private final double normalizingBias;
	private final double maxValue;
	
	public FractalNoise(NoiseGenerator[] sources, double[] frequencies, double[] amplitudes, double bias, double normalizingBias) {
		this.sources = sources;
		this.frequencies = frequencies;
		this.amplitudes = amplitudes;
		this.bias = bias;
		this.normalizingBias = bias;
		
		double maxValue = bias;
		for (double amplitude : amplitudes) {
			maxValue += amplitude;
		}
		
		this.maxValue = maxValue;
	}
	
	public FractalNoise(long seed, double bias, double normalizingBias, double... params) {
		this.sources = new NoiseGenerator[params.length / 2];
		this.frequencies = new double[params.length / 2];
		this.amplitudes = new double[params.length / 2];
		this.bias = bias;
		this.normalizingBias = normalizingBias;

		double maxValue = bias;
		for (int i = 0; i < params.length / 2; ++i) {
			sources[i] = new SimplexNoiseGenerator(seed + i);
			frequencies[i] = params[i * 2];
			maxValue += amplitudes[i] = params[i * 2 + 1];
		}
		
		this.maxValue = maxValue;
	}

	public double getValue(double x, double y) {
		double result = bias;
		
		for (int i = 0; i < sources.length; ++i) {
			result += sources[i].noise(x * frequencies[i], y * frequencies[i]) * amplitudes[i];
		}
		
		return result;
	}
	
	public double getValuePseudoNormalized(double x, double y) {
		return Math.min(1, getValue(x, y) / maxValue + normalizingBias);
	}

	public NoiseGenerator[] getSources() {
		return sources;
	}

	public double[] getFrequencies() {
		return frequencies;
	}

	public double[] getAmplitudes() {
		return amplitudes;
	}

	public double getBias() {
		return bias;
	}

	public double getNormalizingBias() {
		return normalizingBias;
	}

	public double getMaxValue() {
		return maxValue;
	}

}
