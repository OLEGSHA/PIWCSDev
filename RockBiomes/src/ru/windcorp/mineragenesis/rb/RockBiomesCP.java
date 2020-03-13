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
package ru.windcorp.mineragenesis.rb;

import java.util.Collection;
import java.util.Random;

import ru.windcorp.mineragenesis.interfaces.MGChunkProcessor;
import ru.windcorp.mineragenesis.rb.gen.*;
import ru.windcorp.mineragenesis.request.GenerationRequest;

public class RockBiomesCP implements MGChunkProcessor {
	
	/**
	 * A container class for various cached variables.
	 * Its only use is to allow safe object reuse.
	 * Workspace is not thread-safe and must not be used with recursion.
	 * @author Javapony
	 */
	public class Workspace {
		
		private final BlockCollector collector = new BlockCollector();
		private final BlockData blockData = new BlockData();
		private final Dimension.Cache dimensionCacheRegolith = new Dimension.Cache();
		private final Dimension.Cache dimensionCacheBedrock = new Dimension.Cache();
		private final Deposit.Cache[] depositCaches = Deposit.createDepositCaches();
		
		private Random random;
		
		/**
		 * <b>Resets</b> and returns this workspace's {@link BlockCollector}.
		 * @return a reset collector
		 */
		public BlockCollector getCollector() {
			collector.reset();
			return collector;
		}
		
		/**
		 * Returns this workspace's {@link BlockData}.
		 * @return a BlockData object
		 */
		public BlockData getBlockData() {
			return blockData;
		}
		
		/**
		 * Returns this workspace's {@link BlockData}'s {@link ColumnData}.
		 * @return a ColumnData object
		 */
		public ColumnData getColumnData() {
			return blockData.column;
		}
		
		/**
		 * <b>Resets</b> and returns this workspace's {@link Dimension.Cache} for regolith.
		 * @return the dimension cache
		 */
		public Dimension.Cache getDimensionCacheRegolith() {
			dimensionCacheRegolith.reset();
			return dimensionCacheRegolith;
		}
		
		/**
		 * <b>Resets</b> and returns this workspace's {@link Dimension.Cache} for bedrock.
		 * @return the dimension cache
		 */
		public Dimension.Cache getDimensionCacheBedrock() {
			dimensionCacheBedrock.reset();
			return dimensionCacheBedrock;
		}

		/**
		 * Returns this workspace's {@link Deposit.Cache} for given {@link Deposit}.
		 * @return the deposit cache
		 */
		public Deposit.Cache getDepositCache(Deposit deposit) {
			return depositCaches[deposit.getId()];
		}

		public Random getRandom() {
			return random;
		}

		private void setRandom(long seed, int x, int z) {
			this.random = new Random(seed ^ x ^ z);
		}
		
	}

	private final ThreadLocal<Workspace> workspaces = ThreadLocal.withInitial(Workspace::new);
	
	private final Dimension[] dimensionsNonNegative;
	private final Dimension[] dimensionsNegative;

	public RockBiomesCP(Collection<Dimension> dimensions) {
		int nonNegative = -1, negative = -1;
		
		for (Dimension dim : dimensions) {
			int id = dim.getDimensionId();
			if (id >= 0) {
				nonNegative = Math.max(nonNegative, id);
			} else {
				negative = Math.max(negative, -id - 1);
			}
		}
		
		this.dimensionsNonNegative = new Dimension[nonNegative + 1];
		this.dimensionsNegative = new Dimension[negative + 1];
		
		for (Dimension dim : dimensions) {
			int id = dim.getDimensionId();
			if (id >= 0) {
				dimensionsNonNegative[id] = dim;
			} else {
				dimensionsNegative[-id - 1] = dim;
			}
		}
	}

	@Override
	public void processChunk(GenerationRequest request) {
		Dimension dim = getDimension(request.getChunk().dimension);
		if (dim == null) return;
		
		Workspace workspace = workspaces.get();
		workspace.setRandom(dim.getSeed(), request.getChunk().chunkX, request.getChunk().chunkZ);
		dim.processChunk(request, workspace);
	}

	public Dimension getDimension(int id) {
		int index;
		Dimension[] dimArray;
		
		if (id >= 0) {
			index = id;
			dimArray = dimensionsNonNegative;
		} else {
			index = -id - 1;
			dimArray = dimensionsNegative;
		}
			
		if (index >= dimArray.length) {
			return null;
		} else {
			return dimArray[index];
		}
	}
	
	public Dimension[] getDimensions() {
		Dimension[] result = new Dimension[dimensionsNonNegative.length + dimensionsNegative.length];
		System.arraycopy(dimensionsNonNegative, 0, result, 0, dimensionsNonNegative.length);
		System.arraycopy(dimensionsNegative, 0, result, dimensionsNonNegative.length, dimensionsNegative.length);
		return result;
	}
	
	@Override
	public String toString() {
		return "RockBiomes Chunk Processor";
	}

}
