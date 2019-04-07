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
package ru.windcorp.mineragenesis.piwcs;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import ru.windcorp.mineragenesis.interfaces.MGChunkProcessor;
import ru.windcorp.mineragenesis.noise.DiscreteNoise;
import ru.windcorp.mineragenesis.noise.FractalNoise;
import ru.windcorp.mineragenesis.piwcs.gen.BlockCollector;
import ru.windcorp.mineragenesis.piwcs.gen.RockBiome;
import ru.windcorp.mineragenesis.piwcs.gen.RockBiomeType;
import ru.windcorp.mineragenesis.request.ChunkData;
import ru.windcorp.mineragenesis.request.ChunkLocator;
import ru.windcorp.mineragenesis.request.GenerationRequest;

public class MGAPChunkProcessor implements MGChunkProcessor {
	
	private final RockBiomeType bedrock;
	private final DiscreteNoise<RockBiomeType> regoliths;
	private final FractalNoise bedrockHeight;
	
	private final ThreadLocal<BlockCollector[]> blockCollectors = ThreadLocal.withInitial(() -> {
		BlockCollector[] result = new BlockCollector[16];
		for (int i = 0; i < result.length; ++i) {
			result[i] = new BlockCollector(64, 4);
		}
		return result;
	});
	
	private final ThreadLocal<RockBiome[][]> regolithBiomes = ThreadLocal.withInitial(
			() -> new RockBiome[3][3]);
	
	private final ThreadLocal<RockBiome[][]> bedrockBiomes = ThreadLocal.withInitial(
			() -> new RockBiome[3][3]);

	public MGAPChunkProcessor(
			RockBiomeType bedrock,
			DiscreteNoise<RockBiomeType> regoliths,
			FractalNoise bedrockHeight) {
		
		this.bedrock = bedrock;
		this.regoliths = regoliths;
		this.bedrockHeight = bedrockHeight;
	}

	@Override
	public void processChunk(GenerationRequest request) {
		ChunkLocator chunk = request.getChunk();
		if (chunk.dimension != 0) {
			return;
		}
		
		// Gather data
		BlockCollector[] collectors = blockCollectors.get();
		Random random = ThreadLocalRandom.current();

		boolean areRegolithBiomesDifferent = false;
		RockBiome[][] regolithBiomes = this.regolithBiomes.get();
		for (int x = -1; x <= 1; ++x) {
			for (int z = -1; z <= 1; ++z) {
				regolithBiomes[x + 1][z + 1] =
						regoliths.getValue(chunk.chunkX + x, chunk.chunkZ + z)
						.getBiomes().getValue(chunk.chunkX + x, chunk.chunkZ + z);
				
				if (regolithBiomes[0][0] != regolithBiomes[x + 1][z + 1]) {
					areRegolithBiomesDifferent = true;
				}
			}
		}
		RockBiome localRegolithBiome = regolithBiomes[1][1];

		boolean areBedrockBiomesDifferent = false;
		RockBiome[][] bedrockBiomes = this.bedrockBiomes.get();
		for (int x = -1; x <= 1; ++x) {
			for (int z = -1; z <= 1; ++z) {
				bedrockBiomes[x + 1][z + 1] =
						bedrock.getBiomes().getValue(chunk.chunkX + x, chunk.chunkZ + z);
				
				if (bedrockBiomes[0][0] != bedrockBiomes[x + 1][z + 1]) {
					areBedrockBiomesDifferent = true;
				}
			}
		}
		RockBiome localBedrockBiome = bedrockBiomes[1][1];
		
		// Iterate over columns
		for (int x = 0; x < ChunkData.CHUNK_SIZE; ++x) {
			for (int z = 0; z < ChunkData.CHUNK_SIZE; ++z) {
				
				int height = request.getOriginal().getHeight(x, z);
				int bedrockHeight = (int) this.bedrockHeight.getValue(x + chunk.chunkX * 16, z + chunk.chunkZ + 16);

				// These vars are "biome context"
				// Biome context is set to bedrock at the beginning
				boolean areBiomesDifferent = areBedrockBiomesDifferent;
				RockBiome localBiome = localBedrockBiome;
				RockBiome[][] biomes = bedrockBiomes;
				
				// Iterate over blocks
				// Start at height 1 - we will never want to replace the bedrock, which will always be there
				for (int y = 1; y < height; ++y) {
					
					// Switch context to regolith
					if (y == bedrockHeight) {
						areBiomesDifferent = areRegolithBiomesDifferent;
						localBiome = localRegolithBiome;
						biomes = regolithBiomes;
					}
					
					// Handle border and corner biomes if necessary
					if (areBiomesDifferent) {
						for (int bx = 0; bx <= 2; ++bx) {
							for (int bz = 0; bz <= 2; ++bz) {
								
								// Don't handle local biome or its copies
								if (bx == 1 && bz == 1) continue;
								if (biomes[bx][bz] == localBiome) continue;
								
								collectors[0].addMultiplier(
										power4(Math.min(
												getWeight(bx, x / 16.0f),
												getWeight(bz, z / 16.0f)
										))
								);
								
								biomes[bx][bz].populate(
										chunk.dimension, chunk.chunkX, chunk.chunkZ,
										x, z, y,
										request.getOriginal().getBlockMGId(x, z, y),
										random,
										collectors,
										0);
								
								collectors[0].removeMultiplier();
							}
						}
					}
					
					// Handle local biome
					localBiome.populate(
							chunk.dimension, chunk.chunkX, chunk.chunkZ,
							x, z, y,
							request.getOriginal().getBlockMGId(x, z, y),
							random,
							collectors,
							0);
					
					if (!collectors[0].isEmpty()) {
						request.getPatch().setBlock(x, z, y, collectors[0].get(random));
						collectors[0].reset();
					} else if (MGAddonPIWCS.isDebugging()) {
						request.getPatch().setBlock(x, z, y, ChunkData.AIR_MGID);
					}
				}
				
			}
		}
		
	}
	
	private static float power4(float x) {
		x *= x;
		return x * x;
	}
	
	private static float getWeight(int position, float coordinate) {
		switch (position) {
		case 0:
			return 1 - coordinate;
		case 1:
			return 1;
		case 2:
			return coordinate;
		default:
			throw new IllegalArgumentException("position = " + position);
		}
	}

	public RockBiomeType getBedrock() {
		return bedrock;
	}

	public FractalNoise getBedrockHeight() {
		return bedrockHeight;
	}

	public DiscreteNoise<RockBiomeType> getRegoliths() {
		return regoliths;
	}

}
