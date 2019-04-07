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

public class ChunkData {
	
	public static final int CHUNK_SIZE = 16, CHUNK_HEIGHT = 256;
	public static final short AIR_MGID = 0;
	public static final int METADATA_SIZE = 4;
	
	// This array is "big", using shorts to save RAM (also reason for short MGIDs)
	protected final short[] data = new short[CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT];
	
	// This array is "short" enough, using ints to optimize performance
	protected final int[] heightMap = new int[CHUNK_SIZE * CHUNK_SIZE];
	
	public short getBlockMGId(int x, int z, int y) {
		assert y >= 0: "y is invalid: " + y + " given, y > 0 expected";
		if (getHeight(x, z) < y) {
			return AIR_MGID;
		}
		
		return data[x * (CHUNK_SIZE * CHUNK_HEIGHT) + z * CHUNK_HEIGHT + y];
	}

	public int getBlockId(int x, int z, int y) {
		return getBlockMGId(x, z, y) >> METADATA_SIZE; // Bits: 1111 1111 1111 0000
	}
	
	public int getBlockMetadata(int x, int z, int y) {
		return getBlockMGId(x, z, y) & ~(~0 << METADATA_SIZE); // Bits: 0000 0000 0000 1111
	}
	
	public void setBlock(int x, int z, int y, short blockMGId) {
		data[x * (CHUNK_SIZE * CHUNK_HEIGHT) + z * CHUNK_HEIGHT + y] = blockMGId;
	}
	
	public int getHeight(int x, int z) {
		assert x >= 0 && x < CHUNK_SIZE: "x is invalid: " + x + " given, [0; " + CHUNK_SIZE + ") expected";
		assert z >= 0 && z < CHUNK_SIZE: "z is invalid: " + z + " given, [0; " + CHUNK_SIZE + ") expected";
		return heightMap[z * CHUNK_SIZE + x];
	}
	
	public void setHeight(int x, int z, int y) {
		heightMap[z * CHUNK_SIZE + x] = y;
	}
	
	public int[] getHeightMap() {
		return heightMap;
	}
	
	public static short getMGID(int id, int meta) {
		return (short) ((id << METADATA_SIZE) | meta);
	}
	
	public static int getId(short mgId) {
		return mgId >> METADATA_SIZE; // Bits: 1111 1111 1111 0000
	}
	
	public static int getMeta(short mgId) {
		return mgId & ~(~0 << METADATA_SIZE); // Bits: 0000 0000 0000 1111
	}

}
