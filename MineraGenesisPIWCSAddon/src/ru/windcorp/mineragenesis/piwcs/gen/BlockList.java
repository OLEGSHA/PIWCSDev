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

import ru.windcorp.mineragenesis.request.ChunkData;

public class BlockList {
	
	private static final boolean[] EVERYTHING = new boolean[0];
	
	private final boolean[][] flags;
	
	public BlockList(int[] ids, int[] metas) {
		int maxId = 0;
		for (int i = 0; i < ids.length; ++i) {
			if (ids[i] > maxId) {
				maxId = ids[i];
			}
		}
		
		flags = new boolean[maxId + 1][];
		
		for (int i = 0; i < ids.length; ++i) {
			if (metas[i] == -1) {
				flags[ids[i]] = EVERYTHING;
				continue;
			}
			
			if (flags[ids[i]] == null) {
				flags[ids[i]] = new boolean[16];
			}
			
			flags[ids[i]][metas[i]] = true;
		}
	}
	
	public BlockList(int... mess) {
		int maxId = 0;
		for (int i = 0; i < mess.length / 2; ++i) {
			if (mess[2 * i] > maxId) {
				maxId = mess[2 * i];
			}
		}
		
		flags = new boolean[maxId + 1][];
		
		for (int i = 0; i < mess.length / 2; ++i) {
			if (mess[2 * i + 1] == -1) {
				flags[mess[2 * i]] = EVERYTHING;
				continue;
			}
			
			if (flags[mess[2 * i]] == null) {
				flags[mess[2 * i]] = new boolean[16];
			}
			
			flags[mess[2 * i]][mess[2 * i + 1]] = true;
		}
	}
	
	public boolean contains(int id, int meta) {
		if (id >= flags.length) {
			return false;
		}
		if (flags[id] == null) {
			return false;
		}
		if (flags[id] == EVERYTHING) {
			return true;
		}
		return flags[id][meta];
	}
	
	public boolean contains(short mgId) {
		return contains(ChunkData.getId(mgId), ChunkData.getMeta(mgId));
	}

	public boolean[][] getFlags() {
		return flags;
	}

	public static boolean[] getEverythingFlag() {
		return EVERYTHING;
	}

}
