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

import ru.windcorp.mineragenesis.piwcs.MGAddonPIWCS;
import ru.windcorp.mineragenesis.request.ChunkData;

public class RockBiome implements BlockPopulator {
	
	private final BlockList replaceables;
	private final short rockType;
	private final Deposit[] deposits;

	public RockBiome(BlockList replaceables, short rockType, Deposit[] deposits) {
		this.replaceables = replaceables;
		this.rockType = rockType;
		this.deposits = deposits;
	}

	@Override
	public void populate(
			int dimension, int chunkX, int chunkZ,
			int x, int z, int y,
			short originalMgId,
			Random random, BlockCollector[] output, int oindex) {
		
		if (MGAddonPIWCS.isDebugging()) {
			if (
					
					(y == 1)
					||
					(y == 20 && (x == 0 || z == 0))
					
					) {
				output[oindex].add(rockType, 1);
			} else {
				output[oindex].add(ChunkData.AIR_MGID, 1);
			}
		}

		if (!replaceables.contains(originalMgId)) {
			return;
		}
			
		if (!MGAddonPIWCS.isDebugging()) output[oindex].add(rockType, 1);
		
		for (Deposit d : deposits) {
			d.populate(dimension, chunkX, chunkZ, x, z, y, originalMgId, random, output, oindex);
		}
	}

	public BlockList getReplaceables() {
		return replaceables;
	}

	public short getRockType() {
		return rockType;
	}

	public Deposit[] getDeposits() {
		return deposits;
	}

}
