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

import ru.windcorp.mineragenesis.rb.RockBiomesAddon;
import ru.windcorp.mineragenesis.rb.RockBiomesCP.Workspace;
import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;
import ru.windcorp.mineragenesis.rb.fields.DiscreteField2D;
import ru.windcorp.mineragenesis.rb.fields.Field2D;
import ru.windcorp.mineragenesis.request.ChunkLocator;
import ru.windcorp.mineragenesis.request.GenerationRequest;

import static ru.windcorp.mineragenesis.request.ChunkData.*;

import java.util.function.Consumer;

public class DimensionComplex extends Dimension {
	
	private final DiscreteField2D<RockBiomeType> regolith;
	private final RockBiomeType bedrock;
	private final Field2D bedrockHeight;

	public DimensionComplex(
			String name, int id,
			DiscreteField2D<RockBiomeType> regolith, RockBiomeType bedrock, Field2D bedrockHeight,
			long seed) {
		super(name, id, seed);
		this.regolith = regolith;
		this.bedrock = bedrock;
		this.bedrockHeight = bedrockHeight;
	}
	
	@SuppressWarnings("unchecked")
	public static DimensionComplex build(Arguments args) throws ConfigurationException {
		return new DimensionComplex(
				args.get("name", String.class),
				args.get("id", Double.class).intValue(),
				(DiscreteField2D<RockBiomeType>) args.get("regolith", DiscreteField2D.class),
				args.get("bedrock", RockBiomeType.class),
				args.get("bedrockHeight", Field2D.class),
				RockBiomesAddon.computeSeed(args.get("seed", String.class))
		);
	}

	@Override
	public void processChunk(GenerationRequest request, Workspace w) {
		ChunkLocator chunk = request.getChunk();
		
		Cache cacheRegolith = w.getDimensionCacheRegolith();
		cacheBiomes(chunk.chunkX, chunk.chunkZ, this::getRegolithBiomeAt, cacheRegolith, w);
		
		Cache cacheBedrock = w.getDimensionCacheBedrock();
		cacheBiomes(chunk.chunkX, chunk.chunkZ, bedrock, cacheBedrock, w);
		
		BlockData block = w.getBlockData();
		ColumnData column = block.column;
		
		for (int xInChunk = 0; xInChunk < CHUNK_SIZE; ++xInChunk) {
			column.setX(chunk.chunkX * CHUNK_SIZE + xInChunk);
			
			for (int zInChunk = 0; zInChunk < CHUNK_SIZE; ++zInChunk) {
				column.setZ(chunk.chunkZ * CHUNK_SIZE + zInChunk);
				
				int maxHeight = request.getOriginal().getHeight(xInChunk, zInChunk);
				int bedrockHeight = Math.min((int) this.bedrockHeight.get(column.xDouble, column.zDouble), maxHeight);
				
				cacheColumn(column, cacheBedrock, w);
				for (block.yInt = 1; block.yInt < bedrockHeight; ++block.yInt) processBlock(cacheBedrock, w, request);
				
				cacheColumn(column, cacheRegolith, w);
				for (; block.yInt <= maxHeight; ++block.yInt) processBlock(cacheRegolith, w, request);
			}
		}
	}
	
	/**
	 * @param cacheBedrock
	 * @param block
	 */
	private void processBlock(Cache c, Workspace w, GenerationRequest request) {
		BlockCollector collector = w.getCollector();
		BlockData block = w.getBlockData();
		
		int xInChunk = block.column.xInt & 0xF;
		int zInChunk = block.column.zInt & 0xF;
		
		if (RockBiomesAddon.CAN_DEBUG && RockBiomesAddon.isDebugging()) {
			block.original = 0x10; // Stone
		} else {
			block.original = request.getOriginal().getBlockMGId(xInChunk, zInChunk, block.yInt);
		}
		
		c.centerBiome.addBlocks(block, collector, w);
		
		if (c.areNeighborsDifferent) {
			for (int i = 0; i < c.biomeSetSize; ++i) {
				collector.pushMultiplier(c.getMultiplier(i, xInChunk, zInChunk));
				c.biomeSet[i].addBlocks(block, collector, w);
				collector.popMultiplier();
			}
		}
		
		short result = collector.get(block, w);
		if (!collector.hasDepleted()) {
			request.getPatch().setBlock(xInChunk, zInChunk, block.yInt, result);
		} else if (RockBiomesAddon.CAN_DEBUG && RockBiomesAddon.isDebugging()) {
			request.getPatch().setBlock(xInChunk, zInChunk, block.yInt, AIR_MGID);
		}
	}

	@Override
	public RockBiome getRegolithBiomeAt(double chunkX, double chunkZ) {
		return regolith.get(chunkX, chunkZ).get(chunkX, chunkZ);
	}
	
	@Override
	public void forEachRegolithBiome(Consumer<RockBiome> consumer) {
		for (RockBiomeType rbt : getRegolith().getAll()) {
			for (RockBiome rb : rbt.getAll()) {
				consumer.accept(rb);
			}
		}
	}
	
	/**
	 * @return the regolith
	 */
	public DiscreteField2D<RockBiomeType> getRegolith() {
		return regolith;
	}
	
	/**
	 * @return the bedrock
	 */
	public RockBiomeType getBedrock() {
		return bedrock;
	}
	
	/**
	 * @return the bedrockHeight
	 */
	public Field2D getBedrockHeight() {
		return bedrockHeight;
	}

}
