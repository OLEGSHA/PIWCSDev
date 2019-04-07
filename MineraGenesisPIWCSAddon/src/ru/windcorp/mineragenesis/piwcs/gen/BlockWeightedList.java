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

import ru.windcorp.mineragenesis.request.ChunkData;

public class BlockWeightedList {
	
	private final short[] blocks;
	private final float[] weights;
	private final float sum;
	
	public BlockWeightedList(short[] blocks, float[] weights) {
		this.blocks = blocks;
		this.weights = weights;
		
		float sum = 0;
		for (float weight : weights) {
			sum += weight;
		}
		this.sum = sum;
	}
	
	public BlockWeightedList(float... mess) {
		this.blocks = new short[mess.length / 3];
		this.weights = new float[mess.length / 3];
		
		float sum = 0;
		for (int i = 0; i < mess.length / 3; ++i) {
			blocks[i] = ChunkData.getMGID((int) mess[i * 3], (int) mess[i * 3 + 1]);
			sum += weights[i] = mess[i * 3 + 2];
		}
		this.sum = sum;
	}
	
	public short get(Random random) {
		float value = random.nextFloat() * sum;
		
		int i;
		for (i = 0; i < blocks.length - 1; ++i) {
			if (value <= weights[i]) {
				return blocks[i];
			}
			value -= weights[i];
		}
		
		return blocks[i];
	}

	public short[] getBlocks() {
		return blocks;
	}

	public float[] getWeights() {
		return weights;
	}

	public float getSum() {
		return sum;
	}

}
