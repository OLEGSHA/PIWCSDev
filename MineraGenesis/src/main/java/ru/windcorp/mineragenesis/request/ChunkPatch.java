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
package ru.windcorp.mineragenesis.request;

import ru.windcorp.mineragenesis.util.UnsafeFastBitset;

public class ChunkPatch extends ChunkData {
	
	private static final short[] EMPTY_DATA = new short[CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT];
	private static final long[] EMPTY_SET_FLAGS = new long[CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT / 8];
	private static final int[] EMPTY_HEIGHT_MAP = new int[CHUNK_SIZE * CHUNK_SIZE];
	
	private final UnsafeFastBitset setBlocks = new UnsafeFastBitset(CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT);
	
	public boolean hasBlock(int x, int z, int y) {
		return setBlocks.get(x * (CHUNK_SIZE * CHUNK_HEIGHT) + z * CHUNK_HEIGHT + y);
	}
	
	@Override
	public void setBlock(int x, int z, int y, short blockMGId) {
		assert x >= 0 && x < CHUNK_SIZE: "x is invalid: " + x + " given, [0; " + CHUNK_SIZE + ") expected";
		assert z >= 0 && z < CHUNK_SIZE: "z is invalid: " + z + " given, [0; " + CHUNK_SIZE + ") expected";
		assert y >= 0 && y < CHUNK_HEIGHT: "y is invalid: " + y + " given, [0; " + CHUNK_HEIGHT + ") expected";
		super.setBlock(x, z, y, blockMGId);
		
		setBlocks.set(x * (CHUNK_SIZE * CHUNK_HEIGHT) + z * CHUNK_HEIGHT + y);
		
		if (getHeight(x, z) < y + 1) setHeight(x, z, y + 1);
	}
	
	public void setBlock(int x, int z, int y, int blockId, int blockMeta) {
		setBlock(x, z, y, (short) ( (blockId << METADATA_SIZE) | blockMeta ));
	}
	
	public void clear() {
		System.arraycopy(EMPTY_DATA, 0, data, 0, data.length);
		System.arraycopy(EMPTY_HEIGHT_MAP, 0, heightMap, 0, heightMap.length);
		setBlocks.copyData(EMPTY_SET_FLAGS);
	}

}
