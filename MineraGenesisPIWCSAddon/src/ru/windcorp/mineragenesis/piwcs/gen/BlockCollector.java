/* 
 * PIWCS addon for MineraGenesis Minecraft mod
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
package ru.windcorp.mineragenesis.piwcs.gen;

import java.util.Random;

public class BlockCollector {
	
	private final short[] blocks;
	private final float[] weights;
	private int entries = 0;
	
	private float sum = 0.0f;
	
	private final float[] multipliers;
	private int lastMultiplier = 0;
	
	public BlockCollector(int size, int multipliers) {
		this.blocks = new short[size];
		this.weights = new float[size];
		this.multipliers = new float[multipliers];
		
		this.multipliers[0] = 1.0f;
	}

	public void add(short block, float weight) {
		sum += weights[entries] = weight * multipliers[lastMultiplier];
		if (weights[entries] == 0.0f) return;
		blocks[entries] = block;
		++entries;
	}
	
	public boolean isEmpty() {
		return entries == 0;
	}
	
	public short get(Random random) {
		if (sum == 0) {
			throw new IllegalArgumentException("BlockCollector is empty");
		}
		
		float value = random.nextFloat() * sum;
		
		int i;
		for (i = 0; i < entries - 1; ++i) {
			if (value <= weights[i]) {
				return blocks[i];
			}
			value -= weights[i];
		}
		
		return blocks[i];
	}
	
	public void addMultiplier(float multiplier) {
		multipliers[lastMultiplier + 1] = multipliers[lastMultiplier] * multiplier;
		++lastMultiplier;
	}
	
	public void removeMultiplier() {
		--lastMultiplier;
	}
	
	public void reset() {
		lastMultiplier = 0;
		entries = 0;
		sum = 0.0f;
	}

}
