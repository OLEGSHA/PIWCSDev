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
package ru.windcorp.mineragenesis.rb.gen;

import java.util.Arrays;

import ru.windcorp.mineragenesis.rb.RockBiomesCP.Workspace;
import ru.windcorp.mineragenesis.rb.fields.DiscreteField2D;
import ru.windcorp.mineragenesis.request.GenerationRequest;

import static ru.windcorp.mineragenesis.request.ChunkData.*;

public abstract class Dimension {
	
	public static class Cache {
		
		/**
		 * Biome of the center chunk
		 */
		public RockBiome centerBiome;
		
		/**
		 * <code>true</code> when any neighbor biomes are not equal to the center biome.
		 */
		public boolean areNeighborsDifferent;
		
		/**
		 * Contains each of neighbor {@link RockBiome} only once.
		 * Iterate over with:
		 * <pre>
		 * for (int i = 0; i &lt; cache.biomeSetSize; ++i) {
		 *     RockBiome biome = cache.biomeSet[i];
		 *     // ...
		 * }
		 * </pre>
		 */
		public final RockBiome[] biomeSet = new RockBiome[3 * 3 - 1];
		
		/**
		 * The actual size of {@link #biomeSet}. Entries with indices <tt>[biomeSetSize; biomeSet.length)</tt> are garbage.
		 */
		public int biomeSetSize;
		
		/**
		 * For each entry in {@link #biomeSet}, contains a <i>multiplier field</i> (MF).
		 * Each MF assigns a <i>multiplier value</i> to each column in the center chunk.
		 * <p>
		 * Weights of neighbor biome {@link BlockCollector} entries are multiplied by multiplier values.
		 * This is used to produce smooth biome transitions - the weight of a neighbor decreases
		 * further away from the border.
		 * @see DimensionComplex#getMultiplier(int, int)
		 */
		private final double[][] multiplierFields =
				new double[biomeSet.length][CHUNK_SIZE * CHUNK_SIZE];
		
		/**
		 * Clears this cache. Biome set is cleared and MF are filled with <code>0.0</code>.
		 */
		public void reset() {
			centerBiome = null;
			areNeighborsDifferent = false;
			biomeSetSize = 0;
			for (double[] field : multiplierFields) Arrays.fill(field, 0.0);
		}

		/**
		 * Registers the given biome in the biome set. Center biome is stored separately.
		 * @param biome a neighboring biome
		 * @return the index of this biome usable in {@link #addToMultiplier(int, int, int, float)}
		 * and {@link #getMultiplier(int, int, int)}, or <code>-1</code> if <code>biome</code> is the
		 * center biome.
		 */
		public int addBiome(RockBiome biome) {
//			if (biome == centerBiome) return -1;
			
			for (int i = 0; i < biomeSetSize; ++i) {
				if (biomeSet[i] == biome) {
					return i;
				}
			}
			
			biomeSet[biomeSetSize] = biome;
			areNeighborsDifferent |= biome == centerBiome;
			return biomeSetSize++;
		}
		
		/**
		 * Adds the given value to a single multiplier in the MF for the given biome.
		 * @param index the MF identifier as assigned by {@link #addBiome(RockBiome)}
		 * @param xInChunk the X coordinate of the target column in center chunk
		 * @param zInChunk the Z coordinate of the target column in center chunk
		 * @param value the amount to add to the multiplier value
		 */
		public void addToMultiplier(int index, int xInChunk, int zInChunk, double value) {
			multiplierFields[index][toMFIndex(xInChunk, zInChunk)] += value;
		}
		
		/**
		 * Retrieves the multiplier value from the MF at <code>index</code>.
		 * @param index the MF identifier as assigned by {@link #addBiome(RockBiome)}
		 * @param xInChunk the X coordinate of the target column in center chunk
		 * @param zInChunk the Z coordinate of the target column in center chunk
		 * @return the multiplier value
		 */
		public double getMultiplier(int index, int xInChunk, int zInChunk) {
			return multiplierFields[index][toMFIndex(xInChunk, zInChunk)];
		}
		
		/**
		 * Translates center chunk column coordinate pairs to indices in MF.
		 * @param xInChunk the X coordinate of the target column in center chunk
		 * @param zInChunk the Z coordinate of the target column in center chunk
		 * @return the index of the corresponding entry in the MF
		 */
		private int toMFIndex(int xInChunk, int zInChunk) {
			return xInChunk * CHUNK_SIZE + zInChunk;
		}
		
	}
	
	private final String name;
	private final int dimensionId;
	private final long seed;
	
	public Dimension(String name, int dimensionId, long seed) {
		this.name = name;
		this.dimensionId = dimensionId;
		this.seed = seed;
	}

	public abstract void processChunk(GenerationRequest request, Workspace w);
	
	protected void cacheBiomes(int centerChunkX, int centerChunkZ, DiscreteField2D<RockBiome> biomeSupplier, Cache c, Workspace w) {
		c.centerBiome = biomeSupplier.get(centerChunkX, centerChunkZ);
		
		// Explore neighbor chunks
		for (int dChunkX = -1; dChunkX <= 1; ++dChunkX) {
			int chunkX = centerChunkX + dChunkX;
			for (int dChunkZ = -1; dChunkZ <= 1; ++dChunkZ) {
				int chunkZ = centerChunkZ + dChunkZ;
				
				// Skip center chunk
				if (dChunkX == 0 && dChunkZ == 0) continue;
				
				RockBiome biome = biomeSupplier.get(chunkX, chunkZ);
				int index = c.addBiome(biome);
				
				if (index < 0) continue;
				
				// Add to MF
				for (int xInChunk = 0; xInChunk < CHUNK_SIZE; ++xInChunk) {
					for (int zInChunk = 0; zInChunk < CHUNK_SIZE; ++zInChunk) {
						double multiplier = getMultiplier(dChunkX, xInChunk) + getMultiplier(dChunkZ, zInChunk);
						c.addToMultiplier(index, xInChunk, zInChunk, multiplier);
					}
				}
			}
		}
	}
	
	/**
	 * Evaluates the rock biome multiplier for given column along one axis.
	 * @param position <tt>{-1; 0; 1}</tt> to indicate position of the biome's chunk along the axis relative to the center chunk 
	 * @param coordinate the coordinate of current column in center chunk (<tt>[0; 16)</tt>)
	 * @return the multiplier (<tt>[0; 1)</tt>)
	 */
	public static double getMultiplier(int position, int coordinate) {
		switch (position) {
		case -1:
			return multiplierGradient(1 - coordinate / (double) CHUNK_SIZE);
		case 0:
			return 1;
		case 1:
			return multiplierGradient(coordinate / (double) CHUNK_SIZE);
		default:
			throw new IllegalArgumentException("position = " + position);
		}
	}
	
	private static double multiplierGradient(double x) {
		return (x) * (x);
	}
	
	protected void cacheColumn(ColumnData column, Cache c, Workspace w) {
		c.centerBiome.cacheColumn(column, w);
		
		if (c.areNeighborsDifferent) {
			for (int i = 0; i < c.biomeSetSize; ++i) {
				c.biomeSet[i].cacheColumn(column, w);
			}
		}
	}
	
	/**
	 * @return the dimension ID
	 */
	public int getDimensionId() {
		return dimensionId;
	}
	
	/**
	 * @return the seed
	 */
	public long getSeed() {
		return seed;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Dimension " + name + " (" + getDimensionId() + ")";
	}

	/**
	 * @param chunkX
	 * @param chunkZ
	 * @return
	 */
	public abstract RockBiome getRegolithBiomeAt(double chunkX, double chunkZ);
	
}
