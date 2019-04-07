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

import ru.windcorp.mineragenesis.noise.FractalNoise;

public class SimpliestDeposit extends BaseDeposit {
	
	private final BlockWeightedList blocks;
	
	private final float unitWeight;

	public SimpliestDeposit(BlockWeightedList blocks, FractalNoise heightNoise, FractalNoise densityNoise, double unitThickness, float unitWeight) {
		super(heightNoise, densityNoise, unitThickness);
		this.blocks = blocks;
		this.unitWeight = unitWeight;
	}

	@Override
	protected void getBlocks(Random random, BlockCollector blockCollector, float density) {
		blockCollector.add(blocks.get(random), density * unitWeight);
	}

	public BlockWeightedList getBlocks() {
		return blocks;
	}

	public float getUnitWeight() {
		return unitWeight;
	}

}
