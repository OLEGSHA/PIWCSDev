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

public abstract class BaseDeposit extends Deposit {
	
	private final FractalNoise heightNoise;
	private final FractalNoise densityNoise;
	
	private final double unitThickness;

	public BaseDeposit(FractalNoise heightNoise, FractalNoise densityNoise, double unitThickness) {
		this.heightNoise = heightNoise;
		this.densityNoise = densityNoise;
		this.unitThickness = unitThickness;
	}

	@Override
	public void populate(
			int dimension, int chunkX, int chunkZ,
			int x, int z, int y,
			short orignalMgId,
			Random random,
			BlockCollector[] output, int oindex) {
		
		double columnDensity = getColumnDensity(x + chunkX * 16, z + chunkZ * 16);
		
		if (columnDensity <= 0) {
			return;
		}
		
		double height = heightNoise.getValue(x + chunkX * 16, z + chunkZ * 16);
		float density = (float) (columnDensity - (1/unitThickness) * Math.abs(y - height));
		
		if (density <= 0) {
			return;
		}
		
		getBlocks(random, output[oindex], density);
	}
	
	public double getColumnDensity(int x, int z) {
		return densityNoise.getValue(x, z);
	}

	protected abstract void getBlocks(Random random, BlockCollector blockCollector, float density);

	public FractalNoise getHeightNoise() {
		return heightNoise;
	}

	public FractalNoise getDensityNoise() {
		return densityNoise;
	}

	public double getUnitThickness() {
		return unitThickness;
	}

}
